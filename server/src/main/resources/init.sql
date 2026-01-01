drop table if exists commands;
drop table if exists devices;
-- Device table
CREATE TABLE IF NOT EXISTS devices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    remark_name VARCHAR(255),
    ip_address VARCHAR(45) NOT NULL UNIQUE,
    sync_frequency INT DEFAULT 30,
    status_code INT DEFAULT 0,
    info TEXT,
    last_heartbeat_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name (name),
    INDEX idx_ip_address (ip_address),
    INDEX idx_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Command table
CREATE TABLE IF NOT EXISTS commands (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL,
    command_type VARCHAR(100) NOT NULL,
    command_content TEXT,
    status VARCHAR(50) DEFAULT 'pending',
    result TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    executed_at DATETIME,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE,
    INDEX idx_device_id (device_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
