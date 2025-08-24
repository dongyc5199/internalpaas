package com.cmict.internalpaas.config;

import com.cmict.internalpaas.controller.SSHTerminalWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * WebSocket配置类
 * 用于配置STOMP消息代理和WebSocket端点
 */
@Configuration
@EnableWebSocket
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer, WebSocketConfigurer {
    
    @Autowired
    private SSHTerminalWebSocketHandler sshTerminalWebSocketHandler;

    /**
     * 配置消息代理
     * /topic - 用于广播消息（如系统状态更新）
     * /user - 用于点对点消息（如个人日志流）
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单消息代理，用于向客户端发送消息
        config.enableSimpleBroker("/topic", "/user");
        
        // 设置应用消息的前缀，客户端发送消息时需要添加此前缀
        config.setApplicationDestinationPrefixes("/app");
        
        // 设置用户目标前缀，用于点对点消息
        config.setUserDestinationPrefix("/user");
    }

    /**
     * 注册STOMP端点
     * 客户端将通过此端点连接到WebSocket服务
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册WebSocket端点，并允许跨域
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // 允许所有来源（开发环境）
                .withSockJS();  // 启用SockJS fallback选项
        
        // 注册日志专用端点
        registry.addEndpoint("/ws/logs")
                .setAllowedOriginPatterns("*")
                .withSockJS();
                
        // 注册SSH终端专用端点
        registry.addEndpoint("/ws/terminal")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
    
    /**
     * 配置原WebSocket处理器（非STOMP）
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册SSH终端处理器，添加安全握手拦截器以传递用户认证信息
        registry.addHandler(sshTerminalWebSocketHandler, "/ws/ssh-terminal")
                .setAllowedOrigins("*")
                .addInterceptors(new SecurityWebSocketHandshakeInterceptor());
    }
}