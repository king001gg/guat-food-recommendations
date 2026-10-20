#!/usr/bin/env bash
set -euo pipefail

# ============================================================
#  桂航美食推荐排行榜 —— Linux 部署脚本
#  适用于全新 Ubuntu 22.04 / Debian 12，请用 root 运行
#  用法：sudo bash deploy.sh 你的域名.com
#  前置：① 域名已完成 ICP 备案并解析到本机
#        ② 已把 guatfood-backend-1.0.0.jar 上传到本脚本同目录
#  说明：数据库表结构由 Flyway 在应用启动时自动迁移，无需手工执行建表 SQL
# ============================================================

DOMAIN="${1:?用法: sudo bash deploy.sh 你的域名.com}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_DIR=/opt/guatfood
APP_USER=guatfood
JAR=guatfood-backend-1.0.0.jar
ENV_FILE=/etc/guatfood/guatfood.env
NGINX_SITE=/etc/nginx/sites-available/guatfood
CERT_DIR=/etc/nginx/ssl

[ "$(id -u)" -eq 0 ] || { echo "请用 sudo 运行本脚本"; exit 1; }
for f in "$JAR" guatfood.service backup.sh guatfood-backup.service \
         guatfood-backup.timer nginx.conf guatfood.env.example; do
    [ -f "$SCRIPT_DIR/$f" ] || { echo "缺少 $SCRIPT_DIR/$f"; exit 1; }
done

echo "[1/9] 安装依赖 openjdk-17 / nginx / mysql-server / openssl ..."
apt-get update -y
DEBIAN_FRONTEND=noninteractive apt-get install -y \
    openjdk-17-jre-headless nginx mysql-server openssl curl ca-certificates

echo "[2/9] 创建运行用户与目录 ..."
id -u "$APP_USER" >/dev/null 2>&1 || useradd --system --create-home --shell /usr/sbin/nologin "$APP_USER"
install -d -o "$APP_USER" -g "$APP_USER" "$APP_DIR" "$APP_DIR/uploads" "$APP_DIR/releases"
install -d /etc/guatfood "$CERT_DIR"

echo "[3/9] 准备环境变量 ..."
if [ ! -f "$ENV_FILE" ]; then
    install -m 600 -o root -g root "$SCRIPT_DIR/guatfood.env.example" "$ENV_FILE"
fi
if [ -t 0 ]; then
    read -r -p "  >> 请先编辑 $ENV_FILE（DB_PASSWORD / JWT_SECRET / INIT_PASSWORD），完成后按回车继续..." _
fi
chmod 600 "$ENV_FILE"
set -a
# shellcheck disable=SC1090
. "$ENV_FILE"
set +a

need() {
    [ -n "${!1:-}" ] || { echo "环境变量 $1 未设置，请检查 $ENV_FILE"; exit 1; }
}
need DB_USERNAME
need DB_NAME
need DB_PASSWORD
need JWT_SECRET
need INIT_PASSWORD

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"

# 数据库名/用户名会拼进 SQL，限制字符集避免注入
[[ "$DB_NAME" =~ ^[A-Za-z0-9_]+$ ]] || {
    echo "DB_NAME 只能包含字母、数字和下划线"; exit 1;
}
[[ "$DB_USERNAME" =~ ^[A-Za-z0-9_]+$ ]] || {
    echo "DB_USERNAME 只能包含字母、数字和下划线"; exit 1;
}
case "$DB_PASSWORD" in
    *"'"*|*'\'*|*'"'*|*'`'*|*'$'*|*' '*)
        echo "DB_PASSWORD 不能包含引号、反斜杠、反引号、\$ 或空格"; exit 1 ;;
esac

case "$DB_PASSWORD" in
    改成*|changeme*|password*) echo "DB_PASSWORD 仍是占位值，请修改 $ENV_FILE"; exit 1 ;;
esac
[ "${#DB_PASSWORD}" -ge 8 ] || { echo "DB_PASSWORD 至少 8 位"; exit 1; }

case "$INIT_PASSWORD" in
    改成*|changeme*|password*) echo "INIT_PASSWORD 仍是占位值，请修改 $ENV_FILE"; exit 1 ;;
