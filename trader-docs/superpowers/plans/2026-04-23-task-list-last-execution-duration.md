# 任务列表最后一次执行耗时展示 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在任务管理列表中新增 `最后执行耗时` 列，展示每个系统任务最新一条执行历史的耗时，并复用现有耗时格式化规则。

**Architecture:** 基于现有任务列表接口 `POST /task/list` 增量扩展，在 `SystemTaskService.list()` 中继续沿用当前执行统计回填模式。后端通过 `TaskLogDao` 批量查询当前页系统任务的最新日志摘要并回填 `lastExecutionMs`，前端只扩展 DTO 并在 `Manage.vue` 新增一列显示格式化结果。

**Tech Stack:** Spring Boot, MyBatis-Plus, Vue 3, TypeScript, Element Plus, Maven, npm

---

## 文件结构

**后端修改**
- `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/dto/SystemTaskDto.java`
  - 扩展任务列表返回字段，新增 `lastExecutionMs`
- `trader-base/src/main/java/cc/riskswap/trader/base/dao/TaskLogDao.java`
  - 增加批量查询“每个任务最新一条日志摘要”的 DAO 能力
- `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java`
  - 在列表服务中统一回填 `executionCount` 与 `lastExecutionMs`
- `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/SystemTaskServiceTest.java`
  - 先补失败测试，锁定最新执行耗时字段与空值规则

**前端修改**
- `trader-admin/admin-web/src/services/systemTask.ts`
  - 扩展列表项类型声明，新增 `lastExecutionMs`
- `trader-admin/admin-web/src/pages/task/Manage.vue`
  - 新增 `最后执行耗时` 列并复用 `formatExecutionDuration()`

**现有复用文件**
- `trader-admin/admin-web/src/utils/taskLog.ts`
  - 已有耗时格式化工具，无需重写，只需复用

---

### Task 1: 先写后端失败测试锁定最后执行耗时行为

**Files:**
- Modify: `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/SystemTaskServiceTest.java`

- [ ] **Step 1: 为系统任务最新执行耗时新增失败测试**

在 `SystemTaskServiceTest` 中增加这些测试场景：

```java
@Test
void should_fill_last_execution_ms_from_latest_task_log() {
    // 两条相同 taskGroup 的日志，按 startTime 取最新一条的 executionMs
}

@Test
void should_leave_last_execution_ms_null_when_latest_log_has_no_duration() {
    // 最新日志 executionMs 为空，断言 DTO 返回 null
}

@Test
void should_leave_investment_last_execution_ms_null() {
    // sourceType=INVESTMENT 不查日志，断言 null
}
```

- [ ] **Step 2: 运行聚焦测试并确认先失败**

Run:

```bash
./mvnw -pl trader-admin/admin-server -am -Dtest=SystemTaskServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

- `FAIL`
- 失败原因是 `SystemTaskDto` 缺少 `lastExecutionMs`、或 `SystemTaskService` 未回填该字段、或 `TaskLogDao` 缺少最新日志摘要查询能力

- [ ] **Step 3: 提交测试红灯状态前的工作快照**

```bash
git add trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/SystemTaskServiceTest.java
git commit -m "test: cover last execution duration in task list"
```

如果当前分支不适合在红灯阶段提交，则跳过提交，但必须保留测试先失败的执行记录。

---

### Task 2: 实现后端最新执行摘要聚合并让测试转绿

**Files:**
- Modify: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/dto/SystemTaskDto.java`
- Modify: `trader-base/src/main/java/cc/riskswap/trader/base/dao/TaskLogDao.java`
- Modify: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java`
- Test: `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/SystemTaskServiceTest.java`

- [ ] **Step 1: 在 DTO 中新增字段**

在 `SystemTaskDto` 中新增：

```java
private Long lastExecutionMs;
```

- [ ] **Step 2: 在 TaskLogDao 中增加最新日志摘要查询方法**

新增一个按 `task_group` 批量查询最新日志摘要的方法，返回结构可采用：

```java
public Map<String, Long> latestExecutionMsByTaskGroups(List<String> taskGroups)
```

实现要求：

- 输入为空时返回 `Map.of()`
- 仅针对传入的 `task_group`
- 每个 `task_group` 取 `start_time` 最新的一条日志
- 返回该条日志的 `execution_ms`
- 若最新日志 `execution_ms` 为 `null`，则 map 中允许该 key 对应 `null`，或由服务层显式补空值逻辑

可选实现方向：

- 方案 A：先按任务组查询所有候选日志并在 Java 内按 `startTime` 归并
- 方案 B：使用子查询或排序后首条方式直接取每组最新记录

优先选择当前代码库中最容易维护、最容易在测试里稳定模拟的写法。

- [ ] **Step 3: 在 SystemTaskService 中统一回填执行摘要**

把当前：

```java
applyExecutionCounts(items);
```

整理为一个更通用的方法，例如：

```java
applyExecutionSummary(items);
```

要求：

- 仅收集 `sourceType=SYSTEM` 的 `taskCode`
- 一次取回：
  - `executionCount`
  - `lastExecutionMs`
- 系统任务按 `taskCode` 回填
- 策略任务保持：
  - `executionCount = 0L`
  - `lastExecutionMs = null`

- [ ] **Step 4: 运行聚焦测试确认转绿**

Run:

```bash
./mvnw -pl trader-admin/admin-server -am -Dtest=SystemTaskServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

