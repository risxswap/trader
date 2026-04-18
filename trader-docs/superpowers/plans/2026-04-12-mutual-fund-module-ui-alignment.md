# 公募基金模块 UI 对齐 ETF 方案 B Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将公募基金模块的列表页、详情页、净值页统一到 ETF 当前完成态的页面结构和信息层级。

**Architecture:** 本轮只修改公募基金前端 3 个页面，保持现有接口、路由和业务流程不变，复用 ETF 模块已验证的 page-container、meta-strip、table-card、drawer、详情概览区和图表空态模式。实现顺序为先统一列表页，再统一详情页，最后统一净值列表页，并在每个阶段执行诊断与构建验证。

**Tech Stack:** Vue 3、TypeScript、Vite、Element Plus、vue-router、dayjs、vue-echarts

---

## File Map

### Existing files to modify

- `admin-web/src/pages/funds/List.vue`
  - 公募基金列表、筛选、排序、编辑、删除入口
- `admin-web/src/pages/funds/Detail.vue`
  - 公募基金详情、查询区间、净值趋势图
- `admin-web/src/pages/funds/NavList.vue`
  - 公募基金净值列表查询与分页浏览

### Existing files to inspect during implementation

- `admin-web/src/pages/etf/List.vue`
  - 列表页统一样板、摘要条、抽屉上下文来源
- `admin-web/src/pages/etf/Detail.vue`
  - 详情页概览区、查询上下文、图表空态来源
- `admin-web/src/pages/etf/MarketList.vue`
  - 结果上下文和数值表现层级来源
- `admin-web/src/pages/etf/AdjList.vue`
  - 数据列表摘要条来源
- `admin-web/package.json`
  - 当前只有 `build` 可用作最终验证

### Verification targets

- VS Code diagnostics for:
  - `admin-web/src/pages/funds/List.vue`
  - `admin-web/src/pages/funds/Detail.vue`
  - `admin-web/src/pages/funds/NavList.vue`
- `npm run build`

## Constraints

- 不改路由路径、接口调用方法和请求参数
- 不新增依赖
- 不改后端接口和 DTO
- 不新增全局状态
- 列表页编辑交互统一改为右侧抽屉
- 详情页趋势图保留原有数据加载和两侧扩展逻辑

## Task 1: 建立公募基金模块实施基线

**Files:**
- Inspect: `admin-web/package.json`
- Inspect: `admin-web/src/pages/funds/List.vue`
- Inspect: `admin-web/src/pages/funds/Detail.vue`
- Inspect: `admin-web/src/pages/funds/NavList.vue`
- Inspect: `admin-web/src/pages/etf/List.vue`
- Inspect: `admin-web/src/pages/etf/Detail.vue`
- Inspect: `admin-web/src/pages/etf/MarketList.vue`

- [ ] **Step 1: 确认本轮只处理 3 个公募基金页面**

需要确认：

```text
List.vue 为旧式单卡片 + 弹窗编辑
Detail.vue 为旧式 page-header + 简单 descriptions + 单图表
NavList.vue 为旧式单卡片表格页
```

- [ ] **Step 2: 确认可复用的 ETF 页面模式**

重点对照：

```text
etf/List.vue 的 page-header、meta-strip、search-card、table-card、drawer-context
etf/Detail.vue 的 meta-strip、summary-grid、chart-empty
etf/MarketList.vue 的 queryModeText、resultDateRange、数值颜色层级
```

- [ ] **Step 3: 记录可用验证命令**

仅使用以下最终校验命令：

```json
"scripts": {
  "dev": "vite",
  "build": "vite build",
  "preview": "vite preview"
}
```

- [ ] **Step 4: 运行基线构建**

Run:

```bash
npm run build
```

Expected:

```text
vite build 成功，可接受 chunk size warning，但不能有编译错误
```

## Task 2: 重构公募基金列表页为统一复杂列表样板

**Files:**
- Modify: `admin-web/src/pages/funds/List.vue`
- Inspect: `admin-web/src/pages/etf/List.vue`

- [ ] **Step 1: 保留现有数据流和路由跳转接口**

以下逻辑必须保留：

```ts
listFunds({
  pageNo,
  pageSize,
  keyword,
  sortBy,
  sortOrder,
  market: 'O',
  management,
  custodian,
  fundType,
  managementFeeMin,
  managementFeeMax,
  custodianFeeMin,
  custodianFeeMax
})

router.push(`/funds/detail/${row.code}`)
updateFund(editForm.value.code, payload)
deleteFund(row.code)
```

- [ ] **Step 2: 把旧式单卡片模板改成统一列表页骨架**

目标模板形态：

```vue
<div class="page-container">
  <div class="page-header">...</div>
  <div class="meta-strip">...</div>
  <el-card class="search-card">...</el-card>
  <el-card class="table-card">...</el-card>
  <el-drawer v-model="editorVisible">...</el-drawer>
</div>
```

- [ ] **Step 3: 增加列表上下文计算属性**

新增以下计算能力：

```ts
const totalPage = computed(() => ...)
const activeFilterCount = computed(() => ...)
const sortDescription = computed(() => ...)
const averageManagementFee = computed(() => ...)
```

- [ ] **Step 4: 将表格列改为主副文案结构**

需要统一成：

```text
基金信息：名称 + 代码
状态与市场：状态 tag + 市场/交易所
机构信息：管理人 + 托管人
费率信息：管理费 + 托管费
时间信息：上市时间 + 更新时间
```

- [ ] **Step 5: 将编辑弹窗改成右侧抽屉并补充编辑上下文**

目标模板形态：

