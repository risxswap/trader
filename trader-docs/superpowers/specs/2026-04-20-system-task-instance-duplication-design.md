# 任务实例重复创建规则调整设计

## 背景

当前任务管理在创建任务实例时，统一按 `taskType + taskCode` 作为唯一键进行校验：

- 任务定义从 Redis `trader:task:def:<taskType>:<taskCode>` 读取
- 任务实例在数据库 `system_task` 中按 `taskType + taskCode` 存在
- Redis 任务实例缓存 `trader:task:instances:<taskType>` 也以 `taskCode` 作为 hash field
- 刷新消息 `TraderTaskRefreshMessage` 和执行器 `TraderTaskExecutor` 也都依赖 `taskCode` 标识实例

当前 `SystemTaskService.createInstance()` 的行为是：

- 若数据库中已存在同 `taskType + taskCode` 的实例，则统一报错 `任务实例已存在`
- 不区分策略任务和非策略任务

用户希望调整为：

- 非策略任务一旦已经创建实例，则不能再次创建
- 策略任务允许基于同一任务定义重复创建多个实例

## 目标

本次改造目标如下：

1. 保持非策略任务仍然是单实例模型
2. 允许 `taskType=STRATEGY` 的任务基于同一任务定义重复创建多个实例
3. 确保重复创建的策略实例在数据库、Redis、刷新消息、执行器调度中都能独立存在
4. 不改任务定义协议，不扩大为前后端接口重构

## 非目标

- 不调整任务定义发布格式
- 不引入新的数据库表或专门的实例编号表
- 不修改任务实例创建接口入参结构
- 不改变非策略任务现有创建、更新、删除、触发行为
- 不在本次改造中新增专门的“定义码”展示字段

## 当前现状

### 现状一：实例唯一性是硬编码单实例

`SystemTaskService.createInstance()` 当前逻辑：

1. 根据 `taskType + taskCode` 读取任务定义
2. 根据 `taskType + taskCode` 查询数据库是否已有实例
3. 若已有实例则直接报错 `任务实例已存在`
4. 若不存在则写入 `system_task`
5. 将实例写入 Redis `trader:task:instances:<taskType>`
6. 发布刷新消息

### 现状二：实例标识在全链路只认 `taskCode`

系统当前并没有“定义码”和“实例码”的区分：

- 数据库 `system_task.taskCode` 是实例唯一键语义
- Redis hash field 也是 `taskCode`
- 刷新消息中携带的也是 `taskCode`
- 执行器 `TraderTaskExecutor` 从 Redis 按 `taskCode` 取实例
- `TraderTaskPoller`、`TraderTaskRefreshSubscriber` 也按 `taskCode` 处理实例变化

这意味着如果仅放开“策略任务允许重复创建”，但仍沿用同一个 `taskCode`：

- 数据库层会冲突或覆盖
- Redis 任务实例缓存会被后创建实例覆盖
- 执行与刷新链路无法区分多个策略实例

因此策略任务允许重复创建时，必须同时引入“实例码”概念。

## 总体设计

### 设计原则

- 非策略任务保持现有单实例语义
- 策略任务允许多实例，但多实例之间必须有独立实例标识
- 任务定义仍由原始 `taskCode` 标识
- 调度与执行链路仍复用现有基础设施，不新增第二套协议

### 规则封装方式

在 `SystemTaskService` 中新增一个小型规则函数，例如：

- `allowDuplicateInstance(String taskType)`

当前规则：

- `STRATEGY` -> `true`
- 其他类型 -> `false`

这样做的目的是：

- 避免把“策略任务允许重复创建”的业务规则散落在 if 判断里
- 后续如果还有其他任务类型要放开重复创建，只需集中调整这一个规则函数

## 实例标识设计

### 1. 非策略任务

非策略任务继续沿用现状：

- 任务定义码 = 实例码
- `taskCode` 不变
- `taskType + taskCode` 仍然只能存在一个实例

### 2. 策略任务

策略任务引入“定义码 + 实例码”模式：

- 创建请求中的 `taskCode` 仍表示“策略任务定义码”
- 真正落库和下发调度时，使用新生成的实例码写入 `SystemTask.taskCode`

推荐实例码格式：

- `<definitionTaskCode>#<timestamp>`

例如：

- `relativeStrength#1710000000000`

这样做的好处：

- 保持人类可读，能直接看出源自哪个任务定义
- 无需额外查表即可追溯实例来源
- 生成逻辑简单，不引入额外基础设施

### 3. 冲突处理

理论上时间戳冲突概率很低，但仍建议保留简单兜底：

- 若生成的实例码已存在，则重新生成一次
- 无需把该冲突暴露给用户

## 创建链路设计

### 入参层

`SystemTaskInstanceCreateParam` 保持不变：

- 不新增实例码字段
- 前端继续只提交：
  - `taskType`
  - `taskCode`
  - `taskName`
  - `cron`
  - `enabled`
  - `status`
  - `paramsJson`
  - `remark`

