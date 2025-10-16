package com.cmict.metricshub.processing;

import com.cmict.metricshub.model.MetricSample;
import com.cmict.metricshub.model.MetricSample.MetricType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Normalizer.
 *
 * Tests:
 * - Unit conversion (bytes, time, percent)
 * - Label whitelist filtering
 * - Unrecognized metric filtering
 * - Default value handling
 * - Invalid value handling (NaN, Infinite)
 */
public class NormalizerTest {

    private Normalizer normalizer;

    @BeforeEach
    public void setUp() {
        normalizer = new Normalizer();

        // Set test configuration
        ReflectionTestUtils.setField(normalizer, "labelWhitelistConfig",
                "server.id,region,group,env");
        ReflectionTestUtils.setField(normalizer, "logUnrecognized", false);

        // Initialize
        normalizer.init();
    }

    // ============================================================
    // Unit Conversion Tests
    // ============================================================

    @Test
    public void testMemoryUnitConversion_KB_to_Bytes() {
        // Arrange
        MetricSample sample = MetricSample.builder()
                .name("system.memory.usage")
                .type(MetricType.GAUGE)
                .value(100.0)
                .unit("kb")
                .timestamp(Instant.now())
                .build();

        List<MetricSample> samples = List.of(sample);

        // Act
        List<MetricSample> normalized = normalizer.normalize(samples);

        // Assert
        assertEquals(1, normalized.size());
        MetricSample result = normalized.get(0);
        assertEquals(102400.0, result.getValue(), 0.001); // 100 KB = 102400 bytes
        assertEquals("bytes", result.getUnit());
    }

    @Test
    public void testMemoryUnitConversion_MB_to_Bytes() {
        // Arrange
        MetricSample sample = MetricSample.builder()
                .name("system.memory.usage")
                .type(MetricType.GAUGE)
                .value(2.0)
                .unit("mb")
                .timestamp(Instant.now())
                .build();

        // Act
        List<MetricSample> normalized = normalizer.normalize(List.of(sample));

        // Assert
        MetricSample result = normalized.get(0);
        assertEquals(2097152.0, result.getValue(), 0.001); // 2 MB = 2097152 bytes
        assertEquals("bytes", result.getUnit());
    }

    @Test
    public void testMemoryUnitConversion_GB_to_Bytes() {
        // Arrange
        MetricSample sample = MetricSample.builder()
                .name("system.disk.usage")
                .type(MetricType.GAUGE)
                .value(1.5)
                .unit("gb")
                .timestamp(Instant.now())
                .build();

        // Act
        List<MetricSample> normalized = normalizer.normalize(List.of(sample));

        // Assert
        MetricSample result = normalized.get(0);
        assertEquals(1610612736.0, result.getValue(), 0.001); // 1.5 GB = 1610612736 bytes
        assertEquals("bytes", result.getUnit());
    }

    @Test
    public void testTimeUnitConversion_Milliseconds_to_Seconds() {
        // Arrange
        MetricSample sample = MetricSample.builder()
                .name("system.cpu.time")
                .type(MetricType.COUNTER)
                .value(5000.0)
                .unit("ms")
                .timestamp(Instant.now())
                .build();

        // Act
        List<MetricSample> normalized = normalizer.normalize(List.of(sample));

        // Assert
        MetricSample result = normalized.get(0);
        assertEquals(5.0, result.getValue(), 0.001); // 5000 ms = 5 seconds
        assertEquals("seconds", result.getUnit());
    }

    @Test
    public void testTimeUnitConversion_Microseconds_to_Seconds() {
        // Arrange
        MetricSample sample = MetricSample.builder()
                .name("system.cpu.time")
                .type(MetricType.COUNTER)
                .value(1000000.0)
                .unit("us")
                .timestamp(Instant.now())
                .build();

        // Act
        List<MetricSample> normalized = normalizer.normalize(List.of(sample));

        // Assert
        MetricSample result = normalized.get(0);
        assertEquals(1.0, result.getValue(), 0.001); // 1000000 μs = 1 second
        assertEquals("seconds", result.getUnit());
    }

