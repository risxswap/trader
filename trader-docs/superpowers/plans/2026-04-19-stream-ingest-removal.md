# Stream Ingest Removal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove `trader-admin` as the Redis Stream ingest writer so task logs, task status, and node monitoring are written directly by each runtime module through `trader-base`.

**Architecture:** Keep `trader-admin` focused on configuration and query responsibilities, and move all system-event persistence into small store components inside `trader-base`. Reuse existing MySQL, ClickHouse, Redis, task registry, and node metadata infrastructure so the cutover is one-way, direct-write only, and does not disturb the task refresh protocol.

**Tech Stack:** Spring Boot, MyBatis-Plus, Redis, ClickHouse, MySQL, Quartz, Maven, JUnit 5, Mockito

---

## File Map

### 需要新增

- `trader-base/src/main/java/cc/riskswap/trader/base/logging/TaskLogStore.java`
  - 统一封装 `task_log` 的开始、成功、失败写入逻辑
- `trader-base/src/main/java/cc/riskswap/trader/base/task/SystemTaskStatusStore.java`
  - 统一封装 `system_task` 的状态与结果回写逻辑
- `trader-base/src/main/java/cc/riskswap/trader/base/monitor/TraderMonitorProperties.java`
  - 承载 `trader.monitor.*` 配置
- `trader-base/src/main/java/cc/riskswap/trader/base/monitor/HardwareSnapshot.java`
  - 标准化节点监控快照结构，避免直接在 Redis/DAO 之间散落字段拼装
- `trader-base/src/main/java/cc/riskswap/trader/base/monitor/HardwareMonitorService.java`
  - 采集本机 CPU、内存、磁盘等监控数据
- `trader-base/src/main/java/cc/riskswap/trader/base/monitor/NodeMonitorStore.java`
  - 将监控快照直接写 ClickHouse `node_monitor`，并同步 Redis `node:monitor`
- `trader-base/src/main/java/cc/riskswap/trader/base/monitor/HardwareMonitorPublisher.java`
  - 按周期采集并调用 `NodeMonitorStore`
- `trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderMonitorAutoConfiguration.java`
  - 装配监控相关 Bean
- `trader-base/src/test/java/cc/riskswap/trader/base/logging/TaskLogStoreTest.java`
- `trader-base/src/test/java/cc/riskswap/trader/base/task/SystemTaskStatusStoreTest.java`
- `trader-base/src/test/java/cc/riskswap/trader/base/monitor/NodeMonitorStoreTest.java`
- `trader-base/src/test/java/cc/riskswap/trader/base/autoconfigure/TraderMonitorAutoConfigurationTest.java`

### 需要修改

- `trader-base/src/main/java/cc/riskswap/trader/base/logging/TraderTaskLogAspect.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderLoggingAutoConfiguration.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskExecutor.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfiguration.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/dao/TaskLogDao.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/dao/SystemTaskDao.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/dao/NodeMonitorDao.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/dao/entity/NodeMonitor.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/event/SystemTaskStatusEvent.java`
- `trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskExecutorTest.java`
- `trader-base/src/test/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfigurationTest.java`
- `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/NodeService.java`
- `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/NodeServiceTest.java`

### 需要删除

- `trader-base/src/main/java/cc/riskswap/trader/base/event/TraderStreamPublisher.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/event/TaskLogEvent.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/event/NodeMonitorEvent.java`
- `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/config/RedisStreamConfig.java`
- `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/stream/TraderStreamConsumer.java`
- `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/stream/TraderStreamConsumerTest.java`

### 需要核对但不改协议

- `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java`
  - 继续负责把任务实例快照写入 `trader:task:instances:*`
- `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskRefreshPublisher.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskRefreshSubscriber.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskPoller.java`

### Task 1: Direct-write Task Logs In `trader-base`

**Files:**
- Create: `trader-base/src/main/java/cc/riskswap/trader/base/logging/TaskLogStore.java`
- Modify: `trader-base/src/main/java/cc/riskswap/trader/base/logging/TraderTaskLogAspect.java`
- Modify: `trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderLoggingAutoConfiguration.java`
- Modify: `trader-base/src/main/java/cc/riskswap/trader/base/dao/TaskLogDao.java`
- Test: `trader-base/src/test/java/cc/riskswap/trader/base/logging/TaskLogStoreTest.java`

- [ ] **Step 1: Write the failing store test**

```java
TaskLogStore store = new TaskLogStore(taskLogDao);
store.writeRunning("同步基金", "fundSync", startTime, "trace-1");
verify(taskLogDao).createRunningLog("同步基金", "fundSync", startTime, "trace-1");
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -pl trader-base -Dtest=TaskLogStoreTest test`
Expected: FAIL because `TaskLogStore` does not exist yet.

