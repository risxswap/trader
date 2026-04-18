# trader-base 任务模块设计

## 1. 背景与目标

当前各业务项目中的定时任务主要通过 Spring `@Scheduled` 直接声明，例如 `trader-collector` 中的基金同步任务。这种方式存在以下问题：

- 任务定义分散在各业务项目中，缺少统一抽象。
- 运行周期依赖配置文件或代码，无法在管理后台动态调整。
- 任务启停无法由后台统一管理。
- 任务执行所需的额外参数无法标准化配置。
- 配置变更后通常依赖重启或静态刷新，缺少即时生效能力。

本次设计目标是在 `trader-base` 中抽象统一的任务模块，供 `trader-collector`、`trader-executor` 等项目集成使用，并由 `trader-admin` 提供任务管理能力。

目标如下：

- 在 `trader-base` 中沉淀统一的任务定义、调度、配置同步和执行日志能力。
- 业务项目仅负责声明任务和实现任务逻辑，不直接依赖 `@Scheduled`。
- 管理后台可管理任务的启停、Cron 周期和任务额外参数。
- 任务配置修改后通过 `Redis Pub/Sub` 即时通知，并通过定时轮询机制兜底。
- 设计保持模块边界清晰，符合现有前后端分离、MVC 分层、DAO 下沉的架构约束。

## 2. 范围

本设计覆盖以下内容：

- `trader-base` 任务抽象接口与自动配置。
- Quartz 调度集成与统一执行代理。
- 任务元数据与运行配置的数据模型。
- Redis Pub/Sub 配置刷新通知机制。
- 定时轮询兜底同步机制。
- 任务额外参数的 JSON + Schema 配置模型。
- `trader-admin` 管理端的接口与页面能力边界。
- `trader-collector`、`trader-executor` 的任务接入方式。

本设计暂不覆盖以下内容：

- 多节点任务分片执行。
- 分布式抢占锁和单任务单实例执行策略。
- 复杂工作流编排。
- 失败重试编排、告警平台联动。
- 权限系统的细粒度任务操作鉴权。

## 3. 总体方案

采用以下核心方案：

- **调度引擎**：Quartz Scheduler。
- **任务声明方式**：接口实现 + Spring 自动发现。
- **配置变更传播**：Redis Pub/Sub 即时通知 + 数据库定时轮询兜底。
- **参数模型**：JSON + Schema。
- **后台管理方式**：`trader-admin` 修改数据库配置并发布刷新通知，各业务服务自动感知并本地重建任务。

整体思路如下：

1. 业务项目实现统一任务接口 `TraderTask`。
2. `trader-base` 启动时扫描所有任务 Bean，并同步到任务配置表。
3. 调度模块基于数据库中保存的任务状态、Cron 和参数创建或更新 Quartz Job。
4. 管理后台修改任务配置后：
   - 写入数据库；
   - 发布 Redis 刷新消息。
5. 各业务服务收到 Redis 消息后立即刷新对应任务。
6. 若消息丢失，则由本地轮询同步任务定时修正本地调度状态。

## 4. 架构分层

### 4.1 trader-base

负责通用任务基础设施：

- 任务抽象接口。
- 任务上下文对象。
- Quartz 自动配置。
- 调度注册/刷新服务。
- Redis 通知发布与监听。
- 任务配置 DAO、Entity、Query、DTO。
- 统一 Quartz 执行代理 Job。
- 可选的任务运行日志扩展能力。

### 4.2 业务项目

例如 `trader-collector`、`trader-executor`：

- 仅实现 `TraderTask` 接口。
- 提供具体业务执行逻辑。
- 不直接声明 `@Scheduled`。
- 不负责调度器的启停和配置同步。

### 4.3 trader-admin

负责管理能力：

- 查询任务列表。
- 修改启停状态。
- 修改 Cron。
- 修改任务参数。
- 手动触发任务执行。
- 发布任务刷新消息。

## 5. 核心接口设计

### 5.1 任务接口

建议在 `trader-base` 中定义：

```java
public interface TraderTask {

    String getTaskCode();

    String getTaskName();

    String getDefaultCron();

    boolean defaultEnabled();

    String getParamSchema();

    String getDefaultParams();

    void execute(TraderTaskContext context);
}
```

