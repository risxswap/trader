# trader-base

`trader-base` 是基础库模块（Spring Boot Starter），为各业务服务统一提供自动装配与基础设施能力（多数据源、Redis、任务、日志、DAO 下沉等）。

## 入口

- 模块 README：[trader-base/README.md](../../trader-base/README.md)
- 架构总览：[00-Architecture](../architecture/00-Architecture.md)
- 模块职责与边界：[01-Modules](../architecture/01-Modules.md)
- 依赖关系图：[02-Dependency-Graph](../architecture/02-Dependency-Graph.md)
- 开发者文档（基础能力详解）：[03-Backend-Core](../dev/03-Backend-Core.md)
- 开发者文档（使用说明）：[trader-base](../dev/trader-base.md)
- 运维与部署（整体跑起来）：[08-Runbook](../ops/08-Runbook.md)

## 相关设计/计划（superpowers）

- DAO 下沉到 trader-base（计划）：[2026-04-15-extract-dao-to-trader-base](../superpowers/plans/2026-04-15-extract-dao-to-trader-base.md)
- DAO 下沉到 trader-base（设计）：[2026-04-15-extract-dao-to-trader-base-design](../superpowers/specs/2026-04-15-extract-dao-to-trader-base-design.md)
- 任务模块（设计）：[2026-04-15-task-module-design](../superpowers/specs/2026-04-15-task-module-design.md)
- 任务模块（实施计划）：[2026-04-15-task-module-implementation](../superpowers/plans/2026-04-15-task-module-implementation.md)
