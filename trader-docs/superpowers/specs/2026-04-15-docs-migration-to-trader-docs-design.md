# 文档迁移到 trader-docs（方案 A：按模块镜像迁移）

## 目标

- 将仓库内各模块的文档与样例数据统一迁移到 `trader-docs/` 下集中管理
- 迁移后更新所有引用，并删除旧的 `docs/` 目录
- 各模块根目录保留指针 `README.md`，便于开发者从模块入口跳转到新文档位置
- `rules/`、`.trae/rules/` 等工具依赖的规则文件不迁移，避免路径变更导致工具失效

## 范围

### 纳入迁移

- `trader-admin/docs/**`
- `trader-base/docs/**`
- `trader-collector/docs/**`（包含 csv/json/目录等样例数据）
- 各模块根目录 `README.md`（正文迁移到 `trader-docs`，原位置改为指针）

### 不纳入迁移

- `**/rules/**`、`**/.trae/rules/**`（工具/规则文件）
- 构建产物与缓存目录（例如 `**/target/**`）

## 迁移后的目录结构

```
trader-docs/
  README.md
  requirements.md
  design_implementation.md
  trader-admin/
    README.md
    docs/...
  trader-base/
    README.md
    docs/...
  trader-collector/
    README.md
    docs/...
```

约束：

- 采用“按模块镜像”的方式尽量保持原目录层级不变，降低相对链接断裂概率
- `trader-docs/README.md` 作为统一入口，提供导航到各模块文档

## 源路径到目标路径映射

| 源路径 | 目标路径 |
|---|---|
| `trader-admin/docs/**` | `trader-docs/trader-admin/docs/**` |
| `trader-base/docs/**` | `trader-docs/trader-base/docs/**` |
| `trader-collector/docs/**` | `trader-docs/trader-collector/docs/**` |
| `trader-admin/README.md` | `trader-docs/trader-admin/README.md` |
| `trader-base/README.md` | `trader-docs/trader-base/README.md` |
| `trader-collector/README.md` | `trader-docs/trader-collector/README.md` |

## 原位置处理策略

- 删除原 `docs/` 目录（迁移完成且引用更新后）
- 原模块 `README.md` 改为指针：
  - 保留简短模块说明（如需要）
  - 明确标注新文档入口链接到 `trader-docs/<module>/README.md`
  - 如有重要入口（runbook、架构、运维等）可追加 2-3 条直达链接

## 链接与引用修复策略

### 文档内部相对链接

- 目标：迁移后文档内部相对链接可用
- 方式：
  - 镜像迁移可最大化保留相对路径结构
  - 对指向“模块外”的相对链接逐一修复（例如从 `trader-admin/docs/**` 指向 `../README.md` 等）

### 代码/脚本/配置中的文档引用

- 目标：仓库内不再出现指向旧 `docs/` 路径的引用
- 方式：
  - 全仓库扫描并更新 `docs/` 相关引用（例如 README、脚本输出提示、CI 文档链接）

## 执行步骤（高层）

1. 在 `trader-docs/` 下创建模块子目录并搬迁对应 `docs/**` 与 `README.md`
2. 更新各模块根目录 `README.md` 为指针内容
3. 全仓库扫描并修复引用（重点是 Markdown 链接与脚本/配置中的路径）
4. 删除旧 `docs/` 目录
5. 验证：
   - `git grep` 不再出现旧路径引用
   - 随机抽查核心文档的相对链接可用（至少覆盖 code-wiki 与 runbook）

## 验收标准

- `trader-admin/docs`、`trader-base/docs`、`trader-collector/docs` 不再存在
- 迁移后的文档在 `trader-docs/` 下可完整找到且目录结构清晰
- 各模块根目录 `README.md` 可跳转到 `trader-docs` 中对应入口
- 仓库内无旧路径引用残留（以全仓库搜索结果为准）

