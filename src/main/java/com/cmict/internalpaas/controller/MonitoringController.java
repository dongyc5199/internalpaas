package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerMetrics;
import com.cmict.internalpaas.model.UserActivity;
import com.cmict.internalpaas.service.MonitoringService;
import com.cmict.internalpaas.service.MonitoringSchedulerService;
import com.cmict.internalpaas.service.ServerService;
import com.cmict.internalpaas.service.UserActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/monitoring")
public class MonitoringController {

    @Autowired
    private MonitoringService monitoringService;
    
    @Autowired
    private ServerService serverService;
    
    @Autowired
    private UserActivityService userActivityService;
    
    @Autowired
    private MonitoringSchedulerService schedulerService;
    
    /**
     * 监控主页面 - 重定向到服务器管理页面
     */
    @GetMapping("/dashboard")
    public String dashboard() {
        return "redirect:/admin/servers";
    }
    
    /**
     * 服务器详细监控页面
     */
    @GetMapping("/server/{id}")
    public String serverDetails(@PathVariable Long id, Model model) {
        try {
            Server server = serverService.findById(id).orElse(null);
            if (server == null) {
                model.addAttribute("error", "服务器不存在");
                return "monitoring/server-details";
            }
            
            model.addAttribute("server", server);
            
            // 获取最新监控数据
            ServerMetrics latestMetrics = serverService.getServerLatestMetrics(id);
            model.addAttribute("metrics", latestMetrics);
            
            // 获取用户活跃信息
            UserActivityService.UserActivitySummary activitySummary = userActivityService.getUserActivitySummary(id);
            model.addAttribute("activitySummary", activitySummary);
            
            return "monitoring/server-details";
        } catch (Exception e) {
            model.addAttribute("error", "加载服务器详情失败: " + e.getMessage());
            return "monitoring/server-details";
        }
    }
    
