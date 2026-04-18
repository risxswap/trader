<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__content">
        <div class="title">投资列表</div>
        <div class="subtitle">统一查看计划信息、收益表现、策略配置与持仓入口</div>
      </div>
      <div class="page-header__actions">
        <el-button type="primary" @click="onAdd">添加计划</el-button>
      </div>
    </div>

    <el-card class="search-card">
      <div class="search-header">
        <div>
          <div class="section-title">筛选条件</div>
          <div class="section-subtitle">支持按名称、策略、预算和状态快速检索投资计划</div>
        </div>
        <div class="search-meta">
          <span>当前 {{ rows.length }} 条</span>
          <span v-if="strategiesVersion">策略版本 {{ strategiesVersion }}</span>
        </div>
      </div>
      <el-form class="search-form">
        <div class="search-grid">
          <el-form-item label="名称">
            <el-input v-model="searchForm.name" placeholder="请输入名称" clearable />
          </el-form-item>
          <el-form-item label="策略">
            <el-select v-model="searchForm.strategy" placeholder="请选择策略" clearable>
              <el-option v-for="item in strategies" :key="item.className" :label="item.name" :value="item.className" />
            </el-select>
          </el-form-item>
          <el-form-item label="预算">
            <el-input-number v-model="searchForm.budget" :min="0" controls-position="right" placeholder="请输入预算" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
              <el-option label="草稿" value="DRAFT" />
              <el-option label="运行中" value="RUNNING" />
              <el-option label="已停止" value="STOPPED" />
            </el-select>
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
          <div class="section-title">计划列表</div>
          <div class="section-subtitle">详情、持仓、编辑和删除保持统一操作顺序</div>
        </div>
        <div v-if="strategiesUpdated" class="table-toolbar__meta">策略更新时间 {{ strategiesUpdated }}</div>
      </div>
      <el-table :data="rows" v-loading="loading" class="full" stripe>
        <el-table-column prop="id" width="84" label="ID" />
        <el-table-column label="计划信息" min-width="220">
          <template #default="{ row }">
            <div class="primary-cell">{{ row.name }}</div>
            <div class="secondary-cell">{{ row.groupName || '未分组' }}</div>
          </template>
        </el-table-column>
      <el-table-column label="券商" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="primary-cell">{{ getBrokerText(row.brokerId) }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="strategy" label="策略" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="primary-cell">{{ row.strategyInfo?.name || row.strategyInfo?.className }}</div>
          <div class="secondary-cell">{{ row.strategyInfo?.className || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="budget" label="预算" min-width="120">
        <template #default="{ row }">
          <span class="primary-cell">{{ formatCurrency(row.budget) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="收益表现" min-width="140">
        <template #default="{ row }">
          <div :class="(row.profitAmount ?? 0) >= 0 ? 'text-red' : 'text-green'" class="primary-cell">
            {{ formatCurrency(row.profitAmount) }}
          </div>
          <div :class="(row.profitRate ?? 0) >= 0 ? 'text-red' : 'text-green'" class="secondary-cell secondary-cell--strong">
            {{ formatPercent(row.profitRate) }}
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">
            {{ statusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push(`/investment/detail/${row.id}`)">详情</el-button>
          <el-button link type="primary" @click="onPositions(row)">持仓</el-button>
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
      :title="isEdit ? '编辑计划' : '添加计划'"
      direction="rtl"
      size="560px"
      append-to-body
      destroy-on-close
      class="editor-drawer"
    >
      <div class="drawer-header">
        <div>
          <div class="section-title">{{ isEdit ? '编辑计划' : '添加计划' }}</div>
          <div class="section-subtitle">统一维护计划基础信息、执行配置与策略参数</div>
        </div>
        <div class="drawer-header__meta">
          <el-tag effect="plain" type="info">{{ isEdit ? '编辑模式' : '新增模式' }}</el-tag>
          <el-tag v-if="form.id" effect="plain" type="primary">计划ID {{ form.id }}</el-tag>
        </div>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="drawer-form">
        <div class="form-section">
          <div class="form-section__title">基础信息</div>
          <el-form-item label="名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入计划名称" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="form.status" placeholder="请选择状态">
              <el-option label="草稿" value="DRAFT" />
              <el-option label="运行中" value="RUNNING" />
              <el-option label="已停止" value="STOPPED" />
            </el-select>
          </el-form-item>
          <el-form-item label="分组" prop="groupName">
            <el-input v-model="form.groupName" placeholder="请输入分组" />
          </el-form-item>
          <el-form-item label="预算" prop="budget">
            <el-input-number v-model="form.budget" controls-position="right" :min="0" />
          </el-form-item>
          <el-form-item label="券商" prop="brokerId">
            <el-select v-model="form.brokerId" placeholder="请选择券商" filterable>
              <el-option
                v-for="b in brokerOptions"
                :key="b.id"
                :label="`${b.name} (${b.code})`"
                :value="b.id"
              />
            </el-select>
          </el-form-item>
        </div>

        <div class="form-section">
          <div class="form-section__title">执行配置</div>
          <el-form-item label="Cron" prop="cron">
            <el-input v-model="form.cron" placeholder="例如：0 0 9 * * ?" clearable />
          </el-form-item>
          <el-form-item label="策略" prop="strategy">
            <el-select v-model="form.strategy" placeholder="请选择策略" @change="onStrategyChange">
              <el-option v-for="item in strategies" :key="item.className" :label="item.name" :value="item.className" />
            </el-select>
          </el-form-item>
        </div>

        <div v-if="currentSchema && currentSchema.properties" class="form-section">
          <div class="form-section__title">策略参数</div>
          <el-form-item
            v-for="(prop, key) in currentSchema.properties"
            :key="key"
            :label="prop.description || key"
            :prop="'strategyParams.' + key"
          >
            <el-input v-if="prop.type === 'string'" v-model="form.strategyParams[key]" :placeholder="prop.description" />
            <el-input-number
              v-else-if="prop.type === 'integer' || prop.type === 'number'"
              v-model="form.strategyParams[key]"
              controls-position="right"
            />
            <el-switch v-else-if="prop.type === 'boolean'" v-model="form.strategyParams[key]" />
            <el-input v-else v-model="form.strategyParams[key]" :placeholder="prop.description" />
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

    <el-drawer v-model="drawerVisible" size="80%" class="positions-drawer">
      <template #header>
        <div class="positions-drawer__header">
          <div>
            <div class="section-title">持仓列表</div>
            <div class="section-subtitle">查看当前计划的持仓结构并直接进入持仓维护</div>
          </div>
          <div class="positions-drawer__meta">
            <el-tag v-if="currentInvestmentName" effect="plain" type="primary">{{ currentInvestmentName }}</el-tag>
            <el-tag v-if="currentInvestmentId" effect="plain" type="info">投资ID {{ currentInvestmentId }}</el-tag>
          </div>
        </div>
      </template>
      <div class="positions-drawer__body">
        <position-list :investment-id="currentInvestmentId" v-if="drawerVisible" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { listInvestments, addInvestment, updateInvestment, deleteInvestment, listStrategies, listBrokers } from '../../services/basic'
import type { InvestmentDto, StrategyInfoDto, BrokerDto, StrategiesDto } from '../../services/basic'
import { ElMessage, ElMessageBox } from 'element-plus'
import PositionList from './PositionList.vue'

const loading = ref(false)
const rows = ref<InvestmentDto[]>([])
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)
const strategies = ref<StrategyInfoDto[]>([])
const strategiesVersion = ref<string>('')
const strategiesUpdated = ref<string>('')
const brokerOptions = ref<BrokerDto[]>([])

const formatCurrency = (val?: number) => {
  if (val === undefined || val === null) return '-'
  return `¥${Number(val).toFixed(2)}`
}

const formatPercent = (val?: number) => {
  if (val === undefined || val === null) return '-'
  return `${(Number(val) * 100).toFixed(2)}%`
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

const searchForm = reactive({
  name: '',
  strategy: '',
  budget: undefined as number | undefined,
  status: ''
})

const load = async () => {
  loading.value = true
  try {
    const res = await listInvestments({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      name: searchForm.name || undefined,
      strategy: searchForm.strategy || undefined,
      budget: searchForm.budget || undefined,
      status: searchForm.status || undefined
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

const resetSearch = () => {
  searchForm.name = ''
  searchForm.strategy = ''
  searchForm.budget = undefined
  searchForm.status = ''
  load()
}

const loadStrategies = async () => {
  const res = await listStrategies()
  if (res.code === 200 && res.data) {
    strategies.value = (res.data as StrategiesDto).items
    strategiesVersion.value = (res.data as StrategiesDto).version
    strategiesUpdated.value = (res.data as StrategiesDto).lastUpdated
  }
}

const loadBrokers = async () => {
  const res = await listBrokers({ pageNo: 1, pageSize: 200 })
  if (res.code === 200 && res.data) {
    brokerOptions.value = res.data.items
  } else {
    brokerOptions.value = []
  }
}

const getBrokerText = (brokerId?: number) => {
  if (!brokerId) return '-'
  const found = brokerOptions.value.find(b => b.id === brokerId)
  if (!found) return String(brokerId)
  return `${found.name} (${found.code})`
}

// 增删改相关
const editorVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref()
interface InvestmentForm {
  id?: number
  name: string
  groupName: string
  targetType?: string
  investType?: string
  brokerId?: number
  targets?: string[]
  budget: number
  strategy: string
  strategyConfig?: string
  cron: string
  executorId?: string
  status: string
  strategyParams: Record<string, any>
}

const form = reactive<InvestmentForm>({
  name: '',
  groupName: '',
  strategy: '',
  budget: 0,
  brokerId: undefined,
  cron: '',
  status: 'DRAFT',
  strategyParams: {},
  strategyConfig: ''
})

const baseRules: Record<string, any> = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  strategy: [{ required: true, message: '请输入策略', trigger: 'blur' }],
  brokerId: [{ required: true, message: '请选择券商', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}
const rules = reactive<Record<string, any>>({ ...baseRules })

// 当前选中策略的Schema
const currentSchema = computed(() => {
  if (!form.strategy) return null
  const strategy = strategies.value.find(s => s.className === form.strategy)
  if (strategy && strategy.configSchame) {
    try {
      return JSON.parse(strategy.configSchame)
    } catch (e) {
      console.error('Failed to parse strategy schema', e)
      return null
    }
  }
  return null
})

const onStrategyChange = () => {
  // 切换策略时清空参数，或者根据新 schema 初始化默认值
  form.strategyParams = {}
  buildSchemaRules()
}

const onAdd = () => {
  isEdit.value = false
  form.id = undefined
  form.name = ''
  form.groupName = ''
  form.strategy = ''
  form.budget = 0
  form.brokerId = undefined
  form.cron = ''
  form.status = 'DRAFT'
  form.strategyParams = {}
  form.strategyConfig = ''
  editorVisible.value = true
  buildSchemaRules()
  nextTick(() => formRef.value?.clearValidate())
}

const onEdit = (row: InvestmentDto) => {
  isEdit.value = true
  // 复制基本属性
  form.id = row.id
  form.name = row.name || ''
  form.groupName = row.groupName || ''
  form.strategy = row.strategyInfo?.className || ''
  form.budget = row.budget || 0
  form.brokerId = row.brokerId
  form.cron = row.cron || ''
  form.status = row.status || 'DRAFT'
  form.strategyConfig = row.strategyInfo?.config || ''
  
  // 解析已保存的配置
  if (form.strategyConfig) {
    try {
      form.strategyParams = JSON.parse(form.strategyConfig)
    } catch (e) {
      console.error('Failed to parse strategy config', e)
      form.strategyParams = {}
    }
  } else {
    form.strategyParams = {}
  }
  
  editorVisible.value = true
  buildSchemaRules()
  nextTick(() => formRef.value?.clearValidate())
}

const onDelete = (row: InvestmentDto) => {
  ElMessageBox.confirm(`确定要删除 ${row.name} 吗？`, '提示', {
    type: 'warning'
  }).then(async () => {
    const res = await deleteInvestment(row.id!)
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
        // 将动态参数转换为JSON字符串
        if (Object.keys(form.strategyParams).length > 0) {
          form.strategyConfig = JSON.stringify(form.strategyParams)
        } else {
          form.strategyConfig = '{}'
        }
        const extraValidationMsg = validateBySchema()
        if (extraValidationMsg) {
          ElMessage.error(extraValidationMsg)
          return
        }
        const api = isEdit.value ? updateInvestment : addInvestment
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

// 持仓查看
const drawerVisible = ref(false)
const currentInvestmentId = ref<number>()
const currentInvestmentName = ref('')

const onPositions = (row: InvestmentDto) => {
  currentInvestmentId.value = row.id
  currentInvestmentName.value = row.name
  drawerVisible.value = true
}

onMounted(() => {
  load()
  loadStrategies()
  loadBrokers()
})

const buildSchemaRules = () => {
  Object.keys(rules).forEach(k => {
    rules[k] = baseRules[k] || []
  })
  const schema: any = currentSchema.value
  if (!schema || !schema.properties) return
  const required: string[] = schema.required || []
  required.forEach((key: string) => {
    const propKey = `strategyParams.${key}`
    rules[propKey] = [{ required: true, message: `请填写 ${key}`, trigger: 'blur' }]
  })
  Object.keys(schema.properties).forEach((key: string) => {
    const prop = schema.properties[key]
    if (!prop) return
    const propKey = `strategyParams.${key}`
    const arr = rules[propKey] || []
    if ((prop.type === 'number' || prop.type === 'integer') && (prop.minimum !== undefined || prop.maximum !== undefined)) {
      arr.push({
        validator: (_: any, value: any, callback: (err?: Error) => void) => {
          const num = Number(value)
          if (Number.isNaN(num)) {
            callback(new Error(`${key} 需为数字`))
            return
          }
          if (prop.minimum !== undefined && num < prop.minimum) {
            callback(new Error(`${key} 不得小于 ${prop.minimum}`))
            return
          }
          if (prop.maximum !== undefined && num > prop.maximum) {
            callback(new Error(`${key} 不得大于 ${prop.maximum}`))
            return
          }
          callback()
        },
        trigger: 'blur'
      })
    }
    rules[propKey] = arr
  })
}

const validateBySchema = (): string | null => {
  const schema: any = currentSchema.value
  if (!schema || !schema.properties) return null
  const cfg = form.strategyParams || {}
  const required: string[] = schema.required || []
  for (const key of required) {
    if (!(key in cfg)) {
      return `缺少必填字段 ${key}`
    }
  }
  for (const key of Object.keys(schema.properties)) {
    const prop = schema.properties[key]
    const val = cfg[key]
    if (val === undefined || val === null) continue
    if (prop.type === 'integer') {
      if (!Number.isInteger(Number(val))) return `${key} 需为整数`
    } else if (prop.type === 'number') {
      if (Number.isNaN(Number(val))) return `${key} 需为数字`
    } else if (prop.type === 'string') {
      if (typeof val !== 'string') return `${key} 需为字符串`
    }
    if (prop.minimum !== undefined && Number(val) < prop.minimum) return `${key} 不得小于 ${prop.minimum}`
    if (prop.maximum !== undefined && Number(val) > prop.maximum) return `${key} 不得大于 ${prop.maximum}`
    if (prop.enum && Array.isArray(prop.enum) && prop.enum.length > 0) {
      if (!prop.enum.includes(val)) return `${key} 不在允许范围`
    }
  }
  return null
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

.search-meta,
.table-toolbar__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
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

.table-toolbar {
  margin-bottom: 18px;
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

.secondary-cell--strong {
  font-weight: 500;
}

.pager {
  padding-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.positions-drawer__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
}

.positions-drawer__meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.positions-drawer__body {
  min-height: 100%;
  padding: 4px;
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

.positions-drawer :deep(.el-drawer__header) {
  margin-bottom: 0;
  padding-bottom: 8px;
  border-bottom: 1px solid #e8edf5;
}

.positions-drawer :deep(.el-drawer__body) {
  padding: 20px;
  background: #f5f7fb;
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
.drawer-form :deep(.el-input-number) {
  width: 100%;
}

.text-red {
  color: #f56c6c;
}
.text-green {
  color: #67c23a;
}

@media (max-width: 1200px) {
  .search-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .page-header,
  .search-header,
  .table-toolbar,
  .search-actions,
  .positions-drawer__header {
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

  .drawer-header,
  .positions-drawer__header {
    flex-direction: column;
    align-items: flex-start;
  }

  .positions-drawer__meta {
    justify-content: flex-start;
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
