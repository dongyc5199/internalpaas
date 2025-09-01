package com.cmict.internalpaas.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 聚合服务器监控指标数据传输对象
 * 用于时间范围内的数据聚合统计
 */
public class AggregatedServerMetrics {
    
    private Long serverId;
    private String serverName;
    private LocalDateTime timeGroup;
    
    // CPU指标
    private Double avgCpuUsage;
    private Double maxCpuUsage;
    private Double minCpuUsage;
    
    // 内存指标
    private Double avgMemoryUsage;
    private Double maxMemoryUsage;
    private Double minMemoryUsage;
    
    // 磁盘指标
    private Double avgDiskUsage;
    private Double maxDiskUsage;
    private Double minDiskUsage;
    
    // 统计信息
    private Long dataPointCount;
    
    /**
     * 构造函数，用于JPA查询结果映射
     */
    public AggregatedServerMetrics(Long serverId, String serverName, String timeGroupStr,
                                 Double avgCpuUsage, Double maxCpuUsage, Double minCpuUsage,
                                 Double avgMemoryUsage, Double maxMemoryUsage, Double minMemoryUsage,
                                 Double avgDiskUsage, Double maxDiskUsage, Double minDiskUsage,
                                 Long dataPointCount) {
        this.serverId = serverId;
        this.serverName = serverName;
        this.timeGroup = LocalDateTime.parse(timeGroupStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.avgCpuUsage = avgCpuUsage;
        this.maxCpuUsage = maxCpuUsage;
        this.minCpuUsage = minCpuUsage;
        this.avgMemoryUsage = avgMemoryUsage;
        this.maxMemoryUsage = maxMemoryUsage;
        this.minMemoryUsage = minMemoryUsage;
        this.avgDiskUsage = avgDiskUsage;
        this.maxDiskUsage = maxDiskUsage;
        this.minDiskUsage = minDiskUsage;
        this.dataPointCount = dataPointCount;
    }
    
    /**
     * 默认构造函数
     */
    public AggregatedServerMetrics() {}
    
    // Getters and Setters
    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }
    
    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }
    
    public LocalDateTime getTimeGroup() { return timeGroup; }
    public void setTimeGroup(LocalDateTime timeGroup) { this.timeGroup = timeGroup; }
    
    public Double getAvgCpuUsage() { return avgCpuUsage; }
    public void setAvgCpuUsage(Double avgCpuUsage) { this.avgCpuUsage = avgCpuUsage; }
    
    public Double getMaxCpuUsage() { return maxCpuUsage; }
    public void setMaxCpuUsage(Double maxCpuUsage) { this.maxCpuUsage = maxCpuUsage; }
    
    public Double getMinCpuUsage() { return minCpuUsage; }
    public void setMinCpuUsage(Double minCpuUsage) { this.minCpuUsage = minCpuUsage; }
    
    public Double getAvgMemoryUsage() { return avgMemoryUsage; }
    public void setAvgMemoryUsage(Double avgMemoryUsage) { this.avgMemoryUsage = avgMemoryUsage; }
    
    public Double getMaxMemoryUsage() { return maxMemoryUsage; }
    public void setMaxMemoryUsage(Double maxMemoryUsage) { this.maxMemoryUsage = maxMemoryUsage; }
    
    public Double getMinMemoryUsage() { return minMemoryUsage; }
    public void setMinMemoryUsage(Double minMemoryUsage) { this.minMemoryUsage = minMemoryUsage; }
    
    public Double getAvgDiskUsage() { return avgDiskUsage; }
    public void setAvgDiskUsage(Double avgDiskUsage) { this.avgDiskUsage = avgDiskUsage; }
    
    public Double getMaxDiskUsage() { return maxDiskUsage; }
    public void setMaxDiskUsage(Double maxDiskUsage) { this.maxDiskUsage = maxDiskUsage; }
    
    public Double getMinDiskUsage() { return minDiskUsage; }
    public void setMinDiskUsage(Double minDiskUsage) { this.minDiskUsage = minDiskUsage; }
    
    public Long getDataPointCount() { return dataPointCount; }
    public void setDataPointCount(Long dataPointCount) { this.dataPointCount = dataPointCount; }
    
    /**
     * 获取时间组的格式化字符串（用于图表显示）
     */
    public String getTimeGroupFormatted() {
        if (timeGroup == null) return "";
        return timeGroup.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
    }
    
    /**
     * 获取详细的时间组格式化字符串
     */
    public String getTimeGroupDetailFormatted() {
        if (timeGroup == null) return "";
        return timeGroup.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    /**
     * 获取CPU使用率的变化范围
     */
    public Double getCpuUsageRange() {
        if (maxCpuUsage == null || minCpuUsage == null) return 0.0;
        return maxCpuUsage - minCpuUsage;
    }
    
    /**
     * 获取内存使用率的变化范围
     */
    public Double getMemoryUsageRange() {
        if (maxMemoryUsage == null || minMemoryUsage == null) return 0.0;
        return maxMemoryUsage - minMemoryUsage;
    }
    
    /**
     * 获取磁盘使用率的变化范围
     */
    public Double getDiskUsageRange() {
        if (maxDiskUsage == null || minDiskUsage == null) return 0.0;
        return maxDiskUsage - minDiskUsage;
    }
    
    @Override
    public String toString() {
        return "AggregatedServerMetrics{" +
                "serverId=" + serverId +
                ", serverName='" + serverName + '\'' +
                ", timeGroup=" + timeGroup +
                ", avgCpuUsage=" + avgCpuUsage +
                ", avgMemoryUsage=" + avgMemoryUsage +
                ", avgDiskUsage=" + avgDiskUsage +
                ", dataPointCount=" + dataPointCount +
                '}';
    }
}