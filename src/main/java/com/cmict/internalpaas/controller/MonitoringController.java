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
     * 获取服务器最新监控数据 (仅使用 Metrics Hub)
     * 
     * ⚠️ 阶段4迁移: SSH降级逻辑已移除
     * 
     * 数据源: Metrics Hub (OTLP Agent → Hub API)
     * - 采样间隔: 10秒
     * - 延迟: < 30秒
     * 协议: gRPC/HTTP + Protobuf
     * 
     * 如需恢复SSH降级,请参考: docs/archives/2024-phase4/数据存储迁移实施方案.md
     * 
     * @param id 服务器ID
     * @return 监控数据 (Hub数据源) 或 503/404 错误
     */
    @GetMapping("/server/{id}/metrics")
    @ResponseBody
    public ResponseEntity<?> getServerMetrics(@PathVariable Long id) {
        try {
            logger.debug("请求监控数据 - 服务器ID: {}", id);

            // 检查服务器是否存在
            if (!serverService.findById(id).isPresent()) {
                logger.warn("服务器不存在 - ID: {}", id);
                Map<String, Object> error = new HashMap<>();
                error.put("error", "服务器不存在");
                error.put("serverId", id);
                error.put("hint", "请检查服务器ID是否正确");
                return ResponseEntity.status(404).body(error);
            }

            // 检查 Hub 是否可用
            if (metricsHubClient == null || !metricsHubClient.isAvailable()) {
                logger.error("❌ Metrics Hub 不可用 - serverId: {}", id);
                Map<String, Object> error = new HashMap<>();
                error.put("error", "监控服务暂时不可用");
                error.put("hint", "请确保 Metrics Hub 服务正常运行");
                error.put("serverId", id);
                error.put("helpUrl", "/docs/archives/2024-phase4/数据存储迁移实施方案.md");
                return ResponseEntity.status(503).body(error);
            }

            // 从 Hub 读取数据
            logger.debug("从 Metrics Hub 读取数据 - serverId: {}", id);
            ServerMetrics metrics = metricsHubClient.getLatestMetrics(id);

            if (metrics == null) {
                logger.warn("Hub 返回空数据 - serverId: {}", id);
                Map<String, Object> error = new HashMap<>();
                error.put("error", "暂无监控数据");
                error.put("hint", "请确保 OTLP Agent 已部署并正常运行");
                error.put("serverId", id);
                error.put("deploymentGuide", "/admin/servers (自动部署Agent)");
                return ResponseEntity.status(404).body(error);
            }

            logger.info("✅ 数据来源: Metrics Hub - serverId: {}", id);

            // 返回数据
            Map<String, Object> response = new HashMap<>();
            response.put("dataSource", "hub");
            response.put("metrics", metrics);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取监控数据异常 - serverId: {}, error: {}", id, e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("serverId", id);
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
     * 获取所有服务器指标 (仅使用 Metrics Hub)
     * 
     * ⚠️ 阶段4迁移: SSH降级逻辑已移除
     * 
     * @return 所有服务器的监控数据 (Hub数据源)
     */
    @GetMapping("/servers/metrics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAllServersMetrics() {
        try {
            List<Server> servers = serverService.getActiveServers();
            Map<String, ServerMetrics> metricsMap = new HashMap<>();
            int hubCount = 0;
            int errorCount = 0;

            // 检查 Hub 是否可用
            boolean hubAvailable = metricsHubClient != null && metricsHubClient.isAvailable();

            if (!hubAvailable) {
                logger.warn("⚠️ Metrics Hub 不可用，无法获取服务器指标");
                Map<String, Object> error = new HashMap<>();
                error.put("error", "监控服务暂时不可用");
                error.put("hint", "请检查 Metrics Hub 服务状态");
                error.put("hubStatus", "unavailable");
                return ResponseEntity.status(503).body(error);
            }

            // 从 Hub 批量获取所有服务器数据
            for (Server server : servers) {
                try {
                    ServerMetrics metrics = metricsHubClient.getLatestMetrics(server.getId());
                    if (metrics != null) {
                        metricsMap.put(server.getName(), metrics);
                        hubCount++;
                        logger.debug("服务器 {} 数据来源: Hub ✅", server.getName());
                    } else {
                        errorCount++;
                        logger.debug("服务器 {} 暂无 Hub 数据 ⚠️", server.getName());
                    }
                } catch (Exception e) {
                    errorCount++;
                    logger.error("获取服务器 {} Hub数据失败: {}", server.getName(), e.getMessage());
                }
            }

            logger.info("批量获取指标完成 - Hub成功: {}, 错误: {}, 总数: {}", 
                hubCount, errorCount, servers.size());

            // 返回数据
            Map<String, Object> response = new HashMap<>();
            response.put("metrics", metricsMap);
            response.put("dataSource", "hub");
            response.put("timestamp", System.currentTimeMillis());
            response.put("statistics", Map.of(
                "total", servers.size(),
                "hubCount", hubCount,
                "errorCount", errorCount
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("批量获取监控数据异常: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Step 5: 统一Hub查询端点 (优化版)
     * 直接代理Hub API,减少数据转换开销
     * 
     * 用途: 前端直接查询服务器的历史监控数据
     * 优势: 
     * - 减少一层Service调用
     * - 无数据模型转换开销
     * - 支持Hub完整查询能力 (时间范围、降采样、字段过滤)
     * 
     * @param id 服务器ID
     * @param from 起始时间戳(毫秒,可选)
     * @param to 结束时间戳(毫秒,可选)
     * @param step 降采样间隔(秒,可选)
     * @param fields 字段过滤(逗号分隔,如:cpu,memory,可选)
     * @return Hub原始响应
     */
    @GetMapping("/api/server/{id}/hub/query")
    @ResponseBody
    public ResponseEntity<?> proxyHubQuery(
            @PathVariable Long id,
            @RequestParam(required = false) Long from,
            @RequestParam(required = false) Long to,
            @RequestParam(required = false) Integer step,
            @RequestParam(required = false) String fields) {
        
        try {
            // 1. 权限检查: 验证服务器是否存在
            Optional<Server> serverOpt = serverService.getServerById(id);
            if (serverOpt.isEmpty()) {
                logger.warn("服务器 {} 不存在", id);
                return ResponseEntity.status(404).body(Map.of(
                    "error", "服务器不存在",
                    "code", "SERVER_NOT_FOUND",
                    "serverId", id
                ));
            }
            
            Server server = serverOpt.get();
            
            // 2. Hub可用性检查
            if (metricsHubClient == null || !metricsHubClient.isAvailable()) {
                logger.warn("Hub服务不可用,无法查询服务器 {} 的监控数据", id);
                return ResponseEntity.status(503).body(Map.of(
                    "error", "Hub服务不可用,请稍后重试",
                    "code", "HUB_UNAVAILABLE",
                    "serverId", id
                ));
            }
            
            // 3. 直接调用Hub查询 (使用queryMetricsRaw方法)
            logger.debug("代理Hub查询: serverId={}, from={}, to={}, step={}, fields={}", 
                id, from, to, step, fields);
            
            // 使用 queryMetricsRaw() 直接返回Hub响应
            ResponseEntity<Map<String, Object>> hubResponse = 
                    metricsHubClient.queryMetricsRaw(id, from, to, step, fields);
            
            // 直接返回Hub的响应,不做任何转换
            return hubResponse;
            
        } catch (Exception e) {
            logger.error("Hub查询失败: serverId={}, error={}", id, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Hub查询失败: " + e.getMessage(),
                "code", "HUB_QUERY_FAILED",
                "serverId", id
            ));
        }
    }

    /**
     * Step 5: 批量服务器Hub查询 (优化版)
     * 并发查询多台服务器的监控数据
     * 
     * @param request 请求体,包含: serverIds(必须), from(可选), to(可选)
     * @return 所有服务器的监控数据
     */
    @PostMapping("/api/servers/hub/batch-query")
    @ResponseBody
    public ResponseEntity<?> proxyHubBatchQuery(@RequestBody Map<String, Object> request) {
        try {
            // 1. 解析请求参数
            @SuppressWarnings("unchecked")
            List<Number> serverIdNumbers = (List<Number>) request.get("serverIds");
            if (serverIdNumbers == null || serverIdNumbers.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "serverIds参数必须提供",
                    "code", "INVALID_REQUEST"
                ));
            }
            
            List<Long> serverIds = new ArrayList<>();
            for (Number num : serverIdNumbers) {
                serverIds.add(num.longValue());
            }
            
            // 2. 批量权限检查 - 验证所有服务器是否存在
            List<Server> servers = new ArrayList<>();
            for (Long serverId : serverIds) {
                Optional<Server> serverOpt = serverService.getServerById(serverId);
                if (serverOpt.isPresent()) {
                    servers.add(serverOpt.get());
                } else {
                    logger.warn("服务器 {} 不存在", serverId);
                }
            }
            
            if (servers.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "所有服务器均不存在或无权限访问",
                    "code", "NO_VALID_SERVERS"
                ));
            }
            
            if (servers.size() != serverIds.size()) {
                logger.warn("部分服务器不存在或无权限访问,请求数量:{}, 有效数量:{}", 
                    serverIds.size(), servers.size());
            }
            
            // 3. Hub可用性检查
            if (metricsHubClient == null || !metricsHubClient.isAvailable()) {
                logger.warn("Hub服务不可用,无法批量查询监控数据");
                return ResponseEntity.status(503).body(Map.of(
                    "error", "Hub服务不可用,请稍后重试",
                    "code", "HUB_UNAVAILABLE"
                ));
            }
            
            // 4. 并发查询Hub (使用现有方法)
            Map<Long, Object> results = new LinkedHashMap<>();
            int successCount = 0;
            int failureCount = 0;
            
            for (Server server : servers) {
                try {
                    ServerMetrics metrics = metricsHubClient.getLatestMetrics(server.getId());
                    if (metrics != null) {
                        results.put(server.getId(), metrics);
                        successCount++;
                    } else {
                        results.put(server.getId(), Map.of(
                            "error", "暂无数据",
                            "code", "NO_DATA"
                        ));
                        failureCount++;
                    }
                } catch (Exception e) {
                    logger.error("服务器 {} 查询失败: {}", server.getId(), e.getMessage());
                    results.put(server.getId(), Map.of(
                        "error", e.getMessage(),
                        "code", "QUERY_FAILED"
                    ));
                    failureCount++;
                }
            }
            
            logger.info("批量Hub查询完成: 总数={}, 成功={}, 失败={}", 
                servers.size(), successCount, failureCount);
            
            // 5. 返回结果
            return ResponseEntity.ok(Map.of(
                "data", results,
                "timestamp", System.currentTimeMillis(),
                "statistics", Map.of(
                    "total", servers.size(),
                    "success", successCount,
                    "failure", failureCount
                )
            ));
            
        } catch (Exception e) {
            logger.error("批量Hub查询异常: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "批量查询失败: " + e.getMessage(),
                "code", "BATCH_QUERY_FAILED"
            ));
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