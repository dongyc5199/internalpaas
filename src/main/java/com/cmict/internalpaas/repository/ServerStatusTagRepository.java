package com.cmict.internalpaas.repository;

import com.cmict.internalpaas.model.ServerStatusTag;
import com.cmict.internalpaas.model.ServerStatusTag.TagType;
import com.cmict.internalpaas.model.ServerStatusTag.TagStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServerStatusTagRepository extends JpaRepository<ServerStatusTag, Long> {
    
    /**
     * 根据服务器ID查询所有状态标签，按优先级降序排列
     */
    List<ServerStatusTag> findByServerIdOrderByPriorityDesc(Long serverId);
    
    /**
     * 根据服务器ID和标签类型查询状态标签
     */
    Optional<ServerStatusTag> findByServerIdAndTagType(Long serverId, TagType tagType);
    
    /**
     * 根据服务器ID查询指定状态的标签
     */
    List<ServerStatusTag> findByServerIdAndStatus(Long serverId, TagStatus status);
    
    /**
     * 查询服务器的告警级别标签（WARNING, CRITICAL, DANGER, ERROR）
     */
    @Query("SELECT t FROM ServerStatusTag t WHERE t.serverId = :serverId " +
           "AND t.status IN ('WARNING', 'CRITICAL', 'DANGER', 'ERROR') " +
           "ORDER BY t.priority DESC")
    List<ServerStatusTag> findAlertTagsByServerId(@Param("serverId") Long serverId);
    
    /**
     * 查询所有服务器的内存告警标签
     */
    @Query("SELECT t FROM ServerStatusTag t WHERE t.tagType = 'MEMORY_USAGE' " +
           "AND t.status IN ('WARNING', 'CRITICAL', 'DANGER') " +
           "ORDER BY t.serverId, t.priority DESC")
    List<ServerStatusTag> findAllMemoryAlertTags();
    
    /**
     * 查询指定时间之前的过期标签
     */
    List<ServerStatusTag> findByLastUpdatedBefore(LocalDateTime expiredTime);
    
    /**
     * 根据服务器ID和优先级查询高优先级标签
     */
    @Query("SELECT t FROM ServerStatusTag t WHERE t.serverId = :serverId " +
           "AND t.priority >= :minPriority ORDER BY t.priority DESC")
    List<ServerStatusTag> findHighPriorityTagsByServerId(@Param("serverId") Long serverId, 
                                                        @Param("minPriority") Integer minPriority);
    
    /**
     * 根据服务器ID和标签类型删除标签
     */
    void deleteByServerIdAndTagType(Long serverId, TagType tagType);
    
    /**
     * 删除指定标签类型的所有标签
     */
    void deleteByTagType(TagType tagType);
    
    /**
     * 删除指定服务器的所有标签
     */
    void deleteByServerId(Long serverId);
    
    /**
     * 统计服务器的告警数量
     */
    @Query("SELECT COUNT(t) FROM ServerStatusTag t WHERE t.serverId = :serverId " +
           "AND t.status IN ('WARNING', 'CRITICAL', 'DANGER', 'ERROR')")
    Long countAlertTagsByServerId(@Param("serverId") Long serverId);
    
    /**
     * 查询资源相关的标签
     */
    @Query("SELECT t FROM ServerStatusTag t WHERE t.serverId = :serverId " +
           "AND t.tagType IN ('MEMORY_USAGE', 'CPU_USAGE', 'DISK_USAGE', 'SYSTEM_LOAD') " +
           "ORDER BY t.priority DESC")
    List<ServerStatusTag> findResourceTagsByServerId(@Param("serverId") Long serverId);
    
    /**
     * 批量查询多个服务器的高优先级标签
     */
    @Query("SELECT t FROM ServerStatusTag t WHERE t.serverId IN :serverIds " +
           "AND t.priority >= :minPriority ORDER BY t.serverId, t.priority DESC")
    List<ServerStatusTag> findHighPriorityTagsByServerIds(@Param("serverIds") List<Long> serverIds,
                                                         @Param("minPriority") Integer minPriority);
    
    /**
     * 查询所有活跃服务器的状态标签数量统计
     */
    @Query("SELECT t.serverId, COUNT(t) FROM ServerStatusTag t " +
           "WHERE t.serverId IN :serverIds GROUP BY t.serverId")
    List<Object[]> countTagsByServerIds(@Param("serverIds") List<Long> serverIds);
}