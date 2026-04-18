<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__content">
        <div class="page-title-row">
          <el-button text @click="onBack">返回</el-button>
          <div>
            <div class="title">基金详情</div>
            <div class="subtitle">统一查看公募基金基础档案、交易区间和净值走势</div>
          </div>
        </div>
        <div v-if="fund" class="header-tags">
          <el-tag effect="plain" type="primary">{{ fund.code }}</el-tag>
          <el-tag :type="statusTagType(fund.status)">{{ statusText(fund.status) }}</el-tag>
          <el-tag effect="plain" type="info">{{ fund.exchange || fund.market || '-' }}</el-tag>
        </div>
      </div>
    </div>

    <el-card class="search-card">
      <div class="search-header">
        <div>
          <div class="section-title">查询条件</div>
          <div class="section-subtitle">按基金代码与净值区间加载基础信息和净值数据</div>
        </div>
        <div class="search-meta">
          <span v-if="code">代码 {{ code }}</span>
          <span v-if="rangeText">区间 {{ rangeText }}</span>
        </div>
      </div>

      <el-form class="search-form">
        <div class="search-grid">
          <el-form-item label="基金代码">
            <el-select
              v-model="code"
              filterable
              remote
              :remote-method="searchFunds"
              :loading="searchLoading"
              placeholder="请输入代码或名称搜索"
            >
              <el-option
                v-for="opt in fundOptions"
                :key="opt.code"
                :label="`${opt.code} ${opt.name || ''}`"
                :value="opt.code"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="交易日期">
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
        <el-button type="primary" :loading="loading" :icon="Search" @click="loadWithFilters">查询</el-button>
        <el-button @click="resetFilters">重置区间</el-button>
      </div>
    </el-card>

    <div class="meta-strip">
      <div class="meta-item">
        <span class="meta-item__label">当前基金</span>
        <span class="meta-item__value">{{ fund?.name || code || '未选择' }}</span>
      </div>
      <div class="meta-item">
        <span class="meta-item__label">查询区间</span>
        <span class="meta-item__value">{{ rangeText || '最近 90 天' }}</span>
      </div>
      <div class="meta-item">
        <span class="meta-item__label">图表点数</span>
        <span class="meta-item__value">{{ chartPointCount }}</span>
      </div>
      <div class="meta-item">
        <span class="meta-item__label">已加载范围</span>
        <span class="meta-item__value">{{ loadedRangeText }}</span>
      </div>
    </div>

    <div class="content-grid">
      <el-card class="info-card">
        <div class="card-toolbar">
          <div>
            <div class="section-title">基础信息</div>
            <div class="section-subtitle">展示基金代码、机构、费率和市场归属信息</div>
          </div>
          <div class="card-toolbar__meta" v-if="fund?.updatedAt">更新于 {{ formatDateTime(fund.updatedAt) }}</div>
        </div>

        <div v-if="fund" class="summary-grid">
          <div class="summary-card">
            <div class="summary-card__label">基金代码</div>
            <div class="summary-card__value">{{ fund.code }}</div>
            <div class="summary-card__desc">{{ fund.name || '未配置名称' }}</div>
          </div>
          <div class="summary-card">
            <div class="summary-card__label">当前状态</div>
            <div class="summary-card__value">{{ statusText(fund.status) }}</div>
            <div class="summary-card__desc">{{ fund.exchange || marketText(fund.market) }}</div>
          </div>
          <div class="summary-card">
            <div class="summary-card__label">费率概览</div>
            <div class="summary-card__value">{{ formatPercent(fund.managementFee) }}</div>
            <div class="summary-card__desc">托管费 {{ formatPercent(fund.custodianFee) }}</div>
          </div>
          <div class="summary-card">
            <div class="summary-card__label">上市时间</div>
            <div class="summary-card__value">{{ formatDate(fund.listDate) }}</div>
            <div class="summary-card__desc">成立 {{ formatDate(fund.foundDate) }}</div>
          </div>
        </div>

        <div class="info-sections">
          <div class="info-section">
            <div class="info-section__title">基金档案</div>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="代码">{{ fund?.code || '—' }}</el-descriptions-item>
              <el-descriptions-item label="名称">{{ fund?.name || '—' }}</el-descriptions-item>
              <el-descriptions-item label="状态">{{ statusText(fund?.status) }}</el-descriptions-item>
              <el-descriptions-item label="投资类型">{{ fund?.fundType || '—' }}</el-descriptions-item>
            </el-descriptions>
          </div>

          <div class="info-section">
            <div class="info-section__title">机构与费率</div>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="管理人">{{ fund?.management || '—' }}</el-descriptions-item>
              <el-descriptions-item label="托管人">{{ fund?.custodian || '—' }}</el-descriptions-item>
              <el-descriptions-item label="管理费">{{ formatPercent(fund?.managementFee) }}</el-descriptions-item>
              <el-descriptions-item label="托管费">{{ formatPercent(fund?.custodianFee) }}</el-descriptions-item>
            </el-descriptions>
          </div>

          <div class="info-section">
            <div class="info-section__title">市场时间</div>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="市场">{{ marketText(fund?.market) }}</el-descriptions-item>
              <el-descriptions-item label="交易所">{{ fund?.exchange || '—' }}</el-descriptions-item>
              <el-descriptions-item label="上市时间">{{ formatDate(fund?.listDate) }}</el-descriptions-item>
              <el-descriptions-item label="成立日期">{{ formatDate(fund?.foundDate) }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ formatDateTime(fund?.createdAt) }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </div>
      </el-card>

      <el-card class="chart-card">
        <div class="card-toolbar">
          <div>
            <div class="section-title">净值走势</div>
            <div class="section-subtitle">按当前查询区间展示基金净值变化，缩放到边缘时自动加载更多数据</div>
          </div>
          <div class="card-toolbar__meta">
            <span>当前 {{ chartPointCount }} 点</span>
            <span v-if="loadedStart">起始 {{ loadedStart }}</span>
            <span v-if="loadedEnd">结束 {{ loadedEnd }}</span>
          </div>
        </div>
        <div v-if="chartPointCount" class="chart-shell">
          <VChart ref="chartRef" class="chart" :option="option" autoresize />
        </div>
        <div v-else class="chart-empty">
          <el-empty description="当前区间暂无净值数据" :image-size="60" />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getFundDetail, listFunds, listFundNavs, type FundNavDto, type FundDto, type PageRes } from '../../services/fund'
