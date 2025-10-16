package com.cmict.metricshub.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Internal metric sample model.
 *
 * This represents a normalized metric data point after OTLP decoding and normalization.
 * All metrics are converted to standard units and filtered labels.
 *
 * Supported metric types:
 * - GAUGE: Instant value (e.g., CPU usage, memory usage)
 * - COUNTER: Cumulative monotonic value (e.g., bytes sent, requests)
 * - HISTOGRAM: Distribution of values (e.g., latency)
 *
 * Standard units:
 * - bytes (for memory, disk, network)
 * - seconds (for time)
 * - percent (for ratios 0-100)
 * - count (for counters)
 *
 * Whitelisted labels:
 * - server.id: Server identifier
 * - region: Deployment region
 * - group: Server group
 * - env: Environment (dev/staging/prod)
 */
public class MetricSample {

    /**
     * Metric types supported by the system.
     */
    public enum MetricType {
        GAUGE,      // Instant value
        COUNTER,    // Cumulative monotonic value
        HISTOGRAM   // Distribution of values
    }

    private String name;                    // Metric name (e.g., "system.cpu.usage")
    private MetricType type;                // Metric type
    private double value;                   // Metric value
    private String unit;                    // Standard unit (bytes/seconds/percent/count)
    private Instant timestamp;              // Sample timestamp
    private Map<String, String> labels;     // Filtered labels (whitelist only)

    // Additional fields for histograms
    private Long count;                     // Number of samples (for histogram)
    private Double sum;                     // Sum of values (for histogram)

    public MetricSample() {
        this.labels = new HashMap<>();
        this.timestamp = Instant.now();
    }

    public MetricSample(String name, MetricType type, double value, String unit) {
        this();
        this.name = name;
        this.type = type;
        this.value = value;
        this.unit = unit;
    }

    // Builder pattern for convenient construction
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private MetricSample sample = new MetricSample();

        public Builder name(String name) {
            sample.name = name;
            return this;
        }

        public Builder type(MetricType type) {
            sample.type = type;
            return this;
        }

        public Builder value(double value) {
            sample.value = value;
            return this;
        }

        public Builder unit(String unit) {
            sample.unit = unit;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            sample.timestamp = timestamp;
            return this;
        }

        public Builder timestamp(long epochMilli) {
            sample.timestamp = Instant.ofEpochMilli(epochMilli);
            return this;
        }

        public Builder label(String key, String value) {
            sample.labels.put(key, value);
            return this;
        }

        public Builder labels(Map<String, String> labels) {
            sample.labels.putAll(labels);
            return this;
        }

        public Builder count(Long count) {
            sample.count = count;
            return this;
        }

        public Builder sum(Double sum) {
            sample.sum = sum;
            return this;
        }

        public MetricSample build() {
            // Validation
            Objects.requireNonNull(sample.name, "Metric name is required");
            Objects.requireNonNull(sample.type, "Metric type is required");
            Objects.requireNonNull(sample.unit, "Metric unit is required");
            return sample;
        }
    }

    // Getters and setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MetricType getType() {
        return type;
    }

    public void setType(MetricType type) {
        this.type = type;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
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

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public void addLabel(String key, String value) {
        this.labels.put(key, value);
    }

    public String getLabel(String key) {
        return labels.get(key);
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MetricSample{");
        sb.append("name='").append(name).append('\'');
        sb.append(", type=").append(type);
        sb.append(", value=").append(value);
        sb.append(", unit='").append(unit).append('\'');
        sb.append(", timestamp=").append(timestamp);
        if (!labels.isEmpty()) {
            sb.append(", labels=").append(labels);
        }
        if (count != null) {
            sb.append(", count=").append(count);
        }
        if (sum != null) {
            sb.append(", sum=").append(sum);
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetricSample that = (MetricSample) o;
        return Double.compare(that.value, value) == 0 &&
                Objects.equals(name, that.name) &&
                type == that.type &&
                Objects.equals(unit, that.unit) &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(labels, that.labels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, value, unit, timestamp, labels);
    }
}
