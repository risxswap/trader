# 05 - 采集服务（trader-collector）

`trader-collector`（工程名 `trader-fund`）负责从外部数据源拉取数据并落库，典型内容包含交易日历、基金基础信息、净值、行情、复权因子等。

## 1. 入口与启动

- 启动类：[collector Application](../../trader-collector/src/main/java/cc/riskswap/trader/collector/Application.java#L12-L25)
- 关键注解：
  - `@ComponentScan(basePackages={"cc.riskswap.trader"})`：使其能够扫描并使用 `trader-base` 提供的组件
  - `@EnableScheduling`：启用 Spring Scheduler（部分任务类使用定时触发）

## 2. 配置与外部依赖

- `application.yml`：见 [collector application.yml](../../trader-collector/src/main/resources/application.yml#L1-L56)
  - 数据库：使用 `trader.mysql.*`（由 `trader-base` 自动装配 MySQL DataSource + MyBatis-Plus）
  - Redis：使用 `trader.redis.*`
  - 外部数据源：`tushare.url` / `tushare.token`（建议通过环境变量或外部配置文件注入 token）
- 外部配置加载：见 [collector ConfigLoader](../../trader-collector/src/main/java/cc/riskswap/trader/collector/system/ConfigLoader.java#L17-L62)

## 3. 任务模型（TraderTask）

该服务以 `TraderTask` 的方式组织同步任务，便于被统一任务体系管理/调度。

- 示例任务：[CalendarSyncTask](../../trader-collector/src/main/java/cc/riskswap/trader/collector/task/CalendarSyncTask.java#L8-L50)
  - `taskCode=calendarSync`
  - 默认 Cron：`0 0 1 * * ?`
  - 参数 Schema / 默认参数：以 JSON 字符串返回（用于 UI 动态表单）
  - `execute()`：调用 `CalendarService.syncCalendar()`

## 4. 核心服务示例（Calendar）

- 服务：[CalendarService](../../trader-collector/src/main/java/cc/riskswap/trader/collector/service/CalendarService.java#L18-L57)
- 数据流：
  - 读取本地最新日历记录：`CalendarDao.getLatestByExchange`
  - 计算拉取区间并调用外部接口：`CalendarTushare.list(query)`
  - 删除区间内旧数据并批量写入：`CalendarDao.delete + saveBatch`

## 5. 外部数据源接入（TuShare）

- 通用访问层：[TushareManager](../../trader-collector/src/main/java/cc/riskswap/trader/collector/repository/tushare/TushareManager.java#L26-L164)
  - 基于 OkHttp
  - 通过滑动窗口限流（`MAX_REQUESTS_PER_MINUTE=80`）
  - `post(apiName, fields, params)`：封装 TuShare 标准请求体（token/api_name/fields/params）

## 6. 与 trader-base 的关系

该服务依赖 `trader-base`，因此可直接复用：

- DAO（如 [CalendarDao](../../trader-base/src/main/java/cc/riskswap/trader/base/dao/CalendarDao.java)）
- MySQL/ClickHouse 数据源自动装配（若启用）
- 统一任务模块（任务注册/元数据同步/调度刷新）
