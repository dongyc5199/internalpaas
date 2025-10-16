package com.cmict.metricshub.processing;

import com.cmict.metricshub.model.MetricSample;
import com.cmict.metricshub.model.MetricSample.MetricType;
import io.opentelemetry.proto.collector.metrics.v1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OTLP metrics decoder.
 *
 * This component is responsible for decoding OTLP protobuf payloads into
 * internal MetricSample objects.
 *
 * Decoding process:
 * 1. Parse OTLP ExportMetricsServiceRequest from bytes
 * 2. Iterate through ResourceMetrics → ScopeMetrics → Metrics
 * 3. Extract metric name, type, value, timestamp, and labels
 * 4. Convert to MetricSample objects
 *
 * Supported metric types:
 * - Gauge: system.cpu.usage, system.memory.usage
 * - Sum: system.network.bytes_sent, system.disk.operations
 * - Histogram: http.server.duration
 */
@Component
public class OtlpDecoder {

    private static final Logger logger = LoggerFactory.getLogger(OtlpDecoder.class);

    /**
     * Decode OTLP protobuf bytes into a list of MetricSample objects.
     *
     * @param otlpBytes Raw OTLP protobuf bytes
     * @return List of decoded metric samples
     * @throws DecoderException if decoding fails
     */
    public List<MetricSample> decode(byte[] otlpBytes) {
        List<MetricSample> samples = new ArrayList<>();

        try {
            // Parse OTLP request
            ExportMetricsServiceRequest request =
                    ExportMetricsServiceRequest.parseFrom(otlpBytes);

            logger.debug("Decoding OTLP request with {} resource metrics",
                    request.getResourceMetricsCount());

            // Iterate through resource metrics
            for (ResourceMetrics resourceMetrics : request.getResourceMetricsList()) {
                // Extract resource labels (e.g., host, service name)
                Map<String, String> resourceLabels = extractResourceLabels(resourceMetrics);

                // Iterate through scope metrics
                for (ScopeMetrics scopeMetrics : resourceMetrics.getScopeMetricsList()) {
                    // Iterate through individual metrics
                    for (Metric metric : scopeMetrics.getMetricsList()) {
                        List<MetricSample> metricSamples = decodeMetric(metric, resourceLabels);
                        samples.addAll(metricSamples);
                    }
                }
            }

            logger.debug("Successfully decoded {} metric samples", samples.size());
            return samples;

        } catch (Exception e) {
            logger.error("Failed to decode OTLP payload: {}", e.getMessage(), e);
            throw new DecoderException("Failed to decode OTLP payload", e);
        }
    }

    /**
     * Extract resource-level labels (e.g., host, service.name).
     */
    private Map<String, String> extractResourceLabels(ResourceMetrics resourceMetrics) {
        Map<String, String> labels = new HashMap<>();

        if (resourceMetrics.hasResource()) {
            Resource resource = resourceMetrics.getResource();
            for (KeyValue kv : resource.getAttributesList()) {
                String key = kv.getKey();
                String value = extractValue(kv.getValue());
                if (value != null) {
                    labels.put(key, value);
                }
            }
        }

        return labels;
    }

    /**
     * Decode a single OTLP metric into one or more MetricSample objects.
     */
    private List<MetricSample> decodeMetric(Metric metric, Map<String, String> resourceLabels) {
        List<MetricSample> samples = new ArrayList<>();

        String metricName = metric.getName();
        String metricUnit = metric.getUnit();
        String description = metric.getDescription();

        logger.debug("Decoding metric: name={}, unit={}, type={}",
                metricName, metricUnit, metric.getDataCase());

        try {
            switch (metric.getDataCase()) {
                case GAUGE:
                    samples.addAll(decodeGauge(metricName, metricUnit, metric.getGauge(), resourceLabels));
                    break;

                case SUM:
                    samples.addAll(decodeSum(metricName, metricUnit, metric.getSum(), resourceLabels));
                    break;

                case HISTOGRAM:
                    samples.addAll(decodeHistogram(metricName, metricUnit, metric.getHistogram(), resourceLabels));
                    break;

                default:
                    logger.debug("Unsupported metric type: {} for metric {}",
                            metric.getDataCase(), metricName);
                    break;
            }
        } catch (Exception e) {
            logger.warn("Failed to decode metric {}: {}", metricName, e.getMessage());
        }

        return samples;
    }

    /**
     * Decode Gauge metrics (instant values).
     */
    private List<MetricSample> decodeGauge(String name, String unit, Gauge gauge,
                                            Map<String, String> resourceLabels) {
        List<MetricSample> samples = new ArrayList<>();

        for (NumberDataPoint dp : gauge.getDataPointsList()) {
            MetricSample sample = MetricSample.builder()
                    .name(name)
                    .type(MetricType.GAUGE)
                    .value(extractNumberValue(dp))
                    .unit(unit != null && !unit.isEmpty() ? unit : "1")  // Default unit
                    .timestamp(extractTimestamp(dp))
                    .labels(mergeLabels(resourceLabels, extractDataPointLabels(dp)))
                    .build();

            samples.add(sample);
        }

        return samples;
    }

