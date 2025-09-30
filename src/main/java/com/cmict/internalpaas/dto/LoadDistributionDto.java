package com.cmict.internalpaas.dto;

import java.util.List;

/**
 * Load Distribution DTO
 * Contains load distribution data across all servers
 */
public class LoadDistributionDto {

    private List<MetricDistribution> metrics;
    private Statistics statistics;

    public LoadDistributionDto() {
    }

    public LoadDistributionDto(List<MetricDistribution> metrics, Statistics statistics) {
        this.metrics = metrics;
        this.statistics = statistics;
    }

    public List<MetricDistribution> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<MetricDistribution> metrics) {
        this.metrics = metrics;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public void setStatistics(Statistics statistics) {
        this.statistics = statistics;
    }

    /**
     * Metric distribution for a specific type (CPU, Memory, Disk, Network)
     */
    public static class MetricDistribution {
        private String type; // cpu, memory, disk, network
        private String label;
        private Double avgValue;
        private List<ServerMetricValue> servers;

        public MetricDistribution() {
        }

        public MetricDistribution(String type, String label, Double avgValue, List<ServerMetricValue> servers) {
            this.type = type;
            this.label = label;
            this.avgValue = avgValue;
            this.servers = servers;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Double getAvgValue() {
            return avgValue;
        }

        public void setAvgValue(Double avgValue) {
            this.avgValue = avgValue;
        }

        public List<ServerMetricValue> getServers() {
            return servers;
        }

        public void setServers(List<ServerMetricValue> servers) {
            this.servers = servers;
        }
    }

    /**
     * Server metric value
     */
    public static class ServerMetricValue {
        private Long serverId;
        private Double value;
        private String level; // excellent, good, warning, critical

        public ServerMetricValue() {
        }

        public ServerMetricValue(Long serverId, Double value, String level) {
            this.serverId = serverId;
            this.value = value;
            this.level = level;
        }

        public Long getServerId() {
            return serverId;
        }

        public void setServerId(Long serverId) {
            this.serverId = serverId;
        }

        public Double getValue() {
            return value;
        }

        public void setValue(Double value) {
            this.value = value;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }
    }

    /**
     * Statistics summary
     */
    public static class Statistics {
        private Integer excellent;
        private Integer good;
        private Integer warning;
        private Integer critical;

        public Statistics() {
        }

        public Statistics(Integer excellent, Integer good, Integer warning, Integer critical) {
            this.excellent = excellent;
            this.good = good;
            this.warning = warning;
            this.critical = critical;
        }

        public Integer getExcellent() {
            return excellent;
        }

        public void setExcellent(Integer excellent) {
            this.excellent = excellent;
        }

        public Integer getGood() {
            return good;
        }

        public void setGood(Integer good) {
            this.good = good;
        }

        public Integer getWarning() {
            return warning;
        }

        public void setWarning(Integer warning) {
            this.warning = warning;
        }

        public Integer getCritical() {
            return critical;
        }

        public void setCritical(Integer critical) {
            this.critical = critical;
        }
    }
}
