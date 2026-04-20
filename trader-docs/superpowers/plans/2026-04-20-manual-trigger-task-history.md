# Manual Trigger Task History Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Record task execution history for both manual and scheduled task runs by moving `task_log` writes into `TraderTaskExecutor` and preventing duplicate writes from the existing logging aspect.

**Architecture:** Make `TraderTaskExecutor` the single execution-history writer by injecting `TaskLogStore`, generating one trace ID per run, and writing start/finish log records around `task.execute(context)`. Preserve `@TraderTaskLog` compatibility by introducing a lightweight executor-managed logging context so `TraderTaskLogAspect` skips `task_log` persistence when the executor has already taken ownership.

**Tech Stack:** Spring Boot, Redis, Quartz, MyBatis-Plus, Maven, JUnit 5, Mockito

---

## File Map

### 需要新增

- `trader-base/src/main/java/cc/riskswap/trader/base/logging/TaskLogExecutionContext.java`
  - 线程级标记，表示当前任务执行历史已经由执行器接管

### 需要修改

- `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskExecutor.java`
  - 注入 `TaskLogStore`
  - 在执行器中统一记录开始/结束历史
  - 生成并使用统一 `traceId`
- `trader-base/src/main/java/cc/riskswap/trader/base/logging/TraderTaskLogAspect.java`
  - 检查执行器上下文标记并跳过重复日志写入
- `trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfiguration.java`
  - 为 `TraderTaskExecutor` 注入 `TaskLogStore`
- `trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskExecutorTest.java`
  - 增加手动/定时执行写历史测试
- `trader-base/src/test/java/cc/riskswap/trader/base/logging/TraderTaskLogAspectTest.java`
  - 增加执行器接管日志时切面跳过写入测试

### Task 1: Lock Executor-side Task-history Behavior With Failing Tests

**Files:**
- Modify: `trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskExecutorTest.java`

- [ ] **Step 1: Add a failing test proving executor writes running and success logs for a triggered task**

```java
TaskLogStore taskLogStore = mock(TaskLogStore.class);
TraderTaskExecutor executor = new TraderTaskExecutor(registry, redisTemplate, traderTaskLock, statusStore, taskLogStore);

executor.execute("COLLECTOR", "fundSync", 1710000000L);

verify(taskLogStore).writeRunning(eq("同步基金"), eq("fundSync"), any(), anyString());
verify(taskLogStore).writeFinished(anyString(), eq("SUCCESS"), anyLong(), contains("triggerType=SCHEDULED"));
```

- [ ] **Step 2: Add a failing test proving executor writes failed history for exceptions**

```java
assertThrows(IllegalStateException.class, () -> executor.execute("COLLECTOR", "fundSync", 1710000000L));
verify(taskLogStore).writeFinished(anyString(), eq("FAILED"), anyLong(), contains("IllegalStateException"));
```

- [ ] **Step 3: Run the focused executor test class to verify it fails because executor does not yet write task history**

Run: `./mvnw -pl trader-base -Dtest=TraderTaskExecutorTest test`
Expected: FAIL because the constructor and log writes do not exist yet.

- [ ] **Step 4: Implement the minimal executor test setup changes only**

```java
TaskLogStore taskLogStore = mock(TaskLogStore.class);
TraderTaskExecutor executor = new TraderTaskExecutor(registry, redisTemplate, traderTaskLock, statusStore, taskLogStore);
```

- [ ] **Step 5: Re-run the focused executor test class to keep the new behavior red and isolated**

Run: `./mvnw -pl trader-base -Dtest=TraderTaskExecutorTest test`
Expected: FAIL only on missing history-writing behavior.

- [ ] **Step 6: Commit**

```bash
git add trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskExecutorTest.java
git commit -m "test: cover executor task history logging"
```

### Task 2: Implement Executor-owned Task History Logging

**Files:**
- Create: `trader-base/src/main/java/cc/riskswap/trader/base/logging/TaskLogExecutionContext.java`
- Modify: `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskExecutor.java`
- Modify: `trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfiguration.java`

- [ ] **Step 1: Add the minimal executor logging context**

```java
public final class TaskLogExecutionContext {
    private static final ThreadLocal<Boolean> EXECUTOR_MANAGED = ThreadLocal.withInitial(() -> false);
    public static void markExecutorManaged(boolean managed) { EXECUTOR_MANAGED.set(managed); }
    public static boolean isExecutorManaged() { return Boolean.TRUE.equals(EXECUTOR_MANAGED.get()); }
    public static void clear() { EXECUTOR_MANAGED.remove(); }
}
```

