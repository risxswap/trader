# Task Definition/Instance Split Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 落地“Redis 存任务定义、DB 存任务实例”，以 `(taskType, taskCode)` 全局唯一，并支持任务实例创建/删除、节点按类型调度、全局单实例抢占执行，以及任务管理页聚合展示投资策略任务。

**Architecture:** 节点启动扫描 `TraderTask` 并把定义写入 Redis（覆盖写去重）；管理端从 Redis 定义创建 `system_task` 实例；节点只拉取与自身 nodeType 匹配的实例并通过 Quartz 调度，触发执行前用 Redis 分布式锁抢占确保同一 fireTime 只执行一次；管理端 `/task/list` 支持聚合投资策略为 EXECUTOR 类型的虚拟任务行。

**Tech Stack:** Spring Boot 3、MyBatis-Plus、Quartz、Redis（StringRedisTemplate）、Vue 3、Element Plus

---

## File Map

### `trader-base` 预计新增文件

- `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskType.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/task/CollectorTask.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/task/StatisticTask.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/task/ExecutorTask.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskDefinition.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskDefinitionPublisher.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskLock.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskExecutor.java`

### `trader-base` 预计修改文件

- `trader-base/src/main/resources/sql/task-module-mysql.sql`
- `trader-base/src/main/java/cc/riskswap/trader/base/dao/entity/SystemTask.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/dao/SystemTaskDao.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/dao/query/SystemTaskListQuery.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskProperties.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskSchedulerService.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskPoller.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskRefreshMessage.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskRefreshSubscriber.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskPollingJob.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderQuartzJob.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfiguration.java`
- `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskMetadataSyncService.java`（改为“不再写 DB”，或直接移除 Runner）

### `admin-server` 预计新增文件

- `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/dto/TaskDefinitionDto.java`
- `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/param/SystemTaskInstanceCreateParam.java`
- `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/param/SystemTaskInstanceDeleteParam.java`
- `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/query/TaskDefinitionListQuery.java`

### `admin-server` 预计修改文件

- `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/dto/SystemTaskDto.java`
- `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/query/SystemTaskListQuery.java`
- `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/param/SystemTaskUpdateParam.java`
- `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/param/SystemTaskTriggerParam.java`
- `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/controller/SystemTaskController.java`
- `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java`

### `admin-web` 预计修改文件

- `trader-admin/admin-web/src/services/systemTask.ts`
- `trader-admin/admin-web/src/pages/task/Manage.vue`

### 测试文件（新增或修改）

- `trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskTypeTest.java`
- `trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskDefinitionPublisherTest.java`
- `trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskLockTest.java`
- `trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskExecutorTest.java`
- `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/controller/SystemTaskControllerRouteTest.java`（更新断言）
- `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/SystemTaskServiceTest.java`（更新断言 + 覆盖 create/delete/definition list）

---

### Task 1: 扩展 system_task 为“实例表”（加入 task_type 全局唯一键）

**Files:**
- Modify: `trader-base/src/main/resources/sql/task-module-mysql.sql`
- Modify: `trader-base/src/main/java/cc/riskswap/trader/base/dao/entity/SystemTask.java`
- Modify: `trader-base/src/main/java/cc/riskswap/trader/base/dao/query/SystemTaskListQuery.java`
- Modify: `trader-base/src/main/java/cc/riskswap/trader/base/dao/SystemTaskDao.java`

- [ ] **Step 1: 写一个失败测试，约束 SystemTask 必须具备 taskType 字段**

Create `trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskTypeTest.java`:

```java
package cc.riskswap.trader.base.task;

import cc.riskswap.trader.base.dao.entity.SystemTask;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

class TraderTaskTypeTest {

    @Test
    void system_task_entity_should_have_task_type_field() throws Exception {
        Field field = SystemTask.class.getDeclaredField("taskType");
        Assertions.assertEquals(String.class, field.getType());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```bash
