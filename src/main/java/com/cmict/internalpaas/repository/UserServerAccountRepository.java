package com.cmict.internalpaas.repository;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.model.UserServerAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户服务器账户数据访问接口
 */
@Repository
public interface UserServerAccountRepository extends JpaRepository<UserServerAccount, Long> {
    
    /**
     * 根据用户查找所有服务器账户
     */
    List<UserServerAccount> findByUser(User user);
    
    /**
     * 根据用户ID查找所有服务器账户
     */
    List<UserServerAccount> findByUserId(Long userId);
    
    /**
     * 根据服务器查找所有用户账户
     */
    List<UserServerAccount> findByServer(Server server);
    
    /**
     * 根据服务器ID查找所有用户账户
     */
    List<UserServerAccount> findByServerId(Long serverId);
    
    /**
     * 根据用户和服务器查找账户
     */
    Optional<UserServerAccount> findByUserAndServer(User user, Server server);
    
    /**
     * 根据用户ID和服务器ID查找账户
     */
    Optional<UserServerAccount> findByUserIdAndServerId(Long userId, Long serverId);
    
    /**
     * 根据账户状态查找账户
     */
    List<UserServerAccount> findByAccountStatus(UserServerAccount.AccountStatus accountStatus);
    
    /**
     * 根据用户和账户状态查找账户
     */
    List<UserServerAccount> findByUserAndAccountStatus(User user, UserServerAccount.AccountStatus accountStatus);
    
    /**
     * 根据服务器和账户状态查找账户
     */
    List<UserServerAccount> findByServerAndAccountStatus(Server server, UserServerAccount.AccountStatus accountStatus);
    
    /**
     * 查找需要检查的账户（未检测状态）
     */
    @Query("SELECT usa FROM UserServerAccount usa WHERE usa.accountStatus = 'NOT_CHECKED'")
    List<UserServerAccount> findAccountsNeedingCheck();
    
    /**
     * 查找同步失败的账户
     */
    @Query("SELECT usa FROM UserServerAccount usa WHERE usa.accountStatus IN ('CREATE_FAILED', 'SYNC_ERROR')")
    List<UserServerAccount> findFailedAccounts();
    
    /**
     * 查找最近同步失败的账户
     */
    @Query("SELECT usa FROM UserServerAccount usa WHERE usa.accountStatus IN ('CREATE_FAILED', 'SYNC_ERROR') AND usa.lastSyncTime >= :since")
    List<UserServerAccount> findRecentlyFailedAccounts(@Param("since") LocalDateTime since);
    
    /**
     * 查找长时间未同步的账户
     */
    @Query("SELECT usa FROM UserServerAccount usa WHERE usa.lastSyncTime IS NULL OR usa.lastSyncTime < :before")
    List<UserServerAccount> findStaleAccounts(@Param("before") LocalDateTime before);
    
    /**
     * 统计用户的服务器账户数量
     */
    long countByUser(User user);
    
    /**
     * 统计用户的服务器账户数量（按ID）
     */
    long countByUserId(Long userId);
    
    /**
     * 统计服务器的用户账户数量
     */
    long countByServer(Server server);
    
    /**
     * 统计服务器的用户账户数量（按ID）
     */
    long countByServerId(Long serverId);
    
    /**
     * 统计特定状态的账户数量
     */
    long countByAccountStatus(UserServerAccount.AccountStatus accountStatus);
    
    /**
     * 统计服务器上特定状态的账户数量
     */
    long countByServerAndAccountStatus(Server server, UserServerAccount.AccountStatus accountStatus);
    
    /**
     * 检查用户在服务器上是否有账户
     */
    boolean existsByUserAndServer(User user, Server server);
    
    /**
     * 检查用户在服务器上是否有账户（按ID）
     */
    boolean existsByUserIdAndServerId(Long userId, Long serverId);
    
    /**
     * 查找用户在特定服务器类型上的账户
     */
    @Query("SELECT usa FROM UserServerAccount usa WHERE usa.user = :user AND usa.server.serverType = :serverType")
    List<UserServerAccount> findByUserAndServerType(@Param("user") User user, @Param("serverType") Server.ServerType serverType);
    
    /**
     * 查找指定时间范围内创建的账户
     */
    @Query("SELECT usa FROM UserServerAccount usa WHERE usa.createdAt BETWEEN :startTime AND :endTime")
    List<UserServerAccount> findAccountsCreatedBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找指定时间范围内同步的账户
     */
    @Query("SELECT usa FROM UserServerAccount usa WHERE usa.lastSyncTime BETWEEN :startTime AND :endTime")
    List<UserServerAccount> findAccountsSyncedBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 删除用户的所有服务器账户
     */
    void deleteByUser(User user);
    
    /**
     * 删除用户的所有服务器账户（按ID）
     */
    void deleteByUserId(Long userId);
    
    /**
     * 删除服务器的所有用户账户
     */
    void deleteByServer(Server server);
    
    /**
     * 删除服务器的所有用户账户（按ID）
     */
    void deleteByServerId(Long serverId);
}