- [ ] **Step 3: Add the minimal store API**

```java
public class TaskLogStore {
    private final TaskLogDao taskLogDao;

    public void writeRunning(String taskName, String taskGroup, LocalDateTime startTime, String traceId) {
        taskLogDao.createRunningLog(taskName, taskGroup, startTime, traceId);
    }
}
```

- [ ] **Step 4: Add success and failure update coverage**

```java
store.writeFinished("trace-1", "SUCCESS", 1200L, "done");
verify(taskLogDao).updateLogByTraceId("trace-1", "SUCCESS", 1200L, "done");
```

- [ ] **Step 5: Switch `TraderTaskLogAspect` from stream publishing to store calls**

```java
taskLogStore.writeRunning(taskName, taskGroup, startTime, traceId);
taskLogStore.writeFinished(traceId, "SUCCESS", executionMs, remark);
```

- [ ] **Step 6: Run focused tests**

Run: `./mvnw -pl trader-base -Dtest=TaskLogStoreTest test`
Expected: PASS

- [ ] **Step 7: Run logging-related verification**

Run: `./mvnw -pl trader-base -Dtest=TaskLogStoreTest,TraderTaskExecutorTest,TraderTaskAutoConfigurationTest test`
Expected: PASS and no `TraderStreamPublisher` wiring is required for task log writes.

- [ ] **Step 8: Commit**

```bash
git add trader-base/src/main/java/cc/riskswap/trader/base/logging/TaskLogStore.java trader-base/src/main/java/cc/riskswap/trader/base/logging/TraderTaskLogAspect.java trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderLoggingAutoConfiguration.java trader-base/src/main/java/cc/riskswap/trader/base/dao/TaskLogDao.java trader-base/src/test/java/cc/riskswap/trader/base/logging/TaskLogStoreTest.java
git commit -m "feat: write task logs directly from base"
```

### Task 2: Direct-write `system_task` Status And Result

**Files:**
- Create: `trader-base/src/main/java/cc/riskswap/trader/base/task/SystemTaskStatusStore.java`
- Modify: `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskExecutor.java`
- Modify: `trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfiguration.java`
- Modify: `trader-base/src/main/java/cc/riskswap/trader/base/dao/SystemTaskDao.java`
- Modify: `trader-base/src/main/java/cc/riskswap/trader/base/event/SystemTaskStatusEvent.java`
- Test: `trader-base/src/test/java/cc/riskswap/trader/base/task/SystemTaskStatusStoreTest.java`
- Test: `trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskExecutorTest.java`

- [ ] **Step 1: Write the failing store test for `taskType + taskCode` updates**

```java
store.writeStatus("COLLECTOR", "fundSync", "RUNNING", null, 10L);
verify(systemTaskDao).getByTaskTypeAndTaskCode("COLLECTOR", "fundSync");
verify(systemTaskDao).updateById(updatedTask);
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -pl trader-base -Dtest=SystemTaskStatusStoreTest test`
Expected: FAIL because `SystemTaskStatusStore` does not exist yet.

- [ ] **Step 3: Add the minimal direct-write store**

```java
public void writeStatus(String taskType, String taskCode, String status, String result, Long version) {
    SystemTask task = systemTaskDao.getByTaskTypeAndTaskCode(taskType, taskCode);
    if (task == null) {
        return;
    }
    task.setStatus(status);
    task.setResult(result);
    task.setVersion(version);
    systemTaskDao.updateById(task);
}
```

- [ ] **Step 4: Rewrite executor lifecycle to call the store instead of `TraderStreamPublisher`**

```java
statusStore.writeStatus(instance.getTaskType(), instance.getTaskCode(), "RUNNING", null, instance.getVersion());
statusStore.writeStatus(instance.getTaskType(), instance.getTaskCode(), "STOPPED", "SUCCESS", instance.getVersion());
```

- [ ] **Step 5: Update executor tests to assert store interactions instead of stream publishes**

```java
verify(statusStore).writeStatus("COLLECTOR", "fundSync", "RUNNING", null, null);
verify(statusStore).writeStatus("COLLECTOR", "fundSync", "STOPPED", "SUCCESS", null);
```

- [ ] **Step 6: Run focused tests**

