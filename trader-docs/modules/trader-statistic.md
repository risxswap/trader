# trader-statistic

`trader-statistic` 是统计分析服务模块，负责对业务数据进行统计计算与结果落库，供管理端查询与展示（例如相关性统计的后台计算/清理）。

## 入口

- 模块 README：[trader-statistic/README.md](../../trader-statistic/README.md)
- 架构总览：[00-Architecture](../architecture/00-Architecture.md)
- 模块职责与边界：[01-Modules](../architecture/01-Modules.md)
- 依赖关系图：[02-Dependency-Graph](../architecture/02-Dependency-Graph.md)
- 运维与部署（整体跑起来）：[08-Runbook](../ops/08-Runbook.md)

## 相关产品文档

- 相关性统计：[07-相关性统计](../product/07-相关性统计.md)

## 相关设计/计划（superpowers）

- 相关性统计从 admin 迁移到 statistic（计划）：[2026-04-15-trader-statistic-correlation-migration](../superpowers/plans/2026-04-15-trader-statistic-correlation-migration.md)
- 相关性统计从 admin 迁移到 statistic（设计）：[2026-04-15-trader-statistic-correlation-migration-design](../superpowers/specs/2026-04-15-trader-statistic-correlation-migration-design.md)
