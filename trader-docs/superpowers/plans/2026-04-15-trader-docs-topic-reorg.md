# trader-docs Topic Reorg Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reorganize `trader-docs/` into topic-based directories (architecture/product/ops/dev/etc.), move files physically, and fix all links including absolute local file path references.

**Architecture:** Introduce stable top-level topic folders under `trader-docs/`. Move existing module-mirrored docs into topic locations, consolidate superpowers docs under `trader-docs/superpowers/`, and remove old module-mirror folders after link fixes.

**Tech Stack:** Markdown, ripgrep

---

## Task 1: Create topic directories

**Files:**
- Create dirs:
  - `/Users/ming/Workspace/trader/trader-docs/architecture`
  - `/Users/ming/Workspace/trader/trader-docs/product`
  - `/Users/ming/Workspace/trader/trader-docs/ops`
  - `/Users/ming/Workspace/trader/trader-docs/dev`
  - `/Users/ming/Workspace/trader/trader-docs/superpowers/plans`
  - `/Users/ming/Workspace/trader/trader-docs/superpowers/specs`
  - `/Users/ming/Workspace/trader/trader-docs/modules`
  - `/Users/ming/Workspace/trader/trader-docs/examples`

- [ ] **Step 1: Create directories**

Run:

```bash
mkdir -p trader-docs/{architecture,product,ops,dev,modules,examples}
mkdir -p trader-docs/superpowers/{plans,specs}
```

Expected: exit code 0

---

## Task 2: Move top-level docs into topics

**Files:**
- Move:
  - `/Users/ming/Workspace/trader/trader-docs/design_implementation.md` → `/Users/ming/Workspace/trader/trader-docs/architecture/design_implementation.md`
  - `/Users/ming/Workspace/trader/trader-docs/requirements.md` → `/Users/ming/Workspace/trader/trader-docs/product/requirements.md`

- [ ] **Step 1: Move files**

Run:

```bash
mv trader-docs/design_implementation.md trader-docs/architecture/design_implementation.md
mv trader-docs/requirements.md trader-docs/product/requirements.md
```

Expected: exit code 0

---

## Task 3: Move code-wiki and product docs into topics

**Files:**
- Move from:
  - `/Users/ming/Workspace/trader/trader-docs/trader-admin/docs/code-wiki/*.md`
  - `/Users/ming/Workspace/trader/trader-docs/trader-admin/docs/*.md` (excluding `Code-Wiki.md`)

- [ ] **Step 1: Move architecture wiki**

Run:

```bash
mv trader-docs/trader-admin/docs/code-wiki/00-Architecture.md trader-docs/architecture/00-Architecture.md
mv trader-docs/trader-admin/docs/code-wiki/01-Modules.md trader-docs/architecture/01-Modules.md
mv trader-docs/trader-admin/docs/code-wiki/02-Dependency-Graph.md trader-docs/architecture/02-Dependency-Graph.md
```

- [ ] **Step 2: Move ops wiki**

Run:

```bash
mv trader-docs/trader-admin/docs/code-wiki/08-Runbook.md trader-docs/ops/08-Runbook.md
```

- [ ] **Step 3: Move dev wiki**

Run:

```bash
mv trader-docs/trader-admin/docs/code-wiki/03-Backend-Core.md trader-docs/dev/03-Backend-Core.md
mv trader-docs/trader-admin/docs/code-wiki/04-Admin-Server.md trader-docs/dev/04-Admin-Server.md
mv trader-docs/trader-admin/docs/code-wiki/05-Collector.md trader-docs/dev/05-Collector.md
mv trader-docs/trader-admin/docs/code-wiki/06-Executor.md trader-docs/dev/06-Executor.md
mv trader-docs/trader-admin/docs/code-wiki/07-Admin-Web.md trader-docs/dev/07-Admin-Web.md
```

- [ ] **Step 4: Move product docs**

Run:

```bash
mv trader-docs/trader-admin/docs/00需求分析.md trader-docs/product/00需求分析.md
mv trader-docs/trader-admin/docs/00功能模块.md trader-docs/product/00功能模块.md
mv trader-docs/trader-admin/docs/01-鉴权与用户.md trader-docs/product/01-鉴权与用户.md
mv trader-docs/trader-admin/docs/02-仪表盘.md trader-docs/product/02-仪表盘.md
mv trader-docs/trader-admin/docs/03-投资.md trader-docs/product/03-投资.md
mv trader-docs/trader-admin/docs/04-交易所与交易日历.md trader-docs/product/04-交易所与交易日历.md
mv trader-docs/trader-admin/docs/05-公募基金.md trader-docs/product/05-公募基金.md
mv trader-docs/trader-admin/docs/06-ETF.md trader-docs/product/06-ETF.md
mv trader-docs/trader-admin/docs/07-相关性统计.md trader-docs/product/07-相关性统计.md
mv trader-docs/trader-admin/docs/08-消息推送.md trader-docs/product/08-消息推送.md
mv trader-docs/trader-admin/docs/09-基础数据.md trader-docs/product/09-基础数据.md
mv trader-docs/trader-admin/docs/10-系统设置.md trader-docs/product/10-系统设置.md
mv trader-docs/trader-admin/docs/11-节点管理.md trader-docs/product/11-节点管理.md
```

