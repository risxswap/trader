<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__content">
        <div class="title">相关统计</div>
        <div class="subtitle">统一查看标的之间的相关性、显著性与周期分布，快速进入详情分析趋势</div>
      </div>
      <div class="page-header__tags">
        <el-tag effect="plain" type="primary">总量 {{ total }}</el-tag>
        <el-tag effect="plain" type="success">强相关 {{ strongCorrelationCount }}</el-tag>
        <el-tag effect="plain" type="info">周期 {{ periodCount }}</el-tag>
      </div>
    </div>

    <el-card class="search-card">
      <div class="search-header">
        <div>
          <div class="section-title">筛选条件</div>
          <div class="section-subtitle">按标的组合、相关系数区间和周期快速定位目标统计</div>
        </div>
        <div class="search-meta">当前返回 {{ rows.length }} 条，累计 {{ total }} 条</div>
      </div>
      <el-form class="search-form" label-position="top">
        <div class="search-grid">
          <el-form-item label="标的">
            <el-input v-model="asset1" placeholder="输入标的代码" clearable @keyup.enter="load" />
          </el-form-item>
          <el-form-item label="相关标的">
            <el-input v-model="asset2" placeholder="输入相关标的代码" clearable @keyup.enter="load" />
          </el-form-item>
          <el-form-item label="时间周期">
            <el-input v-model="period" placeholder="例如 6M / 1Y" clearable @keyup.enter="load" />
          </el-form-item>
          <el-form-item label="相关系数区间">
            <div class="range-inputs">
              <el-input v-model="minCoefficient" type="number" step="0.1" placeholder="最小值" clearable @keyup.enter="load" />
              <span class="range-separator">至</span>
              <el-input v-model="maxCoefficient" type="number" step="0.1" placeholder="最大值" clearable @keyup.enter="load" />
            </div>
          </el-form-item>
        </div>
      </el-form>
      <div class="search-actions">
        <el-button type="primary" :icon="Search" :loading="loading" @click="load">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </el-card>

    <el-card class="table-card">
      <div class="table-toolbar">
        <div>
          <div class="section-title">相关统计列表</div>
          <div class="section-subtitle">突出标的组合、相关强度、显著性与时间信息，保持分析入口清晰</div>
        </div>
      </div>
      <el-table :data="rows" v-loading="loading" class="full" stripe>
        <el-table-column prop="id" label="ID" min-width="120" />
        <el-table-column label="标的组合" min-width="220">
          <template #default="{ row }">
            <div class="pair-cell">
              <div class="pair-main">
                <span class="asset-link" @click="openAssetDetail(row.asset1)">{{ row.asset1 }}</span>
                <span class="pair-arrow">↔</span>
                <span class="asset-link" @click="openAssetDetail(row.asset2)">{{ row.asset2 }}</span>
              </div>
              <div class="secondary-cell">点击标的代码可查看基础详情</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型信息" min-width="180">
          <template #default="{ row }">
            <div class="tag-group">
              <el-tag effect="plain">{{ row.asset1Type || '—' }}</el-tag>
              <el-tag effect="plain" type="success">{{ row.asset2Type || '—' }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="统计指标" min-width="220">
          <template #default="{ row }">
            <div class="primary-cell" :class="coefficientClass(row.coefficient)">{{ formatCoefficient(row.coefficient) }}</div>
            <div class="secondary-cell">P 值 {{ formatPValue(row.pValue) }} · 周期 {{ row.period || '—' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="时间信息" min-width="220">
          <template #default="{ row }">
            <div class="primary-cell">{{ row.updatedAt || '—' }}</div>
            <div class="secondary-cell">创建于 {{ row.createdAt || '—' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" min-width="120">
          <template #default="{ row }">
            <el-button type="primary" link @click="openCorrelationDetail(row.id)">查看详情</el-button>
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

    <el-dialog v-model="detailVisible" :title="detailTitle" width="720">
    <el-skeleton :loading="detailLoading" animated :rows="6">
      <template #default>
        <div v-if="fundDetail" class="detail-wrap">
          <div class="detail-hero">
            <div class="detail-hero-main">
              <div class="detail-hero-title">
                <span class="detail-symbol">{{ fundDetail.code || '—' }}</span>
                <span class="detail-name">{{ fundDetail.name || '—' }}</span>
              </div>
              <div class="detail-hero-sub">
                <el-tag size="small" effect="plain">{{ fundDetail.status || '—' }}</el-tag>
                <span class="detail-sub-text">{{ fundDetail.market || '—' }}</span>
                <span class="detail-sub-text">{{ fundDetail.exchange || '—' }}</span>
                <span class="detail-sub-text">{{ fundDetail.fundType || '—' }}</span>
              </div>
            </div>
          </div>

          <div class="detail-list">
            <div class="detail-row">
              <span class="detail-label">管理人：</span>
              <span class="detail-value">{{ fundDetail.management || '—' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">托管人：</span>
              <span class="detail-value">{{ fundDetail.custodian || '—' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">管理费：</span>
              <span class="detail-value">{{ fundDetail.managementFee !== undefined && fundDetail.managementFee !== null ? `${fundDetail.managementFee}%` : '—' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">托管费：</span>
              <span class="detail-value">{{ fundDetail.custodianFee !== undefined && fundDetail.custodianFee !== null ? `${fundDetail.custodianFee}%` : '—' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">上市时间：</span>
              <span class="detail-value">{{ fundDetail.listDate || '—' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">成立日期：</span>
              <span class="detail-value">{{ fundDetail.foundDate || '—' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">创建时间：</span>
              <span class="detail-value">{{ fundDetail.createdAt || '—' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">更新时间：</span>
              <span class="detail-value">{{ fundDetail.updatedAt || '—' }}</span>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无数据" :image-size="60" />
      </template>
    </el-skeleton>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { listCorrelations, type CorrelationDto } from '../../services/correlation'
import { getFundDetail, type FundDto } from '../../services/fund'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

const router = useRouter()

const loading = ref(false)
const rows = ref<CorrelationDto[]>([])
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)

const asset1 = ref('')
const asset2 = ref('')
const period = ref('')
const minCoefficient = ref<number>()
const maxCoefficient = ref<number>()

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailCode = ref('')
const fundDetail = ref<FundDto | null>(null)

const detailTitle = computed(() => {
  return detailCode.value ? `标的详情 - ${detailCode.value}` : '标的详情'
})

const strongCorrelationCount = computed(() => rows.value.filter((item) => Math.abs(Number(item.coefficient || 0)) >= 0.8).length)

const periodCount = computed(() => new Set(rows.value.map((item) => item.period).filter(Boolean)).size)

const openCorrelationDetail = (id: string) => {
  if (!id) return
  router.push(`/analysis/correlation/detail/${id}`)
}

const formatCoefficient = (value?: number) => {
  if (value === undefined || value === null) return '—'
  return Number(value).toFixed(4)
}

const formatPValue = (value?: number) => {
  if (value === undefined || value === null) return '—'
  return Number(value).toFixed(4)
}

const coefficientClass = (value?: number) => {
  const coefficient = Math.abs(Number(value || 0))
  if (coefficient >= 0.8) return 'text-strong'
  if (coefficient >= 0.5) return 'text-medium'
  return 'text-weak'
}

const handleReset = () => {
  asset1.value = ''
  asset2.value = ''
  period.value = ''
  minCoefficient.value = undefined
  maxCoefficient.value = undefined
  pageNo.value = 1
  load()
}

const openAssetDetail = async (code: string) => {
  if (!code) return
  detailVisible.value = true
  detailCode.value = code
  fundDetail.value = null
  detailLoading.value = true
  try {
    const res = await getFundDetail(code)
    if (res.code === 200 && res.data) {
      fundDetail.value = res.data as FundDto
    } else {
      ElMessage.error(res.message || '获取标的详情失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '获取标的详情失败')
  } finally {
    detailLoading.value = false
  }
}

const load = async () => {
  loading.value = true
  try {
    const res = await listCorrelations({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      asset1: asset1.value,
      asset2: asset2.value,
      period: period.value,
      minCoefficient: minCoefficient.value,
      maxCoefficient: maxCoefficient.value
    })
    rows.value = res.data.items
    total.value = res.data.total
  } finally {
    loading.value = false
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
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 24px 28px;
  border-radius: 20px;
  background: linear-gradient(135deg, #f7faff 0%, #ffffff 100%);
  border: 1px solid #ebeef5;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
}

.page-header__content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.page-header__tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.title {
  font-size: 26px;
  line-height: 1.2;
  font-weight: 700;
  color: #1f2937;
}

.subtitle {
  font-size: 14px;
  line-height: 1.7;
  color: #6b7280;
}

.search-card,
.table-card {
  border-radius: 18px;
}

.search-header,
.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.section-subtitle,
.search-meta {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.6;
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

.range-inputs {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 8px;
  width: 100%;
}

.range-separator {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
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

.pair-cell,
.tag-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pair-main {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.pair-arrow {
  color: #909399;
  font-size: 14px;
}

.primary-cell {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.secondary-cell {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}

.text-strong {
  color: #16a34a;
}

.text-medium {
  color: #2563eb;
}

.text-weak {
  color: #6b7280;
}

.pager {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.asset-link {
  cursor: pointer;
  color: #409eff;
  font-weight: 600;
}

.asset-link:hover {
  opacity: 0.85;
}

.mx-2 {
  margin: 0 0.5rem;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.detail-wrap {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-hero {
  padding: 14px 16px;
  background: linear-gradient(180deg, #f5f7fa 0%, #ffffff 100%);
  border-radius: 12px;
  border: 1px solid #ebeef5;
}

.detail-hero-title {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 10px;
}

.detail-symbol {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}

.detail-name {
  font-size: 14px;
  color: #606266;
}

.detail-hero-sub {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.detail-sub-text {
  font-size: 12px;
  color: #909399;
}

.detail-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 6px 2px;
}

.detail-row {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  line-height: 24px;
}

.detail-label {
  font-size: 12px;
  color: #909399;
  flex: 0 0 auto;
  letter-spacing: 0.2px;
}

.detail-value {
  font-size: 13px;
  color: #303133;
  text-align: left;
  word-break: break-all;
  flex: 1 1 auto;
}

@media (max-width: 992px) {
  .search-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
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

  .range-inputs {
    grid-template-columns: 1fr;
  }

  .search-actions,
  .pager {
    justify-content: stretch;
  }

  .search-actions :deep(.el-button) {
    width: 100%;
  }
}
</style>
