package com.cmict.internalpaas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;

/**
 * 性能监控服务
 * 负责收集和报告系统性能指标
 */
@Service
public class PerformanceMonitoringService {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitoringService.class);
    private static final Logger metricsLogger = LoggerFactory.getLogger("metrics");
    
    private final MemoryMXBean memoryMXBean;
    private final ThreadMXBean threadMXBean;
    private final List<GarbageCollectorMXBean> gcMXBeans;
    private final MBeanServer mBeanServer;
    
    // 性能计数器
    private final AtomicLong requestCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final AtomicLong dbQueryCount = new AtomicLong(0);
    private final AtomicLong cacheHitCount = new AtomicLong(0);
    private final AtomicLong cacheMissCount = new AtomicLong(0);
    
    // 历史GC数据
    private long lastGcCollectionCount = 0;
    private long lastGcCollectionTime = 0;
    
    public PerformanceMonitoringService() {
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        this.gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
        this.mBeanServer = ManagementFactory.getPlatformMBeanServer();
        
        logger.info("性能监控服务已初始化");
    }
    
    /**
     * 收集系统性能指标
     * 每30秒执行一次
     */
    @Scheduled(fixedRate = 30000)
    public void collectPerformanceMetrics() {
        try {
            PerformanceSnapshot snapshot = createPerformanceSnapshot();
            
            // 记录指标到专用日志文件
            metricsLogger.info("METRICS|{}|{}|{}|{}|{}|{}|{}|{}|{}|{}|{}|{}",
                snapshot.timestamp,
                snapshot.memoryUsedRatio,
                snapshot.heapUsedMB,
                snapshot.heapMaxMB,
                snapshot.threadCount,
                snapshot.gcCollectionCount,
                snapshot.gcCollectionTime,
                snapshot.requestCount,
                snapshot.errorCount,
                snapshot.dbQueryCount,
                snapshot.cacheHitRatio,
                snapshot.cpuUsage
            );
            
            // 检查告警条件
            checkPerformanceAlerts(snapshot);
            
        } catch (Exception e) {
            logger.error("收集性能指标失败", e);
        }
    }
    
    /**
     * 创建性能快照
     */
    private PerformanceSnapshot createPerformanceSnapshot() {
        PerformanceSnapshot snapshot = new PerformanceSnapshot();
        
        // 时间戳
        snapshot.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        // 内存使用情况
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        snapshot.heapUsedMB = heapMemoryUsage.getUsed() / (1024 * 1024);
        snapshot.heapMaxMB = heapMemoryUsage.getMax() / (1024 * 1024);
        snapshot.memoryUsedRatio = (double) heapMemoryUsage.getUsed() / heapMemoryUsage.getMax();
        
        // 线程信息
        snapshot.threadCount = threadMXBean.getThreadCount();
        snapshot.peakThreadCount = threadMXBean.getPeakThreadCount();
        
        // GC信息
        long totalGcCount = 0;
        long totalGcTime = 0;
        for (GarbageCollectorMXBean gcBean : gcMXBeans) {
            totalGcCount += gcBean.getCollectionCount();
            totalGcTime += gcBean.getCollectionTime();
        }
        snapshot.gcCollectionCount = totalGcCount - lastGcCollectionCount;
        snapshot.gcCollectionTime = totalGcTime - lastGcCollectionTime;
        lastGcCollectionCount = totalGcCount;
        lastGcCollectionTime = totalGcTime;
        
        // CPU使用率（通过JMX获取）
        snapshot.cpuUsage = getProcessCpuUsage();
        
        // 应用指标
        snapshot.requestCount = requestCount.get();
        snapshot.errorCount = errorCount.get();
        snapshot.dbQueryCount = dbQueryCount.get();
        
        // 缓存命中率
        long totalCacheRequests = cacheHitCount.get() + cacheMissCount.get();
        snapshot.cacheHitRatio = totalCacheRequests > 0 ? 
            (double) cacheHitCount.get() / totalCacheRequests : 0.0;
        
        return snapshot;
    }
    
    /**
     * 获取进程CPU使用率
     */
    private double getProcessCpuUsage() {
        try {
            ObjectName objectName = new ObjectName("java.lang:type=OperatingSystem");
            Object cpuUsage = mBeanServer.getAttribute(objectName, "ProcessCpuLoad");
            if (cpuUsage instanceof Double) {
                return (Double) cpuUsage;
            }
        } catch (Exception e) {
            logger.debug("获取CPU使用率失败", e);
        }
        return -1.0;
    }
    
    /**
     * 检查性能告警条件
     */
    private void checkPerformanceAlerts(PerformanceSnapshot snapshot) {
        // 内存使用率告警
        if (snapshot.memoryUsedRatio > 0.85) {
            logger.warn("内存使用率告警: {:.2f}% (堆内存: {}/{}MB)", 
                snapshot.memoryUsedRatio * 100, snapshot.heapUsedMB, snapshot.heapMaxMB);
        }
        
        // CPU使用率告警
        if (snapshot.cpuUsage > 0.80) {
            logger.warn("CPU使用率告警: {:.2f}%", snapshot.cpuUsage * 100);
        }
        
        // 线程数告警
        if (snapshot.threadCount > 200) {
            logger.warn("活跃线程数告警: {} (峰值: {})", snapshot.threadCount, snapshot.peakThreadCount);
        }
        
        // GC频率告警
        if (snapshot.gcCollectionCount > 10) {
            logger.warn("GC频率告警: {}次/30秒, 总耗时: {}ms", 
                snapshot.gcCollectionCount, snapshot.gcCollectionTime);
        }
        
        // 错误率告警
        if (snapshot.requestCount > 0) {
            double errorRate = (double) snapshot.errorCount / snapshot.requestCount;
            if (errorRate > 0.05) { // 5%错误率
                logger.warn("错误率告警: {:.2f}% ({}/{})", 
                    errorRate * 100, snapshot.errorCount, snapshot.requestCount);
            }
        }
        
        // 缓存命中率告警
        if (snapshot.cacheHitRatio < 0.70 && (cacheHitCount.get() + cacheMissCount.get()) > 100) {
            logger.warn("缓存命中率告警: {:.2f}%", snapshot.cacheHitRatio * 100);
        }
    }
    
    /**
     * 生成性能报告
     */
    public String generatePerformanceReport() {
        PerformanceSnapshot snapshot = createPerformanceSnapshot();
        
        StringBuilder report = new StringBuilder();
        report.append("=== 性能监控报告 ===\n");
        report.append("生成时间: ").append(snapshot.timestamp).append("\n\n");
        
        report.append("内存使用情况:\n");
        report.append(String.format("  堆内存: %d/%dMB (%.2f%%)\n", 
            snapshot.heapUsedMB, snapshot.heapMaxMB, snapshot.memoryUsedRatio * 100));
        
        report.append("\n线程信息:\n");
        report.append(String.format("  当前线程数: %d\n", snapshot.threadCount));
        report.append(String.format("  峰值线程数: %d\n", snapshot.peakThreadCount));
        
        if (snapshot.cpuUsage >= 0) {
            report.append("\nCPU使用率:\n");
            report.append(String.format("  进程CPU: %.2f%%\n", snapshot.cpuUsage * 100));
        }
        
        report.append("\nGC统计 (最近30秒):\n");
        report.append(String.format("  回收次数: %d\n", snapshot.gcCollectionCount));
        report.append(String.format("  回收耗时: %dms\n", snapshot.gcCollectionTime));
        
        report.append("\n应用指标:\n");
        report.append(String.format("  请求总数: %d\n", snapshot.requestCount));
        report.append(String.format("  错误数量: %d\n", snapshot.errorCount));
        report.append(String.format("  数据库查询: %d\n", snapshot.dbQueryCount));
        
        if ((cacheHitCount.get() + cacheMissCount.get()) > 0) {
            report.append(String.format("  缓存命中率: %.2f%%\n", snapshot.cacheHitRatio * 100));
        }
        
        return report.toString();
    }
    
    /**
     * 重置计数器
     */
    public void resetCounters() {
        requestCount.set(0);
        errorCount.set(0);
        dbQueryCount.set(0);
        cacheHitCount.set(0);
        cacheMissCount.set(0);
        
        logger.info("性能计数器已重置");
    }
    
    // 计数器增加方法
    public void incrementRequestCount() {
        requestCount.incrementAndGet();
    }
    
    public void incrementErrorCount() {
        errorCount.incrementAndGet();
    }
    
    public void incrementDbQueryCount() {
        dbQueryCount.incrementAndGet();
    }
    
    public void incrementCacheHit() {
        cacheHitCount.incrementAndGet();
    }
    
    public void incrementCacheMiss() {
        cacheMissCount.incrementAndGet();
    }
    
    /**
     * 性能快照数据类
     */
    private static class PerformanceSnapshot {
        String timestamp;
        double memoryUsedRatio;
        long heapUsedMB;
        long heapMaxMB;
        int threadCount;
        int peakThreadCount;
        long gcCollectionCount;
        long gcCollectionTime;
        double cpuUsage;
        long requestCount;
        long errorCount;
        long dbQueryCount;
        double cacheHitRatio;
    }
    
    /**
     * 每天生成性能日报
     * 每天凌晨1点执行
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void generateDailyReport() {
        try {
            String report = generatePerformanceReport();
            logger.info("每日性能报告:\n{}", report);
            
            // 重置计数器
            resetCounters();
            
        } catch (Exception e) {
            logger.error("生成每日性能报告失败", e);
        }
    }
}