<template>
  <div class="page-container" :class="{ 'page-container--embedded': embedded }">
    <div v-if="!embedded" class="page-header">
      <div class="page-header__content">
        <div class="title">消息日志</div>
        <div class="subtitle">统一查看消息渠道、消息内容、接收人和发送状态</div>
      </div>
      <div class="page-header__meta">
        <el-tag effect="plain" type="primary">消息中心</el-tag>
        <el-tag effect="plain" type="info">共 {{ total }} 条</el-tag>
      </div>
    </div>

    <el-card class="search-card">
      <div class="search-header">
        <div>
          <div class="section-title">筛选条件</div>
          <div class="section-subtitle">支持按消息类型、消息渠道和发送状态快速过滤日志记录</div>
        </div>
        <div class="search-meta">
          <span>当前页 {{ tableData.length }} 条</span>
          <span v-if="query.type">类型 {{ query.type }}</span>
          <span v-if="query.channel">渠道 {{ query.channel }}</span>
          <span v-if="query.status">状态 {{ statusText(query.status) }}</span>
        </div>
      </div>

      <el-form :model="query" class="search-form">
        <div class="search-grid search-grid--compact">
          <el-form-item label="消息类型">
            <el-input v-model="query.type" placeholder="请输入消息类型" clearable @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="消息渠道">
            <el-input v-model="query.channel" placeholder="请输入消息渠道" clearable @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="发送状态">
            <el-select v-model="query.status" placeholder="请选择状态" clearable @change="handleSearch">
              <el-option label="成功" value="SUCCESS" />
              <el-option label="失败" value="FAIL" />
            </el-select>
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
          <div class="section-title">消息记录</div>
          <div class="section-subtitle">按统一信息层级展示消息渠道、接收对象、消息内容和发送结果</div>
        </div>
        <div class="table-toolbar__meta">
          <span>分页 {{ query.pageNo }} / {{ totalPage }}</span>
          <span>每页 {{ query.pageSize }} 条</span>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
        class="full"
        :class="{ 'full--embedded': embedded }"
        @row-click="handleRowClick">
        <el-table-column label="消息" min-width="260">
          <template #default="{ row }">
            <div class="primary-cell">{{ row.title || '-' }}</div>
            <div v-if="!embedded" class="secondary-cell">{{ row.type || '未分类消息' }}</div>
            <div v-else class="secondary-cell secondary-cell--clamp">{{ row.content || '暂无消息内容' }}</div>
          </template>
        </el-table-column>
        <el-table-column v-if="!embedded" label="渠道与接收人" min-width="200">
          <template #default="{ row }">
            <div class="cell-tags">
              <el-tag effect="plain" type="info" size="small">{{ row.channel || '-' }}</el-tag>
              <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
            </div>
            <div class="secondary-cell">{{ row.recipient || '未填写接收人' }}</div>
          </template>
        </el-table-column>
        <el-table-column v-if="!embedded" label="消息内容" min-width="320">
          <template #default="{ row }">
            <el-tooltip effect="light" placement="top-start" popper-class="message-content-tooltip">
              <template #content>
                <div class="message-tooltip-content">{{ row.content || '-' }}</div>
              </template>
              <div class="content-cell content-cell--clamp">{{ row.content || '-' }}</div>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column v-if="!embedded" label="时间信息" min-width="220">
          <template #default="{ row }">
            <div class="primary-cell">创建 {{ formatDate(row.createdAt) }}</div>
            <div class="secondary-cell">更新 {{ formatDate(row.updatedAt) }}</div>
          </template>
        </el-table-column>
        <el-table-column v-else label="状态" width="88" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="embedded" label="时间" width="150" align="right">
          <template #default="{ row }">
            <div class="embedded-time">{{ formatCompactDate(row.createdAt) }}</div>
          </template>
        </el-table-column>
        <el-table-column v-if="!embedded" label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row.id)">查看详情</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无消息日志" />
        </template>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.pageNo"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-drawer v-model="drawerVisible" title="消息详情" size="56%" class="detail-drawer">
      <div v-loading="detailLoading" class="drawer-content">
        <div v-if="currentDetail" class="detail-body">
          <div class="drawer-header">
            <div>
              <div class="drawer-title">{{ currentDetail.title || '-' }}</div>
              <div class="drawer-subtitle">类型 {{ currentDetail.type || '未分类消息' }}</div>
            </div>
            <el-tag :type="statusTagType(currentDetail.status)" size="large">
              {{ statusText(currentDetail.status) }}
            </el-tag>
          </div>

          <div class="detail-grid">
            <div class="detail-card">
              <div class="detail-label">消息渠道</div>
              <div class="detail-value">{{ currentDetail.channel || '-' }}</div>
            </div>
            <div class="detail-card">
              <div class="detail-label">接收人</div>
              <div class="detail-value">{{ currentDetail.recipient || '未填写接收人' }}</div>
            </div>
            <div class="detail-card">
              <div class="detail-label">创建时间</div>
              <div class="detail-value">{{ formatDate(currentDetail.createdAt) }}</div>
            </div>
            <div class="detail-card">
              <div class="detail-label">更新时间</div>
              <div class="detail-value">{{ formatDate(currentDetail.updatedAt) }}</div>
            </div>
          </div>

          <div class="detail-section">
            <div class="detail-section__header">
              <div class="detail-section__title">消息内容</div>
              <div class="detail-section__hint">Markdown 文档视图</div>
            </div>
            <div class="detail-markdown-shell">
              <div class="detail-markdown" v-html="detailContentHtml"></div>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import DOMPurify from 'dompurify'
