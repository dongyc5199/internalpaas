package com.cmict.metricshub.processing;

import com.cmict.metricshub.ingest.IngestQueue;
import com.cmict.metricshub.ingest.MetricsPayload;
import com.cmict.metricshub.model.MetricSample;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Processing service that pulls metrics from the ingest queue,
 * decodes OTLP payloads, normalizes them, and prepares for storage.
 *
 * This service runs in the background and continuously processes
 * metrics from the IngestQueue.
 *
 * Processing pipeline:
 * 1. Dequeue MetricsPayload from IngestQueue
 * 2. Decode OTLP protobuf → List<MetricSample>
 * 3. Normalize units and filter labels
 * 4. TODO (T3): Write to storage (Redis + TSDB)
 *
 * For T2, we only decode and normalize without persisting to storage.
 */
@Service
public class ProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessingService.class);

    private final IngestQueue ingestQueue;
    protected final OtlpDecoder decoder;
    protected final Normalizer normalizer;
    private final MeterRegistry meterRegistry;

    private ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    // Metrics
    protected Counter processedCounter;
    protected Counter decodedSamplesCounter;
    protected Counter normalizedSamplesCounter;
    protected Counter errorCounter;
    private Timer processingTimer;

    public ProcessingService(IngestQueue ingestQueue,
                             OtlpDecoder decoder,
                             Normalizer normalizer,
                             MeterRegistry meterRegistry) {
        this.ingestQueue = ingestQueue;
        this.decoder = decoder;
        this.normalizer = normalizer;
        this.meterRegistry = meterRegistry;
        initMetrics();
    }

    private void initMetrics() {
        processedCounter = Counter.builder("processing.payloads.processed")
                .description("Number of payloads processed")
                .register(meterRegistry);

        decodedSamplesCounter = Counter.builder("processing.samples.decoded")
                .description("Number of metric samples decoded")
                .register(meterRegistry);

        normalizedSamplesCounter = Counter.builder("processing.samples.normalized")
                .description("Number of metric samples normalized")
                .register(meterRegistry);

        errorCounter = Counter.builder("processing.errors")
                .description("Number of processing errors")
                .register(meterRegistry);

        processingTimer = Timer.builder("processing.duration")
                .description("Time taken to process a payload")
                .register(meterRegistry);
    }

    /**
     * Start the processing service when application is ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (running.compareAndSet(false, true)) {
            logger.info("🚀 Starting Processing Service...");

            // Create executor service with configurable thread pool
            int threads = Runtime.getRuntime().availableProcessors();
            executorService = Executors.newFixedThreadPool(threads);

            // Start processor threads
            for (int i = 0; i < threads; i++) {
                final int processorId = i;
                executorService.submit(() -> processorLoop(processorId));
            }

            logger.info("✅ Processing Service started with {} processor threads", threads);
        }
    }

    /**
     * Stop the processing service gracefully.
     */
    @PreDestroy
    public void stop() {
        if (running.compareAndSet(true, false)) {
            logger.info("⏹️ Stopping Processing Service...");

            if (executorService != null) {
                executorService.shutdown();
                try {
                    if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                        executorService.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executorService.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            logger.info("✅ Processing Service stopped");
        }
    }

    /**
     * Main processor loop.
     * Continuously dequeues and processes metrics.
     */
    private void processorLoop(int processorId) {
        logger.info("Processor-{} started", processorId);

        while (running.get()) {
            try {
                // Dequeue with timeout to allow graceful shutdown
                MetricsPayload payload = ingestQueue.dequeue(1, TimeUnit.SECONDS);

                if (payload != null) {
                    processPayload(payload, processorId);
                }

            } catch (Exception e) {
                logger.error("Processor-{} encountered error: {}", processorId, e.getMessage(), e);
                errorCounter.increment();
            }
        }

        logger.info("Processor-{} stopped", processorId);
    }

    /**
     * Process a single metrics payload.
     * Protected for testing purposes.
     */
    protected void processPayload(MetricsPayload payload, int processorId) {
        processingTimer.record(() -> {
            try {
                logger.debug("Processor-{} processing payload from {} ({} bytes, age: {}ms)",
                        processorId, payload.getSource(), payload.getSize(), payload.getAgeMs());

                // Step 1: Decode OTLP payload
                List<MetricSample> decodedSamples = decoder.decode(payload.getRawData());
                decodedSamplesCounter.increment(decodedSamples.size());

                logger.debug("Processor-{} decoded {} samples from payload",
                        processorId, decodedSamples.size());

                // Step 2: Normalize samples
                List<MetricSample> normalizedSamples = normalizer.normalize(decodedSamples);
                normalizedSamplesCounter.increment(normalizedSamples.size());

                logger.debug("Processor-{} normalized {} samples ({} filtered out)",
                        processorId, normalizedSamples.size(),
                        decodedSamples.size() - normalizedSamples.size());

                // Step 3: TODO (T3) - Write to storage
                // For T2, we just log the processed samples
                if (logger.isTraceEnabled()) {
                    for (MetricSample sample : normalizedSamples) {
                        logger.trace("Processed sample: {}", sample);
                    }
                }

                processedCounter.increment();

                logger.debug("Processor-{} completed processing payload: {} samples normalized",
                        processorId, normalizedSamples.size());

            } catch (OtlpDecoder.DecoderException e) {
                logger.error("Processor-{} failed to decode payload: {}",
                        processorId, e.getMessage());
                errorCounter.increment();

            } catch (Exception e) {
                logger.error("Processor-{} failed to process payload: {}",
                        processorId, e.getMessage(), e);
                errorCounter.increment();
            }
        });
    }

    /**
     * Check if service is running.
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Get processing statistics.
     */
    public ProcessingStats getStats() {
        return new ProcessingStats(
                (long) processedCounter.count(),
                (long) decodedSamplesCounter.count(),
                (long) normalizedSamplesCounter.count(),
                (long) errorCounter.count(),
                running.get()
        );
    }

    /**
     * Processing statistics record.
     */
    public record ProcessingStats(
            long payloadsProcessed,
            long samplesDecoded,
            long samplesNormalized,
            long errors,
            boolean running
    ) {
    }
}
