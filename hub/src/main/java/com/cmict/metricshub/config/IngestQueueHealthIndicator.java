package com.cmict.metricshub.config;

import com.cmict.metricshub.ingest.IngestQueue;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for the ingestion queue.
 *
 * Reports the queue status in the /actuator/health endpoint.
 *
 * Health Status:
 * - UP: Queue is healthy (utilization < 80%)
 * - WARNING: Queue is getting full (utilization 80-95%)
 * - DOWN: Queue is critically full (utilization > 95%)
 */
@Component
public class IngestQueueHealthIndicator implements HealthIndicator {

    private final IngestQueue ingestQueue;

    private static final double WARNING_THRESHOLD = 0.80;  // 80%
    private static final double CRITICAL_THRESHOLD = 0.95;  // 95%

    public IngestQueueHealthIndicator(IngestQueue ingestQueue) {
        this.ingestQueue = ingestQueue;
    }

    @Override
    public Health health() {
        int depth = ingestQueue.getDepth();
        int capacity = ingestQueue.getCapacity();
        double utilization = (double) depth / capacity;

        Health.Builder builder;

        if (utilization >= CRITICAL_THRESHOLD) {
            builder = Health.down()
                    .withDetail("status", "CRITICAL")
                    .withDetail("message", "Queue is critically full");
        } else if (utilization >= WARNING_THRESHOLD) {
            builder = Health.status("WARNING")
                    .withDetail("status", "WARNING")
                    .withDetail("message", "Queue utilization is high");
        } else {
            builder = Health.up()
                    .withDetail("status", "HEALTHY")
                    .withDetail("message", "Queue is healthy");
        }

        return builder
                .withDetail("queueDepth", depth)
                .withDetail("queueCapacity", capacity)
                .withDetail("queueUtilization", String.format("%.2f%%", utilization * 100))
                .withDetail("queueRemainingCapacity", ingestQueue.getRemainingCapacity())
                .build();
    }
}
