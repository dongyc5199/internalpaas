package com.cmict.internalpaas.model;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Agent部署记录实体
 * 记录每次Agent部署的详细信息，包括状态、日志、重试次数等
 */
@Entity
@Table(name = "agent_deployments")
public class AgentDeployment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;

    @Column(name = "agent_version", length = 50)
    private String agentVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private DeploymentStatus status;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "deployment_log", length = 5000)
    private String deploymentLog;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (startTime == null) {
            startTime = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors

    public AgentDeployment() {
    }

    public AgentDeployment(Server server, String agentVersion) {
        this.server = server;
        this.agentVersion = agentVersion;
        this.status = DeploymentStatus.PENDING;
        this.retryCount = 0;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public void setAgentVersion(String agentVersion) {
        this.agentVersion = agentVersion;
    }

    public DeploymentStatus getStatus() {
        return status;
    }

    public void setStatus(DeploymentStatus status) {
        this.status = status;
        if (status == DeploymentStatus.SUCCESS || status == DeploymentStatus.FAILED
                || status == DeploymentStatus.ROLLED_BACK) {
            this.endTime = LocalDateTime.now();
        }
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public void incrementRetryCount() {
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getDeploymentLog() {
        return deploymentLog;
    }

    public void setDeploymentLog(String deploymentLog) {
        this.deploymentLog = deploymentLog;
    }

    public void appendLog(String logMessage) {
        if (this.deploymentLog == null) {
            this.deploymentLog = logMessage;
        } else {
            this.deploymentLog += "\n" + logMessage;
        }
        // 限制日志长度
        if (this.deploymentLog.length() > 4900) {
            this.deploymentLog = this.deploymentLog.substring(0, 4900) + "\n... (truncated)";
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "AgentDeployment{" +
                "id=" + id +
                ", serverId=" + (server != null ? server.getId() : null) +
                ", agentVersion='" + agentVersion + '\'' +
                ", status=" + status +
                ", retryCount=" + retryCount +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}
