package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerStatusTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class ServerStatusScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(ServerStatusScheduler.class);
    
    @Autowired
    private ServerService serverService;
    
    @Autowired
    private ServerStatusTagService statusTagService;
    
    @Autowired
    private ServerResourceMonitoringService resourceMonitoringService;
    
    @Autowired
    private ResourceAlertService resourceAlertService;

    /**
     * 高频检查关键状态 - 每30秒执行一次
     * 重点关注连接状态和内存使用率（80%阈值告警）
     */
    @Scheduled(fixedDelay = 30000) // 30秒
    public void refreshCriticalStatuses() {
        try {
            List<Server> activeServers = serverService.findAllActive();
            
            logger.debug("⚡ 开始高频检查 {} 个服务器的关键状态", activeServers.size());
            
            // 并行检查所有活跃服务器
            List<CompletableFuture<Void>> futures = activeServers.stream()
                .map(server -> refreshCriticalServerStatusAsync(server.getId()))
                .collect(java.util.stream.Collectors.toList());
            
            // 等待所有检查完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> logger.debug("✅ 高频状态检查完成"));
                
        } catch (Exception e) {
            logger.error("高频状态检查失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 全面状态检查 - 每5分钟执行一次
     * 包括工作目录、用户活动、监控状态等
     */
    @Scheduled(cron = "0 */5 * * * *") // 每5分钟
    public void refreshAllStatuses() {
        try {
            List<Server> activeServers = serverService.findAllActive();
            
            logger.info("🔄 开始全面检查 {} 个服务器的状态", activeServers.size());
            
            // 并行刷新所有服务器状态
            List<CompletableFuture<Void>> futures = activeServers.stream()
                .map(server -> refreshAllServerStatusAsync(server.getId()))
                .collect(Collectors.toList());
            
            // 等待所有检查完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    logger.info("✅ 全面状态检查完成 - {}", 
                               LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                });
                
        } catch (Exception e) {
            logger.error("全面状态检查失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 内存告警专项检查 - 每1分钟执行一次
     * 专门检查80%内存阈值告警
     */
    @Scheduled(fixedDelay = 60000) // 1分钟
    public void checkMemoryAlerts() {
        try {
            logger.debug("🧠 开始内存告警专项检查");
            
            // 批量检查所有服务器内存使用率
            resourceMonitoringService.checkAllMemoryUsage();
            
            // 检查并发送资源告警
            resourceAlertService.checkAllServerResourceAlerts();
            
            logger.debug("✅ 内存告警检查完成");
            
        } catch (Exception e) {
            logger.error("内存告警检查失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 清理过期数据 - 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * *") // 每天凌晨2点
    public void cleanupExpiredData() {
        try {
            logger.info("🧹 开始清理过期数据");
            
            List<Server> allServers = serverService.findAll();
            
            for (Server server : allServers) {
                try {
                    // 清理过期的状态标签
                    statusTagService.deleteObsoleteTags(server.getId());
                    
                } catch (Exception e) {
                    logger.error("清理服务器 {} 过期数据失败: {}", server.getId(), e.getMessage());
                }
            }
            
            // 清理告警抑制记录
            resourceAlertService.cleanupExpiredAlertSuppressions();
            
            logger.info("✅ 过期数据清理完成");
            
        } catch (Exception e) {
            logger.error("清理过期数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 状态标签统计报告 - 每小时执行一次
     */
    @Scheduled(cron = "0 0 * * * *") // 每小时
    public void generateStatusReport() {
        try {
            List<Server> activeServers = serverService.findAllActive();
            
            int totalServers = activeServers.size();
            int alertServers = 0;
            int memoryAlertServers = 0;
            int offlineServers = 0;
            
            for (Server server : activeServers) {
                try {
                    List<ServerStatusTag> alertTags = statusTagService.getServerAlertTags(server.getId());
                    
                    if (!alertTags.isEmpty()) {
                        alertServers++;
                    }
                    
                    // 统计内存告警
                    boolean hasMemoryAlert = alertTags.stream()
                        .anyMatch(tag -> tag.getTagType() == ServerStatusTag.TagType.MEMORY_USAGE &&
                                        (tag.isCritical() || tag.isDanger()));
                    if (hasMemoryAlert) {
                        memoryAlertServers++;
                    }
                    
                    // 统计离线服务器
                    boolean isOffline = alertTags.stream()
                        .anyMatch(tag -> tag.getTagType() == ServerStatusTag.TagType.CONNECTION &&
                                        tag.isError());
                    if (isOffline) {
                        offlineServers++;
                    }
                    
                } catch (Exception e) {
                    logger.error("统计服务器 {} 状态失败: {}", server.getId(), e.getMessage());
                }
            }
            
            // 输出统计报告
            logger.info("📊 服务器状态统计报告:");
            logger.info("   📈 总服务器数: {}", totalServers);
            logger.info("   ⚠️  告警服务器: {} ({:.1f}%)", alertServers, totalServers > 0 ? (alertServers * 100.0 / totalServers) : 0);
            logger.info("   🧠 内存告警: {} ({:.1f}%)", memoryAlertServers, totalServers > 0 ? (memoryAlertServers * 100.0 / totalServers) : 0);
            logger.info("   💔 离线服务器: {} ({:.1f}%)", offlineServers, totalServers > 0 ? (offlineServers * 100.0 / totalServers) : 0);
            
            // 如果内存告警服务器过多，发出警告
            if (totalServers > 0 && memoryAlertServers * 100.0 / totalServers > 30) {
                logger.warn("🚨 内存告警服务器比例过高！当前 {}/{} ({:.1f}%) 服务器存在内存告警", 
                           memoryAlertServers, totalServers, memoryAlertServers * 100.0 / totalServers);
            }
            
        } catch (Exception e) {
            logger.error("生成状态报告失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 系统健康检查 - 每10分钟执行一次
     */
    @Scheduled(cron = "0 */10 * * * *") // 每10分钟
    public void systemHealthCheck() {
        try {
            logger.debug("🩺 开始系统健康检查");
            
            // 检查各个服务组件是否正常
            boolean monitoringServiceHealthy = checkMonitoringServiceHealth();
            boolean alertServiceHealthy = checkAlertServiceHealth();
            boolean tagServiceHealthy = checkTagServiceHealth();
            
            if (!monitoringServiceHealthy || !alertServiceHealthy || !tagServiceHealthy) {
                logger.warn("⚠️ 系统组件健康检查发现异常");
                logger.warn("   监控服务: {}", monitoringServiceHealthy ? "正常" : "异常");
                logger.warn("   告警服务: {}", alertServiceHealthy ? "正常" : "异常");
                logger.warn("   标签服务: {}", tagServiceHealthy ? "正常" : "异常");
            } else {
                logger.debug("✅ 系统健康检查正常");
            }
            
        } catch (Exception e) {
            logger.error("系统健康检查失败: {}", e.getMessage(), e);
        }
    }

    // 异步方法

    /**
     * 异步刷新服务器关键状态（连接状态 + 内存监控）
     */
    @Async("serverStatusExecutor")
    public CompletableFuture<Void> refreshCriticalServerStatusAsync(Long serverId) {
        try {
            // 检查连接状态
            statusTagService.checkConnectionStatus(serverId);
            
            // 检查内存使用率（关键的80%阈值告警）
            ServerStatusTag memoryTag = resourceMonitoringService.checkMemoryUsage(serverId);
            
            // 如果内存使用率超过80%，立即处理
            if (memoryTag != null && (memoryTag.isCritical() || memoryTag.isDanger())) {
                logger.warn("⚠️ 服务器 {} 内存告警: {}", serverId, memoryTag.getDisplayText());
            }
            
        } catch (Exception e) {
            logger.error("异步刷新服务器 {} 关键状态失败: {}", serverId, e.getMessage());
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 异步刷新服务器所有状态
     */
    @Async("serverStatusExecutor")
    public CompletableFuture<Void> refreshAllServerStatusAsync(Long serverId) {
        try {
            // 刷新所有状态标签
            statusTagService.refreshAllServerTags(serverId);
            
            // 检查所有资源状态
            List<ServerStatusTag> resourceTags = resourceMonitoringService.checkResourceStatuses(serverId);
            
            // 记录资源告警
            long alertCount = resourceTags.stream()
                .mapToLong(tag -> (tag.isCritical() || tag.isDanger() || tag.isWarning()) ? 1 : 0)
                .sum();
                
            if (alertCount > 0) {
                logger.debug("服务器 {} 检测到 {} 个资源告警", serverId, alertCount);
            }
            
        } catch (Exception e) {
            logger.error("异步刷新服务器 {} 所有状态失败: {}", serverId, e.getMessage());
        }
        
        return CompletableFuture.completedFuture(null);
    }

    // 健康检查方法

    private boolean checkMonitoringServiceHealth() {
        try {
            // 简单的健康检查 - 尝试获取一个服务器的监控数据
            List<Server> servers = serverService.findAllActive();
            if (!servers.isEmpty()) {
                Server firstServer = servers.get(0);
                // 尝试获取监控数据，如果没有异常就认为服务正常
                resourceMonitoringService.checkMemoryUsage(firstServer.getId());
            }
            return true;
        } catch (Exception e) {
            logger.error("监控服务健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkAlertServiceHealth() {
        try {
            // 检查告警服务是否能正常获取统计信息
            List<Server> servers = serverService.findAllActive();
            if (!servers.isEmpty()) {
                Server firstServer = servers.get(0);
                resourceAlertService.getServerAlertStatistics(firstServer.getId());
            }
            return true;
        } catch (Exception e) {
            logger.error("告警服务健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkTagServiceHealth() {
        try {
            // 检查标签服务是否能正常工作
            List<Server> servers = serverService.findAllActive();
            if (!servers.isEmpty()) {
                Server firstServer = servers.get(0);
                statusTagService.getServerStatusTags(firstServer.getId());
            }
            return true;
        } catch (Exception e) {
            logger.error("标签服务健康检查失败: {}", e.getMessage());
            return false;
        }
    }
}