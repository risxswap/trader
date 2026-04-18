# trader-executor

`trader-executor` 是策略执行器服务模块，负责按调度执行策略、生成交易与持仓记录，并与管理端/Redis 等组件协作完成状态同步。

- 策略执行：策略基类与具体策略实现，按任务调度运行
- 数据写入：投资、交易、持仓与执行日志等数据落库
- 协作与通信：Redis 缓存与发布订阅，用于状态同步与事件通知
- 运行与部署：包含 docker-compose、打包脚本与运行脚本

文档入口：

- 模块文档：[trader-docs/modules/trader-executor](../trader-docs/modules/trader-executor.md)
- 文档总入口：[trader-docs](../trader-docs/README.md)
