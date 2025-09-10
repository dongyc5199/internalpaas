package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 命令安全服务
 * 提供命令注入防护、白名单验证、参数转义等安全功能
 * 
 * @author Claude AI
 */
@Service
public class CommandSecurityService {

    private static final Logger logger = LoggerFactory.getLogger(CommandSecurityService.class);

    // 基础系统命令白名单（所有用户都可执行）
    private static final Set<String> BASIC_COMMANDS = Set.of(
        // 系统信息查询
        "whoami", "id", "pwd", "date", "uptime", "uname",
        // 进程和系统状态
        "ps", "top", "htop", "who", "w", "last", "users",
        // 磁盘和内存信息
        "df", "free", "du", "lsblk", "mount",
        // 网络状态
        "netstat", "ss", "ping", "traceroute", "nslookup", "dig",
        // 文件操作（安全的）
        "ls", "cat", "head", "tail", "wc", "grep", "find", "locate",
        // 压缩解压（查看）
        "tar", "gzip", "gunzip", "zip", "unzip",
        // 文本处理
        "awk", "sed", "sort", "uniq", "cut", "tr",
        // 其他安全命令
        "echo", "printf", "test", "which", "whereis", "type"
    );

    // 管理员命令白名单（仅管理员可执行）
    private static final Set<String> ADMIN_COMMANDS = Set.of(
        // 用户管理
        "useradd", "userdel", "usermod", "passwd", "chpasswd", "groups", "groupadd", "groupdel",
        // 权限管理
        "chmod", "chown", "chgrp", "sudo", "su",
        // 系统管理
        "systemctl", "service", "systemd", "crontab", "at",
        // 包管理
        "apt", "yum", "dnf", "rpm", "dpkg", "snap",
        // 进程管理
        "kill", "killall", "pkill", "nohup",
        // 网络管理
        "iptables", "ufw", "firewall-cmd", "ifconfig", "ip",
        // 文件系统管理
        "mkfs", "fsck", "mount", "umount", "fdisk", "parted",
        // 目录操作
        "mkdir", "rmdir", "rm", "mv", "cp", "ln"
    );

    // 危险命令模式（禁止执行）
    private static final Set<Pattern> DANGEROUS_PATTERNS = Set.of(
        Pattern.compile(".*[;&|`$()].*"),           // 包含命令连接符
        Pattern.compile(".*>\\s*/dev/.*"),          // 重定向到设备文件
        Pattern.compile(".*\\$\\(.*\\).*"),         // 命令替换
        Pattern.compile(".*`.*`.*"),                // 反引号命令替换
        Pattern.compile(".*/etc/passwd.*"),         // 访问敏感文件
        Pattern.compile(".*/etc/shadow.*"),         // 访问密码文件
        Pattern.compile(".*\\.\\..*"),              // 路径遍历
        Pattern.compile(".*\\\\x[0-9a-fA-F]+.*"),   // 十六进制编码
        Pattern.compile(".*%[0-9a-fA-F]+.*"),       // URL编码
        Pattern.compile(".*\\\\[0-7]+.*")           // 八进制编码
    );

    // 允许的文件路径模式
    private static final Set<Pattern> SAFE_PATH_PATTERNS = Set.of(
        Pattern.compile("^[\"']?/home/[a-zA-Z0-9_-]+/.*[\"']?$"),    // 用户家目录
        Pattern.compile("^[\"']?/tmp/.*[\"']?$"),                     // 临时目录
        Pattern.compile("^[\"']?/var/log/.*[\"']?$"),                 // 日志目录
        Pattern.compile("^[\"']?/proc/.*[\"']?$"),                    // 系统信息
        Pattern.compile("^[\"']?/sys/.*[\"']?$"),                     // 系统信息
        Pattern.compile("^[\"']?/opt/[a-zA-Z0-9_-]+(/.*)?[\"']?$"),  // /opt目录下的应用目录
        Pattern.compile("^[\"']?/usr/local/[a-zA-Z0-9_-]+(/.*)?[\"']?$"), // /usr/local下的应用目录
        Pattern.compile("^[\"']?/srv/[a-zA-Z0-9_-]+(/.*)?[\"']?$"),  // /srv下的服务目录
        Pattern.compile("^[\"']?/data/[a-zA-Z0-9_-]+(/.*)?[\"']?$"), // /data下的数据目录
        Pattern.compile("^[\"']?/app/[a-zA-Z0-9_-]+(/.*)?[\"']?$"),  // /app下的应用目录
        Pattern.compile("^[\"']?\\./[a-zA-Z0-9_.-]+[\"']?$"),        // 当前目录下的文件
        Pattern.compile("^[\"']?[a-zA-Z0-9_.-]+[\"']?$")             // 纯文件名
    );

