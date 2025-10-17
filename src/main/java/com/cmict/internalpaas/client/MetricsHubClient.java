package com.cmict.internalpaas.client;

import com.cmict.internalpaas.dto.hub.MetricSampleDto;
import com.cmict.internalpaas.dto.hub.MetricsQueryResponseDto;
import com.cmict.internalpaas.model.ServerMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * Metrics Hub 客户端（T4集成版本）
 * 负责与独立部署的 Metrics Hub 服务通信
 *
 * 使用条件: metrics.hub.enabled=true 时才会创建此Bean
 *
 * 功能:
 * - 调用Hub的T4 Read API (/monitoring/server/{id}/metrics/query)
 * - 数据模型转换 (MetricSample → ServerMetrics)
 * - 健康检查和可用性判断
 * - 错误处理和降级支持
 *
 * @author Dev Debug Platform Team
 * @version 2.0 (T4集成版)
 */
@Component
@ConditionalOnProperty(name = "metrics.hub.enabled", havingValue = "true")
public class MetricsHubClient {

    private static final Logger logger = LoggerFactory.getLogger(MetricsHubClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private volatile boolean available = false;
    private long lastHealthCheck = 0;
    private static final long HEALTH_CHECK_INTERVAL = 60000; // 1分钟

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

        logger.info("✅ Metrics Hub Client initialized (T4): {}", baseUrl);
        logger.info("   - Connect timeout: {}ms", connectTimeout);
        logger.info("   - Read timeout: {}ms", readTimeout);
    }

