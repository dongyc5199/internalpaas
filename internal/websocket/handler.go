package websocket

import (
	"encoding/json"
	"net/http"
	"strconv"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/gorilla/websocket"
	"go.uber.org/zap"
)

const (
	// 写入超时时间
	writeWait = 10 * time.Second

	// Pong等待时间
	pongWait = 60 * time.Second

	// Ping发送间隔 (必须小于pongWait)
	pingPeriod = (pongWait * 9) / 10

	// 最大消息大小
	maxMessageSize = 512 * 1024 // 512KB
)

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	CheckOrigin: func(r *http.Request) bool {
		// 生产环境应该检查Origin
		return true
	},
}

// Handler WebSocket处理器
type Handler struct {
	hub    *Hub
	logger *zap.Logger
}

// NewHandler 创建WebSocket处理器
func NewHandler(hub *Hub, logger *zap.Logger) *Handler {
	return &Handler{
		hub:    hub,
		logger: logger,
	}
}

// HandleConnection 处理WebSocket连接
func (h *Handler) HandleConnection(c *gin.Context) {
	// 获取用户ID (从认证中间件)
	userIDVal, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "未认证"})
		return
	}
	userID := userIDVal.(uint)

	// 获取构建ID (可选)
	var buildID uint
	if buildIDStr := c.Query("build_id"); buildIDStr != "" {
		if id, err := strconv.ParseUint(buildIDStr, 10, 32); err == nil {
			buildID = uint(id)
		}
	}

	// 升级HTTP连接为WebSocket
	conn, err := upgrader.Upgrade(c.Writer, c.Request, nil)
	if err != nil {
		h.logger.Error("Failed to upgrade connection", zap.Error(err))
		return
	}

	// 创建客户端
	client := &Client{
		ID:      uuid.New().String(),
		UserID:  userID,
		BuildID: buildID,
		Send:    make(chan *Message, 256),
		hub:     h.hub,
	}

	// 注册客户端
	h.hub.register <- client

	// 启动读写协程
	go h.writePump(conn, client)
	go h.readPump(conn, client)
}

// readPump 从WebSocket读取消息
func (h *Handler) readPump(conn *websocket.Conn, client *Client) {
	defer func() {
		h.hub.unregister <- client
		conn.Close()
	}()

	conn.SetReadDeadline(time.Now().Add(pongWait))
	conn.SetReadLimit(maxMessageSize)
	conn.SetPongHandler(func(string) error {
		conn.SetReadDeadline(time.Now().Add(pongWait))
		return nil
	})

	for {
		_, message, err := conn.ReadMessage()
		if err != nil {
			if websocket.IsUnexpectedCloseError(err, websocket.CloseGoingAway, websocket.CloseAbnormalClosure) {
				h.logger.Error("WebSocket read error",
					zap.String("client_id", client.ID),
					zap.Error(err),
				)
			}
			break
		}

		// 处理客户端消息 (例如：订阅新的构建)
		h.handleClientMessage(client, message)
	}
}

// writePump 向WebSocket写入消息
func (h *Handler) writePump(conn *websocket.Conn, client *Client) {
	ticker := time.NewTicker(pingPeriod)
	defer func() {
		ticker.Stop()
		conn.Close()
	}()

	for {
		select {
		case message, ok := <-client.Send:
			conn.SetWriteDeadline(time.Now().Add(writeWait))
			if !ok {
				// Hub关闭了通道
				conn.WriteMessage(websocket.CloseMessage, []byte{})
				return
			}

			// 添加时间戳
			if message.Timestamp == "" {
				message.Timestamp = time.Now().Format(time.RFC3339)
			}

			// 发送JSON消息
			if err := conn.WriteJSON(message); err != nil {
				h.logger.Error("Failed to write message",
					zap.String("client_id", client.ID),
					zap.Error(err),
				)
				return
			}

		case <-ticker.C:
			conn.SetWriteDeadline(time.Now().Add(writeWait))
			if err := conn.WriteMessage(websocket.PingMessage, nil); err != nil {
				return
			}
		}
	}
}

// handleClientMessage 处理客户端消息
func (h *Handler) handleClientMessage(client *Client, data []byte) {
	var msg struct {
		Type    string `json:"type"`
		BuildID uint   `json:"build_id,omitempty"`
	}

	if err := json.Unmarshal(data, &msg); err != nil {
		h.logger.Warn("Failed to parse client message",
			zap.String("client_id", client.ID),
			zap.Error(err),
		)
		return
	}

	switch msg.Type {
	case "subscribe_build":
		// 订阅新的构建
		if msg.BuildID > 0 && msg.BuildID != client.BuildID {
			h.logger.Info("Client subscribing to new build",
				zap.String("client_id", client.ID),
				zap.Uint("old_build_id", client.BuildID),
				zap.Uint("new_build_id", msg.BuildID),
			)

			// 先取消注册
			h.hub.unregister <- client

			// 更新构建ID
			client.BuildID = msg.BuildID

			// 重新注册
			h.hub.register <- client
		}

	case "ping":
		// 响应ping
		client.Send <- &Message{
			Type: MessageTypeInfo,
			Data: map[string]interface{}{
				"message": "pong",
			},
		}

	default:
		h.logger.Warn("Unknown message type",
			zap.String("client_id", client.ID),
			zap.String("type", msg.Type),
		)
	}
}

// GetStats 获取WebSocket统计信息
func (h *Handler) GetStats(c *gin.Context) {
	stats := gin.H{
		"total_clients": h.hub.GetClientCount(),
	}

	// 如果指定了构建ID，返回该构建的订阅数
	if buildIDStr := c.Query("build_id"); buildIDStr != "" {
		if buildID, err := strconv.ParseUint(buildIDStr, 10, 32); err == nil {
			stats["build_subscriptions"] = h.hub.GetBuildSubscriptionCount(uint(buildID))
		}
	}

	// 如果是管理员，可以看到更多统计信息
	if isAdmin, exists := c.Get("is_admin"); exists && isAdmin.(bool) {
		// 添加更多统计信息
	}

	c.JSON(http.StatusOK, stats)
}
