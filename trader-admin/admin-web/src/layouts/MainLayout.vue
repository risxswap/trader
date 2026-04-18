<template>
  <el-container class="layout-shell">
    <el-aside :width="collapsed ? '72px' : '232px'" class="layout-aside">
      <div class="logo-wrap">
        <div class="logo-mark">{{ collapsed ? 'T' : 'TR' }}</div>
        <div v-if="!collapsed" class="logo-copy">
          <div class="logo-title">Trader</div>
          <div class="logo-subtitle">管理后台</div>
        </div>
      </div>
      <el-menu
        :default-active="active"
        active-text-color="#ffffff"
        background-color="transparent"
        text-color="rgba(226, 232, 240, 0.76)"
        :collapse="collapsed"
        :collapse-transition="true"
        class="side-menu"
        router>
        <template v-for="entry in sideMenus" :key="entry.key">
          <el-menu-item v-if="entry.type === 'item'" :index="entry.index">
            <el-icon><component :is="resolveMenuIcon(entry.icon)" /></el-icon>
            <span>{{ entry.title }}</span>
          </el-menu-item>
          <el-sub-menu v-else :index="entry.index">
            <template #title>
              <el-icon><component :is="resolveMenuIcon(entry.icon)" /></el-icon>
              <span>{{ entry.title }}</span>
            </template>
            <el-menu-item v-for="sub in entry.items" :key="sub.index" :index="sub.index">{{ sub.title }}</el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>
    </el-aside>
    <el-container class="layout-main-shell">
      <el-header class="layout-header">
        <div class="header-left">
          <el-button link class="collapse-button" @click="toggleCollapse">
            <el-icon>
              <component :is="collapsed ? Expand : Fold" />
            </el-icon>
          </el-button>
          <div class="header-context">
            <div class="header-title">{{ currentTitle }}</div>
            <div class="header-subtitle">保持列表优先、信息清晰的后台体验</div>
          </div>
        </div>
        <div class="header-right">
          <div class="notice-entry">
            <el-badge is-dot>
              <el-icon :size="18"><Bell /></el-icon>
            </el-badge>
          </div>
        </div>
        <div class="header-user">
          <el-dropdown>
            <span class="user-entry">
              <span class="user-name">{{ username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="openChangePwd">修改密码</el-dropdown-item>
                <el-dropdown-item divided @click="onLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <TagsView
        class="layout-tags"
        :visited-views="visitedViews"
        :current-full-path="route.fullPath"
        @click-tag="handleClickTag"
        @close-tag="handleCloseTag"
      />
      <el-main class="layout-main">
        <router-view v-slot="{ Component, route: currentRoute }">
          <keep-alive :include="cachedViews">
            <div class="page-shell">
              <component :is="Component" :key="currentRoute.fullPath" />
            </div>
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
  <el-dialog v-model="showChangePwd" title="修改密码" width="420">
    <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="100px">
      <el-form-item label="原密码" prop="oldPassword">
        <el-input v-model="pwdForm.oldPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="pwdForm.newPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="确认新密码" prop="confirm">
        <el-input v-model="pwdForm.confirm" type="password" show-password />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showChangePwd=false">取消</el-button>
      <el-button type="primary" :loading="pwdLoading" @click="submitChangePwd">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch, type Component } from 'vue'
import { useRoute, useRouter, type RouteLocationNormalizedLoaded } from 'vue-router'
import { ElMessage } from 'element-plus'
import { changePassword } from '../services/auth'
import { DataBoard, Histogram, TrendCharts, Coin, Setting, Fold, Expand, Aim, Document, OfficeBuilding, Bell, Monitor } from '@element-plus/icons-vue'
import { appSideMenus } from '../router/index'
import TagsView from './TagsView.vue'

type TagView = {
  title: string
  fullPath: string
  path: string
  name: string
  affix: boolean
}

const route = useRoute()
const router = useRouter()
const active = computed(() => route.path)
const currentTitle = computed(() => String(route.meta?.title || '控制台'))
const iconMap: Record<string, Component> = {
  DataBoard,
  Aim,
  OfficeBuilding,
  Histogram,
  Coin,
  Document,
  Setting,
  Monitor,
  TrendCharts
}
const resolveMenuIcon = (iconKey: string) => iconMap[iconKey] || DataBoard
const sideMenus = appSideMenus
const username = computed(() => {
  const u = localStorage.getItem('user')
  if (!u) return '未登录'
  try {
    const o = JSON.parse(u)
    return o.nickname || o.username || '用户'
  } catch {
    return '用户'
  }
})
const onLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
}
const collapsed = ref(false)
const toggleCollapse = () => {
  collapsed.value = !collapsed.value
}
const visitedViews = ref<TagView[]>([])
const cachedViews = ref<string[]>([])

const resolveTagView = (targetRoute: RouteLocationNormalizedLoaded): TagView | null => {
  if (targetRoute.path === '/login') return null
  const routeName = targetRoute.name ? String(targetRoute.name) : ''
  if (!routeName) return null
  const title = String(targetRoute.meta?.title || routeName || targetRoute.path)
  return {
    title,
    fullPath: targetRoute.fullPath,
    path: targetRoute.path,
    name: routeName,
    affix: Boolean(targetRoute.meta?.affix)
  }
}

