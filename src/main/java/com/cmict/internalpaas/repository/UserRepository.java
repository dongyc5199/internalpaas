package com.cmict.internalpaas.repository;

import com.cmict.internalpaas.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    
    long countByRolesContaining(User.Role role);
    
    /**
     * 查找将指定服务器设为默认服务器的用户
     */
    @Query("SELECT u FROM User u WHERE u.defaultServer.id = :serverId")
    List<User> findByDefaultServerId(@Param("serverId") Long serverId);

    /**
     * Count users associated with a server via default or accessible bindings
     */
    @Query("SELECT COUNT(DISTINCT u.id) FROM User u LEFT JOIN u.availableServers s " +
           "WHERE s.id = :serverId OR u.defaultServer.id = :serverId")
    long countUsersBoundToServer(@Param("serverId") Long serverId);

    /**
     * 查找有权限访问指定服务器的所有用户
     */
    @Query("SELECT u FROM User u JOIN u.availableServers s WHERE s.id = :serverId")
    List<User> findByAvailableServersContaining(@Param("serverId") Long serverId);
    
    /**
     * 从用户可用服务器列表中移除指定服务器
     */
    @Query(value = "DELETE FROM user_servers WHERE server_id = :serverId", nativeQuery = true)
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void removeServerFromUsers(@Param("serverId") Long serverId);
}