    /**
     * 获取服务器最新监控数据
     */
    @GetMapping("/server/{id}/metrics")
    @ResponseBody
    public ResponseEntity<?> getServerMetrics(@PathVariable Long id) {
        try {
            // 日志记录
            System.out.println("请求监控数据 - 服务器ID: " + id);
            
            // 首先检查服务器是否存在
            if (!serverService.findById(id).isPresent()) {
                System.out.println("服务器不存在 - ID: " + id);
                Map<String, String> error = new HashMap<>();
                error.put("error", "服务器不存在");
                error.put("serverId", String.valueOf(id));
                return ResponseEntity.status(404).body(error);
            }
            
            ServerMetrics metrics = serverService.getServerLatestMetrics(id);
            System.out.println("获取监控数据结果 - metrics: " + (metrics != null ? "有数据" : "无数据"));
            
            if (metrics == null) {
                // 如果没有数据，尝试刷新
                System.out.println("尝试刷新服务器监控数据 - ID: " + id);
                metrics = serverService.refreshServerMetrics(id);
            }
            
            if (metrics == null) {
                System.out.println("监控数据为空 - ID: " + id);
                Map<String, String> error = new HashMap<>();
                error.put("error", "监控数据不可用");
                error.put("serverId", String.valueOf(id));
                return ResponseEntity.status(404).body(error);
            }
            
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            System.err.println("获取监控数据异常 - ID: " + id + ", 错误: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("serverId", String.valueOf(id));
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 刷新服务器监控数据
     */
    @PostMapping("/server/{id}/refresh")
    @ResponseBody
    public ResponseEntity<ServerMetrics> refreshServerMetrics(@PathVariable Long id) {
        try {
            ServerMetrics metrics = serverService.refreshServerMetrics(id);
            if (metrics == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取所有服务器指标
     */
    @GetMapping("/servers/metrics")
    @ResponseBody
    public ResponseEntity<Map<String, ServerMetrics>> getAllServersMetrics() {
        try {
            List<Server> servers = serverService.getActiveServers();
            Map<String, ServerMetrics> metricsMap = new HashMap<>();
            
            for (Server server : servers) {
                try {
                    ServerMetrics metrics = serverService.getServerLatestMetrics(server.getId());
                    if (metrics != null) {
                        metricsMap.put(server.getName(), metrics);
                    }
                } catch (Exception e) {
                    // 单个服务器失败不影响其他服务器
                }
            }
            
            return ResponseEntity.ok(metricsMap);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 检查服务器健康状态
     */
    @GetMapping("/server/{id}/health")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getServerHealth(@PathVariable Long id) {
        try {
            Server server = serverService.findById(id).orElse(null);
            if (server == null) {
                return ResponseEntity.notFound().build();
            }
            
            ServerMetrics metrics = serverService.getServerLatestMetrics(id);
            Map<String, Object> health = new HashMap<>();
            
            health.put("serverName", server.getName());
            health.put("hostname", server.getHostname());
            health.put("connectionStatus", server.getConnectionStatus());
            health.put("status", "UP");
            
            if (metrics != null) {
                health.put("metrics", metrics);
                
                // 简单健康检查逻辑
                if (metrics.getCpuUsage() != null && metrics.getCpuUsage() > 90) {
                    health.put("status", "WARNING");
                    health.put("message", "CPU使用率过高");
                } else if (metrics.getMemoryUsage() != null && metrics.getMemoryUsage() > 90) {
                    health.put("status", "WARNING");
                    health.put("message", "内存使用率过高");
                } else if (metrics.getDiskUsage() != null && metrics.getDiskUsage() > 90) {
                    health.put("status", "WARNING");
                    health.put("message", "磁盘使用率过高");
                }
            } else {
                health.put("status", "DOWN");
                health.put("message", "无监控数据");
            }
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "DOWN");
            error.put("error", e.getMessage());
            return ResponseEntity.ok(error);
        }
    }
    
    /**
     * 获取服务器用户活跃信息
     */
    @GetMapping("/server/{id}/users")
    @ResponseBody
    public ResponseEntity<List<UserActivity>> getServerActiveUsers(@PathVariable Long id) {
        try {
            List<UserActivity> activeUsers = userActivityService.getActiveUsers(id);
            return ResponseEntity.ok(activeUsers);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取服务器用户活动摘要
     */
    @GetMapping("/server/{id}/activity-summary")
    @ResponseBody
    public ResponseEntity<UserActivityService.UserActivitySummary> getServerActivitySummary(@PathVariable Long id) {
        try {
            UserActivityService.UserActivitySummary summary = userActivityService.getUserActivitySummary(id);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 用户活动监控页面
     */
    @GetMapping("/user-activity")
    public String userActivity(Model model) {
        try {
            List<Server> servers = serverService.getActiveServers();
            model.addAttribute("servers", servers);
            
            // 获取所有服务器的用户活动摘要
            Map<Long, UserActivityService.UserActivitySummary> activitySummaries = new HashMap<>();
            Map<Long, List<UserActivity>> activeUsersMap = new HashMap<>();
            
            for (Server server : servers) {
                try {
                    UserActivityService.UserActivitySummary summary = userActivityService.getUserActivitySummary(server.getId());
                    activitySummaries.put(server.getId(), summary);
                    
                    List<UserActivity> activeUsers = userActivityService.getActiveUsers(server.getId());
                    activeUsersMap.put(server.getId(), activeUsers);
                } catch (Exception e) {
                    // 单个服务器失败不影响其他服务器
                }
            }
            
            model.addAttribute("activitySummaries", activitySummaries);
            model.addAttribute("activeUsersMap", activeUsersMap);
            
            return "monitoring/user-activity";
        } catch (Exception e) {
            model.addAttribute("error", "加载用户活动数据失败: " + e.getMessage());
            return "monitoring/user-activity";
        }
    }

    /**
     * 手动触发全量健康检查
     */
    @PostMapping("/trigger-health-check")
    @ResponseBody
    public ResponseEntity<Map<String, String>> triggerHealthCheck() {
        try {
            schedulerService.triggerFullHealthCheck();
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "健康检查已触发");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}