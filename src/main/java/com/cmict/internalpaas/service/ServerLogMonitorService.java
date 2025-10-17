package com.cmict.internalpaas.service;

import com.cmict.internalpaas.controller.WebSocketController;
import com.cmict.internalpaas.dto.ServerLogContentDto;
import com.cmict.internalpaas.model.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 服务器日志实时监控服务
 * 提供服务器日志文件的实时监控和WebSocket推送功能
 */
@Service
public class ServerLogMonitorService {
    
    private static final Logger logger = LoggerFactory.getLogger(ServerLogMonitorService.class);
    
    @Autowired
    private RemoteCommandService remoteCommandService;
    
    @Autowired
    private ServerService serverService;
    
    @Autowired
    private WebSocketController webSocketController;
    
    // 监控任务映射：serverId_filePath -> MonitorTask
    private final Map<String, ServerLogMonitorTask> monitorTasks = new ConcurrentHashMap<>();
    
    // 线程池
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    // 日志级别匹配模式
    private static final Pattern LOG_LEVEL_PATTERN = Pattern.compile(
        "\\b(TRACE|DEBUG|INFO|WARN|WARNING|ERROR|FATAL|CRITICAL|EMERG|ALERT|CRIT|ERR|NOTICE)\\b", 
        Pattern.CASE_INSENSITIVE
    );
    
    @PreDestroy
    public void cleanup() {
        logger.info("清理服务器日志监控服务");
        
        // 停止所有监控任务
        for (ServerLogMonitorTask task : monitorTasks.values()) {
            task.stop();
        }
        monitorTasks.clear();
        
        // 关闭线程池
        executorService.shutdown();
    }
    
    /**
     * 开始监控服务器日志文件
     */
    public boolean startMonitoring(Long serverId, String filePath, String username) {
        String taskKey = getTaskKey(serverId, filePath);
        
        if (monitorTasks.containsKey(taskKey)) {
            logger.warn("服务器 {} 日志文件 {} 已在监控中", serverId, filePath);
            return false;
        }
        
        Optional<Server> serverOpt = serverService.getServerById(serverId);
        if (serverOpt.isEmpty()) {
            logger.error("服务器不存在: {}", serverId);
            return false;
        }
        
        Server server = serverOpt.get();
        
        try {
            ServerLogMonitorTask task = new ServerLogMonitorTask(server, filePath, username);
            Future<?> future = executorService.submit(task);
            task.setFuture(future);
            
            monitorTasks.put(taskKey, task);
            
            logger.info("开始监控服务器 {} 日志文件: {} (用户: {})", serverId, filePath, username);
            return true;
            
        } catch (Exception e) {
            logger.error("启动日志监控失败", e);
            return false;
        }
    }
    
    /**
     * 停止监控服务器日志文件
     */
    public boolean stopMonitoring(Long serverId, String filePath) {
        String taskKey = getTaskKey(serverId, filePath);
        
        ServerLogMonitorTask task = monitorTasks.remove(taskKey);
        if (task != null) {
            task.stop();
            logger.info("停止监控服务器 {} 日志文件: {}", serverId, filePath);
            return true;
        } else {
            logger.warn("服务器 {} 日志文件 {} 监控任务不存在", serverId, filePath);
            return false;
        }
    }
    
