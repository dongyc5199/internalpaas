package com.cmict.internalpaas.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "server_status_snapshots")
public class ServerStatusSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "server_id", nullable = false)
    private Long serverId;
    
    // 连接状态相关
    @Column(name = "is_online")
    private Boolean isOnline;
    
    @Column(name = "connection_response_time")
    private Integer connectionResponseTime; // ms
    
    @Column(name = "last_connection_error")
    private String lastConnectionError;
    
    // 工作目录状态
    @Column(name = "work_directory_exists")
    private Boolean workDirectoryExists;
    
    @Column(name = "work_directory_writable")
    private Boolean workDirectoryWritable;
    
    @Column(name = "work_directory_path")
    private String workDirectoryPath;
    
    // 用户活动状态
    @Column(name = "active_user_count")
    private Integer activeUserCount;
    
    @Column(name = "active_session_count")
    private Integer activeSessionCount;
    
    @Column(name = "total_login_count")
    private Integer totalLoginCount;
    
    // 监控状态
    @Column(name = "monitoring_enabled")
    private Boolean monitoringEnabled;
    
    @Column(name = "last_metrics_update")
    private LocalDateTime lastMetricsUpdate;
    
    @Column(name = "monitoring_status")
    private String monitoringStatus; // RUNNING, STOPPED, ERROR
    
    // 资源状态
    @Column(name = "cpu_usage_percent")
    private Double cpuUsagePercent;
    
    @Column(name = "memory_usage_percent")
    private Double memoryUsagePercent;
    
    @Column(name = "disk_usage_percent")
    private Double diskUsagePercent;
    
    @Column(name = "load_average")
    private Double loadAverage;
    
    @Column(name = "total_memory_mb")
    private Long totalMemoryMb;
    
    @Column(name = "used_memory_mb")
    private Long usedMemoryMb;
    
    @Column(name = "total_disk_gb")
    private Long totalDiskGb;
    
    @Column(name = "used_disk_gb")
    private Long usedDiskGb;
    
    // 权限和安全状态
    @Enumerated(EnumType.STRING)
    @Column(name = "privilege_level")
    private Server.PrivilegeLevel privilegeLevel;
    
    @Column(name = "sudo_available")
    private Boolean sudoAvailable;
    
    @Column(name = "root_access")
    private Boolean rootAccess;
    
    // 时间戳
    @Column(name = "snapshot_time")
    private LocalDateTime snapshotTime;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 构造函数
    public ServerStatusSnapshot() {}

    public ServerStatusSnapshot(Long serverId) {
        this.serverId = serverId;
        this.snapshotTime = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (snapshotTime == null) {
            snapshotTime = LocalDateTime.now();
        }
    }

    // 业务方法
    public boolean hasResourceWarnings() {
        return (memoryUsagePercent != null && memoryUsagePercent >= 70.0) ||
               (cpuUsagePercent != null && cpuUsagePercent >= 80.0) ||
               (diskUsagePercent != null && diskUsagePercent >= 80.0);
    }

    public boolean hasMemoryAlert() {
        return memoryUsagePercent != null && memoryUsagePercent >= 80.0;
    }

    public boolean hasCriticalMemoryAlert() {
        return memoryUsagePercent != null && memoryUsagePercent >= 90.0;
    }

    public boolean isHealthy() {
        return isOnline != null && isOnline && 
               !hasResourceWarnings() && 
               workDirectoryExists != null && workDirectoryExists &&
               workDirectoryWritable != null && workDirectoryWritable;
    }

    public String getResourceStatusSummary() {
        if (!hasResourceWarnings()) {
            return "资源正常";
        }
        
        StringBuilder summary = new StringBuilder();
        if (memoryUsagePercent != null && memoryUsagePercent >= 80.0) {
            summary.append("内存").append(String.format("%.1f%%", memoryUsagePercent));
        }
        if (cpuUsagePercent != null && cpuUsagePercent >= 80.0) {
            if (summary.length() > 0) summary.append(", ");
            summary.append("CPU").append(String.format("%.1f%%", cpuUsagePercent));
        }
        if (diskUsagePercent != null && diskUsagePercent >= 80.0) {
            if (summary.length() > 0) summary.append(", ");
            summary.append("磁盘").append(String.format("%.1f%%", diskUsagePercent));
        }
        
        return summary.length() > 0 ? summary.toString() + "偏高" : "资源正常";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }
    
    public Boolean getIsOnline() { return isOnline; }
    public void setIsOnline(Boolean isOnline) { this.isOnline = isOnline; }
    
    public Integer getConnectionResponseTime() { return connectionResponseTime; }
    public void setConnectionResponseTime(Integer connectionResponseTime) { this.connectionResponseTime = connectionResponseTime; }
    
    public String getLastConnectionError() { return lastConnectionError; }
    public void setLastConnectionError(String lastConnectionError) { this.lastConnectionError = lastConnectionError; }
    
    public Boolean getWorkDirectoryExists() { return workDirectoryExists; }
    public void setWorkDirectoryExists(Boolean workDirectoryExists) { this.workDirectoryExists = workDirectoryExists; }
    
    public Boolean getWorkDirectoryWritable() { return workDirectoryWritable; }
    public void setWorkDirectoryWritable(Boolean workDirectoryWritable) { this.workDirectoryWritable = workDirectoryWritable; }
    
    public String getWorkDirectoryPath() { return workDirectoryPath; }
    public void setWorkDirectoryPath(String workDirectoryPath) { this.workDirectoryPath = workDirectoryPath; }
    
    public Integer getActiveUserCount() { return activeUserCount; }
    public void setActiveUserCount(Integer activeUserCount) { this.activeUserCount = activeUserCount; }
    
    public Integer getActiveSessionCount() { return activeSessionCount; }
    public void setActiveSessionCount(Integer activeSessionCount) { this.activeSessionCount = activeSessionCount; }
    
    public Integer getTotalLoginCount() { return totalLoginCount; }
    public void setTotalLoginCount(Integer totalLoginCount) { this.totalLoginCount = totalLoginCount; }
    
    public Boolean getMonitoringEnabled() { return monitoringEnabled; }
    public void setMonitoringEnabled(Boolean monitoringEnabled) { this.monitoringEnabled = monitoringEnabled; }
    
    public LocalDateTime getLastMetricsUpdate() { return lastMetricsUpdate; }
    public void setLastMetricsUpdate(LocalDateTime lastMetricsUpdate) { this.lastMetricsUpdate = lastMetricsUpdate; }
    
    public String getMonitoringStatus() { return monitoringStatus; }
    public void setMonitoringStatus(String monitoringStatus) { this.monitoringStatus = monitoringStatus; }
    
    public Double getCpuUsagePercent() { return cpuUsagePercent; }
    public void setCpuUsagePercent(Double cpuUsagePercent) { this.cpuUsagePercent = cpuUsagePercent; }
    
    public Double getMemoryUsagePercent() { return memoryUsagePercent; }
    public void setMemoryUsagePercent(Double memoryUsagePercent) { this.memoryUsagePercent = memoryUsagePercent; }
    
    public Double getDiskUsagePercent() { return diskUsagePercent; }
    public void setDiskUsagePercent(Double diskUsagePercent) { this.diskUsagePercent = diskUsagePercent; }
    
    public Double getLoadAverage() { return loadAverage; }
    public void setLoadAverage(Double loadAverage) { this.loadAverage = loadAverage; }
    
    public Long getTotalMemoryMb() { return totalMemoryMb; }
    public void setTotalMemoryMb(Long totalMemoryMb) { this.totalMemoryMb = totalMemoryMb; }
    
    public Long getUsedMemoryMb() { return usedMemoryMb; }
    public void setUsedMemoryMb(Long usedMemoryMb) { this.usedMemoryMb = usedMemoryMb; }
    
    public Long getTotalDiskGb() { return totalDiskGb; }
    public void setTotalDiskGb(Long totalDiskGb) { this.totalDiskGb = totalDiskGb; }
    
    public Long getUsedDiskGb() { return usedDiskGb; }
    public void setUsedDiskGb(Long usedDiskGb) { this.usedDiskGb = usedDiskGb; }
    
    public Server.PrivilegeLevel getPrivilegeLevel() { return privilegeLevel; }
    public void setPrivilegeLevel(Server.PrivilegeLevel privilegeLevel) { this.privilegeLevel = privilegeLevel; }
    
    public Boolean getSudoAvailable() { return sudoAvailable; }
    public void setSudoAvailable(Boolean sudoAvailable) { this.sudoAvailable = sudoAvailable; }
    
    public Boolean getRootAccess() { return rootAccess; }
    public void setRootAccess(Boolean rootAccess) { this.rootAccess = rootAccess; }
    
    public LocalDateTime getSnapshotTime() { return snapshotTime; }
    public void setSnapshotTime(LocalDateTime snapshotTime) { this.snapshotTime = snapshotTime; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "ServerStatusSnapshot{" +
                "id=" + id +
                ", serverId=" + serverId +
                ", isOnline=" + isOnline +
                ", memoryUsagePercent=" + memoryUsagePercent +
                ", cpuUsagePercent=" + cpuUsagePercent +
                ", activeUserCount=" + activeUserCount +
                ", snapshotTime=" + snapshotTime +
                '}';
    }
}