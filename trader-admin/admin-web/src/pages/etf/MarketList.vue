<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <div class="title">基金行情</div>
        <div class="subtitle">统一查看 ETF 基金日线行情、涨跌幅和成交额变化</div>
      </div>
      <div class="page-header__meta">
        <el-tag effect="plain" type="primary">行情数据</el-tag>
        <el-tag effect="plain" type="info">共 {{ total }} 条</el-tag>
      </div>
    </div>

    <el-card class="search-card">
      <div class="search-header">
        <div>
          <div class="section-title">筛选条件</div>
          <div class="section-subtitle">输入基金代码并选择时间区间时，将直接返回对应区间的完整行情序列</div>
        </div>
        <div class="search-meta">
          <span v-if="q.code">代码 {{ q.code }}</span>
          <span v-if="rangeText">区间 {{ rangeText }}</span>
        </div>
      </div>

      <el-form :model="q" class="search-form">
        <div class="search-grid">
          <el-form-item label="基金代码">
            <el-input v-model="q.code" placeholder="请输入基金代码" clearable @keyup.enter="load" />
          </el-form-item>
          <el-form-item label="日期范围">
            <el-date-picker
              v-model="range"
              type="daterange"
              value-format="YYYY-MM-DD"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
            />
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
          <div class="section-title">行情列表</div>
          <div class="section-subtitle">按统一信息层级展示价格、涨跌幅和成交额</div>
        </div>
        <div class="table-toolbar__meta">分页 {{ pageNo }} / {{ totalPage }}</div>
      </div>

      <el-table :data="rows" v-loading="loading" stripe class="full">
        <el-table-column label="基金信息" min-width="180">
          <template #default="{ row }">
            <div class="primary-cell">{{ row.code || '-' }}</div>
            <div class="secondary-cell">{{ formatDate(row.time) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="价格区间" min-width="180">
          <template #default="{ row }">
            <div class="primary-cell">开 {{ formatNumber(row.open) }} / 收 {{ formatNumber(row.close) }}</div>
            <div class="secondary-cell">高 {{ formatNumber(row.high) }} / 低 {{ formatNumber(row.low) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="涨跌表现" min-width="140">
          <template #default="{ row }">
            <div class="primary-cell" :class="valueClass(row.pctChg)">{{ formatPercent(row.pctChg) }}</div>
            <div class="secondary-cell">{{ pctText(row.pctChg) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="成交额" min-width="160">
          <template #default="{ row }">
            <div class="primary-cell">{{ formatAmount(row.amount) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="180">
          <template #default="{ row }">
            <div class="primary-cell">{{ formatDateTime(row.updatedAt) }}</div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
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
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { Search } from '@element-plus/icons-vue'
import { getFundMarket, type FundMarketDto, listFundMarkets, type PageRes } from '../../services/fund'

const q = ref({ code: '', startDate: '', endDate: '' })
const range = ref<[string, string]>()
const loading = ref(false)
const rows = ref<FundMarketDto[]>([])
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)

const totalPage = computed(() => {
  if (!total.value) return 1
  return Math.ceil(total.value / pageSize.value)
})

const rangeText = computed(() => {
  if (!range.value?.length) return ''
  return `${range.value[0]} ~ ${range.value[1]}`
})

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

const formatNumber = (value?: number) => {
  if (value === undefined || value === null) return '-'
  return Number(value).toFixed(4)
}

const formatPercent = (value?: number) => {
  if (value === undefined || value === null) return '-'
  return `${Number(value).toFixed(2)}%`
}

const formatAmount = (value?: number) => {
  if (value === undefined || value === null) return '-'
  return Number(value).toLocaleString('zh-CN')
}

const pctText = (value?: number) => {
  if (value === undefined || value === null) return '暂无涨跌幅'
  if (value > 0) return '上涨'
  if (value < 0) return '下跌'
  return '平盘'
}

const valueClass = (value?: number) => {
  if (value === undefined || value === null) return ''
  if (value > 0) return 'is-rise'
  if (value < 0) return 'is-fall'
  return ''
}

const resetSearch = () => {
  q.value.code = ''
  q.value.startDate = ''
  q.value.endDate = ''
  range.value = undefined
  pageNo.value = 1
  load()
}

const load = async () => {
  loading.value = true
  try {
    if (q.value.code && range.value) {
      q.value.code = q.value.code.trim()
      q.value.startDate = range.value[0]
      q.value.endDate = range.value[1]
      const res = await getFundMarket(q.value)
      rows.value = res.code === 200 ? (res.data || []) : []
      total.value = rows.value.length
    } else {
      const payload = { pageNo: pageNo.value, pageSize: pageSize.value }
      const res = await listFundMarkets(payload)
      if (res.code === 200 && res.data) {
        const data = res.data as PageRes<FundMarketDto>
        rows.value = data.items
        total.value = data.total
      } else {
        rows.value = []
        total.value = 0
      }
    }
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header,
.search-header,
.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.page-header__meta,
.search-meta,
.table-toolbar__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  font-size: 13px;
  color: #909399;
}

.title {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.subtitle {
  margin-top: 8px;
  font-size: 14px;
  line-height: 22px;
  color: #606266;
}

.search-card,
.table-card {
  border-radius: 16px;
}

.search-header,
.table-toolbar {
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

.is-rise {
  color: #f56c6c;
}

.is-fall {
  color: #67c23a;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .page-header,
  .search-header,
  .table-toolbar {
    flex-direction: column;
  }

  .search-grid {
    grid-template-columns: 1fr;
  }

  .search-actions,
  .pagination {
    justify-content: stretch;
  }

  .search-actions :deep(.el-button) {
    flex: 1;
  }
}
</style>
