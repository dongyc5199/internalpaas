package com.cmict.metricshub.processing;

import com.cmict.metricshub.model.MetricSample;
import com.cmict.metricshub.model.MetricSample.MetricType;
import io.opentelemetry.proto.collector.metrics.v1.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OtlpDecoder.
 *
 * Tests:
 * - Gauge metric decoding
 * - Sum (Counter) metric decoding
 * - Histogram metric decoding
 * - Resource label extraction
 * - Data point label extraction
 * - Label merging
 * - Timestamp extraction
 */
public class OtlpDecoderTest {

    private OtlpDecoder decoder;

    @BeforeEach
    public void setUp() {
        decoder = new OtlpDecoder();
    }

    // ============================================================
    // Gauge Metric Decoding Tests
    // ============================================================

    @Test
    public void testDecodeGaugeMetric_Double() throws Exception {
        // Arrange
        long nowNanos = System.currentTimeMillis() * 1_000_000;

        ExportMetricsServiceRequest request = ExportMetricsServiceRequest.newBuilder()
                .addResourceMetrics(ResourceMetrics.newBuilder()
                        .setResource(Resource.newBuilder()
                                .addAttributes(KeyValue.newBuilder()
                                        .setKey("service.name")
                                        .setValue(AnyValue.newBuilder().setStringValue("test-service").build())
                                        .build())
                                .build())
                        .addScopeMetrics(ScopeMetrics.newBuilder()
                                .addMetrics(Metric.newBuilder()
                                        .setName("system.cpu.usage")
                                        .setUnit("1")
                                        .setGauge(Gauge.newBuilder()
                                                .addDataPoints(NumberDataPoint.newBuilder()
                                                        .setAsDouble(0.75)
                                                        .setTimeUnixNano(nowNanos)
                                                        .addAttributes(KeyValue.newBuilder()
                                                                .setKey("cpu")
                                                                .setValue(AnyValue.newBuilder().setStringValue("0").build())
                                                                .build())
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        byte[] payload = request.toByteArray();

        // Act
        List<MetricSample> samples = decoder.decode(payload);

        // Assert
        assertEquals(1, samples.size());
        MetricSample sample = samples.get(0);
        assertEquals("system.cpu.usage", sample.getName());
        assertEquals(MetricType.GAUGE, sample.getType());
        assertEquals(0.75, sample.getValue(), 0.001);
        assertEquals("1", sample.getUnit());
        assertNotNull(sample.getTimestamp());
        assertEquals("test-service", sample.getLabel("service.name"));
        assertEquals("0", sample.getLabel("cpu"));
    }

    @Test
    public void testDecodeGaugeMetric_Int() throws Exception {
        // Arrange
        ExportMetricsServiceRequest request = ExportMetricsServiceRequest.newBuilder()
                .addResourceMetrics(ResourceMetrics.newBuilder()
                        .addScopeMetrics(ScopeMetrics.newBuilder()
                                .addMetrics(Metric.newBuilder()
                                        .setName("process.threads")
                                        .setUnit("count")
                                        .setGauge(Gauge.newBuilder()
                                                .addDataPoints(NumberDataPoint.newBuilder()
                                                        .setAsInt(42)
                                                        .setTimeUnixNano(System.currentTimeMillis() * 1_000_000)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        // Act
        List<MetricSample> samples = decoder.decode(request.toByteArray());

        // Assert
        assertEquals(1, samples.size());
        MetricSample sample = samples.get(0);
        assertEquals("process.threads", sample.getName());
        assertEquals(MetricType.GAUGE, sample.getType());
        assertEquals(42.0, sample.getValue(), 0.001);
        assertEquals("count", sample.getUnit());
    }

    // ============================================================
    // Sum (Counter) Metric Decoding Tests
    // ============================================================

    @Test
    public void testDecodeSumMetric() throws Exception {
        // Arrange
        ExportMetricsServiceRequest request = ExportMetricsServiceRequest.newBuilder()
                .addResourceMetrics(ResourceMetrics.newBuilder()
                        .addScopeMetrics(ScopeMetrics.newBuilder()
                                .addMetrics(Metric.newBuilder()
                                        .setName("system.network.io.bytes")
                                        .setUnit("bytes")
                                        .setSum(Sum.newBuilder()
                                                .addDataPoints(NumberDataPoint.newBuilder()
                                                        .setAsInt(1048576)
                                                        .setTimeUnixNano(System.currentTimeMillis() * 1_000_000)
                                                        .addAttributes(KeyValue.newBuilder()
                                                                .setKey("direction")
                                                                .setValue(AnyValue.newBuilder().setStringValue("sent").build())
                                                                .build())
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        // Act
        List<MetricSample> samples = decoder.decode(request.toByteArray());

        // Assert
        assertEquals(1, samples.size());
        MetricSample sample = samples.get(0);
        assertEquals("system.network.io.bytes", sample.getName());
        assertEquals(MetricType.COUNTER, sample.getType());
        assertEquals(1048576.0, sample.getValue(), 0.001);
        assertEquals("bytes", sample.getUnit());
        assertEquals("sent", sample.getLabel("direction"));
    }

    // ============================================================
    // Histogram Metric Decoding Tests
    // ============================================================

    @Test
    public void testDecodeHistogramMetric() throws Exception {
        // Arrange
        ExportMetricsServiceRequest request = ExportMetricsServiceRequest.newBuilder()
                .addResourceMetrics(ResourceMetrics.newBuilder()
                        .addScopeMetrics(ScopeMetrics.newBuilder()
                                .addMetrics(Metric.newBuilder()
                                        .setName("http.server.duration")
                                        .setUnit("ms")
                                        .setHistogram(Histogram.newBuilder()
                                                .addDataPoints(HistogramDataPoint.newBuilder()
                                                        .setCount(100)
                                                        .setSum(12500.0)
                                                        .setTimeUnixNano(System.currentTimeMillis() * 1_000_000)
                                                        .addAttributes(KeyValue.newBuilder()
                                                                .setKey("http.method")
                                                                .setValue(AnyValue.newBuilder().setStringValue("GET").build())
                                                                .build())
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        // Act
        List<MetricSample> samples = decoder.decode(request.toByteArray());

        // Assert
        assertEquals(1, samples.size());
        MetricSample sample = samples.get(0);
        assertEquals("http.server.duration", sample.getName());
        assertEquals(MetricType.HISTOGRAM, sample.getType());
        assertEquals(12500.0, sample.getValue(), 0.001); // Use sum as primary value
        assertEquals("ms", sample.getUnit());
        assertEquals(100L, sample.getCount());
        assertEquals(12500.0, sample.getSum(), 0.001);
        assertEquals("GET", sample.getLabel("http.method"));
    }

    // ============================================================
    // Resource Label Extraction Tests
    // ============================================================

    @Test
    public void testResourceLabelExtraction() throws Exception {
        // Arrange
        ExportMetricsServiceRequest request = ExportMetricsServiceRequest.newBuilder()
                .addResourceMetrics(ResourceMetrics.newBuilder()
                        .setResource(Resource.newBuilder()
                                .addAttributes(KeyValue.newBuilder()
                                        .setKey("service.name")
                                        .setValue(AnyValue.newBuilder().setStringValue("my-service").build())
                                        .build())
                                .addAttributes(KeyValue.newBuilder()
                                        .setKey("host.name")
                                        .setValue(AnyValue.newBuilder().setStringValue("server-001").build())
                                        .build())
                                .addAttributes(KeyValue.newBuilder()
                                        .setKey("service.version")
                                        .setValue(AnyValue.newBuilder().setStringValue("1.0.0").build())
                                        .build())
                                .build())
                        .addScopeMetrics(ScopeMetrics.newBuilder()
                                .addMetrics(Metric.newBuilder()
                                        .setName("system.cpu.usage")
                                        .setUnit("1")
                                        .setGauge(Gauge.newBuilder()
                                                .addDataPoints(NumberDataPoint.newBuilder()
                                                        .setAsDouble(0.5)
                                                        .setTimeUnixNano(System.currentTimeMillis() * 1_000_000)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        // Act
        List<MetricSample> samples = decoder.decode(request.toByteArray());

        // Assert
        assertEquals(1, samples.size());
        MetricSample sample = samples.get(0);
        assertEquals("my-service", sample.getLabel("service.name"));
        assertEquals("server-001", sample.getLabel("host.name"));
        assertEquals("1.0.0", sample.getLabel("service.version"));
    }

    // ============================================================
    // Label Merging Tests
    // ============================================================

    @Test
    public void testLabelMerging_DataPointOverridesResource() throws Exception {
        // Arrange - data point label should override resource label with same key
        ExportMetricsServiceRequest request = ExportMetricsServiceRequest.newBuilder()
                .addResourceMetrics(ResourceMetrics.newBuilder()
                        .setResource(Resource.newBuilder()
                                .addAttributes(KeyValue.newBuilder()
                                        .setKey("env")
                                        .setValue(AnyValue.newBuilder().setStringValue("dev").build())
                                        .build())
                                .build())
                        .addScopeMetrics(ScopeMetrics.newBuilder()
                                .addMetrics(Metric.newBuilder()
                                        .setName("system.cpu.usage")
                                        .setUnit("1")
                                        .setGauge(Gauge.newBuilder()
                                                .addDataPoints(NumberDataPoint.newBuilder()
                                                        .setAsDouble(0.5)
                                                        .setTimeUnixNano(System.currentTimeMillis() * 1_000_000)
                                                        .addAttributes(KeyValue.newBuilder()
                                                                .setKey("env")
                                                                .setValue(AnyValue.newBuilder().setStringValue("prod").build())
                                                                .build())
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        // Act
        List<MetricSample> samples = decoder.decode(request.toByteArray());

        // Assert - data point label "prod" should override resource label "dev"
        assertEquals(1, samples.size());
        assertEquals("prod", samples.get(0).getLabel("env"));
    }

    // ============================================================
    // Timestamp Extraction Tests
    // ============================================================

    @Test
    public void testTimestampExtraction() throws Exception {
        // Arrange
        long expectedMillis = 1704067200000L; // 2024-01-01 00:00:00 UTC
        long nanos = expectedMillis * 1_000_000;

        ExportMetricsServiceRequest request = ExportMetricsServiceRequest.newBuilder()
                .addResourceMetrics(ResourceMetrics.newBuilder()
                        .addScopeMetrics(ScopeMetrics.newBuilder()
                                .addMetrics(Metric.newBuilder()
                                        .setName("system.cpu.usage")
                                        .setUnit("1")
                                        .setGauge(Gauge.newBuilder()
                                                .addDataPoints(NumberDataPoint.newBuilder()
                                                        .setAsDouble(0.5)
                                                        .setTimeUnixNano(nanos)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        // Act
        List<MetricSample> samples = decoder.decode(request.toByteArray());

        // Assert
        assertEquals(1, samples.size());
        Instant timestamp = samples.get(0).getTimestamp();
        assertEquals(expectedMillis, timestamp.toEpochMilli());
    }

    @Test
    public void testTimestampExtraction_MissingTimestamp() throws Exception {
        // Arrange - no timestamp provided
        ExportMetricsServiceRequest request = ExportMetricsServiceRequest.newBuilder()
                .addResourceMetrics(ResourceMetrics.newBuilder()
                        .addScopeMetrics(ScopeMetrics.newBuilder()
                                .addMetrics(Metric.newBuilder()
                                        .setName("system.cpu.usage")
                                        .setUnit("1")
                                        .setGauge(Gauge.newBuilder()
                                                .addDataPoints(NumberDataPoint.newBuilder()
                                                        .setAsDouble(0.5)
                                                        // No setTimeUnixNano()
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        // Act
        long beforeDecode = System.currentTimeMillis();
        List<MetricSample> samples = decoder.decode(request.toByteArray());
        long afterDecode = System.currentTimeMillis();

        // Assert - should use current time
        assertEquals(1, samples.size());
        Instant timestamp = samples.get(0).getTimestamp();
        assertTrue(timestamp.toEpochMilli() >= beforeDecode);
        assertTrue(timestamp.toEpochMilli() <= afterDecode);
    }

    // ============================================================
    // Multiple Metrics Tests
    // ============================================================

    @Test
    public void testDecodeMultipleMetrics() throws Exception {
        // Arrange - multiple metrics in single request
        ExportMetricsServiceRequest request = ExportMetricsServiceRequest.newBuilder()
                .addResourceMetrics(ResourceMetrics.newBuilder()
                        .addScopeMetrics(ScopeMetrics.newBuilder()
                                .addMetrics(Metric.newBuilder()
                                        .setName("system.cpu.usage")
                                        .setUnit("1")
                                        .setGauge(Gauge.newBuilder()
                                                .addDataPoints(NumberDataPoint.newBuilder()
                                                        .setAsDouble(0.5)
                                                        .setTimeUnixNano(System.currentTimeMillis() * 1_000_000)
                                                        .build())
                                                .build())
                                        .build())
                                .addMetrics(Metric.newBuilder()
                                        .setName("system.memory.usage")
                                        .setUnit("bytes")
                                        .setGauge(Gauge.newBuilder()
                                                .addDataPoints(NumberDataPoint.newBuilder()
                                                        .setAsInt(1073741824)
                                                        .setTimeUnixNano(System.currentTimeMillis() * 1_000_000)
                                                        .build())
                                                .build())
                                        .build())
                                .addMetrics(Metric.newBuilder()
                                        .setName("system.network.io.bytes")
                                        .setUnit("bytes")
                                        .setSum(Sum.newBuilder()
                                                .addDataPoints(NumberDataPoint.newBuilder()
                                                        .setAsInt(2048)
                                                        .setTimeUnixNano(System.currentTimeMillis() * 1_000_000)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        // Act
        List<MetricSample> samples = decoder.decode(request.toByteArray());

        // Assert
        assertEquals(3, samples.size());
        assertEquals("system.cpu.usage", samples.get(0).getName());
        assertEquals(MetricType.GAUGE, samples.get(0).getType());
        assertEquals("system.memory.usage", samples.get(1).getName());
        assertEquals(MetricType.GAUGE, samples.get(1).getType());
        assertEquals("system.network.io.bytes", samples.get(2).getName());
        assertEquals(MetricType.COUNTER, samples.get(2).getType());
    }

    @Test
    public void testDecodeMultipleDataPoints() throws Exception {
        // Arrange - single metric with multiple data points
        ExportMetricsServiceRequest request = ExportMetricsServiceRequest.newBuilder()
                .addResourceMetrics(ResourceMetrics.newBuilder()
                        .addScopeMetrics(ScopeMetrics.newBuilder()
                                .addMetrics(Metric.newBuilder()
                                        .setName("system.cpu.usage")
                                        .setUnit("1")
                                        .setGauge(Gauge.newBuilder()
                                                .addDataPoints(NumberDataPoint.newBuilder()
                                                        .setAsDouble(0.5)
                                                        .setTimeUnixNano(System.currentTimeMillis() * 1_000_000)
                                                        .addAttributes(KeyValue.newBuilder()
                                                                .setKey("cpu")
                                                                .setValue(AnyValue.newBuilder().setStringValue("0").build())
                                                                .build())
                                                        .build())
                                                .addDataPoints(NumberDataPoint.newBuilder()
                                                        .setAsDouble(0.7)
                                                        .setTimeUnixNano(System.currentTimeMillis() * 1_000_000)
                                                        .addAttributes(KeyValue.newBuilder()
                                                                .setKey("cpu")
                                                                .setValue(AnyValue.newBuilder().setStringValue("1").build())
                                                                .build())
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        // Act
        List<MetricSample> samples = decoder.decode(request.toByteArray());

        // Assert - should create 2 samples
        assertEquals(2, samples.size());
        assertEquals("system.cpu.usage", samples.get(0).getName());
        assertEquals(0.5, samples.get(0).getValue(), 0.001);
        assertEquals("0", samples.get(0).getLabel("cpu"));
        assertEquals("system.cpu.usage", samples.get(1).getName());
        assertEquals(0.7, samples.get(1).getValue(), 0.001);
        assertEquals("1", samples.get(1).getLabel("cpu"));
    }

    // ============================================================
    // AnyValue Type Tests
    // ============================================================

    @Test
    public void testAnyValueTypes() throws Exception {
        // Arrange - test different AnyValue types
        ExportMetricsServiceRequest request = ExportMetricsServiceRequest.newBuilder()
                .addResourceMetrics(ResourceMetrics.newBuilder()
                        .setResource(Resource.newBuilder()
                                .addAttributes(KeyValue.newBuilder()
                                        .setKey("string.attr")
                                        .setValue(AnyValue.newBuilder().setStringValue("test").build())
                                        .build())
                                .addAttributes(KeyValue.newBuilder()
                                        .setKey("bool.attr")
                                        .setValue(AnyValue.newBuilder().setBoolValue(true).build())
                                        .build())
                                .addAttributes(KeyValue.newBuilder()
                                        .setKey("int.attr")
                                        .setValue(AnyValue.newBuilder().setIntValue(42).build())
                                        .build())
                                .addAttributes(KeyValue.newBuilder()
                                        .setKey("double.attr")
                                        .setValue(AnyValue.newBuilder().setDoubleValue(3.14).build())
                                        .build())
                                .build())
                        .addScopeMetrics(ScopeMetrics.newBuilder()
                                .addMetrics(Metric.newBuilder()
                                        .setName("test.metric")
                                        .setUnit("1")
                                        .setGauge(Gauge.newBuilder()
                                                .addDataPoints(NumberDataPoint.newBuilder()
                                                        .setAsDouble(1.0)
                                                        .setTimeUnixNano(System.currentTimeMillis() * 1_000_000)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        // Act
        List<MetricSample> samples = decoder.decode(request.toByteArray());

        // Assert
        assertEquals(1, samples.size());
        MetricSample sample = samples.get(0);
        assertEquals("test", sample.getLabel("string.attr"));
        assertEquals("true", sample.getLabel("bool.attr"));
        assertEquals("42", sample.getLabel("int.attr"));
        assertEquals("3.14", sample.getLabel("double.attr"));
    }

    // ============================================================
    // Error Handling Tests
    // ============================================================

    @Test
    public void testDecodeInvalidPayload() {
        // Arrange - invalid protobuf bytes
        byte[] invalidPayload = "invalid protobuf data".getBytes();

        // Act & Assert
        assertThrows(OtlpDecoder.DecoderException.class, () -> {
            decoder.decode(invalidPayload);
        });
    }

    @Test
    public void testDecodeEmptyPayload() throws Exception {
        // Arrange - empty request
        ExportMetricsServiceRequest request = ExportMetricsServiceRequest.newBuilder().build();

        // Act
        List<MetricSample> samples = decoder.decode(request.toByteArray());

        // Assert
        assertEquals(0, samples.size());
    }

    @Test
    public void testDecodeUnsupportedMetricType() throws Exception {
        // Arrange - metric without data (no gauge/sum/histogram)
        ExportMetricsServiceRequest request = ExportMetricsServiceRequest.newBuilder()
                .addResourceMetrics(ResourceMetrics.newBuilder()
                        .addScopeMetrics(ScopeMetrics.newBuilder()
                                .addMetrics(Metric.newBuilder()
                                        .setName("unsupported.metric")
                                        .setUnit("1")
                                        // No gauge/sum/histogram set
                                        .build())
                                .build())
                        .build())
                .build();

        // Act
        List<MetricSample> samples = decoder.decode(request.toByteArray());

        // Assert - should skip unsupported metric
        assertEquals(0, samples.size());
    }

    @Test
    public void testDecodeWithDefaultUnit() throws Exception {
        // Arrange - metric without unit
        ExportMetricsServiceRequest request = ExportMetricsServiceRequest.newBuilder()
                .addResourceMetrics(ResourceMetrics.newBuilder()
                        .addScopeMetrics(ScopeMetrics.newBuilder()
                                .addMetrics(Metric.newBuilder()
                                        .setName("system.cpu.usage")
                                        // No unit set
                                        .setGauge(Gauge.newBuilder()
                                                .addDataPoints(NumberDataPoint.newBuilder()
                                                        .setAsDouble(0.5)
                                                        .setTimeUnixNano(System.currentTimeMillis() * 1_000_000)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        // Act
        List<MetricSample> samples = decoder.decode(request.toByteArray());

        // Assert - should use default unit "1"
        assertEquals(1, samples.size());
        assertEquals("1", samples.get(0).getUnit());
    }
}