字段说明：

- `getTaskCode()`：任务唯一编码，全局在当前应用内唯一，例如 `fundMarketSync`。
- `getTaskName()`：后台展示名称，例如“同步基金行情数据”。
- `getDefaultCron()`：任务默认调度周期。
- `defaultEnabled()`：首次注册时是否默认启用。
- `getParamSchema()`：参数表单 Schema，供后台渲染与校验。
- `getDefaultParams()`：默认参数 JSON。
- `execute(context)`：任务执行入口。

### 5.2 任务上下文

建议定义：

```java
public class TraderTaskContext {
    private String appName;
    private String taskCode;
    private String taskName;
    private String triggerType;
    private String paramsJson;
    private Map<String, Object> paramsMap;
    private OffsetDateTime runAt;
    private String traceId;
}
```

说明：

- `triggerType`：区分 `SCHEDULED` 与 `MANUAL`。
- `paramsJson`：原始参数，便于任务自行解析。
- `paramsMap`：基础 Map 结构，便于常规读取。
- `traceId`：用于日志串联。
- 后续若扩展任务执行记录或告警，也可直接在上下文中增加运行标识。

## 6. 数据模型设计

### 6.1 system_task

建议在 `trader-base` 中新增 `SystemTask` 实体及 DAO，对应表 `system_task`。

字段建议如下：

- `id`：主键。
- `app_name`：任务所属应用，例如 `trader-collector`。
- `task_code`：任务编码。
- `task_name`：任务名称。
- `cron`：当前生效的 Cron。
- `status`：任务状态，建议 `RUNNING` / `STOPPED`。
- `param_schema`：任务参数 Schema 快照。
- `params_json`：当前生效参数 JSON。
- `default_params_json`：默认参数 JSON。
- `version`：配置版本号，每次更新递增。
- `remark`：备注。
- `updated_at`：更新时间。
- `created_at`：创建时间。

约束建议：

- 唯一索引：`(app_name, task_code)`。
- 常用查询索引：`app_name`、`status`。

### 6.2 system_task_run_log

建议预留任务执行记录表，用于管理后台后续扩展执行历史。

字段建议：

- `id`
- `app_name`
- `task_code`
- `trigger_type`
- `params_json`
- `status`
- `started_at`
- `finished_at`
- `duration_ms`
- `error_msg`
- `trace_id`
- `created_at`

说明：

- 若当前阶段希望复用现有 `TaskLog`，可以在实现时做兼容映射。
- 如果短期不做完整执行历史页面，也建议先把表结构纳入设计，避免后续重复调整。

## 7. Quartz 调度设计

### 7.1 调度方式

Quartz 作为每个业务服务实例内的本地调度器使用：

- 每个应用只调度属于自己的任务。
- Job 数据以数据库任务表为准，不依赖 Quartz 自身持久化完成管理端控制。
- Quartz 负责本地定时触发。

### 7.2 统一代理 Job

在 `trader-base` 中定义通用 Job，例如 `TraderQuartzJob`：

1. 从 `JobDataMap` 中读取 `appName` 与 `taskCode`。
2. 获取 Spring 容器中的任务注册中心。
3. 根据 `taskCode` 找到实际 `TraderTask` Bean。
4. 从数据库读取该任务最新配置。
5. 构造 `TraderTaskContext`。
6. 执行任务逻辑。
7. 记录执行日志。

### 7.3 调度键设计

- JobKey：`appName:taskCode`
- TriggerKey：`appName:taskCode`

这样便于刷新时直接定位并重建目标任务。

## 8. 自动发现与注册设计

### 8.1 自动发现

启动时通过 Spring 容器收集所有 `TraderTask` Bean，建立任务注册表：

- Key：`taskCode`
- Value：`TraderTask`

要求：

- 同一应用中 `taskCode` 必须唯一。
- 若扫描到重复 `taskCode`，启动失败。

### 8.2 启动注册

`trader-base` 启动后执行任务元数据同步：

1. 读取当前应用名，例如通过配置 `spring.application.name`。
2. 遍历所有 `TraderTask`。
3. 若数据库中不存在该任务，则插入默认配置。
4. 若已存在，则保留数据库中的 `cron`、`status`、`params_json`，但可更新名称、Schema 等元信息。

