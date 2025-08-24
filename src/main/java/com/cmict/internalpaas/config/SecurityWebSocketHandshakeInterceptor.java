package com.cmict.internalpaas.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * 安全WebSocket握手拦截器
 * 确保用户认证信息能够正确传递到WebSocket会话中
 */
public class SecurityWebSocketHandshakeInterceptor implements HandshakeInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityWebSocketHandshakeInterceptor.class);
    
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                 WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        
        logger.debug("WebSocket握手开始，尝试获取用户认证信息");
        
        // 从当前SecurityContext获取认证信息
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            if (username != null && !"anonymousUser".equals(username)) {
                // 将认证信息存储到WebSocket属性中
                attributes.put("SPRING_SECURITY_CONTEXT", securityContext);
                attributes.put("USERNAME", username);
                attributes.put("AUTHENTICATION", authentication);
                
                logger.debug("成功从SecurityContext获取用户认证信息: {}", username);
            }
        }
        
        // 如果是HTTP请求，尝试从HTTP会话获取认证信息
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            HttpSession httpSession = httpRequest.getSession(false);
            
            if (httpSession != null) {
                // 存储HTTP会话到WebSocket属性
                attributes.put("HTTP_SESSION", httpSession);
                
                // 尝试从HTTP会话获取Spring Security上下文
                Object sessionSecurityContext = httpSession.getAttribute("SPRING_SECURITY_CONTEXT");
                if (sessionSecurityContext instanceof SecurityContext) {
                    SecurityContext sessionCtx = (SecurityContext) sessionSecurityContext;
                    Authentication sessionAuth = sessionCtx.getAuthentication();
                    
                    if (sessionAuth != null && sessionAuth.isAuthenticated()) {
                        String sessionUsername = sessionAuth.getName();
                        if (sessionUsername != null && !"anonymousUser".equals(sessionUsername)) {
                            // 如果当前SecurityContext没有认证信息，使用会话中的
                            if (!attributes.containsKey("USERNAME")) {
                                attributes.put("SPRING_SECURITY_CONTEXT", sessionCtx);
                                attributes.put("USERNAME", sessionUsername);
                                attributes.put("AUTHENTICATION", sessionAuth);
                                
                                logger.debug("从HTTP会话获取用户认证信息: {}", sessionUsername);
                            }
                        }
                    }
                }
                
                // 记录会话信息用于调试
                logger.debug("HTTP会话ID: {}, 创建时间: {}", 
                    httpSession.getId(), httpSession.getCreationTime());
            }
        }
        
        // 记录所有传递给WebSocket的属性（用于调试）
        if (logger.isDebugEnabled()) {
            attributes.forEach((key, value) -> {
                if (!"HTTP_SESSION".equals(key)) { // 避免记录敏感信息
                    logger.debug("WebSocket属性: {} = {}", key, value);
                }
            });
        }
        
        return true; // 允许握手继续
    }
    
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                             WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            logger.error("WebSocket握手完成时发生异常", exception);
        } else {
            logger.debug("WebSocket握手成功完成");
        }
    }
}