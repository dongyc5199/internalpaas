package com.cmict.internalpaas.dto;

import java.time.LocalDateTime;

/**
 * Server Group View DTO
 * Used for server group management page display
 */
public class ServerGroupViewDto {

    private Long id;
    private String name;
    private String address;
    private Integer port;

    // Monitoring metrics
    private Integer users;
    private Integer apps;
    private Double cpuUsage;
    private Double memoryUsage;
    private Double diskUsage;
    private String networkTraffic;

    // Health and status
    private Integer healthScore;
    private String status; // online, warning, offline
    private String uptime;
    private LocalDateTime lastUpdateTime;

    public ServerGroupViewDto() {
    }

    // Getters and Setters
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getUsers() {
        return users;
    }

    public void setUsers(Integer users) {
        this.users = users;
    }

    public Integer getApps() {
        return apps;
    }

    public void setApps(Integer apps) {
        this.apps = apps;
    }

    public Double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(Double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public Double getMemoryUsage() {
        return memoryUsage;
    }

    public void setMemoryUsage(Double memoryUsage) {
        this.memoryUsage = memoryUsage;
    }

    public Double getDiskUsage() {
        return diskUsage;
    }

    public void setDiskUsage(Double diskUsage) {
        this.diskUsage = diskUsage;
    }

    public String getNetworkTraffic() {
        return networkTraffic;
    }

    public void setNetworkTraffic(String networkTraffic) {
        this.networkTraffic = networkTraffic;
    }

    public Integer getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(Integer healthScore) {
        this.healthScore = healthScore;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUptime() {
        return uptime;
    }

    public void setUptime(String uptime) {
        this.uptime = uptime;
    }

    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(LocalDateTime lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
}
