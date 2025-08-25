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