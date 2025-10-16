package com.cmict.metricshub.processing;

import com.cmict.metricshub.ingest.IngestQueue;
import com.cmict.metricshub.ingest.MetricsPayload;
import com.cmict.metricshub.model.MetricSample;
import com.cmict.metricshub.model.MetricSample.MetricType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.proto.collector.metrics.v1.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Processing Pipeline.
 *
 * Tests the full pipeline:
 * IngestQueue → ProcessingService → OtlpDecoder → Normalizer → (Storage in T3)
 *
 * Validates:
 * - End-to-end processing
 * - Multiple metric types
 * - Unit conversion + label filtering
 * - Concurrent processing
 * - Error handling
 */
public class ProcessingIntegrationTest {

    private IngestQueue ingestQueue;
    private ProcessingService processingService;
    private OtlpDecoder decoder;
    private Normalizer normalizer;
    private SimpleMeterRegistry meterRegistry;

    // Test helper to capture processed samples
    private final List<MetricSample> processedSamples = new ArrayList<>();
    private final AtomicInteger processedCount = new AtomicInteger(0);

    @BeforeEach
    public void setUp() {
        meterRegistry = new SimpleMeterRegistry();

        // Create components
        ingestQueue = new IngestQueue(meterRegistry);
        decoder = new OtlpDecoder();
        normalizer = new Normalizer();

        // Configure normalizer
        ReflectionTestUtils.setField(normalizer, "labelWhitelistConfig",
                "server.id,region,group,env");
        ReflectionTestUtils.setField(normalizer, "logUnrecognized", false);
        normalizer.init();

        // Configure ingest queue
        ReflectionTestUtils.setField(ingestQueue, "capacity", 100);
        ReflectionTestUtils.setField(ingestQueue, "offerTimeoutMs", 50L);
        ingestQueue.init();

        // Create custom processing service for testing (captures samples)
        processingService = new TestProcessingService(ingestQueue, decoder, normalizer, meterRegistry);

        processedSamples.clear();
        processedCount.set(0);
    }

    @AfterEach
    public void tearDown() {
        if (processingService != null && processingService.isRunning()) {
            processingService.stop();
        }
    }

    // ============================================================
    // End-to-End Processing Tests
    // ============================================================

    @Test
    public void testEndToEndProcessing_SingleMetric() throws Exception {
        // Arrange
        ExportMetricsServiceRequest request = createOtlpRequest(
                "system.cpu.usage",
                "1",
                0.75,
                "server-001",
                "us-west-1"
        );

        MetricsPayload payload = new MetricsPayload(request.toByteArray(), "test");

        // Act
        processingService.start();
        assertTrue(ingestQueue.enqueue(payload));

        // Wait for processing
        waitForProcessing(1, 2000);
        processingService.stop();

        // Assert
        assertEquals(1, processedSamples.size());
        MetricSample sample = processedSamples.get(0);

        assertEquals("system.cpu.usage", sample.getName());
        assertEquals(MetricType.GAUGE, sample.getType());
        assertEquals(75.0, sample.getValue(), 0.001); // Normalized: 0.75 → 75%
        assertEquals("percent", sample.getUnit());
        assertEquals("server-001", sample.getLabel("server.id"));
        assertEquals("us-west-1", sample.getLabel("region"));
    }

