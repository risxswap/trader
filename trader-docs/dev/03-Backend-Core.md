# 03 - 后端基础能力（trader-base）

`trader-base` 是整个仓库的“后端基础设施层”，其它 Spring Boot 服务通过 Maven 依赖它，即可获得数据源、Redis、任务调度、任务日志等能力。

## 1. 自动装配入口

- 总入口：[TraderBaseAutoConfiguration](../../trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderBaseAutoConfiguration.java#L6-L13)
  - 导入：数据源、Redis、日志三类基础能力

## 2. 配置加载（EnvironmentPostProcessor）

- 实现：[ConfigLoader](../../trader-base/src/main/java/cc/riskswap/trader/base/config/ConfigLoader.java#L19-L90)
- 行为要点：
  - 启动早期读取外部 `config.properties` 并 `addFirst` 到 Environment，保证覆盖 `application.yml`
  - 支持两类配置源：
    - common：`--config.path`（默认 `/opt/trader/config.properties`）
    - node：`--node.config.path`（用于“节点级”配置覆盖）

## 3. 多数据源（MySQL + ClickHouse）与 MyBatis-Plus

### 3.1 装配策略

核心实现：[TraderDataSourceAutoConfiguration](../../trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderDataSourceAutoConfiguration.java#L27-L145)

- 条件开关：
  - `trader.mysql.enabled=true` 且 `trader.mysql.url` 存在才创建 `mysqlDataSource`
  - `trader.clickhouse.enabled=true` 且 `trader.clickhouse.url` 存在才创建 `clickHouseDataSource`
- 每个数据源分别配：
  - `MybatisPlusInterceptor`（含分页拦截器 `PaginationInnerInterceptor`）
  - `SqlSessionFactory`、`SqlSessionTemplate`
  - 自定义注解扫描器：基于 `@MysqlMapper` / `@ClickHouseMapper` 区分 Mapper 归属

### 3.2 使用方式（业务工程侧）

业务工程只需要：

- 引入依赖：`cc.riskswap.trader:trader-base:1.0.0-SNAPSHOT`
- 在 `application.yml`（或外部配置）提供：
  - `trader.mysql.url/username/password`
  - `trader.clickhouse.url/username/password`

## 4. Redis（连接、Template、Pub/Sub）

核心实现：[TraderRedisAutoConfiguration](../../trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderRedisAutoConfiguration.java#L24-L81)

- `RedisConnectionFactory`：当 `trader.redis.host` 有值且无自定义 Bean 时创建 `LettuceConnectionFactory`
- `StringRedisTemplate` / `RedisTemplate<String,Object>`：统一序列化策略（key/string + value/jdk 序列化）
- Pub/Sub（用于任务刷新）：
  - 订阅 Topic：`trader:task:refresh`
  - 消费者方法：`TraderTaskRefreshSubscriber.handle`

## 5. 统一任务模块（Task）

### 5.1 抽象与注册

- 任务接口：[TraderTask](../../trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTask.java#L3-L18)
  - `getTaskCode()`：唯一编码（注册中心会检测重复）
  - `getDefaultCron()/defaultEnabled()`：用于首次落库时的默认配置
  - `getParamSchema()/getDefaultParams()`：用于任务参数的 Schema 与默认值（字符串形式的 JSON）
  - `execute(TraderTaskContext)`：任务执行入口（由调度代理调用）
- 注册中心：[TraderTaskRegistry](../../trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskRegistry.java#L8-L28)
- 自动装配：扫描 Spring 容器内全部 `TraderTask` Bean 并构建注册表，见 [TraderTaskAutoConfiguration.traderTaskRegistry](../../trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfiguration.java#L35-L39)

### 5.2 元数据同步（任务首次注册落库）

当服务启动时，会将本服务内实现的任务“定义信息”同步到 `system_task` 表：

- 服务：[TraderTaskMetadataSyncService.sync](../../trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskMetadataSyncService.java#L21-L46)
- 触发：`ApplicationRunner` 自动执行，见 [TraderTaskAutoConfiguration.traderTaskMetadataSyncRunner](../../trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfiguration.java#L95-L98)

### 5.3 调度刷新（Quartz）

- Quartz Scheduler Bean：见 [TraderTaskAutoConfiguration.traderTaskSchedulerFactoryBean](../../trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfiguration.java#L41-L53)
- 刷新逻辑：见 [TraderTaskSchedulerService.refresh](../../trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskSchedulerService.java#L24-L47)
  - `status != RUNNING`：删除 Job
  - `status == RUNNING`：删除旧 Job（若存在）并按 Cron 重建 Job + Trigger

### 5.4 刷新触发（Redis 与轮询）

任务配置更新后，可通过两条路径触发刷新：

- Redis Pub/Sub：见 [TraderTaskRefreshPublisher](../../trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskRefreshPublisher.java#L6-L18) 与 [TraderTaskRefreshSubscriber](../../trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskRefreshSubscriber.java#L8-L35)
- 轮询兜底：见 [TraderTaskPollingJob.poll](../../trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskPollingJob.java#L13-L16)
  - 通过 `system_task.version` 做本地缓存比对，见 [TraderTaskPoller](../../trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskPoller.java#L23-L32)

### 5.5 当前实现边界

- Quartz Job 代理：[TraderQuartzJob.execute](../../trader-base/src/main/java/cc/riskswap/trader/base/task/TraderQuartzJob.java#L6-L12) 当前为空实现
- 这意味着：当前代码能“把任务挂到 Quartz 上”，但还未在定时触发时真正调用到 `TraderTask.execute`
- 如果需要理解该模块的目标形态，可对照仓库内的设计/计划文档：
  - [task-module-design](../../../trader-base/docs/superpowers/specs/2026-04-15-task-module-design.md)
  - [task-module-implementation](../../../trader-base/docs/superpowers/plans/2026-04-15-task-module-implementation.md)

## 6. 任务日志（Task Log）

自动装配入口：[TraderLoggingAutoConfiguration](../../trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderLoggingAutoConfiguration.java#L18-L40)

- `TraderTaskLogStore`：基于 MySQL DataSource 的落库组件（条件：存在 `mysqlDataSource`）
- `TraderTaskLogAspect`：AOP 切面，用于在任务执行前后记录 traceId、耗时与异常等
- `TraderThreadContextTaskDecorator`：用于线程池场景的上下文传递
