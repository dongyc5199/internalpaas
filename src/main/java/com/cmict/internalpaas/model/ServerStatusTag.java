package com.cmict.internalpaas.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "server_status_tags")
public class ServerStatusTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "server_id", nullable = false)
    private Long serverId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tag_type", nullable = false)
    private TagType tagType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false) 
    private TagStatus status;
    
    @Column(name = "display_text")
    private String displayText;
    
    @Column(name = "tag_value")
    private String value; // 用于存储数值类信息
    
    @Column(name = "color_scheme")
    private String colorScheme; // success, warning, danger, info
    
    @Column(name = "priority")
    private Integer priority = 0; // 显示优先级
    
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    @Column(name = "details", length = 500)
    private String details; // 详细信息
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 标签类型枚举
    public enum TagType {
        CONNECTION("连接状态"),
        WORK_DIRECTORY("工作目录"), 
        ACTIVE_USERS("活跃用户"),
        MONITORING("监控状态"),
        PERMISSION("权限状态"),
        MEMORY_USAGE("内存使用"),
        CPU_USAGE("CPU使用"), 
        DISK_USAGE("磁盘使用"),
        SYSTEM_RESOURCE("系统资源"),
        SYSTEM_LOAD("系统负载"),
        SECURITY("安全状态"),
        MAINTENANCE("维护状态");
        
        private final String description;
        
        TagType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 标签状态枚举
    public enum TagStatus {
        ACTIVE("激活"),
        INACTIVE("未激活"),
        WARNING("警告"),          // 资源使用70-80%
        CRITICAL("严重"),         // 资源使用80-90%  
        DANGER("危险"),           // 资源使用>90%
        ERROR("错误"),
        UNKNOWN("未知"),
        MAINTENANCE("维护中");
        
        private final String description;
        
        TagStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }

    // 构造函数
    public ServerStatusTag() {}

    public ServerStatusTag(Long serverId, TagType tagType, TagStatus status, String displayText) {
        this.serverId = serverId;
        this.tagType = tagType;
        this.status = status;
        this.displayText = displayText;
        this.lastUpdated = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (lastUpdated == null) {
            lastUpdated = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        lastUpdated = LocalDateTime.now();
    }

    // 业务方法
    public boolean isWarning() {
        return status == TagStatus.WARNING;
    }

    public boolean isCritical() {
        return status == TagStatus.CRITICAL;
    }

    public boolean isDanger() {
        return status == TagStatus.DANGER;
    }

    public boolean isError() {
        return status == TagStatus.ERROR;
    }

    public boolean isHealthy() {
        return status == TagStatus.ACTIVE;
    }

    public boolean isAlert() {
        return status == TagStatus.WARNING || 
               status == TagStatus.CRITICAL || 
               status == TagStatus.DANGER || 
               status == TagStatus.ERROR;
    }

    // Builder pattern for fluent creation
    public ServerStatusTag setValue(String value) {
        this.value = value;
        return this;
    }

    public ServerStatusTag setColorScheme(String colorScheme) {
        this.colorScheme = colorScheme;
        return this;
    }

    public ServerStatusTag setPriority(Integer priority) {
        this.priority = priority;
        return this;
    }

    public ServerStatusTag setDetails(String details) {
        this.details = details;
        return this;
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
    public void setValue0(String value) { this.value = value; }
    
    public String getColorScheme() { return colorScheme; }
    public void setColorScheme0(String colorScheme) { this.colorScheme = colorScheme; }
    
    public Integer getPriority() { return priority; }
    public void setPriority0(Integer priority) { this.priority = priority; }
    
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    
    public String getDetails() { return details; }
    public void setDetails0(String details) { this.details = details; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "ServerStatusTag{" +
                "id=" + id +
                ", serverId=" + serverId +
                ", tagType=" + tagType +
                ", status=" + status +
                ", displayText='" + displayText + '\'' +
                ", priority=" + priority +
                '}';
    }
}