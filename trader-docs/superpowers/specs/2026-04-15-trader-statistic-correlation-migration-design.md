# Trader Statistic Correlation Migration Design

## 背景

当前相关性统计能力位于 `trader-admin/admin-server` 中，既包含前台查询接口，也包含后台计算与历史清理逻辑。

现在希望把“后台相关性统计职责”拆出到独立的 `trader-statistic` 项目中，并让它像 `trader-collector` 一样具备:

- `trader-base` 依赖
- 可独立打包
- `docker-compose.yml` 部署方式

同时明确要求:

- `trader-statistic` 只负责后台计算和历史清理
- 前台 HTTP 查询接口继续保留在 `trader-admin`

## 目标

- 让 `trader-statistic` 具备与 `trader-collector` 同等级别的工程基础形态
- 将相关性后台任务、计算逻辑、清理逻辑迁入 `trader-statistic`
- 保持 `trader-admin` 继续提供相关性列表/详情/编辑接口
- 两边共享同一份 `correlation` 数据表

## 非目标

- 不把 `admin-web` 改成直接访问 `trader-statistic`
- 不把前台相关的 controller/dto/query/param 一并迁走
- 不在本次设计中抽公共共享模块
- 不重做相关性数据模型

## 总体方案

采用职责拆分方案:

- `trader-statistic` 负责后台相关性任务
- `trader-admin` 负责前台接口和管理能力

拆分边界如下:

### 留在 trader-admin 的内容

- `CorrelationController`
- 面向前台的列表/详情/新增/编辑接口
- `CorrelationDto`
- `CorrelationQuery`
- `CorrelationParam`
- 仅供前台查询使用的 service 入口

### 迁入 trader-statistic 的内容

- `CorrelationTask`
- 相关性计算核心逻辑
- 历史清理逻辑
- `CorrelationDao`
- `CorrelationMapper`
- `Correlation`
- `CorrelationDuplicateGroup`
- 相关的 task/config/datasource 依赖

## 工程形态设计

### 1. trader-statistic 对齐 trader-collector 的基础能力

`trader-statistic` 需要补齐以下内容:

- `pom.xml` 引入 `trader-base`
- Spring Boot Web/Actuator/AOP
- MyBatis-Plus
- MySQL / ClickHouse / HikariCP
- Redis（如任务日志或通道依赖需要）
- `package.sh`
- `docker-compose.yml`
- `bin/run.sh`
- `src/main/resources/config.properties`
- `src/main/resources/logback.xml`

原则上尽量沿用 `trader-collector` 的组织方式和打包方式。

### 2. 包结构建议

在 `trader-statistic` 中新增与统计职责对应的包:

- `config`
- `dao`
- `dao.entity`
- `dao.mapper`
- `service`
- `task`
- `aspect` 或任务日志相关支持

这样可以直接承接从 `trader-admin` 迁移过来的后台统计代码。

## 代码迁移设计

### 1. Correlation 实体与 DAO 链路迁移

迁移以下对象到 `trader-statistic`:

- `Correlation`
- `CorrelationDuplicateGroup`
- `CorrelationMapper`
- `CorrelationDao`

迁移后:

- `trader-statistic` 负责对 `correlation` 表的写入与清理
- `trader-admin` 不再持有这套写入 DAO

### 2. CorrelationService 拆分

当前 `CorrelationService` 同时包含:

- 前台查询/管理逻辑
- 后台计算/清理逻辑

迁移时应拆成两部分:

#### trader-statistic

保留后台职责:

- `calculateAndSave`
- `saveCorrelation`
- `cleanupHistoricalCorrelations`
- 与基金净值数据读取、相关系数计算、历史清理相关的方法

#### trader-admin

保留前台职责:

- `list`
- `detail`
- `add`
- `update`
- `delete`

如果前后台都需要访问 `correlation` 表，可以在 `trader-admin` 中保留只读 DAO 或读侧查询实现，但应移除任务计算/清理责任。

### 3. CorrelationTask 迁移

`CorrelationTask` 完整迁移到 `trader-statistic`。

要求:

- 保持定时任务能力
- 保持任务日志记录能力
- 保持“等待本轮计算结束后再清理历史”的语义

