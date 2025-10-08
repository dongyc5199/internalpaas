package com.cmict.internalpaas.repository;

import com.cmict.internalpaas.model.Application;
import com.cmict.internalpaas.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByUserOrderByCreatedAtDesc(User user);
    
    List<Application> findByUserAndStatus(User user, String status);
    
    List<Application> findByUser(User user);
    
    Optional<Application> findByIdAndUser(Long id, User user);
    
    boolean existsByUserAndPort(User user, Integer port);
    
    boolean existsByUserAndDebugPort(User user, Integer debugPort);
    
    /**
     * 优化的用户应用查询 - 解决N+1查询问题
     * 使用JOIN FETCH预加载User关联，避免懒加载导致的额外查询
     */
    @Query("SELECT a FROM Application a JOIN FETCH a.user WHERE a.user = :user ORDER BY a.createdAt DESC")
    List<Application> findByUserWithUserOrderByCreatedAtDesc(@Param("user") User user);
    
    /**
     * 优化的用户应用查询（包含配置信息） - 解决N+1查询问题
     * 使用LEFT JOIN FETCH预加载User和ActiveConfiguration关联
     */
    @Query("SELECT DISTINCT a FROM Application a " +
           "JOIN FETCH a.user " +
           "LEFT JOIN FETCH a.activeConfiguration " +
           "WHERE a.user = :user " +
           "ORDER BY a.createdAt DESC")
    List<Application> findByUserWithConfigsOrderByCreatedAtDesc(@Param("user") User user);
    
    /**
     * 优化的单个应用查询（包含所有配置）
     * 预加载用户信息、活动配置和所有配置历史
     */
    @Query("SELECT a FROM Application a " +
           "JOIN FETCH a.user " +
           "LEFT JOIN FETCH a.activeConfiguration " +
           "LEFT JOIN FETCH a.configurations " +
           "WHERE a.id = :id")
    Optional<Application> findByIdWithAllRelations(@Param("id") Long id);
    
    /**
     * 优化的按状态查询
     * 预加载User关联避免N+1问题
     */
    @Query("SELECT a FROM Application a JOIN FETCH a.user WHERE a.user = :user AND a.status = :status ORDER BY a.createdAt DESC")
    List<Application> findByUserAndStatusWithUser(@Param("user") User user, @Param("status") String status);
    
    /**
     * 批量查询优化 - 获取多个用户的应用
     * 适用于管理员界面等场景
     */
    @Query("SELECT a FROM Application a JOIN FETCH a.user WHERE a.user IN :users ORDER BY a.createdAt DESC")
    List<Application> findByUsersWithUser(@Param("users") List<User> users);

    /**
     * Count applications that belong to users bound to a given server
     */
    @Query("SELECT COUNT(DISTINCT a.id) FROM Application a " +
           "JOIN a.user u " +
           "LEFT JOIN u.availableServers s " +
           "WHERE s.id = :serverId OR u.defaultServer.id = :serverId")
    long countApplicationsBoundToServer(@Param("serverId") Long serverId);
    
    /**
     * 统计指定用户的应用总数
     */
    int countByUserId(Long userId);
    
    /**
     * 统计指定用户指定状态的应用数量
     */
    int countByUserIdAndStatus(Long userId, String status);
    
    /**
     * 删除指定用户的所有应用
     */
    void deleteByUserId(Long userId);
}
