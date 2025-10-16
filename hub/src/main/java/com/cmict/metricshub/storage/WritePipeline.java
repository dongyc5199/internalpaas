package com.cmict.metricshub.storage;

import com.cmict.metricshub.model.MetricSample;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Write pipeline coordinator for dual-write strategy.
 *
 * Orchestrates writing metric samples to both:
 * 1. Redis (hot data, latest values)
 * 2. TSDB (cold data, historical analysis)
 *
 * Features:
 * - Parallel writes for performance
 * - Independent error handling (Redis failure doesn't block TSDB)
 * - Metrics instrumentation for monitoring
 * - Async execution for non-blocking operation
 *
 * Error handling strategy:
 * - Redis write failure: Log warning, continue with TSDB
 * - TSDB write failure: Log error, critical data loss
 * - Both failures: Return error to caller for retry
 */
@Component
public class WritePipeline {

    private static final Logger logger = LoggerFactory.getLogger(WritePipeline.class);

    @Autowired
    private RedisWriter redisWriter;

    @Autowired
    private TsdbWriter tsdbWriter;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Value("${metrics-hub.write.async:true}")
    private boolean asyncEnabled;

    // Metrics
    private Counter redisWriteSuccess;
    private Counter redisWriteFailure;
    private Counter tsdbWriteSuccess;
    private Counter tsdbWriteFailure;
    private Timer redisWriteTimer;
    private Timer tsdbWriteTimer;

    @PostConstruct
    public void init() {
        if (meterRegistry != null) {
            redisWriteSuccess = Counter.builder("metrics_hub.redis.write.success")
                    .description("Successful Redis writes")
                    .register(meterRegistry);

            redisWriteFailure = Counter.builder("metrics_hub.redis.write.failure")
                    .description("Failed Redis writes")
                    .register(meterRegistry);

            tsdbWriteSuccess = Counter.builder("metrics_hub.tsdb.write.success")
                    .description("Successful TSDB writes")
                    .register(meterRegistry);

            tsdbWriteFailure = Counter.builder("metrics_hub.tsdb.write.failure")
                    .description("Failed TSDB writes")
                    .register(meterRegistry);

            redisWriteTimer = Timer.builder("metrics_hub.redis.write.duration")
                    .description("Redis write duration")
                    .register(meterRegistry);

            tsdbWriteTimer = Timer.builder("metrics_hub.tsdb.write.duration")
                    .description("TSDB write duration")
                    .register(meterRegistry);
        }

        logger.info("✅ WritePipeline initialized (async={})", asyncEnabled);
    }

    /**
     * Write a batch of metric samples to both Redis and TSDB.
     *
     * @param samples List of metric samples
     * @return Result containing write statistics
     */
    public WriteResult writeBatch(List<MetricSample> samples) {
        if (samples == null || samples.isEmpty()) {
            logger.debug("No samples to write");
            return new WriteResult(0, 0, 0);
        }

        logger.info("📝 Writing batch of {} samples", samples.size());

        if (asyncEnabled) {
            return writeBatchAsync(samples);
        } else {
            return writeBatchSync(samples);
        }
    }

    /**
     * Synchronous batch write (for testing or low-latency requirements).
     */
    private WriteResult writeBatchSync(List<MetricSample> samples) {
        int redisCount = 0;
        int tsdbCount = 0;
        int errors = 0;

        // Write to Redis
        try {
            long start = System.nanoTime();
            redisCount = redisWriter.writeBatch(samples);
            long duration = System.nanoTime() - start;

            if (redisWriteSuccess != null) {
                redisWriteSuccess.increment(redisCount);
            }
            if (redisWriteTimer != null) {
                redisWriteTimer.record(duration, java.util.concurrent.TimeUnit.NANOSECONDS);
            }

            logger.debug("Redis write: {} samples in {}ms",
                    redisCount, duration / 1_000_000);

        } catch (Exception e) {
            logger.error("Redis write failed: {}", e.getMessage(), e);
            if (redisWriteFailure != null) {
                redisWriteFailure.increment();
            }
            errors++;
        }

        // Write to TSDB
        try {
            long start = System.nanoTime();
            tsdbCount = tsdbWriter.writeBatch(samples);
            long duration = System.nanoTime() - start;

            if (tsdbWriteSuccess != null) {
                tsdbWriteSuccess.increment(tsdbCount);
            }
            if (tsdbWriteTimer != null) {
                tsdbWriteTimer.record(duration, java.util.concurrent.TimeUnit.NANOSECONDS);
            }

            logger.debug("TSDB write: {} samples in {}ms",
                    tsdbCount, duration / 1_000_000);

        } catch (Exception e) {
            logger.error("TSDB write failed (CRITICAL): {}", e.getMessage(), e);
            if (tsdbWriteFailure != null) {
                tsdbWriteFailure.increment();
            }
            errors++;
        }

        WriteResult result = new WriteResult(redisCount, tsdbCount, errors);
        logger.info("✅ Batch write completed: {} samples (Redis: {}, TSDB: {}, errors: {})",
                samples.size(), redisCount, tsdbCount, errors);

        return result;
    }

    /**
     * Asynchronous batch write (parallel execution).
     */
    @Async
    private WriteResult writeBatchAsync(List<MetricSample> samples) {
        CompletableFuture<Integer> redisFuture = CompletableFuture.supplyAsync(() -> {
            try {
                long start = System.nanoTime();
                int count = redisWriter.writeBatch(samples);
                long duration = System.nanoTime() - start;

                if (redisWriteSuccess != null) {
                    redisWriteSuccess.increment(count);
                }
                if (redisWriteTimer != null) {
                    redisWriteTimer.record(duration, java.util.concurrent.TimeUnit.NANOSECONDS);
                }

                logger.debug("Redis write: {} samples in {}ms",
                        count, duration / 1_000_000);
                return count;

            } catch (Exception e) {
                logger.error("Redis write failed: {}", e.getMessage());
                if (redisWriteFailure != null) {
                    redisWriteFailure.increment();
                }
                return 0;
            }
        });

        CompletableFuture<Integer> tsdbFuture = CompletableFuture.supplyAsync(() -> {
            try {
                long start = System.nanoTime();
                int count = tsdbWriter.writeBatch(samples);
                long duration = System.nanoTime() - start;

                if (tsdbWriteSuccess != null) {
                    tsdbWriteSuccess.increment(count);
                }
                if (tsdbWriteTimer != null) {
                    tsdbWriteTimer.record(duration, java.util.concurrent.TimeUnit.NANOSECONDS);
                }

                logger.debug("TSDB write: {} samples in {}ms",
                        count, duration / 1_000_000);
                return count;

            } catch (Exception e) {
                logger.error("TSDB write failed (CRITICAL): {}", e.getMessage(), e);
                if (tsdbWriteFailure != null) {
                    tsdbWriteFailure.increment();
                }
                return 0;
            }
        });

        // Wait for both to complete
        CompletableFuture.allOf(redisFuture, tsdbFuture).join();

        int redisCount = redisFuture.getNow(0);
        int tsdbCount = tsdbFuture.getNow(0);
        int errors = 0;
        if (redisCount == 0) errors++;
        if (tsdbCount == 0) errors++;

        WriteResult result = new WriteResult(redisCount, tsdbCount, errors);
        logger.info("✅ Async batch write completed: {} samples (Redis: {}, TSDB: {}, errors: {})",
                samples.size(), redisCount, tsdbCount, errors);

        return result;
    }

    /**
     * Write result statistics.
     */
    public static class WriteResult {
        private final int redisCount;
        private final int tsdbCount;
        private final int errors;

        public WriteResult(int redisCount, int tsdbCount, int errors) {
            this.redisCount = redisCount;
            this.tsdbCount = tsdbCount;
            this.errors = errors;
        }

        public int getRedisCount() {
            return redisCount;
        }

        public int getTsdbCount() {
            return tsdbCount;
        }

        public int getErrors() {
            return errors;
        }

        public boolean isSuccess() {
            return errors == 0;
        }

        @Override
        public String toString() {
            return String.format("WriteResult{redis=%d, tsdb=%d, errors=%d}",
                    redisCount, tsdbCount, errors);
        }
    }
}
