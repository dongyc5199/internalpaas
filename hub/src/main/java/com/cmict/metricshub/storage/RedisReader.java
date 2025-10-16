package com.cmict.metricshub.storage;

import com.cmict.metricshub.model.MetricSample;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Redis reader for querying hot (recent) metric data.
 *
 * Wraps RedisWriter's read methods and provides a unified interface
 * consistent with TsdbReader.
 *
 * Features:
 * - Fast retrieval of latest values
 * - Field filtering
 * - Fallback to TSDB if Redis data is unavailable
 */
@Component
public class RedisReader {

    private static final Logger logger = LoggerFactory.getLogger(RedisReader.class);

    @Autowired
    private RedisWriter redisWriter;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Query latest metrics for a server.
     *
     * Note: Redis only stores latest values, so time range parameters are ignored.
     * For historical data queries, use TsdbReader instead.
     *
     * @param serverId Server identifier
     * @param from Start time (ignored for Redis)
     * @param to End time (ignored for Redis)
     * @param step Downsampling interval (ignored for Redis)
     * @param fields Metric name filter (optional)
     * @return List of latest metric samples
     */
    public List<MetricSample> queryMetrics(
            String serverId,
            Instant from,
            Instant to,
            Duration step,
            Set<String> fields) {

        if (serverId == null || serverId.isEmpty()) {
            logger.warn("Cannot query Redis: missing serverId");
            return Collections.emptyList();
        }

        try {
            Map<String, Object> allMetrics = redisWriter.getAllLatestMetrics(serverId);

            if (allMetrics.isEmpty()) {
                logger.debug("No metrics found in Redis for server {}", serverId);
                return Collections.emptyList();
            }

            List<MetricSample> samples = new ArrayList<>();

            for (Map.Entry<String, Object> entry : allMetrics.entrySet()) {
                String metricName = entry.getKey();

                // Filter by fields if specified
                if (fields != null && !fields.isEmpty() && !fields.contains(metricName)) {
                    continue;
                }

                MetricSample sample = deserializeMetricValue(serverId, metricName, entry.getValue());
                if (sample != null) {
                    samples.add(sample);
                }
            }

            logger.debug("Redis query returned {} samples for server {}", samples.size(), serverId);
            return samples;

        } catch (Exception e) {
            logger.error("Failed to query metrics from Redis: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Query a specific metric for a server.
     *
     * @param serverId Server identifier
     * @param metricName Metric name
     * @param from Start time (ignored for Redis)
     * @param to End time (ignored for Redis)
     * @return List containing the latest metric sample (or empty if not found)
     */
    public List<MetricSample> queryMetric(
            String serverId,
            String metricName,
            Instant from,
            Instant to) {

        if (serverId == null || metricName == null) {
            logger.warn("Cannot query Redis: missing serverId or metricName");
            return Collections.emptyList();
        }

        try {
            String valueJson = redisWriter.getLatestMetric(serverId, metricName);

            if (valueJson == null) {
                return Collections.emptyList();
            }

            MetricSample sample = deserializeMetricValue(serverId, metricName, valueJson);
            if (sample != null) {
                return Collections.singletonList(sample);
            }

            return Collections.emptyList();

        } catch (Exception e) {
            logger.error("Failed to query metric {} from Redis: {}", metricName, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get the latest value of a specific metric.
     *
     * @param serverId Server identifier
     * @param metricName Metric name
     * @return Latest metric sample, or null if not found
     */
    public MetricSample getLatestMetric(String serverId, String metricName) {
        try {
            String valueJson = redisWriter.getLatestMetric(serverId, metricName);

            if (valueJson == null) {
                return null;
            }

            return deserializeMetricValue(serverId, metricName, valueJson);

        } catch (Exception e) {
            logger.error("Failed to get latest metric from Redis: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get all latest metrics for a server.
     *
     * @param serverId Server identifier
     * @return Map of metric name to latest sample
     */
    public Map<String, MetricSample> getAllLatestMetrics(String serverId) {
        try {
            Map<String, Object> allMetrics = redisWriter.getAllLatestMetrics(serverId);

            Map<String, MetricSample> result = new HashMap<>();

            for (Map.Entry<String, Object> entry : allMetrics.entrySet()) {
                String metricName = entry.getKey();
                MetricSample sample = deserializeMetricValue(serverId, metricName, entry.getValue());

                if (sample != null) {
                    result.put(metricName, sample);
                }
            }

            return result;

        } catch (Exception e) {
            logger.error("Failed to get all latest metrics from Redis: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Deserialize metric value from Redis JSON string.
     *
     * Expected format:
     * {
     *   "value": 75.0,
     *   "unit": "percent",
     *   "timestamp": 1633024800000,
     *   "type": "GAUGE",
     *   "count": 10,  // optional
     *   "sum": 750.0  // optional
     * }
     *
     * @param serverId Server identifier
     * @param metricName Metric name
     * @param valueObj Value object from Redis (String or Map)
     * @return MetricSample, or null if deserialization fails
     */
    private MetricSample deserializeMetricValue(String serverId, String metricName, Object valueObj) {
        try {
            String valueJson;

            if (valueObj instanceof String) {
                valueJson = (String) valueObj;
            } else {
                valueJson = objectMapper.writeValueAsString(valueObj);
            }

            Map<String, Object> valueMap = objectMapper.readValue(
                    valueJson,
                    new TypeReference<Map<String, Object>>() {});

            Double value = getDoubleValue(valueMap, "value");
            String unit = (String) valueMap.get("unit");
            Long timestamp = getLongValue(valueMap, "timestamp");
            String typeStr = (String) valueMap.get("type");

            if (value == null || unit == null || timestamp == null) {
                logger.warn("Incomplete metric data for {}: {}", metricName, valueJson);
                return null;
            }

            MetricSample.MetricType type = MetricSample.MetricType.GAUGE;
            if (typeStr != null) {
                try {
                    type = MetricSample.MetricType.valueOf(typeStr);
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid metric type: {}", typeStr);
                }
            }

            MetricSample.Builder builder = MetricSample.builder()
                    .name(metricName)
                    .type(type)
                    .value(value)
                    .unit(unit)
                    .timestamp(Instant.ofEpochMilli(timestamp))
                    .label("server.id", serverId);

            // Add optional histogram fields
            Long count = getLongValue(valueMap, "count");
            if (count != null) {
                builder.count(count);
            }

            Double sum = getDoubleValue(valueMap, "sum");
            if (sum != null) {
                builder.sum(sum);
            }

            return builder.build();

        } catch (Exception e) {
            logger.warn("Failed to deserialize metric value for {}: {}",
                    metricName, e.getMessage());
            return null;
        }
    }

    /**
     * Safely extract Double value from map.
     */
    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Safely extract Long value from map.
     */
    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
