<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <el-button text type="primary" class="back-button" @click="onBack">返回列表</el-button>
        <div class="page-title">
          <div class="page-title__main">投资详情</div>
          <div class="page-subtitle">查看投资计划、策略配置、日志与趋势变化</div>
        </div>
      </div>
      <div class="page-header__tags">
        <el-tag v-if="info?.name" effect="plain">{{ info.name }}</el-tag>
        <el-tag v-if="info?.status" size="small" :type="statusTagType(info.status)">{{ statusText(info.status) }}</el-tag>
      </div>
    </div>

    <el-skeleton :loading="loading" animated :rows="6">
      <template v-if="info">
        <div class="meta-strip">
          <div class="meta-item">
            <span class="meta-item__label">分组</span>
            <span class="meta-item__value">{{ info.groupName || '-' }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-item__label">策略</span>
            <span class="meta-item__value">{{ info.strategyInfo?.name || info.strategyInfo?.className || '-' }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-item__label">预算</span>
            <span class="meta-item__value">{{ formatCurrency(info.budget) }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-item__label">执行器</span>
            <span class="meta-item__value">{{ info.executorId || '-' }}</span>
          </div>
        </div>

        <div class="content-shell">
          <el-row :gutter="20" class="mb-4">
            <el-col :xs="24" :sm="12" :md="6" v-for="(metric, index) in detailMetrics" :key="index" class="mb-xs-4">
              <el-card shadow="hover" class="metric-card">
                <div class="metric-content">
                  <div class="icon-wrapper" :class="metric.colorClass">
                    <el-icon><component :is="metric.icon" /></el-icon>
                  </div>
                  <div class="metric-info">
                    <div class="label">{{ metric.label }}</div>
                    <div class="value" :class="metric.valueClass">{{ metric.value }}</div>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>

          <div class="main-layout">
            <div class="main-left">
              <el-card shadow="hover" class="table-card">
                <template #header>
                  <div class="card-header">
                    <div>
                      <span class="section-title">交易数据</span>
                      <div class="section-subtitle">最近持仓和投资日志保持统一浏览入口</div>
                    </div>
                  </div>
                </template>
                <el-tabs v-model="activeDataTab">
                  <el-tab-pane label="最近持仓" name="position">
                    <div class="position-tab-panel">
                      <div class="position-tab-panel__header">
                        <div>
                          <div class="section-title">最近持仓</div>
                          <div class="section-subtitle">在详情页内直接查看并维护当前投资的持仓结构</div>
                        </div>
                        <div class="position-tab-panel__meta">
                          <el-tag v-if="info?.name" effect="plain" type="primary">{{ info.name }}</el-tag>
                          <el-tag effect="plain" type="info">投资ID {{ investmentId }}</el-tag>
                        </div>
                      </div>
                      <position-list :investment-id="investmentId" />
                    </div>
                  </el-tab-pane>
                  <el-tab-pane label="投资日志" name="log">
                    <div class="log-tab-panel">
                      <div class="log-tab-panel__header">
                        <div>
                          <div class="section-title">投资日志</div>
                          <div class="section-subtitle">按时间查看当前投资的资产、现金、收益与通知状态</div>
                        </div>
                        <div class="log-tab-panel__meta">
                          <el-tag v-if="info?.name" effect="plain" type="primary">{{ info.name }}</el-tag>
                          <el-tag effect="plain" type="info">日志 {{ totalLogs }} 条</el-tag>
                        </div>
                      </div>
                      <el-table :data="logRows" v-loading="logLoading" class="full" stripe>
                        <el-table-column prop="recordDate" label="记录时间" min-width="180">
                          <template #default="{ row }">
                            <div class="primary-cell">{{ formatDateValue(row.recordDate) }}</div>
                            <div class="secondary-cell">{{ formatTimeValue(row.recordDate) }}</div>
                          </template>
                        </el-table-column>
                        <el-table-column prop="type" label="类型" width="110">
                          <template #default="{ row }">
                            <el-tag :type="logTypeTagType(row.type)">{{ logTypeText(row.type) }}</el-tag>
                          </template>
                        </el-table-column>
                        <el-table-column prop="asset" label="资产" min-width="140" align="right">
                          <template #default="{ row }">
                            <div class="primary-cell">{{ formatCurrency(row.asset) }}</div>
                            <div class="secondary-cell">总资产</div>
                          </template>
                        </el-table-column>
                        <el-table-column prop="cash" label="现金" min-width="140" align="right">
                          <template #default="{ row }">
                            <div class="primary-cell">{{ formatCurrency(row.cash) }}</div>
                            <div class="secondary-cell">可用现金</div>
                          </template>
                        </el-table-column>
                        <el-table-column prop="profit" label="收益" min-width="140" align="right">
                          <template #default="{ row }">
                            <div class="primary-cell" :class="getChangeClass(row.profit)">{{ formatCurrency(row.profit) }}</div>
                            <div class="secondary-cell">累计盈亏</div>
                          </template>
                        </el-table-column>
                        <el-table-column prop="notified" label="通知" width="110" align="center">
                          <template #default="{ row }">
                            <el-tag size="small" :type="row.notified === 1 ? 'success' : 'info'">{{ row.notified === 1 ? '已通知' : '未通知' }}</el-tag>
                          </template>
                        </el-table-column>
                        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip>
                          <template #default="{ row }">
                            <div class="primary-cell">{{ row.remark || '-' }}</div>
                            <div class="secondary-cell">记录说明</div>
                          </template>
                        </el-table-column>
                      </el-table>
                      <div class="log-pager">
                        <el-pagination
                          v-model:current-page="logPageNo"
                          v-model:page-size="logPageSize"
                          :page-sizes="[10, 20, 50]"
                          :total="totalLogs"
                          layout="total, sizes, prev, pager, next, jumper"
                          @size-change="onLogPageSizeChange"
                          @current-change="onLogPageChange"
                        />
                      </div>
                    </div>
                  </el-tab-pane>
                </el-tabs>
              </el-card>

              <el-card shadow="hover" class="chart-card">
                <template #header>
                  <div class="card-header">
                    <div class="title-wrapper">
                      <span class="section-title">趋势图表</span>
                      <span class="section-subtitle">净值与持仓变化</span>
                    </div>
                  </div>
                </template>
                <el-tabs v-model="activeChart" class="chart-tabs">
                  <el-tab-pane label="净值变化" name="net">
                    <div class="chart-tab-panel">
                      <div class="chart-tab-panel__header">
                        <div>
                          <div class="section-title">净值变化</div>
                          <div class="section-subtitle">按时间查看资产与现金的联动趋势</div>
                        </div>
                        <div class="chart-tab-panel__meta">
                          <el-tag v-if="info?.name" effect="plain" type="primary">{{ info.name }}</el-tag>
                          <el-tag effect="plain" type="info">图表 {{ chartLogs.length }} 点</el-tag>
                        </div>
                      </div>
                      <div v-if="chartLogs.length" class="chart-container main-chart">
                        <v-chart class="chart" :option="netValueOption" autoresize />
                      </div>
                      <div v-else class="chart-empty">
                        <el-empty description="暂无净值数据" :image-size="56" />
                      </div>
                    </div>
                  </el-tab-pane>
                  <el-tab-pane label="持仓变化" name="position">
                    <div class="chart-tab-panel">
                      <div class="chart-tab-panel__header">
                        <div>
                          <div class="section-title">持仓变化</div>
                          <div class="section-subtitle">观察持仓市值与收益波动趋势</div>
                        </div>
                        <div class="chart-tab-panel__meta">
                          <el-tag v-if="info?.name" effect="plain" type="primary">{{ info.name }}</el-tag>
                          <el-tag effect="plain" type="info">图表 {{ chartLogs.length }} 点</el-tag>
                        </div>
                      </div>
                      <div v-if="chartLogs.length" class="chart-container main-chart">
                        <v-chart class="chart" :option="positionOption" autoresize />
                      </div>
                      <div v-else class="chart-empty">
                        <el-empty description="暂无持仓数据" :image-size="56" />
                      </div>
                    </div>
                  </el-tab-pane>
                </el-tabs>
              </el-card>
            </div>

            <div class="main-right">
              <el-card shadow="hover" class="list-card side-panel">
                <template #header>
                  <div class="card-header">
                    <div>
                      <span class="section-title">投资信息</span>
                      <div class="section-subtitle">基础属性与策略配置拆分展示</div>
                    </div>
                  </div>
                </template>
                <el-tabs v-model="activeInfoTab">
                  <el-tab-pane label="基础信息" name="basic">
                    <div class="info-tab-panel">
                      <div class="info-tab-panel__header">
                        <div>
                          <div class="section-title">基础信息</div>
                          <div class="section-subtitle">查看当前投资计划的身份标识、执行配置与时间信息</div>
                        </div>
                        <div class="info-tab-panel__meta">
                          <el-tag v-if="info?.status" size="small" :type="statusTagType(info.status)">{{ statusText(info.status) }}</el-tag>
                          <el-tag effect="plain" type="info">ID {{ info.id }}</el-tag>
                        </div>
                      </div>
                      <div class="info-section">
                        <div class="info-section__title">基础属性</div>
                        <el-descriptions :column="1" border label-width="120px">
                          <el-descriptions-item label="ID">{{ info.id }}</el-descriptions-item>
                          <el-descriptions-item label="名称">{{ info.name || '-' }}</el-descriptions-item>
                          <el-descriptions-item label="分组">{{ info.groupName || '-' }}</el-descriptions-item>
                          <el-descriptions-item label="状态">{{ statusText(info.status) }}</el-descriptions-item>
                          <el-descriptions-item label="券商ID">{{ info.brokerId ?? '-' }}</el-descriptions-item>
                          <el-descriptions-item label="预算">{{ formatCurrency(info.budget) }}</el-descriptions-item>
                          <el-descriptions-item label="目标类型">{{ info.targetType || '-' }}</el-descriptions-item>
                          <el-descriptions-item label="投资类型">{{ info.investType || '-' }}</el-descriptions-item>
                        </el-descriptions>
                      </div>
                      <div class="info-section">
                        <div class="info-section__title">执行信息</div>
                        <el-descriptions :column="1" border label-width="120px">
                          <el-descriptions-item label="执行器">{{ info.executorId || '-' }}</el-descriptions-item>
                          <el-descriptions-item label="执行表达式">{{ info.cron || '-' }}</el-descriptions-item>
                          <el-descriptions-item label="创建时间">{{ formatDate(info.createdAt) }}</el-descriptions-item>
                          <el-descriptions-item label="最近更新">{{ formatDate(info.updatedAt) }}</el-descriptions-item>
                        </el-descriptions>
                      </div>
                    </div>
                  </el-tab-pane>
                  <el-tab-pane label="策略与目标" name="strategy">
                    <div class="info-tab-panel">
                      <div class="info-tab-panel__header">
                        <div>
                          <div class="section-title">策略与目标</div>
                          <div class="section-subtitle">集中查看策略来源、目标范围与参数配置</div>
                        </div>
                        <div class="info-tab-panel__meta">
                          <el-tag effect="plain" type="primary">{{ info.strategyInfo?.name || '未配置策略' }}</el-tag>
                        </div>
                      </div>
                      <div class="info-section">
                        <div class="info-section__title">策略信息</div>
                        <el-descriptions :column="1" border label-width="120px">
                          <el-descriptions-item label="策略名称">{{ info.strategyInfo?.name || '-' }}</el-descriptions-item>
                          <el-descriptions-item label="策略类名">{{ info.strategyInfo?.className || '-' }}</el-descriptions-item>
                          <el-descriptions-item label="目标列表">{{ targetsText }}</el-descriptions-item>
                        </el-descriptions>
                      </div>
                      <div class="info-section">
                        <div class="info-section__title">策略参数</div>
                        <div class="strategy-config-shell">
                          <pre class="strategy-config">{{ strategyConfigText }}</pre>
                        </div>
                      </div>
                    </div>
                  </el-tab-pane>
                </el-tabs>
              </el-card>
            </div>
          </div>
        </div>
      </template>
    </el-skeleton>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { Wallet, TrendCharts, Goods, Warning } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import PositionList from './PositionList.vue'
import { getInvestment, type InvestmentDto, listInvestmentLogs, type InvestmentLogDto } from '../../services/basic'
import dayjs from 'dayjs'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, LegendComponent])

const route = useRoute()
const router = useRouter()
const investmentId = Number(route.params.id)

const loading = ref(false)
const info = ref<InvestmentDto>()

const logLoading = ref(false)
const logs = ref<InvestmentLogDto[]>([])
const logRows = ref<InvestmentLogDto[]>([])
const totalLogs = ref(0)
const logPageNo = ref(1)
const logPageSize = ref(10)
const activeChart = ref('net')
const activeInfoTab = ref('basic')
const activeDataTab = ref('position')

const detailMetrics = computed(() => [
  {
    label: '最新资产',
    value: formatCurrency(latestLog.value?.asset),
    icon: Wallet,
    colorClass: 'bg-blue',
    valueClass: ''
  },
  {
    label: '最新现金',
    value: formatCurrency(latestLog.value?.cash),
    icon: TrendCharts,
    colorClass: 'bg-green',
    valueClass: ''
  },
  {
    label: '最新收益',
    value: formatCurrency(latestLog.value?.profit),
    icon: Goods,
    colorClass: 'bg-orange',
    valueClass: getChangeClass(latestLog.value?.profit)
  },
  {
    label: '日志总数',
    value: `${totalLogs.value}`,
    icon: Warning,
    colorClass: 'bg-red',
    valueClass: ''
  }
])

const latestLog = computed(() => {
  if (!logs.value.length) return undefined
  return [...logs.value].sort((a, b) => dayjs(b.recordDate).valueOf() - dayjs(a.recordDate).valueOf())[0]
})

const chartLogs = computed(() => {
  return [...logs.value]
    .filter(item => item.recordDate)
    .sort((a, b) => dayjs(a.recordDate).valueOf() - dayjs(b.recordDate).valueOf())
})

const netValueOption = computed(() => ({
  tooltip: {
    trigger: 'axis'
  },
  legend: {
    top: 0
  },
  grid: {
    top: 36,
    left: 10,
    right: 10,
    bottom: 8,
    containLabel: true
  },
  xAxis: {
    type: 'category',
    data: chartLogs.value.map(item => dayjs(item.recordDate).format('MM-DD'))
  },
  yAxis: {
    type: 'value',
    splitLine: {
      lineStyle: {
        type: 'dashed'
      }
    }
  },
  series: [
    {
      name: '资产',
      type: 'line',
      smooth: true,
      showSymbol: false,
      data: chartLogs.value.map(item => Number(item.asset ?? 0))
    },
    {
      name: '现金',
      type: 'line',
      smooth: true,
      showSymbol: false,
      data: chartLogs.value.map(item => Number(item.cash ?? 0))
    }
  ]
}))

const positionOption = computed(() => ({
  tooltip: {
    trigger: 'axis'
  },
  legend: {
    top: 0
  },
  grid: {
    top: 36,
    left: 10,
    right: 10,
    bottom: 8,
    containLabel: true
  },
  xAxis: {
    type: 'category',
    data: chartLogs.value.map(item => dayjs(item.recordDate).format('MM-DD'))
  },
  yAxis: {
    type: 'value',
    splitLine: {
      lineStyle: {
        type: 'dashed'
      }
    }
  },
  series: [
    {
      name: '持仓市值',
      type: 'line',
      smooth: true,
      showSymbol: false,
      data: chartLogs.value.map(item => Number(item.asset ?? 0) - Number(item.cash ?? 0))
    },
    {
      name: '收益',
      type: 'line',
      smooth: true,
      showSymbol: false,
      data: chartLogs.value.map(item => Number(item.profit ?? 0))
    }
  ]
}))

const strategyConfigText = computed(() => {
  const raw = info.value?.strategyInfo?.config
  if (!raw) return '-'
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
})

const targetsText = computed(() => {
  if (!info.value?.targets?.length) return '-'
  return info.value.targets.join('，')
})

const formatDate = (dateStr?: string) => {
  if (!dateStr) return ''
  return dayjs(dateStr).format('YYYY-MM-DD HH:mm:ss')
}

const formatDateValue = (dateStr?: string) => {
  if (!dateStr) return '-'
  return dayjs(dateStr).format('YYYY-MM-DD')
}

const formatTimeValue = (dateStr?: string) => {
  if (!dateStr) return '-'
  return dayjs(dateStr).format('HH:mm:ss')
}

const formatCurrency = (value?: number) => {
  const n = Number(value ?? 0)
  if (Number.isNaN(n)) return '¥0.00'
  return `¥${n.toFixed(2)}`
}

const getChangeClass = (value?: number) => {
  const n = Number(value ?? 0)
  if (n > 0) return 'text-red'
  if (n < 0) return 'text-green'
  return ''
}

const statusText = (status?: string) => {
  if (status === 'RUNNING') return '运行中'
  if (status === 'DRAFT') return '草稿'
  if (status === 'STOPPED') return '已停止'
  return status || '-'
}

const statusTagType = (status?: string) => {
  if (status === 'RUNNING') return 'success'
  if (status === 'DRAFT') return 'warning'
  return 'info'
}

const logTypeText = (type?: string) => {
  if (type === 'DEPOSIT') return '入金'
  if (type === 'WITHDRAWAL') return '出金'
  if (type === 'TRADE') return '交易'
  return type || '-'
}

const logTypeTagType = (type?: string) => {
  if (type === 'DEPOSIT') return 'success'
  if (type === 'WITHDRAWAL') return 'warning'
  if (type === 'TRADE') return 'primary'
  return 'info'
}

const loadInfo = async () => {
  loading.value = true
  try {
    const res = await getInvestment(investmentId)
    if (res.code === 200) {
      info.value = res.data as InvestmentDto
    } else {
      info.value = undefined
    }
  } finally {
    loading.value = false
  }
}

const loadLogs = async () => {
  const allLogItems: InvestmentLogDto[] = []
  let pageNo = 1
  const pageSize = 100
  let total = 0

  while (true) {
    const res = await listInvestmentLogs({
      pageNo,
      pageSize,
      investmentId
    })
    if (res.code !== 200 || !res.data) {
      allLogItems.length = 0
      break
    }
    const items = res.data.items ?? []
    total = res.data.total ?? items.length
    allLogItems.push(...items)
    if (!items.length || allLogItems.length >= total) break
    pageNo += 1
  }

  logs.value = allLogItems
  totalLogs.value = total
}

const loadLogPage = async () => {
  logLoading.value = true
  try {
    const res = await listInvestmentLogs({
      pageNo: logPageNo.value,
      pageSize: logPageSize.value,
      investmentId
    })
    if (res.code === 200 && res.data) {
      logRows.value = res.data.items ?? []
      totalLogs.value = res.data.total ?? res.data.items.length
    } else {
      logRows.value = []
      totalLogs.value = 0
    }
  } finally {
    logLoading.value = false
  }
}

const onLogPageSizeChange = () => {
  logPageNo.value = 1
  loadLogPage()
}

const onLogPageChange = () => {
  loadLogPage()
}

const onBack = () => {
  router.push('/investment/list')
}

onMounted(() => {
  loadInfo()
  loadLogs()
  loadLogPage()
})
</script>

<style scoped>
.page-container {
  width: 100%;
  padding: 0;
  min-height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 24px 28px;
  border-radius: 20px;
  background: linear-gradient(135deg, #ffffff 0%, #f8fbff 100%);
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.06);
}

.page-header__main {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
}

.page-header__tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.back-button {
  padding: 0;
}

.page-title__main {
  font-size: 24px;
  font-weight: 600;
  color: #0f172a;
}

.page-subtitle {
  margin-top: 4px;
  color: #64748b;
  font-size: 13px;
}

.meta-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.meta-item {
  padding: 16px 18px;
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
}

.meta-item__label {
  display: block;
  color: #64748b;
  font-size: 13px;
}

.meta-item__value {
  display: block;
  margin-top: 8px;
  color: #0f172a;
  font-size: 16px;
  font-weight: 600;
  line-height: 24px;
}

.content-shell {
  min-width: 0;
}

.mb-4 {
  margin-bottom: 24px;
}

.mb-xs-4 {
  margin-bottom: 0;
}

@media (max-width: 991px) {
  .mb-xs-4 {
    margin-bottom: 24px;
  }
}

.metric-card,
.chart-card,
.list-card,
.table-card {
  height: 100%;
  border: none;
  border-radius: 18px;
  transition: all 0.3s;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
}
.metric-card:hover,
.chart-card:hover,
.list-card:hover,
.table-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08);
}

