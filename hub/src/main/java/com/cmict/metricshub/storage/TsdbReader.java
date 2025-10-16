package com.cmict.metricshub.storage;

import com.cmict.metricshub.model.MetricSample;
import com.cmict.metricshub.model.ServerMetricSample;
import com.cmict.metricshub.repository.ServerMetricSampleRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TSDB reader for querying historical metric data.
 *
 * Provides methods to:
 * - Query metrics by time range
 * - Filter by specific metric names
 * - Perform downsampling (step parameter)
 * - Support field selection
 */
@Component
public class TsdbReader {

    private static final Logger logger = LoggerFactory.getLogger(TsdbReader.class);

    @Autowired
    private ServerMetricSampleRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Query metrics for a server within a time range.
     *
     * @param serverId Server identifier
     * @param from Start time (inclusive)
     * @param to End time (inclusive)
     * @param step Downsampling interval (optional)
     * @param fields Metric name filter (optional, comma-separated)
     * @return List of metric samples
     */
    public List<MetricSample> queryMetrics(
            String serverId,
            Instant from,
            Instant to,
            Duration step,
            Set<String> fields) {

        if (serverId == null || serverId.isEmpty()) {
            logger.warn("Cannot query TSDB: missing serverId");
            return Collections.emptyList();
        }

        // Default time range: last hour
        if (from == null) {
            from = Instant.now().minus(Duration.ofHours(1));
        }
        if (to == null) {
            to = Instant.now();
        }

        try {
            List<ServerMetricSample> entities = repository
                    .findByServerIdAndTimestampBetweenOrderByTimestampDesc(serverId, from, to);

            logger.debug("TSDB query returned {} samples for server {} in range [{}, {}]",
                    entities.size(), serverId, from, to);

            List<MetricSample> samples = entities.stream()
                    .map(this::convertToMetricSample)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // Filter by fields if specified
            if (fields != null && !fields.isEmpty()) {
                samples = samples.stream()
                        .filter(s -> fields.contains(s.getName()))
                        .collect(Collectors.toList());
            }

            // Apply downsampling if step is specified
            if (step != null && !step.isZero()) {
                samples = downsample(samples, step);
            }

            logger.info("✅ TSDB read completed: {} samples returned", samples.size());
            return samples;

        } catch (Exception e) {
            logger.error("Failed to query metrics from TSDB: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Query a specific metric for a server.
     *
     * @param serverId Server identifier
     * @param metricName Metric name
     * @param from Start time
     * @param to End time
     * @return List of metric samples
     */
    public List<MetricSample> queryMetric(
            String serverId,
            String metricName,
            Instant from,
            Instant to) {

        if (serverId == null || metricName == null) {
            logger.warn("Cannot query TSDB: missing serverId or metricName");
            return Collections.emptyList();
        }

        if (from == null) {
            from = Instant.now().minus(Duration.ofHours(1));
        }
        if (to == null) {
            to = Instant.now();
        }

        try {
            List<ServerMetricSample> entities = repository
                    .findByServerIdAndMetricNameAndTimestampBetween(serverId, metricName, from, to);

            List<MetricSample> samples = entities.stream()
                    .map(this::convertToMetricSample)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            logger.debug("TSDB query returned {} samples for metric {}", samples.size(), metricName);
            return samples;

        } catch (Exception e) {
            logger.error("Failed to query metric {} from TSDB: {}", metricName, e.getMessage());
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
            ServerMetricSample entity = repository
                    .findLatestByServerIdAndMetricName(serverId, metricName);

            if (entity == null) {
                return null;
            }

            return convertToMetricSample(entity);

        } catch (Exception e) {
            logger.error("Failed to get latest metric from TSDB: {}", e.getMessage());
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
            // Query recent data (last 5 minutes)
            Instant from = Instant.now().minus(Duration.ofMinutes(5));
            Instant to = Instant.now();

            List<ServerMetricSample> entities = repository
                    .findByServerIdAndTimestampBetweenOrderByTimestampDesc(serverId, from, to);

            // Group by metric name and take the latest
            Map<String, MetricSample> latestMetrics = new HashMap<>();

            for (ServerMetricSample entity : entities) {
                String metricName = entity.getMetricName();
                if (!latestMetrics.containsKey(metricName)) {
                    MetricSample sample = convertToMetricSample(entity);
                    if (sample != null) {
                        latestMetrics.put(metricName, sample);
                    }
                }
            }

            return latestMetrics;

        } catch (Exception e) {
            logger.error("Failed to get all latest metrics from TSDB: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Convert ServerMetricSample entity to MetricSample model.
     *
     * @param entity JPA entity
     * @return MetricSample, or null if conversion fails
     */
    private MetricSample convertToMetricSample(ServerMetricSample entity) {
        if (entity == null) {
            return null;
        }

        try {
            MetricSample sample = MetricSample.builder()
                    .name(entity.getMetricName())
                    .type(entity.getMetricType())
                    .value(entity.getValue())
                    .unit(entity.getUnit())
                    .timestamp(entity.getTimestamp())
                    .build();

            // Deserialize labels
            if (entity.getLabels() != null && !entity.getLabels().isEmpty()) {
                try {
                    Map<String, String> labels = objectMapper.readValue(
                            entity.getLabels(),
                            new TypeReference<Map<String, String>>() {});
                    sample.setLabels(labels);
                } catch (Exception e) {
                    logger.warn("Failed to deserialize labels: {}", e.getMessage());
                }
            }

            // Set histogram fields
            if (entity.getCount() != null) {
                sample.setCount(entity.getCount());
            }
            if (entity.getSum() != null) {
                sample.setSum(entity.getSum());
            }

            // Ensure server.id label is present
            sample.addLabel("server.id", entity.getServerId());

            return sample;

        } catch (Exception e) {
            logger.warn("Failed to convert entity to MetricSample: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Downsample metrics by aggregating data points within each step interval.
     *
     * @param samples Original samples (must be sorted by timestamp DESC)
     * @param step Downsampling interval
     * @return Downsampled samples
     */
    private List<MetricSample> downsample(List<MetricSample> samples, Duration step) {
        if (samples.isEmpty() || step == null || step.isZero()) {
            return samples;
        }

        // Group samples by metric name and time bucket
        Map<String, Map<Long, List<MetricSample>>> buckets = new HashMap<>();

        for (MetricSample sample : samples) {
            String metricName = sample.getName();
            long bucketId = sample.getTimestamp().toEpochMilli() / step.toMillis();

            buckets.computeIfAbsent(metricName, k -> new HashMap<>());
            buckets.get(metricName).computeIfAbsent(bucketId, k -> new ArrayList<>()).add(sample);
        }

        // Aggregate each bucket
        List<MetricSample> downsampled = new ArrayList<>();

        for (Map.Entry<String, Map<Long, List<MetricSample>>> metricEntry : buckets.entrySet()) {
            for (Map.Entry<Long, List<MetricSample>> bucketEntry : metricEntry.getValue().entrySet()) {
                List<MetricSample> bucketSamples = bucketEntry.getValue();

                if (bucketSamples.isEmpty()) {
                    continue;
                }

                // Use average aggregation for downsampling
                MetricSample aggregated = aggregateSamples(bucketSamples);
                if (aggregated != null) {
                    downsampled.add(aggregated);
                }
            }
        }

        // Sort by timestamp descending
        downsampled.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));

        logger.debug("Downsampled {} samples to {} (step: {})", samples.size(), downsampled.size(), step);
        return downsampled;
    }

    /**
     * Aggregate multiple samples into one by averaging values.
     *
     * @param samples Samples to aggregate
     * @return Aggregated sample
     */
    private MetricSample aggregateSamples(List<MetricSample> samples) {
        if (samples.isEmpty()) {
            return null;
        }

        if (samples.size() == 1) {
            return samples.get(0);
        }

        MetricSample first = samples.get(0);
        double avgValue = samples.stream()
                .mapToDouble(MetricSample::getValue)
                .average()
                .orElse(0.0);

        // Use the latest timestamp in the bucket
        Instant latestTimestamp = samples.stream()
                .map(MetricSample::getTimestamp)
                .max(Instant::compareTo)
                .orElse(first.getTimestamp());

        MetricSample aggregated = MetricSample.builder()
                .name(first.getName())
                .type(first.getType())
                .value(avgValue)
                .unit(first.getUnit())
                .timestamp(latestTimestamp)
                .labels(first.getLabels())
                .build();

        return aggregated;
    }

    /**
     * Query metrics from 5-minute rollup table.
     *
     * Returns aggregated data (AVG, MAX, MIN) for better performance on medium-term queries.
     *
     * @param serverId Server identifier
     * @param from Start time (inclusive)
     * @param to End time (inclusive)
     * @param fields Metric name filter (optional)
     * @return List of aggregated metric samples
     */
    public List<MetricSample> query5mRollup(
            String serverId,
            Instant from,
            Instant to,
            Set<String> fields) {

        if (serverId == null || serverId.isEmpty()) {
            logger.warn("Cannot query 5m rollup: missing serverId");
            return Collections.emptyList();
        }

        // Default time range
        if (from == null) {
            from = Instant.now().minus(Duration.ofDays(1));
        }
        if (to == null) {
            to = Instant.now();
        }

        try {
            String sql = "SELECT server_id, metric_name, time_bucket, avg_value, max_value, min_value, sample_count " +
                    "FROM server_metric_samples_5m " +
                    "WHERE server_id = ? AND time_bucket >= ? AND time_bucket <= ? " +
                    "ORDER BY time_bucket DESC";

            List<MetricSample> samples = jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> {
                        String metricName = rs.getString("metric_name");
                        Timestamp timeBucket = rs.getTimestamp("time_bucket");
                        double avgValue = rs.getDouble("avg_value");

                        MetricSample sample = MetricSample.builder()
                                .name(metricName)
                                .type(MetricSample.MetricType.GAUGE)  // Rollup data is treated as gauge
                                .value(avgValue)
                                .timestamp(timeBucket.toInstant())
                                .build();

                        // Add server.id label
                        sample.addLabel("server.id", serverId);
                        sample.addLabel("rollup", "5m");
                        sample.addLabel("max", String.valueOf(rs.getDouble("max_value")));
                        sample.addLabel("min", String.valueOf(rs.getDouble("min_value")));
                        sample.addLabel("sample_count", String.valueOf(rs.getInt("sample_count")));

                        return sample;
                    },
                    serverId,
                    Timestamp.from(from),
                    Timestamp.from(to)
            );

            logger.debug("5m rollup query returned {} samples for server {} in range [{}, {}]",
                    samples.size(), serverId, from, to);

            // Filter by fields if specified
            if (fields != null && !fields.isEmpty()) {
                samples = samples.stream()
                        .filter(s -> fields.contains(s.getName()))
                        .collect(Collectors.toList());
            }

            logger.info("✅ 5m rollup read completed: {} samples returned", samples.size());
            return samples;

        } catch (Exception e) {
            logger.error("Failed to query 5m rollup from TSDB: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Query metrics from 1-hour rollup table.
     *
     * Returns aggregated data (AVG, MAX, MIN) for optimal performance on long-term queries.
     *
     * @param serverId Server identifier
     * @param from Start time (inclusive)
     * @param to End time (inclusive)
     * @param fields Metric name filter (optional)
     * @return List of aggregated metric samples
     */
    public List<MetricSample> query1hRollup(
            String serverId,
            Instant from,
            Instant to,
            Set<String> fields) {

        if (serverId == null || serverId.isEmpty()) {
            logger.warn("Cannot query 1h rollup: missing serverId");
            return Collections.emptyList();
        }

        // Default time range
        if (from == null) {
            from = Instant.now().minus(Duration.ofDays(7));
        }
        if (to == null) {
            to = Instant.now();
        }

        try {
            String sql = "SELECT server_id, metric_name, time_bucket, avg_value, max_value, min_value, sample_count " +
                    "FROM server_metric_samples_1h " +
                    "WHERE server_id = ? AND time_bucket >= ? AND time_bucket <= ? " +
                    "ORDER BY time_bucket DESC";

            List<MetricSample> samples = jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> {
                        String metricName = rs.getString("metric_name");
                        Timestamp timeBucket = rs.getTimestamp("time_bucket");
                        double avgValue = rs.getDouble("avg_value");

                        MetricSample sample = MetricSample.builder()
                                .name(metricName)
                                .type(MetricSample.MetricType.GAUGE)  // Rollup data is treated as gauge
                                .value(avgValue)
                                .timestamp(timeBucket.toInstant())
                                .build();

                        // Add server.id label
                        sample.addLabel("server.id", serverId);
                        sample.addLabel("rollup", "1h");
                        sample.addLabel("max", String.valueOf(rs.getDouble("max_value")));
                        sample.addLabel("min", String.valueOf(rs.getDouble("min_value")));
                        sample.addLabel("sample_count", String.valueOf(rs.getInt("sample_count")));

                        return sample;
                    },
                    serverId,
                    Timestamp.from(from),
                    Timestamp.from(to)
            );

            logger.debug("1h rollup query returned {} samples for server {} in range [{}, {}]",
                    samples.size(), serverId, from, to);

            // Filter by fields if specified
            if (fields != null && !fields.isEmpty()) {
                samples = samples.stream()
                        .filter(s -> fields.contains(s.getName()))
                        .collect(Collectors.toList());
            }

            logger.info("✅ 1h rollup read completed: {} samples returned", samples.size());
            return samples;

        } catch (Exception e) {
            logger.error("Failed to query 1h rollup from TSDB: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
