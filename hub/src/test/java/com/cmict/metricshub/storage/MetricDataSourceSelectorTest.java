package com.cmict.metricshub.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MetricDataSourceSelector - smart query routing.
 *
 * Tests the intelligent data source selection based on query time range:
 * - Redis: Very recent data (last 5 minutes)
 * - TSDB Raw: Recent data (5 min - 24 hours)
 * - 5m Rollup: Medium-term data (1-7 days)
 * - 1h Rollup: Long-term data (>7 days)
 */
class MetricDataSourceSelectorTest {

    private MetricDataSourceSelector selector;

    @BeforeEach
    void setUp() {
        selector = new MetricDataSourceSelector();
        // Set default Redis TTL: 5 minutes (300 seconds)
        ReflectionTestUtils.setField(selector, "redisTtlSeconds", 300);
        // Enable rollup
        ReflectionTestUtils.setField(selector, "rollupEnabled", true);
    }

    @Test
    void testNoTimeRange_UsesRedis() {
        // No time range specified → should use Redis for latest values
        MetricDataSourceSelector.DataSource dataSource = selector.selectDataSource(null, null);
        assertEquals(MetricDataSourceSelector.DataSource.REDIS, dataSource);
    }

    @Test
    void testVeryRecentData_UsesRedis() {
        // Query last 2 minutes → should use Redis (hot data)
        Instant now = Instant.now();
        Instant from = now.minus(Duration.ofMinutes(2));

        MetricDataSourceSelector.DataSource dataSource = selector.selectDataSource(from, now);
        assertEquals(MetricDataSourceSelector.DataSource.REDIS, dataSource);
    }

    @Test
    void testRecentData_UsesTsdbRaw() {
        // Query last 2 hours → should use TSDB raw data (full granularity)
        Instant now = Instant.now();
        Instant from = now.minus(Duration.ofHours(2));

        MetricDataSourceSelector.DataSource dataSource = selector.selectDataSource(from, now);
        assertEquals(MetricDataSourceSelector.DataSource.TSDB_RAW, dataSource);
    }

    @Test
    void testLast24Hours_UsesTsdbRaw() {
        // Query last 24 hours → should use TSDB raw data (boundary case)
        Instant now = Instant.now();
        Instant from = now.minus(Duration.ofHours(24));

        MetricDataSourceSelector.DataSource dataSource = selector.selectDataSource(from, now);
        assertEquals(MetricDataSourceSelector.DataSource.TSDB_RAW, dataSource);
    }

    @Test
    void testMediumTermData_Uses5mRollup() {
        // Query last 3 days → should use 5-minute rollup
        Instant now = Instant.now();
        Instant from = now.minus(Duration.ofDays(3));

        MetricDataSourceSelector.DataSource dataSource = selector.selectDataSource(from, now);
        assertEquals(MetricDataSourceSelector.DataSource.ROLLUP_5M, dataSource);
    }

    @Test
    void testLast7Days_Uses5mRollup() {
        // Query last 7 days → should use 5-minute rollup (boundary case)
        Instant now = Instant.now();
        Instant from = now.minus(Duration.ofDays(7));

        MetricDataSourceSelector.DataSource dataSource = selector.selectDataSource(from, now);
        assertEquals(MetricDataSourceSelector.DataSource.ROLLUP_5M, dataSource);
    }

    @Test
    void testLongTermData_Uses1hRollup() {
        // Query last 30 days → should use 1-hour rollup
        Instant now = Instant.now();
        Instant from = now.minus(Duration.ofDays(30));

        MetricDataSourceSelector.DataSource dataSource = selector.selectDataSource(from, now);
        assertEquals(MetricDataSourceSelector.DataSource.ROLLUP_1H, dataSource);
    }

    @Test
    void testVeryLongTermData_Uses1hRollup() {
        // Query last 90 days → should use 1-hour rollup
        Instant now = Instant.now();
        Instant from = now.minus(Duration.ofDays(90));

        MetricDataSourceSelector.DataSource dataSource = selector.selectDataSource(from, now);
        assertEquals(MetricDataSourceSelector.DataSource.ROLLUP_1H, dataSource);
    }

