# 01 - 模块职责

## trader-admin（聚合工程）

- 位置：[trader-admin/pom.xml](../../trader-admin/pom.xml#L1-L26)
- 职责：聚合 `admin-server` 与 `admin-web`，用于统一构建与发布
- Maven 模块：
  - `admin-server`：管理端后端
  - `admin-web`：管理端前端（同时包含一个 `pom.xml`，用于与 Maven 工程聚合）

## admin-server（管理端后端）

- 入口：[Application](../../trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/Application.java)
- 职责（面向“管理/运营/观测”）：
  - 提供管理后台 API（投资/标的/交易所/基金/统计/日志/任务/节点等）
  - 负责数据库初始化与升级（MySQL + ClickHouse）
  - 聚合查询与报表计算（例如相关性统计）
  - 对接外部通知渠道（如 WeCom webhook）
- 关键目录：
  - `controller/`：HTTP API 入口（Controller 层）
  - `service/`：业务编排与计算
  - `system/`：启动初始化、升级等系统级能力
  - `task/`：Spring Scheduler 的定时任务（与 `trader-base` 的任务体系并存）

## admin-web（管理端前端）

- 入口：[main.ts](../../trader-admin/admin-web/src/main.ts)
- 职责：
  - Vue3 单页应用（路由、页面、表格/表单交互、图表展示）
  - 通过 `services/*` 封装 HTTP API，对接 `admin-server`
- 关键目录：
  - `src/router/`：路由与菜单构建（见 [router/index.ts](../../trader-admin/admin-web/src/router/index.ts#L1-L448)）
  - `src/pages/`：按业务域划分页面（dashboard/investment/funds/etf/analysis/logs/node/task…）
  - `src/services/`：按模块封装 API（auth/basic/fund/correlation/dashboard/log/task…）

## trader-base（基础库 / 自动装配）

- 位置：[trader-base/pom.xml](../../trader-base/pom.xml#L1-L133)
- 职责：
  - 统一封装多数据源（MySQL + ClickHouse）、Redis、任务调度、任务日志、常用 DAO
  - 以 AutoConfiguration 的形式被其它 Spring Boot 服务直接依赖使用
- 自动装配入口：
  - 聚合装配：[TraderBaseAutoConfiguration](../../trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderBaseAutoConfiguration.java#L6-L13)
  - 数据源装配：[TraderDataSourceAutoConfiguration](../../trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderDataSourceAutoConfiguration.java#L27-L145)
  - Redis 装配：[TraderRedisAutoConfiguration](../../trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderRedisAutoConfiguration.java#L24-L81)
  - 任务调度装配：[TraderTaskAutoConfiguration](../../trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfiguration.java#L28-L105)

## trader-collector（数据采集/同步服务）

- 入口：[Application](../../trader-collector/src/main/java/cc/riskswap/trader/collector/Application.java)
- Maven 坐标（注意 artifactId 命名为 `fund`）：[pom.xml](../../trader-collector/pom.xml#L1-L151)
- 职责：
  - 对接外部数据源（如 TuShare），按任务模型同步交易日历、基金净值/行情/复权等
  - 以任务形式编排周期性同步（示例：[CalendarSyncTask](../../trader-collector/src/main/java/cc/riskswap/trader/collector/task/CalendarSyncTask.java#L8-L50)）
  - 将数据写入 MySQL（复用 `trader-base` 的 DAO 与自动装配）

## trader-executor（策略执行/节点服务）

- 入口：[Application](../../trader-executor/src/main/java/cc/riskswap/trader/executor/Application.java)
- 职责：
  - 承载策略执行（Quartz Job），读取投资/持仓/交易等数据，生成交易与日志并广播
  - 上报节点监控（`trader.monitor.*`）与节点标识（`trader.node.id`）
  - 通过 Redis Pub/Sub 或其它机制通知管理端/观察端
- 关键实现：
  - 策略基类（Quartz Job）：[BaseStrategy](../../trader-executor/src/main/java/cc/riskswap/trader/executor/strategy/BaseStrategy.java#L35-L151)
  - 容器启动脚本（生成节点 ID 并注入启动参数）：[bin/run.sh](../../trader-executor/bin/run.sh#L1-L25)

## trader-template（样板工程）

- 入口：[Application](../../trader-template/src/main/java/cc/riskswap/trader/fund/executor/Application.java)
- 职责：提供一个最小可运行的 Spring Boot 示例工程，用于快速复制新服务的基础结构与配置方式
