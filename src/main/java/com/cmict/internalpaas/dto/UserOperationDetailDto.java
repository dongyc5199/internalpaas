package com.cmict.internalpaas.dto;

import com.cmict.internalpaas.model.UserActivity;
import java.time.LocalDateTime;

/**
 * 用户操作详情DTO
 */
public class UserOperationDetailDto {
    
    private Long id;
    private Long serverId;
    private String serverName;
    private String username;
    private String userId;
    private String sessionId;
    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private Boolean isActive;
    private String terminalType;
    private String remoteIp;
    private Integer commandCount;
    private LocalDateTime lastActivity;
    private UserActivity.ActivityType activityType;
    private String activityTypeDescription;
    private String activityDetails;
    private LocalDateTime createdAt;
    private Long sessionDuration; // 会话时长（分钟）
    
    // 扩展信息
    private String operationSummary; // 操作摘要
    private String riskLevel; // 风险等级
    private Integer totalOperations; // 总操作数
    private String lastCommand; // 最后执行命令
    
    public UserOperationDetailDto() {}
    
    public UserOperationDetailDto(UserActivity activity) {
        this.id = activity.getId();
        this.serverId = activity.getServerId();
        this.username = activity.getUsername();
        this.userId = activity.getUserId();
        this.sessionId = activity.getSessionId();
        this.loginTime = activity.getLoginTime();
        this.logoutTime = activity.getLogoutTime();
        this.isActive = activity.getIsActive();
        this.terminalType = activity.getTerminalType();
        this.remoteIp = activity.getRemoteIp();
        this.commandCount = activity.getCommandCount();
        this.lastActivity = activity.getLastActivity();
        this.activityType = activity.getActivityType();
        this.activityTypeDescription = activity.getActivityType() != null ? 
            activity.getActivityType().getDescription() : null;
        this.activityDetails = activity.getActivityDetails();
        this.createdAt = activity.getCreatedAt();
        
        // 计算会话时长
        if (loginTime != null) {
            LocalDateTime endTime = logoutTime != null ? logoutTime : LocalDateTime.now();
            this.sessionDuration = java.time.Duration.between(loginTime, endTime).toMinutes();
        }
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }
    
    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public LocalDateTime getLoginTime() { return loginTime; }
    public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }
    
    public LocalDateTime getLogoutTime() { return logoutTime; }
    public void setLogoutTime(LocalDateTime logoutTime) { this.logoutTime = logoutTime; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public String getTerminalType() { return terminalType; }
    public void setTerminalType(String terminalType) { this.terminalType = terminalType; }
    
    public String getRemoteIp() { return remoteIp; }
    public void setRemoteIp(String remoteIp) { this.remoteIp = remoteIp; }
    
    public Integer getCommandCount() { return commandCount; }
    public void setCommandCount(Integer commandCount) { this.commandCount = commandCount; }
    
    public LocalDateTime getLastActivity() { return lastActivity; }
    public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }
    
    public UserActivity.ActivityType getActivityType() { return activityType; }
    public void setActivityType(UserActivity.ActivityType activityType) { this.activityType = activityType; }
    
    public String getActivityTypeDescription() { return activityTypeDescription; }
    public void setActivityTypeDescription(String activityTypeDescription) { this.activityTypeDescription = activityTypeDescription; }
    
    public String getActivityDetails() { return activityDetails; }
    public void setActivityDetails(String activityDetails) { this.activityDetails = activityDetails; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public Long getSessionDuration() { return sessionDuration; }
    public void setSessionDuration(Long sessionDuration) { this.sessionDuration = sessionDuration; }
    
    public String getOperationSummary() { return operationSummary; }
    public void setOperationSummary(String operationSummary) { this.operationSummary = operationSummary; }
    
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    
    public Integer getTotalOperations() { return totalOperations; }
    public void setTotalOperations(Integer totalOperations) { this.totalOperations = totalOperations; }
    
    public String getLastCommand() { return lastCommand; }
    public void setLastCommand(String lastCommand) { this.lastCommand = lastCommand; }
}