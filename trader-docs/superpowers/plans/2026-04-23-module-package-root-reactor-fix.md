# Module Package Root Reactor Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix `trader-collector`、`trader-executor`、`trader-statistic` packaging scripts so they build from the repository root reactor and reliably include the `trader-base` dependency in packaged runtime artifacts.

**Architecture:** Keep each module’s current packaging layout unchanged, but change the Maven invocation source from the module-local reactor to the repository root reactor. Add focused structure tests for each package script, then verify by running each module’s real `package.sh`.

**Tech Stack:** Bash, Maven Wrapper, Spring Boot packaging, JUnit 5

---

## File Map

- Modify: `/Users/ming/Workspace/trader/trader-collector/package.sh`
- Modify: `/Users/ming/Workspace/trader/trader-executor/package.sh`
- Modify: `/Users/ming/Workspace/trader/trader-statistic/package.sh`
- Create: `/Users/ming/Workspace/trader/trader-collector/src/test/java/cc/riskswap/trader/collector/test/config/PackageScriptRootReactorTest.java`
- Create: `/Users/ming/Workspace/trader/trader-executor/src/test/java/cc/riskswap/trader/executor/PackageScriptRootReactorTest.java`
- Create: `/Users/ming/Workspace/trader/trader-statistic/src/test/java/cc/riskswap/trader/statistic/PackageScriptRootReactorTest.java`

### Task 1: Lock Collector Package Script to Root Reactor

**Files:**
- Create: `/Users/ming/Workspace/trader/trader-collector/src/test/java/cc/riskswap/trader/collector/test/config/PackageScriptRootReactorTest.java`
- Modify: `/Users/ming/Workspace/trader/trader-collector/package.sh`

- [ ] **Step 1: Write the failing test**

Create a structure test:

```java
String script = Files.readString(Path.of("package.sh"));

assertTrue(script.contains("-f \"$ROOT_DIR/../pom.xml\""));
assertTrue(script.contains("-pl trader-collector -am -DskipTests package"));
assertFalse(script.contains("\"$MVNW\" -DskipTests package"));
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
cd /Users/ming/Workspace/trader/trader-collector && ../mvnw -Dtest=PackageScriptRootReactorTest test
```

Expected: FAIL because the current script still builds from the module-local reactor.

- [ ] **Step 3: Write minimal implementation**

Update the packaging line in `/Users/ming/Workspace/trader/trader-collector/package.sh` to:

```bash
"$MVNW" -f "$ROOT_DIR/../pom.xml" -pl trader-collector -am -DskipTests package
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
cd /Users/ming/Workspace/trader/trader-collector && ../mvnw -Dtest=PackageScriptRootReactorTest test
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add trader-collector/package.sh \
        trader-collector/src/test/java/cc/riskswap/trader/collector/test/config/PackageScriptRootReactorTest.java
git commit -m "test: lock collector packaging to root reactor"
```

### Task 2: Lock Executor Package Script to Root Reactor

**Files:**
- Create: `/Users/ming/Workspace/trader/trader-executor/src/test/java/cc/riskswap/trader/executor/PackageScriptRootReactorTest.java`
- Modify: `/Users/ming/Workspace/trader/trader-executor/package.sh`

- [ ] **Step 1: Write the failing test**

Create a structure test:

```java
String script = Files.readString(Path.of("package.sh"));

assertTrue(script.contains("-f \"$ROOT_DIR/../pom.xml\""));
assertTrue(script.contains("-pl trader-executor -am -DskipTests package"));
assertFalse(script.contains("\"$MVNW\" -DskipTests package"));
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
cd /Users/ming/Workspace/trader/trader-executor && ../mvnw -Dtest=PackageScriptRootReactorTest test
```

Expected: FAIL

- [ ] **Step 3: Write minimal implementation**

Update the packaging line in `/Users/ming/Workspace/trader/trader-executor/package.sh` to:

```bash
"$MVNW" -f "$ROOT_DIR/../pom.xml" -pl trader-executor -am -DskipTests package
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
cd /Users/ming/Workspace/trader/trader-executor && ../mvnw -Dtest=PackageScriptRootReactorTest test
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add trader-executor/package.sh \
        trader-executor/src/test/java/cc/riskswap/trader/executor/PackageScriptRootReactorTest.java
git commit -m "test: lock executor packaging to root reactor"
```

