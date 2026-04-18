# DAO 边界调整：trader-base 仅保留监控/上报/任务

## 目标

- `trader-base` 仅保留“数据监控与上报/任务”相关 DAO，作为跨服务共享的基础数据访问层
- 其他业务域 DAO（基金、投资、交易所、用户、升级、执行 SQL 等）迁回使用方项目（当前主要是 `trader-admin/admin-server`）
- 调整后确保编译通过、测试通过、运行时扫描到正确的 Mapper/Entity

## 非目标

- 不改表结构、不改业务逻辑语义
- 不对 DAO 做额外重构（例如命名、拆分、合并），仅做“归属与引用”调整

## 现状

- `trader-base` 当前包含大量 `cc.riskswap.trader.base.dao.*`（以及 entity/mapper/query/param/dto）
- `trader-admin/admin-server` 大量直接依赖 `cc.riskswap.trader.base.dao.*`
- `trader-collector` / `trader-executor` / `trader-statistic` 当前主要使用各自模块内的 DAO，不依赖 `trader-base` 的业务 DAO

## 目标边界（方案 A）

### trader-base 保留（监控/上报/任务）

保留在 `cc.riskswap.trader.base.dao`（以及对应的 `dao/entity`、`dao/mapper`、`dao/query`、`dao/param`、`dao/dto`）：

- 节点与监控：
  - `NodeDao`
  - `NodeGroupDao`
  - `NodeMonitorDao`
- 任务与上报：
  - `SystemTaskDao`
  - `SystemTaskRunLogDao`
  - `TaskLogDao`
  - `MsgPushLogDao`

约束：

- `trader-base` 中与任务调度/刷新相关组件（例如 `TraderTask*`）如果依赖 `SystemTaskDao`，应继续保持依赖关系不变

### trader-base 迁出（回归各项目）

从 `trader-base` 迁出到 `trader-admin/admin-server` 的 `cc.riskswap.trader.base.dao`（以及对应子包）：

- 基础字典与市场数据：
  - `BrokerDao`
  - `CalendarDao`
  - `ExchangeDao`
  - `FundDao`
  - `FundAdjDao`
  - `FundMarketDao`
  - `FundNavDao`
  - `CorrelationDao`
- 投资与交易：
  - `InvestmentDao`
  - `InvestmentLogDao`
  - `InvestmentPositionDao`
  - `InvestmentTradingDao`
- 用户与权限：
  - `UserDao`
- 数据库脚本/升级/执行：
  - `SqlExecDao`
  - `ClickHouseSqlExecDao`
  - `SystemUpgradeDao`
  - `SystemUpgradeStepDao`
- 其它：所有不在“保留清单”里的 `cc.riskswap.trader.base.dao.*`

说明：

- 迁出范围同时包含这些 DAO 所引用的 entity/mapper/query/param/dto；仅当这些类型被“保留清单”复用时，才留在 `trader-base` 并在 `trader-admin` 侧改为依赖 base 的类型

## 包与扫描策略

### admin-server 侧包名

- 新增/使用 `cc.riskswap.trader.base.dao.*` 承接迁入的 DAO/Entity/Mapper 等
- 与现有 `mybatis-plus.type-aliases-package: cc.riskswap.trader.base.dao.entity` 保持一致

### Mapper 扫描与数据源注解

- 迁移到 `trader-admin` 的 Mapper 继续使用 `@MysqlMapper` / `@ClickHouseMapper` 注解（注解类型仍来自 `trader-base`）
- `TraderDataSourceAutoConfiguration` 提供的 `AutoConfiguredMapperScannerConfigurer` 会扫描 Spring Boot 的 AutoConfigurationPackages
- `trader-admin` 的启动类配置 `@ComponentScan(basePackages = {"cc.riskswap.trader"})`，确保迁入的 DAO/Mapper 可被 Spring 管理

## 代码改动要点

- 现状清点：先列出 `trader-base` 当前 `cc.riskswap.trader.base.dao/**` 的类清单，逐个归类到“保留”或“迁出”，避免漏迁/误删
- 迁移文件：以“DAO + 依赖实体/Mapper/Query/Param/DTO”为单位搬迁，保持原类名不变，仅调整 package
- 更新引用：
  - `trader-admin` 中引用迁出的 DAO/Entity 等，改为 `cc.riskswap.trader.base.dao.*`
  - 引用保留清单的类型，继续使用 `cc.riskswap.trader.base.dao.*`
- 验证：
  - `trader-base` 单测通过
  - `trader-admin/admin-server` 编译通过且相关单测通过

## 关键风险与约束

### Mapper 扫描（必须明确处理）

`trader-base` 的 MyBatis Mapper 扫描依赖 `AutoConfigurationPackages` 自动推导 basePackage。`admin-server` 的启动包为 `cc.riskswap.trader.admin`，若保留在 `trader-base` 的 Mapper 位于 `cc.riskswap.trader.base.dao.mapper`，则在 `admin-server` 中可能无法被扫描到（运行时才暴露）。

约束（必须二选一落地）：

1. 在 `admin-server` 显式提供 `mysqlMapperScannerConfigurer` / `clickHouseMapperScannerConfigurer`（同名 Bean）覆盖自动配置，并设置扫描包同时包含 `cc.riskswap.trader.admin` 与 `cc.riskswap.trader.base` 的 Mapper 包；或
2. 在 `admin-server` 显式扩展 AutoConfigurationPackages，使其包含 `cc.riskswap.trader.base`（确保自动推导的 basePackage 覆盖 base）。

### type-aliases-package（建议补齐）

`admin-server` 的 `mybatis-plus.type-aliases-package` 建议同时包含：

- `cc.riskswap.trader.base.dao.entity`
- `cc.riskswap.trader.base.dao.entity`（保留清单仍会被 admin 使用）

### Param/DTO 归属（避免同名与混用）

- HTTP 入参/出参：归 `admin.common.model.*`
- DAO 查询/落库相关 Query/Param/DTO：归 `admin.dao.*`
- Controller 不直接使用 `admin.dao.param`，由 Service 层负责模型转换

### base 纯净性（必须）

保留在 `trader-base` 的 DAO/entity/query/param 不得反向依赖 `cc.riskswap.trader.admin.*`，避免循环依赖与 starter 不可复用。

## 验收标准

- `trader-base` 仅保留“监控/上报/任务” DAO（保留清单内），不再包含业务域 DAO（基金/投资/交易所/用户/升级/执行 SQL 等）
- `trader-admin/admin-server` 不再依赖 `trader-base` 的业务域 DAO（对应 import 均已迁移到 `cc.riskswap.trader.base.dao.*`）
- 构建与测试：
  - `trader-base`：`mvn test` 通过
  - `trader-admin`：`mvn -pl admin-server test` 通过（或至少 `compile` 通过，视现有测试基线）
- 运行时冒烟：
  - `admin-server` 可启动
  - `SystemTaskDao` / `TaskLogDao` / `NodeDao` 等保留清单 Bean 可正常注入使用（至少覆盖 1 条查询路径，例如 SystemTask 列表相关调用）
