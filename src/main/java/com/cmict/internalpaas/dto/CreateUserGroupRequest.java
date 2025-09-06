package com.cmict.internalpaas.dto;

import com.cmict.internalpaas.model.ServerUserGroup;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;

/**
 * 创建用户组请求数据传输对象
 */
public class CreateUserGroupRequest {
    
    @NotBlank(message = "用户组名称不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "用户组名称只能包含字母、数字、下划线和连字符")
    @Size(min = 2, max = 50, message = "用户组名称长度必须在2-50个字符之间")
    private String groupName;
    
    @Size(max = 200, message = "用户组描述不能超过200个字符")
    private String groupDescription;
    
    @NotNull(message = "权限级别不能为空")
    private ServerUserGroup.PermissionLevel permissionLevel;
    
    private Set<String> systemGroups = new HashSet<>();
    private Set<String> sudoCommands = new HashSet<>();
    private Set<GroupFilePermissionDto> filePermissions = new HashSet<>();
    private Boolean isDefault = false;
    private Boolean active = true;
    
    public CreateUserGroupRequest() {}
    
    public CreateUserGroupRequest(String groupName, ServerUserGroup.PermissionLevel permissionLevel) {
        this.groupName = groupName;
        this.permissionLevel = permissionLevel;
    }
    
    // Getters and Setters
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    
    public String getGroupDescription() { return groupDescription; }
    public void setGroupDescription(String groupDescription) { this.groupDescription = groupDescription; }
    
    public ServerUserGroup.PermissionLevel getPermissionLevel() { return permissionLevel; }
    public void setPermissionLevel(ServerUserGroup.PermissionLevel permissionLevel) { this.permissionLevel = permissionLevel; }
    
    public Set<String> getSystemGroups() { return systemGroups; }
    public void setSystemGroups(Set<String> systemGroups) { this.systemGroups = systemGroups; }
    
    public Set<String> getSudoCommands() { return sudoCommands; }
    public void setSudoCommands(Set<String> sudoCommands) { this.sudoCommands = sudoCommands; }
    
    public Set<GroupFilePermissionDto> getFilePermissions() { return filePermissions; }
    public void setFilePermissions(Set<GroupFilePermissionDto> filePermissions) { this.filePermissions = filePermissions; }
    
    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
    
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    
    // Helper methods
    public void addSystemGroup(String group) {
        this.systemGroups.add(group);
    }
    
    public void addSudoCommand(String command) {
        this.sudoCommands.add(command);
    }
    
    public void addFilePermission(GroupFilePermissionDto permission) {
        this.filePermissions.add(permission);
    }
    
    public String getSystemGroupsAsString() {
        return systemGroups.isEmpty() ? null : String.join(",", systemGroups);
    }
}