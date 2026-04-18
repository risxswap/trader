# Admin Web 第一轮全局 UI 优化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 admin-web 建立第一轮可复用的全站 UI 基线，统一布局框架、页签导航、节点卡片页和投资复杂列表页。

**Architecture:** 本轮只处理 4 个前端文件，先统一全局骨架，再沉淀两类页面模板。保持现有路由、接口、业务流程不变，只优化页面结构、视觉层级、响应式布局和局部交互表达。

**Tech Stack:** Vue 3、TypeScript、Vite、Element Plus、vue-router、vue-echarts

---

## File Map

### Existing files to modify

- `admin-web/src/layouts/MainLayout.vue`
  - 负责全站骨架、侧边栏导航、顶部栏、主内容区、修改密码弹窗
- `admin-web/src/layouts/TagsView.vue`
  - 负责页签导航展示、当前激活标签样式、关闭交互
- `admin-web/src/pages/node/List.vue`
  - 负责节点卡片列表、分组筛选、详情弹窗、编辑弹窗、审批弹窗
- `admin-web/src/pages/investment/List.vue`
  - 负责投资列表、筛选栏、表格、分页、新增编辑弹窗、持仓抽屉

### Existing files to inspect during implementation

- `admin-web/package.json`
  - 当前只有 `dev`、`build`、`preview` 脚本，没有 lint/typecheck 独立脚本
- `docs/superpowers/specs/2026-04-12-admin-web-global-ui-redesign-design.md`
  - 当前实施 plan 对应的设计输入

### Verification targets

- `npm run build`
- VS Code diagnostics for the four modified Vue files

## Constraints

- 不改菜单结构、路由、接口调用路径和业务逻辑
- 不新增 UI 依赖
- 不新增全局状态管理
- 不把节点页改成普通表格页，仍保留卡片型表达
- 不把投资页改成看板，仍保持标准后台复杂列表页

## Task 1: 建立实施基线

**Files:**
- Inspect: `admin-web/package.json`
- Inspect: `admin-web/src/layouts/MainLayout.vue`
- Inspect: `admin-web/src/layouts/TagsView.vue`
- Inspect: `admin-web/src/pages/node/List.vue`
- Inspect: `admin-web/src/pages/investment/List.vue`

- [ ] **Step 1: 记录当前可用验证命令**

确认仅有以下脚本可用：

```json
"scripts": {
  "dev": "vite",
  "build": "vite build",
  "preview": "vite preview"
}
```

- [ ] **Step 2: 运行基线构建**

Run:

```bash
npm run build
```

Expected:

```text
vite build 成功完成，可能存在 chunk size warning，但不能有编译错误
```

- [ ] **Step 3: 记录四个目标文件现状**

需要确认：

```text
MainLayout.vue 使用深色传统侧边栏
TagsView.vue 使用基础 el-tag 轻量页签
node/List.vue 包含统计卡 + 分组筛选 + 节点卡片
investment/List.vue 为单卡片包裹的复杂列表页
```

- [ ] **Step 4: 冻结本轮边界**

实现过程中仅允许修改：

```text
admin-web/src/layouts/MainLayout.vue
admin-web/src/layouts/TagsView.vue
admin-web/src/pages/node/List.vue
admin-web/src/pages/investment/List.vue
```

## Task 2: 重构 MainLayout.vue 为统一后台框架

**Files:**
- Modify: `admin-web/src/layouts/MainLayout.vue`
- Verify: `admin-web/src/router/index.ts`

- [ ] **Step 1: 先写最小失败验证**

Run:

```bash
npm run build
```

Expected:

```text
当前构建通过，作为后续布局样式调整的安全网
```

- [ ] **Step 2: 调整模板结构，给布局骨架补充清晰容器层级**

目标模板形态：

```vue
<el-container class="layout-shell">
  <el-aside class="layout-aside">...</el-aside>
  <el-container class="layout-main-shell">
    <el-header class="layout-header">...</el-header>
    <TagsView class="layout-tags" ... />
    <el-main class="layout-main">
      <div class="page-shell">
        <router-view />
      </div>
    </el-main>
  </el-container>
</el-container>
```

- [ ] **Step 3: 收敛菜单、顶部栏和主内容区视觉**

实现重点：

```ts
const active = computed(() => route.path)
const username = computed(() => { ... })
const collapsed = ref(false)
```

