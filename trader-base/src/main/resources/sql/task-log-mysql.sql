CREATE TABLE IF NOT EXISTS task_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_name VARCHAR(100) NOT NULL COMMENT '任务名称',
    task_group VARCHAR(100) COMMENT '任务分组',
    start_time DATETIME(6) NOT NULL COMMENT '任务开始时间',
    end_time DATETIME(6) COMMENT '任务结束时间',
    status VARCHAR(20) NOT NULL COMMENT '任务状态',
    content TEXT COMMENT '执行内容(Markdown)',
    error_msg TEXT COMMENT '错误信息',
    execution_ms BIGINT COMMENT '执行耗时(毫秒)',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    INDEX idx_task_log_name (task_name),
    INDEX idx_task_log_status (status),
    INDEX idx_task_log_start (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务执行日志表';
