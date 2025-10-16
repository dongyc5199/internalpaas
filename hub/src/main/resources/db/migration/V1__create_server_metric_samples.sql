-- V1: Create server_metric_samples table for time-series data storage
-- Compatible with both PostgreSQL and Timescale

-- Main table for storing metric samples
CREATE TABLE IF NOT EXISTS server_metric_samples (
    id BIGSERIAL PRIMARY KEY,
    server_id VARCHAR(100) NOT NULL,
    metric_name VARCHAR(255) NOT NULL,
    metric_type VARCHAR(50) NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    unit VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    labels TEXT,
    count BIGINT,
    sum DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for query optimization
CREATE INDEX idx_server_timestamp ON server_metric_samples(server_id, timestamp DESC);
CREATE INDEX idx_metric_timestamp ON server_metric_samples(metric_name, timestamp DESC);
CREATE INDEX idx_timestamp ON server_metric_samples(timestamp DESC);

-- Optional: If using Timescale, convert to hypertable
-- SELECT create_hypertable('server_metric_samples', 'timestamp', if_not_exists => TRUE);

-- Comments for documentation
COMMENT ON TABLE server_metric_samples IS 'Time-series metric samples from OTLP ingestion';
COMMENT ON COLUMN server_metric_samples.server_id IS 'Server identifier from labels';
COMMENT ON COLUMN server_metric_samples.metric_name IS 'Metric name (e.g., system.cpu.usage)';
COMMENT ON COLUMN server_metric_samples.metric_type IS 'Metric type: GAUGE, COUNTER, or HISTOGRAM';
COMMENT ON COLUMN server_metric_samples.metric_value IS 'Normalized metric value';
COMMENT ON COLUMN server_metric_samples.unit IS 'Normalized unit (bytes/seconds/percent/count)';
COMMENT ON COLUMN server_metric_samples.timestamp IS 'Sample timestamp (UTC)';
COMMENT ON COLUMN server_metric_samples.labels IS 'Additional labels as JSON';
COMMENT ON COLUMN server_metric_samples.count IS 'Histogram sample count (null for non-histogram)';
COMMENT ON COLUMN server_metric_samples.sum IS 'Histogram sum (null for non-histogram)';
COMMENT ON COLUMN server_metric_samples.created_at IS 'Record creation time';
