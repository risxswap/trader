# Correlation Batch Size Config Design

## Goal

将相关性统计里 NAV 批量查询的分片大小从代码常量改为配置项，便于根据 ClickHouse 的实际限制调整，而不需要重新发版。

## Context

当前 `CorrelationService` 在批量计算相关性时，已经把 NAV 查询按固定大小分批执行，避免单条 `IN` SQL 过长触发 ClickHouse `max_query_size` 限制。

但这个批次大小目前仍写死在服务内部：

- 调整需要改代码
- 不同环境无法独立调参
- 以后如果相关性任务还要增加更多可调参数，继续写死会让服务类承担配置职责

## Design

### Configuration Model

在 `trader-statistic` 模块新增配置类：

- 类名：`StatisticCorrelationProperties`
- 前缀：`trader.statistic.correlation`

首个配置项：

- `nav-query-code-batch-size`
- 默认值：`200`

默认值保持与当前行为一致，避免本次变更引入行为漂移。

### Service Wiring

`CorrelationService` 不再持有写死的 `NAV_QUERY_CODE_BATCH_SIZE` 常量，改为注入 `StatisticCorrelationProperties` 并读取：

- `properties.getNavQueryCodeBatchSize()`

`loadNavSeriesByCode(...)` 仍保持当前“按批查询，内存合并”的流程，只把批次来源从常量改为配置。

### Application Config

在 `trader-statistic/src/main/resources/application.yml` 增加显式配置示例：

```yml
trader:
  statistic:
    correlation:
      nav-query-code-batch-size: 200
```

这样本地、测试、生产都可以直接覆写。

## Testing

保留现有分批行为测试，并补充最小配置绑定验证，确保：

- 默认值为 `200`
- 服务仍按配置值拆分查询批次

不做额外行为扩展，不调整现有相关性阈值、批量保存大小或任务入口逻辑。

## Risks

- 如果配置值被设为 `0` 或负数，循环会失效或行为异常

因此实现时应在配置类中提供安全默认值，并对非法值做兜底处理，至少保证最终使用值大于 `0`。

## Out of Scope

本次不处理：

- `saveBatch` 批大小配置化
- 相关性阈值配置化
- 计算并发度配置化
