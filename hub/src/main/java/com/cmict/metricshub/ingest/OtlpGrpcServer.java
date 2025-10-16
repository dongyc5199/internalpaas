package com.cmict.metricshub.ingest;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * gRPC server for OTLP metrics ingestion.
 *
 * This server listens on port 4317 and implements the OpenTelemetry Protocol
 * gRPC service for metrics collection.
 *
 * The service receives ExportMetricsServiceRequest messages and enqueues them
 * for processing.
 *
 * Reference: https://opentelemetry.io/docs/specs/otlp/#otlpgrpc
 */
@Component
public class OtlpGrpcServer {

    private static final Logger logger = LoggerFactory.getLogger(OtlpGrpcServer.class);

    @Value("${metrics-hub.ingest.grpc.port:4317}")
    private int port;

    @Value("${metrics-hub.ingest.grpc.max-inbound-message-size:4194304}")  // 4MB
    private int maxInboundMessageSize;

    private final IngestQueue ingestQueue;
    private final MeterRegistry meterRegistry;
    private Server server;

    public OtlpGrpcServer(IngestQueue ingestQueue, MeterRegistry meterRegistry) {
        this.ingestQueue = ingestQueue;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void start() throws IOException {
        // Create gRPC service implementation
        OtlpMetricsServiceImpl metricsService = new OtlpMetricsServiceImpl(
                ingestQueue, meterRegistry);

        // Build and start server
        server = ServerBuilder.forPort(port)
                .addService(metricsService)
                .maxInboundMessageSize(maxInboundMessageSize)
                .build()
                .start();

        logger.info("✅ OTLP gRPC server started on port {} (max message size: {} bytes)",
                port, maxInboundMessageSize);

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down gRPC server via shutdown hook...");
            try {
                OtlpGrpcServer.this.stop();
            } catch (InterruptedException e) {
                logger.error("Error during gRPC server shutdown", e);
                Thread.currentThread().interrupt();
            }
        }));
    }

    @PreDestroy
    public void stop() throws InterruptedException {
        if (server != null) {
            logger.info("Stopping gRPC server...");
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
            logger.info("✅ gRPC server stopped");
        }
    }

    /**
     * Block until the server shuts down.
     */
    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * Get the port the server is listening on.
     */
    public int getPort() {
        return server != null ? server.getPort() : -1;
    }

    /**
     * Check if server is running.
     */
    public boolean isRunning() {
        return server != null && !server.isShutdown();
    }
}
