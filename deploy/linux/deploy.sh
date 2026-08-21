#!/usr/bin/env bash
set -euo pipefail

# ============================================================
#  桂航美食推荐排行榜 —— Linux 部署脚本
#  适用于全新 Ubuntu 22.04 / Debian 12，请用 root 运行
#  用法：sudo bash deploy.sh 你的域名.com
#  前置：① 域名已完成 ICP 备案并解析到本机
#        ② 已把 guatfood-backend-1.0.0.jar 与 init.sql
#           上传到本脚本同目录（见 README 的 scp 步骤）
# ============================================================

DOMAIN="${1:?用法: sudo bash deploy.sh 你的域名.com}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_DIR=/opt/guatfood
APP_USER=guatfood
JAR=guatfood-backend-1.0.0.jar

[ "$(id -u)" -eq 0 ] || { echo "请用 sudo 运行本脚本"; exit 1; }
[ -f "$SCRIPT_DIR/$JAR" ] || { echo "未找到 $SCRIPT_DIR/$JAR，请先运行 deploy/linux/build.sh 并把 jar 上传到本目录"; exit 1; }
[ -f "$SCRIPT_DIR/init.sql" ] || { echo "未找到 $SCRIPT_DIR/init.sql"; exit 1; }

echo "[1/7] 安装依赖 openjdk-17 / nginx / mysql-server ..."
apt-get update -y
DEBIAN_FRONTEND=noninteractive apt-get install -y openjdk-17-jre-headless nginx mysql-server

echo "[2/7] 创建运行用户与目录 ..."
id -u "$APP_USER" >/dev/null 2>&1 || useradd --system --create-home --shell /usr/sbin/nologin "$APP_USER"
mkdir -p "$APP_DIR/uploads" /etc/guatfood /etc/nginx/ssl

echo "[3/7] 复制后端 jar ..."
cp "$SCRIPT_DIR/$JAR" "$APP_DIR/$JAR"
chown -R "$APP_USER:$APP_USER" "$APP_DIR"

echo "[4/7] 写入环境变量模板 ..."
[ -f /etc/guatfood/guatfood.env ] || cp "$SCRIPT_DIR/guatfood.env.example" /etc/guatfood/guatfood.env
chmod 600 /etc/guatfood/guatfood.env
read -r -p "  >> 请先编辑 /etc/guatfood/guatfood.env（填写 DB_PASSWORD / JWT_SECRET / INIT_PASSWORD），完成后按回车继续..." _

echo "[5/7] 安装 systemd 服务 ..."
cp "$SCRIPT_DIR/guatfood.service" /etc/systemd/system/guatfood.service
systemctl daemon-reload
systemctl enable guatfood

echo "[6/7] 安装 Nginx 配置 ..."
sed "s|your-domain.com|$DOMAIN|g" "$SCRIPT_DIR/nginx.conf" > /etc/nginx/sites-available/guatfood
ln -sf /etc/nginx/sites-available/guatfood /etc/nginx/sites-enabled/guatfood
rm -f /etc/nginx/sites-enabled/default
nginx -t

echo "[7/7] 初始化 MySQL（建库建表 + 应用账号） ..."
mysql < "$SCRIPT_DIR/init.sql"
set -a; . /etc/guatfood/guatfood.env; set +a
mysql -e "CREATE USER IF NOT EXISTS '${DB_USERNAME}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}'; GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USERNAME}'@'localhost'; FLUSH PRIVILEGES;"

echo
echo "================================================================"
echo " 基础部署完成。接下来手动完成："
echo " 1) 配置 HTTPS 证书（见 deploy/linux/README.md 的 acme.sh 步骤）"
echo " 2) systemctl restart guatfood && systemctl reload nginx"
echo " 3) 访问 https://$DOMAIN 验证"
echo "================================================================"
