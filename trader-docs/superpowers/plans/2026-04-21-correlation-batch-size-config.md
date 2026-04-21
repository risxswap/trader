# Correlation Batch Size Config Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将相关性统计 NAV 查询批次大小改为可配置参数，保留默认行为并支持按环境调参。

**Architecture:** 在 `trader-statistic` 模块新增一个本地 `@ConfigurationProperties` 配置类，负责承载 `trader.statistic.correlation.nav-query-code-batch-size`。`CorrelationService` 只消费该配置值并继续沿用现有分批查询与内存合并逻辑，不改任务入口和统计规则。

**Tech Stack:** Java 21, Spring Boot, `@ConfigurationProperties`, JUnit 5, Mockito

---

### Task 1: 建立配置化行为保护测试

**Files:**
- Modify: `trader-statistic/src/test/java/cc/riskswap/trader/statistic/service/CorrelationServiceTest.java`
- Create: `trader-statistic/src/test/java/cc/riskswap/trader/statistic/config/StatisticCorrelationPropertiesTest.java`

- [ ] **Step 1: 写一个失败测试，验证配置类默认值为 `200`**

```java
@Test
void should_use_default_nav_query_code_batch_size() {
    StatisticCorrelationProperties properties = new StatisticCorrelationProperties();
    assertThat(properties.getNavQueryCodeBatchSize()).isEqualTo(200);
}
```

- [ ] **Step 2: 写一个失败测试，验证服务按配置值而不是写死常量分批**

```java
// inject properties with batch size = 3
// verify dao is called in 3-code chunks
```

- [ ] **Step 3: 运行指定测试确认先失败**

Run: `./mvnw -pl trader-statistic -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=StatisticCorrelationPropertiesTest,CorrelationServiceTest test`
Expected: FAIL，因为配置类和服务注入尚未完成

### Task 2: 引入配置类并接入服务

**Files:**
- Create: `trader-statistic/src/main/java/cc/riskswap/trader/statistic/config/StatisticCorrelationProperties.java`
- Modify: `trader-statistic/src/main/java/cc/riskswap/trader/statistic/Application.java`
- Modify: `trader-statistic/src/main/java/cc/riskswap/trader/statistic/service/CorrelationService.java`
- Modify: `trader-statistic/src/main/resources/application.yml`

- [ ] **Step 1: 新增 `StatisticCorrelationProperties`，提供默认值和非法值兜底**

```java
@ConfigurationProperties(prefix = "trader.statistic.correlation")
public class StatisticCorrelationProperties {
    private int navQueryCodeBatchSize = 200;

    public int getSafeNavQueryCodeBatchSize() {
        return navQueryCodeBatchSize > 0 ? navQueryCodeBatchSize : 200;
    }
}
```

- [ ] **Step 2: 在应用入口启用配置属性扫描**

```java
@SpringBootApplication(...)
@ConfigurationPropertiesScan
```

- [ ] **Step 3: 让 `CorrelationService` 注入配置类并替换写死常量**

```java
for (int offset = 0; offset < codes.size(); offset += properties.getSafeNavQueryCodeBatchSize()) {
    ...
}
```

- [ ] **Step 4: 在 `application.yml` 增加显式配置项**

```yml
trader:
  statistic:
    correlation:
      nav-query-code-batch-size: 200
```

- [ ] **Step 5: 运行指定测试确认通过**

Run: `./mvnw -pl trader-statistic -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=StatisticCorrelationPropertiesTest,CorrelationServiceTest test`
Expected: PASS

### Task 3: 模块回归与诊断检查

**Files:**
- Test: `trader-statistic/src/test/java/cc/riskswap/trader/statistic/task/CorrelationTaskTest.java`
- Test: `trader-statistic/src/test/java/cc/riskswap/trader/statistic/CorrelationBackendStructureTest.java`
- Test: `trader-statistic/src/test/java/cc/riskswap/trader/statistic/PackagingStructureTest.java`

- [ ] **Step 1: 运行 `statistic` 相关测试集合**

Run: `./mvnw -pl trader-statistic -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=StatisticCorrelationPropertiesTest,CorrelationServiceTest,CorrelationTaskTest,CorrelationBackendStructureTest,PackagingStructureTest test`
Expected: PASS

- [ ] **Step 2: 检查最近修改文件诊断**

Run: IDE diagnostics for:
- `StatisticCorrelationProperties.java`
- `Application.java`
- `CorrelationService.java`
- `application.yml`
- `StatisticCorrelationPropertiesTest.java`

Expected: no diagnostics

- [ ] **Step 3: 记录这轮结果**

```text
- nav query batch size is configurable
- default behavior remains 200
- service no longer depends on hard-coded batch size
```
