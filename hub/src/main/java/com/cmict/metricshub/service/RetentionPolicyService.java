package com.cmict.metricshub.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Retention Policy Service for automatic data cleanup
 * 
 * Applies time-based retention policies to metric data:
 * - Raw data (server_metric_samples): 30 days
 * - 5-minute rollup (server_metric_samples_5m): 90 days
 * - 1-hour rollup (server_metric_samples_1h): 365 days
 * 
 * Runs daily at 2:00 AM to clean up old data and reclaim storage space.
 * 
 * Enable with: metrics.hub.retention.enabled=true
 * 
 * @author Dev Debug Platform Team
 * @version 1.0 (T5: Data Retention)
 */
@Component
@ConditionalOnProperty(name = "metrics.hub.retention.enabled", havingValue = "true", matchIfMissing = false)
public class RetentionPolicyService {

    private static final Logger logger = LoggerFactory.getLogger(RetentionPolicyService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${metrics.hub.retention.raw-data-days:30}")
    private int rawDataRetentionDays;

    @Value("${metrics.hub.retention.5m-rollup-days:90}")
    private int rollup5mRetentionDays;

    @Value("${metrics.hub.retention.1h-rollup-days:365}")
    private int rollup1hRetentionDays;

    /**
     * Apply retention policy to all metric tables
     * 
     * Runs daily at 2:00 AM (configurable)
     * Cron: "0 0 2 * * *" = Every day at 02:00:00
     */
    @Scheduled(cron = "${metrics.hub.retention.cron:0 0 2 * * *}")
    @Transactional
    public void applyRetentionPolicy() {
        long startTime = System.currentTimeMillis();
        
        logger.info("🗑️ Starting retention policy application");
        
        try {
            // Delete old raw data
            int rawDeleted = deleteOldRawData();
            
            // Delete old 5-minute rollup data
            int rollup5mDeleted = deleteOld5mRollupData();
            
            // Delete old 1-hour rollup data
            int rollup1hDeleted = deleteOld1hRollupData();
            
            long duration = System.currentTimeMillis() - startTime;
            
            logger.info("✅ Retention policy completed - Raw: {} rows, 5m: {} rows, 1h: {} rows, Duration: {}ms",
                       rawDeleted, rollup5mDeleted, rollup1hDeleted, duration);
            
            // Optionally run VACUUM to reclaim disk space (PostgreSQL)
            if (rawDeleted + rollup5mDeleted + rollup1hDeleted > 1000) {
                vacuumTables();
            }
            
        } catch (Exception e) {
            logger.error("❌ Retention policy failed", e);
            throw e;  // Trigger transaction rollback
        }
    }

    /**
     * Delete raw data older than configured retention period (default: 30 days)
     */
    private int deleteOldRawData() {
        try {
            Instant cutoffTime = Instant.now().minus(rawDataRetentionDays, ChronoUnit.DAYS);
            
            String sql = "DELETE FROM server_metric_samples WHERE timestamp < ?";
            
            int rowsDeleted = jdbcTemplate.update(sql, Timestamp.from(cutoffTime));
            
            if (rowsDeleted > 0) {
                logger.info("🗑️ Deleted {} raw metric samples older than {} days", 
                           rowsDeleted, rawDataRetentionDays);
            } else {
                logger.debug("No raw data to delete (retention: {} days)", rawDataRetentionDays);
            }
            
            return rowsDeleted;
            
        } catch (Exception e) {
            logger.error("Failed to delete old raw data", e);
            throw e;
        }
    }

    /**
     * Delete 5-minute rollup data older than configured retention period (default: 90 days)
     */
    private int deleteOld5mRollupData() {
        try {
            Instant cutoffTime = Instant.now().minus(rollup5mRetentionDays, ChronoUnit.DAYS);
            
            String sql = "DELETE FROM server_metric_samples_5m WHERE time_bucket < ?";
            
            int rowsDeleted = jdbcTemplate.update(sql, Timestamp.from(cutoffTime));
            
            if (rowsDeleted > 0) {
                logger.info("🗑️ Deleted {} 5-minute rollup records older than {} days", 
                           rowsDeleted, rollup5mRetentionDays);
            } else {
                logger.debug("No 5-minute rollup data to delete (retention: {} days)", rollup5mRetentionDays);
            }
            
            return rowsDeleted;
            
        } catch (Exception e) {
            logger.error("Failed to delete old 5-minute rollup data", e);
            throw e;
        }
    }

    /**
     * Delete 1-hour rollup data older than configured retention period (default: 365 days)
     */
    private int deleteOld1hRollupData() {
        try {
            Instant cutoffTime = Instant.now().minus(rollup1hRetentionDays, ChronoUnit.DAYS);
            
            String sql = "DELETE FROM server_metric_samples_1h WHERE time_bucket < ?";
            
            int rowsDeleted = jdbcTemplate.update(sql, Timestamp.from(cutoffTime));
            
            if (rowsDeleted > 0) {
                logger.info("🗑️ Deleted {} 1-hour rollup records older than {} days", 
                           rowsDeleted, rollup1hRetentionDays);
            } else {
                logger.debug("No 1-hour rollup data to delete (retention: {} days)", rollup1hRetentionDays);
            }
            
            return rowsDeleted;
            
        } catch (Exception e) {
            logger.error("Failed to delete old 1-hour rollup data", e);
            throw e;
        }
    }

    /**
     * Run VACUUM to reclaim disk space after large deletions (PostgreSQL only)
     */
    private void vacuumTables() {
        try {
            logger.info("🧹 Running VACUUM to reclaim disk space...");
            
            // VACUUM cannot run inside a transaction, so we use autocommit
            jdbcTemplate.execute("VACUUM ANALYZE server_metric_samples");
            jdbcTemplate.execute("VACUUM ANALYZE server_metric_samples_5m");
            jdbcTemplate.execute("VACUUM ANALYZE server_metric_samples_1h");
            
            logger.info("✅ VACUUM completed");
            
        } catch (Exception e) {
            logger.warn("⚠️ VACUUM failed (may not be supported): {}", e.getMessage());
            // Non-critical, don't throw
        }
    }

    /**
     * Get retention policy statistics (for monitoring)
     */
    public RetentionStats getRetentionStats() {
        try {
            RetentionStats stats = new RetentionStats();
            
            // Count raw data
            stats.setRawDataCount(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM server_metric_samples", Long.class));
            stats.setOldestRawData(jdbcTemplate.queryForObject(
                    "SELECT MIN(timestamp) FROM server_metric_samples", Timestamp.class));
            
            // Count 5-minute rollup
            stats.setRollup5mCount(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM server_metric_samples_5m", Long.class));
            stats.setOldest5mRollup(jdbcTemplate.queryForObject(
                    "SELECT MIN(time_bucket) FROM server_metric_samples_5m", Timestamp.class));
            
            // Count 1-hour rollup
            stats.setRollup1hCount(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM server_metric_samples_1h", Long.class));
            stats.setOldest1hRollup(jdbcTemplate.queryForObject(
                    "SELECT MIN(time_bucket) FROM server_metric_samples_1h", Timestamp.class));
            
            stats.setRawDataRetentionDays(rawDataRetentionDays);
            stats.setRollup5mRetentionDays(rollup5mRetentionDays);
            stats.setRollup1hRetentionDays(rollup1hRetentionDays);
            
            return stats;
            
        } catch (Exception e) {
            logger.error("Failed to get retention stats", e);
            return new RetentionStats();
        }
    }

    /**
     * Retention statistics DTO
     */
    public static class RetentionStats {
        private Long rawDataCount;
        private Timestamp oldestRawData;
        private Integer rawDataRetentionDays;
        
        private Long rollup5mCount;
        private Timestamp oldest5mRollup;
        private Integer rollup5mRetentionDays;
        
        private Long rollup1hCount;
        private Timestamp oldest1hRollup;
        private Integer rollup1hRetentionDays;

        // Getters and setters
        public Long getRawDataCount() { return rawDataCount; }
        public void setRawDataCount(Long rawDataCount) { this.rawDataCount = rawDataCount; }
        
        public Timestamp getOldestRawData() { return oldestRawData; }
        public void setOldestRawData(Timestamp oldestRawData) { this.oldestRawData = oldestRawData; }
        
        public Integer getRawDataRetentionDays() { return rawDataRetentionDays; }
        public void setRawDataRetentionDays(Integer rawDataRetentionDays) { 
            this.rawDataRetentionDays = rawDataRetentionDays; 
        }
        
        public Long getRollup5mCount() { return rollup5mCount; }
        public void setRollup5mCount(Long rollup5mCount) { this.rollup5mCount = rollup5mCount; }
        
        public Timestamp getOldest5mRollup() { return oldest5mRollup; }
        public void setOldest5mRollup(Timestamp oldest5mRollup) { this.oldest5mRollup = oldest5mRollup; }
        
        public Integer getRollup5mRetentionDays() { return rollup5mRetentionDays; }
        public void setRollup5mRetentionDays(Integer rollup5mRetentionDays) { 
            this.rollup5mRetentionDays = rollup5mRetentionDays; 
        }
        
        public Long getRollup1hCount() { return rollup1hCount; }
        public void setRollup1hCount(Long rollup1hCount) { this.rollup1hCount = rollup1hCount; }
        
        public Timestamp getOldest1hRollup() { return oldest1hRollup; }
        public void setOldest1hRollup(Timestamp oldest1hRollup) { this.oldest1hRollup = oldest1hRollup; }
        
        public Integer getRollup1hRetentionDays() { return rollup1hRetentionDays; }
        public void setRollup1hRetentionDays(Integer rollup1hRetentionDays) { 
            this.rollup1hRetentionDays = rollup1hRetentionDays; 
        }
    }
}
