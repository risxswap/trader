# Correlation Batch Compute Design

## 背景

当前相关性全量计算链路存在两个明显性能瓶颈:

- 任务入口 [CorrelationTask](file:///Users/ming/Workspace/trader/trader-statistic/src/main/java/cc/riskswap/trader/statistic/task/CorrelationTask.java#L69-L112) 会为每一对资产提交一个异步任务
- 服务实现 [CorrelationService](file:///Users/ming/Workspace/trader/trader-statistic/src/main/java/cc/riskswap/trader/statistic/service/CorrelationService.java#L42-L107) 会对每一对资产分别查询两次 `FundNav`，再单条写入 `correlation`

当基金数量上升时，资产对数量是 `O(n^2)`，当前实现把数据库读取次数也放大到了同一量级，导致:

- `FundNav` 查询次数过多
- 线程池中堆积大量细粒度任务
- 相关性结果逐条保存，写入效率偏低

用户要求是:

- 将当前单对计算优化为通用批量计算接口
- 保持现有统计口径不变
- 保持现有“追加新版本，任务结束后统一清理历史版本”的写入语义

## 目标

- 将 `CorrelationService` 扩展为支持任意 `period` 的批量计算/批量保存入口
- 将全量任务从“逐对异步提交”改为“单次批量执行”
- 将 `FundNav` 读取改为按基金集合一次性批量拉取
- 将 `Correlation` 保存改为缓冲后分批 `saveBatch`
- 保持现有 Pearson 相关系数、p-value 与显著性过滤逻辑不变
- 保持任务结束后再执行历史清理

## 非目标

- 不改动相关性统计的数学公式和过滤阈值
- 不把相关性计算下推到 ClickHouse SQL 中完成
- 不改变 `correlation` 表的 append-only 语义
- 不在本次改造中引入新的结果表或缓存中间表
- 不优先解决极大规模数据下的跨机器分布式计算问题

## 方案选择

### 方案 A: 预加载 NAV + 内存两两计算 + 分批写入

核心思路:

- 一次性按 `period` 批量读取所有目标基金的 NAV
- 在内存中按基金代码组织时间序列
- 在 JVM 内完成两两组合计算
- 把满足条件的 `Correlation` 结果缓存在列表中并按批次保存

优点:

- 数据库读取次数显著下降
- 统计公式保持不变
- 与现有代码结构最兼容
- 可以直接复用 MyBatis-Plus 的 `saveBatch`

缺点:

- 需要新增批量查询 NAV 的 DAO 能力
- 需要控制内存占用

### 方案 B: 分片预加载 + 分段并发计算 + 分批写入

优点:

- 更容易限制单次内存峰值

缺点:

- 实现复杂度高
- 跨片组合调度复杂
- 数据库查询次数容易回升

### 方案 C: 数据库侧聚合计算

优点:

- 理论上能进一步减少 Java 侧计算

缺点:

- SQL 复杂度和维护成本高
- p-value 与显著性过滤在 SQL 中表达和验证成本高
- 不适合本次增量优化

## 结论

采用方案 A:

- 新增通用批量计算入口
- 预加载 NAV
- 内存中两两计算
- 结果分批保存
- 批量保存结束后执行历史清理

## 架构设计

### CorrelationTask

`CorrelationTask` 的职责调整为:

- 收集基金列表
- 做代码去重和顺序稳定化
- 调用 `correlationService.calculateAndSaveBatch(period, funds)`
- 在批量写入完成后触发历史清理

不再为每一对基金提交一个 `CompletableFuture`。因为批量读取后，主要收益来自减少数据库往返，继续保留海量小任务反而会引入线程调度开销。

### CorrelationService

保留现有单对接口，新增批量主入口，例如:

- `calculateAndSaveBatch(List<Fund> funds, String period)`

职责:

1. 计算 `startTime`
2. 批量加载 NAV
3. 构建 `code -> date/nav` 时序索引
4. 进行两两组合计算
5. 生成 `Correlation` 结果
6. 按批次 `saveBatch`
7. 结束后执行历史清理

### FundNavDao

新增批量查询能力，例如:

- `listByCodesAndStartTime(List<String> codes, OffsetDateTime startTime)`

用于替换当前 [listByCodeAndStartTime](file:///Users/ming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/FundNavDao.java#L37-L43) 的逐基金查询模式。

### CorrelationDao

继续复用 `ServiceImpl` 的 `saveBatch` 能力，不额外新增单独的批量插入 SQL。与项目内其他模块的批量保存方式保持一致。

## 数据流设计

### 1. 基金准备

从 `fundDao.listAll()` 获取基金列表后，构建:

- `List<Fund> uniqueFunds`，用于稳定生成资产对
- `Map<String, Fund> fundByCode`，用于快速读取 `type`

### 2. NAV 批量读取

根据 `uniqueFunds` 中的代码集合和 `startTime` 一次性查询所有 NAV 记录。

查询结果按 `code` 分组，并整理为基金级时间序列结构。每个基金只保留计算所需字段:

- `LocalDate`
- `adjNav`

### 3. 两两组合计算

使用双层循环遍历 `uniqueFunds`:

1. 获取两个基金的时间序列
2. 找出共同交易日
3. 构建两个 `double[]`
4. 复用当前 Pearson 相关系数和 p-value 计算逻辑
5. 执行现有过滤规则

保留现有规则:

- 共同数据点少于 `3` 条时跳过
- `correlationValue` 为 `NaN` 时跳过
- 仅保留 `abs(r) > 0.5 && pValue < 0.05`

### 4. 结果缓冲与批量保存

每得到一条满足条件的结果，构建一个 `Correlation` 对象并加入 `buffer`。

当 `buffer` 达到阈值时:

- 调用 `correlationDao.saveBatch(buffer, batchSize)` 或等价实现
- 清空 `buffer`

组合遍历结束后，再 flush 剩余结果。

### 5. 历史清理

在本轮批量保存成功结束后，再调用:

- `cleanupHistoricalCorrelations()`

维持 append-only + 事后清理的既有语义，避免先删旧记录导致短时空窗。

## 数据结构建议

### NAV 索引

建议在服务内部使用如下思路之一:

- `Map<String, Map<LocalDate, BigDecimal>>`
- 或定义轻量值对象，如 `FundNavSeries`

推荐优先采用简单结构，先保证实现清晰，再根据性能需要做局部优化。

### 批量结果缓冲

使用 `List<Correlation> buffer`，并通过常量控制批量大小，例如:

- `CORRELATION_SAVE_BATCH_SIZE = 200`

## 错误处理

### 计算阶段

如果单个资产对的时间序列存在异常数据:

- 记录日志
- 跳过该资产对
- 继续执行其他组合

避免一对坏数据中断整批任务。

### 保存阶段

如果某一批 `saveBatch` 失败:

- 记录失败批次大小和上下文
- 终止本次任务并抛出异常
- 不执行历史清理

这样即使失败，也只是保留旧数据和部分新数据；读路径依然只读最新版本，不会破坏正确性。

### 清理阶段

如果历史清理失败:

- 记录错误日志
- 保留新写入结果
- 容忍旧版本暂时继续存在

这与当前 append-only 设计兼容。

## 并发策略

本次改造默认采用单批量主流程，不继续保留“每个资产对一个线程任务”的模式。

原因:

- 当前主要瓶颈是数据库读取，不是 CPU
- 预加载数据后，再维持大量细粒度异步任务收益有限
- 海量 `CompletableFuture` 会增加调度开销和对象分配

如果未来基金数量持续增长，可以在“数据已在内存中”的前提下，对组合计算做分段并发，这应作为后续独立优化，而不是本次改造的前置条件。

## 验证策略

### 功能一致性

- 选择若干基金对，对比改造前后 `coefficient` 和 `pValue`
- 验证 `period`、`asset1Type`、`asset2Type` 填充逻辑不变

### 行为一致性

- 验证任务完成后仍会调用历史清理
- 验证读路径依然只返回最新版本

### 性能验证

记录以下指标并对比优化前后:

- 全量任务总耗时
- 参与计算基金数量
- 读取的 NAV 总记录数
- 生成的相关性记录数
- 批量保存次数

### 测试建议

优先补充服务层针对性测试，覆盖:

- 共同日期不足时跳过
- 显著性不过滤时跳过
- 满足阈值时进入批量保存
- 尾批 flush 正常执行
- 任务结束后触发历史清理

## 风险与缓解

### 风险 1: NAV 全量读取导致内存压力上升

缓解:

- 只保留计算所需字段
- 不保留多份中间集合
- 控制结果缓冲区大小

### 风险 2: 批量写入成功、清理失败

缓解:

- 接受旧版本暂时存在
- 依赖现有 latest-only 查询保证读正确性

### 风险 3: 去掉细粒度并发后 CPU 利用率下降

缓解:

- 先解决数据库 I/O 主瓶颈
- 后续如需要，再引入基于内存数据的分段并发

## 实施摘要

本次设计把相关性任务的性能优化聚焦在三个点:

- `FundNav` 从逐对查询改为批量查询
- `Correlation` 从逐条保存改为分批保存
- 任务入口从海量小任务改为单次批量流程

这样可以在不改变统计口径和写入语义的前提下，大幅减少数据库往返次数，并为后续继续优化计算并发打下基础。
