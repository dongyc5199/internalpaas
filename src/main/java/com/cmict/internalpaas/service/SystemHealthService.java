package com.cmict.internalpaas.service;

import com.cmict.internalpaas.dto.AggregatedServerMetrics;
import com.cmict.internalpaas.model.AlertThreshold;
import com.cmict.internalpaas.model.Application;
import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerMetrics;
import com.cmict.internalpaas.repository.AlertThresholdRepository;
import com.cmict.internalpaas.repository.ApplicationRepository;
import com.cmict.internalpaas.repository.ServerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 系统健康度计算服务
 * 根据服务器状态、应用状态、告警情况计算整体系统健康度
 */
@Service
public class SystemHealthService {

    @Autowired
    private ServerService serverService;

    @Autowired
    private MonitoringService monitoringService;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private AlertThresholdRepository alertThresholdRepository;

    @Autowired
    private ServerRepository serverRepository;

    /**
     * 计算系统整体健康度评分 (0-100分)
     * 评分规则:
     * - 服务器健康度: 40% 权重
     * - 应用健康度: 30% 权重
     * - 告警状态: 20% 权重
     * - 系统资源: 10% 权重
     */
    public double calculateSystemHealthScore() {
        try {
            double serverHealthScore = calculateServerHealthScore() * 0.4;
            double applicationHealthScore = calculateApplicationHealthScore() * 0.3;
            double alertHealthScore = calculateAlertHealthScore() * 0.2;
            double resourceHealthScore = calculateResourceHealthScore() * 0.1;

            return Math.max(0, Math.min(100, serverHealthScore + applicationHealthScore + alertHealthScore + resourceHealthScore));
        } catch (Exception e) {
            // 发生异常时返回中等健康度
            return 50.0;
        }
    }

    /**
     * 获取系统健康度状态描述
     */
    public String getSystemHealthStatus(double healthScore) {
        if (healthScore >= 90) {
            return "excellent"; // 优秀
        } else if (healthScore >= 75) {
            return "good"; // 良好
        } else if (healthScore >= 60) {
            return "warning"; // 警告
        } else {
            return "critical"; // 严重
        }
    }

    /**
     * 计算服务器健康度 (0-100)
     */
    private double calculateServerHealthScore() {
        try {
            List<Server> servers = serverService.getAllServers();
            if (servers.isEmpty()) {
                return 100.0; // 无服务器时认为健康
            }

            int totalServers = servers.size();
            int healthyServers = 0;

            for (Server server : servers) {
                try {
                    // 获取服务器最新监控指标
                    ServerMetrics metrics = null;
                    try {
                        // 尝试获取监控数据来判断服务器是否正常
                        metrics = monitoringService.getServerMetrics(server);
                    } catch (Exception e) {
                        continue; // 无法获取监控数据的服务器不计入健康
                    }
                    if (metrics == null) {
                        continue; // 无监控数据的服务器不计入健康
                    }

                    // 根据资源使用情况判断服务器健康度
                    if (isServerHealthy(metrics)) {
                        healthyServers++;
                    }
                } catch (Exception e) {
                    // 个别服务器检查失败，跳过
                    continue;
                }
            }

            return (double) healthyServers / totalServers * 100;
        } catch (Exception e) {
            return 50.0; // 异常时返回中等分数
        }
    }

    /**
     * 计算应用健康度 (0-100)
     */
    private double calculateApplicationHealthScore() {
        try {
            List<Application> applications = applicationRepository.findAll();
            if (applications.isEmpty()) {
                return 100.0; // 无应用时认为健康
            }

            int totalApplications = applications.size();
            int healthyApplications = 0;

            for (Application app : applications) {
                // 运行中的应用认为健康
                if ("RUNNING".equals(app.getStatus())) {
                    healthyApplications++;
                }
            }

            return (double) healthyApplications / totalApplications * 100;
        } catch (Exception e) {
            return 50.0; // 异常时返回中等分数
        }
    }

    /**
     * 计算告警健康度 (0-100)
     * 告警越少，健康度越高
     */
    private double calculateAlertHealthScore() {
        try {
            // 获取所有活跃的告警阈值
            List<AlertThreshold> activeThresholds = alertThresholdRepository.findByEnabledTrueOrderByServerIdAscMetricTypeAsc();
            if (activeThresholds.isEmpty()) {
                return 100.0; // 无告警配置时认为健康
            }

            int totalThresholds = activeThresholds.size();
            int triggeredAlerts = 0;

            // 检查每个阈值是否被触发
            for (AlertThreshold threshold : activeThresholds) {
                if (isAlertTriggered(threshold)) {
                    triggeredAlerts++;
                }
            }

            // 告警比例越低，健康度越高
            double alertRatio = (double) triggeredAlerts / totalThresholds;
            return Math.max(0, 100 - (alertRatio * 100));
        } catch (Exception e) {
            return 70.0; // 异常时返回较好分数
        }
    }

    /**
     * 计算系统资源健康度 (0-100)
     * 基于所有服务器的平均资源使用率
     */
    private double calculateResourceHealthScore() {
        try {
            AggregatedServerMetrics aggregatedMetrics = calculateAggregatedMetrics();
            if (aggregatedMetrics == null) {
                return 50.0; // 无数据时返回中等分数
            }

            double avgCpu = aggregatedMetrics.getAvgCpuUsage() != null ? aggregatedMetrics.getAvgCpuUsage() : 0.0;
            double avgMemory = aggregatedMetrics.getAvgMemoryUsage() != null ? aggregatedMetrics.getAvgMemoryUsage() : 0.0;
            double avgDisk = aggregatedMetrics.getAvgDiskUsage() != null ? aggregatedMetrics.getAvgDiskUsage() : 0.0;

            // 资源使用率越低，健康度越高
            double cpuScore = Math.max(0, 100 - avgCpu);
            double memoryScore = Math.max(0, 100 - avgMemory);
            double diskScore = Math.max(0, 100 - avgDisk);

            return (cpuScore + memoryScore + diskScore) / 3;
        } catch (Exception e) {
            return 50.0; // 异常时返回中等分数
        }
    }