```css
.layout-shell {
  min-height: 100vh;
  background: #f5f7fb;
}

.layout-aside {
  background: linear-gradient(180deg, #1f2937 0%, #111827 100%);
  border-right: 1px solid rgba(255, 255, 255, 0.08);
}

.layout-header {
  height: 64px;
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid #e8edf5;
}

.layout-main {
  padding: 20px;
}

.page-shell {
  min-height: calc(100vh - 140px);
}
```

- [ ] **Step 4: 保持业务逻辑零变更**

必须确认这些逻辑不动：

```ts
toggleCollapse()
handleClickTag()
handleCloseTag()
submitChangePwd()
onLogout()
router.beforeEach()
```

- [ ] **Step 5: 运行构建验证布局修改**

Run:

```bash
npm run build
```

Expected:

```text
PASS，MainLayout.vue 无编译错误
```

## Task 3: 重构 TagsView.vue 为现代轻量页签

**Files:**
- Modify: `admin-web/src/layouts/TagsView.vue`
- Verify: `admin-web/src/layouts/MainLayout.vue`

- [ ] **Step 1: 保持 props 和 emits 不变**

以下接口必须完全保留：

```ts
defineProps<{
  visitedViews: TagView[]
  currentFullPath: string
}>()

defineEmits<{
  clickTag: [tag: TagView]
  closeTag: [tag: TagView]
}>()
```

- [ ] **Step 2: 把纯 el-tag 列表改成更稳定的页签容器**

目标模板形态：

```vue
<div class="tags-view">
  <el-scrollbar>
    <div class="tags-wrap">
      <div
        v-for="tag in visitedViews"
        :key="tag.fullPath"
        :class="['tag-chip', { 'is-active': tag.fullPath === currentFullPath }]"
        @click="onClickTag(tag)"
      >
        <span class="tag-chip__title">{{ tag.title }}</span>
      </div>
    </div>
  </el-scrollbar>
</div>
```

如果继续使用 `el-tag` 更稳，也允许保留组件，只需要实现同等层级表达。

- [ ] **Step 3: 实现当前页突出、非当前页安静的样式节奏**

```css
.tags-view {
  background: #f8fafc;
  border-bottom: 1px solid #e8edf5;
  padding: 10px 16px;
}

.tag-chip {
  height: 32px;
  padding: 0 12px;
  border: 1px solid #dde5f0;
  background: #fff;
  color: #4b5563;
  border-radius: 10px;
}

.tag-chip.is-active {
  color: #1d4ed8;
  border-color: #bfdbfe;
  background: #eff6ff;
}
```

- [ ] **Step 4: 确认关闭交互不回归**

要验证：

```text
affix 标签不可关闭
普通标签可关闭
点击标签仍然跳转
关闭当前标签仍回退到最近标签
```

- [ ] **Step 5: 运行构建验证页签修改**

Run:

```bash
npm run build
```

Expected:

```text
PASS，TagsView.vue 无编译错误
```

## Task 4: 重构 node/List.vue 为标准卡片型业务页

**Files:**
- Modify: `admin-web/src/pages/node/List.vue`
- Verify: `admin-web/src/pages/node/Group.vue`

- [ ] **Step 1: 先保留现有数据与弹窗逻辑，限制改动在模板和样式层**

必须保留：

```ts
load()
openDetail()
openEdit()
submitEdit()
openApprove()
submitApprove()
removeNode()
```

- [ ] **Step 2: 将顶部结构改成“标题区 + 精简概览 + 分组筛选区”**

建议调整为：

```vue
<div class="page-header">
  <div class="page-header__content">
    <div class="title">节点列表</div>
    <div class="subtitle">查看节点状态、审批流转与资源占用</div>
  </div>
  <div class="page-header__actions">
    <el-button ...>刷新</el-button>
  </div>
</div>

<div class="node-summary">
  <div class="summary-item">...</div>
</div>

<el-card class="filter-card">...</el-card>
```

- [ ] **Step 3: 将卡片内容重排为“主信息、状态标签、资源使用、操作区”**

卡片重点结构：

```vue
<el-card class="node-card">
  <div class="node-card__hero">
    <div class="node-card__identity">...</div>
    <div class="node-card__tags">...</div>
  </div>
  <div class="node-card__meta">...</div>
  <div class="node-card__usage">...</div>
  <div class="node-card__footer">...</div>
</el-card>
```

- [ ] **Step 4: 控制统计展示的存在感，不让页面回到重看板风格**

约束：

```text
允许保留少量摘要数据，但必须轻量
摘要区不能压过卡片主体
筛选区和卡片区仍然是页面主角
```

