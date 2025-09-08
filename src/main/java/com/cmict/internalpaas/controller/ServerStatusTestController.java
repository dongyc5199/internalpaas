package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerMetrics;
import com.cmict.internalpaas.model.ServerStatusTag;
import com.cmict.internalpaas.service.ServerService;
import com.cmict.internalpaas.service.ServerStatusTagService;
import com.cmict.internalpaas.service.MonitoringService;
import com.cmict.internalpaas.service.ResourceAlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/test")
public class ServerStatusTestController {
    
    private static final Logger logger = LoggerFactory.getLogger(ServerStatusTestController.class);
    
    @Autowired
    private ServerService serverService;
    
    @Autowired
    private ServerStatusTagService statusTagService;
    
    @Autowired
    private MonitoringService monitoringService;
    
    @Autowired
    private ResourceAlertService alertService;
    
    /**
     * 服务器状态标签测试页面
     */
    @GetMapping("/server-status-tags")
    public String serverStatusTagsTest() {
        return "test/server-status-tags";
    }
    
    /**
     * 获取所有服务器的状态信息
     */
    @GetMapping("/api/server-status/all")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAllServerStatus() {
        try {
            List<Server> servers = serverService.findAllActive();
            Map<String, Object> result = new HashMap<>();
            
            for (Server server : servers) {
                Map<String, Object> serverInfo = new HashMap<>();
                
                // 基本信息
                serverInfo.put("id", server.getId());
                serverInfo.put("name", server.getName());
                serverInfo.put("hostname", server.getHostname());
                serverInfo.put("status", server.getConnectionStatus());
                
                // 状态标签
                List<ServerStatusTag> tags = statusTagService.getServerStatusTags(server.getId());
                serverInfo.put("tags", tags.stream().map(this::convertTagToMap).toArray());
                
                // 监控指标
                try {
                    ServerMetrics metrics = monitoringService.getLatestMetrics(server.getId());
                    if (metrics != null) {
                        Map<String, Object> metricsMap = new HashMap<>();
                        metricsMap.put("memoryUsagePercent", metrics.getMemoryUsagePercent());
                        metricsMap.put("cpuUsagePercent", metrics.getCpuUsagePercent());
                        metricsMap.put("diskUsagePercent", metrics.getDiskUsagePercent());
                        metricsMap.put("lastUpdate", metrics.getTimestamp());
                        serverInfo.put("metrics", metricsMap);
                    }
                } catch (Exception e) {
                    logger.warn("获取服务器 {} 监控指标失败: {}", server.getId(), e.getMessage());
                }
                
                // 告警统计
                Map<String, Object> alertStats = alertService.getServerAlertStatistics(server.getId());
                serverInfo.put("alertStats", alertStats);
                
                result.put(String.valueOf(server.getId()), serverInfo);
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("获取服务器状态失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "获取服务器状态失败: " + e.getMessage()));
        }
    }
    
    /**
     * 刷新单个服务器状态
     */
    @PostMapping("/api/server-status/{serverId}/refresh")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> refreshServerStatus(@PathVariable Long serverId) {
        try {
            Optional<Server> serverOpt = serverService.findById(serverId);
            if (!serverOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            Server server = serverOpt.get();
            
            // 刷新服务器状态标签
            statusTagService.refreshAllServerTags(serverId);
            
            // 获取更新后的状态
            List<ServerStatusTag> tags = statusTagService.getServerStatusTags(serverId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("serverId", serverId);
            result.put("serverName", server.getName());
            result.put("tags", tags.stream().map(this::convertTagToMap).toArray());
            result.put("timestamp", System.currentTimeMillis());
            result.put("success", true);
            
            logger.info("已刷新服务器 {} 状态，找到 {} 个标签", serverId, tags.size());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("刷新服务器 {} 状态失败: {}", serverId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "刷新服务器状态失败: " + e.getMessage(), "success", false));
        }
    }
    
    /**
     * 模拟内存告警测试
     */
    @PostMapping("/api/simulate-memory-alert/{serverId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> simulateMemoryAlert(
            @PathVariable Long serverId,
            @RequestParam(defaultValue = "85.0") Double memoryUsage) {
        
        try {
            Optional<Server> serverOpt = serverService.findById(serverId);
            if (!serverOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            Server server = serverOpt.get();
            
            // 模拟触发内存告警
            logger.info("🧪 模拟服务器 {} 内存使用率 {}% 告警测试", serverId, memoryUsage);
            
            // 这里可以调用告警服务进行测试
            // alertService.handleMemoryAlert(serverId, memoryUsage, ThresholdLevel.CRITICAL, 
            //     String.format("模拟内存告警测试 - 内存使用率 %.1f%%", memoryUsage));
            
            Map<String, Object> result = new HashMap<>();
            result.put("serverId", serverId);
            result.put("serverName", server.getName());
            result.put("simulatedMemoryUsage", memoryUsage);
            result.put("alertTriggered", memoryUsage >= 80.0);
            result.put("alertLevel", memoryUsage >= 90.0 ? "DANGER" : (memoryUsage >= 80.0 ? "CRITICAL" : "WARNING"));
            result.put("message", String.format("模拟内存告警测试完成 - 服务器 %s 内存使用率 %.1f%%", 
                                               server.getName(), memoryUsage));
            result.put("timestamp", System.currentTimeMillis());
            result.put("success", true);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("模拟内存告警失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "模拟内存告警失败: " + e.getMessage(), "success", false));
        }
    }
    
    /**
     * 获取服务器告警历史
     */
    @GetMapping("/api/server-status/{serverId}/alert-history")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getServerAlertHistory(@PathVariable Long serverId) {
        try {
            Map<String, Object> alertStats = alertService.getServerAlertStatistics(serverId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("serverId", serverId);
            result.put("alertHistory", alertStats);
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("获取服务器 {} 告警历史失败: {}", serverId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "获取告警历史失败: " + e.getMessage()));
        }
    }
    
    /**
     * 转换标签为前端所需的Map格式
     */
    private Map<String, Object> convertTagToMap(ServerStatusTag tag) {
        Map<String, Object> tagMap = new HashMap<>();
        tagMap.put("id", tag.getId());
        tagMap.put("tagType", tag.getTagType());
        tagMap.put("status", tag.getStatus());
        tagMap.put("displayText", tag.getDisplayText());
        tagMap.put("value", tag.getValue());
        tagMap.put("colorScheme", tag.getColorScheme());
        tagMap.put("priority", tag.getPriority());
        tagMap.put("details", tag.getDetails());
        tagMap.put("isAlert", tag.isAlert());
        tagMap.put("lastUpdated", tag.getLastUpdated());
        return tagMap;
    }
}