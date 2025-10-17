package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.dto.UserOperationDetailDto;
import com.cmict.internalpaas.dto.UserOperationQueryDto;
import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.UserActivity;
import com.cmict.internalpaas.service.ServerService;
import com.cmict.internalpaas.service.UserActivityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户操作详情控制器
 */
@Controller
@RequestMapping("/user-operations")
public class UserOperationController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserOperationController.class);
    
    @Autowired
    private UserActivityService userActivityService;
    
    @Autowired
    private ServerService serverService;
    
    /**
     * 用户操作详情页面
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER')")
    public String userOperationsPage(Model model) {
        List<Server> servers = serverService.getAllServers();
        model.addAttribute("servers", servers);
        model.addAttribute("activityTypes", UserActivity.ActivityType.values());
        return "user-operations";
    }
    
    /**
     * 查询用户操作详情
     */
    @PostMapping("/api/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> searchUserOperations(@RequestBody UserOperationQueryDto queryDto) {
        try {
            // 构建分页参数
            Sort sort = Sort.by(Sort.Direction.fromString(queryDto.getSortDirection()), queryDto.getSortBy());
            Pageable pageable = PageRequest.of(queryDto.getPage(), queryDto.getSize(), sort);
            
            // 执行查询
            Page<UserOperationDetailDto> result = searchUserOperationsWithQuery(queryDto, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", result.getContent());
            response.put("totalElements", result.getTotalElements());
            response.put("totalPages", result.getTotalPages());
            response.put("currentPage", result.getNumber());
            response.put("size", result.getSize());
            response.put("hasNext", result.hasNext());
            response.put("hasPrevious", result.hasPrevious());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("查询用户操作详情失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "查询失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * 获取用户操作详情
     */
    @GetMapping("/api/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER')")
    @ResponseBody
    public ResponseEntity<UserOperationDetailDto> getUserOperationDetail(@PathVariable Long id) {
        try {
            // 这里应该从UserActivityRepository获取详情
            // 暂时使用现有服务的方法，实际应该扩展Repository
            UserOperationDetailDto detail = getUserOperationDetailById(id);
            
            if (detail != null) {
                return ResponseEntity.ok(detail);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("获取用户操作详情失败", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 导出用户操作详情
     */
    @PostMapping("/api/export")
    @PreAuthorize("hasRole('ADMIN')")
    public void exportUserOperations(@RequestParam String queryData, HttpServletResponse response) {
        try {
            // 解析JSON查询参数
            // 这里简化处理，实际应该使用Jackson来解析
            UserOperationQueryDto queryDto = parseQueryData(queryData);
            // 设置导出格式，默认CSV
            String format = queryDto.getExportFormat() != null ? queryDto.getExportFormat().toLowerCase() : "csv";
            
            // 获取所有数据（不分页）
            List<UserOperationDetailDto> data = getAllUserOperationsWithQuery(queryDto);
            
            switch (format) {
                case "csv":
                    exportToCsv(data, queryDto.getExportFields(), response);
                    break;
                case "json":
                    exportToJson(data, response);
                    break;
                default:
                    exportToCsv(data, queryDto.getExportFields(), response);
                    break;
            }
        } catch (Exception e) {
            logger.error("导出用户操作详情失败", e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "导出失败");
            } catch (IOException ex) {
                logger.error("发送错误响应失败", ex);
            }
        }
    }
    
    /**
     * 获取操作统计信息
     */
    @GetMapping("/api/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOperationStatistics(
            @RequestParam(required = false) Long serverId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        try {
            LocalDateTime start = startTime != null ? 
                LocalDateTime.parse(startTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME) : 
                LocalDateTime.now().minusDays(7);
            LocalDateTime end = endTime != null ? 
                LocalDateTime.parse(endTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME) : 
                LocalDateTime.now();
                
            Map<String, Object> statistics = new HashMap<>();
            
            if (serverId != null) {
                // 单个服务器统计
                statistics.put("totalActivities", userActivityService.countActivitiesInTimeRange(start, end));
                statistics.put("activeUsers", userActivityService.countActiveUsers(serverId));
                statistics.put("activityTypeStats", userActivityService.getActivityTypeStats(serverId, start, end));
                statistics.put("recentActivities", userActivityService.getRecentActivities(serverId));
            } else {
                // 全局统计
                Map<String, Object> globalStats = userActivityService.getGlobalStatistics(start, end);
                statistics.putAll(globalStats);
            }
            
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("获取操作统计信息失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取统计信息失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * 获取用户风险评估
     */
    @GetMapping("/api/risk-assessment/{username}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUserRiskAssessment(@PathVariable String username) {
        try {
            Map<String, Object> assessment = userActivityService.getUserRiskAssessment(username);
            return ResponseEntity.ok(assessment);
        } catch (Exception e) {
            logger.error("获取用户风险评估失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取风险评估失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    // 私有辅助方法
    
    private Page<UserOperationDetailDto> searchUserOperationsWithQuery(UserOperationQueryDto queryDto, Pageable pageable) {
        // 使用扩展的复杂查询方法
        Page<UserActivity> activitiesPage = userActivityService.searchUserActivities(
            queryDto.getServerId(),
            queryDto.getUsername(),
            queryDto.getSessionId(),
            queryDto.getActivityType(),
            queryDto.getIsActive(),
            queryDto.getRemoteIp(),
            queryDto.getStartTime(),
            queryDto.getEndTime(),
            queryDto.getTerminalType(),
            queryDto.getKeyword(),
            pageable
        );
        
        // 转换为DTO
        List<UserOperationDetailDto> dtos = activitiesPage.getContent().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
            
        return new org.springframework.data.domain.PageImpl<>(dtos, pageable, activitiesPage.getTotalElements());
    }
    
    private UserOperationDetailDto getUserOperationDetailById(Long id) {
        UserActivity activity = userActivityService.getUserActivityById(id);
        if (activity != null) {
            return convertToDto(activity);
        }
        return null;
    }
    
    private List<UserOperationDetailDto> getAllUserOperationsWithQuery(UserOperationQueryDto queryDto) {
        // 不分页获取所有数据用于导出
        queryDto.setPage(0);
        queryDto.setSize(Integer.MAX_VALUE);
        return searchUserOperationsWithQuery(queryDto, Pageable.unpaged()).getContent();
    }
    
    private UserOperationDetailDto convertToDto(UserActivity activity) {
        UserOperationDetailDto dto = new UserOperationDetailDto(activity);
        
        // 设置服务器名称
        if (activity.getServerId() != null) {
            serverService.getServerById(activity.getServerId())
                .ifPresent(server -> dto.setServerName(server.getName()));
        }
        
        // 生成操作摘要
        dto.setOperationSummary(generateOperationSummary(activity));
        
        // 设置风险等级
        dto.setRiskLevel(calculateRiskLevel(activity));
        
        return dto;
    }
    
    private boolean matchesQuery(UserOperationDetailDto dto, UserOperationQueryDto query) {
        if (query.getActivityType() != null && !query.getActivityType().equals(dto.getActivityType())) {
            return false;
        }
        if (query.getIsActive() != null && !query.getIsActive().equals(dto.getIsActive())) {
            return false;
        }
        if (query.getRemoteIp() != null && !query.getRemoteIp().equals(dto.getRemoteIp())) {
            return false;
        }
        if (query.getTerminalType() != null && !query.getTerminalType().equals(dto.getTerminalType())) {
            return false;
        }
        if (query.getMinCommandCount() != null && 
            (dto.getCommandCount() == null || dto.getCommandCount() < query.getMinCommandCount())) {
            return false;
        }
        if (query.getMaxCommandCount() != null && 
            (dto.getCommandCount() == null || dto.getCommandCount() > query.getMaxCommandCount())) {
            return false;
        }
        if (query.getKeyword() != null && dto.getActivityDetails() != null && 
            !dto.getActivityDetails().toLowerCase().contains(query.getKeyword().toLowerCase())) {
            return false;
        }
        return true;
    }
    
    private String generateOperationSummary(UserActivity activity) {
        if (activity.getActivityType() == null) {
            return "未知操作";
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append(activity.getActivityType().getDescription());
        
        if (activity.getCommandCount() != null && activity.getCommandCount() > 0) {
            summary.append("，执行了").append(activity.getCommandCount()).append("个命令");
        }
        
        if (activity.getActivityDetails() != null) {
            String details = activity.getActivityDetails();
            if (details.length() > 50) {
                details = details.substring(0, 47) + "...";
            }
            summary.append("：").append(details);
        }
        
        return summary.toString();
    }
    
    private String calculateRiskLevel(UserActivity activity) {
        // 简单的风险等级计算逻辑
        if (activity.getActivityType() == UserActivity.ActivityType.SYSTEM_INFO) {
            return "低";
        } else if (activity.getActivityType() == UserActivity.ActivityType.FILE_EDIT ||
                   activity.getActivityType() == UserActivity.ActivityType.PROCESS_START ||
                   activity.getActivityType() == UserActivity.ActivityType.PROCESS_STOP) {
            return "中";
        } else if (activity.getActivityType() == UserActivity.ActivityType.COMMAND_EXECUTE) {
            // 根据命令内容判断
            if (activity.getActivityDetails() != null) {
                String details = activity.getActivityDetails().toLowerCase();
                if (details.contains("rm") || details.contains("delete") || 
                    details.contains("drop") || details.contains("truncate")) {
                    return "高";
                }
            }
            return "中";
        }
        return "低";
    }
    
    private void exportToCsv(List<UserOperationDetailDto> data, List<String> fields, HttpServletResponse response) 
            throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, 
            "attachment; filename=user-operations-" + 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".csv");
        
        try (PrintWriter writer = response.getWriter()) {
            // 写入BOM用于Excel正确识别UTF-8
            writer.write('\ufeff');
            
            // 写入标题行
            writer.println("ID,服务器名称,用户名,会话ID,活动类型,操作详情,创建时间,最后活动时间,命令数量,远程IP,终端类型,会话时长(分钟),操作摘要,风险等级");
            
            // 写入数据行
            for (UserOperationDetailDto dto : data) {
                writer.printf("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%d,\"%s\",\"%s\",%d,\"%s\",\"%s\"%n",
                    dto.getId(),
                    escapeCsv(dto.getServerName()),
                    escapeCsv(dto.getUsername()),
                    escapeCsv(dto.getSessionId()),
                    escapeCsv(dto.getActivityTypeDescription()),
                    escapeCsv(dto.getActivityDetails()),
                    dto.getCreatedAt() != null ? dto.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "",
                    dto.getLastActivity() != null ? dto.getLastActivity().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "",
                    dto.getCommandCount() != null ? dto.getCommandCount() : 0,
                    escapeCsv(dto.getRemoteIp()),
                    escapeCsv(dto.getTerminalType()),
                    dto.getSessionDuration() != null ? dto.getSessionDuration() : 0,
                    escapeCsv(dto.getOperationSummary()),
                    escapeCsv(dto.getRiskLevel())
                );
            }
        }
    }
    
    private void exportToJson(List<UserOperationDetailDto> data, HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, 
            "attachment; filename=user-operations-" + 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".json");
        
        // 使用Jackson或Gson序列化数据
        // 这里简化处理，实际应该使用JSON库
        try (PrintWriter writer = response.getWriter()) {
            writer.write("[");
            for (int i = 0; i < data.size(); i++) {
                if (i > 0) writer.write(",");
                // 简化的JSON输出，实际应该使用Jackson
                writer.write("{\"id\":" + data.get(i).getId() + "}");
            }
            writer.write("]");
        }
    }
    
    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
    
    private UserOperationQueryDto parseQueryData(String queryData) {
        // 简化的JSON解析，实际应该使用Jackson
        UserOperationQueryDto queryDto = new UserOperationQueryDto();
        
        try {
            // 这里应该使用Jackson ObjectMapper来解析JSON
            // 暂时返回默认对象，实际需要完整实现
            return queryDto;
        } catch (Exception e) {
            logger.error("解析查询参数失败", e);
            return new UserOperationQueryDto();
        }
    }
}