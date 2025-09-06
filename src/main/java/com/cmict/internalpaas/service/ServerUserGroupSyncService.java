package com.cmict.internalpaas.service;

import com.cmict.internalpaas.dto.UserGroupSyncResultDto;
import com.cmict.internalpaas.dto.UserGroupSyncResultDto.SyncStepResult;
import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerUserGroup;
import com.cmict.internalpaas.service.RemoteCommandService.CommandResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 服务器用户组系统同步服务
 * 负责将数据库中的用户组配置同步到实际的Linux服务器系统中
 */
@Service
public class ServerUserGroupSyncService {
    
    private static final Logger logger = LoggerFactory.getLogger(ServerUserGroupSyncService.class);
    
    @Autowired
    private RemoteCommandService remoteCommandService;
    
    // 同步超时设置（毫秒）
    private static final long SYNC_TIMEOUT_MS = 30000; // 30秒
    private static final long COMMAND_TIMEOUT_MS = 10000; // 10秒单个命令超时
    
    /**
     * 同步单个用户组到服务器
     * @param userGroup 用户组配置
     * @return 同步结果
     */
    public UserGroupSyncResultDto syncUserGroupToServer(ServerUserGroup userGroup) {
        logger.info("开始同步用户组到服务器: {} -> {}@{}", 
                   userGroup.getGroupName(), userGroup.getServer().getName(), userGroup.getServer().getHostname());
        
        Server server = userGroup.getServer();
        UserGroupSyncResultDto result = new UserGroupSyncResultDto(
            userGroup.getId(), 
            userGroup.getGroupName(), 
            server.getName(), 
            server.getHostname()
        );
        
        try {
            // 步骤1: 检查服务器连接
            if (!checkServerConnection(server, result)) {
                result.markAsFailure("服务器连接失败");
                return result;
            }
            
            // 步骤2: 检查用户组是否已存在
            boolean groupExists = checkGroupExists(server, userGroup.getGroupName(), result);
            
            // 步骤3: 创建用户组（如果不存在）
            if (!groupExists) {
                if (!createSystemGroup(server, userGroup, result)) {
                    result.markAsFailure("用户组创建失败");
                    return result;
                }
            } else {
                logger.info("用户组 {} 已存在，跳过创建步骤", userGroup.getGroupName());
            }
            
            // 步骤4: 配置系统组成员关系
            if (!configureSystemGroupMembership(server, userGroup, result)) {
                result.markAsFailure("系统组成员配置失败");
                return result;
            }
            
            // 步骤5: 配置sudo权限
            if (!configureSudoPermissions(server, userGroup, result)) {
                result.markAsFailure("sudo权限配置失败");
                return result;
            }
            
            // 步骤6: 验证同步结果
            if (!verifyGroupSync(server, userGroup, result)) {
                result.markAsFailure("同步结果验证失败");
                return result;
            }
            
            result.markAsSuccess();
            logger.info("用户组同步成功: {} (总步骤: {}, 成功: {}, 失败: {})", 
                       userGroup.getGroupName(), result.getTotalSteps(), 
                       result.getSuccessfulSteps(), result.getFailedSteps());
            
        } catch (Exception e) {
            logger.error("用户组同步过程中发生异常: {}", e.getMessage(), e);
            result.markAsFailure("同步过程异常: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 批量同步服务器的所有用户组
     * @param server 服务器
     * @param userGroups 用户组列表
     * @return 同步结果列表
     */
    public List<UserGroupSyncResultDto> syncAllUserGroupsToServer(Server server, List<ServerUserGroup> userGroups) {
        logger.info("开始批量同步 {} 个用户组到服务器: {}", userGroups.size(), server.getName());
        
        List<UserGroupSyncResultDto> results = new ArrayList<>();
        
        for (ServerUserGroup userGroup : userGroups) {
            try {
                UserGroupSyncResultDto result = syncUserGroupToServer(userGroup);
                results.add(result);
                
                // 记录单个用户组的同步结果
                if (result.isSuccess()) {
                    logger.info("用户组 {} 同步成功", userGroup.getGroupName());
                } else {
                    logger.error("用户组 {} 同步失败: {}", userGroup.getGroupName(), result.getErrorMessage());
                }
                
            } catch (Exception e) {
                logger.error("同步用户组 {} 时发生异常: {}", userGroup.getGroupName(), e.getMessage(), e);
                
                UserGroupSyncResultDto errorResult = new UserGroupSyncResultDto(
                    userGroup.getId(), userGroup.getGroupName(), server.getName(), server.getHostname());
                errorResult.markAsFailure("同步异常: " + e.getMessage());
                results.add(errorResult);
            }
        }
        
        // 统计总体结果
        long successCount = results.stream().filter(UserGroupSyncResultDto::isSuccess).count();
        long failureCount = results.size() - successCount;
        
        logger.info("批量同步完成: 成功 {}, 失败 {}, 总计 {}", successCount, failureCount, results.size());
        
        return results;
    }
    
    /**
     * 检查服务器连接
     */
    private boolean checkServerConnection(Server server, UserGroupSyncResultDto result) {
        logger.debug("检查服务器连接: {}@{}", server.getSshUsername(), server.getHostname());
        
        SyncStepResult stepResult = new SyncStepResult("检查服务器连接", "echo 'connection test'");
        long startTime = System.currentTimeMillis();
        
        try {
            CommandResult commandResult = remoteCommandService.executeCommand(server, "echo 'connection test'", COMMAND_TIMEOUT_MS);
            stepResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            stepResult.setExitCode(commandResult.getExitCode());
            stepResult.setOutput(commandResult.getOutput());
            stepResult.setError(commandResult.getError());
            
            if (commandResult.isSuccess()) {
                stepResult.setSuccess(true);
                logger.debug("服务器连接正常: {}", server.getHostname());
                result.addStepResult(stepResult);
                return true;
            } else {
                stepResult.setSuccess(false);
                logger.error("服务器连接失败: {} (退出码: {}, 错误: {})", 
                           server.getHostname(), commandResult.getExitCode(), commandResult.getError());
                result.addStepResult(stepResult);
                return false;
            }
            
        } catch (Exception e) {
            stepResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            stepResult.setSuccess(false);
            stepResult.setError("连接异常: " + e.getMessage());
            result.addStepResult(stepResult);
            logger.error("服务器连接检查异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 检查用户组是否存在
     */
    private boolean checkGroupExists(Server server, String groupName, UserGroupSyncResultDto result) {
        logger.debug("检查用户组是否存在: {}", groupName);
        
        String command = String.format("getent group %s", groupName);
        SyncStepResult stepResult = new SyncStepResult("检查用户组存在性", command);
        long startTime = System.currentTimeMillis();
        
        try {
            CommandResult commandResult = remoteCommandService.executeCommand(server, command, COMMAND_TIMEOUT_MS);
            stepResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            stepResult.setExitCode(commandResult.getExitCode());
            stepResult.setOutput(commandResult.getOutput());
            stepResult.setError(commandResult.getError());
            
            boolean exists = commandResult.getExitCode() == 0;
            stepResult.setSuccess(true); // 检查命令本身是成功的
            
            logger.debug("用户组 {} 存在性检查结果: {} (退出码: {})", groupName, exists, commandResult.getExitCode());
            result.addStepResult(stepResult);
            return exists;
            
        } catch (Exception e) {
            stepResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            stepResult.setSuccess(false);
            stepResult.setError("检查异常: " + e.getMessage());
            result.addStepResult(stepResult);
            logger.error("检查用户组存在性异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 创建系统用户组
     */
    private boolean createSystemGroup(Server server, ServerUserGroup userGroup, UserGroupSyncResultDto result) {
        logger.debug("创建系统用户组: {}", userGroup.getGroupName());
        
        String command = String.format("groupadd '%s'", userGroup.getGroupName());
        SyncStepResult stepResult = new SyncStepResult("创建用户组", command);
        long startTime = System.currentTimeMillis();
        
        try {
            CommandResult commandResult = remoteCommandService.executeCommand(server, command, COMMAND_TIMEOUT_MS);
            stepResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            stepResult.setExitCode(commandResult.getExitCode());
            stepResult.setOutput(commandResult.getOutput());
            stepResult.setError(commandResult.getError());
            
            if (commandResult.isSuccess()) {
                stepResult.setSuccess(true);
                logger.info("用户组创建成功: {}", userGroup.getGroupName());
                result.addStepResult(stepResult);
                return true;
            } else {
                stepResult.setSuccess(false);
                logger.error("用户组创建失败: {} (退出码: {}, 错误: {})", 
                           userGroup.getGroupName(), commandResult.getExitCode(), commandResult.getError());
                result.addStepResult(stepResult);
                return false;
            }
            
        } catch (Exception e) {
            stepResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            stepResult.setSuccess(false);
            stepResult.setError("创建异常: " + e.getMessage());
            result.addStepResult(stepResult);
            logger.error("创建用户组异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 配置系统组成员关系
     */
    private boolean configureSystemGroupMembership(Server server, ServerUserGroup userGroup, UserGroupSyncResultDto result) {
        if (userGroup.getSystemGroups() == null || userGroup.getSystemGroups().trim().isEmpty()) {
            logger.debug("用户组 {} 无需配置系统组成员关系", userGroup.getGroupName());
            return true;
        }
        
        logger.debug("配置用户组 {} 的系统组成员关系: {}", userGroup.getGroupName(), userGroup.getSystemGroups());
        
        String[] systemGroups = userGroup.getSystemGroups().split(",");
        boolean allSuccess = true;
        
        for (String systemGroup : systemGroups) {
            systemGroup = systemGroup.trim();
            if (systemGroup.isEmpty()) continue;
            
            // 检查系统组是否存在
            if (!checkSystemGroupExists(server, systemGroup, result)) {
                logger.warn("系统组 {} 不存在，跳过配置", systemGroup);
                continue;
            }
            
            // 将用户组添加到系统组中（实际上是设置用户组的supplementary groups）
            String command = String.format("# 用户组 %s 需要加入系统组 %s - 这通常在用户创建时处理", 
                                         userGroup.getGroupName(), systemGroup);
            
            SyncStepResult stepResult = new SyncStepResult("配置系统组成员关系", command);
            stepResult.setSuccess(true); // 这一步只是记录，不执行实际操作
            stepResult.setOutput("系统组配置已记录，将在用户创建时应用");
            stepResult.setExecutionTimeMs(1);
            result.addStepResult(stepResult);
            
            logger.debug("记录用户组 {} 需要加入系统组 {}", userGroup.getGroupName(), systemGroup);
        }
        
        return allSuccess;
    }
    
    /**
     * 检查系统组是否存在
     */
    private boolean checkSystemGroupExists(Server server, String systemGroup, UserGroupSyncResultDto result) {
        String command = String.format("getent group %s", systemGroup);
        
        try {
            CommandResult commandResult = remoteCommandService.executeCommand(server, command, COMMAND_TIMEOUT_MS);
            boolean exists = commandResult.getExitCode() == 0;
            
            if (exists) {
                logger.debug("系统组 {} 存在", systemGroup);
            } else {
                logger.debug("系统组 {} 不存在 (退出码: {})", systemGroup, commandResult.getExitCode());
            }
            
            return exists;
            
        } catch (Exception e) {
            logger.error("检查系统组 {} 存在性异常: {}", systemGroup, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 配置sudo权限
     */
    private boolean configureSudoPermissions(Server server, ServerUserGroup userGroup, UserGroupSyncResultDto result) {
        if (userGroup.getSudoCommands() == null || userGroup.getSudoCommands().isEmpty()) {
            logger.debug("用户组 {} 无sudo权限配置", userGroup.getGroupName());
            return true;
        }
        
        logger.debug("配置用户组 {} 的sudo权限: {}", userGroup.getGroupName(), userGroup.getSudoCommands());
        
        // 创建sudoers配置文件内容
        String sudoersContent = generateSudoersContent(userGroup);
        String sudoersFile = String.format("/etc/sudoers.d/%s", userGroup.getGroupName());
        
        // 创建临时文件并写入sudo配置
        String command = String.format(
            "echo '%s' | sudo tee %s > /dev/null && sudo chmod 440 %s", 
            sudoersContent, sudoersFile, sudoersFile
        );
        
        SyncStepResult stepResult = new SyncStepResult("配置sudo权限", 
            String.format("创建 %s 并设置权限", sudoersFile));
        long startTime = System.currentTimeMillis();
        
        try {
            CommandResult commandResult = remoteCommandService.executeCommand(server, command, COMMAND_TIMEOUT_MS);
            stepResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            stepResult.setExitCode(commandResult.getExitCode());
            stepResult.setOutput(commandResult.getOutput());
            stepResult.setError(commandResult.getError());
            
            if (commandResult.isSuccess()) {
                stepResult.setSuccess(true);
                logger.info("sudo权限配置成功: {} -> {}", userGroup.getGroupName(), sudoersFile);
                result.addStepResult(stepResult);
                
                // 验证sudoers文件语法
                return validateSudoersFile(server, sudoersFile, result);
            } else {
                stepResult.setSuccess(false);
                logger.error("sudo权限配置失败: {} (退出码: {}, 错误: {})", 
                           userGroup.getGroupName(), commandResult.getExitCode(), commandResult.getError());
                result.addStepResult(stepResult);
                return false;
            }
            
        } catch (Exception e) {
            stepResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            stepResult.setSuccess(false);
            stepResult.setError("配置异常: " + e.getMessage());
            result.addStepResult(stepResult);
            logger.error("配置sudo权限异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 生成sudoers配置内容
     */
    private String generateSudoersContent(ServerUserGroup userGroup) {
        StringBuilder content = new StringBuilder();
        content.append("# Sudo configuration for user group: ").append(userGroup.getGroupName()).append("\n");
        content.append("# Generated by InternalPaaS at: ").append(new Date()).append("\n");
        content.append("# Permission level: ").append(userGroup.getPermissionLevel()).append("\n");
        content.append("\n");
        
        for (String sudoCommand : userGroup.getSudoCommands()) {
            // 清理和验证sudo命令
            String cleanCommand = sudoCommand.trim();
            if (cleanCommand.isEmpty()) continue;
            
            // 为用户组添加sudo权限
            content.append("%").append(userGroup.getGroupName()).append(" ALL=(ALL) NOPASSWD: ").append(cleanCommand).append("\n");
        }
        
        return content.toString();
    }
    
    /**
     * 验证sudoers文件语法
     */
    private boolean validateSudoersFile(Server server, String sudoersFile, UserGroupSyncResultDto result) {
        logger.debug("验证sudoers文件语法: {}", sudoersFile);
        
        String command = String.format("visudo -c -f %s", sudoersFile);
        SyncStepResult stepResult = new SyncStepResult("验证sudoers语法", command);
        long startTime = System.currentTimeMillis();
        
        try {
            CommandResult commandResult = remoteCommandService.executeCommand(server, command, COMMAND_TIMEOUT_MS);
            stepResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            stepResult.setExitCode(commandResult.getExitCode());
            stepResult.setOutput(commandResult.getOutput());
            stepResult.setError(commandResult.getError());
            
            if (commandResult.isSuccess()) {
                stepResult.setSuccess(true);
                logger.info("sudoers文件语法验证通过: {}", sudoersFile);
                result.addStepResult(stepResult);
                return true;
            } else {
                stepResult.setSuccess(false);
                logger.error("sudoers文件语法错误: {} (退出码: {}, 错误: {})", 
                           sudoersFile, commandResult.getExitCode(), commandResult.getError());
                result.addStepResult(stepResult);
                
                // 语法错误时删除文件
                deleteSudoersFile(server, sudoersFile, result);
                return false;
            }
            
        } catch (Exception e) {
            stepResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            stepResult.setSuccess(false);
            stepResult.setError("验证异常: " + e.getMessage());
            result.addStepResult(stepResult);
            logger.error("验证sudoers文件异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 删除sudoers文件
     */
    private void deleteSudoersFile(Server server, String sudoersFile, UserGroupSyncResultDto result) {
        logger.debug("删除错误的sudoers文件: {}", sudoersFile);
        
        String command = String.format("sudo rm -f %s", sudoersFile);
        SyncStepResult stepResult = new SyncStepResult("删除错误sudoers文件", command);
        long startTime = System.currentTimeMillis();
        
        try {
            CommandResult commandResult = remoteCommandService.executeCommand(server, command, COMMAND_TIMEOUT_MS);
            stepResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            stepResult.setExitCode(commandResult.getExitCode());
            stepResult.setOutput(commandResult.getOutput());
            stepResult.setError(commandResult.getError());
            stepResult.setSuccess(commandResult.isSuccess());
            
            result.addStepResult(stepResult);
            
            if (commandResult.isSuccess()) {
                logger.info("错误sudoers文件已删除: {}", sudoersFile);
            } else {
                logger.error("删除sudoers文件失败: {}", sudoersFile);
            }
            
        } catch (Exception e) {
            stepResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            stepResult.setSuccess(false);
            stepResult.setError("删除异常: " + e.getMessage());
            result.addStepResult(stepResult);
            logger.error("删除sudoers文件异常: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 验证用户组同步结果
     */
    private boolean verifyGroupSync(Server server, ServerUserGroup userGroup, UserGroupSyncResultDto result) {
        logger.debug("验证用户组同步结果: {}", userGroup.getGroupName());
        
        // 验证用户组存在
        String command = String.format("getent group %s", userGroup.getGroupName());
        SyncStepResult stepResult = new SyncStepResult("验证用户组同步", command);
        long startTime = System.currentTimeMillis();
        
        try {
            CommandResult commandResult = remoteCommandService.executeCommand(server, command, COMMAND_TIMEOUT_MS);
            stepResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            stepResult.setExitCode(commandResult.getExitCode());
            stepResult.setOutput(commandResult.getOutput());
            stepResult.setError(commandResult.getError());
            
            if (commandResult.isSuccess() && !commandResult.getOutput().trim().isEmpty()) {
                stepResult.setSuccess(true);
                logger.info("用户组同步验证成功: {} -> {}", userGroup.getGroupName(), commandResult.getOutput().trim());
                result.addStepResult(stepResult);
                return true;
            } else {
                stepResult.setSuccess(false);
                logger.error("用户组同步验证失败: {} (退出码: {})", userGroup.getGroupName(), commandResult.getExitCode());
                result.addStepResult(stepResult);
                return false;
            }
            
        } catch (Exception e) {
            stepResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            stepResult.setSuccess(false);
            stepResult.setError("验证异常: " + e.getMessage());
            result.addStepResult(stepResult);
            logger.error("验证用户组同步异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 删除服务器上的用户组
     * @param server 服务器
     * @param groupName 用户组名称
     * @return 删除结果
     */
    public UserGroupSyncResultDto removeUserGroupFromServer(Server server, String groupName) {
        logger.info("开始从服务器删除用户组: {} -> {}@{}", groupName, server.getName(), server.getHostname());
        
        UserGroupSyncResultDto result = new UserGroupSyncResultDto(null, groupName, server.getName(), server.getHostname());
        
        try {
            // 删除sudoers文件
            String sudoersFile = String.format("/etc/sudoers.d/%s", groupName);
            deleteSudoersFile(server, sudoersFile, result);
            
            // 删除用户组
            String command = String.format("groupdel %s", groupName);
            SyncStepResult stepResult = new SyncStepResult("删除用户组", command);
            long startTime = System.currentTimeMillis();
            
            CommandResult commandResult = remoteCommandService.executeCommand(server, command, COMMAND_TIMEOUT_MS);
            stepResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            stepResult.setExitCode(commandResult.getExitCode());
            stepResult.setOutput(commandResult.getOutput());
            stepResult.setError(commandResult.getError());
            stepResult.setSuccess(commandResult.isSuccess());
            
            result.addStepResult(stepResult);
            
            if (commandResult.isSuccess()) {
                result.markAsSuccess();
                logger.info("用户组删除成功: {}", groupName);
            } else {
                result.markAsFailure("用户组删除失败: " + commandResult.getError());
                logger.error("用户组删除失败: {} (退出码: {}, 错误: {})", 
                           groupName, commandResult.getExitCode(), commandResult.getError());
            }
            
        } catch (Exception e) {
            result.markAsFailure("删除过程异常: " + e.getMessage());
            logger.error("删除用户组异常: {}", e.getMessage(), e);
        }
        
        return result;
    }
}