package com.cmict.metricshub.service;

import com.cmict.metricshub.model.MetricSample;
import com.cmict.metricshub.storage.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Unified metric query service with intelligent data source routing.
 *
 * Automatically selects the appropriate data source (Redis or TSDB)
 * based on the query time range:
 * - Recent data (last 5 minutes): Redis
 * - Historical data (older than 5 minutes): TSDB
 *
 * Features:
 * - Smart data source selection
 * - Time range queries with downsampling
 * - Field filtering
 * - Performance metrics
 */
@Service
public class MetricQueryService {

    private static final Logger logger = LoggerFactory.getLogger(MetricQueryService.class);

    @Autowired
    private MetricDataSourceSelector dataSourceSelector;

    @Autowired
    private RedisReader redisReader;

    @Autowired
    private TsdbReader tsdbReader;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    private Counter redisQueryCounter;
    private Counter tsdbQueryCounter;
    private Timer queryTimer;

    /**
     * Initialize metrics if MeterRegistry is available.
     */
    public void init() {
        if (meterRegistry != null) {
            redisQueryCounter = Counter.builder("metric.query.redis")
                    .description("Number of queries served by Redis")
                    .register(meterRegistry);

            tsdbQueryCounter = Counter.builder("metric.query.tsdb")
                    .description("Number of queries served by TSDB")
                    .register(meterRegistry);

            queryTimer = Timer.builder("metric.query.duration")
                    .description("Metric query duration")
                    .register(meterRegistry);
        }
    }

    /**
     * Query metrics for a server with intelligent data source selection.
     *
     * @param serverId Server identifier
     * @param from Start time (null for latest)
     * @param to End time (null for latest)
     * @param step Downsampling interval (null for no downsampling)
     * @param fields Comma-separated metric names (null for all)
     * @return List of metric samples
     */
    public List<MetricSample> queryMetrics(
            String serverId,
            Instant from,
            Instant to,
            Duration step,
            String fields) {

        Timer.Sample timerSample = queryTimer != null ? Timer.start(meterRegistry) : null;

        try {
            // Parse fields
            Set<String> fieldSet = parseFields(fields);

            // Select data source
            MetricDataSourceSelector.DataSource dataSource =
                    dataSourceSelector.selectDataSource(from, to);

            logger.info("Query metrics for server {} using {} (from={}, to={}, step={}, fields={})",
                    serverId, dataSource, from, to, step, fieldSet);

            List<MetricSample> samples;

            if (dataSource == MetricDataSourceSelector.DataSource.REDIS) {
                samples = queryFromRedis(serverId, from, to, step, fieldSet);
                if (redisQueryCounter != null) redisQueryCounter.increment();
            } else {
                samples = queryFromTsdb(serverId, from, to, step, fieldSet);
                if (tsdbQueryCounter != null) tsdbQueryCounter.increment();
            }

            logger.info("✅ Query completed: {} samples returned from {}",
                    samples.size(), dataSource);

            return samples;

        } finally {
            if (timerSample != null && queryTimer != null) {
                timerSample.stop(queryTimer);
            }
        }
    }

    /**
     * Get latest metric values for a server.
     *
     * Always queries from Redis first, falls back to TSDB if needed.
     *
     * @param serverId Server identifier
     * @param fields Comma-separated metric names (null for all)
     * @return Map of metric name to latest value
     */
    public Map<String, MetricSample> getLatestMetrics(String serverId, String fields) {
        Set<String> fieldSet = parseFields(fields);

        // Try Redis first
        Map<String, MetricSample> metrics = redisReader.getAllLatestMetrics(serverId);

        // Filter by fields if specified
        if (fieldSet != null && !fieldSet.isEmpty()) {
            metrics = filterByFields(metrics, fieldSet);
        }

        // If Redis is empty, fallback to TSDB
        if (metrics.isEmpty()) {
            logger.debug("Redis empty for server {}, falling back to TSDB", serverId);
            metrics = tsdbReader.getAllLatestMetrics(serverId);

            if (fieldSet != null && !fieldSet.isEmpty()) {
                metrics = filterByFields(metrics, fieldSet);
            }
        }

        return metrics;
    }

    /**
     * Get latest value of a specific metric.
     *
     * @param serverId Server identifier
     * @param metricName Metric name
     * @return Latest metric sample, or null if not found
     */
    public MetricSample getLatestMetric(String serverId, String metricName) {
        // Try Redis first
        MetricSample sample = redisReader.getLatestMetric(serverId, metricName);

        // Fallback to TSDB if not found in Redis
        if (sample == null) {
            sample = tsdbReader.getLatestMetric(serverId, metricName);
        }

        return sample;
    }

    /**
     * Query metrics from Redis.
     */
    private List<MetricSample> queryFromRedis(
            String serverId,
            Instant from,
            Instant to,
            Duration step,
            Set<String> fields) {

        return redisReader.queryMetrics(serverId, from, to, step, fields);
    }

    /**
     * Query metrics from TSDB.
     */
    private List<MetricSample> queryFromTsdb(
            String serverId,
            Instant from,
            Instant to,
            Duration step,
            Set<String> fields) {

        return tsdbReader.queryMetrics(serverId, from, to, step, fields);
    }

    /**
     * Parse comma-separated fields string into a set.
     *
     * @param fields Comma-separated metric names
     * @return Set of metric names, or null if fields is null/empty
     */
    private Set<String> parseFields(String fields) {
        if (fields == null || fields.trim().isEmpty()) {
            return null;
        }

        Set<String> fieldSet = new HashSet<>();
        for (String field : fields.split(",")) {
            String trimmed = field.trim();
            if (!trimmed.isEmpty()) {
                fieldSet.add(trimmed);
            }
        }

        return fieldSet.isEmpty() ? null : fieldSet;
    }

    /**
     * Filter metrics map by field names.
     *
     * @param metrics Original metrics map
     * @param fields Field names to keep
     * @return Filtered metrics map
     */
    private Map<String, MetricSample> filterByFields(
            Map<String, MetricSample> metrics,
            Set<String> fields) {

        if (fields == null || fields.isEmpty()) {
            return metrics;
        }

        Map<String, MetricSample> filtered = new HashMap<>();
        for (Map.Entry<String, MetricSample> entry : metrics.entrySet()) {
            if (fields.contains(entry.getKey())) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }

        return filtered;
    }
}
