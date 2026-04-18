# Correlation History Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在保持相关性 append-only 写入的前提下，于全量重算结束后批量清理旧版本，只保留每组业务键的最新记录。

**Architecture:** 写路径继续只做 `INSERT`，读路径继续只返回最新版本。新增一条专用的历史清理链路：先等待本轮相关性计算任务全部完成，再按 `(asset1, asset2, period)` 找出旧版本 `id` 并分批执行删除，避免逐条 mutation。

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, ClickHouse, JUnit 5, Mockito

---

### Task 1: 为历史清理写失败测试

**Files:**
- Modify: `admin-server/src/test/java/cc/riskswap/trader/admin/test/service/CorrelationServiceTest.java`
- Modify: `admin-server/src/test/java/cc/riskswap/trader/admin/test/task/CorrelationTaskTest.java`
- Modify: `admin-server/src/test/java/cc/riskswap/trader/admin/test/dao/CorrelationMapperSqlTest.java`

- [ ] **Step 1: 写服务层失败测试，约束清理只删除旧版本**

```java
@Test
void shouldDeleteOnlyHistoricalCorrelationIdsInBatches() {
    // mock duplicate keys + stale ids
    // verify dao.deleteByIds(batch) called
    // verify latest id is not deleted
}
```

- [ ] **Step 2: 写任务层失败测试，约束全量任务完成后触发清理**

```java
@Test
void shouldCleanupHistoryAfterAllCorrelationJobsComplete() {
    // stub executor to run tasks
    // verify correlationService.cleanupHistoricalCorrelations() invoked once
}
```

- [ ] **Step 3: 写 mapper 失败测试，约束存在批量查询/批量删除 SQL**

```java
Assertions.assertNotNull(findMethod("selectDuplicateGroups"));
Assertions.assertNotNull(findMethod("selectHistoricalIds"));
Assertions.assertNotNull(findMethod("deleteByIds"));
```

- [ ] **Step 4: 运行测试确认失败**

Run: `./mvnw -pl admin-server -Dtest=CorrelationServiceTest,CorrelationTaskTest,CorrelationMapperSqlTest test`
Expected: FAIL，因为当前还没有批量历史清理能力和任务完成后的触发逻辑

### Task 2: 实现批量历史清理 SQL 与 DAO

**Files:**
- Modify: `admin-server/src/main/java/cc/riskswap/trader/admin/dao/mapper/CorrelationMapper.java`
- Modify: `admin-server/src/main/java/cc/riskswap/trader/admin/dao/CorrelationDao.java`

- [ ] **Step 1: 定义重复业务键查询 SQL**

```java
@Select("""
SELECT asset1, asset2, period
FROM correlation
GROUP BY asset1, asset2, period
HAVING count() > 1
LIMIT #{limit} OFFSET #{offset}
""")
```

- [ ] **Step 2: 定义某个业务键下历史版本 id 查询 SQL**

```java
@Select("""
SELECT id
FROM correlation
WHERE asset1 = #{asset1} AND asset2 = #{asset2} AND period = #{period}
ORDER BY updated_at DESC, id DESC
LIMIT 100000 OFFSET 1
""")
```

- [ ] **Step 3: 定义按 id 批量删除 SQL**

```java
@Update({
  "<script>",
  "ALTER TABLE correlation DELETE WHERE id IN ",
  "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
  "</script>"
})
```

- [ ] **Step 4: 在 DAO 中封装重复键查询、历史 id 查询和批量删除方法**

- [ ] **Step 5: 运行 mapper/dao 相关测试**

Run: `./mvnw -pl admin-server -Dtest=CorrelationMapperSqlTest test`
Expected: PASS

### Task 3: 实现历史清理服务

**Files:**
- Modify: `admin-server/src/main/java/cc/riskswap/trader/admin/service/CorrelationService.java`
- Test: `admin-server/src/test/java/cc/riskswap/trader/admin/test/service/CorrelationServiceTest.java`

- [ ] **Step 1: 在服务层新增 `cleanupHistoricalCorrelations()`**

```java
public int cleanupHistoricalCorrelations() {
    // page duplicate groups
    // collect stale ids
    // delete in batches
    // return deleted count
}
```

- [ ] **Step 2: 用小批次聚合旧 id 再删除**

```java
if (staleIds.size() >= DELETE_BATCH_SIZE) {
    correlationDao.deleteByIds(batch);
}
```

- [ ] **Step 3: 保持 update/calculateAndSave 继续 append-only**

- [ ] **Step 4: 运行服务测试**

Run: `./mvnw -pl admin-server -Dtest=CorrelationServiceTest test`
Expected: PASS

### Task 4: 在全量任务结束后触发历史清理

**Files:**
- Modify: `admin-server/src/main/java/cc/riskswap/trader/admin/task/CorrelationTask.java`
- Modify: `admin-server/src/test/java/cc/riskswap/trader/admin/test/task/CorrelationTaskTest.java`

- [ ] **Step 1: 将异步提交改为可等待的任务收集**

```java
List<CompletableFuture<Void>> futures = new ArrayList<>();
futures.add(CompletableFuture.runAsync(() -> correlationService.calculateAndSave(...), correlationExecutor));
```

- [ ] **Step 2: 等待全部任务完成**

```java
CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
```

- [ ] **Step 3: 完成后调用历史清理**

```java
int deleted = correlationService.cleanupHistoricalCorrelations();
TaskContextUtil.appendLine(String.format("- 清理旧版本 %d 条", deleted));
```

- [ ] **Step 4: 运行任务测试**

Run: `./mvnw -pl admin-server -Dtest=CorrelationTaskTest test`
Expected: PASS

### Task 5: 做组合回归验证

**Files:**
- Test: `admin-server/src/test/java/cc/riskswap/trader/admin/test/service/CorrelationServiceTest.java`
- Test: `admin-server/src/test/java/cc/riskswap/trader/admin/test/task/CorrelationTaskTest.java`
- Test: `admin-server/src/test/java/cc/riskswap/trader/admin/test/dao/CorrelationMapperSqlTest.java`

- [ ] **Step 1: 运行组合测试**

Run: `./mvnw -pl admin-server -Dtest=CorrelationServiceTest,CorrelationTaskTest,CorrelationMapperSqlTest test`
Expected: PASS

- [ ] **Step 2: 检查最近修改文件诊断**

Run: IDE diagnostics for `CorrelationService.java`, `CorrelationTask.java`, `CorrelationDao.java`, `CorrelationMapper.java`
Expected: no diagnostics
