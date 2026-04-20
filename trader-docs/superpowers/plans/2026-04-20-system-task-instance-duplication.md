# System Task Instance Duplication Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow repeated creation of `STRATEGY` task instances by generating unique instance task codes, while keeping all non-strategy task types single-instance only.

**Architecture:** Keep the existing task definition, Redis instance cache, refresh message, and executor protocol unchanged, and solve the requirement entirely at task-instance creation time. Non-strategy task types continue to use `taskType + taskCode` as a strict uniqueness key; strategy task types derive a new instance code from the definition code and use that generated code consistently for persistence, Redis publication, and refresh events.

**Tech Stack:** Spring Boot, Redis, MyBatis-Plus, Maven, JUnit 5, Mockito, Hutool

---

## File Map

### 需要修改

- `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java`
  - 增加“是否允许重复创建”的规则函数
  - 在 `createInstance()` 中区分“定义码”和“实例码”
  - 为策略任务生成新的实例码并用于后续保存、缓存和刷新
- `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/SystemTaskServiceTest.java`
  - 保留现有非策略创建成功测试
  - 增加非策略重复创建失败测试
  - 增加策略任务首次创建使用实例码测试
  - 增加策略任务重复创建生成不同实例码测试

### 需要核对但原则上不改

- `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/param/SystemTaskInstanceCreateParam.java`
  - 本次不新增实例码字段
- `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/dto/TaskDefinitionDto.java`
  - 本次不新增 `allowDuplicateInstance` 之类的新配置
- `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskRefreshMessage.java`
  - 保持结构不变，继续复用 `taskCode`
- `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskRefreshSubscriber.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskPoller.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskExecutor.java`
  - 都继续把 `taskCode` 视为“真实实例码”

## 设计注意点

- `taskType=STRATEGY` 是唯一允许重复创建的类型
- 前端提交的 `taskCode` 仍然表示“任务定义码”
- 对策略任务，真正落库到 `SystemTask.taskCode` 的值必须是系统生成的实例码
- Redis `trader:task:instances:<taskType>` 的 hash field 也必须写实例码，不能继续写定义码
- 刷新消息 `TraderTaskRefreshMessage.taskCode` 也必须带实例码
- 非策略任务仍然保持 `任务实例已存在` 的错误提示
- 策略任务实例码建议采用 `<definitionTaskCode>#<timestamp>` 形式

### Task 1: Lock Non-strategy Duplication Behavior With Tests

**Files:**
- Modify: `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/SystemTaskServiceTest.java`

- [ ] **Step 1: Add a failing test proving non-strategy duplicate creation is still rejected**

```java
Mockito.when(systemTaskDao.getByAppNameAndTaskCode("COLLECTOR", "fundSync")).thenReturn(existingTask);

Warning warning = Assertions.assertThrows(Warning.class, () -> systemTaskService.createInstance(param));

Assertions.assertEquals("任务实例已存在", warning.getMessage());
Mockito.verify(systemTaskDao, Mockito.never()).save(Mockito.any());
```

- [ ] **Step 2: Run the focused test to verify it fails for the expected reason**

Run: `./mvnw -pl trader-base,trader-admin/admin-server -Dtest=SystemTaskServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: FAIL because the new duplicate-rule test has not been implemented yet.

- [ ] **Step 3: Refactor existing test setup if needed to keep non-strategy creation explicit**

```java
param.setTaskType("COLLECTOR");
param.setTaskCode("fundSync");
```

- [ ] **Step 4: Re-run the focused test class to confirm the new expectation is still red and isolated**

Run: `./mvnw -pl trader-base,trader-admin/admin-server -Dtest=SystemTaskServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: FAIL only on the new non-strategy duplicate rule.

- [ ] **Step 5: Commit**

```bash
git add trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/SystemTaskServiceTest.java
git commit -m "test: cover non-strategy task duplication rules"
```

### Task 2: Implement Strategy-aware Instance Code Generation In `SystemTaskService`

**Files:**
- Modify: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java`

- [ ] **Step 1: Add a failing test proving strategy creation rewrites `taskCode` to a generated instance code**

```java
param.setTaskType("STRATEGY");
param.setTaskCode("relativeStrength");

systemTaskService.createInstance(param);

ArgumentCaptor<SystemTask> taskCaptor = ArgumentCaptor.forClass(SystemTask.class);
Mockito.verify(systemTaskDao).save(taskCaptor.capture());
Assertions.assertTrue(taskCaptor.getValue().getTaskCode().startsWith("relativeStrength#"));
Assertions.assertNotEquals("relativeStrength", taskCaptor.getValue().getTaskCode());
```

- [ ] **Step 2: Run the focused test class to verify the strategy-instance-code test fails**

Run: `./mvnw -pl trader-base,trader-admin/admin-server -Dtest=SystemTaskServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: FAIL because `createInstance()` still writes the original definition `taskCode`.