import { ElMessage } from 'element-plus'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, DataZoomComponent, LegendComponent, TitleComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import dayjs from 'dayjs'
import { Search } from '@element-plus/icons-vue'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, DataZoomComponent, LegendComponent, TitleComponent])

const route = useRoute()
const router = useRouter()
const code = ref<string>(String(route.params.code || ''))

const onBack = () => {
  router.back()
}

const fund = ref<FundDto | null>(null)
const range = ref<[string, string]>()
const fundOptions = ref<FundDto[]>([])
const searchLoading = ref(false)
const loading = ref(false)
const chartRef = ref<any>()
const loadedStart = ref<string>('')
const loadedEnd = ref<string>('')
const loadingMoreLeft = ref(false)
const loadingMoreRight = ref(false)
const rangeText = computed(() => {
  if (!range.value?.length) return ''
  return `${range.value[0]} ~ ${range.value[1]}`
})
const chartPointCount = computed(() => {
  const data = option.value?.series?.[0]?.data
  return Array.isArray(data) ? data.length : 0
})
const loadedRangeText = computed(() => {
  if (loadedStart.value && loadedEnd.value) return `${loadedStart.value} ~ ${loadedEnd.value}`
  return '待加载'
})
const option = ref<any>({
  title: { text: '' },
  tooltip: { trigger: 'axis' },
  legend: { data: ['单位净值', '累计净值', '复权净值'] },
  grid: { left: '3%', right: '3%', bottom: '8%', containLabel: true },
  xAxis: { type: 'category', data: [] },
  yAxis: { scale: true },
  dataZoom: [{ type: 'inside' }, { type: 'slider' }],
  series: [
    { name: '单位净值', type: 'line', data: [], smooth: true, symbol: 'none' },
    { name: '累计净值', type: 'line', data: [], smooth: true, symbol: 'none' },
    { name: '复权净值', type: 'line', data: [], smooth: true, symbol: 'none' }
  ]
})

const formatDate = (value?: string) => {
  if (!value) return '—'
  const formatted = dayjs(value)
  return formatted.isValid() ? formatted.format('YYYY-MM-DD') : value
}

