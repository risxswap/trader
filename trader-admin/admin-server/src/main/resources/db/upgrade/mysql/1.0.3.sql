ALTER TABLE task_log
    ADD COLUMN trace_id VARCHAR(128) DEFAULT NULL COMMENT '执行链路ID' AFTER task_group;

ALTER TABLE task_log
    ADD COLUMN remark TEXT COMMENT '执行备注' AFTER content;

CREATE INDEX idx_task_log_trace_id ON task_log (trace_id);
