package com.cmict.internalpaas.dto;

import java.util.List;

/**
 * Health Trend DTO
 * Contains health score trend data for multiple servers
 */
public class HealthTrendDto {

    private List<String> timePoints;
    private List<ServerHealthTrend> servers;

    public HealthTrendDto() {
    }

    public HealthTrendDto(List<String> timePoints, List<ServerHealthTrend> servers) {
        this.timePoints = timePoints;
        this.servers = servers;
    }

    public List<String> getTimePoints() {
        return timePoints;
    }

    public void setTimePoints(List<String> timePoints) {
        this.timePoints = timePoints;
    }

    public List<ServerHealthTrend> getServers() {
        return servers;
    }

    public void setServers(List<ServerHealthTrend> servers) {
        this.servers = servers;
    }

    /**
     * Inner class for individual server health trend
     */
    public static class ServerHealthTrend {
        private Long serverId;
        private String serverName;
        private List<Integer> healthScores;

        public ServerHealthTrend() {
        }

        public ServerHealthTrend(Long serverId, String serverName, List<Integer> healthScores) {
            this.serverId = serverId;
            this.serverName = serverName;
            this.healthScores = healthScores;
        }

        public Long getServerId() {
            return serverId;
        }

        public void setServerId(Long serverId) {
            this.serverId = serverId;
        }

        public String getServerName() {
            return serverName;
        }

        public void setServerName(String serverName) {
            this.serverName = serverName;
        }

        public List<Integer> getHealthScores() {
            return healthScores;
        }

        public void setHealthScores(List<Integer> healthScores) {
            this.healthScores = healthScores;
        }
    }
}
