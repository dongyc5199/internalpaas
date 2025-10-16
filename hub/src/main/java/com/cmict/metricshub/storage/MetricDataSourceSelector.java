package com.cmict.metricshub.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Smart data source selector for metric queries.
 *
 * Routing strategy:
 * - Recent data (last 5 minutes): Redis (hot storage)
 * - Historical data (older than 5 minutes): TSDB (cold storage)
 * - No time range specified: Redis (latest values)
 *
 * This ensures:
 * - Low latency for real-time queries (Redis)
 * - Full historical data access (TSDB)
 * - Optimal resource utilization
 */
@Component
public class MetricDataSourceSelector {

    /**
     * Data sources available for metric queries.
     */
    public enum DataSource {
        REDIS,   // Hot storage - latest values
        TSDB     // Cold storage - historical data
    }

    @Value("${metrics-hub.redis.ttl-seconds:300}")
    private int redisTtlSeconds; // Default: 5 minutes

    /**
     * Select the appropriate data source based on the query time range.
     *
     * @param from Start time (null for latest)
     * @param to End time (null for latest)
     * @return DataSource to use for this query
     */
    public DataSource selectDataSource(Instant from, Instant to) {
        // No time range specified → use Redis for latest values
        if (from == null && to == null) {
            return DataSource.REDIS;
        }

        Instant now = Instant.now();
        Duration hotDataWindow = Duration.ofSeconds(redisTtlSeconds);
        Instant hotDataThreshold = now.minus(hotDataWindow);

        // Query entirely within hot data window → use Redis
        if (from != null && from.isAfter(hotDataThreshold)) {
            return DataSource.REDIS;
        }

        // Query involves historical data → use TSDB
        return DataSource.TSDB;
    }

    /**
     * Check if a timestamp is within the hot data window.
     *
     * @param timestamp Timestamp to check
     * @return true if timestamp is recent (within Redis TTL)
     */
    public boolean isHotData(Instant timestamp) {
        if (timestamp == null) {
            return false;
        }

        Instant now = Instant.now();
        Duration hotDataWindow = Duration.ofSeconds(redisTtlSeconds);
        Instant hotDataThreshold = now.minus(hotDataWindow);

        return timestamp.isAfter(hotDataThreshold);
    }

    /**
     * Get the hot data threshold timestamp.
     * Data older than this should be queried from TSDB.
     *
     * @return Threshold instant
     */
    public Instant getHotDataThreshold() {
        Instant now = Instant.now();
        Duration hotDataWindow = Duration.ofSeconds(redisTtlSeconds);
        return now.minus(hotDataWindow);
    }

    /**
     * Get the configured hot data window duration.
     *
     * @return Duration of hot data window (Redis TTL)
     */
    public Duration getHotDataWindow() {
        return Duration.ofSeconds(redisTtlSeconds);
    }
}
