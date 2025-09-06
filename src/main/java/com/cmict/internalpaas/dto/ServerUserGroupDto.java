package com.cmict.internalpaas.dto;

import com.cmict.internalpaas.model.ServerUserGroup;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 服务器用户组数据传输对象
 */
public class ServerUserGroupDto {
    
    private Long id;
    private Long serverId;
    private String serverName;
    private String groupName;
    private String groupDescription;
    private String systemGroups;
    private ServerUserGroup.PermissionLevel permissionLevel;
    private Set<String> sudoCommands;
    private Set<GroupFilePermissionDto> filePermissions;
    private Boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 额外的统计信息
    private Integer userCount; // 使用此用户组的用户数量
    private Boolean isSynced; // 是否已同步到服务器
    private String syncStatus; // 同步状态描述
    private Boolean active; // 用户组是否活跃
    
    public ServerUserGroupDto() {}
    
    public ServerUserGroupDto(Long id, String groupName, ServerUserGroup.PermissionLevel permissionLevel) {
        this.id = id;
        this.groupName = groupName;
        this.permissionLevel = permissionLevel;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }
    
    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }
    
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    
    public String getGroupDescription() { return groupDescription; }
    public void setGroupDescription(String groupDescription) { this.groupDescription = groupDescription; }
    
    public String getSystemGroups() { return systemGroups; }
    public void setSystemGroups(String systemGroups) { this.systemGroups = systemGroups; }
    
    public ServerUserGroup.PermissionLevel getPermissionLevel() { return permissionLevel; }
    public void setPermissionLevel(ServerUserGroup.PermissionLevel permissionLevel) { this.permissionLevel = permissionLevel; }
    
    public Set<String> getSudoCommands() { return sudoCommands; }
    public void setSudoCommands(Set<String> sudoCommands) { this.sudoCommands = sudoCommands; }
    
    public Set<GroupFilePermissionDto> getFilePermissions() { return filePermissions; }
    public void setFilePermissions(Set<GroupFilePermissionDto> filePermissions) { this.filePermissions = filePermissions; }
    
    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public Integer getUserCount() { return userCount; }
    public void setUserCount(Integer userCount) { this.userCount = userCount; }
    
    public Boolean getIsSynced() { return isSynced; }
    public void setIsSynced(Boolean isSynced) { this.isSynced = isSynced; }
    
    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
    
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}