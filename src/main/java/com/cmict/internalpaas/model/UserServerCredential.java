package com.cmict.internalpaas.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户服务器凭据实体
 * 存储用户在各个服务器上的SSH凭据信息
 */
@Entity
@Table(name = "user_server_credentials", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "server_id"}))
public class UserServerCredential {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;
    
    @Column(name = "ssh_username", nullable = false)
    private String sshUsername;
    
    @Column(name = "ssh_password", nullable = false, length = 500)
    private String sshPassword; // 加密存储的SSH密码
    
    @Column(name = "password_generated", nullable = false)
    private Boolean passwordGenerated = true; // 标记密码是否由系统生成
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false) 
    private LocalDateTime updatedAt;
    
    @Column(name = "last_used")
    private LocalDateTime lastUsed; // 最后使用时间
    
    // 默认构造函数
    public UserServerCredential() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // 带参数构造函数
    public UserServerCredential(User user, Server server, String sshUsername, String sshPassword) {
        this();
        this.user = user;
        this.server = server;
        this.sshUsername = sshUsername;
        this.sshPassword = sshPassword;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public Server getServer() {
        return server;
    }
    
    public void setServer(Server server) {
        this.server = server;
    }
    
    public String getSshUsername() {
        return sshUsername;
    }
    
    public void setSshUsername(String sshUsername) {
        this.sshUsername = sshUsername;
    }
    
    public String getSshPassword() {
        return sshPassword;
    }
    
    public void setSshPassword(String sshPassword) {
        this.sshPassword = sshPassword;
        this.updatedAt = LocalDateTime.now();
    }
    
    public Boolean getPasswordGenerated() {
        return passwordGenerated;
    }
    
    public void setPasswordGenerated(Boolean passwordGenerated) {
        this.passwordGenerated = passwordGenerated;
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
    
    public LocalDateTime getLastUsed() {
        return lastUsed;
    }
    
    public void setLastUsed(LocalDateTime lastUsed) {
        this.lastUsed = lastUsed;
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}