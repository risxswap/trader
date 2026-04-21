# Task Definition Redis Schema Publish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ensure task definitions published to Redis at application startup always include `paramSchema` and `defaultParamsJson`, so the task creation page can render schema-driven parameter forms.

**Architecture:** Keep the existing startup publish flow as the single source of truth: task implementations expose schema/default params, `TraderTaskDefinitionPublisher` serializes them into Redis, and admin-server reads those Redis definitions unchanged. Add focused tests at the task, publisher, and admin-server read/create layers so future regressions are caught where they happen.

**Tech Stack:** Java 21, Spring Boot, Hutool JSON, Redis, JUnit 5, Mockito

---

## File Map

- Modify: `/Users/ming/Workspace/trader/trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskDefinitionPublisherTest.java`
  - Strengthen Redis publish assertions to require `paramSchema` and `defaultParamsJson`.
- Modify: `/Users/ming/Workspace/trader/trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/SystemTaskServiceTest.java`
  - Ensure admin-server preserves Redis-carried schema/default params when creating instances from definitions.
- Verify: `/Users/ming/Workspace/trader/trader-statistic/src/test/java/cc/riskswap/trader/statistic/task/CorrelationTaskTest.java`
  - Keep the existing task-level schema/default params contract green.
- Verify only: `/Users/ming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskDefinitionPublisher.java`
  - No behavior redesign expected; only adjust if tests reveal a real gap.
- Verify only: `/Users/ming/Workspace/trader/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java`
  - No contract change expected; only adjust if tests reveal a drop in fields.

### Task 1: Lock Redis Publish Contract

**Files:**
- Modify: `/Users/ming/Workspace/trader/trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskDefinitionPublisherTest.java`
- Verify: `/Users/ming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskDefinitionPublisher.java`

- [ ] **Step 1: Write the failing test assertions**

Update `should_publish_definition_to_redis()` so the Redis payload must contain:

```java
Mockito.verify(ops).set(Mockito.eq("trader:task:def:COLLECTOR:fundSync"), Mockito.argThat(json -> {
    if (!JSONUtil.isTypeJSON(json)) {
        return false;
    }
    var obj = JSONUtil.parseObj(json);
    return "fundSync".equals(obj.getStr("taskCode"))
            && "{\"type\":\"object\"}".equals(obj.getStr("paramSchema"))
            && "{\"fullSync\":true}".equals(obj.getStr("defaultParamsJson"));
}));
```

- [ ] **Step 2: Run test to verify it fails if fields are not serialized**

Run: `./mvnw -pl trader-base -Dtest=TraderTaskDefinitionPublisherTest test`

Expected: If publisher ever stops serializing either field, this test fails at the `Mockito.verify(ops).set(...)` matcher.

- [ ] **Step 3: Adjust production code only if needed**

Expected minimal production code should remain:

```java
definition.setParamSchema(task.getParamSchema());
definition.setDefaultParamsJson(task.getDefaultParams());
stringRedisTemplate.opsForValue().set(key(taskType, task.getTaskCode()), JSONUtil.toJsonStr(definition));
```

If the test exposes a real gap, fix only the missing field propagation in `TraderTaskDefinitionPublisher`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -pl trader-base -Dtest=TraderTaskDefinitionPublisherTest test`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskDefinitionPublisherTest.java \
        trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskDefinitionPublisher.java
git commit -m "test: lock task definition schema publish contract"
```

### Task 2: Lock Admin-Server Redis Read/Create Behavior

**Files:**
- Modify: `/Users/ming/Workspace/trader/trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/SystemTaskServiceTest.java`
- Verify: `/Users/ming/Workspace/trader/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java`

- [ ] **Step 1: Write the failing test case**

Extend `should_create_instance_from_definition_and_publish_created_message()` so the mocked Redis definition includes schema/default params:

```java
String json = """
        {"taskType":"COLLECTOR","taskCode":"fundSync","taskName":"同步基金",
        "defaultCron":"0 0 1 * * ?","defaultEnabled":true,
        "paramSchema":"{\\"type\\":\\"object\\",\\"properties\\":{\\"fullSync\\":{\\"type\\":\\"boolean\\"}}",
        "defaultParamsJson":"{\\"fullSync\\":true}"}
        """;
```

