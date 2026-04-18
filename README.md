# Trader

`Trader` 是一个多模块的量化交易管理与分析系统。

本仓库是一个聚合的 Maven 工程，由以下模块组成：

- [**trader-admin**](./trader-admin)：管理端模块，包含后端服务（admin-server）与前端控制台（admin-web），用于统一管理与查看基金/ETF、投资组合、交易记录、任务、节点与统计等能力。
- [**trader-base**](./trader-base)：基础库模块（Spring Boot Starter），为各业务服务统一提供自动装配、基础设施（Redis、MySQL、ClickHouse）、任务调度抽象、以及监控上报等通用能力。
- [**trader-collector**](./trader-collector)：数据采集服务模块，按任务定时从外部数据源抓取基础数据（基金基础信息、净值、行情、交易日历等）并入库。
- [**trader-executor**](./trader-executor)：策略执行器服务模块，负责按调度执行交易策略、生成交易与持仓记录，并与管理端/Redis 等组件协作完成状态同步。
- [**trader-statistic**](./trader-statistic)：统计分析服务模块，负责对业务数据进行复杂的统计计算（例如相关性统计）与结果落库，供管理端查询与展示。
- [**trader-docs**](./trader-docs)：存放项目相关的所有的架构设计、产品需求、部署运维、开发指南及变更计划等文档。

## 快速开始

本项目使用 Maven 统一管理与构建：

```bash
# 在项目根目录执行，将自动编译、打包所有的子模块
./mvnw clean install -DskipTests
```

## 统一构建脚本

仓库根目录提供统一构建入口：

```bash
# 打包单个顶层模块
./build.sh package trader-admin
./build.sh package trader-base
./build.sh package trader-collector
./build.sh package trader-executor
./build.sh package trader-statistic

# 全量 install，跳过测试
./build.sh full-install

# 查看帮助
./build.sh help
```

## 文档指引

有关详细的项目架构、部署说明以及业务模块设计，请参阅：

- [项目文档总入口](./trader-docs/README.md)
- [架构设计](./trader-docs/architecture/00-Architecture.md)
- [产品需求](./trader-docs/product/requirements.md)
- [运维指南](./trader-docs/ops/08-Runbook.md)
- [开发指南](./trader-docs/dev/Code-Wiki.md)