    @Test
    void testRollupDisabled_AlwaysUsesTsdbRaw() {
        // Disable rollup
        ReflectionTestUtils.setField(selector, "rollupEnabled", false);

        // Even for long-term queries, should use TSDB raw when rollup is disabled
        Instant now = Instant.now();
        Instant from = now.minus(Duration.ofDays(30));

        MetricDataSourceSelector.DataSource dataSource = selector.selectDataSource(from, now);
        assertEquals(MetricDataSourceSelector.DataSource.TSDB_RAW, dataSource);
    }

    @Test
    void testBoundary_JustOver5Minutes_UsesTsdbRaw() {
        // Query from 6 minutes ago to now → should use TSDB raw (just outside Redis window)
        Instant now = Instant.now();
        Instant from = now.minus(Duration.ofMinutes(6));

        MetricDataSourceSelector.DataSource dataSource = selector.selectDataSource(from, now);
        assertEquals(MetricDataSourceSelector.DataSource.TSDB_RAW, dataSource);
    }

    @Test
    void testBoundary_JustOver24Hours_Uses5mRollup() {
        // Query from 25 hours ago to now → should use 5m rollup (just outside TSDB raw window)
        Instant now = Instant.now();
        Instant from = now.minus(Duration.ofHours(25));

        MetricDataSourceSelector.DataSource dataSource = selector.selectDataSource(from, now);
        assertEquals(MetricDataSourceSelector.DataSource.ROLLUP_5M, dataSource);
    }

    @Test
    void testBoundary_JustOver7Days_Uses1hRollup() {
        // Query from 8 days ago to now → should use 1h rollup (just outside 5m rollup window)
        Instant now = Instant.now();
        Instant from = now.minus(Duration.ofDays(8));

        MetricDataSourceSelector.DataSource dataSource = selector.selectDataSource(from, now);
        assertEquals(MetricDataSourceSelector.DataSource.ROLLUP_1H, dataSource);
    }

    @Test
    void testHistoricalQuery_WithinRedisWindow_UsesRedis() {
        // Even if 'to' is in the past, if 'from' is within Redis window, use Redis
        Instant now = Instant.now();
        Instant from = now.minus(Duration.ofMinutes(3));
        Instant to = now.minus(Duration.ofMinutes(1));

        MetricDataSourceSelector.DataSource dataSource = selector.selectDataSource(from, to);
        assertEquals(MetricDataSourceSelector.DataSource.REDIS, dataSource);
    }

    @Test
    void testIsHotData_RecentTimestamp_ReturnsTrue() {
        Instant recent = Instant.now().minus(Duration.ofMinutes(2));
        assertTrue(selector.isHotData(recent));
    }

    @Test
    void testIsHotData_OldTimestamp_ReturnsFalse() {
        Instant old = Instant.now().minus(Duration.ofMinutes(10));
        assertFalse(selector.isHotData(old));
    }

    @Test
    void testIsHotData_NullTimestamp_ReturnsFalse() {
        assertFalse(selector.isHotData(null));
    }

    @Test
    void testGetHotDataThreshold() {
        Instant threshold = selector.getHotDataThreshold();
        Instant expected = Instant.now().minus(Duration.ofSeconds(300));

        // Allow 1 second tolerance for test execution time
        long diffSeconds = Math.abs(Duration.between(threshold, expected).toSeconds());
        assertTrue(diffSeconds <= 1, "Threshold should be ~5 minutes ago");
    }

    @Test
    void testGetHotDataWindow() {
        Duration window = selector.getHotDataWindow();
        assertEquals(Duration.ofSeconds(300), window);
    }

    @Test
    void testCustomRedisTtl() {
        // Set custom Redis TTL: 10 minutes
        ReflectionTestUtils.setField(selector, "redisTtlSeconds", 600);

        Instant now = Instant.now();
        Instant from = now.minus(Duration.ofMinutes(8));

        // Should use Redis (within 10-minute window)
        MetricDataSourceSelector.DataSource dataSource = selector.selectDataSource(from, now);
        assertEquals(MetricDataSourceSelector.DataSource.REDIS, dataSource);
    }
}