这样可以保证：

- 新增任务能自动注册到后台。
- 已有任务不会因为代码升级覆盖运维配置。

## 9. 配置刷新机制

### 9.1 Redis Pub/Sub 即时通知

管理端修改任务配置后，除了更新数据库，还需发布 Redis 消息。

Topic 建议：

```text
trader:task:refresh
```

消息体建议：

```json
{
  "appName": "trader-collector",
  "taskCode": "fundMarketSync",
  "version": 12,
  "eventType": "TASK_UPDATED"
}
```

处理逻辑：

1. 业务服务监听该 Topic。
2. 收到消息后校验 `appName` 是否匹配当前应用。
3. 若匹配，则读取数据库中的最新任务配置。
4. 对比本地缓存版本。
5. 若数据库版本更新，则刷新本地 Quartz 调度。

### 9.2 数据库轮询兜底

为防止以下情况：

- Redis 消息丢失；
- Redis 短时不可用；
- 服务刚启动时错过历史消息；
- 网络抖动导致通知未及时消费；

在每个业务服务内增加一个轻量轮询任务，例如每 30 秒轮询一次：

1. 拉取当前应用的任务配置列表。
2. 将数据库中的 `version` 与本地缓存版本比对。
3. 发现差异后刷新对应任务。

该轮询任务本身可使用 Quartz 或 Spring 内部调度，但不暴露给业务任务管理界面。

### 9.3 双机制协同策略

- 以 Redis Pub/Sub 提供秒级刷新体验。
- 以数据库轮询保证最终一致性。
- 所有刷新动作以数据库最新状态为准。
- Redis 消息只作为“有变更，请拉取”的触发信号，不直接作为最终配置来源。

## 10. 参数模型设计

### 10.1 选择 JSON + Schema

任务参数采用：

- `params_json` 保存具体参数值。
- `param_schema` 描述参数结构、字段类型、必填项和约束。

原因：

- 各任务参数差异大，JSON 更灵活。
- Schema 可驱动后台表单生成与基础校验。
- 后续新增任务时不需要额外增加专用参数表。

### 10.2 Schema 示例

```json
{
  "type": "object",
  "properties": {
    "market": {
      "type": "string",
      "title": "市场"
    },
    "fullSync": {
      "type": "boolean",
      "title": "是否全量同步"
    },
    "days": {
      "type": "integer",
      "title": "回溯天数",
      "minimum": 1
    }
  },
  "required": ["market"]
}
```

### 10.3 参数值示例

```json
{
  "market": "CN",
  "fullSync": false,
  "days": 7
}
```

### 10.4 参数校验职责

- 前端根据 Schema 做基础表单校验。
- 后端保存时仍需做 Schema 校验，避免非法请求绕过前端。
- 任务执行前可再次解析 `params_json`，若缺失关键参数则标记执行失败并记录日志。

## 11. 后台管理设计

### 11.1 管理能力

管理后台应支持：

- 查询任务列表。
- 按应用过滤。
- 查看任务状态、Cron、参数、更新时间。
- 修改任务启停状态。
- 修改任务 Cron。
- 修改任务参数。
- 手动触发任务执行。

### 11.2 接口设计建议

遵循现有 REST 风格和 DTO/Param/Query 规范。

建议接口：

- `POST /task/list`
  - 查询任务列表。
- `GET /task/detail/{id}`
  - 获取任务详情。
- `POST /task/update`
  - 更新任务配置。
- `POST /task/trigger/{id}`
  - 手动执行任务。

对象建议：

- `SystemTaskDto`
- `SystemTaskListQuery`
- `SystemTaskUpdateParam`
- `SystemTaskTriggerParam`

### 11.3 更新流程

后台更新任务配置时：

1. Controller 接收请求。
2. Service 完成参数校验。
3. DAO 更新 `system_task`。
4. `version + 1`。
5. 通过 Redis 发布刷新消息。
6. 返回成功结果。

### 11.4 手动执行

后台手动执行建议分两种模式：

- 使用当前已保存参数直接执行。
- 临时覆盖参数执行一次，但不持久化。

