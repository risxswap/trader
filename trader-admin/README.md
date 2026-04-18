# trader-admin

`trader-admin` 是管理端模块，包含后端服务（admin-server）与前端控制台（admin-web），用于统一管理与查看基金/ETF、投资组合、交易记录、任务、节点与统计等能力。

- 后端：Spring Boot 服务，提供 REST API 与数据管理能力
- 前端：Vue 3 管理后台，调用后端 API 展示与操作数据
- 运行与部署：包含 docker-compose、nginx、打包脚本等运维资产

文档入口：

- 模块文档：[trader-docs/modules/trader-admin](../trader-docs/modules/trader-admin.md)
- 文档总入口：[trader-docs](../trader-docs/README.md)
