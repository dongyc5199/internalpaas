package com.cmict.metricshub.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Rollup Job Scheduler for data aggregation
 * 
 * Automatically aggregates raw metric samples into:
 * - 5-minute rollup: Every 5 minutes, aggregates data from 6-11 minutes ago
 * - 1-hour rollup: Every hour, aggregates data from the past 2 hours
 * 
 * This reduces storage requirements and improves query performance for historical data.
 * 
 * Enable with: metrics.hub.rollup.enabled=true
 * 
 * @author Dev Debug Platform Team
 * @version 1.0 (T5: Data Aggregation)
 */
@Component
@ConditionalOnProperty(name = "metrics.hub.rollup.enabled", havingValue = "true", matchIfMissing = false)
public class RollupJobScheduler {

    private static final Logger logger = LoggerFactory.getLogger(RollupJobScheduler.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 5-minute rollup job
     * 
     * Runs every 5 minutes and aggregates data from 6-11 minutes ago.
     * The 1-minute buffer allows late-arriving data to be included.
     * 
     * Cron: "0 *\/5 * * * *" = Every 5 minutes at :00 seconds
     */
    @Scheduled(cron = "${metrics.hub.rollup.5m.cron:0 */5 * * * *}")
    @Transactional
    public void rollup5Minutes() {
        long jobStartTime = System.currentTimeMillis();
        
        try {
            // Calculate time range: 6-11 minutes ago
            Instant now = Instant.now();
            Instant rangeEnd = now.minus(6, ChronoUnit.MINUTES);
            Instant rangeStart = now.minus(11, ChronoUnit.MINUTES);
            
            logger.info("🔄 Starting 5-minute rollup - Range: {} to {}", rangeStart, rangeEnd);
            
            // SQL for 5-minute aggregation using time_bucket function
            // Compatible with both PostgreSQL (manual bucketing) and TimescaleDB (time_bucket)
            String sql = """
                INSERT INTO server_metric_samples_5m (server_id, metric_name, time_bucket, avg_value, max_value, min_value, sample_count)
                SELECT 
                    server_id,
                    metric_name,
                    date_trunc('minute', timestamp) - 
                        (EXTRACT(MINUTE FROM timestamp)::integer % 5 || ' minutes')::interval AS time_bucket,
                    AVG(metric_value) AS avg_value,
                    MAX(metric_value) AS max_value,
                    MIN(metric_value) AS min_value,
                    COUNT(*) AS sample_count
                FROM server_metric_samples
                WHERE timestamp >= ? AND timestamp < ?
                GROUP BY server_id, metric_name, time_bucket
                ON CONFLICT (server_id, metric_name, time_bucket) DO UPDATE SET
                    avg_value = EXCLUDED.avg_value,
                    max_value = EXCLUDED.max_value,
                    min_value = EXCLUDED.min_value,
                    sample_count = EXCLUDED.sample_count,
                    created_at = CURRENT_TIMESTAMP
                """;
            
            int rowsInserted = jdbcTemplate.update(sql, 
                    Timestamp.from(rangeStart), 
                    Timestamp.from(rangeEnd));
            
            long duration = System.currentTimeMillis() - jobStartTime;
            
            logger.info("✅ 5-minute rollup completed - Buckets created/updated: {}, Duration: {}ms", 
                       rowsInserted, duration);
            
        } catch (Exception e) {
            logger.error("❌ 5-minute rollup failed", e);
            throw e;  // Trigger transaction rollback
        }
    }

    /**
     * 1-hour rollup job
     * 
     * Runs every hour and aggregates data from the past 2 hours.
     * Can aggregate from either raw data or 5-minute rollup data.
     * 
     * Cron: "0 0 * * * *" = Every hour at :00:00
     */
    @Scheduled(cron = "${metrics.hub.rollup.1h.cron:0 0 * * * *}")
    @Transactional
    public void rollup1Hour() {
        long jobStartTime = System.currentTimeMillis();
        
        try {
            // Calculate time range: 1-3 hours ago
            Instant now = Instant.now();
            Instant rangeEnd = now.minus(1, ChronoUnit.HOURS);
            Instant rangeStart = now.minus(3, ChronoUnit.HOURS);
            
            logger.info("🔄 Starting 1-hour rollup - Range: {} to {}", rangeStart, rangeEnd);
            
            // Prefer aggregating from 5-minute rollup if available, otherwise use raw data
            String sql = """
                INSERT INTO server_metric_samples_1h (server_id, metric_name, time_bucket, avg_value, max_value, min_value, sample_count)
                SELECT 
                    server_id,
                    metric_name,
                    date_trunc('hour', time_bucket) AS time_bucket,
                    AVG(avg_value) AS avg_value,
                    MAX(max_value) AS max_value,
                    MIN(min_value) AS min_value,
                    SUM(sample_count) AS sample_count
                FROM server_metric_samples_5m
                WHERE time_bucket >= ? AND time_bucket < ?
                GROUP BY server_id, metric_name, time_bucket
                ON CONFLICT (server_id, metric_name, time_bucket) DO UPDATE SET
                    avg_value = EXCLUDED.avg_value,
                    max_value = EXCLUDED.max_value,
                    min_value = EXCLUDED.min_value,
                    sample_count = EXCLUDED.sample_count,
                    created_at = CURRENT_TIMESTAMP
                """;
            
            int rowsInserted = jdbcTemplate.update(sql, 
                    Timestamp.from(rangeStart), 
                    Timestamp.from(rangeEnd));
            
            long duration = System.currentTimeMillis() - jobStartTime;
            
            logger.info("✅ 1-hour rollup completed - Buckets created/updated: {}, Duration: {}ms", 
                       rowsInserted, duration);
            
        } catch (Exception e) {
            logger.error("❌ 1-hour rollup failed", e);
            throw e;  // Trigger transaction rollback
        }
    }

    /**
     * Refresh materialized view for rollup status monitoring
     * Runs every 10 minutes
     */
    @Scheduled(cron = "${metrics.hub.rollup.refresh-status.cron:0 */10 * * * *}")
    public void refreshRollupStatus() {
        try {
            logger.debug("🔄 Refreshing rollup status materialized view");
            
            jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY rollup_status");
            
            logger.debug("✅ Rollup status refreshed");
            
        } catch (Exception e) {
            logger.warn("⚠️ Failed to refresh rollup status: {}", e.getMessage());
            // Non-critical, don't throw
        }
    }

    /**
     * Get rollup job statistics (for monitoring/debugging)
     */
    public RollupStats getRollupStats() {
        try {
            String sql = """
                SELECT 
                    rollup_type,
                    latest_bucket,
                    total_buckets,
                    total_samples
                FROM rollup_status
                ORDER BY rollup_type
                """;
            
            return jdbcTemplate.query(sql, rs -> {
                RollupStats stats = new RollupStats();
                while (rs.next()) {
                    String type = rs.getString("rollup_type");
                    if ("5m".equals(type)) {
                        stats.setLatest5mBucket(rs.getTimestamp("latest_bucket").toInstant());
                        stats.setTotal5mBuckets(rs.getLong("total_buckets"));
                        stats.setTotal5mSamples(rs.getLong("total_samples"));
                    } else if ("1h".equals(type)) {
                        stats.setLatest1hBucket(rs.getTimestamp("latest_bucket").toInstant());
                        stats.setTotal1hBuckets(rs.getLong("total_buckets"));
                        stats.setTotal1hSamples(rs.getLong("total_samples"));
                    }
                }
                return stats;
            });
            
        } catch (Exception e) {
            logger.error("Failed to get rollup stats", e);
            return new RollupStats();
        }
    }

    /**
     * Rollup statistics DTO
     */
    public static class RollupStats {
        private Instant latest5mBucket;
        private Long total5mBuckets;
        private Long total5mSamples;
        private Instant latest1hBucket;
        private Long total1hBuckets;
        private Long total1hSamples;

        // Getters and setters
        public Instant getLatest5mBucket() { return latest5mBucket; }
        public void setLatest5mBucket(Instant latest5mBucket) { this.latest5mBucket = latest5mBucket; }
        
        public Long getTotal5mBuckets() { return total5mBuckets; }
        public void setTotal5mBuckets(Long total5mBuckets) { this.total5mBuckets = total5mBuckets; }
        
        public Long getTotal5mSamples() { return total5mSamples; }
        public void setTotal5mSamples(Long total5mSamples) { this.total5mSamples = total5mSamples; }
        
        public Instant getLatest1hBucket() { return latest1hBucket; }
        public void setLatest1hBucket(Instant latest1hBucket) { this.latest1hBucket = latest1hBucket; }
        
        public Long getTotal1hBuckets() { return total1hBuckets; }
        public void setTotal1hBuckets(Long total1hBuckets) { this.total1hBuckets = total1hBuckets; }
        
        public Long getTotal1hSamples() { return total1hSamples; }
        public void setTotal1hSamples(Long total1hSamples) { this.total1hSamples = total1hSamples; }
    }
}
