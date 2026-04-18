<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__content">
        <div class="title">投资交易</div>
        <div class="subtitle">统一查看成交记录、价格和数量变化，保持投资模块列表型页面的阅读节奏一致</div>
      </div>
    </div>

    <el-card class="search-card">
      <div class="search-header">
        <div>
          <div class="section-title">筛选条件</div>
          <div class="section-subtitle">按标的代码快速定位目标交易</div>
        </div>
        <div class="search-meta">当前 {{ rows.length }} 条</div>
      </div>
      <el-form class="search-form">
        <div class="search-grid">
          <el-form-item label="代码">
            <el-input v-model="symbol" placeholder="标的代码" clearable @keyup.enter="load" />
          </el-form-item>
        </div>
      </el-form>
      <div class="search-actions">
        <el-button type="primary" :loading="loading" :icon="Search" @click="load">查询</el-button>
      </div>
    </el-card>

    <el-card class="table-card">
      <div class="table-toolbar">
        <div>
          <div class="section-title">交易列表</div>
          <div class="section-subtitle">突出标的信息、成交类型、成交数量与单价</div>
        </div>
      </div>
      <el-table :data="rows" v-loading="loading" class="full" stripe>
        <el-table-column prop="id" label="ID" min-width="76" />
        <el-table-column label="标的信息" min-width="200">
          <template #default="{ row }">
            <div class="primary-cell">{{ row.symbol || '-' }}</div>
            <div class="secondary-cell">交易编号 {{ row.id }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" min-width="110">
          <template #default="{ row }">
            <el-tag :type="tradeTypeTagType(row.type)">{{ tradeTypeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="volume" label="数量" min-width="100" />
        <el-table-column label="价格" min-width="120">
          <template #default="{ row }">
            <span class="primary-cell">{{ formatCurrency(row.price) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="成交金额" min-width="140">
          <template #default="{ row }">
            <span class="primary-cell">{{ formatCurrency(Number(row.price || 0) * Number(row.volume || 0)) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="168" />
        <el-table-column prop="updatedAt" label="更新时间" min-width="168" />
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
import { ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { listTradings, type TradingDto } from '../../services/basic'
import type { PageRes } from '../../types'

const symbol = ref('')
const loading = ref(false)
const rows = ref<TradingDto[]>([])
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)

const formatCurrency = (value?: number) => {
  if (value === undefined || value === null) return '-'
  return `¥${Number(value).toFixed(2)}`
}

const tradeTypeText = (value?: string) => {
  if (value === 'BUY') return '买入'
  if (value === 'SELL') return '卖出'
  return value || '-'
}

const tradeTypeTagType = (value?: string) => {
  if (value === 'BUY') return 'danger'
  if (value === 'SELL') return 'success'
  return 'info'
}

const load = async () => {
  loading.value = true
  try {
    const res = await listTradings({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      symbol: symbol.value
    })
    
    if (res.code === 200 && res.data) {
      const data = res.data as PageRes<TradingDto>
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
  align-items: center;
  justify-content: space-between;
  gap: 16px;
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

.search-meta {
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

.pager {
  padding-top: 16px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .page-header,
  .search-header,
  .table-toolbar,
  .search-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .search-actions {
    width: 100%;
  }

  .search-actions :deep(.el-button) {
    width: 100%;
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
