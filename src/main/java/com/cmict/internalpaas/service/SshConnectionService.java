package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.UserActivity;
import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class SshConnectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(SshConnectionService.class);
    
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
            if (cause instanceof JSchException && cause.getMessage() != null && cause.getMessage().contains("Auth fail")) {
                logger.warn("SSH连接检查失败 - 认证错误: {} (用户名: {}, 端口: {})", 
                    server.getHostname(), 
                    server.getSshUsername() != null ? server.getSshUsername() : "root", 
                    server.getSshPort() != null ? server.getSshPort() : 22);
                return Server.ConnectionStatus.AUTH_FAILED;
            } else {
                logger.warn("SSH连接检查失败: {} - 原因: {}", server.getHostname(), cause != null ? cause.getMessage() : "未知错误");
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
                server.getSshPort() != null ? server.getSshPort() : 22
            );
            
            session.setConfig(config);
            session.setTimeout(CONNECTION_TIMEOUT);
            
            // 设置认证方式 - 仅使用密码认证
            if (server.getSshPassword() != null && !server.getSshPassword().isEmpty()) {
                session.setPassword(server.getSshPassword());
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
        return executeCommand(server, command, COMMAND_TIMEOUT);
    }
    
    /**
     * 执行远程命令（指定超时时间）
     */
    public String executeCommand(Server server, String command, int timeoutMs) throws Exception {
        long startTime = System.currentTimeMillis();
        JSch jsch = new JSch();
        Session session = null;
        ChannelExec channel = null;
        BufferedReader reader = null;
        
        logger.info("开始执行远程命令: {} @ {} (超时: {}ms)", command, server.getHostname(), timeoutMs);
        
        try {
            long sessionStartTime = System.currentTimeMillis();
            session = createSession(jsch, server);
            session.connect(CONNECTION_TIMEOUT);
            long sessionConnectTime = System.currentTimeMillis() - sessionStartTime;
            logger.debug("SSH会话连接成功: {} (耗时: {}ms)", server.getHostname(), sessionConnectTime);
            
            long channelStartTime = System.currentTimeMillis();
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            
            channel.setInputStream(null);
            channel.setErrStream(System.err);
            
            // 先连接channel，再获取输入流
            channel.connect(timeoutMs);
            long channelConnectTime = System.currentTimeMillis() - channelStartTime;
            logger.debug("命令通道连接成功: {} (耗时: {}ms)", server.getHostname(), channelConnectTime);
            
            reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            
            StringBuilder output = new StringBuilder();
            String line;
            long readStartTime = System.currentTimeMillis();
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            // 等待命令执行完成
            while (!channel.isClosed()) {
                Thread.sleep(100);
            }
            
            long executeTime = System.currentTimeMillis() - readStartTime;
            long totalTime = System.currentTimeMillis() - startTime;
            int exitStatus = channel.getExitStatus();
            
            logger.info("远程命令执行完成: {} @ {} (总耗时: {}ms, 执行耗时: {}ms, 退出码: {}, 输出长度: {})", 
                command, server.getHostname(), totalTime, executeTime, exitStatus, output.length());
            
            if (exitStatus != 0) {
                logger.warn("远程命令执行错误: {} @ {} (退出码: {})", command, server.getHostname(), exitStatus);
            }
            
            return output.toString().trim();
            
        } catch (JSchException e) {
            long totalTime = System.currentTimeMillis() - startTime;
            if (e.getMessage() != null && e.getMessage().contains("Auth fail")) {
                logger.error("远程命令执行失败 - SSH认证错误: {} @ {} (耗时: {}ms)", command, server.getHostname(), totalTime);
                throw new Exception("认证失败: " + e.getMessage(), e);
            } else if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                logger.error("远程命令执行失败 - 连接超时: {} @ {} (耗时: {}ms)", command, server.getHostname(), totalTime);
                throw new Exception("连接超时: " + e.getMessage(), e);
            } else {
                logger.error("远程命令执行失败 - SSH错误: {} @ {} (耗时: {}ms, 错误: {})", 
                    command, server.getHostname(), totalTime, e.getMessage());
                throw new Exception("SSH连接错误: " + e.getMessage(), e);
            }
        } catch (java.io.IOException e) {
            long totalTime = System.currentTimeMillis() - startTime;
            logger.error("远程命令执行失败 - IO错误: {} @ {} (耗时: {}ms)", command, server.getHostname(), totalTime, e);
            throw new Exception("IO错误: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            long totalTime = System.currentTimeMillis() - startTime;
            logger.error("远程命令执行被中断: {} @ {} (耗时: {}ms)", command, server.getHostname(), totalTime);
            Thread.currentThread().interrupt();
            throw new Exception("命令执行被中断", e);
        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            logger.error("远程命令执行异常: {} @ {} (耗时: {}ms, 异常类型: {})", 
                command, server.getHostname(), totalTime, e.getClass().getSimpleName(), e);
            throw e;
        } finally {
            long totalTime = System.currentTimeMillis() - startTime;
            logger.debug("清理远程命令资源: {} @ {} (总耗时: {}ms)", command, server.getHostname(), totalTime);
            
            if (reader != null) try { reader.close(); } catch (Exception e) { 
                logger.debug("关闭输入流异常", e); 
            }
            if (channel != null && channel.isConnected()) try { 
                channel.disconnect(); 
                logger.debug("关闭命令通道成功"); 
            } catch (Exception e) { 
                logger.debug("关闭命令通道异常", e); 
            }
            if (session != null && session.isConnected()) try { 
                session.disconnect(); 
                logger.debug("SSH会话关闭成功"); 
            } catch (Exception e) { 
                logger.debug("SSH会话关闭异常", e); 
            }
        }
    }
    
    /**
     * 获取当前登录用户信息
     */
    public List<UserActivity> getCurrentUsers(Server server) {
        List<UserActivity> users = new ArrayList<>();
        
        try {
            // 获取当前登录用户
            String whoCommand = "who";
            String result = executeCommand(server, whoCommand);
            
            String[] lines = result.split("\n");
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                
                // 解析who命令输出
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 3) {
                    UserActivity activity = new UserActivity();
                    activity.setServerId(server.getId());
                    activity.setUsername(parts[0]);
                    activity.setTerminalType(parts[1]);
                    activity.setActivityType(UserActivity.ActivityType.LOGIN);
                    activity.setIsActive(true);
                    
                    // 尝试获取远程IP
                    if (parts.length >= 4 && parts[3].contains("(") && parts[3].contains(")")) {
                        String ip = parts[3].substring(parts[3].indexOf("(") + 1, parts[3].indexOf(")"));
                        activity.setRemoteIp(ip);
                    }
                    
                    users.add(activity);
                }
            }
            
            // 为每个用户获取更详细的信息
            for (UserActivity user : users) {
                try {
                    // 获取用户最后一次活动时间
                    String lastCommand = String.format("last -n 1 %s | head -1", user.getUsername());
                    String lastResult = executeCommand(server, lastCommand, 3000);
                    
                    // 获取用户进程数
                    String psCommand = String.format("ps -u %s | wc -l", user.getUsername());
                    String psResult = executeCommand(server, psCommand, 3000);
                    if (!psResult.isEmpty()) {
                        try {
                            int processCount = Integer.parseInt(psResult.trim()) - 1; // 减去标题行
                            user.setCommandCount(Math.max(0, processCount));
                        } catch (NumberFormatException ignored) {}
                    }
                    
                } catch (Exception e) {
                    logger.debug("获取用户详细信息失败: {}", user.getUsername(), e);
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
            server.getSshPort() != null ? server.getSshPort() : 22
        );
        
        session.setConfig(config);
        session.setTimeout(CONNECTION_TIMEOUT);
        
        // 仅支持密码认证
        if (server.getSshPassword() != null && !server.getSshPassword().isEmpty()) {
            session.setPassword(server.getSshPassword());
        } else {
            throw new JSchException("未提供密码，无法进行SSH认证");
        }
        
        return session;
    }
    
    /**
     * 获取连接状态的描述信息
     */
    public String getConnectionStatusDescription(Server.ConnectionStatus status) {
        if (status == null) return "未知";
        return status.getDescription();
    }
}