<template>
  <div class="dashboard" v-loading="loading">
    <!-- Top Metrics -->
    <el-row :gutter="20" class="metric-row">
      <el-col :xs="24" :sm="12" :md="6" v-for="(metric, index) in metrics" :key="index" class="metric-col">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-content">
            <div class="icon-wrapper" :class="metric.colorClass">
              <el-icon><component :is="metric.icon" /></el-icon>
            </div>
            <div class="metric-info">
              <div class="label">{{ metric.label }}</div>
              <div class="value" :class="metric.valueClass" v-if="metric.type !== 'status'">{{ metric.value }}</div>
              <div class="value status-lights" v-else>
                <el-tooltip content="Redis 状态" placement="top">
                  <div class="status-light" :class="systemStatus.redisStatus === 'NORMAL' ? 'light-green' : 'light-red'"></div>
                </el-tooltip>
                <el-tooltip content="MySQL 状态" placement="top">
                  <div class="status-light" :class="systemStatus.mysqlStatus === 'NORMAL' ? 'light-green' : 'light-red'"></div>
                </el-tooltip>
                <el-tooltip content="ClickHouse 状态" placement="top">
                  <div class="status-light" :class="systemStatus.clickHouseStatus === 'NORMAL' ? 'light-green' : (systemStatus.clickHouseStatus === 'DISABLED' ? 'light-gray' : 'light-red')"></div>
                </el-tooltip>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Middle Section: Chart & Broker List -->
    <el-row :gutter="20" class="mb-4">
      <el-col :xs="24" :lg="16" class="mb-xs-4">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <div class="title-wrapper">
                <span class="title">资产走势</span>
                <span class="subtitle">近30天趋势</span>
              </div>
              <el-tag size="small" effect="plain">Monthly</el-tag>
            </div>
          </template>
          <div class="chart-container main-chart">
            <v-chart class="chart" :option="trendOption" autoresize />
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card shadow="hover" class="list-card">
          <div class="side-card-header">
            <el-tabs v-model="sideTab" class="side-tabs">
              <el-tab-pane label="最近投资" name="investment" />
              <el-tab-pane label="热门券商" name="broker" />
            </el-tabs>
            <el-button v-if="sideTab === 'investment'" link type="primary" @click="$router.push('/investment/list')">查看全部</el-button>
            <el-button v-else link type="primary" @click="$router.push('/broker/list')">查看全部</el-button>
          </div>

          <div v-if="sideTab === 'investment'" class="investment-list">
            <div v-for="investment in recentInvestments" :key="investment.id" class="investment-item">
              <el-avatar :size="40" class="broker-avatar" :style="{ backgroundColor: getAvatarColor(investment.name || 'I') }">
                {{ (investment.name || 'I').charAt(0) }}
              </el-avatar>
              <div class="broker-info">
                <div class="broker-name item-title-link" @click="$router.push(`/investment/detail/${investment.id}`)">{{ investment.name || '-' }}</div>
                <div class="broker-code">{{ investment.strategyInfo?.name || '未配置策略' }}</div>
              </div>
              <div class="investment-meta">
                <el-tag size="small" :type="investment.status === 'RUNNING' ? 'success' : (investment.status === 'DRAFT' ? 'warning' : 'info')">
                  {{ investment.status === 'RUNNING' ? '运行中' : (investment.status === 'DRAFT' ? '草稿' : '已停止') }}
                </el-tag>
                <span class="broker-capital">{{ formatCurrency(investment.budget || 0) }}</span>
              </div>
            </div>
            <el-empty v-if="recentInvestments.length === 0" description="暂无数据" :image-size="60" />
          </div>

          <div v-else class="broker-list">
            <div v-for="broker in brokers" :key="broker.id" class="broker-item">
              <el-avatar :size="40" class="broker-avatar" :style="{ backgroundColor: getAvatarColor(broker.name) }">
                {{ broker.name.charAt(0) }}
              </el-avatar>
              <div class="broker-info">
                <div class="broker-name item-title-link" @click="$router.push(`/broker/detail/${broker.id}`)">{{ broker.name }}</div>
                <div class="broker-code">{{ broker.code }}</div>
              </div>
              <div class="broker-capital">
                {{ formatCurrency(broker.currentCapital || 0) }}
              </div>
            </div>
            <el-empty v-if="brokers.length === 0" description="暂无数据" :image-size="60" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Bottom Section: Trades Table & Pie Chart -->
    <el-row :gutter="20">
      <el-col :xs="24" :lg="16" class="mb-xs-4">
        <el-card shadow="hover" class="table-card">
          <template #header>
            <div class="card-header">
              <span class="title">近期交易</span>
              <el-button link type="primary" @click="$router.push('/investment/trading')">查看全部</el-button>
            </div>
          </template>
          <el-table :data="trades" style="width: 100%" :show-header="true" stripe>
            <el-table-column prop="symbol" label="标的" min-width="120">
              <template #default="{ row }">
                <div class="product-info">
                  <span class="product-name font-bold">{{ row.symbol }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="type" label="类型" width="100">
              <template #default="{ row }">
                <el-tag :type="row.type === 'BUY' ? 'danger' : 'success'" effect="light" size="small">
                  {{ row.type === 'BUY' ? '买入' : '卖出' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="price" label="价格" align="right">
              <template #default="{ row }">
                {{ formatCurrency(row.price) }}
              </template>
            </el-table-column>
            <el-table-column prop="volume" label="数量" align="right" />
            <el-table-column prop="amount" label="金额" align="right" min-width="120">
              <template #default="{ row }">
                <span class="font-mono">{{ formatCurrency(row.price * row.volume) }}</span>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无交易记录" :image-size="60" />
            </template>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="title">资产分布</span>
            </div>
          </template>
          <div class="chart-container pie-chart">
            <v-chart class="chart" :option="pieOption" autoresize />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { LineChart, PieChart } from 'echarts/charts';
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components';
import VChart from 'vue-echarts';
import { Wallet, TrendCharts, Goods, Warning, DataLine, HelpFilled, Monitor } from '@element-plus/icons-vue';
import { getDashboardOverview, getSystemStatus, type DashboardDto, type SystemStatusDto } from '../../services/dashboard';
import { listBrokers, listTradings, listInvestments, type BrokerDto, type TradingDto, type InvestmentDto } from '../../services/basic';

use([CanvasRenderer, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent]);

const loading = ref(false);

const dashboardData = ref<DashboardDto>({
  totalAsset: 0,
  todayChange: 0,
  totalProfit: 0,
  holdingCount: 0,
  winRate: '0.00%',
  maxDrawdown: '0.00%',
  riskIndicator: '-',
  assetTrend: [],
  assetDistribution: []
});

const systemStatus = ref<SystemStatusDto>({
  redisStatus: 'DISABLED',
  clickHouseStatus: 'DISABLED',
  mysqlStatus: 'DISABLED'
});

const brokers = ref<BrokerDto[]>([]);
const trades = ref<TradingDto[]>([]);
const recentInvestments = ref<InvestmentDto[]>([]);
const sideTab = ref('investment');

const formatCurrency = (val: number) => {
  if (val === undefined || val === null) return '¥0.00';
  return `¥${val.toFixed(2)}`;
};

const getChangeClass = (val: number) => {
  if (val > 0) return 'text-red';
  if (val < 0) return 'text-green';
  return '';
};

const getAvatarColor = (name: string) => {
  const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#79bbff', '#95d475', '#f3d19e', '#fab6b6'];
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash);
  }
  return colors[Math.abs(hash) % colors.length];
};

const metrics = computed(() => [
  {
    label: '资产总览',
    value: formatCurrency(dashboardData.value.totalAsset),
    icon: Wallet,
    colorClass: 'bg-blue',
    valueClass: ''
  },
  {
    label: '今日变动',
    value: formatCurrency(dashboardData.value.todayChange),
    icon: TrendCharts,
    colorClass: 'bg-green',
    valueClass: getChangeClass(dashboardData.value.todayChange)
  },
  {
    label: '累计盈亏',
    value: formatCurrency(dashboardData.value.totalProfit),
    icon: DataLine,
    colorClass: 'bg-red',
    valueClass: getChangeClass(dashboardData.value.totalProfit)
  },
  {
    label: '胜率',
    value: dashboardData.value.winRate,
    icon: HelpFilled,
    colorClass: 'bg-orange',
    valueClass: 'text-red'
  },
  {
    label: '最大回撤',
    value: dashboardData.value.maxDrawdown,
    icon: Warning,
    colorClass: 'bg-gray',
    valueClass: 'text-green'
  },
  {
    label: '持仓数量',
    value: dashboardData.value.holdingCount.toString(),
    icon: Goods,
    colorClass: 'bg-blue',
    valueClass: ''
  },
  {
    label: '风险指标',
    value: dashboardData.value.riskIndicator,
    icon: Warning,
    colorClass: 'bg-orange',
    valueClass: ''
  },
  {
    label: '系统状态',
    value: '',
    icon: Monitor,
    colorClass: 'bg-green',
    valueClass: '',
    type: 'status'
  }
]);

const trendOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'rgba(255, 255, 255, 0.95)',
    borderColor: '#eee',
    textStyle: { color: '#333' },
    formatter: (params: any) => {
      const item = params[0];
      return `<div class="tooltip-title" style="font-weight:600;margin-bottom:4px;">${item.axisValue}</div>
              <div class="tooltip-item" style="display:flex;align-items:center;">
                <span class="dot" style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${item.color};margin-right:6px;"></span>
                <span class="label" style="margin-right:12px;">${item.seriesName}</span>
                <span class="value" style="font-weight:600;">${formatCurrency(item.value)}</span>
              </div>`;
    }
  },
  grid: {
    top: '10%',
    left: '2%',
    right: '2%',
    bottom: '5%',
    containLabel: true
  },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: dashboardData.value.assetTrend.map(item => item.label),
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#909399' }
  },
  yAxis: {
    type: 'value',
    splitLine: {
      lineStyle: { type: 'dashed', color: '#f0f2f5' }
    },
    axisLabel: { color: '#909399' }
  },
  series: [
    {
      name: '总资产',
      type: 'line',
      smooth: true,
      showSymbol: false,
      itemStyle: { color: '#409EFF' },
      lineStyle: { width: 3, shadowColor: 'rgba(64, 158, 255, 0.3)', shadowBlur: 10 },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(64, 158, 255, 0.2)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0)' }
          ]
        }
      },
      data: dashboardData.value.assetTrend.map(item => item.value)
    }
  ]
}));

