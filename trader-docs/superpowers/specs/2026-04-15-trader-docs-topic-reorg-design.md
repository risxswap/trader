# trader-docs 按主题重组（方案 A）

## 目标

- 将当前 `trader-docs/` 中“按模块镜像”的文档结构重组为“按主题”结构：架构 / 产品 / 运维 / 开发规范等
- 物理移动文件到新目录（不是仅做索引页）
- 重组后删除旧的模块镜像目录（例如 `trader-docs/trader-admin/docs/**`）
- 将文档内的本机绝对路径链接（例如基于 `file://` 的路径跳转）统一替换为仓库内可迁移的相对路径链接
- `superpowers` 文档作为独立主题区统一收口，不与产品/运维/架构混放

## 非目标

- 不改业务代码、不调整模块构建与运行逻辑
- 不在本次重写文档内容（仅做结构重组 + 链接修复 + 新增索引）
- 不引入软链接或兼容旧路径（旧目录直接删除）

## 新目录结构

```
trader-docs/
  README.md
  architecture/
  product/
  ops/
  dev/
  superpowers/
    plans/
    specs/
  modules/
  examples/
  .obsidian/              #（如果保留 Obsidian 配置）
```

说明：

- `modules/`：按模块的入口页（每个模块 1 个文件），用于从模块视角汇总跳转到各主题文档
- `examples/`：样例数据与示例文件（例如 collector 的 csv/json/目录等）
- `.obsidian/`：如果需要把 `trader-docs` 当成 Obsidian Vault，则把原先 `trader-admin/docs/.obsidian` 移到 `trader-docs/.obsidian`

## 迁移来源与映射规则

### 1) 顶层文档

| 旧路径 | 新路径 |
|---|---|
| `trader-docs/design_implementation.md` | `trader-docs/architecture/design_implementation.md` |
| `trader-docs/requirements.md` | `trader-docs/product/requirements.md` |
| `trader-docs/README.md` | `trader-docs/README.md`（更新导航为主题目录） |

### 2) code-wiki（现位于 trader-admin/docs/code-wiki）

来源：`trader-docs/trader-admin/docs/code-wiki/*.md`

映射：

- `00-Architecture.md`、`01-Modules.md`、`02-Dependency-Graph.md` → `architecture/`
- `08-Runbook.md` → `ops/`
- `03-Backend-Core.md`、`04-Admin-Server.md`、`05-Collector.md`、`06-Executor.md`、`07-Admin-Web.md` → `dev/`

### 3) 业务功能文档（现位于 trader-admin/docs 根）

来源：`trader-docs/trader-admin/docs/*.md`（排除 `Code-Wiki.md`、排除 `superpowers/**`、排除 `.obsidian/**`）

映射：

- `00需求分析.md`、`00功能模块.md`、`01-鉴权与用户.md`…`11-节点管理.md` → `product/`
- `Code-Wiki.md`（如果是 code-wiki 导航）→ 合并到 `architecture/` 或改为 `trader-docs/README.md` 中的导航（避免重复入口）

### 4) superpowers 文档（plans/specs）

来源：

- `trader-docs/trader-admin/docs/superpowers/plans/**`
- `trader-docs/trader-admin/docs/superpowers/specs/**`
- `trader-docs/trader-base/docs/superpowers/plans/**`
- `trader-docs/trader-base/docs/superpowers/specs/**`
- `trader-docs/plans/**`（目前用于本仓库的迁移/README 计划）

映射：

- 统一收口到：
  - `trader-docs/superpowers/plans/**`
  - `trader-docs/superpowers/specs/**`

约束：

- 不再按模块拆分 superpowers 文档目录
- 保留原文件名（日期 + 主题），避免破坏可追溯性

### 5) 样例数据（collector docs）

来源：`trader-docs/trader-collector/docs/**`

映射：

- `trader-docs/trader-collector/docs/**` → `trader-docs/examples/collector/**`

### 6) trader-docs 内按模块入口页

来源：

- `trader-docs/trader-admin/README.md`
- `trader-docs/trader-base/README.md`
- `trader-docs/trader-collector/README.md`

映射（按方案 A 收敛入口）：

- 迁移后不再保留 `trader-docs/trader-admin/` 这类模块镜像目录
- 为每个模块新增入口页：
  - `trader-docs/modules/trader-admin.md`
  - `trader-docs/modules/trader-base.md`
  - `trader-docs/modules/trader-collector.md`
  - `trader-docs/modules/trader-executor.md`
  - `trader-docs/modules/trader-statistic.md`

每个入口页包含：

- 模块一句话定位
- 相关主题文档链接（architecture/product/dev/ops/superpowers/examples）
- 指向模块根目录 README（例如 `../.. /trader-admin/README.md` 这种相对路径）

## 链接修复策略

### 1) 文档间相对链接

- 迁移后，统一把跨文档引用改成相对路径（相对于当前 md 文件位置）
- 对导航入口（`trader-docs/README.md` 与 `modules/*.md`）确保链接不依赖旧路径

### 2) 本机绝对路径链接（含 /Users/haiming）

目标：消除机器用户名与绝对路径依赖。

策略：

- 将 `../../../<repo-relative-path>` 替换为“从当前文档到仓库根”的相对链接：
  - 例如在 `trader-docs/dev/06-Executor.md` 中引用 `trader-executor/src/...`：
    - 替换为 `../../trader-executor/src/...`（具体层级按最终文件位置计算）
- 将 `../../../...` 同样替换为相对链接

约束：

- 仅处理指向仓库内的路径；如果指向仓库外路径则保持原样或改为纯文本（视实际内容）

## 删除策略（旧路径移除）

重组完成后，删除以下旧目录/文件（如内容已被迁移）：

- `trader-docs/trader-admin/`（整个模块镜像目录）
- `trader-docs/trader-base/`（整个模块镜像目录）
- `trader-docs/trader-collector/`（整个模块镜像目录）
- `trader-docs/plans/`（迁移到 `trader-docs/superpowers/plans/` 或另行归档后移除）

## 验收标准

- `trader-docs/` 顶层按主题目录可浏览，并且 `trader-docs/README.md` 提供按主题导航与模块入口
- 旧的模块镜像目录不再存在
- `trader-docs/**/*.md` 中不再出现：
  - `/U[s]ers/`
- 文档链接检查通过（以全仓库 grep + 关键页面抽查为准）
