#!/usr/bin/env bash
set -euo pipefail

# ============================================================
#  桂航美食推荐排行榜 —— Linux 打包脚本（单 jar，内置前端）
#  用法：bash deploy/linux/build.sh
#  产物：backend/target/guatfood-backend-1.0.0.jar
# ============================================================

cd "$(dirname "$0")/../.."

echo "[1/3] 构建前端 ..."
(cd frontend && npm install && npm run build)

echo "[2/3] 复制 frontend/dist 到 backend/src/main/resources/static ..."
rm -rf backend/src/main/resources/static
mkdir -p backend/src/main/resources/static
cp -r frontend/dist/. backend/src/main/resources/static/

echo "[3/3] 打包后端 jar ..."
(cd backend && ./mvnw clean package -DskipTests)

echo
echo "打包完成：backend/target/guatfood-backend-1.0.0.jar"
