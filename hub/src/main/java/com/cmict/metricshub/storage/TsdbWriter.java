package com.cmict.metricshub.storage;

import com.cmict.metricshub.model.MetricSample;
import com.cmict.metricshub.model.ServerMetricSample;
import com.cmict.metricshub.repository.ServerMetricSampleRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * TSDB (Time-Series Database) writer for PostgreSQL/Timescale.
 *
 * Writes normalized metric samples to the database for long-term storage
 * and historical analysis.
 *
 * Features:
 * - Batch insert optimization
 * - Transaction management
 * - JSON serialization for labels
 * - Error handling and recovery
 */
@Component
public class TsdbWriter {

    private static final Logger logger = LoggerFactory.getLogger(TsdbWriter.class);

    @Autowired
    private ServerMetricSampleRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Write a batch of metric samples to TSDB.
     *
     * @param samples List of metric samples to write
     * @return Number of samples successfully written
     */
    @Transactional
    public int writeBatch(List<MetricSample> samples) {
        if (samples == null || samples.isEmpty()) {
            return 0;
        }

        List<ServerMetricSample> entities = new ArrayList<>();

        for (MetricSample sample : samples) {
            try {
                ServerMetricSample entity = convertToEntity(sample);
                if (entity != null) {
                    entities.add(entity);
                }
            } catch (Exception e) {
                logger.warn("Failed to convert sample {}: {}", sample.getName(), e.getMessage());
            }
        }

        if (entities.isEmpty()) {
            logger.warn("No valid entities to write to TSDB");
            return 0;
        }

        try {
            List<ServerMetricSample> saved = repository.saveAll(entities);
            int count = saved.size();
            logger.info("✅ TSDB write completed: {} samples written", count);
            return count;

        } catch (Exception e) {
            logger.error("Failed to write batch to TSDB: {}", e.getMessage(), e);
            throw new TsdbWriteException("Batch write failed", e);
        }
    }

    /**
     * Write a single metric sample to TSDB.
     *
     * @param sample Metric sample to write
     * @return Saved entity, or null if failed
     */
    @Transactional
    public ServerMetricSample write(MetricSample sample) {
        try {
            ServerMetricSample entity = convertToEntity(sample);
            if (entity == null) {
                return null;
            }

            ServerMetricSample saved = repository.save(entity);
            logger.trace("Wrote metric {} to TSDB", sample.getName());
            return saved;

        } catch (Exception e) {
            logger.error("Failed to write metric {} to TSDB: {}",
                    sample.getName(), e.getMessage());
            throw new TsdbWriteException("Single write failed", e);
        }
    }

    /**
     * Convert MetricSample to ServerMetricSample entity.
     *
     * @param sample Source metric sample
     * @return JPA entity, or null if conversion fails
     */
    private ServerMetricSample convertToEntity(MetricSample sample) {
        String serverId = sample.getLabel("server.id");
        if (serverId == null || serverId.isEmpty()) {
            logger.warn("Cannot write to TSDB: missing server.id label for metric {}",
                    sample.getName());
            return null;
        }

        ServerMetricSample entity = new ServerMetricSample();
        entity.setServerId(serverId);
        entity.setMetricName(sample.getName());
        entity.setMetricType(sample.getType());
        entity.setValue(sample.getValue());
        entity.setUnit(sample.getUnit());
        entity.setTimestamp(sample.getTimestamp());

        // Serialize labels to JSON
        try {
            String labelsJson = objectMapper.writeValueAsString(sample.getLabels());
            entity.setLabels(labelsJson);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize labels for metric {}: {}",
                    sample.getName(), e.getMessage());
            entity.setLabels("{}");
        }

        // Set histogram fields if present
        if (sample.getCount() != null) {
            entity.setCount(sample.getCount());
        }
        if (sample.getSum() != null) {
            entity.setSum(sample.getSum());
        }

        return entity;
    }

    /**
     * Delete old metrics before a given timestamp (data retention).
     *
     * @param before Delete samples older than this timestamp
     * @return Number of deleted records
     */
    @Transactional
    public int deleteOldMetrics(java.time.Instant before) {
        try {
            int deleted = repository.deleteByTimestampBefore(before);
            logger.info("Deleted {} old metric samples before {}", deleted, before);
            return deleted;
        } catch (Exception e) {
            logger.error("Failed to delete old metrics: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Exception thrown when TSDB write fails.
     */
    public static class TsdbWriteException extends RuntimeException {
        public TsdbWriteException(String message) {
            super(message);
        }

        public TsdbWriteException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
