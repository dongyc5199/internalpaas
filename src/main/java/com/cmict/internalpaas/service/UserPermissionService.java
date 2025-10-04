package com.cmict.internalpaas.service;

import com.cmict.internalpaas.dto.UserPermissionDto;
import com.cmict.internalpaas.model.Application;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.model.UserActivity;
import com.cmict.internalpaas.repository.ApplicationRepository;
import com.cmict.internalpaas.repository.UserActivityRepository;
import com.cmict.internalpaas.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户权限管理服务
 * User Permission Management Service
 */
@Service
public class UserPermissionService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserActivityRepository userActivityRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    /**
     * 获取活跃用户TOP5
     * @param hours 时间范围(小时)
     * @return 活跃用户排名列表
     */
    public List<UserPermissionDto.ActiveUserRanking> getActiveUsersTop5(int hours) {
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);

        // 获取时间范围内的用户活动
        List<UserActivity> activities = userActivityRepository.findByCreatedAtBetween(startTime, LocalDateTime.now());

        // 按用户名分组统计事件数
        Map<String, Long> userEventCounts = activities.stream()
            .filter(a -> a.getUsername() != null)
            .collect(Collectors.groupingBy(
                UserActivity::getUsername,
                Collectors.counting()
            ));

        // 获取上一周期的数据用于计算趋势
        LocalDateTime previousStartTime = startTime.minusHours(hours);
        List<UserActivity> previousActivities = userActivityRepository.findByCreatedAtBetween(
            previousStartTime, startTime
        );
        Map<String, Long> previousEventCounts = previousActivities.stream()
            .filter(a -> a.getUsername() != null)
            .collect(Collectors.groupingBy(
                UserActivity::getUsername,
                Collectors.counting()
            ));

        // 构建排名列表
        List<UserPermissionDto.ActiveUserRanking> rankings = userEventCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .map(entry -> {
                String username = entry.getKey();
                Long currentCount = entry.getValue();
                Long previousCount = previousEventCounts.getOrDefault(username, 0L);

                User user = userRepository.findByUsername(username).orElse(null);
                if (user == null) return null;

                // 计算趋势百分比
                int trend = 0;
                if (previousCount > 0) {
                    trend = (int) (((double) (currentCount - previousCount) / previousCount) * 100);
                } else if (currentCount > 0) {
                    trend = 100;
                }

                UserPermissionDto.ActiveUserRanking ranking = new UserPermissionDto.ActiveUserRanking();
                ranking.setUserId(user.getId());
                ranking.setUserName(user.getUsername());
                // User使用Set<Role>,取第一个角色
                ranking.setRole(user.getRoles() != null && !user.getRoles().isEmpty()
                    ? user.getRoles().iterator().next().name()
                    : "USER");
                ranking.setEventCount(currentCount.intValue());
                ranking.setTrend(trend);

                return ranking;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        return rankings;
    }

    /**
     * 获取服务器活动关联数据
     * @return 服务器关联数据
     */
    public UserPermissionDto.ServerCorrelationData getServerCorrelation() {
        UserPermissionDto.ServerCorrelationData data = new UserPermissionDto.ServerCorrelationData();

        // 获取所有应用和用户
        List<Application> applications = applicationRepository.findAll();
        List<User> users = userRepository.findAll();

        if (users.isEmpty()) {
            data.setAvgServerPerUser("0");
            data.setTopServerUser("--");
            data.setTopServer("--");
            data.setTopPairs(new ArrayList<>());
            return data;
        }

        // 按用户分组统计服务器数量
        Map<Long, Set<Long>> userServerMap = new HashMap<>();
        applications.forEach(app -> {
            if (app.getUser() != null && app.getUser().getId() != null) {
                Long userId = app.getUser().getId();
                // Application没有serverId字段,暂时跳过服务器统计
                // TODO: 需要在Application中添加serverId字段或通过其他方式关联服务器
                userServerMap.computeIfAbsent(userId, k -> new HashSet<>());
            }
        });

        // 计算平均服务器数/用户
        double avgServers = userServerMap.values().stream()
            .mapToInt(Set::size)
            .average()
            .orElse(0.0);
        data.setAvgServerPerUser(String.format("%.1f", avgServers));

        // 找出访问服务器最多的用户
        Optional<Map.Entry<Long, Set<Long>>> topUserEntry = userServerMap.entrySet().stream()
            .max(Comparator.comparingInt(e -> e.getValue().size()));

        if (topUserEntry.isPresent()) {
            Long topUserId = topUserEntry.get().getKey();
            User topUser = userRepository.findById(topUserId).orElse(null);
            if (topUser != null) {
                data.setTopServerUser(topUser.getUsername() + " (" + topUserEntry.get().getValue().size() + ")");
            }
        }

        // Application没有serverId字段,暂时返回模拟数据
        data.setTopServer("--");

        // 构建TOP5用户-服务器访问组合(暂时返回空列表)
        Map<String, Integer> pairCounts = new HashMap<>();

        List<UserPermissionDto.UserServerPair> topPairs = pairCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .map(entry -> {
                String[] parts = entry.getKey().split("-");
                Long userId = Long.parseLong(parts[0]);
                Long serverId = Long.parseLong(parts[1]);

                User user = userRepository.findById(userId).orElse(null);
                if (user == null) return null;

                UserPermissionDto.UserServerPair pair = new UserPermissionDto.UserServerPair();
                pair.setUserName(user.getUsername());
                pair.setServerName("Server-" + serverId);
                pair.setCount(entry.getValue());

                return pair;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        data.setTopPairs(topPairs);

        return data;
    }

    /**
     * 获取应用参与度数据
     * @return 应用参与度数据
     */
    public UserPermissionDto.AppEngagementData getAppEngagement() {
        UserPermissionDto.AppEngagementData data = new UserPermissionDto.AppEngagementData();

        List<Application> applications = applicationRepository.findAll();
        List<User> users = userRepository.findAll();

        if (users.isEmpty()) {
            data.setAvgAppsPerUser("0");
            data.setTopAppOwner("--");
            data.setAvgHealthScore("0");
            data.setOperations(new ArrayList<>());
            return data;
        }

        // 按用户分组统计应用数量
        Map<Long, Long> userAppCounts = applications.stream()
            .filter(app -> app.getUser() != null && app.getUser().getId() != null)
            .collect(Collectors.groupingBy(
                app -> app.getUser().getId(),
                Collectors.counting()
            ));

        // 计算平均应用数/用户
        double avgApps = userAppCounts.values().stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
        data.setAvgAppsPerUser(String.format("%.1f", avgApps));

        // 找出应用最多的用户
        Optional<Map.Entry<Long, Long>> topAppOwnerEntry = userAppCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue());

        if (topAppOwnerEntry.isPresent()) {
            Map.Entry<Long, Long> entry = topAppOwnerEntry.get();
            Long topUserId = entry.getKey();
            User topUser = userRepository.findById(topUserId).orElse(null);
            if (topUser != null) {
                data.setTopAppOwner(topUser.getUsername() + " (" + entry.getValue() + ")");
            }
        }

        // 计算平均健康度(简化版,基于应用状态)
        long runningApps = applications.stream()
            .filter(app -> "running".equalsIgnoreCase(app.getStatus()))
            .count();
        double healthScore = applications.isEmpty() ? 0 : (runningApps * 100.0 / applications.size());
        data.setAvgHealthScore(String.format("%.0f%%", healthScore));

        // 统计应用操作分布
        Map<String, Long> statusCounts = applications.stream()
            .collect(Collectors.groupingBy(
                app -> app.getStatus() != null ? app.getStatus() : "unknown",
                Collectors.counting()
            ));

        List<UserPermissionDto.OperationItem> operations = statusCounts.entrySet().stream()
            .map(entry -> {
                UserPermissionDto.OperationItem item = new UserPermissionDto.OperationItem();
                item.setLabel(getStatusLabel(entry.getKey()));
                item.setValue(entry.getValue().intValue());
                return item;
            })
            .collect(Collectors.toList());

        data.setOperations(operations);

        return data;
    }

    /**
     * 获取用户列表
     * @return 用户列表
     */
    public List<UserPermissionDto.UserListItem> getUserList() {
        List<User> users = userRepository.findAll();

        return users.stream().map(user -> {
            UserPermissionDto.UserListItem item = new UserPermissionDto.UserListItem();
            item.setId(user.getId());
            item.setName(user.getUsername());
            item.setEmail(user.getEmail());
            // User使用Set<Role>,取第一个角色
            item.setRole(user.getRoles() != null && !user.getRoles().isEmpty()
                ? user.getRoles().iterator().next().name()
                : "USER");
            item.setAvatar(user.getAvatarUrl()); // 从User实体获取头像

            // 计算用户状态(简化版)
            LocalDateTime lastActive = getLastActiveTime(user.getId());
            if (lastActive != null) {
                long minutesSinceActive = java.time.Duration.between(
                    lastActive, LocalDateTime.now()
                ).toMinutes();

                if (minutesSinceActive < 5) {
                    item.setStatus("online");
                } else if (minutesSinceActive < 30) {
                    item.setStatus("idle");
                } else {
                    item.setStatus("offline");
                }

                item.setLastActive(lastActive.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            } else {
                item.setStatus("offline");
            }

            // 计算活跃度分数
            int activityScore = calculateActivityScore(user.getId());
            item.setActivityScore(activityScore);

            // 统计服务器和应用数量
            List<Application> userApps = applicationRepository.findByUser(user);
            // Application没有serverId,暂时使用0
            long serverCount = 0;
            long appCount = userApps.size();

            item.setServerCount((int) serverCount);
            item.setAppCount((int) appCount);

            return item;
        }).collect(Collectors.toList());
    }

    /**
     * 获取用户详情
     * @param userId 用户ID
     * @return 用户详情
     */
    public Map<String, Object> getUserDetails(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> details = new HashMap<>();
        details.put("id", user.getId());
        details.put("username", user.getUsername());
        details.put("email", user.getEmail());
        details.put("role", user.getRoles() != null && !user.getRoles().isEmpty()
            ? user.getRoles().iterator().next().name()
            : "USER");
        details.put("createdAt", user.getCreatedAt());

        // 添加统计信息
        List<Application> apps = applicationRepository.findByUser(user);
        details.put("totalApps", apps.size());
        details.put("runningApps", apps.stream().filter(a -> "RUNNING".equalsIgnoreCase(a.getStatus())).count());

        LocalDateTime lastActive = getLastActiveTime(userId);
        details.put("lastActive", lastActive);

        return details;
    }

    /**
     * 获取用户最后活跃时间
     * @param userId 用户ID
     * @return 最后活跃时间
     */
    private LocalDateTime getLastActiveTime(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return null;

        return userActivityRepository.findLastActivityByUsername(user.getUsername());
    }

    /**
     * 计算用户活跃度分数
     * @param userId 用户ID
     * @return 活跃度分数(0-100)
     */
    private int calculateActivityScore(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return 0;

        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        long recentActivities = userActivityRepository.countByUsernameAndCreatedAtBetween(
            user.getUsername(), oneWeekAgo, LocalDateTime.now()
        );

        // 简化的分数计算: 每10个活动记录得10分,最高100分
        int score = (int) Math.min(100, (recentActivities / 10) * 10);
        return score;
    }

    /**
     * 获取状态标签
     * @param status 状态
     * @return 标签
     */
    private String getStatusLabel(String status) {
        switch (status.toLowerCase()) {
            case "running": return "运行中";
            case "stopped": return "已停止";
            case "error": return "错误";
            case "starting": return "启动中";
            case "stopping": return "停止中";
            default: return "未知";
        }
    }
}
