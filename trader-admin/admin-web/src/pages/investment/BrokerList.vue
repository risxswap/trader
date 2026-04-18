<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__content">
        <div class="title">券商列表</div>
        <div class="subtitle">统一维护券商资料、资金规模与详情入口，保持投资模块的列表体验一致</div>
      </div>
      <div class="page-header__actions">
        <el-button type="primary" @click="onAdd">新增券商</el-button>
      </div>
    </div>

    <el-card class="search-card">
      <div class="search-header">
        <div>
          <div class="section-title">筛选条件</div>
          <div class="section-subtitle">支持按券商名称和代号快速检索</div>
        </div>
        <div class="search-meta">当前 {{ rows.length }} 条</div>
      </div>
      <el-form class="search-form">
        <div class="search-grid">
          <el-form-item label="关键字">
            <el-input v-model="keyword" placeholder="名称/代号" clearable @keyup.enter="load" />
          </el-form-item>
        </div>
      </el-form>
      <div class="search-actions">
        <el-button type="primary" :loading="loading" :icon="Search" @click="load">查询</el-button>
      </div>
    </el-card>

    <el-card class="table-card">
      <div class="table-toolbar">
        <div>
          <div class="section-title">券商清单</div>
          <div class="section-subtitle">查看基础信息、资金规模与详情跳转</div>
        </div>
      </div>
      <el-table :data="rows" v-loading="loading" class="full" stripe>
        <el-table-column prop="id" label="ID" min-width="72" />
        <el-table-column label="券商信息" min-width="220">
          <template #default="{ row }">
            <div class="primary-cell">{{ row.name }}</div>
            <div class="secondary-cell">{{ row.code }}</div>
          </template>
        </el-table-column>
        <el-table-column label="资金概览" min-width="180">
          <template #default="{ row }">
            <div class="primary-cell">{{ formatCurrency(row.currentCapital) }}</div>
            <div class="secondary-cell">初始 {{ formatCurrency(row.initialCapital) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="资金变化" min-width="140">
          <template #default="{ row }">
            <span :class="capitalDelta(row) >= 0 ? 'text-red' : 'text-green'" class="primary-cell">
              {{ formatSignedCurrency(capitalDelta(row)) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="168" />
        <el-table-column prop="updatedAt" label="更新时间" min-width="168" />
        <el-table-column label="操作" fixed="right" min-width="180">
          <template #default="{ row }">
            <el-button link type="primary" @click="onDetail(row)">详情</el-button>
            <el-button link type="primary" @click="onEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="pageNo"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="load"
          @current-change="load"
        />
      </div>
    </el-card>

    <el-drawer
      v-model="editorVisible"
      :title="editForm.id ? '编辑券商' : '新增券商'"
      direction="rtl"
      size="460px"
      append-to-body
      destroy-on-close
      class="editor-drawer"
    >
      <div class="drawer-header">
        <div>
          <div class="section-title">{{ editForm.id ? '编辑券商' : '新增券商' }}</div>
          <div class="section-subtitle">统一维护券商标识、资金规模与说明信息</div>
        </div>
        <div class="drawer-header__meta">
          <el-tag effect="plain" type="info">{{ editForm.id ? '编辑模式' : '新增模式' }}</el-tag>
          <el-tag v-if="editForm.id" effect="plain" type="primary">券商ID {{ editForm.id }}</el-tag>
        </div>
      </div>

      <el-form :model="editForm" :rules="rules" ref="formRef" label-position="top" class="drawer-form">
        <div class="form-section">
          <div class="form-section__title">基础信息</div>
          <el-form-item label="名字" prop="name">
            <el-input v-model="editForm.name" />
          </el-form-item>
          <el-form-item label="代号" prop="code">
            <el-input v-model="editForm.code" />
          </el-form-item>
        </div>

        <div class="form-section">
          <div class="form-section__title">资金信息</div>
          <el-form-item label="初始资金" prop="initialCapital">
            <el-input v-model="editForm.initialCapital" type="number" />
          </el-form-item>
          <el-form-item label="当前资金" prop="currentCapital">
            <el-input v-model="editForm.currentCapital" type="number" />
          </el-form-item>
        </div>

        <div class="form-section">
          <div class="form-section__title">补充说明</div>
          <el-form-item label="简介" prop="intro">
            <el-input v-model="editForm.intro" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item label="备注" prop="remark">
            <el-input v-model="editForm.remark" type="textarea" :rows="2" />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="editorVisible = false">取消</el-button>
          <el-button type="primary" :loading="editLoading" @click="submitEdit">保存</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { listBrokers, addBroker, updateBroker, deleteBroker, type BrokerDto } from '../../services/basic'
import type { PageRes } from '../../types'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'

const router = useRouter()
const keyword = ref('')
const loading = ref(false)
const rows = ref<BrokerDto[]>([])
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)

const formatCurrency = (value?: number) => {
  if (value === undefined || value === null) return '-'
  return `¥${Number(value).toFixed(2)}`
}

const formatSignedCurrency = (value?: number) => {
  if (value === undefined || value === null) return '-'
  const amount = Number(value)
  const prefix = amount > 0 ? '+' : ''
  return `${prefix}¥${amount.toFixed(2)}`
}

const capitalDelta = (row: BrokerDto) => {
  return Number(row.currentCapital || 0) - Number(row.initialCapital || 0)
}

const editorVisible = ref(false)
const editLoading = ref(false)
const formRef = ref<FormInstance>()
const editForm = reactive<Partial<BrokerDto>>({})

const rules = {
  name: [{ required: true, message: '请输入名字', trigger: 'blur' }],
  code: [{ required: true, message: '请输入代号', trigger: 'blur' }]
}

const load = async () => {
  loading.value = true
  try {
    const res = await listBrokers({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      keyword: keyword.value
    })
    
    if (res.code === 200 && res.data) {
      const data = res.data as PageRes<BrokerDto>
      rows.value = data.items
      total.value = data.total
    } else {
      rows.value = []
      total.value = 0
    }
  } finally {
    loading.value = false
  }
}

const onAdd = () => {
  Object.keys(editForm).forEach(key => delete (editForm as any)[key])
  editorVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const onDetail = (row: BrokerDto) => {
  router.push(`/broker/detail/${row.id}`)
}

const onEdit = (row: BrokerDto) => {
  Object.assign(editForm, row)
  editorVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const onDelete = (row: BrokerDto) => {
  ElMessageBox.confirm(`确定要删除券商 "${row.name}" 吗？`, '提示', {
    type: 'warning'
  }).then(async () => {
    const res = await deleteBroker(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      load()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  })
}

const submitEdit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      editLoading.value = true
      try {
        const api = editForm.id ? updateBroker : addBroker
        const res = await api(editForm)
        if (res.code === 200) {
          ElMessage.success('保存成功')
          editorVisible.value = false
          load()
        } else {
          ElMessage.error(res.message || '保存失败')
        }
      } finally {
        editLoading.value = false
      }
    }
  })
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-header__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.title {
  color: #0f172a;
  font-size: 24px;
  font-weight: 600;
}

.subtitle {
  margin-top: 4px;
  color: #64748b;
  font-size: 13px;
}

.search-card,
.table-card {
  border: none;
  border-radius: 18px;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
}

.search-header,
.table-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.search-meta {
  color: #64748b;
  font-size: 13px;
}

.section-title {
  color: #0f172a;
  font-size: 16px;
  font-weight: 600;
}

.section-subtitle {
  margin-top: 4px;
  color: #64748b;
  font-size: 13px;
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

.search-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.full {
  width: 100%;
}

.primary-cell {
  color: #0f172a;
  font-weight: 500;
  line-height: 22px;
}

.secondary-cell {
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
}

.pager {
  padding-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.drawer-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.drawer-header__meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.drawer-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-section {
  padding: 16px;
  border-radius: 16px;
  background: #f8fafc;
}

.form-section__title {
  margin-bottom: 16px;
  color: #334155;
  font-size: 14px;
  font-weight: 600;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.editor-drawer :deep(.el-drawer__header) {
  margin-bottom: 0;
  padding-bottom: 8px;
  border-bottom: 1px solid #e8edf5;
}

.editor-drawer :deep(.el-drawer__body) {
  padding-top: 16px;
}

.drawer-form :deep(.el-input) {
  width: 100%;
}

.text-red {
  color: #f56c6c;
}

.text-green {
  color: #67c23a;
}

@media (max-width: 768px) {
  .page-header,
  .search-header,
  .table-toolbar,
  .search-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .page-header__actions,
  .search-actions {
    width: 100%;
  }

  .search-actions :deep(.el-button) {
    width: 100%;
  }

  .drawer-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .drawer-header__meta {
    justify-content: flex-start;
  }

  .search-grid {
    grid-template-columns: 1fr;
  }

  .pager {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>
