package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.Server;
import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * 远程命令执行服务
 * 提供通过SSH在远程服务器上执行命令的功能
 */
@Service
public class RemoteCommandService {

    private static final Logger logger = LoggerFactory.getLogger(RemoteCommandService.class);
    private static final int CONNECTION_TIMEOUT = 10000; // 10秒连接超时
    private static final int COMMAND_TIMEOUT = 30000; // 30秒命令执行超时

    /**
     * 执行远程命令
     * @param server 目标服务器
     * @param command 要执行的命令
     * @return 命令执行结果
     */
    public CommandResult executeCommand(Server server, String command) {
        return executeCommand(server, command, COMMAND_TIMEOUT);
    }

    /**
     * 执行远程命令（带超时设置）
     * @param server 目标服务器
     * @param command 要执行的命令
     * @param timeoutMillis 超时时间（毫秒）
     * @return 命令执行结果
     */
    public CommandResult executeCommand(Server server, String command, long timeoutMillis) {
        logger.info("准备执行远程命令: {}@{} -> {}", 
                   server.getSshUsername(), server.getHostname(), command);
        
        JSch jsch = new JSch();
        Session session = null;
        ChannelExec channel = null;
        
        try {
            session = createSession(jsch, server);
            session.connect(CONNECTION_TIMEOUT);
            
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            
            // 设置输入输出流
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            
            channel.setOutputStream(outputStream);
            channel.setErrStream(errorStream);
            
            channel.connect(1000);
            
            // 等待命令执行完成或超时
            long startTime = System.currentTimeMillis();
            while (!channel.isClosed() && 
                   (System.currentTimeMillis() - startTime) < timeoutMillis) {
                Thread.sleep(100);
            }
            
            if (!channel.isClosed()) {
                logger.warn("命令执行超时，强制关闭: {}", command);
                channel.disconnect();
                return CommandResult.timeout("命令执行超时: " + timeoutMillis + "ms");
            }
            
            int exitCode = channel.getExitStatus();
            String output = outputStream.toString("UTF-8").trim();
            String error = errorStream.toString("UTF-8").trim();
            
            CommandResult result = new CommandResult(exitCode, output, error);
            logger.info("命令执行完成: exitCode={}, outputLength={}, errorLength={}", 
                       exitCode, output.length(), error.length());
            
            return result;
            
        } catch (JSchException e) {
            logger.error("SSH连接失败: {}", e.getMessage());
            return CommandResult.error("SSH连接失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("命令执行异常", e);
            return CommandResult.error("命令执行异常: " + e.getMessage());
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
     * 异步执行远程命令
     * @param server 目标服务器
     * @param command 要执行的命令
     * @return CompletableFuture包装的命令结果
     */
    public CompletableFuture<CommandResult> executeCommandAsync(Server server, String command) {
        return CompletableFuture.supplyAsync(() -> executeCommand(server, command));
    }

    /**
     * 创建SSH会话
     */
    private Session createSession(JSch jsch, Server server) throws JSchException {
        int port = server.getSshPort() != null ? server.getSshPort() : 22;
        String username = server.getSshUsername() != null ? server.getSshUsername() : "root";
        
        Session session = jsch.getSession(username, server.getHostname(), port);
        
        // 配置SSH参数 - 仅支持密码认证
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        config.put("PreferredAuthentications", "password");
        config.put("PasswordAuthentication", "yes");
        config.put("PubkeyAuthentication", "no");
        session.setConfig(config);
        
        // 设置认证方式 - 仅支持密码认证
        if (server.getSshPassword() != null && !server.getSshPassword().isEmpty()) {
            session.setPassword(server.getSshPassword());
        } else {
            throw new JSchException("未提供密码，无法进行SSH认证");
        }
        
        return session;
    }

    /**
     * 执行多个命令
     * @param server 目标服务器
     * @param commands 命令列表
     * @return 每个命令的执行结果
     */
    public CommandResult[] executeCommands(Server server, String[] commands) {
        CommandResult[] results = new CommandResult[commands.length];
        for (int i = 0; i < commands.length; i++) {
            results[i] = executeCommand(server, commands[i]);
        }
        return results;
    }

    /**
     * 获取远程文件内容
     * @param server 目标服务器
     * @param filePath 文件路径
     * @return 文件内容
     */
    public CommandResult readFile(Server server, String filePath) {
        return executeCommand(server, "cat '" + filePath + "'");
    }

    /**
     * 写入远程文件
     * @param server 目标服务器
     * @param filePath 文件路径
     * @param content 文件内容
     * @return 执行结果
     */
    public CommandResult writeFile(Server server, String filePath, String content) {
        String escapedContent = content.replace("'", "'\"'\"'");
        String command = String.format("echo '%s' > '%s'", escapedContent, filePath);
        return executeCommand(server, command);
    }

    /**
     * 命令执行结果类
     */
    public static class CommandResult {
        private final int exitCode;
        private final String output;
        private final String error;
        private final boolean success;
        private final long executionTime;

        public CommandResult(int exitCode, String output, String error) {
            this.exitCode = exitCode;
            this.output = output;
            this.error = error;
            this.success = exitCode == 0;
            this.executionTime = System.currentTimeMillis();
        }

        public static CommandResult success(String output) {
            return new CommandResult(0, output, "");
        }

        public static CommandResult error(String error) {
            return new CommandResult(1, "", error);
        }

        public static CommandResult timeout(String error) {
            return new CommandResult(124, "", error);
        }

        // Getters
        public int getExitCode() { return exitCode; }
        public String getOutput() { return output; }
        public String getError() { return error; }
        public boolean isSuccess() { return success; }
        public long getExecutionTime() { return executionTime; }

        @Override
        public String toString() {
            return "CommandResult{" +
                   "exitCode=" + exitCode +
                   ", success=" + success +
                   ", output='" + output + '\'' +
                   ", error='" + error + '\'' +
                   '}';
        }
    }
}