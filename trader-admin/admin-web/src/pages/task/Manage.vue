<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__content">
        <div class="title">任务管理</div>
        <div class="subtitle">统一管理任务启停、Cron 配置、参数 JSON 和即时执行入口</div>
      </div>
      <div class="page-header__meta">
        <el-tag effect="plain" type="primary">任务调度</el-tag>
        <el-tag effect="plain" type="success">双通道刷新</el-tag>
        <el-tag effect="plain" type="info">共 {{ total }} 条</el-tag>
      </div>
    </div>

    <el-card class="search-card">
      <div class="search-header">
        <div>
          <div class="section-title">筛选条件</div>
          <div class="section-subtitle">支持按应用、任务编码、任务名称和状态快速筛选配置项</div>
        </div>
        <div class="search-meta">
          <span>当前页 {{ tableData.length }} 条</span>
          <span v-if="query.appName">应用 {{ query.appName }}</span>
          <span v-if="query.taskCode">任务 {{ query.taskCode }}</span>
          <span v-if="query.status">运行状态 {{ getStatusLabel(query.status) }}</span>
        </div>
      </div>

      <el-form :model="query" class="search-form">
        <div class="search-grid">
          <el-form-item label="应用/节点">
            <el-input v-model="query.appName" placeholder="请输入应用名称" clearable @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="任务类型">
            <el-select v-model="query.taskType" placeholder="请选择任务类型" clearable @change="handleSearch">
              <el-option label="COLLECTOR" value="COLLECTOR" />
              <el-option label="STATISTIC" value="STATISTIC" />
              <el-option label="EXECUTOR" value="EXECUTOR" />
            </el-select>
          </el-form-item>
          <el-form-item label="任务编码">
            <el-input v-model="query.taskCode" placeholder="请输入任务编码" clearable @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="任务名称">
            <el-input v-model="query.taskName" placeholder="请输入任务名称" clearable @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="运行状态">
            <el-select v-model="query.status" placeholder="请选择运行状态" clearable @change="handleSearch">
              <el-option label="已停止" value="STOPPED" />
              <el-option label="执行中" value="RUNNING" />
            </el-select>
          </el-form-item>
          <el-form-item label="聚合策略">
            <el-switch v-model="query.includeInvestment" active-text="包含策略任务" @change="handleSearch" />
          </el-form-item>
        </div>
      </el-form>

      <div class="search-actions">
        <el-button type="primary" :loading="loading" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>
    </el-card>

    <el-card class="table-card">
      <div class="table-toolbar">
        <div>
          <div class="section-title">任务列表</div>
          <div class="section-subtitle">优先展示调度状态、当前参数和最近更新时间，便于快速修改与触发</div>
        </div>
        <div class="table-toolbar__meta">
          <el-button type="primary" @click="openCreateDialog">创建任务实例</el-button>
          <span style="margin-left: 12px;">分页 {{ query.pageNo }} / {{ totalPage }}</span>
          <span>每页 {{ query.pageSize }} 条</span>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe class="full">
        <el-table-column label="任务信息" min-width="220">
          <template #default="{ row }">
            <div class="primary-cell">
              <el-tag v-if="row.sourceType === 'INVESTMENT'" size="small" type="success" style="margin-right: 4px;">策略</el-tag>
              {{ row.taskName || row.taskCode }}
            </div>
            <div class="secondary-cell">{{ row.taskType || row.appName }} / {{ row.taskCode }}</div>
          </template>
        </el-table-column>
        <el-table-column label="Cron" min-width="180">
          <template #default="{ row }">
            <div class="code-cell">{{ row.cron || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="是否启用" width="100" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" @change="toggleEnabled(row)" />
          </template>
        </el-table-column>
        <el-table-column label="运行状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="执行结果" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getResultType(row.result)" size="small">{{ getResultLabel(row.result) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="任务参数" min-width="280">
          <template #default="{ row }">
            <div class="secondary-cell secondary-cell--clamp">
              {{ formatParamsPreview(row.paramsJson) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="版本 / 更新时间" min-width="180">
          <template #default="{ row }">
            <div class="primary-cell">v{{ row.version ?? '-' }}</div>
            <div class="secondary-cell">{{ formatTime(row.updatedAt) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template #default="{ row }">
            <template v-if="row.sourceType === 'INVESTMENT'">
              <el-button link type="primary" @click="goToInvestment(row)">查看策略</el-button>
            </template>
            <template v-else>
              <el-button link type="primary" @click="goToDetail(row)">详情</el-button>
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="success" @click="triggerTask(row)">执行</el-button>
              <el-button link type="danger" @click="deleteTask(row)">删除</el-button>
            </template>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无任务配置" />
        </template>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.pageNo"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="editVisible" title="编辑任务" width="760px" destroy-on-close>
      <el-form :model="editForm" label-width="96px" class="edit-form">
        <el-form-item label="应用名称">
          <el-input v-model="editForm.appName" disabled />
        </el-form-item>
        <el-form-item label="任务编码">
          <el-input v-model="editForm.taskCode" disabled />
        </el-form-item>
        <el-form-item label="Cron">
          <el-input v-model="editForm.cron" placeholder="请输入 Cron 表达式" />
        </el-form-item>
        <el-form-item label="运行状态">
          <el-input v-model="editForm.status" disabled placeholder="运行状态" />
        </el-form-item>
        <el-form-item label="任务参数">
          <el-input
            v-model="editForm.paramsJson"
            type="textarea"
            :autosize="{ minRows: 6, maxRows: 12 }"
            placeholder="请输入 params_json，例如 {&quot;fullSync&quot;:true}"
          />
        </el-form-item>
        <el-form-item label="参数 Schema">
          <el-input
            :model-value="editForm.paramSchema || '-'"
            type="textarea"
            :autosize="{ minRows: 4, maxRows: 10 }"
            disabled
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="editForm.remark"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 6 }"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
    <el-drawer v-model="createVisible" title="创建任务实例" size="860px" destroy-on-close>
      <div class="create-drawer">
        <el-form :model="createForm" label-width="96px" class="edit-form">
          <el-form-item label="任务实例">
            <el-select
              v-model="createForm.taskRef"
              filterable
              placeholder="请选择任务实例"
              class="create-filter__task-select"
              :loading="loadingDef">
              <el-option
                v-for="item in definitions"
                :key="`${item.taskType}::${item.taskCode}`"
                :label="`${item.taskName || item.taskCode}（${item.taskType}/${item.taskCode}）`"
                :value="`${item.taskType}::${item.taskCode}`" />
            </el-select>
          </el-form-item>
          <el-form-item label="任务类型">
            <el-input :model-value="selectedDefinition?.taskType || '-'" disabled />
          </el-form-item>
          <el-form-item label="任务编码">
            <el-input :model-value="selectedDefinition?.taskCode || '-'" disabled />
          </el-form-item>
          <el-form-item label="任务名称">
            <el-input v-model="createForm.taskName" placeholder="请输入任务名称" />
          </el-form-item>
          <el-form-item label="Cron">
            <el-input v-model="createForm.cron" placeholder="请输入 Cron 表达式" />
          </el-form-item>
          <el-form-item label="运行状态">
            <el-input v-model="createForm.status" disabled placeholder="运行状态" />
          </el-form-item>
          <el-form-item label="任务参数">
            <el-input
              v-model="createForm.paramsJson"
              type="textarea"
              :autosize="{ minRows: 6, maxRows: 12 }"
              placeholder="请输入 params_json，例如 {&quot;fullSync&quot;:true}" />
          </el-form-item>
          <el-form-item label="参数 Schema">
            <el-input
              :model-value="selectedDefinition?.paramSchema || '-'"
              type="textarea"
              :autosize="{ minRows: 4, maxRows: 10 }"
              disabled />
          </el-form-item>
          <el-form-item label="备注">
            <el-input
              v-model="createForm.remark"
              type="textarea"
              :autosize="{ minRows: 2, maxRows: 6 }"
              placeholder="请输入备注" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="create-drawer__footer">
          <el-button @click="createVisible = false">关闭</el-button>
          <el-button type="primary" :loading="creating" @click="handleCreateInstance">创建任务</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import {
  listSystemTasks,
  triggerSystemTask,
  updateSystemTask,
  listTaskDefinitions,
  createTaskInstance,
  deleteTaskInstance,
  type SystemTaskDto,
  type SystemTaskQuery,
  type TaskDefinitionDto
} from '../../services/systemTask'

const DEFAULT_CREATE_CRON = '0 0 9 * * ?'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const editVisible = ref(false)
const createVisible = ref(false)
const loadingDef = ref(false)
const creating = ref(false)
const tableData = ref<SystemTaskDto[]>([])
const definitions = ref<TaskDefinitionDto[]>([])
const total = ref(0)

const query = reactive<SystemTaskQuery>({
  pageNo: 1,
  pageSize: 10,
  appName: '',
  taskType: '',
  includeInvestment: false,
  taskCode: '',
  taskName: '',
  status: ''
})

const editForm = reactive({
  id: 0,
  appName: '',
  taskCode: '',
  cron: '',
  enabled: false,
  status: 'STOPPED',
  paramsJson: '',
  paramSchema: '',
  remark: ''
})
const createForm = reactive({
  taskRef: '',
  taskName: '',
  cron: DEFAULT_CREATE_CRON,
  enabled: false,
  status: 'STOPPED',
  paramsJson: '',
  remark: ''
})

const totalPage = computed(() => {
  if (!total.value) return 1
  return Math.ceil(total.value / query.pageSize)
})

const selectedDefinition = computed(() => {
  if (!createForm.taskRef) return null
  const [taskType, taskCode] = createForm.taskRef.split('::')
  return definitions.value.find((item) => item.taskType === taskType && item.taskCode === taskCode) || null
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await listSystemTasks({
      pageNo: query.pageNo,
      pageSize: query.pageSize,
      appName: query.appName || undefined,
      taskType: query.taskType || undefined,
      includeInvestment: query.includeInvestment,
      taskCode: query.taskCode || undefined,
      taskName: query.taskName || undefined,
      status: query.status || undefined
    })
    if (res.code === 200) {
      tableData.value = res.data?.items || []
      total.value = res.data?.total || 0
      return
    }
    ElMessage.error(res.message || '获取任务列表失败')
  } catch (error: any) {
    ElMessage.error(error.message || '获取任务列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.pageNo = 1
  loadData()
}

const resetQuery = () => {
  query.pageNo = 1
  query.pageSize = 10
  query.appName = ''
  query.taskType = ''
  query.includeInvestment = false
  query.taskCode = ''
  query.taskName = ''
  query.status = ''
  loadData()
}

const handleSizeChange = (value: number) => {
  query.pageSize = value
  loadData()
}

const handleCurrentChange = (value: number) => {
  query.pageNo = value
  loadData()
}

const openEdit = (row: SystemTaskDto) => {
  editForm.id = row.id
  editForm.appName = row.appName
  editForm.taskCode = row.taskCode
  editForm.cron = row.cron
  editForm.enabled = !!row.enabled
  editForm.status = row.status || 'STOPPED'
  editForm.paramsJson = row.paramsJson || ''
  editForm.paramSchema = row.paramSchema || ''
  editForm.remark = row.remark || ''
  editVisible.value = true
}

const submitEdit = async () => {
  if (!editForm.id || !editForm.cron) {
    ElMessage.warning('请先完善 Cron')
    return
  }
  submitting.value = true
  try {
    const res = await updateSystemTask({
      id: editForm.id,
      cron: editForm.cron,
      enabled: editForm.enabled,
      status: editForm.status,
      paramsJson: editForm.paramsJson,
      remark: editForm.remark
    })
    if (res.code === 200) {
      ElMessage.success('任务已更新')
      editVisible.value = false
      await loadData()
      return
    }
    ElMessage.error(res.message || '更新任务失败')
  } catch (error: any) {
    ElMessage.error(error.message || '更新任务失败')
  } finally {
    submitting.value = false
  }
}

const toggleEnabled = async (row: SystemTaskDto) => {
  const targetEnabled = row.enabled
  const actionText = targetEnabled ? '启用' : '停用'
  try {
    const res = await updateSystemTask({
      id: row.id,
      cron: row.cron,
      enabled: targetEnabled,
      status: row.status,
      paramsJson: row.paramsJson,
      remark: row.remark
    })
    if (res.code === 200) {
      ElMessage.success(`任务已${actionText}`)
      loadData()
    } else {
      row.enabled = !targetEnabled // revert
    }
  } catch (e) {
    row.enabled = !targetEnabled // revert
  }
}

const triggerTask = async (row: SystemTaskDto) => {
  try {
    await ElMessageBox.confirm(`确认立即执行任务 ${row.taskCode} 吗？`, '立即执行', { type: 'info' })
    const res = await triggerSystemTask({
      id: row.id
    })
    if (res.code === 200) {
      ElMessage.success('触发指令已发送')
      return
    }
    ElMessage.error(res.message || '立即执行失败')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '立即执行失败')
    }
  }
}

const formatTime = (value?: string) => {
  if (!value) return '-'
  return dayjs(value).format('YYYY-MM-DD HH:mm:ss')
}

const openCreateDialog = async () => {
  createVisible.value = true
  createForm.taskRef = ''
  createForm.taskName = ''
  createForm.cron = DEFAULT_CREATE_CRON
  createForm.enabled = false
  createForm.status = 'STOPPED'
  createForm.paramsJson = ''
  createForm.remark = ''
  loadingDef.value = true
  try {
    const res = await listTaskDefinitions({})
    if (res.code === 200) {
      definitions.value = res.data || []
    } else {
      ElMessage.error(res.message || '获取任务定义失败')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '获取任务定义失败')
  } finally {
    loadingDef.value = false
  }
}

watch(
  () => createForm.taskRef,
  () => {
    const target = selectedDefinition.value
    if (!target) return
    createForm.taskName = target.taskName || target.taskCode
    createForm.cron = target.defaultCron || DEFAULT_CREATE_CRON
    createForm.enabled = target.defaultEnabled ?? false
    createForm.status = 'STOPPED'
    createForm.paramsJson = target.defaultParamsJson || ''
  }
)

const handleCreateInstance = async () => {
  if (!selectedDefinition.value) {
    ElMessage.warning('请先选择任务实例')
    return
  }
  if (!createForm.taskName || !createForm.cron) {
    ElMessage.warning('请先完善任务名称和 Cron')
    return
  }
  creating.value = true
  try {
    const res = await createTaskInstance({
      taskType: selectedDefinition.value.taskType,
      taskCode: selectedDefinition.value.taskCode,
      taskName: createForm.taskName,
      cron: createForm.cron,
      enabled: createForm.enabled,
      status: createForm.status,
      paramsJson: createForm.paramsJson,
      remark: createForm.remark
    })
    if (res.code === 200) {
      ElMessage.success('任务实例创建成功')
      createVisible.value = false
      await loadData()
      return
    }
    ElMessage.error(res.message || '创建任务实例失败')
  } catch (error: any) {
    ElMessage.error(error.message || '创建任务实例失败')
  } finally {
    creating.value = false
  }
}

const deleteTask = async (row: SystemTaskDto) => {
  try {
    await ElMessageBox.confirm(`确认删除任务实例 ${row.taskCode} 吗？`, '删除警告', { type: 'warning' })
    const res = await deleteTaskInstance({ id: row.id })
    if (res.code === 200) {
      ElMessage.success('任务实例已删除')
      await loadData()
      return
    }
    ElMessage.error(res.message || '删除任务实例失败')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除任务实例失败')
    }
  }
}

const goToInvestment = (row: SystemTaskDto) => {
  const investmentId = row.id
  router.push({ path: '/investment/detail', query: { id: investmentId } })
}

const goToDetail = (row: SystemTaskDto) => {
  router.push({ path: '/task/detail', query: { id: row.id } })
}

const formatParamsPreview = (value?: string) => {
  if (!value) return '未配置任务参数'
  return value
}

const getStatusType = (status?: string) => {
  switch (status) {
    case 'RUNNING':
      return 'success'
    case 'STOPPED':
      return 'info'
    default:
      return 'warning'
  }
}

const getStatusLabel = (status?: string) => {
  switch (status) {
    case 'RUNNING':
      return '执行中'
    case 'STOPPED':
      return '已停止'
    default:
      return status || '未知'
  }
}

const getResultType = (result?: string) => {
  switch (result) {
    case 'SUCCESS':
      return 'success'
    case 'FAILED':
      return 'danger'
    default:
      return 'info'
  }
}

const getResultLabel = (result?: string) => {
  switch (result) {
    case 'SUCCESS':
      return '成功'
    case 'FAILED':
      return '失败'
    default:
      return '未执行'
  }
}

onMounted(() => {
  loadData()
})
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

.page-header__content {
  display: flex;
  flex-direction: column;
}

.page-header__meta,
.search-meta,
.table-toolbar__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  font-size: 13px;
  color: #64748b;
}

.page-header__meta span,
.search-meta span,
.table-toolbar__meta span {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: #f5f7fa;
}

.title {
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
}

.subtitle {
  margin-top: 8px;
  font-size: 14px;
  line-height: 22px;
  color: #475569;
}

.search-card,
.table-card {
  border: none;
  border-radius: 18px;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
}

.search-card :deep(.el-card__body),
.table-card :deep(.el-card__body) {
  padding: 24px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.section-subtitle {
  margin-top: 4px;
  font-size: 13px;
  line-height: 20px;
  color: #64748b;
}

.search-form {
  margin: 16px 0;
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
.search-grid :deep(.el-select) {
  width: 100%;
}

.search-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.full {
  width: 100%;
}

.full :deep(.el-table__header th) {
  background: #f8fafc;
}

.full :deep(.el-table__cell) {
  padding-top: 14px;
  padding-bottom: 14px;
}

.primary-cell {
  font-size: 15px;
  font-weight: 600;
  line-height: 22px;
  color: #1e293b;
}

.secondary-cell {
  margin-top: 4px;
  font-size: 13px;
  line-height: 20px;
  color: #64748b;
}

.secondary-cell--clamp {
  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-all;
}

.code-cell {
  padding: 8px 10px;
  border-radius: 10px;
  background: #f8fafc;
  color: #334155;
  font-family: Consolas, Monaco, monospace;
  font-size: 13px;
  line-height: 20px;
}

.pagination {
  margin-top: 24px;
  padding-top: 20px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid #f0f2f5;
}

.edit-form :deep(.el-textarea__inner) {
  font-family: Consolas, Monaco, monospace;
}

.create-drawer {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.create-filter__task-select {
  width: 100%;
}

.create-drawer__footer {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 1024px) {
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

  .search-actions,
  .pagination {
    justify-content: stretch;
  }

  .search-actions :deep(.el-button) {
    flex: 1;
  }

}
</style>
