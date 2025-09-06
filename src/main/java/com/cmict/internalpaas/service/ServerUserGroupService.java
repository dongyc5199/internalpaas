package com.cmict.internalpaas.service;

import com.cmict.internalpaas.dto.CreateUserGroupRequest;
import com.cmict.internalpaas.dto.ServerUserGroupDto;
import com.cmict.internalpaas.dto.UserGroupSyncResultDto;
import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerUserGroup;
import com.cmict.internalpaas.repository.ServerUserGroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 服务器用户组服务
 * 负责管理服务器用户组的创建、更新、删除和权限配置
 */
@Service
public class ServerUserGroupService {
    
    private static final Logger logger = LoggerFactory.getLogger(ServerUserGroupService.class);
    
    @Autowired
    private ServerUserGroupRepository groupRepository;
    
    @Autowired
    private PermissionTemplateService templateService;
    
    @Autowired
    private ServerService serverService;
    
    @Autowired
    private ServerUserGroupSyncService syncService;
    
    /**
     * 为服务器初始化默认用户组
     * @param server 目标服务器
     * @return 初始化的用户组数量
     */
    @Transactional
    public int initializeDefaultUserGroups(Server server) {
        logger.info("为服务器 {} 初始化默认用户组", server.getName());
        
        try {
            // 检查是否已经有用户组
            List<ServerUserGroup> existingGroups = groupRepository.findByServerId(server.getId());
            if (!existingGroups.isEmpty()) {
                logger.info("服务器 {} 已存在 {} 个用户组，检查是否需要同步到Linux系统", server.getName(), existingGroups.size());
                
                // 同步现有用户组到Linux系统
                try {
                    logger.info("开始同步现有用户组到服务器 {}", server.getName());
                    List<UserGroupSyncResultDto> batchSyncResults = syncService.syncAllUserGroupsToServer(server, existingGroups);
                    
                    int totalSuccess = 0;
                    int totalFailed = 0;
                    
                    for (UserGroupSyncResultDto syncResult : batchSyncResults) {
                        if (syncResult.isSuccess()) {
                            totalSuccess++;
                            logger.info("用户组 {} 同步成功: 成功率={:.1f}%, 步骤={}/{}", 
                                      syncResult.getGroupName(), syncResult.getSuccessRate() * 100,
                                      syncResult.getSuccessfulSteps(), syncResult.getTotalSteps());
                        } else {
                            totalFailed++;
                            logger.error("用户组 {} 同步失败: 错误={}, 成功率={:.1f}%, 步骤={}/{}", 
                                       syncResult.getGroupName(), syncResult.getErrorMessage(),
                                       syncResult.getSuccessRate() * 100,
                                       syncResult.getSuccessfulSteps(), syncResult.getTotalSteps());
                            
                            // 记录详细的同步步骤结果
                            for (UserGroupSyncResultDto.SyncStepResult stepResult : syncResult.getStepResults()) {
                                if (!stepResult.isSuccess()) {
                                    logger.error("同步步骤失败 [{}]: {} -> 退出码:{}, 错误:{}", 
                                               stepResult.getStepName(), stepResult.getCommand(), 
                                               stepResult.getExitCode(), stepResult.getError());
                                }
                            }
                        }
                    }
                    
                    logger.info("批量用户组同步完成: 服务器={}, 成功={}, 失败={}, 总数={}", 
                              server.getName(), totalSuccess, totalFailed, batchSyncResults.size());
                              
                } catch (Exception e) {
                    logger.error("同步现有用户组过程中发生异常: 服务器={}, 异常={}", server.getName(), e.getMessage(), e);
                }
                
                return existingGroups.size();
            }
            
            // 获取适合的权限模板
            List<ServerUserGroupDto> templates = templateService.getDefaultTemplates(
                server.getServerType(), server.getPrivilegeLevel());
            
            if (templates.isEmpty()) {
                logger.warn("服务器 {} (类型:{}, 权限:{}) 无可用权限模板", 
                           server.getName(), server.getServerType(), server.getPrivilegeLevel());
                return 0;
            }
            
            // 创建用户组
            int createdCount = 0;
            String defaultGroupName = null;
            
            for (ServerUserGroupDto template : templates) {
                try {
                    ServerUserGroup group = createUserGroupFromTemplate(server, template);
                    ServerUserGroup saved = groupRepository.save(group);
                    createdCount++;
                    
                    if (template.getIsDefault()) {
                        defaultGroupName = saved.getGroupName();
                    }
                    
                    logger.info("成功创建用户组: {} (ID: {})", saved.getGroupName(), saved.getId());
                } catch (Exception e) {
                    logger.error("创建用户组 {} 失败: {}", template.getGroupName(), e.getMessage(), e);
                }
            }
            
            // 更新服务器的默认用户组
            if (defaultGroupName != null) {
                updateServerDefaultUserGroup(server, defaultGroupName);
            }
            
            logger.info("为服务器 {} 成功初始化 {} 个用户组", server.getName(), createdCount);
            return createdCount;
            
        } catch (Exception e) {
            logger.error("为服务器 {} 初始化用户组时发生异常: {}", server.getName(), e.getMessage(), e);
            throw new RuntimeException("初始化用户组失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 根据模板创建用户组
     */
    private ServerUserGroup createUserGroupFromTemplate(Server server, ServerUserGroupDto template) {
        ServerUserGroup group = new ServerUserGroup();
        group.setServer(server);
        group.setGroupName(template.getGroupName());
        group.setGroupDescription(template.getGroupDescription());
        group.setPermissionLevel(template.getPermissionLevel());
        group.setSystemGroups(String.join(",", template.getSystemGroups()));
        group.getSudoCommands().addAll(template.getSudoCommands());
        group.setIsDefault(template.getIsDefault());
        group.setCreatedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());
        
        return group;
    }
    
    /**
     * 更新服务器默认用户组
     */
    private void updateServerDefaultUserGroup(Server server, String defaultGroupName) {
        try {
            Optional<ServerUserGroup> defaultGroup = groupRepository.findByServerIdAndGroupName(
                server.getId(), defaultGroupName);
            
            if (defaultGroup.isPresent()) {
                server.setDefaultUserGroupId(defaultGroup.get().getId());
                serverService.saveServer(server);
                logger.info("已将服务器 {} 的默认用户组设置为: {}", server.getName(), defaultGroupName);
            }
        } catch (Exception e) {
            logger.error("更新服务器默认用户组失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 创建新的用户组
     * @param request 创建用户组请求
     * @param serverId 服务器ID
     * @return 创建的用户组
     */
    @Transactional
    public ServerUserGroup createUserGroup(CreateUserGroupRequest request, Long serverId) {
        logger.info("为服务器 {} 创建用户组: {}", serverId, request.getGroupName());
        
        try {
            // 检查服务器是否存在
            Optional<Server> serverOpt = serverService.getServerById(serverId);
            if (serverOpt.isEmpty()) {
                throw new RuntimeException("服务器不存在: " + serverId);
            }
            
            Server server = serverOpt.get();
            
            // 检查用户组名是否已存在
            Optional<ServerUserGroup> existing = groupRepository.findByServerIdAndGroupName(
                serverId, request.getGroupName());
            if (existing.isPresent()) {
                throw new RuntimeException("用户组名已存在: " + request.getGroupName());
            }
            
            // 创建用户组
            ServerUserGroup group = new ServerUserGroup();
            group.setServer(server);
            group.setGroupName(request.getGroupName());
            group.setGroupDescription(request.getGroupDescription());
            group.setPermissionLevel(request.getPermissionLevel());
            group.setSystemGroups(request.getSystemGroupsAsString());
            group.getSudoCommands().addAll(request.getSudoCommands());
            group.setIsDefault(request.getIsDefault());
            group.setCreatedAt(LocalDateTime.now());
            group.setUpdatedAt(LocalDateTime.now());
            
            ServerUserGroup saved = groupRepository.save(group);
            
            // 如果设置为默认组，更新服务器配置
            if (request.getIsDefault()) {
                updateDefaultUserGroup(server, saved);
            }
            
            logger.info("成功创建用户组: {} (ID: {})", saved.getGroupName(), saved.getId());
            
            // 同步用户组到服务器系统
            try {
                logger.info("开始同步新创建的用户组到服务器: {} -> {}", saved.getGroupName(), server.getName());
                UserGroupSyncResultDto syncResult = syncService.syncUserGroupToServer(saved);
                
                if (syncResult.isSuccess()) {
                    logger.info("用户组 {} 系统同步成功 (步骤: {}/{}, 成功率: {:.1f}%)", 
                              saved.getGroupName(), syncResult.getSuccessfulSteps(), syncResult.getTotalSteps(), 
                              syncResult.getSuccessRate() * 100);
                } else {
                    logger.error("用户组 {} 系统同步失败: {} (步骤: {}/{}, 成功率: {:.1f}%)", 
                               saved.getGroupName(), syncResult.getErrorMessage(),
                               syncResult.getSuccessfulSteps(), syncResult.getTotalSteps(), 
                               syncResult.getSuccessRate() * 100);
                    
                    // 记录详细的同步步骤结果
                    for (UserGroupSyncResultDto.SyncStepResult stepResult : syncResult.getStepResults()) {
                        if (!stepResult.isSuccess()) {
                            logger.error("同步步骤失败 [{}]: {} -> 退出码:{}, 错误:{}", 
                                       stepResult.getStepName(), stepResult.getCommand(), 
                                       stepResult.getExitCode(), stepResult.getError());
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("用户组 {} 系统同步过程中发生异常: {}", saved.getGroupName(), e.getMessage(), e);
                // 不影响数据库操作的成功，只记录同步失败
            }
            
            return saved;
            
        } catch (Exception e) {
            logger.error("创建用户组失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建用户组失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 更新用户组
     * @param groupId 用户组ID
     * @param request 更新请求
     * @return 更新后的用户组
     */
    @Transactional
    public ServerUserGroup updateUserGroup(Long groupId, CreateUserGroupRequest request) {
        logger.info("更新用户组: {}", groupId);
        
        try {
            Optional<ServerUserGroup> groupOpt = groupRepository.findById(groupId);
            if (groupOpt.isEmpty()) {
                throw new RuntimeException("用户组不存在: " + groupId);
            }
            
            ServerUserGroup group = groupOpt.get();
            
            // 检查用户组名是否与其他组冲突
            if (!group.getGroupName().equals(request.getGroupName())) {
                Optional<ServerUserGroup> existing = groupRepository.findByServerIdAndGroupName(
                    group.getServer().getId(), request.getGroupName());
                if (existing.isPresent() && !existing.get().getId().equals(groupId)) {
                    throw new RuntimeException("用户组名已存在: " + request.getGroupName());
                }
            }
            
            // 更新字段
            group.setGroupName(request.getGroupName());
            group.setGroupDescription(request.getGroupDescription());
            group.setPermissionLevel(request.getPermissionLevel());
            group.setSystemGroups(request.getSystemGroupsAsString());
            group.getSudoCommands().clear();
            group.getSudoCommands().addAll(request.getSudoCommands());
            group.setIsDefault(request.getIsDefault());
            group.setUpdatedAt(LocalDateTime.now());
            
            ServerUserGroup saved = groupRepository.save(group);
            
            // 如果设置为默认组，更新服务器配置
            if (request.getIsDefault()) {
                updateDefaultUserGroup(group.getServer(), saved);
            }
            
            logger.info("成功更新用户组: {} (ID: {})", saved.getGroupName(), saved.getId());
            
            // 同步更新后的用户组到服务器系统
            try {
                logger.info("开始同步更新后的用户组到服务器: {} -> {}", saved.getGroupName(), group.getServer().getName());
                UserGroupSyncResultDto syncResult = syncService.syncUserGroupToServer(saved);
                
                if (syncResult.isSuccess()) {
                    logger.info("用户组 {} 更新同步成功 (步骤: {}/{}, 成功率: {:.1f}%)", 
                              saved.getGroupName(), syncResult.getSuccessfulSteps(), syncResult.getTotalSteps(), 
                              syncResult.getSuccessRate() * 100);
                } else {
                    logger.error("用户组 {} 更新同步失败: {} (步骤: {}/{}, 成功率: {:.1f}%)", 
                               saved.getGroupName(), syncResult.getErrorMessage(),
                               syncResult.getSuccessfulSteps(), syncResult.getTotalSteps(), 
                               syncResult.getSuccessRate() * 100);
                    
                    // 记录详细的同步步骤结果
                    for (UserGroupSyncResultDto.SyncStepResult stepResult : syncResult.getStepResults()) {
                        if (!stepResult.isSuccess()) {
                            logger.error("同步步骤失败 [{}]: {} -> 退出码:{}, 错误:{}", 
                                       stepResult.getStepName(), stepResult.getCommand(), 
                                       stepResult.getExitCode(), stepResult.getError());
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("用户组 {} 更新同步过程中发生异常: {}", saved.getGroupName(), e.getMessage(), e);
                // 不影响数据库操作的成功，只记录同步失败
            }
            
            return saved;
            
        } catch (Exception e) {
            logger.error("更新用户组失败: {}", e.getMessage(), e);
            throw new RuntimeException("更新用户组失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 删除用户组
     * @param groupId 用户组ID
     */
    @Transactional
    public void deleteUserGroup(Long groupId) {
        logger.info("删除用户组: {}", groupId);
        
        try {
            Optional<ServerUserGroup> groupOpt = groupRepository.findById(groupId);
            if (groupOpt.isEmpty()) {
                throw new RuntimeException("用户组不存在: " + groupId);
            }
            
            ServerUserGroup group = groupOpt.get();
            
            // 检查是否为默认用户组
            if (group.getIsDefault()) {
                logger.warn("尝试删除默认用户组: {}", group.getGroupName());
                throw new RuntimeException("不能删除默认用户组");
            }
            
            // 检查是否有用户正在使用此用户组
            // TODO: 实现用户关联检查
            
            String groupName = group.getGroupName();
            Server server = group.getServer();
            
            // 先从服务器系统中删除用户组
            try {
                logger.info("开始从服务器系统删除用户组: {} -> {}", groupName, server.getName());
                UserGroupSyncResultDto syncResult = syncService.removeUserGroupFromServer(server, groupName);
                
                if (syncResult.isSuccess()) {
                    logger.info("用户组 {} 系统删除成功 (步骤: {}/{}, 成功率: {:.1f}%)", 
                              groupName, syncResult.getSuccessfulSteps(), syncResult.getTotalSteps(), 
                              syncResult.getSuccessRate() * 100);
                } else {
                    logger.error("用户组 {} 系统删除失败: {} (步骤: {}/{}, 成功率: {:.1f}%)", 
                               groupName, syncResult.getErrorMessage(),
                               syncResult.getSuccessfulSteps(), syncResult.getTotalSteps(), 
                               syncResult.getSuccessRate() * 100);
                    
                    // 记录详细的同步步骤结果
                    for (UserGroupSyncResultDto.SyncStepResult stepResult : syncResult.getStepResults()) {
                        if (!stepResult.isSuccess()) {
                            logger.error("删除步骤失败 [{}]: {} -> 退出码:{}, 错误:{}", 
                                       stepResult.getStepName(), stepResult.getCommand(), 
                                       stepResult.getExitCode(), stepResult.getError());
                        }
                    }
                    
                    // 系统删除失败时，仍然继续删除数据库记录，但记录警告
                    logger.warn("尽管系统删除失败，仍继续删除数据库中的用户组记录: {}", groupName);
                }
            } catch (Exception e) {
                logger.error("用户组 {} 系统删除过程中发生异常: {}", groupName, e.getMessage(), e);
                // 继续删除数据库记录，但记录异常
                logger.warn("尽管系统删除异常，仍继续删除数据库中的用户组记录: {}", groupName);
            }
            
            // 从数据库中删除用户组
            groupRepository.delete(group);
            logger.info("成功删除用户组: {} (ID: {})", groupName, groupId);
            
        } catch (Exception e) {
            logger.error("删除用户组失败: {}", e.getMessage(), e);
            throw new RuntimeException("删除用户组失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取服务器的所有用户组
     * @param serverId 服务器ID
     * @return 用户组列表
     */
    public List<ServerUserGroup> getServerUserGroups(Long serverId) {
        return groupRepository.findByServerIdOrderByCreatedAtDesc(serverId);
    }
    
    /**
     * 获取服务器的活跃用户组
     * @param serverId 服务器ID
     * @return 活跃用户组列表
     */
    public List<ServerUserGroup> getActiveServerUserGroups(Long serverId) {
        // Return all groups since isActive field is not available
        return groupRepository.findByServerId(serverId);
    }
    
    /**
     * 根据权限级别获取用户组
     * @param serverId 服务器ID
     * @param permissionLevel 权限级别
     * @return 用户组列表
     */
    public List<ServerUserGroup> getUserGroupsByPermissionLevel(Long serverId, 
                                                               ServerUserGroup.PermissionLevel permissionLevel) {
        return groupRepository.findByServerIdAndPermissionLevel(serverId, permissionLevel);
    }
    
    /**
     * 获取默认用户组
     * @param serverId 服务器ID
     * @return 默认用户组
     */
    public Optional<ServerUserGroup> getDefaultUserGroup(Long serverId) {
        return groupRepository.findByServerIdAndIsDefaultTrue(serverId);
    }
    
    /**
     * 启用/禁用用户组
     * @param groupId 用户组ID
     * @param active 是否启用
     */
    @Transactional
    public void setUserGroupActive(Long groupId, boolean active) {
        logger.info("{}用户组: {}", active ? "启用" : "禁用", groupId);
        
        try {
            Optional<ServerUserGroup> groupOpt = groupRepository.findById(groupId);
            if (groupOpt.isEmpty()) {
                throw new RuntimeException("用户组不存在: " + groupId);
            }
            
            ServerUserGroup group = groupOpt.get();
            // Note: isActive field not available in current model
            // This method is a placeholder for future implementation
            group.setUpdatedAt(LocalDateTime.now());
            
            groupRepository.save(group);
            logger.info("成功{}用户组: {}", active ? "启用" : "禁用", group.getGroupName());
            
        } catch (Exception e) {
            logger.error("{}用户组失败: {}", active ? "启用" : "禁用", e.getMessage(), e);
            throw new RuntimeException("操作用户组失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 设置默认用户组
     * @param serverId 服务器ID
     * @param groupId 用户组ID
     */
    @Transactional
    public void setDefaultUserGroup(Long serverId, Long groupId) {
        logger.info("设置服务器 {} 的默认用户组为: {}", serverId, groupId);
        
        try {
            // 取消当前默认用户组
            Optional<ServerUserGroup> currentDefaultOpt = groupRepository.findByServerIdAndIsDefaultTrue(serverId);
            if (currentDefaultOpt.isPresent()) {
                ServerUserGroup currentDefault = currentDefaultOpt.get();
                currentDefault.setIsDefault(false);
                currentDefault.setUpdatedAt(LocalDateTime.now());
                groupRepository.save(currentDefault);
            }
            
            // 设置新的默认用户组
            Optional<ServerUserGroup> newDefaultOpt = groupRepository.findById(groupId);
            if (newDefaultOpt.isEmpty()) {
                throw new RuntimeException("用户组不存在: " + groupId);
            }
            
            ServerUserGroup newDefault = newDefaultOpt.get();
            if (!newDefault.getServer().getId().equals(serverId)) {
                throw new RuntimeException("用户组不属于指定服务器");
            }
            
            newDefault.setIsDefault(true);
            newDefault.setUpdatedAt(LocalDateTime.now());
            ServerUserGroup saved = groupRepository.save(newDefault);
            
            // 更新服务器配置
            updateDefaultUserGroup(newDefault.getServer(), saved);
            
            logger.info("成功设置默认用户组: {}", newDefault.getGroupName());
            
        } catch (Exception e) {
            logger.error("设置默认用户组失败: {}", e.getMessage(), e);
            throw new RuntimeException("设置默认用户组失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 更新默认用户组配置
     */
    private void updateDefaultUserGroup(Server server, ServerUserGroup defaultGroup) {
        try {
            server.setDefaultUserGroupId(defaultGroup.getId());
            serverService.saveServer(server);
        } catch (Exception e) {
            logger.error("更新服务器默认用户组配置失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 将DTO转换为实体对象
     */
    public ServerUserGroupDto convertToDto(ServerUserGroup group) {
        ServerUserGroupDto dto = new ServerUserGroupDto();
        dto.setId(group.getId());
        dto.setServerId(group.getServer().getId());
        dto.setServerName(group.getServer().getName());
        dto.setGroupName(group.getGroupName());
        dto.setGroupDescription(group.getGroupDescription());
        dto.setPermissionLevel(group.getPermissionLevel());
        dto.setIsDefault(group.getIsDefault());
        // dto.setIsActive(true); // Field not available in current DTO
        dto.setCreatedAt(group.getCreatedAt());
        dto.setUpdatedAt(group.getUpdatedAt());
        
        // 解析系统组
        if (group.getSystemGroups() != null && !group.getSystemGroups().trim().isEmpty()) {
            Set<String> systemGroupsSet = new HashSet<>();
            String[] groups = group.getSystemGroups().split(",");
            for (String g : groups) {
                systemGroupsSet.add(g.trim());
            }
            dto.setSystemGroups(String.join(",", systemGroupsSet));
        }
        
        // 获取sudo命令
        dto.setSudoCommands(new HashSet<>(group.getSudoCommands()));
        
        return dto;
    }
    
    /**
     * 批量转换为DTO
     */
    public List<ServerUserGroupDto> convertToDtos(List<ServerUserGroup> groups) {
        return groups.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
    }
}