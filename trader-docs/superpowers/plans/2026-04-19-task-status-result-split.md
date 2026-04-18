# Task Status Result Split Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split task runtime status from task execution result so tasks use `STOPPED/RUNNING` for current state and `SUCCESS/FAILED` for the latest execution outcome.

**Architecture:** Keep the existing `system_task.status` field for runtime state and add a new `result` field for the latest execution result. Propagate the new field through database schema, entities, stream events, executor status publishing, and admin-side stream consumption so task lifecycle transitions remain event-driven.

**Tech Stack:** Spring Boot, MyBatis-Plus, Redis Stream, Quartz, Maven, JUnit 5, Mockito

---

### Task 1: Lock Schema Expectations For `system_task.result`

**Files:**
- Modify: `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/UpgradeServiceTest.java`
- Test: `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/UpgradeServiceTest.java`

- [ ] **Step 1: Write the failing schema assertions**

```java
Assertions.assertTrue(initScript.contains("result VARCHAR(16)"));
Assertions.assertTrue(upgradeScript.contains("ADD COLUMN IF NOT EXISTS result"));
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -pl trader-admin/admin-server -Dtest=UpgradeServiceTest#shouldKeepSystemTaskResultSchemaAligned test`
Expected: FAIL because `mysql.sql` and the new upgrade script do not yet contain `system_task.result`.

- [ ] **Step 3: Add a focused classpath assertion for the new upgrade version**

```java
Assertions.assertTrue(Arrays.stream(mysqlResources)
        .anyMatch(resource -> "1.0.4.sql".equals(resource.getFilename())));
```

- [ ] **Step 4: Re-run the focused test to keep it red until schema files are updated**

Run: `./mvnw -pl trader-admin/admin-server -Dtest=UpgradeServiceTest#shouldKeepSystemTaskResultSchemaAligned,UpgradeServiceTest#shouldLoadSeparatedUpgradeScriptsFromClasspath test`
Expected: still FAIL, now clearly showing the missing upgrade file and column definition.

- [ ] **Step 5: Commit**

```bash
git add trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/UpgradeServiceTest.java
git commit -m "test: lock system task result schema"
```

### Task 2: Add Database Support For Runtime Result

**Files:**
- Modify: `trader-admin/admin-server/src/main/resources/db/mysql.sql`
- Create: `trader-admin/admin-server/src/main/resources/db/upgrade/mysql/1.0.4.sql`
- Test: `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/UpgradeServiceTest.java`

- [ ] **Step 1: Update initial schema**

```sql
result VARCHAR(16) DEFAULT NULL COMMENT '最近一次执行结果',
INDEX idx_system_task_result (result)
```

- [ ] **Step 2: Add an upgrade script for existing MySQL databases**

```sql
ALTER TABLE system_task
    ADD COLUMN IF NOT EXISTS result VARCHAR(16) DEFAULT NULL COMMENT '最近一次执行结果' AFTER status;

CREATE INDEX idx_system_task_result ON system_task (result);
```

- [ ] **Step 3: Run the focused upgrade test to verify it now passes**

Run: `./mvnw -pl trader-admin/admin-server -Dtest=UpgradeServiceTest#shouldKeepSystemTaskResultSchemaAligned,UpgradeServiceTest#shouldLoadSeparatedUpgradeScriptsFromClasspath test`
Expected: PASS

- [ ] **Step 4: Run the full upgrade test class**

Run: `./mvnw -pl trader-admin/admin-server -Dtest=UpgradeServiceTest test`
Expected: PASS with all upgrade-related checks green.

- [ ] **Step 5: Commit**

```bash
git add trader-admin/admin-server/src/main/resources/db/mysql.sql trader-admin/admin-server/src/main/resources/db/upgrade/mysql/1.0.4.sql trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/UpgradeServiceTest.java
git commit -m "feat: add system task result schema"
```

### Task 3: Propagate `result` Through Admin Models And Stream Consumer

**Files:**
- Modify: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/dao/base/entity/SystemTask.java`
- Modify: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/dao/entity/SystemTask.java`
- Modify: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/dao/base/dto/SystemTaskDto.java`
- Modify: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/dao/dto/SystemTaskDto.java`
- Modify: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java`
- Modify: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/stream/TraderStreamConsumer.java`
- Test: `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/SystemTaskServiceTest.java`
- Test: `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/stream/TraderStreamConsumerTest.java`

- [ ] **Step 1: Add `result` to admin entities and DTOs**

```java
private String result;
```

- [ ] **Step 2: Ensure task list / create / update flows preserve the new field**

```java
dto.setResult(task.getResult());
task.setResult(param.getResult());
```

- [ ] **Step 3: Write a failing consumer test for `SYSTEM_TASK_STATUS`**

```java
assertEquals("STOPPED", savedTask.getStatus());
assertEquals("SUCCESS", savedTask.getResult());
```

- [ ] **Step 4: Update the stream consumer to persist both state and result**

```java
task.setStatus(event.getStatus());
task.setResult(event.getResult());
systemTaskDao.updateById(task);
```

- [ ] **Step 5: Run the focused tests**

