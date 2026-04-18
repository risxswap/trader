# Task Log History Detail Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix task log completion updates and add a dedicated task detail page that shows task metadata plus execution history.

**Architecture:** Keep task execution log updates event-driven through `TASK_LOG` stream events, but complete the DAO/consumer path so one trace updates one `task_log` row from `RUNNING` to `SUCCESS` or `FAILED`. Add a dedicated task detail route in the admin web that reuses existing task and task-log APIs instead of inventing a second history backend.

**Tech Stack:** Spring Boot, MyBatis-Plus, Redis Stream, Vue 3, Element Plus, TypeScript, Maven, JUnit 5, Mockito

---

### Task 1: Lock Task Log Update Semantics With Failing Tests

**Files:**
- Modify: `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/stream/TraderStreamConsumerTest.java`
- Test: `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/stream/TraderStreamConsumerTest.java`

- [ ] **Step 1: Add a failing test for `RUNNING` log creation**

```java
Mockito.verify(taskLogDao).createRunningLog(
        "同步基金",
        "fundSync",
        "trace-1",
        "RUNNING"
);
```

- [ ] **Step 2: Add a failing test for `SUCCESS` log update by `traceId`**

```java
Mockito.verify(taskLogDao).updateLogByTraceId("trace-1", "SUCCESS", 1200L, "done");
```

- [ ] **Step 3: Add a failing test for `FAILED` log update by `traceId`**

```java
Mockito.verify(taskLogDao).updateLogByTraceId("trace-1", "FAILED", 1200L, "boom");
```

- [ ] **Step 4: Run the focused test class to verify it fails**

Run: `./mvnw -pl trader-admin/admin-server -Dtest=TraderStreamConsumerTest test`
Expected: FAIL because the DAO contract and consumer logic do not yet fully support trace-based completion updates.

- [ ] **Step 5: Commit**

```bash
git add trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/stream/TraderStreamConsumerTest.java
git commit -m "test: lock task log trace update behavior"
```

### Task 2: Implement Task Log DAO And Consumer Completion Updates

**Files:**
- Modify: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/dao/base/TaskLogDao.java`
- Modify: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/dao/base/mapper/TaskLogMapper.java`
- Modify: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/stream/TraderStreamConsumer.java`
- Modify: `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/stream/TraderStreamConsumerTest.java`

- [ ] **Step 1: Add explicit DAO methods for trace-based creation/update**

```java
void createRunningLog(String taskName, String taskGroup, String traceId, LocalDateTime startTime, String status);
void updateLogByTraceId(String traceId, String status, Long costMs, String remark);
```

- [ ] **Step 2: Implement the mapper SQL needed by those DAO methods**

```java
@Insert("INSERT INTO task_log (...) VALUES (...)")
@Update("UPDATE task_log SET status = #{status}, end_time = NOW(6), execution_ms = #{costMs}, remark = #{remark} WHERE trace_id = #{traceId}")
```

- [ ] **Step 3: Update the stream consumer to use the normalized status values**

```java
if ("RUNNING".equals(event.getStatus())) { ... }
else if ("SUCCESS".equals(event.getStatus())) { ... }
else if ("FAILED".equals(event.getStatus())) { ... }
```

- [ ] **Step 4: Run the focused consumer test class**

Run: `./mvnw -pl trader-admin/admin-server -Dtest=TraderStreamConsumerTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/dao/base/TaskLogDao.java trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/dao/base/mapper/TaskLogMapper.java trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/stream/TraderStreamConsumer.java trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/stream/TraderStreamConsumerTest.java
git commit -m "feat: complete task log trace update flow"
```

### Task 3: Expose Task History Query Inputs For Per-Task Detail

**Files:**
- Modify: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/query/TaskLogQuery.java`
- Modify: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/TaskLogService.java`
- Modify: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/controller/TaskLogController.java`
- Test: `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/TaskLogServiceTest.java`
- Test: `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/controller/TaskLogControllerTest.java`

- [ ] **Step 1: Add failing tests for filtering logs by task identity**

```java
query.setTaskCode("fundSync");
query.setTaskName("同步基金");
assertEquals(1, result.getItems().size());
```

- [ ] **Step 2: Run the focused tests to verify they fail**

Run: `./mvnw -pl trader-admin/admin-server -Dtest=TaskLogServiceTest,TaskLogControllerTest test`
Expected: FAIL because the query object/service does not yet support the new filters.

- [ ] **Step 3: Add the minimal query fields and service filtering**

```java
private String taskCode;
private String taskName;
```

- [ ] **Step 4: Keep the existing endpoint shape and just enrich the query semantics**

```java
public ResData<PageDto<TaskLogDto>> list(TaskLogQuery query) {
    return ResData.success(taskLogService.list(query));
}
```

- [ ] **Step 5: Re-run the focused tests**