### Task 3: Lock Statistic Package Script to Root Reactor

**Files:**
- Create: `/Users/ming/Workspace/trader/trader-statistic/src/test/java/cc/riskswap/trader/statistic/PackageScriptRootReactorTest.java`
- Modify: `/Users/ming/Workspace/trader/trader-statistic/package.sh`

- [ ] **Step 1: Write the failing test**

Create a structure test:

```java
String script = Files.readString(Path.of("package.sh"));

assertTrue(script.contains("-f \"$ROOT_DIR/../pom.xml\""));
assertTrue(script.contains("-pl trader-statistic -am -DskipTests package"));
assertFalse(script.contains("\"$MVNW\" -DskipTests package"));
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
cd /Users/ming/Workspace/trader/trader-statistic && ../mvnw -Dtest=PackageScriptRootReactorTest test
```

Expected: FAIL

- [ ] **Step 3: Write minimal implementation**

Update the packaging line in `/Users/ming/Workspace/trader/trader-statistic/package.sh` to:

```bash
"$MVNW" -f "$ROOT_DIR/../pom.xml" -pl trader-statistic -am -DskipTests package
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
cd /Users/ming/Workspace/trader/trader-statistic && ../mvnw -Dtest=PackageScriptRootReactorTest test
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add trader-statistic/package.sh \
        trader-statistic/src/test/java/cc/riskswap/trader/statistic/PackageScriptRootReactorTest.java
git commit -m "test: lock statistic packaging to root reactor"
```

### Task 4: Verify Real Package Flow Per Module

**Files:**
- Verify only: all files touched in Tasks 1-3

- [ ] **Step 1: Run focused script structure tests**

Run:

```bash
cd /Users/ming/Workspace/trader/trader-collector && ../mvnw -Dtest=PackageScriptStructureTest,PackageScriptRootReactorTest test
cd /Users/ming/Workspace/trader/trader-executor && ../mvnw -Dtest=PackageScriptRootReactorTest test
cd /Users/ming/Workspace/trader/trader-statistic && ../mvnw -Dtest=PackagingStructureTest,PackageScriptRootReactorTest test
```

Expected: PASS

- [ ] **Step 2: Run real collector packaging**

Run:

```bash
cd /Users/ming/Workspace/trader/trader-collector && ./package.sh
```

Expected: PASS and packaged collector artifact generated under `target/`.

- [ ] **Step 3: Run real executor packaging**

Run:

```bash
cd /Users/ming/Workspace/trader/trader-executor && ./package.sh
```

Expected: PASS and packaged executor artifact generated under `target/`.

- [ ] **Step 4: Run real statistic packaging**

Run:

```bash
cd /Users/ming/Workspace/trader/trader-statistic && ./package.sh
```

Expected: PASS and packaged statistic artifact generated under `target/`.

- [ ] **Step 5: Run diagnostics on new test files**

Check diagnostics for:

```text
/Users/ming/Workspace/trader/trader-collector/src/test/java/cc/riskswap/trader/collector/test/config/PackageScriptRootReactorTest.java
/Users/ming/Workspace/trader/trader-executor/src/test/java/cc/riskswap/trader/executor/PackageScriptRootReactorTest.java
/Users/ming/Workspace/trader/trader-statistic/src/test/java/cc/riskswap/trader/statistic/PackageScriptRootReactorTest.java
```

Expected: no new diagnostics.

- [ ] **Step 6: Commit**

```bash
git add trader-collector/package.sh trader-executor/package.sh trader-statistic/package.sh \
        trader-collector/src/test/java/cc/riskswap/trader/collector/test/config/PackageScriptRootReactorTest.java \
        trader-executor/src/test/java/cc/riskswap/trader/executor/PackageScriptRootReactorTest.java \
        trader-statistic/src/test/java/cc/riskswap/trader/statistic/PackageScriptRootReactorTest.java
git commit -m "fix: build module packages from root reactor"
```
