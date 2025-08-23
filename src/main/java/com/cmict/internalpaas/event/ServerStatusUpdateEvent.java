package com.cmict.internalpaas.event;

import com.cmict.internalpaas.model.Server;
import org.springframework.context.ApplicationEvent;

/**
 * 服务器状态更新事件
 * 用于解耦ServerService和WebSocketController，避免循环依赖
 */
public class ServerStatusUpdateEvent extends ApplicationEvent {
    
    private final Server server;
    
    public ServerStatusUpdateEvent(Object source, Server server) {
        super(source);
        this.server = server;
    }
    
    public Server getServer() {
        return server;
    }
}