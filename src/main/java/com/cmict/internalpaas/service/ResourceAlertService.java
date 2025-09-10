package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerMetrics;
import com.cmict.internalpaas.model.ServerResourceThreshold;
import com.cmict.internalpaas.model.ServerResourceThreshold.ResourceType;
import com.cmict.internalpaas.model.ServerResourceThreshold.ThresholdLevel;
import com.cmict.internalpaas.model.ServerStatusTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ResourceAlertService {
    
    private static final Logger logger = LoggerFactory.getLogger(ResourceAlertService.class);
    
    // 用于防止重复告警的缓存
    private final Map<String, LocalDateTime> lastAlertTimes = new ConcurrentHashMap<>();
    
    // 告警抑制时间（分钟），避免频繁告警
    private static final int ALERT_SUPPRESS_MINUTES = 5;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private ServerService serverService;
    
    @Autowired
    private ServerResourceMonitoringService resourceMonitoringService;

    /**
     * 检查资源使用率并发送告警
     */
    public void checkAndAlert(Long serverId, ResourceType resourceType, 
                            double currentValue, ServerResourceThreshold threshold) {
        
        if (!threshold.shouldAlert(currentValue)) {
            return;
        }
        
        // 检查是否在抑制期内
        String alertKey = serverId + ":" + resourceType.name();
        LocalDateTime lastAlertTime = lastAlertTimes.get(alertKey);
        LocalDateTime now = LocalDateTime.now();
        
        if (lastAlertTime != null && 
            lastAlertTime.plusMinutes(ALERT_SUPPRESS_MINUTES).isAfter(now)) {
            logger.debug("服务器 {} {} 告警在抑制期内，跳过", serverId, resourceType.getDisplayName());
            return;
        }
        
        // 更新最后告警时间
        lastAlertTimes.put(alertKey, now);
        
        ThresholdLevel level = threshold.evaluateLevel(currentValue);
        String alertMessage = threshold.getAlertMessage(currentValue);
        
        logger.warn("🚨 资源告警 - 服务器 {}: {}", serverId, alertMessage);
        
        // 发送WebSocket实时通知
        sendResourceAlertNotification(serverId, resourceType, currentValue, level, alertMessage);
        
        // 特殊处理内存告警
        if (resourceType == ResourceType.MEMORY) {
            handleMemoryAlert(serverId, currentValue, level, alertMessage);
        }
        
        // 可以扩展：发送邮件、钉钉通知等
        // sendEmailAlert(serverId, resourceType, currentValue, level, alertMessage);
        // sendDingTalkAlert(serverId, resourceType, currentValue, level, alertMessage);
    }

    /**
     * 内存使用率专门告警处理 - 80%阈值重点关注
     */
    public void handleMemoryAlert(Long serverId, double memoryUsage, ThresholdLevel level, String alertMessage) {
        
        Optional<Server> serverOpt = serverService.findById(serverId);
        String serverName = serverOpt.map(Server::getName).orElse("Unknown");
        
        // 80%以上的内存告警需要特别处理
        if (memoryUsage >= 80.0) {
            logger.error("🔥 严重内存告警 - 服务器 {} ({}) 内存使用率 {}% 超过80%阈值", 
                        serverName, serverId, memoryUsage);
                        
            // 发送高优先级通知
            sendCriticalMemoryAlert(serverId, serverName, memoryUsage, level);
            
            // 记录到系统日志
            logCriticalMemoryAlert(serverId, serverName, memoryUsage);
        }
        
        // 90%以上为危险级别
        if (memoryUsage >= 90.0) {
            logger.error("💥 危险内存告警 - 服务器 {} ({}) 内存使用率 {}% 超过90%，系统面临崩溃风险！", 
                        serverName, serverId, memoryUsage);
                        
            // 发送紧急通知
            sendEmergencyMemoryAlert(serverId, serverName, memoryUsage);
        }
    }

    /**
     * 监听服务器指标更新事件，自动检查内存告警
     */
    @EventListener
    @Async("resourceAlertExecutor")
    public void handleServerMetricsUpdate(ServerMetrics metrics) {
        try {
            Long serverId = metrics.getServerId();
            double memoryUsage = metrics.getMemoryUsagePercent();
            
            // 检查80%内存阈值
            if (memoryUsage >= 80.0) {
                logger.warn("📊 监控到服务器 {} 内存使用率 {}% 超过80%阈值", serverId, memoryUsage);
                
                // 触发内存检查和告警
                ServerStatusTag memoryTag = resourceMonitoringService.checkMemoryUsage(serverId);
                
                if (memoryTag != null && (memoryTag.isCritical() || memoryTag.isDanger())) {
                    ThresholdLevel level = memoryUsage >= 90.0 ? ThresholdLevel.DANGER : ThresholdLevel.CRITICAL;
                    String alertMessage = String.format("内存使用率 %.1f%% 超过80%%阈值", memoryUsage);
                    
                    handleMemoryAlert(serverId, memoryUsage, level, alertMessage);
                }
            }
            
            // 同时检查CPU和磁盘
            checkOtherResourceAlerts(serverId, metrics);
            
        } catch (Exception e) {
            logger.error("处理服务器指标更新事件失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送WebSocket资源告警通知
     */
    private void sendResourceAlertNotification(Long serverId, ResourceType resourceType, 
                                             double currentValue, ThresholdLevel level, String alertMessage) {
        
        Map<String, Object> alertData = new HashMap<>();
        alertData.put("type", "resource_alert");
        alertData.put("serverId", serverId);
        alertData.put("resourceType", resourceType.name());
        alertData.put("resourceDisplayName", resourceType.getDisplayName());
        alertData.put("currentValue", currentValue);
        alertData.put("level", level.name());
        alertData.put("levelDisplayName", level.getDisplayName());
        alertData.put("message", alertMessage);
        alertData.put("timestamp", System.currentTimeMillis());
        alertData.put("severity", getSeverityForLevel(level));
        alertData.put("colorScheme", level.getColorScheme());
        
        // 发送到所有管理员
        messagingTemplate.convertAndSend("/topic/alerts", alertData);
        
        // 发送到特定服务器的订阅者
        messagingTemplate.convertAndSend("/topic/server/" + serverId + "/alerts", alertData);
        
        logger.info("📤 已发送WebSocket告警通知: 服务器 {} - {}", serverId, alertMessage);
    }

    /**
     * 发送严重内存告警（80%以上）
     */
    private void sendCriticalMemoryAlert(Long serverId, String serverName, double memoryUsage, ThresholdLevel level) {
        
        Map<String, Object> criticalAlert = new HashMap<>();
        criticalAlert.put("type", "critical_memory_alert");
        criticalAlert.put("serverId", serverId);
        criticalAlert.put("serverName", serverName);
        criticalAlert.put("memoryUsage", memoryUsage);
        criticalAlert.put("level", level.name());
        criticalAlert.put("title", "严重内存告警");
        criticalAlert.put("message", String.format("服务器 %s 内存使用率 %.1f%% 超过80%%严重阈值", serverName, memoryUsage));
        criticalAlert.put("timestamp", System.currentTimeMillis());
        criticalAlert.put("priority", "HIGH");
        criticalAlert.put("autoClose", false); // 严重告警不自动关闭
        criticalAlert.put("sound", true); // 播放提示音
        criticalAlert.put("actions", new String[]{"查看详情", "暂时忽略"});
        
        // 发送到管理员告警频道
        messagingTemplate.convertAndSend("/topic/alerts/critical", criticalAlert);
        
        logger.warn("📢 已发送严重内存告警通知: 服务器 {} 内存 {}%", serverName, memoryUsage);
    }

    /**
     * 发送紧急内存告警（90%以上）
     */
    private void sendEmergencyMemoryAlert(Long serverId, String serverName, double memoryUsage) {
        
        Map<String, Object> emergencyAlert = new HashMap<>();
        emergencyAlert.put("type", "emergency_memory_alert");
        emergencyAlert.put("serverId", serverId);
        emergencyAlert.put("serverName", serverName);
        emergencyAlert.put("memoryUsage", memoryUsage);
        emergencyAlert.put("title", "🚨 紧急内存告警");
        emergencyAlert.put("message", String.format("服务器 %s 内存使用率 %.1f%% 达到危险级别！系统面临崩溃风险，请立即处理！", 
                                                   serverName, memoryUsage));
        emergencyAlert.put("timestamp", System.currentTimeMillis());
        emergencyAlert.put("priority", "EMERGENCY");
        emergencyAlert.put("autoClose", false);
        emergencyAlert.put("sound", true);
        emergencyAlert.put("persistent", true); // 持久显示，直到用户处理
        emergencyAlert.put("actions", new String[]{"立即处理", "查看服务器", "暂时忽略"});
        
        // 发送到紧急告警频道
        messagingTemplate.convertAndSend("/topic/alerts/emergency", emergencyAlert);
        
        // 同时发送到所有在线管理员
        messagingTemplate.convertAndSend("/topic/alerts/broadcast", emergencyAlert);
        
        logger.error("🚨 已发送紧急内存告警: 服务器 {} 内存 {}%", serverName, memoryUsage);
    }

    /**
     * 记录严重内存告警到系统日志
     */
    private void logCriticalMemoryAlert(Long serverId, String serverName, double memoryUsage) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String logMessage = String.format("[%s] CRITICAL MEMORY ALERT - Server: %s (ID: %d), Memory Usage: %.1f%%, Threshold: 80%%", 
                                         timestamp, serverName, serverId, memoryUsage);
        
        // 写入专门的告警日志文件
        logger.error(logMessage);
        
        // 可以扩展：写入数据库告警记录表
        // saveAlertRecord(serverId, ResourceType.MEMORY, memoryUsage, ThresholdLevel.CRITICAL, logMessage);
    }

    /**
     * 检查其他资源告警（CPU、磁盘等）
     */
    private void checkOtherResourceAlerts(Long serverId, ServerMetrics metrics) {
        try {
            // 检查CPU使用率
            double cpuUsage = metrics.getCpuUsagePercent();
            if (cpuUsage >= 90.0) {
                logger.warn("服务器 {} CPU使用率 {}% 超过90%", serverId, cpuUsage);
                sendResourceAlertNotification(serverId, ResourceType.CPU, cpuUsage, 
                                            ThresholdLevel.CRITICAL, 
                                            String.format("CPU使用率 %.1f%% 过高", cpuUsage));
            }
            
            // 检查磁盘使用率
            double diskUsage = metrics.getDiskUsagePercent();
            if (diskUsage >= 90.0) {
                logger.warn("服务器 {} 磁盘使用率 {}% 超过90%", serverId, diskUsage);
                sendResourceAlertNotification(serverId, ResourceType.DISK, diskUsage, 
                                            ThresholdLevel.CRITICAL, 
                                            String.format("磁盘使用率 %.1f%% 过高", diskUsage));
            }
            
        } catch (Exception e) {
            logger.error("检查其他资源告警失败: {}", e.getMessage());
        }
    }

    /**
     * 清理过期的告警抑制记录
     */
    public void cleanupExpiredAlertSuppressions() {
        LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(ALERT_SUPPRESS_MINUTES * 2);
        
        lastAlertTimes.entrySet().removeIf(entry -> 
            entry.getValue().isBefore(expiredTime));
        
        logger.debug("已清理 {} 个过期的告警抑制记录", lastAlertTimes.size());
    }

    /**
     * 获取告警级别对应的严重程度
     */
    private String getSeverityForLevel(ThresholdLevel level) {
        switch (level) {
            case WARNING: return "warning";
            case CRITICAL: return "error";
            case DANGER: return "critical";
            default: return "info";
        }
    }

    /**
     * 批量检查所有服务器的资源告警
     */
    @Async("resourceAlertExecutor")
    public void checkAllServerResourceAlerts() {
        try {
            logger.debug("开始批量检查所有服务器资源告警");
            
            // 检查所有启用的服务器内存使用率
            resourceMonitoringService.checkAllMemoryUsage();
            
            // 清理过期的告警抑制
            cleanupExpiredAlertSuppressions();
            
            logger.debug("批量资源告警检查完成");
            
        } catch (Exception e) {
            logger.error("批量检查资源告警失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取服务器当前告警统计
     */
    public Map<String, Object> getServerAlertStatistics(Long serverId) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 获取当前内存状态
            ServerStatusTag memoryTag = resourceMonitoringService.checkMemoryUsage(serverId);
            if (memoryTag != null) {
                stats.put("memoryAlert", true);
                stats.put("memoryLevel", memoryTag.getStatus().name());
                stats.put("memoryValue", memoryTag.getValue());
            } else {
                stats.put("memoryAlert", false);
            }
            
            // 统计告警抑制状态
            String memoryAlertKey = serverId + ":" + ResourceType.MEMORY.name();
            LocalDateTime lastMemoryAlert = lastAlertTimes.get(memoryAlertKey);
            if (lastMemoryAlert != null) {
                stats.put("lastMemoryAlertTime", lastMemoryAlert);
                stats.put("memoryAlertSuppressed", 
                         lastMemoryAlert.plusMinutes(ALERT_SUPPRESS_MINUTES).isAfter(LocalDateTime.now()));
            }
            
        } catch (Exception e) {
            logger.error("获取服务器 {} 告警统计失败: {}", serverId, e.getMessage());
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
}