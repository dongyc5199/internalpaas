package com.cmict.internalpaas.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ssh_sessions")
public class SSHSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "session_id", unique = true)
    private String sessionId;
    
    @Column(name = "server_id")
    private Long serverId;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "username")
    private String username;
    
    @Column(name = "client_ip")
    private String clientIp;
    
    @Column(name = "start_time")
    private LocalDateTime startTime;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Column(name = "is_active")
    private Boolean isActive;
    
    @Column(name = "terminal_type")
    private String terminalType;
    
    @Column(name = "window_size")
    private String windowSize;
    
    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;
    
    @Column(name = "total_commands")
    private Integer totalCommands = 0;
    
    @Column(name = "total_data_sent")
    private Long totalDataSent = 0L;
    
    @Column(name = "total_data_received")
    private Long totalDataReceived = 0L;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "session_status")
    private SessionStatus sessionStatus;
    
    @PrePersist
    protected void onCreate() {
        if (startTime == null) {
            startTime = LocalDateTime.now();
        }
        if (lastHeartbeat == null) {
            lastHeartbeat = LocalDateTime.now();
        }
        if (isActive == null) {
            isActive = true;
        }
        if (sessionStatus == null) {
            sessionStatus = SessionStatus.CONNECTING;
        }
    }
    
    public enum SessionStatus {
        CONNECTING("连接中"),
        ACTIVE("活跃"),
        IDLE("空闲"),
        DISCONNECTED("已断开"),
        TIMEOUT("超时"),
        ERROR("错误");
        
        private final String description;
        
        SessionStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }

    // Constructors
    public SSHSession() {}
    
    public SSHSession(String sessionId, Long serverId, Long userId, String username, String clientIp) {
        this.sessionId = sessionId;
        this.serverId = serverId;
        this.userId = userId;
        this.username = username;
        this.clientIp = clientIp;
        this.startTime = LocalDateTime.now();
        this.lastHeartbeat = LocalDateTime.now();
        this.isActive = true;
        this.sessionStatus = SessionStatus.CONNECTING;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public String getTerminalType() { return terminalType; }
    public void setTerminalType(String terminalType) { this.terminalType = terminalType; }
    
    public String getWindowSize() { return windowSize; }
    public void setWindowSize(String windowSize) { this.windowSize = windowSize; }
    
    public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(LocalDateTime lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }
    
    public Integer getTotalCommands() { return totalCommands; }
    public void setTotalCommands(Integer totalCommands) { this.totalCommands = totalCommands; }
    
    public Long getTotalDataSent() { return totalDataSent; }
    public void setTotalDataSent(Long totalDataSent) { this.totalDataSent = totalDataSent; }
    
    public Long getTotalDataReceived() { return totalDataReceived; }
    public void setTotalDataReceived(Long totalDataReceived) { this.totalDataReceived = totalDataReceived; }
    
    public SessionStatus getSessionStatus() { return sessionStatus; }
    public void setSessionStatus(SessionStatus sessionStatus) { this.sessionStatus = sessionStatus; }
    
    // Helper methods
    public void updateHeartbeat() {
        this.lastHeartbeat = LocalDateTime.now();
        if (this.sessionStatus == SessionStatus.IDLE) {
            this.sessionStatus = SessionStatus.ACTIVE;
        }
    }
    
    public void incrementCommands() {
        this.totalCommands = (this.totalCommands == null ? 0 : this.totalCommands) + 1;
        updateHeartbeat();
    }
    
    public void addDataSent(long bytes) {
        this.totalDataSent = (this.totalDataSent == null ? 0L : this.totalDataSent) + bytes;
        updateHeartbeat();
    }
    
    public void addDataReceived(long bytes) {
        this.totalDataReceived = (this.totalDataReceived == null ? 0L : this.totalDataReceived) + bytes;
        updateHeartbeat();
    }
    
    public void close() {
        this.isActive = false;
        this.endTime = LocalDateTime.now();
        this.sessionStatus = SessionStatus.DISCONNECTED;
    }
    
    public long getDurationMinutes() {
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, end).toMinutes();
    }
    
    public boolean isTimeout(int timeoutMinutes) {
        if (lastHeartbeat == null) return false;
        return java.time.Duration.between(lastHeartbeat, LocalDateTime.now()).toMinutes() > timeoutMinutes;
    }
}