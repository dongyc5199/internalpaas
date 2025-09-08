package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerStatusTag;
import com.cmict.internalpaas.model.ServerStatusTag.TagType;
import com.cmict.internalpaas.model.ServerStatusTag.TagStatus;
import com.cmict.internalpaas.model.UserActivity;
import com.cmict.internalpaas.model.ServerMetrics;
import com.cmict.internalpaas.repository.ServerStatusTagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ServerStatusTagService {
    
    private static final Logger logger = LoggerFactory.getLogger(ServerStatusTagService.class);
    
    @Autowired
    private ServerStatusTagRepository tagRepository;
    
    @Autowired
    private ServerService serverService;
    
    @Autowired
    private UserActivityService userActivityService;
    
    @Autowired
    private SshConnectionService sshConnectionService;
    
    @Autowired
    private RemoteCommandService remoteCommandService;
    
    @Autowired
    private MonitoringService monitoringService;

    /**
     * 获取服务器的所有状态标签，按优先级排序
     */
    public List<ServerStatusTag> getServerStatusTags(Long serverId) {
        return tagRepository.findByServerIdOrderByPriorityDesc(serverId);
    }

    /**
     * 获取服务器的告警级别标签
     */
    public List<ServerStatusTag> getServerAlertTags(Long serverId) {
        return tagRepository.findAlertTagsByServerId(serverId);
    }

    /**
     * 创建或更新状态标签
     */
    public ServerStatusTag createOrUpdateTag(Long serverId, TagType tagType, TagStatus status, 
                                           String displayText, String details) {
        return createOrUpdateTag(serverId, tagType, status, displayText, null, null, 0, details);
    }

    /**
     * 创建或更新状态标签（完整参数）
     */
    public ServerStatusTag createOrUpdateTag(Long serverId, TagType tagType, TagStatus status,
                                           String displayText, String value, String colorScheme, 
                                           Integer priority, String details) {
        
        Optional<ServerStatusTag> existingTag = tagRepository.findByServerIdAndTagType(serverId, tagType);
        
        ServerStatusTag tag;
        if (existingTag.isPresent()) {
            tag = existingTag.get();
            // 更新现有标签
            tag.setStatus(status);
            tag.setDisplayText(displayText);
            tag.setValue(value);
            tag.setDetails(details);
            tag.setLastUpdated(LocalDateTime.now());
        } else {
            // 创建新标签
            tag = new ServerStatusTag(serverId, tagType, status, displayText);
            tag.setValue(value);
            tag.setDetails(details);
        }
        
        // 设置颜色方案
        if (colorScheme != null) {
            tag.setColorScheme(colorScheme);
        } else {
            tag.setColorScheme(getDefaultColorScheme(status));
        }
        
        // 设置优先级
        if (priority != null && priority > 0) {
            tag.setPriority(priority);
        } else {
            tag.setPriority(getDefaultPriority(status, tagType));
        }
        
        return tagRepository.save(tag);
    }

    /**
     * 刷新服务器的所有状态标签
     */
    public void refreshAllServerTags(Long serverId) {
        logger.debug("开始刷新服务器 {} 的状态标签", serverId);
        
        try {
            // 删除监控异常标签
            deleteTagByType(serverId, TagType.MONITORING);
            
            // 检查连接状态
            checkConnectionStatus(serverId);
            
            // 检查工作目录状态
            checkWorkDirectoryStatus(serverId);
            
            // 检查活跃用户状态
            checkActiveUsersStatus(serverId);
            
            // 跳过监控状态检查，不再创建监控相关标签
            // checkMonitoringStatus(serverId);
            
            // 检查权限状态
            checkPermissionStatus(serverId);
            
            // 检查系统资源状态（内存和磁盘80%阈值）
            checkSystemResourceStatus(serverId);
            
            logger.debug("服务器 {} 状态标签刷新完成", serverId);
            
        } catch (Exception e) {
            logger.error("刷新服务器 {} 状态标签失败: {}", serverId, e.getMessage(), e);
        }
    }

    /**
     * 检查连接状态
     */
    public ServerStatusTag checkConnectionStatus(Long serverId) {
        try {
            Optional<Server> serverOpt = serverService.findById(serverId);
            if (!serverOpt.isPresent()) {
                // 服务器不存在的情况，仍然显示错误标签
                return createOrUpdateTag(serverId, TagType.CONNECTION, TagStatus.ERROR,
                    "服务器不存在", "服务器配置未找到");
            }
            
            Server server = serverOpt.get();
            Server.ConnectionStatus connectionStatus = sshConnectionService.checkConnection(server);
            boolean isConnected = connectionStatus == Server.ConnectionStatus.CONNECTED;
            
            // 删除已存在的连接状态标签，避免与页面原有的在线/离线状态重复显示
            deleteTagByType(serverId, TagType.CONNECTION);
            
            // 连接正常时不创建标签，保留servers.html中的原有在线/离线显示
            // 只有连接异常时才显示错误标签
            if (!isConnected) {
                return createOrUpdateTag(serverId, TagType.CONNECTION, TagStatus.ERROR,
                    "连接异常", null, "danger", 9, "SSH连接失败，请检查网络或认证配置");
            }
            
            return null; // 连接正常时不显示标签
            
        } catch (Exception e) {
            logger.error("检查服务器 {} 连接状态失败: {}", serverId, e.getMessage());
            return createOrUpdateTag(serverId, TagType.CONNECTION, TagStatus.UNKNOWN,
                "状态未知", null, "secondary", 1, "连接检查异常: " + e.getMessage());
        }
    }

    /**
     * 检查工作目录状态
     */
    public ServerStatusTag checkWorkDirectoryStatus(Long serverId) {
        try {
            Optional<Server> serverOpt = serverService.findById(serverId);
            if (!serverOpt.isPresent()) {
                return createOrUpdateTag(serverId, TagType.WORK_DIRECTORY, TagStatus.ERROR,
                    "服务器不存在", "服务器配置未找到");
            }
            
            Server server = serverOpt.get();
            if (server.getBaseWorkDirectory() == null) {
                return createOrUpdateTag(serverId, TagType.WORK_DIRECTORY, TagStatus.ERROR,
                    "目录未配置", "服务器工作目录未配置");
            }
            
            String workDir = server.getBaseWorkDirectory();
            
            // 检查目录是否存在
            boolean exists = remoteCommandService.executeCommand(server, 
                "test -d \"" + workDir + "\"").getExitCode() == 0;
                
            if (!exists) {
                return createOrUpdateTag(serverId, TagType.WORK_DIRECTORY, TagStatus.ERROR,
                    "目录不存在", null, "danger", 7, "工作目录 " + workDir + " 不存在");
            }
            
            // 检查是否可写
            boolean writable = remoteCommandService.executeCommand(server,
                "test -w \"" + workDir + "\"").getExitCode() == 0;
                
            if (writable) {
                // 目录正常可用时，删除已存在的标签，不显示任何标签
                deleteTagByType(serverId, TagType.WORK_DIRECTORY);
                return null;
            } else {
                return createOrUpdateTag(serverId, TagType.WORK_DIRECTORY, TagStatus.WARNING,
                    "目录只读", workDir, "warning", 6, "工作目录只读，可能影响应用部署");
            }
            
        } catch (Exception e) {
            logger.error("检查服务器 {} 工作目录状态失败: {}", serverId, e.getMessage());
            return createOrUpdateTag(serverId, TagType.WORK_DIRECTORY, TagStatus.UNKNOWN,
                "检查失败", null, "secondary", 1, "目录状态检查异常");
        }
    }

    /**
     * 检查活跃用户状态
     */
    public ServerStatusTag checkActiveUsersStatus(Long serverId) {
        try {
            int activeUserCount = userActivityService.getActiveUserCount(serverId);
            int activeSessionCount = userActivityService.getActiveSessionCount(serverId);
            
            // 只在没有活跃用户时显示异常标签，有活跃用户时不显示任何标签
            if (activeUserCount == 0) {
                return createOrUpdateTag(serverId, TagType.ACTIVE_USERS, TagStatus.INACTIVE,
                    "无活跃用户", "0", "secondary", 1, "当前没有活跃用户");
            } else {
                // 有活跃用户时，删除已存在的标签，不显示任何标签
                deleteTagByType(serverId, TagType.ACTIVE_USERS);
                return null;
            }
                
        } catch (Exception e) {
            logger.error("检查服务器 {} 活跃用户状态失败: {}", serverId, e.getMessage());
            return createOrUpdateTag(serverId, TagType.ACTIVE_USERS, TagStatus.UNKNOWN,
                "用户状态未知", null, "secondary", 1, "用户活动检查异常");
        }
    }

    /**
     * 检查监控状态
     */
    public ServerStatusTag checkMonitoringStatus(Long serverId) {
        try {
            Optional<Server> serverOpt = serverService.findById(serverId);
            if (!serverOpt.isPresent()) {
                return createOrUpdateTag(serverId, TagType.MONITORING, TagStatus.ERROR,
                    "配置错误", "服务器配置不存在");
            }
            
            Server server = serverOpt.get();
            
            boolean monitoringEnabled = server.getAutoMonitorEnabled() != null && server.getAutoMonitorEnabled();
            LocalDateTime lastMetricsUpdate = server.getLastMetricsUpdate();
            
            if (!monitoringEnabled) {
                return createOrUpdateTag(serverId, TagType.MONITORING, TagStatus.INACTIVE,
                    "监控已禁用", null, "secondary", 1, "自动监控功能已禁用");
            }
            
            if (lastMetricsUpdate == null) {
                return createOrUpdateTag(serverId, TagType.MONITORING, TagStatus.WARNING,
                    "监控未启动", null, "warning", 4, "监控已启用但未收到指标数据");
            }
            
            // 检查指标数据是否过期（超过5分钟）
            LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
            if (lastMetricsUpdate.isBefore(fiveMinutesAgo)) {
                return createOrUpdateTag(serverId, TagType.MONITORING, TagStatus.WARNING,
                    "监控异常", null, "warning", 5, "指标数据已过期，最后更新: " + lastMetricsUpdate);
            }
            
            return createOrUpdateTag(serverId, TagType.MONITORING, TagStatus.ACTIVE,
                "监控正常", null, "success", 2, "监控服务运行正常");
                
        } catch (Exception e) {
            logger.error("检查服务器 {} 监控状态失败: {}", serverId, e.getMessage());
            return createOrUpdateTag(serverId, TagType.MONITORING, TagStatus.UNKNOWN,
                "监控状态未知", null, "secondary", 1, "监控状态检查异常");
        }
    }

    /**
     * 检查系统资源状态（内存和磁盘）
     * 当内存或磁盘超过80%时显示"系统资源紧张"标签
     */
    public ServerStatusTag checkSystemResourceStatus(Long serverId) {
        try {
            Optional<Server> serverOpt = serverService.findById(serverId);
            if (!serverOpt.isPresent()) {
                return createOrUpdateTag(serverId, TagType.SYSTEM_RESOURCE, TagStatus.ERROR,
                    "配置错误", "服务器配置不存在");
            }
            
            // 获取最新的服务器监控数据
            ServerMetrics latestMetrics = getLatestServerMetrics(serverId);
            if (latestMetrics == null) {
                return createOrUpdateTag(serverId, TagType.SYSTEM_RESOURCE, TagStatus.UNKNOWN,
                    "资源状态未知", null, "secondary", 1, "无法获取系统资源监控数据");
            }
            
            double memoryUsage = latestMetrics.getMemoryUsagePercent();
            double diskUsage = latestMetrics.getDiskUsagePercent();
            
            // 内存和磁盘使用超过80%时触发告警
            boolean memoryAlert = memoryUsage > 80.0;
            boolean diskAlert = diskUsage > 80.0;
            
            // 如果内存或磁盘超过80%，显示"系统资源紧张"标签
            if (memoryAlert || diskAlert) {
                // 删除单独的内存和磁盘标签，避免重复显示
                deleteTagByType(serverId, TagType.MEMORY_USAGE);
                deleteTagByType(serverId, TagType.DISK_USAGE);
                
                // 只显示"系统资源紧张"，不显示具体数量
                return createOrUpdateTag(serverId, TagType.SYSTEM_RESOURCE, TagStatus.CRITICAL,
                    "系统资源紧张", "", "danger", 8, "系统资源使用率过高");
            }
            
            // 资源正常时删除已存在的系统资源标签
            deleteTagByType(serverId, TagType.SYSTEM_RESOURCE);
            return null;
            
        } catch (Exception e) {
            logger.error("检查服务器 {} 系统资源状态失败: {}", serverId, e.getMessage(), e);
            return createOrUpdateTag(serverId, TagType.SYSTEM_RESOURCE, TagStatus.ERROR,
                "资源检查失败", null, "danger", 1, "系统资源状态检查异常");
        }
    }
    
    /**
     * 获取服务器最新监控指标
     */
    private ServerMetrics getLatestServerMetrics(Long serverId) {
        try {
            // 调用监控服务获取最新监控数据
            return monitoringService.getLatestMetrics(serverId);
        } catch (Exception e) {
            logger.error("获取服务器 {} 监控指标失败: {}", serverId, e.getMessage());
            return null;
        }
    }

    /**
     * 检查权限状态
     */
    public ServerStatusTag checkPermissionStatus(Long serverId) {
        try {
            Optional<Server> serverOpt = serverService.findById(serverId);
            if (!serverOpt.isPresent()) {
                return createOrUpdateTag(serverId, TagType.PERMISSION, TagStatus.ERROR,
                    "配置错误", "服务器配置不存在");
            }
            
            Server server = serverOpt.get();
            
            Server.PrivilegeLevel privilegeLevel = server.getPrivilegeLevel();
            if (privilegeLevel == null || privilegeLevel == Server.PrivilegeLevel.UNKNOWN) {
                return createOrUpdateTag(serverId, TagType.PERMISSION, TagStatus.UNKNOWN,
                    "权限未知", null, "secondary", 1, "服务器权限级别未检测");
            }
            
            String displayText = privilegeLevel.getDescription();
            
            // 只在权限不足或有问题时显示标签，正常权限不显示
            switch (privilegeLevel) {
                case ROOT_ACCESS:
                case SUDO_FULL:
                    // 权限正常时，删除已存在的标签，不显示任何标签
                    deleteTagByType(serverId, TagType.PERMISSION);
                    return null;
                case SUDO_LIMITED:
                    return createOrUpdateTag(serverId, TagType.PERMISSION, TagStatus.WARNING,
                        displayText, privilegeLevel.name(), "info", 2, "权限受限，部分功能可能不可用");
                case USER_ONLY:
                    return createOrUpdateTag(serverId, TagType.PERMISSION, TagStatus.WARNING,
                        displayText, privilegeLevel.name(), "warning", 3, "仅用户权限，管理功能受限");
                case NO_ACCESS:
                    return createOrUpdateTag(serverId, TagType.PERMISSION, TagStatus.ERROR,
                        displayText, privilegeLevel.name(), "danger", 8, "无访问权限，功能严重受限");
                default:
                    return createOrUpdateTag(serverId, TagType.PERMISSION, TagStatus.UNKNOWN,
                        displayText, privilegeLevel.name(), "secondary", 1, "未知权限级别");
            }
                
        } catch (Exception e) {
            logger.error("检查服务器 {} 权限状态失败: {}", serverId, e.getMessage());
            return createOrUpdateTag(serverId, TagType.PERMISSION, TagStatus.UNKNOWN,
                "权限检查失败", null, "secondary", 1, "权限状态检查异常");
        }
    }

    /**
     * 删除过期的标签
     */
    public void deleteObsoleteTags(Long serverId) {
        LocalDateTime expiredTime = LocalDateTime.now().minusHours(1); // 1小时前的标签视为过期
        List<ServerStatusTag> expiredTags = tagRepository.findByLastUpdatedBefore(expiredTime);
        
        expiredTags.stream()
            .filter(tag -> tag.getServerId().equals(serverId))
            .forEach(tag -> {
                logger.debug("删除过期标签: {} - {}", tag.getTagType(), tag.getDisplayText());
                tagRepository.delete(tag);
            });
    }

    /**
     * 删除指定类型的标签
     */
    public void deleteTagByType(Long serverId, TagType tagType) {
        tagRepository.deleteByServerIdAndTagType(serverId, tagType);
    }

    /**
     * 删除所有服务器的监控异常标签
     */
    public void deleteAllMonitoringTags() {
        try {
            tagRepository.deleteByTagType(TagType.MONITORING);
            logger.info("已删除所有监控异常标签");
        } catch (Exception e) {
            logger.error("删除监控异常标签失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 统计服务器告警数量
     */
    public long countAlertTags(Long serverId) {
        return tagRepository.countAlertTagsByServerId(serverId);
    }

    /**
     * 获取高优先级标签（用于移动端显示）
     */
    public List<ServerStatusTag> getHighPriorityTags(Long serverId, int minPriority) {
        return tagRepository.findHighPriorityTagsByServerId(serverId, minPriority);
    }

    // 私有辅助方法

    /**
     * 获取状态对应的默认颜色方案
     */
    private String getDefaultColorScheme(TagStatus status) {
        switch (status) {
            case ACTIVE: return "success";
            case WARNING: return "warning";
            case CRITICAL:
            case DANGER:
            case ERROR: return "danger";
            case INACTIVE: return "secondary";
            case MAINTENANCE: return "info";
            default: return "secondary";
        }
    }

    /**
     * 获取状态和类型对应的默认优先级
     */
    private Integer getDefaultPriority(TagStatus status, TagType tagType) {
        // 状态优先级基数
        int statusPriority;
        switch (status) {
            case DANGER: statusPriority = 10; break;
            case ERROR: statusPriority = 9; break;
            case CRITICAL: statusPriority = 8; break;
            case WARNING: statusPriority = 6; break;
            case MAINTENANCE: statusPriority = 4; break;
            case ACTIVE: statusPriority = 2; break;
            case INACTIVE:
            case UNKNOWN:
            default: statusPriority = 1; break;
        }
        
        // 标签类型调整
        switch (tagType) {
            case CONNECTION:
            case MEMORY_USAGE:
            case CPU_USAGE:
            case SYSTEM_RESOURCE:
                // 连接和资源状态更重要
                return statusPriority + 1;
            case WORK_DIRECTORY:
            case MONITORING:
                return statusPriority;
            case ACTIVE_USERS:
            case PERMISSION:
            default:
                return Math.max(1, statusPriority - 1);
        }
    }
}