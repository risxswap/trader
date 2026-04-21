# Correlation Task Schema Param Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为相关性统计任务增加基于 JSON Schema 的 `minAbsCorrelation` 配置，并让任务执行与管理端表单都使用该参数。

**Architecture:** `trader-statistic` 侧新增一个小型参数对象负责解析 `paramsJson` 并提供默认值、范围校验和兜底日志，`CorrelationTask` 把解析后的阈值传给 `CorrelationService`。`trader-admin` 前端只实现最小 schema 渲染子集，支持 `number` 字段转为 `el-input-number`，不支持的 schema 继续回退到原始 `paramsJson` 文本框。

**Tech Stack:** Java, Spring Boot, Hutool JSON, Vue 3, TypeScript, Element Plus, JUnit 5, Mockito

---

### Task 1: 锁定统计侧任务参数行为

**Files:**
- Create: `/Users/ming/Workspace/trader/trader-statistic/src/main/java/cc/riskswap/trader/statistic/task/CorrelationTaskParams.java`
- Modify: `/Users/ming/Workspace/trader/trader-statistic/src/main/java/cc/riskswap/trader/statistic/task/CorrelationTask.java`
- Test: `/Users/ming/Workspace/trader/trader-statistic/src/test/java/cc/riskswap/trader/statistic/task/CorrelationTaskTest.java`

- [ ] **Step 1: 先写失败测试，约束任务定义输出 schema 和默认参数**

```java
@Test
void should_expose_schema_and_default_params_for_min_abs_correlation() {
    CorrelationTask task = new CorrelationTask();

    Assertions.assertTrue(task.getParamSchema().contains("minAbsCorrelation"));
    Assertions.assertTrue(task.getDefaultParams().contains("\"minAbsCorrelation\":0.5"));
}
```

- [ ] **Step 2: 再写失败测试，约束空参数与非法参数会回退默认值**

```java
@Test
void should_fallback_to_default_threshold_when_params_are_blank_or_invalid() {
    Assertions.assertEquals(0.5d, CorrelationTaskParams.fromJson(null).minAbsCorrelation());
    Assertions.assertEquals(0.5d, CorrelationTaskParams.fromJson("{\"minAbsCorrelation\":2}").minAbsCorrelation());
}
```

- [ ] **Step 3: 再写失败测试，约束任务执行把阈值传给 service**

```java
Mockito.verify(correlationService).calculateAndSaveBatch(Mockito.anyList(), Mockito.eq("1Y"), Mockito.eq(0.8d));
```

- [ ] **Step 4: 运行测试确认红灯**

Run: `./mvnw -pl trader-statistic -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=CorrelationTaskTest test`
Expected: FAIL，因为还没有参数对象、schema 内容和新方法签名

- [ ] **Step 5: 最小实现 `CorrelationTaskParams`**

```java
public record CorrelationTaskParams(double minAbsCorrelation) {
    public static final double DEFAULT_MIN_ABS_CORRELATION = 0.5d;
    public static CorrelationTaskParams fromJson(String json) { ... }
}
```

- [ ] **Step 6: 最小实现 `CorrelationTask`**

```java
@Override
public String getParamSchema() { return "...minAbsCorrelation..."; }

@Override
public String getDefaultParams() { return "{\"minAbsCorrelation\":0.5}"; }

public void execute(TraderTaskContext context) {
    CorrelationTaskParams params = CorrelationTaskParams.fromJson(context.getParamsJson());
    doCalculateAllCorrelations(params.minAbsCorrelation());
}
```

- [ ] **Step 7: 重跑测试确认转绿**

Run: `./mvnw -pl trader-statistic -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=CorrelationTaskTest test`
Expected: PASS

### Task 2: 锁定服务层按阈值过滤

**Files:**
- Modify: `/Users/ming/Workspace/trader/trader-statistic/src/main/java/cc/riskswap/trader/statistic/service/CorrelationService.java`
- Test: `/Users/ming/Workspace/trader/trader-statistic/src/test/java/cc/riskswap/trader/statistic/service/CorrelationServiceTest.java`

- [ ] **Step 1: 写失败测试，约束低阈值时结果可保存**

```java
@Test
void should_save_when_abs_correlation_exceeds_configured_threshold() {
    int saved = service.calculateAndSaveBatch(funds, "1Y", 0.4d);
    Assertions.assertEquals(1, saved);
}
```

- [ ] **Step 2: 写失败测试，约束高阈值时结果会被过滤**

```java
@Test
void should_filter_when_abs_correlation_does_not_exceed_configured_threshold() {
    int saved = service.calculateAndSaveBatch(funds, "1Y", 0.99d);
    Assertions.assertEquals(0, saved);
}
```

