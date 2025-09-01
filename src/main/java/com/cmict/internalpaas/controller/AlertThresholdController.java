package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.model.AlertThreshold;
import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.service.AlertThresholdService;
import com.cmict.internalpaas.service.ServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 告警阈值管理控制器
 * 提供阈值配置的Web界面和API接口
 */
@Controller
@RequestMapping("/monitoring/thresholds")
public class AlertThresholdController {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertThresholdController.class);
    
    @Autowired
    private AlertThresholdService thresholdService;
    
    @Autowired
    private ServerService serverService;
    
    /**
     * 阈值管理主页面
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER')")
    public String thresholdDashboard(Model model) {
        try {
            List<Server> servers = serverService.getActiveServers();
            Map<String, Object> statistics = thresholdService.getThresholdStatistics();
            
            model.addAttribute("servers", servers);
            model.addAttribute("statistics", statistics);
            model.addAttribute("metricTypes", AlertThreshold.MetricType.values());
            
            return "monitoring/threshold-dashboard";
        } catch (Exception e) {
            logger.error("加载阈值管理页面失败", e);
            model.addAttribute("error", "加载页面失败: " + e.getMessage());
            return "monitoring/threshold-dashboard";
        }
    }
    
    /**
     * 服务器阈值配置页面
     */
    @GetMapping("/server/{serverId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER')")
    public String serverThresholds(@PathVariable Long serverId, Model model) {
        try {
            Optional<Server> serverOpt = serverService.findById(serverId);
            if (serverOpt.isEmpty()) {
                model.addAttribute("error", "服务器不存在");
                return "monitoring/server-thresholds";
            }
            
            Server server = serverOpt.get();
            List<AlertThreshold> thresholds = thresholdService.getServerThresholds(serverId);
            
            model.addAttribute("server", server);
            model.addAttribute("thresholds", thresholds);
            model.addAttribute("metricTypes", AlertThreshold.MetricType.values());
            
            return "monitoring/server-thresholds";
        } catch (Exception e) {
            logger.error("加载服务器{}阈值配置页面失败", serverId, e);
            model.addAttribute("error", "加载页面失败: " + e.getMessage());
            return "monitoring/server-thresholds";
        }
    }
    
    // ==================== REST API 接口 ====================
    
    /**
     * 获取服务器的所有阈值配置
     */
    @GetMapping("/api/server/{serverId}")
    @ResponseBody
    public ResponseEntity<List<AlertThreshold>> getServerThresholds(@PathVariable Long serverId) {
        try {
            List<AlertThreshold> thresholds = thresholdService.getServerThresholds(serverId);
            return ResponseEntity.ok(thresholds);
        } catch (Exception e) {
            logger.error("获取服务器{}阈值配置失败", serverId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取指定阈值配置
     */
    @GetMapping("/api/{thresholdId}")
    @ResponseBody
    public ResponseEntity<AlertThreshold> getThreshold(@PathVariable Long thresholdId) {
        try {
            // 这里需要添加根据ID查找的方法到service中
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("获取阈值配置{}失败", thresholdId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 创建或更新阈值配置
     */
    @PostMapping("/api/save")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER')")
    public ResponseEntity<Map<String, Object>> saveThreshold(@RequestBody AlertThreshold threshold) {
        try {
            AlertThreshold saved = thresholdService.saveThreshold(threshold);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("threshold", saved);
            response.put("message", "阈值配置保存成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("保存阈值配置失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 批量保存阈值配置
     */
    @PostMapping("/api/batch-save")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER')")
    public ResponseEntity<Map<String, Object>> batchSaveThresholds(@RequestBody List<AlertThreshold> thresholds) {
        try {
            List<AlertThreshold> saved = thresholdService.saveThresholds(thresholds);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("thresholds", saved);
            response.put("count", saved.size());
            response.put("message", String.format("成功保存%d个阈值配置", saved.size()));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("批量保存阈值配置失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 删除阈值配置
     */
    @DeleteMapping("/api/{thresholdId}")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteThreshold(@PathVariable Long thresholdId) {
        try {
            thresholdService.deleteThreshold(thresholdId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "阈值配置删除成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("删除阈值配置{}失败", thresholdId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 重置服务器阈值为默认值
     */
    @PostMapping("/api/server/{serverId}/reset")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER')")
    public ResponseEntity<Map<String, Object>> resetServerThresholds(@PathVariable Long serverId) {
        try {
            List<AlertThreshold> thresholds = thresholdService.resetServerThresholds(serverId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("thresholds", thresholds);
            response.put("message", "阈值配置已重置为默认值");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("重置服务器{}阈值配置失败", serverId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 复制阈值配置到其他服务器
     */
    @PostMapping("/api/server/{sourceServerId}/copy")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER')")
    public ResponseEntity<Map<String, Object>> copyThresholds(
            @PathVariable Long sourceServerId,
            @RequestBody Map<String, Object> request) {
        
        try {
            @SuppressWarnings("unchecked")
            List<Long> targetServerIds = (List<Long>) request.get("targetServerIds");
            
            if (targetServerIds == null || targetServerIds.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "请选择目标服务器");
                return ResponseEntity.badRequest().body(response);
            }
            
            List<AlertThreshold> copiedThresholds = thresholdService
                .copyThresholdsToServers(sourceServerId, targetServerIds);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("thresholds", copiedThresholds);
            response.put("message", String.format("成功复制到%d个服务器", targetServerIds.size()));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("复制阈值配置失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取阈值统计信息
     */
    @GetMapping("/api/statistics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getThresholdStatistics() {
        try {
            Map<String, Object> statistics = thresholdService.getThresholdStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("获取阈值统计信息失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 创建服务器默认阈值配置
     */
    @PostMapping("/api/server/{serverId}/create-defaults")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER')")
    public ResponseEntity<Map<String, Object>> createDefaultThresholds(@PathVariable Long serverId) {
        try {
            List<AlertThreshold> thresholds = thresholdService.createDefaultThresholdsForServer(serverId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("thresholds", thresholds);
            response.put("message", "默认阈值配置创建成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("为服务器{}创建默认阈值配置失败", serverId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 测试阈值配置（模拟告警检查）
     */
    @PostMapping("/api/test")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER')")
    public ResponseEntity<Map<String, Object>> testThreshold(@RequestBody Map<String, Object> request) {
        try {
            Long serverId = Long.valueOf(request.get("serverId").toString());
            String metricTypeStr = request.get("metricType").toString();
            Double testValue = Double.valueOf(request.get("testValue").toString());
            
            AlertThreshold.MetricType metricType = AlertThreshold.MetricType.valueOf(metricTypeStr);
            Optional<AlertThreshold> thresholdOpt = thresholdService.getThreshold(serverId, metricType);
            
            Map<String, Object> response = new HashMap<>();
            
            if (thresholdOpt.isEmpty()) {
                response.put("success", false);
                response.put("error", "未找到对应的阈值配置");
                return ResponseEntity.notFound().build();
            }
            
            AlertThreshold threshold = thresholdOpt.get();
            AlertThreshold.AlertLevel level = threshold.checkAlert(testValue);
            
            response.put("success", true);
            response.put("testValue", testValue);
            response.put("triggered", level != null);
            
            if (level != null) {
                response.put("alertLevel", level);
                response.put("message", String.format("测试值 %.2f 触发了%s级别告警", testValue, level.getDisplayName()));
            } else {
                response.put("message", String.format("测试值 %.2f 未触发告警", testValue));
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("测试阈值配置失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取指标类型信息
     */
    @GetMapping("/api/metric-types")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMetricTypes() {
        try {
            Map<String, Object> response = new HashMap<>();
            Map<String, Map<String, String>> metricTypesInfo = new HashMap<>();
            
            for (AlertThreshold.MetricType type : AlertThreshold.MetricType.values()) {
                Map<String, String> typeInfo = new HashMap<>();
                typeInfo.put("name", type.name());
                typeInfo.put("displayName", type.getDisplayName());
                typeInfo.put("unit", type.getUnit());
                metricTypesInfo.put(type.name(), typeInfo);
            }
            
            response.put("success", true);
            response.put("metricTypes", metricTypesInfo);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取指标类型信息失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}