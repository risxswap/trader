# Trader 系统详细设计与实现文档

## 1. 总体架构
- 多模块单仓：admin（后端+前端）、collector（采集）、executor（执行器）、backtest（回测）、fund-template（模板）。
- 分层原则：展示（前端）/接口（Controller）/服务（Service）/数据访问（DAO/Mapper）/模型（Entity/DTO）/配置（Config）/基础设施（Redis、消息、调度）。
- 数据主线：外部数据 → 采集入库 → 策略回测与执行 → 交易/持仓/日志 → 管理端展示。
参考：[00-Architecture](./00-Architecture.md)

## 2. 模块设计
### 2.1 管理端后端（admin-server）
- 入口：Application.main，[Application.java](../../trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/Application.java)
- 配置加载：EnvironmentPostProcessor 注册于 [spring.factories](../../trader-admin/admin-server/src/main/resources/META-INF/spring.factories)，实现于 [ConfigLoader.java](../../trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/system/ConfigLoader.java)
- 数据源：PostgreSQL/TimescaleDB，HikariCP；MyBatis-Plus Mapper 按领域拆分。
- 控制器（节选）：
  - 基金/行情/净值：[FundController.java](../../trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/controller/FundController.java), [FundNavController.java](../../trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/controller/FundNavController.java)
  - 投资/持仓/交易/日志：[InvestmentController.java](../../trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/controller/InvestmentController.java), [InvestmentPositionController.java](../../trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/controller/InvestmentPositionController.java), [InvestmentTradingController.java](../../trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/controller/InvestmentTradingController.java), [InvestmentLogController.java](../../trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/controller/InvestmentLogController.java)
  - 相关性/交易所/日历/消息/认证：[CorrelationController.java](../../trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/controller/CorrelationController.java), [ExchangeController.java](../../trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/controller/ExchangeController.java), [CalendarController.java](../../trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/controller/CalendarController.java), [MsgPushLogController.java](../../trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/controller/MsgPushLogController.java), [AuthController.java](../../trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/controller/AuthController.java)
- 服务层：封装业务组合、事务边界与校验；调用 DAO 与第三方集成。
- 缓存：Redis（硬件/策略/节点/投资信息），示例 [HardwareInfoCache.java](../../trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/cache/HardwareInfoCache.java)
- 日志与监控：Logback 配置 [logback-spring.xml](../../trader-admin/admin-server/src/main/resources/logback-spring.xml)；Actuator 暴露健康与信息端点。
- API 网关：Nginx 反向代理与前端开发代理，见 [nginx.conf](../../trader-admin/nginx.conf) 与 [vite.config.ts](../../trader-admin/admin-web/vite.config.ts)

