# Task Create Dropdown Availability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hide strategy task definitions in the task-creation dropdown, and gray out already-created non-strategy definitions so users cannot select them again.

**Architecture:** Keep backend contracts unchanged and compute dropdown availability entirely inside `Manage.vue` by merging task definitions with the currently loaded task instance list. Use local derived option state for filtering, disabled display, and submit-time guarding while preserving existing backend duplicate-create protection.

**Tech Stack:** Vue 3, TypeScript, Element Plus, Vite

---

## File Map

### 需要修改

- `trader-admin/admin-web/src/pages/task/Manage.vue`
  - 计算创建弹窗可选定义列表
  - 过滤 `STRATEGY` 类型
  - 标记非策略已创建项为 disabled
  - 调整下拉渲染和创建提交前的兜底拦截

### 需要核对但原则上不改

- `trader-admin/admin-web/src/services/systemTask.ts`
  - 继续使用现有 `TaskDefinitionDto` 和 `SystemTaskDto` 类型
- `trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java`
  - 继续保留后端重复创建兜底，不在本任务中修改

### Task 1: Lock Dropdown Availability Rules In Frontend Behavior

**Files:**
- Modify: `trader-admin/admin-web/src/pages/task/Manage.vue`

- [ ] **Step 1: Add a failing frontend behavior check or equivalent local assertion target for dropdown option derivation**

```ts
const options = buildCreateDefinitionOptions(definitions, tableData)
expect(options.some(item => item.taskType === 'STRATEGY')).toBe(false)
expect(options.find(item => item.taskCode === 'fundSync')?.disabled).toBe(true)
```

- [ ] **Step 2: Run the focused frontend verification to confirm current behavior still shows all definitions as selectable**

Run: `npm --prefix trader-admin/admin-web run build`
Expected: PASS build, but code inspection confirms the dropdown still renders raw `definitions` without filtering or disabled state.

- [ ] **Step 3: Extract a local derived option shape inside `Manage.vue`**

```ts
type CreateTaskDefinitionOption = TaskDefinitionDto & {
  disabled: boolean
  disabledReason?: string
  optionLabel: string
}
```

- [ ] **Step 4: Implement a computed dropdown list that filters strategy definitions and disables existing non-strategy instances**

```ts
const createDefinitionOptions = computed<CreateTaskDefinitionOption[]>(() => {
  const existingKeys = new Set(tableData.value.map(item => `${item.taskType}::${item.taskCode}`))
  return definitions.value
    .filter(item => item.taskType !== 'STRATEGY')
    .map(item => {
      const disabled = existingKeys.has(`${item.taskType}::${item.taskCode}`)
      return {
        ...item,
        disabled,
        disabledReason: disabled ? '已创建' : undefined,
        optionLabel: disabled
          ? `${item.taskName || item.taskCode}（${item.taskType}/${item.taskCode}，已创建）`
          : `${item.taskName || item.taskCode}（${item.taskType}/${item.taskCode}）`
      }
    })
})
```

- [ ] **Step 5: Re-run frontend build to verify the new derived state compiles**

Run: `npm --prefix trader-admin/admin-web run build`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add trader-admin/admin-web/src/pages/task/Manage.vue
git commit -m "feat: compute task create dropdown availability"
```

### Task 2: Update Dropdown Rendering And Selection Guarding

**Files:**
- Modify: `trader-admin/admin-web/src/pages/task/Manage.vue`

- [ ] **Step 1: Switch the `<el-option>` loop from raw `definitions` to derived create options**

```vue
<el-option
  v-for="item in createDefinitionOptions"
  :key="`${item.taskType}::${item.taskCode}`"
  :label="item.optionLabel"
  :value="`${item.taskType}::${item.taskCode}`"
  :disabled="item.disabled" />
```

- [ ] **Step 2: Add a failing guard condition for submit when a disabled option is somehow still referenced**

```ts
if (selectedCreateDefinition.value?.disabled) {
  ElMessage.warning('该任务实例已创建，不能重复创建')
  return
}
```

- [ ] **Step 3: Rename or split the current `selectedDefinition` computed if needed so form display uses the new option shape**

```ts
const selectedCreateDefinition = computed(() => {
  if (!createForm.taskRef) return null
  return createDefinitionOptions.value.find(item => `${item.taskType}::${item.taskCode}` === createForm.taskRef) || null
})
```

- [ ] **Step 4: Update disabled-state cleanup when the selected entry becomes unavailable after reload**

```ts
watch(createDefinitionOptions, (options) => {
  if (!createForm.taskRef) return
  const current = options.find(item => `${item.taskType}::${item.taskCode}` === createForm.taskRef)
  if (!current || current.disabled) {
    createForm.taskRef = ''
  }
})
```

- [ ] **Step 5: Re-run frontend build to verify rendering and guards compile**

Run: `npm --prefix trader-admin/admin-web run build`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add trader-admin/admin-web/src/pages/task/Manage.vue
git commit -m "feat: disable created task definitions in create dropdown"
```

### Task 3: Verify Refresh Timing And No Backend Contract Expansion

**Files:**
- Modify: `trader-admin/admin-web/src/pages/task/Manage.vue`
- Verify: `trader-admin/admin-web/src/services/systemTask.ts`

- [ ] **Step 1: Ensure opening the create drawer refreshes definitions before rendering options**

```ts
const openCreate = async () => {
  createVisible.value = true
  await loadDefinitions()
}
```

- [ ] **Step 2: Ensure successful creation refreshes both table data and definitions so disabled state updates immediately**

```ts
await createTaskInstance(payload)
await Promise.all([loadData(), loadDefinitions()])
createVisible.value = false
```

- [ ] **Step 3: Run full frontend build verification**

Run: `npm --prefix trader-admin/admin-web run build`
Expected: PASS

- [ ] **Step 4: Verify backend contract files remain untouched**

Run:

```bash
git diff -- trader-admin/admin-web/src/services/systemTask.ts trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/service/SystemTaskService.java
```

Expected: no required protocol change for this dropdown-only task.

- [ ] **Step 5: Commit**

```bash
git add trader-admin/admin-web/src/pages/task/Manage.vue
git commit -m "test: verify task create dropdown availability flow"
```