- `PASS`
- 新增的 `lastExecutionMs` 相关测试全部通过

- [ ] **Step 5: 运行相关后端回归测试**

Run:

```bash
./mvnw -pl trader-admin/admin-server -am -Dtest=SystemTaskServiceTest,TaskLogServiceTest,TaskLogControllerRouteTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

- `PASS`
- 不影响已有任务日志删除与执行次数统计行为

- [ ] **Step 6: 提交后端实现**

```bash
git add \
  trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/dto/SystemTaskDto.java \
  trader-base/src/main/java/cc/riskswap/trader/base/dao/TaskLogDao.java \
  trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java \
  trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/SystemTaskServiceTest.java
git commit -m "feat: add last execution duration to task list"
```

---

### Task 3: 扩展前端类型并在任务管理页新增耗时列

**Files:**
- Modify: `trader-admin/admin-web/src/services/systemTask.ts`
- Modify: `trader-admin/admin-web/src/pages/task/Manage.vue`
- Reference: `trader-admin/admin-web/src/utils/taskLog.ts`

- [ ] **Step 1: 扩展前端 DTO 类型**

在 `SystemTask` 接口中新增：

```ts
lastExecutionMs?: number
```

- [ ] **Step 2: 在任务管理页新增列并复用格式化工具**

在 `Manage.vue` 中：

- 引入或继续使用 `formatExecutionDuration`
- 在 `执行次数` 后新增 `最后执行耗时` 列
- 展示逻辑：

```vue
{{ formatExecutionDuration(row.lastExecutionMs) }}
```

要求：

- 列宽控制在能稳定展示常见时分秒文本的范围
- 单元格居中
- 不新增 tag、颜色、tooltip

- [ ] **Step 3: 确认空值展示规则**

手工检查这几种前端输入：

- `undefined`
- `null`
- `500`
- `65000`
- `3600000`

Expected:

- 分别显示 `-`、`-`、`500 ms`、`1分5秒`、`1时0分0秒`

- [ ] **Step 4: 构建前端确认页面可编译**

Run:

```bash
npm run build
```

Working directory:

```bash
/Users/ming/Workspace/trader/trader-admin/admin-web
```

Expected:

- `PASS`
- 无新的 TypeScript 或 Vue 编译错误

- [ ] **Step 5: 提交前端实现**

```bash
git add \
  trader-admin/admin-web/src/services/systemTask.ts \
  trader-admin/admin-web/src/pages/task/Manage.vue
git commit -m "feat: show last execution duration in task list"
```

---

### Task 4: 做诊断检查与最终回归

**Files:**
- Check: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/dto/SystemTaskDto.java`
- Check: `trader-base/src/main/java/cc/riskswap/trader/base/dao/TaskLogDao.java`
- Check: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java`
- Check: `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/SystemTaskServiceTest.java`
- Check: `trader-admin/admin-web/src/services/systemTask.ts`
- Check: `trader-admin/admin-web/src/pages/task/Manage.vue`

- [ ] **Step 1: 运行 IDE 诊断检查最近编辑文件**

使用 `GetDiagnostics` 检查以上文件。

Expected:

- 不出现新的 Java、TypeScript、Vue 诊断错误

- [ ] **Step 2: 手工核对列表口径**

核对这些规则是否都满足：

- 系统任务无历史记录时：`执行次数 = 0`，`最后执行耗时 = -`
- 系统任务有历史记录时：`最后执行耗时` 来自最新一条日志
- 最新日志 `executionMs = null` 时显示 `-`
- 策略任务：`执行次数 = 0`，`最后执行耗时 = -`

- [ ] **Step 3: 运行最终工作区检查**

Run:

```bash
git status --short
```

Expected:

- 只包含本次需求相关改动
- 若工作区本来就有其他改动，记录并在交付说明中明确说明未触碰范围

- [ ] **Step 4: 最终提交**

```bash
git add trader-admin/admin-server trader-admin/admin-web
git commit -m "feat: add latest execution duration to task management list"
```

---

## 实施说明

- 严格遵循 `@superpowers/test-driven-development`：先写失败测试，再实现最小代码
- 不要改动任务详情页已有耗时格式化逻辑，只复用前端公共工具
- 不要为策略任务额外推断日志映射关系，本次保持空值展示
- 如果 `TaskLogDao` 的 SQL 实现复杂度过高，优先选择更可读、更易测的 Java 层归并实现

## 完成定义

满足以下条件才算完成：

- `SystemTaskDto` 返回 `lastExecutionMs`
- 任务管理页新增 `最后执行耗时` 列
- 展示值与任务详情页耗时格式一致
- 所有指定测试与构建命令通过
- 最近编辑文件无新增诊断错误
