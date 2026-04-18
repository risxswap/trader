# Root Build Script Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在仓库根目录新增一个统一构建脚本，支持按顶层模块单独打包，并提供一个跳过测试的全量 `install` 命令。

**Architecture:** 采用“根脚本分发 + 模块脚本复用”的方案。`build.sh` 负责统一解析命令、校验模块、定位根目录并决定执行路径；对已有 `package.sh` 的模块直接转发，对没有 `package.sh` 的模块回退到根目录 `./mvnw -pl <module> -am clean package -DskipTests`。README 只补充最基础的脚本用法，避免和运维文档重复。

**Tech Stack:** Bash、Maven Wrapper、项目现有模块级 `package.sh`

---

## File Map

### 预计新增

- `build.sh`

### 预计修改

- `README.md`

### 参考文件

- `trader-admin/package.sh`
- `trader-collector/package.sh`
- `trader-executor/package.sh`
- `trader-statistic/package.sh`
- `trader-docs/superpowers/specs/2026-04-18-root-build-script-design.md`

---

### Task 1: 新增根目录 `build.sh` 并完成命令解析

**Files:**
- Create: `build.sh`

- [ ] **Step 1: 先写最小 CLI 骨架，覆盖 `help`、缺参和非法命令**

Create `build.sh`:

```bash
#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

SUPPORTED_MODULES=(
  "trader-admin"
  "trader-base"
  "trader-collector"
  "trader-executor"
  "trader-statistic"
)

print_help() {
  cat <<'EOF'
Usage:
  ./build.sh package <module>
  ./build.sh full-install
  ./build.sh help

Supported modules:
  trader-admin
  trader-base
  trader-collector
  trader-executor
  trader-statistic
EOF
}

command="${1:-}"

case "$command" in
  help|"")
    print_help
    [[ -n "$command" ]] || exit 1
    exit 0
    ;;
  package|full-install)
    ;;
  *)
    echo "Unsupported command: $command" >&2
    print_help
    exit 1
    ;;
esac
```

- [ ] **Step 2: 运行语法检查**

Run:

```bash
cd /Users/ming/Workspace/trader && bash -n build.sh
```

Expected: 无输出，退出码 `0`

- [ ] **Step 3: 运行帮助命令确认输出**

Run:

```bash
cd /Users/ming/Workspace/trader && ./build.sh help
```

Expected: 输出 `Usage:`、`Supported modules:` 和 5 个顶层模块名

- [ ] **Step 4: 运行非法命令确认失败**

Run:

```bash
cd /Users/ming/Workspace/trader && ./build.sh foo
```

Expected: 退出码非 `0`，输出 `Unsupported command`

- [ ] **Step 5: Commit**

```bash
git add build.sh
git commit -m "feat: add root build script skeleton"
```

---

### Task 2: 为 `package <module>` 增加模块校验、脚本分发与 Maven 回退

**Files:**
- Modify: `build.sh`

- [ ] **Step 1: 在脚本中增加模块存在校验函数**

Update `build.sh`，增加：

```bash
is_supported_module() {
  local target="$1"
  for module in "${SUPPORTED_MODULES[@]}"; do
    if [[ "$module" == "$target" ]]; then
      return 0
    fi
  done
  return 1
}

require_mvnw() {
  if [[ ! -f "$ROOT_DIR/mvnw" ]]; then
    echo "Cannot find Maven Wrapper: $ROOT_DIR/mvnw" >&2
    exit 1
  fi
  chmod +x "$ROOT_DIR/mvnw"
}
```

- [ ] **Step 2: 实现 `package <module>` 主流程**

Update `build.sh`，在 `case "$command"` 后增加：

```bash
if [[ "$command" == "package" ]]; then
  module="${2:-}"
  if [[ -z "$module" ]]; then
    echo "Missing module name for package command" >&2
    print_help
    exit 1
  fi

  if ! is_supported_module "$module"; then
    echo "Unsupported module: $module" >&2
    print_help
    exit 1
  fi

  module_dir="$ROOT_DIR/$module"
  if [[ ! -d "$module_dir" ]]; then
    echo "Module directory not found: $module_dir" >&2
    exit 1
  fi

  package_script="$module_dir/package.sh"
  if [[ -f "$package_script" ]]; then
    chmod +x "$package_script"
    exec "$package_script"
  fi

  require_mvnw
  exec "$ROOT_DIR/mvnw" -pl "$module" -am clean package -DskipTests
fi
```

