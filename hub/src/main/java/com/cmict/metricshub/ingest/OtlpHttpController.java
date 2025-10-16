package com.cmict.metricshub.ingest;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * OTLP HTTP endpoint for receiving metrics data.
 *
 * Implements the OpenTelemetry Protocol (OTLP) HTTP/protobuf specification:
 * - POST /v1/metrics: Receive metrics in protobuf format
 * - Returns 202 Accepted on success
 * - Returns 503 Service Unavailable when queue is full
 *
 * Endpoint: http://localhost:4318/v1/metrics
 *
 * Reference: https://opentelemetry.io/docs/specs/otlp/
 */
@RestController
@RequestMapping("/v1")
public class OtlpHttpController {

    private static final Logger logger = LoggerFactory.getLogger(OtlpHttpController.class);

    private final IngestQueue ingestQueue;
    private final MeterRegistry meterRegistry;

    @Value("${metrics-hub.ingest.http.max-payload-size:4194304}")  // 4MB default
    private long maxPayloadSize;

    // Metrics
    private Counter requestCounter;
    private Counter successCounter;
    private Counter rejectedCounter;
    private Counter oversizedCounter;
    private Timer requestTimer;

    public OtlpHttpController(IngestQueue ingestQueue, MeterRegistry meterRegistry) {
        this.ingestQueue = ingestQueue;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        requestCounter = Counter.builder("otlp.http.requests")
                .description("Total HTTP OTLP requests received")
                .register(meterRegistry);

        successCounter = Counter.builder("otlp.http.success")
                .description("Successfully accepted HTTP OTLP requests")
                .register(meterRegistry);

        rejectedCounter = Counter.builder("otlp.http.rejected")
                .description("Rejected HTTP OTLP requests (queue full)")
                .register(meterRegistry);

        oversizedCounter = Counter.builder("otlp.http.oversized")
                .description("Rejected HTTP OTLP requests (payload too large)")
                .register(meterRegistry);

        requestTimer = Timer.builder("otlp.http.duration")
                .description("HTTP OTLP request processing duration")
                .register(meterRegistry);

        logger.info("✅ OTLP HTTP endpoint initialized at /v1/metrics (max payload: {} bytes)",
                maxPayloadSize);
    }

    /**
     * Receive OTLP metrics via HTTP POST.
     *
     * @param payload Raw protobuf bytes
     * @param contentType Content-Type header
     * @return 202 Accepted, 413 Payload Too Large, or 503 Service Unavailable
     */
    @PostMapping(value = "/metrics",
            consumes = {"application/x-protobuf", "application/octet-stream"},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> receiveMetrics(
            @RequestBody byte[] payload,
            @RequestHeader(value = "Content-Type", required = false) String contentType) {

        return requestTimer.record(() -> {
            requestCounter.increment();

            logger.debug("Received HTTP OTLP metrics: {} bytes, content-type: {}",
                    payload.length, contentType);

            // Check payload size
            if (payload.length > maxPayloadSize) {
                oversizedCounter.increment();
                logger.warn("⚠️ Rejected oversized payload: {} bytes (max: {})",
                        payload.length, maxPayloadSize);
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                        .body(Map.of(
                                "error", "Payload too large",
                                "maxSize", maxPayloadSize,
                                "actualSize", payload.length
                        ));
            }

            // Create metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("content-type", contentType != null ? contentType : "unknown");
            metadata.put("size", String.valueOf(payload.length));

            // Enqueue
            MetricsPayload metricsPayload = new MetricsPayload(payload, "http", metadata);
            boolean enqueued = ingestQueue.enqueue(metricsPayload);

            if (enqueued) {
                successCounter.increment();
                logger.debug("✅ HTTP OTLP metrics enqueued successfully");

                return ResponseEntity.accepted()
                        .body(Map.of(
                                "status", "accepted",
                                "queueDepth", ingestQueue.getDepth()
                        ));
            } else {
                rejectedCounter.increment();
                logger.warn("⚠️ HTTP OTLP metrics rejected (queue full)");

                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of(
                                "error", "Queue full, backpressure applied",
                                "queueDepth", ingestQueue.getDepth(),
                                "queueCapacity", ingestQueue.getCapacity()
                        ));
            }
        });
    }

    /**
     * Health check endpoint for HTTP ingestion.
     */
    @GetMapping("/metrics/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("endpoint", "http");
        health.put("queueDepth", ingestQueue.getDepth());
        health.put("queueCapacity", ingestQueue.getCapacity());
        health.put("queueUtilization", String.format("%.2f%%",
                (double) ingestQueue.getDepth() / ingestQueue.getCapacity() * 100));

        return ResponseEntity.ok(health);
    }

    /**
     * Get HTTP ingestion statistics.
     */
    @GetMapping("/metrics/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRequests", requestCounter.count());
        stats.put("successfulRequests", successCounter.count());
        stats.put("rejectedRequests", rejectedCounter.count());
        stats.put("oversizedRequests", oversizedCounter.count());
        stats.put("queueDepth", ingestQueue.getDepth());
        stats.put("queueRemainingCapacity", ingestQueue.getRemainingCapacity());

        return ResponseEntity.ok(stats);
    }
}
