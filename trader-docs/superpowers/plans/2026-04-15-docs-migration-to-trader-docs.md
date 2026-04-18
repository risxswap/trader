# Docs Migration to trader-docs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move all documentation (including sample data under module docs) into `trader-docs/`, update references, and remove old `docs/` directories.

**Architecture:** Use “module mirror” relocation to preserve relative link structure: each module’s `docs/**` moves to `trader-docs/<module>/docs/**`, and each module root `README.md` becomes a pointer to the new doc entry.

**Tech Stack:** git, ripgrep, Markdown

---

## File Map

**Create:**
- `trader-docs/README.md`
- `trader-docs/trader-admin/README.md`
- `trader-docs/trader-base/README.md`
- `trader-docs/trader-collector/README.md`

**Move (git mv):**
- `trader-admin/docs/**` → `trader-docs/trader-admin/docs/**`
- `trader-base/docs/**` → `trader-docs/trader-base/docs/**`
- `trader-collector/docs/**` → `trader-docs/trader-collector/docs/**`
- `trader-admin/README.md` → `trader-docs/trader-admin/README.md`
- `trader-base/README.md` → `trader-docs/trader-base/README.md`
- `trader-collector/README.md` → `trader-docs/trader-collector/README.md`

**Modify:**
- `trader-admin/README.md` (becomes pointer)
- `trader-base/README.md` (becomes pointer)
- `trader-collector/README.md` (becomes pointer)
- Any files containing old doc links (found by grep)

**Do not move:**
- `trader-admin/.trae/rules/project_rules.md`
- `trader-executor/rules/project_rules.md`
- Any `rules/**` or `.trae/rules/**`

---

### Task 1: Create trader-docs entrypoint

**Files:**
- Create: `trader-docs/README.md`

- [ ] **Step 1: Create entry README**

Content (edit to match repo reality after moves):

```md
# trader-docs

## Modules

- [trader-admin](./trader-admin/README.md)
- [trader-base](./trader-base/README.md)
- [trader-collector](./trader-collector/README.md)

## Project docs

- [requirements](./requirements.md)
- [design & implementation](./design_implementation.md)
```

- [ ] **Step 2: Verify the file exists**

Run: `ls trader-docs/README.md`
Expected: file path prints, exit code 0

- [ ] **Step 3: Commit**

```bash
git add trader-docs/README.md
git commit -m "docs: add trader-docs entrypoint"
```

---

### Task 2: Move trader-admin docs and README

**Files:**
- Move: `trader-admin/docs/**` → `trader-docs/trader-admin/docs/**`
- Move: `trader-admin/README.md` → `trader-docs/trader-admin/README.md`
- Modify: `trader-admin/README.md` (new pointer)

- [ ] **Step 1: Move directories**

Run:

```bash
mkdir -p trader-docs/trader-admin
git mv trader-admin/docs trader-docs/trader-admin/docs
git mv trader-admin/README.md trader-docs/trader-admin/README.md
```

Expected: `git status --porcelain` shows renames (R) for moved paths

- [ ] **Step 2: Create pointer README at old location**

Create `trader-admin/README.md` with:

```md
# trader-admin

Docs moved to: [trader-docs/trader-admin](../trader-docs/trader-admin/README.md)
```

- [ ] **Step 3: Commit**

```bash
git add trader-admin/README.md trader-docs/trader-admin
git commit -m "docs: move trader-admin docs into trader-docs"
```

---

### Task 3: Move trader-base docs and README

**Files:**
- Move: `trader-base/docs/**` → `trader-docs/trader-base/docs/**`
- Move: `trader-base/README.md` → `trader-docs/trader-base/README.md`
- Modify: `trader-base/README.md` (new pointer)

- [ ] **Step 1: Move directories**

Run:

```bash
mkdir -p trader-docs/trader-base
git mv trader-base/docs trader-docs/trader-base/docs
git mv trader-base/README.md trader-docs/trader-base/README.md
```

- [ ] **Step 2: Create pointer README at old location**

Create `trader-base/README.md` with:

```md
# trader-base

Docs moved to: [trader-docs/trader-base](../trader-docs/trader-base/README.md)
```

- [ ] **Step 3: Commit**

```bash
git add trader-base/README.md trader-docs/trader-base
git commit -m "docs: move trader-base docs into trader-docs"
```

---

### Task 4: Move trader-collector docs (including sample data) and README

**Files:**
- Move: `trader-collector/docs/**` → `trader-docs/trader-collector/docs/**`
- Move: `trader-collector/README.md` → `trader-docs/trader-collector/README.md`
- Modify: `trader-collector/README.md` (new pointer)

- [ ] **Step 1: Move directories**

Run:

```bash
mkdir -p trader-docs/trader-collector
git mv trader-collector/docs trader-docs/trader-collector/docs
git mv trader-collector/README.md trader-docs/trader-collector/README.md
```

- [ ] **Step 2: Create pointer README at old location**

Create `trader-collector/README.md` with:

```md
# trader-collector

Docs moved to: [trader-docs/trader-collector](../trader-docs/trader-collector/README.md)
```

- [ ] **Step 3: Commit**

```bash
git add trader-collector/README.md trader-docs/trader-collector
git commit -m "docs: move trader-collector docs into trader-docs"
```

---

### Task 5: Update repository-wide doc references

**Files:**
- Modify: any file matching old locations:
  - `trader-admin/docs/`
  - `trader-base/docs/`
  - `trader-collector/docs/`

- [ ] **Step 1: Find remaining references**

Run:

```bash
git grep -n "trader-admin/docs/" || true
git grep -n "trader-base/docs/" || true
git grep -n "trader-collector/docs/" || true
```

Expected: no output (or only references inside git history are irrelevant; working tree should be empty)

- [ ] **Step 2: Fix references**

Rules:

- If link is inside module docs that moved together, prefer keeping relative link unchanged unless it points outside the moved tree.
- If a link points to `trader-admin/docs/...`, rewrite to `trader-docs/trader-admin/docs/...` (or a relative path appropriate to file location).
- If a link pointed to module root README, decide whether it should now point to:
  - module pointer README (`trader-admin/README.md`) for developer entry
  - or the new full README (`trader-docs/trader-admin/README.md`) for docs browsing

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "docs: update links after moving docs"
```

---

### Task 6: Remove old doc directories and verify

**Files:**
- Delete: `trader-admin/docs/`
- Delete: `trader-base/docs/`
- Delete: `trader-collector/docs/`

- [ ] **Step 1: Verify old paths no longer exist**

Run:

```bash
test ! -e trader-admin/docs
test ! -e trader-base/docs
test ! -e trader-collector/docs
```

Expected: exit code 0 for all

- [ ] **Step 2: Verify tool rules paths unchanged**

Run:

```bash
test -f trader-admin/.trae/rules/project_rules.md
test -f trader-executor/rules/project_rules.md
```

Expected: exit code 0 for both

- [ ] **Step 3: Repository sanity check**

Run:

```bash
git status --porcelain
```

Expected: empty output

- [ ] **Step 4: Optional Maven sanity build (choose one module)**

Run (example):

```bash
cd trader-admin && ./mvnw -q -DskipTests package
```

Expected: success exit code 0

- [ ] **Step 5: Commit (if any leftover cleanup)**

```bash
git add -A
git commit -m "chore: cleanup after docs migration"
```

