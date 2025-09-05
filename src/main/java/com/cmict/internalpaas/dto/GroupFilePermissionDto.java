package com.cmict.internalpaas.dto;

import java.time.LocalDateTime;

/**
 * 用户组文件权限数据传输对象
 */
public class GroupFilePermissionDto {
    
    private Long id;
    private Long groupId;
    private String path;
    private String permissions;
    private Boolean recursive;
    private LocalDateTime createdAt;
    
    public GroupFilePermissionDto() {}
    
    public GroupFilePermissionDto(String path, String permissions, Boolean recursive) {
        this.path = path;
        this.permissions = permissions;
        this.recursive = recursive;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    
    public String getPermissions() { return permissions; }
    public void setPermissions(String permissions) { this.permissions = permissions; }
    
    public Boolean getRecursive() { return recursive; }
    public void setRecursive(Boolean recursive) { this.recursive = recursive; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}