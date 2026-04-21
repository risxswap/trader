# Correlation Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不改变相关性统计行为的前提下，提炼 `CorrelationService` 内部公共 helper，消除单对与批量路径中的重复计算逻辑。

**Architecture:** 这次只做轻量代码整理，不新增公开组件、不改任务入口或 DAO 接口。核心做法是在 `CorrelationService` 内抽出统一的净值序列转换、相关性统计和实体构造 helper，让 `calculateAndSave(...)` 与 `calculateAndSaveBatch(...)` 共享同一套内部规则。

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Apache Commons Math, JUnit 5, Mockito

---

### Task 1: 为重构建立行为保护测试

**Files:**
- Modify: `trader-statistic/src/test/java/cc/riskswap/trader/statistic/service/CorrelationServiceTest.java`
- Test: `trader-statistic/src/test/java/cc/riskswap/trader/statistic/task/CorrelationTaskTest.java`

- [ ] **Step 1: 补一个失败测试，约束单对接口与批量接口遵循同样的显著性过滤规则**

```java
@Test
void shouldApplySameCorrelationRulesForSingleAndBatchFlows() {
    // arrange equivalent NAV samples
    // verify both flows persist or skip consistently
}
```

- [ ] **Step 2: 运行服务测试确认先失败**

Run: `./mvnw -pl trader-statistic -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=CorrelationServiceTest test`
Expected: FAIL，因为当前单对与批量路径尚未完全共享同一套 helper

### Task 2: 提炼公共统计 helper

**Files:**
- Modify: `trader-statistic/src/main/java/cc/riskswap/trader/statistic/service/CorrelationService.java`
- Test: `trader-statistic/src/test/java/cc/riskswap/trader/statistic/service/CorrelationServiceTest.java`

- [ ] **Step 1: 提炼 `toNavSeries(List<FundNav>)`**

```java
private Map<LocalDate, BigDecimal> toNavSeries(List<FundNav> navs) {
    return navs.stream()
            .filter(nav -> nav.getTime() != null && nav.getAdjNav() != null)
            .collect(...);
}
```

- [ ] **Step 2: 提炼 `calculateCorrelationStats(...)`，统一对齐、Pearson 和 p-value**

```java
private CorrelationStats calculateCorrelationStats(
        Map<LocalDate, BigDecimal> series1,
        Map<LocalDate, BigDecimal> series2) {
    // align dates
    // compute coefficient/pValue
    // return null if pair should be skipped
}
```

- [ ] **Step 3: 提炼 `createCorrelation(...)`，统一实体组装**

```java
private Correlation createCorrelation(Fund fund1, Fund fund2, String period, CorrelationStats stats) {
    // fill id, types, timestamps, coefficient, pValue
}
```

- [ ] **Step 4: 让 `calculateAndSave(...)` 复用新 helper**

```java
Map<LocalDate, BigDecimal> series1 = toNavSeries(navs1);
Map<LocalDate, BigDecimal> series2 = toNavSeries(navs2);
CorrelationStats stats = calculateCorrelationStats(series1, series2);
```

- [ ] **Step 5: 让 `calculateAndSaveBatch(...)` 复用同一套 helper**

```java
CorrelationStats stats = calculateCorrelationStats(series1, series2);
Correlation correlation = createCorrelation(fund1, fund2, period, stats);
```

- [ ] **Step 6: 运行服务测试确认通过**

Run: `./mvnw -pl trader-statistic -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=CorrelationServiceTest test`
Expected: PASS

### Task 3: 回归任务入口与结构测试

**Files:**
- Test: `trader-statistic/src/test/java/cc/riskswap/trader/statistic/task/CorrelationTaskTest.java`
- Test: `trader-statistic/src/test/java/cc/riskswap/trader/statistic/CorrelationBackendStructureTest.java`
- Test: `trader-statistic/src/test/java/cc/riskswap/trader/statistic/PackagingStructureTest.java`

- [ ] **Step 1: 运行任务测试确认整理没有影响任务调用行为**

Run: `./mvnw -pl trader-statistic -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=CorrelationTaskTest test`
Expected: PASS

- [ ] **Step 2: 运行当前模块相关测试集合**

Run: `./mvnw -pl trader-statistic -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=CorrelationBackendStructureTest,PackagingStructureTest,CorrelationServiceTest,CorrelationTaskTest test`
Expected: PASS

- [ ] **Step 3: 检查最近修改文件诊断**

Run: IDE diagnostics for `CorrelationService.java` and `CorrelationServiceTest.java`
Expected: no diagnostics

- [ ] **Step 4: 记录这轮整理结果**

```text
- duplicated stats logic removed
- single and batch flows share the same calculation helper
- behavior preserved by existing tests
```
