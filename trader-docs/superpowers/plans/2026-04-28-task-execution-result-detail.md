# Task Execution Result Detail Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 任务执行结果不再只有 SUCCESS/FAILED，而是把每次执行的统计（synced/failed/message/errorDetail/traceId）落库到 `task_log`，并在任务管理/任务详情/任务日志页面展示。

**Architecture:** 采用方案 A：复用 `task_log.content` 存结构化 JSON，复用 `task_log.error_msg` 存失败简要。任务通过 `TraderTaskContext` 上报统计，执行器/切面在结束时写入 `task_log`。

**Tech Stack:** Java (Spring Boot, MyBatis-Plus, Hutool JSON), Vue 3 + Vite + Element Plus

---

## File Map (Planned)

**Backend (trader-base)**
- Modify: [TraderTaskContext.java](file:///Users/ming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskContext.java)
- Modify: [TraderTaskExecutor.java](file:///Users/ming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskExecutor.java)
- Modify: [TraderTaskLogAspect.java](file:///Users/ming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/logging/TraderTaskLogAspect.java)
- Modify: [TaskLogStore.java](file:///Users/ming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/logging/TaskLogStore.java)
- Modify: [TaskLogDao.java](file:///Users/ming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/TaskLogDao.java)
- Create: `trader-base/src/main/java/cc/riskswap/trader/base/task/TaskExecutionReport.java`
- Create: `trader-base/src/main/java/cc/riskswap/trader/base/task/TaskExecutionReportJson.java` (用于约束 content JSON 结构)
- Test: `trader-base/src/test/java/cc/riskswap/trader/base/task/TaskExecutionReportTest.java`
- Test: `trader-base/src/test/java/cc/riskswap/trader/base/dao/TaskLogDaoUpdateContentTest.java`（按现有测试风格选择是否新建）

**Backend (admin-server)**
- Modify: [SystemTaskDto.java](file:///Users/ming/Workspace/trader/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/dto/SystemTaskDto.java)
- Modify: [SystemTaskService.java](file:///Users/ming/Workspace/trader/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java)

**Frontend (admin-web)**
- Modify: [systemTask.ts](file:///Users/ming/Workspace/trader/trader-admin/admin-web/src/services/systemTask.ts)
- Modify: [Manage.vue](file:///Users/ming/Workspace/trader/trader-admin/admin-web/src/pages/task/Manage.vue)
- Modify: [Detail.vue](file:///Users/ming/Workspace/trader/trader-admin/admin-web/src/pages/task/Detail.vue)
- Modify: [TaskList.vue](file:///Users/ming/Workspace/trader/trader-admin/admin-web/src/pages/logs/TaskList.vue)
- Modify: [taskLog.ts](file:///Users/ming/Workspace/trader/trader-admin/admin-web/src/utils/taskLog.ts)

---

## JSON Contract (task_log.content)

`task_log.content` 存储 JSON 字符串（UTF-8），建议结构如下（字段缺失需兼容）：

```json
{
  "syncedCount": 560,
  "failedCount": 0,
  "message": "完成",
  "errorDetail": {
    "table": "orders",
    "lastId": 123456
  }
}
```

展示规则：
- 列表/详情优先展示 `message`
- 统计展示 `syncedCount` / `failedCount`
- `errorDetail` 仅在 FAILED 时展示，并在 UI 中可展开（不直接展示完整异常堆栈）

---

### Task 1: Define Task Execution Report Model (trader-base)

**Files:**
- Create: `trader-base/src/main/java/cc/riskswap/trader/base/task/TaskExecutionReport.java`
- Create: `trader-base/src/main/java/cc/riskswap/trader/base/task/TaskExecutionReportJson.java`
- Modify: [TraderTaskContext.java](file:///Users/ming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskContext.java)
- Test: `trader-base/src/test/java/cc/riskswap/trader/base/task/TaskExecutionReportTest.java`

- [ ] **Step 1: 写 TaskExecutionReport 的单测（先失败）**

```java
@Test
void should_accumulate_counts_and_message() {
    TaskExecutionReport report = new TaskExecutionReport();
    report.addSynced(10);
    report.addFailed(2);
    report.setMessage("running");
    assertEquals(10L, report.getSyncedCount());
    assertEquals(2L, report.getFailedCount());
    assertEquals("running", report.getMessage());
}
```

- [ ] **Step 2: 运行单测确认失败**

Run:

```bash
./mvnw -pl trader-base test -Dtest=TaskExecutionReportTest
```

Expected: FAIL（类不存在/方法不存在）

- [ ] **Step 3: 实现 TaskExecutionReport（最小实现通过测试）**

要点：
- 内部字段：`syncedCount`、`failedCount`、`message`、`errorDetail`（Map）
- 对外方法：`addSynced(long)`, `addFailed(long)`, `setMessage(String)`, `putErrorDetail(String,Object)`, `snapshot()`（返回 TaskExecutionReportJson 或 Map）

- [ ] **Step 4: 扩展 TraderTaskContext 以承载 report**

要点：
- 新增字段 `TaskExecutionReport report`，默认懒初始化
- 提供 `getReport()` / `report()`（返回非空）
- 不破坏现有任务：旧任务不改也能正常执行

- [ ] **Step 5: 运行 trader-base 单测**

Run:

```bash
./mvnw -pl trader-base test
```

Expected: PASS

---

### Task 2: Persist Detailed Result Into task_log (trader-base)

**Files:**
- Modify: [TaskLogDao.java](file:///Users/ming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/TaskLogDao.java)
- Modify: [TaskLogStore.java](file:///Users/ming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/logging/TaskLogStore.java)
- Modify: [TraderTaskExecutor.java](file:///Users/ming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskExecutor.java)
- Modify: [TraderTaskLogAspect.java](file:///Users/ming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/logging/TraderTaskLogAspect.java)
- Test: `trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskExecutorTest.java`（在现有用例基础上补充）

- [ ] **Step 1: 增强 TaskLogDao 的更新能力**

新增一个方法（不要破坏现有 `updateLogByTraceId` 调用方）：
- `updateLogByTraceId(traceId, status, costMs, remark, content, errorMsg)`

行为：
- 以 traceId 定位记录
- 设置：`status`, `execution_ms`, `remark`, `content`, `error_msg`, `end_time=now`

- [ ] **Step 2: 扩展 TaskLogStore.writeFinished 传递 content/errorMsg**

保持兼容：
- 保留旧签名（内部委托新签名）
- 新签名增加 `content` 与 `errorMsg`

- [ ] **Step 3: 修改 TraderTaskExecutor 写入统计 JSON**

实现要点：
- 任务执行结束时，从 `context.report()` 获取统计
- `task_log.content` 写入 `TaskExecutionReportJson` 的 JSON（Hutool `JSONUtil.toJsonStr`）
- `task_log.error_msg`：
  - SUCCESS：置空或不写（保持历史行为即可）
  - FAILED：写入简要（`ExceptionClass: message`，空 message 也要处理）
- 保留现有 `remark` 写入（如果 remark 当前用于检索/排查，不要删）

- [ ] **Step 4: 修改 TraderTaskLogAspect（非 executor 托管场景）**

策略（轻量兼容）：
- 在 `joinPoint.getArgs()` 里查找 `TraderTaskContext`，如存在则读取 report 并写入 `task_log.content/error_msg`
- 如不存在上下文，则保持现有行为（只写 remark）

- [ ] **Step 5: 为 executor / aspect 补充单测**

建议新增断言：
- SUCCESS 时 content 为合法 JSON，且包含 `syncedCount/failedCount/message`
- FAILED 时 errorMsg 非空，content 仍为合法 JSON（至少包含统计字段）

- [ ] **Step 6: 运行 trader-base 单测**

Run:

```bash
./mvnw -pl trader-base test
```

Expected: PASS

---

### Task 3: Expose Latest Execution Summary in Task APIs (admin-server)

**Files:**
- Modify: [SystemTaskDto.java](file:///Users/ming/Workspace/trader/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/dto/SystemTaskDto.java)
- Modify: [SystemTaskService.java](file:///Users/ming/Workspace/trader/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java)
- Modify: [TaskLogDao.java](file:///Users/ming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/TaskLogDao.java)（新增查询方法）

- [ ] **Step 1: 扩展 SystemTaskDto 返回字段**

新增字段（均可为空）：
- `lastTraceId`
- `lastSyncedCount`
- `lastFailedCount`
- `lastMessage`
- `lastErrorMsg`

- [ ] **Step 2: 在 TaskLogDao 增加“按 task_group 取最新日志”能力**

新增方法示例：
- `latestLogsByTaskGroups(List<String> taskGroups): Map<String, TaskLog>`

实现方式参考 `latestExecutionMsByTaskGroups`：
- 只 select 必要字段（taskGroup/traceId/content/errorMsg/executionMs/startTime/status）
- order by start_time desc
- 遍历取每个 taskGroup 第一条

- [ ] **Step 3: SystemTaskService.applyExecutionSummary 填充新增字段**

要点：
- 仍然保留 executionCount/lastExecutionMs 的逻辑
- 对每个 SYSTEM 任务：
  - 读取最新 log
  - `lastTraceId = log.traceId`
  - `lastErrorMsg = log.errorMsg`
  - parse `log.content` JSON，提取 syncedCount/failedCount/message（容错：空/非法 JSON）

- [ ] **Step 4: detail 接口也返回这些字段**

策略：
- 在 `getDetail` 返回前，复用同一套“取最新日志并 parse”的逻辑（避免代码重复）

- [ ] **Step 5: 编译验证**

Run:

```bash
./mvnw -pl trader-admin/admin-server -am test
```

Expected: PASS（至少能编译通过）

---

### Task 4: Update Admin Web Pages to Display Detailed Results (admin-web)

**Files:**
- Modify: [systemTask.ts](file:///Users/ming/Workspace/trader/trader-admin/admin-web/src/services/systemTask.ts)
- Modify: [Manage.vue](file:///Users/ming/Workspace/trader/trader-admin/admin-web/src/pages/task/Manage.vue)
- Modify: [Detail.vue](file:///Users/ming/Workspace/trader/trader-admin/admin-web/src/pages/task/Detail.vue)
- Modify: [TaskList.vue](file:///Users/ming/Workspace/trader/trader-admin/admin-web/src/pages/logs/TaskList.vue)
- Modify: [taskLog.ts](file:///Users/ming/Workspace/trader/trader-admin/admin-web/src/utils/taskLog.ts)

- [ ] **Step 1: 扩展前端 SystemTaskDto 类型**

在 `services/systemTask.ts` 增加可选字段：
- `lastTraceId?: string`
- `lastSyncedCount?: number`
- `lastFailedCount?: number`
- `lastMessage?: string`
- `lastErrorMsg?: string`

- [ ] **Step 2: 增加解析 task_log.content 的工具函数**

在 `utils/taskLog.ts` 增加：
- `parseTaskLogContent(content?: string)`：返回 `{ syncedCount?: number; failedCount?: number; message?: string; errorDetail?: any }`

行为：
- try/catch `JSON.parse`
- 非 JSON 则返回空对象（兼容历史数据）

- [ ] **Step 3: 任务管理列表页展示“最近一次统计/消息”**

在 `pages/task/Manage.vue` 新增两列：
- “最近同步”：`synced/failed`（例如 `560/0`，没有则 `-`）
- “最近消息”：优先 `row.lastMessage`，失败时兜底 `row.lastErrorMsg`

展示建议：
- 对长文本使用 `secondary-cell--clamp` + tooltip（沿用现有风格）

- [ ] **Step 4: 任务详情页展示“最近一次执行摘要”**

在 `pages/task/Detail.vue` 的“基础信息”下新增一块摘要（KPI 风格）：
- `lastTraceId`、`lastSyncedCount`、`lastFailedCount`、`lastExecutionMs`、`lastMessage`
- FAILED 时显示 `lastErrorMsg`，并在日志抽屉里对 `errorDetail` 做可展开展示（利用 `parseTaskLogContent`）

- [ ] **Step 5: 任务日志页增加 stats 展示**

在 `pages/logs/TaskList.vue`：
- 列表中新增一列展示 `synced/failed/message`（从 `row.content` parse）
- 详情抽屉里增加同样的 stats 展示（避免用户只看到长 content）

- [ ] **Step 6: 前端构建验证**

Run:

```bash
npm -C trader-admin/admin-web run build
```

Expected: build success

---

### Task 5: Manual Verification Checklist

- [ ] 在任一同步类任务中调用 `context.report().addSynced(...) / addFailed(...) / setMessage(...)`，触发一次执行
- [ ] 在 DB `task_log` 中确认：
  - SUCCESS/FAILED 状态正确
  - `content` 为 JSON 且含 synced/failed/message
  - FAILED 时 `error_msg` 为简要错误
- [ ] 在页面确认：
  - `/task/manage` 出现“最近同步/最近消息”
  - `/task/detail` 出现摘要，并且历史记录里能看到统计
  - `/logs/task` 能看到每次执行的统计与错误简要