    @Test
    public void testTimeUnitConversion_Nanoseconds_to_Seconds() {
        // Arrange
        MetricSample sample = MetricSample.builder()
                .name("system.cpu.time")
                .type(MetricType.COUNTER)
                .value(2000000000.0)
                .unit("ns")
                .timestamp(Instant.now())
                .build();

        // Act
        List<MetricSample> normalized = normalizer.normalize(List.of(sample));

        // Assert
        MetricSample result = normalized.get(0);
        assertEquals(2.0, result.getValue(), 0.001); // 2000000000 ns = 2 seconds
        assertEquals("seconds", result.getUnit());
    }

    @Test
    public void testRatioToPercentConversion() {
        // Arrange
        MetricSample sample = MetricSample.builder()
                .name("system.cpu.usage")
                .type(MetricType.GAUGE)
                .value(0.75)
                .unit("1")
                .timestamp(Instant.now())
                .build();

        // Act
        List<MetricSample> normalized = normalizer.normalize(List.of(sample));

        // Assert
        MetricSample result = normalized.get(0);
        assertEquals(75.0, result.getValue(), 0.001); // 0.75 → 75%
        assertEquals("percent", result.getUnit());
    }

    @Test
    public void testRatioToPercentConversion_NotAppliedToNonUsageMetrics() {
        // Arrange - metric name doesn't contain "usage"
        MetricSample sample = MetricSample.builder()
                .name("system.load.average.1m")
                .type(MetricType.GAUGE)
                .value(0.75)
                .unit("1")
                .timestamp(Instant.now())
                .build();

        // Act
        List<MetricSample> normalized = normalizer.normalize(List.of(sample));

        // Assert - should NOT convert to percent
        MetricSample result = normalized.get(0);
        assertEquals(0.75, result.getValue(), 0.001);
        assertEquals("count", result.getUnit()); // "1" becomes "count"
    }

    @Test
    public void testPercentUnitNormalization() {
        // Arrange - already in percent
        MetricSample sample = MetricSample.builder()
                .name("system.cpu.utilization")
                .type(MetricType.GAUGE)
                .value(85.5)
                .unit("%")
                .timestamp(Instant.now())
                .build();

        // Act
        List<MetricSample> normalized = normalizer.normalize(List.of(sample));

        // Assert - should remain unchanged
        MetricSample result = normalized.get(0);
        assertEquals(85.5, result.getValue(), 0.001);
        assertEquals("percent", result.getUnit());
    }

    // ============================================================
    // Label Whitelist Filtering Tests
    // ============================================================

    @Test
    public void testLabelWhitelistFiltering_KeepsWhitelistedLabels() {
        // Arrange
        Map<String, String> labels = new HashMap<>();
        labels.put("server.id", "server-001");
        labels.put("region", "us-west-1");
        labels.put("group", "web");
        labels.put("env", "prod");
        labels.put("host", "example.com"); // Should be filtered out
        labels.put("process.id", "12345");  // Should be filtered out

        MetricSample sample = MetricSample.builder()
                .name("system.cpu.usage")
                .type(MetricType.GAUGE)
                .value(0.75)
                .unit("1")
                .timestamp(Instant.now())
                .labels(labels)
                .build();

        // Act
        List<MetricSample> normalized = normalizer.normalize(List.of(sample));

        // Assert
        MetricSample result = normalized.get(0);
        Map<String, String> resultLabels = result.getLabels();

        assertEquals(4, resultLabels.size());
        assertEquals("server-001", resultLabels.get("server.id"));
        assertEquals("us-west-1", resultLabels.get("region"));
        assertEquals("web", resultLabels.get("group"));
        assertEquals("prod", resultLabels.get("env"));
        assertNull(resultLabels.get("host"));
        assertNull(resultLabels.get("process.id"));
    }

    @Test
    public void testLabelWhitelistFiltering_EmptyLabels() {
        // Arrange
        MetricSample sample = MetricSample.builder()
                .name("system.cpu.usage")
                .type(MetricType.GAUGE)
                .value(0.5)
                .unit("1")
                .timestamp(Instant.now())
                .build();

        // Act
        List<MetricSample> normalized = normalizer.normalize(List.of(sample));

        // Assert
        MetricSample result = normalized.get(0);
        Map<String, String> resultLabels = result.getLabels();

        // Should add default "env" label
        assertEquals(1, resultLabels.size());
        assertEquals("unknown", resultLabels.get("env"));
    }

