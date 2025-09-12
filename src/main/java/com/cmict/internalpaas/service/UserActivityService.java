package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.UserActivity;
import com.cmict.internalpaas.model.SSHSession;
import com.cmict.internalpaas.repository.UserActivityRepository;
import com.cmict.internalpaas.repository.SSHSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserActivityService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserActivityService.class);
    
    @Autowired
    private UserActivityRepository userActivityRepository;
    
    @Autowired
    private SshConnectionService sshConnectionService;
    
    @Autowired
    private SSHSessionRepository sshSessionRepository;
    
    /**
     * 获取指定服务器的活跃用户
     */
    public List<UserActivity> getActiveUsers(Long serverId) {
        return userActivityRepository.findByServerIdAndIsActiveTrueOrderByLastActivityDesc(serverId);
    }
    
    /**
     * 获取指定服务器指定时间范围内的用户活动
     */
    public List<UserActivity> getUserActivitiesInTimeRange(Long serverId, LocalDateTime startTime, LocalDateTime endTime) {
        return userActivityRepository.findByServerIdAndCreatedAtBetweenOrderByCreatedAtDesc(serverId, startTime, endTime);
    }
    
    /**
     * 获取指定用户的活动历史
     */
    public List<UserActivity> getUserHistory(String username) {
        return userActivityRepository.findByUsernameOrderByCreatedAtDesc(username);
    }
    
    /**
     * 记录用户活动
     */
    public UserActivity recordUserActivity(Long serverId, String username, String sessionId, 
                                         UserActivity.ActivityType activityType, String details) {
        UserActivity activity = new UserActivity(serverId, username, sessionId, activityType);
        activity.setActivityDetails(details);
        
        return userActivityRepository.save(activity);
    }
    
    /**
     * 更新用户最后活动时间
     */
    public void updateUserLastActivity(Long serverId, String username) {
        List<UserActivity> activeUsers = userActivityRepository
            .findByServerIdAndUsernameAndIsActiveTrueOrderByLastActivityDesc(serverId, username);
        
        if (!activeUsers.isEmpty()) {
            UserActivity user = activeUsers.get(0);
            user.setLastActivity(LocalDateTime.now());
            userActivityRepository.save(user);
        }
    }
    
    /**
     * 标记用户会话结束
     */
    public void endUserSession(Long serverId, String username, String sessionId) {
        List<UserActivity> activities = userActivityRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);
        
        for (UserActivity activity : activities) {
            if (activity.getIsActive()) {
                activity.setIsActive(false);
                activity.setLogoutTime(LocalDateTime.now());
                activity.setActivityType(UserActivity.ActivityType.LOGOUT);
                userActivityRepository.save(activity);
            }
        }
    }
    
    /**
     * 清理超时的活跃会话
     */
    public void cleanupTimeoutSessions(int timeoutMinutes) {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<UserActivity> timeoutSessions = userActivityRepository.findTimeoutActiveSessions(timeoutThreshold);
        
        for (UserActivity session : timeoutSessions) {
            session.setIsActive(false);
            session.setLogoutTime(LocalDateTime.now());
            session.setActivityDetails("会话超时自动结束");
            userActivityRepository.save(session);
        }
        
        if (!timeoutSessions.isEmpty()) {
            logger.info("清理了{}个超时的用户会话", timeoutSessions.size());
        }
    }
    
    /**
     * 统计指定服务器的活跃用户数
     */
    public long countActiveUsers(Long serverId) {
        return userActivityRepository.countByServerIdAndIsActiveTrue(serverId);
    }
    
    /**
     * 统计指定时间范围内的用户活动数
     */
    public long countActivitiesInTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return userActivityRepository.countByCreatedAtBetween(startTime, endTime);
    }
    
    /**
     * 获取指定服务器最近的用户活动
     */
    public List<UserActivity> getRecentActivities(Long serverId) {
        return userActivityRepository.findTop20ByServerIdOrderByLastActivityDesc(serverId);
    }
    
    /**
     * 统计指定服务器各活动类型的数量
     */
    public Map<UserActivity.ActivityType, Long> getActivityTypeStats(Long serverId, LocalDateTime startTime, LocalDateTime endTime) {
        List<Object[]> results = userActivityRepository.countActivityTypesByServerAndTimeRange(serverId, startTime, endTime);
        
        return results.stream()
            .collect(Collectors.toMap(
                result -> (UserActivity.ActivityType) result[0],
                result -> (Long) result[1]
            ));
    }
    
    /**
     * 获取服务器用户活动摘要
     */
    public UserActivitySummary getUserActivitySummary(Long serverId) {
        UserActivitySummary summary = new UserActivitySummary();
        summary.setServerId(serverId);
        
        // 活跃用户数（基于UserActivity表）
        long activeUserCount = countActiveUsers(serverId);
        summary.setActiveUserCount(activeUserCount);
        
        // SSH会话统计
        long totalSessions = sshSessionRepository.countByServerId(serverId);
        long activeSessions = sshSessionRepository.countByServerIdAndIsActiveTrue(serverId);
        summary.setTotalSessions(totalSessions);
        summary.setActiveSessions(activeSessions);
        
        // 今日活动数
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime todayEnd = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        long todayActivityCount = userActivityRepository.countByServerIdAndCreatedAtBetween(serverId, todayStart, todayEnd);
        summary.setTodayActivityCount(todayActivityCount);
        
        // 总命令数统计（来自SSH会话）
        List<SSHSession> allSessions = sshSessionRepository.findByServerIdOrderByStartTimeDesc(serverId);
        long totalCommands = allSessions.stream()
            .filter(session -> session.getTotalCommands() != null)
            .mapToLong(SSHSession::getTotalCommands)
            .sum();
        summary.setTotalCommands(totalCommands);
        
        // 最近活动
        List<UserActivity> recentActivities = getRecentActivities(serverId);
        summary.setRecentActivities(recentActivities);
        
        // 活动类型统计
        Map<UserActivity.ActivityType, Long> activityTypeStats = getActivityTypeStats(serverId, todayStart, todayEnd);
        summary.setActivityTypeStats(activityTypeStats);
        
        return summary;
    }
    
    /**
     * 获取服务器活跃用户数量
     */
    public int getActiveUserCount(Long serverId) {
        return (int) countActiveUsers(serverId);
    }
    
    /**
     * 获取服务器活跃会话数量
     */
    public int getActiveSessionCount(Long serverId) {
        return (int) sshSessionRepository.countByServerIdAndIsActiveTrue(serverId);
    }
    
    /**
     * 用户活动摘要数据类
     */
    public static class UserActivitySummary {
        private Long serverId;
        private long activeUserCount;
        private long todayActivityCount;
        private long totalSessions;
        private long activeSessions;
        private long totalCommands;
        private List<UserActivity> recentActivities;
        private Map<UserActivity.ActivityType, Long> activityTypeStats;
        
        // Getters and Setters
        public Long getServerId() { return serverId; }
        public void setServerId(Long serverId) { this.serverId = serverId; }
        
        public long getActiveUserCount() { return activeUserCount; }
        public void setActiveUserCount(long activeUserCount) { this.activeUserCount = activeUserCount; }
        
        public long getTodayActivityCount() { return todayActivityCount; }
        public void setTodayActivityCount(long todayActivityCount) { this.todayActivityCount = todayActivityCount; }
        
        public long getTotalSessions() { return totalSessions; }
        public void setTotalSessions(long totalSessions) { this.totalSessions = totalSessions; }
        
        public long getActiveSessions() { return activeSessions; }
        public void setActiveSessions(long activeSessions) { this.activeSessions = activeSessions; }
        
        public long getTotalCommands() { return totalCommands; }
        public void setTotalCommands(long totalCommands) { this.totalCommands = totalCommands; }
        
        public List<UserActivity> getRecentActivities() { return recentActivities; }
        public void setRecentActivities(List<UserActivity> recentActivities) { this.recentActivities = recentActivities; }
        
        public Map<UserActivity.ActivityType, Long> getActivityTypeStats() { return activityTypeStats; }
        public void setActivityTypeStats(Map<UserActivity.ActivityType, Long> activityTypeStats) { this.activityTypeStats = activityTypeStats; }
    }
    
    /**
     * 清理历史活动记录
     */
    public void cleanupOldActivities(int daysToKeep) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysToKeep);
        
        try {
            userActivityRepository.deleteByCreatedAtBefore(cutoffTime);
            logger.info("清理了{}天前的用户活动记录", daysToKeep);
        } catch (Exception e) {
            logger.error("清理历史活动记录失败", e);
        }
    }
    
    /**
     * 根据ID获取用户活动详情
     */
    public UserActivity getUserActivityById(Long id) {
        return userActivityRepository.findByIdWithDetails(id);
    }
    
    /**
     * 复杂条件查询用户活动
     */
    public org.springframework.data.domain.Page<UserActivity> searchUserActivities(
            Long serverId, String username, String sessionId, 
            UserActivity.ActivityType activityType, Boolean isActive, String remoteIp,
            LocalDateTime startTime, LocalDateTime endTime, String terminalType, 
            String keyword, org.springframework.data.domain.Pageable pageable) {
        
        return userActivityRepository.findByComplexConditions(
            serverId, username, sessionId, activityType, isActive, remoteIp,
            startTime, endTime, terminalType, keyword, pageable);
    }
    
    /**
     * 获取全局统计信息
     */
    public Map<String, Object> getGlobalStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> stats = new HashMap<>();
        
        // 总活动数
        stats.put("totalActivities", userActivityRepository.countAllActivities());
        
        // 时间范围内的活动数
        if (startTime != null && endTime != null) {
            stats.put("rangeActivities", userActivityRepository.countByCreatedAtBetween(startTime, endTime));
        }
        
        // 全局活跃用户数
        stats.put("totalActiveUsers", userActivityRepository.countAllActiveUsers());
        
        // 服务器活动分布
        if (startTime != null && endTime != null) {
            List<Object[]> serverStats = userActivityRepository.countActivitiesByServer(startTime, endTime);
            Map<Long, Long> serverActivityMap = serverStats.stream()
                .collect(Collectors.toMap(
                    result -> (Long) result[0],
                    result -> (Long) result[1]
                ));
            stats.put("serverActivityDistribution", serverActivityMap);
        }
        
        // 时间分布统计
        if (startTime != null && endTime != null) {
            List<Object[]> hourStats = userActivityRepository.countActivitiesByHour(startTime, endTime);
            Map<Integer, Long> hourActivityMap = hourStats.stream()
                .collect(Collectors.toMap(
                    result -> (Integer) result[0],
                    result -> (Long) result[1]
                ));
            stats.put("hourlyActivityDistribution", hourActivityMap);
        }
        
        return stats;
    }
    
    /**
     * 获取用户操作风险评估
     */
    public Map<String, Object> getUserRiskAssessment(String username) {
        Map<String, Object> assessment = new HashMap<>();
        
        List<UserActivity> userHistory = getUserHistory(username);
        
        // 计算风险分数
        int riskScore = 0;
        int dangerousOperations = 0;
        
        for (UserActivity activity : userHistory) {
            if (activity.getActivityType() == UserActivity.ActivityType.COMMAND_EXECUTE) {
                riskScore += 2;
                if (activity.getActivityDetails() != null) {
                    String details = activity.getActivityDetails().toLowerCase();
                    if (details.contains("rm") || details.contains("delete") || 
                        details.contains("drop") || details.contains("truncate")) {
                        riskScore += 10;
                        dangerousOperations++;
                    }
                }
            } else if (activity.getActivityType() == UserActivity.ActivityType.FILE_EDIT ||
                      activity.getActivityType() == UserActivity.ActivityType.PROCESS_START ||
                      activity.getActivityType() == UserActivity.ActivityType.PROCESS_STOP) {
                riskScore += 1;
            }
        }
        
        assessment.put("username", username);
        assessment.put("riskScore", riskScore);
        assessment.put("dangerousOperations", dangerousOperations);
        assessment.put("totalOperations", userHistory.size());
        assessment.put("riskLevel", getRiskLevel(riskScore));
        
        // 最近活动时间
        LocalDateTime lastActivity = userActivityRepository.findLastActivityByUsername(username);
        assessment.put("lastActivity", lastActivity);
        
        return assessment;
    }
    
    private String getRiskLevel(int riskScore) {
        if (riskScore >= 50) {
            return "高";
        } else if (riskScore >= 20) {
            return "中";
        } else {
            return "低";
        }
    }
}