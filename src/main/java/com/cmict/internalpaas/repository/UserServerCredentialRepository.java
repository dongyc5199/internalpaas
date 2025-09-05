package com.cmict.internalpaas.repository;

import com.cmict.internalpaas.model.UserServerCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserServerCredentialRepository extends JpaRepository<UserServerCredential, Long> {
    
    /**
     * 根据用户ID和服务器ID查找凭据
     */
    Optional<UserServerCredential> findByUserIdAndServerId(Long userId, Long serverId);
    
    /**
     * 根据用户ID查找所有凭据
     */
    List<UserServerCredential> findByUserId(Long userId);
    
    /**
     * 根据服务器ID查找所有凭据
     */
    List<UserServerCredential> findByServerId(Long serverId);
    
    /**
     * 根据用户名和服务器ID查找凭据
     */
    @Query("SELECT c FROM UserServerCredential c WHERE c.user.username = :username AND c.server.id = :serverId")
    Optional<UserServerCredential> findByUsernameAndServerId(@Param("username") String username, @Param("serverId") Long serverId);
    
    /**
     * 根据SSH用户名和服务器ID查找凭据
     */
    Optional<UserServerCredential> findBySshUsernameAndServerId(String sshUsername, Long serverId);
    
    /**
     * 删除指定服务器的所有凭据
     */
    void deleteByServerId(Long serverId);
    
    /**
     * 删除指定用户的所有凭据
     */
    void deleteByUserId(Long userId);
    
    /**
     * 检查用户是否在指定服务器上有凭据
     */
    boolean existsByUserIdAndServerId(Long userId, Long serverId);
    
    /**
     * 统计指定服务器上的用户凭据数量
     */
    long countByServerId(Long serverId);
}