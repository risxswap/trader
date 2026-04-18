# Task Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `trader-base` 中实现统一任务调度抽象，并让 `trader-admin` 可管理任务启停、Cron 与参数配置，业务项目通过实现统一接口接入任务。

**Architecture:** 以 `trader-base` 作为任务基础设施模块，提供任务接口、数据库模型、Quartz 调度、Redis Pub/Sub 通知与轮询兜底；`trader-admin` 提供配置管理接口与页面；`trader-collector` 迁移现有任务作为样板接入。所有任务配置以数据库为准，Redis 仅作为即时刷新触发信号。

**Tech Stack:** Spring Boot 3、MyBatis-Plus、Quartz Scheduler、Redis Pub/Sub、Vue 3、Element Plus

---

## File Map

### `trader-base` 预计新增文件

- `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTask.java`
- `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskContext.java`
- `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskTriggerType.java`
- `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskRegistry.java`
- `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskProperties.java`
- `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderQuartzJob.java`
- `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskSchedulerService.java`
- `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskMetadataSyncService.java`
- `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskRefreshPublisher.java`
- `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskRefreshSubscriber.java`
- `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskRefreshMessage.java`
- `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskPoller.java`
- `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfiguration.java`
- `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/entity/SystemTask.java`
- `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/entity/SystemTaskRunLog.java`
- `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/mapper/SystemTaskMapper.java`
- `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/mapper/SystemTaskRunLogMapper.java`
- `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/SystemTaskDao.java`
- `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/SystemTaskRunLogDao.java`
- `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/query/SystemTaskListQuery.java`
- `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/query/SystemTaskRunLogListQuery.java`
- `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/param/SystemTaskUpdateParam.java`
- `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/param/SystemTaskTriggerParam.java`
- `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/dto/SystemTaskDto.java`
- `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/dto/SystemTaskRunLogDto.java`

### `trader-base` 预计修改文件

- `/Users/haiming/Workspace/trader/trader-base/pom.xml`
- `/Users/haiming/Workspace/trader/trader-base/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderBaseAutoConfiguration.java`
- `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderRedisAutoConfiguration.java`

### `admin-server` 预计新增文件

- `/Users/haiming/Workspace/trader/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/dto/SystemTaskDto.java`
- `/Users/haiming/Workspace/trader/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/dto/SystemTaskRunLogDto.java`
- `/Users/haiming/Workspace/trader/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/query/SystemTaskListQuery.java`
- `/Users/haiming/Workspace/trader/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/param/SystemTaskUpdateParam.java`
- `/Users/haiming/Workspace/trader/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/param/SystemTaskTriggerParam.java`
- `/Users/haiming/Workspace/trader/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java`
- `/Users/haiming/Workspace/trader/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/controller/SystemTaskController.java`

### `admin-server` 预计修改文件

- `/Users/haiming/Workspace/trader/trader-admin/admin-server/pom.xml`
- `/Users/haiming/Workspace/trader/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/ErrorCode.java`

### `admin-web` 预计新增文件

- `/Users/haiming/Workspace/trader/trader-admin/admin-web/src/services/systemTask.ts`
- `/Users/haiming/Workspace/trader/trader-admin/admin-web/src/pages/task/TaskManage.vue`

### `admin-web` 预计修改文件

- `/Users/haiming/Workspace/trader/trader-admin/admin-web/src/router/index.ts`
- `/Users/haiming/Workspace/trader/trader-admin/admin-web/src/layout/menu.ts`

### `trader-collector` 预计新增文件

- `/Users/haiming/Workspace/trader/trader-collector/src/main/java/cc/riskswap/trader/collector/task/FundSyncTask.java`
- `/Users/haiming/Workspace/trader/trader-collector/src/main/java/cc/riskswap/trader/collector/task/FundMarketSyncTask.java`
- `/Users/haiming/Workspace/trader/trader-collector/src/main/java/cc/riskswap/trader/collector/task/CalendarSyncTask.java`

### `trader-collector` 预计修改文件

- `/Users/haiming/Workspace/trader/trader-collector/pom.xml`
- `/Users/haiming/Workspace/trader/trader-collector/src/main/java/cc/riskswap/trader/collector/task/FundTask.java`
- `/Users/haiming/Workspace/trader/trader-collector/src/main/java/cc/riskswap/trader/collector/task/FundMarketTask.java`
- `/Users/haiming/Workspace/trader/trader-collector/src/main/java/cc/riskswap/trader/collector/task/CalendarTask.java`

### 测试文件

- `/Users/haiming/Workspace/trader/trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskRegistryTest.java`
- `/Users/haiming/Workspace/trader/trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskSchedulerServiceTest.java`
- `/Users/haiming/Workspace/trader/trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskRefreshSubscriberTest.java`
- `/Users/haiming/Workspace/trader/trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/controller/SystemTaskControllerRouteTest.java`

### 说明

- 第一阶段只在 `trader-collector` 迁移三个样板任务，不立即全量迁移所有现有任务。
- `trader-executor` 本阶段主要补充依赖可用性校验，不强制迁移具体任务实现。
- `system_task_run_log` 表结构先落地，后台页面第一阶段先聚焦任务配置页，不同时实现完整运行日志页替换。

---

### Task 1: 扩展 `trader-base` 依赖与自动配置入口

**Files:**
- Modify: `/Users/haiming/Workspace/trader/trader-base/pom.xml`
- Modify: `/Users/haiming/Workspace/trader/trader-base/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Create: `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfiguration.java`

- [ ] **Step 1: 先补充 `trader-base` 的 Quartz 依赖**

在 `/Users/haiming/Workspace/trader/trader-base/pom.xml` 的 `<dependencies>` 中加入：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-quartz</artifactId>
</dependency>
```

- [ ] **Step 2: 注册新的自动配置类**

把 `/Users/haiming/Workspace/trader/trader-base/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 改成：

```text
cc.riskswap.trader.base.autoconfigure.TraderBaseAutoConfiguration
cc.riskswap.trader.base.autoconfigure.TraderTaskAutoConfiguration
```

- [ ] **Step 3: 创建任务自动配置骨架**

新建 `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfiguration.java`：

```java
package cc.riskswap.trader.base.autoconfigure;

