# Trader Statistic Correlation Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将相关性后台计算与历史清理从 `trader-admin` 迁移到 `trader-statistic`，并让 `trader-statistic` 具备与 `trader-collector` 一致的 `trader-base` 依赖、打包和 `docker-compose.yml` 部署能力。

**Architecture:** `trader-statistic` 补齐 collector 风格的可部署骨架，再迁入相关性 task/service/dao/entity/config 等后台职责。`trader-admin` 保留前台查询与管理接口，但移除后台统计任务职责；两边共享同一份 `correlation` 表。

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, ClickHouse, MySQL, Redis, trader-base, JUnit 5

---

### Task 1: 给 trader-statistic 补齐 collector 风格工程骨架

**Files:**
- Modify: `/Users/ming/Workspace/trader/trader-statistic/pom.xml`
- Create: `/Users/ming/Workspace/trader/trader-statistic/package.sh`
- Create: `/Users/ming/Workspace/trader/trader-statistic/docker-compose.yml`
- Create: `/Users/ming/Workspace/trader/trader-statistic/bin/run.sh`
- Modify: `/Users/ming/Workspace/trader/trader-statistic/src/main/resources/application.yml`
- Create: `/Users/ming/Workspace/trader/trader-statistic/src/main/resources/config.properties`
- Create: `/Users/ming/Workspace/trader/trader-statistic/src/main/resources/logback.xml`

- [ ] **Step 1: 写结构测试或最小验证脚本约束部署骨架**

```bash
test -f package.sh
test -f docker-compose.yml
test -f bin/run.sh
```

- [ ] **Step 2: 运行一次构建确认当前 trader-statistic 现状**

Run: `./mvnw test`
Expected: PASS 或暴露缺少依赖/资源文件的问题

- [ ] **Step 3: 参考 trader-collector 修改 pom 和打包文件**

```xml
<dependency>
  <groupId>cc.riskswap.trader</groupId>
  <artifactId>trader-base</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

- [ ] **Step 4: 运行构建确认骨架可用**

Run: `./mvnw test`
Expected: PASS

### Task 2: 迁移相关性后台实体与 DAO 链路到 trader-statistic

**Files:**
- Create: `/Users/ming/Workspace/trader/trader-statistic/src/main/java/cc/riskswap/trader/statistic/dao/entity/Correlation.java`
- Create: `/Users/ming/Workspace/trader/trader-statistic/src/main/java/cc/riskswap/trader/statistic/dao/entity/CorrelationDuplicateGroup.java`
- Create: `/Users/ming/Workspace/trader/trader-statistic/src/main/java/cc/riskswap/trader/statistic/dao/mapper/CorrelationMapper.java`
- Create: `/Users/ming/Workspace/trader/trader-statistic/src/main/java/cc/riskswap/trader/statistic/dao/CorrelationDao.java`
- Create: `/Users/ming/Workspace/trader/trader-statistic/src/main/java/cc/riskswap/trader/statistic/dao/FundDao.java`
- Create: `/Users/ming/Workspace/trader/trader-statistic/src/main/java/cc/riskswap/trader/statistic/dao/FundNavDao.java`
- Create: matching entity/mapper/config files required by the migrated DAOs

- [ ] **Step 1: 先写失败测试或结构测试，约束相关性 DAO 在 statistic 中存在**
- [ ] **Step 2: 迁移 `Correlation` / `CorrelationDuplicateGroup`**
- [ ] **Step 3: 迁移 `CorrelationMapper` / `CorrelationDao` 及其依赖**
- [ ] **Step 4: 迁移 `FundDao` / `FundNavDao` 的最小依赖**
- [ ] **Step 5: 运行针对性测试或编译验证**

Run: `./mvnw -Dtest=*Correlation* test`
Expected: PASS 或至少编译通过相关类

### Task 3: 迁移相关性后台 service 与 task

**Files:**
- Create: `/Users/ming/Workspace/trader/trader-statistic/src/main/java/cc/riskswap/trader/statistic/service/CorrelationService.java`
- Create: `/Users/ming/Workspace/trader/trader-statistic/src/main/java/cc/riskswap/trader/statistic/task/CorrelationTask.java`
- Create/Modify: task logging / datasource / task executor config files required by the migrated task

- [ ] **Step 1: 写失败测试，约束 statistic 中存在相关性计算与清理入口**
- [ ] **Step 2: 迁移 `calculateAndSave`、`saveCorrelation`、`cleanupHistoricalCorrelations`**
- [ ] **Step 3: 迁移 `CorrelationTask` 并保留“等待任务完成后再清理”的行为**
- [ ] **Step 4: 补齐 task 执行器、AOP 或最小日志依赖**
- [ ] **Step 5: 运行相关测试**

Run: `./mvnw -Dtest=*Correlation* test`
Expected: PASS

### Task 4: 回收 trader-admin 中的后台相关性职责

**Files:**
- Modify: `/Users/ming/Workspace/trader/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/CorrelationService.java`
- Modify: `/Users/ming/Workspace/trader/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/task/CorrelationTask.java`
- Modify: `/Users/ming/Workspace/trader/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/dao/CorrelationDao.java`
- Modify: `/Users/ming/Workspace/trader/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/dao/mapper/CorrelationMapper.java`
- Modify: affected tests under `/Users/ming/Workspace/trader/trader-admin/admin-server/src/test/java`

- [ ] **Step 1: 写失败测试，约束 admin 不再承担后台任务职责**
- [ ] **Step 2: 移除或下沉 admin 中的计算/清理 task 与 service 方法**
- [ ] **Step 3: 保留 list/detail/add/update/delete 等前台接口能力**
- [ ] **Step 4: 调整受影响测试**
- [ ] **Step 5: 运行 admin 针对性测试**

Run: `./mvnw -pl admin-server -Dtest=CorrelationDaoTest,CorrelationServiceTest,CorrelationMapperSqlTest test`
Expected: PASS

### Task 5: 端到端构建与打包验证

**Files:**
- Test/Verify both projects build and statistic packaging works

- [ ] **Step 1: 构建 trader-statistic**

Run: `./mvnw test`
Expected: PASS

- [ ] **Step 2: 运行 trader-statistic 打包脚本**

Run: `bash package.sh`
Expected: 生成 `target/trader-statistic.tar.gz`

- [ ] **Step 3: 构建 trader-admin 的 server 模块**

Run: `./mvnw -pl admin-server test`
Expected: PASS

- [ ] **Step 4: 检查最近修改文件 diagnostics**

Run: IDE diagnostics for changed Java/YAML/script files in both repos
Expected: no diagnostics
