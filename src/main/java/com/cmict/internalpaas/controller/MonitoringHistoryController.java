package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerMetrics;
// import com.cmict.internalpaas.repository.ServerMetricsRepository; // ❌ 已废弃 (Phase4-Step4)
import com.cmict.internalpaas.service.MonitoringHistoryService;
import com.cmict.internalpaas.client.MetricsHubClient;
import com.cmict.internalpaas.service.ServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 历史监控数据控制器
 * 提供历史监控数据查询、图表数据和分析功能
 * 
 * ⚠️ 已废弃 (Phase4-Step4: 2025-10-17)
 * 原因: H2数据库已移除,历史监控数据由Hub提供
 * 
 * @deprecated 使用Hub的API端点获取历史监控数据
 */
@Deprecated(since = "2025-10-17", forRemoval = true)
@Controller
@RequestMapping("/monitoring/history")
public class MonitoringHistoryController {
    
    private static final Logger logger = LoggerFactory.getLogger(MonitoringHistoryController.class);
    
    @Autowired
    private MonitoringHistoryService monitoringHistoryService;
    
    @Autowired
    private ServerService serverService;
    
    // ❌ 已废弃 (Phase4-Step4: 2025-10-17)
    // @Autowired
    // private ServerMetricsRepository metricsRepository;
    @Autowired(required = false)
    private MetricsHubClient metricsHubClient;
    