- [ ] **Step 5: 优化响应式与细节**

需要处理：

```text
768px 以下标题区换行
卡片最小宽度调整
meta 信息移动端单列
长 nodeId / IP 自动换行
操作按钮换行不挤压
```

- [ ] **Step 6: 运行构建验证节点页修改**

Run:

```bash
npm run build
```

Expected:

```text
PASS，node/List.vue 无编译错误
```

## Task 5: 重构 investment/List.vue 为标准复杂列表页

**Files:**
- Modify: `admin-web/src/pages/investment/List.vue`
- Verify: `admin-web/src/pages/investment/Detail.vue`
- Verify: `admin-web/src/pages/investment/PositionList.vue`

- [ ] **Step 1: 保持现有查询与 CRUD 行为不变**

必须保留：

```ts
load()
resetSearch()
onAdd()
onEdit()
onDelete()
submitForm()
onPositions()
loadStrategies()
loadBrokers()
```

- [ ] **Step 2: 把单层 el-card 页面改成标准页面骨架**

目标模板形态：

```vue
<div class="page-container">
  <div class="page-header">...</div>
  <el-card class="search-card">...</el-card>
  <el-card class="table-card">
    <div class="table-toolbar">...</div>
    <el-table ... />
    <div class="pager">...</div>
  </el-card>
  <el-dialog ... />
  <el-drawer ... />
</div>
```

- [ ] **Step 3: 统一筛选栏、主操作和表格字段层级**

表格层级建议：

```vue
<el-table-column label="计划信息" min-width="220">
  <template #default="{ row }">
    <div class="primary-cell">{{ row.name }}</div>
    <div class="secondary-cell">{{ row.groupName || '未分组' }}</div>
  </template>
</el-table-column>
```

```vue
<el-table-column label="收益表现">
  <template #default="{ row }">
    <div class="profit-amount">{{ formatCurrency(row.profitAmount) }}</div>
    <div class="profit-rate">{{ formatPercent(row.profitRate) }}</div>
  </template>
</el-table-column>
```

- [ ] **Step 4: 收敛弹窗与抽屉入口的节奏**

要求：

```text
新增按钮固定在标题区或工具栏
详情作为首个行内操作
持仓紧随详情
编辑、删除排在后面
弹窗标题、按钮文案、间距风格与全局保持一致
```

- [ ] **Step 5: 修正响应式与表格可读性**

需要处理：

```text
筛选项换行规则
按钮区在窄屏下垂直堆叠
表格列宽最小化但不挤压收益字段
分页区右对齐并在窄屏下换行
```

- [ ] **Step 6: 运行构建验证投资页修改**

Run:

```bash
npm run build
```

Expected:

```text
PASS，investment/List.vue 无编译错误
```

## Task 6: 做最终验证和交付检查

**Files:**
- Verify: `admin-web/src/layouts/MainLayout.vue`
- Verify: `admin-web/src/layouts/TagsView.vue`
- Verify: `admin-web/src/pages/node/List.vue`
- Verify: `admin-web/src/pages/investment/List.vue`

- [ ] **Step 1: 运行最终构建**

Run:

```bash
npm run build
```

Expected:

```text
PASS，无编译错误
```

- [ ] **Step 2: 获取四个文件的编辑器诊断**

检查目标：

```text
MainLayout.vue
TagsView.vue
node/List.vue
investment/List.vue
```

Expected:

```text
无 TypeScript / template diagnostics
```

- [ ] **Step 3: 如需人工预览，启动本地开发环境**

Run:

```bash
npm run dev -- --host 0.0.0.0
```

Expected:

```text
生成本地预览地址，可用于检查布局、页签、节点页、投资页
```

- [ ] **Step 4: 输出本轮交付结论**

交付说明必须包含：

```text
本轮改动文件
布局与页面样板沉淀结果
构建验证结果
未覆盖的后续模块
下一轮建议顺序
```

## Notes for Executor

- 当前仓库没有独立 `lint` 与 `typecheck` 脚本；执行时必须至少运行 `npm run build` 和编辑器 diagnostics
- 若执行中发现 MainLayout 的样式和单页样式重复冲突，优先在局部组件中解决，不要引入新的全局样式文件
- 若节点页轻量摘要区与用户前述反馈冲突，应优先遵守“列表/卡片主体优先”的要求，必要时可以把摘要区进一步弱化或移除
- 若投资页重构后表格信息过密，应先通过分组列和主次文字层级解决，不要无必要新增统计看板
