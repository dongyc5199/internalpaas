ALTER TABLE server_metrics
    ADD COLUMN IF NOT EXISTS kernel_version VARCHAR(255),
    ADD COLUMN IF NOT EXISTS network_interface VARCHAR(255),
    ADD COLUMN IF NOT EXISTS network_received_bytes BIGINT,
    ADD COLUMN IF NOT EXISTS network_transmitted_bytes BIGINT,
    ADD COLUMN IF NOT EXISTS network_received_rate DOUBLE,
    ADD COLUMN IF NOT EXISTS network_transmitted_rate DOUBLE;
