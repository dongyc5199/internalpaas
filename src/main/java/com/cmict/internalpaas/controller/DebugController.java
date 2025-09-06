package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.repository.UserRepository;
import com.cmict.internalpaas.repository.ServerMetricsRepository;
import com.cmict.internalpaas.service.MonitoringHistoryService;
import com.cmict.internalpaas.service.UserService;
import com.cmict.internalpaas.service.ServerService;
import com.cmict.internalpaas.service.ServerUserGroupService;
import com.cmict.internalpaas.dto.UserProfileDto;
import com.cmict.internalpaas.dto.UserPreferencesDto;
import com.cmict.internalpaas.model.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
public class DebugController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private ServerMetricsRepository metricsRepository;
    
    @Autowired
    private MonitoringHistoryService monitoringHistoryService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ServerService serverService;
    
    @Autowired
    private ServerUserGroupService userGroupService;

    @GetMapping("/debug/check-root")
    public String checkRootUser() {
        return userRepository.findByUsername("root")
            .map(user -> {
                boolean passwordMatches = passwordEncoder.matches("admin123", user.getPassword());
                return String.format("Root用户存在: %s, 密码正确: %s, 角色: %s", 
                    user.getUsername(), 
                    passwordMatches, 
                    user.getRoles());
            })
            .orElse("Root用户不存在");
    }
    
    @GetMapping("/debug/test-aggregated-metrics")
    public String testAggregatedMetrics() {
        try {
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusHours(24);
            
            Object[] hourlyData = metricsRepository.findHourlyAggregatedMetrics(1L, startTime, endTime);
            Object[] dailyData = metricsRepository.findDailyAggregatedMetrics(1L, startTime, endTime);
            
            StringBuilder result = new StringBuilder();
            result.append("测试时间范围: ").append(startTime).append(" 到 ").append(endTime).append("\n\n");
            
            result.append("每小时聚合数据:\n");
            if (hourlyData != null) {
                result.append("外层数组长度: ").append(hourlyData.length).append("\n");
                result.append("类型: ").append(hourlyData.getClass().getSimpleName()).append("\n");
                
                if (hourlyData.length > 0) {
                    result.append("第一个元素类型: ").append(hourlyData[0].getClass().getSimpleName()).append("\n");
                    
                    // 如果第一个元素是数组，展开显示
                    if (hourlyData[0] instanceof Object[]) {
                        Object[] innerArray = (Object[]) hourlyData[0];
                        result.append("内层数组长度: ").append(innerArray.length).append("\n");
                        for (int i = 0; i < innerArray.length && i < 15; i++) {
                            result.append("    内层[").append(i).append("]: ").append(innerArray[i]).append("\n");
                        }
                    } else {
                        // 如果不是数组，直接显示各个元素
                        for (int i = 0; i < hourlyData.length && i < 15; i++) {
                            result.append("  [").append(i).append("]: ").append(hourlyData[i]).append("\n");
                        }
                    }
                }
            } else {
                result.append("null\n");
            }
            
            result.append("\n每日聚合数据:\n");
            if (dailyData != null) {
                result.append("外层数组长度: ").append(dailyData.length).append("\n");
                result.append("类型: ").append(dailyData.getClass().getSimpleName()).append("\n");
                
                if (dailyData.length > 0) {
                    result.append("第一个元素类型: ").append(dailyData[0].getClass().getSimpleName()).append("\n");
                    
                    // 如果第一个元素是数组，展开显示
                    if (dailyData[0] instanceof Object[]) {
                        Object[] innerArray = (Object[]) dailyData[0];
                        result.append("内层数组长度: ").append(innerArray.length).append("\n");
                        for (int i = 0; i < innerArray.length && i < 15; i++) {
                            result.append("    内层[").append(i).append("]: ").append(innerArray[i]).append("\n");
                        }
                    } else {
                        // 如果不是数组，直接显示各个元素
                        for (int i = 0; i < dailyData.length && i < 15; i++) {
                            result.append("  [").append(i).append("]: ").append(dailyData[i]).append("\n");
                        }
                    }
                }
            } else {
                result.append("null\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            return "错误: " + e.getMessage() + "\n堆栈: " + Arrays.toString(e.getStackTrace());
        }
    }
    
    @GetMapping("/debug/test-monitoring-service")
    public String testMonitoringService() {
        try {
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusHours(1);
            
            // 测试聚合数据调用
            Map<String, Object> response = monitoringHistoryService
                .getServerHistoryData(1L, startTime, endTime, true, "hour");
            
            return "监控服务测试成功!\n" +
                   "响应键: " + response.keySet() + "\n" +
                   "数据类型: " + response.get("dataType") + "\n" +
                   "聚合类型: " + response.get("aggregationType") + "\n" +
                   "成功: " + response.get("success");
        } catch (Exception e) {
            return "监控服务测试失败: " + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace());
        }
    }
    
    @GetMapping("/debug/test-user-profile")
    public String testUserProfile() {
        try {
            String username = "root";
            
            // 测试getUserPreferences方法
            UserPreferencesDto preferences = userService.getUserPreferences(username);
            
            return "用户偏好测试成功!\n" +
                   "主题: " + preferences.getTheme() + "\n" +
                   "语言: " + preferences.getLanguage();
        } catch (Exception e) {
            return "用户偏好测试失败: " + e.getMessage();
        }
    }
    
    @GetMapping("/debug/check-server-status")
    public String checkServerStatus() {
        try {
            List<Server> servers = serverService.getAllServers();
            StringBuilder result = new StringBuilder();
            result.append("服务器状态检查:\n\n");
            
            for (Server server : servers) {
                result.append("服务器: ").append(server.getName()).append("\n");
                result.append("  ID: ").append(server.getId()).append("\n");
                result.append("  active字段: ").append(server.getActive()).append("\n");
                result.append("  connectionStatus: ").append(server.getConnectionStatus()).append("\n");
                result.append("  hostname: ").append(server.getHostname()).append("\n");
                result.append("  最后连接检查: ").append(server.getLastConnectionCheck()).append("\n\n");
            }
            
            // 计算统计
            long totalServers = servers.size();
            long activeServersBasedOnConnection = servers.stream()
                .filter(server -> server.getConnectionStatus() == Server.ConnectionStatus.CONNECTED || 
                                 server.getConnectionStatus() == Server.ConnectionStatus.MONITORING)
                .count();
            long activeServersBasedOnFlag = servers.stream()
                .filter(Server::getActive)
                .count();
            
            result.append("统计数据:\n");
            result.append("总服务器数: ").append(totalServers).append("\n");
            result.append("基于connectionStatus的在线数: ").append(activeServersBasedOnConnection).append("\n");
            result.append("基于active字段的在线数: ").append(activeServersBasedOnFlag).append("\n");
            
            return result.toString();
        } catch (Exception e) {
            return "检查服务器状态失败: " + e.getMessage();
        }
    }
    
    @GetMapping("/debug/check-user-groups")
    public String checkUserGroups() {
        try {
            List<Server> servers = serverService.getAllServers();
            StringBuilder result = new StringBuilder();
            result.append("用户组状态检查:\n\n");
            
            for (Server server : servers) {
                result.append("服务器: ").append(server.getName()).append(" (ID: ").append(server.getId()).append(")\n");
                
                // 获取用户组列表
                var userGroups = userGroupService.getServerUserGroups(server.getId());
                result.append("  用户组数量: ").append(userGroups.size()).append("\n");
                
                if (userGroups.isEmpty()) {
                    result.append("  状态: 无用户组\n");
                    
                    // 尝试初始化默认用户组
                    result.append("  尝试初始化默认用户组...\n");
                    try {
                        int createdCount = userGroupService.initializeDefaultUserGroups(server);
                        result.append("  初始化结果: 创建了 ").append(createdCount).append(" 个用户组\n");
                    } catch (Exception e) {
                        result.append("  初始化失败: ").append(e.getMessage()).append("\n");
                    }
                } else {
                    result.append("  用户组详情:\n");
                    for (var group : userGroups) {
                        result.append("    - ").append(group.getGroupName())
                              .append(" (权限: ").append(group.getPermissionLevel())
                              .append(", 默认: ").append(group.getIsDefault()).append(")\n");
                    }
                }
                result.append("\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            return "检查用户组状态失败: " + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace());
        }
    }
}