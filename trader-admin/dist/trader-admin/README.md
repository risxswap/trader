# Trader Admin Deployment

## 部署说明

1. 确保已安装 Docker 和 Docker Compose
2. 按需修改 server/config/application.yml 和 server/config/logback-spring.xml 中的配置
3. 启动服务:
   ```bash
   cd trader-admin
   docker-compose up -d
   ```

## 访问
- 前端页面: http://localhost
- 后端API: http://localhost/api/