    /**
     * 检查Hub服务是否可用（带缓存的健康检查）
     */
    public boolean isAvailable() {
        long now = System.currentTimeMillis();

        // 避免频繁健康检查（1分钟内复用上次结果）
        if (now - lastHealthCheck < HEALTH_CHECK_INTERVAL && available) {
            return true;
        }

        try {
            String healthUrl = baseUrl + "/actuator/health";
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
            available = response.getStatusCode().is2xxSuccessful();
            lastHealthCheck = now;

            if (available) {
                logger.debug("✅ Metrics Hub is available");
            }
            return available;

        } catch (Exception e) {
            available = false;
            lastHealthCheck = now;
            logger.warn("⚠️ Metrics Hub unavailable: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取服务器最新指标（调用T4 API）
     */
    public ServerMetrics getLatestMetrics(Long serverId) {
        return queryMetrics(serverId, null, null, null, null);
    }

    /**
     * 查询服务器监控数据（T4完整API）
     *
     * @param serverId 服务器ID
     * @param from 开始时间（Unix毫秒时间戳）
     * @param to 结束时间（Unix毫秒时间戳）
     * @param step 降采样间隔（秒）
     * @param fields 字段列表（逗号分隔）
     * @return ServerMetrics 或 null
     */
    public ServerMetrics queryMetrics(Long serverId, Long from, Long to, Integer step, String fields) {
        try {
            // 构建T4 API URL: /monitoring/server/{id}/metrics/query
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(baseUrl + "/monitoring/server/" + serverId + "/metrics/query");

            if (from != null) builder.queryParam("from", from);
            if (to != null) builder.queryParam("to", to);
            if (step != null) builder.queryParam("step", step);
            if (fields != null && !fields.trim().isEmpty()) builder.queryParam("fields", fields);

            String url = builder.toUriString();
            logger.debug("📊 Query Hub API: {}", url);

            // 调用Hub API
            ResponseEntity<MetricsQueryResponseDto> response =
                    restTemplate.getForEntity(url, MetricsQueryResponseDto.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                MetricsQueryResponseDto responseDto = response.getBody();
                logger.info("✅ Hub query success: serverId={}, count={}",
                        serverId, responseDto.getCount());

                // 转换为ServerMetrics
                return convertToServerMetrics(serverId, responseDto.getData());
            }

            logger.warn("Hub query returned non-2xx: {}", response.getStatusCode());
            return null;

        } catch (Exception e) {
            logger.error("❌ Hub query failed: serverId={}, error={}", serverId, e.getMessage());
            return null;
        }
    }

    /**
     * Step 5.2: 查询监控数据 (原始响应版本)
     * 直接返回Hub的响应,不做模型转换,减少开销
     * 
     * 用途: 前端直接调用时使用,避免ServerMetrics模型转换
     * 
     * @param serverId 服务器ID
     * @param from 起始时间戳(毫秒,可选)
     * @param to 结束时间戳(毫秒,可选)
     * @param step 降采样间隔(秒,可选)
     * @param fields 字段过滤(逗号分隔,可选)
     * @return ResponseEntity 包含Hub原始响应
     */
    public ResponseEntity<Map<String, Object>> queryMetricsRaw(
            Long serverId,
            Long from,
            Long to,
            Integer step,
            String fields) {
        
        try {
            // 构建Hub API URL
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(baseUrl + "/monitoring/server/" + serverId + "/metrics/query");

            if (from != null) builder.queryParam("from", from);
            if (to != null) builder.queryParam("to", to);
            if (step != null) builder.queryParam("step", step);
            if (fields != null && !fields.trim().isEmpty()) builder.queryParam("fields", fields);

            String url = builder.toUriString();
            logger.debug("📊 Query Hub API (raw): {}", url);

            // 直接调用Hub并返回原始响应
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = 
                    (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) 
                    restTemplate.getForEntity(url, Map.class);

            logger.info("✅ Hub raw query success: serverId={}, status={}", 
                    serverId, response.getStatusCode());

            return response;

        } catch (Exception e) {
            logger.error("❌ Hub raw query failed: serverId={}, error={}", 
                    serverId, e.getMessage());
            
            // 返回500错误
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                        "error", "Hub查询失败: " + e.getMessage(),
                        "code", "HUB_QUERY_FAILED",
                        "serverId", serverId
                    ));
        }
    }

    /**
     * 数据模型转换: MetricSample → ServerMetrics
     */
    private ServerMetrics convertToServerMetrics(Long serverId, List<MetricSampleDto> samples) {
        if (samples == null || samples.isEmpty()) {
            return null;
        }

        ServerMetrics metrics = new ServerMetrics();
        metrics.setServerId(serverId);

        String serverName = null;
        String hostname = null;
        LocalDateTime timestamp = null;

        // 遍历所有样本，按指标名称映射
        for (MetricSampleDto sample : samples) {
            String metricName = sample.getName();
            Double value = sample.getValue();

            // 提取标签信息
            if (serverName == null && sample.getLabels() != null) {
                serverName = sample.getLabels().get("server.name");
                hostname = sample.getLabels().get("hostname");
            }

            // 转换时间戳
            if (timestamp == null && sample.getTimestamp() != null) {
                timestamp = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(sample.getTimestamp()),
                        ZoneId.systemDefault());
            }

            if (value == null) continue;

            // 指标名称映射
            switch (metricName) {
                case "system.cpu.usage":
                    metrics.setCpuUsage(value);
                    break;
                case "system.memory.usage":
                    metrics.setMemoryUsage(value);
                    break;
                case "system.memory.total":
                    metrics.setMemoryTotal(value.longValue());
                    break;
                case "system.memory.used":
                    metrics.setMemoryUsed(value.longValue());
                    break;
                case "system.memory.available":
                    metrics.setMemoryAvailable(value.longValue());
                    break;
                case "system.disk.usage":
                    metrics.setDiskUsage(value);
                    break;
                case "system.disk.total":
                    metrics.setDiskTotal(value.longValue());
                    break;
                case "system.disk.used":
                    metrics.setDiskUsed(value.longValue());
                    break;
                case "system.disk.available":
                    metrics.setDiskAvailable(value.longValue());
                    break;
                case "system.network.io.receive":
                    metrics.setNetworkReceivedBytes(value.longValue());
                    break;
                case "system.network.io.transmit":
                    metrics.setNetworkTransmittedBytes(value.longValue());
                    break;
                case "system.network.io.receive.rate":
                    metrics.setNetworkReceivedRate(value);
                    break;
                case "system.network.io.transmit.rate":
                    metrics.setNetworkTransmittedRate(value);
                    break;
            }
        }

        metrics.setServerName(serverName != null ? serverName : "Unknown");
        metrics.setHostname(hostname != null ? hostname : "Unknown");
        metrics.setTimestamp(timestamp != null ? timestamp : LocalDateTime.now());

        logger.debug("✅ Converted to ServerMetrics: cpu={}, mem={}, disk={}",
                metrics.getCpuUsage(), metrics.getMemoryUsage(), metrics.getDiskUsage());

        return metrics;
    }

    /**
     * 检查 Metrics Hub 健康状态（兼容旧方法）
     */
    public boolean isHealthy() {
        return isAvailable();
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