const addTagView = (targetRoute: RouteLocationNormalizedLoaded) => {
  const tag = resolveTagView(targetRoute)
  if (!tag) return
  if (!visitedViews.value.some(item => item.fullPath === tag.fullPath)) {
    visitedViews.value.push(tag)
  }
  if (targetRoute.meta?.noCache) return
  if (!cachedViews.value.includes(tag.name)) {
    cachedViews.value.push(tag.name)
  }
}

const removeCacheIfUnused = (tag: TagView) => {
  const hasSameName = visitedViews.value.some(item => item.name === tag.name)
  if (hasSameName) return
  cachedViews.value = cachedViews.value.filter(item => item !== tag.name)
}

const handleClickTag = (tag: TagView) => {
  if (route.fullPath === tag.fullPath) return
  router.push(tag.fullPath)
}

const handleCloseTag = (tag: TagView) => {
  const currentIsClosing = route.fullPath === tag.fullPath
  visitedViews.value = visitedViews.value.filter(item => item.fullPath !== tag.fullPath)
  removeCacheIfUnused(tag)
  if (!currentIsClosing) return
  const fallback = visitedViews.value[visitedViews.value.length - 1]
  router.push(fallback?.fullPath || '/dashboard')
}

watch(
  () => route.fullPath,
  () => {
    addTagView(route)
  },
  { immediate: true }
)

const showChangePwd = ref(false)
const pwdFormRef = ref()
const pwdLoading = ref(false)
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirm: ''
})
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
  confirm: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_: any, v: string, cb: Function) => {
        if (v !== pwdForm.newPassword) cb(new Error('两次输入不一致'))
        else cb()
      },
      trigger: 'blur'
    }
  ]
}
const openChangePwd = () => {
  showChangePwd.value = true
}
const submitChangePwd = async () => {
  // @ts-ignore
  const ok = await pwdFormRef.value?.validate?.()
  if (!ok) return
  const u = localStorage.getItem('user')
  let usernameStr = 'admin'
  if (u) {
    try {
      const o = JSON.parse(u)
      usernameStr = o.username || usernameStr
    } catch {}
  }
  pwdLoading.value = true
  try {
    const res = await changePassword({
      username: usernameStr,
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    if (res.code === 200) {
      ElMessage.success('密码修改成功，请重新登录')
      showChangePwd.value = false
      onLogout()
    } else {
      ElMessage.error(res.message || '修改失败')
    }
  } catch {
    ElMessage.error('网络错误')
  } finally {
    pwdLoading.value = false
  }
}
</script>

<style scoped>
.layout-shell {
  height: 100vh;
  background: #f3f6fb;
  overflow: hidden;
}

.layout-aside {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: linear-gradient(180deg, #18212f 0%, #0f1724 100%);
  color: #fff;
  padding: 16px 12px;
  box-shadow: inset -1px 0 0 rgba(255, 255, 255, 0.06);
  transition: width 0.2s ease;
  overflow: hidden;
}

.logo-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 8px 18px;
}

.logo-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: #fff;
  font-weight: 700;
  letter-spacing: 0.08em;
  flex-shrink: 0;
}

.logo-copy {
  overflow: hidden;
}

.logo-title {
  color: #f8fafc;
  font-size: 15px;
  font-weight: 600;
  line-height: 22px;
}

.logo-subtitle {
  color: rgba(226, 232, 240, 0.68);
  font-size: 12px;
  line-height: 18px;
}

.side-menu {
  border-right: none;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

:deep(.side-menu .el-menu-item),
:deep(.side-menu .el-sub-menu__title) {
  height: 44px;
  margin-bottom: 6px;
  border-radius: 12px;
}

:deep(.side-menu .el-menu-item:hover),
:deep(.side-menu .el-sub-menu__title:hover) {
  background: rgba(148, 163, 184, 0.14);
  color: #fff !important;
}

:deep(.side-menu .el-menu-item.is-active) {
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.96) 0%, rgba(59, 130, 246, 0.88) 100%);
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.24);
}

:deep(.side-menu .el-sub-menu .el-menu) {
  background: transparent;
}

:deep(.side-menu .el-menu-item.is-active .el-icon),
:deep(.side-menu .el-menu-item.is-active span) {
  color: #fff;
}

.layout-main-shell {
  min-width: 0;
  height: 100vh;
  overflow: hidden;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  height: 72px;
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid #e7edf5;
  backdrop-filter: blur(10px);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.collapse-button {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: #f3f6fb;
  color: #334155;
}

.header-context {
  min-width: 0;
}

.header-title {
  color: #0f172a;
  font-size: 20px;
  font-weight: 600;
  line-height: 28px;
}

.header-subtitle {
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

.notice-entry,
.user-entry {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.notice-entry {
  width: 40px;
  color: #475569;
}

.header-user {
  display: flex;
  align-items: center;
}

.user-entry {
  padding: 0 14px;
  cursor: pointer;
}

.user-name {
  color: #0f172a;
  font-size: 14px;
  font-weight: 500;
}

.layout-tags {
  position: relative;
  z-index: 1;
  flex-shrink: 0;
}

.layout-main {
  flex: 1;
  min-height: 0;
  padding: 20px;
  background: #f3f6fb;
  overflow-y: auto;
}

.page-shell {
  min-height: 100%;
}

@media (max-width: 768px) {
  .layout-header {
    height: auto;
    padding: 16px;
    flex-wrap: wrap;
  }

  .header-left {
    width: 100%;
  }

  .header-subtitle {
    display: none;
  }

  .header-right {
    margin-left: 0;
  }

  .layout-main {
    padding: 16px;
  }
}
</style>
