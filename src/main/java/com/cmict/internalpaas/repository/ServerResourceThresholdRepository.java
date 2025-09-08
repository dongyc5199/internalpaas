package com.cmict.internalpaas.repository;

import com.cmict.internalpaas.model.ServerResourceThreshold;
import com.cmict.internalpaas.model.ServerResourceThreshold.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServerResourceThresholdRepository extends JpaRepository<ServerResourceThreshold, Long> {
    
    /**
     * 根据服务器ID查询所有资源阈值配置
     */
    List<ServerResourceThreshold> findByServerId(Long serverId);
    
    /**
     * 根据服务器ID查询启用的资源阈值配置
     */
    List<ServerResourceThreshold> findByServerIdAndEnabled(Long serverId, Boolean enabled);
    
    /**
     * 根据服务器ID和资源类型查询阈值配置
     */
    Optional<ServerResourceThreshold> findByServerIdAndResourceType(Long serverId, ResourceType resourceType);
    
    /**
     * 查询所有启用内存监控的服务器阈值配置
     */
    @Query("SELECT t FROM ServerResourceThreshold t WHERE t.resourceType = 'MEMORY' " +
           "AND t.enabled = true ORDER BY t.serverId")
    List<ServerResourceThreshold> findAllEnabledMemoryThresholds();
    
    /**
     * 查询所有启用通知的阈值配置
     */
    @Query("SELECT t FROM ServerResourceThreshold t WHERE t.enabled = true " +
           "AND t.notifyEnabled = true ORDER BY t.serverId, t.resourceType")
    List<ServerResourceThreshold> findAllEnabledWithNotification();
    
    /**
     * 根据资源类型查询所有启用的阈值配置
     */
    List<ServerResourceThreshold> findByResourceTypeAndEnabled(ResourceType resourceType, Boolean enabled);
    
    /**
     * 查询需要立即检查的阈值配置（根据检查间隔）
     */
    @Query("SELECT t FROM ServerResourceThreshold t WHERE t.enabled = true " +
           "AND t.checkIntervalSeconds <= :maxInterval ORDER BY t.checkIntervalSeconds")
    List<ServerResourceThreshold> findThresholdsForImmediateCheck(@Param("maxInterval") Integer maxInterval);
    
    /**
     * 删除服务器的所有阈值配置
     */
    void deleteByServerId(Long serverId);
    
    /**
     * 删除服务器的指定资源类型阈值配置
     */
    void deleteByServerIdAndResourceType(Long serverId, ResourceType resourceType);
    
    /**
     * 查询有效阈值配置的统计信息
     */
    @Query("SELECT t.resourceType, COUNT(t), AVG(t.criticalThreshold) " +
           "FROM ServerResourceThreshold t WHERE t.enabled = true " +
           "GROUP BY t.resourceType")
    List<Object[]> getThresholdStatistics();
    
    /**
     * 查询特定严重阈值范围内的配置
     */
    @Query("SELECT t FROM ServerResourceThreshold t WHERE t.resourceType = :resourceType " +
           "AND t.criticalThreshold >= :minThreshold AND t.criticalThreshold <= :maxThreshold " +
           "AND t.enabled = true")
    List<ServerResourceThreshold> findByResourceTypeAndCriticalThresholdRange(
        @Param("resourceType") ResourceType resourceType,
        @Param("minThreshold") Double minThreshold,
        @Param("maxThreshold") Double maxThreshold);
    
    /**
     * 检查服务器是否已存在指定资源类型的阈值配置
     */
    boolean existsByServerIdAndResourceType(Long serverId, ResourceType resourceType);
    
    /**
     * 查询所有服务器的内存阈值配置，用于80%告警检查
     */
    @Query("SELECT t FROM ServerResourceThreshold t WHERE t.resourceType = 'MEMORY' " +
           "AND t.enabled = true AND t.criticalThreshold IS NOT NULL " +
           "ORDER BY t.serverId")
    List<ServerResourceThreshold> findMemoryThresholdsForAlertCheck();
    
    /**
     * 批量查询多个服务器的阈值配置
     */
    @Query("SELECT t FROM ServerResourceThreshold t WHERE t.serverId IN :serverIds " +
           "AND t.enabled = true ORDER BY t.serverId, t.resourceType")
    List<ServerResourceThreshold> findByServerIdsAndEnabled(@Param("serverIds") List<Long> serverIds);
}