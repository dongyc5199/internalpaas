package com.cmict.metricshub;

import com.cmict.metricshub.ingest.IngestQueue;
import com.cmict.metricshub.ingest.MetricsPayload;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for IngestQueue.
 *
 * Tests:
 * - Basic enqueue/dequeue operations
 * - Queue capacity limits
 * - Backpressure handling
 * - Concurrent access
 * - Metrics collection
 */
public class IngestQueueTest {

    private IngestQueue ingestQueue;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    public void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        ingestQueue = new IngestQueue(meterRegistry);

        // Set test configuration
        ReflectionTestUtils.setField(ingestQueue, "capacity", 100);
        ReflectionTestUtils.setField(ingestQueue, "offerTimeoutMs", 50L);

        // Initialize the queue
        ingestQueue.init();
    }

    @Test
    public void testEnqueueAndDequeue() {
        // Arrange
        byte[] data = "test data".getBytes();
        MetricsPayload payload = new MetricsPayload(data, "test");

        // Act
        boolean enqueued = ingestQueue.enqueue(payload);
        MetricsPayload dequeued = ingestQueue.dequeue(100, TimeUnit.MILLISECONDS);

        // Assert
        assertTrue(enqueued, "Payload should be enqueued");
        assertNotNull(dequeued, "Payload should be dequeued");
        assertEquals("test", dequeued.getSource());
        assertArrayEquals(data, dequeued.getRawData());
    }

    @Test
    public void testQueueCapacity() {
        // Arrange
        int capacity = ingestQueue.getCapacity();

        // Act - Fill the queue
        for (int i = 0; i < capacity; i++) {
            MetricsPayload payload = new MetricsPayload(
                    ("payload-" + i).getBytes(), "test");
            assertTrue(ingestQueue.enqueue(payload),
                    "Should enqueue payload " + i);
        }

        // Assert - Queue is full
        assertEquals(capacity, ingestQueue.getDepth());
        assertEquals(0, ingestQueue.getRemainingCapacity());

        // Try to enqueue one more (should fail due to timeout)
        MetricsPayload extraPayload = new MetricsPayload("extra".getBytes(), "test");
        boolean enqueued = ingestQueue.enqueue(extraPayload);
        assertFalse(enqueued, "Should reject when queue is full");
    }

    @Test
    public void testBackpressure() {
        // Arrange - Fill queue to capacity
        int capacity = ingestQueue.getCapacity();
        for (int i = 0; i < capacity; i++) {
            ingestQueue.enqueue(new MetricsPayload(("p" + i).getBytes(), "test"));
        }

        // Act - Try to enqueue with backpressure
        long startTime = System.currentTimeMillis();
        boolean enqueued = ingestQueue.enqueue(
                new MetricsPayload("backpressure".getBytes(), "test"));
        long duration = System.currentTimeMillis() - startTime;

        // Assert
        assertFalse(enqueued, "Should reject due to backpressure");
        assertTrue(duration >= 50, "Should timeout after configured duration");
    }

    @Test
    public void testConcurrentAccess() throws InterruptedException {
        // Arrange
        int numProducers = 5;
        int numConsumers = 3;
        int itemsPerProducer = 20;
        CountDownLatch producerLatch = new CountDownLatch(numProducers);
        CountDownLatch consumerLatch = new CountDownLatch(numConsumers);
        AtomicInteger producedCount = new AtomicInteger(0);
        AtomicInteger consumedCount = new AtomicInteger(0);

        // Act - Start producers
        for (int i = 0; i < numProducers; i++) {
            final int producerId = i;
            new Thread(() -> {
                for (int j = 0; j < itemsPerProducer; j++) {
                    MetricsPayload payload = new MetricsPayload(
                            ("producer-" + producerId + "-item-" + j).getBytes(),
                            "producer-" + producerId);
                    if (ingestQueue.enqueue(payload)) {
                        producedCount.incrementAndGet();
                    }
                }
                producerLatch.countDown();
            }).start();
        }

        // Act - Start consumers
        for (int i = 0; i < numConsumers; i++) {
            new Thread(() -> {
                while (consumedCount.get() < numProducers * itemsPerProducer) {
                    MetricsPayload payload = ingestQueue.dequeue(100, TimeUnit.MILLISECONDS);
                    if (payload != null) {
                        consumedCount.incrementAndGet();
                    }
                }
                consumerLatch.countDown();
            }).start();
        }

        // Assert
        assertTrue(producerLatch.await(5, TimeUnit.SECONDS), "All producers should finish");
        assertTrue(consumerLatch.await(5, TimeUnit.SECONDS), "All consumers should finish");
        assertEquals(numProducers * itemsPerProducer, producedCount.get(),
                "All items should be produced");
        assertEquals(numProducers * itemsPerProducer, consumedCount.get(),
                "All items should be consumed");
        assertTrue(ingestQueue.isEmpty(), "Queue should be empty");
    }

    @Test
    public void testQueueDepthMetrics() {
        // Arrange & Act
        assertEquals(0, ingestQueue.getDepth());

        ingestQueue.enqueue(new MetricsPayload("p1".getBytes(), "test"));
        assertEquals(1, ingestQueue.getDepth());

        ingestQueue.enqueue(new MetricsPayload("p2".getBytes(), "test"));
        assertEquals(2, ingestQueue.getDepth());

        ingestQueue.dequeue(100, TimeUnit.MILLISECONDS);
        assertEquals(1, ingestQueue.getDepth());

        ingestQueue.dequeue(100, TimeUnit.MILLISECONDS);
        assertEquals(0, ingestQueue.getDepth());
        assertTrue(ingestQueue.isEmpty());
    }

    @Test
    public void testClearQueue() {
        // Arrange - Add some payloads
        for (int i = 0; i < 10; i++) {
            ingestQueue.enqueue(new MetricsPayload(("p" + i).getBytes(), "test"));
        }
        assertEquals(10, ingestQueue.getDepth());

        // Act
        ingestQueue.clear();

        // Assert
        assertEquals(0, ingestQueue.getDepth());
        assertTrue(ingestQueue.isEmpty());
    }

    @Test
    public void testDequeueWithTimeout() {
        // Arrange - Empty queue
        assertTrue(ingestQueue.isEmpty());

        // Act
        long startTime = System.currentTimeMillis();
        MetricsPayload payload = ingestQueue.dequeue(100, TimeUnit.MILLISECONDS);
        long duration = System.currentTimeMillis() - startTime;

        // Assert
        assertNull(payload, "Should return null when queue is empty");
        assertTrue(duration >= 100, "Should timeout after specified duration");
    }

    @Test
    public void testMetricsPayloadProperties() {
        // Arrange
        byte[] data = "test metrics data".getBytes();
        MetricsPayload payload = new MetricsPayload(data, "http");

        // Act & Assert
        assertEquals("http", payload.getSource());
        assertEquals(data.length, payload.getSize());
        assertArrayEquals(data, payload.getRawData());
        assertNotNull(payload.getReceivedAt());
        assertTrue(payload.getAgeMs() >= 0);
    }
}
