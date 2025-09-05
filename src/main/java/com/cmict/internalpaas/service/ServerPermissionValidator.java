package com.cmict.internalpaas.service;

import com.cmict.internalpaas.dto.PermissionCheckResultDto;
import com.cmict.internalpaas.model.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 服务器权限验证服务
 * 负责检测和验证服务器的权限级别
 */
@Service
public class ServerPermissionValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(ServerPermissionValidator.class);
    
    @Autowired
    private RemoteCommandService remoteCommandService;
    
    @Autowired
    private ServerService serverService;
    
    /**
     * 验证服务器权限级别
     * @param server 目标服务器
     * @return 权限检查结果
     */
    public PermissionCheckResultDto validateServerPermissions(Server server) {
        logger.info("开始检查服务器权限: {}", server.getName());
        
        PermissionCheckResultDto result = new PermissionCheckResultDto();
        result.setPrivilegeLevel(Server.PrivilegeLevel.UNKNOWN);
        
        try {
            // 1. 检查当前用户身份
            String currentUser = getCurrentUser(server);
            result.setCurrentUser(currentUser);
            
            if (currentUser == null) {
                result.addErrorMessage("无法获取当前用户信息");
                result.setPrivilegeLevel(Server.PrivilegeLevel.NO_ACCESS);
                return result;
            }
            
            // 2. 检查是否为root用户
            boolean isRoot = "root".equals(currentUser);
            result.setHasRootAccess(isRoot);
            
            if (isRoot) {
                result.setPrivilegeLevel(Server.PrivilegeLevel.ROOT_ACCESS);
                result.addSuccessMessage("当前用户为root，拥有完整系统权限");
                validateRootCapabilities(server, result);
            } else {
                // 3. 检查sudo权限
                validateSudoPermissions(server, result);
            }
            
            // 4. 检查关键命令执行权限
            validateCommandPermissions(server, result);
            
            // 5. 更新服务器权限级别缓存
            updateServerPrivilegeLevel(server, result);
            
        } catch (Exception e) {
            logger.error("检查服务器权限时发生异常: {}", e.getMessage(), e);
            result.addErrorMessage("权限检查过程中发生异常: " + e.getMessage());
            result.setPrivilegeLevel(Server.PrivilegeLevel.UNKNOWN);
        }
        
        logger.info("服务器 {} 权限检查完成，级别: {}", server.getName(), result.getPrivilegeLevel());
        return result;
    }
    
    /**
     * 检查特定命令的执行权限
     */
    public boolean canExecuteCommand(Server server, String command) {
        try {
            RemoteCommandService.CommandResult result = remoteCommandService.executeCommand(server, 
                "which " + command + " && echo 'COMMAND_EXISTS' || echo 'COMMAND_NOT_FOUND'");
            return result.isSuccess() && result.getOutput().contains("COMMAND_EXISTS");
        } catch (Exception e) {
            logger.warn("检查命令 {} 权限时发生异常: {}", command, e.getMessage());
            return false;
        }
    }
    
    /**
     * 更新服务器权限级别缓存
     */
    public void updateServerPrivilegeLevel(Server server, PermissionCheckResultDto result) {
        try {
            server.setPrivilegeLevel(result.getPrivilegeLevel());
            server.setLastPrivilegeCheck(LocalDateTime.now());
            
            // 构建检查详情JSON
            StringBuilder details = new StringBuilder();
            details.append("{");
            details.append("\"currentUser\":\"").append(result.getCurrentUser()).append("\",");
            details.append("\"hasRootAccess\":").append(result.getHasRootAccess()).append(",");
            details.append("\"hasSudoAccess\":").append(result.getHasSudoAccess()).append(",");
            details.append("\"successCount\":").append(result.getSuccessMessages().size()).append(",");
            details.append("\"warningCount\":").append(result.getWarningMessages().size()).append(",");
            details.append("\"errorCount\":").append(result.getErrorMessages().size());
            details.append("}");
            
            server.setPrivilegeCheckDetails(details.toString());
            serverService.saveServer(server);
            
            logger.info("已更新服务器 {} 的权限级别缓存: {}", server.getName(), result.getPrivilegeLevel());
        } catch (Exception e) {
            logger.error("更新服务器权限级别缓存失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 获取当前SSH用户
     */
    private String getCurrentUser(Server server) {
        try {
            RemoteCommandService.CommandResult result = remoteCommandService.executeCommand(server, "whoami");
            if (result.isSuccess()) {
                return result.getOutput().trim();
            }
        } catch (Exception e) {
            logger.warn("获取当前用户失败: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 验证root用户的能力
     */
    private void validateRootCapabilities(Server server, PermissionCheckResultDto result) {
        result.setCanCreateUsers(true);
        result.setCanManageUserGroups(true);
        result.setCanCreateBasicGroups(true);
        result.setCanModifySudoers(true);
        result.setCanManageFilePermissions(true);
        
        result.addSuccessMessage("拥有用户管理权限");
        result.addSuccessMessage("拥有用户组管理权限");
        result.addSuccessMessage("拥有sudo配置权限");
        result.addSuccessMessage("拥有文件权限管理权限");
    }
    
    /**
     * 验证sudo权限
     */
    private void validateSudoPermissions(Server server, PermissionCheckResultDto result) {
        try {
            // 测试基本sudo权限
            RemoteCommandService.CommandResult sudoTest = remoteCommandService.executeCommand(server, 
                "sudo -n echo 'sudo_test' 2>/dev/null || echo 'no_sudo'");
            
            if (sudoTest.isSuccess() && sudoTest.getOutput().contains("sudo_test")) {
                result.setHasSudoAccess(true);
                result.addSuccessMessage("拥有无密码sudo权限");
                
                // 进一步测试sudo权限级别
                validateSudoLevel(server, result);
            } else {
                // 测试需要密码的sudo
                RemoteCommandService.CommandResult sudoPasswordTest = remoteCommandService.executeCommand(server, 
                    "sudo -l 2>/dev/null | head -1 || echo 'no_sudo_list'");
                
                if (sudoPasswordTest.isSuccess() && !sudoPasswordTest.getOutput().contains("no_sudo_list")) {
                    result.setHasSudoAccess(true);
                    result.setPrivilegeLevel(Server.PrivilegeLevel.SUDO_LIMITED);
                    result.addWarningMessage("拥有sudo权限但需要密码");
                    result.setCanCreateUsers(false);
                    result.setCanManageUserGroups(false);
                    result.setCanCreateBasicGroups(true);
                    result.setCanModifySudoers(false);
                    result.setCanManageFilePermissions(false);
                } else {
                    result.setHasSudoAccess(false);
                    result.setPrivilegeLevel(Server.PrivilegeLevel.USER_ONLY);
                    result.addErrorMessage("当前用户无sudo权限");
                    setUserOnlyCapabilities(result);
                }
            }
        } catch (Exception e) {
            logger.warn("检查sudo权限时发生异常: {}", e.getMessage());
            result.setHasSudoAccess(false);
            result.setPrivilegeLevel(Server.PrivilegeLevel.UNKNOWN);
            result.addErrorMessage("无法检查sudo权限: " + e.getMessage());
        }
    }
    
    /**
     * 验证sudo权限级别
     */
    private void validateSudoLevel(Server server, PermissionCheckResultDto result) {
        try {
            // 测试用户管理权限
            boolean canCreateUsers = testCommand(server, "sudo useradd --help");
            result.setCanCreateUsers(canCreateUsers);
            result.setCommandPermission("useradd", canCreateUsers);
            
            // 测试用户组管理权限
            boolean canManageGroups = testCommand(server, "sudo groupadd --help");
            result.setCanManageUserGroups(canManageGroups);
            result.setCommandPermission("groupadd", canManageGroups);
            
            // 测试sudo配置权限
            boolean canModifySudoers = testCommand(server, "sudo test -w /etc/sudoers.d/");
            result.setCanModifySudoers(canModifySudoers);
            result.setCommandPermission("sudoers", canModifySudoers);
            
            // 根据测试结果确定权限级别
            if (canCreateUsers && canManageGroups && canModifySudoers) {
                result.setPrivilegeLevel(Server.PrivilegeLevel.SUDO_FULL);
                result.addSuccessMessage("拥有完整sudo权限");
            } else if (canCreateUsers || canManageGroups) {
                result.setPrivilegeLevel(Server.PrivilegeLevel.SUDO_LIMITED);
                result.addWarningMessage("拥有受限sudo权限");
            } else {
                result.setPrivilegeLevel(Server.PrivilegeLevel.USER_ONLY);
                result.addErrorMessage("sudo权限严重受限");
            }
            
            result.setCanCreateBasicGroups(canManageGroups);
            result.setCanManageFilePermissions(canModifySudoers);
            
        } catch (Exception e) {
            logger.warn("检查sudo级别时发生异常: {}", e.getMessage());
            result.setPrivilegeLevel(Server.PrivilegeLevel.SUDO_LIMITED);
            result.addWarningMessage("无法完全检查sudo权限级别");
        }
    }
    
    /**
     * 设置仅普通用户权限的能力
     */
    private void setUserOnlyCapabilities(PermissionCheckResultDto result) {
        result.setCanCreateUsers(false);
        result.setCanManageUserGroups(false);
        result.setCanCreateBasicGroups(false);
        result.setCanModifySudoers(false);
        result.setCanManageFilePermissions(false);
    }
    
    /**
     * 验证关键命令权限
     */
    private void validateCommandPermissions(Server server, PermissionCheckResultDto result) {
        String[] commands = {"useradd", "usermod", "userdel", "groupadd", "groupmod", "groupdel", 
                           "chmod", "chown", "visudo"};
        
        for (String command : commands) {
            boolean hasPermission = canExecuteCommand(server, command);
            result.setCommandPermission(command, hasPermission);
            
            if (hasPermission) {
                result.addSuccessMessage("命令 " + command + " 可用");
            } else {
                result.addWarningMessage("命令 " + command + " 不可用");
            }
        }
    }
    
    /**
     * 测试命令执行权限
     */
    private boolean testCommand(Server server, String command) {
        try {
            RemoteCommandService.CommandResult result = remoteCommandService.executeCommand(server, command);
            return result.isSuccess() || result.getExitCode() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}