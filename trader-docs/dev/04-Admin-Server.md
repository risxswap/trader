# 04 - 管理端后端（admin-server）

`admin-server` 是管理后台的核心后端服务，提供 REST API，并承担数据库初始化与升级等系统能力。

## 1. 工程与入口

- Maven 坐标与依赖：见 [admin-server/pom.xml](../../trader-admin/admin-server/pom.xml#L22-L140)
  - 关键点：依赖 `trader-base`，因此可直接使用其 AutoConfiguration、DAO 等
- 启动入口：[Application](../../trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/Application.java)
- 默认端口：`8080`（见 [application.yml](../../trader-admin/admin-server/src/main/resources/application.yml#L1-L8)）

## 2. 分层与包结构

典型的 MVC 分层：

- `controller/`：协议适配、参数接收、统一返回（ResData）
- `service/`：业务编排、聚合查询、计算逻辑
- `dao/`：数据访问（部分已迁移到 `trader-base/base/dao`，用于跨服务复用）
- `system/`：系统初始化、数据库脚本执行、升级管理
- `task/`：Spring Scheduler 定时任务（与 `trader-base` 的任务模块并存）

## 3. 启动初始化链路

启动后通过 `ApplicationRunner` 执行系统初始化：

- 入口：[SystemInitializer](../../trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/system/SystemInitializer.java#L12-L31)
- 行为：
  - `databaseInitializer.initializeOnStartup()`：数据库初始化（建表/基础数据）
  - `userService.initAdmin()`：确保存在管理员账号
  - `upgradeService.runOnStartup()`：执行升级脚本（异常会被吞掉，避免阻塞启动）

## 4. DB 初始化与升级

### 4.1 初始化脚本

`application.yml` 指定了 DB 初始化脚本路径（MySQL + ClickHouse）：

- MySQL：`classpath:db/mysql.sql`
- ClickHouse：`classpath:db/clickhouse.sql`

见 [application.yml](../../trader-admin/admin-server/src/main/resources/application.yml#L39-L46)

MySQL 建表示例：见 [db/mysql.sql](../../trader-admin/admin-server/src/main/resources/db/mysql.sql#L1-L120)

### 4.2 升级机制（按版本号执行增量 SQL）

升级服务：[UpgradeService](../../trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/UpgradeService.java#L24-L179)

- 脚本来源：
  - MySQL：`classpath*:db/upgrade/mysql/*.sql`
  - ClickHouse：`classpath*:db/upgrade/clickhouse/*.sql`
- 脚本排序：按 `x.y.z.sql` 版本号升序执行
- 幂等：对每条 SQL 做 fingerprint（SHA-256 of normalized SQL），写入 `system_sql_script`（通过 `SystemUpgradeStepDao`）

## 5. Controller 示例（接口风格）

示例：[ExchangeController.list](../../trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/controller/ExchangeController.java#L14-L24)

- URL：`GET /exchange/list`
- 参数：`ExchangeListQuery`（Spring MVC 自动绑定 query string）
- 返回：`ResData<PageDto<ExchangeDto>>`

## 6. 配置要点（避免在文档中扩散敏感信息）

管理端服务使用 `trader.*` 前缀配置数据源/Redis/外部渠道：

- `trader.redis.*`：Redis 连接
- `trader.mysql.*`：MySQL 连接
- `trader.clickhouse.*`：ClickHouse 连接
- `trader.wecom.webhook-url`：消息推送 webhook（建议通过环境变量注入）

见 [application.yml](../../trader-admin/admin-server/src/main/resources/application.yml#L20-L56)