.metric-content {
  display: flex;
  align-items: center;
  padding: 8px 0;
}

.icon-wrapper {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  font-size: 28px;
  color: #fff;
  flex-shrink: 0;
}

.bg-blue { background: linear-gradient(135deg, #409EFF, #79bbff); box-shadow: 0 8px 16px rgba(64, 158, 255, 0.2); }
.bg-green { background: linear-gradient(135deg, #67C23A, #95d475); box-shadow: 0 8px 16px rgba(103, 194, 58, 0.2); }
.bg-orange { background: linear-gradient(135deg, #E6A23C, #f3d19e); box-shadow: 0 8px 16px rgba(230, 162, 60, 0.2); }
.bg-red { background: linear-gradient(135deg, #F56C6C, #fab6b6); box-shadow: 0 8px 16px rgba(245, 108, 108, 0.2); }

.metric-info {
  flex-grow: 1;
  overflow: hidden;
}

.metric-info .label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 4px;
}

.metric-info .value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.main-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 34%);
  align-items: start;
  gap: 24px;
}
.main-left {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.main-right {
  width: auto;
  min-width: 0;
  max-width: none;
  flex: initial;
}

.chart-card :deep(.el-card__header),
.list-card :deep(.el-card__header),
.table-card :deep(.el-card__header) {
  border-bottom: 1px solid #e8edf5;
  padding: 18px 20px;
}

.chart-card :deep(.el-card__body),
.list-card :deep(.el-card__body),
.table-card :deep(.el-card__body) {
  padding: 16px 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title-wrapper {
  display: flex;
  flex-direction: column;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.section-subtitle {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
}
.full {
  width: 100%;
  max-width: 100%;
}
.mt {
  margin-top: 8px;
}
.chart-panel :deep(.el-card__body) {
  padding-top: 12px;
}
.side-panel :deep(.el-card__body) {
  padding-top: 12px;
}
.chart-tabs :deep(.el-tabs__header) {
  margin-bottom: 12px;
}
.side-panel :deep(.el-tabs__header) {
  margin-bottom: 12px;
}
.side-panel .strategy-config {
  max-height: 320px;
}
.chart-container {
  width: 100%;
  position: relative;
}

.main-chart {
  height: 320px;
}
.chart {
  width: 100%;
  height: 100%;
}
.strategy-config {
  width: 100%;
  max-height: 220px;
  overflow: auto;
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}
.text-red {
  color: #f56c6c;
}
.text-green {
  color: #67c23a;
}
.log-pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.position-tab-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.position-tab-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border-radius: 16px;
  background: #f8fafc;
}

.position-tab-panel__meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.log-tab-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.log-tab-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border-radius: 16px;
  background: #f8fafc;
}

.log-tab-panel__meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.chart-tab-panel,
.info-tab-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chart-tab-panel__header,
.info-tab-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border-radius: 16px;
  background: #f8fafc;
}

.chart-tab-panel__meta,
.info-tab-panel__meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.chart-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 320px;
  border-radius: 16px;
  background: #f8fafc;
}

.info-section {
  padding: 16px;
  border-radius: 16px;
  background: #f8fafc;
}

.info-section__title {
  margin-bottom: 12px;
  color: #334155;
  font-size: 14px;
  font-weight: 600;
}

.info-section :deep(.el-descriptions__body) {
  border-radius: 12px;
  overflow: hidden;
}

.strategy-config-shell {
  padding: 14px 16px;
  border-radius: 12px;
  background: #0f172a;
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

@media (max-width: 768px) {
  .page-header {
    padding: 20px;
    flex-direction: column;
  }

  .page-header__tags {
    width: 100%;
  }

  .meta-strip {
    grid-template-columns: 1fr;
  }

  .position-tab-panel__header {
    flex-direction: column;
    align-items: flex-start;
  }

  .log-tab-panel__header {
    flex-direction: column;
    align-items: flex-start;
  }

  .chart-tab-panel__header,
  .info-tab-panel__header {
    flex-direction: column;
    align-items: flex-start;
  }

  .position-tab-panel__meta {
    justify-content: flex-start;
  }

  .log-tab-panel__meta {
    justify-content: flex-start;
  }

  .chart-tab-panel__meta,
  .info-tab-panel__meta {
    justify-content: flex-start;
  }

  .main-chart {
    height: 260px;
  }

  .chart-empty {
    min-height: 260px;
  }
}
@media (max-width: 1200px) {
  .main-layout {
    grid-template-columns: 1fr;
  }
  .main-right {
    width: 100%;
    min-width: 0;
  }
}
</style>
