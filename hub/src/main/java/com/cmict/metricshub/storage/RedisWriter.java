package com.cmict.metricshub.storage;

import com.cmict.metricshub.model.MetricSample;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis writer for storing hot metric data.
 *
 * Storage strategy:
 * - Key pattern: "metrics:latest:{serverId}"
 * - Data structure: Hash (HSET)
 * - TTL: 5 minutes (configurable)
 *
 * Hash structure:
 * {
 *   "system.cpu.usage": "{value: 75.0, unit: 'percent', timestamp: ...}",
 *   "system.memory.usage": "{value: 8589934592, unit: 'bytes', timestamp: ...}"
 * }
 *
 * This allows:
 * - Fast retrieval of latest values by metric name
 * - Automatic expiration of stale data
 * - O(1) lookup performance
 */
@Component
public class RedisWriter {

    private static final Logger logger = LoggerFactory.getLogger(RedisWriter.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${metrics-hub.redis.key-prefix:metrics:latest:}")
    private String keyPrefix;

    @Value("${metrics-hub.redis.ttl-seconds:300}")
    private int ttlSeconds; // 5 minutes default

    /**
     * Write a batch of metric samples to Redis.
     *
     * @param samples List of metric samples to write
     * @return Number of samples successfully written
     */
    public int writeBatch(List<MetricSample> samples) {
        if (samples == null || samples.isEmpty()) {
            return 0;
        }

        int written = 0;
        Map<String, Map<String, String>> serverMetrics = groupByServer(samples);

        for (Map.Entry<String, Map<String, String>> entry : serverMetrics.entrySet()) {
            String serverId = entry.getKey();
            Map<String, String> metrics = entry.getValue();

            try {
                String key = keyPrefix + serverId;

                // Write all metrics for this server as a hash
                redisTemplate.opsForHash().putAll(key, metrics);

                // Set TTL
                redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);

                written += metrics.size();

                logger.debug("Wrote {} metrics to Redis for server {}", metrics.size(), serverId);

            } catch (Exception e) {
                logger.error("Failed to write metrics to Redis for server {}: {}",
                        serverId, e.getMessage(), e);
            }
        }

        logger.info("✅ Redis write completed: {} samples written", written);
        return written;
    }

    /**
     * Write a single metric sample to Redis.
     *
     * @param sample Metric sample to write
     * @return true if successful, false otherwise
     */
    public boolean write(MetricSample sample) {
        String serverId = sample.getLabel("server.id");
        if (serverId == null || serverId.isEmpty()) {
            logger.warn("Cannot write to Redis: missing server.id label");
            return false;
        }

        try {
            String key = keyPrefix + serverId;
            String metricName = sample.getName();
            String value = serializeMetricValue(sample);

            redisTemplate.opsForHash().put(key, metricName, value);
            redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);

            logger.trace("Wrote metric {} to Redis for server {}", metricName, serverId);
            return true;

        } catch (Exception e) {
            logger.error("Failed to write metric {} to Redis: {}",
                    sample.getName(), e.getMessage());
            return false;
        }
    }

    /**
     * Get latest metric value from Redis.
     *
     * @param serverId Server identifier
     * @param metricName Metric name
     * @return Metric value JSON, or null if not found
     */
    public String getLatestMetric(String serverId, String metricName) {
        try {
            String key = keyPrefix + serverId;
            Object value = redisTemplate.opsForHash().get(key, metricName);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            logger.error("Failed to get metric from Redis: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get all latest metrics for a server.
     *
     * @param serverId Server identifier
     * @return Map of metric name to value JSON
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getAllLatestMetrics(String serverId) {
        try {
            String key = keyPrefix + serverId;
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
            // Convert Map<Object, Object> to Map<String, Object>
            Map<String, Object> result = new HashMap<>();
            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                result.put(entry.getKey().toString(), entry.getValue());
            }
            return result;
        } catch (Exception e) {
            logger.error("Failed to get all metrics from Redis: {}", e.getMessage());
            return Map.of();
        }
    }

    /**
     * Delete all metrics for a server.
     *
     * @param serverId Server identifier
     * @return true if successful
     */
    public boolean delete(String serverId) {
        try {
            String key = keyPrefix + serverId;
            return Boolean.TRUE.equals(redisTemplate.delete(key));
        } catch (Exception e) {
            logger.error("Failed to delete metrics from Redis: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Group samples by server ID.
     */
    private Map<String, Map<String, String>> groupByServer(List<MetricSample> samples) {
        Map<String, Map<String, String>> result = new HashMap<>();

        for (MetricSample sample : samples) {
            String serverId = sample.getLabel("server.id");
            if (serverId == null || serverId.isEmpty()) {
                logger.warn("Skipping sample without server.id: {}", sample.getName());
                continue;
            }

            result.computeIfAbsent(serverId, k -> new HashMap<>());

            try {
                String value = serializeMetricValue(sample);
                result.get(serverId).put(sample.getName(), value);
            } catch (Exception e) {
                logger.warn("Failed to serialize metric {}: {}", sample.getName(), e.getMessage());
            }
        }

        return result;
    }

    /**
     * Serialize metric value to JSON string.
     */
    private String serializeMetricValue(MetricSample sample) throws JsonProcessingException {
        Map<String, Object> value = new HashMap<>();
        value.put("value", sample.getValue());
        value.put("unit", sample.getUnit());
        value.put("timestamp", sample.getTimestamp().toEpochMilli());
        value.put("type", sample.getType().name());

        if (sample.getCount() != null) {
            value.put("count", sample.getCount());
        }
        if (sample.getSum() != null) {
            value.put("sum", sample.getSum());
        }

        return objectMapper.writeValueAsString(value);
    }
}