esac
[ "${#INIT_PASSWORD}" -ge 8 ] || { echo "INIT_PASSWORD 至少 8 位"; exit 1; }
[ "$INIT_PASSWORD" != "$DB_PASSWORD" ] || { echo "INIT_PASSWORD 不能与 DB_PASSWORD 相同"; exit 1; }

[ "${#JWT_SECRET}" -ge 32 ] || {
    echo "JWT_SECRET 至少需要 32 字节，推荐：openssl rand -base64 48"
    exit 1
}
case "$JWT_SECRET" in
    *改成*|*changeme*|*CHANGE_ME*|*change_me*|*your-secret*|*your_secret*)
        echo "JWT_SECRET 仍是模板占位值，请改为随机密钥（openssl rand -base64 48）"; exit 1 ;;
esac

case "$DB_HOST" in
    127.0.0.1|localhost|::1) ;;
    *)
        echo "deploy.sh 只负责本机 MySQL（DB_HOST=$DB_HOST）；远程数据库请手动创建库与账号后，将 DB_HOST 指向远端"
        exit 1 ;;
esac

MGMT_PORT="${MANAGEMENT_PORT:-8081}"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/guatfood}"
[[ "$BACKUP_DIR" =~ ^/[A-Za-z0-9_./-]+$ ]] || {
    echo "BACKUP_DIR 必须是绝对路径，且只能包含字母、数字、下划线、点、斜杠和短横线"; exit 1;
}
[[ "/$BACKUP_DIR/" != *"/../"* ]] || {
    echo "BACKUP_DIR 不能包含 .. 路径段"; exit 1;
}

UPLOAD_DIR="${UPLOAD_PATH:-$APP_DIR/uploads}"
while [ "${UPLOAD_DIR%/}" != "$UPLOAD_DIR" ]; do UPLOAD_DIR="${UPLOAD_DIR%/}"; done
[[ "$UPLOAD_DIR" =~ ^/[A-Za-z0-9_./-]+$ ]] || {
    echo "UPLOAD_PATH 必须是绝对路径，且只能包含字母、数字、下划线、点、斜杠和短横线"; exit 1;
}
[[ "/$UPLOAD_DIR/" != *"/../"* ]] || {
    echo "UPLOAD_PATH 不能包含 .. 路径段"; exit 1;
}
install -d -o "$APP_USER" -g "$APP_USER" "$UPLOAD_DIR"

db_reachable() {
    MYSQL_PWD="$DB_PASSWORD" mysql --no-defaults \
        --host="$DB_HOST" --port="$DB_PORT" --user="$DB_USERNAME" \
        --database="$DB_NAME" --execute="SELECT 1" >/dev/null 2>&1
}

