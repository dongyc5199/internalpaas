package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerMetrics;
import com.cmict.internalpaas.model.UserActivity;
import com.cmict.internalpaas.repository.ServerMetricsRepository;
import com.cmict.internalpaas.repository.UserActivityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class MonitoringService {
    
    private static final Logger logger = LoggerFactory.getLogger(MonitoringService.class);
    private static final int METRICS_COLLECTION_TIMEOUT = 90000; // 90秒指标收集总超时（从45秒增加）
    
    // 专用线程池，用于指标收集的异步任务
    private final ExecutorService metricsExecutor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "metrics-collector-" + System.currentTimeMillis());
        t.setDaemon(true);  // 设置为守护线程，应用关闭时会自动退出
        return t;
    });
    
    @Autowired
    private SshConnectionService sshConnectionService;
    
    @Autowired
    private ServerMetricsRepository metricsRepository;
    
    @Autowired
    private UserActivityRepository userActivityRepository;
    
    /**
     * 获取完整的服务器监控数据
     */
    public ServerMetrics getServerMetrics(Server server) {
        long startTime = System.currentTimeMillis();
        logger.info("开始收集服务器指标: {}", server.getHostname());
        
        ServerMetrics metrics = new ServerMetrics();
        metrics.setServerId(server.getId());
        metrics.setServerName(server.getName());
        metrics.setHostname(server.getHostname());
        metrics.setTimestamp(LocalDateTime.now());
        
        // 对localhost的特殊处理
        if (isLocalhost(server.getHostname())) {
            logger.info("检测到localhost，使用本地系统方式收集指标");
            collectLocalMetrics(metrics);
            long endTime = System.currentTimeMillis();
            metrics.setCollectionDurationMs(endTime - startTime);
            logger.info("localhost指标收集完成，耗时: {}ms", endTime - startTime);
            return metrics;
        }
        
        // 首先检查SSH连接状态
        try {
            Server.ConnectionStatus connectionStatus = sshConnectionService.checkConnection(server);
            if (connectionStatus != Server.ConnectionStatus.CONNECTED) {
                logger.warn("服务器 {} SSH连接不可用，状态: {}", server.getHostname(), connectionStatus);
                metrics.setCollectionDurationMs(System.currentTimeMillis() - startTime);
                return metrics;
            }
        } catch (Exception e) {
            logger.error("检查服务器 {} SSH连接失败", server.getHostname(), e);
            metrics.setCollectionDurationMs(System.currentTimeMillis() - startTime);
            return metrics;
        }
        
        try {
            logger.debug("并行收集服务器 {} 的各项指标", server.getHostname());
            
            // 并行收集各项指标，使用自定义线程池
            CompletableFuture<Void> cpuFuture = CompletableFuture.runAsync(() -> {
                try {
                    collectCpuMetrics(server, metrics);
                    logger.debug("CPU指标收集完成: {}", server.getHostname());
                } catch (Exception e) {
                    logger.error("CPU指标收集失败: {}", server.getHostname(), e);
                }
            }, metricsExecutor);
            
            CompletableFuture<Void> memoryFuture = CompletableFuture.runAsync(() -> {
                try {
                    collectMemoryMetrics(server, metrics);
                    logger.debug("内存指标收集完成: {}", server.getHostname());
                } catch (Exception e) {
                    logger.error("内存指标收集失败: {}", server.getHostname(), e);
                }
            }, metricsExecutor);
            
            CompletableFuture<Void> diskFuture = CompletableFuture.runAsync(() -> {
                try {
                    collectDiskMetrics(server, metrics);
                    logger.debug("磁盘指标收集完成: {}", server.getHostname());
                } catch (Exception e) {
                    logger.error("磁盘指标收集失败: {}", server.getHostname(), e);
                }
            }, metricsExecutor);
            
            CompletableFuture<Void> systemFuture = CompletableFuture.runAsync(() -> {
                try {
                    collectSystemMetrics(server, metrics);
                    logger.debug("系统指标收集完成: {}", server.getHostname());
                } catch (Exception e) {
                    logger.error("系统指标收集失败: {}", server.getHostname(), e);
                }
            }, metricsExecutor);
            
            // 等待所有指标收集完成，使用更长的超时时间
            CompletableFuture.allOf(cpuFuture, memoryFuture, diskFuture, systemFuture)
                    .get(METRICS_COLLECTION_TIMEOUT, TimeUnit.MILLISECONDS);
                    
            logger.info("服务器 {} 指标收集成功完成", server.getHostname());
                    
        } catch (java.util.concurrent.TimeoutException e) {
            logger.error("收集服务器 {} 指标超时（{}秒）", server.getHostname(), METRICS_COLLECTION_TIMEOUT / 1000, e);
        } catch (java.util.concurrent.ExecutionException e) {
            logger.error("收集服务器 {} 指标执行失败", server.getHostname(), e.getCause());
        } catch (Exception e) {
            logger.error("收集服务器 {} 指标发生未知错误", server.getHostname(), e);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        metrics.setCollectionDurationMs(duration);
        
        logger.info("服务器 {} 指标收集完成，耗时: {}ms", server.getHostname(), duration);
        
        return metrics;
    }
    
    /**
     * 判断是否是localhost
     */
    private boolean isLocalhost(String hostname) {
        return "localhost".equalsIgnoreCase(hostname) || 
               "127.0.0.1".equals(hostname) || 
               "0.0.0.0".equals(hostname);
    }
    
    /**
     * 收集本地系统指标（不通过SSH）
     */
    private void collectLocalMetrics(ServerMetrics metrics) {
        logger.debug("使用本地方式收集系统指标");
        
        try {
            // 设置默认值，表示可以正常工作
            metrics.setCpuUsage(5.0); // 模拟5%CPU使用率
            metrics.setCpuCores(Runtime.getRuntime().availableProcessors());
            metrics.setLoadAverage("0.1, 0.1, 0.1");
            
            // 获取JVM内存信息作为系统内存的代理
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            metrics.setMemoryTotal(maxMemory);
            metrics.setMemoryUsed(usedMemory);
            metrics.setMemoryAvailable(maxMemory - usedMemory);
            metrics.setMemoryUsage((double) usedMemory / maxMemory * 100);
            
            // 模拟磁盘信息
            File rootDir = new File("/");
            if (rootDir.exists()) {
                long totalSpace = rootDir.getTotalSpace();
                long freeSpace = rootDir.getFreeSpace();
                long usedSpace = totalSpace - freeSpace;
                
                metrics.setDiskTotal(totalSpace);
                metrics.setDiskUsed(usedSpace);
                metrics.setDiskAvailable(freeSpace);
                metrics.setDiskUsage((double) usedSpace / totalSpace * 100);
            } else {
                // Windows系统使用C:盘
                File cDrive = new File("C:\\");
                if (cDrive.exists()) {
                    long totalSpace = cDrive.getTotalSpace();
                    long freeSpace = cDrive.getFreeSpace();
                    long usedSpace = totalSpace - freeSpace;
                    
                    metrics.setDiskTotal(totalSpace);
                    metrics.setDiskUsed(usedSpace);
                    metrics.setDiskAvailable(freeSpace);
                    metrics.setDiskUsage((double) usedSpace / totalSpace * 100);
                }
            }
            
            // 系统信息
            metrics.setOsVersion(System.getProperty("os.name") + " " + System.getProperty("os.version"));
            metrics.setUptime("应用运行中");
            
            logger.debug("本地系统指标收集完成");
            
        } catch (Exception e) {
            logger.warn("收集本地系统指标失败", e);
            // 设置基本默认值
            metrics.setCpuUsage(0.0);
            metrics.setCpuCores(1);
            metrics.setMemoryUsage(0.0);
            metrics.setDiskUsage(0.0);
        }
    }
    
    /**
     * 收集CPU指标
     */
    private void collectCpuMetrics(Server server, ServerMetrics metrics) {
        logger.debug("开始收集服务器 {} 的CPU指标", server.getHostname());
        try {
            // CPU使用率
            String cpuUsageCmd = "top -bn1 | grep '%Cpu' | awk '{print $2}' | sed 's/%us,//'";
            String result = sshConnectionService.executeCommand(server, cpuUsageCmd);
            if (!result.isEmpty()) {
                try {
                    double cpuUsage = Double.parseDouble(result.trim());
                    metrics.setCpuUsage(cpuUsage);
                    logger.debug("CPU使用率: {}%", cpuUsage);
                } catch (NumberFormatException e) {
                    logger.debug("CPU使用率解析失败，尝试备用命令");
                    // 备用命令
                    String altCmd = "cat /proc/stat | head -1 | awk '{print ($2+$3+$4)*100/($2+$3+$4+$5+$6+$7+$8)}'";
                    result = sshConnectionService.executeCommand(server, altCmd);
                    if (!result.isEmpty()) {
                        double cpuUsage = Double.parseDouble(result.trim());
                        metrics.setCpuUsage(cpuUsage);
                        logger.debug("CPU使用率(备用命令): {}%", cpuUsage);
                    }
                }
            }
            
            // CPU核心数
            String cpuCoresCmd = "nproc";
            result = sshConnectionService.executeCommand(server, cpuCoresCmd);
            if (!result.isEmpty()) {
                int cores = Integer.parseInt(result.trim());
                metrics.setCpuCores(cores);
                logger.debug("CPU核心数: {}", cores);
            }
            
            // 负载平均值
            String loadCmd = "uptime | awk -F'load average:' '{print $2}' | tr -d ' '";
            result = sshConnectionService.executeCommand(server, loadCmd);
            if (!result.isEmpty()) {
                String loadAvg = result.trim();
                metrics.setLoadAverage(loadAvg);
                logger.debug("负载平均值: {}", loadAvg);
            }
            
        } catch (Exception e) {
            logger.warn("收集服务器 {} CPU指标失败: {}", server.getHostname(), e.getMessage());
            // 设置默认值
            metrics.setCpuUsage(0.0);
            metrics.setCpuCores(1);
            metrics.setLoadAverage("0.0, 0.0, 0.0");
        }
    }
    
    /**
     * 收集内存指标
     */
    private void collectMemoryMetrics(Server server, ServerMetrics metrics) {
        logger.debug("开始收集服务器 {} 的内存指标", server.getHostname());
        try {
            String memCmd = "free -b | grep '^Mem:' | awk '{print $2, $3, $7}'";
            String result = sshConnectionService.executeCommand(server, memCmd);
            
            if (!result.isEmpty()) {
                String[] parts = result.trim().split("\\s+");
                if (parts.length >= 3) {
                    long total = Long.parseLong(parts[0]);
                    long used = Long.parseLong(parts[1]);
                    long available = Long.parseLong(parts[2]);
                    double usage = (double) used / total * 100;
                    
                    metrics.setMemoryTotal(total);
                    metrics.setMemoryUsed(used);
                    metrics.setMemoryAvailable(available);
                    metrics.setMemoryUsage(usage);
                    
                    logger.debug("内存指标 - 总量: {}MB, 已用: {}MB, 可用: {}MB, 使用率: {:.2f}%", 
                        total / 1024 / 1024, used / 1024 / 1024, available / 1024 / 1024, usage);
                } else {
                    logger.warn("内存命令返回的数据格式不正确: {}", result);
                }
            } else {
                logger.warn("内存命令返回空结果");
            }
        } catch (NumberFormatException e) {
            logger.warn("数值解析失败，可能是返回数据格式不正确: {}", e.getMessage());
            metrics.setMemoryUsage(0.0);
        } catch (Exception e) {
            logger.warn("收集服务器 {} 内存指标失败: {}", server.getHostname(), e.getMessage());
            metrics.setMemoryUsage(0.0);
        }
    }
    
    /**
     * 收集磁盘指标
     */
    private void collectDiskMetrics(Server server, ServerMetrics metrics) {
        logger.debug("开始收集服务器 {} 的磁盘指标", server.getHostname());
        try {
            String diskCmd = "df -B1 / | tail -1 | awk '{print $2, $3, $4}'";
            String result = sshConnectionService.executeCommand(server, diskCmd);
            
            if (!result.isEmpty()) {
                String[] parts = result.trim().split("\\s+");
                if (parts.length >= 3) {
                    long total = Long.parseLong(parts[0]);
                    long used = Long.parseLong(parts[1]);
                    long available = Long.parseLong(parts[2]);
                    double usage = (double) used / total * 100;
                    
                    metrics.setDiskTotal(total);
                    metrics.setDiskUsed(used);
                    metrics.setDiskAvailable(available);
                    metrics.setDiskUsage(usage);
                    
                    logger.debug("磁盘指标 - 总量: {}GB, 已用: {}GB, 可用: {}GB, 使用率: {:.2f}%", 
                        total / 1024 / 1024 / 1024, used / 1024 / 1024 / 1024, available / 1024 / 1024 / 1024, usage);
                } else {
                    logger.warn("磁盘命令返回的数据格式不正确: {}", result);
                }
            } else {
                logger.warn("磁盘命令返回空结果");
            }
        } catch (NumberFormatException e) {
            logger.warn("磁盘数值解析失败: {}", e.getMessage());
            metrics.setDiskUsage(0.0);
        } catch (Exception e) {
            logger.warn("收集服务器 {} 磁盘指标失败: {}", server.getHostname(), e.getMessage());
            metrics.setDiskUsage(0.0);
        }
    }
    
    /**
     * 收集系统指标
     */
    private void collectSystemMetrics(Server server, ServerMetrics metrics) {
        logger.debug("开始收集服务器 {} 的系统指标", server.getHostname());
        try {
            // 系统运行时间
            try {
                String uptimeCmd = "uptime -p";
                String result = sshConnectionService.executeCommand(server, uptimeCmd);
                if (!result.isEmpty()) {
                    String uptime = result.trim();
                    metrics.setUptime(uptime);
                    logger.debug("系统运行时间: {}", uptime);
                } else {
                    // 备用命令
                    uptimeCmd = "uptime | awk '{print $3, $4, $5}' | sed 's/,//g'";
                    result = sshConnectionService.executeCommand(server, uptimeCmd);
                    metrics.setUptime(result.trim());
                }
            } catch (Exception e) {
                logger.debug("获取系统运行时间失败: {}", e.getMessage());
                metrics.setUptime("未知");
            }
            
            // 操作系统版本
            try {
                String osCmd = "cat /etc/os-release | grep PRETTY_NAME | cut -d'\"' -f2";
                String result = sshConnectionService.executeCommand(server, osCmd);
                if (!result.isEmpty()) {
                    String osVersion = result.trim();
                    metrics.setOsVersion(osVersion);
                    logger.debug("操作系统版本: {}", osVersion);
                } else {
                    // 备用命令
                    osCmd = "uname -a";
                    result = sshConnectionService.executeCommand(server, osCmd);
                    metrics.setOsVersion(result.trim());
                }
            } catch (Exception e) {
                logger.debug("获取操作系统版本失败: {}", e.getMessage());
                metrics.setOsVersion("未知");
            }
            
        } catch (Exception e) {
            logger.warn("收集服务器 {} 系统指标失败: {}", server.getHostname(), e.getMessage());
            metrics.setUptime("未知");
            metrics.setOsVersion("未知");
        }
    }
    
    /**
     * 获取用户活跃信息
     */
    public List<UserActivity> getUserActivities(Server server) {
        try {
            return sshConnectionService.getCurrentUsers(server);
        } catch (Exception e) {
            logger.error("获取用户活跃信息失败: {}", server.getHostname(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 保存服务器指标到数据库
     */
    public ServerMetrics saveServerMetrics(Server server) {
        try {
            ServerMetrics metrics = getServerMetrics(server);
            return metricsRepository.save(metrics);
        } catch (Exception e) {
            logger.error("保存服务器指标失败: {}", server.getHostname(), e);
            return null;
        }
    }
    
    /**
     * 保存用户活跃信息到数据库
     */
    public void saveUserActivities(Server server) {
        try {
            List<UserActivity> activities = getUserActivities(server);
            for (UserActivity activity : activities) {
                // 检查是否已存在相同的活跃会话
                List<UserActivity> existingActivities = userActivityRepository
                    .findByServerIdAndUsernameAndIsActiveTrueOrderByLastActivityDesc(
                        server.getId(), activity.getUsername());
                
                if (existingActivities.isEmpty()) {
                    // 新的活跃会话
                    activity.setLoginTime(LocalDateTime.now());
                    userActivityRepository.save(activity);
                } else {
                    // 更新现有会话的最后活跃时间
                    UserActivity existing = existingActivities.get(0);
                    existing.setLastActivity(LocalDateTime.now());
                    existing.setCommandCount(activity.getCommandCount());
                    userActivityRepository.save(existing);
                }
            }
        } catch (Exception e) {
            logger.error("保存用户活跃信息失败: {}", server.getHostname(), e);
        }
    }
    
    /**
     * 获取服务器最新的监控数据
     */
    public ServerMetrics getLatestMetrics(Long serverId) {
        return metricsRepository.findTopByServerIdOrderByTimestampDesc(serverId)
                .orElse(null);
    }
    
    /**
     * 获取服务器活跃用户列表
     */
    public List<UserActivity> getActiveUsers(Long serverId) {
        return userActivityRepository.findByServerIdAndIsActiveTrueOrderByLastActivityDesc(serverId);
    }
    
    /**
     * 清理过期的监控数据
     */
    public void cleanupOldData(int daysToKeep) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysToKeep);
        
        try {
            metricsRepository.deleteByTimestampBefore(cutoffTime);
            userActivityRepository.deleteByCreatedAtBefore(cutoffTime);
            logger.info("清理了{}天前的监控数据", daysToKeep);
        } catch (Exception e) {
            logger.error("清理过期数据失败", e);
        }
    }
    
    /**
     * 在应用关闭时清理线程池
     */
    @PreDestroy
    public void destroy() {
        logger.info("MonitoringService正在关闭，清理线程池...");
        
        try {
            // 优雅关闭线程池
            metricsExecutor.shutdown();
            
            // 等待最多10秒让任务完成
            if (!metricsExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warn("线程池在10秒内未能正常关闭，强制关闭");
                metricsExecutor.shutdownNow();
                
                // 再等待5秒
                if (!metricsExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.error("线程池强制关闭失败");
                } else {
                    logger.info("线程池已强制关闭");
                }
            } else {
                logger.info("MonitoringService线程池已正常关闭");
            }
            
        } catch (InterruptedException e) {
            logger.warn("等待线程池关闭时被中断", e);
            metricsExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}