    @Test
    public void testLabelWhitelistFiltering_PartialWhitelist() {
        // Arrange
        Map<String, String> labels = new HashMap<>();
        labels.put("server.id", "server-002");
        labels.put("host", "example.com"); // Not in whitelist
        labels.put("custom.tag", "value"); // Not in whitelist

        MetricSample sample = MetricSample.builder()
                .name("system.memory.usage")
                .type(MetricType.GAUGE)
                .value(1024.0)
                .unit("mb")
                .timestamp(Instant.now())
                .labels(labels)
                .build();

        // Act
        List<MetricSample> normalized = normalizer.normalize(List.of(sample));

        // Assert
        MetricSample result = normalized.get(0);
        Map<String, String> resultLabels = result.getLabels();

        assertEquals(2, resultLabels.size()); // server.id + default env
        assertEquals("server-002", resultLabels.get("server.id"));
        assertEquals("unknown", resultLabels.get("env"));
        assertNull(resultLabels.get("host"));
        assertNull(resultLabels.get("custom.tag"));
    }

    // ============================================================
    // Unrecognized Metric Filtering Tests
    // ============================================================

    @Test
    public void testUnrecognizedMetricFiltering_RecognizedMetric() {
        // Arrange
        MetricSample sample = MetricSample.builder()
                .name("system.cpu.usage")
                .type(MetricType.GAUGE)
                .value(0.5)
                .unit("1")
                .timestamp(Instant.now())
                .build();

        // Act
        List<MetricSample> normalized = normalizer.normalize(List.of(sample));

        // Assert
        assertEquals(1, normalized.size());
        assertNotNull(normalized.get(0));
    }

    @Test
    public void testUnrecognizedMetricFiltering_UnrecognizedMetric() {
        // Arrange - unknown metric name
        MetricSample sample = MetricSample.builder()
                .name("unknown.metric.name")
                .type(MetricType.GAUGE)
                .value(123.0)
                .unit("count")
                .timestamp(Instant.now())
                .build();

        // Act
        List<MetricSample> normalized = normalizer.normalize(List.of(sample));

        // Assert - should be filtered out
        assertEquals(0, normalized.size());
    }

    @Test
    public void testUnrecognizedMetricFiltering_CustomMetricAllowed() {
        // Arrange - custom.* prefix is allowed
        MetricSample sample = MetricSample.builder()
                .name("custom.business.metric")
                .type(MetricType.GAUGE)
                .value(999.0)
                .unit("count")
                .timestamp(Instant.now())
                .build();

        // Act
        List<MetricSample> normalized = normalizer.normalize(List.of(sample));

        // Assert - custom metrics are allowed
        assertEquals(1, normalized.size());
        assertEquals("custom.business.metric", normalized.get(0).getName());
    }

    @Test
    public void testUnrecognizedMetricFiltering_MixedBatch() {
        // Arrange - mix of recognized and unrecognized
        List<MetricSample> samples = List.of(
                MetricSample.builder()
                        .name("system.cpu.usage")
                        .type(MetricType.GAUGE)
                        .value(0.5)
                        .unit("1")
                        .timestamp(Instant.now())
                        .build(),
                MetricSample.builder()
                        .name("unknown.metric")
                        .type(MetricType.GAUGE)
                        .value(123.0)
                        .unit("count")
                        .timestamp(Instant.now())
                        .build(),
                MetricSample.builder()
                        .name("system.memory.usage")
                        .type(MetricType.GAUGE)
                        .value(1024.0)
                        .unit("mb")
                        .timestamp(Instant.now())
                        .build()
        );

        // Act
        List<MetricSample> normalized = normalizer.normalize(samples);

        // Assert - only 2 recognized metrics should pass
        assertEquals(2, normalized.size());
        assertEquals("system.cpu.usage", normalized.get(0).getName());
        assertEquals("system.memory.usage", normalized.get(1).getName());
    }

    // ============================================================
    // Default Value Handling Tests
    // ============================================================

    @Test
    public void testDefaultEnvLabel_AddedWhenMissing() {
        // Arrange
        MetricSample sample = MetricSample.builder()
                .name("system.cpu.usage")
                .type(MetricType.GAUGE)
                .value(0.5)
                .unit("1")
                .timestamp(Instant.now())
                .build();

        // Act
        List<MetricSample> normalized = normalizer.normalize(List.of(sample));

        // Assert
        MetricSample result = normalized.get(0);
        assertEquals("unknown", result.getLabel("env"));
    }

