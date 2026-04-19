# DAO 统一迁移到 trader-base 设计

## 背景与目标

当前项目的 DAO / Entity / Mapper 分散在多个模块（`trader-base`、`trader-collector`、`trader-executor`、`trader-statistic`），并存在大量同名类的重复实现，导致：

- 维护成本高：同一业务对象的 CRUD 逻辑散落多处，修复/扩展需要多点同步
- 风险高：不同模块对同一张表的字段/类型/命名不一致（例如 `fund.code` vs `symbol`，`id` 类型不一致）
- 运行配置复杂：各模块的 MyBatis 扫描包不同，合并后若不统一扫描配置，会出现 Mapper 不生效

本次改造目标：

1. 将各模块的 DAO 层统一迁移到 `trader-base` 模块中
2. 相同类名（DAO/Entity/Mapper）合并为一个，避免重复与歧义注入
3. MySQL 数据库中表的 `id` 字段改为 `INT` 类型，并同步将引用列（`*_id`）改为 `INT` 以保持一致
4. `fund` 相关字段命名统一使用 `code`（代码层与数据库列保持一致）

已确认口径：

- 仅改 MySQL（ClickHouse 不改 `correlation.id` 类型）
- MySQL 除主键 `id` 外，引用/外键类列（如 `broker_id`、`investment_id`）一起改为 `INT`
- `fund` 字段统一用 `code`

## 非目标

- 不引入新模块（例如不新建 `trader-dao`），统一承载仍在 `trader-base`
- 不改变 ClickHouse 表结构（只会处理 MySQL 的 `id` / `*_id` 类型）
- 不做与本次迁移无关的业务重构（例如服务层/接口层逻辑改写）

## 当前现状（摘要）

### DAO/Entity/Mapper 分布

- `trader-base`：`cc.riskswap.trader.base.dao.*`（以及 `cc.riskswap.trader.base.dao.base.*`）
- `trader-collector`：`cc.riskswap.trader.collector.repository.dao.*`、`...repository.entity.*`、`...repository.dao.mapper.*`
- `trader-executor`：`cc.riskswap.trader.executor.dao.*`、`...dao.entity.*`、`...dao.mapper.*`
- `trader-statistic`：`cc.riskswap.trader.statistic.dao.*`、`...dao.entity.*`、`...dao.mapper.*`

### 典型重复

- `FundDao/FundNavDao/FundMarketDao/CorrelationDao/...` 在多个模块同时存在
- `trader-base` 内部 `cc.riskswap.trader.base.dao.*` 与 `cc.riskswap.trader.base.dao.base.*` 也存在同名 DAO

### 数据库与实体类型不一致

- MySQL 建表脚本（`mysql.sql`）中多数表主键与引用列为 `BIGINT`
- 代码实体中已大量使用 `Integer id` / `Integer xxxId`

## 目标结构（统一承载位置）

### trader-base 作为唯一 DAO 承载

统一后的代码层结构（以包路径表达）：

- `cc.riskswap.trader.base.dao.entity`：MySQL/ClickHouse 的实体（根据 `@MysqlMapper/@ClickHouseMapper` 的 Mapper 绑定决定数据源）
- `cc.riskswap.trader.base.dao.mapper`：全部 MyBatis Mapper（以注解区分 MySQL/ClickHouse）
- `cc.riskswap.trader.base.dao`：DAO（MyBatis-Plus ServiceImpl 封装等）
- 其它查询/参数/DTO 仍放在 `cc.riskswap.trader.base.dao.query/param/dto`（如已有）

迁移完成后：

- `trader-collector/executor/statistic` 删除其自身的 `dao/entity/mapper` 源码
- 业务模块通过 Maven 依赖 `cc.riskswap.trader:base` 直接复用统一后的 DAO

## 合并规则与策略

### 合并原则

- 相同语义对象（同一张表/同一数据域）的 DAO/Entity/Mapper 只保留 1 份实现，位置在 `trader-base`
- 合并时以“功能并集”为准，保留所有业务模块曾经使用的方法
- 字段命名以数据库列为准（例如 `fund.code`），并统一映射与查询方法

### fund 命名统一

- 代码层实体字段统一为 `code`
- 所有 DAO 查询条件统一使用 `Fund::getCode`

### trader-base 内部重复包收敛

`cc.riskswap.trader.base.dao.base.*` 与 `cc.riskswap.trader.base.dao.*` 的同名 DAO/Mapper/Entity，统一收敛为单份，避免：

- Spring 容器存在两个同名/同类型 Bean 导致注入歧义
- 维护时误改“另一份实现”

