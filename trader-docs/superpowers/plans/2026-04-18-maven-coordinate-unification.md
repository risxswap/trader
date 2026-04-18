# Maven Coordinate Unification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Unify internal Maven coordinates under `cc.riskswap.trader`, remove the `trader-` prefix from internal module artifact IDs, and ensure local runs resolve workspace modules instead of stale `~/.m2` snapshots.

**Architecture:** Keep directory names, Java packages, and runtime entrypoints unchanged. Limit changes to Maven coordinates, internal dependency declarations, launch/test/documentation references, and verification steps so the repository builds as a consistent multi-module reactor.

**Tech Stack:** Maven, Spring Boot, VS Code Java launch configs, Python smoke tests

---

### Task 1: Lock Coordinate Expectations With Smoke Coverage

**Files:**
- Modify: `tests/maven-module-coordinates-smoke.py`

- [ ] **Step 1: Write the failing smoke assertions**

```python
assert text(root_pom, "m:groupId") == "cc.riskswap.trader"
assert text(base_pom, "m:artifactId") == "base"
assert managed_dependency is not None
```

- [ ] **Step 2: Run the smoke check to verify it fails**

Run: `python3 tests/maven-module-coordinates-smoke.py`
Expected: `AssertionError` because current root `groupId` and/or module `artifactId` do not match the desired coordinates.

- [ ] **Step 3: Expand the smoke check for final target shape**

```python
expected_modules = {
    "trader-base/pom.xml": "base",
    "trader-admin/pom.xml": "admin",
    "trader-collector/pom.xml": "collector",
    "trader-executor/pom.xml": "executor",
    "trader-statistic/pom.xml": "statistic",
}
```

- [ ] **Step 4: Re-run the smoke check and keep it failing until POMs are updated**

Run: `python3 tests/maven-module-coordinates-smoke.py`
Expected: still fails, but now points at the full set of coordinates that need to be changed.

- [ ] **Step 5: Commit**

```bash
git add tests/maven-module-coordinates-smoke.py
git commit -m "test: lock internal maven coordinates"
```

### Task 2: Unify Root And Top-Level Module Coordinates

**Files:**
- Modify: `pom.xml`
- Modify: `trader-base/pom.xml`
- Modify: `trader-admin/pom.xml`
- Modify: `trader-collector/pom.xml`
- Modify: `trader-executor/pom.xml`
- Modify: `trader-statistic/pom.xml`

- [ ] **Step 1: Update root group coordinates**

```xml
<groupId>cc.riskswap.trader</groupId>
<artifactId>trader</artifactId>
```

- [ ] **Step 2: Update top-level module artifact IDs**

```xml
<!-- trader-base/pom.xml -->
<artifactId>base</artifactId>

<!-- trader-admin/pom.xml -->
<artifactId>admin</artifactId>

<!-- trader-collector/pom.xml -->
<artifactId>collector</artifactId>

<!-- trader-executor/pom.xml -->
<artifactId>executor</artifactId>

<!-- trader-statistic/pom.xml -->
<artifactId>statistic</artifactId>
```

- [ ] **Step 3: Keep parent references aligned with the root POM**

```xml
<parent>
  <groupId>cc.riskswap.trader</groupId>
  <artifactId>trader</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <relativePath>../pom.xml</relativePath>
</parent>
```

- [ ] **Step 4: Run the smoke test to verify top-level coordinates now satisfy the assertions**

Run: `python3 tests/maven-module-coordinates-smoke.py`
Expected: PASS for the updated coordinate checks, or fail only on downstream dependency references not yet updated.

- [ ] **Step 5: Commit**

```bash
git add pom.xml trader-base/pom.xml trader-admin/pom.xml trader-collector/pom.xml trader-executor/pom.xml trader-statistic/pom.xml tests/maven-module-coordinates-smoke.py
git commit -m "refactor: unify top-level maven coordinates"
```

### Task 3: Rewrite Internal Dependency Coordinates

**Files:**
- Modify: `pom.xml`
- Modify: `trader-admin/admin-server/pom.xml`
- Modify: `trader-collector/pom.xml`
- Modify: `trader-executor/pom.xml`
- Modify: `trader-statistic/pom.xml`

- [ ] **Step 1: Update dependencyManagement to the new base module coordinates**

```xml
<dependency>
  <groupId>cc.riskswap.trader</groupId>
  <artifactId>base</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

- [ ] **Step 2: Replace all internal `trader-base` dependency declarations**

```xml
<dependency>
  <groupId>cc.riskswap.trader</groupId>
  <artifactId>base</artifactId>
