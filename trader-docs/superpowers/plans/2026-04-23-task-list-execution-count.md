# Task List Execution Count Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add cumulative execution-count statistics to the task management list so each task row shows how many execution-history records it has produced.

**Architecture:** Keep the existing `/task/list` entry point and enrich its DTOs with `executionCount`, computed in one backend batch aggregation for the current page of system tasks. Reuse the current `taskCode -> task_log.task_group` mapping, default investment-derived rows to `0`, and keep the admin web change limited to rendering one new centered table column.

**Tech Stack:** Spring Boot, MyBatis-Plus, Vue 3, Element Plus, TypeScript, Maven, JUnit 5, Mockito

---

## File Map

### 需要修改

- `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/dto/SystemTaskDto.java`
  - 新增 `executionCount` 字段承载累计执行次数
- `trader-base/src/main/java/cc/riskswap/trader/base/dao/TaskLogDao.java`
  - 增加按 `taskGroup` 批量统计执行次数的方法
- `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java`
  - 在任务列表查询后为系统任务回填 `executionCount`
- `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/SystemTaskServiceTest.java`
  - 增加执行次数统计与投资任务默认值测试
- `trader-admin/admin-web/src/services/systemTask.ts`
  - 前端 DTO 增加 `executionCount`
- `trader-admin/admin-web/src/pages/task/Manage.vue`
  - 新增 `执行次数` 列并展示返回值

## Task 1: Lock Backend Execution-count Behavior With Failing Tests

**Files:**
- Modify: `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/SystemTaskServiceTest.java`

- [ ] **Step 1: Add a failing test proving system tasks expose aggregated execution counts**

```java
Mockito.when(taskLogDao.countByTaskGroups(List.of("fundSync"))).thenReturn(Map.of("fundSync", 3L));

PageDto<SystemTaskDto> result = systemTaskService.list(query);

Assertions.assertEquals(3L, result.getItems().get(0).getExecutionCount());
```

- [ ] **Step 2: Add a failing test proving tasks without logs default to zero**

```java
Mockito.when(taskLogDao.countByTaskGroups(List.of("fundSync"))).thenReturn(Map.of());
Assertions.assertEquals(0L, result.getItems().get(0).getExecutionCount());
```

- [ ] **Step 3: Add a failing test proving investment rows default to zero without log aggregation**

```java
Assertions.assertEquals("INVESTMENT", result.getItems().get(0).getSourceType());
Assertions.assertEquals(0L, result.getItems().get(0).getExecutionCount());
Mockito.verifyNoInteractions(taskLogDao);
```

- [ ] **Step 4: Run the focused backend test class to verify it fails**

Run: `./mvnw -pl trader-admin/admin-server -am -Dtest=SystemTaskServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: FAIL because `SystemTaskDto` does not have `executionCount`, `SystemTaskService` does not populate it, and `TaskLogDao` has no batch-count method.

- [ ] **Step 5: Commit**

```bash
git add trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/SystemTaskServiceTest.java
git commit -m "test: cover task execution count aggregation"
```

## Task 2: Implement Backend Batch Aggregation For Execution Counts

**Files:**
- Modify: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/dto/SystemTaskDto.java`
- Modify: `trader-base/src/main/java/cc/riskswap/trader/base/dao/TaskLogDao.java`
- Modify: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java`
- Modify: `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/SystemTaskServiceTest.java`

- [ ] **Step 1: Add `executionCount` to the task list DTO**

```java
private Long executionCount;
```

- [ ] **Step 2: Add a batch-count helper to `TaskLogDao`**

```java
public Map<String, Long> countByTaskGroups(List<String> taskGroups) {
    if (taskGroups == null || taskGroups.isEmpty()) {
        return Map.of();
    }
    return lambdaQuery()
            .in(TaskLog::getTaskGroup, taskGroups)
            .list()
            .stream()
            .collect(Collectors.groupingBy(TaskLog::getTaskGroup, Collectors.counting()));
}
```

- [ ] **Step 3: Keep aggregation scoped to the current page of system tasks**

```java
List<String> taskCodes = items.stream()
        .filter(item -> "SYSTEM".equals(item.getSourceType()))
        .map(SystemTaskDto::getTaskCode)
        .filter(StrUtil::isNotBlank)
        .toList();