import cc.riskswap.trader.base.task.TraderTaskProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnClass(SchedulerFactoryBean.class)
@ConditionalOnProperty(prefix = "trader.task", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(TraderTaskProperties.class)
public class TraderTaskAutoConfiguration {
}
```

- [ ] **Step 4: 编译 `trader-base` 验证依赖和自动配置入口无误**

Run:

```bash
cd /Users/haiming/Workspace/trader/trader-base && mvn -DskipTests compile
```

Expected: `BUILD SUCCESS`

- [ ] **Step 5: 提交当前任务**

```bash
git -C /Users/haiming/Workspace/trader/trader-base add pom.xml src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports src/main/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfiguration.java
git -C /Users/haiming/Workspace/trader/trader-base commit -m "feat: add task module auto configuration entry"
```

---

### Task 2: 建立任务领域模型、DAO 与 SQL 脚本

**Files:**
- Create: `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/entity/SystemTask.java`
- Create: `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/entity/SystemTaskRunLog.java`
- Create: `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/mapper/SystemTaskMapper.java`
- Create: `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/mapper/SystemTaskRunLogMapper.java`
- Create: `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/SystemTaskDao.java`
- Create: `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/SystemTaskRunLogDao.java`
- Create: `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/query/SystemTaskListQuery.java`
- Create: `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/query/SystemTaskRunLogListQuery.java`
- Create: `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/param/SystemTaskUpdateParam.java`
- Create: `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/param/SystemTaskTriggerParam.java`
- Create: `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/dto/SystemTaskDto.java`
- Create: `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/dto/SystemTaskRunLogDto.java`
- Create: `/Users/haiming/Workspace/trader/trader-base/src/main/resources/sql/task-module-mysql.sql`

- [ ] **Step 1: 先写 MySQL 表结构脚本**

新建 `/Users/haiming/Workspace/trader/trader-base/src/main/resources/sql/task-module-mysql.sql`：

```sql
CREATE TABLE IF NOT EXISTS system_task (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    app_name VARCHAR(64) NOT NULL,
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
    UNIQUE KEY system_task_app_name_task_code_uidx (app_name, task_code),
    KEY system_task_app_name_idx (app_name),
    KEY system_task_status_idx (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS system_task_run_log (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    app_name VARCHAR(64) NOT NULL,
    task_code VARCHAR(128) NOT NULL,
    trigger_type VARCHAR(32) NOT NULL,
    params_json TEXT,
    status VARCHAR(32) NOT NULL,
    started_at DATETIME(6),
    finished_at DATETIME(6),
    duration_ms BIGINT,
    error_msg TEXT,
    trace_id VARCHAR(128),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY system_task_run_log_app_name_task_code_idx (app_name, task_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

- [ ] **Step 2: 按现有 MyBatis-Plus 风格创建实体与 Mapper**

在 `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/entity/SystemTask.java` 中写入：

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

在 `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/mapper/SystemTaskMapper.java` 中写入：

```java
package cc.riskswap.trader.base.dao.mapper;

import cc.riskswap.trader.base.dao.entity.SystemTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SystemTaskMapper extends BaseMapper<SystemTask> {
}
```

- [ ] **Step 3: 补齐 `SystemTaskRunLog` 对应类**

在 `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/entity/SystemTaskRunLog.java` 中写入：

```java
package cc.riskswap.trader.base.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("system_task_run_log")
public class SystemTaskRunLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String appName;
    private String taskCode;
    private String triggerType;
    private String paramsJson;
    private String status;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private Long durationMs;
    private String errorMsg;
    private String traceId;
    private OffsetDateTime createdAt;
}
```

- [ ] **Step 4: 实现 `SystemTaskDao` 的最小查询能力**

在 `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/dao/SystemTaskDao.java` 中写入：

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

    public SystemTask getByAppNameAndTaskCode(String appName, String taskCode) {
        LambdaQueryWrapper<SystemTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemTask::getAppName, appName);
        wrapper.eq(SystemTask::getTaskCode, taskCode);
        return this.getOne(wrapper);
    }

    public List<SystemTask> listByAppName(String appName) {
        LambdaQueryWrapper<SystemTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemTask::getAppName, appName);
        wrapper.orderByAsc(SystemTask::getTaskCode);
        return this.list(wrapper);
    }

    public Page<SystemTask> pageQuery(SystemTaskListQuery query) {
        Page<SystemTask> page = new Page<>(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<SystemTask> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getAppName())) {
            wrapper.eq(SystemTask::getAppName, query.getAppName());
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
        wrapper.orderByAsc(SystemTask::getAppName).orderByAsc(SystemTask::getTaskCode);
        return this.page(page, wrapper);
    }
}
```

- [ ] **Step 5: 编译 `trader-base` 验证领域模型可通过**

Run:

```bash
cd /Users/haiming/Workspace/trader/trader-base && mvn -DskipTests compile
```

Expected: `BUILD SUCCESS`

- [ ] **Step 6: 提交当前任务**

```bash
git -C /Users/haiming/Workspace/trader/trader-base add src/main/resources/sql/task-module-mysql.sql src/main/java/cc/riskswap/trader/base/dao/entity/SystemTask.java src/main/java/cc/riskswap/trader/base/dao/entity/SystemTaskRunLog.java src/main/java/cc/riskswap/trader/base/dao/mapper/SystemTaskMapper.java src/main/java/cc/riskswap/trader/base/dao/mapper/SystemTaskRunLogMapper.java src/main/java/cc/riskswap/trader/base/dao/SystemTaskDao.java src/main/java/cc/riskswap/trader/base/dao/SystemTaskRunLogDao.java src/main/java/cc/riskswap/trader/base/dao/query/SystemTaskListQuery.java src/main/java/cc/riskswap/trader/base/dao/query/SystemTaskRunLogListQuery.java src/main/java/cc/riskswap/trader/base/dao/param/SystemTaskUpdateParam.java src/main/java/cc/riskswap/trader/base/dao/param/SystemTaskTriggerParam.java src/main/java/cc/riskswap/trader/base/dao/dto/SystemTaskDto.java src/main/java/cc/riskswap/trader/base/dao/dto/SystemTaskRunLogDto.java
git -C /Users/haiming/Workspace/trader/trader-base commit -m "feat: add task module dao models"
```

---

### Task 3: 实现任务接口、注册中心和元数据同步

**Files:**
- Create: `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTask.java`
- Create: `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskContext.java`
- Create: `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskTriggerType.java`
- Create: `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskProperties.java`
- Create: `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskRegistry.java`
- Create: `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskMetadataSyncService.java`
- Test: `/Users/haiming/Workspace/trader/trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskRegistryTest.java`

- [ ] **Step 1: 先写注册中心测试，验证重复 `taskCode` 会失败**

新建 `/Users/haiming/Workspace/trader/trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskRegistryTest.java`：

```java
package cc.riskswap.trader.base.task;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TraderTaskRegistryTest {

    @Test
    void should_register_unique_tasks() {
        TraderTask task = new SampleTask("fundSync");
        TraderTaskRegistry registry = new TraderTaskRegistry(List.of(task));
        assertEquals(task, registry.getTask("fundSync"));
    }

    @Test
    void should_fail_when_task_code_duplicated() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                new TraderTaskRegistry(List.of(new SampleTask("dup"), new SampleTask("dup"))));
        assertEquals("Duplicate trader task code: dup", ex.getMessage());
    }

    private record SampleTask(String code) implements TraderTask {
        @Override public String getTaskCode() { return code; }
        @Override public String getTaskName() { return code; }
        @Override public String getDefaultCron() { return "0 0 1 * * ?"; }
        @Override public boolean defaultEnabled() { return true; }
        @Override public String getParamSchema() { return "{\"type\":\"object\"}"; }
        @Override public String getDefaultParams() { return "{}"; }
        @Override public void execute(TraderTaskContext context) { }
    }
}
```

- [ ] **Step 2: 运行测试确认先失败**

Run:

```bash
cd /Users/haiming/Workspace/trader/trader-base && mvn -Dtest=TraderTaskRegistryTest test
```

Expected: FAIL，提示 `TraderTask` 或 `TraderTaskRegistry` 不存在

- [ ] **Step 3: 写入接口、上下文和注册中心的最小实现**

在 `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTask.java` 中写入：

```java
package cc.riskswap.trader.base.task;

