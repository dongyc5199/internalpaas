-- 创建服务器状态标签表
CREATE TABLE IF NOT EXISTS server_status_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    server_id BIGINT NOT NULL,
    tag_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    display_text VARCHAR(255) NOT NULL,
    tag_value VARCHAR(255),
    color_scheme VARCHAR(20),
    priority INTEGER DEFAULT 1,
    details TEXT,
    is_alert BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_server_status_tags_server FOREIGN KEY (server_id) REFERENCES servers(id) ON DELETE CASCADE,
    CONSTRAINT uk_server_tag_type UNIQUE (server_id, tag_type)
);

-- 创建服务器状态快照表
CREATE TABLE IF NOT EXISTS server_status_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    server_id BIGINT NOT NULL,
    connection_status VARCHAR(20) DEFAULT 'UNKNOWN',
    work_directory_status VARCHAR(20) DEFAULT 'UNKNOWN',
    monitoring_status VARCHAR(20) DEFAULT 'UNKNOWN',
    permission_status VARCHAR(20) DEFAULT 'UNKNOWN',
    active_user_count INTEGER DEFAULT 0,
    active_session_count INTEGER DEFAULT 0,
    memory_usage_percent DOUBLE,
    cpu_usage_percent DOUBLE,
    disk_usage_percent DOUBLE,
    system_load_average DOUBLE,
    total_applications INTEGER DEFAULT 0,
    running_applications INTEGER DEFAULT 0,
    alert_count INTEGER DEFAULT 0,
    last_activity TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_server_snapshots_server FOREIGN KEY (server_id) REFERENCES servers(id) ON DELETE CASCADE
);

-- 创建服务器资源阈值表
CREATE TABLE IF NOT EXISTS server_resource_thresholds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    server_id BIGINT NOT NULL,
    resource_type VARCHAR(30) NOT NULL,
    warning_threshold DOUBLE,
    critical_threshold DOUBLE,
    danger_threshold DOUBLE,
    enabled BOOLEAN DEFAULT TRUE,
    notify_enabled BOOLEAN DEFAULT TRUE,
    check_interval_seconds INTEGER DEFAULT 60,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_server_thresholds_server FOREIGN KEY (server_id) REFERENCES servers(id) ON DELETE CASCADE,
    CONSTRAINT uk_server_resource_type UNIQUE (server_id, resource_type)
);

-- 创建索引
CREATE INDEX idx_server_status_tags_server_id ON server_status_tags(server_id);
CREATE INDEX idx_server_status_tags_priority ON server_status_tags(priority DESC);
CREATE INDEX idx_server_status_tags_alert ON server_status_tags(is_alert);
CREATE INDEX idx_server_status_tags_updated ON server_status_tags(last_updated);

CREATE INDEX idx_server_snapshots_server_id ON server_status_snapshots(server_id);
CREATE INDEX idx_server_snapshots_created ON server_status_snapshots(created_at);

CREATE INDEX idx_server_thresholds_server_id ON server_resource_thresholds(server_id);
CREATE INDEX idx_server_thresholds_enabled ON server_resource_thresholds(enabled);

-- 插入默认的资源阈值配置
INSERT INTO server_resource_thresholds (server_id, resource_type, warning_threshold, critical_threshold, danger_threshold, description)
SELECT 
    s.id,
    'MEMORY',
    70.0,
    80.0,
    90.0,
    '内存使用率监控，80%以上需要立即处理'
FROM servers s
WHERE NOT EXISTS (
    SELECT 1 FROM server_resource_thresholds srt 
    WHERE srt.server_id = s.id AND srt.resource_type = 'MEMORY'
);

INSERT INTO server_resource_thresholds (server_id, resource_type, warning_threshold, critical_threshold, danger_threshold, description)
SELECT 
    s.id,
    'CPU',
    80.0,
    90.0,
    95.0,
    'CPU使用率监控'
FROM servers s
WHERE NOT EXISTS (
    SELECT 1 FROM server_resource_thresholds srt 
    WHERE srt.server_id = s.id AND srt.resource_type = 'CPU'
);

INSERT INTO server_resource_thresholds (server_id, resource_type, warning_threshold, critical_threshold, danger_threshold, description)
SELECT 
    s.id,
    'DISK',
    80.0,
    90.0,
    95.0,
    '磁盘使用率监控'
FROM servers s
WHERE NOT EXISTS (
    SELECT 1 FROM server_resource_thresholds srt 
    WHERE srt.server_id = s.id AND srt.resource_type = 'DISK'
);