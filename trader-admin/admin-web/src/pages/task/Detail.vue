<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__content">
        <div class="title">任务详情</div>
        <div class="subtitle">查看任务当前配置、最近执行结果和该任务的历史执行记录</div>
      </div>
      <div class="page-header__actions">
        <el-button @click="router.push('/task/manage')">返回任务管理</el-button>
      </div>
    </div>

    <el-card class="info-card" v-loading="loadingTask">
      <template v-if="taskDetail">
        <div class="section-title">基础信息</div>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="任务名称">{{ taskDetail.taskName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="任务类型">{{ taskDetail.taskType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="任务编码">{{ taskDetail.taskCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="应用">{{ taskDetail.appName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="是否启用">
            <el-tag :type="taskDetail.enabled ? 'success' : 'info'">{{ taskDetail.enabled ? '启用' : '停用' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="运行状态">
            <el-tag :type="getStatusType(taskDetail.status)">{{ getStatusLabel(taskDetail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="最近结果">
            <el-tag :type="getResultType(taskDetail.result)">{{ getResultLabel(taskDetail.result) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Cron">{{ taskDetail.cron || '-' }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatTime(taskDetail.updatedAt) }}</el-descriptions-item>
        </el-descriptions>
        <div class="params-panel">
          <div class="section-subtitle">任务参数</div>
          <pre class="code-block">{{ taskDetail.paramsJson || '未配置任务参数' }}</pre>
        </div>
      </template>
      <el-empty v-else description="未找到任务详情" />
    </el-card>

    <el-card class="history-card">
      <div class="table-toolbar">
        <div>
          <div class="section-title">执行历史</div>
          <div class="section-subtitle">按当前任务自动过滤执行历史，可查看 traceId、耗时和备注</div>
        </div>
        <div class="table-toolbar__meta">
          <span>共 {{ total }} 条</span>
        </div>
      </div>

      <el-table :data="logs" v-loading="loadingLogs" stripe class="full">
        <el-table-column label="开始时间" min-width="180">
          <template #default="{ row }">{{ formatTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" min-width="180">
          <template #default="{ row }">{{ formatTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="traceId" min-width="180">
          <template #default="{ row }">{{ row.traceId || '-' }}</template>
        </el-table-column>
        <el-table-column label="执行耗时" width="120" align="center">
          <template #default="{ row }">{{ row.executionMs ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="备注 / 错误" min-width="260">
          <template #default="{ row }">
            <div class="secondary-cell secondary-cell--clamp">{{ row.remark || row.errorMsg || row.content || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="showLogDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.pageNo"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadLogs"
          @current-change="loadLogs"
        />
      </div>
    </el-card>

    <el-drawer v-model="detailVisible" title="执行详情" size="50%">
      <div v-if="currentLog" class="drawer-content">
        <div class="section-title">{{ currentLog.taskName || '-' }}</div>
        <div class="detail-grid">
          <div class="detail-card">
            <div class="detail-label">traceId</div>
            <div class="detail-value">{{ currentLog.traceId || '-' }}</div>
          </div>
          <div class="detail-card">
            <div class="detail-label">执行状态</div>
            <div class="detail-value">{{ getStatusLabel(currentLog.status) }}</div>
          </div>
          <div class="detail-card">
            <div class="detail-label">开始时间</div>
            <div class="detail-value">{{ formatTime(currentLog.startTime) }}</div>
          </div>
          <div class="detail-card">
            <div class="detail-label">结束时间</div>
            <div class="detail-value">{{ formatTime(currentLog.endTime) }}</div>
          </div>
        </div>

        <div v-if="currentLog.remark" class="detail-section">
          <div class="detail-section__title">备注</div>
          <pre class="code-block">{{ currentLog.remark }}</pre>
        </div>
        <div v-if="currentLog.content" class="detail-section">
          <div class="detail-section__title">执行内容</div>
          <pre class="code-block">{{ currentLog.content }}</pre>
        </div>
        <div v-if="currentLog.errorMsg" class="detail-section">
          <div class="detail-section__title">错误信息</div>
          <el-alert :title="currentLog.errorMsg" type="error" :closable="false" show-icon />
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { getSystemTaskDetail, type SystemTaskDto } from '../../services/systemTask'
import { getTaskLogDetail, listTaskLogs, type TaskLogDto, type TaskLogQuery } from '../../services/taskLog'

const route = useRoute()
const router = useRouter()
const taskId = Number(route.query.id || 0)

const loadingTask = ref(false)
const loadingLogs = ref(false)
const taskDetail = ref<SystemTaskDto | null>(null)
const logs = ref<TaskLogDto[]>([])
const total = ref(0)
const detailVisible = ref(false)
const currentLog = ref<TaskLogDto | null>(null)

const query = reactive<TaskLogQuery>({
  pageNo: 1,
  pageSize: 10,
  taskCode: '',
  taskName: '',
  status: '',
  startTime: undefined,
  endTime: undefined
})

const loadTaskDetail = async () => {
  if (!taskId) {
    ElMessage.error('缺少任务 ID')
    return
  }
  loadingTask.value = true
  try {
    const res = await getSystemTaskDetail(taskId)
    if (res.code === 200) {
      taskDetail.value = res.data || null
      query.taskCode = res.data?.taskCode || ''
      query.taskName = res.data?.taskName || ''
      return
    }
    ElMessage.error(res.message || '获取任务详情失败')
  } catch (error: any) {
    ElMessage.error(error.message || '获取任务详情失败')
  } finally {
    loadingTask.value = false
  }
}

const loadLogs = async () => {
  loadingLogs.value = true
  try {
    const res = await listTaskLogs(query)
    if (res.code === 200) {
      logs.value = res.data?.items || []
      total.value = res.data?.total || 0
      return
    }
    ElMessage.error(res.message || '获取任务历史失败')
  } catch (error: any) {
    ElMessage.error(error.message || '获取任务历史失败')
  } finally {
    loadingLogs.value = false
  }
}

const showLogDetail = async (row: TaskLogDto) => {
  try {
    const res = await getTaskLogDetail(row.id)
    if (res.code === 200) {
      currentLog.value = res.data || row
      detailVisible.value = true
      return
    }
    ElMessage.error(res.message || '获取执行详情失败')
  } catch (error: any) {
    ElMessage.error(error.message || '获取执行详情失败')
  }
}

const formatTime = (value?: string) => {
  if (!value) return '-'
  return dayjs(value).format('YYYY-MM-DD HH:mm:ss')
}

const getStatusType = (status?: string) => {
  switch (status) {
    case 'RUNNING':
      return 'warning'
    case 'SUCCESS':
      return 'success'
    case 'FAILED':
      return 'danger'
    case 'STOPPED':
      return 'info'
    default:
      return 'info'
  }
}

const getStatusLabel = (status?: string) => {
  switch (status) {
    case 'RUNNING':
      return '执行中'
    case 'SUCCESS':
      return '成功'
    case 'FAILED':
      return '失败'
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

onMounted(async () => {
  await loadTaskDetail()
  await loadLogs()
})
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header,
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

.info-card,
.history-card {
  border: none;
  border-radius: 18px;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
}

.info-card :deep(.el-card__body),
.history-card :deep(.el-card__body) {
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

.params-panel,
.detail-section {
  margin-top: 16px;
}

.code-block {
  margin: 8px 0 0;
  padding: 12px;
  border-radius: 12px;
  background: #f8fafc;
  white-space: pre-wrap;
  word-break: break-all;
}

.table-toolbar__meta {
  display: flex;
  gap: 12px;
  color: #64748b;
  font-size: 13px;
}

.full {
  width: 100%;
  margin-top: 16px;
}

.secondary-cell {
  font-size: 13px;
  line-height: 20px;
  color: #64748b;
}

.secondary-cell--clamp {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-all;
}

.pagination {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

.drawer-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.detail-card {
  padding: 16px;
  border-radius: 12px;
  background: #f8fafc;
}

.detail-label {
  font-size: 12px;
  color: #64748b;
}

.detail-value {
  margin-top: 6px;
  font-size: 14px;
  color: #1e293b;
}
</style>