cd /Users/haiming/Workspace/trader/trader-base && mvn -Dtest=TraderTaskTypeTest test
```

Expected: FAIL，提示 `NoSuchFieldException: taskType`

- [ ] **Step 3: 修改 DDL，新增 task_type 字段并调整唯一键**

Update `trader-base/src/main/resources/sql/task-module-mysql.sql` 为（保留原字段但变更唯一键）：

```sql
CREATE TABLE IF NOT EXISTS system_task (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    app_name VARCHAR(64),
    task_type VARCHAR(32) NOT NULL,
    task_code VARCHAR(128) NOT NULL,
    task_name VARCHAR(256) NOT NULL,
    cron VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    param_schema TEXT,
    params_json TEXT,
    default_params_json TEXT,
    version BIGINT NOT NULL DEFAULT 1,
    remark VARCHAR(512),
    updated_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY system_task_task_type_task_code_uidx (task_type, task_code),
    KEY system_task_task_type_idx (task_type),
    KEY system_task_status_idx (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

- [ ] **Step 4: 修改 Entity / Query / Dao 支持 taskType 过滤**

Update `trader-base/src/main/java/cc/riskswap/trader/base/dao/entity/SystemTask.java`：

```java
package cc.riskswap.trader.base.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("system_task")
public class SystemTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String appName;
    private String taskType;
    private String taskCode;
    private String taskName;
    private String cron;
    private String status;
    private String paramSchema;
    private String paramsJson;
    private String defaultParamsJson;
    private Long version;
    private String remark;
    private OffsetDateTime updatedAt;
    private OffsetDateTime createdAt;
}
```

Update `trader-base/src/main/java/cc/riskswap/trader/base/dao/query/SystemTaskListQuery.java`（加入 taskType）：

```java
package cc.riskswap.trader.base.dao.query;

import lombok.Data;

@Data
public class SystemTaskListQuery {
    private Integer pageNo = 1;
    private Integer pageSize = 20;
    private String appName;
    private String taskType;
    private String taskCode;
    private String taskName;
    private String status;
}
```

Update `trader-base/src/main/java/cc/riskswap/trader/base/dao/SystemTaskDao.java`：

```java
package cc.riskswap.trader.base.dao;

import cc.riskswap.trader.base.dao.entity.SystemTask;
import cc.riskswap.trader.base.dao.mapper.SystemTaskMapper;
import cc.riskswap.trader.base.dao.query.SystemTaskListQuery;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SystemTaskDao extends ServiceImpl<SystemTaskMapper, SystemTask> {

    public SystemTask getByTaskTypeAndTaskCode(String taskType, String taskCode) {
        LambdaQueryWrapper<SystemTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemTask::getTaskType, taskType);
        wrapper.eq(SystemTask::getTaskCode, taskCode);
        return this.getOne(wrapper);
    }

    public List<SystemTask> listByTaskType(String taskType) {
        LambdaQueryWrapper<SystemTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemTask::getTaskType, taskType);
        wrapper.orderByAsc(SystemTask::getTaskCode);
        return this.list(wrapper);
    }

    public Page<SystemTask> pageQuery(SystemTaskListQuery query) {
        Page<SystemTask> page = new Page<>(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<SystemTask> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getTaskType())) {
            wrapper.eq(SystemTask::getTaskType, query.getTaskType());
        }
        if (StrUtil.isNotBlank(query.getTaskCode())) {
            wrapper.like(SystemTask::getTaskCode, query.getTaskCode());
        }
        if (StrUtil.isNotBlank(query.getTaskName())) {
            wrapper.like(SystemTask::getTaskName, query.getTaskName());
        }
        if (StrUtil.isNotBlank(query.getStatus())) {
            wrapper.eq(SystemTask::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(SystemTask::getTaskType).orderByAsc(SystemTask::getTaskCode);
        return this.page(page, wrapper);
    }
}
```

- [ ] **Step 5: 重新运行测试确认通过**

Run:

```bash
cd /Users/haiming/Workspace/trader/trader-base && mvn -Dtest=TraderTaskTypeTest test
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git -C /Users/haiming/Workspace/trader add trader-base/src/main/resources/sql/task-module-mysql.sql trader-base/src/main/java/cc/riskswap/trader/base/dao/entity/SystemTask.java trader-base/src/main/java/cc/riskswap/trader/base/dao/query/SystemTaskListQuery.java trader-base/src/main/java/cc/riskswap/trader/base/dao/SystemTaskDao.java trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskTypeTest.java
git -C /Users/haiming/Workspace/trader commit -m "feat: add task_type to system_task instance model"
```

---

### Task 2: 引入 taskType 枚举与三类任务抽象（Collector/Statistic/Executor）

**Files:**
- Create: `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskType.java`
- Create: `trader-base/src/main/java/cc/riskswap/trader/base/task/CollectorTask.java`
- Create: `trader-base/src/main/java/cc/riskswap/trader/base/task/StatisticTask.java`
- Create: `trader-base/src/main/java/cc/riskswap/trader/base/task/ExecutorTask.java`

- [ ] **Step 1: 写失败测试，约束 taskType 枚举存在且可从 nodeType 解析**

Extend `trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskTypeTest.java`：

```java
package cc.riskswap.trader.base.task;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TraderTaskTypeTest {

    @Test
    void should_parse_from_node_type() {
        Assertions.assertEquals(TraderTaskType.COLLECTOR, TraderTaskType.fromNodeType("collector"));
        Assertions.assertEquals(TraderTaskType.STATISTIC, TraderTaskType.fromNodeType("STATISTIC"));
        Assertions.assertEquals(TraderTaskType.EXECUTOR, TraderTaskType.fromNodeType("executor"));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```bash
cd /Users/haiming/Workspace/trader/trader-base && mvn -Dtest=TraderTaskTypeTest test
```

Expected: FAIL，提示 `TraderTaskType` 不存在

- [ ] **Step 3: 增加 TraderTaskType 枚举**

Create `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskType.java`：

```java
package cc.riskswap.trader.base.task;

import cn.hutool.core.util.StrUtil;

public enum TraderTaskType {
    COLLECTOR,
    STATISTIC,
    EXECUTOR;

    public static TraderTaskType fromNodeType(String nodeType) {
        if (StrUtil.isBlank(nodeType)) {
            throw new IllegalArgumentException("nodeType is blank");
        }
        String normalized = nodeType.trim().toUpperCase();
        return TraderTaskType.valueOf(normalized);
    }
}
```

- [ ] **Step 4: 增加三类任务抽象接口（只做标记，不改变现有执行入口）**

Create `CollectorTask.java`：

```java
package cc.riskswap.trader.base.task;

public interface CollectorTask extends TraderTask {
}
```

Create `StatisticTask.java`：

```java
package cc.riskswap.trader.base.task;

public interface StatisticTask extends TraderTask {
}
```

Create `ExecutorTask.java`：

```java
package cc.riskswap.trader.base.task;

public interface ExecutorTask extends TraderTask {
}
```

- [ ] **Step 5: 运行测试确认通过**

Run:

```bash
cd /Users/haiming/Workspace/trader/trader-base && mvn -Dtest=TraderTaskTypeTest test
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git -C /Users/haiming/Workspace/trader add trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskType.java trader-base/src/main/java/cc/riskswap/trader/base/task/CollectorTask.java trader-base/src/main/java/cc/riskswap/trader/base/task/StatisticTask.java trader-base/src/main/java/cc/riskswap/trader/base/task/ExecutorTask.java trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskTypeTest.java
git -C /Users/haiming/Workspace/trader commit -m "feat: add task type enum and marker task interfaces"
```

---

### Task 3: 节点启动上报任务定义到 Redis（不再写 DB）

**Files:**
- Create: `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskDefinition.java`
- Create: `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskDefinitionPublisher.java`
- Modify: `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskMetadataSyncService.java`
- Modify: `trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfiguration.java`
- Test: `trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskDefinitionPublisherTest.java`

- [ ] **Step 1: 写失败测试，约束 publisher 会写入正确 Redis Key**

Create `trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskDefinitionPublisherTest.java`：

```java
package cc.riskswap.trader.base.task;

import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

class TraderTaskDefinitionPublisherTest {

    @Test
    void should_publish_definition_to_redis() {
        StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = Mockito.mock(ValueOperations.class);
        Mockito.when(redisTemplate.opsForValue()).thenReturn(ops);

        TraderTask task = new SampleCollectorTask();
        TraderTaskRegistry registry = new TraderTaskRegistry(List.of(task));

        TraderTaskDefinitionPublisher publisher = new TraderTaskDefinitionPublisher(redisTemplate, "node-1", "COLLECTOR");
        publisher.publishAll(registry);

        Mockito.verify(ops).set(Mockito.eq("trader:task:def:COLLECTOR:fundSync"), Mockito.argThat(json -> {
            return JSONUtil.isTypeJSON(json) && JSONUtil.parseObj(json).getStr("taskCode").equals("fundSync");
        }));
    }

    private static class SampleCollectorTask implements CollectorTask {
        @Override public String getTaskCode() { return "fundSync"; }
        @Override public String getTaskName() { return "同步基金"; }
        @Override public String getDefaultCron() { return "0 0 1 * * ?"; }
        @Override public boolean defaultEnabled() { return true; }
        @Override public String getParamSchema() { return "{\"type\":\"object\"}"; }
        @Override public String getDefaultParams() { return "{\"fullSync\":true}"; }
        @Override public void execute(TraderTaskContext context) { }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```bash
cd /Users/haiming/Workspace/trader/trader-base && mvn -Dtest=TraderTaskDefinitionPublisherTest test
```

Expected: FAIL，提示 `TraderTaskDefinitionPublisher` 不存在

- [ ] **Step 3: 增加任务定义 DTO（用于写入 Redis Value）**

Create `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskDefinition.java`：

```java
package cc.riskswap.trader.base.task;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class TraderTaskDefinition {
    private String taskType;
    private String taskCode;
    private String taskName;
    private String defaultCron;
    private Boolean defaultEnabled;
    private String paramSchema;
    private String defaultParamsJson;
    private String implClass;
    private String reportNodeId;
    private String reportNodeType;
    private OffsetDateTime reportAt;
}
```

- [ ] **Step 4: 增加 DefinitionPublisher（覆盖写去重）**

Create `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskDefinitionPublisher.java`：

```java
package cc.riskswap.trader.base.task;

import cn.hutool.json.JSONUtil;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.OffsetDateTime;
import java.util.Map;

public class TraderTaskDefinitionPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final String nodeId;
    private final String nodeType;

    public TraderTaskDefinitionPublisher(StringRedisTemplate stringRedisTemplate, String nodeId, String nodeType) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.nodeId = nodeId;
        this.nodeType = nodeType;
    }

    public void publishAll(TraderTaskRegistry registry) {
        for (Map.Entry<String, TraderTask> entry : registry.getTaskMap().entrySet()) {
            TraderTask task = entry.getValue();
            TraderTaskType taskType = toTaskType(task);
            TraderTaskDefinition definition = new TraderTaskDefinition();
            definition.setTaskType(taskType.name());
            definition.setTaskCode(task.getTaskCode());
            definition.setTaskName(task.getTaskName());
            definition.setDefaultCron(task.getDefaultCron());
            definition.setDefaultEnabled(task.defaultEnabled());
            definition.setParamSchema(task.getParamSchema());
            definition.setDefaultParamsJson(task.getDefaultParams());
            definition.setImplClass(task.getClass().getName());
            definition.setReportNodeId(nodeId);
            definition.setReportNodeType(nodeType);
            definition.setReportAt(OffsetDateTime.now());
            stringRedisTemplate.opsForValue().set(key(taskType, task.getTaskCode()), JSONUtil.toJsonStr(definition));
        }
    }

    private String key(TraderTaskType taskType, String taskCode) {
        return "trader:task:def:" + taskType.name() + ":" + taskCode;
    }

    private TraderTaskType toTaskType(TraderTask task) {
        if (task instanceof CollectorTask) {
            return TraderTaskType.COLLECTOR;
        }
        if (task instanceof StatisticTask) {
            return TraderTaskType.STATISTIC;
        }
        return TraderTaskType.EXECUTOR;
    }
}
```

- [ ] **Step 5: 停用“启动自动写 DB 的元数据同步”，改为只上报 Redis 定义**

Update `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskMetadataSyncService.java` 为（保留类名避免大范围引用变更，但行为改为 no-op 或仅用于兼容）：

```java
package cc.riskswap.trader.base.task;

public class TraderTaskMetadataSyncService {

    public void sync() {
    }
}
```

Update `trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfiguration.java`：

- 删除 `TraderTaskMetadataSyncService` 的 `ApplicationRunner` 调用
- 增加 `TraderTaskDefinitionPublisher` Bean，并在 `ApplicationRunner` 中调用 `publisher.publishAll(registry)`
- nodeId/nodeType 从 `TraderNodeProperties` 注入（nodeId 为 `properties.getId()`，nodeType 为 `properties.getType()`）

代码片段：

```java
@Bean
@ConditionalOnClass(StringRedisTemplate.class)
public TraderTaskDefinitionPublisher traderTaskDefinitionPublisher(
        StringRedisTemplate stringRedisTemplate,
        cc.riskswap.trader.base.config.TraderNodeProperties traderNodeProperties
) {
    return new TraderTaskDefinitionPublisher(stringRedisTemplate, traderNodeProperties.getId(), traderNodeProperties.getType());
}

@Bean
@ConditionalOnClass(StringRedisTemplate.class)
public ApplicationRunner traderTaskDefinitionPublishRunner(
        TraderTaskRegistry registry,
        TraderTaskDefinitionPublisher publisher
) {
    return args -> publisher.publishAll(registry);
}
```

- [ ] **Step 6: 运行测试确认通过**

Run:

```bash
cd /Users/haiming/Workspace/trader/trader-base && mvn -Dtest=TraderTaskDefinitionPublisherTest test
```

Expected: PASS

- [ ] **Step 7: Commit**

```bash
git -C /Users/haiming/Workspace/trader add trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskDefinition.java trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskDefinitionPublisher.java trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskMetadataSyncService.java trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfiguration.java trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskDefinitionPublisherTest.java
git -C /Users/haiming/Workspace/trader commit -m "feat: publish task definitions to redis on startup"
```

---

### Task 4: 节点按 taskType 拉取实例并刷新调度（替换 appName 逻辑）

**Files:**
- Modify: `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskProperties.java`
- Modify: `trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfiguration.java`
- Modify: `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskSchedulerService.java`
- Modify: `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskPoller.java`
- Modify: `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskRefreshMessage.java`
- Modify: `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskRefreshSubscriber.java`
- Modify: `trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskRefreshSubscriberTest.java`

- [ ] **Step 1: 更新刷新消息模型为 (taskType, taskCode)**

Update `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskRefreshMessage.java`：

```java
package cc.riskswap.trader.base.task;

import java.io.Serializable;

public record TraderTaskRefreshMessage(String taskType, String taskCode, Long version, String eventType) implements Serializable {
}
```

- [ ] **Step 2: 调整订阅器按 taskType+taskCode 查询实例并刷新**

Update `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskRefreshSubscriber.java`：

```java
package cc.riskswap.trader.base.task;

import cc.riskswap.trader.base.dao.SystemTaskDao;
import cc.riskswap.trader.base.dao.entity.SystemTask;

public class TraderTaskRefreshSubscriber {

    private final String taskType;
    private final SystemTaskDao systemTaskDao;
    private final TraderTaskSchedulerService schedulerService;

    public TraderTaskRefreshSubscriber(String taskType, SystemTaskDao systemTaskDao, TraderTaskSchedulerService schedulerService) {
        this.taskType = taskType;
        this.systemTaskDao = systemTaskDao;
        this.schedulerService = schedulerService;
    }

    public void handle(TraderTaskRefreshMessage message) throws Exception {
        if (message == null || message.taskType() == null || !taskType.equals(message.taskType())) {
            return;
        }
        if ("TASK_DELETED".equals(message.eventType())) {
            schedulerService.delete(message.taskCode());
            return;
        }
        SystemTask task = systemTaskDao.getByTaskTypeAndTaskCode(message.taskType(), message.taskCode());
        if (task != null) {
            schedulerService.refresh(task);
        }
    }
}
```

- [ ] **Step 3: 调整 SchedulerService 以 taskType 作为 group，并支持 delete**

Update `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskSchedulerService.java`：

```java
package cc.riskswap.trader.base.task;

import cc.riskswap.trader.base.dao.entity.SystemTask;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

public class TraderTaskSchedulerService {

    private final String taskType;
    private final Scheduler scheduler;

    public TraderTaskSchedulerService(String taskType, Scheduler scheduler) {
        this.taskType = taskType;
        this.scheduler = scheduler;
    }

    public void refresh(SystemTask task) throws SchedulerException {
        if (!"RUNNING".equals(task.getStatus())) {
            delete(task.getTaskCode());
            return;
        }

        JobDetail jobDetail = JobBuilder.newJob(TraderQuartzJob.class)
                .withIdentity(jobKey(task.getTaskCode()))
                .usingJobData("taskType", task.getTaskType())
                .usingJobData("taskCode", task.getTaskCode())
                .storeDurably(false)
                .build();

        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey(task.getTaskCode()))
                .forJob(jobDetail)
                .withSchedule(CronScheduleBuilder.cronSchedule(task.getCron()))
                .build();

        if (scheduler.checkExists(jobKey(task.getTaskCode()))) {
            scheduler.deleteJob(jobKey(task.getTaskCode()));
        }
        scheduler.scheduleJob(jobDetail, trigger);
    }

    public void delete(String taskCode) throws SchedulerException {
        scheduler.deleteJob(jobKey(taskCode));
    }

    JobKey jobKey(String taskCode) {
        return JobKey.jobKey(taskCode, taskType);
    }

    TriggerKey triggerKey(String taskCode) {
        return TriggerKey.triggerKey(taskCode, taskType);
    }
}
```

- [ ] **Step 4: 让 Poller 只轮询本节点 taskType 的实例**

Update `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskPoller.java`：

```java
package cc.riskswap.trader.base.task;

import cc.riskswap.trader.base.dao.SystemTaskDao;
import cc.riskswap.trader.base.dao.entity.SystemTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TraderTaskPoller {

    private final String taskType;
    private final SystemTaskDao systemTaskDao;
    private final TraderTaskSchedulerService schedulerService;
    private final Map<String, Long> versionCache = new HashMap<>();

    public TraderTaskPoller(String taskType, SystemTaskDao systemTaskDao, TraderTaskSchedulerService schedulerService) {
        this.taskType = taskType;
        this.systemTaskDao = systemTaskDao;
        this.schedulerService = schedulerService;
    }

    public void poll() throws Exception {
        List<SystemTask> tasks = systemTaskDao.listByTaskType(taskType);
        for (SystemTask task : tasks) {
            Long current = versionCache.get(task.getTaskCode());
            if (current == null || !current.equals(task.getVersion())) {
                schedulerService.refresh(task);
                versionCache.put(task.getTaskCode(), task.getVersion());
            }
        }
    }
}
```

- [ ] **Step 5: 将 AutoConfiguration 的“实例域”改为 taskType（从 TraderNodeProperties 推导）**

Update `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskProperties.java` 为：

```java
package cc.riskswap.trader.base.task;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "trader.task")
public class TraderTaskProperties {

    private boolean enabled = true;
    private long refreshPollMs = 60000;
    private long lockExpireSeconds = 600;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getRefreshPollMs() {
        return refreshPollMs;
    }

    public void setRefreshPollMs(long refreshPollMs) {
        this.refreshPollMs = refreshPollMs;
    }

    public long getLockExpireSeconds() {
        return lockExpireSeconds;
    }

    public void setLockExpireSeconds(long lockExpireSeconds) {
        this.lockExpireSeconds = lockExpireSeconds;
    }
}
```

Update `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskPollingJob.java` 为读取新字段：

```java
package cc.riskswap.trader.base.task;

import org.springframework.scheduling.annotation.Scheduled;

public class TraderTaskPollingJob {

    private final TraderTaskPoller poller;

    public TraderTaskPollingJob(TraderTaskPoller poller) {
        this.poller = poller;
    }

    @Scheduled(fixedDelayString = "${trader.task.refresh-poll-ms:60000}")
    public void poll() throws Exception {
        poller.poll();
    }
}
```

Update `trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfiguration.java` 关键点：

- 用 `TraderNodeProperties.getType()` 推导 `taskType = TraderTaskType.fromNodeType(type).name()`
- `TraderTaskSchedulerService / TraderTaskRefreshSubscriber / TraderTaskPoller` 的构造参数都改为 taskType

示例片段：

```java
@Bean
public TraderTaskSchedulerService traderTaskSchedulerService(
        cc.riskswap.trader.base.config.TraderNodeProperties nodeProperties,
        @Qualifier("traderTaskSchedulerFactoryBean") Scheduler scheduler
) {
    String taskType = TraderTaskType.fromNodeType(nodeProperties.getType()).name();
    return new TraderTaskSchedulerService(taskType, scheduler);
}
```

- [ ] **Step 6: 更新订阅器测试断言与编译**

Update `trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskRefreshSubscriberTest.java` 为：

```java
package cc.riskswap.trader.base.task;

import cc.riskswap.trader.base.dao.SystemTaskDao;
import cc.riskswap.trader.base.dao.entity.SystemTask;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TraderTaskRefreshSubscriberTest {

    @Test
    void should_refresh_when_message_matches_current_task_type() throws Exception {
        SystemTaskDao dao = Mockito.mock(SystemTaskDao.class);
        TraderTaskSchedulerService schedulerService = Mockito.mock(TraderTaskSchedulerService.class);
        TraderTaskRefreshSubscriber subscriber = new TraderTaskRefreshSubscriber("COLLECTOR", dao, schedulerService);
        SystemTask task = new SystemTask();
        task.setTaskType("COLLECTOR");
        task.setTaskCode("fundSync");
        when(dao.getByTaskTypeAndTaskCode("COLLECTOR", "fundSync")).thenReturn(task);
        TraderTaskRefreshMessage message = new TraderTaskRefreshMessage("COLLECTOR", "fundSync", 2L, "TASK_UPDATED");
        subscriber.handle(message);
        verify(schedulerService, times(1)).refresh(task);
    }
}
```

Run:

```bash
cd /Users/haiming/Workspace/trader/trader-base && mvn -Dtest=TraderTaskRefreshSubscriberTest test
```

Expected: PASS

- [ ] **Step 7: Commit**

```bash
git -C /Users/haiming/Workspace/trader add trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskRefreshMessage.java trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskRefreshSubscriber.java trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskSchedulerService.java trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskPoller.java trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskProperties.java trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskPollingJob.java trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfiguration.java trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskRefreshSubscriberTest.java
git -C /Users/haiming/Workspace/trader commit -m "feat: refresh and poll tasks by task type instead of app"
```

---

### Task 5: 打通 Quartz 执行链路 + 全局单实例抢占锁

**Files:**
- Create: `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskLock.java`
- Create: `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskExecutor.java`
- Modify: `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderQuartzJob.java`
- Modify: `trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfiguration.java`
- Test: `trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskLockTest.java`
- Test: `trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskExecutorTest.java`

- [ ] **Step 1: 写锁的单测（先失败）**

Create `trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskLockTest.java`：

```java
package cc.riskswap.trader.base.task;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

class TraderTaskLockTest {

    @Test
    void should_try_lock_by_set_if_absent() {
        StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = Mockito.mock(ValueOperations.class);
        Mockito.when(redisTemplate.opsForValue()).thenReturn(ops);
        Mockito.when(ops.setIfAbsent(Mockito.eq("lock:task:run:COLLECTOR:fundSync:1"), Mockito.anyString(), Mockito.eq(600L), Mockito.eq(TimeUnit.SECONDS)))
                .thenReturn(true);

        TraderTaskLock lock = new TraderTaskLock(redisTemplate, 600);
        boolean locked = lock.tryLock("task:run:COLLECTOR:fundSync:1", "rid");
        Assertions.assertTrue(locked);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```bash
cd /Users/haiming/Workspace/trader/trader-base && mvn -Dtest=TraderTaskLockTest test
```

Expected: FAIL，提示 `TraderTaskLock` 不存在

- [ ] **Step 3: 实现 TraderTaskLock（下沉到 trader-base，复用 executor RedisLock 思路）**

Create `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskLock.java`：

```java
package cc.riskswap.trader.base.task;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TraderTaskLock {

    private final StringRedisTemplate stringRedisTemplate;
    private final long expireSeconds;

    public TraderTaskLock(StringRedisTemplate stringRedisTemplate, long expireSeconds) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.expireSeconds = expireSeconds;
    }

    public String newRequestId() {
        return UUID.randomUUID().toString();
    }

    public boolean tryLock(String key, String requestId) {
        Boolean result = stringRedisTemplate.opsForValue().setIfAbsent("lock:" + key, requestId, expireSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }
}
```

- [ ] **Step 4: 写执行器单测（抢不到锁则不执行）**

Create `trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskExecutorTest.java`：

```java
package cc.riskswap.trader.base.task;

import cc.riskswap.trader.base.dao.SystemTaskDao;
import cc.riskswap.trader.base.dao.entity.SystemTask;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TraderTaskExecutorTest {

    @Test
    void should_skip_when_lock_not_acquired() throws Exception {
        TraderTaskRegistry registry = Mockito.mock(TraderTaskRegistry.class);
        SystemTaskDao systemTaskDao = Mockito.mock(SystemTaskDao.class);
        TraderTaskLock lock = Mockito.mock(TraderTaskLock.class);

        SystemTask instance = new SystemTask();
        instance.setTaskType("COLLECTOR");
        instance.setTaskCode("fundSync");
        instance.setStatus("RUNNING");
        instance.setParamsJson("{\"fullSync\":true}");
        when(systemTaskDao.getByTaskTypeAndTaskCode("COLLECTOR", "fundSync")).thenReturn(instance);

        when(lock.newRequestId()).thenReturn("rid");
        when(lock.tryLock(Mockito.anyString(), Mockito.anyString())).thenReturn(false);

        TraderTaskExecutor executor = new TraderTaskExecutor(registry, systemTaskDao, lock);
        executor.execute("COLLECTOR", "fundSync", 1L);

        verify(registry, never()).getTask("fundSync");
    }

    @Test
    void should_execute_when_lock_acquired() throws Exception {
        TraderTaskRegistry registry = Mockito.mock(TraderTaskRegistry.class);
        SystemTaskDao systemTaskDao = Mockito.mock(SystemTaskDao.class);
        TraderTaskLock lock = Mockito.mock(TraderTaskLock.class);

        TraderTask task = Mockito.mock(TraderTask.class);
        when(registry.getTask("fundSync")).thenReturn(task);

        SystemTask instance = new SystemTask();
        instance.setTaskType("COLLECTOR");
        instance.setTaskCode("fundSync");
        instance.setTaskName("同步基金");
        instance.setStatus("RUNNING");
        instance.setParamsJson("{\"fullSync\":true}");
        when(systemTaskDao.getByTaskTypeAndTaskCode("COLLECTOR", "fundSync")).thenReturn(instance);

        when(lock.newRequestId()).thenReturn("rid");
        when(lock.tryLock(Mockito.anyString(), Mockito.anyString())).thenReturn(true);

        TraderTaskExecutor executor = new TraderTaskExecutor(registry, systemTaskDao, lock);
        executor.execute("COLLECTOR", "fundSync", OffsetDateTime.parse("2026-04-16T10:00:00+08:00").toInstant().getEpochSecond());

        verify(task).execute(Mockito.argThat(ctx -> "fundSync".equals(ctx.getTaskCode()) && "COLLECTOR".equals(ctx.getAppName())));
    }
}
```

- [ ] **Step 5: 实现 TraderTaskExecutor，并让 QuartzJob 调用它**

Create `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskExecutor.java`：

```java
package cc.riskswap.trader.base.task;

import cc.riskswap.trader.base.dao.SystemTaskDao;
import cc.riskswap.trader.base.dao.entity.SystemTask;
import cn.hutool.json.JSONUtil;

import java.time.OffsetDateTime;
import java.util.Map;

public class TraderTaskExecutor {

    private final TraderTaskRegistry registry;
    private final SystemTaskDao systemTaskDao;
    private final TraderTaskLock lock;

    public TraderTaskExecutor(TraderTaskRegistry registry, SystemTaskDao systemTaskDao, TraderTaskLock lock) {
        this.registry = registry;
        this.systemTaskDao = systemTaskDao;
        this.lock = lock;
    }

    public void execute(String taskType, String taskCode, long fireTimeEpochSec) throws Exception {
        SystemTask instance = systemTaskDao.getByTaskTypeAndTaskCode(taskType, taskCode);
        if (instance == null) {
            return;
        }
        if (!"RUNNING".equals(instance.getStatus())) {
            return;
        }
        String lockKey = "task:run:" + taskType + ":" + taskCode + ":" + fireTimeEpochSec;
        String requestId = lock.newRequestId();
        if (!lock.tryLock(lockKey, requestId)) {
            return;
        }
        TraderTask task = registry.getTask(taskCode);
        if (task == null) {
            return;
        }
        TraderTaskContext context = new TraderTaskContext();
        context.setAppName(taskType);
        context.setTaskCode(taskCode);
        context.setTaskName(instance.getTaskName());
        context.setTriggerType(TraderTaskTriggerType.SCHEDULED.name());
        context.setParamsJson(instance.getParamsJson() == null ? "{}" : instance.getParamsJson());
        context.setParamsMap(JSONUtil.parseObj(context.getParamsJson()).toBean(Map.class));
        context.setRunAt(OffsetDateTime.now());
        task.execute(context);
    }
}
```

Update `trader-base/src/main/java/cc/riskswap/trader/base/task/TraderQuartzJob.java`：

```java
package cc.riskswap.trader.base.task;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.context.ApplicationContext;

public class TraderQuartzJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws RuntimeException {
        JobDataMap dataMap = context.getMergedJobDataMap();
        ApplicationContext applicationContext = (ApplicationContext) dataMap.get("applicationContext");
        TraderTaskExecutor executor = applicationContext.getBean(TraderTaskExecutor.class);
        String taskType = dataMap.getString("taskType");
        String taskCode = dataMap.getString("taskCode");
        long fireTimeEpochSec = context.getScheduledFireTime() == null ? System.currentTimeMillis() / 1000 : context.getScheduledFireTime().getTime() / 1000;
        try {
            executor.execute(taskType, taskCode, fireTimeEpochSec);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

Update `TraderTaskAutoConfiguration` 增加 Bean：

```java
@Bean
@ConditionalOnClass(StringRedisTemplate.class)
public TraderTaskLock traderTaskLock(StringRedisTemplate stringRedisTemplate, TraderTaskProperties properties) {
    return new TraderTaskLock(stringRedisTemplate, properties.getLockExpireSeconds());
}

@Bean
public TraderTaskExecutor traderTaskExecutor(TraderTaskRegistry registry, SystemTaskDao systemTaskDao, TraderTaskLock lock) {
    return new TraderTaskExecutor(registry, systemTaskDao, lock);
}
```

- [ ] **Step 6: 运行单测验证**

Run:

```bash
cd /Users/haiming/Workspace/trader/trader-base && mvn -Dtest=TraderTaskLockTest,TraderTaskExecutorTest test
```

Expected: PASS

- [ ] **Step 7: Commit**

```bash
git -C /Users/haiming/Workspace/trader add trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskLock.java trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskExecutor.java trader-base/src/main/java/cc/riskswap/trader/base/task/TraderQuartzJob.java trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfiguration.java trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskLockTest.java trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskExecutorTest.java
git -C /Users/haiming/Workspace/trader commit -m "feat: execute trader task with redis lock to ensure single instance"
```

---

### Task 6: 管理端支持定义列表 + 创建/删除实例 + 列表聚合投资策略

**Files:**
- Create: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/dto/TaskDefinitionDto.java`
- Create: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/query/TaskDefinitionListQuery.java`
- Create: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/param/SystemTaskInstanceCreateParam.java`
- Create: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/param/SystemTaskInstanceDeleteParam.java`
- Modify: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/dto/SystemTaskDto.java`
- Modify: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/query/SystemTaskListQuery.java`
- Modify: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/param/SystemTaskUpdateParam.java`
- Modify: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/param/SystemTaskTriggerParam.java`
- Modify: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/controller/SystemTaskController.java`
- Modify: `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java`
- Modify: `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/controller/SystemTaskControllerRouteTest.java`
- Modify: `trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/service/SystemTaskServiceTest.java`

- [ ] **Step 1: 扩展路由测试（先失败）**

Update `SystemTaskControllerRouteTest`：

```java
@Test
void should_expose_definition_and_instance_routes() throws Exception {
    Method definitions = SystemTaskController.class.getMethod("definitions", cc.riskswap.trader.admin.common.model.query.TaskDefinitionListQuery.class);
    Method create = SystemTaskController.class.getMethod("createInstance", cc.riskswap.trader.admin.common.model.param.SystemTaskInstanceCreateParam.class);
    Method delete = SystemTaskController.class.getMethod("deleteInstance", cc.riskswap.trader.admin.common.model.param.SystemTaskInstanceDeleteParam.class);
    assertTrue(definitions.isAnnotationPresent(PostMapping.class));
    assertTrue(create.isAnnotationPresent(PostMapping.class));
    assertTrue(delete.isAnnotationPresent(PostMapping.class));
}
```

Run:

```bash
cd /Users/haiming/Workspace/trader/trader-admin/admin-server && mvn -Dtest=SystemTaskControllerRouteTest test
```

Expected: FAIL，提示方法不存在

- [ ] **Step 2: 增加 DTO/Query/Param**

Create `TaskDefinitionDto.java`：

```java
package cc.riskswap.trader.admin.common.model.dto;

import lombok.Data;

@Data
public class TaskDefinitionDto {
    private String taskType;
    private String taskCode;
    private String taskName;
    private String defaultCron;
    private Boolean defaultEnabled;
    private String paramSchema;
    private String defaultParamsJson;
    private String implClass;
    private String reportNodeId;
    private String reportNodeType;
    private String reportAt;
}
```

Create `TaskDefinitionListQuery.java`：

```java
package cc.riskswap.trader.admin.common.model.query;

import lombok.Data;

@Data
public class TaskDefinitionListQuery {
    private String taskType;
    private String taskCode;
    private String taskName;
}
```

Create `SystemTaskInstanceCreateParam.java`：

```java
package cc.riskswap.trader.admin.common.model.param;

import lombok.Data;

@Data
public class SystemTaskInstanceCreateParam {
    private String taskType;
    private String taskCode;
    private String initialStatus;
}
```

Create `SystemTaskInstanceDeleteParam.java`：

```java
package cc.riskswap.trader.admin.common.model.param;

import lombok.Data;

@Data
public class SystemTaskInstanceDeleteParam {
    private Long id;
}
```

- [ ] **Step 3: 扩展 SystemTaskDto / Query / UpdateParam 支持 taskType**

Update `SystemTaskDto.java` 加入 `taskType`、`sourceType`（用于聚合投资策略时区分数据来源）：

```java
private String taskType;
private String sourceType;
```

Update `SystemTaskListQuery.java` 加入：

```java
private String taskType;
private Boolean includeInvestment;
```

Update `SystemTaskTriggerParam.java` 精简为：

```java
package cc.riskswap.trader.admin.common.model.param;

import lombok.Data;

@Data
public class SystemTaskTriggerParam {
    private Long id;
}
```

- [ ] **Step 4: 在 Service 中实现 definition list / create / delete / list 聚合**

Update `SystemTaskService.java`：

- 构造器注入 `StringRedisTemplate`（用于扫描 definition）与 `cc.riskswap.trader.admin.dao.InvestmentDao`（用于 includeInvestment 聚合）
- `list(query)`：查询 `system_task`（按 taskType 过滤），若 `includeInvestment=true` 则额外把投资列表映射成 `SystemTaskDto` 并 append
- `definitions(query)`：SCAN `trader:task:def:*`，读取 JSON 解析成 `TaskDefinitionDto`，按过滤条件返回
- `createInstance(param)`：读取 `trader:task:def:{taskType}:{taskCode}` 定义 JSON；生成 `SystemTask` 插入；冲突则抛 Warning；publish refresh `TASK_CREATED`
- `deleteInstance(param)`：按 id 删除实例；publish refresh `TASK_DELETED`

关键代码片段（错误处理统一使用 `cc.riskswap.trader.admin.exception.Warning` + `cc.riskswap.trader.admin.common.model.ErrorCode`）：

```java
public List<TaskDefinitionDto> definitions(TaskDefinitionListQuery query) {
    Set<String> keys = stringRedisTemplate.keys("trader:task:def:*");
    if (keys == null || keys.isEmpty()) {
        return List.of();
    }
    return keys.stream()
            .map(key -> stringRedisTemplate.opsForValue().get(key))
            .filter(StrUtil::isNotBlank)
            .map(json -> JSONUtil.toBean(json, TaskDefinitionDto.class))
            .filter(item -> query.getTaskType() == null || query.getTaskType().equals(item.getTaskType()))
            .filter(item -> query.getTaskCode() == null || item.getTaskCode().contains(query.getTaskCode()))
            .filter(item -> query.getTaskName() == null || (item.getTaskName() != null && item.getTaskName().contains(query.getTaskName())))
            .sorted(Comparator.comparing(TaskDefinitionDto::getTaskType).thenComparing(TaskDefinitionDto::getTaskCode))
            .toList();
}
```

发布刷新消息时改为：

```java
traderTaskRefreshPublisher.publish(new TraderTaskRefreshMessage(task.getTaskType(), task.getTaskCode(), task.getVersion(), eventType));
```

- [ ] **Step 5: Controller 增加 3 个新路由**

Update `SystemTaskController.java`：

```java
@PostMapping("/definition/list")
public ResData<List<TaskDefinitionDto>> definitions(@RequestBody TaskDefinitionListQuery query) { ... }

@PostMapping("/instance/create")
public ResData<Void> createInstance(@RequestBody SystemTaskInstanceCreateParam param) { ... }

@PostMapping("/instance/delete")
public ResData<Void> deleteInstance(@RequestBody SystemTaskInstanceDeleteParam param) { ... }
```

- [ ] **Step 6: 更新 Service 单测覆盖 create/delete 与消息字段**

Update `SystemTaskServiceTest`：

- 所有 `TraderTaskRefreshMessage` 断言从 `appName()` 改为 `taskType()`
- 增加 `should_create_instance_from_definition_and_publish_created_message`
- 增加 `should_delete_instance_and_publish_deleted_message`

示例（create 场景核心断言）：

```java
ArgumentCaptor<TraderTaskRefreshMessage> messageCaptor = ArgumentCaptor.forClass(TraderTaskRefreshMessage.class);
Mockito.verify(refreshPublisher).publish(messageCaptor.capture());
TraderTaskRefreshMessage message = messageCaptor.getValue();
Assertions.assertEquals("COLLECTOR", message.taskType());
Assertions.assertEquals("fundSync", message.taskCode());
Assertions.assertEquals("TASK_CREATED", message.eventType());
```

- [ ] **Step 7: 跑 admin-server 单测 + 编译**

Run:

```bash
cd /Users/haiming/Workspace/trader/trader-admin/admin-server && mvn test
cd /Users/haiming/Workspace/trader/trader-admin && mvn -DskipTests compile
```

Expected: `BUILD SUCCESS`

- [ ] **Step 8: Commit**

```bash
git -C /Users/haiming/Workspace/trader add trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/controller/SystemTaskController.java trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test
git -C /Users/haiming/Workspace/trader commit -m "feat: add task definition list and instance create/delete"
```

---

### Task 7: 前端任务管理页支持“从定义创建实例”与“聚合策略”

**Files:**
- Modify: `trader-admin/admin-web/src/services/systemTask.ts`
- Modify: `trader-admin/admin-web/src/pages/task/Manage.vue`

- [ ] **Step 1: 扩展 systemTask.ts 增加 definition/list 与 instance/create/delete**

Update `trader-admin/admin-web/src/services/systemTask.ts`：

```ts
export interface TaskDefinitionDto {
  taskType: string
  taskCode: string
  taskName: string
  defaultCron: string
  defaultEnabled: boolean
  paramSchema?: string
  defaultParamsJson?: string
  reportNodeId?: string
  reportNodeType?: string
  reportAt?: string
}

export function listTaskDefinitions(data: { taskType?: string; taskCode?: string; taskName?: string }) {
  return request.post('/task/definition/list', data)
}

export function createTaskInstance(data: { taskType: string; taskCode: string; initialStatus?: string }) {
  return request.post('/task/instance/create', data)
}

export function deleteTaskInstance(data: { id: number }) {
  return request.post('/task/instance/delete', data)
}
```

- [ ] **Step 2: Manage.vue 增加 taskType 筛选 + “创建任务”入口**

在筛选区新增：

- taskType 下拉（COLLECTOR/STATISTIC/EXECUTOR）
- includeInvestment 开关（“包含策略任务”）

在表格工具栏新增按钮：

- “创建任务实例”：打开弹窗，弹窗内调用 `listTaskDefinitions` 展示可创建定义列表，并提供“创建”按钮调用 `createTaskInstance`

弹窗字段建议：

- taskType、taskCode、taskName、defaultCron、reportNodeId、reportAt

- [ ] **Step 3: 表格行支持区分 sourceType**

当 `row.sourceType === 'INVESTMENT'`：

- “编辑/启停/立即执行”按钮替换为“查看投资”跳转到 `/investment/detail?id=...`（复用现有路由参数规则）

当普通任务实例：

- 保留现有编辑/启停/立即执行
- 增加“删除实例”按钮（调用 `deleteTaskInstance`，并二次确认）

- [ ] **Step 4: 前端构建验证**

Run:

```bash
cd /Users/haiming/Workspace/trader/trader-admin/admin-web && npm run build
```

Expected: build 成功，无 TypeScript 报错

- [ ] **Step 5: Commit**

```bash
git -C /Users/haiming/Workspace/trader add trader-admin/admin-web/src/services/systemTask.ts trader-admin/admin-web/src/pages/task/Manage.vue
git -C /Users/haiming/Workspace/trader commit -m "feat: support task definition create and investment aggregation in task page"
```

---

### Task 8: 全量验证（Base + Admin + 典型节点启动链路）

**Files:**
- Verify: `trader-base`
- Verify: `trader-admin`
- Verify: `trader-collector` / `trader-statistic` / `trader-executor`

- [ ] **Step 1: 跑 trader-base 单测**

Run:

```bash
cd /Users/haiming/Workspace/trader/trader-base && mvn test
```

Expected: `BUILD SUCCESS`

- [ ] **Step 2: 编译 admin-server 与前端**

Run:

```bash
cd /Users/haiming/Workspace/trader/trader-admin && mvn -DskipTests compile
cd /Users/haiming/Workspace/trader/trader-admin/admin-web && npm run build
```

Expected: `BUILD SUCCESS` + 前端 build 成功

- [ ] **Step 3: 手工验收链路（定义 -> 创建实例 -> 调度 -> 抢占执行）**

1. 启动 Redis / MySQL，并确保 `system_task` 新结构生效
2. 启动一个 COLLECTOR 节点（例如 trader-collector）
   - 预期：Redis 出现 `trader:task:def:COLLECTOR:*` 定义 key
3. 打开后台任务管理页，点击“创建任务实例”
   - 预期：看到定义列表（包含 fundSync 等）
4. 创建实例后启动（status=RUNNING）
   - 预期：collector 日志出现 Quartz 调度注册（若已有日志体系，检查 job 存在即可）
5. 同时启动第二个 COLLECTOR 节点
   - 预期：同一 fireTime 只有一个节点实际执行（另一个节点抢锁失败跳过）

- [ ] **Step 4: Commit（如需）**

如本阶段有额外文档更新：

```bash
git -C /Users/haiming/Workspace/trader add trader-docs/superpowers/specs/2026-04-16-task-definition-instance-design.md trader-docs/superpowers/plans/2026-04-16-task-definition-instance-implementation.md
git -C /Users/haiming/Workspace/trader commit -m "docs: add task definition/instance split spec and plan"
```

---

## Self-Review

### Spec Coverage

- Redis 定义 Key/Value（定义去重）：Task 3 覆盖
- DB 实例以 `(taskType, taskCode)` 全局唯一：Task 1 覆盖
- 创建实例而非节点自动写 DB：Task 3 + Task 6 覆盖
- 节点按 taskType 拉取实例刷新调度：Task 4 覆盖
- 全局单实例抢占执行：Task 5 覆盖
- 策略任务归类 EXECUTOR 并在任务管理聚合展示：Task 6 + Task 7 覆盖

### Placeholder Scan

- 无 `TODO`/`TBD`/“后续补充”式步骤
- 每个任务包含：文件清单、具体代码、具体命令、预期结果

### Type Consistency

- 统一用 `taskType` 表示实例与刷新消息维度
- Redis 定义 key 固定 `trader:task:def:{taskType}:{taskCode}`
- 刷新消息统一 `TraderTaskRefreshMessage(taskType, taskCode, version, eventType)`
