# 流式入库链路下线与模块直写设计

## 背景

当前系统里有一条由 `trader-admin` 统一代写数据库的链路：

- 业务模块通过 `TraderStreamPublisher` 把事件写入 Redis Stream
- `admin-server` 通过 `TraderStreamConsumer` 消费三类事件
- `admin-server` 再把事件内容写入 MySQL / ClickHouse

当前实际承载的主要事件类型包括：

- `TASK_LOG`
- `SYSTEM_TASK_STATUS`
- `NODE_MONITOR`

这条链路存在几个问题：

- 职责边界不清晰：执行节点负责产生事实数据，但最终落库依赖管理端中转
- 可用性受限：`trader-admin` 停机、重启或消费异常时，会影响日志、任务状态和监控历史入库
- 链路过长：一次本地事件要经过 Redis Stream、消费组和二次写库，排障复杂
- 查询与写入路径混用：例如节点实时状态已经直接读取 Redis `node:monitor`，但历史监控仍依赖 `admin-server` 消费后写库

用户已确认本次改造目标为：

- `trader-admin` 不再承担流式事件入库职责
- 设备信息监控、任务日志、任务状态都由各业务模块直接写库
- 采用一次性切换
- 不做长期双写

## 目标

本次改造目标如下：

1. 下线 `trader-admin` 的 Redis Stream 入库主链路
2. 将任务日志、任务状态、设备监控三类事件的落库能力统一下沉到 `trader-base`
3. 由 `collector`、`executor`、`statistic` 等业务模块直接调用基础库完成写库
4. 保持 `trader-admin` 作为查询与配置中心，而不是入库中转中心
5. 保留 Redis 中用于实时态和任务刷新分发的能力，不影响既有调度协议

## 非目标

- 不调整 `admin-web` 页面交互和接口协议
- 不重做任务调度模型，不改 `trader:task:instances:*` 与 `trader:task:refresh` 的职责
- 不引入长期双写或灰度双链路
- 不改变节点实时状态查询口径，仍优先读取 Redis `node:monitor`
- 不扩展到投资消息推送、业务通知等其他非本次范围的消息链路

## 当前现状

### 现状一：流式入库由 admin 承接

当前 `admin-server` 存在以下组件：

- `RedisStreamConfig`
- `TraderStreamConsumer`

其职责是消费 Redis Stream 中的系统事件并执行数据库写入：

- `TASK_LOG` -> 写入 `task_log`
- `SYSTEM_TASK_STATUS` -> 更新 `system_task`
- `NODE_MONITOR` -> 写入 `node_monitor`

### 现状二：查询链路已部分去中心化

节点管理查询已经不是纯粹依赖 admin 消费结果：

- 节点实时状态来自 Redis `node:monitor`
- 节点历史曲线来自 ClickHouse `node_monitor`
- 旧版状态兜底仍兼容 `node:status:*`

这意味着监控查询侧已经默认接受“实时态在 Redis、历史态在 ClickHouse”的模型，只是历史写入还保留了 admin 中转。

### 现状三：业务模块已具备直写所需基础依赖

`collector`、`executor`、`statistic` 当前都已依赖 `trader-base`，并且都具备：

- MySQL 连接
- ClickHouse 连接
- Redis 连接
- 节点标识配置 `trader.node.*`

因此从依赖条件看，三类写入都可以直接在业务模块本地完成，不需要借道 `trader-admin`。

## 总体设计

### 设计原则

- 配置与管理归 `trader-admin`
- 事实生成与落库归业务模块
- 落库能力统一沉淀到 `trader-base`
- Redis 保留“实时态缓存”和“任务刷新分发”职责，不再承担 admin 代写数据库的主路径

### 目标职责划分

`trader-base` 负责：

- 统一封装任务日志写入
- 统一封装任务状态回写
- 统一封装节点监控历史写入与实时态同步

业务模块负责：

- 在本地任务执行、监控采集过程中直接调用 `trader-base` 存储组件
- 对落库失败进行日志记录，但不依赖 admin 补写

`trader-admin` 负责：

- 查询 MySQL / ClickHouse / Redis 中已有数据
- 维护任务配置并继续向 Redis 写入任务实例快照
- 保留节点管理、历史查询、任务管理等管理端职责

`trader-admin` 不再负责：

- 消费 `TASK_LOG`
- 消费 `SYSTEM_TASK_STATUS`
- 消费 `NODE_MONITOR`
- 代业务模块完成上述三类数据库写入

## 组件设计

### 1. `TaskLogStore`

在 `trader-base` 新增统一任务日志存储组件，例如：

- `TaskLogStore`

职责：

- 创建任务开始日志
- 根据 `traceId` 更新成功/失败结果
- 统一封装 `task_log` 写入逻辑

