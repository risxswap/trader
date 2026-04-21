# Correlation Batch Compute Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将相关性全量统计从“逐对查数、逐条保存”改为“批量预取 NAV、内存两两计算、分批保存”，在不改变统计口径和 append-only 语义的前提下显著降低数据库往返次数。

**Architecture:** 在 `trader-base` 中补充 `FundNavDao` 的批量查询能力，在 `trader-statistic` 中为 `CorrelationService` 增加批量计算主流程，并将 `CorrelationTask` 改为单次批量调用。统计结果继续写入 `correlation` 表的新版本，任务收尾阶段仍调用既有历史清理逻辑，保证行为兼容。

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Apache Commons Math, ClickHouse, JUnit 5, Mockito

---

### Task 1: 为批量相关性流程补充失败测试

**Files:**
- Create: `trader-statistic/src/test/java/cc/riskswap/trader/statistic/service/CorrelationServiceTest.java`
- Create: `trader-statistic/src/test/java/cc/riskswap/trader/statistic/task/CorrelationTaskTest.java`

- [ ] **Step 1: 写服务层失败测试，约束批量流程会批量读 NAV、过滤无效结果并分批保存**

```java
@Test
void shouldBatchLoadNavsAndFlushMatchingCorrelations() {
    // mock fundNavDao.listByCodesAndStartTime(...)
    // mock correlationDao.saveBatch(...)
    // verify only significant pairs are persisted
}
```

- [ ] **Step 2: 写服务层失败测试，约束尾批结果会被 flush**

```java
@Test
void shouldFlushRemainingCorrelationsWhenBufferNotFull() {
    // prepare one valid pair
    // verify saveBatch called once with remaining buffer
}
```

- [ ] **Step 3: 写任务层失败测试，约束任务入口改为单次批量调用而不是逐对提交**

```java
@Test
void shouldCallBatchCalculationAndCleanupOnce() {
    // verify correlationService.calculateAndSaveBatch(uniqueFunds, "1Y")
    // verify correlationService.cleanupHistoricalCorrelations()
}
```

- [ ] **Step 4: 运行测试确认失败**

Run: `./mvnw -pl trader-statistic -am -Dtest=CorrelationServiceTest,CorrelationTaskTest test`
Expected: FAIL，因为批量计算入口、批量 NAV 查询和任务调用方式尚未实现

### Task 2: 在 `FundNavDao` 增加批量查询能力

**Files:**
- Modify: `trader-base/src/main/java/cc/riskswap/trader/base/dao/FundNavDao.java`
- Test: `trader-statistic/src/test/java/cc/riskswap/trader/statistic/service/CorrelationServiceTest.java`

- [ ] **Step 1: 为 DAO 增加 `listByCodesAndStartTime(List<String> codes, OffsetDateTime startTime)`**

```java
public List<FundNav> listByCodesAndStartTime(List<String> codes, OffsetDateTime startTime) {
    LambdaQueryWrapper<FundNav> wrapper = new LambdaQueryWrapper<>();
    wrapper.in(FundNav::getCode, codes);
    wrapper.ge(FundNav::getTime, startTime);
    wrapper.orderByAsc(FundNav::getCode, FundNav::getTime);
    return this.list(wrapper);
}
```

- [ ] **Step 2: 为空集合输入增加快速返回，避免生成无意义 SQL**

```java
if (codes == null || codes.isEmpty()) {
    return Collections.emptyList();
}
```

- [ ] **Step 3: 运行服务测试，确认新 DAO 能力被批量流程使用**

Run: `./mvnw -pl trader-statistic -am -Dtest=CorrelationServiceTest test`
Expected: 仍然 FAIL，但失败点前移到服务层尚未实现批量主流程

### Task 3: 在 `CorrelationService` 实现批量计算与批量保存

**Files:**
- Modify: `trader-statistic/src/main/java/cc/riskswap/trader/statistic/service/CorrelationService.java`
- Test: `trader-statistic/src/test/java/cc/riskswap/trader/statistic/service/CorrelationServiceTest.java`

- [ ] **Step 1: 新增批量常量和主入口 `calculateAndSaveBatch(List<Fund> funds, String period)`**

```java
public int calculateAndSaveBatch(List<Fund> funds, String period) {
    OffsetDateTime startTime = getStartTimeByPeriod(period);
    // dedupe funds -> load navs -> compute -> flush
}
```

