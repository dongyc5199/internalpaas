package com.cmict.internalpaas.repository;

import com.cmict.internalpaas.model.AgentDeployment;
import com.cmict.internalpaas.model.DeploymentStatus;
import com.cmict.internalpaas.model.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Agent部署记录数据访问接口
 */
@Repository
public interface AgentDeploymentRepository extends JpaRepository<AgentDeployment, Long> {

    /**
     * 查找服务器的所有部署记录
     */
    List<AgentDeployment> findByServerOrderByCreatedAtDesc(Server server);

    /**
     * 查找服务器最新的部署记录
     */
    Optional<AgentDeployment> findFirstByServerOrderByCreatedAtDesc(Server server);

    /**
     * 查找指定状态的所有部署记录
     */
    List<AgentDeployment> findByStatus(DeploymentStatus status);

    /**
     * 查找服务器指定状态的部署记录
     */
    Optional<AgentDeployment> findFirstByServerAndStatusOrderByCreatedAtDesc(Server server, DeploymentStatus status);

    /**
     * 统计服务器的部署次数
     */
    long countByServer(Server server);

    /**
     * 统计指定状态的部署记录数
     */
    long countByStatus(DeploymentStatus status);

    /**
     * 查找指定状态且重试次数小于指定值的部署记录
     * 用于重试调度
     */
    List<AgentDeployment> findByStatusAndRetryCountLessThan(DeploymentStatus status, int retryCount);
}
