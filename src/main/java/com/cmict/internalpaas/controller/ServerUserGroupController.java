package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.dto.CreateUserGroupRequest;
import com.cmict.internalpaas.dto.ServerUserGroupDto;
import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerUserGroup;
import com.cmict.internalpaas.service.PermissionTemplateService;
import com.cmict.internalpaas.service.ServerService;
import com.cmict.internalpaas.service.ServerUserGroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

/**
 * 服务器用户组管理控制器
 * 提供用户组的CRUD操作和权限配置功能
 */
@RestController
@RequestMapping("/api/server-user-groups")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class ServerUserGroupController {
    
    private static final Logger logger = LoggerFactory.getLogger(ServerUserGroupController.class);
    
    @Autowired
    private ServerUserGroupService userGroupService;
    
    @Autowired
    private PermissionTemplateService templateService;
    
    @Autowired
    private ServerService serverService;
    
    /**
     * 获取服务器的所有用户组
     */
    @GetMapping("/server/{serverId}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getServerUserGroups(@PathVariable Long serverId) {
        logger.info("获取服务器 {} 的用户组列表", serverId);
        
        try {
            List<ServerUserGroup> groups = userGroupService.getServerUserGroups(serverId);
            List<ServerUserGroupDto> groupDtos = userGroupService.convertToDtos(groups);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("userGroups", groupDtos);
            response.put("count", groupDtos.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取服务器用户组列表失败", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "获取用户组列表失败");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 获取服务器的活跃用户组
     */
    @GetMapping("/server/{serverId}/active")
    public ResponseEntity<?> getActiveServerUserGroups(@PathVariable Long serverId) {
        logger.info("获取服务器 {} 的活跃用户组", serverId);
        
        try {
            List<ServerUserGroup> groups = userGroupService.getActiveServerUserGroups(serverId);
            List<ServerUserGroupDto> groupDtos = userGroupService.convertToDtos(groups);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("activeUserGroups", groupDtos);
            response.put("count", groupDtos.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取活跃用户组失败", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "获取活跃用户组失败");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 获取权限模板
     */
    @GetMapping("/templates/server/{serverId}")
    public ResponseEntity<?> getPermissionTemplates(@PathVariable Long serverId) {
        logger.info("获取服务器 {} 的权限模板", serverId);
        
        try {
            Optional<Server> serverOpt = serverService.getServerById(serverId);
            if (serverOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "服务器不存在");
                return ResponseEntity.notFound().build();
            }
            
            Server server = serverOpt.get();
            List<ServerUserGroupDto> templates = templateService.getDefaultTemplates(
                server.getServerType(), server.getPrivilegeLevel());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("templates", templates);
            response.put("serverType", server.getServerType());
            response.put("privilegeLevel", server.getPrivilegeLevel());
            response.put("count", templates.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取权限模板失败", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "获取权限模板失败");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 创建用户组
     */
    @PostMapping("/server/{serverId}")
    public ResponseEntity<?> createUserGroup(@PathVariable Long serverId,
                                           @Valid @RequestBody CreateUserGroupRequest request) {
        logger.info("为服务器 {} 创建用户组: {}", serverId, request.getGroupName());
        
        try {
            ServerUserGroup group = userGroupService.createUserGroup(request, serverId);
            ServerUserGroupDto groupDto = userGroupService.convertToDto(group);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用户组创建成功");
            response.put("userGroup", groupDto);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("创建用户组失败", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "创建用户组失败");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * 更新用户组
     */
    @PutMapping("/{groupId}")
    public ResponseEntity<?> updateUserGroup(@PathVariable Long groupId,
                                           @Valid @RequestBody CreateUserGroupRequest request) {
        logger.info("更新用户组: {}", groupId);
        
        try {
            ServerUserGroup group = userGroupService.updateUserGroup(groupId, request);
            ServerUserGroupDto groupDto = userGroupService.convertToDto(group);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用户组更新成功");
            response.put("userGroup", groupDto);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("更新用户组失败", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "更新用户组失败");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * 删除用户组
     */
    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> deleteUserGroup(@PathVariable Long groupId) {
        logger.info("删除用户组: {}", groupId);
        
        try {
            userGroupService.deleteUserGroup(groupId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用户组删除成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("删除用户组失败", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "删除用户组失败");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * 启用/禁用用户组
     */
    @PostMapping("/{groupId}/toggle-active")
    public ResponseEntity<?> toggleUserGroupActive(@PathVariable Long groupId,
                                                 @RequestParam boolean active) {
        logger.info("{}用户组: {}", active ? "启用" : "禁用", groupId);
        
        try {
            userGroupService.setUserGroupActive(groupId, active);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", active ? "用户组已启用" : "用户组已禁用");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("切换用户组状态失败", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "切换用户组状态失败");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * 设置默认用户组
     */
    @PostMapping("/server/{serverId}/default/{groupId}")
    public ResponseEntity<?> setDefaultUserGroup(@PathVariable Long serverId,
                                               @PathVariable Long groupId) {
        logger.info("设置服务器 {} 的默认用户组为: {}", serverId, groupId);
        
        try {
            userGroupService.setDefaultUserGroup(serverId, groupId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "默认用户组设置成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("设置默认用户组失败", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "设置默认用户组失败");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * 初始化服务器默认用户组
     */
    @PostMapping("/server/{serverId}/initialize-defaults")
    public ResponseEntity<?> initializeDefaultUserGroups(@PathVariable Long serverId) {
        logger.info("初始化服务器 {} 的默认用户组", serverId);
        
        try {
            Optional<Server> serverOpt = serverService.getServerById(serverId);
            if (serverOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "服务器不存在");
                return ResponseEntity.notFound().build();
            }
            
            int count = userGroupService.initializeDefaultUserGroups(serverOpt.get());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "默认用户组初始化完成");
            response.put("initializedCount", count);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("初始化默认用户组失败", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "初始化默认用户组失败");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 根据权限级别获取用户组
     */
    @GetMapping("/server/{serverId}/permission-level/{permissionLevel}")
    public ResponseEntity<?> getUserGroupsByPermissionLevel(@PathVariable Long serverId,
                                                          @PathVariable ServerUserGroup.PermissionLevel permissionLevel) {
        logger.info("获取服务器 {} 权限级别 {} 的用户组", serverId, permissionLevel);
        
        try {
            List<ServerUserGroup> groups = userGroupService.getUserGroupsByPermissionLevel(serverId, permissionLevel);
            List<ServerUserGroupDto> groupDtos = userGroupService.convertToDtos(groups);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("userGroups", groupDtos);
            response.put("permissionLevel", permissionLevel);
            response.put("count", groupDtos.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("根据权限级别获取用户组失败", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "获取用户组失败");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 获取默认用户组
     */
    @GetMapping("/server/{serverId}/default")
    public ResponseEntity<?> getDefaultUserGroup(@PathVariable Long serverId) {
        logger.info("获取服务器 {} 的默认用户组", serverId);
        
        try {
            Optional<ServerUserGroup> defaultGroup = userGroupService.getDefaultUserGroup(serverId);
            
            Map<String, Object> response = new HashMap<>();
            if (defaultGroup.isPresent()) {
                ServerUserGroupDto groupDto = userGroupService.convertToDto(defaultGroup.get());
                response.put("success", true);
                response.put("defaultUserGroup", groupDto);
                response.put("hasDefault", true);
            } else {
                response.put("success", true);
                response.put("defaultUserGroup", null);
                response.put("hasDefault", false);
                response.put("message", "未设置默认用户组");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取默认用户组失败", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "获取默认用户组失败");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 批量操作用户组
     */
    @PostMapping("/server/{serverId}/batch")
    public ResponseEntity<?> batchOperateUserGroups(@PathVariable Long serverId,
                                                   @RequestBody Map<String, Object> request) {
        logger.info("批量操作服务器 {} 的用户组", serverId);
        
        try {
            String operation = (String) request.get("operation");
            @SuppressWarnings("unchecked")
            List<Long> groupIds = (List<Long>) request.get("groupIds");
            
            if (operation == null || groupIds == null || groupIds.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "无效的批量操作参数");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            int processedCount = 0;
            String message = "";
            
            switch (operation) {
                case "activate":
                    for (Long groupId : groupIds) {
                        userGroupService.setUserGroupActive(groupId, true);
                        processedCount++;
                    }
                    message = "批量启用用户组完成";
                    break;
                    
                case "deactivate":
                    for (Long groupId : groupIds) {
                        userGroupService.setUserGroupActive(groupId, false);
                        processedCount++;
                    }
                    message = "批量禁用用户组完成";
                    break;
                    
                default:
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("error", "不支持的批量操作: " + operation);
                    return ResponseEntity.badRequest().body(errorResponse);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", message);
            response.put("processedCount", processedCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("批量操作用户组失败", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "批量操作失败");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 一键创建默认用户组 - 简化版本
     * 使用固定的配置模板，无需复杂选择
     */
    @PostMapping("/server/{serverId}/create-simple-default")
    public ResponseEntity<?> createSimpleDefaultUserGroup(@PathVariable Long serverId) {
        logger.info("为服务器 {} 创建简化的默认用户组", serverId);
        
        try {
            // 检查服务器是否存在
            Optional<Server> serverOpt = serverService.getServerById(serverId);
            if (serverOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "服务器不存在");
                return ResponseEntity.notFound().build();
            }
            
            // 检查是否已有默认用户组
            Optional<ServerUserGroup> existingDefault = userGroupService.getDefaultUserGroup(serverId);
            if (existingDefault.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "默认用户组已存在");
                response.put("userGroup", userGroupService.convertToDto(existingDefault.get()));
                return ResponseEntity.ok(response);
            }
            
            // 获取简化的默认模板
            ServerUserGroupDto template = templateService.getSimpleDefaultTemplate();
            
            // 创建请求对象
            CreateUserGroupRequest request = new CreateUserGroupRequest();
            request.setGroupName(template.getGroupName());
            request.setGroupDescription(template.getGroupDescription());
            request.setPermissionLevel(template.getPermissionLevel());
            // 将逗号分隔的字符串转换为Set
            if (template.getSystemGroups() != null && !template.getSystemGroups().isEmpty()) {
                Set<String> systemGroupsSet = new HashSet<>(Arrays.asList(template.getSystemGroups().split(",")));
                request.setSystemGroups(systemGroupsSet);
            }
            request.setSudoCommands(template.getSudoCommands());
            request.setIsDefault(true);
            request.setActive(true);
            
            // 创建用户组
            ServerUserGroup group = userGroupService.createUserGroup(request, serverId);
            ServerUserGroupDto groupDto = userGroupService.convertToDto(group);
            
            // 设置为默认用户组
            userGroupService.setDefaultUserGroup(serverId, group.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "默认用户组创建成功，所有新用户将自动加入此组");
            response.put("userGroup", groupDto);
            response.put("template", "simple_default");
            
            logger.info("成功为服务器 {} 创建简化默认用户组: {}", serverId, group.getGroupName());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("创建简化默认用户组失败", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "创建默认用户组失败");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}