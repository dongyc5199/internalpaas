package com.cmict.internalpaas.service;

import com.cmict.internalpaas.controller.WebSocketController;
import com.cmict.internalpaas.model.Application;
import com.cmict.internalpaas.repository.ApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 日志监控服务
 * 监控应用日志文件变化，并通过WebSocket实时推送给用户
 */
@Service
public class LogMonitoringService {
    
    private static final Logger logger = LoggerFactory.getLogger(LogMonitoringService.class);
    
    @Autowired
    private ApplicationRepository applicationRepository;
    
    @Autowired
    private WebSocketController webSocketController;
    
    // 日志监控器映射表：applicationId -> LogMonitor
    private final Map<Long, LogMonitor> logMonitors = new ConcurrentHashMap<>();
    
    // 线程池执行器
    private ExecutorService executorService;
    
    @PostConstruct
    public void init() {
        executorService = Executors.newCachedThreadPool();
        logger.info("日志监控服务已初始化");
    }
    
    @PreDestroy
    public void cleanup() {
        // 停止所有监控器
        logMonitors.values().forEach(LogMonitor::stop);
        logMonitors.clear();
        
        // 关闭线程池
        if (executorService != null) {
            executorService.shutdown();
        }
        logger.info("日志监控服务已清理");
    }
    
    /**
     * 开始监控指定应用的日志
     */
    public void startMonitoring(Long applicationId, String username) {
        if (logMonitors.containsKey(applicationId)) {
            logger.warn("应用 {} 的日志监控已经在运行中", applicationId);
            return;
        }
        
        Application app = applicationRepository.findById(applicationId).orElse(null);
        if (app == null || app.getLogFilePath() == null) {
            logger.warn("应用 {} 不存在或未配置日志文件", applicationId);
            return;
        }
        
        try {
            LogMonitor monitor = new LogMonitor(applicationId, app.getLogFilePath(), username);
            logMonitors.put(applicationId, monitor);
            executorService.submit(monitor);
            logger.info("开始监控应用 {} 的日志文件: {}", applicationId, app.getLogFilePath());
        } catch (Exception e) {
            logger.error("启动日志监控失败，应用ID: {}", applicationId, e);
        }
    }
    
    /**
     * 停止监控指定应用的日志
     */
    public void stopMonitoring(Long applicationId) {
        LogMonitor monitor = logMonitors.remove(applicationId);
        if (monitor != null) {
            monitor.stop();
            logger.info("停止监控应用 {} 的日志", applicationId);
        }
    }
    
    /**
     * 获取日志文件的最后N行内容
     */
    public String getLastNLines(String logFilePath, int lines) {
        try {
            Path path = Paths.get(logFilePath);
            if (!Files.exists(path)) {
                return "日志文件不存在";
            }
            
            try (RandomAccessFile file = new RandomAccessFile(logFilePath, "r")) {
                StringBuilder content = new StringBuilder();
                long fileLength = file.length();
                long pos = fileLength - 1;
                int lineCount = 0;
                
                // 从文件末尾开始读取
                while (pos >= 0 && lineCount < lines) {
                    file.seek(pos);
                    char c = (char) file.read();
                    if (c == '\n') {
                        lineCount++;
                    }
                    content.insert(0, c);
                    pos--;
                }
                
                return content.toString();
            }
        } catch (IOException e) {
            logger.error("读取日志文件失败: {}", logFilePath, e);
            return "读取日志文件失败: " + e.getMessage();
        }
    }
    
    /**
     * 日志监控器内部类
     */
    private class LogMonitor implements Runnable {
        private final Long applicationId;
        private final String logFilePath;
        private final String username;
        private volatile boolean running = true;
        private long lastReadPosition = 0;
        
        public LogMonitor(Long applicationId, String logFilePath, String username) {
            this.applicationId = applicationId;
            this.logFilePath = logFilePath;
            this.username = username;
            
            // 初始化读取位置到文件末尾
            try {
                Path path = Paths.get(logFilePath);
                if (Files.exists(path)) {
                    this.lastReadPosition = Files.size(path);
                }
            } catch (IOException e) {
                logger.warn("无法获取日志文件大小: {}", logFilePath, e);
            }
        }
        
        @Override
        public void run() {
            try {
                Path logPath = Paths.get(logFilePath);
                Path parentDir = logPath.getParent();
                
                // 确保父目录存在
                if (!Files.exists(parentDir)) {
                    Files.createDirectories(parentDir);
                }
                
                // 如果日志文件不存在，创建空文件
                if (!Files.exists(logPath)) {
                    Files.createFile(logPath);
                }
                
                // 使用WatchService监控文件变化
                try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
                    parentDir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                    
                    logger.info("开始监控日志文件: {}", logFilePath);
                    
                    while (running) {
                        WatchKey key = watchService.take();
                        
                        for (WatchEvent<?> event : key.pollEvents()) {
                            WatchEvent.Kind<?> kind = event.kind();
                            
                            if (kind == StandardWatchEventKinds.OVERFLOW) {
                                continue;
                            }
                            
                            Path changedFile = (Path) event.context();
                            if (changedFile.equals(logPath.getFileName()) && 
                                kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                                
                                // 读取新增的日志内容
                                String newContent = readNewContent();
                                if (newContent != null && !newContent.isEmpty()) {
                                    // 通过WebSocket发送给用户
                                    webSocketController.sendLogToUser(username, applicationId, newContent);
                                }
                            }
                        }
                        
                        boolean valid = key.reset();
                        if (!valid) {
                            break;
                        }
                    }
                }
            } catch (InterruptedException e) {
                logger.info("日志监控被中断: {}", logFilePath);
            } catch (Exception e) {
                logger.error("日志监控异常: {}", logFilePath, e);
            } finally {
                logger.info("日志监控结束: {}", logFilePath);
            }
        }
        
        /**
         * 读取新增的日志内容
         */
        private String readNewContent() {
            try (RandomAccessFile file = new RandomAccessFile(logFilePath, "r")) {
                long currentSize = file.length();
                
                if (currentSize < lastReadPosition) {
                    // 文件被重写，从头开始读
                    lastReadPosition = 0;
                }
                
                if (currentSize > lastReadPosition) {
                    file.seek(lastReadPosition);
                    byte[] buffer = new byte[(int) (currentSize - lastReadPosition)];
                    file.readFully(buffer);
                    lastReadPosition = currentSize;
                    
                    return new String(buffer, "UTF-8");
                }
                
                return null;
            } catch (IOException e) {
                logger.error("读取日志文件新内容失败: {}", logFilePath, e);
                return null;
            }
        }
        
        /**
         * 停止监控
         */
        public void stop() {
            running = false;
        }
    }
}