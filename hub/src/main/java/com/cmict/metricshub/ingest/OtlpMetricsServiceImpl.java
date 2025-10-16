package com.cmict.metricshub.ingest;

import io.grpc.stub.StreamObserver;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse;
import io.opentelemetry.proto.collector.metrics.v1.MetricsServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.util.Map;

/**
 * gRPC service implementation for OTLP metrics export.
 *
 * This service implements the MetricsService from the OTLP specification,
 * receiving ExportMetricsServiceRequest and returning ExportMetricsServiceResponse.
 *
 * Reference proto:
 * - opentelemetry/proto/collector/metrics/v1/metrics_service.proto
 */
public class OtlpMetricsServiceImpl extends MetricsServiceGrpc.MetricsServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(OtlpMetricsServiceImpl.class);

    private final IngestQueue ingestQueue;
    private final MeterRegistry meterRegistry;

    // Metrics
    private Counter requestCounter;
    private Counter successCounter;
    private Counter rejectedCounter;
    private Timer requestTimer;

    public OtlpMetricsServiceImpl(IngestQueue ingestQueue, MeterRegistry meterRegistry) {
        this.ingestQueue = ingestQueue;
        this.meterRegistry = meterRegistry;
        initMetrics();
    }

    @PostConstruct
    public void initMetrics() {
        requestCounter = Counter.builder("otlp.grpc.requests")
                .description("Total gRPC OTLP requests received")
                .register(meterRegistry);

        successCounter = Counter.builder("otlp.grpc.success")
                .description("Successfully accepted gRPC OTLP requests")
                .register(meterRegistry);

        rejectedCounter = Counter.builder("otlp.grpc.rejected")
                .description("Rejected gRPC OTLP requests (queue full)")
                .register(meterRegistry);

        requestTimer = Timer.builder("otlp.grpc.duration")
                .description("gRPC OTLP request processing duration")
                .register(meterRegistry);

        logger.info("✅ OTLP gRPC service metrics initialized");
    }

    /**
     * Handle metrics export request.
     *
     * @param request The OTLP metrics export request
     * @param responseObserver The response observer
     */
    @Override
    public void export(
            ExportMetricsServiceRequest request,
            StreamObserver<ExportMetricsServiceResponse> responseObserver) {

        requestTimer.record(() -> {
            requestCounter.increment();

            try {
                // Serialize the protobuf request to bytes
                byte[] payload = request.toByteArray();

                logger.debug("Received gRPC OTLP metrics: {} bytes", payload.length);

                // Create metadata
                Map<String, String> metadata = Map.of(
                        "protocol", "grpc",
                        "size", String.valueOf(payload.length)
                );

                // Enqueue
                MetricsPayload metricsPayload = new MetricsPayload(payload, "grpc", metadata);
                boolean enqueued = ingestQueue.enqueue(metricsPayload);

                if (enqueued) {
                    successCounter.increment();
                    logger.debug("✅ gRPC OTLP metrics enqueued successfully");

                    // Return success response
                    ExportMetricsServiceResponse response =
                            ExportMetricsServiceResponse.newBuilder().build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();

                } else {
                    rejectedCounter.increment();
                    logger.warn("⚠️ gRPC OTLP metrics rejected (queue full)");

                    // Return error
                    responseObserver.onError(io.grpc.Status.RESOURCE_EXHAUSTED
                            .withDescription("Ingestion queue full, backpressure applied")
                            .asRuntimeException());
                }

            } catch (Exception e) {
                logger.error("Error processing gRPC OTLP request", e);
                responseObserver.onError(io.grpc.Status.INTERNAL
                        .withDescription("Internal error: " + e.getMessage())
                        .asRuntimeException());
            }
        });
    }
}