```vue
<el-drawer v-model="editorVisible" direction="rtl" size="520px">
  <div class="drawer-header">...</div>
  <el-form class="drawer-form">
    <div class="drawer-context">...</div>
    <div class="form-section">...</div>
  </el-form>
</el-drawer>
```

- [ ] **Step 6: 运行页面诊断并做构建验证**

Run:

```bash
npm run build
```

Expected:

```text
PASS，List.vue 无模板或类型错误
```

## Task 3: 重构公募基金详情页为统一详情承载结构

**Files:**
- Modify: `admin-web/src/pages/funds/Detail.vue`
- Inspect: `admin-web/src/pages/etf/Detail.vue`

- [ ] **Step 1: 保留详情查询和趋势图数据加载逻辑**

以下逻辑必须保持：

```ts
getFundDetail(code.value)
listFundNavs({
  pageNo: 1,
  pageSize: 1000,
  code: code.value,
  startTime: range.value[0],
  endTime: range.value[1]
})
loadMoreLeft()
loadMoreRight()
onDataZoom()
```

- [ ] **Step 2: 把旧页面改成统一详情骨架**

目标模板形态：

```vue
<div class="page-container">
  <div class="page-header">...</div>
  <el-card class="search-card">...</el-card>
  <div class="meta-strip">...</div>
  <div class="content-grid">
    <el-card class="info-card">...</el-card>
    <el-card class="chart-card">...</el-card>
  </div>
</div>
```

- [ ] **Step 3: 增加查询上下文和概览计算属性**

新增以下能力：

```ts
const rangeText = computed(() => ...)
const chartPointCount = computed(() => ...)
const loadedRangeText = computed(() => ...)
const formatDate = (value?: string) => ...
const formatDateTime = (value?: string) => ...
const formatPercent = (value?: number) => ...
```

- [ ] **Step 4: 把基础信息拆成概览卡 + 分组 descriptions**

信息区至少包括：

```text
概览卡：基金代码、当前状态、费率概览、成立/上市时间
基金档案：代码、名称、状态、投资类型
机构与费率：管理人、托管人、管理费、托管费
市场时间：市场、交易所、上市时间、成立日期、创建时间、更新时间
```

- [ ] **Step 5: 给趋势图区补充上下文和空态**

目标模板形态：

```vue
<div class="card-toolbar__meta">
  <span>当前 {{ chartPointCount }} 点</span>
  <span v-if="loadedStart">起始 {{ loadedStart }}</span>
  <span v-if="loadedEnd">结束 {{ loadedEnd }}</span>
</div>

<div v-if="chartPointCount" class="chart-shell">
  <VChart ... />
</div>
<div v-else class="chart-empty">
  <el-empty description="当前区间暂无净值数据" :image-size="60" />
</div>
```

- [ ] **Step 6: 运行页面诊断并做构建验证**

Run:

```bash
npm run build
```

Expected:

```text
PASS，Detail.vue 无编译错误，图表实例引用无类型问题
```

## Task 4: 重构公募基金净值页为统一数据列表样板

**Files:**
- Modify: `admin-web/src/pages/funds/NavList.vue`
- Inspect: `admin-web/src/pages/etf/MarketList.vue`
- Inspect: `admin-web/src/pages/etf/AdjList.vue`

- [ ] **Step 1: 保留净值页查询与分页逻辑**

以下调用保持不变：

```ts
listFundNavs({
  pageNo: pageNo.value,
  pageSize: pageSize.value,
  code: q.value.code,
  startTime,
  endTime
})
```

- [ ] **Step 2: 改成统一业务列表页骨架**

目标模板形态：

```vue
<div class="page-container">
  <div class="page-header">...</div>
  <el-card class="search-card">...</el-card>
  <div class="meta-strip">...</div>
  <el-card class="table-card">...</el-card>
</div>
```

- [ ] **Step 3: 增加结果上下文计算属性**

新增以下能力：

```ts
const totalPage = computed(() => ...)
const rangeText = computed(() => ...)
const queryModeText = computed(() => ...)
const resultDateRange = computed(() => ...)
const averageUnitNav = computed(() => ...)
```

- [ ] **Step 4: 把净值列改成更清晰的层级表达**

建议统一成：

```text
基金信息：代码 + 日期
单位净值：主值
累计净值：主值
复权净值：主值 + 级别提示
更新时间：格式化时间
```

- [ ] **Step 5: 补全格式化和响应式样式**

至少新增：

```ts
formatDate()
formatDateTime()
formatNumber()
navLevelText()
```

```css
.meta-strip { ... }
.primary-cell { ... }
.secondary-cell { ... }
@media (max-width: 768px) { ... }
```

- [ ] **Step 6: 运行页面诊断并做构建验证**

Run:

```bash
npm run build
```

Expected:

```text
PASS，NavList.vue 无模板或类型错误
```

## Task 5: 全量验证与收尾

**Files:**
- Verify: `admin-web/src/pages/funds/List.vue`
- Verify: `admin-web/src/pages/funds/Detail.vue`
- Verify: `admin-web/src/pages/funds/NavList.vue`
- Verify: `admin-web/package.json`

- [ ] **Step 1: 检查 3 个目标文件诊断**

Expected:

```text
无 Error 级别诊断；Hint 可按需清理，但不允许存在新增未使用变量
```

- [ ] **Step 2: 运行最终构建**

Run:

```bash
npm run build
```

Expected:

```text
vite build 成功完成
```

- [ ] **Step 3: 记录结果边界**

最终结果应满足：

```text
公募基金列表页对齐 ETF 列表完成态
公募基金详情页对齐 ETF 详情完成态
公募基金净值页对齐 ETF 数据列表完成态
未改任何后端接口、路由路径和业务语义
```
