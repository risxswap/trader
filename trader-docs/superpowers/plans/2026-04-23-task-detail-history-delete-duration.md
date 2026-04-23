# Task Detail History Delete And Duration Formatting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add single-record execution-history deletion in the task detail page and convert raw execution milliseconds into readable duration text.

**Architecture:** Extend the existing task-log backend instead of introducing a second history data path: add one delete endpoint on `TaskLogController`, validate existence in `TaskLogService`, and delete by primary key through the existing `TaskLogDao`. In the admin web, keep the current `task/detail` page structure, add a delete action with confirm-and-refresh behavior, and extract duration formatting into a small shared utility so task detail and the global task-log page can use the same display rules.

**Tech Stack:** Spring Boot, MyBatis-Plus, Vue 3, Element Plus, TypeScript, Vite, Maven, JUnit 5, Mockito

---

## File Map

### 需要新增

- `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/param/TaskLogDeleteParam.java`
  - 任务日志删除接口入参，只包含日志 `id`
- `trader-admin/admin-web/src/utils/taskLog.ts`
  - 放置任务日志相关的纯前端展示工具，先承载执行耗时格式化方法

### 需要修改

- `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/controller/TaskLogController.java`
  - 新增删除路由
- `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/TaskLogService.java`
  - 新增删除业务逻辑，记录不存在时返回明确错误
- `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/controller/TaskLogControllerRouteTest.java`
  - 补充删除路由断言
- `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/TaskLogServiceTest.java`
  - 增加删除成功/失败测试
- `trader-admin/admin-web/src/services/taskLog.ts`
  - 增加删除接口方法
- `trader-admin/admin-web/src/pages/task/Detail.vue`
  - 增加删除按钮、确认弹窗调用、页码回退和耗时格式化展示
- `trader-admin/admin-web/src/pages/logs/TaskList.vue`
  - 复用新的耗时格式化工具，统一历史耗时口径

## Task 1: Lock Backend Delete Behavior With Failing Tests

**Files:**
- Modify: `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/TaskLogServiceTest.java`
- Modify: `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/controller/TaskLogControllerRouteTest.java`

- [ ] **Step 1: Add a failing test proving task logs can be deleted by id**

```java
taskLogService.delete(1L);
Mockito.verify(taskLogDao).removeById(1L);
```

- [ ] **Step 2: Add a failing test proving deleting a missing log raises a clear error**

```java
Mockito.when(taskLogDao.getById(99L)).thenReturn(null);
Warning warning = Assertions.assertThrows(Warning.class, () -> taskLogService.delete(99L));
Assertions.assertEquals("执行历史不存在", warning.getMessage());
```

- [ ] **Step 3: Add a failing controller route assertion for the delete endpoint**

```java
boolean hasDeleteRoute = Arrays.stream(methods)
        .anyMatch(method -> hasPostMapping(method, "/delete"));
Assertions.assertTrue(hasDeleteRoute);
```

- [ ] **Step 4: Run the focused backend tests to verify they fail**

Run: `./mvnw -pl trader-admin/admin-server -Dtest=TaskLogServiceTest,TaskLogControllerRouteTest test`
Expected: FAIL because `TaskLogService` has no delete method and `TaskLogController` has no delete route yet.

- [ ] **Step 5: Commit**

```bash
git add trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/TaskLogServiceTest.java trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/controller/TaskLogControllerRouteTest.java
git commit -m "test: cover task log delete behavior"
```

## Task 2: Implement Backend Task-log Delete API