    /**
     * 停止服务器的所有监控任务
     */
    public void stopAllMonitoring(Long serverId) {
        String prefix = serverId + "_";
        
        monitorTasks.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(prefix)) {
                entry.getValue().stop();
                logger.info("停止监控任务: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    /**
     * 获取活跃监控任务列表
     */
    public Map<String, Object> getActiveMonitoringTasks() {
        Map<String, Object> result = new HashMap<>();
        
        for (Map.Entry<String, ServerLogMonitorTask> entry : monitorTasks.entrySet()) {
            ServerLogMonitorTask task = entry.getValue();
            Map<String, Object> taskInfo = new HashMap<>();
            taskInfo.put("serverId", task.getServer().getId());
            taskInfo.put("serverName", task.getServer().getName());
            taskInfo.put("filePath", task.getFilePath());
            taskInfo.put("username", task.getUsername());
            taskInfo.put("startTime", task.getStartTime());
            taskInfo.put("isRunning", task.isRunning());
            taskInfo.put("linesProcessed", task.getLinesProcessed());
            
            result.put(entry.getKey(), taskInfo);
        }
        
        return result;
    }
    
    /**
     * 生成任务键
     */
    private String getTaskKey(Long serverId, String filePath) {
        return serverId + "_" + filePath.replaceAll("[^a-zA-Z0-9]", "_");
    }
    
    /**
     * 服务器日志监控任务
     */
    private class ServerLogMonitorTask implements Runnable {
        
        private final Server server;
        private final String filePath;
        private final String username;
        private final LocalDateTime startTime;
        
        private volatile boolean running = true;
        private volatile long linesProcessed = 0;
        private Future<?> future;
        
        public ServerLogMonitorTask(Server server, String filePath, String username) {
            this.server = server;
            this.filePath = filePath;
            this.username = username;
            this.startTime = LocalDateTime.now();
        }
        
        @Override
        public void run() {
            logger.info("开始执行日志监控任务: 服务器={}, 文件={}", server.getName(), filePath);
            
            try {
                // 构建tail命令
                String command = String.format("tail -f '%s' 2>/dev/null", filePath);
                
                // 使用自定义的流式命令执行
                executeStreamCommand(command);
                
            } catch (InterruptedException e) {
                logger.info("日志监控任务被中断: {}", filePath);
            } catch (Exception e) {
                logger.error("日志监控任务异常", e);
            } finally {
                logger.info("日志监控任务结束: {}", filePath);
                running = false;
            }
        }
        
        /**
         * 执行流式命令 - 简化版本，实际应该使用JSch的流式处理
         */
        private void executeStreamCommand(String command) throws Exception {
            // 这里是简化实现，实际应该使用JSch的InputStream进行实时读取
            // 当前使用轮询方式模拟实时监控
            
            long lastPosition = 0;
            
            while (running) {
                try {
                    // 获取新增内容
                    String tailCommand = String.format("tail -c +%d '%s' | tail -10", lastPosition + 1, filePath);
                    RemoteCommandService.CommandResult result = remoteCommandService.executeCommand(server, tailCommand, 5000);
                    
                    if (result.isSuccess() && !result.getOutput().trim().isEmpty()) {
                        String[] newLines = result.getOutput().split("\\n");
                        
                        for (String line : newLines) {
                            if (line.trim().isEmpty()) continue;
                            
                            // 处理新日志行
                            processNewLogLine(line);
                            linesProcessed++;
                            lastPosition += line.length() + 1; // +1 for newline
                        }
                    }
                    
                    // 避免过于频繁的轮询
                    Thread.sleep(1000);
                    
                } catch (InterruptedException e) {
                    throw e;
                } catch (Exception e) {
                    logger.warn("读取日志新内容失败: {}", e.getMessage());
                    Thread.sleep(5000); // 出错时等待更长时间
                }
            }
        }
        
        /**
         * 处理新的日志行
         */
        private void processNewLogLine(String line) {
            try {
                // 解析日志行
                ServerLogContentDto.LogLineDto logLine = parseLogLine(line, linesProcessed + 1);
                
                // 构建WebSocket消息
                Map<String, Object> message = new HashMap<>();
                message.put("type", "server_log_line");
                message.put("serverId", server.getId());
                message.put("serverName", server.getName());
                message.put("filePath", filePath);
                message.put("timestamp", LocalDateTime.now().toString());
                message.put("line", logLine);
                message.put("username", username);
                
                // 发送到WebSocket
                webSocketController.sendToUser(username, "/topic/server-logs", message);
                
                logger.debug("推送新日志行到用户 {}: {}", username, line.substring(0, Math.min(50, line.length())));
                
            } catch (Exception e) {
                logger.warn("处理日志行失败: {}", e.getMessage());
            }
        }
        
        /**
         * 解析单行日志
         */
        private ServerLogContentDto.LogLineDto parseLogLine(String content, long lineNumber) {
            ServerLogContentDto.LogLineDto lineDto = new ServerLogContentDto.LogLineDto(lineNumber, content);
            
            // 解析日志级别
            Matcher matcher = LOG_LEVEL_PATTERN.matcher(content);
            if (matcher.find()) {
                String level = matcher.group(1).toUpperCase();
                lineDto.setLevel(level);
            } else {
                lineDto.setLevel("INFO"); // 默认级别
            }
            
            // 设置时间戳为当前时间
            lineDto.setTimestamp(LocalDateTime.now());
            lineDto.setSource("real-time");
            
            return lineDto;
        }
        
        public void stop() {
            running = false;
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
        }
        
        // Getters
        public Server getServer() { return server; }
        public String getFilePath() { return filePath; }
        public String getUsername() { return username; }
        public LocalDateTime getStartTime() { return startTime; }
        public boolean isRunning() { return running; }
        public long getLinesProcessed() { return linesProcessed; }
        
        public void setFuture(Future<?> future) { this.future = future; }
    }
}