public interface TraderTask {
    String getTaskCode();
    String getTaskName();
    String getDefaultCron();
    boolean defaultEnabled();
    String getParamSchema();
    String getDefaultParams();
    void execute(TraderTaskContext context);
}
```

在 `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskRegistry.java` 中写入：

```java
package cc.riskswap.trader.base.task;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TraderTaskRegistry {

    private final Map<String, TraderTask> taskMap = new LinkedHashMap<>();

    public TraderTaskRegistry(List<TraderTask> tasks) {
        for (TraderTask task : tasks) {
            String code = task.getTaskCode();
            if (taskMap.containsKey(code)) {
                throw new IllegalStateException("Duplicate trader task code: " + code);
            }
            taskMap.put(code, task);
        }
    }

    public TraderTask getTask(String taskCode) {
        return taskMap.get(taskCode);
    }

    public Map<String, TraderTask> getTaskMap() {
        return taskMap;
    }
}
```

- [ ] **Step 4: 实现元数据同步服务**

在 `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskMetadataSyncService.java` 中写入：

```java
package cc.riskswap.trader.base.task;

import cc.riskswap.trader.base.dao.SystemTaskDao;
import cc.riskswap.trader.base.dao.entity.SystemTask;

import java.time.OffsetDateTime;
import java.util.Map;

public class TraderTaskMetadataSyncService {

    private final String appName;
    private final TraderTaskRegistry registry;
    private final SystemTaskDao systemTaskDao;

    public TraderTaskMetadataSyncService(String appName, TraderTaskRegistry registry, SystemTaskDao systemTaskDao) {
        this.appName = appName;
        this.registry = registry;
        this.systemTaskDao = systemTaskDao;
    }

    public void sync() {
        for (Map.Entry<String, TraderTask> entry : registry.getTaskMap().entrySet()) {
            TraderTask task = entry.getValue();
            SystemTask existed = systemTaskDao.getByAppNameAndTaskCode(appName, task.getTaskCode());
            if (existed == null) {
                SystemTask entity = new SystemTask();
                entity.setAppName(appName);
                entity.setTaskCode(task.getTaskCode());
                entity.setTaskName(task.getTaskName());
                entity.setCron(task.getDefaultCron());
                entity.setStatus(task.defaultEnabled() ? "RUNNING" : "STOPPED");
                entity.setParamSchema(task.getParamSchema());
                entity.setParamsJson(task.getDefaultParams());
                entity.setDefaultParamsJson(task.getDefaultParams());
                entity.setVersion(1L);
                entity.setCreatedAt(OffsetDateTime.now());
                entity.setUpdatedAt(OffsetDateTime.now());
                systemTaskDao.save(entity);
                continue;
            }
            existed.setTaskName(task.getTaskName());
            existed.setParamSchema(task.getParamSchema());
            existed.setDefaultParamsJson(task.getDefaultParams());
            existed.setUpdatedAt(OffsetDateTime.now());
            systemTaskDao.updateById(existed);
        }
    }
}
```

- [ ] **Step 5: 重新运行注册中心测试**

Run:

```bash
cd /Users/haiming/Workspace/trader/trader-base && mvn -Dtest=TraderTaskRegistryTest test
```

Expected: `Tests run: 2, Failures: 0`

- [ ] **Step 6: 提交当前任务**

```bash
git -C /Users/haiming/Workspace/trader/trader-base add src/main/java/cc/riskswap/trader/base/task/TraderTask.java src/main/java/cc/riskswap/trader/base/task/TraderTaskContext.java src/main/java/cc/riskswap/trader/base/task/TraderTaskTriggerType.java src/main/java/cc/riskswap/trader/base/task/TraderTaskProperties.java src/main/java/cc/riskswap/trader/base/task/TraderTaskRegistry.java src/main/java/cc/riskswap/trader/base/task/TraderTaskMetadataSyncService.java src/test/java/cc/riskswap/trader/base/task/TraderTaskRegistryTest.java
git -C /Users/haiming/Workspace/trader/trader-base commit -m "feat: add trader task abstractions"
```

---

### Task 4: 实现 Quartz 调度器与任务执行代理

**Files:**
- Create: `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderQuartzJob.java`
- Create: `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskSchedulerService.java`
- Modify: `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfiguration.java`
- Test: `/Users/haiming/Workspace/trader/trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskSchedulerServiceTest.java`

- [ ] **Step 1: 先写调度服务测试，验证可创建或删除 Quartz 任务**

新建 `/Users/haiming/Workspace/trader/trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskSchedulerServiceTest.java`：

```java
package cc.riskswap.trader.base.task;

import cc.riskswap.trader.base.dao.entity.SystemTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraderTaskSchedulerServiceTest {

    private Scheduler scheduler;

    @AfterEach
    void tearDown() throws Exception {
        if (scheduler != null) {
            scheduler.shutdown(true);
        }
    }

    @Test
    void should_schedule_running_task() throws Exception {
        scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();
        TraderTaskSchedulerService service = new TraderTaskSchedulerService("trader-collector", scheduler);
        service.refresh(toTask("fundSync", "RUNNING", "0 0 1 * * ?"));
        assertTrue(scheduler.checkExists(service.jobKey("fundSync")));
    }

    @Test
    void should_remove_stopped_task() throws Exception {
        scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();
        TraderTaskSchedulerService service = new TraderTaskSchedulerService("trader-collector", scheduler);
        service.refresh(toTask("fundSync", "RUNNING", "0 0 1 * * ?"));
        service.refresh(toTask("fundSync", "STOPPED", "0 0 1 * * ?"));
        assertFalse(scheduler.checkExists(service.jobKey("fundSync")));
    }

    private SystemTask toTask(String code, String status, String cron) {
        SystemTask task = new SystemTask();
        task.setAppName("trader-collector");
        task.setTaskCode(code);
        task.setTaskName(code);
        task.setStatus(status);
        task.setCron(cron);
        task.setVersion(1L);
        task.setUpdatedAt(OffsetDateTime.now());
        return task;
    }
}
```

- [ ] **Step 2: 运行测试确认先失败**

Run:

```bash
cd /Users/haiming/Workspace/trader/trader-base && mvn -Dtest=TraderTaskSchedulerServiceTest test
```

Expected: FAIL，提示 `TraderTaskSchedulerService` 不存在

- [ ] **Step 3: 写 `TraderQuartzJob` 与 `TraderTaskSchedulerService` 最小实现**

在 `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderQuartzJob.java` 中写入：

```java
package cc.riskswap.trader.base.task;

