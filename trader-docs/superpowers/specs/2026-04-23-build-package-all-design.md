# Build Package-All Design

## 背景

根目录 `build.sh` 当前支持：

- `./build.sh package <module>`
- `./build.sh full-install`
- `./build.sh help`

其中 `package <module>` 会优先复用模块自己的 `package.sh`，没有模块脚本时再回退到根目录 Maven 构建。现在缺少一个“按既有模块列表逐个打包所有模块”的统一入口，导致需要手工多次执行命令。

## 目标

在根目录 `build.sh` 中增加 `package-all` 命令，用一条命令顺序打包所有受支持模块，并保持各模块现有产物结构与打包方式不变。

## 非目标

- 不统一各模块产物目录
- 不改造各模块 `package.sh` 的发布包格式
- 不引入并行打包
- 不替代 `full-install`

## 设计

### 命令

新增：

```bash
./build.sh package-all
```

### 执行策略

`package-all` 按 `SUPPORTED_MODULES` 当前顺序逐个处理：

1. `trader-admin`
2. `trader-base`
3. `trader-collector`
4. `trader-executor`
5. `trader-statistic`

每个模块复用当前单模块 `package` 的行为：

- 如果 `<module>/package.sh` 存在：
  - 赋予执行权限
  - 直接执行该脚本
- 如果 `<module>/package.sh` 不存在：
  - 调用根目录 Maven Wrapper：
  - `./mvnw -pl <module> -am clean package -DskipTests`

### 失败处理

- 任一模块打包失败，`package-all` 立即退出非 `0`
- 不做失败模块收集后继续执行
- 保持现有脚本的 `set -euo pipefail` 行为

### 输出

- 在每个模块开始前打印 `Packaging <module>...`
- 模块成功后打印 `Packaged <module>`
- 帮助文案中增加 `./build.sh package-all`

## 兼容性

- 现有 `package <module>` 行为不变
- 现有 `full-install` 行为不变
- 各模块现有 `package.sh` 无需修改即可被 `package-all` 复用

## 测试策略

### 结构测试

增加脚本结构测试，锁定：

- `help` 文案包含 `package-all`
- 脚本能识别 `package-all`
- `package-all` 会遍历 `SUPPORTED_MODULES`
- `package-all` 内部复用单模块打包逻辑，而不是复制一套新的模块分支

### 集成验证

执行：

```bash
./build.sh package-all
```

验证：

- 命令退出码为 `0`
- 各模块按顺序完成打包
- 每个模块继续产出其既有格式的发布物

## 风险与取舍

### 串行耗时

串行打包会比单模块更慢，但实现最简单，也最符合当前各模块脚本各自管理产物的结构。

### 模块脚本差异

不同模块当前产物目录不同，但 `package-all` 只负责编排，不负责统一，因此不会扩大本次改动范围。
