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
    
    private ServerStats serverStats = new ServerStats();
    private UserStats userStats = new UserStats();
    private AlertStats alertStats = new AlertStats();
    private HealthStats health = new HealthStats();

    private long todayActiveUsers;
    private long yesterdayActiveUsers;
    private long threeDaysAgoActiveUsers;
    private Double userDelta;
    private Double conversionRate;
    private String usersUpdatedAt;

    private long unresolvedAlerts;
    private String alertSlaStatus;
    private String alertsUpdatedAt;

    private String serverUpdatedAt;
    private String healthUpdatedAt;
    private String systemHealthSummary;


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
    
    public ServerStats getServerStats() {
        return serverStats;
    }

    public void setServerStats(ServerStats serverStats) {
        this.serverStats = serverStats;
    }

    public UserStats getUserStats() {
        return userStats;
    }

    public void setUserStats(UserStats userStats) {
        this.userStats = userStats;
    }

    public AlertStats getAlertStats() {
        return alertStats;
    }

    public void setAlertStats(AlertStats alertStats) {
        this.alertStats = alertStats;
    }

    public HealthStats getHealth() {
        return health;
    }

    public void setHealth(HealthStats health) {
        this.health = health;
    }

    public long getTodayActiveUsers() {
        return todayActiveUsers;
    }

    public void setTodayActiveUsers(long todayActiveUsers) {
        this.todayActiveUsers = todayActiveUsers;
    }

    public long getYesterdayActiveUsers() {
        return yesterdayActiveUsers;
    }

    public void setYesterdayActiveUsers(long yesterdayActiveUsers) {
        this.yesterdayActiveUsers = yesterdayActiveUsers;
    }

    public long getThreeDaysAgoActiveUsers() {
        return threeDaysAgoActiveUsers;
    }

    public void setThreeDaysAgoActiveUsers(long threeDaysAgoActiveUsers) {
        this.threeDaysAgoActiveUsers = threeDaysAgoActiveUsers;
    }

    public Double getUserDelta() {
        return userDelta;
    }

    public void setUserDelta(Double userDelta) {
        this.userDelta = userDelta;
    }

    public Double getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(Double conversionRate) {
        this.conversionRate = conversionRate;
    }

    public String getUsersUpdatedAt() {
        return usersUpdatedAt;
    }

    public void setUsersUpdatedAt(String usersUpdatedAt) {
        this.usersUpdatedAt = usersUpdatedAt;
    }

    public long getUnresolvedAlerts() {
        return unresolvedAlerts;
    }

    public void setUnresolvedAlerts(long unresolvedAlerts) {
        this.unresolvedAlerts = unresolvedAlerts;
    }

    public String getAlertSlaStatus() {
        return alertSlaStatus;
    }

    public void setAlertSlaStatus(String alertSlaStatus) {
        this.alertSlaStatus = alertSlaStatus;
    }

    public String getAlertsUpdatedAt() {
        return alertsUpdatedAt;
    }

    public void setAlertsUpdatedAt(String alertsUpdatedAt) {
        this.alertsUpdatedAt = alertsUpdatedAt;
    }

    public String getServerUpdatedAt() {
        return serverUpdatedAt;
    }

    public void setServerUpdatedAt(String serverUpdatedAt) {
        this.serverUpdatedAt = serverUpdatedAt;
    }

    public String getHealthUpdatedAt() {
        return healthUpdatedAt;
    }

    public void setHealthUpdatedAt(String healthUpdatedAt) {
        this.healthUpdatedAt = healthUpdatedAt;
    }

    public String getSystemHealthSummary() {
        return systemHealthSummary;
    }

    public void setSystemHealthSummary(String systemHealthSummary) {
        this.systemHealthSummary = systemHealthSummary;
    }

    public static class ServerStats {
        private long total;
        private long online;
        private long offline;
        private long maintaining;
        private double onlineRate;
        private String updatedAt;

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public long getOnline() {
            return online;
        }

        public void setOnline(long online) {
            this.online = online;
        }

        public long getOffline() {
            return offline;
        }

        public void setOffline(long offline) {
            this.offline = offline;
        }

        public long getMaintaining() {
            return maintaining;
        }

        public void setMaintaining(long maintaining) {
            this.maintaining = maintaining;
        }

        public double getOnlineRate() {
            return onlineRate;
        }

        public void setOnlineRate(double onlineRate) {
            this.onlineRate = onlineRate;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
        }
    }

    public static class UserStats {
        private long activeToday;
        private long activeYesterday;
        private long activeThreeDaysAgo;
        private long total;
        private Double delta;
        private Double conversionRate;
        private String updatedAt;

        public long getActiveToday() {
            return activeToday;
        }

        public void setActiveToday(long activeToday) {
            this.activeToday = activeToday;
        }

        public long getActiveYesterday() {
            return activeYesterday;
        }

        public void setActiveYesterday(long activeYesterday) {
            this.activeYesterday = activeYesterday;
        }

        public long getActiveThreeDaysAgo() {
            return activeThreeDaysAgo;
        }

        public void setActiveThreeDaysAgo(long activeThreeDaysAgo) {
            this.activeThreeDaysAgo = activeThreeDaysAgo;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public Double getDelta() {
            return delta;
        }

        public void setDelta(Double delta) {
            this.delta = delta;
        }

        public Double getConversionRate() {
            return conversionRate;
        }

        public void setConversionRate(Double conversionRate) {
            this.conversionRate = conversionRate;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
        }
    }

    public static class AlertStats {
        private long unresolved;
        private long critical;
        private long warning;
        private long rules;
        private Double delta;
        private String slaStatus;
        private String updatedAt;

        public long getUnresolved() {
            return unresolved;
        }

        public void setUnresolved(long unresolved) {
            this.unresolved = unresolved;
        }

        public long getCritical() {
            return critical;
        }

        public void setCritical(long critical) {
            this.critical = critical;
        }

        public long getWarning() {
            return warning;
        }

        public void setWarning(long warning) {
            this.warning = warning;
        }

        public long getRules() {
            return rules;
        }

        public void setRules(long rules) {
            this.rules = rules;
        }

        public Double getDelta() {
            return delta;
        }

        public void setDelta(Double delta) {
            this.delta = delta;
        }

        public String getSlaStatus() {
            return slaStatus;
        }

        public void setSlaStatus(String slaStatus) {
            this.slaStatus = slaStatus;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
        }
    }

    public static class HealthStats {
        private double score;
        private String status;
        private String summary;
        private String updatedAt;

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
        }
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