package com.cmict.metricshub.repository;

import com.cmict.metricshub.model.ServerMetricSample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for ServerMetricSample entities.
 *
 * Provides data access methods for querying time-series metric data
 * from PostgreSQL/Timescale.
 */
@Repository
public interface ServerMetricSampleRepository extends JpaRepository<ServerMetricSample, Long> {

    /**
     * Find all metrics for a server within a time range.
     *
     * @param serverId Server identifier
     * @param from Start time (inclusive)
     * @param to End time (inclusive)
     * @return List of metric samples ordered by timestamp DESC
     */
    List<ServerMetricSample> findByServerIdAndTimestampBetweenOrderByTimestampDesc(
            String serverId, Instant from, Instant to);

    /**
     * Find specific metric for a server within a time range.
     *
     * @param serverId Server identifier
     * @param metricName Metric name
     * @param from Start time
     * @param to End time
     * @return List of metric samples
     */
    List<ServerMetricSample> findByServerIdAndMetricNameAndTimestampBetween(
            String serverId, String metricName, Instant from, Instant to);

    /**
     * Get latest metric value for a server.
     *
     * @param serverId Server identifier
     * @param metricName Metric name
     * @return Latest metric sample, or null if not found
     */
    @Query("SELECT s FROM ServerMetricSample s WHERE s.serverId = :serverId " +
            "AND s.metricName = :metricName ORDER BY s.timestamp DESC LIMIT 1")
    ServerMetricSample findLatestByServerIdAndMetricName(
            @Param("serverId") String serverId,
            @Param("metricName") String metricName);

    /**
     * Delete old metrics before a given timestamp.
     * Used for data retention policy.
     *
     * @param before Delete samples older than this timestamp
     * @return Number of deleted records
     */
    @Query("DELETE FROM ServerMetricSample s WHERE s.timestamp < :before")
    int deleteByTimestampBefore(@Param("before") Instant before);

    /**
     * Count metrics for a server.
     *
     * @param serverId Server identifier
     * @return Total count
     */
    long countByServerId(String serverId);
}
