package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.dto.UserGroupSyncResultDto;
import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerUserGroup;
import com.cmict.internalpaas.service.RemoteCommandService;
import com.cmict.internalpaas.service.ServerService;
import com.cmict.internalpaas.service.ServerUserGroupService;
import com.cmict.internalpaas.service.ServerUserGroupSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户组同步调试控制器
 * 提供详细的测试接口和调试工具，用于精确定位同步过程中的问题
 */
@Controller
@RequestMapping("/debug/user-group-sync")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class UserGroupSyncDebugController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserGroupSyncDebugController.class);
    
    @Autowired
    private ServerService serverService;
    
    @Autowired
    private ServerUserGroupService userGroupService;
    
    @Autowired
    private ServerUserGroupSyncService syncService;
    
    @Autowired
    private RemoteCommandService remoteCommandService;
    
    // 测试会话存储，用于跟踪测试过程
    private final Map<String, TestSession> testSessions = new ConcurrentHashMap<>();
    
    /**
     * 调试控制台主页面
     */
    @GetMapping("/console")
    public String debugConsole(Model model) {
        logger.info("访问用户组同步调试控制台");
        
        try {
            // 获取所有服务器
            List<Server> servers = serverService.getAllServers();
            model.addAttribute("servers", servers);
            
            // 获取活跃测试会话数量
            int activeSessionCount = testSessions.size();
            model.addAttribute("activeSessionCount", activeSessionCount);
            
            // 获取最近的测试会话信息
            List<Map<String, Object>> recentSessions = new ArrayList<>();
            testSessions.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().getCreatedAt().compareTo(e1.getValue().getCreatedAt()))
                .limit(10)
                .forEach(entry -> {
                    Map<String, Object> sessionInfo = new HashMap<>();
                    sessionInfo.put("sessionId", entry.getKey());
                    sessionInfo.put("testType", entry.getValue().getTestType());
                    sessionInfo.put("status", entry.getValue().getStatus());
                    sessionInfo.put("createdAt", entry.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("MM-dd HH:mm:ss")));
                    sessionInfo.put("serverName", entry.getValue().getServerName());
                    sessionInfo.put("groupName", entry.getValue().getGroupName());
                    recentSessions.add(sessionInfo);
                });
            model.addAttribute("recentSessions", recentSessions);
            
            logger.info("调试控制台页面加载完成，活跃会话: {}, 服务器数量: {}", activeSessionCount, servers.size());
            return "debug/user-group-sync-console";
            
        } catch (Exception e) {
            logger.error("加载调试控制台失败", e);
            model.addAttribute("error", "加载调试控制台失败: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * 测试服务器连接
     */
    @PostMapping("/test-connection")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testConnection(@RequestParam Long serverId) {
        logger.info("测试服务器连接: {}", serverId);
        
        String sessionId = generateSessionId();
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<Server> serverOpt = serverService.getServerById(serverId);
            if (serverOpt.isEmpty()) {
                result.put("success", false);
                result.put("error", "服务器不存在: " + serverId);
                return ResponseEntity.badRequest().body(result);
            }
            
            Server server = serverOpt.get();
            TestSession session = new TestSession(sessionId, "CONNECTION_TEST", server.getName(), null);
            testSessions.put(sessionId, session);
            
            // 测试SSH连接
            long startTime = System.currentTimeMillis();
            RemoteCommandService.CommandResult testResult = remoteCommandService.executeCommand(server, "echo 'Connection Test - $(date)'");
            long duration = System.currentTimeMillis() - startTime;
            
            if (testResult.isSuccess()) {
                session.setStatus("SUCCESS");
                session.addResult("连接测试成功", testResult.getOutput().trim(), duration);
                
                result.put("success", true);
                result.put("sessionId", sessionId);
                result.put("duration", duration + "ms");
                result.put("output", testResult.getOutput().trim());
                result.put("message", "服务器连接正常");
                
                logger.info("服务器 {} 连接测试成功，耗时: {}ms", server.getName(), duration);
            } else {
                session.setStatus("FAILED");
                session.addResult("连接测试失败", testResult.getError(), duration);
                
                result.put("success", false);
                result.put("sessionId", sessionId);
                result.put("error", testResult.getError());
                result.put("duration", duration + "ms");
                
                logger.error("服务器 {} 连接测试失败: {}", server.getName(), testResult.getError());
            }
            
        } catch (Exception e) {
            logger.error("测试服务器连接异常", e);
            result.put("success", false);
            result.put("error", "连接测试异常: " + e.getMessage());
            result.put("sessionId", sessionId);
            
            TestSession session = testSessions.get(sessionId);
            if (session != null) {
                session.setStatus("ERROR");
                session.addResult("连接测试异常", e.getMessage(), 0);
            }
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 测试单个用户组同步
     */
    @PostMapping("/test-sync-group")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testSyncUserGroup(@RequestParam Long groupId) {
        logger.info("测试用户组同步: {}", groupId);
        
        String sessionId = generateSessionId();
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<ServerUserGroup> groupOpt = userGroupService.getServerUserGroups(null).stream()
                .filter(g -> g.getId().equals(groupId))
                .findFirst();
            
            if (groupOpt.isEmpty()) {
                result.put("success", false);
                result.put("error", "用户组不存在: " + groupId);
                return ResponseEntity.badRequest().body(result);
            }
            
            ServerUserGroup group = groupOpt.get();
            TestSession session = new TestSession(sessionId, "GROUP_SYNC_TEST", group.getServer().getName(), group.getGroupName());
            testSessions.put(sessionId, session);
            
            // 执行同步测试
            long startTime = System.currentTimeMillis();
            UserGroupSyncResultDto syncResult = syncService.syncUserGroupToServer(group);
            long duration = System.currentTimeMillis() - startTime;
            
            // 收集详细结果
            Map<String, Object> syncDetails = new HashMap<>();
            syncDetails.put("groupName", syncResult.getGroupName());
            syncDetails.put("serverName", syncResult.getServerName());
            syncDetails.put("success", syncResult.isSuccess());
            syncDetails.put("totalSteps", syncResult.getTotalSteps());
            syncDetails.put("successfulSteps", syncResult.getSuccessfulSteps());
            syncDetails.put("failedSteps", syncResult.getFailedSteps());
            syncDetails.put("successRate", String.format("%.1f%%", syncResult.getSuccessRate() * 100));
            syncDetails.put("errorMessage", syncResult.getErrorMessage());
            syncDetails.put("duration", duration + "ms");
            
            // 详细步骤结果
            List<Map<String, Object>> stepDetails = new ArrayList<>();
            for (UserGroupSyncResultDto.SyncStepResult stepResult : syncResult.getStepResults()) {
                Map<String, Object> step = new HashMap<>();
                step.put("stepName", stepResult.getStepName());
                step.put("command", stepResult.getCommand());
                step.put("success", stepResult.isSuccess());
                step.put("output", stepResult.getOutput());
                step.put("error", stepResult.getError());
                step.put("exitCode", stepResult.getExitCode());
                step.put("executionTime", stepResult.getExecutionTimeMs() + "ms");
                stepDetails.add(step);
            }
            syncDetails.put("steps", stepDetails);
            
            result.put("success", syncResult.isSuccess());
            result.put("sessionId", sessionId);
            result.put("syncResult", syncDetails);
            
            if (syncResult.isSuccess()) {
                session.setStatus("SUCCESS");
                session.addResult("用户组同步成功", String.format("成功率: %.1f%%, 步骤: %d/%d", 
                    syncResult.getSuccessRate() * 100, syncResult.getSuccessfulSteps(), syncResult.getTotalSteps()), duration);
                logger.info("用户组 {} 同步测试成功，耗时: {}ms", group.getGroupName(), duration);
            } else {
                session.setStatus("FAILED");
                session.addResult("用户组同步失败", syncResult.getErrorMessage(), duration);
                logger.error("用户组 {} 同步测试失败: {}", group.getGroupName(), syncResult.getErrorMessage());
            }
            
        } catch (Exception e) {
            logger.error("测试用户组同步异常", e);
            result.put("success", false);
            result.put("error", "同步测试异常: " + e.getMessage());
            result.put("sessionId", sessionId);
            
            TestSession session = testSessions.get(sessionId);
            if (session != null) {
                session.setStatus("ERROR");
                session.addResult("同步测试异常", e.getMessage(), 0);
            }
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 批量测试服务器所有用户组同步
     */
    @PostMapping("/test-sync-all")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testSyncAllUserGroups(@RequestParam Long serverId) {
        logger.info("批量测试服务器用户组同步: {}", serverId);
        
        String sessionId = generateSessionId();
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<Server> serverOpt = serverService.getServerById(serverId);
            if (serverOpt.isEmpty()) {
                result.put("success", false);
                result.put("error", "服务器不存在: " + serverId);
                return ResponseEntity.badRequest().body(result);
            }
            
            Server server = serverOpt.get();
            TestSession session = new TestSession(sessionId, "BATCH_SYNC_TEST", server.getName(), "ALL_GROUPS");
            testSessions.put(sessionId, session);
            
            List<ServerUserGroup> groups = userGroupService.getServerUserGroups(serverId);
            
            if (groups.isEmpty()) {
                result.put("success", false);
                result.put("error", "服务器无用户组配置");
                return ResponseEntity.badRequest().body(result);
            }
            
            // 执行批量同步测试
            long startTime = System.currentTimeMillis();
            List<UserGroupSyncResultDto> batchResults = syncService.syncAllUserGroupsToServer(server, groups);
            long duration = System.currentTimeMillis() - startTime;
            
            // 收集批量测试结果
            Map<String, Object> batchDetails = new HashMap<>();
            batchDetails.put("serverName", server.getName());
            batchDetails.put("totalGroups", groups.size());
            batchDetails.put("duration", duration + "ms");
            
            // 统计总体结果
            int totalSuccess = 0;
            int totalFailed = 0;
            int totalSteps = 0;
            int successfulSteps = 0;
            StringBuilder errorMessages = new StringBuilder();
            
            List<Map<String, Object>> groupResults = new ArrayList<>();
            for (UserGroupSyncResultDto syncResult : batchResults) {
                if (syncResult.isSuccess()) {
                    totalSuccess++;
                } else {
                    totalFailed++;
                    if (errorMessages.length() > 0) {
                        errorMessages.append("; ");
                    }
                    errorMessages.append(syncResult.getGroupName()).append(": ").append(syncResult.getErrorMessage());
                }
                totalSteps += syncResult.getTotalSteps();
                successfulSteps += syncResult.getSuccessfulSteps();
                
                // 添加每个用户组的详细结果
                Map<String, Object> groupDetail = new HashMap<>();
                groupDetail.put("groupName", syncResult.getGroupName());
                groupDetail.put("success", syncResult.isSuccess());
                groupDetail.put("totalSteps", syncResult.getTotalSteps());
                groupDetail.put("successfulSteps", syncResult.getSuccessfulSteps());
                groupDetail.put("successRate", String.format("%.1f%%", syncResult.getSuccessRate() * 100));
                groupDetail.put("errorMessage", syncResult.getErrorMessage());
                
                // 详细步骤结果
                List<Map<String, Object>> stepDetails = new ArrayList<>();
                for (UserGroupSyncResultDto.SyncStepResult stepResult : syncResult.getStepResults()) {
                    Map<String, Object> step = new HashMap<>();
                    step.put("stepName", stepResult.getStepName());
                    step.put("command", stepResult.getCommand());
                    step.put("success", stepResult.isSuccess());
                    step.put("output", stepResult.getOutput());
                    step.put("error", stepResult.getError());
                    step.put("exitCode", stepResult.getExitCode());
                    step.put("executionTime", stepResult.getExecutionTimeMs() + "ms");
                    stepDetails.add(step);
                }
                groupDetail.put("stepResults", stepDetails);
                groupResults.add(groupDetail);
            }
            
            boolean overallSuccess = totalFailed == 0;
            batchDetails.put("success", overallSuccess);
            batchDetails.put("successfulGroups", totalSuccess);
            batchDetails.put("failedGroups", totalFailed);
            batchDetails.put("totalSteps", totalSteps);
            batchDetails.put("successfulSteps", successfulSteps);
            batchDetails.put("successRate", totalSteps > 0 ? String.format("%.1f%%", (double) successfulSteps / totalSteps * 100) : "0%");
            batchDetails.put("errorMessage", errorMessages.toString());
            batchDetails.put("groupResults", groupResults);
            
            result.put("success", overallSuccess);
            result.put("sessionId", sessionId);
            result.put("batchResult", batchDetails);
            
            if (overallSuccess) {
                session.setStatus("SUCCESS");
                session.addResult("批量同步成功", String.format("成功率: %.1f%%, 步骤: %d/%d, 用户组数: %d", 
                    totalSteps > 0 ? (double) successfulSteps / totalSteps * 100 : 0, successfulSteps, 
                    totalSteps, groups.size()), duration);
                logger.info("服务器 {} 批量同步测试成功，耗时: {}ms", server.getName(), duration);
            } else {
                session.setStatus("FAILED");
                session.addResult("批量同步失败", errorMessages.toString(), duration);
                logger.error("服务器 {} 批量同步测试失败: {}", server.getName(), errorMessages.toString());
            }
            
        } catch (Exception e) {
            logger.error("批量测试用户组同步异常", e);
            result.put("success", false);
            result.put("error", "批量同步测试异常: " + e.getMessage());
            result.put("sessionId", sessionId);
            
            TestSession session = testSessions.get(sessionId);
            if (session != null) {
                session.setStatus("ERROR");
                session.addResult("批量同步测试异常", e.getMessage(), 0);
            }
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 检查服务器系统状态
     */
    @PostMapping("/check-system-state")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkSystemState(@RequestParam Long serverId) {
        logger.info("检查服务器系统状态: {}", serverId);
        
        String sessionId = generateSessionId();
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<Server> serverOpt = serverService.getServerById(serverId);
            if (serverOpt.isEmpty()) {
                result.put("success", false);
                result.put("error", "服务器不存在: " + serverId);
                return ResponseEntity.badRequest().body(result);
            }
            
            Server server = serverOpt.get();
            TestSession session = new TestSession(sessionId, "SYSTEM_STATE_CHECK", server.getName(), "SYSTEM");
            testSessions.put(sessionId, session);
            
            Map<String, Object> systemState = new HashMap<>();
            long totalStartTime = System.currentTimeMillis();
            
            // 1. 检查/etc/group文件
            logger.info("检查 /etc/group 文件");
            long startTime = System.currentTimeMillis();
            RemoteCommandService.CommandResult groupsResult = remoteCommandService.executeCommand(
                server, "cat /etc/group | grep -E '^(dev_|ops_|admin_)' | head -20");
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> groupsInfo = new HashMap<>();
            groupsInfo.put("success", groupsResult.isSuccess());
            groupsInfo.put("output", groupsResult.getOutput());
            groupsInfo.put("error", groupsResult.getError());
            groupsInfo.put("duration", duration + "ms");
            systemState.put("groups", groupsInfo);
            
            // 2. 检查sudoers配置
            logger.info("检查 sudoers 配置");
            startTime = System.currentTimeMillis();
            RemoteCommandService.CommandResult sudoersResult = remoteCommandService.executeCommand(
                server, "ls -la /etc/sudoers.d/ && find /etc/sudoers.d/ -name 'user_group_*' -exec cat {} \\;");
            duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> sudoersInfo = new HashMap<>();
            sudoersInfo.put("success", sudoersResult.isSuccess());
            sudoersInfo.put("output", sudoersResult.getOutput());
            sudoersInfo.put("error", sudoersResult.getError());
            sudoersInfo.put("duration", duration + "ms");
            systemState.put("sudoers", sudoersInfo);
            
            // 3. 检查系统用户组
            logger.info("检查系统用户组");
            startTime = System.currentTimeMillis();
            RemoteCommandService.CommandResult sysGroupsResult = remoteCommandService.executeCommand(
                server, "getent group | grep -E '^(docker|sudo|wheel|users):' | head -10");
            duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> sysGroupsInfo = new HashMap<>();
            sysGroupsInfo.put("success", sysGroupsResult.isSuccess());
            sysGroupsInfo.put("output", sysGroupsResult.getOutput());
            sysGroupsInfo.put("error", sysGroupsResult.getError());
            sysGroupsInfo.put("duration", duration + "ms");
            systemState.put("systemGroups", sysGroupsInfo);
            
            // 4. 检查系统信息
            logger.info("检查系统基本信息");
            startTime = System.currentTimeMillis();
            RemoteCommandService.CommandResult sysInfoResult = remoteCommandService.executeCommand(
                server, "uname -a && id && whoami && pwd");
            duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> sysInfo = new HashMap<>();
            sysInfo.put("success", sysInfoResult.isSuccess());
            sysInfo.put("output", sysInfoResult.getOutput());
            sysInfo.put("error", sysInfoResult.getError());
            sysInfo.put("duration", duration + "ms");
            systemState.put("systemInfo", sysInfo);
            
            long totalDuration = System.currentTimeMillis() - totalStartTime;
            
            boolean allSuccess = groupsResult.isSuccess() && sudoersResult.isSuccess() && 
                               sysGroupsResult.isSuccess() && sysInfoResult.isSuccess();
            
            result.put("success", allSuccess);
            result.put("sessionId", sessionId);
            result.put("systemState", systemState);
            result.put("totalDuration", totalDuration + "ms");
            
            if (allSuccess) {
                session.setStatus("SUCCESS");
                session.addResult("系统状态检查成功", "所有检查项通过", totalDuration);
                logger.info("服务器 {} 系统状态检查成功，耗时: {}ms", server.getName(), totalDuration);
            } else {
                session.setStatus("PARTIAL");
                session.addResult("系统状态检查部分失败", "部分检查项失败", totalDuration);
                logger.warn("服务器 {} 系统状态检查部分失败", server.getName());
            }
            
        } catch (Exception e) {
            logger.error("检查服务器系统状态异常", e);
            result.put("success", false);
            result.put("error", "系统状态检查异常: " + e.getMessage());
            result.put("sessionId", sessionId);
            
            TestSession session = testSessions.get(sessionId);
            if (session != null) {
                session.setStatus("ERROR");
                session.addResult("系统状态检查异常", e.getMessage(), 0);
            }
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取测试会话详情
     */
    @GetMapping("/session/{sessionId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSessionDetails(@PathVariable String sessionId) {
        TestSession session = testSessions.get(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> details = new HashMap<>();
        details.put("sessionId", sessionId);
        details.put("testType", session.getTestType());
        details.put("status", session.getStatus());
        details.put("serverName", session.getServerName());
        details.put("groupName", session.getGroupName());
        details.put("createdAt", session.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        details.put("results", session.getResults());
        
        return ResponseEntity.ok(details);
    }
    
    /**
     * 清理测试会话
     */
    @DeleteMapping("/session/{sessionId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cleanupSession(@PathVariable String sessionId) {
        TestSession session = testSessions.remove(sessionId);
        Map<String, Object> result = new HashMap<>();
        
        if (session != null) {
            result.put("success", true);
            result.put("message", "测试会话已清理: " + sessionId);
        } else {
            result.put("success", false);
            result.put("error", "测试会话不存在: " + sessionId);
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 清理所有测试会话
     */
    @DeleteMapping("/sessions/cleanup")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cleanupAllSessions() {
        int cleanedCount = testSessions.size();
        testSessions.clear();
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "已清理 " + cleanedCount + " 个测试会话");
        result.put("cleanedCount", cleanedCount);
        
        logger.info("清理了所有测试会话，共 {} 个", cleanedCount);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 生成会话ID
     */
    private String generateSessionId() {
        return "TEST_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
    
    /**
     * 测试会话类
     */
    private static class TestSession {
        private final String testType;
        private final String serverName;
        private final String groupName;
        private final LocalDateTime createdAt;
        private String status = "RUNNING";
        private final List<Map<String, Object>> results = new ArrayList<>();
        
        public TestSession(String sessionId, String testType, String serverName, String groupName) {
            this.testType = testType;
            this.serverName = serverName;
            this.groupName = groupName;
            this.createdAt = LocalDateTime.now();
        }
        
        public void addResult(String description, String details, long duration) {
            Map<String, Object> result = new HashMap<>();
            result.put("description", description);
            result.put("details", details);
            result.put("duration", duration + "ms");
            result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")));
            results.add(result);
        }
        
        public String getTestType() { return testType; }
        public String getServerName() { return serverName; }
        public String getGroupName() { return groupName; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public List<Map<String, Object>> getResults() { return results; }
    }
}