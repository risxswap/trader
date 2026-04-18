<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__content">
        <div class="title">节点列表</div>
        <div class="subtitle">查看节点状态、审批流转与资源占用，保持节点管理的专业与清晰</div>
      </div>
      <div class="page-header__actions">
        <el-button type="primary" plain @click="showAllGroups">查看全部</el-button>
        <el-button type="primary" :loading="loading" @click="load">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <div class="summary-strip">
      <div class="summary-item">
        <span class="summary-label">总节点数</span>
        <span class="summary-value">{{ totalNodes }}</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">在线节点</span>
        <span class="summary-value success">{{ onlineNodes }}</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">离线节点</span>
        <span class="summary-value danger">{{ offlineNodes }}</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">待审批</span>
        <span class="summary-value warning">{{ pendingNodes }}</span>
      </div>
    </div>

    <el-card class="search-card">
      <div class="search-header">
        <div>
          <div class="section-title">筛选条件</div>
          <div class="section-subtitle">按分组、关键字和状态快速定位目标节点</div>
        </div>
        <span class="search-meta">当前 {{ filteredNodes.length }} 个节点</span>
      </div>
      <el-form class="search-form" label-position="top">
        <div class="search-grid">
          <el-form-item label="关键字" class="search-grid__keyword">
            <el-input
              v-model="filterForm.keyword"
              clearable
              placeholder="搜索节点名称、节点 ID、主机名或 IP"
              @keyup.enter="applyFilters" />
          </el-form-item>
          <el-form-item label="节点状态" class="search-grid__status">
            <el-select v-model="filterForm.status">
              <el-option label="全部状态" value="all" />
              <el-option label="在线" value="online" />
              <el-option label="离线" value="offline" />
              <el-option label="待审批" value="pending" />
            </el-select>
          </el-form-item>
          <el-form-item label="节点分组" class="search-grid__full">
            <el-radio-group v-model="selectedGroup" class="group-filter">
              <el-radio-button label="all">全部分组</el-radio-button>
              <el-radio-button
                v-for="group in groups"
                :key="group.id"
                :label="group.id">
                {{ group.name }}
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
        </div>
      </el-form>
      <div class="search-actions">
        <el-button type="primary" :icon="Search" @click="applyFilters">搜索</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>
    </el-card>

    <div v-loading="loading" class="node-grid">
      <el-empty v-if="!filteredNodes.length && !loading" description="暂无节点数据" />
      <el-card v-for="node in filteredNodes" :key="node.nodeId" shadow="hover" class="node-card">
        <div class="node-card-header">
          <div class="node-identity">
            <div class="node-name-row">
              <div class="node-name">{{ node.nodeName || node.nodeId }}</div>
              <el-tag size="small" :type="node.online ? 'success' : 'danger'">
                {{ node.online ? '在线' : '离线' }}
              </el-tag>
            </div>
            <div class="node-id">{{ node.nodeId }}</div>
          </div>
          <div class="node-tags">
            <el-tag size="small" effect="plain" :type="nodeTypeTag(node.nodeType)">
              {{ nodeTypeLabel(node.nodeType) }}
            </el-tag>
            <el-tag size="small" effect="plain" :type="node.approvalStatus === 'APPROVED' ? 'success' : 'warning'">
              {{ node.approvalStatus === 'APPROVED' ? '已审批' : '待审批' }}
            </el-tag>
          </div>
        </div>

        <div class="meta-grid">
          <div class="meta-item">
            <span class="meta-label">分组</span>
            <span class="meta-value">{{ node.nodeGroupName || '-' }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">主机名</span>
            <span class="meta-value">{{ node.hostname || '-' }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">IP</span>
            <span class="meta-value">{{ node.ipAddress || '-' }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">最后上报</span>
            <span class="meta-value">{{ formatTime(node.timestamp) }}</span>
          </div>
        </div>

        <div class="usage-panel">
          <div class="usage-item">
            <div class="usage-title">
              <span>CPU</span>
              <span>{{ formatPercent(node.cpuUsage) }}%</span>
            </div>
            <el-progress :percentage="formatPercent(node.cpuUsage)" :status="progressStatus(node.cpuUsage)" />
          </div>
          <div class="usage-item">
            <div class="usage-title">
              <span>内存</span>
              <span>{{ formatPercent(node.memoryUsage) }}%</span>
            </div>
            <el-progress :percentage="formatPercent(node.memoryUsage)" :status="progressStatus(node.memoryUsage)" />
          </div>
          <div class="usage-item">
            <div class="usage-title">
              <span>磁盘</span>
              <span>{{ formatPercent(node.diskUsage) }}%</span>
            </div>
            <el-progress :percentage="formatPercent(node.diskUsage)" :status="progressStatus(node.diskUsage)" />
          </div>
        </div>

        <div class="node-remark">{{ node.remark || '暂无备注' }}</div>

        <div class="card-actions">
          <el-button link type="primary" @click="openDetail(node)">详情</el-button>
          <el-button
            v-if="node.approvalStatus !== 'APPROVED'"
            link
            type="warning"
            @click="openApprove(node)">
            审批
          </el-button>
          <el-button link type="primary" @click="openEdit(node)">编辑</el-button>
          <el-button link type="danger" @click="removeNode(node)">删除</el-button>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="detailVisible" :title="detailTitle" width="960px" destroy-on-close>
      <div v-loading="detailLoading">
        <el-descriptions :column="2" border class="detail-descriptions">
          <el-descriptions-item label="节点名称">{{ detailNode?.nodeName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="节点ID">{{ detailNode?.nodeId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="节点类型">{{ nodeTypeLabel(detailNode?.nodeType) }}</el-descriptions-item>
          <el-descriptions-item label="审批状态">
            {{ detailNode?.approvalStatus === 'APPROVED' ? '已审批' : '待审批' }}
          </el-descriptions-item>
          <el-descriptions-item label="节点分组">{{ detailNode?.nodeGroupName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="IP">{{ detailNode?.ipAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="主机名">{{ detailNode?.hostname || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最后上报">{{ formatTime(detailNode?.timestamp) }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detailNode?.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div class="chart-container">
          <VChart class="chart" :option="chartOption" autoresize />
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="editVisible" title="编辑节点" width="520px" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="100px">
        <el-form-item label="节点名称" prop="nodeName">
          <el-input v-model="editForm.nodeName" />
        </el-form-item>
        <el-form-item label="节点类型" prop="nodeType">
          <el-select v-model="editForm.nodeType" style="width: 100%">
            <el-option label="执行器" value="executor" />
            <el-option label="采集器" value="collector" />
          </el-select>
        </el-form-item>
        <el-form-item label="节点分组" prop="nodeGroupId">
          <el-select v-model="editForm.nodeGroupId" style="width: 100%">
            <el-option
              v-for="group in groups"
              :key="group.id"
              :label="group.name"
              :value="group.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="主机名">
          <el-input v-model="editForm.hostname" />
        </el-form-item>
        <el-form-item label="IP">
          <el-input v-model="editForm.primaryIp" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="approveVisible" title="审批节点" width="420px" destroy-on-close>
      <el-form ref="approveFormRef" :model="approveForm" :rules="approveRules" label-width="100px">
        <el-form-item label="目标分组" prop="nodeGroupId">
          <el-select v-model="approveForm.nodeGroupId" style="width: 100%">
            <el-option
              v-for="group in approveGroups"
              :key="group.id"
              :label="group.name"
              :value="group.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitApprove">确认审批</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import dayjs from 'dayjs'

import { http } from '../../services/http'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent])

type NodeStatusDto = {
  id?: number
  nodeId: string
  nodeName: string
  nodeType: string
  nodeGroupId?: number
  nodeGroupName?: string
  approvalStatus?: string
  hostname?: string
  ipAddress?: string
  version?: string
  cpuUsage?: number
  memoryUsage?: number
  diskUsage?: number
  timestamp?: number
  online?: boolean
  remark?: string
}

type NodeGroupDto = {
  id: number
  name: string
  code: string
  sort?: number
  defaultPending?: boolean
}

type NodeMetricsHistoryDto = {
  timestamps: string[]
  cpuUsages: number[]
  memoryUsages: number[]
}

const loading = ref(false)
const detailLoading = ref(false)
const submitLoading = ref(false)
const nodes = ref<NodeStatusDto[]>([])
const groups = ref<NodeGroupDto[]>([])
const timer = ref<number | undefined>()
const selectedGroup = ref<'all' | number>('all')
const activeGroup = ref<'all' | number>('all')
const filterForm = reactive({
  keyword: '',
  status: 'all'
})
const appliedKeyword = ref('')
const appliedStatus = ref('all')

const detailVisible = ref(false)
const detailTitle = ref('')
const detailNode = ref<NodeStatusDto | null>(null)
const chartOption = ref({})

const editVisible = ref(false)
const editFormRef = ref<FormInstance>()
const editForm = reactive({
  id: undefined as number | undefined,
  nodeName: '',
  nodeType: 'collector',
  nodeGroupId: undefined as number | undefined,
  hostname: '',
  primaryIp: '',
  remark: ''
})

const approveVisible = ref(false)
const approveFormRef = ref<FormInstance>()
const approveForm = reactive({
  id: undefined as number | undefined,
  nodeGroupId: undefined as number | undefined
})

const editRules: FormRules = {
  nodeName: [{ required: true, message: '请输入节点名称', trigger: 'blur' }],
  nodeType: [{ required: true, message: '请选择节点类型', trigger: 'change' }],
  nodeGroupId: [{ required: true, message: '请选择节点分组', trigger: 'change' }]
}

const approveRules: FormRules = {
  nodeGroupId: [{ required: true, message: '请选择目标分组', trigger: 'change' }]
}

const filteredNodes = computed(() => {
  const keyword = appliedKeyword.value.trim().toLowerCase()
  return nodes.value.filter((item) => {
    const matchesGroup = activeGroup.value === 'all' || item.nodeGroupId === activeGroup.value
    const matchesKeyword = !keyword || [
      item.nodeName,
      item.nodeId,
      item.hostname,
      item.ipAddress
    ].some(value => (value || '').toLowerCase().includes(keyword))
    const matchesStatus = appliedStatus.value === 'all'
      || (appliedStatus.value === 'online' && item.online)
      || (appliedStatus.value === 'offline' && !item.online)
      || (appliedStatus.value === 'pending' && item.approvalStatus !== 'APPROVED')
    return matchesGroup && matchesKeyword && matchesStatus
  })
})

const totalNodes = computed(() => nodes.value.length)
const onlineNodes = computed(() => nodes.value.filter(item => item.online).length)
const offlineNodes = computed(() => nodes.value.filter(item => !item.online).length)
const pendingNodes = computed(() => nodes.value.filter(item => item.approvalStatus !== 'APPROVED').length)
const approveGroups = computed(() => groups.value.filter(item => !item.defaultPending))

const applyFilters = () => {
  appliedKeyword.value = filterForm.keyword
  appliedStatus.value = filterForm.status
  activeGroup.value = selectedGroup.value
}

const resetFilters = () => {
  filterForm.keyword = ''
  filterForm.status = 'all'
  selectedGroup.value = 'all'
  applyFilters()
}

const showAllGroups = () => {
  selectedGroup.value = 'all'
  activeGroup.value = 'all'
}

const load = async () => {
  loading.value = true
  try {
    const [groupRes, nodeRes] = await Promise.all([
      http.get('/node/group/list'),
      http.get('/node/list')
    ])
    if (groupRes.data.code !== 200) {
      throw new Error(groupRes.data.message || '获取节点分组失败')
    }
    if (nodeRes.data.code !== 200) {
      throw new Error(nodeRes.data.message || '获取节点列表失败')
    }
    groups.value = groupRes.data.data || []
    nodes.value = nodeRes.data.data || []
  } catch (error: any) {
    ElMessage.error(error.message || '加载节点数据失败')
  } finally {
    loading.value = false
  }
}

const formatPercent = (value?: number) => {
  if (value === undefined || value === null) {
    return 0
  }
  return Number((Math.max(0, Math.min(1, value)) * 100).toFixed(1))
}

const progressStatus = (value?: number) => {
  const percent = formatPercent(value)
  if (percent >= 90) return 'exception'
  if (percent >= 75) return 'warning'
  return 'success'
}

const nodeTypeLabel = (value?: string) => {
  if ((value || '').toLowerCase() === 'executor') {
    return '执行器'
  }
  return '采集器'
}

const nodeTypeTag = (value?: string) => {
  if ((value || '').toLowerCase() === 'executor') {
    return 'primary'
  }
  return 'success'
}

const formatTime = (value?: number) => {
  if (!value) {
    return '-'
  }
  return dayjs(value).format('YYYY-MM-DD HH:mm:ss')
}

const buildChartOption = (data?: NodeMetricsHistoryDto) => {
  const history = data || { timestamps: [], cpuUsages: [], memoryUsages: [] }
  chartOption.value = {
    tooltip: {
      trigger: 'axis',
      formatter: (params: Array<{ name: string; marker: string; seriesName: string; value: number }>) => {
        if (!params.length) return ''
        return params.reduce((text, item, index) => {
          const prefix = index === 0 ? `${item.name}<br/>` : text
          return `${prefix}${item.marker}${item.seriesName}：${(item.value * 100).toFixed(2)}%<br/>`
        }, '')
      }
    },
    legend: {
      data: ['CPU使用率', '内存使用率']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: history.timestamps
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        formatter: (value: number) => `${(value * 100).toFixed(0)}%`
      }
    },
    series: [
      {
        name: 'CPU使用率',
        type: 'line',
        data: history.cpuUsages,
        smooth: true,
        showSymbol: false,
        itemStyle: { color: '#409EFF' }
      },
      {
        name: '内存使用率',
        type: 'line',
        data: history.memoryUsages,
        smooth: true,
        showSymbol: false,
        itemStyle: { color: '#67C23A' }
      }
    ]
  }
}

const openDetail = async (node: NodeStatusDto) => {
  detailVisible.value = true
  detailLoading.value = true
  detailTitle.value = `节点详情 - ${node.nodeName || node.nodeId}`
  buildChartOption()
  try {
    let currentNode = node
    if (node.id) {
      const detailRes = await http.get(`/node/${node.id}`)
      if (detailRes.data.code !== 200) {
        throw new Error(detailRes.data.message || '获取节点详情失败')
      }
      currentNode = detailRes.data.data || node
    }
    detailNode.value = currentNode
    const endTime = dayjs().toISOString()
    const startTime = dayjs().subtract(1, 'hour').toISOString()
    const historyRes = await http.get(`/node/${currentNode.nodeId}/history`, {
      params: { startTime, endTime }
    })
    if (historyRes.data.code !== 200) {
      throw new Error(historyRes.data.message || '获取历史数据失败')
    }
    buildChartOption(historyRes.data.data)
  } catch (error: any) {
    ElMessage.error(error.message || '获取节点详情失败')
  } finally {
    detailLoading.value = false
  }
}

const openEdit = (node: NodeStatusDto) => {
  editForm.id = node.id
  editForm.nodeName = node.nodeName || ''
  editForm.nodeType = (node.nodeType || 'collector').toLowerCase()
  editForm.nodeGroupId = node.nodeGroupId
  editForm.hostname = node.hostname || ''
  editForm.primaryIp = node.ipAddress || ''
  editForm.remark = node.remark || ''
  editVisible.value = true
}

const submitEdit = async () => {
  const valid = await editFormRef.value?.validate()
  if (!valid || !editForm.id) {
    return
  }
  submitLoading.value = true
  try {
    const res = await http.put('/node', { ...editForm })
    if (res.data.code !== 200) {
      throw new Error(res.data.message || '保存失败')
    }
    ElMessage.success('节点已更新')
    editVisible.value = false
    await load()
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    submitLoading.value = false
  }
}

const openApprove = (node: NodeStatusDto) => {
  approveForm.id = node.id
  approveForm.nodeGroupId = approveGroups.value[0]?.id
  approveVisible.value = true
}

const submitApprove = async () => {
  const valid = await approveFormRef.value?.validate()
  if (!valid || !approveForm.id) {
    return
  }
  submitLoading.value = true
  try {
    const res = await http.post('/node/approve', { ...approveForm })
    if (res.data.code !== 200) {
      throw new Error(res.data.message || '审批失败')
    }
    ElMessage.success('节点审批成功')
    approveVisible.value = false
    await load()
  } catch (error: any) {
    ElMessage.error(error.message || '审批失败')
  } finally {
    submitLoading.value = false
  }
}

const removeNode = async (node: NodeStatusDto) => {
  if (!node.id) {
    return
  }
  try {
    await ElMessageBox.confirm(`确认删除节点 ${node.nodeName || node.nodeId} 吗？`, '删除节点', {
      type: 'warning'
    })
    const res = await http.delete(`/node/${node.id}`)
    if (res.data.code !== 200) {
      throw new Error(res.data.message || '删除失败')
    }
    ElMessage.success('节点已删除')
    await load()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

onMounted(() => {
  load()
  timer.value = window.setInterval(load, 30000)
})

onUnmounted(() => {
  if (timer.value) {
    clearInterval(timer.value)
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
  align-items: center;
  gap: 16px;
}

.page-header__content {
  min-width: 0;
}

.page-header__actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.title {
  font-size: 24px;
  font-weight: 600;
  color: #0f172a;
}

.subtitle {
  margin-top: 4px;
  color: #64748b;
  font-size: 13px;
}

.summary-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px 18px;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.summary-label {
  color: #64748b;
  font-size: 13px;
}

.summary-value {
  color: #0f172a;
  font-size: 28px;
  font-weight: 700;
  line-height: 32px;
}

.summary-value.success {
  color: #67c23a;
}

.summary-value.danger {
  color: #f56c6c;
}

.summary-value.warning {
  color: #e6a23c;
}

.search-card {
  border: none;
  border-radius: 18px;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
}

.search-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
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

.search-meta {
  color: #64748b;
  font-size: 13px;
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

.search-grid__keyword {
  grid-column: span 3;
}

.search-grid__status {
  grid-column: span 1;
}

.search-grid__full {
  grid-column: span 4;
}

.group-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.search-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.node-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 18px;
}

.node-card {
  border: none;
  border-radius: 20px;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.08);
}

.node-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eef2f7;
}

.node-identity {
  min-width: 0;
}

.node-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.node-name {
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
}

.node-id {
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
  word-break: break-all;
}

.node-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin: 18px 0;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 14px;
  border-radius: 14px;
  background: #f8fafc;
}

.meta-label {
  color: #64748b;
  font-size: 12px;
}

.meta-value {
  color: #0f172a;
  font-size: 14px;
  word-break: break-all;
}

.usage-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 16px;
}

.usage-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  color: #475569;
  font-size: 13px;
}

.node-remark {
  min-height: 44px;
  padding: 12px 14px;
  border-radius: 14px;
  background: #f8fafc;
  color: #475569;
  line-height: 20px;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid #eef2f7;
}

.detail-descriptions {
  margin-bottom: 20px;
}

.chart-container {
  height: 400px;
  width: 100%;
}

.chart {
  width: 100%;
  height: 100%;
}

@media (max-width: 768px) {
  .page-header,
  .search-header,
  .search-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .summary-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .search-grid {
    grid-template-columns: 1fr;
  }

  .search-grid__keyword,
  .search-grid__status,
  .search-grid__full {
    grid-column: span 1;
  }

  .search-actions :deep(.el-button) {
    width: 100%;
  }

  .meta-grid {
    grid-template-columns: 1fr;
  }

  .node-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .summary-strip {
    grid-template-columns: 1fr;
  }
}
</style>
