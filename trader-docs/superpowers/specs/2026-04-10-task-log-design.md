# 任务日志功能设计文档 (Task Log Design Spec)

## 1. 需求背景与目标
系统中有诸如“证券相关性计算”、“基金净值同步”等各种定时任务和异步任务。为了能够直观地在后台管理系统中监控任务执行状态、分析执行耗时、并追溯执行过程中发生的业务细节与异常情况，需要增加“任务执行日志”功能。

## 2. 数据库设计
使用 **MySQL** 作为存储引擎，以方便与管理端其余业务表做关联或分页查询。

**表名：** `task_log`

| 字段名 | 类型 | 约束 | 描述 |
| --- | --- | --- | --- |
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 日志ID |
| task_name | VARCHAR(100) | NOT NULL | 任务名称，如“相关性计算” |
| task_group | VARCHAR(100) | NULL | 任务分组，如“统计分析” |
| start_time | DATETIME | NOT NULL | 任务开始时间 |
| end_time | DATETIME | NULL | 任务结束时间 |
| status | VARCHAR(20) | NOT NULL | 任务状态 (RUNNING, SUCCESS, FAILED) |
| content | TEXT | NULL | 任务执行过程内容的记录 (Markdown 格式) |
| error_msg | TEXT | NULL | 任务失败时的错误堆栈/简要信息 |
| execution_ms | BIGINT | NULL | 任务执行耗时 (毫秒) |
| created_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 记录创建时间 |
| updated_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 记录更新时间 |

## 3. 后端服务设计
基于 Java Spring Boot 和 MyBatis-Plus。

### 3.1 实体与映射
*   **Entity:** `cc.riskswap.trader.admin.dao.entity.TaskLog`
*   **Mapper:** `TaskLogMapper`
*   **Dao:** `TaskLogDao` (继承 `ServiceImpl<TaskLogMapper, TaskLog>`)

### 3.2 业务逻辑层 (TaskLogService)
暴露供系统内各处定时/异步任务手动调用的API：
*   `Long startTask(String taskName, String taskGroup)`：开始任务时调用，初始化一条 `status='RUNNING'`，`start_time=Now` 的记录。
*   `void appendContent(Long logId, String markdownContent)`：执行过程中，追加 Markdown 内容（方便长耗时任务阶段性记录状态）。
*   `void finishTask(Long logId, boolean success, String markdownContent, String errorMsg)`：任务结束或抛出异常时调用，更新 `status` (SUCCESS/FAILED)，`end_time`，以及最终耗时 `execution_ms`。

### 3.3 控制器层 (TaskLogController)
暴露供前端页面查询和管理的API：
*   `GET /api/logs/task`：分页查询任务日志，支持通过 `taskName`、`status`、`timeRange` (基于 `start_time`) 进行条件筛选。
*   `GET /api/logs/task/{id}`：获取某一条记录的详细内容，主要为了获取完整的 `content` 或 `error_msg`。

## 4. 任务改造接入点
在系统中已有的定时任务（如 `cc.riskswap.trader.admin.task.CorrelationTask` 等），注入 `TaskLogService`，并按如下模式进行包裹：
```java
Long logId = taskLogService.startTask("证券相关性计算", "统计分析");
StringBuilder content = new StringBuilder("### 任务启动\n");
try {
    // ... 业务执行 ...
    content.append("- 处理了 X 条数据\n");
    taskLogService.finishTask(logId, true, content.toString(), null);
} catch(Exception e) {
    taskLogService.finishTask(logId, false, content.toString(), e.getMessage());
}
```

## 5. 前端页面设计
基于 Vue 3 + Element Plus。

### 5.1 路由与导航
*   在 `src/router/index.ts` 注册路由 `/logs/task`，对应组件 `TaskLogList`。
*   在 `src/layouts/MainLayout.vue` 左侧菜单中，放入“系统管理”或与消息推送日志同级的“日志管理”目录下。

### 5.2 列表页 (List.vue)
*   **查询表单**：任务名称（输入框）、状态（下拉框）、执行时间（日期范围选择器）。
*   **数据表格**：列出 `id`、`task_name`、`task_group`、`status`（用 `el-tag`）、`start_time`、`execution_ms` 等核心字段。
*   **操作列**：包含“查看详情”按钮。

### 5.3 详情弹窗/抽屉
*   点击“查看详情”后，打开抽屉（`el-drawer`）或弹窗（`el-dialog`）。
*   展示完整的 `content`（Markdown 渲染展示，需引入 markdown 解析器如 `markdown-it` 或直接文本展示），并显示 `error_msg`。
