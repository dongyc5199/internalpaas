package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.repository.ServerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ServerService {
    
    @Autowired
    private ServerRepository serverRepository;
    
    @Autowired
    private SshConnectionService sshConnectionService;
    
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
     * 检查服务器的SSH连接状态
     */
    public String checkConnectionStatus(Long id) {
        Server server = serverRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Server not found"));
        
        if (!server.getActive()) {
            return "FAILED";
        }
        
        Server.ConnectionStatus status = sshConnectionService.checkConnection(server);
        server.setConnectionStatus(status);
        server.setLastConnectionCheck(LocalDateTime.now());
        serverRepository.save(server);
        
        return status.name();
    }
    
    public Server saveServer(Server server) {
        // 设置默认值
        if (server.getSshPort() == null) {
            server.setSshPort(22);
        }
        if (server.getConnectionStatus() == null) {
            server.setConnectionStatus(Server.ConnectionStatus.UNKNOWN);
        }
        return serverRepository.save(server);
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
     * 检查服务器的SSH连接状态
     */
    public Server checkServerConnection(Long id) {
        Server server = serverRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Server not found"));
        
        if (!server.getActive()) {
            server.setConnectionStatus(Server.ConnectionStatus.FAILED);
            server.setLastConnectionCheck(LocalDateTime.now());
            return serverRepository.save(server);
        }
        
        Server.ConnectionStatus status = sshConnectionService.checkConnection(server);
        server.setConnectionStatus(status);
        server.setLastConnectionCheck(LocalDateTime.now());
        
        return serverRepository.save(server);
    }
    
    /**
     * 检查所有活动服务器的连接状态
     */
    public void checkAllServerConnections() {
        List<Server> activeServers = getActiveServers();
        for (Server server : activeServers) {
            try {
                checkServerConnection(server.getId());
            } catch (Exception e) {
                logger.warn("检查服务器连接时出错: {}", server.getName(), e);
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
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ServerService.class);
}