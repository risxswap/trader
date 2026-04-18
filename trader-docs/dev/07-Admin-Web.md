# 07 - 管理端前端（admin-web）

`admin-web` 是 Vue 3 + Vite 的单页应用，主要负责管理端页面、路由与 API 调用封装。

## 1. 工程入口

- 依赖与脚本：[package.json](../../trader-admin/admin-web/package.json#L6-L27)
  - `npm run dev`：本地开发
  - `npm run build`：生产构建
- Vite 配置（含代理）：[vite.config.ts](../../trader-admin/admin-web/vite.config.ts#L1-L15)
  - 默认端口：`5173`
  - 代理：`/api -> http://localhost:8080`，并 rewrite 去掉 `/api` 前缀

## 2. HTTP 封装（axios）

- 实现：[services/http.ts](../../trader-admin/admin-web/src/services/http.ts#L1-L13)
- 行为：
  - `baseURL = VITE_API_BASE_URL || '/api'`
  - 默认 timeout：15s
  - 当前仅配置了 response interceptor 的透传/抛错（未统一处理业务 code）

## 3. 路由、菜单与权限（前端侧）

- 路由定义与侧边栏菜单生成：见 [router/index.ts](../../trader-admin/admin-web/src/router/index.ts#L1-L448)
  - `appRouteChildren`：业务路由树（按模块分组）
  - `buildSideMenus()`：根据路由 meta 生成侧边栏菜单结构
- 简易登录拦截：
  - `router.beforeEach` 仅检查 `localStorage.token` 是否存在
  - 未实现服务端 token 校验或接口级鉴权（如需强鉴权，需在后端增加拦截器/会话存储，并在前端请求携带 token）

## 4. 页面与服务的映射方式

前端通常按“页面 -> services -> 后端 controller”组织调用：

- 页面目录：`src/pages/*`
  - 例：`src/pages/exchange/List.vue` 对应交易所列表
- services 目录：`src/services/*`
  - 例：`src/services/exchange.ts`（若存在）封装 `/exchange/*` API
- 后端 controller：`admin-server/src/main/java/.../controller/*`
  - 例：`GET /exchange/list` 见 [ExchangeController](../../trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/controller/ExchangeController.java#L14-L24)

## 5. 构建产物与部署位置（admin 模块）

管理端的 docker-compose 将静态文件由 nginx 托管：

- admin compose：见 [trader-admin/docker-compose.yml](../../trader-admin/docker-compose.yml#L1-L34)
  - `nginx` 容器挂载 `./web:/usr/share/nginx/html`
  - `trader`（admin-server）对外 `8090:8080`