Map<String, Long> counts = taskLogDao.countByTaskGroups(taskCodes);
items.forEach(item -> item.setExecutionCount(resolveExecutionCount(item, counts)));
```

- [ ] **Step 4: Default investment rows and missing counts to zero**

```java
private long resolveExecutionCount(SystemTaskDto item, Map<String, Long> counts) {
    if (!"SYSTEM".equals(item.getSourceType())) {
        return 0L;
    }
    return counts.getOrDefault(item.getTaskCode(), 0L);
}
```

- [ ] **Step 5: Run the focused backend test class**

Run: `./mvnw -pl trader-admin/admin-server -am -Dtest=SystemTaskServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/dto/SystemTaskDto.java trader-base/src/main/java/cc/riskswap/trader/base/dao/TaskLogDao.java trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/SystemTaskServiceTest.java
git commit -m "feat: add task execution counts to list api"
```

## Task 3: Expose Execution Count In Admin Web Types

**Files:**
- Modify: `trader-admin/admin-web/src/services/systemTask.ts`

- [ ] **Step 1: Add `executionCount` to the frontend task DTO**

```ts
executionCount?: number
```

- [ ] **Step 2: Keep the service layer otherwise unchanged**

```ts
export const listSystemTasks = async (payload: SystemTaskQuery): Promise<ResData<PageRes<SystemTaskDto>>> => {
  const res = await http.post('/task/list', payload)
  return res.data as ResData<PageRes<SystemTaskDto>>
}
```

- [ ] **Step 3: Run diagnostics for the touched service file**

Use: diagnostics for `trader-admin/admin-web/src/services/systemTask.ts`
Expected: no TypeScript errors

- [ ] **Step 4: Commit**

```bash
git add trader-admin/admin-web/src/services/systemTask.ts
git commit -m "feat: expose execution count in task dto"
```

## Task 4: Render Execution Count In Task Management Table

**Files:**
- Modify: `trader-admin/admin-web/src/pages/task/Manage.vue`
- Modify: `trader-admin/admin-web/src/services/systemTask.ts`

- [ ] **Step 1: Add the new `执行次数` column after `执行结果`**

```vue
<el-table-column label="执行次数" width="100" align="center">
  <template #default="{ row }">
    {{ row.executionCount ?? 0 }}
  </template>
</el-table-column>
```

- [ ] **Step 2: Keep the list-loading path unchanged so the new value flows from the existing API**

```ts
tableData.value = res.data?.items || []
```

- [ ] **Step 3: Verify the new column does not crowd the operations area**

```vue
<el-table-column label="操作" width="280" fixed="right" align="center">
```

- [ ] **Step 4: Run diagnostics for the page**

Use: diagnostics for `trader-admin/admin-web/src/pages/task/Manage.vue`
Expected: no TypeScript or template errors

- [ ] **Step 5: Commit**

```bash
git add trader-admin/admin-web/src/pages/task/Manage.vue trader-admin/admin-web/src/services/systemTask.ts
git commit -m "feat: show execution count in task list"
```

## Task 5: Verify Regression Surface

**Files:**
- Modify: none

- [ ] **Step 1: Run the focused backend regression tests**

Run: `./mvnw -pl trader-admin/admin-server -am -Dtest=SystemTaskServiceTest,TaskLogServiceTest,TaskLogControllerRouteTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: PASS

- [ ] **Step 2: Run the admin-web production build**

Run: `npm run build`
Directory: `/Users/ming/Workspace/trader/trader-admin/admin-web`
Expected: PASS with no TypeScript build errors

- [ ] **Step 3: Collect diagnostics for touched frontend files**

Use: diagnostics for:
- `trader-admin/admin-web/src/services/systemTask.ts`
- `trader-admin/admin-web/src/pages/task/Manage.vue`

Expected: no new diagnostics

- [ ] **Step 4: Perform manual verification in the UI**

Check:
- 任务管理页新增 `执行次数` 列
- 有历史日志的任务显示累计数字
- 无历史日志的任务显示 `0`
- 策略任务显示 `0`
- 现有编辑、执行、删除、详情按钮布局未错位

- [ ] **Step 5: Commit**

```bash
git add trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/dto/SystemTaskDto.java trader-base/src/main/java/cc/riskswap/trader/base/dao/TaskLogDao.java trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/SystemTaskServiceTest.java trader-admin/admin-web/src/services/systemTask.ts trader-admin/admin-web/src/pages/task/Manage.vue
git commit -m "feat: add task execution counts to task list"
```