echo "[4/9] 初始化 MySQL 数据库与账号 ..."
# 只创建库和账号；表结构与升级由 Flyway 负责。
# 通过 stdin 执行，避免密码出现在 mysql 进程参数中。
# 同时创建 localhost / 127.0.0.1 两个 host，兼容启用 skip-name-resolve 的 MySQL。
mysql <<SQL
CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\`
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${DB_USERNAME}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
CREATE USER IF NOT EXISTS '${DB_USERNAME}'@'127.0.0.1' IDENTIFIED BY '${DB_PASSWORD}';
CREATE USER IF NOT EXISTS '${DB_USERNAME}'@'::1' IDENTIFIED BY '${DB_PASSWORD}';
ALTER USER '${DB_USERNAME}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
ALTER USER '${DB_USERNAME}'@'127.0.0.1' IDENTIFIED BY '${DB_PASSWORD}';
ALTER USER '${DB_USERNAME}'@'::1' IDENTIFIED BY '${DB_PASSWORD}';
GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USERNAME}'@'localhost';
GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USERNAME}'@'127.0.0.1';
GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USERNAME}'@'::1';
FLUSH PRIVILEGES;
SQL

# Flyway 在启动时执行迁移；先确认数据库可达且账号可用，
# 避免把“数据库故障”误判成“新版本代码故障”而触发无意义回滚。
if ! db_reachable; then
    echo "数据库连接/认证失败：$DB_HOST:$DB_PORT / $DB_NAME（用户 $DB_USERNAME），请检查 MySQL 与 DB_* 配置"
    exit 1
fi

echo "[5/9] 安装 Nginx 配置与证书 ..."
SITE_EXISTED=0
NGINX_BACKUP=""
if [ -f "$NGINX_SITE" ]; then
    SITE_EXISTED=1
    NGINX_BACKUP="${NGINX_SITE}.bak"
    cp -p "$NGINX_SITE" "$NGINX_BACKUP"
fi

restore_nginx_site() {
    if [ "$SITE_EXISTED" -eq 1 ] && [ -n "$NGINX_BACKUP" ] && [ -f "$NGINX_BACKUP" ]; then
        mv -f "$NGINX_BACKUP" "$NGINX_SITE"
    else
        rm -f "$NGINX_SITE" /etc/nginx/sites-enabled/guatfood
    fi
}

sed -e "s|your-domain.com|$DOMAIN|g" "$SCRIPT_DIR/nginx.conf" > "$NGINX_SITE"
ln -sfn "$NGINX_SITE" /etc/nginx/sites-enabled/guatfood
rm -f /etc/nginx/sites-enabled/default

if [ ! -s "$CERT_DIR/fullchain.pem" ] && [ ! -s "$CERT_DIR/privkey.pem" ]; then
    echo "  >> 未找到 TLS 证书，先生成 30 天自签证书（仅用于首次启动，请尽快替换）"
    openssl req -x509 -nodes -newkey rsa:2048 -days 30 \
        -keyout "$CERT_DIR/privkey.pem" \
        -out "$CERT_DIR/fullchain.pem" \
        -subj "/CN=$DOMAIN" \
        -addext "subjectAltName=DNS:$DOMAIN" >/dev/null 2>&1
    chmod 600 "$CERT_DIR/privkey.pem"
elif [ ! -s "$CERT_DIR/fullchain.pem" ] || [ ! -s "$CERT_DIR/privkey.pem" ]; then
    echo "证书文件不完整，请检查 $CERT_DIR/fullchain.pem 与 $CERT_DIR/privkey.pem"
    restore_nginx_site
    exit 1
fi

if ! nginx -t; then
    echo "Nginx 配置校验失败，恢复原配置并中止部署"
    restore_nginx_site
    exit 1
fi
if [ -n "$NGINX_BACKUP" ]; then rm -f "$NGINX_BACKUP"; fi
systemctl enable nginx
systemctl restart nginx

echo "[6/9] 安装 systemd 服务与备份定时器 ..."
# ReadWritePaths 必须与实际 UPLOAD_PATH 一致，否则 ProtectSystem=strict 下上传会失败
sed "s|/opt/guatfood/uploads|$UPLOAD_DIR|g" "$SCRIPT_DIR/guatfood.service" \
    > /etc/systemd/system/guatfood.service.tmp
install -m 644 /etc/systemd/system/guatfood.service.tmp /etc/systemd/system/guatfood.service
rm -f /etc/systemd/system/guatfood.service.tmp
install -m 755 "$SCRIPT_DIR/backup.sh" /usr/local/bin/guatfood-backup.sh
sed "s|/var/backups/guatfood|$BACKUP_DIR|g" \
    "$SCRIPT_DIR/guatfood-backup.service" > /etc/systemd/system/guatfood-backup.service
install -m 644 "$SCRIPT_DIR/guatfood-backup.timer" /etc/systemd/system/guatfood-backup.timer
install -d -m 700 -o root -g root "$BACKUP_DIR"
systemctl daemon-reload
systemctl enable guatfood
systemctl enable --now guatfood-backup.timer

echo "[7/9] 发布 jar（releases/current） ..."
RELEASE_ID="$(basename "$JAR" .jar)-$(sha256sum "$SCRIPT_DIR/$JAR" | cut -c1-12)"
RELEASE_DIR="$APP_DIR/releases/$RELEASE_ID"
install -d -o "$APP_USER" -g "$APP_USER" "$RELEASE_DIR"
install -m 0644 -o "$APP_USER" -g "$APP_USER" "$SCRIPT_DIR/$JAR" "$RELEASE_DIR/app.jar"

PREVIOUS_TARGET=""
if [ -L "$APP_DIR/current" ]; then
    PREVIOUS_TARGET="$(readlink -f "$APP_DIR/current" || true)"
fi
ln -sfn "$RELEASE_DIR" "$APP_DIR/current"
chown -h "$APP_USER:$APP_USER" "$APP_DIR/current"

echo "[8/9] 启动并健康检查（存活/就绪分离） ..."
systemctl restart guatfood

liveness_ok() {
    curl -fsS --max-time 2 \
        "http://127.0.0.1:${MGMT_PORT}/actuator/health/liveness" 2>/dev/null \
        | grep -q '"status"[[:space:]]*:[[:space:]]*"UP"'
}

readiness_check() {
    curl -fsS --max-time 3 \
        "http://127.0.0.1:${MGMT_PORT}/actuator/health/readiness" 2>/dev/null \
        | grep -q '"status"[[:space:]]*:[[:space:]]*"UP"'
}

readiness_body() {
    curl -sS --max-time 3 \
        "http://127.0.0.1:${MGMT_PORT}/actuator/health/readiness" 2>/dev/null || true
}

wait_for() {
    local check_fn="$1" seconds="$2"
    for _ in $(seq 1 "$seconds"); do
        if "$check_fn"; then
            return 0
        fi
        sleep 1
    done
    return 1
}

if ! wait_for liveness_ok 60; then
    echo "应用未通过存活检查（启动失败或管理端口 ${MGMT_PORT} 不可达）。最近日志："
    journalctl -u guatfood -n 80 --no-pager || true
    # Spring 启动阶段就会初始化 DataSource/Flyway：数据库故障同样会导致存活检查失败。
    # 先确认数据库可达，避免把“数据库宕机”误判成“新版本代码故障”而错误回滚。
    if ! db_reachable; then
        echo "数据库当前不可达：$DB_HOST:$DB_PORT / $DB_NAME（用户 $DB_USERNAME）。"
        echo "本次不自动回滚：数据库恢复后执行 systemctl restart guatfood 即可，当前版本保持不变。"
        exit 1
    fi
    if [ -n "$PREVIOUS_TARGET" ] && [ -d "$PREVIOUS_TARGET" ]; then
        echo "回滚到上一个版本：$PREVIOUS_TARGET"
        ln -sfn "$PREVIOUS_TARGET" "$APP_DIR/current"
        chown -h "$APP_USER:$APP_USER" "$APP_DIR/current"
        systemctl restart guatfood || true
        if wait_for liveness_ok 60; then
            if wait_for readiness_check 30; then
                echo "回滚后存活/就绪检查均通过；请根据日志修复新版本后重新发布。"
            else
                echo "回滚版本已存活，但数据库仍未就绪，请检查 MySQL 与 DB_* 配置："
                readiness_body || true
            fi
        else
            echo "回滚后仍未通过存活检查，请立即人工排查："
            journalctl -u guatfood -n 80 --no-pager || true
        fi
    fi
    exit 1
fi

if ! wait_for readiness_check 60; then
    echo "应用存活但未就绪，数据库可能不可用。最近就绪详情："
    readiness_body || true
    echo
    echo "排查建议：systemctl status mysql；mysqladmin ping；检查 $ENV_FILE 中的 DB_* 配置。"
    echo "本次不自动回滚：数据库故障无法通过回滚代码修复。"
    exit 1
fi

echo "[9/9] 清理旧版本（保留最近 3 个，且不删除 current/previous） ..."
CURRENT_TARGET="$(readlink -f "$APP_DIR/current" || true)"
idx=0
while IFS= read -r dir; do
    dir="${dir%/}"
    idx=$((idx + 1))
    [ "$idx" -le 3 ] && continue
    [ "$dir" = "$CURRENT_TARGET" ] && continue
    if [ -n "$PREVIOUS_TARGET" ] && [ "$dir" = "$PREVIOUS_TARGET" ]; then
        continue
    fi
    case "$dir" in
        "$APP_DIR"/releases/*) rm -rf -- "$dir" ;;
    esac
done < <(ls -1dt "$APP_DIR/releases"/*/ 2>/dev/null || true)

echo
echo "================================================================"
echo " 部署完成：https://$DOMAIN"
echo " 当前版本：$APP_DIR/current -> $RELEASE_DIR"
echo " 健康检查：http://127.0.0.1:${MGMT_PORT}/actuator/health/readiness"
echo " 备份目录：$BACKUP_DIR（每天 03:00，systemd timer）"
echo " 自签证书仅用于首次启动，请尽快替换为正式证书："
echo "   见 deploy/linux/README.md 第 4 节（acme.sh / 云厂商免费证书）"
echo " 服务状态：$(systemctl is-active guatfood || true)"
echo "================================================================"