Run: `./mvnw -pl trader-base -Dtest=SystemTaskStatusStoreTest,TraderTaskExecutorTest test`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add trader-base/src/main/java/cc/riskswap/trader/base/task/SystemTaskStatusStore.java trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskExecutor.java trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfiguration.java trader-base/src/main/java/cc/riskswap/trader/base/dao/SystemTaskDao.java trader-base/src/main/java/cc/riskswap/trader/base/event/SystemTaskStatusEvent.java trader-base/src/test/java/cc/riskswap/trader/base/task/SystemTaskStatusStoreTest.java trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskExecutorTest.java
git commit -m "feat: write system task status directly from executor"
```

### Task 3: Build Node Monitor Direct-write Infrastructure

**Files:**
- Create: `trader-base/src/main/java/cc/riskswap/trader/base/monitor/TraderMonitorProperties.java`
- Create: `trader-base/src/main/java/cc/riskswap/trader/base/monitor/HardwareSnapshot.java`
- Create: `trader-base/src/main/java/cc/riskswap/trader/base/monitor/HardwareMonitorService.java`
- Create: `trader-base/src/main/java/cc/riskswap/trader/base/monitor/NodeMonitorStore.java`
- Create: `trader-base/src/main/java/cc/riskswap/trader/base/monitor/HardwareMonitorPublisher.java`
- Create: `trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderMonitorAutoConfiguration.java`
- Modify: `trader-base/src/main/java/cc/riskswap/trader/base/dao/NodeMonitorDao.java`
- Modify: `trader-base/src/main/java/cc/riskswap/trader/base/dao/entity/NodeMonitor.java`
- Test: `trader-base/src/test/java/cc/riskswap/trader/base/monitor/NodeMonitorStoreTest.java`
- Test: `trader-base/src/test/java/cc/riskswap/trader/base/autoconfigure/TraderMonitorAutoConfigurationTest.java`

- [ ] **Step 1: Write the failing `NodeMonitorStore` test**

```java
store.write(snapshot);
verify(nodeMonitorDao).save(any(NodeMonitor.class));
verify(stringRedisTemplate.opsForHash()).put(eq("node:monitor"), eq("node-1"), anyString());
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -pl trader-base -Dtest=NodeMonitorStoreTest test`
Expected: FAIL because the monitor package and store do not exist yet.

- [ ] **Step 3: Add the snapshot model and minimal store implementation**

```java
public record HardwareSnapshot(String nodeId, String nodeType, String nodeName, OffsetDateTime collectedAt, BigDecimal cpuLoad, Long physicalMemoryTotal, Long physicalMemoryAvailable, Long diskTotal, Long diskAvailable, String hostname, String primaryIp) {}
```

```java
public void write(HardwareSnapshot snapshot) {
    nodeMonitorDao.save(toEntity(snapshot));
    stringRedisTemplate.opsForHash().put("node:monitor", snapshot.nodeId(), JSONUtil.toJsonStr(snapshot));
}
```

- [ ] **Step 4: Add a minimal publisher that samples and writes on an interval**

```java
@Scheduled(fixedDelayString = "#{@traderMonitorProperties.getInterval().toMillis()}")
public void publish() {
    nodeMonitorStore.write(hardwareMonitorService.snapshot());
}
```

- [ ] **Step 5: Add monitor auto-configuration with datasource/redis guards**

```java
@ConditionalOnProperty(prefix = "trader.monitor", name = "enabled", havingValue = "true")
@EnableConfigurationProperties({TraderMonitorProperties.class, TraderNodeProperties.class})
```

- [ ] **Step 6: Run focused monitor tests**

Run: `./mvnw -pl trader-base -Dtest=NodeMonitorStoreTest,TraderMonitorAutoConfigurationTest test`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add trader-base/src/main/java/cc/riskswap/trader/base/monitor/TraderMonitorProperties.java trader-base/src/main/java/cc/riskswap/trader/base/monitor/HardwareSnapshot.java trader-base/src/main/java/cc/riskswap/trader/base/monitor/HardwareMonitorService.java trader-base/src/main/java/cc/riskswap/trader/base/monitor/NodeMonitorStore.java trader-base/src/main/java/cc/riskswap/trader/base/monitor/HardwareMonitorPublisher.java trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderMonitorAutoConfiguration.java trader-base/src/main/java/cc/riskswap/trader/base/dao/NodeMonitorDao.java trader-base/src/main/java/cc/riskswap/trader/base/dao/entity/NodeMonitor.java trader-base/src/test/java/cc/riskswap/trader/base/monitor/NodeMonitorStoreTest.java trader-base/src/test/java/cc/riskswap/trader/base/autoconfigure/TraderMonitorAutoConfigurationTest.java
git commit -m "feat: write node monitor directly from runtime modules"
```

### Task 4: Remove Stream Ingest Plumbing From Base And Admin