- [ ] **Step 3: Add the minimal rule helper and instance-code generator**

```java
private boolean allowDuplicateInstance(String taskType) {
    return "STRATEGY".equals(taskType);
}

private String generateInstanceTaskCode(String definitionTaskCode) {
    return definitionTaskCode + "#" + System.currentTimeMillis();
}
```

- [ ] **Step 4: Update `createInstance()` so non-strategy tasks keep the old uniqueness check and strategy tasks use generated instance codes**

```java
String definitionTaskCode = param.getTaskCode();
String instanceTaskCode = definitionTaskCode;

if (!allowDuplicateInstance(param.getTaskType())) {
    SystemTask existing = systemTaskDao.getByAppNameAndTaskCode(param.getTaskType(), definitionTaskCode);
    if (existing != null) {
        throw new Warning(ErrorCode.PARAM_INVALID.code(), "任务实例已存在");
    }
} else {
    instanceTaskCode = generateInstanceTaskCode(definitionTaskCode);
}

task.setTaskCode(instanceTaskCode);
```

- [ ] **Step 5: Re-run the focused test class to verify the strategy-instance-code test passes**

Run: `./mvnw -pl trader-base,trader-admin/admin-server -Dtest=SystemTaskServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: PASS for the strategy-instance-code assertion.

- [ ] **Step 6: Commit**

```bash
git add trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/SystemTaskServiceTest.java
git commit -m "feat: generate strategy task instance codes"
```

### Task 3: Verify Repeated Strategy Creation Does Not Overwrite Existing Instances

**Files:**
- Modify: `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/SystemTaskServiceTest.java`

- [ ] **Step 1: Add a failing test proving strategy duplicate creation generates different instance codes**

```java
systemTaskService.createInstance(param);
systemTaskService.createInstance(param);

ArgumentCaptor<SystemTask> taskCaptor = ArgumentCaptor.forClass(SystemTask.class);
Mockito.verify(systemTaskDao, Mockito.times(2)).save(taskCaptor.capture());
Assertions.assertNotEquals(taskCaptor.getAllValues().get(0).getTaskCode(), taskCaptor.getAllValues().get(1).getTaskCode());
```

- [ ] **Step 2: Assert Redis writes and refresh publishes both use generated instance codes**

```java
Mockito.verify(hashOperations, Mockito.times(2))
        .put(Mockito.eq("trader:task:instances:STRATEGY"), fieldCaptor.capture(), Mockito.anyString());
Assertions.assertNotEquals(fieldCaptor.getAllValues().get(0), fieldCaptor.getAllValues().get(1));

Mockito.verify(refreshPublisher, Mockito.times(2)).publish(messageCaptor.capture());
Assertions.assertNotEquals(messageCaptor.getAllValues().get(0).taskCode(), messageCaptor.getAllValues().get(1).taskCode());
```

- [ ] **Step 3: Run the focused test class to verify the duplicate-strategy-instance test fails if codes collide**

Run: `./mvnw -pl trader-base,trader-admin/admin-server -Dtest=SystemTaskServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: FAIL if the generator or downstream write path still reuses the same code.

- [ ] **Step 4: Add the smallest collision-avoidance loop if needed**

```java
private String generateUniqueInstanceTaskCode(String taskType, String definitionTaskCode) {
    String candidate = generateInstanceTaskCode(definitionTaskCode);
    while (systemTaskDao.getByAppNameAndTaskCode(taskType, candidate) != null) {
        candidate = generateInstanceTaskCode(definitionTaskCode);
    }
    return candidate;
}
```

- [ ] **Step 5: Re-run the focused test class to verify repeated strategy creation now stays green**

Run: `./mvnw -pl trader-base,trader-admin/admin-server -Dtest=SystemTaskServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: PASS with two independent strategy instance codes persisted and published.

- [ ] **Step 6: Commit**

```bash
git add trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/SystemTaskServiceTest.java
git commit -m "test: preserve distinct strategy task instances"
```

### Task 4: Final Verification For Task-instance Creation Rules

**Files:**
- Modify: none
- Verify: `trader-docs/superpowers/specs/2026-04-20-system-task-instance-duplication-design.md`

- [ ] **Step 1: Run the focused service test class**

Run: `./mvnw -pl trader-base,trader-admin/admin-server -Dtest=SystemTaskServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: PASS

- [ ] **Step 2: Run a compile check for the affected modules**

Run: `./mvnw -pl trader-base,trader-admin/admin-server -DskipTests compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Verify there is no accidental param or protocol expansion**

Run:

```bash
git diff -- trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/param/SystemTaskInstanceCreateParam.java trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskRefreshMessage.java trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskExecutor.java
```

Expected: no required protocol changes outside the intended service/test scope.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "test: verify strategy task instance duplication rules"
```

