package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.model.ServerStatusTag;
import com.cmict.internalpaas.service.ResourceAlertService;
import com.cmict.internalpaas.service.ServerResourceMonitoringService;
import com.cmict.internalpaas.service.ServerStatusTagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/servers/{serverId}/status")
@PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER')")
public class ServerStatusController {
    
    private static final Logger logger = LoggerFactory.getLogger(ServerStatusController.class);
    
    @Autowired
    private ServerStatusTagService statusTagService;
    
    @Autowired
    private ServerResourceMonitoringService resourceMonitoringService;
    
    @Autowired
    private ResourceAlertService resourceAlertService;

    /**
     * 获取服务器的所有状态标签
     */
    @GetMapping("/tags")
    public ResponseEntity<Map<String, Object>> getServerStatusTags(@PathVariable Long serverId) {
        try {
            List<ServerStatusTag> statusTags = statusTagService.getServerStatusTags(serverId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("serverId", serverId);
            response.put("statusTags", statusTags);
            response.put("totalTags", statusTags.size());
            response.put("alertCount", statusTagService.countAlertTags(serverId));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取服务器 {} 状态标签失败: {}", serverId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取状态标签失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 获取服务器的告警级别标签
     */
    @GetMapping("/alerts")
    public ResponseEntity<Map<String, Object>> getServerAlertTags(@PathVariable Long serverId) {
        try {
            List<ServerStatusTag> alertTags = statusTagService.getServerAlertTags(serverId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("serverId", serverId);
            response.put("alertTags", alertTags);
            response.put("alertCount", alertTags.size());
            response.put("hasMemoryAlert", alertTags.stream()
                .anyMatch(tag -> tag.getTagType() == ServerStatusTag.TagType.MEMORY_USAGE &&
                                (tag.isCritical() || tag.isDanger())));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取服务器 {} 告警标签失败: {}", serverId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取告警标签失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 刷新服务器状态标签
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshServerStatus(@PathVariable Long serverId) {
        try {
            logger.info("手动刷新服务器 {} 状态", serverId);
            
            // 刷新所有状态标签
            statusTagService.refreshAllServerTags(serverId);
            
            // 检查资源状态
            List<ServerStatusTag> resourceTags = resourceMonitoringService.checkResourceStatuses(serverId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "状态刷新成功");
            response.put("serverId", serverId);
            response.put("refreshedResourceTags", resourceTags.size());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("刷新服务器 {} 状态失败: {}", serverId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "状态刷新失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 检查服务器内存使用率 - 80%阈值告警检查
     */
    @PostMapping("/check-memory")
    public ResponseEntity<Map<String, Object>> checkMemoryUsage(@PathVariable Long serverId) {
        try {
            logger.info("检查服务器 {} 内存使用率", serverId);
            
            ServerStatusTag memoryTag = resourceMonitoringService.checkMemoryUsage(serverId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("serverId", serverId);
            
            if (memoryTag != null) {
                response.put("hasMemoryAlert", true);
                response.put("memoryTag", memoryTag);
                response.put("memoryUsage", memoryTag.getValue());
                response.put("alertLevel", memoryTag.getStatus().name());
                response.put("isOver80Percent", memoryTag.isCritical() || memoryTag.isDanger());
                
                // 如果超过80%，标记为严重告警
                if (memoryTag.isCritical() || memoryTag.isDanger()) {
                    response.put("alertType", "CRITICAL_MEMORY");
                    response.put("alertMessage", "内存使用率超过80%阈值，需要立即关注");
                }
            } else {
                response.put("hasMemoryAlert", false);
                response.put("memoryStatus", "NORMAL");
                response.put("message", "内存使用率正常");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("检查服务器 {} 内存使用率失败: {}", serverId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "内存检查失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 获取服务器资源状态摘要
     */
    @GetMapping("/resources")
    public ResponseEntity<Map<String, Object>> getResourceStatus(@PathVariable Long serverId) {
        try {
            List<ServerStatusTag> resourceTags = resourceMonitoringService.checkResourceStatuses(serverId);
            
            // 分类统计资源状态
            Map<String, ServerStatusTag> resourceMap = new HashMap<>();
            int warningCount = 0;
            int criticalCount = 0;
            int dangerCount = 0;
            
            for (ServerStatusTag tag : resourceTags) {
                resourceMap.put(tag.getTagType().name(), tag);
                
                if (tag.isWarning()) warningCount++;
                else if (tag.isCritical()) criticalCount++;
                else if (tag.isDanger()) dangerCount++;
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("serverId", serverId);
            response.put("resourceTags", resourceTags);
            response.put("resourceMap", resourceMap);
            response.put("summary", Map.of(
                "totalResources", resourceTags.size(),
                "warningCount", warningCount,
                "criticalCount", criticalCount,
                "dangerCount", dangerCount,
                "hasResourceAlerts", warningCount + criticalCount + dangerCount > 0
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取服务器 {} 资源状态失败: {}", serverId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取资源状态失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 获取服务器告警统计信息
     */
    @GetMapping("/alert-statistics")
    public ResponseEntity<Map<String, Object>> getAlertStatistics(@PathVariable Long serverId) {
        try {
            Map<String, Object> alertStats = resourceAlertService.getServerAlertStatistics(serverId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("serverId", serverId);
            response.put("alertStatistics", alertStats);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取服务器 {} 告警统计失败: {}", serverId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取告警统计失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 获取高优先级标签（移动端使用）
     */
    @GetMapping("/priority-tags")
    public ResponseEntity<Map<String, Object>> getHighPriorityTags(@PathVariable Long serverId,
                                                                  @RequestParam(defaultValue = "6") Integer minPriority) {
        try {
            List<ServerStatusTag> highPriorityTags = statusTagService.getHighPriorityTags(serverId, minPriority);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("serverId", serverId);
            response.put("minPriority", minPriority);
            response.put("priorityTags", highPriorityTags);
            response.put("count", highPriorityTags.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取服务器 {} 高优先级标签失败: {}", serverId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取高优先级标签失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 检查连接状态
     */
    @PostMapping("/check-connection")
    public ResponseEntity<Map<String, Object>> checkConnection(@PathVariable Long serverId) {
        try {
            ServerStatusTag connectionTag = statusTagService.checkConnectionStatus(serverId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("serverId", serverId);
            response.put("connectionTag", connectionTag);
            response.put("isOnline", connectionTag.isHealthy());
            response.put("connectionStatus", connectionTag.getStatus().name());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("检查服务器 {} 连接状态失败: {}", serverId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "连接检查失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 删除指定类型的状态标签
     */
    @DeleteMapping("/tags/{tagType}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteTagByType(@PathVariable Long serverId,
                                                              @PathVariable String tagType) {
        try {
            ServerStatusTag.TagType type = ServerStatusTag.TagType.valueOf(tagType.toUpperCase());
            statusTagService.deleteTagByType(serverId, type);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "标签删除成功");
            response.put("serverId", serverId);
            response.put("deletedTagType", tagType);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "无效的标签类型: " + tagType);
            
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("删除服务器 {} 标签类型 {} 失败: {}", serverId, tagType, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "删除标签失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}