## MyBatis 扫描与配置设计

### 目标

确保每个业务模块在运行时都能扫描到 `trader-base` 提供的 Mapper，并且 MySQL/ClickHouse 分别绑定到对应的 SqlSessionTemplate。

### 扫描策略

每个业务模块都显式提供两个 `MapperScannerConfigurer` Bean（名称与 `trader-base` 自动配置相同，用于覆盖 `@ConditionalOnMissingBean`）：

- `mysqlMapperScannerConfigurer`
  - basePackage：`cc.riskswap.trader.base.dao.mapper`
  - annotationClass：`@MysqlMapper`
  - sqlSessionTemplateBeanName：`mysqlSqlSessionTemplate`
  - nameGenerator：`FullyQualifiedAnnotationBeanNameGenerator`
- `clickHouseMapperScannerConfigurer`
  - basePackage：`cc.riskswap.trader.base.dao.mapper`
  - annotationClass：`@ClickHouseMapper`
  - sqlSessionTemplateBeanName：`clickHouseSqlSessionTemplate`
  - nameGenerator：`FullyQualifiedAnnotationBeanNameGenerator`

说明：

- `trader-base` 内置 `AutoConfiguredMapperScannerConfigurer` 会在未显式设置 `basePackage` 时，通过 `AutoConfigurationPackages` 自动推导扫描包；但此推导依赖启动类包路径覆盖范围，不同模块可能不一致
- 显式配置可避免“启动类包路径不覆盖 base 的 mapper 包”而导致 mapper 不生效
- 统一使用 `FullyQualifiedAnnotationBeanNameGenerator` 可降低多模块/多包扫描时 Bean 命名冲突风险

### type-aliases-package

业务模块的 `mybatis-plus.type-aliases-package` 统一指向：

- `cc.riskswap.trader.base.dao.entity`

并移除旧的：

- `cc.riskswap.trader.collector.repository.entity`
- `cc.riskswap.trader.executor.dao.entity`
- `cc.riskswap.trader.statistic.dao.entity`

## 数据库变更设计（MySQL）

### 变更范围

- 主键列：所有 `id` 主键由 `BIGINT` 改为 `INT`
- 引用列：所有与主键关联/引用的 `*_id` 列由 `BIGINT` 改为 `INT`，与被引用主键保持一致
- 非主键语义的 `BIGINT` 保持不变（例如耗时、版本号等）

### 变更文件

- 建表脚本：`trader-admin/admin-server/src/main/resources/db/mysql.sql`
- 升级脚本：新增 `trader-admin/admin-server/src/main/resources/db/upgrade/mysql/1.0.5.sql`（版本号可根据现有最新版本顺延）

### 兼容性与风险控制

- 若线上已有数据且 `id` 已超过 `INT` 上限，会导致 `ALTER TABLE` 失败
- 建议使用 `INT UNSIGNED` 以扩大上限（需要进一步确认是否采用 unsigned，本次默认按 `INT`）
- 迁移脚本执行前需确认：所有受影响列现有值均在目标类型范围内

## 迁移步骤（高层）

1. 在 `trader-base` 内合并并规范化 DAO/Entity/Mapper（优先从重复最多的 fund/investment/correlation 开始）
2. 将 `trader-collector/executor/statistic` 的 DAO/Entity/Mapper 全部迁移到 `trader-base`，并删除源模块对应代码
3. 更新各业务模块的 MyBatis 扫描配置与 type-aliases-package，使其扫描 `trader-base` 的 mapper/entity
4. 批量修正业务模块对旧包路径的 import 与注入点
5. 更新 MySQL 建表脚本与新增升级脚本，将 `id` 与引用列类型改为 `INT`
6. 全量编译与测试验证（至少 Maven compile + 关键单测）

## 验证策略

### 编译验证

- 根目录执行多模块编译（优先 `mvn -DskipTests compile`，再视情况跑测试）
- 分模块编译：`trader-base`、`trader-collector`、`trader-executor`、`trader-statistic`、`trader-admin/admin-server`

### 运行时验证（冒烟）

- 启动 `admin-server` / `collector` / `executor` / `statistic`，观察启动日志中 Mapper 扫描是否包含 base 包
- 执行典型 DAO 查询路径（例如 fund 列表、investment 相关查询）确认无 `Invalid bound statement` 类错误

## 回滚策略

- 代码回滚：保留迁移前分支或在迁移提交前打标签（本次不自动提交）
- 数据库回滚：若已执行 `ALTER TABLE`，需要准备逆向脚本（`INT` → `BIGINT`）以应对紧急回滚

