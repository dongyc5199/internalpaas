package com.cmict.internalpaas.service;

import com.cmict.internalpaas.dto.*;
import com.cmict.internalpaas.model.Application;
import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerMetrics;
import com.cmict.internalpaas.repository.ApplicationRepository;
import com.cmict.internalpaas.repository.ServerMetricsRepository;
import com.cmict.internalpaas.repository.ServerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ServerGroupService {

    private static final Logger logger = LoggerFactory.getLogger(ServerGroupService.class);

    @Autowired
    private ServerRepository serverRepository;

    @Autowired
    private ServerMetricsRepository metricsRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private MonitoringService monitoringService;

    /**
     * Get enhanced server list with monitoring data and health scores
     */
    public List<ServerGroupViewDto> getServerGroupView() {
        logger.info("Getting server group view");

        List<Server> servers = serverRepository.findAll();
        List<ServerGroupViewDto> result = new ArrayList<>();

        for (Server server : servers) {
            ServerGroupViewDto dto = new ServerGroupViewDto();
            dto.setId(server.getId());
            dto.setName(server.getName());
            dto.setAddress(server.getHostname());
            dto.setPort(server.getPort());

            // Get latest metrics
            ServerMetrics metrics = metricsRepository.findTopByServerIdOrderByTimestampDesc(server.getId()).orElse(null);
            if (metrics != null) {
                dto.setCpuUsage(metrics.getCpuUsage());
                dto.setMemoryUsage(metrics.getMemoryUsage());
                dto.setDiskUsage(metrics.getDiskUsage());
                dto.setUptime(metrics.getUptime());
                dto.setLastUpdateTime(metrics.getTimestamp());

                // Calculate health score
                int healthScore = calculateHealthScore(metrics);
                dto.setHealthScore(healthScore);
            } else {
                dto.setHealthScore(0);
            }

            // Get application count (currently not tracked per server)
            dto.setApps(0);

            // Get user count (placeholder, can be enhanced)
            dto.setUsers(0);

            // Determine status
            String status = determineServerStatus(server, metrics);
            dto.setStatus(status);

            result.add(dto);
        }

        logger.info("Retrieved {} servers for group view", result.size());
        return result;
    }

    /**
     * Get health trend data for the last N hours
     */
    public HealthTrendDto getHealthTrend(int hours) {
        logger.info("Getting health trend for last {} hours", hours);

        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
        List<Server> servers = serverRepository.findAll();

        // Generate time points (one per hour)
        List<String> timePoints = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        for (int i = hours; i >= 0; i--) {
            timePoints.add(LocalDateTime.now().minusHours(i).format(formatter));
        }

        // Get health data for each server
        List<HealthTrendDto.ServerHealthTrend> serverTrends = new ArrayList<>();
        for (Server server : servers) {
            LocalDateTime endTime = LocalDateTime.now();
            List<ServerMetrics> metricsList = metricsRepository.findByServerIdAndTimestampBetweenOrderByTimestampAsc(
                    server.getId(), startTime, endTime);

            List<Integer> healthScores = new ArrayList<>();
            for (String timePoint : timePoints) {
                // Find closest metrics for this time point
                Integer score = findHealthScoreForTimePoint(metricsList, timePoint);
                healthScores.add(score);
            }

            HealthTrendDto.ServerHealthTrend trend = new HealthTrendDto.ServerHealthTrend(
                    server.getId(), server.getName(), healthScores);
            serverTrends.add(trend);
        }

        return new HealthTrendDto(timePoints, serverTrends);
    }

    /**
     * Get load distribution across all servers
     */
    public LoadDistributionDto getLoadDistribution() {
        logger.info("Getting load distribution");

        List<Server> servers = serverRepository.findAll();
        List<LoadDistributionDto.MetricDistribution> metrics = new ArrayList<>();

        // CPU distribution
        metrics.add(createMetricDistribution("cpu", "CPU Average Load", servers));

        // Memory distribution
        metrics.add(createMetricDistribution("memory", "Memory Average Usage", servers));

        // Disk distribution
        metrics.add(createMetricDistribution("disk", "Disk Average Usage", servers));

        // Network distribution (placeholder)
        metrics.add(createMetricDistribution("network", "Network Average Traffic", servers));

        // Calculate statistics
        LoadDistributionDto.Statistics stats = calculateDistributionStatistics(metrics);

        return new LoadDistributionDto(metrics, stats);
    }

    /**
     * Get application distribution across servers
     */
    public AppDistributionDto getAppDistribution() {
        logger.info("Getting application distribution");

        List<Server> servers = serverRepository.findAll();
        List<String> serverNames = servers.stream()
                .map(Server::getName)
                .collect(Collectors.toList());

        // Get application counts by server
        List<AppDistributionDto.AppTypeData> appTypes = new ArrayList<>();

        // For now, return total application count per server
        // Currently applications are not tracked per server, so we set to 0
        List<Integer> appCounts = new ArrayList<>();
        for (Server server : servers) {
            appCounts.add(0);
        }

        appTypes.add(new AppDistributionDto.AppTypeData(
                "total", "Total Applications", appCounts, "#2F9BFF"));

        return new AppDistributionDto(serverNames, appTypes);
    }

    /**
     * Calculate health score based on metrics
     */
    public int calculateHealthScore(ServerMetrics metrics) {
        if (metrics == null) {
            return 0;
        }

        double cpuUsage = metrics.getCpuUsage() != null ? metrics.getCpuUsage() : 0;
        double memoryUsage = metrics.getMemoryUsage() != null ? metrics.getMemoryUsage() : 0;
        double diskUsage = metrics.getDiskUsage() != null ? metrics.getDiskUsage() : 0;

        // Health score = 100 - weighted average of usage
        double score = 100 - (cpuUsage * 0.3 + memoryUsage * 0.3 + diskUsage * 0.2);

        return Math.max(0, Math.min(100, (int) score));
    }

    /**
     * Batch refresh server status
     * Note: Currently simplified implementation
     */
    public void batchRefreshServers(List<Long> serverIds) {
        logger.info("Batch refreshing {} servers", serverIds.size());

        for (Long serverId : serverIds) {
            try {
                Optional<Server> serverOpt = serverRepository.findById(serverId);
                if (serverOpt.isPresent()) {
                    // TODO: Implement actual refresh logic when available
                    logger.debug("Refresh requested for server: {}", serverOpt.get().getName());
                }
            } catch (Exception e) {
                logger.error("Failed to refresh server {}: {}", serverId, e.getMessage());
            }
        }
    }

    // Helper methods

    private String determineServerStatus(Server server, ServerMetrics metrics) {
        if (metrics == null) {
            return "offline";
        }

        int healthScore = calculateHealthScore(metrics);
        if (healthScore >= 75) {
            return "online";
        } else if (healthScore >= 60) {
            return "warning";
        } else {
            return "offline";
        }
    }

    private Integer findHealthScoreForTimePoint(List<ServerMetrics> metricsList, String timePoint) {
        if (metricsList.isEmpty()) {
            return 0;
        }

        // For simplicity, use the first available metrics
        // In production, should find the closest time match
        ServerMetrics metrics = metricsList.get(0);
        return calculateHealthScore(metrics);
    }

    private LoadDistributionDto.MetricDistribution createMetricDistribution(
            String type, String label, List<Server> servers) {

        List<LoadDistributionDto.ServerMetricValue> serverValues = new ArrayList<>();
        double total = 0;
        int count = 0;

        for (Server server : servers) {
            ServerMetrics metrics = metricsRepository.findTopByServerIdOrderByTimestampDesc(server.getId()).orElse(null);
            if (metrics != null) {
                Double value = getMetricValue(type, metrics);
                if (value != null) {
                    String level = determineMetricLevel(value);
                    serverValues.add(new LoadDistributionDto.ServerMetricValue(
                            server.getId(), value, level));
                    total += value;
                    count++;
                }
            }
        }

        double avgValue = count > 0 ? total / count : 0;
        return new LoadDistributionDto.MetricDistribution(type, label, avgValue, serverValues);
    }

    private Double getMetricValue(String type, ServerMetrics metrics) {
        switch (type) {
            case "cpu":
                return metrics.getCpuUsage();
            case "memory":
                return metrics.getMemoryUsage();
            case "disk":
                return metrics.getDiskUsage();
            case "network":
                return 0.0; // Placeholder
            default:
                return 0.0;
        }
    }

    private String determineMetricLevel(Double value) {
        if (value < 50) return "excellent";
        if (value < 70) return "good";
        if (value < 85) return "warning";
        return "critical";
    }

    private LoadDistributionDto.Statistics calculateDistributionStatistics(
            List<LoadDistributionDto.MetricDistribution> metrics) {

        int excellent = 0, good = 0, warning = 0, critical = 0;

        for (LoadDistributionDto.MetricDistribution metric : metrics) {
            for (LoadDistributionDto.ServerMetricValue value : metric.getServers()) {
                switch (value.getLevel()) {
                    case "excellent":
                        excellent++;
                        break;
                    case "good":
                        good++;
                        break;
                    case "warning":
                        warning++;
                        break;
                    case "critical":
                        critical++;
                        break;
                }
            }
        }

        return new LoadDistributionDto.Statistics(excellent, good, warning, critical);
    }
}
