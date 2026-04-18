# Correlation List Normal Page Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将相关性列表分页恢复为普通查询，不再按业务键去重，同时保留 latest-only 的写路径与历史清理逻辑。

**Architecture:** 仅回退 `CorrelationDao.pageQuery()` 到 MyBatis-Plus 普通分页查询，沿用已有筛选条件与排序。`getByUniqueKey()`、历史清理 SQL 和 append-only 写入逻辑保持不变，避免影响现有重算与清理流程。

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, ClickHouse, JUnit 5

---

### Task 1: 先写失败测试约束列表分页走普通查询

**Files:**
- Modify: `admin-server/src/test/java/cc/riskswap/trader/admin/test/dao/CorrelationMapperSqlTest.java`
- Modify: `admin-server/src/main/java/cc/riskswap/trader/admin/dao/CorrelationDao.java`

- [ ] **Step 1: 写失败测试**

```java
@Test
void pageQueryShouldNotDependOnLatestOnlyPageSql() {
    // assert list paging does not require selectLatestPage/countLatestPage
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw -pl admin-server -Dtest=CorrelationMapperSqlTest test`
Expected: FAIL，因为当前列表分页仍依赖 latest-only SQL

- [ ] **Step 3: 写最小实现**

```java
Page<Correlation> page = new Page<>(query.getPageNo(), query.getPageSize());
LambdaQueryWrapper<Correlation> wrapper = new LambdaQueryWrapper<>();
// apply filters
wrapper.orderByDesc(Correlation::getCreatedAt);
return this.page(page, wrapper);
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./mvnw -pl admin-server -Dtest=CorrelationMapperSqlTest test`
Expected: PASS

### Task 2: 保持 latest-only 辅助逻辑不变

**Files:**
- Modify: `admin-server/src/test/java/cc/riskswap/trader/admin/test/service/CorrelationServiceTest.java`
- Modify: `admin-server/src/main/java/cc/riskswap/trader/admin/dao/CorrelationDao.java`

- [ ] **Step 1: 保持 `getByUniqueKey()` 继续走 latest-only mapper**
- [ ] **Step 2: 补或保留针对 latest-only 辅助逻辑的断言**
- [ ] **Step 3: 运行服务测试确认 append-only 和历史清理链路未受影响**

Run: `./mvnw -pl admin-server -Dtest=CorrelationServiceTest test`
Expected: PASS

### Task 3: 做组合回归验证

**Files:**
- Test: `admin-server/src/test/java/cc/riskswap/trader/admin/test/dao/CorrelationMapperSqlTest.java`
- Test: `admin-server/src/test/java/cc/riskswap/trader/admin/test/service/CorrelationServiceTest.java`
- Test: `admin-server/src/test/java/cc/riskswap/trader/admin/test/task/CorrelationTaskTest.java`

- [ ] **Step 1: 运行组合测试**

Run: `./mvnw -pl admin-server -Dtest=CorrelationMapperSqlTest,CorrelationServiceTest,CorrelationTaskTest test`
Expected: PASS

- [ ] **Step 2: 检查最近修改文件诊断**

Run: IDE diagnostics for `CorrelationDao.java` and any touched tests
Expected: no diagnostics
