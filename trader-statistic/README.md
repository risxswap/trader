# trader-statistic

`trader-statistic` 是统计分析服务模块，负责对业务数据进行统计计算与结果落库，提供给管理端查询与展示。

- 统计任务：以定时/批处理方式执行统计计算（例如相关性统计）
- 数据访问：通过 DAO/Mapper 读取基础数据并写入统计结果
- 运行与部署：包含 docker-compose、打包脚本与运行脚本

文档入口：

- 模块文档：[trader-docs/modules/trader-statistic](../trader-docs/modules/trader-statistic.md)
- 文档总入口：[trader-docs](../trader-docs/README.md)
