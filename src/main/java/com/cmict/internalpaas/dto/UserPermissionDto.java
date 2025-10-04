package com.cmict.internalpaas.dto;

import java.util.List;

/**
 * 用户权限管理DTO集合
 * User Permission Management DTOs
 */
public class UserPermissionDto {

    /**
     * 活跃用户排名
     */
    public static class ActiveUserRanking {
        private Long userId;
        private String userName;
        private String role;
        private Integer eventCount;
        private Integer trend; // 百分比变化

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public Integer getEventCount() {
            return eventCount;
        }

        public void setEventCount(Integer eventCount) {
            this.eventCount = eventCount;
        }

        public Integer getTrend() {
            return trend;
        }

        public void setTrend(Integer trend) {
            this.trend = trend;
        }
    }

    /**
     * 服务器关联数据
     */
    public static class ServerCorrelationData {
        private String avgServerPerUser;
        private String topServerUser;
        private String topServer;
        private List<UserServerPair> topPairs;

        public String getAvgServerPerUser() {
            return avgServerPerUser;
        }

        public void setAvgServerPerUser(String avgServerPerUser) {
            this.avgServerPerUser = avgServerPerUser;
        }

        public String getTopServerUser() {
            return topServerUser;
        }

        public void setTopServerUser(String topServerUser) {
            this.topServerUser = topServerUser;
        }

        public String getTopServer() {
            return topServer;
        }

        public void setTopServer(String topServer) {
            this.topServer = topServer;
        }

        public List<UserServerPair> getTopPairs() {
            return topPairs;
        }

        public void setTopPairs(List<UserServerPair> topPairs) {
            this.topPairs = topPairs;
        }
    }

    /**
     * 用户-服务器访问对
     */
    public static class UserServerPair {
        private String userName;
        private String serverName;
        private Integer count;

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getServerName() {
            return serverName;
        }

        public void setServerName(String serverName) {
            this.serverName = serverName;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }
    }

    /**
     * 应用参与度数据
     */
    public static class AppEngagementData {
        private String avgAppsPerUser;
        private String topAppOwner;
        private String avgHealthScore;
        private List<OperationItem> operations;

        public String getAvgAppsPerUser() {
            return avgAppsPerUser;
        }

        public void setAvgAppsPerUser(String avgAppsPerUser) {
            this.avgAppsPerUser = avgAppsPerUser;
        }

        public String getTopAppOwner() {
            return topAppOwner;
        }

        public void setTopAppOwner(String topAppOwner) {
            this.topAppOwner = topAppOwner;
        }

        public String getAvgHealthScore() {
            return avgHealthScore;
        }

        public void setAvgHealthScore(String avgHealthScore) {
            this.avgHealthScore = avgHealthScore;
        }

        public List<OperationItem> getOperations() {
            return operations;
        }

        public void setOperations(List<OperationItem> operations) {
            this.operations = operations;
        }
    }

    /**
     * 操作项(用于图表)
     */
    public static class OperationItem {
        private String label;
        private Integer value;

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }
    }

    /**
     * 用户列表项
     */
    public static class UserListItem {
        private Long id;
        private String name;
        private String email;
        private String avatar;
        private String role;
        private String status; // online, idle, offline
        private Integer activityScore; // 0-100
        private Integer serverCount;
        private Integer appCount;
        private Long lastActive; // timestamp

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getAvatar() {
            return avatar;
        }

        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Integer getActivityScore() {
            return activityScore;
        }

        public void setActivityScore(Integer activityScore) {
            this.activityScore = activityScore;
        }

        public Integer getServerCount() {
            return serverCount;
        }

        public void setServerCount(Integer serverCount) {
            this.serverCount = serverCount;
        }

        public Integer getAppCount() {
            return appCount;
        }

        public void setAppCount(Integer appCount) {
            this.appCount = appCount;
        }

        public Long getLastActive() {
            return lastActive;
        }

        public void setLastActive(Long lastActive) {
            this.lastActive = lastActive;
        }
    }
}
