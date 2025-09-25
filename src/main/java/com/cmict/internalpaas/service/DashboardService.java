package com.cmict.internalpaas.service;

import com.cmict.internalpaas.dto.AdminDashboardDto;
import com.cmict.internalpaas.dto.DeveloperDashboardDto;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.Application;
import com.cmict.internalpaas.repository.UserRepository;
import com.cmict.internalpaas.repository.ServerRepository;
import com.cmict.internalpaas.repository.ApplicationRepository;
import com.cmict.internalpaas.repository.AlertThresholdRepository;
import com.cmict.internalpaas.dto.AggregatedServerMetrics;
import com.cmict.internalpaas.model.AlertThreshold;
import com.cmict.internalpaas.model.ServerMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DashboardService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ServerRepository serverRepository;
    
    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private AlertThresholdRepository alertThresholdRepository;

    @Autowired
    private MonitoringService monitoringService;

    @Autowired
    private SystemHealthService systemHealthService;

    @Autowired
    private UserActivityService userActivityService;

    /**
     * 获取管理员工作台数据
     * @return AdminDashboardDto 管理员工作台数据传输对象
     */
    public AdminDashboardDto getAdminDashboardData() {
        AdminDashboardDto dto = new AdminDashboardDto();
        
        // 服务器统计数据
        List<Server> servers = serverRepository.findAll();
        long totalServers = servers.size();
        long activeServers = servers.stream().filter(Server::getActive).count();
        long inactiveServers = totalServers - activeServers;
        long monitoringServers = servers.stream()
            .filter(server -> server.getConnectionStatus() != null && 
                             server.getConnectionStatus() == Server.ConnectionStatus.MONITORING)
            .count();
        
        dto.setTotalServers(totalServers);
        dto.setActiveServers(activeServers);
        dto.setInactiveServers(inactiveServers);
        dto.setMonitoringServers(monitoringServers);
        
        // 用户统计数据
        List<User> users = userRepository.findAll();
        long totalUsers = users.size();
        long adminUsers = users.stream().filter(user -> 
            user.getRoles().contains(User.Role.ADMIN) || 
            user.getRoles().contains(User.Role.SUPER_ADMIN)).count();
        long regularUsers = users.stream().filter(user -> 
            user.getRoles().contains(User.Role.USER) && 
            !user.getRoles().contains(User.Role.ADMIN) && 
            !user.getRoles().contains(User.Role.SUPER_ADMIN)).count();
        long firstLoginUsers = users.stream().filter(User::getIsFirstLogin).count();
        
        dto.setTotalUsers(totalUsers);
        dto.setAdminUsers(adminUsers);
        dto.setRegularUsers(regularUsers);
        dto.setFirstLoginUsers(firstLoginUsers);
        
        // 应用统计数据
        List<Application> applications = applicationRepository.findAll();
        long totalApplications = applications.size();
        long runningApplications = applications.stream()
            .filter(app -> "RUNNING".equals(app.getStatus())).count();
        long stoppedApplications = applications.stream()
            .filter(app -> "STOPPED".equals(app.getStatus())).count();
        long errorApplications = applications.stream()
            .filter(app -> "ERROR".equals(app.getStatus())).count();

        dto.setTotalApplications(totalApplications);
        dto.setRunningApplications(runningApplications);
        dto.setStoppedApplications(stoppedApplications);
        dto.setErrorApplications(errorApplications);

        // 告警统计数据
        List<AlertThreshold> allThresholds = alertThresholdRepository.findAll();
        List<AlertThreshold> activeThresholds = alertThresholdRepository.findByEnabledTrueOrderByServerIdAscMetricTypeAsc();
        long totalAlerts = allThresholds.size();
        long activeThresholdsCount = activeThresholds.size();

        // 统计触发的告警
        long criticalAlerts = 0;
        long warningAlerts = 0;
        try {
            for (AlertThreshold threshold : activeThresholds) {
                if (isThresholdTriggered(threshold)) {
                    AlertThreshold.AlertLevel level = getTriggeredLevel(threshold);
                    if (level == AlertThreshold.AlertLevel.CRITICAL) {
                        criticalAlerts++;
                    } else if (level == AlertThreshold.AlertLevel.WARNING) {
                        warningAlerts++;
                    }
                }
            }
        } catch (Exception e) {
            // 告警统计出错时使用默认值
        }

        dto.setTotalAlerts(totalAlerts);
        dto.setCriticalAlerts(criticalAlerts);
        dto.setWarningAlerts(warningAlerts);
        dto.setActiveThresholds(activeThresholdsCount);

        // 系统监控指标 - 手动计算聚合数据
        try {
            AggregatedServerMetrics aggregated = calculateAggregatedMetrics();
            if (aggregated != null) {
                dto.setCpuUsage(aggregated.getAvgCpuUsage() != null ? aggregated.getAvgCpuUsage() : 0.0);
                dto.setMemoryUsage(aggregated.getAvgMemoryUsage() != null ? aggregated.getAvgMemoryUsage() : 0.0);
                dto.setDiskUsage(aggregated.getAvgDiskUsage() != null ? aggregated.getAvgDiskUsage() : 0.0);
            } else {
                dto.setCpuUsage(0.0);
                dto.setMemoryUsage(0.0);
                dto.setDiskUsage(0.0);
            }
        } catch (Exception e) {
            // 监控数据获取失败时使用默认值
            dto.setCpuUsage(0.0);
            dto.setMemoryUsage(0.0);
            dto.setDiskUsage(0.0);
        }

        // 系统健康度
        try {
            double healthScore = systemHealthService.calculateSystemHealthScore();
            String healthStatus = systemHealthService.getSystemHealthStatus(healthScore);
            dto.setSystemHealthScore(healthScore);
            dto.setSystemHealthStatus(healthStatus);
        } catch (Exception e) {
            // 健康度计算失败时使用默认值
            dto.setSystemHealthScore(50.0);
            dto.setSystemHealthStatus("unknown");
        }

        // 用户活动数据 - 聚合所有服务器的最近活动
        try {
            List<AdminDashboardDto.ActivityRecord> recentActivities = aggregateUserActivitiesFromAllServers();
            dto.setRecentActivities(recentActivities);
        } catch (Exception e) {
            // 用户活动获取失败时使用空列表
            dto.setRecentActivities(new java.util.ArrayList<>());
        }

        return dto;
    }

    /**
     * 获取研发工作台数据
     * @param username 研发人员用户名
     * @return DeveloperDashboardDto 研发工作台数据传输对象
     */
    public DeveloperDashboardDto getDeveloperDashboardData(String username) {
        DeveloperDashboardDto dto = new DeveloperDashboardDto();
        
        // 获取用户
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (!userOptional.isPresent()) {
            // 如果用户不存在，返回空的统计数据
            dto.setTotalApplications(0);
            dto.setRunningApplications(0);
            dto.setStoppedApplications(0);
            dto.setActiveDebugSessions(0);
            dto.setTotalDebugSessions(0);
            dto.setCpuUsage(0.0);
            dto.setMemoryUsage(0.0);
            return dto;
        }
        
        User user = userOptional.get();
        
        // 应用统计数据 - 使用优化查询避免N+1问题
        List<Application> applications = applicationRepository.findByUserWithUserOrderByCreatedAtDesc(user);
        long totalApplications = applications.size();
        long runningApplications = applications.stream()
            .filter(app -> "RUNNING".equals(app.getStatus())).count();
        long stoppedApplications = totalApplications - runningApplications;
        
        dto.setTotalApplications(totalApplications);
        dto.setRunningApplications(runningApplications);
        dto.setStoppedApplications(stoppedApplications);
        
        // 调试会话统计（模拟数据）
        dto.setActiveDebugSessions(2);
        dto.setTotalDebugSessions(15);
        
        // 资源使用情况（模拟数据）
        dto.setCpuUsage(32.5);
        dto.setMemoryUsage(45.8);
        
        // 生成最近调试记录（模拟数据）
        java.util.List<DeveloperDashboardDto.DebugRecord> records = new java.util.ArrayList<>();
        
        // 基于实际应用数据生成一些活动记录
        if (!applications.isEmpty()) {
            Application app = applications.get(0);
            DeveloperDashboardDto.DebugRecord record1 = new DeveloperDashboardDto.DebugRecord();
            record1.setApplicationName(app.getName());
            record1.setAction("应用启动");
            record1.setStatus("成功");
            record1.setTimestamp("5分钟前");
            records.add(record1);
            
            if (applications.size() > 1) {
                Application app2 = applications.get(1);
                DeveloperDashboardDto.DebugRecord record2 = new DeveloperDashboardDto.DebugRecord();
                record2.setApplicationName(app2.getName());
                record2.setAction("调试会话");
                record2.setStatus("开始");
                record2.setTimestamp("25分钟前");
                records.add(record2);
            }
        }
        
        // 添加一些通用活动记录
        DeveloperDashboardDto.DebugRecord record3 = new DeveloperDashboardDto.DebugRecord();
        record3.setApplicationName("test-app-2.0.jar");
        record3.setAction("历史记录查看");
        record3.setStatus("查看");
        record3.setTimestamp("2小时前");
        records.add(record3);
        
        DeveloperDashboardDto.DebugRecord record4 = new DeveloperDashboardDto.DebugRecord();
        record4.setApplicationName("broken-app-1.0.jar");
        record4.setAction("应用启动");
        record4.setStatus("失败");
        record4.setTimestamp("1天前");
        records.add(record4);
        
        dto.setRecentDebugRecords(records);
        
        return dto;
    }

    /**
     * 获取管理员仪表板总览数据
     * 为AdminDashboardController提供的主要方法
     */
    public AdminDashboardDto getDashboardOverview() {
        return getAdminDashboardData();
    }

    /**
     * 刷新仪表板数据
     * 清除缓存并重新计算
     */
    public void refreshDashboardData() {
        // 这里可以添加缓存清理逻辑
        // 例如清理MonitoringService的缓存
        try {
            // 触发一次数据重新计算
            systemHealthService.calculateSystemHealthScore();
        } catch (Exception e) {
            // 刷新失败时记录日志但不抛出异常
        }
    }

    /**
     * 检查告警阈值是否被触发
     */
    private boolean isThresholdTriggered(AlertThreshold threshold) {
        try {
            // 获取服务器数据
            Optional<Server> serverOpt = serverRepository.findById(threshold.getServerId());
            if (!serverOpt.isPresent()) {
                return false;
            }

            Server server = serverOpt.get();
            ServerMetrics metrics = monitoringService.getServerMetrics(server);
            if (metrics == null) {
                return false;
            }

            // 根据阈值类型获取对应的监控值
            Double value = null;
            switch (threshold.getMetricType()) {
                case CPU:
                    value = metrics.getCpuUsage();
                    break;
                case MEMORY:
                    value = metrics.getMemoryUsage();
                    break;
                case DISK:
                    value = metrics.getDiskUsage();
                    break;
                case LOAD_AVERAGE:
                    value = metrics.getLoadAverageValue();
                    break;
                default:
                    return false;
            }

            return threshold.checkAlert(value) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取触发的告警级别
     */
    private AlertThreshold.AlertLevel getTriggeredLevel(AlertThreshold threshold) {
        try {
            Optional<Server> serverOpt = serverRepository.findById(threshold.getServerId());
            if (!serverOpt.isPresent()) {
                return null;
            }

            Server server = serverOpt.get();
            ServerMetrics metrics = monitoringService.getServerMetrics(server);
            if (metrics == null) {
                return null;
            }

            Double value = null;
            switch (threshold.getMetricType()) {
                case CPU:
                    value = metrics.getCpuUsage();
                    break;
                case MEMORY:
                    value = metrics.getMemoryUsage();
                    break;
                case DISK:
                    value = metrics.getDiskUsage();
                    break;
                case LOAD_AVERAGE:
                    value = metrics.getLoadAverageValue();
                    break;
                default:
                    return null;
            }

            return threshold.checkAlert(value);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 聚合所有服务器的最近用户活动
     */
    private List<AdminDashboardDto.ActivityRecord> aggregateUserActivitiesFromAllServers() {
        List<AdminDashboardDto.ActivityRecord> aggregatedActivities = new java.util.ArrayList<>();

        try {
            // 获取所有服务器
            List<Server> servers = serverRepository.findAll();

            for (Server server : servers) {
                try {
                    // 获取每个服务器的最近活动（限制数量避免数据过多）
                    List<com.cmict.internalpaas.model.UserActivity> serverActivities =
                        userActivityService.getRecentActivities(server.getId());

                    // 转换为ActivityRecord并添加到聚合列表
                    for (com.cmict.internalpaas.model.UserActivity activity : serverActivities) {
                        AdminDashboardDto.ActivityRecord record = convertToActivityRecord(activity, server);
                        if (record != null) {
                            aggregatedActivities.add(record);
                        }
                    }
                } catch (Exception e) {
                    // 个别服务器活动获取失败，跳过继续处理其他服务器
                    continue;
                }
            }

            // 按时间降序排序，取最近的20条活动
            aggregatedActivities.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
            return aggregatedActivities.stream()
                .limit(20)
                .collect(java.util.stream.Collectors.toList());

        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }

    /**
     * 将UserActivity转换为ActivityRecord
     */
    private AdminDashboardDto.ActivityRecord convertToActivityRecord(
            com.cmict.internalpaas.model.UserActivity activity, Server server) {
        try {
            AdminDashboardDto.ActivityRecord record = new AdminDashboardDto.ActivityRecord();

            // 根据ActivityType设置活动类型
            String type = getActivityTypeString(activity.getActivityType());
            record.setType(type);

            // 设置描述信息
            String description = buildActivityDescription(activity, server.getName());
            record.setDescription(description);

            // 设置用户
            record.setUser(activity.getUsername() != null ? activity.getUsername() : "unknown");

            // 设置时间戳 - 使用相对时间格式
            String timestamp = formatRelativeTime(activity.getLastActivity());
            record.setTimestamp(timestamp);

            return record;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取活动类型的字符串表示
     */
    private String getActivityTypeString(com.cmict.internalpaas.model.UserActivity.ActivityType activityType) {
        if (activityType == null) {
            return "系统";
        }

        switch (activityType) {
            case LOGIN:
                return "登录";
            case LOGOUT:
                return "登出";
            case COMMAND_EXECUTE:
                return "命令";
            case FILE_TRANSFER:
                return "文件";
            case PROCESS_START:
                return "启动";
            case PROCESS_STOP:
                return "停止";
            case FILE_EDIT:
                return "编辑";
            case DIRECTORY_CHANGE:
                return "目录";
            case SYSTEM_INFO:
                return "信息";
            default:
                return "操作";
        }
    }

    /**
     * 构建活动描述信息
     */
    private String buildActivityDescription(com.cmict.internalpaas.model.UserActivity activity, String serverName) {
        StringBuilder description = new StringBuilder();

        // 基础描述
        if (activity.getActivityType() != null) {
            description.append(activity.getActivityType().getDescription());
        } else {
            description.append("系统操作");
        }

        // 添加服务器信息
        description.append(" - ").append(serverName);

        // 如果有详细信息，添加部分详细信息（截断长文本）
        if (activity.getActivityDetails() != null && !activity.getActivityDetails().trim().isEmpty()) {
            String details = activity.getActivityDetails().trim();
            if (details.length() > 50) {
                details = details.substring(0, 50) + "...";
            }
            description.append(" (").append(details).append(")");
        }

        return description.toString();
    }

    /**
     * 格式化相对时间
     */
    private String formatRelativeTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return "未知时间";
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        long minutes = java.time.Duration.between(dateTime, now).toMinutes();

        if (minutes < 1) {
            return "刚刚";
        } else if (minutes < 60) {
            return minutes + "分钟前";
        } else if (minutes < 1440) { // 24小时
            long hours = minutes / 60;
            return hours + "小时前";
        } else {
            long days = minutes / 1440;
            if (days == 1) {
                return "1天前";
            } else if (days < 7) {
                return days + "天前";
            } else {
                // 超过7天显示具体日期
                return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm"));
            }
        }
    }

    /**
     * 计算聚合的服务器监控指标
     */
    private AggregatedServerMetrics calculateAggregatedMetrics() {
        try {
            List<Server> servers = serverRepository.findAll();
            if (servers.isEmpty()) {
                return null;
            }

            double totalCpu = 0.0;
            double totalMemory = 0.0;
            double totalDisk = 0.0;
            int validCount = 0;

            for (Server server : servers) {
                try {
                    ServerMetrics metrics = monitoringService.getLatestMetrics(server.getId());
                    if (metrics != null) {
                        if (metrics.getCpuUsage() != null) {
                            totalCpu += metrics.getCpuUsage();
                        }
                        if (metrics.getMemoryUsage() != null) {
                            totalMemory += metrics.getMemoryUsage();
                        }
                        if (metrics.getDiskUsage() != null) {
                            totalDisk += metrics.getDiskUsage();
                        }
                        validCount++;
                    }
                } catch (Exception e) {
                    // 个别服务器指标获取失败，继续处理其他服务器
                    continue;
                }
            }

            if (validCount == 0) {
                return null;
            }

            AggregatedServerMetrics aggregated = new AggregatedServerMetrics();
            aggregated.setAvgCpuUsage(totalCpu / validCount);
            aggregated.setAvgMemoryUsage(totalMemory / validCount);
            aggregated.setAvgDiskUsage(totalDisk / validCount);
            aggregated.setDataPointCount((long) validCount);

            return aggregated;
        } catch (Exception e) {
            return null;
        }
    }
}