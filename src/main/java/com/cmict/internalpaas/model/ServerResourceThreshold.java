package com.cmict.internalpaas.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "server_resource_thresholds", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"server_id", "resource_type"}))
public class ServerResourceThreshold {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "server_id", nullable = false)
    private Long serverId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    private ResourceType resourceType;
    
    @Column(name = "warning_threshold")
    private Double warningThreshold; // 警告阈值，如 70.0
    
    @Column(name = "critical_threshold") 
    private Double criticalThreshold; // 严重阈值，如 80.0
    
    @Column(name = "danger_threshold")
    private Double dangerThreshold; // 危险阈值，如 90.0
    
    @Column(name = "enabled")
    private Boolean enabled = true;
    
    @Column(name = "notify_enabled")
    private Boolean notifyEnabled = true; // 是否启用通知
    
    @Column(name = "check_interval_seconds")
    private Integer checkIntervalSeconds = 60; // 检查间隔，默认60秒
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 资源类型枚举
    public enum ResourceType {
        MEMORY("内存使用率", "%"),
        CPU("CPU使用率", "%"),
        DISK("磁盘使用率", "%"),
        LOAD_AVERAGE("系统负载", ""),
        SWAP("交换空间使用率", "%");
        
        private final String displayName;
        private final String unit;
        
        ResourceType(String displayName, String unit) {
            this.displayName = displayName;
            this.unit = unit;
        }
        
        public String getDisplayName() { return displayName; }
        public String getUnit() { return unit; }
    }
    
    // 阈值级别枚举
    public enum ThresholdLevel {
        NORMAL("正常", "success"),
        WARNING("警告", "warning"), 
        CRITICAL("严重", "danger"),
        DANGER("危险", "danger");
        
        private final String displayName;
        private final String colorScheme;
        
        ThresholdLevel(String displayName, String colorScheme) {
            this.displayName = displayName;
            this.colorScheme = colorScheme;
        }
        
        public String getDisplayName() { return displayName; }
        public String getColorScheme() { return colorScheme; }
    }

    // 构造函数
    public ServerResourceThreshold() {}

    public ServerResourceThreshold(Long serverId, ResourceType resourceType) {
        this.serverId = serverId;
        this.resourceType = resourceType;
        setDefaultThresholds(resourceType);
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 业务方法
    
    /**
     * 判断当前值的严重程度
     */
    public ThresholdLevel evaluateLevel(Double currentValue) {
        if (currentValue == null) {
            return ThresholdLevel.NORMAL;
        }
        
        if (dangerThreshold != null && currentValue >= dangerThreshold) {
            return ThresholdLevel.DANGER;
        }
        if (criticalThreshold != null && currentValue >= criticalThreshold) {
            return ThresholdLevel.CRITICAL;
        }
        if (warningThreshold != null && currentValue >= warningThreshold) {
            return ThresholdLevel.WARNING;
        }
        return ThresholdLevel.NORMAL;
    }

    /**
     * 检查是否需要告警
     */
    public boolean shouldAlert(Double currentValue) {
        return enabled && notifyEnabled && evaluateLevel(currentValue) != ThresholdLevel.NORMAL;
    }

    /**
     * 获取告警消息
     */
    public String getAlertMessage(Double currentValue) {
        if (currentValue == null) {
            return null;
        }
        
        ThresholdLevel level = evaluateLevel(currentValue);
        String resourceName = resourceType.getDisplayName();
        String formattedValue = String.format("%.1f%s", currentValue, resourceType.getUnit());
        
        switch (level) {
            case DANGER:
                return String.format("%s %s 达到危险级别，超过 %.1f%s 阈值", 
                    resourceName, formattedValue, dangerThreshold, resourceType.getUnit());
            case CRITICAL:
                return String.format("%s %s 达到严重级别，超过 %.1f%s 阈值", 
                    resourceName, formattedValue, criticalThreshold, resourceType.getUnit());
            case WARNING:
                return String.format("%s %s 达到警告级别，超过 %.1f%s 阈值", 
                    resourceName, formattedValue, warningThreshold, resourceType.getUnit());
            default:
                return null;
        }
    }

    /**
     * 设置默认阈值
     */
    private void setDefaultThresholds(ResourceType resourceType) {
        switch (resourceType) {
            case MEMORY:
                this.warningThreshold = 70.0;
                this.criticalThreshold = 80.0;  // 关键的80%阈值
                this.dangerThreshold = 90.0;
                this.description = "内存使用率监控，80%以上需要立即处理";
                break;
            case CPU:
                this.warningThreshold = 80.0;
                this.criticalThreshold = 90.0;
                this.dangerThreshold = 95.0;
                this.description = "CPU使用率监控";
                break;
            case DISK:
                this.warningThreshold = 80.0;
                this.criticalThreshold = 90.0;
                this.dangerThreshold = 95.0;
                this.description = "磁盘使用率监控";
                break;
            case LOAD_AVERAGE:
                this.warningThreshold = 2.0;
                this.criticalThreshold = 4.0;
                this.dangerThreshold = 6.0;
                this.description = "系统负载监控";
                break;
            case SWAP:
                this.warningThreshold = 50.0;
                this.criticalThreshold = 70.0;
                this.dangerThreshold = 90.0;
                this.description = "交换空间使用率监控";
                break;
        }
    }

    /**
     * 创建默认的内存阈值配置
     */
    public static ServerResourceThreshold createDefaultMemoryThreshold(Long serverId) {
        ServerResourceThreshold threshold = new ServerResourceThreshold(serverId, ResourceType.MEMORY);
        threshold.setEnabled(true);
        threshold.setNotifyEnabled(true);
        threshold.setCheckIntervalSeconds(30); // 内存检查更频繁
        return threshold;
    }

    /**
     * 验证阈值设置的有效性
     */
    public boolean isValid() {
        if (warningThreshold == null && criticalThreshold == null && dangerThreshold == null) {
            return false;
        }
        
        // 确保阈值递增
        if (warningThreshold != null && criticalThreshold != null && warningThreshold >= criticalThreshold) {
            return false;
        }
        if (criticalThreshold != null && dangerThreshold != null && criticalThreshold >= dangerThreshold) {
            return false;
        }
        if (warningThreshold != null && dangerThreshold != null && warningThreshold >= dangerThreshold) {
            return false;
        }
        
        return true;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }
    
    public ResourceType getResourceType() { return resourceType; }
    public void setResourceType(ResourceType resourceType) { this.resourceType = resourceType; }
    
    public Double getWarningThreshold() { return warningThreshold; }
    public void setWarningThreshold(Double warningThreshold) { this.warningThreshold = warningThreshold; }
    
    public Double getCriticalThreshold() { return criticalThreshold; }
    public void setCriticalThreshold(Double criticalThreshold) { this.criticalThreshold = criticalThreshold; }
    
    public Double getDangerThreshold() { return dangerThreshold; }
    public void setDangerThreshold(Double dangerThreshold) { this.dangerThreshold = dangerThreshold; }
    
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    
    public Boolean getNotifyEnabled() { return notifyEnabled; }
    public void setNotifyEnabled(Boolean notifyEnabled) { this.notifyEnabled = notifyEnabled; }
    
    public Integer getCheckIntervalSeconds() { return checkIntervalSeconds; }
    public void setCheckIntervalSeconds(Integer checkIntervalSeconds) { this.checkIntervalSeconds = checkIntervalSeconds; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "ServerResourceThreshold{" +
                "id=" + id +
                ", serverId=" + serverId +
                ", resourceType=" + resourceType +
                ", warningThreshold=" + warningThreshold +
                ", criticalThreshold=" + criticalThreshold +
                ", dangerThreshold=" + dangerThreshold +
                ", enabled=" + enabled +
                '}';
    }
}