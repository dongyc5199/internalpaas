package com.cmict.metricshub.processing;

import com.cmict.metricshub.model.MetricSample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Metric normalizer.
 *
 * This component performs:
 * 1. Unit normalization: Convert all units to standard forms
 *    - Memory: KB/MB/GB → bytes
 *    - Time: ms/us/ns → seconds
 *    - Ratios: 0-1 → percent (0-100)
 * 2. Label filtering: Only keep whitelisted labels
 *    - server.id, region, group, env
 * 3. Default value handling: Fill in missing values
 *
 * Configuration:
 * - Label whitelist can be configured via application.yaml
 * - Unrecognized metrics are logged at DEBUG level and ignored
 */
@Component
public class Normalizer {

    private static final Logger logger = LoggerFactory.getLogger(Normalizer.class);

    @Value("${metrics-hub.normalizer.label-whitelist:server.id,region,group,env}")
    private String labelWhitelistConfig;

    @Value("${metrics-hub.normalizer.log-unrecognized:true}")
    private boolean logUnrecognized;

    private Set<String> labelWhitelist;

    // Unit conversion factors
    private static final Map<String, Double> BYTE_CONVERSIONS = Map.of(
            "b", 1.0,
            "bytes", 1.0,
            "kb", 1024.0,
            "mb", 1024.0 * 1024.0,
            "gb", 1024.0 * 1024.0 * 1024.0,
            "tb", 1024.0 * 1024.0 * 1024.0 * 1024.0
    );

    private static final Map<String, Double> TIME_CONVERSIONS = Map.of(
            "s", 1.0,
            "seconds", 1.0,
            "ms", 0.001,
            "milliseconds", 0.001,
            "us", 0.000001,
            "microseconds", 0.000001,
            "ns", 0.000000001,
            "nanoseconds", 0.000000001
    );

    // Known metric name patterns
    private static final Set<String> RECOGNIZED_METRICS = Set.of(
            // CPU
            "system.cpu.usage",
            "system.cpu.utilization",
            "system.cpu.time",
            "process.cpu.usage",

            // Memory
            "system.memory.usage",
            "system.memory.utilization",
            "system.memory.available",
            "system.memory.used",
            "process.memory.usage",
            "process.memory.virtual",
            "process.memory.physical",

            // Disk
            "system.disk.usage",
            "system.disk.utilization",
            "system.disk.io.bytes",
            "system.disk.io.operations",
            "system.disk.io.time",
            "system.filesystem.usage",
            "system.filesystem.utilization",

            // Network
            "system.network.io.bytes",
            "system.network.io.packets",
            "system.network.errors",
            "system.network.dropped",
            "system.network.connections",

            // Load
            "system.load.average.1m",
            "system.load.average.5m",
            "system.load.average.15m",

            // Process
            "process.threads",
            "process.open.file.descriptors"
    );

