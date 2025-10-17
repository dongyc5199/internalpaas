package com.cmict.internalpaas.service;

import com.cmict.internalpaas.client.MetricsHubClient;
import com.cmict.internalpaas.model.ServerMetrics;
import com.cmict.internalpaas.model.ServerResourceThreshold;
import com.cmict.internalpaas.model.ServerResourceThreshold.ResourceType;
import com.cmict.internalpaas.model.ServerResourceThreshold.ThresholdLevel;
import com.cmict.internalpaas.model.ServerStatusTag;
import com.cmict.internalpaas.model.ServerStatusTag.TagType;
import com.cmict.internalpaas.model.ServerStatusTag.TagStatus;
import com.cmict.internalpaas.repository.ServerResourceThresholdRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@Transactional
public class ServerResourceMonitoringService {
    
    private static final Logger logger = LoggerFactory.getLogger(ServerResourceMonitoringService.class);
    
    /**
     * Metrics Hub客户端 (Phase4迁移: 从Hub获取监控数据)
     */
    @Autowired(required = false)
    private MetricsHubClient metricsHubClient;
    
    @Autowired
    private ServerResourceThresholdRepository thresholdRepository;
    
    @Autowired
    private ServerStatusTagService tagService;
    
    /**
     * 检查服务器资源状态并生成相应的状态标签
     * 
     * Phase4迁移: 从 Hub 获取监控数据
     */
    public List<ServerStatusTag> checkResourceStatuses(Long serverId) {
        List<ServerStatusTag> resourceTags = new ArrayList<>();
        
        try {
            // 检查 Hub 是否可用
            if (metricsHubClient == null || !metricsHubClient.isAvailable()) {
                logger.warn("⚠️ Hub服务不可用，无法检查服务器 {} 的资源状态", serverId);
                return Collections.singletonList(createUnknownResourceTag(serverId));
            }
            
            // 从 Hub 获取最新的服务器指标
            ServerMetrics latestMetrics = metricsHubClient.getLatestMetrics(serverId);
            if (latestMetrics == null) {
                logger.warn("服务器 {} 无法从Hub获取最新指标数据", serverId);
                return Collections.singletonList(createUnknownResourceTag(serverId));
            }
            
            // 获取该服务器的资源阈值配置
            List<ServerResourceThreshold> thresholds = thresholdRepository.findByServerIdAndEnabled(serverId, true);
            
            // 如果没有配置阈值，使用默认值
            if (thresholds.isEmpty()) {
                thresholds = createDefaultThresholds(serverId);
            }
            
            // 检查每种资源类型
            for (ServerResourceThreshold threshold : thresholds) {
                Double currentValue = getCurrentResourceValue(latestMetrics, threshold.getResourceType());
                if (currentValue != null) {
                    ServerStatusTag tag = evaluateResourceStatus(serverId, threshold, currentValue);
                    if (tag != null) {
                        resourceTags.add(tag);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("检查服务器 {} 资源状态失败: {}", serverId, e.getMessage(), e);
        }
        
        return resourceTags;
    }

    /**
     * 专门检查内存使用率 - 重点实现80%阈值告警
     * 
     * Phase4迁移: 从 Hub 获取监控数据
     */
    public ServerStatusTag checkMemoryUsage(Long serverId) {
        try {
            // 检查 Hub 是否可用
            if (metricsHubClient == null || !metricsHubClient.isAvailable()) {
                logger.warn("⚠️ Hub服务不可用，无法检查服务器 {} 的内存使用率", serverId);
                return createTag(serverId, TagType.MEMORY_USAGE, TagStatus.UNKNOWN, 
                    "内存状态未知", "secondary", null, "Hub服务不可用");
            }
            
            // 从 Hub 获取最新监控数据
            ServerMetrics latestMetrics = metricsHubClient.getLatestMetrics(serverId);
            if (latestMetrics == null) {
                logger.warn("服务器 {} 无法从Hub获取内存使用数据", serverId);
                return createTag(serverId, TagType.MEMORY_USAGE, TagStatus.UNKNOWN, 
                    "内存状态未知", "secondary", null, "无法获取内存数据");
            }
            
            double memoryUsage = latestMetrics.getMemoryUsagePercent();
            logger.debug("服务器 {} 当前内存使用率: {}%", serverId, memoryUsage);
            
            // 获取内存阈值配置，默认警告70%，严重80%，危险90%
            ServerResourceThreshold memoryThreshold = getOrCreateMemoryThreshold(serverId);
            
            return evaluateMemoryStatus(serverId, memoryUsage, memoryThreshold);
            
        } catch (Exception e) {
            logger.error("检查服务器 {} 内存使用率失败: {}", serverId, e.getMessage(), e);
            return createTag(serverId, TagType.MEMORY_USAGE, TagStatus.ERROR, 
                "内存检查失败", "danger", null, "内存监控异常: " + e.getMessage());
        }
    }

    /**
     * 评估内存状态 - 核心80%阈值告警逻辑
     */
    private ServerStatusTag evaluateMemoryStatus(Long serverId, double memoryUsage, 
                                               ServerResourceThreshold threshold) {
        
        ThresholdLevel level = threshold.evaluateLevel(memoryUsage);
        String formattedUsage = String.format("%.1f%%", memoryUsage);
        
        switch (level) {
            case DANGER:
                logger.warn("服务器 {} 内存使用率 {}% 达到危险级别(>{}%)", 
                           serverId, memoryUsage, threshold.getDangerThreshold());
                return createTag(serverId, TagType.MEMORY_USAGE, TagStatus.DANGER,
                    "内存 " + formattedUsage, "danger", formattedUsage,
                    String.format("内存使用率 %s 超过危险阈值 %.1f%%，系统面临崩溃风险，请立即处理！", 
                                formattedUsage, threshold.getDangerThreshold()));
                    
            case CRITICAL:
                // 关键的80%阈值告警
                logger.warn("⚠️ 服务器 {} 内存使用率 {}% 超过80%严重阈值！", serverId, memoryUsage);
                return createTag(serverId, TagType.MEMORY_USAGE, TagStatus.CRITICAL,
                    "内存 " + formattedUsage, "danger", formattedUsage,
                    String.format("内存使用率 %s 超过严重阈值 %.1f%%，需要立即关注和处理", 
                                formattedUsage, threshold.getCriticalThreshold()));
                    
            case WARNING:
                logger.info("服务器 {} 内存使用率 {}% 超过警告阈值", serverId, memoryUsage);
                return createTag(serverId, TagType.MEMORY_USAGE, TagStatus.WARNING,
                    "内存 " + formattedUsage, "warning", formattedUsage,
                    String.format("内存使用率 %s 超过警告阈值 %.1f%%，建议关注", 
                                formattedUsage, threshold.getWarningThreshold()));
                    
            case NORMAL:
                // 正常状态下不显示内存标签，减少界面干扰
                logger.debug("服务器 {} 内存使用率 {}% 正常", serverId, memoryUsage);
                return null;
                
            default:
                return createTag(serverId, TagType.MEMORY_USAGE, TagStatus.UNKNOWN,
                    "内存状态未知", "secondary", null, "无法评估内存状态");
        }
    }

    /**
     * 检查CPU使用率
     * 
     * Phase4迁移: 从 Hub 获取监控数据
     */
    public ServerStatusTag checkCpuUsage(Long serverId) {
        try {
            // 检查 Hub 是否可用
            if (metricsHubClient == null || !metricsHubClient.isAvailable()) {
                logger.debug("Hub服务不可用，无法检查服务器 {} 的CPU使用率", serverId);
                return null; // CPU正常时不显示标签
            }
            
            // 从 Hub 获取最新监控数据
            ServerMetrics latestMetrics = metricsHubClient.getLatestMetrics(serverId);
            if (latestMetrics == null) {
                return null; // CPU正常时不显示标签
            }
            
            double cpuUsage = latestMetrics.getCpuUsagePercent();
            ServerResourceThreshold cpuThreshold = getOrCreateCpuThreshold(serverId);
            
            return evaluateCpuStatus(serverId, cpuUsage, cpuThreshold);
            
        } catch (Exception e) {
            logger.error("检查服务器 {} CPU使用率失败: {}", serverId, e.getMessage());
            return null;
        }
    }

    /**
     * 检查磁盘使用率
     * 
     * Phase4迁移: 从 Hub 获取监控数据
     */
    public ServerStatusTag checkDiskUsage(Long serverId) {
        try {
            // 检查 Hub 是否可用
            if (metricsHubClient == null || !metricsHubClient.isAvailable()) {
                logger.debug("Hub服务不可用，无法检查服务器 {} 的磁盘使用率", serverId);
                return null;
            }
            
            // 从 Hub 获取最新监控数据
            ServerMetrics latestMetrics = metricsHubClient.getLatestMetrics(serverId);
            if (latestMetrics == null) {
                return null;
            }
            
            double diskUsage = latestMetrics.getDiskUsagePercent();
            ServerResourceThreshold diskThreshold = getOrCreateDiskThreshold(serverId);
            
            return evaluateDiskStatus(serverId, diskUsage, diskThreshold);
            
        } catch (Exception e) {
            logger.error("检查服务器 {} 磁盘使用率失败: {}", serverId, e.getMessage());
            return null;
        }
    }

    /**
     * 批量检查所有启用内存监控的服务器
     */
    public void checkAllMemoryUsage() {
        List<ServerResourceThreshold> memoryThresholds = thresholdRepository.findAllEnabledMemoryThresholds();
        
        logger.debug("开始检查 {} 个服务器的内存使用率", memoryThresholds.size());
        
        for (ServerResourceThreshold threshold : memoryThresholds) {
            try {
                ServerStatusTag tag = checkMemoryUsage(threshold.getServerId());
                
                // 如果是告警级别，发布事件
                if (tag != null && (tag.isCritical() || tag.isDanger())) {
                    publishMemoryAlertEvent(threshold.getServerId(), tag);
                }
                
            } catch (Exception e) {
                logger.error("批量检查服务器 {} 内存失败: {}", threshold.getServerId(), e.getMessage());
            }
        }
    }

    // 私有辅助方法

    /**
     * 获取或创建默认的内存阈值配置
     */
    private ServerResourceThreshold getOrCreateMemoryThreshold(Long serverId) {
        return thresholdRepository.findByServerIdAndResourceType(serverId, ResourceType.MEMORY)
            .orElseGet(() -> {
                logger.info("为服务器 {} 创建默认内存阈值配置", serverId);
                ServerResourceThreshold defaultThreshold = ServerResourceThreshold.createDefaultMemoryThreshold(serverId);
                return thresholdRepository.save(defaultThreshold);
            });
    }

    private ServerResourceThreshold getOrCreateCpuThreshold(Long serverId) {
        return thresholdRepository.findByServerIdAndResourceType(serverId, ResourceType.CPU)
            .orElseGet(() -> {
                ServerResourceThreshold defaultThreshold = new ServerResourceThreshold(serverId, ResourceType.CPU);
                return thresholdRepository.save(defaultThreshold);
            });
    }

    private ServerResourceThreshold getOrCreateDiskThreshold(Long serverId) {
        return thresholdRepository.findByServerIdAndResourceType(serverId, ResourceType.DISK)
            .orElseGet(() -> {
                ServerResourceThreshold defaultThreshold = new ServerResourceThreshold(serverId, ResourceType.DISK);
                return thresholdRepository.save(defaultThreshold);
            });
    }

    /**
     * 评估CPU状态
     */
    private ServerStatusTag evaluateCpuStatus(Long serverId, double cpuUsage, 
                                            ServerResourceThreshold threshold) {
        
        ThresholdLevel level = threshold.evaluateLevel(cpuUsage);
        String formattedUsage = String.format("%.1f%%", cpuUsage);
        
        // 只有超过警告阈值才显示标签
        if (level == ThresholdLevel.NORMAL) {
            return null;
        }
        
        TagStatus status = convertThresholdLevelToTagStatus(level);
        String colorScheme = getColorSchemeForLevel(level);
        
        return createTag(serverId, TagType.CPU_USAGE, status,
            "CPU " + formattedUsage, colorScheme, formattedUsage,
            "CPU使用率 " + formattedUsage);
    }

    /**
     * 评估磁盘状态
     */
    private ServerStatusTag evaluateDiskStatus(Long serverId, double diskUsage, 
                                             ServerResourceThreshold threshold) {
        
        ThresholdLevel level = threshold.evaluateLevel(diskUsage);
        String formattedUsage = String.format("%.1f%%", diskUsage);
        
        // 只有超过警告阈值才显示标签
        if (level == ThresholdLevel.NORMAL) {
            return null;
        }
        
        TagStatus status = convertThresholdLevelToTagStatus(level);
        String colorScheme = getColorSchemeForLevel(level);
        
        return createTag(serverId, TagType.DISK_USAGE, status,
            "磁盘 " + formattedUsage, colorScheme, formattedUsage,
            "磁盘使用率 " + formattedUsage);
    }

    /**
     * 根据资源类型获取当前值
     */
    private Double getCurrentResourceValue(ServerMetrics metrics, ResourceType resourceType) {
        switch (resourceType) {
            case MEMORY: return metrics.getMemoryUsagePercent();
            case CPU: return metrics.getCpuUsagePercent();
            case DISK: return metrics.getDiskUsagePercent();
            case LOAD_AVERAGE: return metrics.getLoadAverageValue();
            default: return null;
        }
    }

    /**
     * 评估通用资源状态
     */
    private ServerStatusTag evaluateResourceStatus(Long serverId, ServerResourceThreshold threshold, 
                                                 Double currentValue) {
        
        switch (threshold.getResourceType()) {
            case MEMORY:
                return evaluateMemoryStatus(serverId, currentValue, threshold);
            case CPU:
                return evaluateCpuStatus(serverId, currentValue, threshold);
            case DISK:
                return evaluateDiskStatus(serverId, currentValue, threshold);
            default:
                return evaluateGenericResourceStatus(serverId, threshold, currentValue);
        }
    }

    private ServerStatusTag evaluateGenericResourceStatus(Long serverId, ServerResourceThreshold threshold, 
                                                        Double currentValue) {
        
        ThresholdLevel level = threshold.evaluateLevel(currentValue);
        if (level == ThresholdLevel.NORMAL) {
            return null;
        }
        
        String formattedValue = String.format("%.1f%s", currentValue, threshold.getResourceType().getUnit());
        TagStatus status = convertThresholdLevelToTagStatus(level);
        String colorScheme = getColorSchemeForLevel(level);
        TagType tagType = getTagTypeForResourceType(threshold.getResourceType());
        
        return createTag(serverId, tagType, status,
            threshold.getResourceType().getDisplayName() + " " + formattedValue,
            colorScheme, formattedValue, 
            threshold.getResourceType().getDisplayName() + "使用率 " + formattedValue);
    }

    /**
     * 创建默认阈值配置
     */
    private List<ServerResourceThreshold> createDefaultThresholds(Long serverId) {
        logger.info("为服务器 {} 创建默认资源阈值配置", serverId);
        
        List<ServerResourceThreshold> defaults = Arrays.asList(
            ServerResourceThreshold.createDefaultMemoryThreshold(serverId),
            new ServerResourceThreshold(serverId, ResourceType.CPU),
            new ServerResourceThreshold(serverId, ResourceType.DISK)
        );
        
        return thresholdRepository.saveAll(defaults);
    }

    /**
     * 创建未知资源标签
     */
    private ServerStatusTag createUnknownResourceTag(Long serverId) {
        return tagService.createOrUpdateTag(serverId, TagType.MONITORING, TagStatus.UNKNOWN,
            "监控异常", "无法获取资源监控数据");
    }

    /**
     * 创建状态标签的辅助方法
     */
    private ServerStatusTag createTag(Long serverId, TagType tagType, TagStatus status,
                                    String displayText, String colorScheme, String value, String details) {
        
        // 设置优先级：危险>严重>警告
        int priority;
        switch (status) {
            case DANGER: priority = 10; break;
            case CRITICAL: priority = 8; break;  
            case WARNING: priority = 6; break;
            case ERROR: priority = 9; break;
            default: priority = 1; break;
        }
        
        // 内存告警优先级更高
        if (tagType == TagType.MEMORY_USAGE && (status == TagStatus.CRITICAL || status == TagStatus.DANGER)) {
            priority += 1;
        }
        
        return tagService.createOrUpdateTag(serverId, tagType, status, displayText, value, colorScheme, priority, details);
    }

    /**
     * 转换阈值级别到标签状态
     */
    private TagStatus convertThresholdLevelToTagStatus(ThresholdLevel level) {
        switch (level) {
            case WARNING: return TagStatus.WARNING;
            case CRITICAL: return TagStatus.CRITICAL;
            case DANGER: return TagStatus.DANGER;
            default: return TagStatus.ACTIVE;
        }
    }

    /**
     * 获取级别对应的颜色方案
     */
    private String getColorSchemeForLevel(ThresholdLevel level) {
        switch (level) {
            case WARNING: return "warning";
            case CRITICAL:
            case DANGER: return "danger";
            default: return "success";
        }
    }

    /**
     * 资源类型到标签类型的映射
     */
    private TagType getTagTypeForResourceType(ResourceType resourceType) {
        switch (resourceType) {
            case MEMORY: return TagType.MEMORY_USAGE;
            case CPU: return TagType.CPU_USAGE;
            case DISK: return TagType.DISK_USAGE;
            case LOAD_AVERAGE: return TagType.SYSTEM_LOAD;
            default: return TagType.MONITORING;
        }
    }

    /**
     * 发布内存告警事件
     */
    private void publishMemoryAlertEvent(Long serverId, ServerStatusTag tag) {
        // 这里可以发布事件给 ResourceAlertService 处理
        logger.warn("📢 发布内存告警事件: 服务器 {} - {}", serverId, tag.getDisplayText());
        
        // 可以在这里发布 ApplicationEvent，由 ResourceAlertService 监听处理
        // eventPublisher.publishEvent(new MemoryAlertEvent(serverId, tag));
    }
}