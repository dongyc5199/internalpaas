package com.cmict.internalpaas.model;

import java.time.LocalDateTime;

/**
 * 服务器监控指标数据传输对象
 * <p>
 * 注意: 该类不再是JPA实体,仅作为DTO使用。
 * 实际的监控数据存储在Hub模块的TimescaleDB中,该类用于:
 * 1. MetricsHubClient从Hub API获取数据后转换
 * 2. Controller层返回给前端
 * 3. Service层业务逻辑处理
 * </p>
 * 
 * @see com.cmict.internalpaas.client.MetricsHubClient#convertToServerMetrics
 */
public class ServerMetrics {
    private Long id;
    private Long serverId;
    private String serverName;
    private String hostname;
    
    // CPU信息
    private Double cpuUsage;
    private Integer cpuCores;
    private String loadAverage;
    
    // 内存信息
    private Long memoryTotal;
    private Long memoryUsed;
    private Long memoryAvailable;
    private Double memoryUsage;
    
    // 磁盘信息
    private Long diskTotal;
    private Long diskUsed;
    private Long diskAvailable;
    private Double diskUsage;

    // 系统信息
    private String uptime;
    private String osVersion;
    private String kernelVersion;

    // 网络信息
    private String networkInterface;
    private Long networkReceivedBytes;
    private Long networkTransmittedBytes;
    private Double networkReceivedRate;
    private Double networkTransmittedRate;

    private LocalDateTime timestamp;
    private Long collectionDurationMs;
    
    public ServerMetrics() {
        this.timestamp = LocalDateTime.now();
    }
    
    public ServerMetrics(String serverName, String hostname) {
        this();
        this.serverName = serverName;
        this.hostname = hostname;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }
    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }
    
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }
    
    public Double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(Double cpuUsage) { this.cpuUsage = cpuUsage; }
    
    public Double getMemoryUsage() { return memoryUsage; }
    public void setMemoryUsage(Double memoryUsage) { this.memoryUsage = memoryUsage; }
    
    public Double getDiskUsage() { return diskUsage; }
    public void setDiskUsage(Double diskUsage) { this.diskUsage = diskUsage; }
    
    public Long getMemoryTotal() { return memoryTotal; }
    public void setMemoryTotal(Long memoryTotal) { this.memoryTotal = memoryTotal; }
    
    public Long getMemoryUsed() { return memoryUsed; }
    public void setMemoryUsed(Long memoryUsed) { this.memoryUsed = memoryUsed; }
    
    public Long getDiskTotal() { return diskTotal; }
    public void setDiskTotal(Long diskTotal) { this.diskTotal = diskTotal; }
    
    public Long getDiskUsed() { return diskUsed; }
    public void setDiskUsed(Long diskUsed) { this.diskUsed = diskUsed; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public Long getCollectionDurationMs() { return collectionDurationMs; }
    public void setCollectionDurationMs(Long collectionDurationMs) { this.collectionDurationMs = collectionDurationMs; }
    
    public Integer getCpuCores() { return cpuCores; }
    public void setCpuCores(Integer cpuCores) { this.cpuCores = cpuCores; }
    
    public String getLoadAverage() { return loadAverage; }
    public void setLoadAverage(String loadAverage) { this.loadAverage = loadAverage; }
    
    public Long getMemoryAvailable() { return memoryAvailable; }
    public void setMemoryAvailable(Long memoryAvailable) { this.memoryAvailable = memoryAvailable; }
    
    public Long getDiskAvailable() { return diskAvailable; }
    public void setDiskAvailable(Long diskAvailable) { this.diskAvailable = diskAvailable; }
    
    public String getUptime() { return uptime; }
    public void setUptime(String uptime) { this.uptime = uptime; }
    
    public String getOsVersion() { return osVersion; }
    public void setOsVersion(String osVersion) { this.osVersion = osVersion; }

    public String getKernelVersion() { return kernelVersion; }
    public void setKernelVersion(String kernelVersion) { this.kernelVersion = kernelVersion; }

    public String getNetworkInterface() { return networkInterface; }
    public void setNetworkInterface(String networkInterface) { this.networkInterface = networkInterface; }

    public Long getNetworkReceivedBytes() { return networkReceivedBytes; }
    public void setNetworkReceivedBytes(Long networkReceivedBytes) { this.networkReceivedBytes = networkReceivedBytes; }

    public Long getNetworkTransmittedBytes() { return networkTransmittedBytes; }
    public void setNetworkTransmittedBytes(Long networkTransmittedBytes) { this.networkTransmittedBytes = networkTransmittedBytes; }

    public Double getNetworkReceivedRate() { return networkReceivedRate; }
    public void setNetworkReceivedRate(Double networkReceivedRate) { this.networkReceivedRate = networkReceivedRate; }

    public Double getNetworkTransmittedRate() { return networkTransmittedRate; }
    public void setNetworkTransmittedRate(Double networkTransmittedRate) { this.networkTransmittedRate = networkTransmittedRate; }
    
    // 计算百分比的方法
    public Double getMemoryUsagePercent() {
        if (memoryUsage != null) {
            return memoryUsage;
        }
        if (memoryTotal != null && memoryTotal > 0 && memoryUsed != null) {
            return (memoryUsed.doubleValue() / memoryTotal.doubleValue()) * 100.0;
        }
        return 0.0;
    }
    
    public Double getCpuUsagePercent() {
        return cpuUsage != null ? cpuUsage : 0.0;
    }
    
    public Double getDiskUsagePercent() {
        if (diskUsage != null) {
            return diskUsage;
        }
        if (diskTotal != null && diskTotal > 0 && diskUsed != null) {
            return (diskUsed.doubleValue() / diskTotal.doubleValue()) * 100.0;
        }
        return 0.0;
    }
    
    public Double getLoadAverageValue() {
        if (loadAverage != null && !loadAverage.trim().isEmpty()) {
            try {
                // 解析负载平均值，通常格式为 "1.23 1.45 1.67"
                String[] parts = loadAverage.trim().split("\\s+");
                if (parts.length > 0) {
                    return Double.parseDouble(parts[0]);
                }
            } catch (NumberFormatException e) {
                // 忽略解析错误
            }
        }
        return 0.0;
    }
}