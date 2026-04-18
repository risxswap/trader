ALTER TABLE system_task
    MODIFY COLUMN task_type VARCHAR(64) NOT NULL DEFAULT '' COMMENT '任务类型';

UPDATE system_task
SET task_type = app_name
WHERE (task_type IS NULL OR task_type = '')
  AND app_name IS NOT NULL
  AND app_name <> '';
