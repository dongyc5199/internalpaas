package com.cmict.metricshub.config;

import com.cmict.metricshub.ingest.OtlpGrpcServer;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for the gRPC server.
 *
 * Reports the gRPC server status in the /actuator/health endpoint.
 */
@Component
public class GrpcServerHealthIndicator implements HealthIndicator {

    private final OtlpGrpcServer grpcServer;

    public GrpcServerHealthIndicator(OtlpGrpcServer grpcServer) {
        this.grpcServer = grpcServer;
    }

    @Override
    public Health health() {
        boolean running = grpcServer.isRunning();
        int port = grpcServer.getPort();

        if (running) {
            return Health.up()
                    .withDetail("status", "RUNNING")
                    .withDetail("port", port)
                    .withDetail("protocol", "grpc")
                    .withDetail("message", "gRPC server is running")
                    .build();
        } else {
            return Health.down()
                    .withDetail("status", "STOPPED")
                    .withDetail("message", "gRPC server is not running")
                    .build();
        }
    }
}
