package com.cmict.metricshub.perf;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * OTLP Load Simulator for Performance Testing (T8)
 * 
 * Simulates high-volume OTLP metric ingestion to test Metrics Hub performance.
 * 
 * Features:
 * - Configurable concurrency (number of virtual hosts)
 * - Configurable QPS (queries per second)
 * - Real-time latency tracking (P50, P95, P99)
 * - Success/failure rate monitoring
 * - Detailed performance report generation
 * 
 * Usage:
 * <pre>
 * LoadSimulator simulator = LoadSimulator.builder()
 *     .endpoint("http://localhost:4318/v1/metrics")
 *     .numHosts(1000)
 *     .intervalSeconds(10)
 *     .durationMinutes(30)
 *     .build();
 * 
 * PerformanceReport report = simulator.run();
 * System.out.println(report.generateReport());
 * </pre>
 * 
 * @author Metrics Hub Team
 * @since 1.0.0
 */
@Slf4j
@Builder
public class LoadSimulator {

    // Configuration
    @Builder.Default
    private String endpoint = "http://localhost:4318/v1/metrics";

    @Builder.Default
    private int numHosts = 100;

    @Builder.Default
    private int intervalSeconds = 15;

    @Builder.Default
    private int durationMinutes = 10;

    @Builder.Default
    private int metricsPerHost = 20;

    @Builder.Default
    private int maxConcurrency = 50; // Max parallel requests