**Files:**
- Delete: `trader-base/src/main/java/cc/riskswap/trader/base/event/TraderStreamPublisher.java`
- Delete: `trader-base/src/main/java/cc/riskswap/trader/base/event/TaskLogEvent.java`
- Delete: `trader-base/src/main/java/cc/riskswap/trader/base/event/NodeMonitorEvent.java`
- Delete: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/config/RedisStreamConfig.java`
- Delete: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/stream/TraderStreamConsumer.java`
- Delete: `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/stream/TraderStreamConsumerTest.java`
- Modify: `trader-base/src/test/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfigurationTest.java`

- [ ] **Step 1: Write a failing structure test proving task auto-configuration no longer exposes the stream publisher**

```java
assertThatThrownBy(() -> context.getBean(TraderStreamPublisher.class))
        .isInstanceOf(NoSuchBeanDefinitionException.class);
```

- [ ] **Step 2: Run the focused structure test**

Run: `./mvnw -pl trader-base -Dtest=TraderTaskAutoConfigurationTest test`
Expected: FAIL until the stream publisher bean and references are removed.

- [ ] **Step 3: Remove the stream publisher bean and dead event classes**

```java
// delete traderStreamPublisher() bean from TraderTaskAutoConfiguration
```

- [ ] **Step 4: Delete admin-side stream consumer wiring**

```bash
git rm trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/config/RedisStreamConfig.java
git rm trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/stream/TraderStreamConsumer.java
git rm trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/stream/TraderStreamConsumerTest.java
```

- [ ] **Step 5: Re-run focused base and admin tests**

Run: `./mvnw -pl trader-base,trader-admin/admin-server -Dtest=TraderTaskAutoConfigurationTest,NodeServiceTest test`
Expected: PASS and no code path depends on stream ingest.

- [ ] **Step 6: Commit**

```bash
git add trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfiguration.java trader-base/src/test/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfigurationTest.java trader-base/src/main/java/cc/riskswap/trader/base/event trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/config trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/stream trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/stream
git commit -m "refactor: remove stream ingest plumbing"
```

### Task 5: Keep Query-side Behavior Stable For Admin

**Files:**
- Modify: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/NodeService.java`
- Test: `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/NodeServiceTest.java`

- [ ] **Step 1: Add a focused test that `NodeService` can still build node history from direct-write data**

```java
Mockito.when(nodeMonitorDao.listHistory("node-1", start, end)).thenReturn(List.of(l1, l2));
Assertions.assertEquals(2, result.getCpuUsages().size());
```

- [ ] **Step 2: Run the focused admin node test**

Run: `./mvnw -pl trader-admin/admin-server -Dtest=NodeServiceTest test`
Expected: PASS if direct-write data shape still matches the existing query contract.

- [ ] **Step 3: Normalize Redis snapshot fields if monitor payload naming changed**

```java
node.setCpuUsage(toFloat(monitor.getDouble("cpuLoad")));
node.setMemoryUsage(calculateUsageRatio(monitor.getLong("physicalMemoryAvailable"), monitor.getLong("physicalMemoryTotal")));
```

- [ ] **Step 4: Re-run the node test class**

Run: `./mvnw -pl trader-admin/admin-server -Dtest=NodeServiceTest test`
Expected: PASS with both `node:monitor` and legacy fallback cases green.

- [ ] **Step 5: Commit**

```bash
git add trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/NodeService.java trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/NodeServiceTest.java
git commit -m "test: preserve node query behavior after direct writes"
```

### Task 6: Full Verification And Cleanup

**Files:**
- Modify: none
- Verify: `trader-docs/superpowers/specs/2026-04-19-stream-ingest-removal-design.md`

- [ ] **Step 1: Run base targeted tests**

Run: `./mvnw -pl trader-base -Dtest=TaskLogStoreTest,SystemTaskStatusStoreTest,NodeMonitorStoreTest,TraderMonitorAutoConfigurationTest,TraderTaskExecutorTest,TraderTaskAutoConfigurationTest test`
Expected: PASS

- [ ] **Step 2: Run admin targeted tests**

Run: `./mvnw -pl trader-admin/admin-server -Dtest=NodeServiceTest test`
Expected: PASS

- [ ] **Step 3: Run a cross-module compile**

Run: `./mvnw -pl trader-base,trader-admin/admin-server,trader-collector,trader-executor,trader-statistic -DskipTests compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: Manually verify the cutover assumptions**

Run:

```bash
git grep -n "TraderStreamConsumer\|RedisStreamConfig\|TraderStreamPublisher\|TaskLogEvent\|NodeMonitorEvent"
```

Expected: no remaining production references for the removed ingest chain.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "test: verify direct-write ingest cutover"
```

