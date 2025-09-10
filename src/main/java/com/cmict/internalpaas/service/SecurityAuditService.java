package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * 安全审计日志服务
 * 记录所有安全相关的操作和事件
 * 
 * @author Claude AI
 */
@Service
public class SecurityAuditService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityAuditService.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");
    
    private static final DateTimeFormatter AUDIT_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * 记录命令执行审计日志
     * 
     * @param user 执行用户
     * @param server 目标服务器
     * @param command 执行的命令
     * @param result 执行结果
     * @param validationPassed 是否通过安全验证
     * @param executionTimeMs 执行时间（毫秒）
     */
    public void logCommandExecution(User user, Server server, String command, String result, 
                                   boolean validationPassed, long executionTimeMs) {
        try {
            String timestamp = LocalDateTime.now().format(AUDIT_TIME_FORMAT);
            String userInfo = user != null ? user.getUsername() + "(" + getUserRoleString(user) + ")" : "SYSTEM";
            String serverInfo = server.getHostname() + ":" + server.getSshPort();
            
            AuditEvent event = AuditEvent.builder()
                .timestamp(timestamp)
                .eventType("COMMAND_EXECUTION")
                .user(userInfo)
                .server(serverInfo)
                .command(command)
                .result(result != null ? result.substring(0, Math.min(result.length(), 200)) : "")
                .validationPassed(validationPassed)
                .executionTimeMs(executionTimeMs)
                .build();
            
            // 记录到专用审计日志
            auditLogger.info("AUDIT|{}|{}|{}|{}|{}|{}|{}|{}", 
                timestamp, "COMMAND_EXECUTION", userInfo, serverInfo, 
                command, validationPassed ? "PASS" : "FAIL", executionTimeMs, 
                result != null ? "SUCCESS" : "ERROR");
            
            // 异步处理审计事件（可扩展为数据库存储、发送告警等）
            CompletableFuture.runAsync(() -> processAuditEvent(event));
            
        } catch (Exception e) {
            logger.error("记录审计日志失败", e);
        }
    }

    /**
     * 记录安全违规事件
     * 
     * @param user 用户
     * @param server 服务器
     * @param command 违规命令
     * @param violationType 违规类型
     * @param reason 违规原因
     */
    public void logSecurityViolation(User user, Server server, String command, 
                                    String violationType, String reason) {
        try {
            String timestamp = LocalDateTime.now().format(AUDIT_TIME_FORMAT);
            String userInfo = user != null ? user.getUsername() + "(" + getUserRoleString(user) + ")" : "UNKNOWN";
            String serverInfo = server != null ? server.getHostname() + ":" + server.getSshPort() : "UNKNOWN";
            
            // 记录安全违规到审计日志
            auditLogger.warn("SECURITY_VIOLATION|{}|{}|{}|{}|{}|{}", 
                timestamp, userInfo, serverInfo, command, violationType, reason);
            
            // 同时记录到普通日志
            logger.warn("安全违规检测: 用户={}, 服务器={}, 命令={}, 类型={}, 原因={}", 
                userInfo, serverInfo, command, violationType, reason);
            
            // 异步处理安全违规事件（可扩展为发送告警、锁定账户等）
            CompletableFuture.runAsync(() -> processSecurityViolation(
                userInfo, serverInfo, command, violationType, reason));
            
        } catch (Exception e) {
            logger.error("记录安全违规日志失败", e);
        }
    }

    /**
     * 记录用户认证事件
     * 
     * @param username 用户名
     * @param success 是否成功
     * @param source 来源IP/系统
     * @param reason 失败原因（如果失败）
     */
    public void logAuthenticationEvent(String username, boolean success, String source, String reason) {
        try {
            String timestamp = LocalDateTime.now().format(AUDIT_TIME_FORMAT);
            
            auditLogger.info("AUTH|{}|{}|{}|{}|{}", 
                timestamp, username, success ? "SUCCESS" : "FAILURE", source, 
                reason != null ? reason : "");
            
            if (!success) {
                logger.warn("认证失败: 用户={}, 来源={}, 原因={}", username, source, reason);
            }
            
        } catch (Exception e) {
            logger.error("记录认证日志失败", e);
        }
    }

    /**
     * 记录权限检查事件
     * 
     * @param user 用户
     * @param resource 资源
     * @param action 操作
     * @param granted 是否授权
     */
    public void logPermissionCheck(User user, String resource, String action, boolean granted) {
        try {
            String timestamp = LocalDateTime.now().format(AUDIT_TIME_FORMAT);
            String userInfo = user != null ? user.getUsername() + "(" + getUserRoleString(user) + ")" : "UNKNOWN";
            
            auditLogger.info("PERMISSION|{}|{}|{}|{}|{}", 
                timestamp, userInfo, resource, action, granted ? "GRANTED" : "DENIED");
            
            if (!granted) {
                logger.warn("权限拒绝: 用户={}, 资源={}, 操作={}", userInfo, resource, action);
            }
            
        } catch (Exception e) {
            logger.error("记录权限检查日志失败", e);
        }
    }

    /**
     * 记录数据访问事件
     * 
     * @param user 用户
     * @param dataType 数据类型
     * @param operation 操作（CREATE/READ/UPDATE/DELETE）
     * @param recordId 记录ID
     * @param success 是否成功
     */
    public void logDataAccess(User user, String dataType, String operation, String recordId, boolean success) {
        try {
            String timestamp = LocalDateTime.now().format(AUDIT_TIME_FORMAT);
            String userInfo = user != null ? user.getUsername() + "(" + getUserRoleString(user) + ")" : "SYSTEM";
            
            auditLogger.info("DATA_ACCESS|{}|{}|{}|{}|{}|{}", 
                timestamp, userInfo, dataType, operation, recordId, success ? "SUCCESS" : "FAILURE");
            
        } catch (Exception e) {
            logger.error("记录数据访问日志失败", e);
        }
    }

    /**
     * 处理审计事件（可扩展）
     */
    private void processAuditEvent(AuditEvent event) {
        try {
            // TODO: 可以扩展为以下功能：
            // 1. 存储到数据库
            // 2. 发送到日志中心（如ELK Stack）
            // 3. 触发告警规则
            // 4. 生成审计报告
            
            logger.debug("处理审计事件: {}", event);
            
        } catch (Exception e) {
            logger.error("处理审计事件失败", e);
        }
    }

    /**
     * 处理安全违规事件
     */
    private void processSecurityViolation(String user, String server, String command, 
                                        String violationType, String reason) {
        try {
            // TODO: 可以扩展为以下功能：
            // 1. 发送安全告警邮件
            // 2. 更新安全风险评分
            // 3. 触发自动响应（如临时锁定账户）
            // 4. 记录到安全事件数据库
            
            logger.info("处理安全违规: 用户={}, 服务器={}, 命令={}, 类型={}, 原因={}", 
                user, server, command, violationType, reason);
            
        } catch (Exception e) {
            logger.error("处理安全违规事件失败", e);
        }
    }

    /**
     * 获取用户角色字符串
     */
    private String getUserRoleString(User user) {
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return "UNKNOWN";
        }
        return user.getRoles().stream()
            .map(role -> role.name())
            .reduce((r1, r2) -> r1 + "," + r2)
            .orElse("UNKNOWN");
    }

    /**
     * 审计事件数据类
     */
    public static class AuditEvent {
        private String timestamp;
        private String eventType;
        private String user;
        private String server;
        private String command;
        private String result;
        private boolean validationPassed;
        private long executionTimeMs;

        // Builder模式
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private AuditEvent event = new AuditEvent();

            public Builder timestamp(String timestamp) {
                event.timestamp = timestamp;
                return this;
            }

            public Builder eventType(String eventType) {
                event.eventType = eventType;
                return this;
            }

            public Builder user(String user) {
                event.user = user;
                return this;
            }

            public Builder server(String server) {
                event.server = server;
                return this;
            }

            public Builder command(String command) {
                event.command = command;
                return this;
            }

            public Builder result(String result) {
                event.result = result;
                return this;
            }

            public Builder validationPassed(boolean validationPassed) {
                event.validationPassed = validationPassed;
                return this;
            }

            public Builder executionTimeMs(long executionTimeMs) {
                event.executionTimeMs = executionTimeMs;
                return this;
            }

            public AuditEvent build() {
                return event;
            }
        }

        // Getters
        public String getTimestamp() { return timestamp; }
        public String getEventType() { return eventType; }
        public String getUser() { return user; }
        public String getServer() { return server; }
        public String getCommand() { return command; }
        public String getResult() { return result; }
        public boolean isValidationPassed() { return validationPassed; }
        public long getExecutionTimeMs() { return executionTimeMs; }

        @Override
        public String toString() {
            return "AuditEvent{" +
                   "timestamp='" + timestamp + '\'' +
                   ", eventType='" + eventType + '\'' +
                   ", user='" + user + '\'' +
                   ", server='" + server + '\'' +
                   ", command='" + command + '\'' +
                   ", validationPassed=" + validationPassed +
                   ", executionTimeMs=" + executionTimeMs +
                   '}';
        }
    }
}