调用方：

- `TraderTaskLogAspect`

替换方式：

- 当前 `TraderTaskLogAspect` 通过 `TraderStreamPublisher` 发送 `TASK_LOG`
- 改造后直接调用 `TaskLogStore`

### 2. `SystemTaskStatusStore`

在 `trader-base` 新增统一任务状态回写组件，例如：

- `SystemTaskStatusStore`

职责：

- 根据 `taskType + taskCode` 查询 `system_task`
- 更新 `status`
- 更新 `result`
- 更新必要的版本或更新时间字段

调用方：

- `TraderTaskExecutor`

替换方式：

- 当前 `TraderTaskExecutor` 在任务执行前后发送 `SYSTEM_TASK_STATUS`
- 改造后改为直接调用 `SystemTaskStatusStore`

### 3. `NodeMonitorStore`

在 `trader-base` 新增统一节点监控存储组件，例如：

- `NodeMonitorStore`

职责：

- 将监控快照写入 ClickHouse `node_monitor`
- 将最新快照写入 Redis Hash `node:monitor`
- 统一快照字段映射与序列化格式

调用方：

- 当前监控发布器/监控调度链路

替换方式：

- 当前监控链路需要去掉向 `NODE_MONITOR` 发流的逻辑
- 改为直接调用 `NodeMonitorStore`

## 三条链路的数据流

### 任务日志链路

改造前：

1. 任务进入切面
2. `TraderTaskLogAspect` 发送 `TASK_LOG`
3. `admin-server` 消费事件
4. `TaskLogDao` 写入 `task_log`

改造后：

1. 任务进入切面
2. `TraderTaskLogAspect` 直接调用 `TaskLogStore`
3. `TaskLogStore` 直接通过 `TaskLogDao` 写入 `task_log`

结果：

- 日志写入与任务执行处于同一模块上下文
- 不再依赖 `trader-admin` 是否在线

### 任务状态链路

改造前：

1. `TraderTaskExecutor` 在开始和结束时发送 `SYSTEM_TASK_STATUS`
2. `admin-server` 消费事件
3. `SystemTaskDao` 更新 `system_task`

改造后：

1. `TraderTaskExecutor` 在开始和结束时直接调用 `SystemTaskStatusStore`
2. `SystemTaskStatusStore` 直接更新 `system_task`

状态语义保持现有设计：

- 开始执行：`status = RUNNING`
- 成功结束：`status = STOPPED`，`result = SUCCESS`
- 失败结束：`status = STOPPED`，`result = FAILED`

### 设备监控链路

改造前：

1. 监控采集器生成快照
2. 发送 `NODE_MONITOR`
3. `admin-server` 消费事件
4. `node_monitor` 写入 ClickHouse

改造后：

1. 监控采集器生成快照
2. 直接调用 `NodeMonitorStore`
3. `NodeMonitorStore` 写入 ClickHouse `node_monitor`
4. `NodeMonitorStore` 同步写 Redis `node:monitor`

结果：

- 历史监控与实时态写入都由业务模块本地完成
- `NodeService` 可继续使用现有查询口径

## admin 侧保留与删除范围

### 保留

- `NodeService`
- `SystemTaskService`
- 节点列表与节点历史查询逻辑
- 任务配置编辑与任务刷新发布逻辑
- Redis 中 `trader:task:instances:*`
- Redis Pub/Sub `trader:task:refresh`

### 删除

- `RedisStreamConfig`
- `TraderStreamConsumer`
- `TraderStreamPublisher` 在三类系统事件中的主用途
- 与流式入库相关的管理端测试

说明：

- 如果 `TraderStreamPublisher` 仅剩其他无关用途，需要按实际剩余引用决定是否完全删除
- 对于本次范围内的三类系统事件，不再保留它作为主链路依赖

## 自动装配调整

### 当前问题

`trader-base` 中 `TraderTaskAutoConfiguration` 和 `TraderLoggingAutoConfiguration` 仍直接依赖 `TraderStreamPublisher`。

### 调整方向

- `TraderLoggingAutoConfiguration`
  - 不再装配依赖 `TraderStreamPublisher` 的切面
  - 改为装配依赖 `TaskLogStore` 的 `TraderTaskLogAspect`

- `TraderTaskAutoConfiguration`
  - 不再默认提供 `TraderStreamPublisher`
  - `TraderTaskExecutor` 改为依赖 `SystemTaskStatusStore`
  - 监控相关自动装配改为依赖 `NodeMonitorStore`

自动装配目标是：

- 业务模块只要具备对应数据源，就自动获得直写能力
- 缺失某类数据源时，该类写入能力自动降级为“仅记录错误日志/不装配相关 Bean”

## 异常处理策略

### 任务日志

