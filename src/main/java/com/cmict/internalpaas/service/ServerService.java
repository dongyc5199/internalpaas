package com.cmict.internalpaas.service;

import com.cmict.internalpaas.event.ServerStatusUpdateEvent;
import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerMetrics;
import com.cmict.internalpaas.repository.ServerRepository;
import com.cmict.internalpaas.repository.ServerMetricsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class ServerService {
    
    private static final Logger logger = LoggerFactory.getLogger(ServerService.class);
    
    @Autowired
    private ServerRepository serverRepository;
    
    @Autowired
    private SshConnectionService sshConnectionService;
    
    @Autowired
    private MonitoringService monitoringService;
    
    @Autowired
    private ServerMetricsRepository metricsRepository;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Autowired
    private UserSessionService userSessionService;
    
    public List<Server> getAllServers() {
        return serverRepository.findAll();
    }
    
    public List<Server> getActiveServers() {
        return serverRepository.findByActiveTrueOrderByName();
    }
    
    public Optional<Server> getServerById(Long id) {
        return serverRepository.findById(id);
    }
    
    public Optional<Server> findById(Long id) {
        return serverRepository.findById(id);
    }
    
    /**
     * 检查服务器连接并获取监控数据
     */
    public Server checkServerConnectionAndMetrics(Long id) {
        Server server = findById(id).orElseThrow(() -> new RuntimeException("Server not found"));
        
        try {
            // 检查SSH连接
            Server.ConnectionStatus status = sshConnectionService.checkConnection(server);
            server.setConnectionStatus(status);
            server.setLastConnectionCheck(LocalDateTime.now());
            
            // 如果连接成功，获取监控数据
            if (status == Server.ConnectionStatus.CONNECTED) {
                ServerMetrics metrics = monitoringService.getServerMetrics(server);
                metricsRepository.save(metrics);
                server.setLastMetricsUpdate(LocalDateTime.now());
                server.setConnectionStatus(Server.ConnectionStatus.MONITORING);
                
                // 保存用户活跃信息
                monitoringService.saveUserActivities(server);
            }
            
            Server savedServer = serverRepository.save(server);
            
            // 发布服务器状态更新事件
            eventPublisher.publishEvent(new ServerStatusUpdateEvent(this, savedServer));
            
            return savedServer;
            
        } catch (Exception e) {
            logger.error("检查服务器连接失败: {}", server.getName(), e);
            server.setConnectionStatus(Server.ConnectionStatus.FAILED);
            server.setLastConnectionCheck(LocalDateTime.now());
            Server savedServer = serverRepository.save(server);
            
            // 即使失败也要发布状态更新事件
            eventPublisher.publishEvent(new ServerStatusUpdateEvent(this, savedServer));
            
            return savedServer;
        }
    }
    
    public Server saveServer(Server server) {
        // 设置默认值
        if (server.getSshPort() == null) {
            server.setSshPort(22);
        }
        if (server.getConnectionStatus() == null) {
            server.setConnectionStatus(Server.ConnectionStatus.UNKNOWN);
        }
        if (server.getAutoMonitorEnabled() == null) {
            server.setAutoMonitorEnabled(true);
        }
        if (server.getMonitorIntervalSeconds() == null) {
            server.setMonitorIntervalSeconds(60);
        }
        return serverRepository.save(server);
    }
    
    /**
     * 保存服务器并自动检测连接
     */
    @Transactional
    public Server saveServerWithAutoCheck(Server server) {
        Server savedServer = saveServer(server);
        
        // 异步执行连接检测和监控
        CompletableFuture.runAsync(() -> {
            try {
                // 检查是否有活跃用户，只有在有用户登录时才执行自动检测
                if (!userSessionService.hasActiveUsers()) {
                    logger.debug("当前系统没有活跃用户，跳过服务器 {} 的自动连接检测", savedServer.getName());
                    return;
                }
                
                logger.debug("开始自动检测服务器 {} 的连接状态", savedServer.getName());
                
                // 检测SSH连接
                Server.ConnectionStatus status = sshConnectionService.checkConnection(savedServer);
                savedServer.setConnectionStatus(status);
                savedServer.setLastConnectionCheck(LocalDateTime.now());
                
                // 如果连接成功，立即获取系统状态
                if (status == Server.ConnectionStatus.CONNECTED) {
                    ServerMetrics metrics = monitoringService.getServerMetrics(savedServer);
                    metricsRepository.save(metrics);
                    savedServer.setLastMetricsUpdate(LocalDateTime.now());
                    savedServer.setConnectionStatus(Server.ConnectionStatus.MONITORING);
                    
                    // 保存用户活跃信息
                    monitoringService.saveUserActivities(savedServer);
                }
                
                serverRepository.save(savedServer);
                
            } catch (Exception e) {
                logger.error("服务器自动检测失败: {}", savedServer.getName(), e);
                savedServer.setConnectionStatus(Server.ConnectionStatus.FAILED);
                savedServer.setLastConnectionCheck(LocalDateTime.now());
                serverRepository.save(savedServer);
            }
        });
        
        return savedServer;
    }
    
    /**
     * 检查服务器的SSH连接状态
     */
    public String checkConnectionStatus(Long id) {
        Server server = checkServerConnectionAndMetrics(id);
        return server.getConnectionStatus().name();
    }
    
    public void deleteServer(Long id) {
        serverRepository.deleteById(id);
    }
    
    public Server updateServer(Long id, Server serverDetails) {
        Server server = serverRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Server not found"));
        
        server.setName(serverDetails.getName());
        server.setHostname(serverDetails.getHostname());
        server.setPort(serverDetails.getPort());
        server.setDescription(serverDetails.getDescription());
        server.setBaseWorkDirectory(serverDetails.getBaseWorkDirectory());
        server.setActive(serverDetails.getActive());
        
        // 更新SSH相关字段
        server.setSshPort(serverDetails.getSshPort());
        server.setSshUsername(serverDetails.getSshUsername());
        server.setSshPassword(serverDetails.getSshPassword());
        server.setSshKeyPath(serverDetails.getSshKeyPath());
        server.setSshKeyPassphrase(serverDetails.getSshKeyPassphrase());
        
        // 更新监控相关字段
        if (serverDetails.getAutoMonitorEnabled() != null) {
            server.setAutoMonitorEnabled(serverDetails.getAutoMonitorEnabled());
        }
        if (serverDetails.getMonitorIntervalSeconds() != null) {
            server.setMonitorIntervalSeconds(serverDetails.getMonitorIntervalSeconds());
        }
        
        return serverRepository.save(server);
    }
    
    public String getDefaultBaseWorkDirectory() {
        List<Server> activeServers = getActiveServers();
        if (activeServers.isEmpty()) {
            return "./workspaces";
        }
        return activeServers.get(0).getBaseWorkDirectory();
    }
    
    /**
     * 获取服务器最新的监控数据
     */
    public ServerMetrics getServerLatestMetrics(Long serverId) {
        return monitoringService.getLatestMetrics(serverId);
    }
    
    /**
     * 强制刷新服务器监控数据
     */
    public ServerMetrics refreshServerMetrics(Long id) {
        Server server = findById(id).orElseThrow(() -> new RuntimeException("Server not found"));
        
        if (server.getConnectionStatus() == Server.ConnectionStatus.CONNECTED || 
            server.getConnectionStatus() == Server.ConnectionStatus.MONITORING) {
            try {
                ServerMetrics metrics = monitoringService.saveServerMetrics(server);
                server.setLastMetricsUpdate(LocalDateTime.now());
                serverRepository.save(server);
                
                // 更新用户活跃信息
                monitoringService.saveUserActivities(server);
                
                return metrics;
            } catch (Exception e) {
                logger.error("刷新服务器监控数据失败: {}", server.getName(), e);
            }
        }
        
        return null;
    }
    
    /**
     * 检查所有活动服务器的连接状态和监控数据
     */
    public void checkAllServerConnectionsAndMetrics() {
        // 检查是否有活跃用户，只有在有用户登录时才执行批量检查
        if (!userSessionService.hasActiveUsers()) {
            logger.debug("当前系统没有活跃用户，跳过批量服务器检查");
            return;
        }
        
        List<Server> activeServers = getActiveServers();
        for (Server server : activeServers) {
            if (server.getAutoMonitorEnabled() != null && server.getAutoMonitorEnabled()) {
                try {
                    checkServerConnectionAndMetrics(server.getId());
                } catch (Exception e) {
                    logger.warn("检查服务器连接和监控时出错: {}", server.getName(), e);
                }
            }
        }
    }
    
    /**
     * 获取连接状态的描述信息
     */
    public String getConnectionStatusDescription(Server.ConnectionStatus status) {
        return sshConnectionService.getConnectionStatusDescription(status);
    }
    
    /**
     * 获取连接状态的CSS类名
     */
    public String getConnectionStatusClass(Server.ConnectionStatus status) {
        if (status == null) return "text-secondary";
        
        switch (status) {
            case CONNECTED:
                return "text-success";
            case FAILED:
                return "text-danger";
            case TIMEOUT:
                return "text-danger";  // 超时也显示红色，表示失败
            case AUTH_FAILED:
                return "text-danger";  // 认证失败也显示红色
            case UNKNOWN:
            default:
                return "text-secondary";
        }
    }
}