const formatDateTime = (value?: string) => {
  if (!value) return '—'
  const formatted = dayjs(value)
  return formatted.isValid() ? formatted.format('YYYY-MM-DD HH:mm:ss') : value
}

const formatPercent = (value?: number) => {
  if (value === undefined || value === null) return '—'
  return `${Number(value).toFixed(4)}%`
}

const statusText = (value?: string) => {
  if (value === 'L') return '已上市'
  if (value === 'I') return '发行中'
  if (value === 'D') return '已摘牌'
  return value || '—'
}

const statusTagType = (value?: string) => {
  if (value === 'L') return 'success'
  if (value === 'I') return 'warning'
  if (value === 'D') return 'info'
  return 'info'
}

const marketText = (value?: string) => {
  if (value === 'O') return '公募基金市场'
  return value || '—'
}

const searchFunds = async (kw: string) => {
  searchLoading.value = true
  try {
    const res = await listFunds({ pageNo: 1, pageSize: 10, keyword: kw, market: 'O' })
    if (res.code === 200 && res.data) {
      const data = res.data as PageRes<FundDto>
      fundOptions.value = data.items
    } else {
      fundOptions.value = []
    }
  } finally {
    searchLoading.value = false
  }
}

const loadWithFilters = async () => {
  if (!code.value || !range.value) {
    ElMessage.error('缺少基金代码')
    return
  }
  loading.value = true
  try {
    const dr = await getFundDetail(code.value)
    if (dr.code === 200 && dr.data) {
      fund.value = dr.data as FundDto
      option.value.title.text = `${fund.value.code} ${fund.value.name || ''}`.trim()
    }
    const payload = {
      pageNo: 1,
      pageSize: 1000,
      code: code.value,
      startTime: range.value[0],
      endTime: range.value[1]
    }
    const mr = await listFundNavs(payload)
    if (mr.code === 200 && mr.data) {
      const data = mr.data.items as FundNavDto[]
      data.sort((a, b) => new Date(a.time).getTime() - new Date(b.time).getTime())
      option.value.xAxis.data = data.map(item => item.time.substring(0, 10))
      option.value.series[0].data = data.map(item => item.unitNav)
      option.value.series[1].data = data.map(item => item.accumNav)
      option.value.series[2].data = data.map(item => item.adjNav)
      loadedStart.value = payload.startTime
      loadedEnd.value = payload.endTime
    } else {
      option.value.xAxis.data = []
      option.value.series[0].data = []
      option.value.series[1].data = []
      option.value.series[2].data = []
    }
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  const end = new Date()
  const start = new Date(end.getTime() - 90 * 24 * 3600 * 1000)
  range.value = [fmt(start), fmt(end)]
}

const fmt = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
const parse = (s: string) => {
  const parts = s.split('-').map(Number)
  return new Date(parts[0], parts[1] - 1, parts[2])
}
const addDaysStr = (s: string, n: number) => {
  const d = parse(s)
  d.setDate(d.getDate() + n)
  return fmt(d)
}

const loadMoreLeft = async () => {
  if (loadingMoreLeft.value || !code.value || !loadedStart.value) return
  loadingMoreLeft.value = true
  try {
    const newStart = addDaysStr(loadedStart.value, -90)
    const chunkEnd = addDaysStr(loadedStart.value, -1)
    const payload = {
      pageNo: 1,
      pageSize: 1000,
      code: code.value,
      startTime: newStart,
      endTime: chunkEnd
    }
    const mr = await listFundNavs(payload)
    if (mr.code === 200 && mr.data) {
      const data = (mr.data.items as FundNavDto[]).filter(Boolean)
      if (data.length) {
        data.sort((a, b) => new Date(a.time).getTime() - new Date(b.time).getTime())
        const existing = new Set<string>(option.value.xAxis.data as string[])
        const prepend = data.filter(item => !existing.has(item.time.substring(0, 10)))
        if (prepend.length) {
          option.value.xAxis.data = [...prepend.map(item => item.time.substring(0, 10)), ...(option.value.xAxis.data as string[])]
          option.value.series[0].data = [...prepend.map(item => item.unitNav), ...(option.value.series[0].data as any[])]
          option.value.series[1].data = [...prepend.map(item => item.accumNav), ...(option.value.series[1].data as any[])]
          option.value.series[2].data = [...prepend.map(item => item.adjNav), ...(option.value.series[2].data as any[])]
          loadedStart.value = newStart
        }
      }
    }
  } finally {
    loadingMoreLeft.value = false
  }
}

const loadMoreRight = async () => {
  if (loadingMoreRight.value || !code.value || !loadedEnd.value) return
  loadingMoreRight.value = true
  try {
    const newStart = addDaysStr(loadedEnd.value, 1)
    const newEnd = addDaysStr(loadedEnd.value, 90)
    const payload = {
      pageNo: 1,
      pageSize: 1000,
      code: code.value,
      startTime: newStart,
      endTime: newEnd
    }
    const mr = await listFundNavs(payload)
    if (mr.code === 200 && mr.data) {
      const data = (mr.data.items as FundNavDto[]).filter(Boolean)
      if (data.length) {
        data.sort((a, b) => new Date(a.time).getTime() - new Date(b.time).getTime())
        const existing = new Set<string>(option.value.xAxis.data as string[])
        const append = data.filter(item => !existing.has(item.time.substring(0, 10)))
        if (append.length) {
          option.value.xAxis.data = [...(option.value.xAxis.data as string[]), ...append.map(item => item.time.substring(0, 10))]
          option.value.series[0].data = [...(option.value.series[0].data as any[]), ...append.map(item => item.unitNav)]
          option.value.series[1].data = [...(option.value.series[1].data as any[]), ...append.map(item => item.accumNav)]
          option.value.series[2].data = [...(option.value.series[2].data as any[]), ...append.map(item => item.adjNav)]
          loadedEnd.value = newEnd
        }
      }
    }
  } finally {
    loadingMoreRight.value = false
  }
}

const getChart = () => chartRef.value?.chart || chartRef.value?.getEchartsInstance?.()
const onDataZoom = () => {
  const chart = getChart()
  if (!chart) return
  const opt = chart.getOption()
  const x = (opt.xAxis as any[])[0].data as string[]
  const dz = Array.isArray(opt.dataZoom) ? (opt.dataZoom as any[])[0] : (opt.dataZoom as any)
  const start = Number(dz.start || 0)
  const end = Number(dz.end || 100)
  const len = x.length
  const iStart = Math.floor((start / 100) * (len - 1))
  const iEnd = Math.floor((end / 100) * (len - 1))
  if (iStart < 3) loadMoreLeft()
  if (iEnd > len - 4) loadMoreRight()
}

const init = async () => {
  resetFilters()
  if (code.value) {
    await loadWithFilters()
  }
}

onMounted(async () => {
  await init()
  await nextTick()
  const chart = getChart()
  if (chart) {
    chart.on('dataZoom', onDataZoom)
  }
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
  align-items: flex-start;
  gap: 16px;
}

.page-header__content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.page-title-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.title {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.subtitle {
  margin-top: 6px;
  font-size: 14px;
  line-height: 22px;
  color: #606266;
}

.header-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.search-card,
.info-card,
.chart-card {
  border-radius: 16px;
}

.meta-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
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
.card-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
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
.card-toolbar__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  font-size: 13px;
  color: #909399;
}

.search-form {
  margin-bottom: 20px;
}

.search-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.search-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(320px, 420px) minmax(0, 1fr);
  gap: 16px;
}

.info-sections {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}

.summary-card {
  padding: 16px;
  border-radius: 14px;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
}

.summary-card__label {
  font-size: 12px;
  line-height: 18px;
  color: #909399;
}

.summary-card__value {
  margin-top: 8px;
  font-size: 18px;
  font-weight: 700;
  line-height: 28px;
  color: #303133;
}

.summary-card__desc {
  margin-top: 6px;
  font-size: 12px;
  line-height: 18px;
  color: #909399;
}

.info-section__title {
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.chart {
  height: 560px;
}

.chart-shell {
  min-height: 560px;
}

.chart-empty {
  min-height: 360px;
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 1024px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .page-title-row,
  .search-header,
  .card-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .search-grid {
    grid-template-columns: 1fr;
  }

  .meta-strip,
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .search-actions {
    justify-content: stretch;
  }

  .search-actions :deep(.el-button) {
    flex: 1;
  }
}
</style>
