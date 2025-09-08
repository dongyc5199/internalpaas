package com.cmict.internalpaas.repository;

import com.cmict.internalpaas.model.ServerStatusSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServerStatusSnapshotRepository extends JpaRepository<ServerStatusSnapshot, Long> {
    
    /**
     * 根据服务器ID查询最新的快照
     */
    @Query("SELECT s FROM ServerStatusSnapshot s WHERE s.serverId = :serverId " +
           "ORDER BY s.snapshotTime DESC")
    List<ServerStatusSnapshot> findByServerIdOrderBySnapshotTimeDesc(Long serverId);
    
    /**
     * 获取服务器最新的快照
     */
    default Optional<ServerStatusSnapshot> findLatestByServerId(Long serverId) {
        List<ServerStatusSnapshot> snapshots = findByServerIdOrderBySnapshotTimeDesc(serverId);
        return snapshots.isEmpty() ? Optional.empty() : Optional.of(snapshots.get(0));
    }
    
    /**
     * 根据时间范围查询服务器快照历史
     */
    @Query("SELECT s FROM ServerStatusSnapshot s WHERE s.serverId = :serverId " +
           "AND s.snapshotTime >= :startTime AND s.snapshotTime <= :endTime " +
           "ORDER BY s.snapshotTime DESC")
    List<ServerStatusSnapshot> findByServerIdAndTimeRange(@Param("serverId") Long serverId,
                                                         @Param("startTime") LocalDateTime startTime,
                                                         @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询指定时间之后的服务器快照
     */
    List<ServerStatusSnapshot> findByServerIdAndSnapshotTimeAfter(Long serverId, LocalDateTime afterTime);
    
    /**
     * 查询有内存告警的快照
     */
    @Query("SELECT s FROM ServerStatusSnapshot s WHERE s.serverId = :serverId " +
           "AND s.memoryUsagePercent >= :threshold ORDER BY s.snapshotTime DESC")
    List<ServerStatusSnapshot> findMemoryAlertSnapshots(@Param("serverId") Long serverId,
                                                       @Param("threshold") Double threshold);
    
    /**
     * 查询所有服务器的最新快照
     */
    @Query("SELECT s1 FROM ServerStatusSnapshot s1 WHERE s1.snapshotTime = " +
           "(SELECT MAX(s2.snapshotTime) FROM ServerStatusSnapshot s2 WHERE s2.serverId = s1.serverId)")
    List<ServerStatusSnapshot> findLatestSnapshotsForAllServers();
    
    /**
     * 查询有资源告警的服务器快照
     */
    @Query("SELECT s FROM ServerStatusSnapshot s WHERE " +
           "(s.memoryUsagePercent >= :memoryThreshold OR " +
           " s.cpuUsagePercent >= :cpuThreshold OR " +
           " s.diskUsagePercent >= :diskThreshold) " +
           "ORDER BY s.snapshotTime DESC")
    List<ServerStatusSnapshot> findResourceAlertSnapshots(@Param("memoryThreshold") Double memoryThreshold,
                                                         @Param("cpuThreshold") Double cpuThreshold,
                                                         @Param("diskThreshold") Double diskThreshold);
    
    /**
     * 删除指定时间之前的快照数据（用于清理历史数据）
     */
    void deleteBySnapshotTimeBefore(LocalDateTime beforeTime);
    
    /**
     * 删除指定服务器的所有快照
     */
    void deleteByServerId(Long serverId);
    
    /**
     * 统计服务器快照数量
     */
    Long countByServerId(Long serverId);
    
    /**
     * 查询服务器快照的统计信息
     */
    @Query("SELECT " +
           "AVG(s.memoryUsagePercent), " +
           "MAX(s.memoryUsagePercent), " +
           "AVG(s.cpuUsagePercent), " +
           "MAX(s.cpuUsagePercent), " +
           "COUNT(s) " +
           "FROM ServerStatusSnapshot s " +
           "WHERE s.serverId = :serverId " +
           "AND s.snapshotTime >= :startTime")
    Object[] getServerStatistics(@Param("serverId") Long serverId,
                                @Param("startTime") LocalDateTime startTime);
    
    /**
     * 查询在线服务器快照
     */
    @Query("SELECT s FROM ServerStatusSnapshot s WHERE s.isOnline = true " +
           "ORDER BY s.snapshotTime DESC")
    List<ServerStatusSnapshot> findOnlineServerSnapshots();
    
    /**
     * 查询有活跃用户的服务器快照
     */
    @Query("SELECT s FROM ServerStatusSnapshot s WHERE s.activeUserCount > 0 " +
           "ORDER BY s.activeUserCount DESC, s.snapshotTime DESC")
    List<ServerStatusSnapshot> findActiveUserSnapshots();
    
    /**
     * 按服务器ID查询最近N个快照
     */
    @Query(value = "SELECT * FROM server_status_snapshots WHERE server_id = :serverId " +
                   "ORDER BY snapshot_time DESC LIMIT :limit", 
           nativeQuery = true)
    List<ServerStatusSnapshot> findRecentSnapshotsByServerId(@Param("serverId") Long serverId,
                                                           @Param("limit") int limit);
}