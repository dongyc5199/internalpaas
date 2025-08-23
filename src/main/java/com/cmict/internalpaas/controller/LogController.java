package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.model.Application;
import com.cmict.internalpaas.repository.ApplicationRepository;
import com.cmict.internalpaas.service.LogMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 日志控制器
 * 处理日志相关的HTTP请求
 */
@RestController
@RequestMapping("/api/logs")
public class LogController {
    
    @Autowired
    private LogMonitoringService logMonitoringService;
    
    @Autowired
    private ApplicationRepository applicationRepository;
    
    /**
     * 开始监控应用日志
     */
    @PostMapping("/{applicationId}/start-monitoring")
    public ResponseEntity<Map<String, Object>> startMonitoring(@PathVariable Long applicationId,
                                                              Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 验证应用是否存在
            Application app = applicationRepository.findById(applicationId).orElse(null);
            if (app == null) {
                response.put("success", false);
                response.put("message", "应用不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证用户权限（简化版，后续可以加强权限检查）
            String username = authentication.getName();
            
            // 开始监控
            logMonitoringService.startMonitoring(applicationId, username);
            
            response.put("success", true);
            response.put("message", "日志监控已启动");
            response.put("applicationId", applicationId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "启动日志监控失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 停止监控应用日志
     */
    @PostMapping("/{applicationId}/stop-monitoring")
    public ResponseEntity<Map<String, Object>> stopMonitoring(@PathVariable Long applicationId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logMonitoringService.stopMonitoring(applicationId);
            
            response.put("success", true);
            response.put("message", "日志监控已停止");
            response.put("applicationId", applicationId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "停止日志监控失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取日志文件的最后N行内容
     */
    @GetMapping("/{applicationId}/tail")
    public ResponseEntity<Map<String, Object>> getTailLogs(@PathVariable Long applicationId,
                                                          @RequestParam(defaultValue = "50") int lines) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Application app = applicationRepository.findById(applicationId).orElse(null);
            if (app == null) {
                response.put("success", false);
                response.put("message", "应用不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (app.getLogFilePath() == null) {
                response.put("success", false);
                response.put("message", "应用未配置日志文件");
                return ResponseEntity.badRequest().body(response);
            }
            
            String logContent = logMonitoringService.getLastNLines(app.getLogFilePath(), lines);
            
            response.put("success", true);
            response.put("content", logContent);
            response.put("applicationId", applicationId);
            response.put("lines", lines);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取日志内容失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取应用的日志信息
     */
    @GetMapping("/{applicationId}/info")
    public ResponseEntity<Map<String, Object>> getLogInfo(@PathVariable Long applicationId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Application app = applicationRepository.findById(applicationId).orElse(null);
            if (app == null) {
                response.put("success", false);
                response.put("message", "应用不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            response.put("success", true);
            response.put("applicationId", applicationId);
            response.put("applicationName", app.getName());
            response.put("logFilePath", app.getLogFilePath());
            response.put("status", app.getStatus());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取日志信息失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}