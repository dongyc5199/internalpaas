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
    
    /**
     * 统计指定用户的总会话数
     */
    int countByUsername(String username);
    
    /**
     * 统计指定用户的总命令执行数
     */
    @Query("SELECT COALESCE(SUM(ua.commandCount), 0) FROM UserActivity ua WHERE ua.username = :username")
    Integer sumCommandCountByUsername(@Param("username") String username);
    
    /**
     * 查找指定用户的最后活动时间
     */
    @Query("SELECT MAX(ua.lastActivity) FROM UserActivity ua WHERE ua.username = :username")
    LocalDateTime findLastActivityByUsername(@Param("username") String username);
    
    /**
     * 删除指定用户的所有活动记录
     */
    void deleteByUsername(String username);
    
    /**
     * 根据ID获取用户活动详情
     */
    @Query("SELECT ua FROM UserActivity ua WHERE ua.id = :id")
    UserActivity findByIdWithDetails(@Param("id") Long id);
    
    /**
     * 复杂条件查询用户活动
     */
    @Query("SELECT ua FROM UserActivity ua WHERE " +
           "(:serverId IS NULL OR ua.serverId = :serverId) AND " +
           "(:username IS NULL OR ua.username LIKE %:username%) AND " +
           "(:sessionId IS NULL OR ua.sessionId = :sessionId) AND " +
           "(:activityType IS NULL OR ua.activityType = :activityType) AND " +
           "(:isActive IS NULL OR ua.isActive = :isActive) AND " +
           "(:remoteIp IS NULL OR ua.remoteIp = :remoteIp) AND " +
           "(:startTime IS NULL OR ua.createdAt >= :startTime) AND " +
           "(:endTime IS NULL OR ua.createdAt <= :endTime) AND " +
           "(:terminalType IS NULL OR ua.terminalType = :terminalType) AND " +
           "(:keyword IS NULL OR ua.activityDetails LIKE %:keyword%)")
    org.springframework.data.domain.Page<UserActivity> findByComplexConditions(
            @Param("serverId") Long serverId,
            @Param("username") String username,
            @Param("sessionId") String sessionId,
            @Param("activityType") UserActivity.ActivityType activityType,
            @Param("isActive") Boolean isActive,
            @Param("remoteIp") String remoteIp,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("terminalType") String terminalType,
            @Param("keyword") String keyword,
            org.springframework.data.domain.Pageable pageable);
    
    /**
     * 统计总的活动数量
     */
    @Query("SELECT COUNT(ua) FROM UserActivity ua")
    long countAllActivities();
    
    /**
     * 统计所有服务器的活跃用户数
     */
    @Query("SELECT COUNT(DISTINCT ua.username) FROM UserActivity ua WHERE ua.isActive = true")
    long countAllActiveUsers();
    
    /**
     * 获取用户活动的服务器分布统计
     */
    @Query("SELECT ua.serverId, COUNT(ua) FROM UserActivity ua " +
           "WHERE ua.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY ua.serverId")
    List<Object[]> countActivitiesByServer(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);
    
    /**
     * 获取用户活动的时间分布统计（按小时）
     */
    @Query("SELECT HOUR(ua.createdAt), COUNT(ua) FROM UserActivity ua " +
           "WHERE ua.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY HOUR(ua.createdAt) " +
           "ORDER BY HOUR(ua.createdAt)")
    List<Object[]> countActivitiesByHour(@Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);
}