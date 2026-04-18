#!/bin/bash

BASE_DIR="/Users/haiming/Workspace/trader"
ADMIN_DAO="${BASE_DIR}/trader-admin/admin-server/src/main/java/cc/riskswap/trader/admin/dao"
COLLECTOR_DAO="${BASE_DIR}/trader-collector/src/main/java/cc/riskswap/trader/collector/repository"
EXECUTOR_DAO="${BASE_DIR}/trader-executor/src/main/java/cc/riskswap/trader/executor/dao"
BASE_MODULE_DAO="${BASE_DIR}/trader-base/src/main/java/cc/riskswap/trader/base/dao"

# Move Executor DAOs as the base (since they seem the most complete for trading)
# We will just copy everything from Executor to Base
cp -rn ${EXECUTOR_DAO}/* ${BASE_MODULE_DAO}/

# Update package declarations in Base DAOs
find ${BASE_MODULE_DAO} -type f -name "*.java" -exec sed -i '' 's/cc\.riskswap\.trader\.executor\.dao/cc.riskswap.trader.base.dao/g' {} +
find ${BASE_MODULE_DAO} -type f -name "*.java" -exec sed -i '' 's/cc\.riskswap\.trader\.admin\.dao/cc.riskswap.trader.base.dao/g' {} +
find ${BASE_MODULE_DAO} -type f -name "*.java" -exec sed -i '' 's/cc\.riskswap\.trader\.collector\.repository/cc.riskswap.trader.base.dao/g' {} +

echo "Move and package update complete. Manual merging of duplicate DAO methods is still required."
