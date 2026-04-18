# Correlation Append-Only Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将相关性存储从 ClickHouse mutation 模式改为 append-only 模式，避免 `DELETE/UPDATE` 导致 mutation 积压。

**Architecture:** 保留业务上的“更新生成新 id”语义，但底层只做 `INSERT`。`correlation` 的读取改为按业务键 `(asset1, asset2, period)` 取 `updated_at` 最新版本，列表查询也只返回每组业务键的最新一条记录，从而不再依赖 `ALTER TABLE ... DELETE`。

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, ClickHouse, JUnit 5, Mockito

---

### Task 1: 先写失败测试约束 append-only 读取语义

**Files:**
- Modify: `admin-server/src/test/java/cc/riskswap/trader/admin/test/service/CorrelationServiceTest.java`
- Modify: `admin-server/src/main/java/cc/riskswap/trader/admin/service/CorrelationService.java`

- [ ] **Step 1: 写失败测试**

```java
@Test
void shouldInsertNewCorrelationWithoutDeletingOldRecord() {
    // update() 不再调用 removeByPrimaryId()
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw -pl admin-server -Dtest=CorrelationServiceTest test`
Expected: FAIL，因为当前实现仍会调用 `removeByPrimaryId()`

- [ ] **Step 3: 写最小实现**

```java
correlation.setId(IdUtil.getSnowflakeNextId());
correlation.setUpdatedAt(now);
correlationDao.save(correlation);
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./mvnw -pl admin-server -Dtest=CorrelationServiceTest test`
Expected: PASS

### Task 2: 改 DAO/Mapper 查询只取最新版本

**Files:**
- Modify: `admin-server/src/main/java/cc/riskswap/trader/admin/dao/mapper/CorrelationMapper.java`
- Modify: `admin-server/src/main/java/cc/riskswap/trader/admin/dao/CorrelationDao.java`
- Modify: `admin-server/src/test/java/cc/riskswap/trader/admin/test/dao/CorrelationMapperSqlTest.java`

- [ ] **Step 1: 写失败测试约束 mapper 不再暴露 mutation 删除方法**
- [ ] **Step 2: 把 `selectByPrimaryId`、`getByUniqueKey`、列表查询改成只读最新版本**
- [ ] **Step 3: 删除 `deleteByPrimaryId()` 相关入口**
- [ ] **Step 4: 运行 mapper/dao 相关测试确认通过**

### Task 3: 针对性回归验证

**Files:**
- Test: `admin-server/src/test/java/cc/riskswap/trader/admin/test/service/CorrelationServiceTest.java`
- Test: `admin-server/src/test/java/cc/riskswap/trader/admin/test/dao/CorrelationMapperSqlTest.java`

- [ ] **Step 1: 运行组合测试**

Run: `./mvnw -pl admin-server -Dtest=CorrelationServiceTest,CorrelationMapperSqlTest test`
Expected: PASS

- [ ] **Step 2: 检查最近修改文件诊断**

Run: IDE diagnostics for `CorrelationService.java`, `CorrelationDao.java`, `CorrelationMapper.java`, `CorrelationServiceTest.java`
Expected: no diagnostics
