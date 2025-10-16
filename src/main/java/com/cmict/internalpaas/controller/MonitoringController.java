package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.client.MetricsHubClient;
import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerMetrics;
import com.cmict.internalpaas.model.UserActivity;
import com.cmict.internalpaas.service.MonitoringSchedulerService;
import com.cmict.internalpaas.service.MonitoringService;
import com.cmict.internalpaas.service.ServerService;
import com.cmict.internalpaas.service.UserActivityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/monitoring")
public class MonitoringController {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringController.class);

    @Autowired
    private ServerService serverService;

    @Autowired
    private UserActivityService userActivityService;

    @Autowired
    private MonitoringSchedulerService schedulerService;

    @Autowired
    private MonitoringService monitoringService;

    /**
     * Metrics Hub客户端（可选依赖，Hub未启动时不报错）
     */
    @Autowired(required = false)
    private MetricsHubClient metricsHubClient;

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
     * 获取服务器最新监控数据（智能路由版本：Hub优先，SSH降级）
     *
     * 数据源选择策略:
     * 1. 优先尝试从Metrics Hub读取（准实时，10-15秒延迟）
     * 2. Hub不可用时降级到SSH轮询（传统方式，1-5分钟延迟）
     * 3. 对用户完全透明，无需前端改动
     */
    @GetMapping("/server/{id}/metrics")
    @ResponseBody
    public ResponseEntity<?> getServerMetrics(@PathVariable Long id) {
        try {
            logger.debug("请求监控数据 - 服务器ID: {}", id);

            // 首先检查服务器是否存在
            if (!serverService.findById(id).isPresent()) {
                logger.warn("服务器不存在 - ID: {}", id);
                Map<String, String> error = new HashMap<>();
                error.put("error", "服务器不存在");
                error.put("serverId", String.valueOf(id));
                return ResponseEntity.status(404).body(error);
            }

            // ========== 第一优先级: 尝试从Metrics Hub读取 ==========
            if (metricsHubClient != null && metricsHubClient.isAvailable()) {
                try {
                    logger.debug("尝试从Metrics Hub读取数据 - serverId: {}", id);
                    ServerMetrics hubMetrics = metricsHubClient.getLatestMetrics(id);

                    if (hubMetrics != null) {
                        logger.info("✅ 数据来源: Metrics Hub (准实时) - serverId: {}", id);

                        // 添加数据源标记
                        Map<String, Object> response = new HashMap<>();
                        response.put("dataSource", "hub");
                        response.put("metrics", hubMetrics);
                        response.put("timestamp", System.currentTimeMillis());

                        return ResponseEntity.ok(response);
                    } else {
                        logger.warn("Hub返回空数据，降级到SSH - serverId: {}", id);
                    }

                } catch (Exception e) {
                    logger.warn("Hub查询失败，降级到SSH - serverId: {}, error: {}", id, e.getMessage());
                }
            } else {
                logger.debug("Metrics Hub不可用或未配置，使用SSH监控 - serverId: {}", id);
            }

            // ========== 第二优先级: 降级到SSH轮询（现有逻辑） ==========
            logger.info("⚠️ 数据来源: SSH轮询（降级） - serverId: {}", id);

            ServerMetrics metrics = serverService.getServerLatestMetrics(id);
            logger.debug("SSH监控数据结果 - metrics: {}", (metrics != null ? "有数据" : "无数据"));

            if (metrics == null) {
                // 如果没有数据，尝试刷新
                logger.debug("尝试刷新SSH监控数据 - serverId: {}", id);
                metrics = serverService.refreshServerMetrics(id);
            }

            if (metrics == null) {
                logger.warn("SSH监控数据为空 - serverId: {}", id);
                Map<String, String> error = new HashMap<>();
                error.put("error", "监控数据不可用");
                error.put("serverId", String.valueOf(id));
                return ResponseEntity.status(404).body(error);
            }

            // 添加数据源标记
            Map<String, Object> response = new HashMap<>();
            response.put("dataSource", "ssh");
            response.put("metrics", metrics);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取监控数据异常 - serverId: {}, error: {}", id, e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("serverId", String.valueOf(id));
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * T4 API: 获取服务器监控数据（支持时间范围查询和字段选择）
     *
     * @param id 服务器ID
     * @param from 开始时间（Unix毫秒时间戳，可选）
     * @param to 结束时间（Unix毫秒时间戳，可选）
     * @param step 降采样间隔（秒，可选）
     * @param fields 字段列表（逗号分隔，如: cpu,memory,disk，可选）
     * @return 监控数据列表
     */
    @GetMapping("/server/{id}/metrics/query")
    @ResponseBody
    public ResponseEntity<?> queryServerMetrics(
            @PathVariable Long id,
            @RequestParam(required = false) Long from,
            @RequestParam(required = false) Long to,
            @RequestParam(required = false) Integer step,
            @RequestParam(required = false) String fields) {
        try {
            // 检查服务器是否存在
            if (!serverService.findById(id).isPresent()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "服务器不存在");
                error.put("serverId", String.valueOf(id));
                return ResponseEntity.status(404).body(error);
            }

            // 获取监控数据（使用新的查询服务）
            List<ServerMetrics> metrics = monitoringService.queryMetrics(id, from, to, step);

            // 应用字段过滤
            if (fields != null && !fields.trim().isEmpty()) {
                metrics = monitoringService.filterMetricsByFields(metrics, fields);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("serverId", id);
            response.put("from", from);
            response.put("to", to);
            response.put("step", step);
            response.put("fields", fields);
            response.put("count", metrics.size());
            response.put("data", metrics);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("查询监控数据异常 - ID: " + id + ", 错误: " + e.getMessage());
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
     * 获取所有服务器指标（智能路由版本：Hub优先，SSH降级）
     */
    @GetMapping("/servers/metrics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAllServersMetrics() {
        try {
            List<Server> servers = serverService.getActiveServers();
            Map<String, ServerMetrics> metricsMap = new HashMap<>();
            int hubCount = 0;
            int sshCount = 0;

            for (Server server : servers) {
                try {
                    ServerMetrics metrics = null;

                    // 尝试从Hub获取
                    if (metricsHubClient != null && metricsHubClient.isAvailable()) {
                        try {
                            metrics = metricsHubClient.getLatestMetrics(server.getId());
                            if (metrics != null) {
                                hubCount++;
                                logger.debug("服务器 {} 数据来源: Hub", server.getName());
                            }
                        } catch (Exception e) {
                            logger.debug("服务器 {} Hub查询失败: {}", server.getName(), e.getMessage());
                        }
                    }

                    // 降级到SSH
                    if (metrics == null) {
                        metrics = serverService.getServerLatestMetrics(server.getId());
                        if (metrics != null) {
                            sshCount++;
                            logger.debug("服务器 {} 数据来源: SSH", server.getName());
                        }
                    }

                    if (metrics != null) {
                        metricsMap.put(server.getName(), metrics);
                    }

                } catch (Exception e) {
                    // 单个服务器失败不影响其他服务器
                    logger.error("获取服务器 {} 监控数据失败", server.getName(), e);
                }
            }

            // 添加数据源统计
            Map<String, Object> response = new HashMap<>();
            response.put("metrics", metricsMap);
            response.put("dataSourceStats", Map.of(
                    "hub", hubCount,
                    "ssh", sshCount,
                    "total", metricsMap.size()
            ));
            response.put("timestamp", System.currentTimeMillis());

            logger.info("批量查询完成 - Hub: {}, SSH: {}, 总计: {}", hubCount, sshCount, metricsMap.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("批量获取监控数据异常", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * T4 API: 批量获取所有服务器的监控数据（支持字段选择）
     *
     * @param fields 字段列表（逗号分隔，如: cpu,memory,disk，可选）
     * @return 所有服务器的最新监控数据
     */
    @GetMapping("/servers/metrics/query")
    @ResponseBody
    public ResponseEntity<?> queryAllServersMetrics(@RequestParam(required = false) String fields) {
        try {
            List<Server> servers = serverService.getActiveServers();
            Map<String, Object> results = new LinkedHashMap<>();
            List<Map<String, Object>> serverMetrics = new ArrayList<>();

            for (Server server : servers) {
                try {
                    ServerMetrics metrics = serverService.getServerLatestMetrics(server.getId());

                    if (metrics != null) {
                        // 应用字段过滤
                        if (fields != null && !fields.trim().isEmpty()) {
                            List<ServerMetrics> metricsList = Collections.singletonList(metrics);
                            metricsList = monitoringService.filterMetricsByFields(metricsList, fields);
                            if (!metricsList.isEmpty()) {
                                metrics = metricsList.get(0);
                            }
                        }

                        Map<String, Object> serverData = new HashMap<>();
                        serverData.put("serverId", server.getId());
                        serverData.put("serverName", server.getName());
                        serverData.put("hostname", server.getHostname());
                        serverData.put("metrics", metrics);

                        serverMetrics.add(serverData);
                    }
                } catch (Exception e) {
                    // 单个服务器失败不影响其他服务器
                    logger.error("获取服务器 {} 监控数据失败: {}", server.getId(), e.getMessage());
                }
            }

            results.put("count", serverMetrics.size());
            results.put("fields", fields);
            results.put("servers", serverMetrics);

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
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