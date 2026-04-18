# Trader 系统需求文档

## 1. 背景与目标
- 面向量化交易与基金投资管理的多模块系统，覆盖数据采集、策略回测、交易执行与管理端展示。
- 目标：稳定采集与存储数据；支持策略研究与回测；安全可控地执行策略；提供管理与监控界面。
- 范围：当前仓库包含 admin（后端+前端）、collector（采集）、executor（执行器）、backtest（回测）、fund-template（微服务模板）。

参考架构概览：[00-Architecture](../architecture/00-Architecture.md)

## 2. 用户与角色
- 研究员：编写/调试策略，运行回测，查看指标与报表。
- 运维/工程师：部署各服务，监控健康与性能，管理配置。
- 交易管理员：在管理端查看市场/持仓/交易日志，校验与审计。

## 3. 功能性需求
### 3.1 数据采集（collector）
- 从外部源（Tushare 等）定时抓取基金基础信息、净值、行情、交易日历。
- 数据入库到 PostgreSQL/TimescaleDB，具备失败重试与异常拦截。
- 可通过配置启停任务、控制频率与限流。
代码参考：
- 任务与服务：[task/*](../../trader-collector/src/main/java/cc/riskswap/trader/collector/task/), [service/*](../../trader-collector/src/main/java/cc/riskswap/trader/collector/service/)
- 外部API封装：[TushareManager.java](../../trader-collector/src/main/java/cc/riskswap/trader/collector/repository/tushare/TushareManager.java), [HttpUtil.java](../../trader-collector/src/main/java/cc/riskswap/trader/collector/common/util/HttpUtil.java)

### 3.2 策略回测（backtest）
- 提供历史数据加载、指标计算（MA、ATR 等）、策略执行、结果分析（收益、回撤、夏普等）。
- 支持批量回测、实时模拟（live feed）与可视化绘图。
代码参考：
- 入口与运行器：[main.py](../../trader-backtest/src/main.py), [relative_runner.py](../../trader-backtest/src/runner/relative_runner.py)
- 策略示例：[simple_ma_strategy.py](../../trader-backtest/src/strategy/simple_ma_strategy.py), [relative_strength_strategy.py](../../trader-backtest/src/strategy/relative_strength_strategy.py)

### 3.3 策略执行（executor）
- 加载策略配置，按调度执行，记录投资、持仓与交易，发布/订阅状态。
- 提供对市场数据、策略信息、硬件信息的缓存；与管理端交互。
代码参考：
- 策略与基类：[RelativeStrengthStrategy.java](../../trader-executor/src/main/java/cc/riskswap/trader/executor/strategy/RelativeStrengthStrategy.java), [BaseStrategy.java](../../trader-executor/src/main/java/cc/riskswap/trader/executor/strategy/BaseStrategy.java)
- 服务/缓存/并发配置：[service/*](../../trader-executor/src/main/java/cc/riskswap/trader/executor/service/), [cache/*](../../trader-executor/src/main/java/cc/riskswap/trader/executor/cache/), [config/*](../../trader-executor/src/main/java/cc/riskswap/trader/executor/config/)

### 3.4 管理端（admin）
- 后端提供 REST 接口：基金、行情、投资、持仓、交易、日志、相关性、认证等。
- 前端提供页面：仪表盘、基金/ETF、投资/交易、相关性分析、设置与登录。
代码参考：
- 后端控制器示例：[FundController.java](../../trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/controller/FundController.java), [DashboardController.java](../../trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/controller/DashboardController.java)
- 前端页面示例：[pages/*](../../trader-admin/admin-web/src/pages/), [services/*](../../trader-admin/admin-web/src/services/)

## 4. 非功能性需求
- 可用性：各服务通过 Actuator 暴露健康检查；采集任务具备重试与告警。
- 性能：数据库连接池（HikariCP），查询分页与索引；执行器线程池与调度控制。
- 安全：认证与密码修改接口；敏感配置通过外部化配置；禁止泄露密钥。
- 可靠性：Redis 用于缓存与 Pub/Sub；异常统一处理与日志记录。
- 维护性：模块化分层（Controller/Service/DAO）；策略与数据源实现可插拔。
- 可观测性：关键操作与任务日志；指标可扩展至 Prometheus。

## 5. 接口与交互
### 5.1 前端代理与后端端点
- 前端开发代理 /api → 8080 后端根路径（去前缀），参考 [vite.config.ts](../../trader-admin/admin-web/vite.config.ts#L7-L16)
- 主要接口（示例）：基金/行情/净值/投资/持仓/交易/日志/相关性/认证等
接口定义参考：
- Admin 后端控制器总览：见上文“管理端（admin）”代码参考
- 前端调用封装：[http.ts](../../trader-admin/admin-web/src/services/http.ts)

### 5.2 执行器与管理端交互
- 执行器通过 Feign/HTTP 调用管理端接口，配置于 [application.yml](../../trader-executor/src/main/resources/application.yml)
- 通过 Redis 发布/订阅通道与缓存进行状态传递与共享。

## 6. 数据模型与存储
- PostgreSQL/TimescaleDB 作为主数据存储。
- 典型表：
  - 基金行情（fund_market）：时间序列数据，按代码+日期索引，DAO 参考各模块 FundMarketDao。
  - 投资与交易（investment_trading / investment_position / investment_log）：策略执行写入，DDL 参考 [db.sql](../../trader-admin/admin-server/src/main/resources/database/db.sql#L229-L275)
  - 基础字典/交易日历等。

## 7. 业务流程（文字时序）
- 数据采集：定时任务触发 → 外部 API 拉取 → 数据清洗 → DAO 入库。
- 回测：加载历史数据 → 配置策略与分析器 → 运行 → 输出指标与图表。
- 执行：按调度执行策略 → 生成交易与持仓记录 → Redis 通知/缓存 → 管理端展示。
- 管理端：前端请求 → 后端 Controller 聚合 → Service/DAO 查询 → 响应页面展示。

## 8. 约束与依赖
- 外部 API（Tushare/WeCom）需配置密钥与限流策略。
- 数据库与 Redis 连接需在 application.yml 与 config.properties 外部化。
- 前端与后端版本需匹配，代理与 Nginx 配置需一致。

## 9. 交付物
- 源码各模块与打包脚本（package.sh、docker-compose.yml）。
- 本需求文档与详细设计实现文档（trader-docs）。

## 10. 开放问题与后续规划
- 是否需要统一的 OpenAPI/Swagger 文档生成与前端类型联动。
- 执行器与管理端之间的鉴权与审计细化。
- 回测指标可扩展性与策略版本化管理。
