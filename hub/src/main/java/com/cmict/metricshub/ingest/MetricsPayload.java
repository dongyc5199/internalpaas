package com.cmict.metricshub.ingest;

import java.time.Instant;
import java.util.Map;

/**
 * Represents an OTLP metrics payload in the ingestion queue.
 *
 * This is a wrapper around the raw OTLP data with metadata for tracking
 * and processing purposes.
 */
public class MetricsPayload {

    private final byte[] rawData;
    private final String source;  // "grpc" or "http"
    private final Instant receivedAt;
    private final Map<String, String> metadata;

    public MetricsPayload(byte[] rawData, String source, Map<String, String> metadata) {
        this.rawData = rawData;
        this.source = source;
        this.receivedAt = Instant.now();
        this.metadata = metadata;
    }

    public MetricsPayload(byte[] rawData, String source) {
        this(rawData, source, Map.of());
    }

    /**
     * Get the raw OTLP protobuf data.
     */
    public byte[] getRawData() {
        return rawData;
    }

    /**
     * Get the ingestion source ("grpc" or "http").
     */
    public String getSource() {
        return source;
    }

    /**
     * Get the timestamp when this payload was received.
     */
    public Instant getReceivedAt() {
        return receivedAt;
    }

    /**
     * Get metadata (e.g., content-type, encoding).
     */
    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * Get the size of the raw data in bytes.
     */
    public int getSize() {
        return rawData != null ? rawData.length : 0;
    }

    /**
     * Get processing age in milliseconds.
     */
    public long getAgeMs() {
        return Instant.now().toEpochMilli() - receivedAt.toEpochMilli();
    }

    @Override
    public String toString() {
        return String.format("MetricsPayload{source='%s', size=%d bytes, age=%dms}",
                source, getSize(), getAgeMs());
    }
}
