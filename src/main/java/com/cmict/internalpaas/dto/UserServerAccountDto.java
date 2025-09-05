package com.cmict.internalpaas.dto;

import com.cmict.internalpaas.model.UserServerAccount;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 用户服务器账户数据传输对象
 */
public class UserServerAccountDto {
    
    private Long id;
    private Long userId;
    private String username;
    private Long serverId;
    private String serverName;
    private String serverHostname;
    private UserServerAccount.AccountStatus accountStatus;
    private String homeDirectory;
    private String workDirectory;
    private LocalDateTime lastSyncTime;
    private Long userGroupId;
    private String userGroupName;
    private String syncErrorMessage;
    private Set<String> additionalSystemGroups;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 额外的状态信息
    private String statusDescription;
    private Boolean needsSync;
    private String syncStatusBadge; // 用于前端显示的状态徽章
    
    public UserServerAccountDto() {}
    
    public UserServerAccountDto(Long userId, String username, Long serverId, String serverName, 
                               UserServerAccount.AccountStatus accountStatus) {
        this.userId = userId;
        this.username = username;
        this.serverId = serverId;
        this.serverName = serverName;
        this.accountStatus = accountStatus;
        this.statusDescription = accountStatus.getDescription();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }
    
    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }
    
    public String getServerHostname() { return serverHostname; }
    public void setServerHostname(String serverHostname) { this.serverHostname = serverHostname; }
    
    public UserServerAccount.AccountStatus getAccountStatus() { return accountStatus; }
    public void setAccountStatus(UserServerAccount.AccountStatus accountStatus) { 
        this.accountStatus = accountStatus;
        this.statusDescription = accountStatus != null ? accountStatus.getDescription() : null;
    }
    
    public String getHomeDirectory() { return homeDirectory; }
    public void setHomeDirectory(String homeDirectory) { this.homeDirectory = homeDirectory; }
    
    public String getWorkDirectory() { return workDirectory; }
    public void setWorkDirectory(String workDirectory) { this.workDirectory = workDirectory; }
    
    public LocalDateTime getLastSyncTime() { return lastSyncTime; }
    public void setLastSyncTime(LocalDateTime lastSyncTime) { this.lastSyncTime = lastSyncTime; }
    
    public Long getUserGroupId() { return userGroupId; }
    public void setUserGroupId(Long userGroupId) { this.userGroupId = userGroupId; }
    
    public String getUserGroupName() { return userGroupName; }
    public void setUserGroupName(String userGroupName) { this.userGroupName = userGroupName; }
    
    public String getSyncErrorMessage() { return syncErrorMessage; }
    public void setSyncErrorMessage(String syncErrorMessage) { this.syncErrorMessage = syncErrorMessage; }
    
    public Set<String> getAdditionalSystemGroups() { return additionalSystemGroups; }
    public void setAdditionalSystemGroups(Set<String> additionalSystemGroups) { this.additionalSystemGroups = additionalSystemGroups; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getStatusDescription() { return statusDescription; }
    public void setStatusDescription(String statusDescription) { this.statusDescription = statusDescription; }
    
    public Boolean getNeedsSync() { return needsSync; }
    public void setNeedsSync(Boolean needsSync) { this.needsSync = needsSync; }
    
    public String getSyncStatusBadge() { return syncStatusBadge; }
    public void setSyncStatusBadge(String syncStatusBadge) { this.syncStatusBadge = syncStatusBadge; }
    
    // Helper methods
    public boolean isAccountCreated() {
        return accountStatus == UserServerAccount.AccountStatus.CREATED || 
               accountStatus == UserServerAccount.AccountStatus.EXISTS;
    }
    
    public boolean hasError() {
        return accountStatus == UserServerAccount.AccountStatus.CREATE_FAILED || 
               accountStatus == UserServerAccount.AccountStatus.SYNC_ERROR;
    }
    
    public boolean isProcessing() {
        return accountStatus == UserServerAccount.AccountStatus.CREATING;
    }
}