package com.cmict.internalpaas.service;

import com.cmict.internalpaas.dto.ServerUserGroupDto;
import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerUserGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 权限模板服务
 * 提供预定义的权限模板和适配性配置策略
 */
@Service
public class PermissionTemplateService {
    
    private static final Logger logger = LoggerFactory.getLogger(PermissionTemplateService.class);
    
    /**
     * 获取服务器类型的默认权限模板
     * @param serverType 服务器类型
     * @param privilegeLevel 权限级别
     * @return 权限模板列表
     */
    public List<ServerUserGroupDto> getDefaultTemplates(Server.ServerType serverType, Server.PrivilegeLevel privilegeLevel) {
        logger.info("获取服务器类型 {} 权限级别 {} 的默认模板", serverType, privilegeLevel);
        
        List<ServerUserGroupDto> templates = new ArrayList<>();
        
        switch (serverType) {
            case DEVELOPMENT:
                templates = getDevelopmentTemplates(privilegeLevel);
                break;
            case TESTING:
                templates = getTestingTemplates(privilegeLevel);
                break;
            case STAGING:
                templates = getStagingTemplates(privilegeLevel);
                break;
            case PRODUCTION:
                templates = getProductionTemplates(privilegeLevel);
                break;
            default:
                logger.warn("未知的服务器类型: {}", serverType);
                templates = getBasicTemplates(privilegeLevel);
        }
        
        logger.info("为服务器类型 {} 生成了 {} 个权限模板", serverType, templates.size());
        return templates;
    }
    
    /**
     * 获取开发环境权限模板
     */
    private List<ServerUserGroupDto> getDevelopmentTemplates(Server.PrivilegeLevel privilegeLevel) {
        List<ServerUserGroupDto> templates = new ArrayList<>();
        
        // 开发管理员组 - 完整权限
        if (canCreateFullAdminGroup(privilegeLevel)) {
            ServerUserGroupDto devAdmin = new ServerUserGroupDto();
            devAdmin.setGroupName("dev_admin");
            devAdmin.setGroupDescription("开发环境管理员组，拥有完整权限");
            devAdmin.setPermissionLevel(ServerUserGroup.PermissionLevel.ADMIN);
            
            Set<String> adminGroups = new HashSet<>();
            adminGroups.add("sudo");
            adminGroups.add("wheel");
            adminGroups.add("docker");
            adminGroups.add("systemd-journal");
            devAdmin.setSystemGroups(String.join(",", adminGroups));
            
            Set<String> adminCommands = new HashSet<>();
            adminCommands.add("ALL");
            devAdmin.setSudoCommands(adminCommands);
            devAdmin.setIsDefault(false);
            templates.add(devAdmin);
        }
        
        // 开发者组 - 开发相关权限
        if (canCreateDeveloperGroup(privilegeLevel)) {
            ServerUserGroupDto developer = new ServerUserGroupDto();
            developer.setGroupName("developer");
            developer.setGroupDescription("开发者组，拥有开发相关权限");
            developer.setPermissionLevel(ServerUserGroup.PermissionLevel.DEVELOPER);
            
            Set<String> devGroups = new HashSet<>();
            devGroups.add("docker");
            devGroups.add("systemd-journal");
            developer.setSystemGroups(String.join(",", devGroups));
            
            Set<String> devCommands = new HashSet<>();
            devCommands.add("/bin/systemctl start *");
            devCommands.add("/bin/systemctl stop *");
            devCommands.add("/bin/systemctl restart *");
            devCommands.add("/usr/bin/docker *");
            devCommands.add("/usr/local/bin/docker-compose *");
            developer.setSudoCommands(devCommands);
            developer.setIsDefault(true);
            templates.add(developer);
        }
        
        // 测试用户组 - 基础权限
        ServerUserGroupDto tester = new ServerUserGroupDto();
        tester.setGroupName("tester");
        tester.setGroupDescription("测试用户组，拥有基础访问权限");
        tester.setPermissionLevel(ServerUserGroup.PermissionLevel.READONLY);
        
        Set<String> testGroups = new HashSet<>();
        testGroups.add("users");
        tester.setSystemGroups(String.join(",", testGroups));
        tester.setSudoCommands(new HashSet<>());
        tester.setIsDefault(false);
        templates.add(tester);
        
        return templates;
    }
    
    /**
     * 获取测试环境权限模板
     */
    private List<ServerUserGroupDto> getTestingTemplates(Server.PrivilegeLevel privilegeLevel) {
        List<ServerUserGroupDto> templates = new ArrayList<>();
        
        // 基础用户组
        ServerUserGroupDto basicUser = new ServerUserGroupDto();
        basicUser.setGroupName("basic_user");
        basicUser.setGroupDescription("基础用户组");
        basicUser.setPermissionLevel(ServerUserGroup.PermissionLevel.READONLY);
        
        Set<String> basicGroups = new HashSet<>();
        basicGroups.add("users");
        basicUser.setSystemGroups(String.join(",", basicGroups));
        basicUser.setSudoCommands(new HashSet<>());
        basicUser.setIsDefault(true);
        templates.add(basicUser);
        
        return templates;
    }
    
    /**
     * 获取预发布环境权限模板
     */
    private List<ServerUserGroupDto> getStagingTemplates(Server.PrivilegeLevel privilegeLevel) {
        List<ServerUserGroupDto> templates = new ArrayList<>();
        
        // 基础用户组
        ServerUserGroupDto basicUser = new ServerUserGroupDto();
        basicUser.setGroupName("basic_user");
        basicUser.setGroupDescription("基础用户组");
        basicUser.setPermissionLevel(ServerUserGroup.PermissionLevel.READONLY);
        
        Set<String> basicGroups = new HashSet<>();
        basicGroups.add("users");
        basicUser.setSystemGroups(String.join(",", basicGroups));
        basicUser.setSudoCommands(new HashSet<>());
        basicUser.setIsDefault(true);
        templates.add(basicUser);
        
        return templates;
    }
    
