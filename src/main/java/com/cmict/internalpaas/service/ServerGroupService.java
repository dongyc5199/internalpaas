package com.cmict.internalpaas.service;

import com.cmict.internalpaas.dto.*;
import com.cmict.internalpaas.model.Application;
import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerMetrics;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.repository.ApplicationRepository;
import com.cmict.internalpaas.repository.ServerMetricsRepository;
import com.cmict.internalpaas.repository.ServerRepository;
import com.cmict.internalpaas.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
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
    private UserRepository userRepository;

    @Autowired
    private MonitoringService monitoringService;

    @Autowired
    private ServerService serverService;

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
                dto.setCpuUsage(metrics.getCpuUsagePercent());
                dto.setMemoryUsage(metrics.getMemoryUsagePercent());
                dto.setDiskUsage(metrics.getDiskUsagePercent());
                dto.setUptime(metrics.getUptime());
                dto.setLastUpdateTime(metrics.getTimestamp());

                // Calculate health score
                int healthScore = calculateHealthScore(metrics);
                dto.setHealthScore(healthScore);
            } else {
                dto.setCpuUsage(0.0);
                dto.setMemoryUsage(0.0);
                dto.setDiskUsage(0.0);
                dto.setHealthScore(0);
            }

            // Get application count
            dto.setApps(fetchAppCountForServer(server.getId()));

            // Get user count
            dto.setUsers(fetchUserCountForServer(server.getId()));

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

        // CPU distribution - 返回标识符，由前端国际化
        metrics.add(createMetricDistribution("cpu", "cpu", servers));

        // Memory distribution - 返回标识符，由前端国际化
        metrics.add(createMetricDistribution("memory", "memory", servers));

        // Disk distribution - 返回标识符，由前端国际化
        metrics.add(createMetricDistribution("disk", "disk", servers));

        // Network distribution (placeholder) - 返回标识符，由前端国际化
        //metrics.add(createMetricDistribution("network", "network", servers));

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

        // 返回标识符，由前端国际化
        appTypes.add(new AppDistributionDto.AppTypeData(
                "total", "total", appCounts, "#2F9BFF"));

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
                    Server server = serverOpt.get();
                    logger.debug("Refreshing metrics for server: {}", server.getName());

                    // Collect and save server metrics
                    serverService.checkServerConnectionAndMetrics(serverId);
                    logger.info("Successfully refreshed metrics for server: {}", server.getName());
                }
            } catch (Exception e) {
                logger.error("Failed to refresh server {}: {}", serverId, e.getMessage());
            }
        }
    }

    /**
     * 获取指定服务器的详情信息
     */
    public ServerDetailDto getServerDetail(Long serverId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new IllegalArgumentException("服务器不存在: " + serverId));

        ServerDetailDto dto = new ServerDetailDto();

        // 概览信息
        ServerDetailDto.Overview overview = buildOverview(server);
        dto.setOverview(overview);

        // 指标信息
        ServerDetailDto.Metrics metrics = buildMetrics(serverId);
        dto.setMetrics(metrics);

        // 进程（使用关联应用的运行状态表示）
        dto.setProcesses(buildProcessInfos(serverId));

        // 应用列表
        dto.setApplications(buildApplicationInfos(serverId));

        // 用户授权
        dto.setUsers(buildUserAccessInfos(serverId));

        return dto;
    }

    private ServerDetailDto.Overview buildOverview(Server server) {
        ServerDetailDto.Overview overview = new ServerDetailDto.Overview();
        overview.setId(server.getId());
        overview.setName(server.getName());
        overview.setDescription(server.getDescription());
        overview.setHostname(server.getHostname());
        overview.setPort(server.getPort());
        overview.setConnectionStatus(server.getConnectionStatus());
        overview.setConnectionStatusDescription(
                server.getConnectionStatus() != null ? server.getConnectionStatus().getDescription() : "");
        overview.setAutoMonitorEnabled(server.getAutoMonitorEnabled());
        overview.setMonitorInterval(server.getMonitorIntervalSeconds());

        ServerMetrics latestMetrics = metricsRepository
                .findTopByServerIdOrderByTimestampDesc(server.getId())
                .orElse(null);

        if (latestMetrics != null) {
            overview.setOsVersion(latestMetrics.getOsVersion());
            overview.setUptime(latestMetrics.getUptime());
            overview.setLastMetricsTime(latestMetrics.getTimestamp());
            overview.setHealthScore(calculateHealthScore(latestMetrics));
        }

        List<ServerDetailDto.InfoItem> infoItems = new ArrayList<>();
        infoItems.add(new ServerDetailDto.InfoItem("hostname", "主机名", "Host", server.getHostname()));
        infoItems.add(new ServerDetailDto.InfoItem("port", "服务端口", "Port",
                server.getPort() != null ? String.valueOf(server.getPort()) : "--"));
        infoItems.add(new ServerDetailDto.InfoItem("workdir", "工作目录", "Workdir",
                Optional.ofNullable(server.getBaseWorkDirectory()).orElse("--")));
        infoItems.add(new ServerDetailDto.InfoItem("ssh", "SSH 账户", "SSH Account",
                Optional.ofNullable(server.getSshUsername()).orElse("--")));
        infoItems.add(new ServerDetailDto.InfoItem("sshPort", "SSH 端口", "SSH Port",
                server.getSshPort() != null ? String.valueOf(server.getSshPort()) : "--"));

        if (latestMetrics != null) {
            infoItems.add(new ServerDetailDto.InfoItem("cpu",
                    "CPU 核心",
                    "CPU Cores",
                    latestMetrics.getCpuCores() != null ? latestMetrics.getCpuCores() + "" : "--"));
            infoItems.add(new ServerDetailDto.InfoItem("memory",
                    "内存总量",
                    "Memory",
                    formatBytes(latestMetrics.getMemoryTotal())));
            infoItems.add(new ServerDetailDto.InfoItem("disk",
                    "磁盘容量",
                    "Disk",
                    formatBytes(latestMetrics.getDiskTotal())));
        }

        overview.setInfoItems(infoItems);
        return overview;
    }

    private ServerDetailDto.Metrics buildMetrics(Long serverId) {
        ServerDetailDto.Metrics metrics = new ServerDetailDto.Metrics();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = now.minusHours(3);

        List<ServerMetrics> recentMetrics = metricsRepository.findRecentMetrics(serverId, since);
        recentMetrics.sort(Comparator.comparing(ServerMetrics::getTimestamp));

        ServerDetailDto.MetricSeries cpuSeries = new ServerDetailDto.MetricSeries();
        cpuSeries.setId("cpu");
        cpuSeries.setLabelZh("CPU 使用率");
        cpuSeries.setLabelEn("CPU Usage");
        cpuSeries.setUnit("%");

        ServerDetailDto.MetricSeries memorySeries = new ServerDetailDto.MetricSeries();
        memorySeries.setId("memory");
        memorySeries.setLabelZh("内存使用率");
        memorySeries.setLabelEn("Memory Usage");
        memorySeries.setUnit("%");

        ServerDetailDto.MetricSeries diskSeries = new ServerDetailDto.MetricSeries();
        diskSeries.setId("disk");
        diskSeries.setLabelZh("磁盘使用率");
        diskSeries.setLabelEn("Disk Usage");
        diskSeries.setUnit("%");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        for (ServerMetrics metric : recentMetrics) {
            String timestamp = metric.getTimestamp() != null
                    ? metric.getTimestamp().format(formatter)
                    : now.format(formatter);

            cpuSeries.getPoints().add(new ServerDetailDto.MetricPoint(timestamp,
                    metric.getCpuUsagePercent()));
            memorySeries.getPoints().add(new ServerDetailDto.MetricPoint(timestamp,
                    metric.getMemoryUsagePercent()));
            diskSeries.getPoints().add(new ServerDetailDto.MetricPoint(timestamp,
                    metric.getDiskUsagePercent()));
        }

        metrics.getSeries().add(cpuSeries);
        metrics.getSeries().add(memorySeries);
        metrics.getSeries().add(diskSeries);

        ServerMetrics latest = recentMetrics.isEmpty()
                ? metricsRepository.findTopByServerIdOrderByTimestampDesc(serverId).orElse(null)
                : recentMetrics.get(recentMetrics.size() - 1);

        if (latest != null) {
            metrics.setCurrentCpu(latest.getCpuUsagePercent());
            metrics.setCurrentMemory(latest.getMemoryUsagePercent());
            metrics.setCurrentDisk(latest.getDiskUsagePercent());
        }

        applyMetricsSummaries(metrics, latest);

        return metrics;
    }

    private List<ServerDetailDto.ProcessInfo> buildProcessInfos(Long serverId) {
        List<Application> applications = applicationRepository.findApplicationsBoundToServer(serverId);
        LocalDateTime now = LocalDateTime.now();

        return applications.stream()
                .filter(app -> "RUNNING".equalsIgnoreCase(app.getStatus()))
                .map(app -> {
                    ServerDetailDto.ProcessInfo info = new ServerDetailDto.ProcessInfo();
                    info.setPid(app.getProcessId());
                    info.setName(app.getName());
                    info.setCommand(app.getJarFileName());
                    info.setStatusKey("running");
                    info.setStatusZh("运行中");
                    info.setStatusEn("Running");

                    if (app.getLastStartedAt() != null) {
                        Duration duration = Duration.between(app.getLastStartedAt(), now);
                        info.setUptimeZh(formatDuration(duration));
                        info.setUptimeEn(formatDurationEn(duration));
                    } else {
                        info.setUptimeZh("--");
                        info.setUptimeEn("--");
                    }

                    return info;
                })
                .collect(Collectors.toList());
    }

    private List<ServerDetailDto.ApplicationInfo> buildApplicationInfos(Long serverId) {
        List<Application> applications = applicationRepository.findApplicationsBoundToServer(serverId);

        return applications.stream()
                .map(app -> {
                    ServerDetailDto.ApplicationInfo info = new ServerDetailDto.ApplicationInfo();
                    info.setId(app.getId());
                    info.setName(app.getName());
                    if (app.getUser() != null) {
                        info.setOwner(app.getUser().getUsername());
                        info.setOwnerDisplay(Optional.ofNullable(app.getUser().getFullName())
                                .filter(name -> !name.isBlank())
                                .orElse(app.getUser().getUsername()));
                    }
                    info.setPort(app.getPort());
                    info.setDebugPort(app.getDebugPort());
                    info.setLastStartedAt(app.getLastStartedAt());
                    info.setRemarks(app.getJvmOptions());
                    info.setVersion(app.getJarFileName());

                    String status = Optional.ofNullable(app.getStatus()).orElse("UNKNOWN");
                    info.setStatusKey(status.toLowerCase());
                    switch (status.toUpperCase()) {
                        case "RUNNING":
                            info.setStatusZh("运行中");
                            info.setStatusEn("Running");
                            break;
                        case "STOPPED":
                            info.setStatusZh("已停止");
                            info.setStatusEn("Stopped");
                            break;
                        case "ERROR":
                            info.setStatusZh("异常");
                            info.setStatusEn("Error");
                            break;
                        default:
                            info.setStatusZh("未知");
                            info.setStatusEn("Unknown");
                    }

                    return info;
                })
                .collect(Collectors.toList());
    }

    private List<ServerDetailDto.UserAccessInfo> buildUserAccessInfos(Long serverId) {
        LinkedHashMap<Long, User> userMap = new LinkedHashMap<>();
        userRepository.findByAvailableServersContaining(serverId)
                .forEach(user -> userMap.putIfAbsent(user.getId(), user));
        userRepository.findByDefaultServerId(serverId)
                .forEach(user -> userMap.putIfAbsent(user.getId(), user));

        return userMap.values().stream()
                .map(user -> {
                    ServerDetailDto.UserAccessInfo info = new ServerDetailDto.UserAccessInfo();
                    info.setId(user.getId());
                    info.setUsername(user.getUsername());
                    info.setFullName(Optional.ofNullable(user.getFullName()).orElse(""));
                    info.setEmail(user.getEmail());
                    info.setLastLoginTime(user.getLastLoginTime());

                    // 角色
                    String roleKey = user.getRoles() != null && !user.getRoles().isEmpty()
                            ? user.getRoles().iterator().next().name().toLowerCase()
                            : "user";
                    info.setRoleKey(roleKey);
                    switch (roleKey) {
                        case "super_admin":
                            info.setRoleZh("超级管理员");
                            info.setRoleEn("Super Admin");
                            break;
                        case "admin":
                            info.setRoleZh("管理员");
                            info.setRoleEn("Administrator");
                            break;
                        default:
                            info.setRoleZh("普通用户");
                            info.setRoleEn("User");
                    }

                    boolean isDefault = user.getDefaultServer() != null
                            && Objects.equals(user.getDefaultServer().getId(), serverId);
                    info.setAccessTypeKey(isDefault ? "default" : "authorized");
                    if (isDefault) {
                        info.setAccessTypeZh("默认服务器");
                        info.setAccessTypeEn("Default server");
                    } else {
                        info.setAccessTypeZh("授权访问");
                        info.setAccessTypeEn("Authorized access");
                    }

                    return info;
                })
                .collect(Collectors.toList());
    }

    private void applyMetricsSummaries(ServerDetailDto.Metrics metrics, ServerMetrics latest) {
        if (latest == null) {
            metrics.setCpuSummaryZh("暂无数据");
            metrics.setCpuSummaryEn("No data");
            metrics.setMemorySummaryZh("暂无数据");
            metrics.setMemorySummaryEn("No data");
            metrics.setDiskSummaryZh("暂无数据");
            metrics.setDiskSummaryEn("No data");
            metrics.setNetworkSummaryZh("暂无数据");
            metrics.setNetworkSummaryEn("Not available");
            metrics.setNetworkAvailable(Boolean.FALSE);
            return;
        }

        String[] loadAverage = parseLoadAverage(latest.getLoadAverage());
        if (allUnknown(loadAverage)) {
            metrics.setCpuSummaryZh("暂无数据");
            metrics.setCpuSummaryEn("No data");
        } else {
            metrics.setCpuSummaryZh(String.format("1 分钟负载 %s · 5 分钟 %s · 15 分钟 %s",
                    loadAverage[0], loadAverage[1], loadAverage[2]));
            metrics.setCpuSummaryEn(String.format("1 min load %s · 5 min %s · 15 min %s",
                    loadAverage[0], loadAverage[1], loadAverage[2]));
        }

        String memoryTotal = formatBytes(latest.getMemoryTotal());
        String memoryUsed = formatBytes(latest.getMemoryUsed());
        String memoryAvailable = formatBytes(latest.getMemoryAvailable());
        if (allUnknown(memoryTotal, memoryUsed, memoryAvailable)) {
            metrics.setMemorySummaryZh("暂无数据");
            metrics.setMemorySummaryEn("No data");
        } else {
            metrics.setMemorySummaryZh(String.format("总计 %s · 已用 %s · 可用 %s",
                    memoryTotal, memoryUsed, memoryAvailable));
            metrics.setMemorySummaryEn(String.format("Total %s · Used %s · Free %s",
                    memoryTotal, memoryUsed, memoryAvailable));
        }

        String diskTotal = formatBytes(latest.getDiskTotal());
        String diskUsed = formatBytes(latest.getDiskUsed());
        String diskAvailable = formatBytes(latest.getDiskAvailable());
        if (allUnknown(diskTotal, diskUsed, diskAvailable)) {
            metrics.setDiskSummaryZh("暂无数据");
            metrics.setDiskSummaryEn("No data");
        } else {
            metrics.setDiskSummaryZh(String.format("总计 %s · 已用 %s · 可用 %s",
                    diskTotal, diskUsed, diskAvailable));
            metrics.setDiskSummaryEn(String.format("Total %s · Used %s · Free %s",
                    diskTotal, diskUsed, diskAvailable));
        }

        metrics.setNetworkSummaryZh("暂无数据");
        metrics.setNetworkSummaryEn("Not available");
        metrics.setNetworkAvailable(Boolean.FALSE);
    }

    private String[] parseLoadAverage(String loadAverageValue) {
        String[] result = {"--", "--", "--"};
        if (loadAverageValue == null || loadAverageValue.trim().isEmpty()) {
            return result;
        }

        String[] parts = loadAverageValue.trim().split("\\s+");
        for (int i = 0; i < Math.min(parts.length, 3); i++) {
            try {
                double value = Double.parseDouble(parts[i]);
                result[i] = String.format(Locale.US, "%.2f", value);
            } catch (NumberFormatException ignored) {
                result[i] = "--";
            }
        }
        return result;
    }

    private boolean allUnknown(String... values) {
        if (values == null || values.length == 0) {
            return true;
        }
        for (String value : values) {
            if (value != null && !"--".equals(value.trim())) {
                return false;
            }
        }
        return true;
    }

    private String formatBytes(Long bytes) {
        if (bytes == null || bytes <= 0) {
            return "--";
        }
        double value = bytes.doubleValue();
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int index = 0;
        while (value >= 1024 && index < units.length - 1) {
            value /= 1024;
            index++;
        }
        return String.format(Locale.CHINA, "%.1f %s", value, units[index]);
    }

    private String formatDuration(Duration duration) {
        if (duration == null || duration.isNegative()) {
            return "--";
        }
        long days = duration.toDays();
        long hours = duration.minusDays(days).toHours();
        long minutes = duration.minusDays(days).minusHours(hours).toMinutes();
        if (days > 0) {
            return String.format("%d 天 %02d:%02d", days, hours, minutes);
        }
        return String.format("%02d:%02d", hours, minutes);
    }

    private String formatDurationEn(Duration duration) {
        if (duration == null || duration.isNegative()) {
            return "--";
        }
        long days = duration.toDays();
        long hours = duration.minusDays(days).toHours();
        long minutes = duration.minusDays(days).minusHours(hours).toMinutes();
        if (days > 0) {
            return String.format(Locale.ENGLISH, "%d d %02d:%02d", days, hours, minutes);
        }
        return String.format(Locale.ENGLISH, "%02d:%02d", hours, minutes);
    }

    private int fetchAppCountForServer(Long serverId) {
        if (serverId == null) {
            return 0;
        }

        try {
            long count = applicationRepository.countApplicationsBoundToServer(serverId);
            if (count > Integer.MAX_VALUE) {
                logger.warn("Application count for server {} exceeds int range: {}", serverId, count);
                return Integer.MAX_VALUE;
            }
            return (int) count;
        } catch (Exception ex) {
            logger.warn("Failed to count applications for server {}", serverId, ex);
            return 0;
        }
    }

    private int fetchUserCountForServer(Long serverId) {
        if (serverId == null) {
            return 0;
        }

        try {
            long count = userRepository.countUsersBoundToServer(serverId);
            if (count > Integer.MAX_VALUE) {
                logger.warn("User count for server {} exceeds int range: {}", serverId, count);
                return Integer.MAX_VALUE;
            }
            return (int) count;
        } catch (Exception ex) {
            logger.warn("Failed to count users for server {}", serverId, ex);
            return 0;
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
