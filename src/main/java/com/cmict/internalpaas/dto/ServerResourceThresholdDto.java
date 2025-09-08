package com.cmict.internalpaas.dto;

import com.cmict.internalpaas.model.ServerResourceThreshold;
import com.cmict.internalpaas.model.ServerResourceThreshold.ResourceType;
import com.cmict.internalpaas.model.ServerResourceThreshold.ThresholdLevel;

import java.time.LocalDateTime;

/**
 * 服务器资源阈值配置DTO
 */
public class ServerResourceThresholdDto {
    
    private Long id;
    private Long serverId;
    private ResourceType resourceType;
    private Double warningThreshold;
    private Double criticalThreshold;
    private Double dangerThreshold;
    private Boolean enabled;
    private Boolean notifyEnabled;
    private Integer checkIntervalSeconds;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 扩展字段
    private String resourceTypeDisplayName;
    private String resourceUnit;
    private boolean isValid;
    private String validationMessage;

    public ServerResourceThresholdDto() {}

    /**
     * 从实体类创建DTO
     */
    public static ServerResourceThresholdDto fromEntity(ServerResourceThreshold threshold) {
        ServerResourceThresholdDto dto = new ServerResourceThresholdDto();
        dto.setId(threshold.getId());
        dto.setServerId(threshold.getServerId());
        dto.setResourceType(threshold.getResourceType());
        dto.setWarningThreshold(threshold.getWarningThreshold());
        dto.setCriticalThreshold(threshold.getCriticalThreshold());
        dto.setDangerThreshold(threshold.getDangerThreshold());
        dto.setEnabled(threshold.getEnabled());
        dto.setNotifyEnabled(threshold.getNotifyEnabled());
        dto.setCheckIntervalSeconds(threshold.getCheckIntervalSeconds());
        dto.setDescription(threshold.getDescription());
        dto.setCreatedAt(threshold.getCreatedAt());
        dto.setUpdatedAt(threshold.getUpdatedAt());
        
        // 设置扩展字段
        dto.setResourceTypeDisplayName(threshold.getResourceType().getDisplayName());
        dto.setResourceUnit(threshold.getResourceType().getUnit());
        dto.setValid(threshold.isValid());
        dto.setValidationMessage(generateValidationMessage(threshold));
        
        return dto;
    }

    /**
     * 转换为实体类
     */
    public ServerResourceThreshold toEntity() {
        ServerResourceThreshold threshold = new ServerResourceThreshold();
        threshold.setId(this.id);
        threshold.setServerId(this.serverId);
        threshold.setResourceType(this.resourceType);
        threshold.setWarningThreshold(this.warningThreshold);
        threshold.setCriticalThreshold(this.criticalThreshold);
        threshold.setDangerThreshold(this.dangerThreshold);
        threshold.setEnabled(this.enabled);
        threshold.setNotifyEnabled(this.notifyEnabled);
        threshold.setCheckIntervalSeconds(this.checkIntervalSeconds);
        threshold.setDescription(this.description);
        return threshold;
    }

    /**
     * 生成验证消息
     */
    private static String generateValidationMessage(ServerResourceThreshold threshold) {
        if (!threshold.isValid()) {
            if (threshold.getWarningThreshold() != null && threshold.getCriticalThreshold() != null &&
                threshold.getWarningThreshold() >= threshold.getCriticalThreshold()) {
                return "警告阈值不能大于或等于严重阈值";
            }
            if (threshold.getCriticalThreshold() != null && threshold.getDangerThreshold() != null &&
                threshold.getCriticalThreshold() >= threshold.getDangerThreshold()) {
                return "严重阈值不能大于或等于危险阈值";
            }
            if (threshold.getWarningThreshold() != null && threshold.getDangerThreshold() != null &&
                threshold.getWarningThreshold() >= threshold.getDangerThreshold()) {
                return "警告阈值不能大于或等于危险阈值";
            }
            return "阈值配置无效";
        }
        return null;
    }

    /**
     * 评估当前值的级别
     */
    public ThresholdLevel evaluateLevel(Double currentValue) {
        ServerResourceThreshold temp = this.toEntity();
        return temp.evaluateLevel(currentValue);
    }

    /**
     * 检查是否应该告警
     */
    public boolean shouldAlert(Double currentValue) {
        ServerResourceThreshold temp = this.toEntity();
        return temp.shouldAlert(currentValue);
    }

    /**
     * 获取告警消息
     */
    public String getAlertMessage(Double currentValue) {
        ServerResourceThreshold temp = this.toEntity();
        return temp.getAlertMessage(currentValue);
    }

    /**
     * 判断是否为内存阈值配置
     */
    public boolean isMemoryThreshold() {
        return resourceType == ResourceType.MEMORY;
    }

    /**
     * 判断是否为80%严重阈值配置
     */
    public boolean isOver80CriticalThreshold() {
        return criticalThreshold != null && criticalThreshold >= 80.0;
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
    
    public String getResourceTypeDisplayName() { return resourceTypeDisplayName; }
    public void setResourceTypeDisplayName(String resourceTypeDisplayName) { this.resourceTypeDisplayName = resourceTypeDisplayName; }
    
    public String getResourceUnit() { return resourceUnit; }
    public void setResourceUnit(String resourceUnit) { this.resourceUnit = resourceUnit; }
    
    public boolean isValid() { return isValid; }
    public void setValid(boolean valid) { isValid = valid; }
    
    public String getValidationMessage() { return validationMessage; }
    public void setValidationMessage(String validationMessage) { this.validationMessage = validationMessage; }
}