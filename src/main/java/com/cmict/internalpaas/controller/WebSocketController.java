package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.event.ServerStatusUpdateEvent;
import com.cmict.internalpaas.model.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket消息处理控制器
 * 处理客户端的WebSocket连接和消息
 */
@Controller
public class WebSocketController {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * 处理客户端连接消息
     */
    @MessageMapping("/connect")
    @SendTo("/topic/status")
    public Map<String, Object> handleConnect(@Payload Map<String, Object> message, 
                                           SimpMessageHeaderAccessor headerAccessor,
                                           Principal principal) {
        String username = principal != null ? principal.getName() : "Anonymous";
        String sessionId = headerAccessor.getSessionId();
        
        logger.info("用户 {} 已连接 WebSocket, Session: {}", username, sessionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "user_connected");
        response.put("username", username);
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        response.put("message", "用户已连接");
        
        return response;
    }
    
    /**
     * 处理日志订阅请求
     */
    @SubscribeMapping("/logs/{applicationId}")
    public Map<String, Object> handleLogSubscription(SimpMessageHeaderAccessor headerAccessor,
                                                   Principal principal) {
        String username = principal != null ? principal.getName() : "Anonymous";
        String sessionId = headerAccessor.getSessionId();
        
        logger.info("用户 {} 订阅日志流, Session: {}", username, sessionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "log_subscription");
        response.put("status", "success");
        response.put("message", "日志流订阅成功");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return response;
    }
    
    /**
     * 处理系统状态订阅
     */
    @SubscribeMapping("/status")
    public Map<String, Object> handleStatusSubscription(Principal principal) {
        String username = principal != null ? principal.getName() : "Anonymous";
        
        logger.info("用户 {} 订阅系统状态", username);
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "status_subscription");
        response.put("status", "success");
        response.put("message", "系统状态订阅成功");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return response;
    }
    
    /**
     * 向指定用户发送消息
     */
    public void sendToUser(String username, String destination, Object message) {
        messagingTemplate.convertAndSendToUser(username, destination, message);
    }
    
    /**
     * 广播消息给所有用户
     */
    public void broadcast(String destination, Object message) {
        messagingTemplate.convertAndSend(destination, message);
    }
    
    /**
     * 发送日志消息给特定用户
     */
    public void sendLogToUser(String username, Long applicationId, String logContent) {
        Map<String, Object> logMessage = new HashMap<>();
        logMessage.put("type", "log");
        logMessage.put("applicationId", applicationId);
        logMessage.put("content", logContent);
        logMessage.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        sendToUser(username, "/logs/" + applicationId, logMessage);
    }
    
    /**
     * 发送应用状态更新
     */
    public void sendApplicationStatusUpdate(Long applicationId, String status, String message) {
        Map<String, Object> statusMessage = new HashMap<>();
        statusMessage.put("type", "app_status");
        statusMessage.put("applicationId", applicationId);
        statusMessage.put("status", status);
        statusMessage.put("message", message);
        statusMessage.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        broadcast("/topic/app-status", statusMessage);
    }
    
    /**
     * 发送服务器状态更新
     */
    public void sendServerStatusUpdate(Server server) {
        Map<String, Object> statusMessage = new HashMap<>();
        statusMessage.put("type", "server_connection_status");
        statusMessage.put("serverId", server.getId());
        statusMessage.put("serverName", server.getName());
        statusMessage.put("connectionStatus", server.getConnectionStatus().name());
        statusMessage.put("connectionDescription", getConnectionStatusDescription(server.getConnectionStatus()));
        statusMessage.put("lastCheck", server.getLastConnectionCheck() != null ? 
            server.getLastConnectionCheck().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        statusMessage.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        logger.info("发送服务器状态更新: 服务器={}, 状态={}", 
            server.getName(), server.getConnectionStatus());
        
        broadcast("/topic/server-status", statusMessage);
    }
    
    /**
     * 获取连接状态描述
     */
    private String getConnectionStatusDescription(Server.ConnectionStatus status) {
        if (status == null) return "未知状态";
        
        switch (status) {
            case CONNECTED:
                return "连接正常";
            case MONITORING:
                return "监控中";
            case FAILED:
                return "连接失败";
            case TIMEOUT:
                return "连接超时";
            case AUTH_FAILED:
                return "认证失败";
            case UNKNOWN:
            default:
                return "未知状态";
        }
    }
    
    /**
     * 处理服务器日志订阅请求
     */
    @SubscribeMapping("/server-logs/{serverId}")
    public Map<String, Object> handleServerLogSubscription(@PathVariable Long serverId,
                                                          SimpMessageHeaderAccessor headerAccessor,
                                                          Principal principal) {
        String username = principal != null ? principal.getName() : "Anonymous";
        String sessionId = headerAccessor.getSessionId();
        
        logger.info("用户 {} 订阅服务器 {} 日志流, Session: {}", username, serverId, sessionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "server_log_subscription");
        response.put("serverId", serverId);
        response.put("status", "success");
        response.put("message", "服务器日志流订阅成功");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return response;
    }
    
    /**
     * 发送服务器日志消息给特定用户
     */
    public void sendServerLogToUser(String username, Long serverId, String filePath, Object logData) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "server_log");
        message.put("serverId", serverId);
        message.put("filePath", filePath);
        message.put("data", logData);
        message.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        sendToUser(username, "/topic/server-logs", message);
        logger.debug("发送服务器日志到用户 {}: 服务器={}, 文件={}", username, serverId, filePath);
    }
    
    /**
     * 广播服务器日志状态更新
     */
    public void broadcastServerLogStatus(Long serverId, String filePath, String status, String message) {
        Map<String, Object> statusMessage = new HashMap<>();
        statusMessage.put("type", "server_log_status");
        statusMessage.put("serverId", serverId);
        statusMessage.put("filePath", filePath);
        statusMessage.put("status", status);
        statusMessage.put("message", message);
        statusMessage.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        broadcast("/topic/server-logs-status", statusMessage);
    }
    
    /**
     * 监听服务器状态更新事件
     */
    @EventListener
    public void handleServerStatusUpdateEvent(ServerStatusUpdateEvent event) {
        sendServerStatusUpdate(event.getServer());
    }
}