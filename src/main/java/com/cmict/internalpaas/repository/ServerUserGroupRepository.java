package com.cmict.internalpaas.repository;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerUserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 服务器用户组数据访问接口
 */
@Repository
public interface ServerUserGroupRepository extends JpaRepository<ServerUserGroup, Long> {
    
    /**
     * 根据服务器查找用户组
     */
    List<ServerUserGroup> findByServer(Server server);
    
    /**
     * 根据服务器ID查找用户组
     */
    List<ServerUserGroup> findByServerId(Long serverId);
    
    /**
     * 根据服务器和用户组名称查找
     */
    Optional<ServerUserGroup> findByServerAndGroupName(Server server, String groupName);
    
    /**
     * 根据服务器ID和用户组名称查找
     */
    Optional<ServerUserGroup> findByServerIdAndGroupName(Long serverId, String groupName);
    
    /**
     * 查找服务器的默认用户组
     */
    Optional<ServerUserGroup> findByServerAndIsDefaultTrue(Server server);
    
    /**
     * 根据服务器ID查找默认用户组
     */
    Optional<ServerUserGroup> findByServerIdAndIsDefaultTrue(Long serverId);
    
    /**
     * 根据权限级别查找用户组
     */
    List<ServerUserGroup> findByPermissionLevel(ServerUserGroup.PermissionLevel permissionLevel);
    
    /**
     * 根据服务器和权限级别查找用户组
     */
    List<ServerUserGroup> findByServerAndPermissionLevel(Server server, ServerUserGroup.PermissionLevel permissionLevel);
    
    /**
     * 检查服务器上是否存在指定名称的用户组
     */
    boolean existsByServerAndGroupName(Server server, String groupName);
    
    /**
     * 检查服务器上是否存在指定名称的用户组（按ID）
     */
    boolean existsByServerIdAndGroupName(Long serverId, String groupName);
    
    /**
     * 统计服务器的用户组数量
     */
    long countByServer(Server server);
    
    /**
     * 统计服务器的用户组数量（按ID）
     */
    long countByServerId(Long serverId);
    
    /**
     * 查找所有默认用户组
     */
    @Query("SELECT sug FROM ServerUserGroup sug WHERE sug.isDefault = true")
    List<ServerUserGroup> findAllDefaultGroups();
    
    /**
     * 查找指定服务器类型的所有用户组
     */
    @Query("SELECT sug FROM ServerUserGroup sug WHERE sug.server.serverType = :serverType")
    List<ServerUserGroup> findByServerType(@Param("serverType") Server.ServerType serverType);
    
    /**
     * 查找包含特定系统组的用户组
     */
    @Query("SELECT sug FROM ServerUserGroup sug WHERE sug.systemGroups LIKE CONCAT('%', :systemGroup, '%')")
    List<ServerUserGroup> findBySystemGroupsContaining(@Param("systemGroup") String systemGroup);
    
    /**
     * 删除服务器的所有用户组
     */
    void deleteByServer(Server server);
    
    /**
     * 删除服务器的所有用户组（按ID）
     */
    void deleteByServerId(Long serverId);
    
    /**
     * 根据服务器ID查找用户组按创建时间倒序
     */
    List<ServerUserGroup> findByServerIdOrderByCreatedAtDesc(Long serverId);
    
    /**
     * 根据服务器ID查找活跃用户组
     */
    @Query("SELECT sug FROM ServerUserGroup sug WHERE sug.server.id = :serverId")
    List<ServerUserGroup> findByServerIdAndIsActiveTrue(@Param("serverId") Long serverId);
    
    /**
     * 根据服务器ID和权限级别查找用户组
     */
    @Query("SELECT sug FROM ServerUserGroup sug WHERE sug.server.id = :serverId AND sug.permissionLevel = :permissionLevel")
    List<ServerUserGroup> findByServerIdAndPermissionLevel(@Param("serverId") Long serverId, 
                                                          @Param("permissionLevel") ServerUserGroup.PermissionLevel permissionLevel);
}