package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.AlertThreshold;
import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerMetrics;
import com.cmict.internalpaas.repository.AlertThresholdRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 告警阈值管理服务
 * 提供阈值配置的CRUD操作和告警检查功能
 */
@Service
@Transactional
public class AlertThresholdService {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertThresholdService.class);
    
    @Autowired
    private AlertThresholdRepository thresholdRepository;
    
    @Autowired
    private ServerService serverService;
    
    // 默认阈值配置
    private static final Map<AlertThreshold.MetricType, double[]> DEFAULT_THRESHOLDS = new HashMap<>();
    static {
        // 格式: [警告阈值, 严重阈值]
        DEFAULT_THRESHOLDS.put(AlertThreshold.MetricType.CPU, new double[]{80.0, 95.0});
        DEFAULT_THRESHOLDS.put(AlertThreshold.MetricType.MEMORY, new double[]{85.0, 95.0});
        DEFAULT_THRESHOLDS.put(AlertThreshold.MetricType.DISK, new double[]{85.0, 95.0});
    }
    
    /**
     * 获取服务器的所有阈值配置
     */
    public List<AlertThreshold> getServerThresholds(Long serverId) {
        try {
            List<AlertThreshold> thresholds = thresholdRepository.findByServerIdOrderByMetricType(serverId);
            
            // 如果没有配置，创建默认阈值
            if (thresholds.isEmpty()) {
                thresholds = createDefaultThresholdsForServer(serverId);
            }
            
            return thresholds;
        } catch (Exception e) {
            logger.error("获取服务器{}的阈值配置失败", serverId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 获取指定服务器和指标类型的阈值配置
     */
    public Optional<AlertThreshold> getThreshold(Long serverId, AlertThreshold.MetricType metricType) {
        try {
            Optional<AlertThreshold> threshold = thresholdRepository.findByServerIdAndMetricType(serverId, metricType);
            
            // 如果没有配置，尝试创建默认阈值
            if (threshold.isEmpty()) {
                AlertThreshold defaultThreshold = createDefaultThreshold(serverId, metricType);
                return Optional.of(defaultThreshold);
            }
            
            return threshold;
        } catch (Exception e) {
            logger.error("获取服务器{}的{}阈值配置失败", serverId, metricType, e);
            return Optional.empty();
        }
    }
    
    /**
     * 创建或更新阈值配置
     */
    public AlertThreshold saveThreshold(AlertThreshold threshold) {
        try {
            if (threshold.getId() == null) {
                // 新建阈值配置
                threshold.setCreatedAt(LocalDateTime.now());
                logger.info("创建新的阈值配置: 服务器ID={}, 指标类型={}", threshold.getServerId(), threshold.getMetricType());
            } else {
                // 更新现有配置
                threshold.setUpdatedAt(LocalDateTime.now());
                logger.info("更新阈值配置: ID={}", threshold.getId());
            }
            
            return thresholdRepository.save(threshold);
        } catch (Exception e) {
            logger.error("保存阈值配置失败", e);
            throw new RuntimeException("保存阈值配置失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 批量保存阈值配置
     */
    public List<AlertThreshold> saveThresholds(List<AlertThreshold> thresholds) {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            for (AlertThreshold threshold : thresholds) {
                if (threshold.getId() == null) {
                    threshold.setCreatedAt(now);
                }
                threshold.setUpdatedAt(now);
            }
            
            List<AlertThreshold> savedThresholds = thresholdRepository.saveAll(thresholds);
            logger.info("批量保存了{}个阈值配置", savedThresholds.size());
            
            return savedThresholds;
        } catch (Exception e) {
            logger.error("批量保存阈值配置失败", e);
            throw new RuntimeException("批量保存阈值配置失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 删除阈值配置
     */
    public void deleteThreshold(Long thresholdId) {
        try {
            thresholdRepository.deleteById(thresholdId);
            logger.info("删除阈值配置: ID={}", thresholdId);
        } catch (Exception e) {
            logger.error("删除阈值配置失败: ID={}", thresholdId, e);
            throw new RuntimeException("删除阈值配置失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 删除服务器的所有阈值配置
     */
    public void deleteServerThresholds(Long serverId) {
        try {
            thresholdRepository.deleteByServerId(serverId);
            logger.info("删除服务器{}的所有阈值配置", serverId);
        } catch (Exception e) {
            logger.error("删除服务器{}的阈值配置失败", serverId, e);
            throw new RuntimeException("删除服务器阈值配置失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 检查服务器指标是否触发告警
     */
    public List<AlertResult> checkServerAlerts(ServerMetrics metrics) {
        List<AlertResult> results = new ArrayList<>();
        
        try {
            Long serverId = metrics.getServerId();
            List<AlertThreshold> thresholds = getServerThresholds(serverId);
            
            for (AlertThreshold threshold : thresholds) {
                if (!threshold.getEnabled()) {
                    continue;
                }
                
                Double value = getMetricValue(metrics, threshold.getMetricType());
                if (value == null) {
                    continue;
                }
                
                AlertThreshold.AlertLevel level = threshold.checkAlert(value);
                if (level != null) {
                    AlertResult result = new AlertResult(
                        threshold,
                        level,
                        value,
                        metrics.getTimestamp(),
                        generateAlertMessage(threshold, level, value)
                    );
                    
                    results.add(result);
                    
                    // 更新最后触发时间
                    threshold.updateLastTriggered();
                    thresholdRepository.save(threshold);
                    
                    logger.warn("触发告警: 服务器ID={}, 指标={}, 级别={}, 当前值={}, 阈值={}",
                        serverId, threshold.getMetricType(), level, value,
                        level == AlertThreshold.AlertLevel.CRITICAL ? 
                            threshold.getCriticalThreshold() : threshold.getWarningThreshold());
                }
            }
        } catch (Exception e) {
            logger.error("检查服务器{}告警失败", metrics.getServerId(), e);
        }
        
        return results;
    }
    
    /**
     * 批量检查多个服务器的告警
     */
    public Map<Long, List<AlertResult>> checkMultipleServerAlerts(List<ServerMetrics> metricsList) {
        Map<Long, List<AlertResult>> results = new HashMap<>();
        
        for (ServerMetrics metrics : metricsList) {
            List<AlertResult> serverAlerts = checkServerAlerts(metrics);
            if (!serverAlerts.isEmpty()) {
                results.put(metrics.getServerId(), serverAlerts);
            }
        }
        
        return results;
    }
    
    /**
     * 获取全局阈值统计信息
     */
    public Map<String, Object> getThresholdStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            stats.put("totalThresholds", thresholdRepository.count());
            stats.put("enabledThresholds", thresholdRepository.countByEnabledTrue());
            stats.put("notificationEnabledThresholds", thresholdRepository.countByEnabledTrueAndNotificationEnabledTrue());
            
            // 按服务器统计
            List<Object[]> serverStats = thresholdRepository.countThresholdsByServer();
            Map<Long, Long> serverThresholdCounts = serverStats.stream()
                .collect(Collectors.toMap(
                    row -> (Long) row[0],
                    row -> (Long) row[1]
                ));
            stats.put("thresholdsByServer", serverThresholdCounts);
            
            // 按指标类型统计
            Map<AlertThreshold.MetricType, Long> metricTypeCounts = new HashMap<>();
            for (AlertThreshold.MetricType type : AlertThreshold.MetricType.values()) {
                long count = thresholdRepository.findByMetricTypeOrderByServerIdAsc(type).size();
                metricTypeCounts.put(type, count);
            }
            stats.put("thresholdsByMetricType", metricTypeCounts);
            
        } catch (Exception e) {
            logger.error("获取阈值统计信息失败", e);
        }
        
        return stats;
    }
    
    /**
     * 为服务器创建默认阈值配置
     */
    public List<AlertThreshold> createDefaultThresholdsForServer(Long serverId) {
        List<AlertThreshold> thresholds = new ArrayList<>();
        
        try {
            for (Map.Entry<AlertThreshold.MetricType, double[]> entry : DEFAULT_THRESHOLDS.entrySet()) {
                AlertThreshold.MetricType metricType = entry.getKey();
                double[] values = entry.getValue();
                
                AlertThreshold threshold = new AlertThreshold(
                    serverId, metricType, values[0], values[1]
                );
                threshold.setDescription("系统默认阈值");
                threshold.setCreatedBy("system");
                
                thresholds.add(threshold);
            }
            
            thresholds = thresholdRepository.saveAll(thresholds);
            logger.info("为服务器{}创建了{}个默认阈值配置", serverId, thresholds.size());
            
        } catch (Exception e) {
            logger.error("为服务器{}创建默认阈值配置失败", serverId, e);
        }
        
        return thresholds;
    }
    
    /**
     * 重置服务器阈值为默认值
     */
    public List<AlertThreshold> resetServerThresholds(Long serverId) {
        try {
            // 删除现有配置
            thresholdRepository.deleteByServerId(serverId);
            
            // 创建默认配置
            return createDefaultThresholdsForServer(serverId);
        } catch (Exception e) {
            logger.error("重置服务器{}阈值配置失败", serverId, e);
            throw new RuntimeException("重置阈值配置失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 复制阈值配置到其他服务器
     */
    public List<AlertThreshold> copyThresholdsToServers(Long sourceServerId, List<Long> targetServerIds) {
        List<AlertThreshold> copiedThresholds = new ArrayList<>();
        
        try {
            List<AlertThreshold> sourceThresholds = getServerThresholds(sourceServerId);
            
            for (Long targetServerId : targetServerIds) {
                for (AlertThreshold sourceThreshold : sourceThresholds) {
                    AlertThreshold copy = new AlertThreshold(
                        targetServerId,
                        sourceThreshold.getMetricType(),
                        sourceThreshold.getWarningThreshold(),
                        sourceThreshold.getCriticalThreshold()
                    );
                    
                    copy.setEnabled(sourceThreshold.getEnabled());
                    copy.setNotificationEnabled(sourceThreshold.getNotificationEnabled());
                    copy.setNotificationIntervalMinutes(sourceThreshold.getNotificationIntervalMinutes());
                    copy.setDescription("从服务器" + sourceServerId + "复制");
                    
                    copiedThresholds.add(copy);
                }
            }
            
            copiedThresholds = thresholdRepository.saveAll(copiedThresholds);
            logger.info("从服务器{}复制了{}个阈值配置到{}个目标服务器", 
                sourceServerId, sourceThresholds.size(), targetServerIds.size());
            
        } catch (Exception e) {
            logger.error("复制阈值配置失败", e);
            throw new RuntimeException("复制阈值配置失败: " + e.getMessage(), e);
        }
        
        return copiedThresholds;
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 创建单个默认阈值配置
     */
    private AlertThreshold createDefaultThreshold(Long serverId, AlertThreshold.MetricType metricType) {
        double[] values = DEFAULT_THRESHOLDS.get(metricType);
        if (values == null) {
            values = new double[]{80.0, 95.0}; // 默认值
        }
        
        AlertThreshold threshold = new AlertThreshold(serverId, metricType, values[0], values[1]);
        threshold.setDescription("系统默认阈值");
        threshold.setCreatedBy("system");
        
        return thresholdRepository.save(threshold);
    }
    
    /**
     * 从监控指标中获取指定类型的值
     */
    private Double getMetricValue(ServerMetrics metrics, AlertThreshold.MetricType metricType) {
        switch (metricType) {
            case CPU:
                return metrics.getCpuUsage();
            case MEMORY:
                return metrics.getMemoryUsage();
            case DISK:
                return metrics.getDiskUsage();
            case LOAD_AVERAGE:
                // 解析负载平均值（取第一个值）
                String loadAvg = metrics.getLoadAverage();
                if (loadAvg != null && !loadAvg.isEmpty()) {
                    try {
                        String firstValue = loadAvg.split(",")[0].trim();
                        return Double.parseDouble(firstValue);
                    } catch (Exception e) {
                        logger.debug("解析负载平均值失败: {}", loadAvg);
                    }
                }
                return null;
            default:
                return null;
        }
    }
    
    /**
     * 生成告警消息
     */
    private String generateAlertMessage(AlertThreshold threshold, AlertThreshold.AlertLevel level, Double value) {
        String serverName = "未知服务器";
        if (threshold.getServerId() != null) {
            Optional<Server> server = serverService.findById(threshold.getServerId());
            if (server.isPresent()) {
                serverName = server.get().getName();
            }
        }
        
        String metricName = threshold.getMetricType().getDisplayName();
        String unit = threshold.getMetricType().getUnit();
        String thresholdValue = level == AlertThreshold.AlertLevel.CRITICAL ?
            threshold.getCriticalThreshold().toString() :
            threshold.getWarningThreshold().toString();
        
        return String.format("[%s] 服务器 %s 的 %s 达到 %.2f%s，超过%s阈值 %s%s",
            level.getDisplayName(),
            serverName,
            metricName,
            value,
            unit,
            level.getDisplayName(),
            thresholdValue,
            unit
        );
    }
    
    /**
     * 告警结果内部类
     */
    public static class AlertResult {
        private final AlertThreshold threshold;
        private final AlertThreshold.AlertLevel level;
        private final Double value;
        private final LocalDateTime timestamp;
        private final String message;
        
        public AlertResult(AlertThreshold threshold, AlertThreshold.AlertLevel level, 
                         Double value, LocalDateTime timestamp, String message) {
            this.threshold = threshold;
            this.level = level;
            this.value = value;
            this.timestamp = timestamp;
            this.message = message;
        }
        
        // Getters
        public AlertThreshold getThreshold() { return threshold; }
        public AlertThreshold.AlertLevel getLevel() { return level; }
        public Double getValue() { return value; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getMessage() { return message; }
        
        public Long getServerId() { return threshold.getServerId(); }
        public AlertThreshold.MetricType getMetricType() { return threshold.getMetricType(); }
        
        @Override
        public String toString() {
            return "AlertResult{" +
                    "level=" + level +
                    ", value=" + value +
                    ", timestamp=" + timestamp +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
}