Run: `./mvnw -pl trader-admin/admin-server -Dtest=TaskLogServiceTest,TaskLogControllerTest test`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/query/TaskLogQuery.java trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/TaskLogService.java trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/controller/TaskLogController.java trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/TaskLogServiceTest.java trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/controller/TaskLogControllerTest.java
git commit -m "feat: support per-task log history queries"
```

### Task 4: Add Task Detail API Coverage

**Files:**
- Modify: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/controller/SystemTaskController.java`
- Modify: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java`
- Test: `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/SystemTaskServiceTest.java`
- Test: `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/controller/SystemTaskControllerTest.java`

- [ ] **Step 1: Verify whether existing task detail support is enough**

```java
SystemTaskDto detail = systemTaskService.getById(1L);
assertEquals("fundSync", detail.getTaskCode());
```

- [ ] **Step 2: Write a failing test if there is no detail method/endpoint yet**

Run: `./mvnw -pl trader-admin/admin-server -Dtest=SystemTaskServiceTest,SystemTaskControllerTest test`
Expected: FAIL only if task detail access is missing or incomplete.

- [ ] **Step 3: Implement the minimal task detail read path**

```java
@GetMapping("/{id}")
public ResData<SystemTaskDto> get(@PathVariable Long id) { ... }
```

- [ ] **Step 4: Re-run the focused tests**

Run: `./mvnw -pl trader-admin/admin-server -Dtest=SystemTaskServiceTest,SystemTaskControllerTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/controller/SystemTaskController.java trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/SystemTaskServiceTest.java trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/controller/SystemTaskControllerTest.java
git commit -m "feat: expose system task detail endpoint"
```

### Task 5: Add Task Detail Services In Admin Web

**Files:**
- Modify: `trader-admin/admin-web/src/services/systemTask.ts`
- Modify: `trader-admin/admin-web/src/services/taskLog.ts`
- Test: `trader-admin/admin-web/src/services/systemTask.ts`
- Test: `trader-admin/admin-web/src/services/taskLog.ts`

- [ ] **Step 1: Add the service methods and types needed by the detail page**

```ts
export const getSystemTaskDetail = async (id: number) => http.get(`/task/${id}`)
export type TaskLogQuery = { taskCode?: string; taskName?: string; ... }
```

- [ ] **Step 2: Keep the service layer thin and aligned to backend DTOs**

```ts
result?: string
traceId?: string
```

- [ ] **Step 3: Run project diagnostics for the touched TS files**

Use: diagnostics for `systemTask.ts` and `taskLog.ts`
Expected: no TypeScript errors

- [ ] **Step 4: Commit**

```bash
git add trader-admin/admin-web/src/services/systemTask.ts trader-admin/admin-web/src/services/taskLog.ts
git commit -m "feat: add task detail web services"
```

### Task 6: Add Dedicated Task Detail Route And Page

**Files:**
- Modify: `trader-admin/admin-web/src/router/index.ts`
- Modify: `trader-admin/admin-web/src/pages/task/Manage.vue`
- Create: `trader-admin/admin-web/src/pages/task/Detail.vue`
- Test: `trader-admin/admin-web/src/pages/task/Manage.vue`
- Test: `trader-admin/admin-web/src/pages/task/Detail.vue`

- [ ] **Step 1: Add a failing interaction expectation for task detail navigation**

```ts
router.push({ path: '/task/detail', query: { id: row.id } })
```

- [ ] **Step 2: Add the detail button to the manage page**

```vue
<el-button link type="primary" @click="goToDetail(row)">详情</el-button>
```

- [ ] **Step 3: Create the dedicated detail page**

```vue
<el-descriptions title="任务详情">...</el-descriptions>
<el-table :data="logs">...</el-table>
```

- [ ] **Step 4: Reuse the existing log detail interaction**

```vue
<el-drawer v-model="detailVisible">...</el-drawer>
```

- [ ] **Step 5: Run diagnostics on the touched Vue/TS files**

Use: diagnostics for `Manage.vue`, `Detail.vue`, `router/index.ts`
Expected: no frontend diagnostics

- [ ] **Step 6: Commit**

```bash
git add trader-admin/admin-web/src/router/index.ts trader-admin/admin-web/src/pages/task/Manage.vue trader-admin/admin-web/src/pages/task/Detail.vue
git commit -m "feat: add task execution history detail page"
```

### Task 7: Final Verification

**Files:**
- Modify: none
- Verify: existing task/log routes and stream updates

- [ ] **Step 1: Run targeted admin-server tests**

Run: `./mvnw -pl trader-admin/admin-server -Dtest=UpgradeServiceTest,SystemTaskServiceTest,TraderStreamConsumerTest,TaskLogServiceTest,TaskLogControllerTest,SystemTaskControllerTest test`
Expected: PASS

- [ ] **Step 2: Run frontend diagnostics for the task pages**

Use: diagnostics for `trader-admin/admin-web/src/pages/task/Manage.vue`, `trader-admin/admin-web/src/pages/task/Detail.vue`, `trader-admin/admin-web/src/services/systemTask.ts`, `trader-admin/admin-web/src/services/taskLog.ts`
Expected: no diagnostics

- [ ] **Step 3: Restart local services and trigger one task**

Run:

```bash
# restart admin-server and collector locally
```

Expected: task executes and emits `TASK_LOG` events with the same `traceId`.

- [ ] **Step 4: Verify the log row is updated, not duplicated**

Run:

```bash
python3 -c 'import urllib.request; print(urllib.request.urlopen("http://127.0.0.1:8080/logs/task?pageNo=1&pageSize=10&taskCode=fundSync").read().decode())'
```

Expected: the latest row ends in `SUCCESS` or `FAILED`, with cost/remark populated for the same trace.

- [ ] **Step 5: Open the task detail page and confirm history visibility**

Expected:
- task metadata loads
- history list filters to the selected task
- log detail drawer shows the selected execution

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "test: verify task history detail flow"
```
