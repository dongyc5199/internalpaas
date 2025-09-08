package com.cmict.internalpaas.dto;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerStatusTag.TagType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 服务器状态聚合DTO - 包含服务器的完整状态信息
 */
public class ServerStatusAggregationDto {
    
    private Long serverId;
    private String serverName;
    private String serverHost;
    private Server.ConnectionStatus connectionStatus;
    private List<ServerStatusTagDto> statusTags;
    private LocalDateTime lastUpdated;
    
    // 快捷状态字段
    private boolean isOnline;
    private boolean hasActiveUsers;
    private boolean isMonitoringActive;
    private boolean hasResourceWarnings;
    private boolean hasMemoryAlert;
    private boolean isCriticalMemoryAlert; // 80%以上内存告警
    
    // 统计信息
    private int totalTags;
    private int alertTagCount;
    private int criticalTagCount;
    private int warningTagCount;
    
    // 资源状态摘要
    private String memoryStatus;
    private String cpuStatus;
    private String diskStatus;
    private String connectionStatusDisplay;

    public ServerStatusAggregationDto() {}

    /**
     * 从状态标签列表构建聚合DTO
     */
    public static ServerStatusAggregationDto fromStatusTags(Long serverId, String serverName, String serverHost,
                                                           List<ServerStatusTagDto> statusTags) {
        ServerStatusAggregationDto dto = new ServerStatusAggregationDto();
        dto.setServerId(serverId);
        dto.setServerName(serverName);
        dto.setServerHost(serverHost);
        dto.setStatusTags(statusTags);
        dto.setLastUpdated(LocalDateTime.now());
        
        // 计算快捷状态
        dto.calculateQuickStatuses(statusTags);
        
        // 计算统计信息
        dto.calculateStatistics(statusTags);
        
        // 生成资源状态摘要
        dto.generateResourceSummary(statusTags);
        
        return dto;
    }

    /**
     * 计算快捷状态
     */
    private void calculateQuickStatuses(List<ServerStatusTagDto> statusTags) {
        for (ServerStatusTagDto tag : statusTags) {
            switch (tag.getTagType()) {
                case CONNECTION:
                    this.isOnline = tag.getStatus().name().equals("ACTIVE");
                    this.connectionStatus = this.isOnline ? 
                        Server.ConnectionStatus.CONNECTED : Server.ConnectionStatus.FAILED;
                    this.connectionStatusDisplay = tag.getDisplayText();
                    break;
                    
                case ACTIVE_USERS:
                    this.hasActiveUsers = tag.getStatus().name().equals("ACTIVE");
                    break;
                    
                case MONITORING:
                    this.isMonitoringActive = tag.getStatus().name().equals("ACTIVE");
                    break;
                    
                case MEMORY_USAGE:
                    this.hasMemoryAlert = tag.isAlert();
                    this.isCriticalMemoryAlert = tag.isCriticalAlert();
                    this.memoryStatus = tag.getDisplayText();
                    break;
                    
                case CPU_USAGE:
                    this.cpuStatus = tag.getDisplayText();
                    break;
                    
                case DISK_USAGE:
                    this.diskStatus = tag.getDisplayText();
                    break;
            }
        }
        
        // 判断是否有资源警告
        this.hasResourceWarnings = statusTags.stream()
            .anyMatch(tag -> (tag.getTagType() == TagType.MEMORY_USAGE ||
                            tag.getTagType() == TagType.CPU_USAGE ||
                            tag.getTagType() == TagType.DISK_USAGE) && tag.isAlert());
    }

    /**
     * 计算统计信息
     */
    private void calculateStatistics(List<ServerStatusTagDto> statusTags) {
        this.totalTags = statusTags.size();
        this.alertTagCount = (int) statusTags.stream().filter(ServerStatusTagDto::isAlert).count();
        this.criticalTagCount = (int) statusTags.stream().filter(ServerStatusTagDto::isCriticalAlert).count();
        this.warningTagCount = (int) statusTags.stream()
            .filter(tag -> tag.getStatus().name().equals("WARNING")).count();
    }

    /**
     * 生成资源状态摘要
     */
    private void generateResourceSummary(List<ServerStatusTagDto> statusTags) {
        // 如果没有相应的资源标签，说明资源状态正常
        if (memoryStatus == null) memoryStatus = "正常";
        if (cpuStatus == null) cpuStatus = "正常";
        if (diskStatus == null) diskStatus = "正常";
        if (connectionStatusDisplay == null) connectionStatusDisplay = "未知";
    }

    /**
     * 获取高优先级标签（用于前端展示）
     */
    public List<ServerStatusTagDto> getHighPriorityTags(int minPriority) {
        return statusTags.stream()
            .filter(tag -> tag.getPriority() != null && tag.getPriority() >= minPriority)
            .sorted((a, b) -> Integer.compare(b.getPriority(), a.getPriority()))
            .collect(Collectors.toList());
    }

