package com.cmict.internalpaas.repository;

import com.cmict.internalpaas.model.GroupFilePermission;
import com.cmict.internalpaas.model.ServerUserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户组文件权限数据访问接口
 */
@Repository
public interface GroupFilePermissionRepository extends JpaRepository<GroupFilePermission, Long> {
    
    /**
     * 根据用户组查找文件权限
     */
    List<GroupFilePermission> findByServerUserGroup(ServerUserGroup serverUserGroup);
    
    /**
     * 根据用户组ID查找文件权限
     */
    List<GroupFilePermission> findByServerUserGroupId(Long groupId);
    
    /**
     * 根据用户组和路径查找权限
     */
    Optional<GroupFilePermission> findByServerUserGroupAndPath(ServerUserGroup serverUserGroup, String path);
    
    /**
     * 根据用户组ID和路径查找权限
     */
    Optional<GroupFilePermission> findByServerUserGroupIdAndPath(Long groupId, String path);
    
    /**
     * 根据路径模糊查找权限
     */
    @Query("SELECT gfp FROM GroupFilePermission gfp WHERE gfp.path LIKE CONCAT('%', :pathPattern, '%')")
    List<GroupFilePermission> findByPathContaining(@Param("pathPattern") String pathPattern);
    
    /**
     * 根据权限字符串查找
     */
    List<GroupFilePermission> findByPermissions(String permissions);
    
    /**
     * 查找递归权限
     */
    List<GroupFilePermission> findByRecursiveTrue();
    
    /**
     * 根据用户组查找递归权限
     */
    List<GroupFilePermission> findByServerUserGroupAndRecursiveTrue(ServerUserGroup serverUserGroup);
    
    /**
     * 检查用户组是否有特定路径的权限
     */
    boolean existsByServerUserGroupAndPath(ServerUserGroup serverUserGroup, String path);
    
    /**
     * 检查用户组是否有特定路径的权限（按ID）
     */
    boolean existsByServerUserGroupIdAndPath(Long groupId, String path);
    
    /**
     * 统计用户组的文件权限数量
     */
    long countByServerUserGroup(ServerUserGroup serverUserGroup);
    
    /**
     * 统计用户组的文件权限数量（按ID）
     */
    long countByServerUserGroupId(Long groupId);
    
    /**
     * 删除用户组的所有文件权限
     */
    void deleteByServerUserGroup(ServerUserGroup serverUserGroup);
    
    /**
     * 删除用户组的所有文件权限（按ID）
     */
    void deleteByServerUserGroupId(Long groupId);
}