</dependency>
```

- [ ] **Step 3: Verify there are no stale internal dependency references**

Run: `rg "artifactId>trader-base|cc\\.riskswap\\.trader:trader-base" pom.xml trader-*`
Expected: no matches in active POM files.

- [ ] **Step 4: Verify reactor dependency resolution**

Run: `./mvnw -pl trader-collector -am dependency:tree -Dincludes=cc.riskswap.trader -Dverbose`
Expected: `collector` depends on `cc.riskswap.trader:base:1.0.0-SNAPSHOT`.

- [ ] **Step 5: Commit**

```bash
git add pom.xml trader-admin/admin-server/pom.xml trader-collector/pom.xml trader-executor/pom.xml trader-statistic/pom.xml
git commit -m "refactor: align internal module dependencies"
```

### Task 4: Align Tooling And Docs With The New Coordinates

**Files:**
- Modify: `.vscode/launch.json`
- Modify: `tests/vscode-launch-config-smoke.py`
- Modify: `README.md`
- Modify: `trader-docs/dev/03-Backend-Core.md`
- Modify: `trader-docs/architecture/02-Dependency-Graph.md`
- Modify: `trader-docs/superpowers/specs/2026-04-18-maven-coordinate-unification-design.md`

- [ ] **Step 1: Update launch config metadata if it still points at old Maven project names**

```json
{
  "name": "Collector",
  "projectName": "collector"
}
```

- [ ] **Step 2: Update the VS Code smoke test to assert the new project name**

```python
assert collector.get("projectName") == "collector"
```

- [ ] **Step 3: Rewrite docs that describe internal coordinates**

```text
cc.riskswap.trader:base
cc.riskswap.trader:collector
cc.riskswap.trader:statistic
```

- [ ] **Step 4: Re-run the docs/config smoke checks**

Run: `python3 tests/vscode-launch-config-smoke.py`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add .vscode/launch.json tests/vscode-launch-config-smoke.py README.md trader-docs/dev/03-Backend-Core.md trader-docs/architecture/02-Dependency-Graph.md trader-docs/superpowers/specs/2026-04-18-maven-coordinate-unification-design.md
git commit -m "docs: update tooling and coordinate references"
```

### Task 5: Rebuild Local Module Artifacts And Verify Runtime Resolution

**Files:**
- Modify: none
- Verify: `~/.m2/repository/cc/riskswap/trader/base/1.0.0-SNAPSHOT/`

- [ ] **Step 1: Install the unified base module into the local repository**

Run: `./mvnw -pl trader-base install`
Expected: BUILD SUCCESS and installation of `cc.riskswap.trader:base:1.0.0-SNAPSHOT`.

- [ ] **Step 2: Build a downstream module against the reactor**

Run: `./mvnw -pl trader-collector -am package -DskipTests`
Expected: BUILD SUCCESS with downstream resolution against the unified coordinates.

- [ ] **Step 3: Verify the installed artifact path now matches the new coordinates**

Run: `ls ~/.m2/repository/cc/riskswap/trader/base/1.0.0-SNAPSHOT`
Expected: jar and pom files for `base-1.0.0-SNAPSHOT`.

- [ ] **Step 4: Re-run runtime classpath inspection after restarting services**

Run: `ps -p <admin_pid>,<collector_pid> -o pid=,command=`
Expected: classpath resolves internal modules using the unified coordinates; no dependency on stale `trader-base` coordinates.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "build: verify unified maven coordinates"
```

### Task 6: End-to-End Regression Verification

**Files:**
- Modify: none
- Verify: `trader-base/src/main/java/cc/riskswap/trader/base/autoconfigure/TraderTaskAutoConfiguration.java`

- [ ] **Step 1: Run the coordinate smoke test**

Run: `python3 tests/maven-module-coordinates-smoke.py`
Expected: PASS

- [ ] **Step 2: Run the launch-config smoke test**

Run: `python3 tests/vscode-launch-config-smoke.py`
Expected: PASS

- [ ] **Step 3: Run focused `trader-base` tests for task dispatch**

Run: `./mvnw -pl trader-base -Dtest=TraderTaskAutoConfigurationTest,TraderQuartzJobTest,TraderTaskSchedulerServiceTest,TraderTaskExecutorTest,TraderTaskRefreshSubscriberTest test`
Expected: BUILD SUCCESS, 0 failures.

- [ ] **Step 4: Restart local services and replay the manual trigger flow**

Run:

```bash
./mvnw -pl trader-base install
# restart admin-server and collector in the IDE
```

Then verify:

```bash
python3 -c "import json,time,urllib.request; base='http://127.0.0.1:8080'; headers={'Content-Type':'application/json'}; post=lambda path,body: (lambda req=urllib.request.Request(base+path,data=json.dumps(body).encode(),headers=headers,method='POST'): (lambda resp=urllib.request.urlopen(req,timeout=10): json.loads(resp.read().decode()))())(); item=post('/task/list',{'pageNo':1,'pageSize':20,'taskType':'COLLECTOR'})['data']['items'][0]; print(post('/task/trigger',{'id':item['id']})); time.sleep(3); print(post('/task/list',{'pageNo':1,'pageSize':20,'taskType':'COLLECTOR'})['data']['items'][0])"
```

Expected: trigger succeeds and the task status/version reflects runtime consumption instead of staying permanently unchanged.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "test: verify coordinate unification and local task dispatch"
```
