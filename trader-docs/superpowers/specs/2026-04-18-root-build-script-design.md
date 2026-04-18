# 根目录统一构建脚本（方案 A）

## 目标

- 在仓库根目录新增一个统一入口脚本，用参数控制顶层模块的单独打包
- 为仓库提供一个显式的全量安装命令，执行跳过测试的 `install`
- 复用各模块现有 `package.sh`，避免重复维护打包逻辑

## 范围

### 新增

- 仓库根目录统一脚本，暂定名为 `build.sh`

### 修改

- `README.md` 中的快速开始或脚本说明，补充统一脚本的使用方式

### 不在本次范围

- 不改造各模块已有 `package.sh` 的具体打包实现
- 不支持 `admin-server`、`admin-web` 这类二级模块直传参数
- 不增加 CI 配置、发布流水线或 Docker 镜像构建逻辑

## 支持的模块

统一脚本按顶层模块工作，支持以下参数：

- `trader-admin`
- `trader-base`
- `trader-collector`
- `trader-executor`
- `trader-statistic`

## 命令设计

统一脚本提供以下命令：

1. `./build.sh package <module>`
2. `./build.sh full-install`
3. `./build.sh help`

### package

- 用途：打包单个顶层模块
- 行为：
  - 若模块目录下存在 `package.sh`，优先调用该脚本
  - 若不存在 `package.sh`，回退为根目录 Maven Wrapper 命令
  - 回退命令为 `./mvnw -pl <module> -am clean package -DskipTests`

### full-install

- 用途：执行全仓库跳过测试的安装构建
- 行为：
  - 在仓库根目录执行 `./mvnw clean install -DskipTests`

### help

- 输出用法、支持的命令和模块列表
- 当参数缺失或非法时自动输出帮助并返回非零状态

## 脚本行为约束

- 脚本运行目录固定为仓库根目录，不依赖调用者当前所在目录
- 优先使用根目录 `./mvnw`，不依赖全局 `mvn`
- 对非法模块名给出清晰错误提示
- 对缺失 `mvnw`、缺失模块目录、缺失 `package.sh` 回退失败等情况给出明确报错
- 脚本使用 `set -euo pipefail`，在任何步骤失败时立即退出

## 模块处理策略

### `trader-admin`

- 复用 `trader-admin/package.sh`
- 保留其现有后端 + 前端打包与归档流程

### `trader-collector`

- 复用 `trader-collector/package.sh`

### `trader-executor`

- 复用 `trader-executor/package.sh`

### `trader-statistic`

- 复用 `trader-statistic/package.sh`

### `trader-base`

- 当前无 `package.sh`
- 使用 Maven 回退路径完成 `clean package -DskipTests`

## 数据流

### `./build.sh package trader-collector`

1. 解析命令与模块参数
2. 校验模块是否在支持列表内
3. 进入对应模块目录
4. 若存在 `package.sh`，执行该脚本
5. 若不存在，回退执行根目录 Maven 打包命令
6. 将模块脚本或 Maven 的退出码原样传递给调用方

### `./build.sh full-install`

1. 解析命令
2. 校验根目录 `mvnw` 存在
3. 执行 `./mvnw clean install -DskipTests`
4. 将退出码原样传递给调用方

## 错误处理

- 未提供命令：输出帮助并退出 `1`
- `package` 缺少模块名：输出帮助并退出 `1`
- 模块名不受支持：输出支持列表并退出 `1`
- 根目录 `mvnw` 不存在或不可执行：退出 `1`
- 模块目录不存在：退出 `1`
- 模块级脚本执行失败：直接返回失败
- Maven 回退命令失败：直接返回失败

## 测试与验证

本次实现完成后需要验证：

1. `./build.sh help` 能输出帮助
2. `./build.sh package trader-base` 能触发 Maven 回退打包
3. `./build.sh package trader-collector` 能调用模块级 `package.sh`
4. `./build.sh full-install` 能执行根目录 `install -DskipTests`
5. 非法参数如 `./build.sh package foo` 能正确失败并输出提示

## 验收标准

- 根目录存在统一脚本且具备执行权限
- 支持按顶层模块执行单独打包
- 支持一个显式的全量跳过测试安装命令
- `trader-admin`、`trader-collector`、`trader-executor`、`trader-statistic` 复用现有 `package.sh`
- `trader-base` 通过 Maven 回退路径完成打包
- `README.md` 包含该脚本的基础使用说明
