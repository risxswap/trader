# 06 - 执行服务（trader-executor）

`trader-executor` 是策略执行节点服务，核心职责是根据投资配置执行策略、生成交易与日志，并通过消息机制广播给其它组件（如管理端）。

## 1. 入口与启动方式

- 启动类：[executor Application](../../trader-executor/src/main/java/cc/riskswap/trader/executor/Application.java#L7-L13)
- 默认端口：`8090`（见 [executor application.yml](../../trader-executor/src/main/resources/application.yml#L1-L3)）
- Docker 启动脚本（关键：生成并持久化节点 ID）：[bin/run.sh](../../trader-executor/bin/run.sh#L1-L25)
  - 读写 `/app/config/config.properties` 中的 `trader.node.id`
  - 将 `TRADER_NODE_ID` 作为环境变量导出，并以 `--node.config.path=...` 注入 Spring

## 2. 策略执行模型（Quartz Job）

策略抽象以 Quartz `Job` 的形式存在：

- 基类：[BaseStrategy](../../trader-executor/src/main/java/cc/riskswap/trader/executor/strategy/BaseStrategy.java#L35-L151)
  - `execute(JobExecutionContext)`：Quartz 调度入口
  - `initContext(investmentId)`：从数据库加载 Investment/最新日志/持仓/交易，装配到 `ExecutorContext`
  - `run(ExecutorContext)`：抽象方法，由具体策略实现
  - `trade(List<TradingParam>)`：通用交易执行流程（落库日志、持仓/交易写入、发布消息等）

策略实现示例：

- [RelativeStrengthStrategy](../../trader-executor/src/main/java/cc/riskswap/trader/executor/strategy/RelativeStrengthStrategy.java)

## 3. 分布式锁（RedisLock）

执行侧提供 Redis 分布式锁工具，适用于避免重复执行/并发冲突：

- [RedisLock](../../trader-executor/src/main/java/cc/riskswap/trader/executor/lock/RedisLock.java#L18-L82)
  - `tryLock(key, requestId, expireSeconds)`：`SET NX EX` 获取锁
  - `unlock(key, requestId)`：Lua 脚本校验 value 后删除
  - `generateRequestId()`：生成锁请求标识

## 4. 消息发布（Redis Pub/Sub）

执行侧通过 Redis Pub/Sub 发布事件，供订阅方消费：

- 频道枚举：[Channels](../../trader-executor/src/main/java/cc/riskswap/trader/executor/pubsub/Channels.java#L3-L11)
  - `investment_log`：投资日志事件
- 发布基类：[BasePublisher.publish](../../trader-executor/src/main/java/cc/riskswap/trader/executor/pubsub/publisher/BasePublisher.java#L32-L49)
  - 序列化：Hutool `JSONUtil.toJsonStr`
  - 发布：`StringRedisTemplate.convertAndSend(topic, messageJson)`
- 投资日志发布者：[InvestmentLogPublisher](../../trader-executor/src/main/java/cc/riskswap/trader/executor/pubsub/publisher/InvestmentLogPublisher.java#L13-L27)
  - `create(id)`：发布 `MessageTypeEnum.CREATE` 的日志消息

## 5. 配置要点

- 依赖 `trader-base` 的通用配置前缀：
  - `trader.redis.*` / `trader.mysql.*` / `trader.clickhouse.*`
  - `trader.monitor.*`：节点监控上报间隔等（见 [executor application.yml](../../trader-executor/src/main/resources/application.yml#L16-L43)）
  - `trader.node.id`：节点 ID（默认来自 `TRADER_NODE_ID` 或主机名）
- 管理端 API 地址：
  - `admin.api.base-url`（见 [executor application.yml](../../trader-executor/src/main/resources/application.yml#L44-L45)）