- [ ] **Step 3: 保留兼容测试，默认阈值 `0.5` 仍可通过旧行为**

```java
@Test
void should_keep_default_threshold_compatible() {
    CorrelationStats stats = invokeHelper(..., 0.5d);
    Assertions.assertNotNull(stats);
}
```

- [ ] **Step 4: 运行测试确认红灯**

Run: `./mvnw -pl trader-statistic -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=CorrelationServiceTest test`
Expected: FAIL，因为批量入口和统计 helper 还没有阈值参数

- [ ] **Step 5: 最小实现服务签名与过滤逻辑**

```java
public int calculateAndSaveBatch(List<Fund> funds, String period, double minAbsCorrelation) { ... }

private CorrelationStats calculateCorrelationStats(
        Map<LocalDate, BigDecimal> series1,
        Map<LocalDate, BigDecimal> series2,
        double minAbsCorrelation) { ... }
```

- [ ] **Step 6: 在日志中补充本次任务阈值**

```java
log.info("Start correlation batch calculation... minAbsCorrelation={}", minAbsCorrelation);
```

- [ ] **Step 7: 重跑测试确认转绿**

Run: `./mvnw -pl trader-statistic -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=CorrelationServiceTest,CorrelationTaskTest test`
Expected: PASS

### Task 3: 接入管理端最小 schema 渲染

**Files:**
- Modify: `/Users/ming/Workspace/trader/trader-admin/admin-web/src/pages/task/Manage.vue`
- Modify: `/Users/ming/Workspace/trader/trader-admin/admin-web/src/services/systemTask.ts`

- [ ] **Step 1: 先补最小类型定义**

```ts
export interface JsonSchemaNumberProperty {
  type: 'number'
  title?: string
  description?: string
  default?: number
  minimum?: number
  maximum?: number
}
```

- [ ] **Step 2: 在 `Manage.vue` 增加 schema 解析辅助函数**

```ts
const parseTaskSchema = (schema?: string) => { ... }
const parseParamsJson = (value?: string) => { ... }
const stringifyParamsJson = (value: Record<string, unknown>) => JSON.stringify(value)
```

- [ ] **Step 3: 在创建任务区域先写最小渲染逻辑**

```vue
<el-form-item v-if="createSchemaMode === 'number'" :label="field.title">
  <el-input-number v-model="createSchemaForm.minAbsCorrelation" :min="field.minimum" :max="field.maximum" />
</el-form-item>
```

- [ ] **Step 4: 在编辑任务区域复用同一渲染逻辑**

```vue
<el-form-item v-if="editSchemaMode === 'number'" ... />
```

- [ ] **Step 5: 提交前统一把表单值回写到 `paramsJson`**

```ts
createForm.paramsJson = buildParamsJsonFromSchemaForm(...)
editForm.paramsJson = buildParamsJsonFromSchemaForm(...)
```

- [ ] **Step 6: 遇到不支持 schema 时继续显示原始 JSON 文本框**

```ts
const shouldUseRawJsonEditor = computed(() => !supportedSchema.value)
```

### Task 4: 做最小前端/后端回归

**Files:**
- Test: `/Users/ming/Workspace/trader/trader-statistic/src/test/java/cc/riskswap/trader/statistic/task/CorrelationTaskTest.java`
- Test: `/Users/ming/Workspace/trader/trader-statistic/src/test/java/cc/riskswap/trader/statistic/service/CorrelationServiceTest.java`
- Modify: `/Users/ming/Workspace/trader/trader-admin/admin-web/src/pages/task/Manage.vue`

- [ ] **Step 1: 跑统计模块聚焦测试**

Run: `./mvnw -pl trader-statistic -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=CorrelationTaskTest,CorrelationServiceTest test`
Expected: PASS

- [ ] **Step 2: 跑统计模块回归测试**

Run: `./mvnw -pl trader-statistic -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=StatisticCorrelationPropertiesTest,TraderTransactionConfigTest,CorrelationServiceTest,CorrelationTaskTest,CorrelationBackendStructureTest,PackagingStructureTest test`
Expected: PASS

- [ ] **Step 3: 做前端静态检查或构建验证**

Run: `npm run build`
cwd: `/Users/ming/Workspace/trader/trader-admin/admin-web`
Expected: BUILD SUCCESS 或 Vite build 通过

- [ ] **Step 4: 检查诊断并收尾**

Check:
- `CorrelationTaskParams.java`
- `CorrelationTask.java`
- `CorrelationService.java`
- `CorrelationTaskTest.java`
- `CorrelationServiceTest.java`
- `systemTask.ts`
- `Manage.vue`

Expected: 无新增错误诊断
