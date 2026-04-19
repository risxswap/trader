# 策略配置定义迁移到 trader-executor 设计

## 背景

当前策略配置定义位于 `trader-base`：

- `cc.riskswap.trader.base.strategy.config.BaseStrategyConfig`
- `cc.riskswap.trader.base.strategy.config.RelativeStrengthStrategyConfig`

但实际引用方仅存在于 `trader-executor`：

- `cc.riskswap.trader.executor.strategy.BaseStrategy`
- `cc.riskswap.trader.executor.strategy.RelativeStrengthStrategy`

这意味着策略配置定义并不是跨模块通用基础能力，而是执行器模块内部的策略实现细节。继续放在 `trader-base` 会造成职责边界不清晰。

## 目标

将策略配置定义从 `trader-base` 迁移到 `trader-executor`，并同步调整包名与引用路径，使配置定义与策略执行实现保持同模块归属。

## 采用方案

采用方案 A：

- 将配置类移动到 `trader-executor`
- 包名改为 `cc.riskswap.trader.executor.strategy.config`
- 同步修改 `BaseStrategy` 与 `RelativeStrengthStrategy` 的 import
- 删除 `trader-base` 中原有 `strategy/config` 目录，不保留兼容副本

## 为什么采用该方案

- 职责更清晰：配置定义与策略执行实现同属 `trader-executor`
- 改动最小：当前仅有 `trader-executor` 引用旧包，迁移范围非常集中
- 避免重复定义：不保留旧类，防止后续再次出现“双份配置类”

## 迁移范围

### 从 trader-base 删除

- `trader-base/src/main/java/cc/riskswap/trader/base/strategy/config/BaseStrategyConfig.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/strategy/config/RelativeStrengthStrategyConfig.java`

### 在 trader-executor 新增

- `trader-executor/src/main/java/cc/riskswap/trader/executor/strategy/config/BaseStrategyConfig.java`
- `trader-executor/src/main/java/cc/riskswap/trader/executor/strategy/config/RelativeStrengthStrategyConfig.java`

### 需要同步修改的引用点

- `trader-executor/src/main/java/cc/riskswap/trader/executor/strategy/BaseStrategy.java`
- `trader-executor/src/main/java/cc/riskswap/trader/executor/strategy/RelativeStrengthStrategy.java`

## 包与依赖边界

迁移后边界如下：

- `trader-base`：只保留通用基础设施、DAO、通用任务/日志/数据源能力
- `trader-executor`：保留策略实现、策略配置、执行器特有上下文

本次迁移不会引入新的模块依赖，也不会形成循环依赖：

- `trader-executor` 本来就依赖 `trader-base`
- 将配置类放回 `trader-executor` 后，只是减少 `trader-base` 对执行器概念的承载

## 风险与兼容性

### 风险

- 若存在遗漏的旧包引用，会在编译阶段直接失败

### 控制方式

- 全仓搜索旧包 `cc.riskswap.trader.base.strategy.config`
- 先编译 `trader-executor`
- 再执行根目录全量编译确认没有隐藏依赖

## 验证方式

### 编译验证

- `./mvnw -pl trader-executor -am -DskipTests compile`
- `./mvnw -DskipTests compile`

### 结果预期

- `trader-executor` 编译通过
- 根模块全量编译通过
- 全仓不再存在 `cc.riskswap.trader.base.strategy.config` 的 Java 引用

