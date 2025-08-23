package com.cmict.internalpaas.repository;

import com.cmict.internalpaas.model.ServerMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
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
}