# 任务定义/实例拆分设计（方案 2）

## 1. 背景

当前任务模块在 `trader-base` 中以 `SystemTask` 为核心实现了“任务配置 + Quartz 调度 + Redis 刷新”的闭环（参考 [2026-04-15-task-module-design.md](file:///Users/haiming/Workspace/trader/trader-docs/superpowers/specs/2026-04-15-task-module-design.md)）。但在产品语义上，需要进一步区分：

- 任务“被某个节点实现并具备执行能力”（定义/能力）
- 任务“被系统创建并开始调度”（实例/调度配置）

同时，数据采集、数据统计、策略执行都需要抽象为统一的定时任务体系，但在管理端表现与生命周期不同：

- 采集/统计：通过任务管理“创建任务实例”开启调度
- 策略：仍在投资模块创建/调度，但需在任务管理页聚合展示

本设计选择“方案 2：Redis 存任务定义，DB 存任务实例”，并以 `(taskType, taskCode)` 作为全局唯一键。

## 2. 目标

- 任务定义与任务实例解耦：节点上报定义到 Redis；任务实例由管理端创建并落库。
- 同一 `(taskType, taskCode)` 的定义在 Redis 只保留一条记录。
- 任务实例不选择执行器；执行节点在运行时抢占，保证“全局单实例”执行语义。
- 管理端任务管理页展示：
  - 普通任务实例（采集/统计/通用执行器任务）
  - 策略任务（归类为 EXECUTOR，但仍由投资模块创建/调度）

## 3. 术语与约束

- **taskType**：任务类型枚举，固定为 `COLLECTOR / STATISTIC / EXECUTOR`。
- **taskCode**：任务编码，在同一 taskType 下全局唯一。
- **Task Definition（任务定义/能力）**：节点实现并上报到 Redis 的任务元数据。
- **Task Instance（任务实例）**：管理端创建并写入数据库 `system_task` 的可调度任务配置。
- **执行语义**：全局单实例；同一触发点（同一 fireTime）只能有一个在线节点执行。
- **不选执行器**：任务实例不绑定 nodeId，不在创建时指定执行节点。

## 4. 总体架构

- **任务定义**：节点启动时扫描本地任务实现，将定义写入 Redis，Key 以 `(taskType, taskCode)` 去重（覆盖写）。
- **任务实例**：管理端从 Redis 定义列表选择任务进行创建，写入 `system_task`；节点调度只读取 `system_task` 中已创建的实例。
- **动态刷新**：管理端对任务实例的创建/更新/删除后，通过 Redis Pub/Sub 发布刷新事件，节点立即刷新对应任务；并保留数据库轮询兜底机制。
- **执行抢占**：Quartz 触发后执行前通过 Redis 分布式锁抢占执行权，保证全局单实例。
- **策略任务**：投资模块继续负责策略的创建/调度；任务管理页聚合展示为 EXECUTOR 类型“虚拟任务行”。

## 5. 任务抽象与实现约定

### 5.1 任务接口

复用现有 `TraderTask` 执行入口（见 [TraderTask](file:///Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTask.java)），新增 taskType 维度建议用“三个抽象类/接口”体现，不改变现有 execute 语义：

- `CollectorTask extends TraderTask`
- `StatisticTask extends TraderTask`
- `ExecutorTask extends TraderTask`

每类任务在运行时都能映射出一个 `taskType`，用于：

- 上报 Redis 定义 key
- `system_task` 实例唯一键的一部分
- 节点侧仅调度与自身 nodeType 匹配的任务实例

节点类型读取来源为现有 [TraderNodeProperties](file:///Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/config/TraderNodeProperties.java) 的 `type`（由部署环境注入）。

### 5.2 任务实现上报约定

节点启动后：

1. 扫描 Spring 容器内所有 `TraderTask`（或三类子接口）。
2. 将任务定义 upsert 到 Redis（覆盖写）。
3. 不向数据库写入 `system_task`（实例表）记录；实例必须由管理端创建。

## 6. Redis：任务定义（Definition）数据模型

### 6.1 Key 设计

- Key：`trader:task:def:{taskType}:{taskCode}`

同一 `(taskType, taskCode)` 多节点上报时覆盖写即可，天然保证“只保留一条记录”。

### 6.2 Value 结构（JSON）

建议字段：

- `taskType`：`COLLECTOR | STATISTIC | EXECUTOR`
- `taskCode`
- `taskName`
- `defaultCron`
- `defaultEnabled`
- `paramSchema`
- `defaultParamsJson`
- `implClass`
- `reportNodeId`
- `reportNodeType`
- `reportAt`

说明：

- `defaultEnabled` 仅作为“创建任务实例”的默认值参考，定义本身不触发调度。
- `paramSchema` 与 `defaultParamsJson` 用于后台创建实例时的默认表单渲染与初始化。

### 6.3 列表查询策略

管理端需要展示“可创建的任务定义列表”，建议使用：

- `SCAN trader:task:def:*`（优先）

若担心 Redis keys 操作的性能风险，可在后续迭代加一个索引集合：

- `SADD trader:task:def:index {taskType}:{taskCode}`

本阶段默认 SCAN，且约束系统内任务定义数量处于可控规模（远小于百万级）。

## 7. DB：任务实例（Instance）数据模型

### 7.1 system_task 表定位调整

`system_task` 从“应用内任务配置表”调整为“全局任务实例表”，以 `(task_type, task_code)` 作为唯一键。

### 7.2 字段与索引

建议字段（在现有字段基础上扩展）：

- `task_type`：varchar，非空，枚举 `COLLECTOR/STATISTIC/EXECUTOR`
- `task_code`：varchar，非空
- `task_name`：varchar
- `cron`：varchar
- `status`：varchar，`RUNNING/STOPPED`
- `param_schema`：text（实例快照）
- `params_json`：text
- `default_params_json`：text
- `version`：bigint
- `updated_at` / `created_at`

索引建议：

- 唯一索引：`(task_type, task_code)`
- 查询索引：`(task_type, status)`、`task_code`

兼容建议：

- 原字段 `app_name` 可保留但不再参与唯一约束；历史数据若需要迁移可将其归档或统一置为固定值。

## 8. 管理端：任务实例生命周期

### 8.1 创建任务实例

1. 管理端从 Redis 定义列表选择 `(taskType, taskCode)`。
2. 若实例已存在（DB 唯一键冲突）则提示“已创建”。
3. 若不存在则插入 `system_task`：
   - `task_type/task_code/task_name` 来自定义
   - `cron` 使用 `defaultCron`
   - `param_schema/default_params_json` 取定义快照
   - `params_json` 初始化为 `defaultParamsJson`
   - `status` 初始值：建议 `STOPPED`（创建后再由用户显式启动），或由 `defaultEnabled` 决定；实现时二选一并在 UI 上明确提示
4. `version` 初始化为 1；发布刷新消息。

### 8.2 更新任务实例

支持修改：

- `cron`
- `status`
- `params_json`
- `remark`

更新后：

- `version + 1`
- 发布刷新消息

### 8.3 删除任务实例

删除用于严格语义“通过创建开启，通过删除关闭”：

- 删除 DB 记录
- 发布刷新消息（事件类型为 DELETE）
- 节点收到后移除对应 Quartz job

若希望保留历史配置，可使用“软删除”：

- 不删除记录，只设置 `status=STOPPED`

本设计默认支持物理删除，软删除可作为权限/审计诉求出现后的补充。

## 9. 节点侧：调度刷新与执行流程

### 9.1 调度可见范围

节点只调度与自身 nodeType 匹配的任务实例：

- COLLECTOR 节点：仅调度 `task_type=COLLECTOR`
- STATISTIC 节点：仅调度 `task_type=STATISTIC`
- EXECUTOR 节点：仅调度 `task_type=EXECUTOR`

### 9.2 刷新触发

沿用现有任务模块刷新机制：

- Pub/Sub Topic：`trader:task:refresh`
- 消息结构：`{ taskType, taskCode, version, eventType }`
- 轮询兜底：定时拉取当前 nodeType 对应的实例列表，比对版本刷新

### 9.3 执行抢占（全局单实例）

在 Quartz job 执行入口（统一代理 Job）中：

1. 从 DB 获取最新实例配置（以 DB 为准）。
2. 若 `status=STOPPED` 则不执行。
3. 基于触发时间构造分布式锁 key：
   - `lockKey = task:run:{taskType}:{taskCode}:{fireTimeEpochSec}`
4. 通过 Redis 分布式锁抢占：
   - 成功：继续执行任务
   - 失败：直接返回（表示该触发点已被其他节点执行）

锁过期时间建议配置化：

- `trader.task.lock-expire-seconds`，默认 `600`

说明：

- 触发点维度的锁 key 允许下一次 cron 触发重新抢占，不依赖“释放锁成功与否”来恢复调度能力。

## 10. 策略任务（投资）在任务管理展示

### 10.1 归类规则

- 策略任务归类为 `taskType=EXECUTOR`
- 但它不是 `system_task` 实例，不走“创建实例/刷新/抢占”这条链路
- 策略调度仍由投资模块创建（见 [InvestmentService](file:///Users/haiming/Workspace/trader/trader-executor/src/main/java/cc/riskswap/trader/executor/service/InvestmentService.java#L160-L219)）

### 10.2 管理端聚合展示

任务管理列表以“统一视图”展示两类数据：

- 普通任务：查询 `system_task`
- 策略任务：查询投资列表（投资模块接口或 admin-server 直查投资表），映射为“虚拟任务行”

虚拟任务行字段映射建议：

- `taskType=EXECUTOR`
- `taskCode=investment:{investmentId}`
- `taskName=investment.name`
- `cron=investment.cron`
- `status=investment.status`
- `sourceType=INVESTMENT`（用于前端决定操作走向）

操作约束：

- 普通任务：启停/改 cron/改 params 走 `system_task` 的接口
- 策略任务：启停/改 cron 跳转或调用投资接口，不写 `system_task`

## 11. 接口清单（admin-server）

在现有 `/task/*` 基础上扩展：

- `POST /task/definition/list`
  - 从 Redis 扫描任务定义列表，支持按 `taskType/taskCode/taskName` 过滤
- `POST /task/instance/create`
  - 入参：`taskType, taskCode`
  - 行为：从 Redis 读取定义快照并创建 `system_task` 记录
- `POST /task/list`
  - 行为：返回普通任务实例列表；支持 `includeInvestment=true` 时聚合返回策略任务虚拟行

## 12. 风险与控制

- Redis 定义列表依赖 SCAN：限制任务定义数量规模；必要时引入集合索引。
- 多节点抢占导致“执行节点漂移”：符合“不选执行器”的设计，若后续需要固定执行节点，可在实例上增加 `preferredNodeId` 作为扩展字段。
- 锁过期导致重复执行：通过 `{fireTime}` 维度 key 降低概率；过期时间需要根据任务最长耗时设置，或后续引入续租机制。

