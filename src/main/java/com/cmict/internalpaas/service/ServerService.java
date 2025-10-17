package com.cmict.internalpaas.service;

import com.cmict.internalpaas.client.MetricsHubClient;
import com.cmict.internalpaas.event.ServerCreatedEvent;
import com.cmict.internalpaas.event.ServerStatusUpdateEvent;
import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerMetrics;
import com.cmict.internalpaas.repository.ServerRepository;
// import com.cmict.internalpaas.repository.ServerMetricsRepository; // ❌ 已废弃 (Phase4-Step4)
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;

/**
 * 服务器管理服务
 * 
 * Phase4迁移: 监控数据从 Hub 获取
 */
@Service
public class ServerService {
    
    private static final Logger logger = LoggerFactory.getLogger(ServerService.class);
    
    // 专用线程池，用于异步服务器检测任务
    private final ExecutorService serverCheckExecutor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "server-check-" + System.currentTimeMillis());
        t.setDaemon(true);  // 设置为守护线程
        return t;
    });
    
    @Autowired
    private ServerRepository serverRepository;
    
    @Autowired
    private SshConnectionService sshConnectionService;
    
    /**
     * MonitoringService - 保留用于用户活动监控
     * 注意: 监控数据收集功能已废弃，仅保留用户活动相关功能
     */
    @Autowired
    private MonitoringService monitoringService;
    
    /**
     * Metrics Hub客户端 (Phase4迁移: 从Hub获取监控数据)
     */
    @Autowired(required = false)
    private MetricsHubClient metricsHubClient;
    
    // ❌ 已废弃 (Phase4-Step4: 2025-10-17) - 监控数据已迁移到Hub
    // @Autowired
    // private ServerMetricsRepository metricsRepository;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Autowired
    private UserSessionService userSessionService;
    
    @Autowired
    private com.cmict.internalpaas.repository.ServerUserGroupRepository serverUserGroupRepository;
    
    @Autowired
    private com.cmict.internalpaas.repository.SSHSessionRepository sshSessionRepository;
    
    @Autowired
    private com.cmict.internalpaas.repository.UserRepository userRepository;
    
    @Autowired
    private PasswordEncryptionService passwordEncryptionService;
    
    public List<Server> getAllServers() {
        List<Server> servers = serverRepository.findAll();
        ensurePasswordEncryptionService(servers);
        return servers;
    }
    
    public List<Server> getActiveServers() {
        List<Server> servers = serverRepository.findByActiveTrueOrderByName();
        ensurePasswordEncryptionService(servers);
        return servers;
    }
    
    public List<Server> findAllActive() {
        List<Server> servers = serverRepository.findByActiveTrueOrderByName();
        ensurePasswordEncryptionService(servers);
        return servers;
    }
    
    public List<Server> findAll() {
        List<Server> servers = serverRepository.findAll();
        ensurePasswordEncryptionService(servers);
        return servers;
    }
    
    public Optional<Server> getServerById(Long id) {
        Optional<Server> serverOpt = serverRepository.findById(id);
        serverOpt.ifPresent(this::ensurePasswordEncryptionService);
        return serverOpt;
    }
    
    public Optional<Server> findById(Long id) {
        Optional<Server> serverOpt = serverRepository.findById(id);
        serverOpt.ifPresent(this::ensurePasswordEncryptionService);
        return serverOpt;
    }

    public List<Server> findByConnectionStatusIn(List<Server.ConnectionStatus> connectionStatuses) {
        List<Server> servers = serverRepository.findByConnectionStatusIn(connectionStatuses);
        ensurePasswordEncryptionService(servers);
        return servers;
    }
    
    /**
     * 检查服务器连接并获取监控数据
     */
    @Transactional
    public Server checkServerConnectionAndMetrics(Long id) {
        Server server = findById(id).orElseThrow(() -> new RuntimeException("Server not found"));
        
        try {
            // 检查SSH连接
            Server.ConnectionStatus status = sshConnectionService.checkConnection(server);
            server.setConnectionStatus(status);
            server.setLastConnectionCheck(LocalDateTime.now());
            
            // 如果连接成功,获取监控数据
            if (status == Server.ConnectionStatus.CONNECTED) {
                // ❌ 已废弃 (Phase4-Step4) - SSH监控数据收集已停用
                // 监控数据现由 OTLP Agent 上报到 Hub,通过 MetricsHubClient 查询
                // ServerMetrics metrics = monitoringService.getServerMetrics(server);
                // metricsRepository.save(metrics);
                
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
    
    @Transactional
    public Server saveServer(Server server) {
        // 判断是新建还是更新
        boolean isNew = (server.getId() == null);

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

        // 确保设置密码加密服务
        server.setPasswordEncryptionService(passwordEncryptionService);

        // 验证必填字段完整性
        validateServerBeforeSave(server);
        Server savedServer = serverRepository.save(server);

        // 如果是新建服务器，发布ServerCreatedEvent
        if (isNew && eventPublisher != null) {
            logger.info("发布ServerCreatedEvent - serverId: {}, serverName: {}",
                    savedServer.getId(), savedServer.getName());
            eventPublisher.publishEvent(new ServerCreatedEvent(this, savedServer));
        }

        return savedServer;
    }
    
    /**
     * 创建服务器并设置密码（确保密码正确加密）
     */
    @Transactional
    public Server createServerWithPassword(Server server, String plainPassword) {
        logger.info("创建服务器: {}, 明文密码长度: {}", server.getName(),
            plainPassword != null ? plainPassword.length() : 0);

        // 设置默认值（在加密密码之前）
        if (server.getSshPort() == null) {
            server.setSshPort(22);
        }
        if (server.getConnectionStatus() == null) {
            server.setConnectionStatus(Server.ConnectionStatus.UNKNOWN);
        }

        // 注入密码加密服务
        server.setPasswordEncryptionService(passwordEncryptionService);
        logger.debug("密码加密服务已注入");

        // 设置密码（会自动加密）
        if (plainPassword != null && !plainPassword.isEmpty()) {
            try {
                server.setSshPassword(plainPassword);
                String encryptedPwd = server.getSshPasswordEncrypted();
                logger.info("密码加密成功 - 加密后长度: {}, 格式检查: {}",
                    encryptedPwd != null ? encryptedPwd.length() : 0,
                    encryptedPwd != null && encryptedPwd.contains(":") ? "VALID(包含:)" : "INVALID(无:)");

                // 验证加密格式
                if (encryptedPwd != null && !encryptedPwd.contains(":")) {
                    logger.error("密码加密格式异常！加密后的密码不包含分隔符 ':' - 内容: {}",
                        encryptedPwd.substring(0, Math.min(50, encryptedPwd.length())));
                    throw new RuntimeException("密码加密格式异常");
                }
            } catch (Exception e) {
                logger.error("密码加密失败", e);
                throw new RuntimeException("密码加密失败: " + e.getMessage(), e);
            }
        } else {
            logger.warn("未提供密码");
        }

        // 调用标准保存方法（不会再次加密密码）
        Server savedServer = saveServer(server);
        logger.info("服务器已保存到数据库，ID: {}, 密码字段长度: {}",
            savedServer.getId(),
            savedServer.getSshPasswordEncrypted() != null ? savedServer.getSshPasswordEncrypted().length() : 0);

        return savedServer;
    }
    
    /**
     * 保存服务器并自动检测连接
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Server saveServerWithAutoCheck(Server server) {
        Server savedServer = saveServer(server);
        
        // 异步执行连接检测和监控，使用专用线程池
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
                
                // 如果连接成功,立即获取系统状态
                if (status == Server.ConnectionStatus.CONNECTED) {
                    // ❌ 已废弃 (Phase4-Step4) - SSH监控数据收集已停用
                    // ServerMetrics metrics = monitoringService.getServerMetrics(savedServer);
                    // metricsRepository.save(metrics);
                    
                    savedServer.setLastMetricsUpdate(LocalDateTime.now());
                    savedServer.setConnectionStatus(Server.ConnectionStatus.MONITORING);
                    
                    // 保存用户活跃信息
                    monitoringService.saveUserActivities(savedServer);
                }
                
                // 验证必填字段完整性
                validateServerBeforeSave(savedServer);
                serverRepository.save(savedServer);
                
            } catch (Exception e) {
                logger.error("服务器自动检测失败: {}", savedServer.getName(), e);
                savedServer.setConnectionStatus(Server.ConnectionStatus.FAILED);
                savedServer.setLastConnectionCheck(LocalDateTime.now());
                // 验证必填字段完整性
                validateServerBeforeSave(savedServer);
                serverRepository.save(savedServer);
            }
        }, serverCheckExecutor);
        
        return savedServer;
    }
    
    /**
     * 检查服务器的SSH连接状态
     */
    public String checkConnectionStatus(Long id) {
        Server server = checkServerConnectionAndMetrics(id);
        return server.getConnectionStatus().name();
    }
    
    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteServer(Long id) {
        logger.info("开始删除服务器，ID: {}", id);
        
        try {
            // 首先检查服务器是否存在
            Server server = serverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("服务器不存在，ID: " + id));
            
            logger.info("准备删除服务器: {} ({})", server.getName(), server.getHostname());
            
            // 删除关联的用户组
            deleteServerUserGroups(id);
            
            // 删除关联的监控指标
            deleteServerMetrics(id);
            
            // 删除关联的SSH会话
            deleteServerSshSessions(id);
            
            // 清理用户的服务器关联关系
            clearUserServerAssociations(id);
            
            // 最后删除服务器本身
            serverRepository.deleteById(id);
            
            logger.info("成功删除服务器: {} (ID: {})", server.getName(), id);
            
        } catch (Exception e) {
            logger.error("删除服务器失败，ID: {} - {}", id, e.getMessage(), e);
            throw new RuntimeException("删除服务器失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 删除服务器关联的用户组
     */
    @Transactional
    private void deleteServerUserGroups(Long serverId) {
        try {
            // 注入ServerUserGroupRepository来删除用户组
            if (serverUserGroupRepository != null) {
                List<com.cmict.internalpaas.model.ServerUserGroup> userGroups = 
                    serverUserGroupRepository.findByServerId(serverId);
                
                if (!userGroups.isEmpty()) {
                    logger.info("删除服务器 {} 关联的 {} 个用户组", serverId, userGroups.size());
                    serverUserGroupRepository.deleteAll(userGroups);
                }
            }
        } catch (Exception e) {
            logger.error("删除服务器用户组失败: {}", e.getMessage(), e);
            // 继续执行，不中断删除流程
        }
    }
    
    /**
     * 删除服务器关联的监控指标
     * ❌ 已废弃 (Phase4-Step4) - H2监控数据已移除
     */
    @Transactional
    private void deleteServerMetrics(Long serverId) {
        try {
            // 监控数据已迁移到 Hub,无需在主应用删除
            // if (metricsRepository != null) {
            //     List<ServerMetrics> metrics = metricsRepository.findByServerIdOrderByTimestampDesc(serverId);
            //     if (!metrics.isEmpty()) {
            //         logger.info("删除服务器 {} 关联的 {} 个监控指标记录", serverId, metrics.size());
            //         metricsRepository.deleteAll(metrics);
            //     }
            // }
            logger.debug("跳过H2监控数据删除(已迁移到Hub),服务器ID: {}", serverId);
        } catch (Exception e) {
            logger.error("删除服务器监控指标失败: {}", e.getMessage(), e);
            // 继续执行,不中断删除流程
        }
    }
    
    /**
     * 删除服务器关联的SSH会话
     */
    @Transactional
    private void deleteServerSshSessions(Long serverId) {
        try {
            if (sshSessionRepository != null) {
                List<com.cmict.internalpaas.model.SSHSession> sessions = 
                    sshSessionRepository.findByServerId(serverId);
                
                if (!sessions.isEmpty()) {
                    logger.info("删除服务器 {} 关联的 {} 个SSH会话", serverId, sessions.size());
                    sshSessionRepository.deleteAll(sessions);
                }
            }
        } catch (Exception e) {
            logger.error("删除SSH会话失败: {}", e.getMessage(), e);
            // 继续执行，不中断删除流程
        }
    }
    
    /**
     * 清理用户的服务器关联关系
     */
    @Transactional
    private void clearUserServerAssociations(Long serverId) {
        try {
            if (userRepository != null) {
                // 查找将此服务器设为默认服务器的用户
                List<com.cmict.internalpaas.model.User> users = userRepository.findByDefaultServerId(serverId);
                for (com.cmict.internalpaas.model.User user : users) {
                    user.setDefaultServer(null);
                    logger.info("清除用户 {} 的默认服务器设置", user.getUsername());
                }
                
                if (!users.isEmpty()) {
                    userRepository.saveAll(users);
                }
                
                // 使用JPQL查询直接处理多对多关系，避免懒加载问题
                logger.info("清理用户与服务器 {} 的关联关系", serverId);
                userRepository.removeServerFromUsers(serverId);
            }
        } catch (Exception e) {
            logger.error("清理用户服务器关联失败: {}", e.getMessage(), e);
            // 继续执行，不中断删除流程
        }
    }
    
    @Transactional
    public Server updateServer(Long id, Server serverDetails) {
        Server server = serverRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Server not found"));
        
        // 确保设置密码加密服务
        server.setPasswordEncryptionService(passwordEncryptionService);
        serverDetails.setPasswordEncryptionService(passwordEncryptionService);
        
        server.setName(serverDetails.getName());
        server.setHostname(serverDetails.getHostname());
        server.setPort(serverDetails.getPort());
        server.setDescription(serverDetails.getDescription());
        server.setBaseWorkDirectory(serverDetails.getBaseWorkDirectory());
        server.setActive(serverDetails.getActive());
        
        // 更新SSH相关字段
        server.setSshPort(serverDetails.getSshPort());
        server.setSshUsername(serverDetails.getSshUsername());
        
        // 处理密码更新 - 只有在提供了新密码时才更新
        if (serverDetails.getSshPasswordEncrypted() != null && !serverDetails.getSshPasswordEncrypted().isEmpty()) {
            // 如果传入的是明文密码，自动加密
            if (!passwordEncryptionService.isPasswordEncrypted(serverDetails.getSshPasswordEncrypted())) {
                server.setSshPassword(serverDetails.getSshPasswordEncrypted()); // 会自动加密
                logger.info("更新服务器 {} 的SSH密码（已加密）", server.getName());
            } else {
                server.setSshPasswordEncrypted(serverDetails.getSshPasswordEncrypted()); // 直接设置已加密的密码
            }
        }
        
        server.setSshKeyPath(serverDetails.getSshKeyPath());
        
        // 处理密钥密码更新
        if (serverDetails.getSshKeyPassphraseEncrypted() != null && !serverDetails.getSshKeyPassphraseEncrypted().isEmpty()) {
            if (!passwordEncryptionService.isPasswordEncrypted(serverDetails.getSshKeyPassphraseEncrypted())) {
                server.setSshKeyPassphrase(serverDetails.getSshKeyPassphraseEncrypted()); // 会自动加密
            } else {
                server.setSshKeyPassphraseEncrypted(serverDetails.getSshKeyPassphraseEncrypted()); // 直接设置已加密的密码
            }
        }
        
        // 更新监控相关字段
        if (serverDetails.getAutoMonitorEnabled() != null) {
            server.setAutoMonitorEnabled(serverDetails.getAutoMonitorEnabled());
        }
        if (serverDetails.getMonitorIntervalSeconds() != null) {
            server.setMonitorIntervalSeconds(serverDetails.getMonitorIntervalSeconds());
        }
        
        // 验证必填字段完整性
        validateServerBeforeSave(server);
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
     * Phase4迁移: 从Hub获取监控数据
     */
    public ServerMetrics getServerLatestMetrics(Long serverId) {
        // Phase4: 优先从Hub获取
        if (metricsHubClient != null && metricsHubClient.isAvailable()) {
            try {
                return metricsHubClient.getLatestMetrics(serverId);
            } catch (Exception e) {
                logger.warn("从Hub获取服务器{}监控数据失败: {}", serverId, e.getMessage());
            }
        }
        
        // 降级: Hub不可用时返回空
        logger.debug("Hub不可用,服务器{}暂无监控数据", serverId);
        return null;
    }
    
    /**
     * 强制刷新服务器监控数据
     * Phase4迁移: Hub自动收集监控数据,此方法仅更新用户活动
     */
    @Transactional
    public ServerMetrics refreshServerMetrics(Long id) {
        Server server = findById(id).orElseThrow(() -> new RuntimeException("Server not found"));
        
        if (server.getConnectionStatus() == Server.ConnectionStatus.CONNECTED || 
            server.getConnectionStatus() == Server.ConnectionStatus.MONITORING) {
            try {
                // Phase4: Hub自动收集监控数据,无需手动保存
                // 仅更新用户活跃信息(保留非监控功能)
                monitoringService.saveUserActivities(server);
                
                server.setLastMetricsUpdate(LocalDateTime.now());
                validateServerBeforeSave(server);
                serverRepository.save(server);
                
                // 从Hub获取最新监控数据并返回
                return getServerLatestMetrics(id);
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
    
    /**
     * 验证服务器对象的必填字段，防止约束违反错误
     */
    private void validateServerBeforeSave(Server server) {
        if (server == null) {
            throw new IllegalArgumentException("服务器对象不能为null");
        }
        
        // 检查必填字段
        if (server.getName() == null || server.getName().trim().isEmpty()) {
            logger.warn("服务器名称为空，使用默认值");
            server.setName("Unknown Server");
        }
        
        if (server.getHostname() == null || server.getHostname().trim().isEmpty()) {
            logger.warn("服务器主机名为空，使用localhost");
            server.setHostname("localhost");
        }
        
        if (server.getPort() == null) {
            logger.warn("服务器端口为空，使用默认值8080");
            server.setPort(8080);
        }
        
        if (server.getBaseWorkDirectory() == null || server.getBaseWorkDirectory().trim().isEmpty()) {
            logger.warn("服务器工作目录为空，使用默认值");
            server.setBaseWorkDirectory("/tmp");
        }
        
        if (server.getActive() == null) {
            server.setActive(true);
        }
        
        if (server.getCreatedAt() == null) {
            server.setCreatedAt(LocalDateTime.now());
        }
        
        if (server.getUpdatedAt() == null) {
            server.setUpdatedAt(LocalDateTime.now());
        }
        
        // 验证字符串长度限制
        if (server.getName().length() > 100) {
            server.setName(server.getName().substring(0, 100));
        }
        
        if (server.getHostname().length() > 100) {
            server.setHostname(server.getHostname().substring(0, 100));
        }
        
        if (server.getDescription() != null && server.getDescription().length() > 500) {
            server.setDescription(server.getDescription().substring(0, 500));
        }
    }
    
    /**
     * 确保服务器实体具有密码加密服务
     */
    private void ensurePasswordEncryptionService(Server server) {
        if (server != null) {
            server.setPasswordEncryptionService(passwordEncryptionService);
        }
    }
    
    /**
     * 为服务器列表设置密码加密服务
     */
    private void ensurePasswordEncryptionService(List<Server> servers) {
        if (servers != null) {
            servers.forEach(server -> server.setPasswordEncryptionService(passwordEncryptionService));
        }
    }
    
    /**
     * 验证服务器密码设置状态
     */
    public boolean validateServerPasswordSetup(Long serverId) {
        Optional<Server> serverOpt = getServerById(serverId);
        if (serverOpt.isPresent()) {
            Server server = serverOpt.get();
            return sshConnectionService.validateServerPassword(server);
        }
        return false;
    }
    
    @PreDestroy
    public void cleanup() {
        logger.info("ServerService正在清理资源...");
        
        try {
            serverCheckExecutor.shutdown();
            
            if (!serverCheckExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warn("ServerCheckExecutor在10秒内未能正常关闭，尝试强制关闭");
                serverCheckExecutor.shutdownNow();
                
                if (!serverCheckExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.error("ServerCheckExecutor强制关闭失败");
                } else {
                    logger.info("ServerCheckExecutor已强制关闭");
                }
            } else {
                logger.info("ServerCheckExecutor已正常关闭");
            }
            
        } catch (InterruptedException e) {
            logger.warn("等待ServerCheckExecutor关闭时被中断", e);
            serverCheckExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 测试服务器连接 (不保存到数据库)
     */
    public boolean testServerConnection(Server server) {
        try {
            Server.ConnectionStatus status = sshConnectionService.checkConnection(server);
            return status == Server.ConnectionStatus.CONNECTED || status == Server.ConnectionStatus.MONITORING;
        } catch (Exception e) {
            logger.error("测试服务器连接失败: {}", server.getHostname(), e);
            return false;
        }
    }
}