    @Value("${app.security.command.strict-mode:true}")
    private boolean strictMode;

    @Value("${app.security.command.log-all-commands:true}")
    private boolean logAllCommands;

    @Value("${app.security.command.max-command-length:1000}")
    private int maxCommandLength;

    private Set<String> allAllowedCommands;

    @PostConstruct
    public void init() {
        // 初始化允许的命令集合
        allAllowedCommands = new HashSet<>();
        allAllowedCommands.addAll(BASIC_COMMANDS);
        allAllowedCommands.addAll(ADMIN_COMMANDS);
        
        logger.info("命令安全服务初始化完成 - 严格模式: {}, 日志记录: {}, 最大命令长度: {}", 
                   strictMode, logAllCommands, maxCommandLength);
        logger.info("允许的基础命令数量: {}, 管理员命令数量: {}", 
                   BASIC_COMMANDS.size(), ADMIN_COMMANDS.size());
    }

    /**
     * 验证命令是否安全
     * 
     * @param command 要验证的命令
     * @param user 执行命令的用户
     * @return 验证结果
     */
    public CommandValidationResult validateCommand(String command, User user) {
        if (logAllCommands) {
            logger.info("命令安全验证: 用户={}, 命令={}", 
                       user != null ? user.getUsername() : "unknown", command);
        }

        try {
            // 1. 基础验证
            CommandValidationResult basicResult = performBasicValidation(command);
            if (!basicResult.isValid()) {
                return basicResult;
            }

            // 2. 危险模式检测
            CommandValidationResult dangerResult = checkDangerousPatterns(command);
            if (!dangerResult.isValid()) {
                return dangerResult;
            }

            // 3. 命令白名单验证
            CommandValidationResult whitelistResult = validateCommandWhitelist(command, user);
            if (!whitelistResult.isValid()) {
                return whitelistResult;
            }

            // 4. 参数安全验证
            CommandValidationResult paramResult = validateCommandParameters(command);
            if (!paramResult.isValid()) {
                return paramResult;
            }

            logger.debug("命令验证通过: {}", command);
            return CommandValidationResult.success("命令验证通过");

        } catch (Exception e) {
            logger.error("命令验证异常: {}", command, e);
            return CommandValidationResult.error("命令验证异常: " + e.getMessage());
        }
    }

    /**
     * 转义命令参数，防止注入攻击
     * 
     * @param command 原始命令
     * @return 转义后的安全命令
     */
    public String escapeCommand(String command) {
        if (command == null || command.isEmpty()) {
            return command;
        }

        // 移除危险字符
        String escaped = command
            .replaceAll("[;&|`$()]", "")           // 移除命令连接符
            .replaceAll("\\$\\([^)]*\\)", "")      // 移除命令替换
            .replaceAll("`[^`]*`", "")             // 移除反引号
            .replaceAll("\\\\x[0-9a-fA-F]+", "")   // 移除十六进制编码
            .replaceAll("%[0-9a-fA-F]+", "")       // 移除URL编码
            .replaceAll("\\\\[0-7]+", "");         // 移除八进制编码

        // 转义单引号和双引号
        escaped = escaped.replace("'", "\\'").replace("\"", "\\\"");

        if (!escaped.equals(command)) {
            logger.warn("命令已被转义: 原始={}, 转义后={}", command, escaped);
        }

        return escaped;
    }

    /**
     * 获取用户可执行的命令列表
     * 
     * @param user 用户
     * @return 可执行命令列表
     */
    public Set<String> getAllowedCommands(User user) {
        Set<String> allowedCommands = new HashSet<>(BASIC_COMMANDS);
        
        if (user != null && isAdmin(user)) {
            allowedCommands.addAll(ADMIN_COMMANDS);
        }
        
        return allowedCommands;
    }

    /**
     * 检查用户是否为管理员
     */
    private boolean isAdmin(User user) {
        if (user == null || user.getRoles() == null) return false;
        
        // 检查用户是否有管理员角色
        return user.getRoles().stream()
            .anyMatch(role -> "ADMIN".equals(role.name()) || "SUPER_ADMIN".equals(role.name()));
    }