如果当前依赖 `TaskLogAspect`、`TaskContextUtil` 或其他 `trader-base`/admin 内组件，需要在 `trader-statistic` 中一并补齐最小依赖链。

## admin 与 statistic 的职责边界

### trader-statistic

- 定时触发相关性全量计算
- 读基金净值数据
- 计算相关系数
- 写入 `correlation`
- 清理旧版本

### trader-admin

- 查询相关性列表
- 查询详情
- 人工新增/编辑/删除
- 为前台页面提供管理接口

## 数据访问设计

两边共享现有数据库与 ClickHouse 表。

推荐职责:

- `trader-statistic` 写 `correlation`
- `trader-admin` 读 `correlation`

若保留 `trader-admin` 的人工编辑能力，则 admin 仍需要写权限，但统计任务不再由 admin 承担。

## 部署设计

### package.sh

参照 `trader-collector/package.sh`:

- 先执行 Maven 打包
- 校验 jar 可读
- 复制 `application.yml`、`logback.xml`、`config.properties`
- 复制 `docker-compose.yml`
- 复制 `bin/run.sh`
- 打出 `target/trader-statistic.tar.gz`

### docker-compose.yml

参照 `trader-collector/docker-compose.yml`，调整:

- 服务名为 `statistic` 或 `trader-statistic`
- 容器名为 `trader-statistic`
- jar 名为 `trader-statistic.jar`
- 端口单独分配

### run.sh

参照 collector 的启动方式:

- 从 `/app/config.properties` 读取外部配置
- 挂载 `/app/config`
- 支持 `JAVA_OPTS`

## 配置与依赖注意点

### 1. datasource

`trader-statistic` 需要具备:

- MySQL 数据源
- ClickHouse 数据源

若依赖 `trader-base` 的多数据源注解与配置，需要将所需配置类一并迁入或复用。

### 2. 任务日志

如果希望保留和 admin 一致的任务日志体验，需要迁入:

- `TaskLogRecord`
- `TaskLogAspect`
- `TaskContextUtil`
- 对应 DAO/实体/日志写入逻辑

如果本次不完整迁移任务日志，则至少要保证:

- 定时任务可运行
- 关键步骤有日志输出

### 3. 基金数据依赖

相关性计算依赖基金及净值数据读取:

- `FundDao`
- `FundNavDao`

这些 DAO 需要在 `trader-statistic` 中可用，或者由共享数据库直接读取。

## 测试策略

### trader-statistic

- 结构测试: `pom.xml`、打包脚本、docker-compose 是否存在
- 服务测试: 相关性计算与清理逻辑
- 任务测试: `CorrelationTask` 可启动并触发计算/清理

### trader-admin

- 保留相关性查询接口测试
- 验证前台查询链路在后台迁出后仍可工作

## 迁移顺序

1. 为 `trader-statistic` 补齐工程骨架和依赖
2. 迁入相关性实体/dao/mapper/service/task
3. 补齐配置、日志与数据源依赖
4. 增加打包脚本与 docker-compose
5. 回收 `trader-admin` 中的后台统计职责
6. 保留 admin 的查询与管理接口

## 风险

### 风险 1: CorrelationService 拆分不彻底

后台计算和前台管理逻辑目前耦合在同一个 service 中。

缓解:

- 明确以“后台职责迁入、前台职责留在 admin”为切分标准

### 风险 2: task log 依赖链较深

如果 `CorrelationTask` 强依赖 admin 的任务日志体系，直接迁移可能牵出较多代码。

缓解:

- 先识别最小运行依赖
- 必要时第一阶段只保留基础日志，不强求完整管理日志后台

### 风险 3: 两边同时写 correlation

如果 admin 仍保留人工编辑，而 statistic 负责批量写入，就会形成双写场景。

缓解:

- 明确保留 admin 人工写权限是业务要求
- 依赖现有 append-only + 清理策略保持一致性

## 结论

采用方案 1:

- `trader-statistic` 仿照 `trader-collector` 补齐工程、打包和部署
- 后台相关性计算与历史清理迁入 `trader-statistic`
- `trader-admin` 保留前台查询与管理接口

这样可以在不扰动 `admin-web` 的前提下，把统计任务职责从 admin 中拆出去。
