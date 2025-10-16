package com.cmict.metricshub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Metrics Hub - OTLP Metrics Ingestion Service
 *
 * This service receives metrics data via OTLP protocol (both gRPC and HTTP),
 * queues them for processing, and provides health monitoring endpoints.
 *
 * Ports:
 * - 8080: Main HTTP API and Actuator endpoints
 * - 4317: OTLP gRPC endpoint
 * - 4318: OTLP HTTP endpoint
 *
 * @author CMICT PaaS Team
 * @version 0.1.0
 */
@SpringBootApplication
public class HubApplication {

    private static final Logger logger = LoggerFactory.getLogger(HubApplication.class);

    public static void main(String[] args) {
        logger.info("========================================");
        logger.info("  Metrics Hub Starting...");
        logger.info("========================================");

        SpringApplication.run(HubApplication.class, args);

        logger.info("========================================");
        logger.info("  Metrics Hub Started Successfully!");
        logger.info("  - HTTP API: http://localhost:8080");
        logger.info("  - OTLP gRPC: localhost:4317");
        logger.info("  - OTLP HTTP: http://localhost:4318");
        logger.info("  - Health: http://localhost:8080/actuator/health");
        logger.info("  - Metrics: http://localhost:8080/actuator/metrics");
        logger.info("========================================");
    }
}
