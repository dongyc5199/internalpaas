package com.cmict.internalpaas.dto;

import java.util.List;

public class AdminDashboardDto {
    // 服务器状态统计
    private long totalServers;
    private long activeServers;
    private long inactiveServers;
    private long monitoringServers;
    
    // 用户统计
    private long totalUsers;
    private long adminUsers;
    private long regularUsers;
    private long firstLoginUsers;
    
    // 系统监控指标
    private double cpuUsage;
    private double memoryUsage;
    private double diskUsage;
    
    // 应用统计
    private long totalApplications;
    private long runningApplications;
    private long stoppedApplications;
    private long errorApplications;

    // 告警统计
    private long totalAlerts;
    private long criticalAlerts;
    private long warningAlerts;
    private long activeThresholds;

    // 系统健康度
    private double systemHealthScore;
    private String systemHealthStatus;

    // 最近活动记录
    private List<ActivityRecord> recentActivities;
    
    // Getters and Setters
    public long getTotalServers() {
        return totalServers;
    }
    
    public void setTotalServers(long totalServers) {
        this.totalServers = totalServers;
    }
    
    public long getActiveServers() {
        return activeServers;
    }
    
    public void setActiveServers(long activeServers) {
        this.activeServers = activeServers;
    }
    
    public long getInactiveServers() {
        return inactiveServers;
    }
    
    public void setInactiveServers(long inactiveServers) {
        this.inactiveServers = inactiveServers;
    }
    
    public long getMonitoringServers() {
        return monitoringServers;
    }
    
    public void setMonitoringServers(long monitoringServers) {
        this.monitoringServers = monitoringServers;
    }
    
    public long getTotalUsers() {
        return totalUsers;
    }
    
    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }
    
    public long getAdminUsers() {
        return adminUsers;
    }
    
    public void setAdminUsers(long adminUsers) {
        this.adminUsers = adminUsers;
    }
    
    public long getRegularUsers() {
        return regularUsers;
    }
    
    public void setRegularUsers(long regularUsers) {
        this.regularUsers = regularUsers;
    }
    
    public long getFirstLoginUsers() {
        return firstLoginUsers;
    }
    
    public void setFirstLoginUsers(long firstLoginUsers) {
        this.firstLoginUsers = firstLoginUsers;
    }
    
    public double getCpuUsage() {
        return cpuUsage;
    }
    
    public void setCpuUsage(double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }
    
    public double getMemoryUsage() {
        return memoryUsage;
    }
    
    public void setMemoryUsage(double memoryUsage) {
        this.memoryUsage = memoryUsage;
    }
    
    public double getDiskUsage() {
        return diskUsage;
    }
    
    public void setDiskUsage(double diskUsage) {
        this.diskUsage = diskUsage;
    }
    
    public List<ActivityRecord> getRecentActivities() {
        return recentActivities;
    }
    
    public void setRecentActivities(List<ActivityRecord> recentActivities) {
        this.recentActivities = recentActivities;
    }

    // 应用统计的getter和setter方法
    public long getTotalApplications() {
        return totalApplications;
    }

    public void setTotalApplications(long totalApplications) {
        this.totalApplications = totalApplications;
    }

    public long getRunningApplications() {
        return runningApplications;
    }

    public void setRunningApplications(long runningApplications) {
        this.runningApplications = runningApplications;
    }

    public long getStoppedApplications() {
        return stoppedApplications;
    }

    public void setStoppedApplications(long stoppedApplications) {
        this.stoppedApplications = stoppedApplications;
    }

    public long getErrorApplications() {
        return errorApplications;
    }

    public void setErrorApplications(long errorApplications) {
        this.errorApplications = errorApplications;
    }

    // 告警统计的getter和setter方法
    public long getTotalAlerts() {
        return totalAlerts;
    }

    public void setTotalAlerts(long totalAlerts) {
        this.totalAlerts = totalAlerts;
    }

    public long getCriticalAlerts() {
        return criticalAlerts;
    }

    public void setCriticalAlerts(long criticalAlerts) {
        this.criticalAlerts = criticalAlerts;
    }

    public long getWarningAlerts() {
        return warningAlerts;
    }

    public void setWarningAlerts(long warningAlerts) {
        this.warningAlerts = warningAlerts;
    }

    public long getActiveThresholds() {
        return activeThresholds;
    }

    public void setActiveThresholds(long activeThresholds) {
        this.activeThresholds = activeThresholds;
    }

    // 系统健康度的getter和setter方法
    public double getSystemHealthScore() {
        return systemHealthScore;
    }

    public void setSystemHealthScore(double systemHealthScore) {
        this.systemHealthScore = systemHealthScore;
    }

    public String getSystemHealthStatus() {
        return systemHealthStatus;
    }

    public void setSystemHealthStatus(String systemHealthStatus) {
        this.systemHealthStatus = systemHealthStatus;
    }
    
    // 内部类：活动记录
    public static class ActivityRecord {
        private String type;
        private String description;
        private String user;
        private String timestamp;
        
        // Getters and Setters
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public String getUser() {
            return user;
        }
        
        public void setUser(String user) {
            this.user = user;
        }
        
        public String getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }
    }
}