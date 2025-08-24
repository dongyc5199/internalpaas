package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.UserActivity;
import com.cmict.internalpaas.model.ServerMetrics;
import com.cmict.internalpaas.repository.UserActivityRepository;
import com.cmict.internalpaas.repository.ServerMetricsRepository;
import com.cmict.internalpaas.repository.SSHSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 批处理服务
 * 负责优化大量数据操作的性能
 */
@Service
@Transactional
public class BatchProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchProcessingService.class);
    private static final int BATCH_SIZE = 100;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private UserActivityRepository activityRepository;
    
    @Autowired
    private ServerMetricsRepository metricsRepository;
    
    @Autowired
    private SSHSessionRepository sshSessionRepository;
    
    /**
     * 批量保存用户活动记录
     * @param activities 用户活动列表
     */
    public void batchSaveUserActivities(List<UserActivity> activities) {
        if (activities == null || activities.isEmpty()) {
            return;
        }
        
        logger.debug("开始批量保存{}条用户活动记录", activities.size());
        
        // 分批处理，每批最多100条
        for (int i = 0; i < activities.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, activities.size());
            List<UserActivity> batch = activities.subList(i, endIndex);
            
            // 批量保存
            activityRepository.saveAll(batch);
            
            // 每5批强制刷新一次，避免内存积累
            if (i % (BATCH_SIZE * 5) == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        
        logger.debug("批量保存用户活动记录完成");
    }
    
    /**
     * 批量保存服务器监控数据
     * @param metrics 监控数据列表
     */
    public void batchSaveServerMetrics(List<ServerMetrics> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return;
        }
        
        logger.debug("开始批量保存{}条服务器监控数据", metrics.size());
        
        for (int i = 0; i < metrics.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, metrics.size());
            List<ServerMetrics> batch = metrics.subList(i, endIndex);
            
            metricsRepository.saveAll(batch);
            
            if (i % (BATCH_SIZE * 5) == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        
        logger.debug("批量保存服务器监控数据完成");
    }
    
    /**
     * 批量更新会话状态为非活跃
     * @param timeoutThreshold 超时阈值
     * @return 更新的记录数
     */
    @Transactional
    public int batchDeactivateTimeoutSessions(LocalDateTime timeoutThreshold) {
        logger.debug("开始批量更新超时会话状态，超时阈值: {}", timeoutThreshold);
        
        String jpql = "UPDATE UserActivity ua SET ua.isActive = false " +
                     "WHERE ua.isActive = true AND ua.lastActivity < :timeoutThreshold";
        
        int updatedCount = entityManager.createQuery(jpql)
            .setParameter("timeoutThreshold", timeoutThreshold)
            .executeUpdate();
        
        logger.info("批量更新超时会话状态完成，更新了{}条记录", updatedCount);
        return updatedCount;
    }
    
    /**
     * 定时清理历史数据
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupHistoricalData() {
        logger.info("开始执行历史数据清理任务");
        
        try {
            // 清理30天前的用户活动记录
            LocalDateTime activityCutoff = LocalDateTime.now().minusDays(30);
            cleanupUserActivities(activityCutoff);
            
            // 清理7天前的详细监控数据（保留每小时的汇总数据）
            LocalDateTime metricsCutoff = LocalDateTime.now().minusDays(7);
            cleanupServerMetrics(metricsCutoff);
            
            // 清理30天前的SSH会话记录
            LocalDateTime sessionCutoff = LocalDateTime.now().minusDays(30);
            cleanupSSHSessions(sessionCutoff);
            
            logger.info("历史数据清理任务完成");
            
        } catch (Exception e) {
            logger.error("历史数据清理任务执行失败", e);
        }
    }
    
    /**
     * 清理用户活动历史记录
     */
    private void cleanupUserActivities(LocalDateTime cutoffTime) {
        logger.debug("清理{}之前的用户活动记录", cutoffTime);
        
        String jpql = "DELETE FROM UserActivity ua WHERE ua.createdAt < :cutoffTime";
        int deletedCount = entityManager.createQuery(jpql)
            .setParameter("cutoffTime", cutoffTime)
            .executeUpdate();
        
        logger.info("清理用户活动记录完成，删除了{}条记录", deletedCount);
    }
    
    /**
     * 清理服务器监控历史数据
     */
    private void cleanupServerMetrics(LocalDateTime cutoffTime) {
        logger.debug("清理{}之前的服务器监控数据", cutoffTime);
        
        String jpql = "DELETE FROM ServerMetrics sm WHERE sm.timestamp < :cutoffTime";
        int deletedCount = entityManager.createQuery(jpql)
            .setParameter("cutoffTime", cutoffTime)
            .executeUpdate();
        
        logger.info("清理服务器监控数据完成，删除了{}条记录", deletedCount);
    }
    
    /**
     * 清理SSH会话历史记录
     */
    private void cleanupSSHSessions(LocalDateTime cutoffTime) {
        logger.debug("清理{}之前的SSH会话记录", cutoffTime);
        
        String jpql = "DELETE FROM SSHSession ss WHERE ss.startTime < :cutoffTime AND ss.isActive = false";
        int deletedCount = entityManager.createQuery(jpql)
            .setParameter("cutoffTime", cutoffTime)
            .executeUpdate();
        
        logger.info("清理SSH会话记录完成，删除了{}条记录", deletedCount);
    }
    
    /**
     * 优化数据库统计信息
     * 每周日凌晨3点执行
     */
    @Scheduled(cron = "0 0 3 * * 0")
    public void optimizeDatabase() {
        logger.info("开始执行数据库优化任务");
        
        try {
            // 对于H2数据库，执行ANALYZE命令更新统计信息
            entityManager.createNativeQuery("ANALYZE").executeUpdate();
            
            // 压缩数据库文件
            entityManager.createNativeQuery("SHUTDOWN COMPACT").executeUpdate();
            
            logger.info("数据库优化任务完成");
            
        } catch (Exception e) {
            logger.warn("数据库优化任务执行失败", e);
        }
    }
    
    /**
     * 批量更新活跃会话的最后活动时间
     * 每5分钟执行一次，将频繁更新操作批量化
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void batchUpdateActiveSessionHeartbeat() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime fiveMinutesAgo = now.minusMinutes(5);
            
            // 批量更新最近5分钟内有活动的会话
            String jpql = "UPDATE UserActivity ua SET ua.lastActivity = :now " +
                         "WHERE ua.isActive = true AND ua.lastActivity > :fiveMinutesAgo";
            
            int updatedCount = entityManager.createQuery(jpql)
                .setParameter("now", now)
                .setParameter("fiveMinutesAgo", fiveMinutesAgo)
                .executeUpdate();
            
            if (updatedCount > 0) {
                logger.debug("批量更新{}个活跃会话的心跳时间", updatedCount);
            }
            
        } catch (Exception e) {
            logger.warn("批量更新活跃会话心跳时间失败", e);
        }
    }
    
    /**
     * 获取数据库统计信息
     * @return 统计信息字符串
     */
    public String getDatabaseStatistics() {
        try {
            // 获取各表的记录数
            long userActivityCount = activityRepository.count();
            long serverMetricsCount = metricsRepository.count();
            long sshSessionCount = sshSessionRepository.count();
            
            StringBuilder stats = new StringBuilder();
            stats.append("数据库统计信息:\n");
            stats.append(String.format("- 用户活动记录: %,d 条\n", userActivityCount));
            stats.append(String.format("- 服务器监控数据: %,d 条\n", serverMetricsCount));
            stats.append(String.format("- SSH会话记录: %,d 条\n", sshSessionCount));
            
            return stats.toString();
            
        } catch (Exception e) {
            logger.error("获取数据库统计信息失败", e);
            return "获取统计信息失败: " + e.getMessage();
        }
    }
}