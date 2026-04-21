# Correlation ClickHouse Batch Config Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将相关性统计写入 ClickHouse 的保存批次和历史清理删除批次改为可配置参数。

**Architecture:** 继续沿用 `StatisticCorrelationProperties` 作为统一的相关性配置入口，新增写入批次配置及安全兜底方法。`CorrelationService` 只读取配置类，不再写死保存和删除批次常量，保持现有计算逻辑与任务入口不变。

**Tech Stack:** Java, Spring Boot, ConfigurationProperties, JUnit 5, Mockito, MyBatis-Plus

---

### Task 1: 扩展配置类与默认值测试

**Files:**
- Modify: `/Users/ming/Workspace/trader/trader-statistic/src/main/java/cc/riskswap/trader/statistic/config/StatisticCorrelationProperties.java`
- Test: `/Users/ming/Workspace/trader/trader-statistic/src/test/java/cc/riskswap/trader/statistic/config/StatisticCorrelationPropertiesTest.java`

- [ ] **Step 1: 写失败测试，约束新增默认值**

```java
@Test
void should_use_default_clickhouse_batch_sizes() {
    StatisticCorrelationProperties properties = new StatisticCorrelationProperties();

    Assertions.assertEquals(200, properties.getSaveBatchSize());
    Assertions.assertEquals(200, properties.getCleanupDeleteBatchSize());
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw -pl trader-statistic -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=StatisticCorrelationPropertiesTest test`
Expected: FAIL，因为 `StatisticCorrelationProperties` 还没有对应字段和 getter

- [ ] **Step 3: 最小实现配置字段与安全方法**

```java
private static final int DEFAULT_SAVE_BATCH_SIZE = 200;
private static final int DEFAULT_CLEANUP_DELETE_BATCH_SIZE = 200;

private int saveBatchSize = DEFAULT_SAVE_BATCH_SIZE;
private int cleanupDeleteBatchSize = DEFAULT_CLEANUP_DELETE_BATCH_SIZE;
```

- [ ] **Step 4: 再跑测试确认通过**

Run: `./mvnw -pl trader-statistic -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=StatisticCorrelationPropertiesTest test`
Expected: PASS

### Task 2: 服务层切到配置化批次

**Files:**
- Modify: `/Users/ming/Workspace/trader/trader-statistic/src/main/java/cc/riskswap/trader/statistic/service/CorrelationService.java`
- Test: `/Users/ming/Workspace/trader/trader-statistic/src/test/java/cc/riskswap/trader/statistic/service/CorrelationServiceTest.java`

- [ ] **Step 1: 写失败测试，约束 `saveBatch(...)` 使用配置值**

```java
@Test
void should_use_configured_save_batch_size_when_persisting_correlations() {
    // properties.setSaveBatchSize(1)
    // verify correlationDao.saveBatch(..., 1)
}
```

- [ ] **Step 2: 写失败测试，约束清理删除批次使用配置值**

```java
@Test
void should_use_configured_cleanup_delete_batch_size_when_cleaning_historical_correlations() {
    // properties.setCleanupDeleteBatchSize(2)
    // verify correlationDao.deleteByIds(...) batching follows 2
}
```

- [ ] **Step 3: 运行测试确认失败**

Run: `./mvnw -pl trader-statistic -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=CorrelationServiceTest test`
Expected: FAIL，因为 `CorrelationService` 仍然使用写死常量

- [ ] **Step 4: 最小实现服务层读取配置**

```java
int saveBatchSize = correlationProperties.getSafeSaveBatchSize();
int cleanupDeleteBatchSize = correlationProperties.getSafeCleanupDeleteBatchSize();
```

- [ ] **Step 5: 再跑测试确认通过**

Run: `./mvnw -pl trader-statistic -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=CorrelationServiceTest test`
Expected: PASS

### Task 3: 暴露应用配置并做回归

**Files:**
- Modify: `/Users/ming/Workspace/trader/trader-statistic/src/main/resources/application.yml`
- Test: `/Users/ming/Workspace/trader/trader-statistic/src/test/java/cc/riskswap/trader/statistic/config/StatisticCorrelationPropertiesTest.java`
- Test: `/Users/ming/Workspace/trader/trader-statistic/src/test/java/cc/riskswap/trader/statistic/service/CorrelationServiceTest.java`

- [ ] **Step 1: 在 `application.yml` 增加配置项**

```yml
trader:
  statistic:
    correlation:
      save-batch-size: ${TRADER_CORRELATION_SAVE_BATCH_SIZE:200}
      cleanup-delete-batch-size: ${TRADER_CORRELATION_CLEANUP_DELETE_BATCH_SIZE:200}
```

- [ ] **Step 2: 跑聚焦回归**

Run: `./mvnw -pl trader-statistic -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=StatisticCorrelationPropertiesTest,CorrelationServiceTest test`
Expected: PASS

- [ ] **Step 3: 跑模块回归**

Run: `./mvnw -pl trader-statistic -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=StatisticCorrelationPropertiesTest,TraderTransactionConfigTest,CorrelationServiceTest,CorrelationTaskTest,CorrelationBackendStructureTest,PackagingStructureTest test`
Expected: PASS
