package com.cmict.internalpaas.client;

import com.cmict.internalpaas.model.ServerMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Metrics Hub 客户端
 * 负责与独立部署的 Metrics Hub 服务通信
 *
 * 使用条件: metrics.hub.enabled=true 时才会创建此Bean
 *
 * @author Dev Debug Platform Team
 * @version 1.0
 */
@Component
@ConditionalOnProperty(name = "metrics.hub.enabled", havingValue = "true")
public class MetricsHubClient {

    private static final Logger logger = LoggerFactory.getLogger(MetricsHubClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public MetricsHubClient(
            RestTemplateBuilder builder,
            @Value("${metrics.hub.base-url}") String baseUrl,
            @Value("${metrics.hub.timeout.connect-ms:5000}") int connectTimeout,
            @Value("${metrics.hub.timeout.read-ms:10000}") int readTimeout) {

        this.baseUrl = baseUrl;
        this.restTemplate = builder
            .setConnectTimeout(Duration.ofMillis(connectTimeout))
            .setReadTimeout(Duration.ofMillis(readTimeout))
            .build();

        logger.info("✅ Metrics Hub Client initialized: {}", baseUrl);
        logger.info("   - Connect timeout: {}ms", connectTimeout);
        logger.info("   - Read timeout: {}ms", readTimeout);
    }

    /**
     * 获取服务器最新指标
     *
     * @param serverId 服务器ID
     * @return 服务器监控指标
     * @throws MetricsHubException 当 Hub 不可用时抛出
     */
    public ServerMetrics getLatestMetrics(Long serverId) {
        try {
            String url = String.format("%s/api/v1/metrics/server/%d/latest", baseUrl, serverId);
            logger.debug("📊 Fetching latest metrics from Hub: {}", url);

            ServerMetrics metrics = restTemplate.getForObject(url, ServerMetrics.class);

            if (metrics != null) {
                logger.debug("✅ Successfully fetched metrics for server {} from Hub", serverId);
            } else {
                logger.warn("⚠️ Hub returned null metrics for server {}", serverId);
            }

            return metrics;

        } catch (Exception e) {
            logger.error("❌ Failed to fetch metrics from Hub for server {}: {}",
                serverId, e.getMessage());
            throw new MetricsHubException("Metrics Hub unavailable for server " + serverId, e);
        }
    }

    /**
     * 获取历史范围指标
     *
     * @param serverId 服务器ID
     * @param from 开始时间戳（毫秒）
     * @param to 结束时间戳（毫秒）
     * @return 服务器监控指标
     * @throws MetricsHubException 当 Hub 不可用时抛出
     */
    public ServerMetrics getRangeMetrics(Long serverId, Long from, Long to) {
        try {
            String url = String.format(
                "%s/api/v1/metrics/server/%d/range?from=%d&to=%d",
                baseUrl, serverId, from, to
            );

            logger.debug("📈 Fetching range metrics from Hub: {}", url);
            ServerMetrics metrics = restTemplate.getForObject(url, ServerMetrics.class);

            if (metrics != null) {
                logger.debug("✅ Successfully fetched range metrics for server {}", serverId);
            }

            return metrics;

        } catch (Exception e) {
            logger.error("❌ Failed to fetch range metrics from Hub: {}", e.getMessage());
            throw new MetricsHubException("Metrics Hub range query failed", e);
        }
    }

    /**
     * 批量获取多服务器最新指标
     *
     * @param serverIds 服务器ID列表
     * @return 服务器ID到指标的映射
     */
    public Map<Long, ServerMetrics> getBatchLatestMetrics(List<Long> serverIds) {
        try {
            String url = baseUrl + "/api/v1/metrics/servers/latest";
            logger.debug("📊 Fetching batch metrics for {} servers", serverIds.size());

            @SuppressWarnings("unchecked")
            Map<Long, ServerMetrics> metricsMap = restTemplate.postForObject(
                url,
                Map.of("serverIds", serverIds),
                Map.class
            );

            logger.debug("✅ Successfully fetched batch metrics for {} servers",
                metricsMap != null ? metricsMap.size() : 0);

            return metricsMap;

        } catch (Exception e) {
            logger.error("❌ Failed to fetch batch metrics from Hub: {}", e.getMessage());
            throw new MetricsHubException("Metrics Hub batch query failed", e);
        }
    }

    /**
     * 检查 Metrics Hub 健康状态
     *
     * @return true 如果 Hub 健康，false 否则
     */
    public boolean isHealthy() {
        try {
            String url = baseUrl + "/actuator/health";

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            boolean healthy = "UP".equals(response.get("status"));

            if (healthy) {
                logger.debug("✅ Metrics Hub is healthy");
            } else {
                logger.warn("⚠️ Metrics Hub health check returned: {}", response.get("status"));
            }

            return healthy;

        } catch (Exception e) {
            logger.warn("❌ Metrics Hub health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取 Metrics Hub 基础URL
     *
     * @return 基础URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Metrics Hub 异常
     * 当 Hub 服务不可用或请求失败时抛出
     */
    public static class MetricsHubException extends RuntimeException {

        public MetricsHubException(String message) {
            super(message);
        }

        public MetricsHubException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