    /**
     * 获取告警级别标签
     */
    public List<ServerStatusTagDto> getAlertTags() {
        return statusTags.stream()
            .filter(ServerStatusTagDto::isAlert)
            .sorted((a, b) -> Integer.compare(b.getPriority(), a.getPriority()))
            .collect(Collectors.toList());
    }

    /**
     * 获取内存告警标签
     */
    public ServerStatusTagDto getMemoryAlertTag() {
        return statusTags.stream()
            .filter(tag -> tag.getTagType() == TagType.MEMORY_USAGE && tag.isAlert())
            .findFirst()
            .orElse(null);
    }

    /**
     * 获取服务器健康度评分 (0-100)
     */
    public int getHealthScore() {
        int score = 100;
        
        // 连接状态影响
        if (!isOnline) score -= 30;
        
        // 资源告警影响
        if (isCriticalMemoryAlert) score -= 25;
        else if (hasMemoryAlert) score -= 15;
        
        if (criticalTagCount > 0) score -= (criticalTagCount * 10);
        if (warningTagCount > 0) score -= (warningTagCount * 5);
        
        // 监控状态影响
        if (!isMonitoringActive) score -= 5;
        
        return Math.max(0, score);
    }

    /**
     * 获取健康度等级
     */
    public String getHealthLevel() {
        int score = getHealthScore();
        if (score >= 90) return "优秀";
        if (score >= 75) return "良好";
        if (score >= 60) return "一般";
        if (score >= 40) return "较差";
        return "危险";
    }

    /**
     * 判断是否需要立即关注
     */
    public boolean requiresImmediateAttention() {
        return !isOnline || isCriticalMemoryAlert || criticalTagCount > 0;
    }

    // Getters and Setters
    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }
    
    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }
    
    public String getServerHost() { return serverHost; }
    public void setServerHost(String serverHost) { this.serverHost = serverHost; }
    
    public Server.ConnectionStatus getConnectionStatus() { return connectionStatus; }
    public void setConnectionStatus(Server.ConnectionStatus connectionStatus) { this.connectionStatus = connectionStatus; }
    
    public List<ServerStatusTagDto> getStatusTags() { return statusTags; }
    public void setStatusTags(List<ServerStatusTagDto> statusTags) { this.statusTags = statusTags; }
    
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    
    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }
    
    public boolean hasActiveUsers() { return hasActiveUsers; }
    public void setHasActiveUsers(boolean hasActiveUsers) { this.hasActiveUsers = hasActiveUsers; }
    
    public boolean isMonitoringActive() { return isMonitoringActive; }
    public void setMonitoringActive(boolean monitoringActive) { isMonitoringActive = monitoringActive; }
    
    public boolean hasResourceWarnings() { return hasResourceWarnings; }
    public void setHasResourceWarnings(boolean hasResourceWarnings) { this.hasResourceWarnings = hasResourceWarnings; }
    
    public boolean hasMemoryAlert() { return hasMemoryAlert; }
    public void setHasMemoryAlert(boolean hasMemoryAlert) { this.hasMemoryAlert = hasMemoryAlert; }
    
    public boolean isCriticalMemoryAlert() { return isCriticalMemoryAlert; }
    public void setCriticalMemoryAlert(boolean criticalMemoryAlert) { isCriticalMemoryAlert = criticalMemoryAlert; }
    
    public int getTotalTags() { return totalTags; }
    public void setTotalTags(int totalTags) { this.totalTags = totalTags; }
    
    public int getAlertTagCount() { return alertTagCount; }
    public void setAlertTagCount(int alertTagCount) { this.alertTagCount = alertTagCount; }
    
    public int getCriticalTagCount() { return criticalTagCount; }
    public void setCriticalTagCount(int criticalTagCount) { this.criticalTagCount = criticalTagCount; }
    
    public int getWarningTagCount() { return warningTagCount; }
    public void setWarningTagCount(int warningTagCount) { this.warningTagCount = warningTagCount; }
    
    public String getMemoryStatus() { return memoryStatus; }
    public void setMemoryStatus(String memoryStatus) { this.memoryStatus = memoryStatus; }
    
    public String getCpuStatus() { return cpuStatus; }
    public void setCpuStatus(String cpuStatus) { this.cpuStatus = cpuStatus; }
    
    public String getDiskStatus() { return diskStatus; }
    public void setDiskStatus(String diskStatus) { this.diskStatus = diskStatus; }
    
    public String getConnectionStatusDisplay() { return connectionStatusDisplay; }
    public void setConnectionStatusDisplay(String connectionStatusDisplay) { this.connectionStatusDisplay = connectionStatusDisplay; }
}