    /**
     * 获取生产环境权限模板
     */
    private List<ServerUserGroupDto> getProductionTemplates(Server.PrivilegeLevel privilegeLevel) {
        List<ServerUserGroupDto> templates = new ArrayList<>();
        
        // 基础用户组
        ServerUserGroupDto basicUser = new ServerUserGroupDto();
        basicUser.setGroupName("basic_user");
        basicUser.setGroupDescription("基础用户组");
        basicUser.setPermissionLevel(ServerUserGroup.PermissionLevel.READONLY);
        
        Set<String> basicGroups = new HashSet<>();
        basicGroups.add("users");
        basicUser.setSystemGroups(String.join(",", basicGroups));
        basicUser.setSudoCommands(new HashSet<>());
        basicUser.setIsDefault(true);
        templates.add(basicUser);
        
        return templates;
    }
    
    /**
     * 获取基础权限模板（当服务器类型未知时使用）
     */
    private List<ServerUserGroupDto> getBasicTemplates(Server.PrivilegeLevel privilegeLevel) {
        List<ServerUserGroupDto> templates = new ArrayList<>();
        
        // 基础用户组
        ServerUserGroupDto basicUser = new ServerUserGroupDto();
        basicUser.setGroupName("basic_user");
        basicUser.setGroupDescription("基础用户组");
        basicUser.setPermissionLevel(ServerUserGroup.PermissionLevel.READONLY);
        
        Set<String> basicGroups = new HashSet<>();
        basicGroups.add("users");
        basicUser.setSystemGroups(String.join(",", basicGroups));
        basicUser.setSudoCommands(new HashSet<>());
        basicUser.setIsDefault(true);
        templates.add(basicUser);
        
        return templates;
    }
    
    /**
     * 检查是否可以创建完整管理员组
     */
    private boolean canCreateFullAdminGroup(Server.PrivilegeLevel privilegeLevel) {
        return privilegeLevel == Server.PrivilegeLevel.ROOT_ACCESS || 
               privilegeLevel == Server.PrivilegeLevel.SUDO_FULL ||
               privilegeLevel == Server.PrivilegeLevel.UNKNOWN; // 为UNKNOWN级别也创建基本管理组
    }
    
    /**
     * 检查是否可以创建开发者组
     */
    private boolean canCreateDeveloperGroup(Server.PrivilegeLevel privilegeLevel) {
        return privilegeLevel == Server.PrivilegeLevel.ROOT_ACCESS || 
               privilegeLevel == Server.PrivilegeLevel.SUDO_FULL ||
               privilegeLevel == Server.PrivilegeLevel.SUDO_LIMITED ||
               privilegeLevel == Server.PrivilegeLevel.UNKNOWN; // 为UNKNOWN级别也创建基本开发组
    }
    
    /**
     * 获取推荐的默认用户组名称
     * @param serverType 服务器类型
     * @param privilegeLevel 权限级别
     * @return 默认用户组名称
     */
    public String getRecommendedDefaultGroupName(Server.ServerType serverType, Server.PrivilegeLevel privilegeLevel) {
        if (privilegeLevel == Server.PrivilegeLevel.NO_ACCESS) {
            return null;
        }
        
        // 对于UNKNOWN权限级别，提供基本的默认组
        if (privilegeLevel == Server.PrivilegeLevel.UNKNOWN) {
            return "basic_user"; // 为未知权限级别提供基本用户组
        }
        
        switch (serverType) {
            case DEVELOPMENT:
                return "developer";
            case TESTING:
            case STAGING:
            case PRODUCTION:
            default:
                return "basic_user";
        }
    }
    
    /**
     * 获取简化的默认用户组模板 - 固定配置
     * 这是一个简化版本，适合大多数场景使用
     * @return 默认用户组配置
     */
    public ServerUserGroupDto getSimpleDefaultTemplate() {
        logger.info("获取简化的默认用户组模板");
        
        ServerUserGroupDto template = new ServerUserGroupDto();
        template.setGroupName("default_users");
        template.setGroupDescription("默认用户组 - 适用于所有用户的标准权限配置");
        template.setPermissionLevel(ServerUserGroup.PermissionLevel.DEVELOPER);
        
        // 固定的系统组配置
        Set<String> systemGroups = new HashSet<>();
        systemGroups.add("users");
        systemGroups.add("docker");
        systemGroups.add("systemd-journal");
        template.setSystemGroups(String.join(",", systemGroups));
        
        // 固定的sudo命令配置
        Set<String> sudoCommands = new HashSet<>();
        sudoCommands.add("/bin/systemctl start *");
        sudoCommands.add("/bin/systemctl stop *");
        sudoCommands.add("/bin/systemctl restart *");
        sudoCommands.add("/usr/bin/docker *");
        sudoCommands.add("/usr/local/bin/docker-compose *");
        sudoCommands.add("/bin/ps *");
        sudoCommands.add("/bin/netstat *");
        sudoCommands.add("/usr/bin/tail *");
        template.setSudoCommands(sudoCommands);
        
        template.setIsDefault(true);
        template.setActive(true);
        
        logger.info("生成简化默认用户组模板: {}", template.getGroupName());
        return template;
    }
    
    /**
     * 检查服务器是否已有默认用户组，如果没有则创建
     * @param serverId 服务器ID
     * @return 是否需要创建默认组
     */
    public boolean shouldCreateDefaultGroup(Long serverId) {
        // 这里应该检查数据库中是否已存在默认用户组
        // 为了简化，暂时返回true，实际使用时应该查询数据库
        return true;
    }
}