Add assertions on the saved instance:

```java
Assertions.assertEquals(expectedSchema, savedTask.getParamSchema());
Assertions.assertEquals("{\"fullSync\":true}", savedTask.getDefaultParamsJson());
Assertions.assertEquals("{\"fullSync\":false}", savedTask.getParamsJson());
```

Also add one focused definition-list assertion if coverage is missing:

```java
Assertions.assertEquals(expectedSchema, definitions.getFirst().getParamSchema());
Assertions.assertEquals("{\"fullSync\":true}", definitions.getFirst().getDefaultParamsJson());
```

- [ ] **Step 2: Run test to verify it fails when fields are dropped**

Run: `./mvnw -pl trader-admin/admin-server -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SystemTaskServiceTest test`

Expected: FAIL if `SystemTaskService` drops schema/default params while reading Redis or creating instances.

- [ ] **Step 3: Write minimal implementation only if tests expose a gap**

Expected production behavior should remain:

```java
task.setParamSchema(def.getParamSchema());
task.setDefaultParamsJson(def.getDefaultParamsJson());
task.setParamsJson(StrUtil.isNotBlank(param.getParamsJson()) ? param.getParamsJson() : def.getDefaultParamsJson());
```

If the test fails, fix only the missing field assignment or DTO mapping.

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -pl trader-admin/admin-server -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SystemTaskServiceTest test`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/SystemTaskServiceTest.java \
        trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java
git commit -m "test: preserve task schema from redis definitions"
```

### Task 3: Verify Correlation Task Definition Content

**Files:**
- Verify: `/Users/ming/Workspace/trader/trader-statistic/src/test/java/cc/riskswap/trader/statistic/task/CorrelationTaskTest.java`
- Verify: `/Users/ming/Workspace/trader/trader-statistic/src/main/java/cc/riskswap/trader/statistic/task/CorrelationTask.java`

- [ ] **Step 1: Re-read existing test coverage**

Confirm the test still asserts:

```java
Assertions.assertTrue(task.getParamSchema().contains("minAbsCorrelation"));
Assertions.assertTrue(task.getDefaultParams().contains("\"minAbsCorrelation\":0.5"));
```

- [ ] **Step 2: Tighten assertions only if needed**

If coverage is too loose, strengthen it to also assert schema metadata that the frontend relies on:

```java
Assertions.assertTrue(task.getParamSchema().contains("\"type\":\"object\""));
Assertions.assertTrue(task.getParamSchema().contains("\"minimum\":0"));
Assertions.assertTrue(task.getParamSchema().contains("\"maximum\":1"));
```

- [ ] **Step 3: Run test to verify it passes**

Run: `./mvnw -pl trader-statistic -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=CorrelationTaskTest test`

Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add trader-statistic/src/test/java/cc/riskswap/trader/statistic/task/CorrelationTaskTest.java \
        trader-statistic/src/main/java/cc/riskswap/trader/statistic/task/CorrelationTask.java
git commit -m "test: lock correlation task schema definition"
```

### Task 4: Final Verification

**Files:**
- Verify only: all files touched in Tasks 1-3

- [ ] **Step 1: Run focused base verification**

Run: `./mvnw -pl trader-base -Dtest=TraderTaskDefinitionPublisherTest test`

Expected: PASS

- [ ] **Step 2: Run focused admin-server verification**

Run: `./mvnw -pl trader-admin/admin-server -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SystemTaskServiceTest test`

Expected: PASS

- [ ] **Step 3: Run focused statistic verification**

Run: `./mvnw -pl trader-statistic -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=CorrelationTaskTest test`

Expected: PASS

- [ ] **Step 4: Run diagnostics on edited files**

Check diagnostics for:

```text
/Users/ming/Workspace/trader/trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskDefinitionPublisherTest.java
/Users/ming/Workspace/trader/trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/SystemTaskServiceTest.java
/Users/ming/Workspace/trader/trader-statistic/src/test/java/cc/riskswap/trader/statistic/task/CorrelationTaskTest.java
```

Expected: no new diagnostics introduced by this work.

- [ ] **Step 5: Commit verification-only follow-up if needed**

```bash
git add <any final touched files>
git commit -m "test: verify task definition schema redis flow"
```
