package com.cmict.metricshub.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Smart data source selector for metric queries.
 *
 * Routing strategy (with rollup optimization):
 * - Very recent data (last 5 minutes): Redis (hot storage)
 * - Recent data (5 min - 24 hours): TSDB raw data
 * - Medium-term data (1-7 days): TSDB 5-minute rollup
 * - Long-term data (>7 days): TSDB 1-hour rollup
 * - No time range specified: Redis (latest values)
 *
 * This ensures:
 * - Low latency for real-time queries (Redis)
 * - Full granularity for recent history (TSDB raw)
 * - Fast queries for medium-term analysis (5m rollup)
 * - Efficient storage for long-term trends (1h rollup)
 * - Optimal resource utilization
 */
@Component
public class MetricDataSourceSelector {

    /**
     * Data sources available for metric queries.
     */
    public enum DataSource {
        REDIS,       // Hot storage - latest values (last 5 minutes)
        TSDB_RAW,    // Cold storage - raw historical data (5min - 24h)
        ROLLUP_5M,   // Medium-term storage - 5-minute aggregations (1-7 days)
        ROLLUP_1H    // Long-term storage - 1-hour aggregations (>7 days)
    }

    @Value("${metrics-hub.redis.ttl-seconds:300}")
    private int redisTtlSeconds; // Default: 5 minutes

    @Value("${metrics-hub.rollup.enabled:false}")
    private boolean rollupEnabled;

    // Thresholds for rollup routing
    private static final int TSDB_RAW_HOURS = 24;      // Use raw data for queries up to 24 hours
    private static final int ROLLUP_5M_DAYS = 7;       // Use 5m rollup for queries up to 7 days

    /**
     * Select the appropriate data source based on the query time range.
     *
     * Routing logic:
     * 1. No time range → Redis (latest values)
     * 2. Query start within last 5 minutes → Redis
     * 3. Query range <= 24 hours → TSDB raw data (full granularity)
     * 4. Query range <= 7 days → 5-minute rollup (if enabled)
     * 5. Query range > 7 days → 1-hour rollup (if enabled)
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

        // If rollup is not enabled, always use raw TSDB
        if (!rollupEnabled) {
            return DataSource.TSDB_RAW;
        }

        // Calculate query range duration
        Instant queryStart = from != null ? from : now.minus(Duration.ofHours(1));
        Instant queryEnd = to != null ? to : now;
        Duration queryRange = Duration.between(queryStart, queryEnd);

        // Route based on query range
        if (queryRange.toHours() <= TSDB_RAW_HOURS) {
            // Recent queries (up to 24 hours) → use raw data for full granularity
            return DataSource.TSDB_RAW;
        } else if (queryRange.toDays() <= ROLLUP_5M_DAYS) {
            // Medium-term queries (1-7 days) → use 5-minute rollup
            return DataSource.ROLLUP_5M;
        } else {
            // Long-term queries (>7 days) → use 1-hour rollup
            return DataSource.ROLLUP_1H;
        }
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