    /**
     * 判断服务器是否健康
     * 基于CPU、内存、磁盘使用率的综合判断
     */
    private boolean isServerHealthy(ServerMetrics metrics) {
        double cpuUsage = metrics.getCpuUsage() != null ? metrics.getCpuUsage() : 0.0;
        double memoryUsage = metrics.getMemoryUsage() != null ? metrics.getMemoryUsage() : 0.0;
        double diskUsage = metrics.getDiskUsage() != null ? metrics.getDiskUsage() : 0.0;

        // 健康标准: CPU < 80%, 内存 < 85%, 磁盘 < 90%
        return cpuUsage < 80.0 && memoryUsage < 85.0 && diskUsage < 90.0;
    }

    /**
     * 检查告警是否被触发
     */
    private boolean isAlertTriggered(AlertThreshold threshold) {
        try {
            Optional<Server> serverOpt = serverService.findById(threshold.getServerId());
            if (!serverOpt.isPresent()) {
                return false;
            }
            Server server = serverOpt.get();
            if (server == null) {
                return false;
            }
            ServerMetrics metrics = monitoringService.getServerMetrics(server);
            if (metrics == null) {
                return false;
            }

            Double value = null;
            switch (threshold.getMetricType()) {
                case CPU:
                    value = metrics.getCpuUsage();
                    break;
                case MEMORY:
                    value = metrics.getMemoryUsage();
                    break;
                case DISK:
                    value = metrics.getDiskUsage();
                    break;
                case LOAD_AVERAGE:
                    value = metrics.getLoadAverageValue();
                    break;
                default:
                    return false;
            }

            return threshold.checkAlert(value) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取系统健康度详细信息
     */
    public SystemHealthDetails getSystemHealthDetails() {
        double totalScore = calculateSystemHealthScore();
        String status = getSystemHealthStatus(totalScore);

        SystemHealthDetails details = new SystemHealthDetails();
        details.setOverallScore(totalScore);
        details.setStatus(status);
        details.setLastCalculated(LocalDateTime.now());

        // 计算各组件分数
        details.setServerHealthScore(calculateServerHealthScore());
        details.setApplicationHealthScore(calculateApplicationHealthScore());
        details.setAlertHealthScore(calculateAlertHealthScore());
        details.setResourceHealthScore(calculateResourceHealthScore());

        return details;
    }

    /**
     * 系统健康度详细信息DTO
     */
    public static class SystemHealthDetails {
        private double overallScore;
        private String status;
        private LocalDateTime lastCalculated;

        // 各组件分数
        private double serverHealthScore;
        private double applicationHealthScore;
        private double alertHealthScore;
        private double resourceHealthScore;

        // Getters and Setters
        public double getOverallScore() { return overallScore; }
        public void setOverallScore(double overallScore) { this.overallScore = overallScore; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public LocalDateTime getLastCalculated() { return lastCalculated; }
        public void setLastCalculated(LocalDateTime lastCalculated) { this.lastCalculated = lastCalculated; }

        public double getServerHealthScore() { return serverHealthScore; }
        public void setServerHealthScore(double serverHealthScore) { this.serverHealthScore = serverHealthScore; }

        public double getApplicationHealthScore() { return applicationHealthScore; }
        public void setApplicationHealthScore(double applicationHealthScore) { this.applicationHealthScore = applicationHealthScore; }

        public double getAlertHealthScore() { return alertHealthScore; }
        public void setAlertHealthScore(double alertHealthScore) { this.alertHealthScore = alertHealthScore; }

        public double getResourceHealthScore() { return resourceHealthScore; }
        public void setResourceHealthScore(double resourceHealthScore) { this.resourceHealthScore = resourceHealthScore; }
    }

    /**
     * 计算聚合的服务器监控指标
     */
    private AggregatedServerMetrics calculateAggregatedMetrics() {
        try {
            List<Server> servers = serverRepository.findAll();
            if (servers.isEmpty()) {
                return null;
            }

            double totalCpu = 0.0;
            double totalMemory = 0.0;
            double totalDisk = 0.0;
            int validCount = 0;

            for (Server server : servers) {
                try {
                    ServerMetrics metrics = monitoringService.getLatestMetrics(server.getId());
                    if (metrics != null) {
                        if (metrics.getCpuUsage() != null) {
                            totalCpu += metrics.getCpuUsage();
                        }
                        if (metrics.getMemoryUsage() != null) {
                            totalMemory += metrics.getMemoryUsage();
                        }
                        if (metrics.getDiskUsage() != null) {
                            totalDisk += metrics.getDiskUsage();
                        }
                        validCount++;
                    }
                } catch (Exception e) {
                    // 个别服务器指标获取失败，继续处理其他服务器
                    continue;
                }
            }

            if (validCount == 0) {
                return null;
            }

            AggregatedServerMetrics aggregated = new AggregatedServerMetrics();
            aggregated.setAvgCpuUsage(totalCpu / validCount);
            aggregated.setAvgMemoryUsage(totalMemory / validCount);
            aggregated.setAvgDiskUsage(totalDisk / validCount);
            aggregated.setDataPointCount((long) validCount);

            return aggregated;
        } catch (Exception e) {
            return null;
        }
    }
}