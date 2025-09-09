package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.UserActivity;
import com.cmict.internalpaas.service.RemoteCommandService.CommandResult;
import com.jcraft.jsch.*;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.Data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class SshConnectionService {

    private static final Logger logger = LoggerFactory.getLogger(SshConnectionService.class);

    @Autowired
    private PasswordEncryptionService passwordEncryptionService;

    private static final int CONNECTION_TIMEOUT = 15000; // 15秒连接超时 (从8秒增加)
    private static final int CHECK_TIMEOUT = 20000; // 20秒检测超时 (从10秒增加)
    private static final int COMMAND_TIMEOUT = 30000; // 30秒命令超时 (从15秒增加)

    /**
     * 检查服务器SSH连接状态
     */
    public Server.ConnectionStatus checkConnection(Server server) {
        if (server == null || server.getHostname() == null) {
            return Server.ConnectionStatus.FAILED;
        }

        try {
            logger.info("开始SSH连接检查: {}@{}:{}",
                    server.getSshUsername() != null ? server.getSshUsername() : "root",
                    server.getHostname(),
                    server.getSshPort() != null ? server.getSshPort() : 22);

            return CompletableFuture.supplyAsync(() -> performConnectionCheck(server))
                    .get(CHECK_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            logger.warn("SSH连接检查超时({}ms): {} - 建议检查网络连接或增加超时时间", CHECK_TIMEOUT, server.getHostname());
            return Server.ConnectionStatus.TIMEOUT;
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof JSchException && cause.getMessage() != null
                    && cause.getMessage().contains("Auth fail")) {
                logger.warn("SSH连接检查失败 - 认证错误: {} (用户名: {}, 端口: {})",
                        server.getHostname(),
                        server.getSshUsername() != null ? server.getSshUsername() : "root",
                        server.getSshPort() != null ? server.getSshPort() : 22);
                return Server.ConnectionStatus.AUTH_FAILED;
            } else {
                logger.warn("SSH连接检查失败: {} - 原因: {}", server.getHostname(),
                        cause != null ? cause.getMessage() : "未知错误");
                return Server.ConnectionStatus.FAILED;
            }
        } catch (Exception e) {
            logger.error("SSH连接检查异常: {} - 异常类型: {}, 错误信息: {}",
                    server.getHostname(), e.getClass().getSimpleName(), e.getMessage(), e);
            return Server.ConnectionStatus.FAILED;
        }
    }

    /**
     * 执行实际的SSH连接检查
     */
    private Server.ConnectionStatus performConnectionCheck(Server server) {
        JSch jsch = new JSch();
        Session session = null;
        ChannelExec channel = null;

        try {
            // 设置主机密钥检查为不检查（生产环境应配置已知主机）
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            config.put("PreferredAuthentications", "password");
            config.put("PasswordAuthentication", "yes");

            session = jsch.getSession(
                    server.getSshUsername() != null ? server.getSshUsername() : "root",
                    server.getHostname(),
                    server.getSshPort() != null ? server.getSshPort() : 22);

            session.setConfig(config);
            session.setTimeout(CONNECTION_TIMEOUT);

            // 设置认证方式 - 仅使用密码认证
            String decryptedPassword = getDecryptedPassword(server);
            if (decryptedPassword != null && !decryptedPassword.isEmpty()) {
                session.setPassword(decryptedPassword);
                logger.debug("使用密码认证: 用户名={}, 主机={}, 端口={}",
                        session.getUserName(), server.getHostname(), server.getSshPort());
            } else {
                logger.warn("未提供密码，无法进行SSH认证: {}", server.getHostname());
                return Server.ConnectionStatus.AUTH_FAILED;
            }

            // 尝试连接
            logger.debug("开始SSH连接: {}@{}:{}", session.getUserName(), server.getHostname(), server.getSshPort());
            long startTime = System.currentTimeMillis();
            session.connect(CONNECTION_TIMEOUT);
            long connectTime = System.currentTimeMillis() - startTime;

            // 如果会话连接成功，直接返回成功状态
            if (session.isConnected()) {
                logger.info("SSH会话连接成功: {} (耗时: {}ms)", server.getHostname(), connectTime);
                return Server.ConnectionStatus.CONNECTED;
            } else {
                logger.warn("SSH会话连接失败: {} (耗时: {}ms)", server.getHostname(), connectTime);
                return Server.ConnectionStatus.FAILED;
            }

        } catch (JSchException e) {
            if (e.getMessage() != null && e.getMessage().contains("Auth fail")) {
                logger.warn("SSH认证失败: {}", server.getHostname());
                return Server.ConnectionStatus.AUTH_FAILED;
            } else {
                logger.warn("SSH连接失败: {}", server.getHostname(), e);
                return Server.ConnectionStatus.FAILED;
            }
        } catch (Exception e) {
            logger.error("SSH连接检查异常: {}", server.getHostname(), e);
            return Server.ConnectionStatus.FAILED;
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * 测试服务器是否可达（不尝试认证）
     */
    public boolean isReachable(Server server) {
        try {
            String host = server.getHostname();
            int port = server.getSshPort() != null ? server.getSshPort() : 22;

            java.net.Socket socket = new java.net.Socket();
            socket.connect(new java.net.InetSocketAddress(host, port), 2000);
            socket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 执行远程命令
     */
    public String executeCommand(Server server, String command) throws Exception {
        return executeCommand(server, command, COMMAND_TIMEOUT,new CommandOptions()).getOutput();
    }

    /**
     * 执行远程命令（指定超时时间）
     */
    public String executeCommand(Server server, String command, int timeoutMs) throws Exception {
        return executeCommand(server, command, timeoutMs,new CommandOptions()).getOutput();
    }

    /**
     * 执行SSH远程命令的通用方法
     * 
     * @param server    服务器信息
     * @param command   要执行的命令
     * @param timeoutMs 超时时间(毫秒)
     * @param options   执行选项(可选)
     * @return 命令执行结果
     * @throws Exception 执行过程中的异常
     */
    public CommandResult executeCommand(Server server, String command, int timeoutMs,
            CommandOptions options) throws Exception {
        // 参数验证
        if (server == null)
            throw new IllegalArgumentException("服务器信息不能为空");
        if (command == null || command.trim().isEmpty())
            throw new IllegalArgumentException("命令不能为空");
        if (timeoutMs <= 0)
            throw new IllegalArgumentException("超时时间必须大于0");

        // 使用默认选项
        if (options == null)
            options = new CommandOptions();

        long startTime = System.currentTimeMillis();
        JSch jsch = new JSch();
        Session session = null;
        ChannelExec channel = null;
        InputStream inputStream = null;
        InputStream errorStream = null;

        logger.info("开始执行远程命令: {} @ {} (超时: {}ms, 选项: {})",
                command, server.getHostname(), timeoutMs, options);

        try {
            // 建立SSH会话
            session = createSession(jsch, server);
            if (options.getSessionConfig() != null) {
                for (Map.Entry<String, String> entry : options.getSessionConfig().entrySet()) {
                    session.setConfig(entry.getKey(), entry.getValue());
                }
            }
            session.connect(CONNECTION_TIMEOUT);

            // 创建执行通道
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            // 设置终端模式(对交互式命令很重要)
            channel.setPty(options.isUsePty());

            // 设置流
            channel.setInputStream(null);
            ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
            if (options.isCaptureErrorOutput()) {
                channel.setErrStream(errorOutput);
            } else {
                channel.setErrStream(System.err);
            }

            // 连接通道
            channel.connect(Math.min(timeoutMs / 2, 15000));

            // 获取输入流
            inputStream = channel.getInputStream();
            errorStream = channel.getErrStream();

            // 读取输出
            CommandResult result = readCommandOutput(channel, inputStream, errorStream, errorOutput,
                    command, server.getHostname(), startTime, timeoutMs, options);

            // 处理退出状态
            int exitStatus = channel.getExitStatus();
            result.setExitStatus(exitStatus);

            long totalTime = System.currentTimeMillis() - startTime;
            result.setExecutionTimeMs(totalTime);

            logger.info("远程命令执行完成: {} @ {} (总耗时: {}ms, 退出码: {}, 输出长度: {})",
                    command, server.getHostname(), totalTime, exitStatus, result.getOutput().length());

            if (exitStatus != 0 && !options.isIgnoreExitStatus()) {
                logger.warn("远程命令执行错误: {} @ {} (退出码: {})", command, server.getHostname(), exitStatus);
                if (options.isThrowOnError()) {
                    throw new CommandExecutionException("命令执行失败，退出码: " + exitStatus, result);
                }
            }

            return result;

        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            handleExecutionException(e, command, server.getHostname(), totalTime);
            throw e;
        } finally {
            cleanupResources(inputStream, errorStream, channel, session, command, server.getHostname(), startTime);
        }
    }

    private void handleExecutionException(Exception e, String command, String hostname, long totalTime) {
        if (e instanceof JSchException) {
            JSchException je = (JSchException) e;
            if (je.getMessage() != null) {
                if (je.getMessage().contains("Auth fail")) {
                    logger.error("SSH认证错误: {} @ {} (耗时: {}ms)", command, hostname, totalTime);
                } else if (je.getMessage().contains("timeout")) {
                    logger.error("连接超时: {} @ {} (耗时: {}ms)", command, hostname, totalTime);
                } else {
                    logger.error("SSH错误: {} @ {} (耗时: {}ms, 错误: {})",
                            command, hostname, totalTime, je.getMessage());
                }
            }
        } else if (e instanceof java.io.IOException) {
            logger.error("IO错误: {} @ {} (耗时: {}ms)", command, hostname, totalTime, e);
        } else if (e instanceof InterruptedException) {
            logger.error("命令执行被中断: {} @ {} (耗时: {}ms)", command, hostname, totalTime);
            Thread.currentThread().interrupt();
        } else {
            logger.error("命令执行异常: {} @ {} (耗时: {}ms, 异常: {})",
                    command, hostname, totalTime, e.getClass().getSimpleName(), e);
        }
    }

    private void cleanupResources(InputStream inputStream, InputStream errorStream, ChannelExec channel,
            Session session,
            String command, String hostname, long startTime) {
        long totalTime = System.currentTimeMillis() - startTime;
        logger.debug("清理资源: {} @ {} (总耗时: {}ms)", command, hostname, totalTime);

        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Exception e) {
                logger.debug("关闭输入流异常", e);
            }
        }

        // 关闭错误输入流
        if (errorStream != null) {
            try {
                errorStream.close();
                logger.debug("关闭错误输入流成功");
            } catch (Exception e) {
                logger.debug("关闭错误输入流异常", e);
            }
        }

        if (channel != null && channel.isConnected()) {
            try {
                channel.disconnect();
                logger.debug("关闭命令通道成功");
            } catch (Exception e) {
                logger.debug("关闭命令通道异常", e);
            }
        }

        if (session != null && session.isConnected()) {
            try {
                session.disconnect();
                logger.debug("SSH会话关闭成功");
            } catch (Exception e) {
                logger.debug("SSH会话关闭异常", e);
            }
        }
    }

    /**
     * 非阻塞方式读取命令输出
     */
    private CommandResult readCommandOutput(ChannelExec channel, InputStream inputStream,
            InputStream errorStream, ByteArrayOutputStream errorOutput,
            String command, String hostname,
            long startTime, int timeoutMs, CommandOptions options) throws Exception {

        StringBuilder output = new StringBuilder();
        byte[] buffer = new byte[options.getBufferSize()];
        String charset = options.getCharset();

        long readStartTime = System.currentTimeMillis();
        int maxOutputSize = options.getMaxOutputSize();
        boolean outputLimitReached = false;

        while (true) {
            // 检查超时
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed > timeoutMs) {
                logger.warn("命令执行超时: {} @ {} (已耗时: {}ms, 阈值: {}ms)",
                        command, hostname, elapsed, timeoutMs);
                throw new CommandTimeoutException("命令执行超时: " + elapsed + "ms", output.toString());
            }

            boolean hasOutput = false;

            // 读取标准输出
            while (inputStream.available() > 0) {
                int bytesRead = inputStream.read(buffer);
                if (bytesRead > 0) {
                    // 检查输出大小限制
                    if (maxOutputSize > 0 && output.length() + bytesRead > maxOutputSize && !outputLimitReached) {
                        logger.warn("命令输出超过最大限制 {}B: {} @ {}",
                                maxOutputSize, command, hostname);
                        outputLimitReached = true;
                        // 根据选项决定是抛出异常还是截断输出
                        if (options.isThrowOnOutputLimit()) {
                            throw new CommandOutputLimitException(
                                    "命令输出超过最大限制: " + maxOutputSize + "字节", output.toString());
                        }
                    }

                    if (maxOutputSize <= 0 || output.length() < maxOutputSize) {
                        output.append(new String(buffer, 0, Math.min(bytesRead,
                                maxOutputSize - output.length()), charset));
                    }
                    hasOutput = true;
                }
            }

            // 检查命令是否执行完成
            if (channel.isClosed()) {
                // 命令完成后，最后读取一次剩余的输出
                while (inputStream.available() > 0) {
                    int bytesRead = inputStream.read(buffer);
                    if (bytesRead > 0) {
                        if (maxOutputSize <= 0 || output.length() < maxOutputSize) {
                            int appendLength = Math.min(bytesRead,
                                    maxOutputSize > 0 ? maxOutputSize - output.length() : bytesRead);
                            output.append(new String(buffer, 0, appendLength, charset));
                        }
                    }
                }
                break;
            }

            // 如果没有输出且通道还未关闭，稍作等待避免CPU占用过高
            if (!hasOutput) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedException("命令执行被中断: " + command);
                }
            }

            // 防止读取过程耗时过长
            long readElapsed = System.currentTimeMillis() - readStartTime;
            if (readElapsed > timeoutMs) {
                logger.warn("读取命令输出超时: {} @ {} (读取时间: {}ms)", command, hostname, readElapsed);
                break;
            }
        }

        // 创建结果对象
        CommandResult result = new CommandResult();
        result.setOutput(output.toString());

        // 处理错误输出
        if (options.isCaptureErrorOutput() && errorOutput != null) {
            String errorString = errorOutput.toString(charset);
            if (!errorString.isEmpty()) {
                result.setErrorOutput(errorString);
                logger.debug("命令产生错误输出: {} @ {} - {}", command, hostname,
                        errorString.length() > 1000 ? errorString.substring(0, 1000) + "..." : errorString);
            }
        }

        return result;
    }

    /**
     * 获取当前登录用户信息
     */
    public List<UserActivity> getCurrentUsers(Server server) {
        List<UserActivity> users = new ArrayList<>();

        try {
            // 首先检查服务器连接状态
            if (server.getConnectionStatus() != Server.ConnectionStatus.CONNECTED &&
                    server.getConnectionStatus() != Server.ConnectionStatus.MONITORING) {
                logger.debug("服务器 {} 未连接，跳过用户信息获取", server.getHostname());
                return users;
            }

            // 获取当前登录用户，使用较短的超时时间
            String whoCommand = "who";
            logger.debug("开始获取登录用户信息: {} @ {}", whoCommand, server.getHostname());

            String result;
            try {
                result = executeCommand(server, whoCommand, 10000); // 10秒超时
                logger.debug("who命令执行成功，输出长度: {} @ {}", result.length(), server.getHostname());
            } catch (Exception e) {
                logger.warn("执行who命令失败: {} @ {} - {}", whoCommand, server.getHostname(), e.getMessage());

                // 尝试备用命令 (适用于某些系统)
                try {
                    String backupCommand = "w -h 2>/dev/null || echo 'no users'";
                    logger.debug("尝试备用用户查询命令: {} @ {}", backupCommand, server.getHostname());
                    result = executeCommand(server, backupCommand, 5000);
                } catch (Exception e2) {
                    logger.warn("备用用户查询命令也失败: {}", e2.getMessage());
                    throw new Exception("无法获取用户信息: " + e.getMessage(), e);
                }
            }

            if (result == null || result.trim().isEmpty() || result.contains("no users")) {
                logger.debug("服务器 {} 当前无登录用户", server.getHostname());
                return users;
            }

            String[] lines = result.split("\n");
            for (String line : lines) {
                if (line.trim().isEmpty())
                    continue;

                try {
                    // 解析who命令输出
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 2) { // 至少需要用户名和终端
                        UserActivity activity = new UserActivity();
                        activity.setServerId(server.getId());
                        activity.setUsername(parts[0]);
                        activity.setTerminalType(parts.length > 1 ? parts[1] : "unknown");
                        activity.setActivityType(UserActivity.ActivityType.LOGIN);
                        activity.setIsActive(true);

                        // 尝试获取远程IP (如果存在)
                        if (parts.length >= 4 && parts[3].contains("(") && parts[3].contains(")")) {
                            String ip = parts[3].substring(parts[3].indexOf("(") + 1, parts[3].indexOf(")"));
                            activity.setRemoteIp(ip);
                        }

                        users.add(activity);
                        logger.debug("解析到用户: {} @ {}", parts[0], server.getHostname());
                    }
                } catch (Exception e) {
                    logger.debug("解析用户信息行失败: '{}' - {}", line, e.getMessage());
                }
            }

            // 为每个用户获取更详细的信息 (仅在连接成功时)
            if (!users.isEmpty()) {
                for (UserActivity user : users) {
                    try {
                        // 获取用户进程数 - 使用更兼容的命令
                        String psCommand = String.format("ps -u %s 2>/dev/null | wc -l || echo '0'",
                                user.getUsername());
                        logger.debug("获取用户进程数: {} @ {}", psCommand, server.getHostname());
                        String psResult = executeCommand(server, psCommand, 5000); // 5秒超时

                        if (!psResult.isEmpty() && !psResult.trim().equals("0")) {
                            try {
                                int processCount = Integer.parseInt(psResult.trim()) - 1; // 减去标题行
                                user.setCommandCount(Math.max(0, processCount));
                                logger.debug("用户 {} 进程数: {}", user.getUsername(), processCount);
                            } catch (NumberFormatException e) {
                                logger.debug("解析进程数失败: {}", psResult.trim());
                            }
                        }

                    } catch (Exception e) {
                        logger.debug("获取用户 {} 详细信息失败: {}", user.getUsername(), e.getMessage());
                        // 不影响主要的用户信息，继续处理下一个用户
                    }
                }
            }

        } catch (Exception e) {
            logger.error("获取当前用户失败: {}", server.getHostname(), e);
        }

        return users;
    }

    /**
     * 获取系统负载信息
     */
    public Map<String, Object> getSystemLoad(Server server) {
        Map<String, Object> loadInfo = new HashMap<>();

        try {
            // 获取负载平均值
            String uptimeCmd = "uptime";
            String result = executeCommand(server, uptimeCmd);
            loadInfo.put("uptime", result.trim());

            // 解析负载平均值
            if (result.contains("load average:")) {
                String loadPart = result.substring(result.indexOf("load average:") + 13).trim();
                String[] loads = loadPart.split(",");
                if (loads.length >= 3) {
                    loadInfo.put("load1m", loads[0].trim());
                    loadInfo.put("load5m", loads[1].trim());
                    loadInfo.put("load15m", loads[2].trim());
                }
            }

            // 获取进程数
            String psCmd = "ps aux | wc -l";
            String psResult = executeCommand(server, psCmd);
            loadInfo.put("processCount", psResult.trim());

        } catch (Exception e) {
            logger.error("获取系统负载信息失败: {}", server.getHostname(), e);
        }

        return loadInfo;
    }

    /**
     * 创建SSH会话
     */
    public Session createSession(JSch jsch, Server server) throws JSchException {
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        config.put("PreferredAuthentications", "password");
        config.put("PasswordAuthentication", "yes");
        config.put("PubkeyAuthentication", "no");

        Session session = jsch.getSession(
                server.getSshUsername() != null ? server.getSshUsername() : "root",
                server.getHostname(),
                server.getSshPort() != null ? server.getSshPort() : 22);

        session.setConfig(config);
        session.setTimeout(CONNECTION_TIMEOUT);

        // 仅支持密码认证
        String decryptedPassword = getDecryptedPassword(server);
        if (decryptedPassword != null && !decryptedPassword.isEmpty()) {
            session.setPassword(decryptedPassword);
        } else {
            throw new JSchException("未提供密码，无法进行SSH认证");
        }

        return session;
    }

    /**
     * 安全获取解密后的SSH密码
     */
    private String getDecryptedPassword(Server server) {
        try {
            // 设置密码加密服务到Server实体中
            server.setPasswordEncryptionService(passwordEncryptionService);
            
            // 获取解密后的密码
            String password = server.getSshPassword();
            
            // 如果获取到的仍然是加密密码，尝试直接解密
            if (password != null && passwordEncryptionService.isPasswordEncrypted(password)) {
                password = passwordEncryptionService.decryptPassword(password);
            }
            
            return password;
            
        } catch (Exception e) {
            logger.error("SSH密码解密失败: {} - {}", server.getHostname(), e.getMessage());
            
            // 如果解密失败，可能是旧的明文密码，尝试直接使用
            String encryptedPassword = server.getSshPasswordEncrypted();
            if (encryptedPassword != null && !passwordEncryptionService.isPasswordEncrypted(encryptedPassword)) {
                logger.warn("检测到明文密码，建议升级加密存储: {}", server.getHostname());
                return encryptedPassword;
            }
            
            logger.error("无法获取有效的SSH密码: {}", server.getHostname());
            return null;
        }
    }

    /**
     * 验证服务器密码设置
     */
    public boolean validateServerPassword(Server server) {
        try {
            String password = getDecryptedPassword(server);
            return password != null && !password.isEmpty();
        } catch (Exception e) {
            logger.error("验证服务器密码失败: {} - {}", server.getHostname(), e.getMessage());
            return false;
        }
    }

    /**
     * 获取连接状态的描述信息
     */
    public String getConnectionStatusDescription(Server.ConnectionStatus status) {
        if (status == null)
            return "未知";
        return status.getDescription();
    }

    @Data
    public static class CommandResult {
        private String output = "";
        private String errorOutput = "";
        private int exitStatus = -1;
        private long executionTimeMs = 0;

        public boolean isSuccess() {
            return exitStatus == 0;
        }

        @Override
        public String toString() {
            return "CommandResult{exitStatus=" + exitStatus +
                    ", executionTime=" + executionTimeMs + "ms, " +
                    "outputLength=" + output.length() +
                    (errorOutput.isEmpty() ? "" : ", errorOutputLength=" + errorOutput.length()) + "}";
        }
    }

    @Data
    public static class CommandOptions {
        private boolean usePty = false; // 是否使用伪终端
        private boolean captureErrorOutput = true; // 是否捕获错误输出
        private boolean throwOnError = false; // 非零退出码是否抛出异常
        private boolean ignoreExitStatus = false; // 是否忽略退出状态码
        private int bufferSize = 8192; // 读取缓冲区大小
        private int maxOutputSize = 10 * 1024 * 1024; // 最大输出大小(10MB)
        private boolean throwOnOutputLimit = false; // 超出输出限制是否抛出异常
        private String charset = "UTF-8"; // 字符编码
        private Map<String, String> sessionConfig = null; // 会话配置

        // Getters and setters...

        // 流式构建器方法
        public CommandOptions withPty(boolean usePty) {
            this.usePty = usePty;
            return this;
        }

        public CommandOptions withCaptureErrorOutput(boolean captureErrorOutput) {
            this.captureErrorOutput = captureErrorOutput;
            return this;
        }

        // 其他builder方法...
    }

    /**
     * 命令执行异常类
     */
    public static class CommandExecutionException extends Exception {
        private CommandResult result;

        public CommandExecutionException(String message, CommandResult result) {
            super(message);
            this.result = result;
        }

        public CommandResult getResult() {
            return result;
        }
    }

    /**
     * 命令超时异常类
     */
    public static class CommandTimeoutException extends Exception {
        private String partialOutput;

        public CommandTimeoutException(String message, String partialOutput) {
            super(message);
            this.partialOutput = partialOutput;
        }

        public String getPartialOutput() {
            return partialOutput;
        }
    }

    /**
     * 命令输出限制异常类
     */
    public static class CommandOutputLimitException extends Exception {
        private String partialOutput;

        public CommandOutputLimitException(String message, String partialOutput) {
            super(message);
            this.partialOutput = partialOutput;
        }

        public String getPartialOutput() {
            return partialOutput;
        }
    }
}