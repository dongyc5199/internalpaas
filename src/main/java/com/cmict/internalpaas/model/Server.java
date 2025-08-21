package com.cmict.internalpaas.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "servers")
public class Server {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String hostname;
    
    @Column(nullable = false)
    private Integer port;
    
    @Column
    private String description;
    
    @Column(nullable = false)
    private String baseWorkDirectory;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    // SSH连接配置
    @Column
    private Integer sshPort = 22;
    
    @Column
    private String sshUsername;
    
    @Column
    private String sshPassword;
    
    @Column
    private String sshKeyPath;
    
    @Column
    private String sshKeyPassphrase;
    
    // 连接状态缓存
    @Column
    @Enumerated(EnumType.STRING)
    private ConnectionStatus connectionStatus = ConnectionStatus.UNKNOWN;
    
    @Column
    private LocalDateTime lastConnectionCheck;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 连接状态枚举
    public enum ConnectionStatus {
        CONNECTED,      // 连接成功
        FAILED,         // 连接失败
        TIMEOUT,        // 连接超时
        AUTH_FAILED,    // 认证失败
        UNKNOWN         // 未知状态
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }
    
    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getBaseWorkDirectory() { return baseWorkDirectory; }
    public void setBaseWorkDirectory(String baseWorkDirectory) { this.baseWorkDirectory = baseWorkDirectory; }
    
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    
    public Integer getSshPort() { return sshPort; }
    public void setSshPort(Integer sshPort) { this.sshPort = sshPort; }
    
    public String getSshUsername() { return sshUsername; }
    public void setSshUsername(String sshUsername) { this.sshUsername = sshUsername; }
    
    public String getSshPassword() { return sshPassword; }
    public void setSshPassword(String sshPassword) { this.sshPassword = sshPassword; }
    
    public String getSshKeyPath() { return sshKeyPath; }
    public void setSshKeyPath(String sshKeyPath) { this.sshKeyPath = sshKeyPath; }
    
    public String getSshKeyPassphrase() { return sshKeyPassphrase; }
    public void setSshKeyPassphrase(String sshKeyPassphrase) { this.sshKeyPassphrase = sshKeyPassphrase; }
    
    public ConnectionStatus getConnectionStatus() { return connectionStatus; }
    public void setConnectionStatus(ConnectionStatus connectionStatus) { this.connectionStatus = connectionStatus; }
    
    public LocalDateTime getLastConnectionCheck() { return lastConnectionCheck; }
    public void setLastConnectionCheck(LocalDateTime lastConnectionCheck) { this.lastConnectionCheck = lastConnectionCheck; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}