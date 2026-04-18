# Correlation List Normal Page Design

## 背景

当前 `correlation` 已采用 append-only 写入，并在部分读路径中按业务键 `(asset1, asset2, period)` 只读取最新版本。

其中列表分页查询 `CorrelationDao.pageQuery()` 目前也走了 latest-only 逻辑，会对同一业务键做去重后再返回分页结果。

新需求是:

- 相关性列表分页数据不需要去重
- 列表按普通查询返回即可
- 按业务键获取最新版本的逻辑继续保留，供写入和历史清理使用

## 目标

- 仅回退列表分页查询为普通分页
- 不影响 `getByUniqueKey()` 的 latest-only 语义
- 不影响 append-only 写入
- 不影响历史清理能力

## 非目标

- 不修改详情页按 `id` 查询逻辑
- 不删除当前用于 latest-only 的 mapper 方法
- 不调整历史清理任务
- 不引入新的接口参数或查询开关

## 设计

### 1. 列表分页恢复普通查询

`CorrelationDao.pageQuery()` 改回 MyBatis-Plus 的普通分页查询:

- 使用 `LambdaQueryWrapper`
- 按现有筛选条件过滤 `asset1`、`asset2`、`period`、`minCoefficient`、`maxCoefficient`
- 按时间倒序返回
- 直接 `this.page(page, wrapper)`

这样列表会返回原始记录，包括历史版本。

### 2. latest-only 逻辑保留

以下能力继续保留:

- `getByUniqueKey(asset1, asset2, period)` 返回最新版本
- latest-only 的 mapper SQL 继续存在，供写路径与清理路径使用

这样写路径依旧可以在 append-only 前提下正确定位“当前最新版本”。

### 3. 前端无需改动

前端列表页仍然调用 `/correlation`，只是返回结果从“去重后的最新版本列表”变为“普通分页结果”。

## 影响

### 正向影响

- 满足“列表分页不去重”的需求
- 改动范围最小
- 不影响已完成的 append-only 与历史清理设计

### 行为变化

- 列表页可能看到同一组 `(asset1, asset2, period)` 的多条历史记录
- `total` 也会变成原始记录总数，而不是去重后的业务键总数

## 测试策略

- 恢复或新增 DAO/服务测试，约束 `pageQuery()` 走普通分页
- 保留现有 latest-only 测试，确保 `getByUniqueKey()` 和历史清理相关 mapper 不受影响

## 结论

采用最小回退方案:

- 只回退 `CorrelationDao.pageQuery()` 为普通分页查询
- 其他 latest-only 逻辑保持不变