---

## Task 4: Consolidate superpowers docs

**Files:**
- Move:
  - `/Users/ming/Workspace/trader/trader-docs/trader-admin/docs/superpowers/**` → `/Users/ming/Workspace/trader/trader-docs/superpowers/**`
  - `/Users/ming/Workspace/trader/trader-docs/trader-base/docs/superpowers/**` → `/Users/ming/Workspace/trader/trader-docs/superpowers/**`
  - `/Users/ming/Workspace/trader/trader-docs/plans/**` → `/Users/ming/Workspace/trader/trader-docs/superpowers/plans/**`
  - Design specs at `trader-docs/*.md` with `*-design.md` suffix → `/Users/ming/Workspace/trader/trader-docs/superpowers/specs/`

- [ ] **Step 1: Move superpowers plans/specs**

Run:

```bash
mv trader-docs/trader-admin/docs/superpowers/plans/* trader-docs/superpowers/plans/
mv trader-docs/trader-admin/docs/superpowers/specs/* trader-docs/superpowers/specs/
mv trader-docs/trader-base/docs/superpowers/plans/* trader-docs/superpowers/plans/
mv trader-docs/trader-base/docs/superpowers/specs/* trader-docs/superpowers/specs/
```

- [ ] **Step 2: Move local plan docs**

Run:

```bash
if [ -d trader-docs/plans ]; then mv trader-docs/plans/* trader-docs/superpowers/plans/; fi
```

- [ ] **Step 3: Move local design docs**

Run:

```bash
mv trader-docs/*-design.md trader-docs/superpowers/specs/
mv trader-docs/2026-04-15-trader-docs-topic-reorg-design.md trader-docs/superpowers/specs/
```

Expected: exit code 0

---

## Task 5: Move examples (collector docs)

**Files:**
- Move:
  - `/Users/ming/Workspace/trader/trader-docs/trader-collector/docs/**` → `/Users/ming/Workspace/trader/trader-docs/examples/collector/**`

- [ ] **Step 1: Move docs directory**

Run:

```bash
mkdir -p trader-docs/examples
mv trader-docs/trader-collector/docs trader-docs/examples/collector
```

Expected: exit code 0

---

## Task 6: Move Obsidian config (optional)

**Files:**
- Move:
  - `/Users/ming/Workspace/trader/trader-docs/trader-admin/docs/.obsidian/**` → `/Users/ming/Workspace/trader/trader-docs/.obsidian/**`

- [ ] **Step 1: Move .obsidian if present**

Run:

```bash
if [ -d trader-docs/trader-admin/docs/.obsidian ]; then mv trader-docs/trader-admin/docs/.obsidian trader-docs/.obsidian; fi
```

Expected: exit code 0

---

## Task 7: Create module entry pages and update trader-docs/README.md

**Files:**
- Create:
  - `/Users/ming/Workspace/trader/trader-docs/modules/trader-admin.md`
  - `/Users/ming/Workspace/trader/trader-docs/modules/trader-base.md`
  - `/Users/ming/Workspace/trader/trader-docs/modules/trader-collector.md`
  - `/Users/ming/Workspace/trader/trader-docs/modules/trader-executor.md`
  - `/Users/ming/Workspace/trader/trader-docs/modules/trader-statistic.md`
- Modify:
  - `/Users/ming/Workspace/trader/trader-docs/README.md`

- [ ] **Step 1: Add module pages**
- [ ] **Step 2: Update README navigation to topics**

---

## Task 8: Fix all links

**Files:**
- Modify: affected markdown under `/Users/ming/Workspace/trader/trader-docs/**`

- [ ] **Step 1: Remove old module-mirror path references**

Run:

```bash
rg -n "trader-docs/trader-admin/" trader-docs || true
rg -n "trader-docs/trader-base/" trader-docs || true
rg -n "trader-docs/trader-collector/" trader-docs || true
```

Expected: empty output

- [ ] **Step 2: Replace absolute file links**

Run:

```bash
rg -n "../../../" trader-docs || true
```

Expected: empty output

---

## Task 9: Delete old module-mirror folders

**Files:**
- Delete:
  - `/Users/ming/Workspace/trader/trader-docs/trader-admin/`
  - `/Users/ming/Workspace/trader/trader-docs/trader-base/`
  - `/Users/ming/Workspace/trader/trader-docs/trader-collector/`

- [ ] **Step 1: Remove empty directories**

Run:

```bash
rm -rf trader-docs/trader-admin trader-docs/trader-base trader-docs/trader-collector
```

Expected: exit code 0

---

## Task 10: Verification

- [ ] **Step 1: Validate key topic docs exist**

Run:

```bash
test -f trader-docs/architecture/00-Architecture.md
test -f trader-docs/ops/08-Runbook.md
test -f trader-docs/dev/06-Executor.md
test -f trader-docs/product/00需求分析.md
```

- [ ] **Step 2: Confirm no absolute file links remain**

Run:

```bash
rg -n "/U[s]ers/" trader-docs || true
```

Expected: empty output
