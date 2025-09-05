package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.dto.PermissionCheckResultDto;
import com.cmict.internalpaas.dto.ServerUserGroupDto;
import com.cmict.internalpaas.dto.UserServerAccountDto;
import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerUserGroup;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.model.UserServerAccount;
import com.cmict.internalpaas.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 权限测试控制器
 * 用于测试和验证权限管理功能
 */
@RestController
@RequestMapping("/api/permission-test")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class PermissionTestController {
    
    private static final Logger logger = LoggerFactory.getLogger(PermissionTestController.class);
    
    @Autowired
    private ServerPermissionValidator permissionValidator;
    
    @Autowired
    private ServerService serverService;
    
    @Autowired
    private PermissionTemplateService templateService;
    
    @Autowired
    private ServerUserGroupService userGroupService;
    
    @Autowired
    private UserServerAccountService accountService;
    
    @Autowired
    private UserService userService;
    
    /**
     * 测试服务器权限检查功能
     */
    @PostMapping("/server/{serverId}/check-permissions")
    public ResponseEntity<?> testServerPermissions(@PathVariable Long serverId) {
        logger.info("开始测试服务器权限检查功能: serverId={}", serverId);
        
        try {
            Optional<Server> serverOpt = serverService.getServerById(serverId);
            if (serverOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Server server = serverOpt.get();
            PermissionCheckResultDto result = permissionValidator.validateServerPermissions(server);
            
            Map<String, Object> response = new HashMap<>();
            response.put("serverId", serverId);
            response.put("serverName", server.getName());
            response.put("privilegeLevel", result.getPrivilegeLevel());
            response.put("currentUser", result.getCurrentUser());
            response.put("hasRootAccess", result.getHasRootAccess());
            response.put("hasSudoAccess", result.getHasSudoAccess());
            response.put("canManageUserGroups", result.canManageUserGroups());
            response.put("canCreateBasicGroups", result.canCreateBasicGroups());
            response.put("successMessages", result.getSuccessMessages());
            response.put("warningMessages", result.getWarningMessages());
            response.put("errorMessages", result.getErrorMessages());
            response.put("commandPermissions", result.getCommandPermissions());
            
            logger.info("服务器权限检查测试完成: serverId={}, privilegeLevel={}", 
                       serverId, result.getPrivilegeLevel());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("测试服务器权限检查时发生异常: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "权限检查测试失败");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("serverId", serverId);
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 测试特定命令权限
     */
    @PostMapping("/server/{serverId}/test-command")
    public ResponseEntity<?> testCommand(@PathVariable Long serverId, 
                                       @RequestParam String command) {
        logger.info("开始测试命令权限: serverId={}, command={}", serverId, command);
        
        try {
            Optional<Server> serverOpt = serverService.getServerById(serverId);
            if (serverOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Server server = serverOpt.get();
            boolean canExecute = permissionValidator.canExecuteCommand(server, command);
            
            Map<String, Object> response = new HashMap<>();
            response.put("serverId", serverId);
            response.put("serverName", server.getName());
            response.put("command", command);
            response.put("canExecute", canExecute);
            response.put("message", canExecute ? "命令可执行" : "命令不可执行");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("测试命令权限时发生异常: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "命令权限测试失败");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("serverId", serverId);
            errorResponse.put("command", command);
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 获取服务器权限状态摘要
     */
    @GetMapping("/server/{serverId}/permission-summary")
    public ResponseEntity<?> getPermissionSummary(@PathVariable Long serverId) {
        logger.info("获取服务器权限状态摘要: serverId={}", serverId);
        
        try {
            Optional<Server> serverOpt = serverService.getServerById(serverId);
            if (serverOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Server server = serverOpt.get();
            
            Map<String, Object> response = new HashMap<>();
            response.put("serverId", serverId);
            response.put("serverName", server.getName());
            response.put("serverType", server.getServerType());
            response.put("privilegeLevel", server.getPrivilegeLevel());
            response.put("privilegeLevelDescription", 
                        server.getPrivilegeLevel() != null ? server.getPrivilegeLevel().getDescription() : null);
            response.put("lastPrivilegeCheck", server.getLastPrivilegeCheck());
            response.put("privilegeCheckDetails", server.getPrivilegeCheckDetails());
            response.put("needsCheck", server.getLastPrivilegeCheck() == null || 
                                      server.getPrivilegeLevel() == Server.PrivilegeLevel.UNKNOWN);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取服务器权限状态摘要时发生异常: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取权限状态摘要失败");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("serverId", serverId);
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 测试权限模板功能
     */
    @GetMapping("/server/{serverId}/test-templates")
    public ResponseEntity<?> testPermissionTemplates(@PathVariable Long serverId) {
        logger.info("测试服务器 {} 的权限模板功能", serverId);
        
        try {
            Optional<Server> serverOpt = serverService.getServerById(serverId);
            if (serverOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Server server = serverOpt.get();
            
            // 获取默认模板
            List<ServerUserGroupDto> templates = templateService.getDefaultTemplates(
                server.getServerType(), server.getPrivilegeLevel());
            
            // 获取推荐默认组名
            String recommendedDefaultGroup = templateService.getRecommendedDefaultGroupName(
                server.getServerType(), server.getPrivilegeLevel());
            
            Map<String, Object> response = new HashMap<>();
            response.put("serverId", serverId);
            response.put("serverName", server.getName());
            response.put("serverType", server.getServerType());
            response.put("privilegeLevel", server.getPrivilegeLevel());
            response.put("templates", templates);
            response.put("templateCount", templates.size());
            response.put("recommendedDefaultGroup", recommendedDefaultGroup);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("测试权限模板时发生异常: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "权限模板测试失败");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("serverId", serverId);
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 测试用户组管理功能
     */
    @GetMapping("/server/{serverId}/test-user-groups")
    public ResponseEntity<?> testUserGroupManagement(@PathVariable Long serverId) {
        logger.info("测试服务器 {} 的用户组管理功能", serverId);
        
        try {
            Optional<Server> serverOpt = serverService.getServerById(serverId);
            if (serverOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Server server = serverOpt.get();
            
            // 获取现有用户组
            List<ServerUserGroup> existingGroups = userGroupService.getServerUserGroups(serverId);
            
            // 获取活跃用户组
            List<ServerUserGroup> activeGroups = userGroupService.getActiveServerUserGroups(serverId);
            
            // 获取默认用户组
            Optional<ServerUserGroup> defaultGroup = userGroupService.getDefaultUserGroup(serverId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("serverId", serverId);
            response.put("serverName", server.getName());
            response.put("existingGroups", userGroupService.convertToDtos(existingGroups));
            response.put("activeGroups", userGroupService.convertToDtos(activeGroups));
            response.put("existingGroupCount", existingGroups.size());
            response.put("activeGroupCount", activeGroups.size());
            response.put("hasDefaultGroup", defaultGroup.isPresent());
            
            if (defaultGroup.isPresent()) {
                response.put("defaultGroup", userGroupService.convertToDto(defaultGroup.get()));
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("测试用户组管理时发生异常: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "用户组管理测试失败");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("serverId", serverId);
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 测试初始化默认用户组
     */
    @PostMapping("/server/{serverId}/test-initialize-groups")
    public ResponseEntity<?> testInitializeDefaultGroups(@PathVariable Long serverId) {
        logger.info("测试初始化服务器 {} 的默认用户组", serverId);
        
        try {
            Optional<Server> serverOpt = serverService.getServerById(serverId);
            if (serverOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Server server = serverOpt.get();
            
            // 执行初始化
            int initializedCount = userGroupService.initializeDefaultUserGroups(server);
            
            // 获取初始化后的用户组
            List<ServerUserGroup> groups = userGroupService.getServerUserGroups(serverId);
            Optional<ServerUserGroup> defaultGroup = userGroupService.getDefaultUserGroup(serverId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "默认用户组初始化完成");
            response.put("serverId", serverId);
            response.put("serverName", server.getName());
            response.put("initializedCount", initializedCount);
            response.put("totalGroups", groups.size());
            response.put("groups", userGroupService.convertToDtos(groups));
            response.put("hasDefaultGroup", defaultGroup.isPresent());
            
            if (defaultGroup.isPresent()) {
                response.put("defaultGroup", userGroupService.convertToDto(defaultGroup.get()));
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("测试初始化默认用户组时发生异常: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "初始化测试失败");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("serverId", serverId);
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 测试用户账户同步功能
     */
    @PostMapping("/server/{serverId}/test-account-sync")
    public ResponseEntity<?> testAccountSync(@PathVariable Long serverId, Authentication authentication) {
        logger.info("测试服务器 {} 的用户账户同步功能", serverId);
        
        try {
            Optional<Server> serverOpt = serverService.getServerById(serverId);
            if (serverOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Server server = serverOpt.get();
            
            // 获取当前用户
            String username = authentication.getName();
            Optional<User> userOpt = userService.findByUsername(username);
            if (userOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "当前用户不存在");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            User currentUser = userOpt.get();
            
            // 执行账户同步
            UserServerAccount account = accountService.syncUserAccount(currentUser, server);
            UserServerAccountDto accountDto = accountService.convertToDto(account);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用户账户同步测试完成");
            response.put("serverId", serverId);
            response.put("serverName", server.getName());
            response.put("userId", currentUser.getId());
            response.put("username", currentUser.getUsername());
            response.put("account", accountDto);
            response.put("canCreateAccount", server.getPrivilegeLevel() == Server.PrivilegeLevel.ROOT_ACCESS || 
                                          server.getPrivilegeLevel() == Server.PrivilegeLevel.SUDO_FULL);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("测试用户账户同步时发生异常: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "账户同步测试失败");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("serverId", serverId);
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 测试账户创建功能
     */
    @PostMapping("/server/{serverId}/test-account-creation")
    public ResponseEntity<?> testAccountCreation(@PathVariable Long serverId, 
                                               @RequestParam(required = false) Long userGroupId,
                                               Authentication authentication) {
        logger.info("测试服务器 {} 的账户创建功能", serverId);
        
        try {
            Optional<Server> serverOpt = serverService.getServerById(serverId);
            if (serverOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Server server = serverOpt.get();
            
            // 获取当前用户
            String username = authentication.getName();
            Optional<User> userOpt = userService.findByUsername(username);
            if (userOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "当前用户不存在");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            User currentUser = userOpt.get();
            
            // 获取用户组
            ServerUserGroup userGroup = null;
            if (userGroupId != null) {
                List<ServerUserGroup> groups = userGroupService.getServerUserGroups(serverId);
                userGroup = groups.stream()
                    .filter(g -> g.getId().equals(userGroupId))
                    .findFirst()
                    .orElse(null);
                
                if (userGroup == null) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("error", "指定的用户组不存在");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }
            
            // 执行账户创建
            UserServerAccount account = accountService.createUserAccount(currentUser, server, userGroup);
            UserServerAccountDto accountDto = accountService.convertToDto(account);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "账户创建测试完成");
            response.put("serverId", serverId);
            response.put("serverName", server.getName());
            response.put("userId", currentUser.getId());
            response.put("username", currentUser.getUsername());
            response.put("account", accountDto);
            response.put("usedUserGroup", userGroup != null ? userGroupService.convertToDto(userGroup) : null);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("测试账户创建时发生异常: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "账户创建测试失败");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("serverId", serverId);
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 测试综合权限管理功能
     */
    @PostMapping("/server/{serverId}/test-comprehensive")
    public ResponseEntity<?> testComprehensivePermissionManagement(@PathVariable Long serverId, 
                                                                 Authentication authentication) {
        logger.info("测试服务器 {} 的综合权限管理功能", serverId);
        
        try {
            Optional<Server> serverOpt = serverService.getServerById(serverId);
            if (serverOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Server server = serverOpt.get();
            
            Map<String, Object> response = new HashMap<>();
            response.put("serverId", serverId);
            response.put("serverName", server.getName());
            response.put("serverType", server.getServerType());
            response.put("privilegeLevel", server.getPrivilegeLevel());
            
            // 1. 权限检查测试
            PermissionCheckResultDto permissionResult = permissionValidator.validateServerPermissions(server);
            response.put("permissionCheck", permissionResult);
            
            // 2. 权限模板测试
            List<ServerUserGroupDto> templates = templateService.getDefaultTemplates(
                server.getServerType(), server.getPrivilegeLevel());
            response.put("availableTemplates", templates);
            response.put("templateCount", templates.size());
            
            // 3. 用户组状态测试
            List<ServerUserGroup> existingGroups = userGroupService.getServerUserGroups(serverId);
            Optional<ServerUserGroup> defaultGroup = userGroupService.getDefaultUserGroup(serverId);
            response.put("existingGroups", userGroupService.convertToDtos(existingGroups));
            response.put("hasDefaultGroup", defaultGroup.isPresent());
            
            // 4. 账户同步测试（如果有当前用户）
            String username = authentication.getName();
            Optional<User> userOpt = userService.findByUsername(username);
            if (userOpt.isPresent()) {
                User currentUser = userOpt.get();
                UserServerAccount account = accountService.checkUserAccountStatus(currentUser, server);
                response.put("currentUserAccount", accountService.convertToDto(account));
            }
            
            // 5. 框架状态汇总
            Map<String, Object> frameworkStatus = new HashMap<>();
            frameworkStatus.put("permissionDetectionWorking", permissionResult.getPrivilegeLevel() != Server.PrivilegeLevel.UNKNOWN);
            frameworkStatus.put("templatesAvailable", !templates.isEmpty());
            frameworkStatus.put("userGroupsConfigured", !existingGroups.isEmpty());
            frameworkStatus.put("defaultGroupConfigured", defaultGroup.isPresent());
            frameworkStatus.put("accountSyncWorking", userOpt.isPresent());
            
            response.put("frameworkStatus", frameworkStatus);
            response.put("testComplete", true);
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("综合权限管理测试时发生异常: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "综合测试失败");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("serverId", serverId);
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 获取框架版本信息
     */
    @GetMapping("/framework-info")
    public ResponseEntity<?> getFrameworkInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("framework", "用户服务器账户自动同步与权限管理");
        response.put("version", "v2.0 - Phase 2 完成");
        response.put("completedPhases", new String[]{
            "Phase 1: 数据模型和权限验证基础",
            "Phase 2: 权限管理核心功能"
        });
        response.put("features", new String[]{
            "服务器权限级别检测",
            "预定义权限模板系统",
            "服务器用户组管理",
            "用户账户自动同步",
            "权限适配性配置",
            "用户组权限管理",
            "用户账户状态同步",
            "权限检查结果缓存",
            "账户创建与删除",
            "用户组分配管理"
        });
        response.put("status", "Phase 2 核心功能完成，可进行全面测试");
        response.put("nextPhase", "Phase 3: 账户同步和分配功能");
        
        return ResponseEntity.ok(response);
    }
}