import { marked } from 'marked'
import { getMsgPushLogDetail, listMsgPushLogs } from '../../services/log'
import type { MsgPushLogDto, MsgPushLogQuery } from '../../services/log'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const { embedded = false } = defineProps<{
  embedded?: boolean
}>()

const loading = ref(false)
const detailLoading = ref(false)
const tableData = ref<MsgPushLogDto[]>([])
const total = ref(0)
const drawerVisible = ref(false)
const currentDetail = ref<MsgPushLogDto | null>(null)

const query = reactive<MsgPushLogQuery>({
  pageNo: 1,
  pageSize: 20,
  type: '',
  channel: '',
  status: ''
})

const totalPage = computed(() => {
  if (!total.value) return 1
  return Math.ceil(total.value / query.pageSize)
})

const detailContentHtml = computed(() => renderMarkdown(currentDetail.value?.content))

const loadData = async () => {
  loading.value = true
  try {
    const res = await listMsgPushLogs(query)
    if (res.code === 200) {
      tableData.value = res.data.items
      total.value = res.data.total
    } else {
      ElMessage.error(res.message || '加载失败')
    }
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.pageNo = 1
  loadData()
}

const resetQuery = () => {
  query.type = ''
  query.channel = ''
  query.status = ''
  handleSearch()
}

const handleSizeChange = (val: number) => {
  query.pageSize = val
  loadData()
}

const handleCurrentChange = (val: number) => {
  query.pageNo = val
  loadData()
}

const showDetail = async (id: number) => {
  drawerVisible.value = true
  currentDetail.value = null
  detailLoading.value = true
  try {
    const res = await getMsgPushLogDetail(id)
    if (res.code === 200) {
      currentDetail.value = res.data
    } else {
      ElMessage.error(res.message || '加载详情失败')
      drawerVisible.value = false
    }
  } catch (error) {
    ElMessage.error('加载详情失败')
    drawerVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

const handleRowClick = (row: MsgPushLogDto) => {
  if (!embedded) return
  showDetail(row.id)
}

const formatDate = (date: string) => {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

const formatCompactDate = (date: string) => {
  if (!date) return '-'
  return dayjs(date).format('MM-DD HH:mm')
}

const renderMarkdown = (content?: string) => {
  const source = content?.trim() || '暂无消息内容'
  return DOMPurify.sanitize(marked.parse(source) as string)
}

const statusText = (status?: string) => {
  if (status === 'SUCCESS') return '成功'
  if (status === 'FAIL') return '失败'
  return status || '未知'
}

const statusTagType = (status?: string) => {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAIL') return 'danger'
  return 'info'
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

.page-container--embedded {
  gap: 12px;
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

.page-container--embedded .search-card,
.page-container--embedded .table-card {
  border-radius: 12px;
  box-shadow: none;
}

.search-card :deep(.el-card__body),
.table-card :deep(.el-card__body) {
  padding: 24px;
}

.page-container--embedded .search-card :deep(.el-card__body),
.page-container--embedded .table-card :deep(.el-card__body) {
  padding: 16px;
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

.search-grid--compact :deep(.el-form-item) {
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
  line-clamp: 1;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.content-cell {
  font-size: 13px;
  line-height: 20px;
  color: #606266;
  word-break: break-word;
  cursor: pointer;
}

.content-cell--clamp {
  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.cell-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pagination {
  margin-top: 24px;
  padding-top: 20px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid #f0f2f5;
}

.full--embedded :deep(.el-table__row) {
  cursor: pointer;
}

.full--embedded :deep(.el-table__cell) {
  padding-top: 12px;
  padding-bottom: 12px;
}

.full--embedded .primary-cell {
  font-size: 14px;
}

.embedded-time {
  font-size: 12px;
  color: #64748b;
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

.detail-drawer :deep(.el-drawer__header) {
  margin-bottom: 0;
  padding: 20px 24px 0;
}

.detail-drawer :deep(.el-drawer__body) {
  padding: 20px 24px 24px;
}

.drawer-content {
  min-height: 240px;
}

.detail-body {
  display: flex;
  flex-direction: column;
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
  background: linear-gradient(180deg, #fcfcfd 0%, #f7f8fa 100%);
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
  padding: 18px;
}

.detail-section__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.detail-section__title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.detail-section__hint {
  padding: 4px 10px;
  border-radius: 999px;
  background: #eef2ff;
  font-size: 12px;
  color: #6366f1;
}

.detail-markdown-shell {
  padding: 24px 28px;
  border: 1px solid #ebeef5;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.detail-markdown {
  max-width: 760px;
  margin: 0 auto;
  color: #303133;
  line-height: 1.7;
  word-break: break-word;
  font-size: 14px;
}

.detail-markdown :deep(h1),
.detail-markdown :deep(h2),
.detail-markdown :deep(h3),
.detail-markdown :deep(h4) {
  margin: 24px 0 12px;
  line-height: 1.4;
  font-weight: 700;
  color: #1f2937;
}

.detail-markdown :deep(h1:first-child),
.detail-markdown :deep(h2:first-child),
.detail-markdown :deep(h3:first-child),
.detail-markdown :deep(h4:first-child) {
  margin-top: 0;
}

.detail-markdown :deep(h1) {
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
  font-size: 28px;
}

.detail-markdown :deep(h2) {
  padding-bottom: 8px;
  border-bottom: 1px solid #eef2f7;
  font-size: 22px;
}

.detail-markdown :deep(h3) {
  font-size: 18px;
}

.detail-markdown :deep(h4) {
  font-size: 16px;
}

.detail-markdown :deep(p),
.detail-markdown :deep(ul),
.detail-markdown :deep(ol),
.detail-markdown :deep(blockquote),
.detail-markdown :deep(pre),
.detail-markdown :deep(table),
.detail-markdown :deep(hr) {
  margin: 0 0 12px;
}

.detail-markdown :deep(ul),
.detail-markdown :deep(ol) {
  padding-left: 22px;
}

.detail-markdown :deep(li + li) {
  margin-top: 6px;
}

.detail-markdown :deep(pre) {
  overflow: auto;
  padding: 16px 18px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #111827;
  color: #f9fafb;
}

.detail-markdown :deep(code) {
  font-family: Consolas, Monaco, monospace;
  font-size: 13px;
}

.detail-markdown :deep(:not(pre) > code) {
  padding: 2px 6px;
  border-radius: 6px;
  background: #f3f4f6;
  color: #b42318;
}

.detail-markdown :deep(blockquote) {
  padding: 10px 14px;
  border-left: 4px solid #93c5fd;
  border-radius: 0 10px 10px 0;
  background: #eff6ff;
  color: #475467;
}

.detail-markdown :deep(a) {
  color: #2563eb;
  text-decoration: none;
}

.detail-markdown :deep(a:hover) {
  text-decoration: underline;
}

.detail-markdown :deep(strong) {
  color: #111827;
  font-weight: 700;
}

.detail-markdown :deep(table) {
  width: 100%;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  border-collapse: separate;
  border-spacing: 0;
}

.detail-markdown :deep(th),
.detail-markdown :deep(td) {
  padding: 10px 12px;
  border-bottom: 1px solid #e5e7eb;
  text-align: left;
}

.detail-markdown :deep(th) {
  background: #f8fafc;
  font-weight: 600;
  color: #1f2937;
}

.detail-markdown :deep(tr:last-child td) {
  border-bottom: none;
}

.detail-markdown :deep(hr) {
  border: none;
  border-top: 1px solid #e5e7eb;
}

:deep(.message-content-tooltip) {
  max-width: 420px;
  max-height: 320px;
  overflow: auto;
}

:deep(.message-tooltip-content) {
  font-size: 13px;
  line-height: 20px;
  color: #606266;
  white-space: pre-wrap;
  word-break: break-word;
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

  .detail-markdown-shell {
    padding: 18px 16px;
  }

  .detail-section__header {
    flex-direction: column;
    align-items: flex-start;
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
