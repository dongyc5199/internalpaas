package com.cmict.internalpaas.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户组文件权限实体
 * 管理用户组对特定文件/目录的权限配置
 */
@Entity
@Table(name = "group_file_permissions")
public class GroupFilePermission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private ServerUserGroup serverUserGroup;
    
    @Column(name = "file_path", nullable = false)
    private String path;
    
    @Column(name = "permissions", nullable = false)
    private String permissions; // 权限字符串，如：rwx, r--, rw-
    
    @Column(name = "recursive")
    private Boolean recursive = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public ServerUserGroup getServerUserGroup() { return serverUserGroup; }
    public void setServerUserGroup(ServerUserGroup serverUserGroup) { this.serverUserGroup = serverUserGroup; }
    
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    
    public String getPermissions() { return permissions; }
    public void setPermissions(String permissions) { this.permissions = permissions; }
    
    public Boolean getRecursive() { return recursive; }
    public void setRecursive(Boolean recursive) { this.recursive = recursive; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    @Override
    public String toString() {
        return "GroupFilePermission{" +
                "id=" + id +
                ", path='" + path + '\'' +
                ", permissions='" + permissions + '\'' +
                ", recursive=" + recursive +
                '}';
    }
}