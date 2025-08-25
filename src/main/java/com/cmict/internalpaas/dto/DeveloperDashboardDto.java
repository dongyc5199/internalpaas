package com.cmict.internalpaas.dto;

import java.util.List;

public class DeveloperDashboardDto {
    // 应用状态统计
    private long runningApplications;
    private long stoppedApplications;
    private long totalApplications;
    
    // 调试会话统计
    private long activeDebugSessions;
    private long totalDebugSessions;
    
    // 资源使用情况
    private double cpuUsage;
    private double memoryUsage;
    
    // 最近调试记录
    private List<DebugRecord> recentDebugRecords;
    
    // Getters and Setters
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
    
    public long getTotalApplications() {
        return totalApplications;
    }
    
    public void setTotalApplications(long totalApplications) {
        this.totalApplications = totalApplications;
    }
    
    public long getActiveDebugSessions() {
        return activeDebugSessions;
    }
    
    public void setActiveDebugSessions(long activeDebugSessions) {
        this.activeDebugSessions = activeDebugSessions;
    }
    
    public long getTotalDebugSessions() {
        return totalDebugSessions;
    }
    
    public void setTotalDebugSessions(long totalDebugSessions) {
        this.totalDebugSessions = totalDebugSessions;
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
    
    public List<DebugRecord> getRecentDebugRecords() {
        return recentDebugRecords;
    }
    
    public void setRecentDebugRecords(List<DebugRecord> recentDebugRecords) {
        this.recentDebugRecords = recentDebugRecords;
    }
    
    // 内部类：调试记录
    public static class DebugRecord {
        private String applicationName;
        private String action;
        private String status;
        private String timestamp;
        
        // Getters and Setters
        public String getApplicationName() {
            return applicationName;
        }
        
        public void setApplicationName(String applicationName) {
            this.applicationName = applicationName;
        }
        
        public String getAction() {
            return action;
        }
        
        public void setAction(String action) {
            this.action = action;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public String getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }
    }
}