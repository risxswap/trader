<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__content">
        <div class="title">任务日志</div>
        <div class="subtitle">聚合查看任务运行状态、执行时长、异常信息和执行内容</div>
      </div>
      <div class="page-header__meta">
        <el-tag effect="plain" type="primary">日志管理</el-tag>
        <el-tag effect="plain" type="warning">任务日志</el-tag>
        <el-tag effect="plain" type="info">共 {{ total }} 条</el-tag>
      </div>
    </div>

    <el-card class="search-card">
      <div class="search-header">
        <div>
          <div class="section-title">筛选条件</div>
          <div class="section-subtitle">支持按任务名称、执行状态和执行时间区间快速定位记录</div>
        </div>
        <div class="search-meta">
          <span>当前页 {{ tableData.length }} 条</span>
          <span v-if="query.taskName">任务 {{ query.taskName }}</span>
          <span v-if="query.status">状态 {{ getStatusLabel(query.status) }}</span>
          <span v-if="dateRangeText">{{ dateRangeText }}</span>
        </div>
      </div>

      <el-form :model="query" class="search-form">
        <div class="search-grid search-grid--task">
          <el-form-item label="任务名称">
            <el-input v-model="query.taskName" placeholder="请输入任务名称" clearable @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="执行状态">
            <el-select v-model="query.status" placeholder="请选择状态" clearable @change="handleSearch">
              <el-option label="运行中" value="RUNNING" />
              <el-option label="成功" value="SUCCESS" />
              <el-option label="失败" value="FAILED" />
            </el-select>
          </el-form-item>
          <el-form-item label="执行时间">
            <el-date-picker
              v-model="dateRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              value-format="YYYY-MM-DDTHH:mm:ss.SSSZ"
              @change="handleDateChange"
            />
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
          <div class="section-title">任务记录</div>
          <div class="section-subtitle">突出展示任务分组、执行状态、时间区间和异常结果</div>
        </div>
        <div class="table-toolbar__meta">
          <span>分页 {{ query.pageNo }} / {{ totalPage }}</span>
          <span>每页 {{ query.pageSize }} 条</span>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe class="full">
        <el-table-column label="任务信息" min-width="220">
          <template #default="{ row }">
            <div class="primary-cell">{{ row.taskName || '-' }}</div>
            <div class="secondary-cell">分组 {{ row.taskGroup || '默认分组' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="执行状态" width="140" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="执行时间" min-width="240">
          <template #default="{ row }">
            <div class="primary-cell">{{ formatTime(row.startTime) }}</div>
            <div class="secondary-cell">结束 {{ formatTime(row.endTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="执行结果" min-width="260">
          <template #default="{ row }">
            <div class="primary-cell">{{ getExecutionText(row.executionMs) }}</div>
            <div class="secondary-cell secondary-cell--clamp">
              {{ row.errorMsg || row.content || '本次任务未返回额外执行内容' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="记录时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无任务日志" />
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

    <el-drawer v-model="drawerVisible" title="任务详情" size="50%">
      <div v-if="currentDetail" class="drawer-content">
        <div class="drawer-header">
          <div>
            <div class="drawer-title">{{ currentDetail.taskName || '-' }}</div>
            <div class="drawer-subtitle">任务分组 {{ currentDetail.taskGroup || '默认分组' }}</div>
          </div>
          <el-tag :type="getStatusType(currentDetail.status)" size="large">
            {{ getStatusLabel(currentDetail.status) }}
          </el-tag>
        </div>

        <div class="detail-grid">
          <div class="detail-card">
            <div class="detail-label">开始时间</div>
            <div class="detail-value">{{ formatTime(currentDetail.startTime) }}</div>
          </div>
          <div class="detail-card">
            <div class="detail-label">结束时间</div>
            <div class="detail-value">{{ formatTime(currentDetail.endTime) }}</div>
          </div>
          <div class="detail-card">
            <div class="detail-label">执行耗时</div>
            <div class="detail-value">{{ getExecutionText(currentDetail.executionMs) }}</div>
          </div>
          <div class="detail-card">
            <div class="detail-label">记录时间</div>
            <div class="detail-value">{{ formatTime(currentDetail.createdAt) }}</div>
          </div>
        </div>

        <div v-if="currentDetail.content" class="detail-section">
          <div class="detail-section__title">执行内容</div>
          <pre class="detail-pre">{{ currentDetail.content }}</pre>
        </div>

        <div v-if="currentDetail.errorMsg" class="detail-section">
          <div class="detail-section__title">错误信息</div>
          <el-alert :title="currentDetail.errorMsg" type="error" :closable="false" show-icon />
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { listTaskLogs, type TaskLogQuery, type TaskLogDto } from '../../services/taskLog'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const loading = ref(false)
const tableData = ref<TaskLogDto[]>([])
const total = ref(0)
const dateRange = ref<[string, string] | null>(null)

const query = ref<TaskLogQuery>({
  pageNo: 1,
  pageSize: 10,
  taskName: '',
  status: '',
  startTime: undefined,
  endTime: undefined
})

const drawerVisible = ref(false)
const currentDetail = ref<TaskLogDto | null>(null)

const totalPage = computed(() => {
  if (!total.value) return 1
  return Math.ceil(total.value / query.value.pageSize)
})

const dateRangeText = computed(() => {
  if (!dateRange.value?.length) return ''
  return `${formatTime(dateRange.value[0])} 至 ${formatTime(dateRange.value[1])}`
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await listTaskLogs(query.value)
    if (res.code === 200) {
      tableData.value = res.data?.items || []
      total.value = res.data?.total || 0
    } else {
      ElMessage.error(res.message || '获取列表失败')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '获取列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.value.pageNo = 1
  loadData()
}

const resetQuery = () => {
  query.value = {
    pageNo: 1,
    pageSize: 10,
    taskName: '',
    status: '',
    startTime: undefined,
    endTime: undefined
  }
  dateRange.value = null
  loadData()
}

const handleDateChange = (val: [string, string] | null) => {
  if (val && val.length === 2) {
    query.value.startTime = val[0]
    query.value.endTime = val[1]
  } else {
    query.value.startTime = undefined
    query.value.endTime = undefined
  }
}

const handleSizeChange = (val: number) => {
  query.value.pageSize = val
  loadData()
}

const handleCurrentChange = (val: number) => {
  query.value.pageNo = val
  loadData()
}

const showDetail = (row: TaskLogDto) => {
  currentDetail.value = row
  drawerVisible.value = true
}

const formatTime = (time: string | undefined) => {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

const getStatusType = (status: string) => {
  switch (status) {
    case 'SUCCESS':
      return 'success'
    case 'FAILED':
      return 'danger'
    case 'RUNNING':
      return 'warning'
    default:
      return 'info'
  }
}

const getStatusLabel = (status?: string) => {
  switch (status) {
    case 'SUCCESS':
      return '成功'
    case 'FAILED':
      return '失败'
    case 'RUNNING':
      return '运行中'
    default:
      return status || '未知'
  }
}

const getExecutionText = (executionMs?: number) => {
  if (executionMs === undefined || executionMs === null) return '耗时 -'
  return `耗时 ${executionMs} ms`
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
.table-toolbar,
.drawer-header {
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

.page-header__content {
  display: flex;
  flex-direction: column;
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
  border: none;
  border-radius: 18px;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
}

.search-card :deep(.el-card__body),
.table-card :deep(.el-card__body) {
  padding: 24px;
}

.search-header,
.table-toolbar {
  
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.section-subtitle {
  margin-top: 4px;
  font-size: 13px;
  line-height: 20px;
  color: #64748b;
}

.search-form {
  
  margin-bottom: 16px;
}

.search-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px 16px;
}

.search-grid :deep(.el-form-item) {
  margin-bottom: 0;
}

.search-grid--task :deep(.el-date-editor.el-input__wrapper),
.search-grid--task :deep(.el-date-editor.el-range-editor.el-input__wrapper),
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

.full :deep(.el-table__empty-block) {
  min-height: 220px;
}

.primary-cell {
  font-size: 15px;
  font-weight: 600;
  line-height: 22px;
  color: #303133;
}

.secondary-cell {
  margin-top: 4px;
  font-size: 13px;
  line-height: 20px;
  color: #909399;
}

.secondary-cell--clamp {
  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.pagination {
  margin-top: 24px;
  padding-top: 20px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid #f0f2f5;
}

.drawer-content {
  padding: 0 8px 20px;
}

.drawer-content :deep(.el-alert) {
  border-radius: 10px;
}

.drawer-title {
  font-size: 18px;
  font-weight: 600;
  line-height: 26px;
  color: #303133;
}

.drawer-subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: #909399;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 20px;
}

.detail-card,
.detail-section {
  border: 1px solid #ebeef5;
  border-radius: 12px;
  background: #fafafa;
}

.detail-card {
  padding: 16px;
}

.detail-label {
  font-size: 12px;
  color: #909399;
}

.detail-value {
  margin-top: 8px;
  font-size: 14px;
  font-weight: 600;
  line-height: 22px;
  color: #303133;
}

.detail-section {
  margin-top: 20px;
  padding: 16px;
}

.detail-section__title {
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.detail-pre {
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 10px;
  font-family: Consolas, Monaco, monospace;
  white-space: pre-wrap;
  word-wrap: break-word;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
}

.search-meta span,
.table-toolbar__meta span {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: #f5f7fa;
}

@media (max-width: 768px) {
  .page-header,
  .search-header,
  .table-toolbar,
  .drawer-header {
    flex-direction: column;
  }

  .search-grid,
  .detail-grid {
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
