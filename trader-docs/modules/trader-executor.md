# trader-executor

`trader-executor` 是策略执行节点服务模块，负责按调度执行策略、生成交易与持仓记录，并通过 Redis 等机制与管理端协作完成状态同步与事件通知。

## 入口

- 模块 README：[trader-executor/README.md](../../trader-executor/README.md)
- 架构总览：[00-Architecture](../architecture/00-Architecture.md)
- 模块职责与边界：[01-Modules](../architecture/01-Modules.md)
- 开发者文档（执行服务）：[06-Executor](../dev/06-Executor.md)
- 运维与部署（整体跑起来）：[08-Runbook](../ops/08-Runbook.md)

## 相关产品文档

- 投资：[03-投资](../product/03-投资.md)
- 节点管理：[11-节点管理](../product/11-节点管理.md)
