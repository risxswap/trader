import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const routerSource = readFileSync(resolve(root, 'src/router/index.ts'), 'utf8')
const layoutSource = readFileSync(resolve(root, 'src/layouts/MainLayout.vue'), 'utf8')

assert.ok(routerSource.includes("path: '/task/log'"), '任务日志路由应移动到 /task/log')
assert.ok(!routerSource.includes("path: '/logs'"), '不应保留日志管理父菜单路由')
assert.ok(!routerSource.includes("path: '/logs/push'"), '消息日志不应作为独立路由出现')
assert.ok(!routerSource.includes("title: '日志管理'"), '侧边栏不应出现日志管理菜单')
assert.ok(!routerSource.includes("name: 'PushList'"), '消息日志不应作为可导航页面注册')

assert.ok(layoutSource.includes('@click="openMessageLogDrawer"'), '顶部铃铛应打开消息日志抽屉')
assert.ok(layoutSource.includes('<PushList'), '消息日志列表应在顶栏抽屉中展示')
assert.ok(layoutSource.includes('size="640px"'), '消息日志抽屉桌面宽度应收窄到 640px')
assert.ok(routerSource.includes("path: '/task/log'"), '任务日志路由应移动到 /task/log')

const pushListSource = readFileSync(resolve(root, 'src/pages/logs/PushList.vue'), 'utf8')
assert.ok(pushListSource.includes('@row-click="handleRowClick"'), '嵌入抽屉中的消息日志应支持点击行查看详情')
assert.ok(pushListSource.includes('v-if="!embedded" label="渠道与接收人"'), '嵌入模式应隐藏渠道与接收人列')
assert.ok(pushListSource.includes('v-if="!embedded" label="操作"'), '嵌入模式应隐藏操作列')