const pieOption = computed(() => ({
  tooltip: {
    trigger: 'item',
    backgroundColor: 'rgba(255, 255, 255, 0.95)',
    borderColor: '#eee',
    textStyle: { color: '#333' },
    formatter: (params: any) => {
      return `<div class="tooltip-item" style="display:flex;align-items:center;">
                <span class="dot" style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${params.color};margin-right:6px;"></span>
                <span class="label" style="margin-right:12px;">${params.name}</span>
                <span class="value" style="font-weight:600;">${params.percent}% (${formatCurrency(params.value)})</span>
              </div>`;
    }
  },
  legend: {
    bottom: '0%',
    left: 'center',
    icon: 'circle',
    itemWidth: 8,
    itemHeight: 8,
    textStyle: { color: '#606266' }
  },
  series: [
    {
      name: '资产分布',
      type: 'pie',
      radius: ['45%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 8,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: { show: false },
      emphasis: {
        label: {
          show: true,
          fontSize: 16,
          fontWeight: 'bold',
          formatter: '{b}\n{d}%'
        }
      },
      data: dashboardData.value.assetDistribution.map(item => ({
        name: item.label,
        value: item.value
      }))
    }
  ]
}));

const loadAllData = async () => {
  loading.value = true;
  try {
    const [overviewRes, statusRes, brokerRes, tradeRes, investmentRes] = await Promise.all([
      getDashboardOverview(),
      getSystemStatus(),
      listBrokers({ pageNo: 1, pageSize: 5 }),
      listTradings({ pageNo: 1, pageSize: 5 }),
      listInvestments({ pageNo: 1, pageSize: 5 })
    ]);

    if (overviewRes.code === 200) {
      dashboardData.value = overviewRes.data;
    }
    if (statusRes.code === 200) {
      systemStatus.value = statusRes.data;
    }
    if (brokerRes.code === 200) {
      brokers.value = brokerRes.data.items;
    }
    if (tradeRes.code === 200) {
      trades.value = tradeRes.data.items;
    }
    if (investmentRes.code === 200) {
      recentInvestments.value = investmentRes.data.items;
    }
  } catch (error) {
    console.error('Failed to load dashboard data:', error);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadAllData();
});
</script>

<style scoped>
.dashboard {
  padding: 0;
  min-height: 100%;
}

.header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #1f2f3d;
  margin: 0;
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

.metric-row {
  margin-bottom: 4px;
}

.metric-col {
  margin-bottom: 20px;
}

/* Cards */
.metric-card,
.chart-card,
.list-card,
.table-card {
  height: 100%;
  border: none;
  border-radius: 12px;
  transition: all 0.3s;
}

.metric-card:hover,
.chart-card:hover,
.list-card:hover,
.table-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08);
}