    @Test
    public void testInvalidValueHandling_NaN() {
        // Arrange
        MetricSample sample = MetricSample.builder()
                .name("system.cpu.usage")
                .type(MetricType.GAUGE)
                .value(Double.NaN)
                .unit("1")
                .timestamp(Instant.now())
                .build();

        // Act
        List<MetricSample> normalized = normalizer.normalize(List.of(sample));

        // Assert - NaN should be replaced with 0
        MetricSample result = normalized.get(0);
        assertEquals(0.0, result.getValue(), 0.001);
    }

    @Test
    public void testInvalidValueHandling_Infinity() {
        // Arrange
        MetricSample sample = MetricSample.builder()
                .name("system.cpu.usage")
                .type(MetricType.GAUGE)
                .value(Double.POSITIVE_INFINITY)
                .unit("1")
                .timestamp(Instant.now())
                .build();

        // Act
        List<MetricSample> normalized = normalizer.normalize(List.of(sample));

        // Assert - Infinity should be replaced with 0
        MetricSample result = normalized.get(0);
        assertEquals(0.0, result.getValue(), 0.001);
    }

    // ============================================================
    // Complex Scenarios
    // ============================================================

    @Test
    public void testComplexNormalization_AllTransformations() {
        // Arrange - metric with unit conversion, label filtering, default env
        Map<String, String> labels = new HashMap<>();
        labels.put("server.id", "server-123");
        labels.put("region", "eu-west-1");
        labels.put("extra.label", "should-be-filtered");

        MetricSample sample = MetricSample.builder()
                .name("system.memory.usage")
                .type(MetricType.GAUGE)
                .value(512.0)
                .unit("mb")
                .timestamp(Instant.now())
                .labels(labels)
                .build();

        // Act
        List<MetricSample> normalized = normalizer.normalize(List.of(sample));

        // Assert
        MetricSample result = normalized.get(0);

        // Unit conversion: 512 MB → bytes
        assertEquals(536870912.0, result.getValue(), 0.001);
        assertEquals("bytes", result.getUnit());

        // Label filtering + default env
        Map<String, String> resultLabels = result.getLabels();
        assertEquals(3, resultLabels.size());
        assertEquals("server-123", resultLabels.get("server.id"));
        assertEquals("eu-west-1", resultLabels.get("region"));
        assertEquals("unknown", resultLabels.get("env"));
        assertNull(resultLabels.get("extra.label"));
    }

    @Test
    public void testBatchNormalization_MultipleMetrics() {
        // Arrange - batch of different metric types
        List<MetricSample> samples = new ArrayList<>();

        samples.add(MetricSample.builder()
                .name("system.cpu.usage")
                .type(MetricType.GAUGE)
                .value(0.75)
                .unit("1")
                .timestamp(Instant.now())
                .label("server.id", "server-001")
                .build());

        samples.add(MetricSample.builder()
                .name("system.memory.usage")
                .type(MetricType.GAUGE)
                .value(2048.0)
                .unit("mb")
                .timestamp(Instant.now())
                .label("server.id", "server-001")
                .build());

        samples.add(MetricSample.builder()
                .name("system.network.io.bytes")
                .type(MetricType.COUNTER)
                .value(5000.0)
                .unit("kb")
                .timestamp(Instant.now())
                .label("server.id", "server-001")
                .build());

        // Act
        List<MetricSample> normalized = normalizer.normalize(samples);

        // Assert
        assertEquals(3, normalized.size());

        // CPU: 0.75 → 75%
        assertEquals(75.0, normalized.get(0).getValue(), 0.001);
        assertEquals("percent", normalized.get(0).getUnit());

        // Memory: 2048 MB → bytes
        assertEquals(2147483648.0, normalized.get(1).getValue(), 0.001);
        assertEquals("bytes", normalized.get(1).getUnit());

        // Network: 5000 KB → bytes
        assertEquals(5120000.0, normalized.get(2).getValue(), 0.001);
        assertEquals("bytes", normalized.get(2).getUnit());
    }

    @Test
    public void testLabelWhitelistConfiguration() {
        // Assert - verify whitelist was initialized correctly
        assertEquals(4, normalizer.getLabelWhitelist().size());
        assertTrue(normalizer.getLabelWhitelist().contains("server.id"));
        assertTrue(normalizer.getLabelWhitelist().contains("region"));
        assertTrue(normalizer.getLabelWhitelist().contains("group"));
        assertTrue(normalizer.getLabelWhitelist().contains("env"));
    }
}