    // HTTP Client
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(10))
            .writeTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(10))
            .connectionPool(new ConnectionPool(100, 5, TimeUnit.MINUTES))
            .build();

    // Metrics
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);
    private final List<Long> latencies = new CopyOnWriteArrayList<>();

    /**
     * Run load test
     * 
     * @return Performance report
     */
    public PerformanceReport run() {
        log.info("🚀 Starting Load Simulator");
        log.info("   Endpoint: {}", endpoint);
        log.info("   Hosts: {}", numHosts);
        log.info("   Interval: {}s", intervalSeconds);
        log.info("   Duration: {}min", durationMinutes);
        log.info("   Metrics/Host: {}", metricsPerHost);

        Instant startTime = Instant.now();
        int totalIterations = durationMinutes * 60 / intervalSeconds;
        
        log.info("   Total Iterations: {}", totalIterations);
        log.info("   Expected QPS: ~{}", numHosts / intervalSeconds);
        log.info("");

        ExecutorService executor = Executors.newFixedThreadPool(maxConcurrency);
        Semaphore semaphore = new Semaphore(maxConcurrency);

        try {
            for (int iteration = 1; iteration <= totalIterations; iteration++) {
                Instant iterationStart = Instant.now();
                
                log.info("📊 [Iteration {}/{}] Sending metrics from {} hosts...", 
                    iteration, totalIterations, numHosts);

                // Submit all host requests
                List<Future<?>> futures = new ArrayList<>();
                for (int hostId = 1; hostId <= numHosts; hostId++) {
                    final int id = hostId;
                    
                    // Throttle submissions
                    try {
                        semaphore.acquire();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }

                    Future<?> future = executor.submit(() -> {
                        try {
                            sendMetrics(id);
                        } finally {
                            semaphore.release();
                        }
                    });
                    futures.add(future);
                }

                // Wait for all requests to complete
                for (Future<?> future : futures) {
                    try {
                        future.get();
                    } catch (Exception e) {
                        log.error("Request failed", e);
                    }
                }

                long iterationDuration = Duration.between(iterationStart, Instant.now()).toMillis();
                
                // Print iteration summary
                LatencyStats stats = calculateLatencyStats();
                log.info("   ✓ Completed in {}ms", iterationDuration);
                log.info("   Success: {} | Failure: {} | Success Rate: {:.2f}%",
                    successCount.get(), failureCount.get(),
                    calculateSuccessRate());
                log.info("   Latency: P50={:.0f}ms, P95={:.0f}ms, P99={:.0f}ms",
                    stats.p50, stats.p95, stats.p99);

                // Sleep until next interval
                if (iteration < totalIterations) {
                    long sleepTime = (intervalSeconds * 1000L) - iterationDuration;
                    if (sleepTime > 0) {
                        log.info("   Waiting {}ms until next iteration...", sleepTime);
                        Thread.sleep(sleepTime);
                    }
                }
                log.info("");
            }

        } catch (InterruptedException e) {
            log.error("Load test interrupted", e);
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
            try {
                executor.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }

        Instant endTime = Instant.now();
        long totalDuration = Duration.between(startTime, endTime).toSeconds();

        log.info("✅ Load test completed in {}s", totalDuration);

        return PerformanceReport.builder()
                .startTime(startTime)
                .endTime(endTime)
                .totalRequests(successCount.get() + failureCount.get())
                .successCount(successCount.get())
                .failureCount(failureCount.get())
                .latencies(new ArrayList<>(latencies))
                .numHosts(numHosts)
                .intervalSeconds(intervalSeconds)
                .durationMinutes(durationMinutes)
                .build();
    }

    /**
     * Send metrics for one host
     */
    private void sendMetrics(int serverId) {
        long startTime = System.currentTimeMillis();
        
        String payload = generatePayload(serverId);
        
        Request request = new Request.Builder()
                .url(endpoint)
                .post(RequestBody.create(payload, MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            long latency = System.currentTimeMillis() - startTime;
            latencies.add(latency);

            if (response.isSuccessful()) {
                successCount.incrementAndGet();
            } else {
                failureCount.incrementAndGet();
                log.warn("Request failed for server-{}: HTTP {}", serverId, response.code());
            }

        } catch (IOException e) {
            long latency = System.currentTimeMillis() - startTime;
            latencies.add(latency);
            failureCount.incrementAndGet();
            log.error("Request exception for server-{}: {}", serverId, e.getMessage());
        }
    }

    /**
     * Generate OTLP payload
     */
    private String generatePayload(int serverId) {
        long timestamp = System.currentTimeMillis() * 1_000_000; // nanoseconds
        Random random = ThreadLocalRandom.current();

        StringBuilder metrics = new StringBuilder();
        String[] metricNames = {
            "system.cpu.utilization",
            "system.memory.utilization",
            "system.disk.io.read",
            "system.disk.io.write",
            "system.network.io.receive",
            "system.network.io.transmit"
        };

        for (int i = 0; i < Math.min(metricsPerHost, metricNames.length); i++) {
            if (i > 0) metrics.append(",");
            metrics.append(String.format(
                "{\"name\":\"%s\",\"gauge\":{\"dataPoints\":[{\"asDouble\":%.2f,\"timeUnixNano\":\"%d\"}]}}",
                metricNames[i],
                random.nextDouble() * 100,
                timestamp
            ));
        }

        return String.format(
            "{\"resourceMetrics\":[{\"resource\":{\"attributes\":[" +
            "{\"key\":\"server.id\",\"value\":{\"stringValue\":\"server-%d\"}}," +
            "{\"key\":\"region\",\"value\":{\"stringValue\":\"us-east-1\"}}," +
            "{\"key\":\"env\",\"value\":{\"stringValue\":\"prod\"}}" +
            "]},\"scopeMetrics\":[{\"metrics\":[%s]}]}]}",
            serverId,
            metrics.toString()
        );
    }

    /**
     * Calculate success rate
     */
    private double calculateSuccessRate() {
        long total = successCount.get() + failureCount.get();
        return total > 0 ? (successCount.get() * 100.0 / total) : 0.0;
    }

    /**
     * Calculate latency statistics
     */
    private LatencyStats calculateLatencyStats() {
        if (latencies.isEmpty()) {
            return new LatencyStats(0, 0, 0, 0, 0);
        }

        List<Long> sorted = latencies.stream()
                .sorted()
                .collect(Collectors.toList());

        int size = sorted.size();
        return new LatencyStats(
            sorted.get(0), // min
            sorted.get(size - 1), // max
            sorted.get(size / 2), // p50
            sorted.get((int) (size * 0.95)), // p95
            sorted.get((int) (size * 0.99))  // p99
        );
    }

    /**
     * Latency statistics holder
     */
    @Data
    private static class LatencyStats {
        final double min;
        final double max;
        final double p50;
        final double p95;
        final double p99;
    }

    /**
     * Performance report
     */
    @Data
    @Builder
    public static class PerformanceReport {
        private Instant startTime;
        private Instant endTime;
        private long totalRequests;
        private long successCount;
        private long failureCount;
        private List<Long> latencies;
        private int numHosts;
        private int intervalSeconds;
        private int durationMinutes;

        /**
         * Generate human-readable report
         */
        public String generateReport() {
            long durationSeconds = Duration.between(startTime, endTime).toSeconds();
            double successRate = totalRequests > 0 
                ? (successCount * 100.0 / totalRequests) 
                : 0.0;
            double qps = durationSeconds > 0 
                ? (totalRequests * 1.0 / durationSeconds) 
                : 0.0;

            // Calculate latency percentiles
            List<Long> sorted = latencies.stream().sorted().collect(Collectors.toList());
            int size = sorted.size();
            
            double minLatency = size > 0 ? sorted.get(0) : 0;
            double maxLatency = size > 0 ? sorted.get(size - 1) : 0;
            double p50 = size > 0 ? sorted.get(size / 2) : 0;
            double p95 = size > 0 ? sorted.get((int) (size * 0.95)) : 0;
            double p99 = size > 0 ? sorted.get((int) (size * 0.99)) : 0;
            double avgLatency = latencies.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

            StringBuilder report = new StringBuilder();
            report.append("═══════════════════════════════════════════════════════\n");
            report.append("        OTLP Load Test Performance Report\n");
            report.append("═══════════════════════════════════════════════════════\n\n");

            report.append("Test Configuration:\n");
            report.append(String.format("  Simulated Hosts: %d\n", numHosts));
            report.append(String.format("  Interval: %ds\n", intervalSeconds));
            report.append(String.format("  Duration: %dmin (%ds actual)\n", durationMinutes, durationSeconds));
            report.append("\n");

            report.append("Request Statistics:\n");
            report.append(String.format("  Total Requests: %,d\n", totalRequests));
            report.append(String.format("  Success: %,d\n", successCount));
            report.append(String.format("  Failure: %,d\n", failureCount));
            report.append(String.format("  Success Rate: %.2f%%\n", successRate));
            report.append(String.format("  QPS: %.2f req/s\n", qps));
            report.append("\n");

            report.append("Latency Statistics:\n");
            report.append(String.format("  Min: %.0fms\n", minLatency));
            report.append(String.format("  Max: %.0fms\n", maxLatency));
            report.append(String.format("  Average: %.0fms\n", avgLatency));
            report.append(String.format("  P50 (Median): %.0fms\n", p50));
            report.append(String.format("  P95: %.0fms\n", p95));
            report.append(String.format("  P99: %.0fms\n", p99));
            report.append("\n");

            report.append("Performance Assessment:\n");
            if (p95 < 100) {
                report.append("  ✅ EXCELLENT - P95 latency < 100ms\n");
            } else if (p95 < 300) {
                report.append("  ✓ GOOD - P95 latency < 300ms\n");
            } else if (p95 < 1000) {
                report.append("  ⚠ FAIR - P95 latency < 1s\n");
            } else {
                report.append("  ❌ POOR - P95 latency >= 1s\n");
            }

            if (successRate >= 99.9) {
                report.append("  ✅ EXCELLENT - Success rate >= 99.9%\n");
            } else if (successRate >= 99.0) {
                report.append("  ✓ GOOD - Success rate >= 99%\n");
            } else if (successRate >= 95.0) {
                report.append("  ⚠ FAIR - Success rate >= 95%\n");
            } else {
                report.append("  ❌ POOR - Success rate < 95%\n");
            }

            report.append("\n");
            report.append("═══════════════════════════════════════════════════════\n");

            return report.toString();
        }
    }

    /**
     * Main method for standalone testing
     */
    public static void main(String[] args) {
        // Parse command line arguments
        int numHosts = 100;
        int intervalSeconds = 15;
        int durationMinutes = 10;
        String endpoint = "http://localhost:4318/v1/metrics";

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-n":
                case "--hosts":
                    numHosts = Integer.parseInt(args[++i]);
                    break;
                case "-i":
                case "--interval":
                    intervalSeconds = Integer.parseInt(args[++i]);
                    break;
                case "-d":
                case "--duration":
                    durationMinutes = Integer.parseInt(args[++i]);
                    break;
                case "-e":
                case "--endpoint":
                    endpoint = args[++i];
                    break;
                case "-h":
                case "--help":
                    System.out.println("Usage: LoadSimulator [OPTIONS]");
                    System.out.println("Options:");
                    System.out.println("  -n, --hosts NUM       Number of simulated hosts (default: 100)");
                    System.out.println("  -i, --interval SECS   Reporting interval in seconds (default: 15)");
                    System.out.println("  -d, --duration MINS   Test duration in minutes (default: 10)");
                    System.out.println("  -e, --endpoint URL    OTLP HTTP endpoint (default: http://localhost:4318/v1/metrics)");
                    System.exit(0);
                    break;
            }
        }

        // Run load test
        LoadSimulator simulator = LoadSimulator.builder()
                .endpoint(endpoint)
                .numHosts(numHosts)
                .intervalSeconds(intervalSeconds)
                .durationMinutes(durationMinutes)
                .build();

        PerformanceReport report = simulator.run();
        System.out.println(report.generateReport());
    }
}