第一阶段建议先支持“使用当前参数立即执行”，以降低复杂度。

## 12. 业务项目接入方式

### 12.1 任务示例

以 `trader-collector` 的基金行情同步任务为例：

```java
@Component
public class FundMarketSyncTask implements TraderTask {

    @Autowired
    private FundMarketService fundMarketService;

    @Override
    public String getTaskCode() {
        return "fundMarketSync";
    }

    @Override
    public String getTaskName() {
        return "同步基金行情数据";
    }

    @Override
    public String getDefaultCron() {
        return "0 0 1 * * ?";
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public String getParamSchema() {
        return "{\"type\":\"object\",\"properties\":{\"days\":{\"type\":\"integer\",\"title\":\"回溯天数\"}}}";
    }

    @Override
    public String getDefaultParams() {
        return "{\"days\":1}";
    }

    @Override
    public void execute(TraderTaskContext context) {
        fundMarketService.syncFundMarket();
    }
}
```

### 12.2 改造原则

- 删除原有 `@Scheduled`。
- 任务逻辑保留在原 Service 中。
- Task 类只做任务声明和调用入口。
- 参数从 `TraderTaskContext` 中获取，不直接从配置文件读取。

## 13. 错误处理与一致性

### 13.1 配置异常

- Cron 非法：后台保存时拒绝。
- 参数不符合 Schema：后台保存时拒绝。
- `taskCode` 不存在：发布或执行时返回业务异常。

### 13.2 执行异常

- 任务执行时抛出异常，统一捕获并记录日志。
- 不影响 Quartz 调度器本身运行。
- 后续可扩展失败告警。

### 13.3 刷新异常

- Redis 通知失败不影响数据库更新成功。
- 若 Redis 通知失败，由轮询兜底恢复。
- 本地刷新失败记录错误日志，等待下一次通知或轮询重试。

## 14. 与现有模块的关系

### 14.1 与 TaskLog 的关系

现有 `TaskLog` 更偏向任务执行日志展示，新的任务模块关注的是“任务定义 + 调度控制”。

两者关系建议：

- `system_task` 负责任务元数据和配置。
- `TaskLog` 或新增 `system_task_run_log` 负责执行记录。

第一阶段可以优先打通任务模块本身，执行日志可复用现有 `TaskLog` 体系，后续再统一抽象。

### 14.2 与 Redis 自动配置的关系

`trader-base` 已具备 Redis 自动配置基础，可在此之上扩展：

- 任务刷新消息发布器。
- 任务刷新消息监听器。

避免业务项目重复编写 Redis 订阅逻辑。

## 15. 分阶段实施建议

### 第一阶段：基础抽象

- 新增 `TraderTask` 与 `TraderTaskContext`。
- 新增 `SystemTask` 数据模型与 DAO。
- 接入 Quartz 自动配置。
- 完成任务自动发现与启动注册。

### 第二阶段：动态刷新

- 实现任务调度注册中心。
- 实现 Redis Pub/Sub 刷新。
- 实现数据库轮询兜底。

### 第三阶段：后台管理

- 新增 `trader-admin` 任务管理接口。
- 新增任务管理页面。
- 支持启停、Cron、参数修改。

### 第四阶段：业务迁移

- 先迁移 `trader-collector` 中现有任务作为样板。
- 再迁移 `trader-executor` 中需要调度的任务。

## 16. 推荐结论

本设计建议采用：

- Quartz 作为调度引擎；
- 接口 + 自动发现作为任务声明方式；
- Redis Pub/Sub 即时通知 + 定时轮询兜底；
- JSON + Schema 作为任务参数配置模型。

该方案具备以下优势：

- 与当前 Spring Boot 架构兼容；
- 对业务项目侵入低；
- 满足后台动态管理需求；
- 支持不同任务差异化参数配置；
- 即时刷新与最终一致性兼顾；
- 能以较低风险分阶段落地。

## 17. 风险与后续扩展

当前方案仍有以下扩展点：

- 多实例下同一任务是否只允许单节点执行；
- 手动执行的并发控制；
- 任务超时中断；
- 任务失败重试和告警；
- 参数 Schema 的前端组件化渲染。

这些内容不影响当前设计主链路，可在后续迭代中逐步补充。
