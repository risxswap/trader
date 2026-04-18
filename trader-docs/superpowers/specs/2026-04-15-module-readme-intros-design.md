# 各模块 README 增加简单介绍（方案 A）

## 目标

- 为仓库各模块根目录的 `README.md` 增加简洁的模块说明，方便从目录结构快速理解模块职责
- 保持模块根目录 `README.md` 内容短小一致：简介 + 要点 + 文档入口链接
- 对尚无 `README.md` 的模块补齐基础 README（本次包含 `trader-executor`、`trader-statistic`）

## 范围

### 修改

- `trader-admin/README.md`
- `trader-base/README.md`
- `trader-collector/README.md`

### 新增

- `trader-executor/README.md`
- `trader-statistic/README.md`

### 不在本次范围

- `trader-docs/<module>/README.md`（本次仅补模块根目录入口 README）
- 代码与配置文件

## README 统一结构

每个模块根目录 `README.md` 采用相同结构：

1. 标题：`# <module>`
2. 简介：一句话说明模块作用
3. 要点：3–5 条，覆盖模块职责 / 运行形态 / 与其它模块关系等
4. 文档入口：
   - 对已有模块文档入口的模块：链接到 `../trader-docs/<module>/README.md`
   - 对暂无模块文档入口的模块：链接到 `../trader-docs/README.md`（总入口）
   - 如存在 code-wiki 对应页，可增加直达链接

## 内容要求

- 不写冗长教程与完整运行手册（避免与 `trader-docs` 重复）
- 不引入路径不稳定的绝对路径（仅用相对路径）
- 文案以中文为主，语气保持工程化、客观

## 验收标准

- 以上 5 个模块根目录均存在 `README.md`
- README 顶部能读到模块职责与关键要点
- README 中包含 `trader-docs` 文档入口链接且路径可用

