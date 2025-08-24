package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.service.ServerService;
import com.cmict.internalpaas.service.SSHTerminalService;
import com.cmict.internalpaas.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SSHTerminalWebSocketHandler implements WebSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(SSHTerminalWebSocketHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private SSHTerminalService sshTerminalService;
    
    @Autowired
    private ServerService serverService;
    
    @Autowired
    private UserService userService;
    
    // 存储WebSocket会话与SSH会话的映射
    private final Map<String, String> webSocketToSSHMapping = new ConcurrentHashMap<>();
    
    // 存储WebSocket会话的发送锁，防止并发发送消息
    private final Map<String, Object> sessionSendLocks = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("WebSocket连接建立: {}", session.getId());
        // 为每个会话创建发送锁
        sessionSendLocks.put(session.getId(), new Object());
    }
    
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (message instanceof TextMessage) {
            handleTextMessage(session, (TextMessage) message);
        }
    }
    
    private void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message.getPayload());
            String type = jsonNode.get("type").asText();
            
            switch (type) {
                case "connect":
                    handleConnect(session, jsonNode);
                    break;
                case "input":
                    handleInput(session, jsonNode);
                    break;
                case "resize":
                    handleResize(session, jsonNode);
                    break;
                case "ping":
                    handlePing(session);
                    break;
                default:
                    logger.warn("未知的消息类型: {}", type);
            }
            
        } catch (Exception e) {
            logger.error("处理WebSocket消息失败", e);
            sendErrorMessage(session, "处理消息失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理SSH连接请求
     */
    private void handleConnect(WebSocketSession session, JsonNode jsonNode) {
        try {
            Long serverId = jsonNode.get("serverId").asLong();
            String terminalType = jsonNode.has("terminalType") ? jsonNode.get("terminalType").asText() : "xterm-256color";
            String windowSize = jsonNode.has("windowSize") ? jsonNode.get("windowSize").asText() : "80x24";
            
            // 获取当前用户（多种方式尝试）
            String username = getCurrentUsername(session, jsonNode);
            if (username == null) {
                throw new RuntimeException("无法获取用户认证信息，请确保已登录");
            }
            
            User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username));
            
            // 获取服务器信息
            Server server = serverService.findById(serverId)
                .orElseThrow(() -> new RuntimeException("服务器不存在: " + serverId));
            
            // 创建SSH会话
            String sshSessionId = sshTerminalService.createSSHSession(server, user, session, terminalType, windowSize);
            
            // 建立映射关系
            webSocketToSSHMapping.put(session.getId(), sshSessionId);
            
            // 发送连接成功消息
            sendMessage(session, Map.of(
                "type", "connected",
                "sessionId", sshSessionId,
                "serverId", serverId,
                "serverName", server.getName(),
                "username", username,
                "message", "SSH连接建立成功"
            ));
            
        } catch (Exception e) {
            logger.error("建立SSH连接失败", e);
            sendErrorMessage(session, "连接失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理终端输入
     */
    private void handleInput(WebSocketSession session, JsonNode jsonNode) {
        try {
            String input = jsonNode.get("data").asText();
            String sshSessionId = webSocketToSSHMapping.get(session.getId());
            
            if (sshSessionId != null) {
                sshTerminalService.sendCommand(sshSessionId, input);
            } else {
                sendErrorMessage(session, "SSH会话不存在");
            }
            
        } catch (Exception e) {
            logger.error("处理终端输入失败", e);
            sendErrorMessage(session, "输入处理失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理终端窗口大小调整
     */
    private void handleResize(WebSocketSession session, JsonNode jsonNode) {
        try {
            int cols = jsonNode.get("cols").asInt();
            int rows = jsonNode.get("rows").asInt();
            String sshSessionId = webSocketToSSHMapping.get(session.getId());
            
            if (sshSessionId != null) {
                sshTerminalService.resizeTerminal(sshSessionId, cols, rows);
            }
            
        } catch (Exception e) {
            logger.error("调整终端大小失败", e);
        }
    }
    
    /**
     * 处理心跳消息
     */
    private void handlePing(WebSocketSession session) {
        try {
            sendMessage(session, Map.of("type", "pong"));
        } catch (Exception e) {
            logger.error("发送心跳响应失败", e);
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket传输错误: {}", session.getId(), exception);
        closeSSHSession(session);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        logger.info("WebSocket连接关闭: {}, 状态: {}", session.getId(), closeStatus);
        closeSSHSession(session);
        // 清理发送锁
        sessionSendLocks.remove(session.getId());
    }
    
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
    
    /**
     * 获取当前用户名（多种方式尝试）
     */
    private String getCurrentUsername(WebSocketSession session, JsonNode jsonNode) {
        // 方式1：从 SecurityContext 获取
        try {
            SecurityContext securityContext = SecurityContextHolder.getContext();
            if (securityContext != null && securityContext.getAuthentication() != null) {
                Authentication auth = securityContext.getAuthentication();
                if (auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
                    logger.debug("从SecurityContext获取用户名: {}", auth.getName());
                    return auth.getName();
                }
            }
        } catch (Exception e) {
            logger.debug("从SecurityContext获取用户名失败", e);
        }
        
        // 方式2：从WebSocket会话属性获取（通过HttpSessionHandshakeInterceptor传递）
        try {
            Object principal = session.getAttributes().get("SPRING_SECURITY_CONTEXT");
            if (principal instanceof SecurityContext) {
                SecurityContext ctx = (SecurityContext) principal;
                if (ctx.getAuthentication() != null) {
                    String username = ctx.getAuthentication().getName();
                    if (username != null && !"anonymousUser".equals(username)) {
                        logger.debug("从WebSocket会话属性获取用户名: {}", username);
                        return username;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("从WebSocket会话属性获取用户名失败", e);
        }
        
        // 方式3：从消息体中获取（前端可以传递username参数）
        try {
            if (jsonNode.has("username")) {
                String username = jsonNode.get("username").asText();
                if (username != null && !username.trim().isEmpty()) {
                    // 验证用户是否存在（安全检查）
                    if (userService.existsByUsername(username)) {
                        logger.debug("从消息体获取用户名: {}", username);
                        return username;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("从消息体获取用户名失败", e);
        }
        
        // 方式4：从HTTP会话中获取
        try {
            Object httpSessionObj = session.getAttributes().get("HTTP_SESSION");
            if (httpSessionObj instanceof javax.servlet.http.HttpSession) {
                javax.servlet.http.HttpSession httpSession = (javax.servlet.http.HttpSession) httpSessionObj;
                Object authObj = httpSession.getAttribute("SPRING_SECURITY_CONTEXT");
                if (authObj instanceof SecurityContext) {
                    SecurityContext ctx = (SecurityContext) authObj;
                    if (ctx.getAuthentication() != null) {
                        String username = ctx.getAuthentication().getName();
                        if (username != null && !"anonymousUser".equals(username)) {
                            logger.debug("从HTTP会话获取用户名: {}", username);
                            return username;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("从HTTP会话获取用户名失败", e);
        }
        
        logger.warn("无法获取用户认证信息，WebSocket会话 ID: {}", session.getId());
        return null;
    }

    /**
     * 关闭SSH会话
     */
    private void closeSSHSession(WebSocketSession session) {
        String sshSessionId = webSocketToSSHMapping.remove(session.getId());
        if (sshSessionId != null) {
            try {
                sshTerminalService.closeSession(sshSessionId);
            } catch (Exception e) {
                logger.error("关闭SSH会话失败: {}", sshSessionId, e);
            }
        }
    }
    
    /**
     * 发送消息到WebSocket
     */
    private void sendMessage(WebSocketSession session, Object message) {
        if (session == null || !session.isOpen()) {
            return;
        }
        
        Object lock = sessionSendLocks.get(session.getId());
        if (lock == null) {
            logger.warn("未找到会话发送锁: {}", session.getId());
            return;
        }
        
        synchronized (lock) {
            try {
                if (session.isOpen()) {
                    String jsonMessage = objectMapper.writeValueAsString(message);
                    session.sendMessage(new TextMessage(jsonMessage));
                    logger.debug("成功发送WebSocket消息到会话: {}", session.getId());
                } else {
                    logger.debug("WebSocket会话已关闭，跳过发送消息: {}", session.getId());
                }
            } catch (IllegalStateException e) {
                // 处理WebSocket状态异常
                if (e.getMessage() != null && e.getMessage().contains("TEXT_PARTIAL_WRITING")) {
                    logger.warn("WebSocket正在写入中，稍后重试发送消息: {}", session.getId());
                    // 可以选择重试或者直接丢弃消息
                    try {
                        Thread.sleep(10); // 短暂等待
                        if (session.isOpen()) {
                            String jsonMessage = objectMapper.writeValueAsString(message);
                            session.sendMessage(new TextMessage(jsonMessage));
                            logger.debug("重试发送WebSocket消息成功: {}", session.getId());
                        }
                    } catch (Exception retryException) {
                        logger.error("重试发送WebSocket消息失败: {}", session.getId(), retryException);
                    }
                } else {
                    logger.error("WebSocket状态异常: {}", session.getId(), e);
                }
            } catch (Exception e) {
                logger.error("发送WebSocket消息失败: {}", session.getId(), e);
            }
        }
    }
    
    /**
     * 发送错误消息
     */
    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        sendMessage(session, Map.of(
            "type", "error",
            "message", errorMessage
        ));
    }
}