    /**
     * Decode Sum metrics (cumulative counters).
     */
    private List<MetricSample> decodeSum(String name, String unit, Sum sum,
                                          Map<String, String> resourceLabels) {
        List<MetricSample> samples = new ArrayList<>();

        for (NumberDataPoint dp : sum.getDataPointsList()) {
            MetricSample sample = MetricSample.builder()
                    .name(name)
                    .type(MetricType.COUNTER)
                    .value(extractNumberValue(dp))
                    .unit(unit != null && !unit.isEmpty() ? unit : "1")
                    .timestamp(extractTimestamp(dp))
                    .labels(mergeLabels(resourceLabels, extractDataPointLabels(dp)))
                    .build();

            samples.add(sample);
        }

        return samples;
    }

    /**
     * Decode Histogram metrics (distributions).
     */
    private List<MetricSample> decodeHistogram(String name, String unit, Histogram histogram,
                                                Map<String, String> resourceLabels) {
        List<MetricSample> samples = new ArrayList<>();

        for (HistogramDataPoint dp : histogram.getDataPointsList()) {
            MetricSample sample = MetricSample.builder()
                    .name(name)
                    .type(MetricType.HISTOGRAM)
                    .value(dp.getSum())  // Use sum as primary value
                    .unit(unit != null && !unit.isEmpty() ? unit : "1")
                    .timestamp(extractTimestampFromHistogram(dp))
                    .labels(mergeLabels(resourceLabels, extractHistogramLabels(dp)))
                    .count(dp.getCount())
                    .sum(dp.getSum())
                    .build();

            samples.add(sample);
        }

        return samples;
    }

    /**
     * Extract numeric value from NumberDataPoint.
     */
    private double extractNumberValue(NumberDataPoint dp) {
        switch (dp.getValueCase()) {
            case AS_DOUBLE:
                return dp.getAsDouble();
            case AS_INT:
                return (double) dp.getAsInt();
            default:
                return 0.0;
        }
    }

    /**
     * Extract timestamp from NumberDataPoint.
     */
    private Instant extractTimestamp(NumberDataPoint dp) {
        long nanos = dp.getTimeUnixNano();
        if (nanos > 0) {
            return Instant.ofEpochSecond(nanos / 1_000_000_000, nanos % 1_000_000_000);
        }
        return Instant.now();
    }

    /**
     * Extract timestamp from HistogramDataPoint.
     */
    private Instant extractTimestampFromHistogram(HistogramDataPoint dp) {
        long nanos = dp.getTimeUnixNano();
        if (nanos > 0) {
            return Instant.ofEpochSecond(nanos / 1_000_000_000, nanos % 1_000_000_000);
        }
        return Instant.now();
    }

    /**
     * Extract labels from NumberDataPoint.
     */
    private Map<String, String> extractDataPointLabels(NumberDataPoint dp) {
        Map<String, String> labels = new HashMap<>();
        for (KeyValue kv : dp.getAttributesList()) {
            String value = extractValue(kv.getValue());
            if (value != null) {
                labels.put(kv.getKey(), value);
            }
        }
        return labels;
    }

    /**
     * Extract labels from HistogramDataPoint.
     */
    private Map<String, String> extractHistogramLabels(HistogramDataPoint dp) {
        Map<String, String> labels = new HashMap<>();
        for (KeyValue kv : dp.getAttributesList()) {
            String value = extractValue(kv.getValue());
            if (value != null) {
                labels.put(kv.getKey(), value);
            }
        }
        return labels;
    }

    /**
     * Extract string value from AnyValue.
     */
    private String extractValue(AnyValue anyValue) {
        switch (anyValue.getValueCase()) {
            case STRING_VALUE:
                return anyValue.getStringValue();
            case BOOL_VALUE:
                return String.valueOf(anyValue.getBoolValue());
            case INT_VALUE:
                return String.valueOf(anyValue.getIntValue());
            case DOUBLE_VALUE:
                return String.valueOf(anyValue.getDoubleValue());
            default:
                return null;
        }
    }

    /**
     * Merge resource labels and data point labels.
     */
    private Map<String, String> mergeLabels(Map<String, String> resourceLabels,
                                             Map<String, String> dataPointLabels) {
        Map<String, String> merged = new HashMap<>(resourceLabels);
        merged.putAll(dataPointLabels);  // Data point labels override resource labels
        return merged;
    }

    /**
     * Exception thrown when decoding fails.
     */
    public static class DecoderException extends RuntimeException {
        public DecoderException(String message) {
            super(message);
        }

        public DecoderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
