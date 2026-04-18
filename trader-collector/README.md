# trader-collector

`trader-collector` 是数据采集服务模块，按任务定时从外部数据源抓取基础数据并入库。

- 采集内容：基金基础信息、净值、行情、交易日历等
- 任务编排：以定时任务方式组织同步流程，支持周期性运行
- 运行与部署：包含 docker-compose、打包脚本与运行脚本

文档入口：

- 模块文档（含样例数据）：[trader-docs/modules/trader-collector](../trader-docs/modules/trader-collector.md)
- 文档总入口：[trader-docs](../trader-docs/README.md)
