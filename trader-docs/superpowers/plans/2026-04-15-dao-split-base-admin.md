# DAO Split (base vs admin) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep only monitoring/reporting/task DAOs in `trader-base`, and move all other DAOs used by `trader-admin/admin-server` back into `admin-server` so each project owns its business DAOs.

**Architecture:** Retain cross-service “monitoring & reporting” tables (Node*, SystemTask*, TaskLog, MsgPushLog) in `trader-base`. Introduce `cc.riskswap.trader.base.dao.*` in admin-server for business DAOs. Ensure Mapper scanning in admin includes both `admin` and `base` mapper packages.

**Tech Stack:** Java, Spring Boot, MyBatis-Plus

---

## Task 1: Inventory & freeze the target lists

**Files:**
- Inspect: `/Users/ming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/**`
- Inspect: `/Users/ming/Workspace/trader/trader-admin/admin-server/src/main/java/**`

- [ ] **Step 1: List all classes under trader-base dao**

Run:

```bash
find trader-base/src/main/java/cc/riskswap/trader/base/dao -type f | sort
```

- [ ] **Step 2: Confirm “keep list” stays in trader-base**

Keep:

- `NodeDao` / `NodeGroupDao` / `NodeMonitorDao`
- `SystemTaskDao` / `SystemTaskRunLogDao`
- `TaskLogDao`
- `MsgPushLogDao`

- [ ] **Step 3: Everything else is “move to admin-server”**

---

## Task 2: Make admin-server scan both admin and base mappers (mandatory)

**Files:**
- Create: `/Users/ming/Workspace/trader/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/config/TraderMybatisScanConfig.java`
- Modify: `/Users/ming/Workspace/trader/trader-admin/admin-server/src/main/resources/application.yml`

- [ ] **Step 1: Add explicit mysql/clickhouse MapperScannerConfigurer beans**

Requirements:
- Define beans with the same names as `trader-base` auto-config (`mysqlMapperScannerConfigurer`, `clickHouseMapperScannerConfigurer`) so they override `@ConditionalOnMissingBean`.
- Set base package to include both:
  - `cc.riskswap.trader.base.dao.mapper`
  - `cc.riskswap.trader.base.dao.mapper`
- Keep annotation filtering:
  - `setAnnotationClass(MysqlMapper.class)` and `setAnnotationClass(ClickHouseMapper.class)`

- [ ] **Step 2: Extend mybatis-plus type-aliases-package**

Update:

```yaml
mybatis-plus:
  type-aliases-package: cc.riskswap.trader.base.dao.entity,cc.riskswap.trader.base.dao.entity
```

- [ ] **Step 3: Compile admin-server**

Run:

```bash
cd trader-admin && mvn -pl admin-server -DskipTests compile
```

Expected: `BUILD SUCCESS`

---

## Task 3: Create admin-server DAO packages and move business DAOs from trader-base

**Files:**
- Create/Move into admin-server:
  - `/Users/ming/Workspace/trader/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/dao/**`
- Delete from trader-base:
  - `/Users/ming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/**` (excluding keep list + dependencies)

- [ ] **Step 1: Move business DAOs (DAO + entity + mapper + query + param + dto)**

Move targets (non-keep):
- Market & dictionary: `Broker*`, `Calendar*`, `Exchange*`, `Fund*`, `Correlation*`
- Trading domain: `Investment*`
- User domain: `User*`
- DB exec/upgrade: `SqlExec*`, `ClickHouseSqlExec*`, `SystemUpgrade*`
- Import logs (if present): `ImportLog*`

- [ ] **Step 2: Fix packages inside moved files**

Change:

- `package cc.riskswap.trader.base.dao...` → `package cc.riskswap.trader.base.dao...`
- Update imports for moved entities/mappers/queries/params accordingly.
- Keep `MysqlMapper/ClickHouseMapper` annotations imported from `trader-base`.

- [ ] **Step 3: Update admin-server services imports**

Update references from `cc.riskswap.trader.base.dao.*` to `cc.riskswap.trader.base.dao.*` for moved classes.

- [ ] **Step 4: Compile admin-server**

Run:

```bash
cd trader-admin && mvn -pl admin-server -DskipTests compile
```

Expected: `BUILD SUCCESS`

---

## Task 4: Remove moved DAOs from trader-base and verify base still compiles

**Files:**
- Modify/Delete: `/Users/ming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/**`
- Verify: `/Users/ming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/**`

- [ ] **Step 1: Ensure trader-base only contains keep list DAOs and dependencies**

- [ ] **Step 2: Compile and test trader-base**

Run:

```bash
cd trader-base && mvn test
```

Expected: `BUILD SUCCESS`

---

## Task 5: Runtime smoke test (admin-server)

**Files:**
- Verify runtime: `/Users/ming/Workspace/trader/trader-admin/admin-server`

- [ ] **Step 1: Start admin-server**

Run:

```bash
cd trader-admin && mvn -pl admin-server spring-boot:run
```

- [ ] **Step 2: Verify key beans work**

Acceptance:
- `NodeDao` / `SystemTaskDao` / `TaskLogDao` can be injected and used (at least 1 query path runs without MyBatis binding errors).

