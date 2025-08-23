package com.cmict.internalpaas.repository;

import com.cmict.internalpaas.model.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {
    
    /**
     * 查找指定服务器的活跃用户
     */
    List<UserActivity> findByServerIdAndIsActiveTrueOrderByLastActivityDesc(Long serverId);
    
    /**
     * 查找指定服务器指定时间范围内的用户活动
     */
    List<UserActivity> findByServerIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long serverId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 查找指定用户的活动历史
     */
    List<UserActivity> findByUsernameOrderByCreatedAtDesc(String username);
    
    /**
     * 查找指定会话的活动记录
     */
    List<UserActivity> findBySessionIdOrderByCreatedAtDesc(String sessionId);
    
    /**
     * 统计指定服务器的活跃用户数
     */
    long countByServerIdAndIsActiveTrue(Long serverId);
    
    /**
     * 统计指定时间范围内的用户活动数
     */
    long countByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 统计指定服务器在指定时间范围内的用户活动数
     */
    long countByServerIdAndCreatedAtBetween(Long serverId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 查找超时的活跃会话
     */
    @Query("SELECT ua FROM UserActivity ua WHERE ua.isActive = true " +
           "AND ua.lastActivity < :timeoutThreshold")
    List<UserActivity> findTimeoutActiveSessions(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);
    
    /**
     * 查询指定服务器最近的用户活动
     */
    List<UserActivity> findTop20ByServerIdOrderByLastActivityDesc(Long serverId);
    
    /**
     * 统计指定服务器各活动类型的数量
     */
    @Query("SELECT ua.activityType, COUNT(ua) FROM UserActivity ua " +
           "WHERE ua.serverId = :serverId AND ua.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY ua.activityType")
    List<Object[]> countActivityTypesByServerAndTimeRange(@Param("serverId") Long serverId,
                                                          @Param("startTime") LocalDateTime startTime,
                                                          @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找指定用户在指定服务器上的当前活跃会话
     */
    List<UserActivity> findByServerIdAndUsernameAndIsActiveTrueOrderByLastActivityDesc(
            Long serverId, String username);
    
    /**
     * 删除指定时间之前的历史活动记录
     */
    void deleteByCreatedAtBefore(LocalDateTime cutoffTime);
}