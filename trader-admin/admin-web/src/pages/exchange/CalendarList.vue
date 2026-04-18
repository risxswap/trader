<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__content">
        <div class="title">交易日历</div>
        <div class="subtitle">统一查看交易所开休市状态、上一交易日与记录维护时间</div>
      </div>
      <div class="page-header__meta">
        <el-tag effect="plain" type="info">共 {{ total }} 条</el-tag>
      </div>
    </div>

    <el-card class="search-card">
      <div class="search-header">
        <div>
          <div class="section-title">筛选条件</div>
          <div class="section-subtitle">支持按交易所与日期范围筛选交易日历记录</div>
        </div>
        <div class="search-meta">
          <span>当前页 {{ rows.length }} 条</span>
          <span v-if="rangeText">范围 {{ rangeText }}</span>
        </div>
      </div>

      <el-form class="search-form">
        <div class="search-grid">
          <el-form-item label="交易所">
            <el-input v-model="exchange" placeholder="请输入交易所代码" clearable @keyup.enter="load" />
          </el-form-item>
          <el-form-item label="日期范围">
            <el-date-picker
              v-model="range"
              type="daterange"
              value-format="YYYY-MM-DD"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
            />
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
          <div class="section-title">交易日历列表</div>
          <div class="section-subtitle">按日期、交易所、开休市状态与维护时间组织信息层级</div>
        </div>
        <div class="table-toolbar__meta">分页 {{ pageNo }} / {{ totalPage }}</div>
      </div>

      <el-table :data="rows" v-loading="loading" class="full" stripe>
        <el-table-column label="交易日" min-width="220">
          <template #default="{ row }">
            <div class="primary-cell">{{ row.date || '-' }}</div>
            <div class="secondary-cell">上一交易日 {{ row.preDate || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="交易所" min-width="180">
          <template #default="{ row }">
            <div class="primary-cell">{{ row.exchange || '-' }}</div>
            <div class="secondary-cell">日历归属交易所</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="140">
          <template #default="{ row }">
            <div class="status-cell">
              <el-tag :type="row.open === 1 ? 'success' : 'info'">{{ row.open === 1 ? '开市' : '休市' }}</el-tag>
              <div class="secondary-cell">{{ row.open === 1 ? '支持交易' : '暂停交易' }}</div>
            </div>
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
import { listCalendars, type CalendarDto } from '../../services/basic'
import type { PageRes } from '../../types'

const exchange = ref('')
const range = ref<[string, string]>()
const loading = ref(false)
const rows = ref<CalendarDto[]>([])
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)

const totalPage = computed(() => {
  if (!total.value) return 1
  return Math.ceil(total.value / pageSize.value)
})

const rangeText = computed(() => {
  if (!range.value?.length) return ''
  return `${range.value[0]} ~ ${range.value[1]}`
})

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  const formatted = dayjs(value)
  return formatted.isValid() ? formatted.format('YYYY-MM-DD HH:mm:ss') : value
}

const resetSearch = () => {
  exchange.value = ''
  range.value = undefined
  pageNo.value = 1
  load()
}

const load = async () => {
  loading.value = true
  try {
    const startDate = range.value ? range.value[0] : undefined
    const endDate = range.value ? range.value[1] : undefined
    const res = await listCalendars({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      exchange: exchange.value,
      startDate,
      endDate
    })

    if (res.code === 200 && res.data) {
      const data = res.data as PageRes<CalendarDto>
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

.status-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
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

  .search-grid {
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