    /**
     * 基础验证
     */
    private CommandValidationResult performBasicValidation(String command) {
        if (command == null || command.trim().isEmpty()) {
            return CommandValidationResult.error("命令不能为空");
        }

        command = command.trim();
        
        if (command.length() > maxCommandLength) {
            return CommandValidationResult.error("命令长度超过限制: " + maxCommandLength);
        }

        return CommandValidationResult.success("基础验证通过");
    }

    /**
     * 检查危险模式
     */
    private CommandValidationResult checkDangerousPatterns(String command) {
        for (Pattern pattern : DANGEROUS_PATTERNS) {
            if (pattern.matcher(command).matches()) {
                String reason = "命令包含危险模式: " + pattern.pattern();
                logger.warn("检测到危险命令: {} - {}", command, reason);
                return CommandValidationResult.error(reason);
            }
        }
        return CommandValidationResult.success("危险模式检查通过");
    }

    /**
     * 白名单验证
     */
    private CommandValidationResult validateCommandWhitelist(String command, User user) {
        String[] parts = command.trim().split("\\s+");
        String baseCommand = parts[0];

        // 检查基础命令
        if (BASIC_COMMANDS.contains(baseCommand)) {
            return CommandValidationResult.success("基础命令验证通过");
        }

        // 检查管理员命令
        if (ADMIN_COMMANDS.contains(baseCommand)) {
            if (isAdmin(user)) {
                return CommandValidationResult.success("管理员命令验证通过");
            } else {
                String reason = "普通用户无权执行管理员命令: " + baseCommand;
                logger.warn("权限不足: 用户={}, 命令={}", 
                           user != null ? user.getUsername() : "unknown", baseCommand);
                return CommandValidationResult.error(reason);
            }
        }

        // 严格模式下，不在白名单的命令一律拒绝
        if (strictMode) {
            String reason = "命令不在白名单中: " + baseCommand;
            logger.warn("白名单验证失败: {}", reason);
            return CommandValidationResult.error(reason);
        }

        return CommandValidationResult.success("白名单验证通过");
    }

    /**
     * 参数安全验证
     */
    private CommandValidationResult validateCommandParameters(String command) {
        String[] parts = command.trim().split("\\s+");
        
        // 检查文件路径参数
        for (int i = 1; i < parts.length; i++) {
            String param = parts[i];
            
            // 跳过选项参数
            if (param.startsWith("-")) {
                continue;
            }
            
            // 检查是否为文件路径
            if (param.contains("/") || param.contains("\\")) {
                if (!isValidFilePath(param)) {
                    String reason = "不安全的文件路径参数: " + param;
                    logger.warn("文件路径验证失败: {}", reason);
                    return CommandValidationResult.error(reason);
                }
            }
        }
        
        return CommandValidationResult.success("参数验证通过");
    }

    /**
     * 验证文件路径是否安全
     */
    private boolean isValidFilePath(String path) {
        // 检查安全路径模式
        for (Pattern pattern : SAFE_PATH_PATTERNS) {
            if (pattern.matcher(path).matches()) {
                return true;
            }
        }
        
        logger.debug("文件路径不在安全模式中: {}", path);
        return false;
    }

    /**
     * 命令验证结果类
     */
    public static class CommandValidationResult {
        private final boolean valid;
        private final String message;
        private final String errorCode;

        private CommandValidationResult(boolean valid, String message, String errorCode) {
            this.valid = valid;
            this.message = message;
            this.errorCode = errorCode;
        }

        public static CommandValidationResult success(String message) {
            return new CommandValidationResult(true, message, null);
        }

        public static CommandValidationResult error(String message) {
            return new CommandValidationResult(false, message, "VALIDATION_FAILED");
        }

        public static CommandValidationResult error(String message, String errorCode) {
            return new CommandValidationResult(false, message, errorCode);
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public String getErrorCode() { return errorCode; }

        @Override
        public String toString() {
            return "CommandValidationResult{" +
                   "valid=" + valid +
                   ", message='" + message + '\'' +
                   ", errorCode='" + errorCode + '\'' +
                   '}';
        }
    }

    /**
     * 命令安全异常
     */
    public static class CommandSecurityException extends RuntimeException {
        private final String errorCode;

        public CommandSecurityException(String message) {
            this(message, "SECURITY_VIOLATION");
        }

        public CommandSecurityException(String message, String errorCode) {
            super(message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }
}