package com.cmict.internalpaas.dto;

import com.cmict.internalpaas.model.Server;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 聚合服务器详情所需的数据结构
 */
public class ServerDetailDto {

    private Overview overview;
    private Metrics metrics;
    private List<ProcessInfo> processes = new ArrayList<>();
    private List<ApplicationInfo> applications = new ArrayList<>();
    private List<UserAccessInfo> users = new ArrayList<>();

    public Overview getOverview() {
        return overview;
    }

    public void setOverview(Overview overview) {
        this.overview = overview;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    public List<ProcessInfo> getProcesses() {
        return processes;
    }

    public void setProcesses(List<ProcessInfo> processes) {
        this.processes = processes;
    }

    public List<ApplicationInfo> getApplications() {
        return applications;
    }

    public void setApplications(List<ApplicationInfo> applications) {
        this.applications = applications;
    }

    public List<UserAccessInfo> getUsers() {
        return users;
    }

    public void setUsers(List<UserAccessInfo> users) {
        this.users = users;
    }

    // ========================= 子结构 =========================

    public static class Overview {
        private Long id;
        private String name;
        private String description;
        private String hostname;
        private Integer port;
        private Server.ConnectionStatus connectionStatus;
        private String connectionStatusDescription;
        private String osVersion;
        private String uptime;
        private LocalDateTime lastMetricsTime;
        private Boolean autoMonitorEnabled;
        private Integer monitorInterval;
        private Integer healthScore;
        private List<InfoItem> infoItems = new ArrayList<>();

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

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public Server.ConnectionStatus getConnectionStatus() {
            return connectionStatus;
        }

        public void setConnectionStatus(Server.ConnectionStatus connectionStatus) {
            this.connectionStatus = connectionStatus;
        }

        public String getConnectionStatusDescription() {
            return connectionStatusDescription;
        }

        public void setConnectionStatusDescription(String connectionStatusDescription) {
            this.connectionStatusDescription = connectionStatusDescription;
        }

        public String getOsVersion() {
            return osVersion;
        }

        public void setOsVersion(String osVersion) {
            this.osVersion = osVersion;
        }

        public String getUptime() {
            return uptime;
        }

        public void setUptime(String uptime) {
            this.uptime = uptime;
        }

        public LocalDateTime getLastMetricsTime() {
            return lastMetricsTime;
        }

        public void setLastMetricsTime(LocalDateTime lastMetricsTime) {
            this.lastMetricsTime = lastMetricsTime;
        }

        public Boolean getAutoMonitorEnabled() {
            return autoMonitorEnabled;
        }

        public void setAutoMonitorEnabled(Boolean autoMonitorEnabled) {
            this.autoMonitorEnabled = autoMonitorEnabled;
        }

        public Integer getMonitorInterval() {
            return monitorInterval;
        }

        public void setMonitorInterval(Integer monitorInterval) {
            this.monitorInterval = monitorInterval;
        }

        public Integer getHealthScore() {
            return healthScore;
        }

        public void setHealthScore(Integer healthScore) {
            this.healthScore = healthScore;
        }

        public List<InfoItem> getInfoItems() {
            return infoItems;
        }

        public void setInfoItems(List<InfoItem> infoItems) {
            this.infoItems = infoItems;
        }
    }

    public static class InfoItem {
        private String key;
        private String labelZh;
        private String labelEn;
        private String value;

        public InfoItem() {}

        public InfoItem(String key, String labelZh, String labelEn, String value) {
            this.key = key;
            this.labelZh = labelZh;
            this.labelEn = labelEn;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getLabelZh() {
            return labelZh;
        }

        public void setLabelZh(String labelZh) {
            this.labelZh = labelZh;
        }

        public String getLabelEn() {
            return labelEn;
        }

        public void setLabelEn(String labelEn) {
            this.labelEn = labelEn;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static class Metrics {
        private Double currentCpu;
        private Double currentMemory;
        private Double currentDisk;
        private List<MetricSeries> series = new ArrayList<>();
        private String cpuSummaryZh;
        private String cpuSummaryEn;
        private String memorySummaryZh;
        private String memorySummaryEn;
        private String diskSummaryZh;
        private String diskSummaryEn;
        private String networkSummaryZh;
        private String networkSummaryEn;
        private Boolean networkAvailable;

        public Double getCurrentCpu() {
            return currentCpu;
        }

        public void setCurrentCpu(Double currentCpu) {
            this.currentCpu = currentCpu;
        }

        public Double getCurrentMemory() {
            return currentMemory;
        }

        public void setCurrentMemory(Double currentMemory) {
            this.currentMemory = currentMemory;
        }

        public Double getCurrentDisk() {
            return currentDisk;
        }

        public void setCurrentDisk(Double currentDisk) {
            this.currentDisk = currentDisk;
        }

        public List<MetricSeries> getSeries() {
            return series;
        }

        public void setSeries(List<MetricSeries> series) {
            this.series = series;
        }

        public String getCpuSummaryZh() {
            return cpuSummaryZh;
        }

        public void setCpuSummaryZh(String cpuSummaryZh) {
            this.cpuSummaryZh = cpuSummaryZh;
        }

        public String getCpuSummaryEn() {
            return cpuSummaryEn;
        }

        public void setCpuSummaryEn(String cpuSummaryEn) {
            this.cpuSummaryEn = cpuSummaryEn;
        }

        public String getMemorySummaryZh() {
            return memorySummaryZh;
        }

        public void setMemorySummaryZh(String memorySummaryZh) {
            this.memorySummaryZh = memorySummaryZh;
        }

        public String getMemorySummaryEn() {
            return memorySummaryEn;
        }

        public void setMemorySummaryEn(String memorySummaryEn) {
            this.memorySummaryEn = memorySummaryEn;
        }

        public String getDiskSummaryZh() {
            return diskSummaryZh;
        }

        public void setDiskSummaryZh(String diskSummaryZh) {
            this.diskSummaryZh = diskSummaryZh;
        }

        public String getDiskSummaryEn() {
            return diskSummaryEn;
        }

        public void setDiskSummaryEn(String diskSummaryEn) {
            this.diskSummaryEn = diskSummaryEn;
        }

        public String getNetworkSummaryZh() {
            return networkSummaryZh;
        }

        public void setNetworkSummaryZh(String networkSummaryZh) {
            this.networkSummaryZh = networkSummaryZh;
        }

        public String getNetworkSummaryEn() {
            return networkSummaryEn;
        }

        public void setNetworkSummaryEn(String networkSummaryEn) {
            this.networkSummaryEn = networkSummaryEn;
        }

        public Boolean getNetworkAvailable() {
            return networkAvailable;
        }

        public void setNetworkAvailable(Boolean networkAvailable) {
            this.networkAvailable = networkAvailable;
        }
    }

    public static class MetricSeries {
        private String id;
        private String labelZh;
        private String labelEn;
        private String unit;
        private List<MetricPoint> points = new ArrayList<>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getLabelZh() {
            return labelZh;
        }

        public void setLabelZh(String labelZh) {
            this.labelZh = labelZh;
        }

        public String getLabelEn() {
            return labelEn;
        }

        public void setLabelEn(String labelEn) {
            this.labelEn = labelEn;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public List<MetricPoint> getPoints() {
            return points;
        }

        public void setPoints(List<MetricPoint> points) {
            this.points = points;
        }
    }

    public static class MetricPoint {
        private String timestamp;
        private Double value;

        public MetricPoint() {}

        public MetricPoint(String timestamp, Double value) {
            this.timestamp = timestamp;
            this.value = value;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public Double getValue() {
            return value;
        }

        public void setValue(Double value) {
            this.value = value;
        }
    }

    public static class ProcessInfo {
        private String pid;
        private String name;
        private Double cpu;
        private Double memory;
        private String statusKey;
        private String statusZh;
        private String statusEn;
        private String uptimeZh;
        private String uptimeEn;
        private String command;

        public String getPid() {
            return pid;
        }

        public void setPid(String pid) {
            this.pid = pid;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Double getCpu() {
            return cpu;
        }

        public void setCpu(Double cpu) {
            this.cpu = cpu;
        }

        public Double getMemory() {
            return memory;
        }

        public void setMemory(Double memory) {
            this.memory = memory;
        }

        public String getStatusKey() {
            return statusKey;
        }

        public void setStatusKey(String statusKey) {
            this.statusKey = statusKey;
        }

        public String getStatusZh() {
            return statusZh;
        }

        public void setStatusZh(String statusZh) {
            this.statusZh = statusZh;
        }

        public String getStatusEn() {
            return statusEn;
        }

        public void setStatusEn(String statusEn) {
            this.statusEn = statusEn;
        }

        public String getUptimeZh() {
            return uptimeZh;
        }

        public void setUptimeZh(String uptimeZh) {
            this.uptimeZh = uptimeZh;
        }

        public String getUptimeEn() {
            return uptimeEn;
        }

        public void setUptimeEn(String uptimeEn) {
            this.uptimeEn = uptimeEn;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }
    }

    public static class ApplicationInfo {
        private Long id;
        private String name;
        private String owner;
        private String ownerDisplay;
        private String statusKey;
        private String statusZh;
        private String statusEn;
        private Integer port;
        private Integer debugPort;
        private String version;
        private LocalDateTime lastStartedAt;
        private String remarks;

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

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public String getOwnerDisplay() {
            return ownerDisplay;
        }

        public void setOwnerDisplay(String ownerDisplay) {
            this.ownerDisplay = ownerDisplay;
        }

        public String getStatusKey() {
            return statusKey;
        }

        public void setStatusKey(String statusKey) {
            this.statusKey = statusKey;
        }

        public String getStatusZh() {
            return statusZh;
        }

        public void setStatusZh(String statusZh) {
            this.statusZh = statusZh;
        }

        public String getStatusEn() {
            return statusEn;
        }

        public void setStatusEn(String statusEn) {
            this.statusEn = statusEn;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public Integer getDebugPort() {
            return debugPort;
        }

        public void setDebugPort(Integer debugPort) {
            this.debugPort = debugPort;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public LocalDateTime getLastStartedAt() {
            return lastStartedAt;
        }

        public void setLastStartedAt(LocalDateTime lastStartedAt) {
            this.lastStartedAt = lastStartedAt;
        }

        public String getRemarks() {
            return remarks;
        }

        public void setRemarks(String remarks) {
            this.remarks = remarks;
        }
    }

    public static class UserAccessInfo {
        private Long id;
        private String username;
        private String fullName;
        private String email;
        private String roleKey;
        private String roleZh;
        private String roleEn;
        private String accessTypeKey;
        private String accessTypeZh;
        private String accessTypeEn;
        private LocalDateTime lastLoginTime;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getRoleKey() {
            return roleKey;
        }

        public void setRoleKey(String roleKey) {
            this.roleKey = roleKey;
        }

        public String getRoleZh() {
            return roleZh;
        }

        public void setRoleZh(String roleZh) {
            this.roleZh = roleZh;
        }

        public String getRoleEn() {
            return roleEn;
        }

        public void setRoleEn(String roleEn) {
            this.roleEn = roleEn;
        }

        public String getAccessTypeKey() {
            return accessTypeKey;
        }

        public void setAccessTypeKey(String accessTypeKey) {
            this.accessTypeKey = accessTypeKey;
        }

        public String getAccessTypeZh() {
            return accessTypeZh;
        }

        public void setAccessTypeZh(String accessTypeZh) {
            this.accessTypeZh = accessTypeZh;
        }

        public String getAccessTypeEn() {
            return accessTypeEn;
        }

        public void setAccessTypeEn(String accessTypeEn) {
            this.accessTypeEn = accessTypeEn;
        }

        public LocalDateTime getLastLoginTime() {
            return lastLoginTime;
        }

        public void setLastLoginTime(LocalDateTime lastLoginTime) {
            this.lastLoginTime = lastLoginTime;
        }
    }
}
