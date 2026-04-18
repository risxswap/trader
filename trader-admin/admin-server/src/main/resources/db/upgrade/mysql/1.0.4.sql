ALTER TABLE system_task
    ADD COLUMN result VARCHAR(16) DEFAULT NULL COMMENT '最近一次执行结果' AFTER status;

CREATE INDEX idx_system_task_result ON system_task (result);
