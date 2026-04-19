# Strategy Config Move To Executor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将策略配置定义从 `trader-base` 迁移到 `trader-executor`，并清理旧包引用，确保模块职责边界更清晰且全量编译通过。

**Architecture:** 仅迁移 `BaseStrategyConfig` 与 `RelativeStrengthStrategyConfig` 两个配置类，不新增兼容层。`trader-executor` 的策略实现直接依赖新的 `cc.riskswap.trader.executor.strategy.config` 包，`trader-base` 删除旧的 `strategy/config` 目录。

**Tech Stack:** Java, Maven 多模块, Spring Boot, Lombok, Jakarta Validation, Jackson

---

## 影响文件清单

**Create:**

- `/Users/ming/Workspace/trader/trader-executor/src/main/java/cc/riskswap/trader/executor/strategy/config/BaseStrategyConfig.java`
- `/Users/ming/Workspace/trader/trader-executor/src/main/java/cc/riskswap/trader/executor/strategy/config/RelativeStrengthStrategyConfig.java`

**Modify:**

- `/Users/ming/Workspace/trader/trader-executor/src/main/java/cc/riskswap/trader/executor/strategy/BaseStrategy.java`
- `/Users/ming/Workspace/trader/trader-executor/src/main/java/cc/riskswap/trader/executor/strategy/RelativeStrengthStrategy.java`

**Delete:**

- `/Users/ming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/strategy/config/BaseStrategyConfig.java`
- `/Users/ming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/strategy/config/RelativeStrengthStrategyConfig.java`

---

### Task 1: 在 trader-executor 新建策略配置定义

**Files:**
- Create: `/Users/ming/Workspace/trader/trader-executor/src/main/java/cc/riskswap/trader/executor/strategy/config/BaseStrategyConfig.java`
- Create: `/Users/ming/Workspace/trader/trader-executor/src/main/java/cc/riskswap/trader/executor/strategy/config/RelativeStrengthStrategyConfig.java`

- [ ] **Step 1: 写一个聚焦检索，确认没有更多配置类需要一起迁移**

Run: `rg -n "package cc\\.riskswap\\.trader\\.base\\.strategy\\.config|extends BaseStrategyConfig" trader-base trader-executor`

Expected: 仅命中当前两个配置类和 executor 内的两个引用点

- [ ] **Step 2: 在 executor 下创建新的基础配置类**

```java
package cc.riskswap.trader.executor.strategy.config;

public class BaseStrategyConfig {
}
```

- [ ] **Step 3: 在 executor 下创建相对强弱策略配置类**

```java
package cc.riskswap.trader.executor.strategy.config;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RelativeStrengthStrategyConfig extends BaseStrategyConfig {

    @JsonPropertyDescription("调仓周期")
    @NotBlank
    private String rebalanceCycle;

    @JsonPropertyDescription("排名")
    private Integer topNum;
}
```

- [ ] **Step 4: 编译 executor 验证新类本身无语法问题**

Run: `./mvnw -pl trader-executor -am -DskipTests compile`

Expected: `BUILD SUCCESS`

---

### Task 2: 更新 executor 策略实现的 import

**Files:**
- Modify: `/Users/ming/Workspace/trader/trader-executor/src/main/java/cc/riskswap/trader/executor/strategy/BaseStrategy.java`
- Modify: `/Users/ming/Workspace/trader/trader-executor/src/main/java/cc/riskswap/trader/executor/strategy/RelativeStrengthStrategy.java`

- [ ] **Step 1: 修改 BaseStrategy 的配置类 import**

将：

```java
import cc.riskswap.trader.base.strategy.config.BaseStrategyConfig;
```

改为：

```java
import cc.riskswap.trader.executor.strategy.config.BaseStrategyConfig;
```

- [ ] **Step 2: 修改 RelativeStrengthStrategy 的配置类 import**

将：

```java
import cc.riskswap.trader.base.strategy.config.RelativeStrengthStrategyConfig;
```

改为：

```java
import cc.riskswap.trader.executor.strategy.config.RelativeStrengthStrategyConfig;
```

- [ ] **Step 3: 全仓搜索旧包引用**

Run: `rg -n "cc\\.riskswap\\.trader\\.base\\.strategy\\.config" /Users/ming/Workspace/trader`

Expected: 只剩待删除的旧源文件，或没有任何 Java 引用

- [ ] **Step 4: 编译 executor，确认策略实现已切换成功**

Run: `./mvnw -pl trader-executor -am -DskipTests compile`

Expected: `BUILD SUCCESS`

---

### Task 3: 删除 trader-base 中旧配置定义

**Files:**
- Delete: `/Users/ming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/strategy/config/BaseStrategyConfig.java`
- Delete: `/Users/ming/Workspace/trader/trader-base/src/main/java/cc/riskswap/trader/base/strategy/config/RelativeStrengthStrategyConfig.java`

- [ ] **Step 1: 删除旧的两个配置类**

删除 `trader-base` 下原有配置类，不保留桥接类或兼容副本

- [ ] **Step 2: 若目录为空则一并清理 strategy/config 目录**

目标：`trader-base` 不再承载策略配置定义

- [ ] **Step 3: 搜索确认旧包已不存在**

Run: `rg -n "package cc\\.riskswap\\.trader\\.base\\.strategy\\.config|cc\\.riskswap\\.trader\\.base\\.strategy\\.config" /Users/ming/Workspace/trader`

Expected: 不再命中 Java 源码中的旧包定义/引用

---

### Task 4: 全量验证

**Files:**
- None

- [ ] **Step 1: 获取最近改动文件的诊断**

使用 IDE diagnostics 检查刚修改的 4 个 Java 文件，确认没有新增错误

- [ ] **Step 2: 根目录全量编译**

Run: `./mvnw -DskipTests compile`

Expected: `BUILD SUCCESS`

- [ ] **Step 3: 给出迁移结果说明**

说明：
- 新配置类所在模块和包路径
- 删除的旧文件
- 编译验证结果

