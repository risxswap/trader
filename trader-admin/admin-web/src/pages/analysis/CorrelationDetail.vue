<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <el-button text type="primary" class="back-button" @click="onBack">返回列表</el-button>
        <div class="page-title">
          <div class="page-title__main">相关统计详情</div>
          <div class="page-subtitle">查看相关性结果、显著性指标与净值对比走势，保持分析结论和图表信息同屏可读</div>
        </div>
      </div>
      <div class="page-header__tags">
        <el-tag v-if="detail?.id" effect="plain" type="info">ID {{ detail.id }}</el-tag>
        <el-tag effect="plain" :type="coefficientTagType(detail?.coefficient)">{{ coefficientLevelText(detail?.coefficient) }}</el-tag>
        <el-tag v-if="detail?.period" effect="plain" type="primary">{{ detail.period }}</el-tag>
      </div>
    </div>

    <el-skeleton :loading="loading" animated :rows="8">
      <template #default>
        <template v-if="detail">
          <div class="meta-strip">
            <div class="meta-item">
              <span class="meta-item__label">分析组合</span>
              <span class="meta-item__value">{{ detail.asset1 || '—' }} ↔ {{ detail.asset2 || '—' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-item__label">周期范围</span>
              <span class="meta-item__value">{{ detail.period || '—' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-item__label">结果强度</span>
              <span class="meta-item__value">{{ coefficientLevelText(detail.coefficient) }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-item__label">更新时间</span>
              <span class="meta-item__value">{{ detail.updatedAt || '—' }}</span>
            </div>
          </div>

          <div class="metric-grid">
            <el-card shadow="hover" class="metric-card">
              <div class="metric-card__label">相关系数</div>
              <div class="metric-card__value" :class="coefficientValueClass(detail.coefficient)">{{ formatMetric(detail.coefficient, 4) }}</div>
              <div class="metric-card__meta">{{ coefficientSummary(detail.coefficient) }}</div>
            </el-card>
            <el-card shadow="hover" class="metric-card">
              <div class="metric-card__label">P 值</div>
              <div class="metric-card__value">{{ formatMetric(detail.pValue, 4) }}</div>
              <div class="metric-card__meta">{{ significanceSummary(detail.pValue) }}</div>
            </el-card>
            <el-card shadow="hover" class="metric-card">
              <div class="metric-card__label">标的类型</div>
              <div class="metric-card__value metric-card__value--small">{{ detail.asset1Type || '—' }} / {{ detail.asset2Type || '—' }}</div>
              <div class="metric-card__meta">用于标识本次统计所覆盖的资产类型</div>
            </el-card>
            <el-card shadow="hover" class="metric-card">
              <div class="metric-card__label">净值区间</div>
              <div class="metric-card__value metric-card__value--small">{{ navDateRange }}</div>
              <div class="metric-card__meta">基于当前周期内基金净值序列的对比走势</div>
            </el-card>
          </div>

          <div class="content-grid">
            <el-card shadow="hover" class="info-card">
              <template #header>
                <div class="card-header">
                  <div>
                    <div class="section-title">统计信息</div>
                    <div class="section-subtitle">集中展示标的组合、显著性和时间字段，便于快速形成判断</div>
                  </div>
                </div>
              </template>
              <div class="info-section">
                <div class="info-section__title">标的组合</div>
                <el-descriptions :column="1" border label-width="110px">
                  <el-descriptions-item label="标的 1">{{ detail.asset1 || '—' }}</el-descriptions-item>
                  <el-descriptions-item label="标的 2">{{ detail.asset2 || '—' }}</el-descriptions-item>
                  <el-descriptions-item label="标的 1 类型">{{ detail.asset1Type || '—' }}</el-descriptions-item>
                  <el-descriptions-item label="标的 2 类型">{{ detail.asset2Type || '—' }}</el-descriptions-item>
                </el-descriptions>
              </div>
              <div class="info-section">
                <div class="info-section__title">分析结果</div>
                <el-descriptions :column="1" border label-width="110px">
                  <el-descriptions-item label="相关系数">{{ formatMetric(detail.coefficient, 4) }}</el-descriptions-item>
                  <el-descriptions-item label="P 值">{{ formatMetric(detail.pValue, 4) }}</el-descriptions-item>
                  <el-descriptions-item label="时间周期">{{ detail.period || '—' }}</el-descriptions-item>
                  <el-descriptions-item label="结论">{{ coefficientSummary(detail.coefficient) }}</el-descriptions-item>
                </el-descriptions>
              </div>
              <div class="info-section">
                <div class="info-section__title">时间信息</div>
                <el-descriptions :column="1" border label-width="110px">
                  <el-descriptions-item label="创建时间">{{ detail.createdAt || '—' }}</el-descriptions-item>
                  <el-descriptions-item label="更新时间">{{ detail.updatedAt || '—' }}</el-descriptions-item>
                </el-descriptions>
              </div>
            </el-card>

            <el-card shadow="hover" class="chart-card">
              <template #header>
                <div class="card-header">
                  <div>
                    <div class="section-title">基金净值对比</div>
                    <div class="section-subtitle">按统计周期拉取两只基金净值数据，观察走势同步程度和波动差异</div>
                  </div>
                  <div class="chart-header__meta">
                    <el-tag effect="plain" type="primary">{{ detail.asset1 || '—' }}</el-tag>
                    <el-tag effect="plain" type="success">{{ detail.asset2 || '—' }}</el-tag>
                  </div>
                </div>
              </template>
              <div class="chart-summary">
                <div class="chart-summary__item">
                  <span class="chart-summary__label">分析窗口</span>
                  <span class="chart-summary__value">{{ navDateRange }}</span>
                </div>
                <div class="chart-summary__item">
                  <span class="chart-summary__label">数据点</span>
                  <span class="chart-summary__value">{{ chartPointCount }}</span>
                </div>
                <div class="chart-summary__item">
                  <span class="chart-summary__label">显著性</span>
                  <span class="chart-summary__value">{{ significanceSummary(detail.pValue) }}</span>
                </div>
              </div>
              <el-skeleton :loading="navLoading" animated :rows="6">
                <template #default>
                  <div v-if="chartReady" class="chart-shell">
                    <VChart class="chart" :option="option" autoresize />
                  </div>
                  <el-empty v-else description="当前周期未查询到可用于对比的净值数据" :image-size="60" />
                </template>
              </el-skeleton>
            </el-card>
          </div>
        </template>
        <el-empty v-else description="暂无数据" :image-size="60" />
      </template>
    </el-skeleton>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, DataZoomComponent, LegendComponent, TitleComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import { getCorrelationDetail, type CorrelationDto } from '../../services/correlation'
import { listFundNavs, type FundNavDto } from '../../services/fund'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, DataZoomComponent, LegendComponent, TitleComponent])

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const navLoading = ref(false)
const detail = ref<CorrelationDto | null>(null)
const chartPointCount = ref(0)
const navStart = ref('')
const navEnd = ref('')

const option = ref<any>({
  title: { text: '' },
  tooltip: { trigger: 'axis' },
  legend: { data: [] },
  grid: { left: '3%', right: '3%', bottom: '10%', containLabel: true },
  xAxis: { type: 'category', data: [] },
  yAxis: { scale: true },
  dataZoom: [{ type: 'inside' }, { type: 'slider' }],
  series: []
})

const onBack = () => {
  router.back()
}

const chartReady = computed(() => chartPointCount.value > 0)

const navDateRange = computed(() => {
  if (!navStart.value || !navEnd.value) return '暂无区间'
  return `${navStart.value} ~ ${navEnd.value}`
})

const formatMetric = (value?: number, digits = 2) => {
  if (value === undefined || value === null) return '—'
  return Number(value).toFixed(digits)
}

const coefficientLevelText = (value?: number) => {
  const coefficient = Math.abs(Number(value || 0))
  if (coefficient >= 0.8) return '高相关'
  if (coefficient >= 0.5) return '中等相关'
  if (coefficient > 0) return '弱相关'
  return '待分析'
}

const coefficientTagType = (value?: number) => {
  const coefficient = Math.abs(Number(value || 0))
  if (coefficient >= 0.8) return 'success'
  if (coefficient >= 0.5) return 'primary'
  return 'info'
}

const coefficientValueClass = (value?: number) => {
  const coefficient = Math.abs(Number(value || 0))
  if (coefficient >= 0.8) return 'is-strong'
  if (coefficient >= 0.5) return 'is-medium'
  return 'is-weak'
}

const coefficientSummary = (value?: number) => {
  const coefficient = Number(value || 0)
  const absValue = Math.abs(coefficient)
  if (absValue >= 0.8) {
    return coefficient >= 0 ? '两只基金走势高度同向' : '两只基金走势高度反向'
  }
  if (absValue >= 0.5) {
    return coefficient >= 0 ? '两只基金存在较明显的同向波动' : '两只基金存在较明显的反向波动'
  }
  if (absValue > 0) {
    return coefficient >= 0 ? '两只基金仅表现为弱同向关系' : '两只基金仅表现为弱反向关系'
  }
  return '当前结果尚未形成明显相关结论'
}

const significanceSummary = (value?: number) => {
  if (value === undefined || value === null) return '显著性待分析'
  if (Number(value) <= 0.05) return '统计显著'
  return '统计显著性较弱'
}

const calcStartDate = (period?: string) => {
  const now = dayjs()
  const p = String(period || '').trim().toUpperCase()
  if (!p) return now.subtract(1, 'year')
  const last = p.slice(-1)
  const numPart = p.slice(0, -1)
  if (last === 'D' && Number(numPart)) return now.subtract(Number(numPart), 'day')
  if (last === 'M' && Number(numPart)) return now.subtract(Number(numPart), 'month')
  if (last === 'Y' && Number(numPart)) return now.subtract(Number(numPart), 'year')
  if (Number(p)) return now.subtract(Number(p), 'day')
  return now.subtract(1, 'year')
}

const buildSeries = (asset1: string, asset2: string, navs1: FundNavDto[], navs2: FundNavDto[]) => {
  const map1 = new Map<string, number>()
  const map2 = new Map<string, number>()

  for (const n of navs1) {
    const d = dayjs(n.time).format('YYYY-MM-DD')
    const v = (n.adjNav ?? n.unitNav) as number | undefined
    if (v !== undefined && v !== null) map1.set(d, v)
  }
  for (const n of navs2) {
    const d = dayjs(n.time).format('YYYY-MM-DD')
    const v = (n.adjNav ?? n.unitNav) as number | undefined
    if (v !== undefined && v !== null) map2.set(d, v)
  }

  const x = Array.from(new Set([...map1.keys(), ...map2.keys()])).sort()
  chartPointCount.value = x.length
  navStart.value = x[0] || ''
  navEnd.value = x[x.length - 1] || ''
  const s1 = x.map((d) => (map1.has(d) ? map1.get(d) : null))
  const s2 = x.map((d) => (map2.has(d) ? map2.get(d) : null))

  option.value = {
    tooltip: { trigger: 'axis' },
    legend: { top: 0, icon: 'roundRect', itemWidth: 12, data: [asset1, asset2] },
    grid: { left: '4%', right: '4%', top: 56, bottom: 72, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisLabel: { color: '#909399' },
      data: x
    },
    yAxis: {
      scale: true,
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#ebeef5' } },
      axisLabel: { color: '#909399' }
    },
    dataZoom: [{ type: 'inside' }, { type: 'slider', height: 22, bottom: 18 }],
    series: [
      { name: asset1, type: 'line', data: s1, smooth: true, symbol: 'none', connectNulls: true, lineStyle: { width: 3 }, emphasis: { focus: 'series' } },
      { name: asset2, type: 'line', data: s2, smooth: true, symbol: 'none', connectNulls: true, lineStyle: { width: 3 }, emphasis: { focus: 'series' } }
    ]
  }
}

const loadNav = async () => {
  if (!detail.value) return
  const start = calcStartDate(detail.value.period).format('YYYY-MM-DD')
  const end = dayjs().format('YYYY-MM-DD')
  chartPointCount.value = 0
  navStart.value = ''
  navEnd.value = ''
  navLoading.value = true
  try {
    const [r1, r2] = await Promise.all([
      listFundNavs({ pageNo: 1, pageSize: 5000, code: detail.value.asset1, startTime: start, endTime: end }),
      listFundNavs({ pageNo: 1, pageSize: 5000, code: detail.value.asset2, startTime: start, endTime: end })
    ])
    const navs1 = (r1.data?.items || []).slice().sort((a, b) => dayjs(a.time).valueOf() - dayjs(b.time).valueOf())
    const navs2 = (r2.data?.items || []).slice().sort((a, b) => dayjs(a.time).valueOf() - dayjs(b.time).valueOf())
    buildSeries(detail.value.asset1, detail.value.asset2, navs1, navs2)
  } catch (e: any) {
    ElMessage.error(e?.message || '加载净值数据失败')
  } finally {
    navLoading.value = false
  }
}

const load = async () => {
  const id = String(route.params.id || '')
  if (!id) return
  loading.value = true
  try {
    const res = await getCorrelationDetail(id)
    if (res.code === 200 && res.data) {
      detail.value = res.data
    } else {
      ElMessage.error(res.message || '获取相关统计详情失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '获取相关统计详情失败')
  } finally {
    loading.value = false
  }
}

watch(
  () => detail.value?.id,
  (v) => {
    if (v) loadNav()
  }
)

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
  align-items: flex-start;
  gap: 16px;
  padding: 24px 28px;
  border-radius: 20px;
  background: linear-gradient(135deg, #f7faff 0%, #ffffff 100%);
  border: 1px solid #ebeef5;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
}

.page-header__main {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.page-header__tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.back-button {
  align-self: flex-start;
  padding-left: 0;
}

.page-title {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.page-title__main {
  font-size: 26px;
  line-height: 1.2;
  font-weight: 700;
  color: #1f2937;
}

.page-subtitle {
  font-size: 14px;
  line-height: 1.7;
  color: #6b7280;
}

.meta-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 18px 20px;
  background: #fff;
  border-radius: 16px;
  border: 1px solid #ebeef5;
}

.meta-item__label {
  font-size: 12px;
  color: #909399;
}

.meta-item__value {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  line-height: 1.6;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.metric-card {
  border-radius: 16px;
}

.metric-card__label {
  font-size: 13px;
  color: #909399;
}

.metric-card__value {
  margin-top: 12px;
  font-size: 30px;
  line-height: 1.2;
  font-weight: 700;
  color: #1f2937;
}

.metric-card__value--small {
  font-size: 18px;
  line-height: 1.5;
}

.metric-card__meta {
  margin-top: 10px;
  font-size: 13px;
  line-height: 1.7;
  color: #909399;
}

.is-strong {
  color: #16a34a;
}

.is-medium {
  color: #2563eb;
}

.is-weak {
  color: #6b7280;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(320px, 420px) minmax(0, 1fr);
  gap: 16px;
}

.info-card,
.chart-card {
  border-radius: 18px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.section-subtitle {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.6;
  color: #909399;
}

.info-section + .info-section {
  margin-top: 18px;
}

.info-section__title {
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.chart-header__meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.chart-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.chart-summary__item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px 16px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #ebeef5;
}

.chart-summary__label {
  font-size: 12px;
  color: #909399;
}

.chart-summary__value {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.chart-shell {
  padding: 8px 0 0;
}

.chart-card .chart {
  height: 460px;
  width: 100%;
}

@media (max-width: 1200px) {
  .metric-grid,
  .meta-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .page-header,
  .card-header {
    flex-direction: column;
  }

  .metric-grid,
  .meta-strip,
  .chart-summary {
    grid-template-columns: 1fr;
  }

  .page-header {
    padding: 20px;
  }
}
</style>
