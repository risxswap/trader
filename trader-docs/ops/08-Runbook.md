# 08 - 运行手册（Build & Run）

本文档给出“从零到跑起来”的最短路径，并标注各模块所需的外部依赖与配置位置。

## 0. 环境要求

- JDK：21（`trader-base` / `trader-admin` / `trader-executor`）
- Maven：3.9+
- Node.js：建议 18+（用于 `admin-web`）
- 依赖服务（按需）：
  - MySQL（admin/executor 常用）
  - ClickHouse（admin/executor 常用）
  - Redis（admin/executor 常用；collector 也可能使用）

## 1. 本地构建（Maven）

### 1.1 构建 trader-base（其它服务依赖它）

```bash
cd trader-base
mvn -DskipTests install
```

### 1.2 运行/构建管理端后端（admin-server）

```bash
cd trader-admin
mvn -pl admin-server spring-boot:run
```

默认端口：`8080`（可通过 `server.port` 覆盖）。

### 1.3 运行采集服务（trader-collector）

```bash
cd trader-collector
mvn spring-boot:run
```

### 1.4 运行执行服务（trader-executor）

```bash
cd trader-executor
mvn spring-boot:run
```

## 2. 前端本地开发（admin-web）

```bash
cd trader-admin/admin-web
npm install
npm run dev
```

- dev server：`http://localhost:5173`
- API 代理：`/api -> http://localhost:8080`（见 [vite.config.ts](../../trader-admin/admin-web/vite.config.ts#L6-L14)）

## 3. Docker Compose 运行（推荐用于部署/联调）

### 3.1 trader-admin

- compose 文件：[trader-admin/docker-compose.yml](../../trader-admin/docker-compose.yml#L1-L34)
- 服务：
  - `trader`：运行 `trader-admin.jar`，映射端口 `8090:8080`
  - `nginx`：托管前端静态文件，映射端口 `6060:80`
- 配置注入方式：
  - 挂载 `/opt/trader/config.properties` 到容器 `/app/config.properties`
  - 挂载 `./server/config` 到容器 `/app/config`，并通过启动参数指定：
    - `--spring.config.location=file:/app/config/application.yml`
    - `--logging.config=file:/app/config/logback.xml`

### 3.2 trader-collector

- compose 文件：[trader-collector/docker-compose.yml](../../trader-collector/docker-compose.yml#L1-L20)
- 配置注入方式：
  - 挂载 `/opt/trader-fund/config.properties` 到容器 `/app/config.properties`
  - 通过 `CONFIG_PATH=/app/config.properties` 指定外部配置文件路径

### 3.3 trader-executor

- compose 文件：[trader-executor/docker-compose.yml](../../trader-executor/docker-compose.yml#L1-L19)
- 启动命令：`sh /app/bin/run.sh`（见 [bin/run.sh](../../trader-executor/bin/run.sh#L1-L25)）
  - 生成/持久化 `trader.node.id`
  - 注入 `--node.config.path=/app/config/config.properties`

## 4. 配置建议（避免明文敏感信息）

仓库中的 `application.yml` 可能包含示例密码或 webhook key。推荐做法：

- 本地开发：
  - 创建个人私有的外部 `config.properties`
  - 或使用环境变量覆盖（例如 `WECOM_WEBHOOK_URL`、数据库口令、Redis 口令等）
- 部署环境：
  - 通过 compose 挂载外部配置文件
  - 或通过密钥管理系统注入环境变量

## 5. 常见端口与访问入口

- admin-server：`http://localhost:8080`（compose 部署后通常为 `http://localhost:8090`）
- admin-web：开发模式 `http://localhost:5173`；nginx 部署后 `http://localhost:6060`
- executor：默认 `http://localhost:8090`（compose 示例映射 `18090:8090`）
