package com.cmict.metricshub.ingest;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Bounded in-memory queue for OTLP metrics ingestion.
 *
 * This queue acts as a buffer between the ingestion endpoints (gRPC/HTTP)
 * and the processing pipeline. It provides:
 * - Bounded capacity to prevent memory overflow
 * - Non-blocking offer with timeout for backpressure handling
 * - Micrometer metrics for observability
 *
 * Design decisions:
 * - ArrayBlockingQueue: O(1) offer/poll, thread-safe
 * - Offer timeout: Fast rejection when queue is full
 * - Monitoring: Queue depth, enqueue/reject counters
 */
@Component
public class IngestQueue {

    private static final Logger logger = LoggerFactory.getLogger(IngestQueue.class);

    @Value("${metrics-hub.ingest.queue.capacity:10000}")
    private int capacity;

    @Value("${metrics-hub.ingest.queue.offer-timeout-ms:100}")
    private long offerTimeoutMs;

    private BlockingQueue<MetricsPayload> queue;
    private final MeterRegistry meterRegistry;

    // Metrics
    private Counter enqueueSuccessCounter;
    private Counter enqueueRejectCounter;
    private Counter dequeueCounter;
    private Timer enqueueTimer;

    public IngestQueue(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        this.queue = new ArrayBlockingQueue<>(capacity);

        // Register metrics
        enqueueSuccessCounter = Counter.builder("ingest.queue.enqueue.success")
                .description("Number of successful enqueue operations")
                .register(meterRegistry);

        enqueueRejectCounter = Counter.builder("ingest.queue.enqueue.rejected")
                .description("Number of rejected enqueue operations (queue full)")
                .register(meterRegistry);

        dequeueCounter = Counter.builder("ingest.queue.dequeue")
                .description("Number of dequeue operations")
                .register(meterRegistry);

        enqueueTimer = Timer.builder("ingest.queue.enqueue.duration")
                .description("Time taken to enqueue a payload")
                .register(meterRegistry);

        // Register queue depth gauge
        meterRegistry.gauge("ingest.queue.depth", queue, BlockingQueue::size);
        meterRegistry.gauge("ingest.queue.capacity", this, IngestQueue::getCapacity);

        logger.info("✅ IngestQueue initialized with capacity: {}, offerTimeout: {}ms",
                capacity, offerTimeoutMs);
    }

    /**
     * Enqueue a metrics payload.
     *
     * @param payload The OTLP metrics payload
     * @return true if successfully enqueued, false if rejected (queue full)
     */
    public boolean enqueue(MetricsPayload payload) {
        return enqueueTimer.record(() -> {
            try {
                boolean success = queue.offer(payload, offerTimeoutMs, TimeUnit.MILLISECONDS);

                if (success) {
                    enqueueSuccessCounter.increment();
                    logger.debug("Enqueued payload from {}, queue depth: {}",
                            payload.getSource(), queue.size());
                } else {
                    enqueueRejectCounter.increment();
                    logger.warn("⚠️ Queue full! Rejected payload from {}, capacity: {}",
                            payload.getSource(), capacity);
                }

                return success;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                enqueueRejectCounter.increment();
                logger.error("Enqueue interrupted for payload from {}", payload.getSource(), e);
                return false;
            }
        });
    }

    /**
     * Dequeue a metrics payload (blocking).
     *
     * @return The next payload, or null if interrupted
     */
    public MetricsPayload dequeue() {
        try {
            MetricsPayload payload = queue.take();
            dequeueCounter.increment();
            logger.debug("Dequeued payload from {}, remaining: {}",
                    payload.getSource(), queue.size());
            return payload;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Dequeue interrupted", e);
            return null;
        }
    }

    /**
     * Dequeue with timeout.
     *
     * @param timeout Timeout duration
     * @param unit Time unit
     * @return The next payload, or null if timeout or interrupted
     */
    public MetricsPayload dequeue(long timeout, TimeUnit unit) {
        try {
            MetricsPayload payload = queue.poll(timeout, unit);
            if (payload != null) {
                dequeueCounter.increment();
                logger.debug("Dequeued payload from {} (timeout: {}{}), remaining: {}",
                        payload.getSource(), timeout, unit, queue.size());
            }
            return payload;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Dequeue interrupted", e);
            return null;
        }
    }

    /**
     * Get current queue depth.
     */
    public int getDepth() {
        return queue.size();
    }

    /**
     * Get queue capacity.
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Check if queue is empty.
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Get remaining capacity.
     */
    public int getRemainingCapacity() {
        return queue.remainingCapacity();
    }

    /**
     * Clear the queue (for testing/emergency).
     */
    public void clear() {
        int size = queue.size();
        queue.clear();
        logger.warn("⚠️ Queue cleared! {} payloads discarded", size);
    }
}
