package com.cmict.internalpaas.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 用户服务器账户实体
 * 管理用户在特定服务器上的账户状态和配置
 */
@Entity
@Table(name = "user_server_accounts")
public class UserServerAccount {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    private AccountStatus accountStatus = AccountStatus.NOT_CHECKED;
    
    @Column(name = "home_directory")
    private String homeDirectory;
    
    @Column(name = "work_directory")
    private String workDirectory;
    
    @Column(name = "last_sync_time")
    private LocalDateTime lastSyncTime;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_group_id")
    private ServerUserGroup userGroup;
    
    @Column(name = "sync_error_message", columnDefinition = "TEXT")
    private String syncErrorMessage;
    
    @ElementCollection
    @CollectionTable(name = "user_additional_groups", joinColumns = @JoinColumn(name = "user_server_account_id"))
    @Column(name = "additional_group")
    private Set<String> additionalSystemGroups = new HashSet<>();
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
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
    
    // 账户状态枚举
    public enum AccountStatus {
        NOT_CHECKED("未检测"),
        NOT_EXISTS("不存在"),
        EXISTS("已存在"),
        CREATING("创建中"),
        CREATED("已创建"),
        CREATE_FAILED("创建失败"),
        SYNC_ERROR("同步错误");
        
        private final String description;
        
        AccountStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public Server getServer() { return server; }
    public void setServer(Server server) { this.server = server; }
    
    public AccountStatus getAccountStatus() { return accountStatus; }
    public void setAccountStatus(AccountStatus accountStatus) { this.accountStatus = accountStatus; }
    
    public String getHomeDirectory() { return homeDirectory; }
    public void setHomeDirectory(String homeDirectory) { this.homeDirectory = homeDirectory; }
    
    public String getWorkDirectory() { return workDirectory; }
    public void setWorkDirectory(String workDirectory) { this.workDirectory = workDirectory; }
    
    public LocalDateTime getLastSyncTime() { return lastSyncTime; }
    public void setLastSyncTime(LocalDateTime lastSyncTime) { this.lastSyncTime = lastSyncTime; }
    
    public ServerUserGroup getUserGroup() { return userGroup; }
    public void setUserGroup(ServerUserGroup userGroup) { this.userGroup = userGroup; }
    
    public String getSyncErrorMessage() { return syncErrorMessage; }
    public void setSyncErrorMessage(String syncErrorMessage) { this.syncErrorMessage = syncErrorMessage; }
    
    public Set<String> getAdditionalSystemGroups() { return additionalSystemGroups; }
    public void setAdditionalSystemGroups(Set<String> additionalSystemGroups) { this.additionalSystemGroups = additionalSystemGroups; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // Helper methods
    public void addAdditionalGroup(String group) {
        this.additionalSystemGroups.add(group);
    }
    
    public void removeAdditionalGroup(String group) {
        this.additionalSystemGroups.remove(group);
    }
    
    public boolean isAccountCreated() {
        return accountStatus == AccountStatus.CREATED || accountStatus == AccountStatus.EXISTS;
    }
    
    public boolean isAccountCreating() {
        return accountStatus == AccountStatus.CREATING;
    }
    
    public boolean hasError() {
        return accountStatus == AccountStatus.CREATE_FAILED || accountStatus == AccountStatus.SYNC_ERROR;
    }
    
    public void markAsCreating() {
        this.accountStatus = AccountStatus.CREATING;
        this.syncErrorMessage = null;
        this.lastSyncTime = LocalDateTime.now();
    }
    
    public void markAsCreated() {
        this.accountStatus = AccountStatus.CREATED;
        this.syncErrorMessage = null;
        this.lastSyncTime = LocalDateTime.now();
    }
    
    public void markAsExists() {
        this.accountStatus = AccountStatus.EXISTS;
        this.syncErrorMessage = null;
        this.lastSyncTime = LocalDateTime.now();
    }
    
    public void markAsCreateFailed(String errorMessage) {
        this.accountStatus = AccountStatus.CREATE_FAILED;
        this.syncErrorMessage = errorMessage;
        this.lastSyncTime = LocalDateTime.now();
    }
    
    public void markAsSyncError(String errorMessage) {
        this.accountStatus = AccountStatus.SYNC_ERROR;
        this.syncErrorMessage = errorMessage;
        this.lastSyncTime = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "UserServerAccount{" +
                "id=" + id +
                ", user=" + (user != null ? user.getUsername() : null) +
                ", server=" + (server != null ? server.getName() : null) +
                ", accountStatus=" + accountStatus +
                ", lastSyncTime=" + lastSyncTime +
                '}';
    }
}