    @PostConstruct
    public void init() {
        // Parse label whitelist from config
        labelWhitelist = Arrays.stream(labelWhitelistConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        logger.info("✅ Normalizer initialized with label whitelist: {}", labelWhitelist);
    }

    /**
     * Normalize a list of metric samples.
     *
     * @param samples Raw metric samples from decoder
     * @return Normalized metric samples
     */
    public List<MetricSample> normalize(List<MetricSample> samples) {
        List<MetricSample> normalized = new ArrayList<>();

        for (MetricSample sample : samples) {
            try {
                MetricSample normalizedSample = normalizeSample(sample);
                if (normalizedSample != null) {
                    normalized.add(normalizedSample);
                }
            } catch (Exception e) {
                logger.warn("Failed to normalize metric {}: {}",
                        sample.getName(), e.getMessage());
            }
        }

        logger.debug("Normalized {} samples ({} input, {} output)",
                samples.size(), samples.size(), normalized.size());

        return normalized;
    }

    /**
     * Normalize a single metric sample.
     */
    private MetricSample normalizeSample(MetricSample sample) {
        // Check if metric is recognized
        if (!isRecognizedMetric(sample.getName())) {
            if (logUnrecognized) {
                logger.debug("Unrecognized metric (ignored): {}", sample.getName());
            }
            return null;  // Ignore unrecognized metrics
        }

        // Create normalized copy
        MetricSample normalized = MetricSample.builder()
                .name(sample.getName())
                .type(sample.getType())
                .value(sample.getValue())
                .unit(sample.getUnit())
                .timestamp(sample.getTimestamp())
                .labels(sample.getLabels())
                .count(sample.getCount())
                .sum(sample.getSum())
                .build();

        // 1. Normalize unit
        normalizeUnit(normalized);

        // 2. Filter labels
        filterLabels(normalized);

        // 3. Apply default values
        applyDefaults(normalized);

        return normalized;
    }

    /**
     * Check if a metric name is recognized.
     */
    private boolean isRecognizedMetric(String metricName) {
        // Exact match
        if (RECOGNIZED_METRICS.contains(metricName)) {
            return true;
        }

        // Prefix match for custom metrics
        return metricName.startsWith("system.") ||
                metricName.startsWith("process.") ||
                metricName.startsWith("custom.");
    }

    /**
     * Normalize metric unit to standard form.
     */
    private void normalizeUnit(MetricSample sample) {
        String unit = sample.getUnit().toLowerCase();

        // Memory units → bytes
        if (BYTE_CONVERSIONS.containsKey(unit)) {
            double factor = BYTE_CONVERSIONS.get(unit);
            sample.setValue(sample.getValue() * factor);
            sample.setUnit("bytes");
            logger.trace("Converted {} {} to {} bytes",
                    sample.getValue() / factor, unit, sample.getValue());
        }
        // Time units → seconds
        else if (TIME_CONVERSIONS.containsKey(unit)) {
            double factor = TIME_CONVERSIONS.get(unit);
            sample.setValue(sample.getValue() * factor);
            sample.setUnit("seconds");
            logger.trace("Converted {} {} to {} seconds",
                    sample.getValue() / factor, unit, sample.getValue());
        }
        // Ratio (0-1) → percent (0-100), or dimensionless → count
        else if (unit.equals("1") || unit.equals("ratio")) {
            // Check if value looks like a ratio (0-1)
            if (sample.getValue() >= 0 && sample.getValue() <= 1.1) {
                // Check metric name to determine if it should be percentage
                if (isPercentageMetric(sample.getName())) {
                    sample.setValue(sample.getValue() * 100);
                    sample.setUnit("percent");
                    logger.trace("Converted ratio {} to {}%",
                            sample.getValue() / 100, sample.getValue());
                } else {
                    // Non-percentage dimensionless metric → count
                    sample.setUnit("count");
                }
            } else {
                // Value outside ratio range → count
                sample.setUnit("count");
            }
        }
        // Percent already → keep as-is
        else if (unit.equals("%") || unit.equals("percent") || unit.equals("percentage")) {
            sample.setUnit("percent");
        }
        // Count → keep as-is
        else if (unit.equals("count")) {
            sample.setUnit("count");
        }
        // Unknown unit → keep as-is and log
        else {
            logger.debug("Unknown unit '{}' for metric {}, keeping as-is",
                    unit, sample.getName());
        }
    }

    /**
     * Check if a metric should be expressed as percentage.
     */
    private boolean isPercentageMetric(String metricName) {
        return metricName.contains("usage") ||
                metricName.contains("utilization") ||
                metricName.contains("percent");
    }

    /**
     * Filter labels to only keep whitelisted ones.
     */
    private void filterLabels(MetricSample sample) {
        Map<String, String> originalLabels = sample.getLabels();
        Map<String, String> filteredLabels = new HashMap<>();

        for (Map.Entry<String, String> entry : originalLabels.entrySet()) {
            if (labelWhitelist.contains(entry.getKey())) {
                filteredLabels.put(entry.getKey(), entry.getValue());
            } else {
                logger.trace("Filtered out label: {}={}", entry.getKey(), entry.getValue());
            }
        }

        sample.setLabels(filteredLabels);

        if (originalLabels.size() != filteredLabels.size()) {
            logger.debug("Filtered labels for metric {}: {} → {} labels",
                    sample.getName(), originalLabels.size(), filteredLabels.size());
        }
    }

    /**
     * Apply default values for missing required labels.
     */
    private void applyDefaults(MetricSample sample) {
        // Ensure required labels exist with defaults
        if (!sample.getLabels().containsKey("env")) {
            sample.addLabel("env", "unknown");
        }

        // Ensure value is not NaN or infinite
        if (Double.isNaN(sample.getValue()) || Double.isInfinite(sample.getValue())) {
            logger.warn("Invalid value for metric {}: {}, setting to 0",
                    sample.getName(), sample.getValue());
            sample.setValue(0.0);
        }
    }

    /**
     * Get the configured label whitelist.
     */
    public Set<String> getLabelWhitelist() {
        return Collections.unmodifiableSet(labelWhitelist);
    }
}