**Files:**
- Create: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/param/TaskLogDeleteParam.java`
- Modify: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/controller/TaskLogController.java`
- Modify: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/TaskLogService.java`
- Modify: `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/TaskLogServiceTest.java`
- Modify: `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/controller/TaskLogControllerRouteTest.java`

- [ ] **Step 1: Add the delete request DTO**

```java
@Data
public class TaskLogDeleteParam {
    private Long id;
}
```

- [ ] **Step 2: Implement delete validation in `TaskLogService`**

```java
public void delete(Long id) {
    TaskLog log = taskLogDao.getById(id);
    if (log == null) {
        throw new Warning(ErrorCode.RESOURCE_NOT_FOUND.code(), "执行历史不存在");
    }
    taskLogDao.removeById(id);
}
```

- [ ] **Step 3: Expose the controller endpoint**

```java
@PostMapping("/delete")
public ResData<Void> delete(@RequestBody TaskLogDeleteParam param) {
    taskLogService.delete(param.getId());
    return ResData.success();
}
```

- [ ] **Step 4: Run the focused backend tests**

Run: `./mvnw -pl trader-admin/admin-server -Dtest=TaskLogServiceTest,TaskLogControllerRouteTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/param/TaskLogDeleteParam.java trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/controller/TaskLogController.java trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/TaskLogService.java trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/TaskLogServiceTest.java trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/controller/TaskLogControllerRouteTest.java
git commit -m "feat: add task log delete api"
```

## Task 3: Lock Duration-formatting Rules In Frontend Utility

**Files:**
- Create: `trader-admin/admin-web/src/utils/taskLog.ts`

- [ ] **Step 1: Write the formatting examples directly in the utility as temporary assertions**

```ts
console.assert(formatExecutionDuration(850) === '850 ms')
console.assert(formatExecutionDuration(65_000) === '1分5秒')
console.assert(formatExecutionDuration(3_600_000) === '1时0分0秒')
console.assert(formatExecutionDuration(undefined) === '-')
```

- [ ] **Step 2: Implement the minimal formatter**

```ts
export const formatExecutionDuration = (executionMs?: number | null) => {
  if (executionMs === undefined || executionMs === null || executionMs < 0) return '-'
  if (executionMs < 1000) return `${executionMs} ms`
  const totalSeconds = Math.floor(executionMs / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  if (hours > 0) return `${hours}时${minutes}分${seconds}秒`
  return `${minutes}分${seconds}秒`
}
```

- [ ] **Step 3: Remove the temporary assertions and keep the exported helper only**

```ts
export { formatExecutionDuration }
```

- [ ] **Step 4: Run TypeScript diagnostics for the new utility**

Use: diagnostics for `trader-admin/admin-web/src/utils/taskLog.ts`
Expected: no TypeScript errors

- [ ] **Step 5: Commit**

```bash
git add trader-admin/admin-web/src/utils/taskLog.ts
git commit -m "feat: add task log duration formatter"
```

## Task 4: Extend Task-log Service Layer In Admin Web

**Files:**
- Modify: `trader-admin/admin-web/src/services/taskLog.ts`
- Test: `trader-admin/admin-web/src/services/taskLog.ts`

- [ ] **Step 1: Add the delete request type**

```ts
export type TaskLogDeleteParam = {
  id: number
}
```

- [ ] **Step 2: Add the delete API wrapper**

```ts
export const deleteTaskLog = async (payload: TaskLogDeleteParam): Promise<ResData<void>> => {
  const res = await http.post('/logs/task/delete', payload)
  return res.data as ResData<void>
}
```

- [ ] **Step 3: Run diagnostics for the touched service file**

Use: diagnostics for `trader-admin/admin-web/src/services/taskLog.ts`
Expected: no TypeScript errors

- [ ] **Step 4: Commit**

```bash
git add trader-admin/admin-web/src/services/taskLog.ts
git commit -m "feat: add task log delete web service"
```

## Task 5: Add Delete Interaction To Task Detail Page

**Files:**
- Modify: `trader-admin/admin-web/src/pages/task/Detail.vue`
- Modify: `trader-admin/admin-web/src/services/taskLog.ts`
- Modify: `trader-admin/admin-web/src/utils/taskLog.ts`

- [ ] **Step 1: Import the delete service and duration formatter into the page**

```ts
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteTaskLog, getTaskLogDetail, listTaskLogs } from '../../services/taskLog'
import { formatExecutionDuration } from '../../utils/taskLog'
```

- [ ] **Step 2: Replace raw execution milliseconds in the table**

```vue
<template #default="{ row }">{{ formatExecutionDuration(row.executionMs) }}</template>
```

- [ ] **Step 3: Add the delete button next to detail**

```vue
<el-button link type="danger" @click="handleDelete(row)">删除</el-button>
```

- [ ] **Step 4: Implement confirm-delete-refresh logic with page fallback**

```ts
const handleDelete = async (row: TaskLogDto) => {
  await ElMessageBox.confirm('删除后不可恢复，确认删除该执行历史吗？', '删除确认', { type: 'warning' })
  const res = await deleteTaskLog({ id: row.id })
  if (res.code !== 200) throw new Error(res.message || '删除执行历史失败')
  const isLastRowOnPage = logs.value.length === 1 && query.pageNo > 1
  if (isLastRowOnPage) query.pageNo -= 1
  ElMessage.success('删除成功')
  await loadLogs()
}
```

- [ ] **Step 5: Keep cancellation silent and only toast real failures**

```ts
if (error === 'cancel' || error === 'close') return
ElMessage.error(error.message || '删除执行历史失败')
```

- [ ] **Step 6: Run diagnostics for the detail page**

Use: diagnostics for `trader-admin/admin-web/src/pages/task/Detail.vue`
Expected: no TypeScript or template errors

- [ ] **Step 7: Commit**

```bash
git add trader-admin/admin-web/src/pages/task/Detail.vue trader-admin/admin-web/src/services/taskLog.ts trader-admin/admin-web/src/utils/taskLog.ts
git commit -m "feat: add delete action to task detail history"
```

## Task 6: Reuse The Duration Formatter In Global Task Logs

**Files:**
- Modify: `trader-admin/admin-web/src/pages/logs/TaskList.vue`
- Modify: `trader-admin/admin-web/src/utils/taskLog.ts`

- [ ] **Step 1: Import the shared formatter**

```ts
import { formatExecutionDuration } from '../../utils/taskLog'
```

- [ ] **Step 2: Replace the page-local helper with the shared one**

```ts
const getExecutionText = (executionMs?: number) => formatExecutionDuration(executionMs)
```

- [ ] **Step 3: Keep existing copy stable in the page where needed**

```vue
<div class="primary-cell">{{ getExecutionText(row.executionMs) }}</div>
```

- [ ] **Step 4: Run diagnostics for the global log page**

Use: diagnostics for `trader-admin/admin-web/src/pages/logs/TaskList.vue`
Expected: no TypeScript or template errors

- [ ] **Step 5: Commit**

```bash
git add trader-admin/admin-web/src/pages/logs/TaskList.vue trader-admin/admin-web/src/utils/taskLog.ts
git commit -m "refactor: reuse task log duration formatter"
```

## Task 7: Verify End-to-end Build And Regression Surface

**Files:**
- Modify: none

- [ ] **Step 1: Run the focused backend tests again as a final regression check**

Run: `./mvnw -pl trader-admin/admin-server -Dtest=TaskLogServiceTest,TaskLogControllerRouteTest,SystemTaskServiceTest test`
Expected: PASS

- [ ] **Step 2: Run the admin-web production build**

Run: `npm run build`
Directory: `/Users/ming/Workspace/trader/trader-admin/admin-web`
Expected: PASS with no TypeScript build errors

- [ ] **Step 3: Collect diagnostics for touched frontend files**

Use: diagnostics for:
- `trader-admin/admin-web/src/services/taskLog.ts`
- `trader-admin/admin-web/src/utils/taskLog.ts`
- `trader-admin/admin-web/src/pages/task/Detail.vue`
- `trader-admin/admin-web/src/pages/logs/TaskList.vue`

Expected: no new diagnostics

- [ ] **Step 4: Perform manual verification in the UI**

Check:
- A task detail history row shows `850 ms` for sub-second runs
- A task detail history row shows `1分5秒` for `65000`
- A task detail history row shows `1时0分0秒` for `3600000`
- Deleting a non-final row refreshes the current page
- Deleting the final row on a non-first page falls back one page and refreshes

- [ ] **Step 5: Commit**

```bash
git add trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/param/TaskLogDeleteParam.java trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/controller/TaskLogController.java trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/TaskLogService.java trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/TaskLogServiceTest.java trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/controller/TaskLogControllerRouteTest.java trader-admin/admin-web/src/services/taskLog.ts trader-admin/admin-web/src/utils/taskLog.ts trader-admin/admin-web/src/pages/task/Detail.vue trader-admin/admin-web/src/pages/logs/TaskList.vue
git commit -m "feat: support task detail history deletion"
```
