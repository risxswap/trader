# 手动执行任务记录执行历史设计

## 背景

当前任务详情页“执行历史”读取的是 `task_log` 数据。

系统中任务执行分为两类：

- 定时执行：Quartz 调度 -> `TraderTaskExecutor`
- 手动执行：`/task/trigger` -> `TASK_TRIGGER` -> `TraderTaskSchedulerService.trigger()` -> `TraderTaskExecutor`

目前任务历史记录主要依赖任务方法上的 `@TraderTaskLog` 注解切面。

这导致一个隐含问题：

- 如果任务本身没有标注 `@TraderTaskLog`，那么即便手动执行或定时执行成功，`task_log` 里也不会留下执行历史

用户希望：

- 手动执行也要记录任务执行历史

为了避免行为分裂，本次设计进一步统一为：

- 只要经过 `TraderTaskExecutor` 的任务执行，无论手动还是定时，都统一记录执行历史

## 目标

1. 手动执行任务时必须写入 `task_log`
2. 定时执行任务继续保留执行历史
3. 不再依赖任务类是否手工标注 `@TraderTaskLog`
4. 同一次任务执行只写一条执行历史，不出现双写
5. 不修改任务详情页读取链路

## 非目标

- 不将任务详情页切换到 `system_task_run_log`
- 不重做执行历史接口协议
- 不在本次改造中清理所有任务类上的 `@TraderTaskLog`
- 不改变任务调度主链路与状态回写链路

## 当前现状

### 现状一：手动执行与定时执行最终都进入执行器

无论手动还是定时执行，最终都走到：

- `TraderTaskExecutor.execute(taskType, taskCode, fireTimeEpochSec)`

这意味着执行器是最稳定、最集中、最适合做统一日志记录的位置。

### 现状二：执行历史目前依赖注解切面

当前 `task_log` 记录主要由：

- `TraderTaskLogAspect`
- `TaskLogStore`

共同完成。

但是 `TraderTaskLogAspect` 只对带 `@TraderTaskLog` 的任务方法生效，因此：

- 注解遗漏时没有历史
- 是否有历史取决于任务作者是否记得加注解

### 现状三：直接在执行器补日志会产生双写风险

如果简单在 `TraderTaskExecutor` 增加 `TaskLogStore` 写入，而不处理现有切面：

- 带 `@TraderTaskLog` 的任务将同时由执行器和切面各写一条 `task_log`

因此必须把“执行器记录历史”和“切面记录历史”的职责边界重新收口。

## 总体设计

### 设计原则

- 执行器成为任务执行历史的统一主链路
- 切面不再对执行器已接管的执行重复写历史
- 对已有任务类注解保持兼容，不要求一次性清理全部 `@TraderTaskLog`

## 统一日志记录设计

### 1. 执行器负责记录历史

在 `TraderTaskExecutor` 中注入：

- `TaskLogStore`

执行器在任务真正调用 `task.execute(context)` 前后统一写日志：

- 开始时：`writeRunning`
- 成功时：`writeFinished(..., "SUCCESS", ...)`
- 失败时：`writeFinished(..., "FAILED", ...)`

### 2. 触发类型统一进入 remark

执行器已有 `TraderTaskContext.triggerType`：

- 定时执行：`SCHEDULED`
- 手动执行：`MANUAL`

本次将其写入 `task_log.remark`，便于前端历史页在不改协议的情况下区分来源。

### 3. traceId 由执行器统一生成

执行器为每次执行生成一次唯一 `traceId`：

- 开始日志和结束日志使用同一 `traceId`

这样任务详情页现有按 `traceId` 的能力仍然可用。

## 切面兼容与去重设计

### 1. 保留切面，但增加“执行器接管日志”标记

在 `TraderTaskExecutor` 进入任务执行前，设置一个线程级别的标记，例如：

- `TaskLogExecutionContext.markManagedByExecutor(true)`

在任务执行结束后清除。

### 2. `TraderTaskLogAspect` 检测该标记

当切面发现当前线程已经由执行器接管日志时：

- 直接跳过 `task_log` 写入
- 仍然放行业务方法执行

这样可以做到：

- 保留任务上的 `@TraderTaskLog` 不报错
- 执行器统一写历史
- 不会双写

### 3. 非执行器场景仍允许切面工作

如果未来存在某些独立任务方法并不经过 `TraderTaskExecutor`，那么：

- 线程标记不存在
- `TraderTaskLogAspect` 仍然保持原有行为

这让本次改造更平滑，不会把系统中非执行器场景一起破坏。

## 日志内容设计

### 开始日志

执行器在开始时写入：

- `taskName`：实例中的 `taskName`
- `taskGroup`：实例的 `taskCode`
- `traceId`：执行器生成
- `status`：`RUNNING`

### 结束日志

成功时：

- `status`：`SUCCESS`
- `executionMs`：执行耗时
- `remark`：包含 `triggerType`、`taskType`、`taskCode`、`paramsJson`、结果摘要

失败时：

- `status`：`FAILED`
- `executionMs`：执行耗时
- `remark`：包含 `triggerType`、`taskType`、`taskCode`、`paramsJson`、异常摘要

## 手动执行链路效果

调整后手动执行链路为：

1. `SystemTaskService.trigger()` 发布 `TASK_TRIGGER`
2. `TraderTaskRefreshSubscriber` 将消息交给调度器
3. `TraderTaskSchedulerService.trigger()` 触发 Quartz Job
4. `TraderTaskExecutor.execute()` 统一写开始日志
5. 执行任务
6. `TraderTaskExecutor.execute()` 统一写结束日志

因此手动执行天然具备与定时执行一致的历史记录能力。

## 实施边界

预计主要改动文件：

- `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskExecutor.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/logging/TraderTaskLogAspect.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfiguration.java`

可能新增：

- `trader-base/src/main/java/cc/riskswap/trader/base/logging/TaskLogExecutionContext.java`

测试文件预计包括：

- `trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskExecutorTest.java`
- `trader-base/src/test/java/cc/riskswap/trader/base/logging/TraderTaskLogAspectTest.java`

## 验证策略

重点验证四组场景：

### 1. 手动执行成功

- 经过执行器后写入一条 `RUNNING`
- 执行成功后更新为 `SUCCESS`
- `remark` 中包含 `triggerType=MANUAL`

### 2. 手动执行失败

- 写入 `RUNNING`
- 执行失败后更新为 `FAILED`
- `remark` 中包含错误摘要

### 3. 定时执行继续有效

- 定时执行仍有历史
- `remark` 中包含 `triggerType=SCHEDULED`

### 4. 无重复历史

- 带 `@TraderTaskLog` 的任务通过执行器执行时，只生成一条历史
- `TraderTaskLogAspect` 在“执行器已接管”场景下不再重复写日志

## 测试策略

- 以 `TraderTaskExecutorTest` 为主补执行器日志行为测试
- 以 `TraderTaskLogAspectTest` 补“执行器已接管时跳过写日志”的测试
- 不新增跨模块重型集成测试

## 验收标准

- 手动执行任务后，任务详情页能看到新的执行历史
- 定时执行历史仍然存在
- 同一次执行不会产生两条重复历史
- 任务是否标注 `@TraderTaskLog` 不再决定是否有执行历史

