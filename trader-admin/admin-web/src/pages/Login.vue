<template>
  <div class="login-wrap">
    <div class="ambient-orb ambient-orb-left"></div>
    <div class="ambient-orb ambient-orb-right"></div>
    <div class="login-hero">
      <div class="hero-copy">
        <div class="brand-badge">
          <div class="brand-badge-mark">T</div>
          <div class="brand-badge-text">
            <span class="brand-badge-title">Trader Admin</span>
            <span class="brand-badge-subtitle">交易后台</span>
          </div>
        </div>
        <h1 class="hero-title">计划你的交易，交易你的计划。</h1>
        <p class="hero-subtitle">
          历史不会简单重复，但总会压着相同的韵脚。
        </p>
      </div>

      <div class="login-panel">
        <div class="panel-header">
          <div class="panel-kicker">欢迎登录</div>
          <div class="panel-title">进入 Trader 控制台</div>
          <div class="panel-subtitle">请使用你的管理账号登录，继续处理今日任务。</div>
        </div>

        <el-form :model="form" :rules="rules" ref="formRef" class="login-form" @keydown.enter="onSubmit">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名">
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password>
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item class="actions">
            <el-button type="primary" class="submit-button" :loading="loading" @click="onSubmit">登录</el-button>
            <el-button class="ghost-button" @click="onReset">重置</el-button>
          </el-form-item>
        </el-form>

        <div class="panel-footer">
          <span class="panel-footer-line"></span>
          <span>安全登录 · 数据管理后台</span>
          <span class="panel-footer-line"></span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { login } from '../services/auth'
import { useRouter } from 'vue-router'

const formRef = ref()
const loading = ref(false)
const router = useRouter()
const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const onSubmit = async () => {
  // @ts-ignore
  const valid = await formRef.value?.validate?.()
  if (!valid) return
  loading.value = true
  try {
    const res = await login({ username: form.username, password: form.password })
    if (res.code === 200 && res.data) {
      ElMessage.success(`欢迎 ${res.data.nickname || res.data.username}`)
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('user', JSON.stringify(res.data))
      router.push('/dashboard')
    } else {
      ElMessage.error(res.message || '登录失败')
    }
  } catch (e) {
    ElMessage.error('网络错误')
  } finally {
    loading.value = false
  }
}

const onReset = () => {
  form.username = ''
  form.password = ''
  // @ts-ignore
  formRef.value?.clearValidate?.()
}
</script>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(circle at top left, rgba(96, 165, 250, 0.18), transparent 30%),
    radial-gradient(circle at right center, rgba(59, 130, 246, 0.14), transparent 32%),
    linear-gradient(180deg, #f8fbff 0%, #eef3fb 100%);
  padding: 32px;
}

.ambient-orb {
  position: absolute;
  border-radius: 999px;
  filter: blur(16px);
  opacity: 0.8;
}

.ambient-orb-left {
  top: 10%;
  left: 8%;
  width: 260px;
  height: 260px;
  background: rgba(125, 211, 252, 0.26);
}

.ambient-orb-right {
  right: 10%;
  bottom: 12%;
  width: 320px;
  height: 320px;
  background: rgba(59, 130, 246, 0.14);
}

.login-hero {
  position: relative;
  z-index: 1;
  width: min(1120px, 100%);
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(360px, 420px);
  gap: 48px;
  align-items: center;
}

.hero-copy {
  padding: 8px 0 8px 12px;
  color: #0f172a;
}

.brand-badge {
  display: inline-flex;
  align-items: center;
  gap: 14px;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.08);
}

.brand-badge-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background: linear-gradient(135deg, #2563eb 0%, #60a5fa 100%);
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.brand-badge-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-badge-title {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.brand-badge-subtitle {
  font-size: 12px;
  color: #64748b;
}

.hero-eyebrow {
  margin-top: 28px;
  color: #2563eb;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.hero-title {
  margin: 16px 0 0;
  max-width: 620px;
  color: #0f172a;
  font-size: 44px;
  line-height: 1.2;
  font-weight: 700;
}

.hero-subtitle {
  max-width: 560px;
  margin: 18px 0 0;
  color: #475569;
  font-size: 16px;
  line-height: 1.8;
}

.security-points {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 28px;
}

.security-point {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.66);
  border: 1px solid rgba(226, 232, 240, 0.92);
  color: #334155;
  font-size: 13px;
}

.security-point-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: linear-gradient(135deg, #2563eb 0%, #60a5fa 100%);
  box-shadow: 0 0 0 4px rgba(96, 165, 250, 0.16);
}

.login-panel {
  padding: 30px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(226, 232, 240, 0.95);
  box-shadow: 0 28px 70px rgba(15, 23, 42, 0.10);
  backdrop-filter: blur(18px);
}

.panel-header {
  margin-bottom: 26px;
}

.panel-kicker {
  color: #2563eb;
  font-size: 13px;
  font-weight: 700;
}

.panel-title {
  margin-top: 12px;
  color: #0f172a;
  font-size: 28px;
  line-height: 1.3;
  font-weight: 700;
}

.panel-subtitle {
  margin-top: 10px;
  color: #64748b;
  font-size: 14px;
  line-height: 1.7;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 52px;
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.98);
  box-shadow: none;
  padding: 0 16px;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px rgba(37, 99, 235, 0.65), 0 10px 24px rgba(37, 99, 235, 0.10);
}

.login-form :deep(.el-input__prefix) {
  margin-right: 8px;
  color: #64748b;
}

.actions {
  margin-top: 4px;
}

.actions :deep(.el-form-item__content) {
  width: 100%;
  display: flex;
  gap: 8px;
}

.actions :deep(.el-button) {
  flex: 1;
  min-height: 50px;
  border-radius: 16px;
  font-weight: 600;
}

.submit-button {
  border: none;
  background: linear-gradient(135deg, #0f172a 0%, #1e3a8a 100%);
  box-shadow: 0 18px 28px rgba(15, 23, 42, 0.16);
}

.ghost-button {
  background: rgba(248, 250, 252, 0.92);
  border-color: #dbe3f1;
  color: #334155;
}

.panel-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 26px;
  color: #94a3b8;
  font-size: 12px;
}

.panel-footer-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent 0%, #dbe3f1 50%, transparent 100%);
}

@media (max-width: 960px) {
  .login-hero {
    grid-template-columns: 1fr;
    gap: 24px;
  }

  .hero-copy {
    padding: 0;
  }

  .hero-title {
    font-size: 34px;
    max-width: none;
  }
}

@media (max-width: 640px) {
  .login-wrap {
    padding: 20px;
  }

  .login-panel {
    padding: 24px 18px;
  }

  .hero-title {
    font-size: 28px;
  }

  .security-points {
    flex-direction: column;
    align-items: stretch;
  }

  .actions :deep(.el-form-item__content) {
    flex-direction: column;
  }
}
</style>
