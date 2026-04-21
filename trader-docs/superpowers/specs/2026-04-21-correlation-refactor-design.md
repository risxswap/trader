# Correlation Refactor Design

## 背景

当前 [CorrelationService](file:///Users/ming/Workspace/trader/trader-statistic/src/main/java/cc/riskswap/trader/statistic/service/CorrelationService.java) 已经支持批量相关性计算，但在代码结构上还存在一处明显重复:

- 单对接口 `calculateAndSave(...)` 自己完成时间序列构建、日期对齐、Pearson 相关系数和 p-value 计算
- 批量接口 `calculateAndSaveBatch(...)` 走的是另一套相近但不完全统一的内部流程

这会带来两个问题:

- 统计规则散落在多个代码块里，后续维护容易出现行为漂移
- service 文件已经承担批量读取、批量保存、单对保存、历史清理等职责，重复代码进一步降低可读性

用户这轮要求是做一轮代码整理，优先做“去重提炼”，尽量不扩大改动范围。

## 目标

- 在不改变外部行为的前提下，提炼 `CorrelationService` 内部的公共统计 helper
- 让单对接口和批量接口共享同一套核心统计规则
- 提升 `CorrelationService` 的可读性和后续维护性

## 非目标

- 不新增新的公开 service 或 calculator 组件
- 不改变 `CorrelationTask`、`FundNavDao` 的现有接口
- 不改变 Pearson 公式、p-value 公式或阈值条件
- 不改变 append-only 保存语义和历史清理流程

## 方案

采用轻量去重提炼方案，只在 [CorrelationService](file:///Users/ming/Workspace/trader/trader-statistic/src/main/java/cc/riskswap/trader/statistic/service/CorrelationService.java) 内调整内部结构。

### Helper 拆分

提炼 3 类内部 helper:

1. `toNavSeries(...)`
   - 输入 `List<FundNav>`
   - 输出 `Map<LocalDate, BigDecimal>`
   - 统一处理 `adjNav == null` 过滤和同日重复值保留策略

2. `calculateCorrelationStats(...)`
   - 输入两条时间序列
   - 负责共同日期对齐、Pearson 相关系数、p-value 和阈值判断
   - 返回私有结果对象，例如 `CorrelationStats`

3. `createCorrelation(...)`
   - 输入基金、周期和统计结果
   - 输出 `Correlation`
   - 统一填充 `id / createdAt / updatedAt / type / coefficient / pValue`

### 职责整理

- `calculateAndSave(...)`
  - 只负责加载两边 NAV、调用公共 helper、单条保存

- `calculateAndSaveBatch(...)`
  - 只负责批量加载 NAV、遍历基金对、调用公共 helper、批量缓存与 flush

- `cleanupHistoricalCorrelations()`
  - 保持现状，不纳入本次整理

## 测试策略

- 保留现有 `CorrelationServiceTest` 和 `CorrelationTaskTest` 的外部行为断言
- 如整理过程中需要补测试，只补充与“共享同一套统计规则”直接相关的聚焦测试
- 不为私有 helper 增加实现耦合过强的测试

## 风险与控制

- 风险: 去重提炼时不小心改变统计边界条件
- 控制: 复用现有测试命令做回归，确保单对和批量路径的外部行为保持一致

## 结论

本次整理只做轻量重构:

- 提炼公共统计 helper
- 消除单对/批量路径中的重复计算逻辑
- 保持接口、行为、数据语义和测试目标不变