- [ ] **Step 3: 先验证缺少模块名时正确失败**

Run:

```bash
cd /Users/ming/Workspace/trader && ./build.sh package
```

Expected: 退出码非 `0`，输出 `Missing module name for package command`

- [ ] **Step 4: 验证非法模块名时正确失败**

Run:

```bash
cd /Users/ming/Workspace/trader && ./build.sh package foo
```

Expected: 退出码非 `0`，输出 `Unsupported module: foo`

- [ ] **Step 5: 验证 `trader-base` 走 Maven 回退路径**

Run:

```bash
cd /Users/ming/Workspace/trader && ./build.sh package trader-base
```

Expected: 触发根目录 `./mvnw -pl trader-base -am clean package -DskipTests`，退出码 `0`

- [ ] **Step 6: 验证 `trader-collector` 走模块级脚本**

Run:

```bash
cd /Users/ming/Workspace/trader && ./build.sh package trader-collector
```

Expected: 调用 `trader-collector/package.sh`，最终输出 `Packaged:` 并退出 `0`

- [ ] **Step 7: Commit**

```bash
git add build.sh
git commit -m "feat: support per-module packaging in root script"
```

---

### Task 3: 增加 `full-install` 全量跳过测试命令

**Files:**
- Modify: `build.sh`

- [ ] **Step 1: 在脚本中实现 `full-install`**

Update `build.sh`：

```bash
if [[ "$command" == "full-install" ]]; then
  require_mvnw
  exec "$ROOT_DIR/mvnw" clean install -DskipTests
fi
```

- [ ] **Step 2: 运行 `full-install` 进行真实验证**

Run:

```bash
cd /Users/ming/Workspace/trader && ./build.sh full-install
```

Expected: 执行根目录 `./mvnw clean install -DskipTests`，退出码 `0`

- [ ] **Step 3: 再次检查帮助信息与语法**

Run:

```bash
cd /Users/ming/Workspace/trader && bash -n build.sh && ./build.sh help
```

Expected: 语法检查通过，帮助信息包含 `full-install`

- [ ] **Step 4: Commit**

```bash
git add build.sh
git commit -m "feat: add full install command to root build script"
```

---

### Task 4: 更新 README 的统一脚本用法

**Files:**
- Modify: `README.md`

- [ ] **Step 1: 在 README 快速开始后补充脚本说明**

Update `README.md`，在现有 Maven 构建示例后追加：

```md
## 统一构建脚本

仓库根目录提供统一构建入口：

```bash
# 打包单个顶层模块
./build.sh package trader-admin
./build.sh package trader-base
./build.sh package trader-collector
./build.sh package trader-executor
./build.sh package trader-statistic

# 全量 install，跳过测试
./build.sh full-install

# 查看帮助
./build.sh help
```
```

- [ ] **Step 2: 检查 README 格式**

Run:

```bash
cd /Users/ming/Workspace/trader && python3 - <<'PY'
from pathlib import Path
text = Path("README.md").read_text()
assert "## 统一构建脚本" in text
assert "./build.sh package trader-admin" in text
assert "./build.sh full-install" in text
PY
```

Expected: 无输出，退出码 `0`

- [ ] **Step 3: Commit**

```bash
git add README.md
git commit -m "docs: document root build script usage"
```

---

### Task 5: 最终验证与收尾

**Files:**
- Verify: `build.sh`
- Verify: `README.md`

- [ ] **Step 1: 运行最终帮助与错误分支验证**

Run:

```bash
cd /Users/ming/Workspace/trader && \
bash -n build.sh && \
./build.sh help && \
(! ./build.sh package foo)
```

Expected: 帮助输出正常，非法模块返回失败

- [ ] **Step 2: 运行一次模块打包和一次全量安装**

Run:

```bash
cd /Users/ming/Workspace/trader && ./build.sh package trader-base
cd /Users/ming/Workspace/trader && ./build.sh full-install
```

Expected: 两条命令都退出 `0`

- [ ] **Step 3: 查看最终变更**

Run:

```bash
cd /Users/ming/Workspace/trader && git diff -- build.sh README.md
```

Expected: 只包含统一脚本和 README 用法说明

- [ ] **Step 4: Commit**

```bash
git add build.sh README.md
git commit -m "feat: add unified root build entrypoint"
```
