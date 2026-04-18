CREATE TABLE IF NOT EXISTS system_task (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    app_name VARCHAR(64),
    task_type VARCHAR(32) NOT NULL,
    task_code VARCHAR(128) NOT NULL,
    task_name VARCHAR(256) NOT NULL,
    cron VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    param_schema TEXT,
    params_json TEXT,
    default_params_json TEXT,
    version BIGINT NOT NULL DEFAULT 1,
    remark VARCHAR(512),
    updated_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY system_task_task_type_task_code_uidx (task_type, task_code),
    KEY system_task_task_type_idx (task_type),
    KEY system_task_status_idx (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS system_task_run_log (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    app_name VARCHAR(64) NOT NULL,
    task_code VARCHAR(128) NOT NULL,
    trigger_type VARCHAR(32) NOT NULL,
    params_json TEXT,
    status VARCHAR(32) NOT NULL,
    started_at DATETIME(6),
    finished_at DATETIME(6),
    duration_ms BIGINT,
    error_msg TEXT,
    trace_id VARCHAR(128),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY system_task_run_log_app_name_task_code_idx (app_name, task_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
