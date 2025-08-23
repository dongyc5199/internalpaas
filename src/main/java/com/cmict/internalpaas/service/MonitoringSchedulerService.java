package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MonitoringSchedulerService {
    
    private static final Logger logger = LoggerFactory.getLogger(MonitoringSchedulerService.class);
    
    @Autowired
    private ServerService serverService;
    
    @Autowired
    private UserActivityService userActivityService;
    
    @Autowired
    private MonitoringService monitoringService;
    
    @Autowired
    private SSHTerminalService sshTerminalService;
    
    @Autowired
    private UserSessionService userSessionService;
    
    /**
     * 每分钟检查所有活跃服务器的连接状态和监控数据
     * 只在有活跃用户时才执行，避免无用的资源消耗
     */
    @Scheduled(fixedRate = 60000) // 每60秒执行一次
    public void checkServerConnectionsAndMetrics() {
        // 检查是否有活跃用户，只有在有用户登录时才执行监控
        if (!userSessionService.hasActiveUsers()) {
            logger.debug("当前系统没有活跃用户，跳过SSH连接检查");
            return;
        }
        
        logger.debug("开始定时检查服务器连接状态和监控数据");
        
        try {
            List<Server> activeServers = serverService.getActiveServers();
            
            for (Server server : activeServers) {
                // 只检查启用了自动监控的服务器
                if (server.getAutoMonitorEnabled() != null && server.getAutoMonitorEnabled()) {
                    
                    // 检查是否需要更新（根据监控间隔）
                    if (shouldUpdateMetrics(server)) {
                        try {
                            serverService.checkServerConnectionAndMetrics(server.getId());
                            logger.debug("更新了服务器监控数据: {}", server.getName());
                        } catch (Exception e) {
                            logger.warn("更新服务器监控数据失败: {}", server.getName(), e);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("定时检查服务器状态时发生错误", e);
        }
    }
    
    /**
     * 检查是否需要更新监控指标
     */
    private boolean shouldUpdateMetrics(Server server) {
        if (server.getLastMetricsUpdate() == null) {
            return true; // 从未更新过，需要更新
        }
        
        int intervalSeconds = server.getMonitorIntervalSeconds() != null ? server.getMonitorIntervalSeconds() : 60;
        LocalDateTime nextUpdateTime = server.getLastMetricsUpdate().plusSeconds(intervalSeconds);
        
        return LocalDateTime.now().isAfter(nextUpdateTime);
    }
    
    /**
     * 每5分钟清理超时的用户会话和SSH终端会话
     * 只在有活跃用户时才执行，避免无用的资源消耗
     */
    @Scheduled(fixedRate = 300000) // 每5分钟执行一次
    public void cleanupTimeoutSessions() {
        // 检查是否有活跃用户
        if (!userSessionService.hasActiveUsers()) {
            logger.debug("当前系统没有活跃用户，跳过清理超时会话");
            return;
        }
        
        logger.debug("开始清理超时的用户会话");
        
        try {
            // 清理30分钟无活动的会话
            userActivityService.cleanupTimeoutSessions(30);
            
            // 清理SSH终端超时会话
            sshTerminalService.cleanupTimeoutSessions(30);
        } catch (Exception e) {
            logger.error("清理超时会话时发生错误", e);
        }
    }
    
    /**
     * 每天凌晨3点清理历史数据
     */
    @Scheduled(cron = "0 0 3 * * ?") // 每天凌晨3点执行
    public void cleanupHistoricalData() {
        logger.info("开始清理历史数据");
        
        try {
            // 清理30天前的监控数据
            monitoringService.cleanupOldData(30);
            
            // 清理90天前的用户活动数据
            userActivityService.cleanupOldActivities(90);
            
            logger.info("历史数据清理完成");
        } catch (Exception e) {
            logger.error("清理历史数据时发生错误", e);
        }
    }
    
    /**
     * 每小时检查服务器健康状态
     * 只在有活跃用户时才执行
     */
    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    public void checkServerHealth() {
        // 检查是否有活跃用户
        if (!userSessionService.hasActiveUsers()) {
            logger.debug("当前系统没有活跃用户，跳过服务器健康检查");
            return;
        }
        
        logger.debug("开始检查服务器健康状态");
        
        try {
            List<Server> activeServers = serverService.getActiveServers();
            
            for (Server server : activeServers) {
                if (server.getConnectionStatus() == Server.ConnectionStatus.MONITORING) {
                    try {
                        // 获取最新的监控数据并检查阈值
                        checkServerHealthThresholds(server);
                    } catch (Exception e) {
                        logger.warn("检查服务器健康状态失败: {}", server.getName(), e);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("检查服务器健康状态时发生错误", e);
        }
    }
    
    /**
     * 检查服务器健康状态阈值
     */
    private void checkServerHealthThresholds(Server server) {
        var latestMetrics = serverService.getServerLatestMetrics(server.getId());
        
        if (latestMetrics != null) {
            // 检查CPU使用率阈值
            if (latestMetrics.getCpuUsage() != null && latestMetrics.getCpuUsage() > 90.0) {
                logger.warn("服务器 {} CPU使用率过高: {}%", server.getName(), latestMetrics.getCpuUsage());
            }
            
            // 检查内存使用率阈值
            if (latestMetrics.getMemoryUsage() != null && latestMetrics.getMemoryUsage() > 85.0) {
                logger.warn("服务器 {} 内存使用率过高: {}%", server.getName(), latestMetrics.getMemoryUsage());
            }
            
            // 检查磁盘使用率阈值
            if (latestMetrics.getDiskUsage() != null && latestMetrics.getDiskUsage() > 90.0) {
                logger.warn("服务器 {} 磁盘使用率过高: {}%", server.getName(), latestMetrics.getDiskUsage());
            }
        }
    }
    
    /**
     * 每10分钟强制刷新连接失败的服务器状态
     * 只在有活跃用户时才执行
     */
    @Scheduled(fixedRate = 600000) // 每10分钟执行一次
    public void retryFailedConnections() {
        // 检查是否有活跃用户
        if (!userSessionService.hasActiveUsers()) {
            logger.debug("当前系统没有活跃用户，跳过重试失败连接");
            return;
        }
        
        logger.debug("开始重试失败的服务器连接");
        
        try {
            List<Server> activeServers = serverService.getActiveServers();
            
            for (Server server : activeServers) {
                // 只重试失败状态的服务器
                if (server.getConnectionStatus() == Server.ConnectionStatus.FAILED ||
                    server.getConnectionStatus() == Server.ConnectionStatus.TIMEOUT ||
                    server.getConnectionStatus() == Server.ConnectionStatus.AUTH_FAILED) {
                    
                    // 检查上次尝试时间，避免频繁重试
                    if (server.getLastConnectionCheck() == null || 
                        server.getLastConnectionCheck().isBefore(LocalDateTime.now().minusMinutes(10))) {
                        
                        try {
                            serverService.checkServerConnectionAndMetrics(server.getId());
                            logger.debug("重试服务器连接: {}", server.getName());
                        } catch (Exception e) {
                            logger.debug("重试服务器连接失败: {}", server.getName(), e);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("重试失败连接时发生错误", e);
        }
    }
    
    /**
     * 手动触发所有服务器状态检查
     */
    public void triggerFullHealthCheck() {
        logger.info("手动触发全量服务器健康检查");
        
        try {
            serverService.checkAllServerConnectionsAndMetrics();
            cleanupTimeoutSessions();
            logger.info("全量健康检查完成");
        } catch (Exception e) {
            logger.error("全量健康检查失败", e);
        }
    }
}