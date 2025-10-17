package com.cmict.internalpaas.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 告警阈值实体类
 * 用于管理监控指标的告警阈值配置
 */
@Entity
@Table(name = "alert_thresholds")
public class AlertThreshold {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "server_id")
    private Long serverId;
    
    @Column(name = "metric_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private MetricType metricType;
    
    @Column(name = "warning_threshold")
    private Double warningThreshold;
    
    @Column(name = "critical_threshold")
    private Double criticalThreshold;
    
    @Column(name = "enabled")
    private Boolean enabled = true;
    
    @Column(name = "notification_enabled")
    private Boolean notificationEnabled = true;
    
    @Column(name = "notification_interval_minutes")
    private Integer notificationIntervalMinutes = 30; // 通知间隔（分钟）
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "created_by")
    private String createdBy;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "last_triggered")
    private LocalDateTime lastTriggered;
    
    /**
     * 监控指标类型枚举
     */
    public enum MetricType {
        CPU("CPU使用率", "%"),
        MEMORY("内存使用率", "%"),
        DISK("磁盘使用率", "%"),
        LOAD_AVERAGE("负载平均值", ""),
        NETWORK_IN("网络入流量", "MB/s"),
        NETWORK_OUT("网络出流量", "MB/s");
        
        private final String displayName;
        private final String unit;
        
        MetricType(String displayName, String unit) {
            this.displayName = displayName;
            this.unit = unit;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getUnit() {
            return unit;
        }
    }
    
    /**
     * 告警级别枚举
     */
    public enum AlertLevel {
        WARNING("警告", "#ffc107"),
        CRITICAL("严重", "#dc3545");
        
        private final String displayName;
        private final String color;
        
        AlertLevel(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getColor() {
            return color;
        }
    }
    
    // 构造函数
    public AlertThreshold() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public AlertThreshold(Long serverId, MetricType metricType, Double warningThreshold, Double criticalThreshold) {
        this();
        this.serverId = serverId;
        this.metricType = metricType;
        this.warningThreshold = warningThreshold;
        this.criticalThreshold = criticalThreshold;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }
    
    public MetricType getMetricType() { return metricType; }
    public void setMetricType(MetricType metricType) { this.metricType = metricType; }
    
    public Double getWarningThreshold() { return warningThreshold; }
    public void setWarningThreshold(Double warningThreshold) { this.warningThreshold = warningThreshold; }
    
    public Double getCriticalThreshold() { return criticalThreshold; }
    public void setCriticalThreshold(Double criticalThreshold) { this.criticalThreshold = criticalThreshold; }
    
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    
    public Boolean getNotificationEnabled() { return notificationEnabled; }
    public void setNotificationEnabled(Boolean notificationEnabled) { this.notificationEnabled = notificationEnabled; }
    
    public Integer getNotificationIntervalMinutes() { return notificationIntervalMinutes; }
    public void setNotificationIntervalMinutes(Integer notificationIntervalMinutes) { this.notificationIntervalMinutes = notificationIntervalMinutes; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getLastTriggered() { return lastTriggered; }
    public void setLastTriggered(LocalDateTime lastTriggered) { this.lastTriggered = lastTriggered; }
    
    /**
     * 检查指定值是否触发告警
     * @param value 监控值
     * @return 告警级别，如果未触发返回null
     */
    public AlertLevel checkAlert(Double value) {
        if (!enabled || value == null) {
            return null;
        }
        
        if (criticalThreshold != null && value >= criticalThreshold) {
            return AlertLevel.CRITICAL;
        }
        
        if (warningThreshold != null && value >= warningThreshold) {
            return AlertLevel.WARNING;
        }
        
        return null;
    }
    
    /**
     * 检查是否可以发送通知（基于通知间隔）
     */
    public boolean canSendNotification() {
        if (!notificationEnabled) {
            return false;
        }
        
        if (lastTriggered == null) {
            return true;
        }
        
        return LocalDateTime.now().isAfter(lastTriggered.plusMinutes(notificationIntervalMinutes));
    }
    
    /**
     * 更新最后触发时间
     */
    public void updateLastTriggered() {
        this.lastTriggered = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 更新修改时间
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "AlertThreshold{" +
                "id=" + id +
                ", serverId=" + serverId +
                ", metricType=" + metricType +
                ", warningThreshold=" + warningThreshold +
                ", criticalThreshold=" + criticalThreshold +
                ", enabled=" + enabled +
                '}';
    }
}