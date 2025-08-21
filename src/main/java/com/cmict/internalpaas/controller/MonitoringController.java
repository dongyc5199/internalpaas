package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerMetrics;
import com.cmict.internalpaas.service.MonitoringService;
import com.cmict.internalpaas.service.ServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

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
    
    /**
     * 获取单个服务器指标
     */
    @GetMapping("/server/{id}/metrics")
    @ResponseBody
    public ResponseEntity<ServerMetrics> getServerMetrics(@PathVariable Long id) {
        try {
            Server server = serverService.getServerById(id).orElse(null);
            if (server == null) {
                return ResponseEntity.notFound().build();
            }
            
            ServerMetrics metrics = monitoringService.getServerMetrics(server);
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
                    ServerMetrics metrics = monitoringService.getServerMetrics(server);
                    metricsMap.put(server.getName(), metrics);
                } catch (Exception e) {
                    // 单个服务器失败不影响其他服务器
                    ServerMetrics errorMetrics = new ServerMetrics(server.getName(), server.getHostname());
                    metricsMap.put(server.getName(), errorMetrics);
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
            Server server = serverService.getServerById(id).orElse(null);
            if (server == null) {
                return ResponseEntity.notFound().build();
            }
            
            ServerMetrics metrics = monitoringService.getServerMetrics(server);
            Map<String, Object> health = new HashMap<>();
            
            health.put("serverName", server.getName());
            health.put("hostname", server.getHostname());
            health.put("status", "UP");
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
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "DOWN");
            error.put("error", e.getMessage());
            return ResponseEntity.ok(error);
        }
    }
}