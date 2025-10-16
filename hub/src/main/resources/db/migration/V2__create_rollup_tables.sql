-- V2: Create rollup tables for data aggregation and retention
-- This migration creates 5-minute and 1-hour aggregation tables
-- Compatible with both PostgreSQL and Timescale

-- ===================================================================
-- 5-minute rollup table
-- ===================================================================
CREATE TABLE IF NOT EXISTS server_metric_samples_5m (
    id BIGSERIAL PRIMARY KEY,
    server_id VARCHAR(100) NOT NULL,
    metric_name VARCHAR(255) NOT NULL,
    time_bucket TIMESTAMP NOT NULL,  -- 5-minute time bucket
    avg_value DOUBLE PRECISION NOT NULL,
    max_value DOUBLE PRECISION NOT NULL,
    min_value DOUBLE PRECISION NOT NULL,
    sample_count INTEGER NOT NULL,  -- Number of original samples aggregated
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for 5-minute rollup table
CREATE INDEX idx_5m_server_time ON server_metric_samples_5m(server_id, time_bucket DESC);
CREATE INDEX idx_5m_metric_time ON server_metric_samples_5m(metric_name, time_bucket DESC);
CREATE INDEX idx_5m_time ON server_metric_samples_5m(time_bucket DESC);

-- Unique constraint to prevent duplicate aggregations
CREATE UNIQUE INDEX idx_5m_unique ON server_metric_samples_5m(server_id, metric_name, time_bucket);

-- Optional: If using Timescale, convert to hypertable
-- SELECT create_hypertable('server_metric_samples_5m', 'time_bucket', if_not_exists => TRUE);

-- Comments for 5-minute rollup table
COMMENT ON TABLE server_metric_samples_5m IS '5-minute aggregated metric samples';
COMMENT ON COLUMN server_metric_samples_5m.server_id IS 'Server identifier';
COMMENT ON COLUMN server_metric_samples_5m.metric_name IS 'Metric name';
COMMENT ON COLUMN server_metric_samples_5m.time_bucket IS '5-minute time bucket (start of the bucket)';
COMMENT ON COLUMN server_metric_samples_5m.avg_value IS 'Average value in the 5-minute window';
COMMENT ON COLUMN server_metric_samples_5m.max_value IS 'Maximum value in the 5-minute window';
COMMENT ON COLUMN server_metric_samples_5m.min_value IS 'Minimum value in the 5-minute window';
COMMENT ON COLUMN server_metric_samples_5m.sample_count IS 'Number of original samples aggregated';

-- ===================================================================
-- 1-hour rollup table
-- ===================================================================
CREATE TABLE IF NOT EXISTS server_metric_samples_1h (
    id BIGSERIAL PRIMARY KEY,
    server_id VARCHAR(100) NOT NULL,
    metric_name VARCHAR(255) NOT NULL,
    time_bucket TIMESTAMP NOT NULL,  -- 1-hour time bucket
    avg_value DOUBLE PRECISION NOT NULL,
    max_value DOUBLE PRECISION NOT NULL,
    min_value DOUBLE PRECISION NOT NULL,
    sample_count INTEGER NOT NULL,  -- Number of samples aggregated (from 5m rollup or raw)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for 1-hour rollup table
CREATE INDEX idx_1h_server_time ON server_metric_samples_1h(server_id, time_bucket DESC);
CREATE INDEX idx_1h_metric_time ON server_metric_samples_1h(metric_name, time_bucket DESC);
CREATE INDEX idx_1h_time ON server_metric_samples_1h(time_bucket DESC);

-- Unique constraint to prevent duplicate aggregations
CREATE UNIQUE INDEX idx_1h_unique ON server_metric_samples_1h(server_id, metric_name, time_bucket);

-- Optional: If using Timescale, convert to hypertable
-- SELECT create_hypertable('server_metric_samples_1h', 'time_bucket', if_not_exists => TRUE);

-- Comments for 1-hour rollup table
COMMENT ON TABLE server_metric_samples_1h IS '1-hour aggregated metric samples';
COMMENT ON COLUMN server_metric_samples_1h.server_id IS 'Server identifier';
COMMENT ON COLUMN server_metric_samples_1h.metric_name IS 'Metric name';
COMMENT ON COLUMN server_metric_samples_1h.time_bucket IS '1-hour time bucket (start of the bucket)';
COMMENT ON COLUMN server_metric_samples_1h.avg_value IS 'Average value in the 1-hour window';
COMMENT ON COLUMN server_metric_samples_1h.max_value IS 'Maximum value in the 1-hour window';
COMMENT ON COLUMN server_metric_samples_1h.min_value IS 'Minimum value in the 1-hour window';
COMMENT ON COLUMN server_metric_samples_1h.sample_count IS 'Number of samples aggregated';

-- ===================================================================
-- Materialized view for latest rollup status (optional, for monitoring)
-- ===================================================================
CREATE MATERIALIZED VIEW IF NOT EXISTS rollup_status AS
SELECT 
    '5m' as rollup_type,
    MAX(time_bucket) as latest_bucket,
    COUNT(*) as total_buckets,
    SUM(sample_count) as total_samples
FROM server_metric_samples_5m
UNION ALL
SELECT 
    '1h' as rollup_type,
    MAX(time_bucket) as latest_bucket,
    COUNT(*) as total_buckets,
    SUM(sample_count) as total_samples
FROM server_metric_samples_1h;

-- Index for materialized view
CREATE INDEX idx_rollup_status_type ON rollup_status(rollup_type);

COMMENT ON MATERIALIZED VIEW rollup_status IS 'Rollup job status monitoring';
