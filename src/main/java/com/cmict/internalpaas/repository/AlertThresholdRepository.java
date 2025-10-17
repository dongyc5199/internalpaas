package com.cmict.internalpaas.repository;

import com.cmict.internalpaas.model.AlertThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 告警阈值数据访问接口
 */
@Repository
public interface AlertThresholdRepository extends JpaRepository<AlertThreshold, Long> {
    
    /**
     * 根据服务器ID查找所有阈值配置
     */
    List<AlertThreshold> findByServerIdOrderByMetricType(Long serverId);
    
    /**
     * 根据服务器ID和指标类型查找阈值配置
     */
    Optional<AlertThreshold> findByServerIdAndMetricType(Long serverId, AlertThreshold.MetricType metricType);
    
    /**
     * 查找所有启用的阈值配置
     */
    List<AlertThreshold> findByEnabledTrueOrderByServerIdAscMetricTypeAsc();
    
    /**
     * 根据服务器ID查找所有启用的阈值配置
     */
    List<AlertThreshold> findByServerIdAndEnabledTrueOrderByMetricType(Long serverId);
    
    /**
     * 查找所有启用通知的阈值配置
     */
    List<AlertThreshold> findByEnabledTrueAndNotificationEnabledTrueOrderByServerIdAscMetricTypeAsc();
    
    /**
     * 根据指标类型查找所有阈值配置
     */
    List<AlertThreshold> findByMetricTypeOrderByServerIdAsc(AlertThreshold.MetricType metricType);
    
    /**
     * 查找指定服务器的默认阈值配置（全局服务器ID为null）
     */
    @Query("SELECT t FROM AlertThreshold t WHERE t.serverId IS NULL ORDER BY t.metricType")
    List<AlertThreshold> findDefaultThresholds();
    
    /**
     * 根据指标类型查找默认阈值配置
     */
    @Query("SELECT t FROM AlertThreshold t WHERE t.serverId IS NULL AND t.metricType = :metricType")
    Optional<AlertThreshold> findDefaultThresholdByMetricType(@Param("metricType") AlertThreshold.MetricType metricType);
    
    /**
     * 查找需要清理的旧阈值配置(超过指定天数未更新)
     */
    @Query("SELECT t FROM AlertThreshold t WHERE t.updatedAt < CURRENT_TIMESTAMP - :days DAY")
    List<AlertThreshold> findOldThresholds(@Param("days") int days);
    
    /**
     * 统计每个服务器的阈值配置数量
     */
    @Query("SELECT t.serverId, COUNT(t) FROM AlertThreshold t WHERE t.serverId IS NOT NULL GROUP BY t.serverId")
    List<Object[]> countThresholdsByServer();
    
    /**
     * 查找CPU使用率阈值超过指定值的配置
     */
    @Query("SELECT t FROM AlertThreshold t WHERE t.metricType = 'CPU' AND " +
           "(t.warningThreshold > :threshold OR t.criticalThreshold > :threshold)")
    List<AlertThreshold> findCpuThresholdsAbove(@Param("threshold") Double threshold);
    
    /**
     * 查找内存使用率阈值超过指定值的配置
     */
    @Query("SELECT t FROM AlertThreshold t WHERE t.metricType = 'MEMORY' AND " +
           "(t.warningThreshold > :threshold OR t.criticalThreshold > :threshold)")
    List<AlertThreshold> findMemoryThresholdsAbove(@Param("threshold") Double threshold);
    
    /**
     * 查找磁盘使用率阈值超过指定值的配置
     */
    @Query("SELECT t FROM AlertThreshold t WHERE t.metricType = 'DISK' AND " +
           "(t.warningThreshold > :threshold OR t.criticalThreshold > :threshold)")
    List<AlertThreshold> findDiskThresholdsAbove(@Param("threshold") Double threshold);
    
    /**
     * 删除指定服务器的所有阈值配置
     */
    void deleteByServerId(Long serverId);
    
    /**
     * 删除指定服务器和指标类型的阈值配置
     */
    void deleteByServerIdAndMetricType(Long serverId, AlertThreshold.MetricType metricType);
    
    /**
     * 检查服务器是否存在指定指标类型的阈值配置
     */
    boolean existsByServerIdAndMetricType(Long serverId, AlertThreshold.MetricType metricType);
    
    /**
     * 统计启用的阈值配置数量
     */
    long countByEnabledTrue();
    
    /**
     * 统计启用通知的阈值配置数量
     */
    long countByEnabledTrueAndNotificationEnabledTrue();
}