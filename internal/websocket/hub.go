package websocket

import (
	"sync"

	"go.uber.org/zap"
)

// MessageType 消息类型
type MessageType string

const (
	MessageTypeBuildLog    MessageType = "build_log"
	MessageTypeBuildStatus MessageType = "build_status"
	MessageTypeError       MessageType = "error"
	MessageTypeInfo        MessageType = "info"
)

// Message WebSocket消息
type Message struct {
	Type      MessageType            `json:"type"`
	BuildID   uint                   `json:"build_id,omitempty"`
	Timestamp string                 `json:"timestamp"`
	Data      map[string]interface{} `json:"data"`
}

// Client WebSocket客户端
type Client struct {
	ID       string
	UserID   uint
	BuildID  uint // 订阅的构建ID，0表示订阅所有
	Send     chan *Message
	hub      *Hub
}

// Hub WebSocket连接管理中心
type Hub struct {
	// 注册的客户端
	clients map[*Client]bool

	// 按构建ID索引的客户端
	buildClients map[uint]map[*Client]bool

	// 按用户ID索引的客户端
	userClients map[uint]map[*Client]bool

	// 注册请求
	register chan *Client

	// 注销请求
	unregister chan *Client

	// 广播消息
	broadcast chan *Message

	// 互斥锁
	mu sync.RWMutex

	// 日志记录器
	logger *zap.Logger
}

// NewHub 创建新的Hub
func NewHub(logger *zap.Logger) *Hub {
	return &Hub{
		clients:      make(map[*Client]bool),
		buildClients: make(map[uint]map[*Client]bool),
		userClients:  make(map[uint]map[*Client]bool),
		register:     make(chan *Client),
		unregister:   make(chan *Client),
		broadcast:    make(chan *Message, 256),
		logger:       logger,
	}
}

// Run 运行Hub
func (h *Hub) Run() {
	for {
		select {
		case client := <-h.register:
			h.registerClient(client)

		case client := <-h.unregister:
			h.unregisterClient(client)

		case message := <-h.broadcast:
			h.broadcastMessage(message)
		}
	}
}

// registerClient 注册客户端
func (h *Hub) registerClient(client *Client) {
	h.mu.Lock()
	defer h.mu.Unlock()

	h.clients[client] = true

	// 按用户ID索引
	if h.userClients[client.UserID] == nil {
		h.userClients[client.UserID] = make(map[*Client]bool)
	}
	h.userClients[client.UserID][client] = true

	// 如果订阅了特定构建，按构建ID索引
	if client.BuildID > 0 {
		if h.buildClients[client.BuildID] == nil {
			h.buildClients[client.BuildID] = make(map[*Client]bool)
		}
		h.buildClients[client.BuildID][client] = true

		h.logger.Info("Client registered with build subscription",
			zap.String("client_id", client.ID),
			zap.Uint("user_id", client.UserID),
			zap.Uint("build_id", client.BuildID),
		)
	} else {
		h.logger.Info("Client registered without build subscription",
			zap.String("client_id", client.ID),
			zap.Uint("user_id", client.UserID),
		)
	}

	h.logger.Info("Hub status",
		zap.Int("total_clients", len(h.clients)),
		zap.Int("build_subscriptions", len(h.buildClients)),
	)
}

// unregisterClient 注销客户端
func (h *Hub) unregisterClient(client *Client) {
	h.mu.Lock()
	defer h.mu.Unlock()

	if _, ok := h.clients[client]; ok {
		// 从总客户端列表中移除
		delete(h.clients, client)

		// 从用户索引中移除
		if clients, ok := h.userClients[client.UserID]; ok {
			delete(clients, client)
			if len(clients) == 0 {
				delete(h.userClients, client.UserID)
			}
		}

		// 从构建索引中移除
		if client.BuildID > 0 {
			if clients, ok := h.buildClients[client.BuildID]; ok {
				delete(clients, client)
				if len(clients) == 0 {
					delete(h.buildClients, client.BuildID)
				}
			}
		}

		// 关闭发送通道
		close(client.Send)

		h.logger.Info("Client unregistered",
			zap.String("client_id", client.ID),
			zap.Uint("user_id", client.UserID),
			zap.Uint("build_id", client.BuildID),
		)
	}
}

// broadcastMessage 广播消息
func (h *Hub) broadcastMessage(message *Message) {
	h.mu.RLock()
	defer h.mu.RUnlock()

	var targetClients []*Client

	// 如果消息指定了构建ID，只发送给订阅该构建的客户端
	if message.BuildID > 0 {
		if clients, ok := h.buildClients[message.BuildID]; ok {
			for client := range clients {
				targetClients = append(targetClients, client)
			}
		}
	} else {
		// 否则发送给所有客户端
		for client := range h.clients {
			targetClients = append(targetClients, client)
		}
	}

	// 发送消息
	for _, client := range targetClients {
		select {
		case client.Send <- message:
			// 消息已发送
		default:
			// 发送缓冲区已满，关闭客户端
			h.logger.Warn("Client send buffer full, closing",
				zap.String("client_id", client.ID),
			)
			go func(c *Client) {
				h.unregister <- c
			}(client)
		}
	}

	h.logger.Debug("Message broadcasted",
		zap.String("type", string(message.Type)),
		zap.Uint("build_id", message.BuildID),
		zap.Int("recipients", len(targetClients)),
	)
}

// BroadcastBuildLog 广播构建日志
func (h *Hub) BroadcastBuildLog(buildID uint, line string) {
	message := &Message{
		Type:    MessageTypeBuildLog,
		BuildID: buildID,
		Data: map[string]interface{}{
			"line": line,
		},
	}
	h.broadcast <- message
}

// BroadcastBuildStatus 广播构建状态
func (h *Hub) BroadcastBuildStatus(buildID uint, status string, data map[string]interface{}) {
	if data == nil {
		data = make(map[string]interface{})
	}
	data["status"] = status

	message := &Message{
		Type:    MessageTypeBuildStatus,
		BuildID: buildID,
		Data:    data,
	}
	h.broadcast <- message
}

// SendToUser 发送消息给特定用户的所有连接
func (h *Hub) SendToUser(userID uint, message *Message) {
	h.mu.RLock()
	defer h.mu.RUnlock()

	if clients, ok := h.userClients[userID]; ok {
		for client := range clients {
			select {
			case client.Send <- message:
				// 消息已发送
			default:
				// 发送缓冲区已满
				h.logger.Warn("Client send buffer full",
					zap.String("client_id", client.ID),
					zap.Uint("user_id", userID),
				)
			}
		}
	}
}

// GetClientCount 获取客户端总数
func (h *Hub) GetClientCount() int {
	h.mu.RLock()
	defer h.mu.RUnlock()
	return len(h.clients)
}

// GetBuildSubscriptionCount 获取特定构建的订阅数
func (h *Hub) GetBuildSubscriptionCount(buildID uint) int {
	h.mu.RLock()
	defer h.mu.RUnlock()

	if clients, ok := h.buildClients[buildID]; ok {
		return len(clients)
	}
	return 0
}

// GetUserConnectionCount 获取特定用户的连接数
func (h *Hub) GetUserConnectionCount(userID uint) int {
	h.mu.RLock()
	defer h.mu.RUnlock()

	if clients, ok := h.userClients[userID]; ok {
		return len(clients)
	}
	return 0
}
