# Correlation Replace Update Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将相关性更新从 ClickHouse `UPDATE` 改为“删除旧记录并生成新 `id` 插入新记录”。

**Architecture:** 保持 `CorrelationController` 接口不变，在 `CorrelationService.update()` 中实现 replace 语义。DAO 继续负责 ClickHouse 删除与插入，Mapper 中移除不再允许使用的 `updateByPrimaryId()`，并用单元测试约束新行为。

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, ClickHouse, JUnit 5, Mockito

---

### Task 1: 先写失败测试约束 replace 语义

**Files:**
- Create: `admin-server/src/test/java/cc/riskswap/trader/admin/test/service/CorrelationServiceTest.java`
- Modify: `admin-server/src/main/java/cc/riskswap/trader/admin/service/CorrelationService.java`

- [ ] **Step 1: 写失败测试**

```java
@Test
void shouldDeleteOldCorrelationAndInsertNewCorrelationWithNewId() {
    CorrelationDao correlationDao = Mockito.mock(CorrelationDao.class);
    FundDao fundDao = Mockito.mock(FundDao.class);
    FundNavDao fundNavDao = Mockito.mock(FundNavDao.class);
    CorrelationService service = new CorrelationService(correlationDao, fundDao, fundNavDao);

    Correlation existing = new Correlation();
    existing.setId(1L);
    Mockito.when(correlationDao.getByPrimaryId(1L)).thenReturn(existing);

    CorrelationParam param = new CorrelationParam();
    param.setId(1L);
    param.setAsset1("A");
    param.setAsset2("B");
    param.setPeriod("1Y");

    service.update(param);

    Mockito.verify(correlationDao).removeByPrimaryId(1L);
    Mockito.verify(correlationDao).save(Mockito.argThat(item -> !item.getId().equals(1L)));
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw -pl admin-server -Dtest=CorrelationServiceTest test`
Expected: FAIL，因为当前实现仍调用 `updateByPrimaryId()`

- [ ] **Step 3: 写最小实现**

```java
Correlation existing = correlationDao.getByPrimaryId(param.getId());
if (existing == null) {
    throw new Warning(ErrorCode.NOT_FOUND.code(), "相关统计不存在");
}

Correlation correlation = BeanUtil.copyProperties(param, Correlation.class);
correlation.setId(IdUtil.getSnowflakeNextId());
correlation.setCreatedAt(OffsetDateTime.now());
correlation.setUpdatedAt(OffsetDateTime.now());
correlationDao.removeByPrimaryId(existing.getId());
correlationDao.save(correlation);
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./mvnw -pl admin-server -Dtest=CorrelationServiceTest test`
Expected: PASS

### Task 2: 清理 mapper 误导性更新入口并补 SQL 约束

**Files:**
- Modify: `admin-server/src/main/java/cc/riskswap/trader/admin/dao/mapper/CorrelationMapper.java`
- Modify: `admin-server/src/main/java/cc/riskswap/trader/admin/dao/CorrelationDao.java`
- Modify: `admin-server/src/test/java/cc/riskswap/trader/admin/test/dao/CorrelationMapperSqlTest.java`

- [ ] **Step 1: 移除不再允许使用的 mapper 更新方法**

```java
// 删除 updateByPrimaryId()
```

- [ ] **Step 2: 同步移除 DAO 包装方法**

```java
// 删除 CorrelationDao.updateByPrimaryId()
```

- [ ] **Step 3: 调整 SQL 测试**

```java
Assertions.assertNull(findUpdateMethod);
```

- [ ] **Step 4: 运行 mapper 相关测试**

Run: `./mvnw -pl admin-server -Dtest=CorrelationMapperSqlTest test`
Expected: PASS

### Task 3: 做一次针对性回归验证

**Files:**
- Test: `admin-server/src/test/java/cc/riskswap/trader/admin/test/service/CorrelationServiceTest.java`
- Test: `admin-server/src/test/java/cc/riskswap/trader/admin/test/dao/CorrelationMapperSqlTest.java`

- [ ] **Step 1: 运行组合测试**

Run: `./mvnw -pl admin-server -Dtest=CorrelationServiceTest,CorrelationMapperSqlTest test`
Expected: PASS

- [ ] **Step 2: 检查最近修改文件诊断**

Run: IDE diagnostics for `CorrelationService.java`, `CorrelationDao.java`, `CorrelationMapper.java`, `CorrelationServiceTest.java`
Expected: no diagnostics
