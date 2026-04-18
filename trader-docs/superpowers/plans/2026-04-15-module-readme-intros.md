# Module README Intros Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add short, consistent introductions to each module’s root `README.md`, and create missing module READMEs.

**Architecture:** Keep module root READMEs as short entrypoints (intro + bullets + doc links), pointing to `trader-docs` for detailed documentation.

**Tech Stack:** Markdown

---

### Task 1: Update existing module READMEs (admin/base/collector)

**Files:**
- Modify: `/Users/ming/Workspace/trader/trader-admin/README.md`
- Modify: `/Users/ming/Workspace/trader/trader-base/README.md`
- Modify: `/Users/ming/Workspace/trader/trader-collector/README.md`

- [ ] **Step 1: Update trader-admin/README.md**
- [ ] **Step 2: Update trader-base/README.md**
- [ ] **Step 3: Update trader-collector/README.md**
- [ ] **Step 4: Verify doc links are relative and valid**

Run:

```bash
test -f trader-docs/README.md
test -f trader-docs/trader-admin/README.md
test -f trader-docs/trader-base/README.md
test -f trader-docs/trader-collector/README.md
```

Expected: exit code 0 for all

---

### Task 2: Add missing module READMEs (executor/statistic)

**Files:**
- Create: `/Users/ming/Workspace/trader/trader-executor/README.md`
- Create: `/Users/ming/Workspace/trader/trader-statistic/README.md`

- [ ] **Step 1: Create trader-executor/README.md**
- [ ] **Step 2: Create trader-statistic/README.md**
- [ ] **Step 3: Verify both files exist**

Run:

```bash
test -f trader-executor/README.md
test -f trader-statistic/README.md
```

Expected: exit code 0 for both

