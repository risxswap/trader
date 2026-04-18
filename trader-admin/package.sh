#!/bin/bash

set -euo pipefail

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
PACKAGE_NAME="trader-admin"
TEMP_DIR="${ROOT_DIR}/package_temp"
TARGET_DIR="${ROOT_DIR}/dist"
SERVER_DIR="${ROOT_DIR}/admin-server"
WEB_DIR="${ROOT_DIR}/admin-web"
RESOURCES_DIR="${SERVER_DIR}/src/main/resources"
SERVER_PACKAGE_DIR="${TEMP_DIR}/${PACKAGE_NAME}/server"
SERVER_CONFIG_DIR="${SERVER_PACKAGE_DIR}/config"
WEB_PACKAGE_DIR="${TEMP_DIR}/${PACKAGE_NAME}/web"
JAR_NAME="trader-admin.jar"
JAR_FILE="${SERVER_DIR}/target/${JAR_NAME}"

echo -e "${GREEN}开始打包 ${PACKAGE_NAME}...${NC}"

MVN_CMD=""
if [ -x "${ROOT_DIR}/mvnw" ]; then
    MVN_CMD="${ROOT_DIR}/mvnw"
elif [ -x "${ROOT_DIR}/../mvnw" ]; then
    MVN_CMD="${ROOT_DIR}/../mvnw"
elif command -v mvn &> /dev/null; then
    MVN_CMD="mvn"
else
    echo -e "${RED}错误: 未找到 Maven 或 mvnw，请先安装 Maven 或使用项目自带的 mvnw${NC}"
    exit 1
fi

echo -e "${GREEN}执行Maven打包(admin-server模块)...${NC}"
(
    cd "${ROOT_DIR}"
    "$MVN_CMD" clean package -pl admin-server -am -Dmaven.test.skip=true
)

if [ ! -f "${JAR_FILE}" ]; then
    echo -e "${RED}错误: 未找到后端产物 ${JAR_FILE}${NC}"
    exit 1
fi

echo -e "${GREEN}执行前端打包(admin-web模块)...${NC}"
cd "${WEB_DIR}"

if [ -s "$HOME/.nvm/nvm.sh" ]; then
    export NVM_DIR="$HOME/.nvm"
    source "$NVM_DIR/nvm.sh"
    echo -e "${GREEN}尝试切换到 Node.js 20...${NC}"
    nvm use 20 || nvm install 20
fi

if ! command -v npm &> /dev/null; then
    echo -e "${RED}错误: 未找到 npm，请先安装 Node.js${NC}"
    exit 1
fi

NODE_VERSION=$(node -v | cut -d. -f1 | tr -d 'v')
if [ "$NODE_VERSION" -lt 18 ]; then
    echo -e "${RED}错误: Node.js 版本必须 >= 18 (当前版本: $(node -v))${NC}"
    echo -e "${RED}请升级 Node.js 或使用 nvm 切换版本${NC}"
    exit 1
fi

npm ci
npm run build
cd ..

echo -e "${GREEN}创建发布目录...${NC}"
rm -rf "${TEMP_DIR}"
mkdir -p "${SERVER_CONFIG_DIR}" "${WEB_PACKAGE_DIR}"

echo -e "${GREEN}复制后端文件...${NC}"
cp "${JAR_FILE}" "${SERVER_PACKAGE_DIR}/${JAR_NAME}"
cp "${RESOURCES_DIR}/application.yml" "${SERVER_CONFIG_DIR}/application.yml"
cp "${RESOURCES_DIR}/logback-spring.xml" "${SERVER_CONFIG_DIR}/logback-spring.xml"

echo -e "${GREEN}复制前端文件...${NC}"
cp -r "${WEB_DIR}/dist/." "${WEB_PACKAGE_DIR}/"

echo -e "${GREEN}复制配置和脚本...${NC}"
cp "${ROOT_DIR}/docker-compose.yml" "${TEMP_DIR}/${PACKAGE_NAME}/"
cp "${ROOT_DIR}/nginx.conf" "${TEMP_DIR}/${PACKAGE_NAME}/"

cat > "${TEMP_DIR}/${PACKAGE_NAME}/README.md" << EOF
# Trader Admin Deployment

## 部署说明

1. 确保已安装 Docker 和 Docker Compose
2. 按需修改 server/config/application.yml 和 server/config/logback-spring.xml 中的配置
3. 启动服务:
   \`\`\`bash
   cd trader-admin
   docker-compose up -d
   \`\`\`

## 访问
- 前端页面: http://localhost
- 后端API: http://localhost/api/
EOF

echo -e "${GREEN}创建压缩包...${NC}"
mkdir -p "${TARGET_DIR}"
tar -czf "${TARGET_DIR}/${PACKAGE_NAME}.tar.gz" -C "${TEMP_DIR}" "${PACKAGE_NAME}"

echo -e "${GREEN}复制未压缩的文件夹到 ${TARGET_DIR}...${NC}"
rm -rf "${TARGET_DIR}/${PACKAGE_NAME}"
cp -r "${TEMP_DIR}/${PACKAGE_NAME}" "${TARGET_DIR}/"

echo -e "${GREEN}清理临时目录...${NC}"
rm -rf "${TEMP_DIR}"

echo -e "${GREEN}打包完成: ${TARGET_DIR}/${PACKAGE_NAME}.tar.gz${NC}"
echo -e "${GREEN}未压缩文件夹: ${TARGET_DIR}/${PACKAGE_NAME}${NC}"
echo -e "${GREEN}包含以下文件:${NC}"
tar -tf "${TARGET_DIR}/${PACKAGE_NAME}.tar.gz"

exit 0
