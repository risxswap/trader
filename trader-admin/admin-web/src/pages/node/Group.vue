<template>
  <div class="page-container">
    <el-card class="table-card" shadow="never">
      <div class="table-header">
        <div>
          <div class="title">分组管理</div>
          <div class="section-subtitle">支持按名称、编码和分组类型搜索，完成新增、编辑和删除操作。</div>
        </div>
        <div class="actions">
          <el-button :loading="loading" @click="load">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
          <el-button type="primary" @click="onAdd">
            <el-icon><Plus /></el-icon>
            新增分组
          </el-button>
        </div>
      </div>

      <el-card class="search-card" shadow="never">
        <div class="search-header">
          <div>
            <div class="section-title">筛选条件</div>
            <div class="section-subtitle">支持按名称、编码和分组类型快速过滤目标分组</div>
          </div>
          <div class="search-meta">当前 {{ filteredRows.length }} 条记录</div>
        </div>
        <el-form class="search-form" label-position="top">
          <div class="search-grid">
            <el-form-item label="关键字" class="search-grid__keyword">
              <el-input
                v-model="searchForm.keyword"
                clearable
                placeholder="搜索分组名称或编码"
                @keyup.enter="applySearch" />
            </el-form-item>
            <el-form-item label="分组类型" class="search-grid__type">
              <el-select v-model="searchForm.type">
                <el-option label="全部类型" value="all" />
                <el-option label="普通分组" value="common" />
                <el-option label="待审批默认分组" value="pending" />
              </el-select>
            </el-form-item>
          </div>
        </el-form>
        <div class="search-actions">
          <el-button type="primary" :icon="Search" @click="applySearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </div>
      </el-card>

      <div class="table-meta">
        <span>当前 {{ filteredRows.length }} 条记录</span>
        <span>默认待审批分组仅支持查看，不支持编辑和删除</span>
      </div>

      <el-table :data="filteredRows" v-loading="loading" class="group-table" stripe empty-text="暂无分组数据">
        <el-table-column prop="id" label="ID" min-width="72" />
        <el-table-column label="分组信息" min-width="240">
          <template #default="{ row }">
            <div class="group-main">
              <div class="group-name-row">
                <span class="group-name">{{ row.name }}</span>
                <el-tag v-if="row.defaultPending" type="warning" effect="light">只读</el-tag>
              </div>
              <div class="group-code">{{ row.code }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" min-width="100" />
        <el-table-column label="节点数量" min-width="130">
          <template #default="{ row }">
            <div class="count-badge">{{ row.nodeCount || 0 }}</div>
          </template>
        </el-table-column>
        <el-table-column label="分组类型" min-width="200">
          <template #default="{ row }">
            <div class="type-cell">
              <el-tag :type="row.defaultPending ? 'warning' : 'success'" effect="light">
                {{ row.defaultPending ? '待审批默认分组' : '普通分组' }}
              </el-tag>
              <span class="type-note">
                {{ row.defaultPending ? '系统内置只读规则' : '可维护的业务分组' }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" min-width="200">
          <template #default="{ row }">
            <div class="action-cell">
              <el-button link type="primary" :disabled="row.defaultPending" @click="onEdit(row)">编辑</el-button>
              <el-button link type="danger" :disabled="row.defaultPending" @click="onDelete(row)">删除</el-button>
              <span v-if="row.defaultPending" class="readonly-tip">默认分组仅查看</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>

  <el-dialog v-model="showEdit" :title="editForm.id ? '编辑分组' : '新增分组'" width="520px" destroy-on-close>
    <div class="dialog-intro">
      <div class="dialog-title">{{ editForm.id ? '更新分组信息' : '创建新的节点分组' }}</div>
      <div class="dialog-subtitle">优先填写分组名称和分组编码，排序用于控制展示顺序。</div>
    </div>
    <el-form ref="formRef" :model="editForm" :rules="rules" label-width="90px">
      <el-form-item label="分组名称" prop="name">
        <el-input v-model="editForm.name" maxlength="32" show-word-limit />
      </el-form-item>
      <el-form-item label="分组编码" prop="code">
        <el-input v-model="editForm.code" maxlength="32" show-word-limit placeholder="如：executor-default" />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="editForm.sort" :min="0" :step="1" class="sort-input" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showEdit = false">取消</el-button>
      <el-button type="primary" :loading="editLoading" @click="submitEdit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  addNodeGroup,
  deleteNodeGroup,
  listNodeGroups,
  updateNodeGroup,
  type NodeGroupDto,
  type NodeGroupParam
} from '../../services/basic'

const loading = ref(false)
const editLoading = ref(false)
const showEdit = ref(false)
const rows = ref<NodeGroupDto[]>([])
const formRef = ref<FormInstance>()
const searchForm = reactive({
  keyword: '',
  type: 'all'
})
const appliedSearch = reactive({
  keyword: '',
  type: 'all'
})
const editForm = reactive<NodeGroupParam>({
  name: '',
  code: '',
  sort: 0
})

const rules = {
  name: [{ required: true, message: '请输入分组名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入分组编码', trigger: 'blur' }]
}

const filteredRows = computed(() => {
  const keyword = appliedSearch.keyword.trim().toLowerCase()
  return rows.value.filter((row) => {
    const matchesKeyword = !keyword
      || row.name.toLowerCase().includes(keyword)
      || row.code.toLowerCase().includes(keyword)
    const matchesType = appliedSearch.type === 'all'
      || (appliedSearch.type === 'pending' && row.defaultPending)
      || (appliedSearch.type === 'common' && !row.defaultPending)
    return matchesKeyword && matchesType
  })
})

const resetEditForm = () => {
  editForm.id = undefined
  editForm.name = ''
  editForm.code = ''
  editForm.sort = 0
}

const applySearch = () => {
  appliedSearch.keyword = searchForm.keyword
  appliedSearch.type = searchForm.type
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.type = 'all'
  applySearch()
}

const load = async () => {
  loading.value = true
  try {
    const res = await listNodeGroups()
    if (res.code === 200 && res.data) {
      rows.value = res.data
      return
    }
    rows.value = []
    ElMessage.error(res.message || '获取分组列表失败')
  } finally {
    loading.value = false
  }
}

const onAdd = () => {
  resetEditForm()
  showEdit.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const onEdit = (row: NodeGroupDto) => {
  editForm.id = row.id
  editForm.name = row.name
  editForm.code = row.code
  editForm.sort = row.sort ?? 0
  showEdit.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const onDelete = (row: NodeGroupDto) => {
  ElMessageBox.confirm(`确定删除分组“${row.name}”吗？`, '提示', {
    type: 'warning'
  }).then(async () => {
    const res = await deleteNodeGroup(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      load()
      return
    }
    ElMessage.error(res.message || '删除失败')
  })
}

const submitEdit = async () => {
  if (!formRef.value) {
    return
  }
  await formRef.value.validate(async (valid) => {
    if (!valid) {
      return
    }
    editLoading.value = true
    try {
      const payload: NodeGroupParam = {
        id: editForm.id,
        name: editForm.name.trim(),
        code: editForm.code.trim(),
        sort: editForm.sort ?? 0
      }
      const api = payload.id ? updateNodeGroup : addNodeGroup
      const res = await api(payload)
      if (res.code === 200) {
        ElMessage.success('保存成功')
        showEdit.value = false
        load()
        return
      }
      ElMessage.error(res.message || '保存失败')
    } finally {
      editLoading.value = false
    }
  })
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.page-container {
  min-width: 0;
}

.title {
  font-size: 22px;
  font-weight: 600;
  color: #0f172a;
}

.actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.table-card {
  border-radius: 20px;
  border: 1px solid #e5e7eb;
}

.table-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.section-subtitle {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.6;
  color: #64748b;
}

.search-card {
  margin-bottom: 16px;
  border: none;
  border-radius: 18px;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
}

.search-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.section-title {
  color: #0f172a;
  font-size: 16px;
  font-weight: 600;
}

.search-meta {
  color: #64748b;
  font-size: 13px;
  white-space: nowrap;
}

.search-form {
  margin-bottom: 8px;
}

.search-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px 16px;
}

.search-grid :deep(.el-form-item) {
  margin-bottom: 0;
}

.search-grid :deep(.el-input),
.search-grid :deep(.el-select),
.search-grid :deep(.el-date-editor),
.search-grid :deep(.el-input-number) {
  width: 100%;
}

.search-grid__keyword {
  grid-column: span 3;
}

.search-grid__type {
  grid-column: span 1;
}

.search-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.table-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
  color: #64748b;
  font-size: 13px;
}

.group-table {
  width: 100%;
}

:deep(.group-table .el-table__cell) {
  padding-top: 14px;
  padding-bottom: 14px;
}

.group-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.group-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.group-name {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
}

.group-code {
  font-size: 13px;
  color: #64748b;
}

.count-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 40px;
  height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: #ecfeff;
  color: #0f766e;
  font-weight: 600;
}

.type-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.type-note {
  font-size: 12px;
  line-height: 1.5;
  color: #64748b;
}

.action-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.readonly-tip {
  font-size: 12px;
  color: #a16207;
}

.dialog-intro {
  margin-bottom: 20px;
  padding: 14px 16px;
  border-radius: 14px;
  background: #f8fafc;
}

.dialog-title {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
}

.dialog-subtitle {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.6;
  color: #64748b;
}

.sort-input {
  width: 100%;
}

@media (max-width: 960px) {
  .table-header {
    flex-direction: column;
  }
}

@media (max-width: 768px) {
  .search-header,
  .search-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .search-grid {
    grid-template-columns: 1fr;
  }

  .search-grid__keyword,
  .search-grid__type {
    grid-column: span 1;
  }

  .search-actions :deep(.el-button) {
    width: 100%;
  }
}

@media (max-width: 640px) {
  .table-meta {
    flex-direction: column;
    align-items: flex-start;
  }

  .actions {
    width: 100%;
  }

  .actions :deep(.el-button) {
    flex: 1;
  }
}
</style>
