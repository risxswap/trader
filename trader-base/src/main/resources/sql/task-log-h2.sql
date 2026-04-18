CREATE TABLE IF NOT EXISTS task_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_name VARCHAR(100) NOT NULL,
    task_group VARCHAR(100),
    start_time TIMESTAMP(6) NOT NULL,
    end_time TIMESTAMP(6),
    status VARCHAR(20) NOT NULL,
    content CLOB,
    error_msg CLOB,
    execution_ms BIGINT,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP()
);