    @Test
    public void testEndToEndProcessing_MultipleMetrics() throws Exception {
        // Arrange - create OTLP request with multiple metrics
        ExportMetricsServiceRequest request = ExportMetricsServiceRequest.newBuilder()
                .addResourceMetrics(ResourceMetrics.newBuilder()
                        .setResource(Resource.newBuilder()
                                .addAttributes(KeyValue.newBuilder()
                                        .setKey("server.id")
                                        .setValue(AnyValue.newBuilder().setStringValue("server-001").build())
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
                                .addMetrics(Metric.newBuilder()
                                        .setName("system.memory.usage")
                                        .setUnit("mb")
                                        .setGauge(Gauge.newBuilder()
                                                .addDataPoints(NumberDataPoint.newBuilder()
                                                        .setAsInt(1024)
                                                        .setTimeUnixNano(System.currentTimeMillis() * 1_000_000)
                                                        .build())
                                                .build())
                                        .build())
                                .addMetrics(Metric.newBuilder()
                                        .setName("system.network.io.bytes")
                                        .setUnit("kb")
                                        .setSum(Sum.newBuilder()
                                                .addDataPoints(NumberDataPoint.newBuilder()
                                                        .setAsInt(5000)
                                                        .setTimeUnixNano(System.currentTimeMillis() * 1_000_000)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        MetricsPayload payload = new MetricsPayload(request.toByteArray(), "test");

        // Act
        processingService.start();
        assertTrue(ingestQueue.enqueue(payload));

        waitForProcessing(3, 2000);
        processingService.stop();

        // Assert
        assertEquals(3, processedSamples.size());

        // CPU: 0.5 → 50%
        MetricSample cpuSample = findSample("system.cpu.usage");
        assertNotNull(cpuSample);
        assertEquals(50.0, cpuSample.getValue(), 0.001);
        assertEquals("percent", cpuSample.getUnit());

        // Memory: 1024 MB → bytes
        MetricSample memorySample = findSample("system.memory.usage");
        assertNotNull(memorySample);
        assertEquals(1073741824.0, memorySample.getValue(), 0.001);
        assertEquals("bytes", memorySample.getUnit());

        // Network: 5000 KB → bytes
        MetricSample networkSample = findSample("system.network.io.bytes");
        assertNotNull(networkSample);
        assertEquals(5120000.0, networkSample.getValue(), 0.001);
        assertEquals("bytes", networkSample.getUnit());
    }

    @Test
    public void testEndToEndProcessing_LabelFiltering() throws Exception {
        // Arrange - OTLP with both whitelisted and non-whitelisted labels
        ExportMetricsServiceRequest request = ExportMetricsServiceRequest.newBuilder()
                .addResourceMetrics(ResourceMetrics.newBuilder()
                        .setResource(Resource.newBuilder()
                                .addAttributes(KeyValue.newBuilder()
                                        .setKey("server.id")
                                        .setValue(AnyValue.newBuilder().setStringValue("server-001").build())
                                        .build())
                                .addAttributes(KeyValue.newBuilder()
                                        .setKey("host.name")
                                        .setValue(AnyValue.newBuilder().setStringValue("example.com").build())
                                        .build())
                                .addAttributes(KeyValue.newBuilder()
                                        .setKey("region")
                                        .setValue(AnyValue.newBuilder().setStringValue("us-east-1").build())
                                        .build())
                                .addAttributes(KeyValue.newBuilder()
                                        .setKey("process.id")
                                        .setValue(AnyValue.newBuilder().setStringValue("12345").build())
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

        MetricsPayload payload = new MetricsPayload(request.toByteArray(), "test");

        // Act
        processingService.start();
        assertTrue(ingestQueue.enqueue(payload));

        waitForProcessing(1, 2000);
        processingService.stop();

        // Assert - only whitelisted labels should remain
        assertEquals(1, processedSamples.size());
        MetricSample sample = processedSamples.get(0);

        assertEquals("server-001", sample.getLabel("server.id"));
        assertEquals("us-east-1", sample.getLabel("region"));
        assertEquals("unknown", sample.getLabel("env")); // Default added

        // Non-whitelisted labels should be filtered out
        assertNull(sample.getLabel("host.name"));
        assertNull(sample.getLabel("process.id"));
    }

    @Test
    public void testEndToEndProcessing_UnrecognizedMetricFiltered() throws Exception {
        // Arrange - mix of recognized and unrecognized metrics
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
                                        .setName("unknown.weird.metric")
                                        .setUnit("count")
                                        .setGauge(Gauge.newBuilder()
                                                .addDataPoints(NumberDataPoint.newBuilder()
                                                        .setAsInt(999)
                                                        .setTimeUnixNano(System.currentTimeMillis() * 1_000_000)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        MetricsPayload payload = new MetricsPayload(request.toByteArray(), "test");

        // Act
        processingService.start();
        assertTrue(ingestQueue.enqueue(payload));

        waitForProcessing(1, 2000);
        processingService.stop();

        // Assert - only recognized metric should be processed
        assertEquals(1, processedSamples.size());
        assertEquals("system.cpu.usage", processedSamples.get(0).getName());
    }

    // ============================================================
    // Concurrent Processing Tests
    // ============================================================

    @Test
    public void testConcurrentProcessing() throws Exception {
        // Arrange - multiple payloads
        int payloadCount = 10;
        List<MetricsPayload> payloads = new ArrayList<>();

        for (int i = 0; i < payloadCount; i++) {
            ExportMetricsServiceRequest request = createOtlpRequest(
                    "system.cpu.usage",
                    "1",
                    0.5 + (i * 0.01),
                    "server-" + String.format("%03d", i),
                    "us-west-1"
            );
            payloads.add(new MetricsPayload(request.toByteArray(), "test-" + i));
        }

        // Act
        processingService.start();

        for (MetricsPayload payload : payloads) {
            assertTrue(ingestQueue.enqueue(payload));
        }

        waitForProcessing(payloadCount, 5000);
        processingService.stop();

        // Assert
        assertEquals(payloadCount, processedSamples.size());

        // Verify all samples were processed
        for (int i = 0; i < payloadCount; i++) {
            String expectedServerId = "server-" + String.format("%03d", i);
            boolean found = processedSamples.stream()
                    .anyMatch(s -> expectedServerId.equals(s.getLabel("server.id")));
            assertTrue(found, "Sample with server.id=" + expectedServerId + " not found");
        }
    }

    // ============================================================
    // Error Handling Tests
    // ============================================================

    @Test
    public void testProcessing_InvalidPayload() throws Exception {
        // Arrange - invalid OTLP data
        byte[] invalidData = "this is not valid protobuf".getBytes();
        MetricsPayload payload = new MetricsPayload(invalidData, "test");

        // Act
        processingService.start();
        assertTrue(ingestQueue.enqueue(payload));

        // Wait a bit
        Thread.sleep(500);
        processingService.stop();

        // Assert - no samples should be processed, but service should continue running
        assertEquals(0, processedSamples.size());

        // Verify error counter increased
        assertTrue(processingService.getStats().errors() > 0);
    }

    @Test
    public void testProcessing_ServiceMetrics() throws Exception {
        // Arrange
        ExportMetricsServiceRequest request = createOtlpRequest(
                "system.cpu.usage",
                "1",
                0.75,
                "server-001",
                "us-west-1"
        );

        MetricsPayload payload = new MetricsPayload(request.toByteArray(), "test");

        // Act
        processingService.start();
        assertTrue(ingestQueue.enqueue(payload));

        waitForProcessing(1, 2000);
        processingService.stop();

        // Assert - verify processing stats
        ProcessingService.ProcessingStats stats = processingService.getStats();
        assertEquals(1, stats.payloadsProcessed());
        assertEquals(1, stats.samplesDecoded());
        assertEquals(1, stats.samplesNormalized());
        assertEquals(0, stats.errors());
        assertFalse(stats.running());
    }

    // ============================================================
    // Helper Methods
    // ============================================================

    private ExportMetricsServiceRequest createOtlpRequest(
            String metricName, String unit, double value, String serverId, String region) {
        return ExportMetricsServiceRequest.newBuilder()
                .addResourceMetrics(ResourceMetrics.newBuilder()
                        .setResource(Resource.newBuilder()
                                .addAttributes(KeyValue.newBuilder()
                                        .setKey("server.id")
                                        .setValue(AnyValue.newBuilder().setStringValue(serverId).build())
                                        .build())
                                .addAttributes(KeyValue.newBuilder()
                                        .setKey("region")
                                        .setValue(AnyValue.newBuilder().setStringValue(region).build())
                                        .build())
                                .build())
                        .addScopeMetrics(ScopeMetrics.newBuilder()
                                .addMetrics(Metric.newBuilder()
                                        .setName(metricName)
                                        .setUnit(unit)
                                        .setGauge(Gauge.newBuilder()
                                                .addDataPoints(NumberDataPoint.newBuilder()
                                                        .setAsDouble(value)
                                                        .setTimeUnixNano(System.currentTimeMillis() * 1_000_000)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private void waitForProcessing(int expectedSamples, long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (processedSamples.size() < expectedSamples &&
                System.currentTimeMillis() - start < timeoutMs) {
            Thread.sleep(50);
        }
    }

    private MetricSample findSample(String metricName) {
        return processedSamples.stream()
                .filter(s -> s.getName().equals(metricName))
                .findFirst()
                .orElse(null);
    }

    // ============================================================
    // Test Processing Service (captures samples for verification)
    // ============================================================

    private class TestProcessingService extends ProcessingService {
        public TestProcessingService(IngestQueue ingestQueue,
                                      OtlpDecoder decoder,
                                      Normalizer normalizer,
                                      SimpleMeterRegistry meterRegistry) {
            super(ingestQueue, decoder, normalizer, meterRegistry);
        }

        @Override
        protected void processPayload(MetricsPayload payload, int processorId) {
            try {
                // Decode
                List<MetricSample> decodedSamples = decoder.decode(payload.getRawData());
                decodedSamplesCounter.increment(decodedSamples.size());

                // Normalize
                List<MetricSample> normalizedSamples = normalizer.normalize(decodedSamples);
                normalizedSamplesCounter.increment(normalizedSamples.size());

                // Increment processed counter
                processedCounter.increment();

                // Capture for test verification
                synchronized (processedSamples) {
                    processedSamples.addAll(normalizedSamples);
                    processedCount.addAndGet(normalizedSamples.size());
                }

            } catch (Exception e) {
                // Increment error counter
                errorCounter.increment();
            }
        }
    }
}
