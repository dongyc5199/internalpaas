package com.cmict.internalpaas.service;

import com.cmict.internalpaas.controller.WebSocketController;
import com.cmict.internalpaas.model.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 服务器状态标签定时任务调度服务
 * 高性能并发更新服务器状态标签
 */
@Service
public class ServerStatusTagSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(ServerStatusTagSchedulerService.class);

    @Autowired
    private ServerService serverService;

    @Autowired
    private ServerStatusTagService statusTagService;

    @Autowired
    private WebSocketController webSocketController;

    // 线程池配置
    private ExecutorService executorService;
    private final int THREAD_POOL_SIZE = 8; // 并发处理服务器的线程数
    private final int MAX_QUEUE_SIZE = 100;

    // 性能监控
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private volatile LocalDateTime lastExecutionTime;
    private final Map<Long, LocalDateTime> serverLastUpdateTime = new ConcurrentHashMap<>();
    private final Map<Long, Integer> serverUpdateInterval = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 创建自定义线程池
        this.executorService = new ThreadPoolExecutor(
            THREAD_POOL_SIZE,
            THREAD_POOL_SIZE * 2,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(MAX_QUEUE_SIZE),
            new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "ServerStatusTag-" + threadNumber.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy() // 当队列满时，调用者线程执行任务
        );

        logger.info("🚀 服务器状态标签调度服务已启动，线程池大小: {}", THREAD_POOL_SIZE);
    }

    @PreDestroy
    public void destroy() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        logger.info("📊 服务器状态标签调度服务已关闭");
    }

    /**
     * 主要定时任务 - 每30秒执行一次
     */
    @Scheduled(fixedRate = 30000, initialDelay = 10000)
    public void updateServerStatusTags() {
        if (executorService.isShutdown()) {
            logger.warn("⚠️ 线程池已关闭，跳过本次状态更新");
            return;
        }

        LocalDateTime startTime = LocalDateTime.now();
        logger.info("🔄 开始批量更新服务器状态标签 - {}", startTime);

        try {
            List<Server> servers = serverService.findAllActive();
            if (servers.isEmpty()) {
                logger.debug("📭 没有找到活跃服务器，跳过状态更新");
                return;
            }

            // 分批处理服务器列表，避免一次性创建过多任务
            int batchSize = Math.min(THREAD_POOL_SIZE * 2, servers.size());
            processBatch(servers, batchSize);

            lastExecutionTime = LocalDateTime.now();
            logger.info("✅ 状态标签更新任务提交完成，服务器数量: {}, 耗时: {}ms", 
                       servers.size(), 
                       java.time.Duration.between(startTime, lastExecutionTime).toMillis());

        } catch (Exception e) {
            logger.error("❌ 批量更新服务器状态标签失败", e);
        }
    }

    /**
     * 快速状态检查 - 每10秒执行一次，只检查关键状态
     */
    @Scheduled(fixedRate = 10000, initialDelay = 5000)
    public void quickStatusCheck() {
        try {
            List<Server> criticalServers = serverService.findByConnectionStatusIn(
                List.of(Server.ConnectionStatus.CONNECTED, Server.ConnectionStatus.MONITORING)
            );

            for (Server server : criticalServers) {
                // 提交轻量级状态检查任务
                executorService.submit(new QuickStatusCheckTask(server));
            }

        } catch (Exception e) {
            logger.error("❌ 快速状态检查失败", e);
        }
    }

    /**
     * 分批处理服务器
     */
    private void processBatch(List<Server> servers, int batchSize) {
        CompletionService<ServerStatusUpdateResult> completionService = 
            new ExecutorCompletionService<>(executorService);

        int submittedTasks = 0;
        
        for (Server server : servers) {
            // 检查是否需要更新（智能调度）
            if (shouldUpdateServer(server)) {
                completionService.submit(new ServerStatusUpdateTask(server));
                submittedTasks++;
            }
        }

        // 异步处理结果
        if (submittedTasks > 0) {
            executorService.submit(new ResultProcessor(completionService, submittedTasks));
        }
    }

    /**
     * 智能判断服务器是否需要更新
     */
    private boolean shouldUpdateServer(Server server) {
        Long serverId = server.getId();
        LocalDateTime lastUpdate = serverLastUpdateTime.get(serverId);
        
        if (lastUpdate == null) {
            // 首次更新
            return true;
        }

        // 根据服务器状态确定更新间隔
        int intervalSeconds = serverUpdateInterval.getOrDefault(serverId, getDefaultUpdateInterval(server));
        LocalDateTime nextUpdateTime = lastUpdate.plusSeconds(intervalSeconds);
        
        return LocalDateTime.now().isAfter(nextUpdateTime);
    }

    /**
     * 获取默认更新间隔（秒）
     */
    private int getDefaultUpdateInterval(Server server) {
        if (server.getConnectionStatus() == null) {
            return 60; // 未知状态：60秒
        }
        
        switch (server.getConnectionStatus()) {
            case CONNECTED:
            case MONITORING:
                return 30; // 在线服务器：30秒
            case FAILED:
            case TIMEOUT:
                return 120; // 失败服务器：2分钟
            case AUTH_FAILED:
                return 300; // 认证失败：5分钟
            default:
                return 90; // 其他状态：90秒
        }
    }

    /**
     * 服务器状态更新任务
     */
    private class ServerStatusUpdateTask implements Callable<ServerStatusUpdateResult> {
        private final Server server;

        public ServerStatusUpdateTask(Server server) {
            this.server = server;
        }

        @Override
        public ServerStatusUpdateResult call() {
            long startTime = System.currentTimeMillis();
            String serverName = server.getName();
            Long serverId = server.getId();

            try {
                logger.debug("🔍 开始更新服务器 {} 的状态标签", serverName);

                // 执行完整的状态标签刷新
                statusTagService.refreshAllServerTags(serverId);

                long duration = System.currentTimeMillis() - startTime;
                
                // 更新最后更新时间
                serverLastUpdateTime.put(serverId, LocalDateTime.now());
                
                // 根据执行时间调整更新间隔
                adjustUpdateInterval(serverId, duration, true);

                successCount.incrementAndGet();
                logger.debug("✅ 服务器 {} 状态标签更新成功，耗时: {}ms", serverName, duration);

                return new ServerStatusUpdateResult(serverId, serverName, true, duration, null);

            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                
                // 调整失败服务器的更新间隔
                adjustUpdateInterval(serverId, duration, false);

                failureCount.incrementAndGet();
                logger.warn("⚠️ 服务器 {} 状态标签更新失败，耗时: {}ms, 错误: {}", 
                           serverName, duration, e.getMessage());

                return new ServerStatusUpdateResult(serverId, serverName, false, duration, e.getMessage());
            }
        }
    }

    /**
     * 快速状态检查任务
     */
    private class QuickStatusCheckTask implements Runnable {
        private final Server server;

        public QuickStatusCheckTask(Server server) {
            this.server = server;
        }

        @Override
        public void run() {
            try {
                // 只检查连接状态，不做完整的状态标签刷新
                statusTagService.checkConnectionStatus(server.getId());
                
            } catch (Exception e) {
                logger.debug("快速状态检查失败 - 服务器: {}, 错误: {}", server.getName(), e.getMessage());
            }
        }
    }

    /**
     * 结果处理器
     */
    private class ResultProcessor implements Runnable {
        private final CompletionService<ServerStatusUpdateResult> completionService;
        private final int taskCount;

        public ResultProcessor(CompletionService<ServerStatusUpdateResult> completionService, int taskCount) {
            this.completionService = completionService;
            this.taskCount = taskCount;
        }

        @Override
        public void run() {
            int completedTasks = 0;
            int successTasks = 0;
            
            try {
                while (completedTasks < taskCount) {
                    try {
                        Future<ServerStatusUpdateResult> future = completionService.poll(30, TimeUnit.SECONDS);
                        if (future != null) {
                            ServerStatusUpdateResult result = future.get();
                            completedTasks++;
                            
                            if (result.isSuccess()) {
                                successTasks++;
                                // 通知前端更新
                                notifyStatusUpdate(result.getServerId());
                            }
                        } else {
                            logger.warn("⏰ 等待任务完成超时");
                            break;
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (ExecutionException e) {
                        logger.error("任务执行异常", e);
                        completedTasks++;
                    }
                }
                
                logger.info("📈 批量更新完成 - 总数: {}, 成功: {}, 失败: {}", 
                           completedTasks, successTasks, completedTasks - successTasks);
                           
            } catch (Exception e) {
                logger.error("结果处理失败", e);
            }
        }
    }

    /**
     * 调整服务器更新间隔
     */
    private void adjustUpdateInterval(Long serverId, long executionTimeMs, boolean success) {
        if (success) {
            if (executionTimeMs < 1000) {
                // 执行很快，可以缩短间隔
                serverUpdateInterval.put(serverId, Math.max(20, 
                    serverUpdateInterval.getOrDefault(serverId, 30) - 5));
            }
        } else {
            // 执行失败，延长间隔
            serverUpdateInterval.put(serverId, Math.min(600, 
                serverUpdateInterval.getOrDefault(serverId, 30) * 2));
        }
    }

    /**
     * 通知前端状态更新
     */
    private void notifyStatusUpdate(Long serverId) {
        try {
            // 发送WebSocket通知
            webSocketController.broadcast("/topic/server-status", Map.of(
                "type", "STATUS_TAGS_UPDATED",
                "serverId", serverId,
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            logger.debug("发送WebSocket通知失败: {}", e.getMessage());
        }
    }

    /**
     * 获取调度服务统计信息
     */
    public Map<String, Object> getSchedulerStatistics() {
        return Map.of(
            "threadPoolSize", THREAD_POOL_SIZE,
            "activeThreads", ((ThreadPoolExecutor) executorService).getActiveCount(),
            "queueSize", ((ThreadPoolExecutor) executorService).getQueue().size(),
            "successCount", successCount.get(),
            "failureCount", failureCount.get(),
            "lastExecutionTime", lastExecutionTime,
            "managedServers", serverLastUpdateTime.size()
        );
    }

    /**
     * 状态更新结果
     */
    private static class ServerStatusUpdateResult {
        private final Long serverId;
        private final String serverName;
        private final boolean success;
        private final long duration;
        private final String errorMessage;

        public ServerStatusUpdateResult(Long serverId, String serverName, boolean success, long duration, String errorMessage) {
            this.serverId = serverId;
            this.serverName = serverName;
            this.success = success;
            this.duration = duration;
            this.errorMessage = errorMessage;
        }

        public Long getServerId() { return serverId; }
        public String getServerName() { return serverName; }
        public boolean isSuccess() { return success; }
        public long getDuration() { return duration; }
        public String getErrorMessage() { return errorMessage; }
    }
}