import cc.riskswap.trader.base.dao.SystemTaskDao;
import cc.riskswap.trader.base.dao.SystemTaskRunLogDao;
import cc.riskswap.trader.base.dao.entity.SystemTask;
import cc.riskswap.trader.base.dao.entity.SystemTaskRunLog;
import cn.hutool.json.JSONUtil;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.context.ApplicationContext;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public class TraderQuartzJob implements Job {

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap dataMap = context.getMergedJobDataMap();
        ApplicationContext applicationContext = (ApplicationContext) dataMap.get("applicationContext");
        String appName = dataMap.getString("appName");
        String taskCode = dataMap.getString("taskCode");
        TraderTaskRegistry registry = applicationContext.getBean(TraderTaskRegistry.class);
        SystemTaskDao systemTaskDao = applicationContext.getBean(SystemTaskDao.class);
        SystemTaskRunLogDao runLogDao = applicationContext.getBean(SystemTaskRunLogDao.class);
        TraderTask task = registry.getTask(taskCode);
        SystemTask taskConfig = systemTaskDao.getByAppNameAndTaskCode(appName, taskCode);
        OffsetDateTime startedAt = OffsetDateTime.now();
        String traceId = UUID.randomUUID().toString();
        SystemTaskRunLog runLog = new SystemTaskRunLog();
        runLog.setAppName(appName);
        runLog.setTaskCode(taskCode);
        runLog.setTriggerType("SCHEDULED");
        runLog.setParamsJson(taskConfig == null ? "{}" : taskConfig.getParamsJson());
        runLog.setStatus("RUNNING");
        runLog.setStartedAt(startedAt);
        runLog.setTraceId(traceId);
        runLog.setCreatedAt(startedAt);
        runLogDao.save(runLog);
        try {
            TraderTaskContext taskContext = new TraderTaskContext();
            taskContext.setAppName(appName);
            taskContext.setTaskCode(taskCode);
            taskContext.setTaskName(taskConfig == null ? taskCode : taskConfig.getTaskName());
            taskContext.setTriggerType("SCHEDULED");
            taskContext.setParamsJson(taskConfig == null ? "{}" : taskConfig.getParamsJson());
            taskContext.setParamsMap(JSONUtil.parseObj(taskContext.getParamsJson()).toBean(Map.class));
            taskContext.setRunAt(startedAt);
            taskContext.setTraceId(traceId);
            task.execute(taskContext);
            runLog.setStatus("SUCCESS");
        } catch (Exception ex) {
            runLog.setStatus("FAILED");
            runLog.setErrorMsg(ex.getMessage());
            throw ex;
        } finally {
            OffsetDateTime finishedAt = OffsetDateTime.now();
            runLog.setFinishedAt(finishedAt);
            runLog.setDurationMs(finishedAt.toInstant().toEpochMilli() - startedAt.toInstant().toEpochMilli());
            runLogDao.updateById(runLog);
        }
    }
}
```

- [ ] **Step 4: 在自动配置中把 Scheduler 服务和注册中心连起来**

把 `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfiguration.java` 扩展为：

```java
package cc.riskswap.trader.base.autoconfigure;

