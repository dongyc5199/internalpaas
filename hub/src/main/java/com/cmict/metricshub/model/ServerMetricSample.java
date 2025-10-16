package com.cmict.metricshub.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

/**
 * JPA entity for storing metric samples in PostgreSQL/Timescale.
 *
 * This entity represents a single metric sample that has been normalized
 * and is ready for long-term storage in the time-series database.
 *
 * Table structure optimized for Timescale hypertable:
 * - Primary key: (id) for uniqueness
 * - Time column: timestamp for time-series partitioning
 * - Indexed columns: server_id, metric_name for fast queries
 *
 * Typical query patterns:
 * - SELECT * FROM server_metric_samples WHERE server_id = ? AND timestamp BETWEEN ? AND ?
 * - SELECT * FROM server_metric_samples WHERE metric_name = ? AND timestamp > ?
 */
@Entity
@Table(name = "server_metric_samples",
        indexes = {
                @Index(name = "idx_server_timestamp", columnList = "server_id, timestamp DESC"),
                @Index(name = "idx_metric_timestamp", columnList = "metric_name, timestamp DESC"),
                @Index(name = "idx_timestamp", columnList = "timestamp DESC")
        })
public class ServerMetricSample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Server identifier (from labels).
     * Required field for partitioning and query optimization.
     */
    @Column(name = "server_id", nullable = false, length = 100)
    private String serverId;

    /**
     * Metric name (e.g., "system.cpu.usage").
     */
    @Column(name = "metric_name", nullable = false, length = 255)
    private String metricName;

    /**
     * Metric type: GAUGE, COUNTER, HISTOGRAM.
     */
    @Column(name = "metric_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private MetricSample.MetricType metricType;

    /**
     * Normalized metric value.
     */
    @Column(name = "metric_value", nullable = false)
    private Double value;

    /**
     * Normalized unit (bytes/seconds/percent/count).
     */
    @Column(name = "unit", nullable = false, length = 50)
    private String unit;

    /**
     * Sample timestamp (UTC).
     * This column is used as the time dimension for Timescale hypertable.
     */
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    /**
     * Additional labels stored as JSON (PostgreSQL JSONB).
     * Contains: region, group, env, etc.
     */
    @Column(name = "labels", columnDefinition = "TEXT")
    private String labels; // Stored as JSON string

    /**
     * Histogram count (only for HISTOGRAM type).
     */
    @Column(name = "count")
    private Long count;

    /**
     * Histogram sum (only for HISTOGRAM type).
     */
    @Column(name = "sum")
    private Double sum;

    /**
     * Record creation time (for audit).
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    // Constructors

    public ServerMetricSample() {
    }

    public ServerMetricSample(String serverId, String metricName,
                               MetricSample.MetricType metricType,
                               Double value, String unit, Instant timestamp) {
        this.serverId = serverId;
        this.metricName = metricName;
        this.metricType = metricType;
        this.value = value;
        this.unit = unit;
        this.timestamp = timestamp;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public MetricSample.MetricType getMetricType() {
        return metricType;
    }

    public void setMetricType(MetricSample.MetricType metricType) {
        this.metricType = metricType;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getLabels() {
        return labels;
    }

    public void setLabels(String labels) {
        this.labels = labels;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public Double getSum() {
        return sum;
    }

    public void setSum(Double sum) {
        this.sum = sum;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerMetricSample that = (ServerMetricSample) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ServerMetricSample{" +
                "id=" + id +
                ", serverId='" + serverId + '\'' +
                ", metricName='" + metricName + '\'' +
                ", metricType=" + metricType +
                ", value=" + value +
                ", unit='" + unit + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
