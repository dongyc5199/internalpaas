package com.cmict.internalpaas.repository;

import com.cmict.internalpaas.model.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 服务器数据访问层
 * Server Repository
 *
 * @author GitHub Copilot
 * @since 2025-10-18
 */
@Repository
public interface ServerRepository extends JpaRepository<Server, Long> {

    // ==================== 现有方法 ====================

    /**
     * 查询所有活跃服务器
     *
     * @return 活跃服务器列表
     */
    List<Server> findByActiveTrue();

    /**
     * 查询所有活跃服务器并按名称排序
     *
     * @return 活跃服务器列表（按名称排序）
     */
    List<Server> findByActiveTrueOrderByName();

    /**
     * 根据连接状态查询服务器
     *
     * @param connectionStatuses 连接状态列表
     * @return 服务器列表
     */
    List<Server> findByConnectionStatusIn(List<Server.ConnectionStatus> connectionStatuses);

    // ==================== SSH配置导入相关方法 ====================

    /**
     * 检查是否存在相同主机名和SSH端口的服务器（用于去重）
     * Check if a server with the same hostname and SSH port exists
     *
     * 用于SSH配置导入时的去重检查
     *
     * @param hostname 主机名或IP地址
     * @param sshPort SSH端口
     * @return true表示存在重复
     */
    boolean existsByHostnameAndSshPort(String hostname, Integer sshPort);

    /**
     * 根据主机名和SSH端口查找服务器（用于去重）
     * Find a server by hostname and SSH port
     *
     * 用于SSH配置导入时查找重复的服务器
     *
     * @param hostname 主机名或IP地址
     * @param sshPort SSH端口
     * @return 服务器对象（如果存在）
     */
    Optional<Server> findByHostnameAndSshPort(String hostname, Integer sshPort);

    /**
     * 查询所有服务器的主机名列表
     * Get all server hostnames
     *
     * 用于批量去重检查或显示已有服务器列表
     *
     * @return 主机名列表
     */
    @Query("SELECT s.hostname FROM Server s")
    List<String> findAllHostnames();

    /**
     * 根据名称查找服务器
     * Find a server by name
     *
     * 用于检查服务器名称是否重复
     *
     * @param name 服务器名称
     * @return 服务器对象（如果存在）
     */
    Optional<Server> findByName(String name);

    /**
     * 检查服务器名称是否存在
     * Check if a server name exists
     *
     * @param name 服务器名称
     * @return true表示名称已存在
     */
    boolean existsByName(String name);
}