package com.cmict.internalpaas.repository;

import com.cmict.internalpaas.model.ServerMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
// import org.springframework.stereotype.Repository; // ❌ 已废弃 - @Repository注解已停用

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ⚠️ 已废弃: ServerMetrics JPA 仓库
 * 
 * 原因: 监控数据存储已迁移到 Metrics Hub (PostgreSQL/TimescaleDB)
 * 迁移日期: 2025-10-17 (Phase4-Step3)
 * 
 * 架构演进:
 * - 旧: SSH轮询 → H2数据库 → ServerMetricsRepository
 * - 新: OTLP Agent → Hub → MetricsHubClient
 * 
 * 替代方案:
 * - 实时数据查询: {@link com.cmict.internalpaas.client.MetricsHubClient#getLatestMetrics(Long)}
 * - 历史数据查询: {@link com.cmict.internalpaas.client.MetricsHubClient#queryMetrics(...)}
 * - 时间范围查询: Hub API支持from/to参数
 * - 聚合数据查询: Hub提供5分钟/1小时Rollup表
 * 
 * Hub数据存储优势:
 * - ✅ 4级智能路由: Redis(5min) → Raw(30d) → 5m(90d) → 1h(365d)
 * - ✅ 自动聚合: 5分钟和1小时Rollup表
 * - ✅ 自动保留策略: 30/90/365天分级存储
 * - ✅ 高性能查询: TimescaleDB时序数据库优化
 * - ✅ 扩展性强: 支持1000+ 服务器
 * 
 * 如需临时恢复H2存储:
 * 1. 恢复 pom.xml 中的 H2 依赖
 * 2. 恢复 application.properties 中的 H2 配置
 * 3. 取消注释下方 @Repository 注解
 * 4. 重启应用
 * 
 * @deprecated 使用 {@link com.cmict.internalpaas.client.MetricsHubClient} 替代
 */
@Deprecated(since = "2025-10-17", forRemoval = true)
// @Repository // ❌ 已停用 (2025-10-17) - 取消注释可恢复H2存储
public interface ServerMetricsRepository extends JpaRepository<ServerMetrics, Long> {
    
    /**
     * 根据服务器ID查找最新的监控数据
     */
    Optional<ServerMetrics> findTopByServerIdOrderByTimestampDesc(Long serverId);
    
    /**
     * 根据服务器ID查找指定时间范围内的监控数据
     */
    List<ServerMetrics> findByServerIdAndTimestampBetweenOrderByTimestampDesc(
            Long serverId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 查找指定服务器最近N条记录
     */
    List<ServerMetrics> findTop10ByServerIdOrderByTimestampDesc(Long serverId);
    
    /**
     * 根据服务器ID查找所有监控数据（按时间降序）
     */
    List<ServerMetrics> findByServerIdOrderByTimestampDesc(Long serverId);
    
    /**
     * 删除指定时间之前的历史数据
     */
    void deleteByTimestampBefore(LocalDateTime cutoffTime);
    
    /**
     * 统计服务器监控数据数量
     */
    long countByServerId(Long serverId);
    
    /**
     * 查询指定时间范围内所有服务器的最新监控数据
     */
    @Query("SELECT m FROM ServerMetrics m WHERE m.timestamp = " +
           "(SELECT MAX(m2.timestamp) FROM ServerMetrics m2 WHERE m2.serverId = m.serverId " +
           "AND m2.timestamp BETWEEN :startTime AND :endTime)")
    List<ServerMetrics> findLatestMetricsForAllServers(@Param("startTime") LocalDateTime startTime, 
                                                       @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询CPU使用率超过阈值的服务器
     */
    @Query("SELECT m FROM ServerMetrics m WHERE m.cpuUsage > :threshold " +
           "AND m.timestamp = (SELECT MAX(m2.timestamp) FROM ServerMetrics m2 WHERE m2.serverId = m.serverId)")
    List<ServerMetrics> findServersWithHighCpuUsage(@Param("threshold") Double threshold);
    
    /**
     * 查询内存使用率超过阈值的服务器
     */
    @Query("SELECT m FROM ServerMetrics m WHERE m.memoryUsage > :threshold " +
           "AND m.timestamp = (SELECT MAX(m2.timestamp) FROM ServerMetrics m2 WHERE m2.serverId = m.serverId)")
    List<ServerMetrics> findServersWithHighMemoryUsage(@Param("threshold") Double threshold);
    
    /**
     * 查询磁盘使用率超过阈值的服务器
     */
    @Query("SELECT m FROM ServerMetrics m WHERE m.diskUsage > :threshold " +
           "AND m.timestamp = (SELECT MAX(m2.timestamp) FROM ServerMetrics m2 WHERE m2.serverId = m.serverId)")
    List<ServerMetrics> findServersWithHighDiskUsage(@Param("threshold") Double threshold);
    
    // ==================== 历史监控数据查询方法 ====================
    
    /**
     * 获取指定时间范围内的历史数据（按时间降序）
     */
    List<ServerMetrics> findByServerIdAndTimestampBetweenOrderByTimestampAsc(
            Long serverId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 获取所有服务器在指定时间范围内的历史数据
     */
    @Query("SELECT m FROM ServerMetrics m WHERE m.timestamp BETWEEN :startTime AND :endTime ORDER BY m.timestamp ASC")
    List<ServerMetrics> findAllByTimestampBetweenOrderByTimestampAsc(
            @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 获取多个服务器在指定时间范围内的历史数据
     */
    @Query("SELECT m FROM ServerMetrics m WHERE m.serverId IN :serverIds " +
           "AND m.timestamp BETWEEN :startTime AND :endTime ORDER BY m.timestamp ASC")
    List<ServerMetrics> findByServerIdsAndTimestampBetween(
            @Param("serverIds") List<Long> serverIds,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 按小时聚合数据（用于长时间范围的性能优化）
     * 简化版本，兼容H2数据库
     */
    @Query("SELECT AVG(m.cpuUsage), MAX(m.cpuUsage), MIN(m.cpuUsage), " +
           "AVG(m.memoryUsage), MAX(m.memoryUsage), MIN(m.memoryUsage), " +
           "AVG(m.diskUsage), MAX(m.diskUsage), MIN(m.diskUsage), " +
           "COUNT(m.id) " +
           "FROM ServerMetrics m WHERE m.serverId = :serverId " +
           "AND m.timestamp BETWEEN :startTime AND :endTime")
    Object[] findHourlyAggregatedMetrics(@Param("serverId") Long serverId,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);
    
    /**
     * 按天聚合数据（用于更长时间范围）
     * 简化版本，兼容H2数据库
     */
    @Query("SELECT AVG(m.cpuUsage), MAX(m.cpuUsage), MIN(m.cpuUsage), " +
           "AVG(m.memoryUsage), MAX(m.memoryUsage), MIN(m.memoryUsage), " +
           "AVG(m.diskUsage), MAX(m.diskUsage), MIN(m.diskUsage), " +
           "COUNT(m.id) " +
           "FROM ServerMetrics m WHERE m.serverId = :serverId " +
           "AND m.timestamp BETWEEN :startTime AND :endTime")
    Object[] findDailyAggregatedMetrics(@Param("serverId") Long serverId,
                                      @Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);
    
    /**
     * 获取服务器历史数据的统计信息
     */
    @Query("SELECT COUNT(m), MIN(m.timestamp), MAX(m.timestamp), " +
           "AVG(m.cpuUsage), AVG(m.memoryUsage), AVG(m.diskUsage) " +
           "FROM ServerMetrics m WHERE m.serverId = :serverId")
    Object[] getServerMetricsStatistics(@Param("serverId") Long serverId);
    
    /**
     * 查找CPU使用率异常高的时间点
     */
    @Query("SELECT m FROM ServerMetrics m WHERE m.serverId = :serverId " +
           "AND m.cpuUsage > :threshold AND m.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY m.cpuUsage DESC")
    List<ServerMetrics> findHighCpuPeriodsForServer(@Param("serverId") Long serverId,
                                                   @Param("threshold") Double threshold,
                                                   @Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找内存使用率异常高的时间点
     */
    @Query("SELECT m FROM ServerMetrics m WHERE m.serverId = :serverId " +
           "AND m.memoryUsage > :threshold AND m.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY m.memoryUsage DESC")
    List<ServerMetrics> findHighMemoryPeriodsForServer(@Param("serverId") Long serverId,
                                                      @Param("threshold") Double threshold,
                                                      @Param("startTime") LocalDateTime startTime,
                                                      @Param("endTime") LocalDateTime endTime);
    
    /**
     * 获取服务器性能排名数据
     */
    @Query("SELECT m.serverId, m.serverName, AVG(m.cpuUsage), AVG(m.memoryUsage), AVG(m.diskUsage) " +
           "FROM ServerMetrics m WHERE m.timestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY m.serverId, m.serverName " +
           "ORDER BY AVG(m.cpuUsage) DESC")
    List<Object[]> getServerPerformanceRanking(@Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);
    
    /**
     * 获取最近N分钟的数据（用于实时图表）
     */
    @Query("SELECT m FROM ServerMetrics m WHERE m.serverId = :serverId " +
           "AND m.timestamp >= :sinceTime ORDER BY m.timestamp ASC")
    List<ServerMetrics> findRecentMetrics(@Param("serverId") Long serverId,
                                        @Param("sinceTime") LocalDateTime sinceTime);
    
    /**
     * 按指定间隔获取采样数据（用于大数据量时的性能优化）
     * 简化版本，兼容H2数据库
     */
    @Query("SELECT m FROM ServerMetrics m WHERE m.serverId = :serverId " +
           "AND m.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY m.timestamp ASC")
    List<ServerMetrics> findSampledMetrics(@Param("serverId") Long serverId, 
                                         @Param("startTime") LocalDateTime startTime, 
                                         @Param("endTime") LocalDateTime endTime);
}