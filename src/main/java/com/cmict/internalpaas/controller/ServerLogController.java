package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.dto.LogSearchRequest;
import com.cmict.internalpaas.dto.ServerLogContentDto;
import com.cmict.internalpaas.dto.ServerLogFileDto;
import com.cmict.internalpaas.service.ServerLogService;
import com.cmict.internalpaas.service.ServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务器日志控制器
 * 提供服务器日志查看、搜索和监控相关的REST API接口
 */
@RestController
@RequestMapping("/api/server-logs")
@PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER')")
public class ServerLogController {
    
    private static final Logger logger = LoggerFactory.getLogger(ServerLogController.class);
    
    @Autowired
    private ServerLogService serverLogService;
    
    @Autowired
    private ServerService serverService;
    
    @Autowired
    private com.cmict.internalpaas.service.ServerLogMonitorService serverLogMonitorService;
    
    /**
     * 获取服务器的可用日志文件列表
     * GET /api/server-logs/{serverId}/files
     */
    @GetMapping("/{serverId}/files")
    public ResponseEntity<Map<String, Object>> getLogFiles(@PathVariable Long serverId,
                                                          @RequestParam(defaultValue = "false") boolean refresh) {
        logger.info("获取服务器 {} 的日志文件列表，刷新: {}", serverId, refresh);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 检查服务器是否存在
            if (!serverService.getServerById(serverId).isPresent()) {
                response.put("success", false);
                response.put("error", "服务器不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 如果需要刷新，先清除缓存
            if (refresh) {
                serverLogService.clearLogFileCache(serverId);
            }
            
            List<ServerLogFileDto> logFiles = serverLogService.getAvailableLogFiles(serverId);
            
            // 按类型分组
            Map<String, List<ServerLogFileDto>> groupedFiles = new HashMap<>();
            for (ServerLogFileDto file : logFiles) {
                String category = getCategoryFromType(file.getType());
                groupedFiles.computeIfAbsent(category, k -> new java.util.ArrayList<>()).add(file);
            }
            
            response.put("success", true);
            response.put("serverId", serverId);
            response.put("totalFiles", logFiles.size());
            response.put("files", logFiles);
            response.put("groupedFiles", groupedFiles);
            response.put("refreshed", refresh);
            
            logger.info("成功返回服务器 {} 的 {} 个日志文件", serverId, logFiles.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取服务器日志文件列表失败", e);
            response.put("success", false);
            response.put("error", "获取日志文件列表失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取日志文件内容
     * GET /api/server-logs/{serverId}/content?path=/var/log/syslog&lines=100&fromEnd=true
     */
    @GetMapping("/{serverId}/content")
    public ResponseEntity<Map<String, Object>> getLogContent(@PathVariable Long serverId,
                                                           @RequestParam String path,
                                                           @RequestParam(defaultValue = "100") int lines,
                                                           @RequestParam(defaultValue = "true") boolean fromEnd,
                                                           @RequestParam(defaultValue = "false") boolean includeLineNumbers) {
        logger.info("获取服务器 {} 日志内容: path={}, lines={}, fromEnd={}", serverId, path, lines, fromEnd);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 参数验证
            if (path == null || path.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "日志文件路径不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (lines <= 0 || lines > 10000) {
                response.put("success", false);
                response.put("error", "行数必须在1-10000之间");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 检查服务器是否存在
            if (!serverService.getServerById(serverId).isPresent()) {
                response.put("success", false);
                response.put("error", "服务器不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            ServerLogContentDto content = serverLogService.getLogContent(serverId, path, lines, fromEnd);
            
            if (content == null) {
                response.put("success", false);
                response.put("error", "无法读取日志文件");
                return ResponseEntity.internalServerError().body(response);
            }
            
            response.put("success", true);
            response.put("serverId", serverId);
            response.put("content", content);
            response.put("parameters", Map.of(
                "path", path,
                "lines", lines,
                "fromEnd", fromEnd,
                "includeLineNumbers", includeLineNumbers
            ));
            
            logger.info("成功返回日志内容: {} 行", content.getDisplayedLines());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取日志内容失败", e);
            response.put("success", false);
            response.put("error", "获取日志内容失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 搜索日志内容
     * POST /api/server-logs/{serverId}/search
     */
    @PostMapping("/{serverId}/search")
    public ResponseEntity<Map<String, Object>> searchLogContent(@PathVariable Long serverId,
                                                              @RequestParam String path,
                                                              @RequestBody LogSearchRequest searchRequest) {
        logger.info("搜索服务器 {} 日志: path={}, searchRequest={}", serverId, path, searchRequest);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 参数验证
            if (path == null || path.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "日志文件路径不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!searchRequest.isValid()) {
                response.put("success", false);
                response.put("error", "搜索请求参数无效");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 检查服务器是否存在
            if (!serverService.getServerById(serverId).isPresent()) {
                response.put("success", false);
                response.put("error", "服务器不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            ServerLogContentDto searchResult = serverLogService.searchLogContent(serverId, path, searchRequest);
            
            if (searchResult == null) {
                response.put("success", false);
                response.put("error", "无法执行搜索操作");
                return ResponseEntity.internalServerError().body(response);
            }
            
            response.put("success", true);
            response.put("serverId", serverId);
            response.put("searchRequest", searchRequest);
            response.put("results", searchResult);
            response.put("summary", Map.of(
                "totalMatches", searchResult.getDisplayedLines(),
                "searchTime", System.currentTimeMillis(),
                "filePath", path
            ));
            
            logger.info("搜索完成: 找到 {} 个匹配结果", searchResult.getDisplayedLines());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("搜索日志内容失败", e);
            response.put("success", false);
            response.put("error", "搜索日志内容失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取日志文件统计信息
     * GET /api/server-logs/{serverId}/stats?path=/var/log/syslog
     */
    @GetMapping("/{serverId}/stats")
    public ResponseEntity<Map<String, Object>> getLogStats(@PathVariable Long serverId,
                                                         @RequestParam String path) {
        logger.info("获取服务器 {} 日志统计: path={}", serverId, path);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 参数验证
            if (path == null || path.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "日志文件路径不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 检查服务器是否存在
            if (!serverService.getServerById(serverId).isPresent()) {
                response.put("success", false);
                response.put("error", "服务器不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            Map<String, Object> stats = serverLogService.getLogFileStats(serverId, path);
            
            response.put("success", true);
            response.put("serverId", serverId);
            response.put("filePath", path);
            response.put("stats", stats);
            response.put("retrieveTime", System.currentTimeMillis());
            
            logger.info("成功获取日志文件统计信息");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取日志统计失败", e);
            response.put("success", false);
            response.put("error", "获取日志统计失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 开始实时日志监控
     * POST /api/server-logs/{serverId}/monitor/start
     */
    @PostMapping("/{serverId}/monitor/start")
    public ResponseEntity<Map<String, Object>> startRealTimeMonitoring(@PathVariable Long serverId,
                                                                      @RequestBody Map<String, Object> request,
                                                                      org.springframework.security.core.Authentication authentication) {
        logger.info("开始实时监控服务器 {} 日志", serverId);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String path = (String) request.get("path");
            if (path == null || path.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "日志文件路径不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 检查服务器是否存在
            if (!serverService.getServerById(serverId).isPresent()) {
                response.put("success", false);
                response.put("error", "服务器不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            String username = authentication.getName();
            boolean started = serverLogMonitorService.startMonitoring(serverId, path, username);
            
            if (started) {
                response.put("success", true);
                response.put("message", "实时日志监控已启动");
                response.put("serverId", serverId);
                response.put("filePath", path);
                response.put("username", username);
            } else {
                response.put("success", false);
                response.put("error", "启动监控失败，可能已经在监控中");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("开始实时监控失败", e);
            response.put("success", false);
            response.put("error", "开始实时监控失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 停止实时日志监控
     * POST /api/server-logs/{serverId}/monitor/stop
     */
    @PostMapping("/{serverId}/monitor/stop")
    public ResponseEntity<Map<String, Object>> stopRealTimeMonitoring(@PathVariable Long serverId,
                                                                     @RequestBody Map<String, Object> request) {
        logger.info("停止实时监控服务器 {} 日志", serverId);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String path = (String) request.get("path");
            
            // 检查服务器是否存在
            if (!serverService.getServerById(serverId).isPresent()) {
                response.put("success", false);
                response.put("error", "服务器不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            boolean stopped = serverLogMonitorService.stopMonitoring(serverId, path);
            
            if (stopped) {
                response.put("success", true);
                response.put("message", "实时日志监控已停止");
                response.put("serverId", serverId);
                response.put("filePath", path);
            } else {
                response.put("success", false);
                response.put("error", "停止监控失败，监控任务可能不存在");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("停止实时监控失败", e);
            response.put("success", false);
            response.put("error", "停止实时监控失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取活跃的监控任务状态
     * GET /api/server-logs/{serverId}/monitor/status
     */
    @GetMapping("/{serverId}/monitor/status")
    public ResponseEntity<Map<String, Object>> getMonitoringStatus(@PathVariable Long serverId) {
        logger.info("获取服务器 {} 的日志监控状态", serverId);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 检查服务器是否存在
            if (!serverService.getServerById(serverId).isPresent()) {
                response.put("success", false);
                response.put("error", "服务器不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            Map<String, Object> activeTasks = serverLogMonitorService.getActiveMonitoringTasks();
            
            // 过滤出当前服务器的任务
            Map<String, Object> serverTasks = new HashMap<>();
            for (Map.Entry<String, Object> entry : activeTasks.entrySet()) {
                Map<String, Object> taskInfo = (Map<String, Object>) entry.getValue();
                if (serverId.equals(taskInfo.get("serverId"))) {
                    serverTasks.put(entry.getKey(), taskInfo);
                }
            }
            
            response.put("success", true);
            response.put("serverId", serverId);
            response.put("activeTasks", serverTasks);
            response.put("totalActiveTasks", serverTasks.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取监控状态失败", e);
            response.put("success", false);
            response.put("error", "获取监控状态失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 清除日志文件缓存
     * DELETE /api/server-logs/{serverId}/cache
     */
    @DeleteMapping("/{serverId}/cache")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> clearCache(@PathVariable Long serverId) {
        logger.info("清除服务器 {} 的日志文件缓存", serverId);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            serverLogService.clearLogFileCache(serverId);
            
            response.put("success", true);
            response.put("message", "缓存清除成功");
            response.put("serverId", serverId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("清除缓存失败", e);
            response.put("success", false);
            response.put("error", "清除缓存失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 根据日志类型获取分类名称
     */
    private String getCategoryFromType(String type) {
        try {
            com.cmict.internalpaas.model.ServerLogType logType = 
                com.cmict.internalpaas.model.ServerLogType.valueOf(type);
            return logType.getCategory();
        } catch (IllegalArgumentException e) {
            return "自定义";
        }
    }
}