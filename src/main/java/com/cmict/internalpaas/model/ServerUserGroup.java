package com.cmict.internalpaas.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 服务器用户组实体
 * 管理服务器上的用户组和权限配置
 */
@Entity
@Table(name = "server_user_groups")
public class ServerUserGroup {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;
    
    @Column(name = "group_name", nullable = false)
    private String groupName;
    
    @Column(name = "group_description")
    private String groupDescription;
    
    @Column(name = "system_groups")
    private String systemGroups; // 逗号分隔，如：docker,sudo
    
    @Enumerated(EnumType.STRING)
    @Column(name = "permission_level", nullable = false)
    private PermissionLevel permissionLevel;
    
    @ElementCollection
    @CollectionTable(name = "group_sudo_commands", joinColumns = @JoinColumn(name = "group_id"))
    @Column(name = "sudo_command")
    private Set<String> sudoCommands = new HashSet<>();
    
    @OneToMany(mappedBy = "serverUserGroup", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<GroupFilePermission> filePermissions = new HashSet<>();
    
    @Column(name = "is_default")
    private Boolean isDefault = false;
    
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
    
    // 权限级别枚举
    public enum PermissionLevel {
        READONLY("只读权限"),
        DEVELOPER("开发权限"),
        OPERATOR("运维权限"),
        ADMIN("管理员权限");
        
        private final String description;
        
        PermissionLevel(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Server getServer() { return server; }
    public void setServer(Server server) { this.server = server; }
    
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    
    public String getGroupDescription() { return groupDescription; }
    public void setGroupDescription(String groupDescription) { this.groupDescription = groupDescription; }
    
    public String getSystemGroups() { return systemGroups; }
    public void setSystemGroups(String systemGroups) { this.systemGroups = systemGroups; }
    
    public PermissionLevel getPermissionLevel() { return permissionLevel; }
    public void setPermissionLevel(PermissionLevel permissionLevel) { this.permissionLevel = permissionLevel; }
    
    public Set<String> getSudoCommands() { return sudoCommands; }
    public void setSudoCommands(Set<String> sudoCommands) { this.sudoCommands = sudoCommands; }
    
    public Set<GroupFilePermission> getFilePermissions() { return filePermissions; }
    public void setFilePermissions(Set<GroupFilePermission> filePermissions) { this.filePermissions = filePermissions; }
    
    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // Helper methods
    public void addSudoCommand(String command) {
        this.sudoCommands.add(command);
    }
    
    public void removeSudoCommand(String command) {
        this.sudoCommands.remove(command);
    }
    
    public void addFilePermission(GroupFilePermission permission) {
        this.filePermissions.add(permission);
        permission.setServerUserGroup(this);
    }
    
    public void removeFilePermission(GroupFilePermission permission) {
        this.filePermissions.remove(permission);
        permission.setServerUserGroup(null);
    }
    
    @Override
    public String toString() {
        return "ServerUserGroup{" +
                "id=" + id +
                ", groupName='" + groupName + '\'' +
                ", permissionLevel=" + permissionLevel +
                ", isDefault=" + isDefault +
                '}';
    }
}