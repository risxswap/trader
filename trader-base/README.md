# trader-base

`trader-base` 是基础库模块（Spring Boot Starter），为各业务服务统一提供自动装配与基础设施能力。

- 配置加载：统一外部配置读取与节点配置覆盖
- 基础设施：Redis、MySQL、ClickHouse 数据源与 MyBatis 组件自动装配
- 任务与日志：任务抽象、任务调度相关组件与任务日志落库能力
- 监控：节点硬件信息采集与上报相关能力

文档入口：

- 模块文档：[trader-docs/modules/trader-base](../trader-docs/modules/trader-base.md)
- 文档总入口：[trader-docs](../trader-docs/README.md)
