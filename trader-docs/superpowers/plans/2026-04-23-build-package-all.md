# Build Package-All Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `./build.sh package-all` so one command sequentially packages every supported module using the existing per-module packaging behavior.

**Architecture:** Keep `build.sh` as the single orchestration entrypoint. `package-all` iterates the existing `SUPPORTED_MODULES` list and reuses the same module packaging branch already used by `package <module>`, so we do not duplicate module-specific logic or change artifact layouts.

**Tech Stack:** Bash, Maven Wrapper, existing module `package.sh` scripts, JUnit 5

---

## File Map

- Modify: `/Users/ming/Workspace/trader/build.sh`
  - Add `package-all`, update command parsing, and print progress for each module.
- Modify: `/Users/ming/Workspace/trader/README.md`
  - Document the new root command.
- Create: `/Users/ming/Workspace/trader/trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/config/RootBuildScriptStructureTest.java`
  - Lock the new command’s help text and orchestration behavior.
- Verify only: existing module scripts under:
  - `/Users/ming/Workspace/trader/trader-admin/package.sh`
  - `/Users/ming/Workspace/trader/trader-collector/package.sh`
  - `/Users/ming/Workspace/trader/trader-executor/package.sh`
  - `/Users/ming/Workspace/trader/trader-statistic/package.sh`

### Task 1: Lock `build.sh package-all` Contract With a Failing Test

**Files:**
- Create: `/Users/ming/Workspace/trader/trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/config/RootBuildScriptStructureTest.java`
- Verify: `/Users/ming/Workspace/trader/build.sh`

- [ ] **Step 1: Write the failing test**

Create a structure test that reads the root build script and asserts:

```java
String script = Files.readString(Path.of("..", "..", "..", "..", "build.sh"));

Assertions.assertTrue(script.contains("./build.sh package-all"));
Assertions.assertTrue(script.contains("package|package-all|full-install"));
Assertions.assertTrue(script.contains("if [[ \"$command\" == \"package-all\" ]]"));
Assertions.assertTrue(script.contains("for module in \"${SUPPORTED_MODULES[@]}\""));
Assertions.assertTrue(script.contains("package_script=\"$module_dir/package.sh\""));
Assertions.assertTrue(script.contains("\"$ROOT_DIR/mvnw\" -pl \"$module\" -am clean package -DskipTests"));
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
./mvnw -pl trader-admin/admin-server -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=RootBuildScriptStructureTest test
```

Expected: FAIL because `build.sh` does not yet mention `package-all`.

- [ ] **Step 3: Write minimal implementation**

Update `/Users/ming/Workspace/trader/build.sh` to:

```bash
  ./build.sh package-all
```

Accept `package-all` in the command parser:

```bash
  package|package-all|full-install)
```

Add a shared helper:

```bash
package_one_module() {
  local module="$1"
  local module_dir="$ROOT_DIR/$module"
  local package_script="$module_dir/package.sh"

  echo "Packaging $module..."
  if [[ -f "$package_script" ]]; then
    chmod +x "$package_script"
    "$package_script"
  else
    require_mvnw
    "$ROOT_DIR/mvnw" -pl "$module" -am clean package -DskipTests
  fi
  echo "Packaged $module"
}
```

Then add:

```bash
if [[ "$command" == "package-all" ]]; then
  for module in "${SUPPORTED_MODULES[@]}"; do
    package_one_module "$module"
  done
  exit 0
fi
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
./mvnw -pl trader-admin/admin-server -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=RootBuildScriptStructureTest test
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add build.sh trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/config/RootBuildScriptStructureTest.java
git commit -m "feat: add package-all root build command"
```

### Task 2: Document the New Root Command

**Files:**
- Modify: `/Users/ming/Workspace/trader/README.md`
- Verify: `/Users/ming/Workspace/trader/build.sh`

- [ ] **Step 1: Write the failing documentation assertion**

Add one more test assertion in `RootBuildScriptStructureTest` or a separate simple assertion test that `README.md` includes:

```java
String readme = Files.readString(Path.of("..", "..", "..", "..", "README.md"));
Assertions.assertTrue(readme.contains("./build.sh package-all"));
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
./mvnw -pl trader-admin/admin-server -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=RootBuildScriptStructureTest test
```

Expected: FAIL because the README does not yet mention `package-all`.

- [ ] **Step 3: Write minimal implementation**

Update `/Users/ming/Workspace/trader/README.md` command list to include:

```text
./build.sh package-all
```

Keep the wording concise and aligned with the current root script usage section.

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
./mvnw -pl trader-admin/admin-server -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=RootBuildScriptStructureTest test
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add README.md trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/config/RootBuildScriptStructureTest.java
git commit -m "docs: add package-all build command"
```

### Task 3: Verify the Real Packaging Flow

**Files:**
- Verify only: `/Users/ming/Workspace/trader/build.sh`
- Verify only: existing module packaging scripts and generated artifacts

- [ ] **Step 1: Run focused test coverage**

Run:

```bash
./mvnw -pl trader-admin/admin-server -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=PackageScriptStructureTest,RootBuildScriptStructureTest test
```

Expected: PASS

- [ ] **Step 2: Run the real command**

Run:

```bash
./build.sh package-all
```

Expected:
- exits `0`
- prints each module packaging step in `SUPPORTED_MODULES` order
- produces existing module-specific artifacts without changing their current locations

- [ ] **Step 3: Check diagnostics on new test file**

Check diagnostics for:

```text
/Users/ming/Workspace/trader/trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/config/RootBuildScriptStructureTest.java
```

Expected: no new diagnostics.

- [ ] **Step 4: Commit final verification**

```bash
git add build.sh README.md trader-admin/admin-server/src/test/java/cc/riskswap/trader/admin/test/config/RootBuildScriptStructureTest.java
git commit -m "test: verify package-all build flow"
```