- [ ] **Step 2: Inject `TaskLogStore` into `TraderTaskExecutor`**

```java
private final TaskLogStore taskLogStore;

public TraderTaskExecutor(..., SystemTaskStatusStore statusStore, TaskLogStore taskLogStore) {
    this.taskLogStore = taskLogStore;
}
```

- [ ] **Step 3: Generate one `traceId`, write the running log, and mark executor-managed scope**

```java
String traceId = UUID.randomUUID().toString();
TaskLogExecutionContext.markExecutorManaged(true);
taskLogStore.writeRunning(instance.getTaskName(), instance.getTaskCode(), LocalDateTime.now(), traceId);
```

- [ ] **Step 4: Write success/failure completion logs from executor**

```java
taskLogStore.writeFinished(traceId, "SUCCESS", executionMs, buildRemark(context, "SUCCESS", null));
taskLogStore.writeFinished(traceId, "FAILED", executionMs, buildRemark(context, "FAILED", e));
```

- [ ] **Step 5: Always clear executor-managed context in `finally`**

```java
finally {
    TaskLogExecutionContext.clear();
}
```

- [ ] **Step 6: Update auto-configuration so executor receives `TaskLogStore`**

```java
public TraderTaskExecutor traderTaskExecutor(..., SystemTaskStatusStore systemTaskStatusStore, TaskLogStore taskLogStore) {
    return new TraderTaskExecutor(registry, stringRedisTemplate, lock, systemTaskStatusStore, taskLogStore);
}
```

- [ ] **Step 7: Re-run focused executor tests**

Run: `./mvnw -pl trader-base -Dtest=TraderTaskExecutorTest test`
Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add trader-base/src/main/java/cc/riskswap/trader/base/logging/TaskLogExecutionContext.java trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskExecutor.java trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfiguration.java trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskExecutorTest.java
git commit -m "feat: log task history from executor"
```

### Task 3: Prevent Duplicate History Writes From `TraderTaskLogAspect`

**Files:**
- Modify: `trader-base/src/main/java/cc/riskswap/trader/base/logging/TraderTaskLogAspect.java`
- Modify: `trader-base/src/test/java/cc/riskswap/trader/base/logging/TraderTaskLogAspectTest.java`

- [ ] **Step 1: Add a failing aspect test proving executor-managed runs skip aspect persistence**

```java
TaskLogExecutionContext.markExecutorManaged(true);
ReflectionTestUtils.invokeMethod(aspect, "persistRunningLog", "同步基金", "fundSync", startTime, "trace-1");
verifyNoInteractions(taskLogStore);
TaskLogExecutionContext.clear();
```

- [ ] **Step 2: Run the focused aspect test class to verify it fails before the guard exists**

Run: `./mvnw -pl trader-base -Dtest=TraderTaskLogAspectTest test`
Expected: FAIL because the aspect still writes through to `TaskLogStore`.

- [ ] **Step 3: Add a single guard in each persistence path**

```java
if (TaskLogExecutionContext.isExecutorManaged()) {
    return;
}
```

- [ ] **Step 4: Re-run the focused aspect test class**

Run: `./mvnw -pl trader-base -Dtest=TraderTaskLogAspectTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add trader-base/src/main/java/cc/riskswap/trader/base/logging/TraderTaskLogAspect.java trader-base/src/test/java/cc/riskswap/trader/base/logging/TraderTaskLogAspectTest.java
git commit -m "fix: avoid duplicate task history logs"
```

### Task 4: Final Verification For Manual-trigger History

**Files:**
- Modify: none
- Verify: `trader-docs/superpowers/specs/2026-04-20-manual-trigger-task-history-design.md`

- [ ] **Step 1: Run all focused task-history tests**

Run: `./mvnw -pl trader-base -Dtest=TraderTaskExecutorTest,TraderTaskLogAspectTest,TaskLogStoreTest test`
Expected: PASS

- [ ] **Step 2: Run module compile verification**

Run: `./mvnw -pl trader-base,trader-admin/admin-server -DskipTests compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Verify no admin-side task-log API changes were required**

Run:

```bash
git diff -- trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/controller/TaskLogController.java trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/TaskLogService.java trader-admin/admin-web/src/pages/task/Detail.vue
```

Expected: no changes, proving the existing history UI can consume the new records unchanged.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "test: verify manual task trigger history logging"
```

