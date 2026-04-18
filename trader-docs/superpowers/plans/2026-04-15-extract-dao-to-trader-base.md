# Extract DAO Layer to trader-base Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract the duplicated DAO layer (Entities, Mappers, DAOs) from `trader-admin`, `trader-collector`, and `trader-executor` into the shared `trader-base` module and update all references.

**Architecture:** We will create `cc.riskswap.trader.base.dao`, `cc.riskswap.trader.base.dao.entity`, and `cc.riskswap.trader.base.dao.mapper` packages in `trader-base`. We will merge duplicate classes across the three projects. Classes like `FundListQuery` that are used by DAOs will also be moved to `trader-base` to avoid circular dependencies.

**Tech Stack:** Java, Spring Boot, MyBatis-Plus

---

### Task 1: Setup Packages in trader-base

**Files:**
- Create: `trader-base/src/main/java/cc/riskswap/trader/base/dao`
- Create: `trader-base/src/main/java/cc/riskswap/trader/base/dao/entity`
- Create: `trader-base/src/main/java/cc/riskswap/trader/base/dao/mapper`
- Create: `trader-base/src/main/java/cc/riskswap/trader/base/dao/query`
- Create: `trader-base/src/main/java/cc/riskswap/trader/base/dao/param`

- [ ] **Step 1: Create directories**
Run:
```bash
mkdir -p /Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/entity
mkdir -p /Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/mapper
mkdir -p /Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/query
mkdir -p /Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/param
```

### Task 2: Move and Merge Fund-related DAOs

**Files:**
- Create: `trader-base/src/main/java/cc/riskswap/trader/base/dao/FundDao.java` (and Entity/Mapper)
- Modify: Remove them from `trader-admin`, `trader-collector`, `trader-executor`

- [ ] **Step 1: Move Entities and Mappers**
Copy `Fund.java`, `FundAdj.java`, `FundMarket.java`, `FundNav.java` and their Mappers from `trader-executor` to `trader-base`. Update their package names to `cc.riskswap.trader.base.dao.entity` and `cc.riskswap.trader.base.dao.mapper`.
Merge any missing fields from `trader-admin` and `trader-collector` entities.

- [ ] **Step 2: Move DAOs and Queries**
Copy `FundDao.java`, `FundAdjDao.java`, `FundMarketDao.java`, `FundNavDao.java` to `trader-base`. Update package to `cc.riskswap.trader.base.dao`.
Move `FundListQuery` and `FundUpdateParam` from `trader-admin` to `trader-base/src/main/java/cc/riskswap/trader/base/dao/query` and `param`.
Merge all methods from `trader-admin`, `trader-collector`, and `trader-executor` versions of these DAOs into the new `trader-base` DAOs.

### Task 3: Move and Merge Investment-related DAOs

**Files:**
- Create: `trader-base/src/main/java/cc/riskswap/trader/base/dao/InvestmentDao.java` (and related Log, Position, Trading)

- [ ] **Step 1: Move Entities and Mappers**
Copy `Investment.java`, `InvestmentLog.java`, `InvestmentPosition.java`, `InvestmentTrading.java` and their Mappers to `trader-base`. Update package names.

- [ ] **Step 2: Move DAOs**
Copy `InvestmentDao.java`, `InvestmentLogDao.java`, `InvestmentPositionDao.java`, `InvestmentTradingDao.java` to `trader-base`. Update package names and merge any duplicate methods from other projects.

### Task 4: Move and Merge Other DAOs (Correlation, Calendar, ImportLog)

**Files:**
- Create: `CorrelationDao.java`, `CalendarDao.java`, `ImportLogDao.java` (and Entities/Mappers)

- [ ] **Step 1: Move to trader-base**
Copy these classes to `trader-base`, update their package names, and merge any duplicates.

### Task 5: Delete Old DAO Packages

- [ ] **Step 1: Delete old packages**
Run:
```bash
rm -rf /Users/haiming/Workspace/trader/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/dao
rm -rf /Users/haiming/Workspace/trader/trader-collector/src/main/java/cc/riskswap/trader/collector/repository
rm -rf /Users/haiming/Workspace/trader/trader-executor/src/main/java/cc/riskswap/trader/executor/dao
```

### Task 6: Update Imports in Consuming Projects

- [ ] **Step 1: Update trader-admin**
Find and replace all `cc.riskswap.trader.admin.dao` imports with `cc.riskswap.trader.base.dao` in `trader-admin`.
Find and replace `cc.riskswap.trader.admin.common.model.query.FundListQuery` with `cc.riskswap.trader.base.dao.query.FundListQuery`.

- [ ] **Step 2: Update trader-collector**
Find and replace all `cc.riskswap.trader.collector.repository.dao` and `cc.riskswap.trader.collector.repository.entity` imports with `cc.riskswap.trader.base.dao` and `cc.riskswap.trader.base.dao.entity`.

- [ ] **Step 3: Update trader-executor**
Find and replace all `cc.riskswap.trader.executor.dao` imports with `cc.riskswap.trader.base.dao`.

- [ ] **Step 4: Verify Build**
Run:
```bash
cd /Users/haiming/Workspace/trader
mvn clean install -DskipTests
```
Fix any remaining import or compilation errors until the build succeeds.
