# Correlation Task Schema Param Design

## Goal

为“相关系数统计任务”增加可配置的过滤参数，并通过任务定义中的 JSON Schema 驱动前端创建/编辑表单，而不是只让用户手工填写 `paramsJson`。

本次范围只包含一个参数：

- `minAbsCorrelation`：最小绝对相关系数阈值

## Background

当前相关性任务的任务定义如下：

- `CorrelationTask.getParamSchema()` 返回 `{}`，前端无法根据 schema 渲染结构化表单
- `CorrelationTask.getDefaultParams()` 返回 `{}`，新建任务实例没有参数默认值
- `CorrelationService` 内部把相关系数过滤条件写死为 `absR > 0.5d`

同时，管理端 `task/Manage.vue` 当前的任务创建/编辑界面只暴露原始 `paramsJson` 文本框，虽然任务定义里已经有 `paramSchema` 和 `defaultParamsJson` 字段，但没有真正用于表单生成。

## Scope

本次只做以下内容：

- 为相关性任务定义 `minAbsCorrelation` 的 JSON Schema
- 为相关性任务提供默认参数
- 任务执行时读取实例参数并传递给统计服务
- 相关性过滤逻辑改为使用任务参数中的阈值
- 管理端根据 schema 渲染最小可用的数字输入表单
- 保留原始 `paramsJson` 回退能力

本次不做：

- `pValue` 阈值配置化
- 统计周期配置化
- 资产类型、最少样本数等更多过滤条件
- 通用完整 JSON Schema 引擎

## Design

### 1. Task Definition

`CorrelationTask` 暴露结构化参数定义：

- `getParamSchema()` 返回 JSON Schema
- `getDefaultParams()` 返回默认参数 JSON

推荐 schema 结构：

```json
{
  "type": "object",
  "title": "相关性过滤参数",
  "properties": {
    "minAbsCorrelation": {
      "type": "number",
      "title": "最小绝对相关系数",
      "description": "仅保存绝对值大于该阈值的相关性结果",
      "default": 0.5,
      "minimum": 0,
      "maximum": 1
    }
  },
  "required": ["minAbsCorrelation"]
}
```

默认参数：

```json
{
  "minAbsCorrelation": 0.5
}
```

### 2. Runtime Param Parsing

在 `trader-statistic` 中增加一个很小的参数对象或解析 helper，例如：

- `CorrelationTaskParams`

职责：

- 从 `TraderTaskContext` 的 `paramsJson` 解析任务参数
- 提供 `minAbsCorrelation` 默认值
- 对非法值做兜底与范围校验

建议规则：

- 为空或 `{}` 时，使用默认值 `0.5`
- 不是数字时，回退默认值 `0.5`
- 小于 `0` 或大于 `1` 时，回退默认值 `0.5`
- 回退时记录 warn 日志，便于定位配置问题

### 3. Service Contract

`CorrelationService` 的批量计算入口增加阈值参数，例如：

- `calculateAndSaveBatch(List<Fund> funds, String period, double minAbsCorrelation)`

过滤逻辑从：

```java
absR > 0.5d && pValue < 0.05d
```

改为：

```java
absR > minAbsCorrelation && pValue < 0.05d
```

注意：

- `pValue < 0.05d` 本次保持不变
- 单对计算路径是否同步复用同一阈值规则，建议一并收敛到共享 helper，避免批量与单对规则分叉

### 4. Task Execution Flow

执行链路调整为：

1. `CorrelationTask.execute(context)` 读取 `paramsJson`
2. 解析为 `CorrelationTaskParams`
3. 取出 `minAbsCorrelation`
4. 调用 `CorrelationService.calculateAndSaveBatch(uniqueFunds, "1Y", minAbsCorrelation)`
5. 日志打印本次任务实际使用的阈值

### 5. Admin Web Schema Rendering

管理端不做完整通用 JSON Schema 引擎，只支持本次需要的最小子集：

- `type: object`
- `properties`
- `title`
- `description`
- `required`
- 字段 `type: number`
- `default`
- `minimum`
- `maximum`

渲染策略：

- `number` 字段渲染为 `el-input-number`
- 使用 schema 中的 `title` 作为表单标签
- 使用 `description` 作为辅助说明
- 使用 `minimum` / `maximum` 约束输入范围
- 使用 `default` 初始化表单值

回退策略：

- 如果 schema 为空、解析失败或出现当前不支持的结构
- 保留并显示现有原始 `paramsJson` 文本框
- 不阻塞其他任务定义的使用

### 6. Compatibility

对旧任务实例保持兼容：

- 已存在的任务实例若 `paramsJson` 为空或为 `{}`，仍可执行
- 后端自动回退到 `0.5`
- 不需要数据库迁移
- 不需要批量修复历史任务实例

## File Impact

### Statistic

- `trader-statistic/.../task/CorrelationTask.java`
  - 增加 schema
  - 增加默认参数
  - 读取并解析 `paramsJson`
  - 调用带阈值的新服务入口

- `trader-statistic/.../service/CorrelationService.java`
  - 批量计算入口增加阈值参数
  - 过滤逻辑使用传入阈值
  - 增加参数相关日志

- `trader-statistic/...`
  - 新增 `CorrelationTaskParams` 或同等职责 helper

### Admin Web

- `trader-admin/admin-web/src/pages/task/Manage.vue`
  - 创建任务实例时根据 schema 渲染最小表单
  - 编辑任务时根据 schema 渲染最小表单
  - 生成并同步 `paramsJson`
  - 不支持 schema 时回退文本框

- `trader-admin/admin-web/src/services/systemTask.ts`
  - 如有需要，补充 schema 相关类型定义

### Admin Server

- 原则上不需要新增接口字段
- 现有 `TaskDefinitionDto` 已包含：
  - `paramSchema`
  - `defaultParamsJson`

## Validation

### Statistic Tests

- `CorrelationTaskTest`
  - 空参数时回退默认值
  - 非法参数时回退默认值
  - 指定阈值时把阈值传给 service

- `CorrelationServiceTest`
  - 阈值较低时结果可保存
  - 阈值较高时结果被过滤
  - 旧行为默认值 `0.5` 保持兼容

### Admin Web Tests

- 最小渲染测试或组件行为测试：
  - number schema 渲染为数字输入
  - 默认值自动带出
  - 修改输入后能正确组装 `paramsJson`
  - 不支持 schema 时回退文本框

## Risks

### Risk 1: Frontend Schema Support Too Broad

如果一开始试图做完整 JSON Schema 引擎，成本高且容易引入边界问题。

Mitigation:

- 只支持本次需要的最小 schema 子集
- 其余情况统一回退原始 JSON 编辑

### Risk 2: Invalid Task Params Affect Results

非法阈值可能导致保存结果过多或过少。

Mitigation:

- 后端统一做 `0..1` 范围校验
- 非法值回退默认值并输出 warn 日志

### Risk 3: Old Instances Lack Params

旧任务实例没有结构化参数。

Mitigation:

- 后端默认值兜底
- 不依赖数据迁移

## Recommended Implementation Strategy

按以下顺序实现：

1. 先完成 `statistic` 侧参数对象、任务定义和服务过滤逻辑
2. 用测试锁定默认值和不同阈值的行为
3. 再接入 `admin-web` 的最小 schema 渲染
4. 最后做创建/编辑任务的端到端回归
