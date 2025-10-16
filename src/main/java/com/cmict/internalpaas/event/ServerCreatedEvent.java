package com.cmict.internalpaas.event;

import com.cmict.internalpaas.model.Server;
import org.springframework.context.ApplicationEvent;

/**
 * 服务器创建事件
 * 当新服务器被添加到系统时触发此事件
 */
public class ServerCreatedEvent extends ApplicationEvent {

    private final Server server;

    public ServerCreatedEvent(Object source, Server server) {
        super(source);
        this.server = server;
    }

    public Server getServer() {
        return server;
    }

    @Override
    public String toString() {
        return "ServerCreatedEvent{" +
                "serverId=" + (server != null ? server.getId() : null) +
                ", serverName=" + (server != null ? server.getName() : null) +
                '}';
    }
}