也就是说，前端只负责选择“基于哪个任务定义创建实例”，不负责管理实例编码。

### 服务层

`SystemTaskService.createInstance()` 调整为：

1. 按请求中的原始 `taskType + taskCode` 读取任务定义
2. 判断当前 `taskType` 是否允许重复创建
3. 若不允许：
   - 按当前逻辑执行 `systemTaskDao.getByAppNameAndTaskCode(taskType, taskCode)`
   - 若存在则报错 `任务实例已存在`
   - 最终实例码仍使用原始 `taskCode`
4. 若允许：
   - 跳过“同码实例已存在”拦截
   - 基于原始 `taskCode` 生成新的实例码 `generatedTaskCode`
   - 后续落库、缓存、刷新都使用 `generatedTaskCode`

### 实体落库

`SystemTask` 实体字段写入规则：

- `taskType`：保持请求值，例如 `STRATEGY`
- `taskName`：保持用户输入或定义名称
- `taskCode`：
  - 非策略任务：原始定义码
  - 策略任务：新生成的实例码
- `cron / enabled / status / paramsJson / remark / version`：保持现有规则
- `paramSchema / defaultParamsJson`：继续从任务定义继承

## Redis 与刷新链路设计

### Redis 任务实例缓存

当前写入方式为：

- key: `trader:task:instances:<taskType>`
- field: `taskCode`
- value: `SystemTaskStatusEvent` JSON

新规则下：

- 非策略任务：field 继续使用原始 `taskCode`
- 策略任务：field 改为新生成的实例码

这样 Redis 可以同时保存多个源自同一策略定义的实例，而不会彼此覆盖。

### 刷新消息

`TraderTaskRefreshMessage` 仍保持现有结构：

- `taskType`
- `taskCode`
- `version`
- `eventType`

但其中的 `taskCode` 语义调整为“真实实例码”：

- 非策略任务：仍是原始 `taskCode`
- 策略任务：是新生成的实例码

### 订阅与执行

由于现有链路本来就是按 `taskCode` 拉取实例，因此无需改协议：

- `TraderTaskRefreshSubscriber` 按实例码从 Redis 读取
- `TraderTaskPoller` 按实例码比较版本缓存
- `TraderTaskExecutor` 按实例码执行任务

因此只要创建阶段把策略实例码写对，后续调度与执行即可天然支持多实例。

## 展示影响

### 列表展示

任务管理列表中的 `taskCode` 会体现最终实例码：

- 非策略任务：表现不变
- 策略任务：会出现多个相同 `taskName`、不同 `taskCode` 的实例

这是当前设计的预期结果。

### 本次不扩展展示字段

虽然可以进一步新增例如：

- `definitionTaskCode`
- `instanceCode`

之类的展示字段，但这会扩大到 DTO、接口和前端展示的改造。

本次按 YAGNI 处理：

- 先保证功能正确
- 暂不新增专门的“定义码展示字段”

## 异常提示

### 非策略任务

重复创建时继续返回：

- `任务实例已存在`

### 策略任务

- 正常允许重复创建
- 不向用户暴露“实例码生成”细节
- 实例码冲突若发生，内部自动重试生成，不额外返回业务错误

## 实施边界

本次改造仅覆盖：

- 任务实例创建规则
- 策略任务实例码生成规则

不改动：

- 任务定义发布协议
- 任务实例创建接口参数结构
- 列表、详情、删除、更新、触发接口的入参结构
- 执行器调度主流程

预计主要改动位置：

- `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java`
- `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/SystemTaskServiceTest.java`

可能会新增小型私有方法：

- `allowDuplicateInstance(taskType)`
- `generateInstanceTaskCode(definitionTaskCode)`

## 验证策略

重点验证四组场景：

### 1. 非策略任务首次创建

- 创建成功
- `SystemTask` 正常落库
- Redis `trader:task:instances:<taskType>` 正常写入
- 刷新消息正常发布

### 2. 非策略任务重复创建

- 再次创建被拒绝
- 返回 `任务实例已存在`

### 3. 策略任务首次创建

- 创建成功
- `taskCode` 被改写为策略实例码
- Redis 和刷新消息都使用实例码

### 4. 策略任务重复创建

- 第二次创建成功
- 两次创建出来的实例码不同
- 数据库、Redis、刷新消息均以不同实例码独立存在
- 不会出现后创建实例覆盖先创建实例的情况

## 测试策略

- 主要在 `SystemTaskServiceTest` 增加或调整用例
- 不新增重型集成测试
- 优先覆盖以下断言：
  - 非策略任务仍然单实例
  - 策略任务允许重复创建
  - 策略任务重复创建后得到不同实例码
  - Redis 写入和刷新消息都使用实例码而非原定义码

## 验收标准

- 非策略任务仍然只能创建一个实例
- `taskType=STRATEGY` 的同一定义可以创建多个实例
- 多个策略实例不会在数据库、Redis、刷新链路上互相覆盖
- 删除、更新、触发基于实例码工作，不受本次改造破坏

