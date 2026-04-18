# 任务状态与执行结果拆分设计

## 1. 背景

当前任务执行链路把“是否正在执行”和“最近一次执行结果”都混在 `system_task.status` 一个字段里：

- 执行开始时会写 `RUNNING`
- 执行结束后又可能写回 `IDLE`
- 失败时还可能写 `ERROR`

这会让状态含义不稳定：

- 页面上很难区分“当前正在运行”和“上一次执行成功/失败”
- 用户希望保留任务是否在运行中的状态语义，同时单独展示最近一次执行结果

因此需要把任务运行状态与任务执行结果拆开。

## 2. 目标

- `status` 只表达当前运行态
- `result` 只表达最近一次执行结果
- 执行开始时进入 `RUNNING`
- 执行结束后恢复为 `STOPPED`
- 成功时记录 `SUCCESS`
- 失败时记录 `FAILED`
- 管理端列表、事件消费和数据库结构保持一致

## 3. 状态模型

### 3.1 运行状态 `status`

仅允许以下值：

- `STOPPED`
- `RUNNING`

### 3.2 执行结果 `result`

表示最近一次执行结果：

- `SUCCESS`
- `FAILED`
- `NULL` 或空值：表示尚未执行过

## 4. 状态流转

### 4.1 开始执行

- `status -> RUNNING`
- `result` 保持原值不变

### 4.2 执行成功

- `status -> STOPPED`
- `result -> SUCCESS`

### 4.3 执行失败

- `status -> STOPPED`
- `result -> FAILED`

## 5. 影响范围

### 5.1 数据库

`system_task` 表新增字段：

- `result VARCHAR(16)`，存储最近一次执行结果

需要同时更新：

- 初始建表脚本 `db/mysql.sql`
- 增量升级脚本，新增版本 `1.0.4.sql`

### 5.2 后端模型

以下对象增加 `result` 字段：

- `SystemTask`
- `SystemTaskDto`
- `SystemTaskStatusEvent`
- 相关前后端参数/返回模型

### 5.3 执行链路

在 `TraderTaskExecutor` 中调整状态事件发送逻辑：

- 执行开始：发送 `RUNNING`
- 成功结束：发送 `STOPPED + SUCCESS`
- 失败结束：发送 `STOPPED + FAILED`

### 5.4 管理端流消费

`TraderStreamConsumer.handleSystemTaskStatus()` 需要：

- 更新 `system_task.status`
- 同步更新 `system_task.result`

## 6. 兼容性策略

- 保留 `status` 现有字段，避免大范围重命名
- 新增 `result` 字段来承接“最近一次结果”语义
- 老数据默认 `result` 为空，不强制回填历史结果

## 7. 测试策略

### 7.1 执行器测试

更新 `TraderTaskExecutorTest`：

- 成功场景断言事件顺序：
  - `RUNNING`
  - `STOPPED + SUCCESS`
- 失败场景断言事件顺序：
  - `RUNNING`
  - `STOPPED + FAILED`

### 7.2 流消费测试

增加或更新管理端测试，验证：

- 收到 `SYSTEM_TASK_STATUS` 后
- 能同时把 `status` 和 `result` 写回 `system_task`

### 7.3 升级脚本测试

更新升级脚本测试，验证：

- `1.0.4.sql` 存在
- `mysql.sql` 包含 `system_task.result`

## 8. 验收标准

- 执行任务时列表状态变为 `RUNNING`
- 执行完成后列表状态变回 `STOPPED`
- 成功任务显示 `result=SUCCESS`
- 失败任务显示 `result=FAILED`
- 新库初始化和老库升级都不会缺 `result` 字段