    /**
     * 历史监控主页面
     */
    @GetMapping("/dashboard")
    public String historyDashboard(Model model) {
        try {
            List<Server> servers = serverService.getActiveServers();
            model.addAttribute("servers", servers);
            
            // 添加默认时间范围（最近24小时）
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusHours(24);
            
            model.addAttribute("defaultStartTime", startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
            model.addAttribute("defaultEndTime", endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
            
            return "monitoring/history-dashboard";
        } catch (Exception e) {
            logger.error("加载历史监控页面失败", e);
            model.addAttribute("error", "加载页面失败: " + e.getMessage());
            return "monitoring/history-dashboard";
        }
    }
    
    /**
     * 获取单个服务器的历史数据
     */
    @GetMapping("/api/server/{serverId}/data")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getServerHistoryData(
            @PathVariable Long serverId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "false") boolean aggregated,
            @RequestParam(defaultValue = "hour") String aggregationType) {
        
        try {
            logger.info("获取服务器 {} 的历史数据，时间范围: {} 到 {}", serverId, startTime, endTime);
            
            Map<String, Object> response = monitoringHistoryService
                .getServerHistoryData(serverId, startTime, endTime, aggregated, aggregationType);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取服务器历史数据失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("success", false);
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 获取多个服务器的比较数据
     */
    @GetMapping("/api/servers/compare")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> compareServers(
            @RequestParam List<Long> serverIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "hour") String aggregationType) {
        
        try {
            logger.info("比较服务器数据，服务器IDs: {}, 时间范围: {} 到 {}", serverIds, startTime, endTime);
            
            Map<String, Object> response = monitoringHistoryService
                .compareServers(serverIds, startTime, endTime, aggregationType);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("比较服务器数据失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("success", false);
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 获取实时数据（最近N分钟）
     */
    @GetMapping("/api/server/{serverId}/realtime")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRealtimeData(
            @PathVariable Long serverId,
            @RequestParam(defaultValue = "30") int minutes) {
        
        try {
            LocalDateTime sinceTime = LocalDateTime.now().minusMinutes(minutes);
            List<ServerMetrics> metrics = Collections.emptyList();

            if (metricsHubClient != null && metricsHubClient.isAvailable()) {
                try {
                    Long fromMillis = sinceTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                    Long toMillis = LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                    ServerMetrics sample = metricsHubClient.queryMetrics(serverId, fromMillis, toMillis, null, null);
                    if (sample != null) metrics = List.of(sample);
                } catch (Exception ex) {
                    logger.warn("MetricsHub realtime query failed: {}", ex.getMessage());
                }
            } else {
                logger.debug("MetricsHub not available - realtime will be empty or use service fallback");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", convertToChartData(metrics, "realtime"));
            response.put("serverInfo", getServerInfo(serverId));
            response.put("dataCount", metrics.size());
            response.put("timeRange", minutes + " minutes");

            // 如果没有数据，尝试返回统计信息作为降级
            if (metrics.isEmpty()) {
                Map<String, Object> stats = monitoringHistoryService.getServerStatistics(serverId, sinceTime, LocalDateTime.now());
                response.put("fallbackStatistics", stats);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取实时数据失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage(), "success", false));
        }
    }
    
    /**
     * 获取服务器性能统计信息
     */
    @GetMapping("/api/server/{serverId}/statistics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getServerStatistics(
            @PathVariable Long serverId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        try {
            Map<String, Object> statistics = monitoringHistoryService
                .getServerStatistics(serverId, startTime, endTime);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("获取服务器统计信息失败", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage(), "success", false));
        }
    }
    
    /**
     * 获取异常事件列表
     */
    @GetMapping("/api/server/{serverId}/anomalies")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAnomalies(
            @PathVariable Long serverId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "80.0") Double cpuThreshold,
            @RequestParam(defaultValue = "80.0") Double memoryThreshold) {
        
        try {
            Map<String, Object> anomalies = monitoringHistoryService
                .getAnomalies(serverId, startTime, endTime, cpuThreshold, memoryThreshold);
            return ResponseEntity.ok(anomalies);
        } catch (Exception e) {
            logger.error("获取异常事件失败", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage(), "success", false));
        }
    }
    
    /**
     * 获取所有服务器的性能排名
     */
    @GetMapping("/api/servers/ranking")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getServerRanking(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        try {
            // 性能排名由 Hub 提供，当前返回空占位
            List<Map<String, Object>> rankingData = new ArrayList<>();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("ranking", rankingData);
            response.put("timeRange", Map.of("start", startTime, "end", endTime));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取服务器排名失败", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage(), "success", false));
        }
    }
    
    /**
     * 导出历史数据为CSV
     */
    @GetMapping("/api/server/{serverId}/export")
    public ResponseEntity<byte[]> exportHistoryData(
            @PathVariable Long serverId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        try {
            byte[] csvData = monitoringHistoryService.exportToCsv(serverId, startTime, endTime);
            
            String filename = String.format("server_%d_metrics_%s_to_%s.csv", 
                serverId, 
                startTime.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")),
                endTime.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")));
            
            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=" + filename)
                .header("Content-Type", "text/csv; charset=UTF-8")
                .body(csvData);
                
        } catch (Exception e) {
            logger.error("导出历史数据失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取快捷时间范围的数据
     */
    @GetMapping("/api/server/{serverId}/quick-range")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getQuickRangeData(
            @PathVariable Long serverId,
            @RequestParam String range) {
        
        try {
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime;
            
            switch (range.toLowerCase()) {
                case "1h":
                    startTime = endTime.minusHours(1);
                    break;
                case "6h":
                    startTime = endTime.minusHours(6);
                    break;
                case "24h":
                    startTime = endTime.minusHours(24);
                    break;
                case "7d":
                    startTime = endTime.minusDays(7);
                    break;
                case "30d":
                    startTime = endTime.minusDays(30);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的时间范围: " + range);
            }
            
            // 根据时间范围选择聚合类型
            String aggregationType = range.equals("1h") || range.equals("6h") ? "none" : "hour";
            if (range.equals("7d") || range.equals("30d")) {
                aggregationType = "day";
            }
            
            Map<String, Object> response = monitoringHistoryService
                .getServerHistoryData(serverId, startTime, endTime, !aggregationType.equals("none"), aggregationType);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取快捷时间范围数据失败", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage(), "success", false));
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 将监控数据转换为图表数据格式
     */
    private Map<String, Object> convertToChartData(List<ServerMetrics> metrics, String type) {
        Map<String, Object> chartData = new HashMap<>();
        
        List<String> timestamps = new ArrayList<>();
        List<Double> cpuData = new ArrayList<>();
        List<Double> memoryData = new ArrayList<>();
        List<Double> diskData = new ArrayList<>();
        
        DateTimeFormatter formatter = type.equals("realtime") 
            ? DateTimeFormatter.ofPattern("HH:mm:ss")
            : DateTimeFormatter.ofPattern("MM-dd HH:mm");
        
        for (ServerMetrics metric : metrics) {
            timestamps.add(metric.getTimestamp().format(formatter));
            cpuData.add(metric.getCpuUsage() != null ? metric.getCpuUsage() : 0.0);
            memoryData.add(metric.getMemoryUsage() != null ? metric.getMemoryUsage() : 0.0);
            diskData.add(metric.getDiskUsage() != null ? metric.getDiskUsage() : 0.0);
        }
        
        chartData.put("timestamps", timestamps);
        chartData.put("cpu", cpuData);
        chartData.put("memory", memoryData);
        chartData.put("disk", diskData);
        
        return chartData;
    }
    
    /**
     * 获取服务器基本信息
     */
    private Map<String, Object> getServerInfo(Long serverId) {
        Optional<Server> serverOpt = serverService.findById(serverId);
        if (serverOpt.isPresent()) {
            Server server = serverOpt.get();
            Map<String, Object> info = new HashMap<>();
            info.put("id", server.getId());
            info.put("name", server.getName());
            info.put("hostname", server.getHostname());
            info.put("status", server.getConnectionStatus());
            return info;
        }
        return Collections.emptyMap();
    }
}