# 模块：ETF

## 目标与范围

ETF 模块用于维护 ETF 基础信息，并提供行情（K 线）与复权因子数据的查询与列表查看能力。

本模块与“公募基金”共用后端基金数据模型与接口，通过 market='E' 区分 ETF 数据集。

## 前端设计（admin-web）

### 页面入口

- ETF 列表：/etf/list
  - 页面：[List.vue](../../trader-admin/admin-web/src/pages/etf/List.vue)
- ETF 详情：/etf/detail/:symbol
  - 页面：[Detail.vue](../../trader-admin/admin-web/src/pages/etf/Detail.vue)
- 基金行情：/etf/market
  - 页面：[MarketList.vue](../../trader-admin/admin-web/src/pages/etf/MarketList.vue)
- 复权因子：/etf/adj
  - 页面：[AdjList.vue](../../trader-admin/admin-web/src/pages/etf/AdjList.vue)

### ETF 列表页交互

- 查询条件：keyword、management、custodian、fundType、管理费/托管费区间
- 固定条件：market='E'
- 排序：
  - listDate/managementFee/custodianFee：服务端排序（sortBy/sortOrder）
  - updatedAt：前端对当前页数据排序
- 编辑/删除：
  - 编辑弹窗提交 PUT /fund/update/{symbol}
  - 删除提交 DELETE /fund/delete/{symbol}

### ETF 详情页交互（K 线图）

- 远程搜索基金代码：调用列表接口（pageSize=10）
- 查询条件：symbol + 日期范围
- 数据来源：POST /fund/market（返回 FundMarketDto[]）
- 图表：
  - 使用 ECharts candlestick 展示 OHLC
  - dataZoom 接近边界时，按 90 天向左/向右增量加载并合并去重（以页面实现为准）

### 行情/复权列表页交互

- 当同时提供 code + 日期范围：
  - 行情：POST /fund/market（返回 List）
  - 复权：POST /fund/adj（返回 List）
  - 页面将 total 设置为返回数组长度（不走分页）
- 当未提供 code 或日期范围：
  - 行情：POST /fund/market/list（分页）
  - 复权：POST /fund/adj/list（分页）

### API 调用

- POST /fund/list（market='E'）
- GET /fund/detail/{symbol}
- PUT /fund/update/{symbol}
- DELETE /fund/delete/{symbol}
- POST /fund/market
- POST /fund/market/list
- POST /fund/adj
- POST /fund/adj/list
  - API 实现：[fund.ts](../../trader-admin/admin-web/src/services/fund.ts)

## 后端设计（admin-server）

### 接口与职责分层

- Controller：[FundController.java](../../trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/controller/FundController.java)
- Service：
  - 基金基础/行情/复权查询：[FundService.java](../../trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/FundService.java)
  - 行情数据读取：[FundMarketService.java](../../trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/FundMarketService.java)
- Dao：FundDao / FundMarketDao / FundAdjDao

### 接口定义（与 ETF 相关部分）

- 行情区间查询
  - Path：POST /fund/market
  - Request：FundMarketQuery（code/startDate/endDate）
  - Response：ResData<List<FundMarketDto>>
  - 处理逻辑：FundMarketService.getData -> 映射 DTO

- 行情分页列表
  - Path：POST /fund/market/list
  - Request：FundMarketListQuery（pageNo/pageSize/code?/startDate?/endDate?）
  - Response：ResData<PageDto<FundMarketDto>>
  - 处理逻辑：FundMarketDao.pageQuery -> 映射 DTO

- 复权因子区间查询
  - Path：POST /fund/adj
  - Request：FundAdjQuery（code/startDate/endDate）
  - Response：ResData<List<FundAdjDto>>
  - 处理逻辑：FundAdjDao.listBySymbolAndDateRange -> 映射 DTO

- 复权因子分页列表
  - Path：POST /fund/adj/list
  - Request：FundAdjListQuery（pageNo/pageSize/code?/startDate?/endDate?）
  - Response：ResData<PageDto<FundAdjDto>>
  - 处理逻辑：FundAdjDao.pageQuery -> 映射 DTO

## 数据模型（MySQL）

表定义参考：[db.sql](../../trader-admin/admin-server/src/main/resources/database/db.sql)

- fund：ETF 与公募基金共用，ETF 记录通过 market='E' 区分
- fund_market：日行情（open/high/low/close/amount/pct_chg）
- fund_adj：复权因子（adj_factor）

## 关键流程（时序）

### ETF 详情 K 线查询

1. 前端请求 GET /fund/detail/{symbol} 获取 ETF 基本信息
2. 前端请求 POST /fund/market 获取指定日期区间行情
3. 前端将行情按日期升序转换为 candlestick 数据结构并渲染
4. dataZoom 接近边界时请求更多区间并合并去重

## 约束与扩展点

- 行情/复权列表页在“指定 code + 日期范围”时走非分页接口；数据量较大时可统一改为分页接口或在后端增加 limit 参数。
- ETF 与公募基金的区分当前完全依赖 market 字段；若需要更严格的数据约束，可在写入侧或数据库侧增加约束与索引策略。 