import cc.riskswap.trader.base.dao.SystemTaskDao;
import cc.riskswap.trader.base.task.TraderTask;
import cc.riskswap.trader.base.task.TraderTaskMetadataSyncService;
import cc.riskswap.trader.base.task.TraderTaskProperties;
import cc.riskswap.trader.base.task.TraderTaskRegistry;
import cc.riskswap.trader.base.task.TraderTaskSchedulerService;
import org.quartz.Scheduler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnClass(SchedulerFactoryBean.class)
@ConditionalOnProperty(prefix = "trader.task", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(TraderTaskProperties.class)
public class TraderTaskAutoConfiguration {

    @Bean
    public TraderTaskRegistry traderTaskRegistry(ObjectProvider<List<TraderTask>> taskProvider) {
        List<TraderTask> tasks = taskProvider.getIfAvailable(List::of);
        return new TraderTaskRegistry(tasks);
    }

    @Bean
    public SchedulerFactoryBean traderTaskSchedulerFactoryBean(ApplicationContext applicationContext) {
        SchedulerFactoryBean factoryBean = new SchedulerFactoryBean();
        factoryBean.setApplicationContext(applicationContext);
        factoryBean.setOverwriteExistingJobs(true);
        factoryBean.setAutoStartup(true);
        return factoryBean;
    }

    @Bean
    public TraderTaskSchedulerService traderTaskSchedulerService(TraderTaskProperties properties, Scheduler scheduler) {
        return new TraderTaskSchedulerService(properties.getAppName(), scheduler);
    }

    @Bean
    public TraderTaskMetadataSyncService traderTaskMetadataSyncService(
            TraderTaskProperties properties,
            TraderTaskRegistry registry,
            SystemTaskDao systemTaskDao
    ) {
        return new TraderTaskMetadataSyncService(properties.getAppName(), registry, systemTaskDao);
    }
}
```

- [ ] **Step 5: 运行调度服务测试**

Run:

```bash
cd /Users/haiming/Workspace/trader/trader-base && mvn -Dtest=TraderTaskSchedulerServiceTest test
```

Expected: `Tests run: 2, Failures: 0`

- [ ] **Step 6: 提交当前任务**

```bash
git -C /Users/haiming/Workspace/trader/trader-base add src/main/java/cc/riskswap/trader/base/task/TraderQuartzJob.java src/main/java/cc/riskswap/trader/base/task/TraderTaskSchedulerService.java src/main/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfiguration.java src/test/java/cc/riskswap/trader/base/task/TraderTaskSchedulerServiceTest.java
git -C /Users/haiming/Workspace/trader/trader-base commit -m "feat: add quartz task scheduler"
```

---

### Task 5: 实现 Redis Pub/Sub 刷新与轮询兜底

**Files:**
- Create: `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskRefreshMessage.java`
- Create: `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskRefreshPublisher.java`
- Create: `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskRefreshSubscriber.java`
- Create: `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskPoller.java`
- Modify: `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderRedisAutoConfiguration.java`
- Modify: `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfiguration.java`
- Test: `/Users/haiming/Workspace/trader/trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskRefreshSubscriberTest.java`

- [ ] **Step 1: 先写订阅器测试，验证只刷新当前应用且根据版本触发**

新建 `/Users/haiming/Workspace/trader/trader-base/src/test/java/cc/riskswap/trader/base/task/TraderTaskRefreshSubscriberTest.java`：

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
    void should_refresh_when_message_matches_current_app() {
        SystemTaskDao dao = Mockito.mock(SystemTaskDao.class);
        TraderTaskSchedulerService schedulerService = Mockito.mock(TraderTaskSchedulerService.class);
        TraderTaskRefreshSubscriber subscriber = new TraderTaskRefreshSubscriber("trader-collector", dao, schedulerService);
        SystemTask task = new SystemTask();
        task.setAppName("trader-collector");
        task.setTaskCode("fundSync");
        when(dao.getByAppNameAndTaskCode("trader-collector", "fundSync")).thenReturn(task);
        TraderTaskRefreshMessage message = new TraderTaskRefreshMessage("trader-collector", "fundSync", 2L, "TASK_UPDATED");
        subscriber.handle(message);
        verify(schedulerService, times(1)).refresh(task);
    }
}
```

- [ ] **Step 2: 运行测试确认先失败**

Run:

```bash
cd /Users/haiming/Workspace/trader/trader-base && mvn -Dtest=TraderTaskRefreshSubscriberTest test
```

Expected: FAIL，提示 `TraderTaskRefreshSubscriber` 或 `TraderTaskRefreshMessage` 不存在

- [ ] **Step 3: 完成刷新消息、发布器和订阅器**

在 `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskRefreshMessage.java` 中写入：

```java
package cc.riskswap.trader.base.task;

import java.io.Serializable;

public record TraderTaskRefreshMessage(String appName, String taskCode, Long version, String eventType) implements Serializable {
}
```

在 `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskRefreshSubscriber.java` 中写入：

```java
package cc.riskswap.trader.base.task;

import cc.riskswap.trader.base.dao.SystemTaskDao;
import cc.riskswap.trader.base.dao.entity.SystemTask;

public class TraderTaskRefreshSubscriber {

    private final String appName;
    private final SystemTaskDao systemTaskDao;
    private final TraderTaskSchedulerService schedulerService;

    public TraderTaskRefreshSubscriber(String appName, SystemTaskDao systemTaskDao, TraderTaskSchedulerService schedulerService) {
        this.appName = appName;
        this.systemTaskDao = systemTaskDao;
        this.schedulerService = schedulerService;
    }

    public void handle(TraderTaskRefreshMessage message) {
        if (message == null || !appName.equals(message.appName())) {
            return;
        }
        SystemTask task = systemTaskDao.getByAppNameAndTaskCode(message.appName(), message.taskCode());
        if (task != null) {
            schedulerService.refresh(task);
        }
    }
}
```

- [ ] **Step 4: 在 Redis 自动配置中注册监听容器**

在 `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderRedisAutoConfiguration.java` 中追加：

```java
@Bean
@ConditionalOnBean({RedisConnectionFactory.class, TraderTaskRefreshSubscriber.class})
@ConditionalOnMissingBean(name = "traderTaskRedisListenerContainer")
public RedisMessageListenerContainer traderTaskRedisListenerContainer(
        RedisConnectionFactory redisConnectionFactory,
        TraderTaskRefreshSubscriber subscriber
) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(redisConnectionFactory);
    MessageListenerAdapter adapter = new MessageListenerAdapter(subscriber, "handle");
    adapter.afterPropertiesSet();
    container.addMessageListener(adapter, new PatternTopic("trader:task:refresh"));
    return container;
}
```

- [ ] **Step 5: 加入轮询兜底类并运行测试**

在 `/Users/haiming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/task/TraderTaskPoller.java` 中写入：

```java
package cc.riskswap.trader.base.task;

import cc.riskswap.trader.base.dao.SystemTaskDao;
import cc.riskswap.trader.base.dao.entity.SystemTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TraderTaskPoller {

    private final String appName;
    private final SystemTaskDao systemTaskDao;
    private final TraderTaskSchedulerService schedulerService;
    private final Map<String, Long> versionCache = new HashMap<>();

    public TraderTaskPoller(String appName, SystemTaskDao systemTaskDao, TraderTaskSchedulerService schedulerService) {
        this.appName = appName;
        this.systemTaskDao = systemTaskDao;
        this.schedulerService = schedulerService;
    }

    public void poll() {
        List<SystemTask> tasks = systemTaskDao.listByAppName(appName);
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

然后运行：

```bash
cd /Users/haiming/Workspace/trader/trader-base && mvn -Dtest=TraderTaskRefreshSubscriberTest test
```

Expected: `Tests run: 1, Failures: 0`

- [ ] **Step 6: 提交当前任务**

```bash
git -C /Users/haiming/Workspace/trader/trader-base add src/main/java/cc/riskswap/trader/base/task/TraderTaskRefreshMessage.java src/main/java/cc/riskswap/trader/base/task/TraderTaskRefreshPublisher.java src/main/java/cc/riskswap/trader/base/task/TraderTaskRefreshSubscriber.java src/main/java/cc/riskswap/trader/base/task/TraderTaskPoller.java src/main/java/cc/riskswap/trader/base/autoconfigure/TraderRedisAutoConfiguration.java src/main/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfiguration.java src/test/java/cc/riskswap/trader/base/task/TraderTaskRefreshSubscriberTest.java
git -C /Users/haiming/Workspace/trader/trader-base commit -m "feat: add task refresh subscriber and poller"
```

---

### Task 6: 实现 `admin-server` 任务管理接口

**Files:**
- Create: `/Users/haiming/Workspace/trader/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/dto/SystemTaskDto.java`
- Create: `/Users/haiming/Workspace/trader/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/dto/SystemTaskRunLogDto.java`
- Create: `/Users/haiming/Workspace/trader/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/query/SystemTaskListQuery.java`
- Create: `/Users/haiming/Workspace/trader/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/param/SystemTaskUpdateParam.java`
- Create: `/Users/haiming/Workspace/trader/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/param/SystemTaskTriggerParam.java`
- Create: `/Users/haiming/Workspace/trader/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java`
- Create: `/Users/haiming/Workspace/trader/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/controller/SystemTaskController.java`
- Test: `/Users/haiming/Workspace/trader/trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/controller/SystemTaskControllerRouteTest.java`

- [ ] **Step 1: 先写路由测试**

新建 `/Users/haiming/Workspace/trader/trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/controller/SystemTaskControllerRouteTest.java`：

```java
package cc.riskswap.trader.admin.test.controller;

import cc.riskswap.trader.admin.controller.SystemTaskController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemTaskControllerRouteTest {

    @Test
    void should_use_task_base_route() {
        RequestMapping mapping = SystemTaskController.class.getAnnotation(RequestMapping.class);
        assertEquals("/task", mapping.value()[0]);
    }

    @Test
    void should_expose_list_update_and_trigger_routes() throws Exception {
        Method list = SystemTaskController.class.getMethod("list", cc.riskswap.trader.admin.common.model.query.SystemTaskListQuery.class);
        Method update = SystemTaskController.class.getMethod("update", cc.riskswap.trader.admin.common.model.param.SystemTaskUpdateParam.class);
        Method trigger = SystemTaskController.class.getMethod("trigger", cc.riskswap.trader.admin.common.model.param.SystemTaskTriggerParam.class);
        assertTrue(list.isAnnotationPresent(PostMapping.class));
        assertTrue(update.isAnnotationPresent(PostMapping.class));
        assertTrue(trigger.isAnnotationPresent(PostMapping.class));
    }
}
```

- [ ] **Step 2: 运行测试确认先失败**

Run:

```bash
cd /Users/haiming/Workspace/trader/trader-admin/admin-server && mvn -Dtest=SystemTaskControllerRouteTest test
```

Expected: FAIL，提示 `SystemTaskController` 或任务 DTO/Param/Query 不存在

- [ ] **Step 3: 创建管理端 DTO / Query / Param**

在 `/Users/haiming/Workspace/trader/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/query/SystemTaskListQuery.java` 中写入：

```java
package cc.riskswap.trader.admin.common.model.query;

import lombok.Data;

@Data
public class SystemTaskListQuery {
    private Integer pageNo = 1;
    private Integer pageSize = 20;
    private String appName;
    private String taskCode;
    private String taskName;
    private String status;
}
```

在 `/Users/haiming/Workspace/trader/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/common/model/param/SystemTaskUpdateParam.java` 中写入：

```java
package cc.riskswap.trader.admin.common.model.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SystemTaskUpdateParam {
    @NotNull
    private Long id;
    @NotBlank
    private String cron;
    @NotBlank
    private String status;
    private String paramsJson;
    private String remark;
}
```

- [ ] **Step 4: 实现 Service 与 Controller**

在 `/Users/haiming/Workspace/trader/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java` 中写入：

```java
package cc.riskswap.trader.admin.service;

import cc.riskswap.trader.admin.common.model.dto.PageDto;
import cc.riskswap.trader.admin.common.model.dto.SystemTaskDto;
import cc.riskswap.trader.admin.common.model.param.SystemTaskTriggerParam;
import cc.riskswap.trader.admin.common.model.param.SystemTaskUpdateParam;
import cc.riskswap.trader.admin.common.model.query.SystemTaskListQuery;
import cc.riskswap.trader.base.dao.SystemTaskDao;
import cc.riskswap.trader.base.dao.entity.SystemTask;
import cc.riskswap.trader.base.task.TraderTaskRefreshMessage;
import cc.riskswap.trader.base.task.TraderTaskRefreshPublisher;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SystemTaskService {

    private final SystemTaskDao systemTaskDao;
    private final TraderTaskRefreshPublisher refreshPublisher;

    public SystemTaskService(SystemTaskDao systemTaskDao, TraderTaskRefreshPublisher refreshPublisher) {
        this.systemTaskDao = systemTaskDao;
        this.refreshPublisher = refreshPublisher;
    }

    public PageDto<SystemTaskDto> list(SystemTaskListQuery query) {
        cc.riskswap.trader.base.dao.query.SystemTaskListQuery listQuery = new cc.riskswap.trader.base.dao.query.SystemTaskListQuery();
        BeanUtil.copyProperties(query, listQuery);
        Page<SystemTask> page = systemTaskDao.pageQuery(listQuery);
        List<SystemTaskDto> items = page.getRecords().stream()
                .map(item -> BeanUtil.copyProperties(item, SystemTaskDto.class))
                .collect(Collectors.toList());
        PageDto<SystemTaskDto> dto = new PageDto<>();
        dto.setItems(items);
        dto.setTotal(page.getTotal());
        dto.setPageNo((int) page.getCurrent());
        dto.setPageSize((int) page.getSize());
        return dto;
    }

    public void update(SystemTaskUpdateParam param) {
        SystemTask task = systemTaskDao.getById(param.getId());
        task.setCron(param.getCron());
        task.setStatus(param.getStatus());
        task.setParamsJson(param.getParamsJson());
        task.setRemark(param.getRemark());
        task.setVersion(task.getVersion() == null ? 1L : task.getVersion() + 1);
        task.setUpdatedAt(OffsetDateTime.now());
        systemTaskDao.updateById(task);
        refreshPublisher.publish(new TraderTaskRefreshMessage(task.getAppName(), task.getTaskCode(), task.getVersion(), "TASK_UPDATED"));
    }

    public void trigger(SystemTaskTriggerParam param) {
        refreshPublisher.publish(new TraderTaskRefreshMessage(param.getAppName(), param.getTaskCode(), 0L, "TASK_TRIGGER"));
    }
}
```

- [ ] **Step 5: 运行路由测试与模块编译**

Run:

```bash
cd /Users/haiming/Workspace/trader/trader-admin/admin-server && mvn -Dtest=SystemTaskControllerRouteTest test
cd /Users/haiming/Workspace/trader/trader-admin && mvn -DskipTests compile
```

Expected:

```text
Tests run: 2, Failures: 0
BUILD SUCCESS
```

- [ ] **Step 6: 提交当前任务**

```bash
git -C /Users/haiming/Workspace/trader/trader-admin add admin-server/src/main/java/cc/riskswap/trader/admin/common/model/dto/SystemTaskDto.java admin-server/src/main/java/cc/riskswap/trader/admin/common/model/dto/SystemTaskRunLogDto.java admin-server/src/main/java/cc/riskswap/trader/admin/common/model/query/SystemTaskListQuery.java admin-server/src/main/java/cc/riskswap/trader/admin/common/model/param/SystemTaskUpdateParam.java admin-server/src/main/java/cc/riskswap/trader/admin/common/model/param/SystemTaskTriggerParam.java admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java admin-server/src/main/java/cc/riskswap/trader/admin/controller/SystemTaskController.java admin-server/src/test/java/cc/riskswap/trader/admin/test/controller/SystemTaskControllerRouteTest.java
git -C /Users/haiming/Workspace/trader/trader-admin commit -m "feat: add system task management apis"
```

---

### Task 7: 实现 `admin-web` 任务管理页

**Files:**
- Create: `/Users/haiming/Workspace/trader/trader-admin/admin-web/src/services/systemTask.ts`
- Create: `/Users/haiming/Workspace/trader/trader-admin/admin-web/src/pages/task/TaskManage.vue`
- Modify: `/Users/haiming/Workspace/trader/trader-admin/admin-web/src/router/index.ts`
- Modify: `/Users/haiming/Workspace/trader/trader-admin/admin-web/src/layout/menu.ts`

- [ ] **Step 1: 参考现有服务模式创建 `systemTask.ts`**

新建 `/Users/haiming/Workspace/trader/trader-admin/admin-web/src/services/systemTask.ts`：

```ts
import request from './http'

export interface SystemTaskQuery {
  pageNo: number
  pageSize: number
  appName?: string
  taskCode?: string
  taskName?: string
  status?: string
}

export interface SystemTaskDto {
  id: number
  appName: string
  taskCode: string
  taskName: string
  cron: string
  status: string
  paramsJson?: string
  paramSchema?: string
  remark?: string
  version?: number
  updatedAt?: string
}

export interface SystemTaskUpdateParam {
  id: number
  cron: string
  status: string
  paramsJson?: string
  remark?: string
}

export function listSystemTasks(data: SystemTaskQuery) {
  return request.post('/task/list', data)
}

export function updateSystemTask(data: SystemTaskUpdateParam) {
  return request.post('/task/update', data)
}

export function triggerSystemTask(data: { appName: string; taskCode: string }) {
  return request.post('/task/trigger', data)
}
```

- [ ] **Step 2: 创建基础页面，先实现列表与启停 / 立即执行按钮**

新建 `/Users/haiming/Workspace/trader/trader-admin/admin-web/src/pages/task/TaskManage.vue`：

```vue
<template>
  <div class="page-container">
    <el-card>
      <el-form :model="query" inline>
        <el-form-item label="应用">
          <el-input v-model="query.appName" clearable />
        </el-form-item>
        <el-form-item label="任务编码">
          <el-input v-model="query.taskCode" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable>
            <el-option label="运行中" value="RUNNING" />
            <el-option label="已停止" value="STOPPED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">搜索</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="appName" label="应用" />
        <el-table-column prop="taskCode" label="任务编码" />
        <el-table-column prop="taskName" label="任务名称" />
        <el-table-column prop="cron" label="Cron" min-width="180" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button link type="primary" @click="toggleStatus(row)">
              {{ row.status === 'RUNNING' ? '停止' : '启动' }}
            </el-button>
            <el-button link type="success" @click="triggerTask(row)">立即执行</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>
```

- [ ] **Step 3: 接通路由和菜单**

在 `/Users/haiming/Workspace/trader/trader-admin/admin-web/src/router/index.ts` 中加入：

```ts
{
  path: '/task/manage',
  name: 'TaskManage',
  component: () => import('../pages/task/TaskManage.vue')
}
```

在 `/Users/haiming/Workspace/trader/trader-admin/admin-web/src/layout/menu.ts` 中加入：

```ts
{
  path: '/task/manage',
  title: '任务管理'
}
```

- [ ] **Step 4: 本地构建前端验证页面与接口引用**

Run:

```bash
cd /Users/haiming/Workspace/trader/trader-admin/admin-web && npm run build
```

Expected: 构建成功，无 TypeScript 报错

- [ ] **Step 5: 提交当前任务**

```bash
git -C /Users/haiming/Workspace/trader/trader-admin add admin-web/src/services/systemTask.ts admin-web/src/pages/task/TaskManage.vue admin-web/src/router/index.ts admin-web/src/layout/menu.ts
git -C /Users/haiming/Workspace/trader/trader-admin commit -m "feat: add task management page"
```

---

### Task 8: 迁移 `trader-collector` 样板任务

**Files:**
- Create: `/Users/haiming/Workspace/trader/trader-collector/src/main/java/cc/riskswap/trader/collector/task/FundSyncTask.java`
- Create: `/Users/haiming/Workspace/trader/trader-collector/src/main/java/cc/riskswap/trader/collector/task/FundMarketSyncTask.java`
- Create: `/Users/haiming/Workspace/trader/trader-collector/src/main/java/cc/riskswap/trader/collector/task/CalendarSyncTask.java`
- Modify: `/Users/haiming/Workspace/trader/trader-collector/src/main/java/cc/riskswap/trader/collector/task/FundTask.java`
- Modify: `/Users/haiming/Workspace/trader/trader-collector/src/main/java/cc/riskswap/trader/collector/task/FundMarketTask.java`
- Modify: `/Users/haiming/Workspace/trader/trader-collector/src/main/java/cc/riskswap/trader/collector/task/CalendarTask.java`

- [ ] **Step 1: 先新增新的 `TraderTask` 样板类**

在 `/Users/haiming/Workspace/trader/trader-collector/src/main/java/cc/riskswap/trader/collector/task/FundSyncTask.java` 中写入：

```java
package cc.riskswap.trader.collector.task;

import cc.riskswap.trader.base.task.TraderTask;
import cc.riskswap.trader.base.task.TraderTaskContext;
import cc.riskswap.trader.collector.service.FundService;
import org.springframework.stereotype.Component;

@Component
public class FundSyncTask implements TraderTask {

    private final FundService fundService;

    public FundSyncTask(FundService fundService) {
        this.fundService = fundService;
    }

    @Override
    public String getTaskCode() {
        return "fundSync";
    }

    @Override
    public String getTaskName() {
        return "同步基金基础数据";
    }

    @Override
    public String getDefaultCron() {
        return "0 0 0 * * ?";
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public String getParamSchema() {
        return "{\"type\":\"object\",\"properties\":{\"fullSync\":{\"type\":\"boolean\",\"title\":\"是否全量同步\"}}}";
    }

    @Override
    public String getDefaultParams() {
        return "{\"fullSync\":true}";
    }

    @Override
    public void execute(TraderTaskContext context) {
        fundService.syncFund();
    }
}
```

- [ ] **Step 2: 给行情和日历任务也写同样的样板实现**

在 `/Users/haiming/Workspace/trader/trader-collector/src/main/java/cc/riskswap/trader/collector/task/FundMarketSyncTask.java` 中写入：

```java
package cc.riskswap.trader.collector.task;

import cc.riskswap.trader.base.task.TraderTask;
import cc.riskswap.trader.base.task.TraderTaskContext;
import cc.riskswap.trader.collector.service.FundMarketService;
import org.springframework.stereotype.Component;

@Component
public class FundMarketSyncTask implements TraderTask {

    private final FundMarketService fundMarketService;

    public FundMarketSyncTask(FundMarketService fundMarketService) {
        this.fundMarketService = fundMarketService;
    }

    @Override
    public String getTaskCode() {
        return "fundMarketSync";
    }

    @Override
    public String getTaskName() {
        return "同步基金行情数据";
    }

    @Override
    public String getDefaultCron() {
        return "0 0 1 * * ?";
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public String getParamSchema() {
        return "{\"type\":\"object\",\"properties\":{\"days\":{\"type\":\"integer\",\"title\":\"回溯天数\",\"minimum\":1}}}";
    }

    @Override
    public String getDefaultParams() {
        return "{\"days\":1}";
    }

    @Override
    public void execute(TraderTaskContext context) {
        fundMarketService.syncFundMarket();
    }
}
```

在 `/Users/haiming/Workspace/trader/trader-collector/src/main/java/cc/riskswap/trader/collector/task/CalendarSyncTask.java` 中写入：

```java
package cc.riskswap.trader.collector.task;

import cc.riskswap.trader.base.task.TraderTask;
import cc.riskswap.trader.base.task.TraderTaskContext;
import cc.riskswap.trader.collector.service.CalendarService;
import org.springframework.stereotype.Component;

@Component
public class CalendarSyncTask implements TraderTask {

    private final CalendarService calendarService;

    public CalendarSyncTask(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @Override
    public String getTaskCode() {
        return "calendarSync";
    }

    @Override
    public String getTaskName() {
        return "同步交易日历";
    }

    @Override
    public String getDefaultCron() {
        return "0 0 1 * * ?";
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public String getParamSchema() {
        return "{\"type\":\"object\",\"properties\":{\"exchange\":{\"type\":\"string\",\"title\":\"交易所\"}}}";
    }

    @Override
    public String getDefaultParams() {
        return "{\"exchange\":\"SSE\"}";
    }

    @Override
    public void execute(TraderTaskContext context) {
        calendarService.syncCalendar();
    }
}
```

- [ ] **Step 3: 停用旧的 `@Scheduled` 类，避免重复执行**

把 `/Users/haiming/Workspace/trader/trader-collector/src/main/java/cc/riskswap/trader/collector/task/FundTask.java` 改成：

```java
package cc.riskswap.trader.collector.task;

@Deprecated
public class FundTask {
}
```

把 `/Users/haiming/Workspace/trader/trader-collector/src/main/java/cc/riskswap/trader/collector/task/FundMarketTask.java` 改成：

```java
package cc.riskswap.trader.collector.task;

@Deprecated
public class FundMarketTask {
}
```

把 `/Users/haiming/Workspace/trader/trader-collector/src/main/java/cc/riskswap/trader/collector/task/CalendarTask.java` 改成：

```java
package cc.riskswap.trader.collector.task;

@Deprecated
public class CalendarTask {
}
```

- [ ] **Step 4: 编译 `trader-collector` 验证迁移样板**

Run:

```bash
cd /Users/haiming/Workspace/trader/trader-base && mvn -DskipTests install
cd /Users/haiming/Workspace/trader/trader-collector && mvn -DskipTests compile
```

Expected:

```text
BUILD SUCCESS
BUILD SUCCESS
```

- [ ] **Step 5: 提交当前任务**

```bash
git -C /Users/haiming/Workspace/trader/trader-collector add src/main/java/cc/riskswap/trader/collector/task/FundSyncTask.java src/main/java/cc/riskswap/trader/collector/task/FundMarketSyncTask.java src/main/java/cc/riskswap/trader/collector/task/CalendarSyncTask.java src/main/java/cc/riskswap/trader/collector/task/FundTask.java src/main/java/cc/riskswap/trader/collector/task/FundMarketTask.java src/main/java/cc/riskswap/trader/collector/task/CalendarTask.java
git -C /Users/haiming/Workspace/trader/trader-collector commit -m "feat: migrate collector tasks to trader task abstraction"
```

---

### Task 9: 全量联调与验收

**Files:**
- Modify: `../specs/2026-04-15-task-module-design.md`
- Verify: `/Users/haiming/Workspace/trader/trader-base`
- Verify: `/Users/haiming/Workspace/trader/trader-admin`
- Verify: `/Users/haiming/Workspace/trader/trader-collector`
- Verify: `/Users/haiming/Workspace/trader/trader-executor`

- [ ] **Step 1: 运行 `trader-base` 测试**

Run:

```bash
cd /Users/haiming/Workspace/trader/trader-base && mvn test
```

Expected: `BUILD SUCCESS`

- [ ] **Step 2: 编译三个消费模块**

Run:

```bash
cd /Users/haiming/Workspace/trader/trader-admin && mvn -DskipTests compile
cd /Users/haiming/Workspace/trader/trader-collector && mvn -DskipTests compile
cd /Users/haiming/Workspace/trader/trader-executor && mvn -DskipTests compile
```

Expected:

```text
BUILD SUCCESS
BUILD SUCCESS
BUILD SUCCESS
```

- [ ] **Step 3: 手工验收动态任务链路**

按顺序执行：

```bash
cd /Users/haiming/Workspace/trader/trader-admin && mvn -pl admin-server spring-boot:run
cd /Users/haiming/Workspace/trader/trader-collector && mvn spring-boot:run
```

验收项：

```text
1. 打开后台任务管理页，能看到 trader-collector 的样板任务
2. 修改 Cron 后，collector 日志中出现刷新记录
3. 点击停止后，Quartz 中对应任务被删除或暂停
4. 点击启动后，Quartz 重新注册
5. 修改 params_json 后，下一次执行能读取最新参数
6. 关闭 Redis 通知后，等待轮询周期，任务状态仍能自动收敛
```

- [ ] **Step 4: 更新设计文档中的实施状态说明**

在 `../specs/2026-04-15-task-module-design.md` 末尾增加：

```md
## 实施状态

- [x] `trader-base` 任务抽象已落地
- [x] `trader-admin` 任务管理接口已落地
- [x] `admin-web` 任务管理页面已落地
- [x] `trader-collector` 样板任务已迁移
- [ ] 其它业务模块任务迁移待继续推进
```

- [ ] **Step 5: 提交最终验收结果**

```bash
git -C /Users/haiming/Workspace/trader add trader-docs/trader-base/docs/superpowers/specs/2026-04-15-task-module-design.md trader-docs/trader-base/docs/superpowers/plans/2026-04-15-task-module-implementation.md
git -C /Users/haiming/Workspace/trader/trader-base commit -m "docs: finalize task module implementation plan"
```

---

## Self-Review

### Spec Coverage

- 任务统一抽象：Task 3 覆盖
- Quartz 调度：Task 4 覆盖
- Redis Pub/Sub + 轮询兜底：Task 5 覆盖
- 参数配置 JSON + Schema：Task 2、Task 6、Task 8 覆盖
- 管理后台能力：Task 6、Task 7 覆盖
- 业务项目集成样板：Task 8 覆盖

### Placeholder Scan

- 未使用 `TODO`、`TBD`、`后续补充` 作为实现步骤
- 每个任务均包含具体文件、具体命令与预期结果

### Type Consistency

- 任务抽象统一使用 `TraderTask`、`TraderTaskContext`
- 配置消息统一使用 `TraderTaskRefreshMessage`
- 管理对象统一使用 `SystemTask*` 命名
