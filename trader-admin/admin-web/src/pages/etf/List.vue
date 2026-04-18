<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__content">
        <div class="title">ETF基金列表</div>
        <div class="subtitle">统一查看 ETF 基金基础档案、费用结构、管理机构与维护状态</div>
      </div>
      <div class="page-header__meta">
        <el-tag effect="plain" type="primary">ETF 市场</el-tag>
        <el-tag effect="plain" type="info">共 {{ total }} 条</el-tag>
      </div>
    </div>

    <el-card class="search-card">
      <div class="search-header">
        <div>
          <div class="section-title">筛选条件</div>
          <div class="section-subtitle">支持按代码、机构、投资类型和费用区间快速筛选基金列表</div>
        </div>
        <div class="search-meta">
          <span>当前页 {{ rows.length }} 条</span>
          <span v-if="sortBy">排序字段 {{ sortBy }}</span>
        </div>
      </div>

      <el-form class="search-form">
        <div class="search-grid">
          <el-form-item label="关键字">
            <el-input v-model="keyword" placeholder="请输入代码或名称" clearable @keyup.enter="load" />
          </el-form-item>
          <el-form-item label="管理人">
            <el-input v-model="management" placeholder="请输入管理人" clearable @keyup.enter="load" />
          </el-form-item>
          <el-form-item label="托管人">
            <el-input v-model="custodian" placeholder="请输入托管人" clearable @keyup.enter="load" />
          </el-form-item>
          <el-form-item label="投资类型">
            <el-input v-model="fundType" placeholder="请输入投资类型" clearable @keyup.enter="load" />
          </el-form-item>
          <el-form-item label="管理费区间">
            <div class="range-inputs">
              <el-input-number v-model="managementFeeMin" :min="0" controls-position="right" />
              <span class="range-separator">至</span>
              <el-input-number v-model="managementFeeMax" :min="0" controls-position="right" />
            </div>
          </el-form-item>
          <el-form-item label="托管费区间">
            <div class="range-inputs">
              <el-input-number v-model="custodianFeeMin" :min="0" controls-position="right" />
              <span class="range-separator">至</span>
              <el-input-number v-model="custodianFeeMax" :min="0" controls-position="right" />
            </div>
          </el-form-item>
        </div>
      </el-form>

      <div class="search-actions">
        <el-button type="primary" :loading="loading" :icon="Search" @click="load">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>
    </el-card>

    <el-card class="table-card">
      <div class="table-toolbar">
        <div>
          <div class="section-title">基金列表</div>
          <div class="section-subtitle">详情、编辑和删除遵循统一的后台列表交互顺序</div>
        </div>
        <div class="table-toolbar__meta">分页 {{ pageNo }} / {{ totalPage }}</div>
      </div>

      <el-table :data="rows" v-loading="loading" class="full" stripe @sort-change="onSortChange">
        <el-table-column label="基金信息" min-width="220">
          <template #default="{ row }">
            <div class="primary-cell">{{ row.name || '-' }}</div>
            <div class="secondary-cell">{{ row.code || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态与市场" min-width="180">
          <template #default="{ row }">
            <div class="cell-tags">
              <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
              <el-tag effect="plain" type="info">{{ row.exchange || row.market || '-' }}</el-tag>
            </div>
            <div class="secondary-cell">{{ marketText(row.market) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="机构信息" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="primary-cell">{{ row.management || '-' }}</div>
            <div class="secondary-cell">{{ row.custodian || '未配置托管人' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="投资分类" min-width="140">
          <template #default="{ row }">
            <div class="primary-cell">{{ row.fundType || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="managementFee" label="费率信息" min-width="140" sortable="custom">
          <template #default="{ row }">
            <div class="primary-cell">管理费 {{ formatPercent(row.managementFee) }}</div>
            <div class="secondary-cell">托管费 {{ formatPercent(row.custodianFee) }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="listDate" label="时间信息" min-width="210" sortable="custom">
          <template #default="{ row }">
            <div class="primary-cell">上市 {{ formatDate(row.listDate) }}</div>
            <div class="secondary-cell">更新 {{ formatDateTime(row.updatedAt) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onView(row)">详情</el-button>
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
      title="编辑基金"
      direction="rtl"
      size="520px"
      append-to-body
      destroy-on-close
      class="editor-drawer"
    >
      <div class="drawer-header">
        <div>
          <div class="section-title">编辑基金</div>
          <div class="section-subtitle">统一维护 ETF 基金名称、状态和市场归属信息</div>
        </div>
        <div class="drawer-header__meta">
          <el-tag effect="plain" type="primary">{{ editForm.code || '-' }}</el-tag>
          <el-tag effect="plain" type="info">ETF</el-tag>
        </div>
      </div>

      <el-form :model="editForm" label-position="top" class="drawer-form">
        <div class="drawer-context">
          <div class="drawer-context__title">编辑上下文</div>
          <div class="drawer-context__meta">
            <div class="meta-item">
              <span class="meta-item__label">基金代码</span>
              <span class="meta-item__value">{{ editForm.code || '-' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-item__label">当前状态</span>
              <span class="meta-item__value">{{ statusText(editForm.status) }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-item__label">市场归属</span>
              <span class="meta-item__value">{{ editForm.exchange || editForm.market || '-' }}</span>
            </div>
          </div>
        </div>

        <div class="form-section">
          <div class="form-section__title">基础信息</div>
          <el-form-item label="名称">
            <el-input v-model="editForm.name" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="editForm.status" placeholder="选择状态">
              <el-option label="已上市" value="L" />
              <el-option label="发行中" value="I" />
              <el-option label="已摘牌" value="D" />
            </el-select>
          </el-form-item>
        </div>

        <div class="form-section">
          <div class="form-section__title">市场归属</div>
          <el-form-item label="市场">
            <el-input v-model="editForm.market" />
          </el-form-item>
          <el-form-item label="交易所">
            <el-input v-model="editForm.exchange" />
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
import { computed, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { Search } from '@element-plus/icons-vue'
import { listFunds, type FundDto, type PageRes, updateFund, deleteFund } from '../../services/fund'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'

const loading = ref(false)
const rows = ref<FundDto[]>([])
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)
const keyword = ref('')
const management = ref('')
const custodian = ref('')
const fundType = ref('')
const managementFeeMin = ref<number>()
const managementFeeMax = ref<number>()
const custodianFeeMin = ref<number>()
const custodianFeeMax = ref<number>()
const sortBy = ref<string>('')
const sortOrder = ref<'asc' | 'desc' | ''>('')
const router = useRouter()
const editorVisible = ref(false)
const editLoading = ref(false)
const editForm = ref<{ code: string; name?: string; status?: string; market?: string; exchange?: string }>({ code: '' })

const totalPage = computed(() => {
  if (!total.value) return 1
  return Math.ceil(total.value / pageSize.value)
})

const formatPercent = (value?: number) => {
  if (value === undefined || value === null) return '-'
  return `${Number(value).toFixed(4)}%`
}

const formatDate = (value?: string) => {
  if (!value) return '-'
  const formatted = dayjs(value)
  return formatted.isValid() ? formatted.format('YYYY-MM-DD') : value
}

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  const formatted = dayjs(value)
  return formatted.isValid() ? formatted.format('YYYY-MM-DD HH:mm:ss') : value
}

const statusText = (value?: string) => {
  if (value === 'L') return '已上市'
  if (value === 'I') return '发行中'
  if (value === 'D') return '已摘牌'
  return value || '-'
}

const statusTagType = (value?: string) => {
  if (value === 'L') return 'success'
  if (value === 'I') return 'warning'
  if (value === 'D') return 'info'
  return 'info'
}

const marketText = (value?: string) => {
  if (value === 'E') return 'ETF 市场'
  return value || '-'
}

const resetSearch = () => {
  keyword.value = ''
  management.value = ''
  custodian.value = ''
  fundType.value = ''
  managementFeeMin.value = undefined
  managementFeeMax.value = undefined
  custodianFeeMin.value = undefined
  custodianFeeMax.value = undefined
  sortBy.value = ''
  sortOrder.value = ''
  pageNo.value = 1
  load()
}

const load = async () => {
  loading.value = true
  try {
    const res = await listFunds({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      keyword: keyword.value,
      sortBy: sortBy.value,
      sortOrder: sortOrder.value || undefined,
      market: 'E',
      management: management.value,
      custodian: custodian.value,
      fundType: fundType.value,
      managementFeeMin: managementFeeMin.value,
      managementFeeMax: managementFeeMax.value,
      custodianFeeMin: custodianFeeMin.value,
      custodianFeeMax: custodianFeeMax.value
    })

    if (res.code === 200 && res.data) {
      const data = res.data as PageRes<FundDto>
      rows.value = data.items
      total.value = data.total
      return
    }

    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(load)

const onSortChange = (e: { prop: string; order: 'ascending' | 'descending' | null }) => {
  if (['listDate', 'managementFee', 'custodianFee'].includes(e.prop)) {
    sortBy.value = e.prop
    sortOrder.value = e.order === 'ascending' ? 'asc' : e.order === 'descending' ? 'desc' : ''
    load()
    return
  }

  if (e.prop === 'updatedAt') {
    const dir = e.order === 'ascending' ? 1 : e.order === 'descending' ? -1 : 0
    if (dir === 0) {
      load()
      return
    }
    rows.value = [...rows.value].sort((a, b) => {
      const av = a.updatedAt || ''
      const bv = b.updatedAt || ''
      if (av === bv) return 0
      return av > bv ? dir : -dir
    })
  }
}

const onView = (row: FundDto) => {
  router.push(`/etf/detail/${row.code}`)
}

const onEdit = (row: FundDto) => {
  editForm.value = {
    code: row.code,
    name: row.name,
    status: row.status,
    market: row.market,
    exchange: row.exchange
  }
  editorVisible.value = true
}

const submitEdit = async () => {
  editLoading.value = true
  try {
    const res = await updateFund(editForm.value.code, {
      name: editForm.value.name,
      status: editForm.value.status,
      market: editForm.value.market,
      exchange: editForm.value.exchange
    })
    if (res.code === 200) {
      ElMessage.success('更新成功')
      editorVisible.value = false
      load()
    } else {
      ElMessage.error(res.message || '更新失败')
    }
  } finally {
    editLoading.value = false
  }
}

const onDelete = async (row: FundDto) => {
  try {
    await ElMessageBox.confirm(`确认删除基金 ${row.code} 吗？`, '提示', { type: 'warning' })
    const res = await deleteFund(row.code)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      load()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch {
  }
}
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.page-header__content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.page-header__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.title {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.subtitle {
  font-size: 14px;
  line-height: 22px;
  color: #606266;
}

.search-card,
.table-card {
  border-radius: 16px;
}

.meta-item {
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid #ebeef5;
  background: linear-gradient(180deg, #ffffff 0%, #f7faff 100%);
}

.meta-item__label {
  display: block;
  font-size: 12px;
  line-height: 18px;
  color: #909399;
}

.meta-item__value {
  display: block;
  margin-top: 6px;
  font-size: 16px;
  font-weight: 600;
  line-height: 24px;
  color: #303133;
}

.search-header,
.table-toolbar,
.drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.section-subtitle {
  margin-top: 6px;
  font-size: 13px;
  line-height: 20px;
  color: #909399;
}

.search-meta,
.table-toolbar__meta,
.drawer-header__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  font-size: 13px;
  color: #909399;
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

.range-inputs {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 8px;
  width: 100%;
}

.range-separator {
  color: #909399;
  font-size: 12px;
}

.search-actions,
.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.search-actions {
  margin-top: 12px;
}

.full {
  width: 100%;
}

.primary-cell {
  font-size: 14px;
  font-weight: 600;
  line-height: 22px;
  color: #303133;
}

.secondary-cell {
  margin-top: 4px;
  font-size: 12px;
  line-height: 18px;
  color: #909399;
}

.cell-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pager {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.drawer-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.drawer-context {
  padding: 16px;
  border-radius: 12px;
  background: #f5f7fa;
}

.drawer-context__title {
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.drawer-context__meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.form-section {
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  background: #fafafa;
}

.form-section__title {
  margin-bottom: 16px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

@media (max-width: 768px) {
  .page-header,
  .search-header,
  .table-toolbar,
  .drawer-header {
    flex-direction: column;
  }

  .search-grid {
    grid-template-columns: 1fr;
  }

  .drawer-context__meta {
    grid-template-columns: 1fr;
  }

  .range-inputs {
    grid-template-columns: 1fr;
  }

  .search-actions,
  .pager,
  .drawer-footer {
    justify-content: stretch;
  }

  .search-actions :deep(.el-button),
  .drawer-footer :deep(.el-button) {
    flex: 1;
  }
}
</style>
