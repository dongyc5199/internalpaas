package com.cmict.internalpaas.dto;

import com.cmict.internalpaas.model.Server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 权限检查结果数据传输对象
 */
public class PermissionCheckResultDto {
    
    private Server.PrivilegeLevel privilegeLevel;
    private String currentUser;
    private Boolean hasRootAccess;
    private Boolean hasSudoAccess;
    private List<String> successMessages = new ArrayList<>();
    private List<String> warningMessages = new ArrayList<>();
    private List<String> errorMessages = new ArrayList<>();
    private Map<String, Boolean> commandPermissions = new HashMap<>();
    
    // 检查详情
    private Boolean canCreateUsers;
    private Boolean canManageUserGroups;
    private Boolean canCreateBasicGroups;
    private Boolean canModifySudoers;
    private Boolean canManageFilePermissions;
    
    public PermissionCheckResultDto() {}
    
    public PermissionCheckResultDto(Server.PrivilegeLevel privilegeLevel, String currentUser) {
        this.privilegeLevel = privilegeLevel;
        this.currentUser = currentUser;
    }
    
    // Getters and Setters
    public Server.PrivilegeLevel getPrivilegeLevel() { return privilegeLevel; }
    public void setPrivilegeLevel(Server.PrivilegeLevel privilegeLevel) { this.privilegeLevel = privilegeLevel; }
    
    public String getCurrentUser() { return currentUser; }
    public void setCurrentUser(String currentUser) { this.currentUser = currentUser; }
    
    public Boolean getHasRootAccess() { return hasRootAccess; }
    public void setHasRootAccess(Boolean hasRootAccess) { this.hasRootAccess = hasRootAccess; }
    
    public Boolean getHasSudoAccess() { return hasSudoAccess; }
    public void setHasSudoAccess(Boolean hasSudoAccess) { this.hasSudoAccess = hasSudoAccess; }
    
    public List<String> getSuccessMessages() { return successMessages; }
    public void setSuccessMessages(List<String> successMessages) { this.successMessages = successMessages; }
    
    public List<String> getWarningMessages() { return warningMessages; }
    public void setWarningMessages(List<String> warningMessages) { this.warningMessages = warningMessages; }
    
    public List<String> getErrorMessages() { return errorMessages; }
    public void setErrorMessages(List<String> errorMessages) { this.errorMessages = errorMessages; }
    
    public Map<String, Boolean> getCommandPermissions() { return commandPermissions; }
    public void setCommandPermissions(Map<String, Boolean> commandPermissions) { this.commandPermissions = commandPermissions; }
    
    public Boolean getCanCreateUsers() { return canCreateUsers; }
    public void setCanCreateUsers(Boolean canCreateUsers) { this.canCreateUsers = canCreateUsers; }
    
    public Boolean getCanManageUserGroups() { return canManageUserGroups; }
    public void setCanManageUserGroups(Boolean canManageUserGroups) { this.canManageUserGroups = canManageUserGroups; }
    
    public Boolean getCanCreateBasicGroups() { return canCreateBasicGroups; }
    public void setCanCreateBasicGroups(Boolean canCreateBasicGroups) { this.canCreateBasicGroups = canCreateBasicGroups; }
    
    public Boolean getCanModifySudoers() { return canModifySudoers; }
    public void setCanModifySudoers(Boolean canModifySudoers) { this.canModifySudoers = canModifySudoers; }
    
    public Boolean getCanManageFilePermissions() { return canManageFilePermissions; }
    public void setCanManageFilePermissions(Boolean canManageFilePermissions) { this.canManageFilePermissions = canManageFilePermissions; }
    
    // Helper methods
    public void addSuccessMessage(String message) {
        this.successMessages.add(message);
    }
    
    public void addWarningMessage(String message) {
        this.warningMessages.add(message);
    }
    
    public void addErrorMessage(String message) {
        this.errorMessages.add(message);
    }
    
    public void setCommandPermission(String command, Boolean hasPermission) {
        this.commandPermissions.put(command, hasPermission);
    }
    
    public boolean canManageUserGroups() {
        return privilegeLevel == Server.PrivilegeLevel.ROOT_ACCESS || 
               privilegeLevel == Server.PrivilegeLevel.SUDO_FULL;
    }
    
    public boolean canCreateBasicGroups() {
        return privilegeLevel != Server.PrivilegeLevel.NO_ACCESS &&
               privilegeLevel != Server.PrivilegeLevel.USER_ONLY;
    }
    
    public boolean hasAnyErrors() {
        return !errorMessages.isEmpty();
    }
    
    public boolean hasWarnings() {
        return !warningMessages.isEmpty();
    }
    
    public boolean isFullyFunctional() {
        return privilegeLevel == Server.PrivilegeLevel.ROOT_ACCESS || 
               privilegeLevel == Server.PrivilegeLevel.SUDO_FULL;
    }
}