/* Metric Content */
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
.bg-gray { background: linear-gradient(135deg, #909399, #d3d4d6); box-shadow: 0 8px 16px rgba(144, 147, 153, 0.2); }

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

.status-lights {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 32px;
}

.status-light {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.1);
}

.light-green { background: #67C23A; box-shadow: 0 0 8px #67C23A; }
.light-red { background: #F56C6C; box-shadow: 0 0 8px #F56C6C; }
.light-gray { background: #909399; }

.text-red { color: #F56C6C !important; }
.text-green { color: #67C23A !important; }

/* Card Headers */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-wrapper {
  display: flex;
  flex-direction: column;
}

.title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.subtitle {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

/* Charts */
.chart-container {
  width: 100%;
  position: relative;
}

.main-chart { height: 320px; }
.pie-chart { height: 280px; }

.chart {
  width: 100%;
  height: 100%;
}

.side-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.side-tabs {
  flex: 1;
}

.side-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.side-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

/* Broker List */
.broker-list {
  height: 320px;
  overflow-y: auto;
  padding-right: 4px;
}

.broker-item {
  display: flex;
  align-items: center;
  padding: 12px 8px;
  border-bottom: 1px solid #f0f2f5;
  transition: background-color 0.2s;
}

.broker-item:hover {
  background-color: #f5f7fa;
  border-radius: 8px;
}

.broker-item:last-child {
  border-bottom: none;
}

.broker-avatar {
  margin-right: 12px;
  color: #fff;
  font-weight: 600;
  font-size: 16px;
}

.broker-info {
  flex-grow: 1;
}

.broker-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.item-title-link {
  cursor: pointer;
}

.item-title-link:hover {
  color: #409EFF;
}

.investment-list {
  height: 320px;
  overflow-y: auto;
  padding-right: 4px;
}

.investment-item {
  display: flex;
  align-items: center;
  padding: 12px 8px;
  border-bottom: 1px solid #f0f2f5;
  transition: background-color 0.2s;
}

.investment-item:hover {
  background-color: #f5f7fa;
  border-radius: 8px;
}

.investment-item:last-child {
  border-bottom: none;
}

.investment-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}

.broker-code {
  font-size: 12px;
  color: #909399;
}

.broker-capital {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

/* Table */
.product-info {
  display: flex;
  align-items: center;
}

.font-bold { font-weight: 600; }
.font-mono { font-family: monospace; }
</style>
