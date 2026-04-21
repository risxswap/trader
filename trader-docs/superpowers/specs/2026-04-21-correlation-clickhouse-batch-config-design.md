# Correlation ClickHouse Batch Config Design

## Goal

将相关性统计里 ClickHouse 写入相关的批次参数改为配置项，便于根据 ClickHouse 的实际吞吐和写入表现调整，而不需要改代码发版。

## Context

当前 `CorrelationService` 已经支持通过配置控制 NAV 查询分批大小：

- `trader.statistic.correlation.nav-query-code-batch-size`

但写入相关的两个批次参数仍写死在服务内部：

- `CORRELATION_SAVE_BATCH_SIZE`
- `CLEANUP_DELETE_BATCH_SIZE`

这两个参数分别影响：

- 相关性结果批量写入 ClickHouse 的 `saveBatch(...)`
- 历史重复相关性清理时的 `deleteByIds(...)`

## Design

### Configuration Model

继续沿用现有 `StatisticCorrelationProperties`，不新增新的配置类。

在 `trader.statistic.correlation` 下扩展两个配置项：

- `save-batch-size`
- `cleanup-delete-batch-size`

默认值保持现有行为不变：

- `save-batch-size = 200`
- `cleanup-delete-batch-size = 200`

### Service Wiring

`CorrelationService` 不再写死：

- `CORRELATION_SAVE_BATCH_SIZE`
- `CLEANUP_DELETE_BATCH_SIZE`

改为统一从 `StatisticCorrelationProperties` 读取安全值：

- `getSafeSaveBatchSize()`
- `getSafeCleanupDeleteBatchSize()`

其中：

- `flushCorrelations(...)` 使用 `save-batch-size`
- `cleanupHistoricalCorrelations()` 使用 `cleanup-delete-batch-size`

### Application Config

在 `trader-statistic/src/main/resources/application.yml` 中补充显式配置项，与已有 NAV 查询批次放在同一处：

```yml
trader:
  statistic:
    correlation:
      nav-query-code-batch-size: ${TRADER_CORRELATION_NAV_QUERY_CODE_BATCH_SIZE:1000}
      save-batch-size: ${TRADER_CORRELATION_SAVE_BATCH_SIZE:200}
      cleanup-delete-batch-size: ${TRADER_CORRELATION_CLEANUP_DELETE_BATCH_SIZE:200}
```

## Validation

增加两类最小保护测试：

- 配置类默认值测试
- 服务行为测试，验证：
  - `saveBatch(...)` 使用配置值
  - `deleteByIds(...)` 使用配置值

## Risks

如果批次配置被设置为 `0` 或负数，循环和批处理逻辑会异常。

因此配置类需要继续提供安全兜底：

- 非法值时自动回退到默认值

## Out of Scope

本次不处理：

- 相关性阈值配置化
- 任务入口参数化
- ClickHouse 事务管理器或日志策略调整