### 2.2 管理端前端（admin-web）
- 技术栈：Vue 3 + Vite + TypeScript。
- 入口与路由：[main.ts](../../trader-admin/admin-web/src/main.ts), [router/index.ts](../../trader-admin/admin-web/src/router/index.ts)
- 页面：仪表盘、基金/ETF、投资、持仓、交易、相关性、登录等，见 [pages/*](../../trader-admin/admin-web/src/pages/)
- 服务封装：axios 客户端与拦截器 [http.ts](../../trader-admin/admin-web/src/services/http.ts)；业务服务 [services/*](../../trader-admin/admin-web/src/services/)
- 构建与部署：Vite 构建与静态资源输出；打包脚本整合后端 Jar 与前端静态，见 [package.sh](../../trader-admin/package.sh)

### 2.3 数据采集（collector）
- 入口：Application.main，[Application.java](../../trader-collector/src/main/java/cc/riskswap/trader/collector/Application.java)
- 调度：Quartz/Scheduling，任务在 [task/*](../../trader-collector/src/main/java/cc/riskswap/trader/collector/task/) 中按领域拆分（基金、净值、行情、交易日历）。
- 服务与异常：统一异常拦截 [ExceptionAdvice.java](../../trader-collector/src/main/java/cc/riskswap/trader/collector/advice/ExceptionAdvice.java)，服务层 [service/*](../../trader-collector/src/main/java/cc/riskswap/trader/collector/service/)
- 外部 API：Tushare 接入与管理 [TushareManager.java](../../trader-collector/src/main/java/cc/riskswap/trader/collector/repository/tushare/TushareManager.java)，HTTP 封装 [HttpUtil.java](../../trader-collector/src/main/java/cc/riskswap/trader/collector/common/util/HttpUtil.java)

### 2.4 执行器（executor）
- 入口：Application.main。
- 策略：相对强度策略 [RelativeStrengthStrategy.java](../../trader-executor/src/main/java/cc/riskswap/trader/executor/strategy/RelativeStrengthStrategy.java)，基类与交易记录写入 [BaseStrategy.java](../../trader-executor/src/main/java/cc/riskswap/trader/executor/strategy/BaseStrategy.java)
- 服务：InvestmentService、StrategyService、HardwareInfoService 等，见 [service/*](../../trader-executor/src/main/java/cc/riskswap/trader/executor/service/)
- 并发与调度：线程池 [ThreadConfig.java](../../trader-executor/src/main/java/cc/riskswap/trader/executor/config/ThreadConfig.java)，Quartz 调度 [QuartzConfig.java](../../trader-executor/src/main/java/cc/riskswap/trader/executor/config/QuartzConfig.java)
- 数据访问：MyBatis-Plus DAO，示例 [FundMarketDao.java](../../trader-executor/src/main/java/cc/riskswap/trader/executor/dao/FundMarketDao.java)
- 缓存与通信：Redis 缓存 [cache/*](../../trader-executor/src/main/java/cc/riskswap/trader/executor/cache/)，发布订阅 [PubSubConfig.java](../../trader-executor/src/main/java/cc/riskswap/trader/executor/config/PubSubConfig.java)

### 2.5 回测（backtest）
- 技术栈：backtrader、pandas、SQLAlchemy 等，依赖见 [requirements.txt](../../trader-backtest/requirements.txt)
- 数据 Feed：历史 [fund_feeddata.py](../../trader-backtest/src/feeddata/fund_feeddata.py)，实时 [fund_live_feed.py](../../trader-backtest/src/feeddata/fund_live_feed.py)
- 策略：均线与相对强度等，见 [strategy/*](../../trader-backtest/src/strategy/)
- 运行器：主程序与批量回测，见 [main.py](../../trader-backtest/src/main.py)、[relative_runner.py](../../trader-backtest/src/runner/relative_runner.py)
- 分析与可视化：收益/回撤/夏普等分析器，见 [analysis/fund_analysis.py](../../trader-backtest/src/analysis/fund_analysis.py)

## 3. 关键设计与实现细节
### 3.1 配置加载与外部化
- 统一通过 application.yml 配置数据源、Redis、Actuator 等；额外属性由 ConfigLoader 注入（spring.factories 注册）。
- 安全项（如 webhook-url、API token）通过外部化文件与环境变量提供，不在代码中硬编码。

### 3.2 数据访问与模型对齐
- DAO 层以 MyBatis-Plus Mapper 组织，领域实体与 DTO 分离；分页与索引保障查询性能。
- 时间序列（fund_market）在 PostgreSQL/TimescaleDB 中存储，按 symbol+date 查询；各模块有对应 DAO。

### 3.3 策略执行与交易记录
- 执行器策略通过 BaseStrategy 统一执行：生成交易记录、写入 investment_trading/position/log；并与 Redis/Feign 交互。
- 风控在策略内部实现（如 ATR 止损、仓位与预算控制），可扩展外部风控服务。

### 3.4 并发与调度
- 执行器线程池统一于 ThreadConfig；Quartz 统一任务调度；采集任务使用 Scheduling/Quartz 定时。
- 保证任务幂等与失败重试，避免重复入库与级联异常。

### 3.5 前后端交互与代理
- 前端 axios 全量经 /api 代理，开发环境由 Vite 代理至 8080 后端；生产由 Nginx 反代至后端；路径重写去除 /api。
- 服务端 REST 风格统一：资源化路径、分页列表 POST、详情 GET、更新 PUT、删除 DELETE。

### 3.6 缓存与通信
- Redis 用于热点数据缓存（硬件/策略/节点/投资），以及发布/订阅通道。
- 关键缓存失效策略：写入交易/持仓后刷新相关缓存；系统初始化加载全量必要字典与策略信息。

### 3.7 异常处理与日志
- 采集服务统一异常拦截返回标准响应；后端统一使用 Logback 按模块与级别记录；必要时输出审计日志。
- Actuator 暴露健康与信息端点；可接入 Prometheus/Grafana 做指标采集。

## 4. 部署与运维
- 打包：Admin/Collector 提供 package.sh，整合 Jar/静态资源/Nginx 配置与 docker-compose。
- 配置：通过外部化属性与环境变量提供数据库、Redis、Webhook、远程 API base-url。
- 监控：Actuator 健康检查；错误告警与重试策略；硬件信息采集与展示（executor→admin）。

## 5. 安全与合规
- 鉴权：认证登录与密码修改接口；未来可接入 OAuth2/JWT。
- 配置保密：敏感配置不入库不进代码；按环境隔离。
- 审计：交易与持仓变更保留日志与记录；消息推送持久化。

## 6. 扩展点与改进建议
- 文档：引入 springdoc-openapi 自动生成接口文档；前端根据 OpenAPI 生成类型。
- 策略：引入策略版本化与灰度；统一风控服务与审批流。
- 数据：加强 TimescaleDB 压缩策略与索引优化；数据归档与冷热分层。
- 可观测性：统一追踪与链路日志；指标与告警标准化。

## 7. 验证与测试
- Python 回测测试覆盖指标与DAO；Java 模块分服务/DAO/集成测试；前端 E2E 可后续补充。
- 现有测试参考：
  - Backtest：[test/*](../../trader-backtest/test/)
  - Admin/Executor/Collector：各模块 test 路径详见仓库引用清单。

## 8. 依赖与版本
- 后端：Spring Boot 3，MyBatis-Plus，HikariCP，OpenFeign，Redis，Guava，Lombok，OSHI 等，见各模块 pom.xml。
- 前端：Vue 3 + Vite + TypeScript。
- 回测：backtrader、pandas、SQLAlchemy 等，见 requirements.txt。

## 9. 风险与缓解
- 外部 API 限流与稳定性：设置重试与退避；本地缓存与队列缓冲。
- 数据一致性：DAO 操作事务化；执行器写入后刷新缓存；幂等校验。
- 配置错误：统一 ConfigLoader 校验关键项；启动时断言并告警。

## 10. 附录：代码定位
- 详尽链接参见本文件与需求文档中的“代码参考”和“接口清单”章节。
