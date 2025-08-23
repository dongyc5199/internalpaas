package com.cmict.internalpaas.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_activities")
public class UserActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "server_id", nullable = false)
    private Long serverId;
    
    @Column(name = "username")
    private String username;
    
    @Column(name = "user_id")
    private String userId;
    
    @Column(name = "session_id")
    private String sessionId;
    
    @Column(name = "login_time")
    private LocalDateTime loginTime;
    
    @Column(name = "logout_time")
    private LocalDateTime logoutTime;
    
    @Column(name = "is_active")
    private Boolean isActive;
    
    @Column(name = "terminal_type")
    private String terminalType;
    
    @Column(name = "remote_ip")
    private String remoteIp;
    
    @Column(name = "command_count")
    private Integer commandCount = 0;
    
    @Column(name = "last_activity")
    private LocalDateTime lastActivity;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type")
    private ActivityType activityType;
    
    @Column(name = "activity_details", length = 1000)
    private String activityDetails;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (lastActivity == null) {
            lastActivity = LocalDateTime.now();
        }
    }
    
    public enum ActivityType {
        LOGIN("SSH登录"),
        LOGOUT("SSH登出"),
        COMMAND_EXECUTE("命令执行"),
        FILE_TRANSFER("文件传输"),
        PROCESS_START("进程启动"),
        PROCESS_STOP("进程停止"),
        DIRECTORY_CHANGE("目录切换"),
        FILE_EDIT("文件编辑"),
        SYSTEM_INFO("系统信息查看");
        
        private final String description;
        
        ActivityType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }

    // Constructors
    public UserActivity() {}
    
    public UserActivity(Long serverId, String username, String sessionId, ActivityType activityType) {
        this.serverId = serverId;
        this.username = username;
        this.sessionId = sessionId;
        this.activityType = activityType;
        this.isActive = true;
        this.lastActivity = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }
    
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
    
    public ActivityType getActivityType() { return activityType; }
    public void setActivityType(ActivityType activityType) { this.activityType = activityType; }
    
    public String getActivityDetails() { return activityDetails; }
    public void setActivityDetails(String activityDetails) { this.activityDetails = activityDetails; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    // Helper methods
    public void incrementCommandCount() {
        this.commandCount = (this.commandCount == null ? 0 : this.commandCount) + 1;
        this.lastActivity = LocalDateTime.now();
    }
    
    public void updateActivity(ActivityType activityType, String details) {
        this.activityType = activityType;
        this.activityDetails = details;
        this.lastActivity = LocalDateTime.now();
    }
    
    public boolean isActiveSession() {
        return isActive != null && isActive && logoutTime == null;
    }
}