Run: `./mvnw -pl trader-admin/admin-server -Dtest=SystemTaskServiceTest,TraderStreamConsumerTest test`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/dao/base/entity/SystemTask.java trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/dao/entity/SystemTask.java trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/dao/base/dto/SystemTaskDto.java trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/dao/dto/SystemTaskDto.java trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/stream/TraderStreamConsumer.java trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/SystemTaskServiceTest.java trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/stream/TraderStreamConsumerTest.java
git commit -m "feat: persist system task execution result"
```

### Task 4: Extend Base Event Model With `result`

**Files:**
- Modify: `trader-base/src/main/java/cc/riskswap/trader/base/event/SystemTaskStatusEvent.java`
- Test: `trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskExecutorTest.java`

- [ ] **Step 1: Add the event field**

```java
private String result;
```

- [ ] **Step 2: Write failing executor assertions for the new lifecycle**

```java
assertEquals("RUNNING", firstEvent.getStatus());
assertNull(firstEvent.getResult());
assertEquals("STOPPED", secondEvent.getStatus());
assertEquals("SUCCESS", secondEvent.getResult());
```

- [ ] **Step 3: Verify the new test fails before executor changes**

Run: `./mvnw -pl trader-base -Dtest=TraderTaskExecutorTest test`
Expected: FAIL because executor currently emits `RUNNING` and `IDLE`, without a `result`.

- [ ] **Step 4: Keep the event model minimal and backward-compatible**

```java
@Data
public class SystemTaskStatusEvent {
    private String result;
}
```

- [ ] **Step 5: Commit**

```bash
git add trader-base/src/main/java/cc/riskswap/trader/base/event/SystemTaskStatusEvent.java trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskExecutorTest.java
git commit -m "test: lock task status result event model"
```

### Task 5: Implement Runtime Status And Result Publishing

**Files:**
- Modify: `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskExecutor.java`
- Test: `trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskExecutorTest.java`
- Test: `trader-base/src/test/java/cc/riskswap/trader/base/task/TraderQuartzJobTest.java`

- [ ] **Step 1: Change executor lifecycle publishing**

```java
sendStatus(instance, "RUNNING", null);
task.execute(context);
sendStatus(instance, "STOPPED", "SUCCESS");
```

- [ ] **Step 2: Map failures to `STOPPED + FAILED`**

```java
catch (Exception e) {
    sendStatus(instance, "STOPPED", "FAILED");
    throw e;
}
```

- [ ] **Step 3: Remove the old `IDLE` / `ERROR` behavior**

```java
private void sendStatus(SystemTaskStatusEvent instance, String status, String result) {
    instance.setStatus(status);
    instance.setResult(result);
    streamPublisher.publish("SYSTEM_TASK_STATUS", instance);
}
```

- [ ] **Step 4: Re-run base tests**

Run: `./mvnw -pl trader-base -Dtest=TraderTaskExecutorTest,TraderQuartzJobTest,TraderTaskRefreshSubscriberTest,TraderTaskSchedulerServiceTest,TraderTaskAutoConfigurationTest,TraderTaskRedisListenerContainerTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskExecutor.java trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskExecutorTest.java trader-base/src/test/java/cc/riskswap/trader/base/task/TraderQuartzJobTest.java
git commit -m "feat: publish task runtime result lifecycle"
```

### Task 6: End-to-End Verification

**Files:**
- Modify: none
- Verify: `trader-admin/admin-server/src/main/resources/db/upgrade/mysql/1.0.4.sql`

- [ ] **Step 1: Run admin-server targeted tests**

Run: `./mvnw -pl trader-admin/admin-server -Dtest=UpgradeServiceTest,SystemTaskServiceTest,TraderStreamConsumerTest test`
Expected: PASS

- [ ] **Step 2: Run base targeted tests**

Run: `./mvnw -pl trader-base -Dtest=TraderTaskExecutorTest,TraderQuartzJobTest,TraderTaskRefreshSubscriberTest,TraderTaskSchedulerServiceTest,TraderTaskAutoConfigurationTest,TraderTaskRedisListenerContainerTest test`
Expected: PASS

- [ ] **Step 3: Restart `admin-server` so `1.0.4.sql` executes**

Run:

```bash
# restart admin-server locally
```

Expected: startup completes without schema errors and `system_task.result` exists.

- [ ] **Step 4: Trigger a task and verify state/result split**

Run:

```bash
python3 -c 'import json,time,urllib.request; headers={"Content-Type":"application/json"}; post=lambda p,b: json.loads(urllib.request.urlopen(urllib.request.Request("http://127.0.0.1:8080"+p,data=json.dumps(b).encode(),headers=headers,method="POST"),timeout=10).read().decode()); print(post("/task/trigger",{"id":1})); time.sleep(2); print(post("/task/list",{"pageNo":1,"pageSize":20,"taskType":"COLLECTOR"}))'
```

Expected:
- during execution `status=RUNNING`
- after success `status=STOPPED`, `result=SUCCESS`
- after failure `status=STOPPED`, `result=FAILED`

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "test: verify task status and result split"
```
