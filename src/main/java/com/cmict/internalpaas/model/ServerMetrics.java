package com.cmict.internalpaas.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "server_metrics")
public class ServerMetrics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "server_id")
    private Long serverId;
    
    @Column(name = "server_name")
    private String serverName;
    
    @Column(name = "hostname")
    private String hostname;
    
    // CPU信息
    @Column(name = "cpu_usage")
    private Double cpuUsage;
    
    @Column(name = "cpu_cores")
    private Integer cpuCores;
    
    @Column(name = "load_average")
    private String loadAverage;
    
    // 内存信息
    @Column(name = "memory_total")
    private Long memoryTotal;
    
    @Column(name = "memory_used")
    private Long memoryUsed;
    
    @Column(name = "memory_available")
    private Long memoryAvailable;
    
    @Column(name = "memory_usage")
    private Double memoryUsage;
    
    // 磁盘信息
    @Column(name = "disk_total")
    private Long diskTotal;
    
    @Column(name = "disk_used")
    private Long diskUsed;
    
    @Column(name = "disk_available")
    private Long diskAvailable;
    
    @Column(name = "disk_usage")
    private Double diskUsage;
    
    // 系统信息
    @Column(name = "uptime")
    private String uptime;
    
    @Column(name = "os_version")
    private String osVersion;
    
    @Column(name = "timestamp")
    private LocalDateTime timestamp;
    
    @Column(name = "collection_duration_ms")
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
}