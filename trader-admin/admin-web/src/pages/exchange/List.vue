<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__content">
        <div class="title">交易所列表</div>
        <div class="subtitle">统一查看交易所基础档案、时区配置与更新时间信息</div>
      </div>
      <div class="page-header__meta">
        <el-tag effect="plain" type="info">共 {{ total }} 条</el-tag>
      </div>
    </div>

    <el-card class="search-card">
      <div class="search-header">
        <div>
          <div class="section-title">筛选条件</div>
          <div class="section-subtitle">支持按交易所代码或名称快速检索基础档案</div>
        </div>
        <div class="search-meta">
          <span>当前页 {{ rows.length }} 条</span>
          <span v-if="keyword">关键词 {{ keyword }}</span>
        </div>
      </div>

      <el-form class="search-form">
        <div class="search-grid search-grid--single">
          <el-form-item label="关键字">
            <el-input v-model="keyword" placeholder="请输入代码或名称" clearable @keyup.enter="load" />
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
          <div class="section-title">交易所档案</div>
          <div class="section-subtitle">按基础标识、时区信息和维护时间组织列表层级</div>
        </div>
        <div class="table-toolbar__meta">分页 {{ pageNo }} / {{ totalPage }}</div>
      </div>

      <el-table :data="rows" v-loading="loading" class="full" stripe>
        <el-table-column label="交易所信息" min-width="240">
          <template #default="{ row }">
            <div class="primary-cell">{{ row.name || '-' }}</div>
            <div class="secondary-cell">{{ row.code || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="时区配置" min-width="200">
          <template #default="{ row }">
            <div class="primary-cell">{{ row.timezone || '-' }}</div>
            <div class="secondary-cell">{{ row.timezone ? '系统时区' : '未配置时区' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="时间信息" min-width="220">
          <template #default="{ row }">
            <div class="primary-cell">{{ formatDateTime(row.updatedAt) }}</div>
            <div class="secondary-cell">创建于 {{ formatDateTime(row.createdAt) }}</div>
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
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { listExchanges, type ExchangeDto } from '../../services/basic'
import type { PageRes } from '../../types'

const keyword = ref('')
const loading = ref(false)
const rows = ref<ExchangeDto[]>([])
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)

const totalPage = computed(() => {
  if (!total.value) return 1
  return Math.ceil(total.value / pageSize.value)
})

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  const formatted = dayjs(value)
  return formatted.isValid() ? formatted.format('YYYY-MM-DD HH:mm:ss') : value
}

const resetSearch = () => {
  keyword.value = ''
  pageNo.value = 1
  load()
}

const load = async () => {
  loading.value = true
  try {
    const res = await listExchanges({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      keyword: keyword.value
    })

    if (res.code === 200 && res.data) {
      const data = res.data as PageRes<ExchangeDto>
      rows.value = data.items
      total.value = data.total
    } else {
      rows.value = []
      total.value = 0
    }
  } finally {
    loading.value = false
  }
}

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
}

.page-header__content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.page-header__meta {
  display: flex;
  align-items: center;
}

.title {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.subtitle {
  font-size: 14px;
  line-height: 22px;
  color: #606266;
}

.search-card,
.table-card {
  border-radius: 16px;
}

.search-header,
.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
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
.table-toolbar__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  font-size: 13px;
  color: #909399;
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

.search-grid--single {
  grid-template-columns: minmax(0, 360px);
}

.search-grid :deep(.el-form-item),
.search-grid--single :deep(.el-form-item) {
  margin-bottom: 0;
}

.search-grid :deep(.el-input),
.search-grid :deep(.el-select),
.search-grid :deep(.el-date-editor),
.search-grid :deep(.el-input-number),
.search-grid--single :deep(.el-input),
.search-grid--single :deep(.el-select),
.search-grid--single :deep(.el-date-editor),
.search-grid--single :deep(.el-input-number) {
  width: 100%;
}

.search-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.full {
  width: 100%;
}

.primary-cell {
  font-size: 14px;
  font-weight: 600;
  line-height: 22px;
  color: #303133;
}

.secondary-cell {
  margin-top: 4px;
  font-size: 12px;
  line-height: 18px;
  color: #909399;
}

.pager {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .page-header,
  .search-header,
  .table-toolbar {
    flex-direction: column;
  }

  .search-grid,
  .search-grid--single {
    grid-template-columns: 1fr;
  }

  .search-actions,
  .pager {
    justify-content: stretch;
  }

  .search-actions :deep(.el-button) {
    flex: 1;
  }

  .page-header__meta {
    width: 100%;
  }
}
</style>
