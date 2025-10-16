package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.AgentDeployment;
import com.cmict.internalpaas.model.DeploymentStatus;
import com.cmict.internalpaas.repository.AgentDeploymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Agent部署重试调度服务
 * 负责自动重试失败的部署，使用指数退避策略
 *
 * 重试策略：
 * - 第1次重试：失败后1分钟
 * - 第2次重试：失败后5分钟
 * - 第3次重试：失败后15分钟
 * - 超过最大重试次数后不再重试
 *
 * @author Dev Debug Platform Team
 * @version 2.0 (阶段3 - 重试机制)
 */
@Service
public class DeploymentRetryService {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentRetryService.class);

    @Autowired
    private AgentDeploymentRepository deploymentRepository;

    @Autowired
    private AgentDeployService agentDeployService;

    @Value("${agent.deploy.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${agent.deploy.retry.delay-minutes:1,5,15}")
    private String retryDelayMinutesConfig;

    private List<Integer> retryDelayMinutes;

    /**
     * 每分钟检查一次是否有需要重试的部署
     * fixedDelay: 上一次执行结束后等待60秒再执行
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    @Transactional
    public void checkAndRetryFailedDeployments() {
        try {
            // 解析重试延迟配置
            if (retryDelayMinutes == null) {
                parseRetryDelayConfig();
            }

            // 查找所有失败且未超过最大重试次数的部署
            List<AgentDeployment> failedDeployments = deploymentRepository
                    .findByStatusAndRetryCountLessThan(DeploymentStatus.FAILED, maxRetryAttempts);

            if (failedDeployments.isEmpty()) {
                return;
            }

            logger.debug("发现 {} 个失败的部署记录，检查是否需要重试", failedDeployments.size());

            for (AgentDeployment deployment : failedDeployments) {
                processRetry(deployment);
            }

        } catch (Exception e) {
            logger.error("重试调度器执行异常", e);
        }
    }

    /**
     * 处理单个部署的重试逻辑
     *
     * @param deployment 部署记录
     */
    private void processRetry(AgentDeployment deployment) {
        int currentRetryCount = deployment.getRetryCount() != null ? deployment.getRetryCount() : 0;

        // 已达到最大重试次数
        if (currentRetryCount >= maxRetryAttempts) {
            logger.debug("部署 {} 已达到最大重试次数 {}, 不再重试",
                    deployment.getId(), maxRetryAttempts);
            return;
        }

        // 检查是否到了重试时间
        if (!shouldRetryNow(deployment, currentRetryCount)) {
            return;
        }

        // 开始重试
        logger.info("🔄 开始重试部署 - deploymentId: {}, serverId: {}, 重试次数: {}/{}",
                deployment.getId(),
                deployment.getServer().getId(),
                currentRetryCount + 1,
                maxRetryAttempts);

        try {
            // 更新状态为重试中
            deployment.setStatus(DeploymentStatus.RETRYING);
            deployment.incrementRetryCount();
            deployment.appendLog(String.format("[重试] 第%d次重试开始 (最大%d次)",
                    deployment.getRetryCount(), maxRetryAttempts));
            deploymentRepository.save(deployment);

            // 触发新的部署
            agentDeployService.retryDeployment(deployment);

        } catch (Exception e) {
            logger.error("重试部署异常 - deploymentId: {}", deployment.getId(), e);
            deployment.setStatus(DeploymentStatus.FAILED);
            deployment.setErrorMessage("重试异常: " + e.getMessage());
            deployment.appendLog("[重试] 异常: " + e.getMessage());
            deploymentRepository.save(deployment);
        }
    }

    /**
     * 判断是否应该立即重试
     *
     * @param deployment 部署记录
     * @param currentRetryCount 当前重试次数
     * @return true 如果应该重试
     */
    private boolean shouldRetryNow(AgentDeployment deployment, int currentRetryCount) {
        if (deployment.getUpdatedAt() == null) {
            return true; // 如果没有更新时间，立即重试
        }

        // 获取对应重试次数的延迟时间
        int delayMinutes = getRetryDelayForAttempt(currentRetryCount);

        // 计算距离上次更新的时间
        LocalDateTime lastUpdate = deployment.getUpdatedAt();
        long minutesSinceLastUpdate = ChronoUnit.MINUTES.between(lastUpdate, LocalDateTime.now());

        // 如果距离上次更新超过了延迟时间，则可以重试
        boolean shouldRetry = minutesSinceLastUpdate >= delayMinutes;

        if (!shouldRetry) {
            logger.trace("部署 {} 尚未到重试时间 (需要等待{}分钟, 已等待{}分钟)",
                    deployment.getId(), delayMinutes, minutesSinceLastUpdate);
        }

        return shouldRetry;
    }

    /**
     * 获取指定重试次数对应的延迟时间（分钟）
     *
     * @param retryCount 重试次数（从0开始）
     * @return 延迟分钟数
     */
    private int getRetryDelayForAttempt(int retryCount) {
        if (retryDelayMinutes == null || retryDelayMinutes.isEmpty()) {
            return 1; // 默认1分钟
        }

        // 如果重试次数超过配置的延迟数量，使用最后一个延迟值
        if (retryCount >= retryDelayMinutes.size()) {
            return retryDelayMinutes.get(retryDelayMinutes.size() - 1);
        }

        return retryDelayMinutes.get(retryCount);
    }

    /**
     * 解析重试延迟配置
     * 格式: "1,5,15" -> [1, 5, 15]
     */
    private void parseRetryDelayConfig() {
        try {
            retryDelayMinutes = Arrays.stream(retryDelayMinutesConfig.split(","))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());

            logger.info("重试延迟配置已加载: {} 分钟", retryDelayMinutes);
        } catch (Exception e) {
            logger.error("解析重试延迟配置失败，使用默认值: {}", retryDelayMinutesConfig, e);
            retryDelayMinutes = Arrays.asList(1, 5, 15);
        }
    }

    /**
     * 手动触发重试（用于管理界面）
     *
     * @param deploymentId 部署ID
     * @return 是否成功触发重试
     */
    @Transactional
    public boolean manualRetry(Long deploymentId) {
        AgentDeployment deployment = deploymentRepository.findById(deploymentId).orElse(null);

        if (deployment == null) {
            logger.warn("部署记录不存在: {}", deploymentId);
            return false;
        }

        if (!deployment.getStatus().isFailed()) {
            logger.warn("只能重试失败的部署: {}, 当前状态: {}", deploymentId, deployment.getStatus());
            return false;
        }

        int currentRetryCount = deployment.getRetryCount() != null ? deployment.getRetryCount() : 0;
        if (currentRetryCount >= maxRetryAttempts) {
            logger.warn("部署已达到最大重试次数: {}", deploymentId);
            return false;
        }

        logger.info("手动触发重试 - deploymentId: {}", deploymentId);
        processRetry(deployment);
        return true;
    }
}
