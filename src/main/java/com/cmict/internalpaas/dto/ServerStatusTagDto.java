package com.cmict.internalpaas.dto;

import com.cmict.internalpaas.model.ServerStatusTag;
import com.cmict.internalpaas.model.ServerStatusTag.TagType;
import com.cmict.internalpaas.model.ServerStatusTag.TagStatus;

import java.time.LocalDateTime;

/**
 * 服务器状态标签DTO - 用于API响应
 */
public class ServerStatusTagDto {
    
    private Long id;
    private Long serverId;
    private TagType tagType;
    private TagStatus status;
    private String displayText;
    private String value;
    private String colorScheme;
    private Integer priority;
    private String details;
    private LocalDateTime lastUpdated;
    
    // 扩展字段
    private String tagTypeDisplayName;
    private String statusDisplayName;
    private String cssClass;
    private String iconClass;
    private boolean isAlert;
    private boolean isMemoryAlert;

    public ServerStatusTagDto() {}

    /**
     * 从实体类创建DTO
     */
    public static ServerStatusTagDto fromEntity(ServerStatusTag tag) {
        ServerStatusTagDto dto = new ServerStatusTagDto();
        dto.setId(tag.getId());
        dto.setServerId(tag.getServerId());
        dto.setTagType(tag.getTagType());
        dto.setStatus(tag.getStatus());
        dto.setDisplayText(tag.getDisplayText());
        dto.setValue(tag.getValue());
        dto.setColorScheme(tag.getColorScheme());
        dto.setPriority(tag.getPriority());
        dto.setDetails(tag.getDetails());
        dto.setLastUpdated(tag.getLastUpdated());
        
        // 设置扩展字段
        dto.setTagTypeDisplayName(tag.getTagType().getDescription());
        dto.setStatusDisplayName(tag.getStatus().getDescription());
        dto.setCssClass(generateCssClass(tag));
        dto.setIconClass(generateIconClass(tag));
        dto.setAlert(isAlertLevel(tag.getStatus()));
        dto.setMemoryAlert(tag.getTagType() == TagType.MEMORY_USAGE && isAlertLevel(tag.getStatus()));
        
        return dto;
    }

    /**
     * 生成CSS类名
     */
    private static String generateCssClass(ServerStatusTag tag) {
        StringBuilder cssClass = new StringBuilder("badge");
        
        // 根据颜色方案设置样式
        if (tag.getColorScheme() != null) {
            cssClass.append(" badge-").append(tag.getColorScheme());
        } else {
            // 默认样式
            switch (tag.getStatus()) {
                case ACTIVE:
                    cssClass.append(" badge-success");
                    break;
                case WARNING:
                    cssClass.append(" badge-warning");
                    break;
                case CRITICAL:
                case DANGER:
                case ERROR:
                    cssClass.append(" badge-danger");
                    break;
                case INACTIVE:
                case UNKNOWN:
                    cssClass.append(" badge-secondary");
                    break;
                case MAINTENANCE:
                    cssClass.append(" badge-info");
                    break;
            }
        }
        
        // 添加特殊样式
        if (tag.getTagType() == TagType.MEMORY_USAGE && 
            (tag.getStatus() == TagStatus.CRITICAL || tag.getStatus() == TagStatus.DANGER)) {
            cssClass.append(" memory-alert");
        }
        
        return cssClass.toString();
    }

    /**
     * 生成图标类名
     */
    private static String generateIconClass(ServerStatusTag tag) {
        switch (tag.getTagType()) {
            case CONNECTION:
                return tag.isHealthy() ? "fas fa-wifi" : "fas fa-exclamation-triangle";
            case WORK_DIRECTORY:
                return "fas fa-folder";
            case ACTIVE_USERS:
                return "fas fa-users";
            case MONITORING:
                return "fas fa-chart-line";
            case PERMISSION:
                return "fas fa-key";
            case MEMORY_USAGE:
                return "fas fa-memory";
            case CPU_USAGE:
                return "fas fa-microchip";
            case DISK_USAGE:
                return "fas fa-hdd";
            case SYSTEM_LOAD:
                return "fas fa-tachometer-alt";
            case SECURITY:
                return "fas fa-shield-alt";
            case MAINTENANCE:
                return "fas fa-tools";
            default:
                return "fas fa-info-circle";
        }
    }

    /**
     * 判断是否为告警级别
     */
    private static boolean isAlertLevel(TagStatus status) {
        return status == TagStatus.WARNING || status == TagStatus.CRITICAL || 
               status == TagStatus.DANGER || status == TagStatus.ERROR;
    }

    /**
     * 判断是否为严重告警
     */
    public boolean isCriticalAlert() {
        return status == TagStatus.CRITICAL || status == TagStatus.DANGER;
    }

    /**
     * 判断是否为内存严重告警（80%以上）
     */
    public boolean isCriticalMemoryAlert() {
        return tagType == TagType.MEMORY_USAGE && isCriticalAlert();
    }

    /**
     * 获取数值形式的值（如果是百分比）
     */
    public Double getNumericValue() {
        if (value != null && value.endsWith("%")) {
            try {
                return Double.parseDouble(value.replace("%", ""));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 判断是否为80%以上的内存使用率
     */
    public boolean isOver80PercentMemory() {
        if (tagType == TagType.MEMORY_USAGE) {
            Double numericValue = getNumericValue();
            return numericValue != null && numericValue >= 80.0;
        }
        return false;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }
    
    public TagType getTagType() { return tagType; }
    public void setTagType(TagType tagType) { this.tagType = tagType; }
    
    public TagStatus getStatus() { return status; }
    public void setStatus(TagStatus status) { this.status = status; }
    
    public String getDisplayText() { return displayText; }
    public void setDisplayText(String displayText) { this.displayText = displayText; }
    
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    
    public String getColorScheme() { return colorScheme; }
    public void setColorScheme(String colorScheme) { this.colorScheme = colorScheme; }
    
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    
    public String getTagTypeDisplayName() { return tagTypeDisplayName; }
    public void setTagTypeDisplayName(String tagTypeDisplayName) { this.tagTypeDisplayName = tagTypeDisplayName; }
    
    public String getStatusDisplayName() { return statusDisplayName; }
    public void setStatusDisplayName(String statusDisplayName) { this.statusDisplayName = statusDisplayName; }
    
    public String getCssClass() { return cssClass; }
    public void setCssClass(String cssClass) { this.cssClass = cssClass; }
    
    public String getIconClass() { return iconClass; }
    public void setIconClass(String iconClass) { this.iconClass = iconClass; }
    
    public boolean isAlert() { return isAlert; }
    public void setAlert(boolean alert) { isAlert = alert; }
    
    public boolean isMemoryAlert() { return isMemoryAlert; }
    public void setMemoryAlert(boolean memoryAlert) { isMemoryAlert = memoryAlert; }
}