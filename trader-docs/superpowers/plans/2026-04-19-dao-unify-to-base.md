# DAO Unify To trader-base Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `trader-collector/executor/statistic` 的 DAO/Entity/Mapper 统一迁移并合并到 `trader-base`，并把 MySQL 的 `id` 与引用列改为 `INT`，保证全工程编译通过与可运行。

**Architecture:** 以 `trader-base` 作为唯一 DAO 承载模块；业务模块仅通过 Maven 依赖复用 DAO。通过 `@MysqlMapper/@ClickHouseMapper` 区分数据源，并在各业务模块显式配置 Mapper 扫描包指向 `trader-base`。

**Tech Stack:** Java, Spring Boot, MyBatis-Plus, MyBatis MapperScannerConfigurer, Maven 多模块, MySQL/ClickHouse

---

## 影响文件清单（概览）

**主要改动：**

- `trader-base`
  - Modify: `src/main/java/cc/riskswap/trader/base/dao/**`（合并/修正 Fund 等命名与方法）
  - Add/Modify: `src/main/java/cc/riskswap/trader/base/dao/**`（从 collector/executor/statistic 迁入的 DAO/Entity/Mapper）
- `trader-collector`
  - Delete: `src/main/java/cc/riskswap/trader/collector/repository/dao/**`
  - Delete: `src/main/java/cc/riskswap/trader/collector/repository/entity/**`
  - Modify: `src/main/java/cc/riskswap/trader/collector/config/TraderMybatisScanConfig.java`（扫描 base 包）
  - Modify: `src/main/resources/application.yml`（type-aliases-package 指向 base）
- `trader-executor`
  - Delete: `src/main/java/cc/riskswap/trader/executor/dao/**`
  - Add: `src/main/java/cc/riskswap/trader/executor/config/TraderMybatisScanConfig.java`（若不存在）
  - Modify: `src/main/resources/application.yml`（type-aliases-package 指向 base）
- `trader-statistic`
  - Delete: `src/main/java/cc/riskswap/trader/statistic/dao/**`
  - Add: `src/main/java/cc/riskswap/trader/statistic/config/TraderMybatisScanConfig.java`（若不存在）
  - Modify: `src/main/resources/application.yml`（type-aliases-package 指向 base）
- `trader-admin/admin-server`
  - Modify: `src/main/resources/db/mysql.sql`（id 与引用列改 INT）
  - Add: `src/main/resources/db/upgrade/mysql/1.0.5.sql`（迁移脚本，版本号按现有最新顺延）

---

### Task 1: 统一 fund 字段命名为 code，并修正相关 DAO 查询条件

**Files:**
- Modify: `/Users/ming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/entity/Fund.java`
- Modify: `/Users/ming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/FundDao.java`
- Modify: 其它引用 `Fund::getSymbol`/`fund.symbol` 的代码（全仓搜索后逐个修正）

- [ ] **Step 1: 全仓检索 symbol 用法并记录命中点**
  - Run: `rg -n "\\bsymbol\\b|getSymbol\\(" -S trader-base trader-admin trader-collector trader-executor trader-statistic`
  - Expected: 输出包含 `Fund`、`FundDao` 以及可能的查询/DTO/服务层引用

- [ ] **Step 2: 修改 Fund 实体字段 symbol -> code**
  - 将字段名改为 `private String code;`
  - 同步 getter/setter（Lombok @Data 自动生成）

- [ ] **Step 3: 修正 FundDao 查询条件**
  - `getByCode` / `deleteByCode` / `pageQuery` 等使用 `Fund::getCode`

- [ ] **Step 4: 编译 trader-base**
  - Run: `mvn -pl trader-base -DskipTests compile`
  - Expected: `BUILD SUCCESS`

---

### Task 2: 收敛 trader-base 内部 dao.base 重复 DAO（同名类合并为单份）

**Files:**
- Modify/Delete: `/Users/ming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/base/**`
- Modify: `/Users/ming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/**`（保留的那一份）
- Modify: 相关引用 import（若存在）

- [ ] **Step 1: 列出重复类清单并对比差异**
  - Focus: `MsgPushLogDao/NodeDao/NodeGroupDao/NodeMonitorDao/SystemTaskDao/SystemTaskRunLogDao/TaskLogDao`
  - 逐个对比方法差异，确定保留版本（以功能并集）

- [ ] **Step 2: 合并方法并删除多余实现**
  - 目标：每个 DAO 仅保留在 `cc.riskswap.trader.base.dao`（不保留同名双份）

- [ ] **Step 3: 编译 trader-base**
  - Run: `mvn -pl trader-base -DskipTests compile`
  - Expected: `BUILD SUCCESS`

---

### Task 3: 迁移并合并 trader-collector 的 DAO/Entity/Mapper 到 trader-base

**Files:**
- Move/Delete: `/Users/ming/Workspace/trader/trader-collector/src/main/java/cc/riskswap/trader/collector/repository/dao/**`
- Move/Delete: `/Users/ming/Workspace/trader/trader-collector/src/main/java/cc/riskswap/trader/collector/repository/entity/**`
- Move/Delete: `/Users/ming/Workspace/trader/trader-collector/src/main/java/cc/riskswap/trader/collector/repository/dao/mapper/**`
- Modify/Add: `/Users/ming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/**`（合并后的落点）
- Modify: `/Users/ming/Workspace/trader/trader-collector/src/main/java/cc/riskswap/trader/collector/config/TraderMybatisScanConfig.java`
- Modify: `/Users/ming/Workspace/trader/trader-collector/src/main/resources/application.yml`

