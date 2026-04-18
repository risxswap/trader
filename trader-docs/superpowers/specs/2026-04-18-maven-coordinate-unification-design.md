# Maven 坐标统一设计

## 1. 背景

当前仓库的 Maven 坐标存在两类不一致：

- 根 POM `groupId` 为 `cc.riskswap`，但子模块和内部依赖普遍使用 `cc.riskswap.trader`
- `trader-base` 目录对应模块的 `artifactId` 为 `base`，但其它模块依赖写的是 `trader-base`

这会带来两个直接问题：

- Reactor 联编时，模块坐标与依赖声明无法稳定对齐
- IDE 本地运行时容易回退到 `~/.m2` 中的旧快照，而不是工作区模块产物

本次设计目标是先统一 Maven 坐标体系，去掉内部模块 `artifactId` 的 `trader-` 前缀，同时保持目录名、Java 包名和运行入口不变，降低对现有代码与开发习惯的冲击。

## 2. 目标

- 统一根 POM 与所有内部模块的 Maven 坐标命名
- 内部模块 `artifactId` 去掉 `trader-` 前缀
- 保持模块目录名不变，如 `trader-base`、`trader-collector`
- 保持 Java 包名和主类不变
- 让 Reactor 构建和 IDE 本地运行优先使用工作区模块产物，而不是 `~/.m2` 的旧包

## 3. 范围

### 3.1 本次修改

- 根 POM `groupId`
- 顶层内部模块 `artifactId`
- 聚合模块 `artifactId`
- 子模块对内部依赖的坐标声明
- 与坐标强绑定的测试、文档和运行校验

### 3.2 不在本次范围

- 不改目录名
- 不改 Java 包名
- 不改 `mainClass`
- 不改打包 `finalName`
- 不做发布仓库、CI/CD、Docker 镜像标签体系调整

## 4. 统一后的坐标方案

### 4.1 根工程

- `groupId`: `cc.riskswap.trader`
- `artifactId`: `trader`
- `version`: `1.0.0-SNAPSHOT`

### 4.2 顶层模块

目录名保持不变，Maven `artifactId` 统一去掉 `trader-` 前缀：

- `trader-base` 目录 -> `cc.riskswap.trader:base`
- `trader-admin` 目录 -> `cc.riskswap.trader:admin`
- `trader-collector` 目录 -> `cc.riskswap.trader:collector`
- `trader-executor` 目录 -> `cc.riskswap.trader:executor`
- `trader-statistic` 目录 -> `cc.riskswap.trader:statistic`

### 4.3 二级模块

`trader-admin` 下的二级模块继续保持自然命名，不额外加 `trader-` 前缀：

- `cc.riskswap.trader:admin-server`
- `cc.riskswap.trader:admin-web`

## 5. 依赖与继承规则

### 5.1 父子继承

- 所有顶层模块的父 POM 都继承根工程 `cc.riskswap.trader:trader:1.0.0-SNAPSHOT`
- 所有二级模块继续继承其所属聚合模块

### 5.2 内部依赖

所有对基础模块的依赖统一改为：

```xml
<dependency>
  <groupId>cc.riskswap.trader</groupId>
  <artifactId>base</artifactId>
</dependency>
```

根 POM 的 `dependencyManagement` 也统一管理为：

```xml
<dependency>
  <groupId>cc.riskswap.trader</groupId>
  <artifactId>base</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## 6. 对本地运行的影响

本次设计不改 VS Code `launch.json` 的 `mainClass` 和 `cwd` 语义，但坐标统一后，IDE 重新导入 Maven 项目时应满足：

- 模块依赖能在 Reactor 内正确解析到工作区模块
- 启动 `collector` / `admin-server` 时，classpath 中不再依赖错误坐标的旧快照

如果本地仍存在旧的 `~/.m2` 快照，统一坐标后需要执行一次模块安装并重新导入 Maven 项目，以避免缓存干扰。

## 7. 风险与兼容性

### 7.1 风险

- 旧文档、测试、脚本中可能仍引用 `cc.riskswap.trader:trader-base`
- IDE 可能缓存旧模型，需要重新导入 Maven 工程
- 本地仓库中会同时存在旧坐标与新坐标快照，短期内可能造成排查混淆

### 7.2 控制策略

- 本次只改坐标，不改目录和包名，降低联动面
- 补一条坐标一致性的 smoke test，防止后续再次出现“目录名 / 依赖名 / 模块坐标”漂移
- 用 `dependency:tree` 和实际运行 classpath 双重验证依赖解析结果

## 8. 验证方案

完成后至少验证以下内容：

1. 根 POM `groupId` 为 `cc.riskswap.trader`
2. 顶层模块 `artifactId` 全部去掉 `trader-` 前缀
3. `collector` 的依赖树中基础模块解析为 `cc.riskswap.trader:base`
4. 坐标 smoke test 通过
5. 重新安装模块后，运行中的 `admin-server` / `collector` classpath 与统一后的模块坐标一致

## 9. 验收标准

- 根工程、聚合模块、顶层业务模块 Maven 坐标全部统一
- 内部依赖不再出现 `trader-base` 这类旧前缀写法
- 目录名和 Java 包名保持不变
- 本地运行可稳定使用工作区模块构建结果
- 有自动化检查覆盖核心坐标一致性
