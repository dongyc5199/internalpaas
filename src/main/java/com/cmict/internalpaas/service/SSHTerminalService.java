package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.SSHSession;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.repository.SSHSessionRepository;
import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.io.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SSHTerminalService {
    
    private static final Logger logger = LoggerFactory.getLogger(SSHTerminalService.class);
    
    @Autowired
    private SSHSessionRepository sshSessionRepository;
    
    @Autowired
    private SshConnectionService sshConnectionService;
    
    @Autowired
    private UserActivityService userActivityService;
    
    // 存储活跃的SSH会话
    private final Map<String, SSHTerminalSession> activeSessions = new ConcurrentHashMap<>();
    
    /**
     * SSH终端会话封装类
     */
    public static class SSHTerminalSession {
        private final String sessionId;
        private final Server server;
        private final User user;
        private final WebSocketSession webSocketSession;
        private Session jschSession;
        private ChannelShell channelShell;
        private PrintWriter writer;
        private BufferedReader reader;
        private Thread outputThread;
        private volatile boolean connected = false;
        
        public SSHTerminalSession(String sessionId, Server server, User user, WebSocketSession webSocketSession) {
            this.sessionId = sessionId;
            this.server = server;
            this.user = user;
            this.webSocketSession = webSocketSession;
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public Server getServer() { return server; }
        public User getUser() { return user; }
        public WebSocketSession getWebSocketSession() { return webSocketSession; }
        public Session getJschSession() { return jschSession; }
        public ChannelShell getChannelShell() { return channelShell; }
        public PrintWriter getWriter() { return writer; }
        public BufferedReader getReader() { return reader; }
        public Thread getOutputThread() { return outputThread; }
        public boolean isConnected() { return connected; }
        
        // Setters
        public void setJschSession(Session jschSession) { this.jschSession = jschSession; }
        public void setChannelShell(ChannelShell channelShell) { this.channelShell = channelShell; }
        public void setWriter(PrintWriter writer) { this.writer = writer; }
        public void setReader(BufferedReader reader) { this.reader = reader; }
        public void setOutputThread(Thread outputThread) { this.outputThread = outputThread; }
        public void setConnected(boolean connected) { this.connected = connected; }
    }
    
    /**
     * 创建SSH终端会话
     */
    public String createSSHSession(Server server, User user, WebSocketSession webSocketSession, 
                                  String terminalType, String windowSize) {
        String sessionId = generateSessionId();
        
        try {
            // 创建SSH终端会话对象
            SSHTerminalSession terminalSession = new SSHTerminalSession(sessionId, server, user, webSocketSession);
            
            // 建立SSH连接
            JSch jsch = new JSch();
            Session jschSession = sshConnectionService.createSession(jsch, server);
            jschSession.connect(10000);
            
            // 创建Shell通道
            ChannelShell channelShell = (ChannelShell) jschSession.openChannel("shell");
            
            // 设置终端类型和窗口大小
            if (terminalType != null && !terminalType.isEmpty()) {
                channelShell.setPtyType(terminalType);
            } else {
                channelShell.setPtyType("xterm-256color");
            }
            
            if (windowSize != null && !windowSize.isEmpty()) {
                String[] size = windowSize.split("x");
                if (size.length == 2) {
                    try {
                        int cols = Integer.parseInt(size[0]);
                        int rows = Integer.parseInt(size[1]);
                        channelShell.setPtySize(cols, rows, cols * 8, rows * 16);
                    } catch (NumberFormatException e) {
                        logger.warn("无效的窗口大小格式: {}", windowSize);
                    }
                }
            }
            
            // 设置输入输出流
            PipedOutputStream shellInput = new PipedOutputStream();
            PipedInputStream shellInputPipe = new PipedInputStream(shellInput);
            channelShell.setInputStream(shellInputPipe);
            
            PipedInputStream shellOutput = new PipedInputStream();
            PipedOutputStream shellOutputPipe = new PipedOutputStream(shellOutput);
            channelShell.setOutputStream(shellOutputPipe);
            
            // 连接Shell通道
            channelShell.connect(5000);
            
            // 设置会话属性
            terminalSession.setJschSession(jschSession);
            terminalSession.setChannelShell(channelShell);
            terminalSession.setWriter(new PrintWriter(shellInput, true));
            terminalSession.setReader(new BufferedReader(new InputStreamReader(shellOutput)));
            terminalSession.setConnected(true);
            
            // 存储会话
            activeSessions.put(sessionId, terminalSession);
            
            // 保存到数据库
            SSHSession dbSession = new SSHSession(sessionId, server.getId(), user.getId(), 
                                                 user.getUsername(), getClientIP(webSocketSession));
            dbSession.setTerminalType(terminalType);
            dbSession.setWindowSize(windowSize);
            dbSession.setSessionStatus(SSHSession.SessionStatus.ACTIVE);
            sshSessionRepository.save(dbSession);
            
            // 记录用户活动
            userActivityService.recordUserActivity(server.getId(), user.getUsername(), sessionId, 
                com.cmict.internalpaas.model.UserActivity.ActivityType.LOGIN, 
                "创建SSH终端会话");
            
            // 启动输出监听线程
            startOutputThread(terminalSession);
            
            logger.info("SSH终端会话创建成功: {} -> {}@{}", sessionId, user.getUsername(), server.getHostname());
            return sessionId;
            
        } catch (Exception e) {
            logger.error("创建SSH终端会话失败: {}@{}", user.getUsername(), server.getHostname(), e);
            
            // 清理资源
            activeSessions.remove(sessionId);
            throw new RuntimeException("创建SSH终端会话失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送命令到SSH终端
     */
    public void sendCommand(String sessionId, String command) {
        SSHTerminalSession session = activeSessions.get(sessionId);
        if (session == null || !session.isConnected()) {
            throw new RuntimeException("SSH会话不存在或已断开: " + sessionId);
        }
        
        try {
            session.getWriter().write(command);
            session.getWriter().flush();
            
            // 更新会话心跳和统计
            updateSessionHeartbeat(sessionId);
            
            // 记录命令执行活动
            if (!command.trim().isEmpty() && !command.equals("\r") && !command.equals("\n")) {
                userActivityService.recordUserActivity(session.getServer().getId(), 
                    session.getUser().getUsername(), sessionId,
                    com.cmict.internalpaas.model.UserActivity.ActivityType.COMMAND_EXECUTE,
                    "执行命令: " + command.trim());
            }
            
        } catch (Exception e) {
            logger.error("发送命令失败: {}", sessionId, e);
            throw new RuntimeException("发送命令失败: " + e.getMessage());
        }
    }
    
    /**
     * 调整终端窗口大小
     */
    public void resizeTerminal(String sessionId, int cols, int rows) {
        SSHTerminalSession session = activeSessions.get(sessionId);
        if (session == null || !session.isConnected()) {
            return;
        }
        
        try {
            session.getChannelShell().setPtySize(cols, rows, cols * 8, rows * 16);
            
            // 更新数据库中的窗口大小
            sshSessionRepository.findBySessionId(sessionId).ifPresent(dbSession -> {
                dbSession.setWindowSize(cols + "x" + rows);
                sshSessionRepository.save(dbSession);
            });
            
        } catch (Exception e) {
            logger.warn("调整终端窗口大小失败: {}", sessionId, e);
        }
    }
    
    /**
     * 关闭SSH终端会话
     */
    public void closeSession(String sessionId) {
        SSHTerminalSession session = activeSessions.get(sessionId);
        if (session == null) {
            return;
        }
        
        try {
            session.setConnected(false);
            
            // 停止输出线程
            if (session.getOutputThread() != null && session.getOutputThread().isAlive()) {
                session.getOutputThread().interrupt();
            }
            
            // 关闭通道和连接
            if (session.getChannelShell() != null) {
                session.getChannelShell().disconnect();
            }
            
            if (session.getJschSession() != null) {
                session.getJschSession().disconnect();
            }
            
            // 从活跃会话中移除
            activeSessions.remove(sessionId);
            
            // 更新数据库状态
            sshSessionRepository.findBySessionId(sessionId).ifPresent(dbSession -> {
                dbSession.close();
                sshSessionRepository.save(dbSession);
            });
            
            // 记录用户活动
            userActivityService.recordUserActivity(session.getServer().getId(), 
                session.getUser().getUsername(), sessionId,
                com.cmict.internalpaas.model.UserActivity.ActivityType.LOGOUT,
                "关闭SSH终端会话");
            
            // 结束用户会话
            userActivityService.endUserSession(session.getServer().getId(), 
                session.getUser().getUsername(), sessionId);
            
            logger.info("SSH终端会话已关闭: {}", sessionId);
            
        } catch (Exception e) {
            logger.error("关闭SSH终端会话失败: {}", sessionId, e);
        }
    }
    
    /**
     * 获取会话状态
     */
    public boolean isSessionActive(String sessionId) {
        SSHTerminalSession session = activeSessions.get(sessionId);
        return session != null && session.isConnected() && 
               session.getChannelShell() != null && session.getChannelShell().isConnected();
    }
    
    /**
     * 获取活跃会话数量
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }
    
    /**
     * 更新会话心跳
     */
    private void updateSessionHeartbeat(String sessionId) {
        sshSessionRepository.findBySessionId(sessionId).ifPresent(dbSession -> {
            dbSession.updateHeartbeat();
            dbSession.incrementCommands();
            sshSessionRepository.save(dbSession);
        });
    }
    
    /**
     * 启动输出监听线程
     */
    private void startOutputThread(SSHTerminalSession session) {
        Thread outputThread = new Thread(() -> {
            try {
                char[] buffer = new char[1024];
                int bytesRead;
                
                while (session.isConnected() && !Thread.currentThread().isInterrupted()) {
                    try {
                        if (session.getReader().ready()) {
                            bytesRead = session.getReader().read(buffer);
                            if (bytesRead > 0) {
                                String output = new String(buffer, 0, bytesRead);
                                sendOutputToWebSocket(session, output);
                            }
                        } else {
                            Thread.sleep(10); // 短暂休眠避免CPU占用过高
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (IOException e) {
                        if (session.isConnected()) {
                            logger.warn("SSH输出读取异常: {}", session.getSessionId(), e);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("SSH输出监听线程异常: {}", session.getSessionId(), e);
            } finally {
                // 会话异常断开，进行清理
                if (session.isConnected()) {
                    closeSession(session.getSessionId());
                }
            }
        });
        
        outputThread.setName("SSH-Output-" + session.getSessionId());
        outputThread.setDaemon(true);
        session.setOutputThread(outputThread);
        outputThread.start();
    }
    
    /**
     * 发送输出到WebSocket
     */
    private void sendOutputToWebSocket(SSHTerminalSession session, String output) {
        try {
            if (session.getWebSocketSession().isOpen()) {
                session.getWebSocketSession().sendMessage(
                    new org.springframework.web.socket.TextMessage(output));
            }
        } catch (Exception e) {
            logger.warn("发送WebSocket消息失败: {}", session.getSessionId(), e);
        }
    }
    
    /**
     * 生成会话ID
     */
    private String generateSessionId() {
        return "ssh-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
    
    /**
     * 获取客户端IP
     */
    private String getClientIP(WebSocketSession webSocketSession) {
        try {
            return webSocketSession.getRemoteAddress().getAddress().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * 清理超时的会话
     */
    public void cleanupTimeoutSessions(int timeoutMinutes) {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(timeoutMinutes);
        
        // 清理内存中的超时会话
        activeSessions.entrySet().removeIf(entry -> {
            SSHTerminalSession session = entry.getValue();
            if (!session.isConnected() || 
                (session.getChannelShell() != null && !session.getChannelShell().isConnected())) {
                
                closeSession(entry.getKey());
                return true;
            }
            return false;
        });
        
        // 清理数据库中的超时会话
        try {
            var timeoutSessions = sshSessionRepository.findTimeoutActiveSessions(timeoutThreshold);
            for (var dbSession : timeoutSessions) {
                if (activeSessions.containsKey(dbSession.getSessionId())) {
                    closeSession(dbSession.getSessionId());
                } else {
                    dbSession.setSessionStatus(SSHSession.SessionStatus.TIMEOUT);
                    dbSession.setIsActive(false);
                    dbSession.setEndTime(LocalDateTime.now());
                    sshSessionRepository.save(dbSession);
                }
            }
        } catch (Exception e) {
            logger.error("清理超时SSH会话失败", e);
        }
    }
}