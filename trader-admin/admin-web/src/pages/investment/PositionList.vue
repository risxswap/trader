<template>
  <div class="nested-page">
    <div class="nested-header">
      <div class="nested-header__content">
        <div class="title">持仓管理</div>
        <div class="subtitle">统一查看持仓标的、方向、价格与更新时间</div>
      </div>
      <div class="nested-header__meta">
        <el-tag v-if="fixedInvestmentId" effect="plain" type="primary">{{ currentContextText }}</el-tag>
        <el-tag effect="plain" type="info">当前 {{ total }} 条</el-tag>
      </div>
    </div>

    <el-card class="search-card">
      <div class="search-header">
        <div>
          <div class="section-title">筛选条件</div>
          <div class="section-subtitle">支持按标的和所属投资快速检索持仓记录</div>
        </div>
      </div>
      <el-form class="search-form">
        <div class="search-grid">
          <el-form-item label="标的">
            <el-input v-model="asset" placeholder="请输入标的代码" clearable @keyup.enter="load" />
          </el-form-item>
          <el-form-item v-if="!fixedInvestmentId" label="投资ID">
            <el-input v-model="filterInvestmentId" placeholder="请输入投资ID" clearable @keyup.enter="load" />
          </el-form-item>
        </div>
      </el-form>
      <div class="search-actions">
        <el-button type="primary" :loading="loading" :icon="Search" @click="load">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>
    </el-card>

    <el-card class="table-card">
      <div class="table-toolbar">
        <div>
          <div class="section-title">持仓列表</div>
          <div class="section-subtitle">详情、编辑和删除保持统一操作顺序</div>
        </div>
        <el-button type="primary" @click="onAdd">添加持仓</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" class="full" stripe>
        <el-table-column prop="id" label="ID" width="84" />
        <el-table-column v-if="!fixedInvestmentId" label="所属投资" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="primary-cell">{{ getInvestmentDisplayName(row.investmentId) }}</div>
            <div class="secondary-cell">投资ID {{ row.investmentId || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="标的信息" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="primary-cell">{{ row.asset || '-' }}</div>
            <div class="secondary-cell">{{ assetTypeText(row.assetType) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="方向" width="108">
          <template #default="{ row }">
            <el-tag :type="sideTagType(row.side)">{{ sideText(row.side) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="仓位与价格" min-width="180">
          <template #default="{ row }">
            <div class="primary-cell">数量 {{ formatQuantity(row.quantity) }}</div>
            <div class="secondary-cell">买入 {{ formatPrice(row.buyPrice) }} · 成本 {{ formatPrice(row.costPrice) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="时间" min-width="180">
          <template #default="{ row }">
            <div class="primary-cell">{{ formatDate(row.updatedAt) || '-' }}</div>
            <div class="secondary-cell">创建于 {{ formatDate(row.createdAt) || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
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
      :title="isEdit ? '编辑持仓' : '添加持仓'"
      direction="rtl"
      size="460px"
      append-to-body
      destroy-on-close
      class="editor-drawer"
    >
      <div class="drawer-header">
        <div class="section-title">{{ isEdit ? '编辑持仓' : '添加持仓' }}</div>
        <div class="section-subtitle">统一维护所属投资、标的属性与价格数量信息</div>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="drawer-form">
        <div class="form-section">
          <div class="form-section__title">归属信息</div>
          <el-form-item v-if="!fixedInvestmentId" label="所属投资" prop="investmentId">
            <el-select v-model="form.investmentId" placeholder="请选择投资计划" filterable>
              <el-option v-for="item in investmentOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item v-else label="所属投资">
            <el-input :model-value="currentInvestmentName || '-'" disabled />
          </el-form-item>
        </div>

        <div class="form-section">
          <div class="form-section__title">标的属性</div>
          <el-form-item label="类型" prop="assetType">
            <el-select v-model="form.assetType" placeholder="请选择类型" @change="onTypeChange">
              <el-option label="基金" value="FUND" />
              <el-option label="ETF" value="ETF" />
              <el-option label="股票" value="STOCK" />
              <el-option label="期货" value="FUTURES" />
            </el-select>
          </el-form-item>
          <el-form-item label="标的" prop="asset">
            <el-select
              v-model="form.asset"
              placeholder="请选择或输入标的"
              filterable
              remote
              :remote-method="searchSymbols"
              :loading="symbolLoading"
            >
              <el-option v-for="item in symbolOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="方向" prop="side">
            <el-select v-model="form.side" placeholder="请选择方向">
              <el-option label="多头" value="LONG" />
              <el-option label="空头" value="SHORT" />
            </el-select>
          </el-form-item>
        </div>

        <div class="form-section">
          <div class="form-section__title">仓位信息</div>
          <el-form-item label="数量" prop="quantity">
            <el-input-number v-model="form.quantity" controls-position="right" :min="0" style="width: 100%" />
          </el-form-item>
          <el-form-item label="买入价" prop="buyPrice">
            <el-input-number v-model="form.buyPrice" controls-position="right" :min="0" :precision="4" style="width: 100%" />
          </el-form-item>
          <el-form-item label="成本价" prop="costPrice">
            <el-input-number v-model="form.costPrice" controls-position="right" :min="0" :precision="4" style="width: 100%" />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="editorVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">保存</el-button>
        </div>
      </template>
    </el-drawer>

    <el-drawer
      v-model="detailVisible"
      title="持仓详情"
      direction="rtl"
      size="520px"
      append-to-body
      destroy-on-close
      class="detail-drawer"
    >
      <div class="drawer-header">
        <div>
          <div class="section-title">持仓详情</div>
          <div class="section-subtitle">查看当前持仓的归属信息、价格结构与时间记录</div>
        </div>
        <div class="drawer-header__meta">
          <el-tag v-if="detailRow?.id" effect="plain" type="info">持仓ID {{ detailRow.id }}</el-tag>
        </div>
      </div>

      <div class="detail-panel">
        <div class="detail-summary">
          <div>
            <div class="detail-title">{{ detailRow?.asset || '-' }}</div>
            <div class="detail-subtitle">{{ assetTypeText(detailRow?.assetType) }}</div>
          </div>
          <el-tag :type="sideTagType(detailRow?.side)">{{ sideText(detailRow?.side) }}</el-tag>
        </div>

        <div class="form-section">
          <div class="form-section__title">基础信息</div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="ID">{{ detailRow?.id || '-' }}</el-descriptions-item>
            <el-descriptions-item label="所属投资">{{ getInvestmentDisplayName(detailRow?.investmentId) }}</el-descriptions-item>
            <el-descriptions-item label="方向">{{ sideText(detailRow?.side) }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="form-section">
          <div class="form-section__title">仓位信息</div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="持仓数量">{{ formatQuantity(detailRow?.quantity) }}</el-descriptions-item>
            <el-descriptions-item label="买入价">{{ formatPrice(detailRow?.buyPrice) }}</el-descriptions-item>
            <el-descriptions-item label="成本价">{{ formatPrice(detailRow?.costPrice) }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="form-section">
          <div class="form-section__title">时间信息</div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="创建时间">{{ formatDate(detailRow?.createdAt) || '-' }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ formatDate(detailRow?.updatedAt) || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </div>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch, nextTick } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { listPositions, addPosition, updatePosition, deletePosition, getSymbols, listInvestments } from '../../services/basic'
import type { InvestmentPositionDto, InvestmentDto } from '../../services/basic'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps<{
  investmentId?: number
}>()

const fixedInvestmentId = computed(() => props.investmentId)
const asset = ref('')
const filterInvestmentId = ref('')
const loading = ref(false)
const rows = ref<InvestmentPositionDto[]>([])
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)

const investments = ref<InvestmentDto[]>([])
const investmentOptions = computed(() => investments.value)
const currentInvestmentName = computed(() => {
  if (!fixedInvestmentId.value) return ''
  return getInvestmentDisplayName(fixedInvestmentId.value)
})
const currentContextText = computed(() => {
  return currentInvestmentName.value ? `当前投资：${currentInvestmentName.value}` : '当前为指定投资持仓'
})

const formatPrice = (value?: number) => {
  if (value === undefined || value === null) return '-'
  return `¥${Number(value).toFixed(4)}`
}

const formatQuantity = (value?: number) => {
  if (value === undefined || value === null) return '-'
  return `${Number(value)}`
}

const formatDate = (value?: string) => {
  if (!value) return ''
  return value
}

const assetTypeText = (type?: string) => {
  if (type === 'FUND') return '基金'
  if (type === 'ETF') return 'ETF'
  if (type === 'STOCK') return '股票'
  if (type === 'FUTURES') return '期货'
  return type || '-'
}

const sideText = (side?: string) => {
  if (side === 'LONG') return '多头'
  if (side === 'SHORT') return '空头'
  return side || '-'
}

const sideTagType = (side?: string) => {
  return side === 'LONG' ? 'success' : 'danger'
}

const loadInvestments = async () => {
  try {
    const res = await listInvestments({ pageNo: 1, pageSize: 100 })
    if (res.code === 200 && res.data) {
      investments.value = res.data.items
    }
  } catch (e) {
    console.error(e)
  }
}

const getInvestmentDisplayName = (id?: number) => {
  if (!id) return '-'
  const found = investments.value.find(i => i.id === id)
  return found ? `${found.name} (${id})` : `${id}`
}

const resetSearch = () => {
  asset.value = ''
  filterInvestmentId.value = ''
  pageNo.value = 1
  load()
}

const load = async () => {
  loading.value = true
  try {
    const res = await listPositions({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      asset: asset.value,
      investmentId: fixedInvestmentId.value || (filterInvestmentId.value ? Number(filterInvestmentId.value) : undefined)
    })
    if (res.code === 200 && res.data) {
      rows.value = res.data.items
      total.value = res.data.total
    } else {
      rows.value = []
      total.value = 0
    }
  } finally {
    loading.value = false
  }
}

watch(() => props.investmentId, () => {
  pageNo.value = 1
  load()
})

const editorVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref()
const form = reactive<Partial<InvestmentPositionDto>>({
  investmentId: undefined,
  assetType: '',
  asset: '',
  side: '',
  quantity: 0,
  buyPrice: 0,
  costPrice: 0
})

const rules = {
  investmentId: [{ required: true, message: '请选择投资计划', trigger: 'change' }],
  assetType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  asset: [{ required: true, message: '请输入或选择标的', trigger: 'change' }],
  side: [{ required: true, message: '请选择方向', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }],
  buyPrice: [{ required: true, message: '请输入买入价', trigger: 'blur' }],
  costPrice: [{ required: true, message: '请输入成本价', trigger: 'blur' }]
}

const symbolLoading = ref(false)
const symbolOptions = ref<{ value: string; label: string }[]>([])

const searchSymbols = async (query: string) => {
  if (!form.assetType) {
    ElMessage.warning('请先选择类型')
    return
  }
  if (query) {
    symbolLoading.value = true
    try {
      const res = await getSymbols(form.assetType, query)
      if (res.code === 200 && res.data) {
        symbolOptions.value = res.data
      } else {
        symbolOptions.value = []
      }
    } finally {
      symbolLoading.value = false
    }
  } else {
    symbolOptions.value = []
  }
}

const onTypeChange = () => {
  form.asset = ''
  symbolOptions.value = []
}

const resetForm = () => {
  form.id = undefined
  form.investmentId = fixedInvestmentId.value
  form.assetType = ''
  form.asset = ''
  form.side = ''
  form.quantity = 0
  form.buyPrice = 0
  form.costPrice = 0
  symbolOptions.value = []
}

const onAdd = () => {
  isEdit.value = false
  resetForm()
  editorVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const onEdit = (row: InvestmentPositionDto) => {
  isEdit.value = true
  Object.assign(form, row)
  symbolOptions.value = [{ value: row.asset, label: row.asset }]
  editorVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const onDelete = (row: InvestmentPositionDto) => {
  ElMessageBox.confirm(`确定要删除 ${row.asset} 的持仓吗？`, '提示', {
    type: 'warning'
  }).then(async () => {
    const res = await deletePosition(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      load()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  })
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      submitLoading.value = true
      try {
        const api = isEdit.value ? updatePosition : addPosition
        const res = await api(form)
        if (res.code === 200) {
          ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
          editorVisible.value = false
          load()
        } else {
          ElMessage.error(res.message || '操作失败')
        }
      } finally {
        submitLoading.value = false
      }
    }
  })
}

const detailVisible = ref(false)
const detailRow = ref<InvestmentPositionDto>()

const onDetail = (row: InvestmentPositionDto) => {
  detailRow.value = row
  detailVisible.value = true
}

onMounted(() => {
  loadInvestments()
  load()
})
</script>

<style scoped>
.nested-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.nested-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 24px;
  border-radius: 18px;
  background: linear-gradient(135deg, #ffffff 0%, #f8fbff 100%);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
}

.nested-header__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.title {
  color: #0f172a;
  font-size: 20px;
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
  align-items: center;
  justify-content: space-between;
  gap: 16px;
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
  margin-top: 16px;
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

.table-toolbar {
  margin-bottom: 16px;
}

.full {
  width: 100%;
}

.primary-cell {
  color: #0f172a;
  font-weight: 600;
  line-height: 22px;
}

.secondary-cell {
  margin-top: 4px;
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
  margin-bottom: 20px;
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

.detail-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-summary {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border-radius: 16px;
  background: #f8fafc;
}

.detail-title {
  color: #0f172a;
  font-size: 18px;
  font-weight: 600;
}

.detail-subtitle {
  margin-top: 4px;
  color: #64748b;
  font-size: 13px;
}

.editor-drawer :deep(.el-drawer__body) {
  padding-top: 8px;
}

.detail-drawer :deep(.el-drawer__header) {
  margin-bottom: 0;
  padding-bottom: 8px;
  border-bottom: 1px solid #e8edf5;
}

.detail-drawer :deep(.el-drawer__body) {
  padding-top: 16px;
}

.detail-panel :deep(.el-descriptions__body) {
  border-radius: 12px;
  overflow: hidden;
}

.drawer-form :deep(.el-select),
.drawer-form :deep(.el-input),
.drawer-form :deep(.el-input-number) {
  width: 100%;
}

@media (max-width: 768px) {
  .nested-header,
  .search-header,
  .table-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-grid {
    grid-template-columns: 1fr;
  }

  .search-actions {
    justify-content: flex-start;
  }

  .drawer-header,
  .detail-summary {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
