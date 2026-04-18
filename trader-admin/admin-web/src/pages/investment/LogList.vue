<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__content">
        <div class="title">资金记录</div>
        <div class="subtitle">集中查看入金、出金与交易型资金变动，保持投资模块的数据追踪清晰可查</div>
      </div>
      <div class="page-header__actions">
        <el-button type="primary" :icon="Plus" @click="handleAdd">添加</el-button>
      </div>
    </div>

    <el-card class="search-card">
      <div class="search-header">
        <div>
          <div class="section-title">筛选条件</div>
          <div class="section-subtitle">按记录类型和投资计划快速过滤资金流水</div>
        </div>
        <div class="search-meta">当前 {{ rows.length }} 条</div>
      </div>
      <el-form class="search-form">
        <div class="search-grid">
          <el-form-item label="类型">
            <el-select v-model="type" placeholder="全部" clearable @change="load">
              <el-option label="入金" value="DEPOSIT" />
              <el-option label="出金" value="WITHDRAWAL" />
              <el-option label="交易" value="TRADE" />
            </el-select>
          </el-form-item>
          <el-form-item label="投资ID">
            <el-input v-model="investmentId" placeholder="投资ID" clearable @keyup.enter="load" />
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
          <div class="section-title">记录列表</div>
          <div class="section-subtitle">统一展示类型、金额结构、记录时间和编辑入口</div>
        </div>
      </div>
      <el-table :data="rows" v-loading="loading" class="full" stripe>
        <el-table-column prop="id" label="ID" min-width="76" />
        <el-table-column prop="investmentId" label="投资ID" min-width="92" />
        <el-table-column prop="type" label="类型" min-width="110">
          <template #default="{ row }">
            <el-tag :type="logTypeTagType(row.type)">{{ logTypeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="recordDate" label="记录日期" min-width="180">
          <template #default="{ row }">
            {{ formatDate(row.recordDate) }}
          </template>
        </el-table-column>
        <el-table-column label="资金结构" min-width="180">
          <template #default="{ row }">
            <div class="primary-cell">{{ formatCurrency(row.asset) }}</div>
            <div class="secondary-cell">现金 {{ formatCurrency(row.cash) }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="profit" label="收益" min-width="120" align="right">
          <template #default="{ row }">
            <span :class="(row.profit || 0) >= 0 ? 'text-red' : 'text-green'" class="primary-cell">
              {{ formatSignedCurrency(row.profit) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" fixed="right" min-width="150">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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
      :title="dialogTitle"
      direction="rtl"
      size="460px"
      append-to-body
      destroy-on-close
      class="editor-drawer"
    >
      <div class="drawer-header">
        <div>
          <div class="section-title">{{ dialogTitle }}</div>
          <div class="section-subtitle">统一维护投资资金记录的类型、金额结构与时间信息</div>
        </div>
        <div class="drawer-header__meta">
          <el-tag effect="plain" type="info">{{ form.id ? '编辑模式' : '新增模式' }}</el-tag>
          <el-tag v-if="form.id" effect="plain" type="primary">记录ID {{ form.id }}</el-tag>
        </div>
      </div>

      <el-form :model="form" label-position="top" class="drawer-form">
        <div class="form-section">
          <div class="form-section__title">基础信息</div>
          <el-form-item label="投资ID">
            <el-input v-model.number="form.investmentId" />
          </el-form-item>
          <el-form-item label="类型">
            <el-select v-model="form.type" placeholder="选择类型">
              <el-option label="入金" value="DEPOSIT" />
              <el-option label="出金" value="WITHDRAWAL" />
              <el-option label="交易" value="TRADE" />
            </el-select>
          </el-form-item>
          <el-form-item label="记录日期">
            <el-date-picker v-model="form.recordDate" type="datetime" placeholder="选择日期时间" />
          </el-form-item>
        </div>

        <div class="form-section">
          <div class="form-section__title">金额与备注</div>
          <el-form-item label="现金">
            <el-input v-model="form.cash" />
          </el-form-item>
          <el-form-item label="资产">
            <el-input v-model="form.asset" />
          </el-form-item>
          <el-form-item label="收益">
            <el-input v-model="form.profit" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="4" />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="editorVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">保存</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
    listInvestmentLogs, 
    addInvestmentLog, 
    updateInvestmentLog, 
    deleteInvestmentLog,
    type InvestmentLogDto 
} from '../../services/basic'
import type { PageRes } from '../../types'
import dayjs from 'dayjs'

const type = ref('')
const investmentId = ref('')
const loading = ref(false)
const rows = ref<InvestmentLogDto[]>([])
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)

const editorVisible = ref(false)
const dialogTitle = ref('')
const form = ref<Partial<InvestmentLogDto>>({})

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

const logTypeText = (value?: string) => {
  if (value === 'DEPOSIT') return '入金'
  if (value === 'WITHDRAWAL') return '出金'
  if (value === 'TRADE') return '交易'
  return value || '-'
}

const logTypeTagType = (value?: string) => {
  if (value === 'DEPOSIT') return 'success'
  if (value === 'WITHDRAWAL') return 'warning'
  if (value === 'TRADE') return 'primary'
  return 'info'
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  return dayjs(dateStr).format('YYYY-MM-DD HH:mm:ss')
}

const load = async () => {
  loading.value = true
  try {
    const res = await listInvestmentLogs({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      investmentId: investmentId.value ? Number(investmentId.value) : undefined,
      type: type.value || undefined
    })
    
    if (res.code === 200 && res.data) {
      const data = res.data as PageRes<InvestmentLogDto>
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

const handleAdd = () => {
    dialogTitle.value = '添加日志'
    form.value = {
        recordDate: dayjs().toISOString()
    }
    editorVisible.value = true
}

const handleEdit = (row: InvestmentLogDto) => {
    dialogTitle.value = '编辑日志'
    form.value = { ...row }
    editorVisible.value = true
}

const handleDelete = (row: InvestmentLogDto) => {
    ElMessageBox.confirm('确认删除该记录?', '警告', {
        type: 'warning'
    }).then(async () => {
        const res = await deleteInvestmentLog(row.id)
        if (res.code === 200) {
            ElMessage.success('删除成功')
            load()
        } else {
            ElMessage.error(res.message || '删除失败')
        }
    })
}

const submitForm = async () => {
    const payload = { ...form.value }
    if (payload.recordDate) {
        payload.recordDate = dayjs(payload.recordDate).toISOString()
    }

    let res;
    if (payload.id) {
        res = await updateInvestmentLog(payload)
    } else {
        res = await addInvestmentLog(payload)
    }

    if (res.code === 200) {
        ElMessage.success('保存成功')
        editorVisible.value = false
        load()
    } else {
        ElMessage.error(res.message || '保存失败')
    }
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

.drawer-form :deep(.el-input),
.drawer-form :deep(.el-select),
.drawer-form :deep(.el-date-editor) {
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
