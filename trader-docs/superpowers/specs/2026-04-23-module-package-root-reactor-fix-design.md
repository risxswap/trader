# Module Package Root Reactor Fix Design

## 背景

`trader-collector`、`trader-executor`、`trader-statistic` 三个模块都依赖 `cc.riskswap.trader:base`。它们当前的 `package.sh` 都是在模块目录内直接执行：

```bash
"$MVNW" -DskipTests package
```

这种做法会让 Maven 以当前子模块 `pom.xml` 作为构建入口，依赖 `base` 是否能被正确解析，取决于本地仓库中是否恰好已有可用的 `base` 构件。运行时出现：

```text
java.lang.NoClassDefFoundError: cc/riskswap/trader/base/dao/entity/FundNav
```

说明最终运行包没有稳定地把 `base` 相关依赖带进去。

## 目标

修复这三个模块的 `package.sh`，让它们都从仓库根 reactor 发起 Maven 构建，稳定带上 `trader-base`，避免运行包缺少 `base` 中的类。

## 非目标

- 不修改 `trader-admin/package.sh`
- 不修改根 `build.sh`
- 不调整各模块产物目录和发布包结构
- 不增加并行打包

## 设计

### 修复范围

仅修改：

- `/Users/ming/Workspace/trader/trader-collector/package.sh`
- `/Users/ming/Workspace/trader/trader-executor/package.sh`
- `/Users/ming/Workspace/trader/trader-statistic/package.sh`

### Maven 入口

把当前模块内直接执行的：

```bash
"$MVNW" -DskipTests package
```

统一替换为：

```bash
"$MVNW" -f "$ROOT_DIR/../pom.xml" -pl <module> -am -DskipTests package
```

其中：

- `trader-collector` 使用 `-pl trader-collector`
- `trader-executor` 使用 `-pl trader-executor`
- `trader-statistic` 使用 `-pl trader-statistic`

### 保持不变的部分

以下逻辑保持原样：

- 产物文件名
- `target/` 下的打包目录结构
- `docker-compose.yml` / `application.yml` / `logback-spring.xml` / `config.properties` 复制逻辑
- `tar.gz` 压缩逻辑
- `sha256` 打印逻辑

## 为什么这样设计

根因不是业务代码，而是构建入口不稳定。只要从仓库根 `pom.xml` 发起并加上 `-am`，Maven 就能在同一个 reactor 中先构建 `trader-base`，再构建目标模块，避免对本地仓库旧产物的隐式依赖。

这也是已经在 `trader-admin/package.sh` 上验证过可行的修复思路，改动最小、风险最低。

## 测试策略

### 结构测试

新增或补充脚本结构测试，锁定这三个模块的 `package.sh` 必须包含：

- `-f "$ROOT_DIR/../pom.xml"`
- 对应模块的 `-pl ...`
- `-am`

### 真实打包验证

分别执行：

```bash
cd /Users/ming/Workspace/trader/trader-collector && ./package.sh
cd /Users/ming/Workspace/trader/trader-executor && ./package.sh
cd /Users/ming/Workspace/trader/trader-statistic && ./package.sh
```

验证：

- 命令退出码为 `0`
- 构建日志显示从根 `pom.xml` 启动或带上 `base`
- 现有发布包产物正常生成

## 风险与取舍

### 构建耗时略增

从根 reactor 构建可能比子模块内直接执行略慢，但换来的是依赖解析稳定性，属于可接受成本。

### 不处理历史错误产物

本次只修脚本，不清理远端或历史部署包。部署时仍需使用修复后重新生成的产物。