- [ ] **Step 2: 提取 NAV 预加载和分组逻辑**

```java
private Map<String, Map<LocalDate, BigDecimal>> loadNavSeriesByCode(List<String> codes, OffsetDateTime startTime) {
    // group by code
    // keep only adjNav != null
}
```

- [ ] **Step 3: 提取单对资产的内存计算逻辑，复用现有 Pearson 和 p-value 公式**

```java
private Correlation buildCorrelationIfEligible(
        Fund fund1, Fund fund2, String period,
        Map<LocalDate, BigDecimal> series1,
        Map<LocalDate, BigDecimal> series2) {
    // align dates
    // compute coefficient and p-value
    // return null when pair does not pass filters
}
```

- [ ] **Step 4: 增加批量缓冲与 flush 逻辑**

```java
private int flushCorrelations(List<Correlation> buffer) {
    if (buffer.isEmpty()) {
        return 0;
    }
    correlationDao.saveBatch(new ArrayList<>(buffer), CORRELATION_SAVE_BATCH_SIZE);
    int flushed = buffer.size();
    buffer.clear();
    return flushed;
}
```

- [ ] **Step 5: 保留单对接口，但让 `calculateAndSave(...)` 复用批量内部逻辑或保持兼容实现**

```java
public void calculateAndSave(String asset1, String asset2, String period) {
    // either delegate through shared helper or keep as compatibility wrapper
}
```

- [ ] **Step 6: 运行服务测试确认通过**

Run: `./mvnw -pl trader-statistic -am -Dtest=CorrelationServiceTest test`
Expected: PASS

### Task 4: 将 `CorrelationTask` 改为单次批量执行

**Files:**
- Modify: `trader-statistic/src/main/java/cc/riskswap/trader/statistic/task/CorrelationTask.java`
- Test: `trader-statistic/src/test/java/cc/riskswap/trader/statistic/task/CorrelationTaskTest.java`

- [ ] **Step 1: 移除逐对 `CompletableFuture` 提交逻辑和 `correlationExecutor` 依赖**

```java
correlationService.calculateAndSaveBatch(uniqueFunds, "1Y");
```

- [ ] **Step 2: 保留基金去重逻辑，并在批量计算完成后调用历史清理**

```java
int count = correlationService.calculateAndSaveBatch(uniqueFunds, "1Y");
int deletedCount = correlationService.cleanupHistoricalCorrelations();
```

- [ ] **Step 3: 调整日志，输出参与基金数、生成记录数、清理条数和总耗时**

```java
log.info("Calculated {} correlations for {} funds and cleaned {} historical records in {} ms",
        savedCount, uniqueFunds.size(), deletedCount, elapsed);
```

- [ ] **Step 4: 运行任务测试确认通过**

Run: `./mvnw -pl trader-statistic -am -Dtest=CorrelationTaskTest test`
Expected: PASS

### Task 5: 做模块回归与诊断检查

**Files:**
- Modify: `trader-base/src/main/java/cc/riskswap/trader/base/dao/FundNavDao.java`
- Modify: `trader-statistic/src/main/java/cc/riskswap/trader/statistic/service/CorrelationService.java`
- Modify: `trader-statistic/src/main/java/cc/riskswap/trader/statistic/task/CorrelationTask.java`
- Test: `trader-statistic/src/test/java/cc/riskswap/trader/statistic/service/CorrelationServiceTest.java`
- Test: `trader-statistic/src/test/java/cc/riskswap/trader/statistic/task/CorrelationTaskTest.java`

- [ ] **Step 1: 运行目标测试集**

Run: `./mvnw -pl trader-statistic -am -Dtest=CorrelationServiceTest,CorrelationTaskTest test`
Expected: PASS

- [ ] **Step 2: 运行模块级测试，确认没有结构性回归**

Run: `./mvnw -pl trader-statistic -am test`
Expected: PASS

- [ ] **Step 3: 检查最近修改文件的 IDE 诊断**

Run: IDE diagnostics for `FundNavDao.java`, `CorrelationService.java`, `CorrelationTask.java`, `CorrelationServiceTest.java`, `CorrelationTaskTest.java`
Expected: no diagnostics

- [ ] **Step 4: 记录验证结果并准备交付**

```text
- batch NAV query added
- batch correlation compute/save verified
- task flow switched to single batch execution
```
