<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__main">
        <el-button text type="primary" class="back-button" @click="onBack">返回列表</el-button>
        <div class="page-header__content">
          <div class="title">券商详情</div>
          <div class="subtitle">查看券商基础资料、资金规模与维护信息</div>
        </div>
      </div>
      <el-tag v-if="broker?.code" type="info" effect="plain">{{ broker.code }}</el-tag>
    </div>

    <el-skeleton :loading="loading" animated>
      <template #default>
        <div class="summary-grid">
          <el-card class="summary-card" shadow="hover">
            <div class="summary-label">当前资金</div>
            <div class="summary-value">{{ formatCurrency(broker.currentCapital) }}</div>
          </el-card>
          <el-card class="summary-card" shadow="hover">
            <div class="summary-label">初始资金</div>
            <div class="summary-value">{{ formatCurrency(broker.initialCapital) }}</div>
          </el-card>
          <el-card class="summary-card" shadow="hover">
            <div class="summary-label">资金变化</div>
            <div :class="['summary-value', capitalDelta >= 0 ? 'text-red' : 'text-green']">{{ formatSignedCurrency(capitalDelta) }}</div>
          </el-card>
          <el-card class="summary-card" shadow="hover">
            <div class="summary-label">券商名称</div>
            <div class="summary-value summary-value--name">{{ broker.name || '-' }}</div>
          </el-card>
        </div>

        <div class="content-grid">
          <el-card class="detail-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <div class="card-title">基础信息</div>
                <div class="card-subtitle">统一展示核心属性和时间信息</div>
              </div>
            </template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="ID">{{ broker.id || '-' }}</el-descriptions-item>
              <el-descriptions-item label="代号">{{ broker.code || '-' }}</el-descriptions-item>
              <el-descriptions-item label="名称">{{ broker.name || '-' }}</el-descriptions-item>
              <el-descriptions-item label="初始资金">{{ formatCurrency(broker.initialCapital) }}</el-descriptions-item>
              <el-descriptions-item label="当前资金">{{ formatCurrency(broker.currentCapital) }}</el-descriptions-item>
              <el-descriptions-item label="资金变化">
                <span :class="capitalDelta >= 0 ? 'text-red' : 'text-green'">{{ formatSignedCurrency(capitalDelta) }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ broker.createdAt || '-' }}</el-descriptions-item>
              <el-descriptions-item label="更新时间">{{ broker.updatedAt || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <el-card class="detail-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <div class="card-title">补充说明</div>
                <div class="card-subtitle">用于查看简介和日常维护备注</div>
              </div>
            </template>
            <div class="note-section">
              <div class="note-block">
                <div class="note-label">简介</div>
                <div class="note-content">{{ broker.intro || '暂无简介' }}</div>
              </div>
              <div class="note-block">
                <div class="note-label">备注</div>
                <div class="note-content">{{ broker.remark || '暂无备注' }}</div>
              </div>
            </div>
          </el-card>
        </div>
      </template>
    </el-skeleton>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getBroker, type BrokerDto } from '../../services/basic'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const broker = ref<BrokerDto>({} as BrokerDto)

const formatCurrency = (value?: number) => {
  if (value === undefined || value === null) return '-'
  return `¥${Number(value).toFixed(2)}`
}

const formatSignedCurrency = (value?: number) => {
  if (value === undefined || value === null) return '-'
  const amount = Number(value)
  const prefix = amount > 0 ? '+' : ''
  return `${prefix}¥${amount.toFixed(2)}`
}

const capitalDelta = computed(() => {
  return Number(broker.value.currentCapital || 0) - Number(broker.value.initialCapital || 0)
})

const onBack = () => {
  router.back()
}

const load = async () => {
  const id = Number(route.params.id)
  if (!id) return
  
  loading.value = true
  try {
    const res = await getBroker(id)
    if (res.code === 200 && res.data) {
      broker.value = res.data
    } else {
      ElMessage.error(res.message || '获取详情失败')
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
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 24px 28px;
  border-radius: 20px;
  background: linear-gradient(135deg, #ffffff 0%, #f8fbff 100%);
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.06);
}

.page-header__main {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
}

.page-header__content {
  min-width: 0;
}

.back-button {
  padding: 0;
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

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.summary-card,
.detail-card {
  border: none;
  border-radius: 18px;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
}

.summary-label {
  color: #64748b;
  font-size: 13px;
}

.summary-value {
  margin-top: 10px;
  color: #0f172a;
  font-size: 24px;
  font-weight: 600;
  line-height: 32px;
}

.summary-value--name {
  font-size: 20px;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(0, 1fr);
  gap: 16px;
}

.card-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-title {
  color: #0f172a;
  font-size: 16px;
  font-weight: 600;
}

.card-subtitle {
  color: #64748b;
  font-size: 13px;
}

.note-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.note-block {
  padding: 16px;
  border-radius: 14px;
  background: #f8fafc;
}

.note-label {
  color: #475569;
  font-size: 13px;
  font-weight: 600;
}

.note-content {
  margin-top: 8px;
  color: #0f172a;
  line-height: 22px;
  white-space: pre-wrap;
  word-break: break-word;
}

.text-red {
  color: #f56c6c;
}

.text-green {
  color: #67c23a;
}

@media (max-width: 1100px) {
  .summary-grid,
  .content-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 768px) {
  .page-header {
    padding: 20px;
  }

  .summary-grid,
  .content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