- [ ] **Step 1: 将 collector 的 entity 逐个映射到 base entity 并删除重复实体**
  - 规则：同表同语义只保留 base 的 entity；若字段不同则合并字段，并以数据库列命名为准

- [ ] **Step 2: 将 collector 的 mapper 合并到 base 的 mapper 包**
  - 保持 `@MysqlMapper/@ClickHouseMapper` 注解正确

- [ ] **Step 3: 将 collector 的 DAO 合并到 base 的 DAO 包**
  - 保留 collector 额外方法（例如 `listByMarket`）

- [ ] **Step 4: 修改 collector 的 MyBatis 扫描配置为扫描 base mapper 包**
  - basePackage 设置为 `cc.riskswap.trader.base.dao.mapper`
  - 配置 nameGenerator 为 `FullyQualifiedAnnotationBeanNameGenerator`

- [ ] **Step 5: 修改 collector 的 type-aliases-package 指向 base entity 包**

- [ ] **Step 6: 编译 trader-collector**
  - Run: `mvn -pl trader-collector -DskipTests compile`
  - Expected: `BUILD SUCCESS`

---

### Task 4: 迁移并合并 trader-executor 的 DAO/Entity/Mapper 到 trader-base

**Files:**
- Move/Delete: `/Users/ming/Workspace/trader/trader-executor/src/main/java/cc/riskswap/trader/executor/dao/**`
- Modify/Add: `/Users/ming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/**`
- Add/Modify: `/Users/ming/Workspace/trader/trader-executor/src/main/java/cc/riskswap/trader/executor/config/TraderMybatisScanConfig.java`
- Modify: `/Users/ming/Workspace/trader/trader-executor/src/main/resources/application.yml`

- [ ] **Step 1: 合并 executor 的 entity 与 base entity**
  - 注意 ClickHouse/MySQL 的区分（按 mapper 注解）

- [ ] **Step 2: 合并 executor 的 mapper 与 base mapper**

- [ ] **Step 3: 合并 executor 的 DAO 与 base DAO**

- [ ] **Step 4: 增加/更新 executor 的 MyBatis 扫描配置为扫描 base mapper 包**

- [ ] **Step 5: 修改 executor 的 type-aliases-package 指向 base entity 包**

- [ ] **Step 6: 编译 trader-executor**
  - Run: `mvn -pl trader-executor -DskipTests compile`
  - Expected: `BUILD SUCCESS`

---

### Task 5: 迁移并合并 trader-statistic 的 DAO/Entity/Mapper 到 trader-base

**Files:**
- Move/Delete: `/Users/ming/Workspace/trader/trader-statistic/src/main/java/cc/riskswap/trader/statistic/dao/**`
- Modify/Add: `/Users/ming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/**`
- Add/Modify: `/Users/ming/Workspace/trader/trader-statistic/src/main/java/cc/riskswap/trader/statistic/config/TraderMybatisScanConfig.java`
- Modify: `/Users/ming/Workspace/trader/trader-statistic/src/main/resources/application.yml`

- [ ] **Step 1: 合并 statistic 的 entity/mapper/dao 到 base**
  - 对 `CorrelationMapper` 等含自定义 SQL 的 mapper，合并时保留方法与注解 SQL

- [ ] **Step 2: 增加/更新 statistic 的 MyBatis 扫描配置为扫描 base mapper 包**

- [ ] **Step 3: 修改 statistic 的 type-aliases-package 指向 base entity 包**

- [ ] **Step 4: 编译 trader-statistic**
  - Run: `mvn -pl trader-statistic -DskipTests compile`
  - Expected: `BUILD SUCCESS`

---

### Task 6: MySQL DDL 将 id 与引用列 BIGINT -> INT，并增加升级脚本

**Files:**
- Modify: `/Users/ming/Workspace/trader/trader-admin/admin-server/src/main/resources/db/mysql.sql`
- Add: `/Users/ming/Workspace/trader/trader-admin/admin-server/src/main/resources/db/upgrade/mysql/1.0.5.sql`

- [ ] **Step 1: 列出 mysql.sql 中所有 BIGINT 列并分类**
  - 分类：主键 id / 引用列 *_id / 非主键语义（耗时、版本号等）

- [ ] **Step 2: 修改建表脚本**
  - `id BIGINT ... AUTO_INCREMENT` -> `id INT ... AUTO_INCREMENT`
  - `*_id BIGINT` -> `*_id INT`
  - 其它 `BIGINT`（如 `execution_ms/version/duration_ms`）保持不变

- [ ] **Step 3: 编写升级脚本**
  - 为每张受影响表写 `ALTER TABLE ... MODIFY COLUMN ...`
  - 覆盖主键与引用列

- [ ] **Step 4: 编译 admin-server**
  - Run: `mvn -pl trader-admin/admin-server -DskipTests compile`
  - Expected: `BUILD SUCCESS`

---

### Task 7: 全量编译验证

**Files:**
- None

- [ ] **Step 1: 根目录全量编译**
  - Run: `mvn -DskipTests compile`
  - Expected: `BUILD SUCCESS`

