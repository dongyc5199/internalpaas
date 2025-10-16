package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ⚠️ 监控调度服务 (部分功能已废弃)
 * 
 * 迁移说明 (2025-10-17):
 * - ❌ SSH监控定时任务已停用 → 使用 Metrics Hub + OTLP Agent 替代
 * - ✅ 会话清理功能保留
 * - ✅ 失败连接重试保留
 * 
 * 架构演进:
 * - 旧: SSH轮询(60秒) + H2存储
 * - 新: OTLP Agent(10秒) + Hub(Redis + TSDB)
 * 
 * 优势:
 * - 采样频率提升 6倍 (60秒 → 10秒)
 * - 延迟降低 10倍 (0-60秒 → 0-30秒)
 * - 稳定性提升 (gRPC vs SSH)
 * - 扩展性更好 (支持1000+ 服务器)
 */
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
     * ⚠️ 已废弃: SSH监控定时任务
     * 
     * 原因: 已迁移到 Metrics Hub + OTLP Agent 架构
     * 迁移日期: 2025-10-17
     * 
     * 说明:
     * 1. SSH监控已被 OTLP Agent 替代,采样间隔从60秒提升到10秒
     * 2. 数据存储已迁移到 Hub (Redis + PostgreSQL/TimescaleDB)
     * 3. SSH连接能力保留,用于Agent部署和服务器管理
     * 
     * 替代方案:
     * - 实时监控: OTLP Agent自动上报到Hub (10秒间隔)
     * - 数据查询: 使用 MetricsHubClient API
     * - 健康检查: Hub提供统一的健康检查API
     * 
     * 如需临时恢复SSH监控,请:
     * 1. 取消下方 @Scheduled 注释
     * 2. 设置 monitoring.ssh.scheduler.enabled=true
     * 3. 重启应用
     * 
     * @deprecated 使用 Metrics Hub + OTLP Agent 替代
     */
    // @Scheduled(fixedRate = 60000) // ❌ 已停用 (2025-10-17)
    @Deprecated(since = "2025-10-17", forRemoval = true)
    public void checkServerConnectionsAndMetrics() {
        logger.warn("⚠️ SSH监控定时任务已停用,请使用 Metrics Hub + OTLP Agent");
        logger.warn("提示: 监控数据现在通过 OTLP Agent 自动上报到 Hub (10秒间隔)");
        logger.warn("文档: doc/阶段4-数据存储迁移实施方案.md");
        
        /* 原SSH监控逻辑已注释,保留以便回滚
        
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
        */
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
     * ⚠️ 已废弃: 清理H2历史监控数据
     * 
     * 原因: H2数据库已移除,数据存储迁移到Hub
     * 迁移日期: 2025-10-17
     * 
     * 替代方案:
     * - Hub自动执行数据保留策略:
     *   - Redis热数据: 5分钟
     *   - TSDB原始数据: 30天
     *   - TSDB 5分钟聚合: 90天
     *   - TSDB 1小时聚合: 365天
     * 
     * @deprecated Hub自动管理数据生命周期
     */
    // @Scheduled(cron = "0 0 3 * * ?") // ❌ 已停用 (2025-10-17)
    @Deprecated(since = "2025-10-17", forRemoval = true)
    public void cleanupHistoricalData() {
        logger.warn("⚠️ H2历史数据清理任务已停用");
        logger.warn("提示: Hub会自动执行数据保留策略 (30/90/365天)");
        logger.warn("配置: hub/src/main/resources/application.yaml -> metrics-hub.retention");
        
        /* 原清理逻辑已注释,保留以便回滚
        
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
        */
    }
    
    /**
     * ⚠️ 已废弃: 基于SSH数据检查服务器健康状态
     * 
     * 原因: 健康检查已迁移到Hub的智能路由和告警系统
     * 迁移日期: 2025-10-17
     * 
     * 替代方案:
     * - Hub实时健康检查: 基于最新OTLP数据(10秒间隔)
     * - Hub告警规则: 支持CPU/内存/磁盘阈值配置
     * - Prometheus告警: 更专业的告警管理
     * 
     * @deprecated 使用Hub的告警系统替代
     */
    // @Scheduled(fixedRate = 3600000) // ❌ 已停用 (2025-10-17)
    @Deprecated(since = "2025-10-17", forRemoval = true)
    public void checkServerHealth() {
        logger.warn("⚠️ SSH健康检查任务已停用");
        logger.warn("提示: Hub提供实时健康检查和告警功能");
        logger.warn("API: GET /api/v1/health/server/{id}");
        
        /* 原健康检查逻辑已注释,保留以便回滚
        
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
        */
    }
    
    /**
     * ⚠️ 已废弃: 检查服务器健康阈值 (基于H2数据)
     * 
     * @deprecated 使用Hub的告警规则替代
     */
    @Deprecated(since = "2025-10-17", forRemoval = true)
    private void checkServerHealthThresholds(Server server) {
        logger.warn("⚠️ checkServerHealthThresholds 已废弃,使用Hub告警系统");
        
        /* 原阈值检查逻辑已注释,保留以便回滚
        
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
        */
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