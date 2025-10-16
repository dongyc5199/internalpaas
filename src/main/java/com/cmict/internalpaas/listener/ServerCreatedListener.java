package com.cmict.internalpaas.listener;

import com.cmict.internalpaas.dto.agent.DeployResult;
import com.cmict.internalpaas.event.ServerCreatedEvent;
import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.service.AgentDeployService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 服务器创建事件监听器
 * 监听服务器创建事件，自动触发Agent部署
 *
 * @author Dev Debug Platform Team
 * @version 2.0 (阶段2 - Agent自动部署)
 */
@Component
public class ServerCreatedListener {

    private static final Logger logger = LoggerFactory.getLogger(ServerCreatedListener.class);

    @Autowired
    private AgentDeployService agentDeployService;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 监听服务器创建事件
     * 当新服务器被添加到系统时，自动触发Agent部署
     *
     * @param event 服务器创建事件
     */
    @EventListener
    @Async("agentDeployExecutor")
    public void onServerCreated(ServerCreatedEvent event) {
        Server server = event.getServer();

        logger.info("收到服务器创建事件 - serverId: {}, serverName: {}", server.getId(), server.getName());

        // 检查是否启用自动部署
        if (!agentDeployService.isAutoDeployEnabled()) {
            logger.info("Agent自动部署已禁用，跳过部署 - serverId: {}", server.getId());
            return;
        }

        logger.info("开始自动部署Agent - serverId: {}, serverName: {}", server.getId(), server.getName());

        // 推送WebSocket通知
        sendNotification(server.getId(), "开始自动部署Agent", "info");

        // 异步部署Agent
        CompletableFuture<DeployResult> deployFuture = agentDeployService.deployAgent(server);

        deployFuture.thenAccept(result -> {
            if (result.isSuccess()) {
                logger.info("✅ Agent自动部署成功 - serverId: {}, deploymentId: {}",
                        server.getId(), result.getDeploymentId());
                sendNotification(server.getId(), "Agent部署完成", "success");

            } else {
                logger.error("❌ Agent自动部署失败 - serverId: {}, error: {}",
                        server.getId(), result.getErrorMessage());
                sendNotification(server.getId(), "Agent部署失败: " + result.getErrorMessage(), "error");

                // TODO: 实现重试机制
                // scheduleRetry(server, 1);
            }
        }).exceptionally(ex -> {
            logger.error("Agent自动部署异常 - serverId: {}", server.getId(), ex);
            sendNotification(server.getId(), "Agent部署异常: " + ex.getMessage(), "error");
            return null;
        });
    }

    /**
     * 发送WebSocket通知
     *
     * @param serverId 服务器ID
     * @param message  通知消息
     * @param type     通知类型 (info, success, error, warning)
     */
    private void sendNotification(Long serverId, String message, String type) {
        if (messagingTemplate == null) {
            logger.debug("WebSocket未配置，跳过通知推送");
            return;
        }

        try {
            Map<String, Object> notification = Map.of(
                    "serverId", serverId,
                    "message", message,
                    "type", type,
                    "timestamp", System.currentTimeMillis()
            );

            messagingTemplate.convertAndSend("/topic/agent-deploy-notification/" + serverId, notification);
            logger.debug("发送通知 - serverId: {}, message: {}, type: {}", serverId, message, type);

        } catch (Exception e) {
            logger.error("发送通知失败 - serverId: {}", serverId, e);
        }
    }

    /**
     * TODO: 实现重试调度
     * 根据重试次数计算延迟时间（指数退避）
     *
     * @param server     服务器
     * @param retryCount 当前重试次数
     */
    private void scheduleRetry(Server server, int retryCount) {
        // 重试延迟: 1分钟、5分钟、15分钟
        int[] retryDelays = {1, 5, 15};

        if (retryCount > retryDelays.length) {
            logger.warn("Agent部署达到最大重试次数，停止重试 - serverId: {}", server.getId());
            sendNotification(server.getId(), "Agent部署失败，已达到最大重试次数", "warning");
            return;
        }

        int delayMinutes = retryDelays[retryCount - 1];
        logger.info("安排Agent部署重试 - serverId: {}, retryCount: {}, delay: {}分钟",
                server.getId(), retryCount, delayMinutes);

        // TODO: 使用ScheduledExecutorService实现延迟重试
        // 或者使用Spring的@Scheduled + 数据库状态检查
    }
}
