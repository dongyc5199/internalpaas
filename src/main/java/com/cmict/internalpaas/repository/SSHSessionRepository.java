package com.cmict.internalpaas.repository;

import com.cmict.internalpaas.model.SSHSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SSHSessionRepository extends JpaRepository<SSHSession, Long> {
    
    /**
     * 根据会话ID查找SSH会话
     */
    Optional<SSHSession> findBySessionId(String sessionId);
    
    /**
     * 查找指定服务器的活跃SSH会话
     */
    List<SSHSession> findByServerIdAndIsActiveTrueOrderByStartTimeDesc(Long serverId);
    
    /**
     * 查找指定用户的活跃SSH会话
     */
    List<SSHSession> findByUserIdAndIsActiveTrueOrderByStartTimeDesc(Long userId);
    
    /**
     * 查找指定服务器和用户的活跃SSH会话
     */
    List<SSHSession> findByServerIdAndUserIdAndIsActiveTrueOrderByStartTimeDesc(Long serverId, Long userId);
    
    /**
     * 查找指定时间范围内的SSH会话
     */
    List<SSHSession> findByStartTimeBetweenOrderByStartTimeDesc(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 统计指定服务器的活跃会话数
     */
    long countByServerIdAndIsActiveTrue(Long serverId);
    
    /**
     * 统计指定服务器的总会话数
     */
    long countByServerId(Long serverId);
    
    /**
     * 查找指定服务器的所有会话（按开始时间降序）
     */
    List<SSHSession> findByServerIdOrderByStartTimeDesc(Long serverId);
    
    /**
     * 统计指定用户的活跃会话数
     */
    long countByUserIdAndIsActiveTrue(Long userId);
    
    /**
     * 查找超时的活跃会话
     */
    @Query("SELECT s FROM SSHSession s WHERE s.isActive = true " +
           "AND s.lastHeartbeat < :timeoutThreshold")
    List<SSHSession> findTimeoutActiveSessions(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);
    
    /**
     * 查找需要清理的会话（结束时间超过指定天数）
     */
    @Query("SELECT s FROM SSHSession s WHERE s.isActive = false " +
           "AND s.endTime < :cutoffTime")
    List<SSHSession> findSessionsForCleanup(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * 统计各会话状态的数量
     */
    @Query("SELECT s.sessionStatus, COUNT(s) FROM SSHSession s " +
           "WHERE s.serverId = :serverId GROUP BY s.sessionStatus")
    List<Object[]> countSessionStatusByServer(@Param("serverId") Long serverId);
    
    /**
     * 查找指定客户端IP的会话
     */
    List<SSHSession> findByClientIpOrderByStartTimeDesc(String clientIp);
    
    /**
     * 查找最近的会话记录
     */
    List<SSHSession> findTop20ByOrderByStartTimeDesc();
    
    /**
     * 删除指定时间之前的历史会话记录
     */
    void deleteByStartTimeBefore(LocalDateTime cutoffTime);
    
    /**
     * 查找长时间运行的会话
     */
    @Query("SELECT s FROM SSHSession s WHERE s.isActive = true " +
           "AND s.startTime < :timeThreshold")
    List<SSHSession> findLongRunningSessions(@Param("timeThreshold") LocalDateTime timeThreshold);
}