- 保持“日志写入失败不影响主任务执行”的原则
- `TraderTaskLogAspect` 中对存储失败继续 `catch` 并记录错误日志
- 不因为 `task_log` 写入失败改变任务真实执行结果

### 任务状态

- 开始时回写 `RUNNING` 失败，只记录日志，不阻断任务执行
- 结束时回写 `STOPPED/SUCCESS` 或 `STOPPED/FAILED` 失败，只记录日志
- 任务最终抛错或成功返回，仍以真实业务执行结果为准

### 节点监控

- 写 ClickHouse 失败，只记录日志
- 写 Redis `node:monitor` 失败，只记录日志
- 任何一端失败都不影响业务模块继续存活和后续任务执行

## 一次性切换策略

本次采用一次性切换，不做长期双写。

切换步骤建议为：

1. 在 `trader-base` 补齐三个 Store 和对应自动装配
2. 将任务日志、任务状态、监控链路的发送方改为本地直写
3. 删除 `admin-server` 的 Stream 消费配置与消费实现
4. 更新测试，验证直写链路与查询链路
5. 统一部署新版本

### 为什么不做长期双写

- `task_log` 和 `node_monitor` 容易产生重复数据
- `system_task` 可能出现状态竞争和最后写入覆盖问题
- 排障时很难判断数据来自直写还是 admin 消费补写
- 双链路长期存在会模糊职责边界，与本次改造目标冲突

## 兼容策略

### 保留的兼容

- 节点实时状态查询仍允许 `NodeService` 保留对 `node:status:*` 的 fallback
- 任务刷新与配置分发协议保持不变

### 不保留的兼容

- 不保留 `TASK_LOG` 的 Stream 入库兜底
- 不保留 `SYSTEM_TASK_STATUS` 的 Stream 入库兜底
- 不保留 `NODE_MONITOR` 的 Stream 入库兜底
- 不做直写与 Stream 的长期双链路运行

## 实施边界

本次改造只覆盖“入库链路替换”，不改变：

- 管理端页面
- 节点查询接口
- 任务调度触发协议
- 任务刷新 Redis 通知协议

预计主要改动位置如下：

### `trader-base`

- 新增 `TaskLogStore`
- 新增 `SystemTaskStatusStore`
- 新增 `NodeMonitorStore`
- 调整 `TraderTaskLogAspect`
- 调整 `TraderTaskExecutor`
- 调整监控自动装配和监控发布链路
- 清理 `TraderStreamPublisher` 及其关联装配

### `trader-admin`

- 删除 `RedisStreamConfig`
- 删除 `TraderStreamConsumer`
- 删除对应测试
- 保留查询与配置职责

## 验证策略

### 1. 任务日志验证

- 任务启动后生成 `RUNNING` 日志
- 任务成功结束后更新为 `SUCCESS`
- 任务失败结束后更新为 `FAILED`
- 日志写入异常不影响任务真实执行结果

### 2. 任务状态验证

- 节点执行任务时能正确回写 `system_task.status/result/version`
- admin 修改任务配置后，节点仍可通过 Redis 刷新链路拿到最新实例
- 状态回写失败不改变任务真实成功/失败语义

### 3. 设备监控验证

- 周期采集后 ClickHouse `node_monitor` 存在历史记录
- Redis `node:monitor` 存在最新快照
- 节点列表继续能读取实时状态
- 节点历史曲线继续能查询监控历史

### 4. 结构性验证

- `admin-server` 启动时不再依赖 Stream 消费器 Bean
- 代码中不再存在三类系统事件经 admin 代写数据库的主路径

## 测试策略

- `trader-base` 增加 Store 单测和调用方单测
- `trader-admin` 删除失效的 Stream 消费测试
- 保留或更新查询侧测试，验证 Redis/ClickHouse 查询未受影响
- 优先做聚焦回归测试，不引入过重的跨模块集成测试

## 风险与注意事项

- 任务状态直写后，业务模块需要直接访问 `system_task`，要确保运行环境具备 MySQL 可写权限
- 监控直写后，业务模块需要直接访问 ClickHouse，若某环境未配置 ClickHouse，需要自动装配层提供明确降级行为
- 若后续仍有其他事件使用 `TraderStreamPublisher`，需要区分“本次三类系统事件下线”与“全量删除该工具类”的边界，避免误删无关能力

## 验收标准

- `trader-admin` 停机时，业务模块仍可继续写入任务日志、任务状态和设备监控
- `trader-admin` 恢复后，页面能直接查询到业务模块已写入的数据
- 代码中不再存在 `admin-server` 消费 `TASK_LOG`、`SYSTEM_TASK_STATUS`、`NODE_MONITOR` 后代写数据库的主路径
- 不存在长期双写逻辑

