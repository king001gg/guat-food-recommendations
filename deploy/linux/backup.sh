#!/usr/bin/env bash
set -euo pipefail

# ============================================================
#  桂航美食推荐排行榜 —— 数据库 + 上传图片备份
#  用法：sudo /usr/local/bin/guatfood-backup.sh
#  环境：读取 /etc/guatfood/guatfood.env（root 600）
#  定时：由 guatfood-backup.timer 每天触发
#  说明：密码通过 MYSQL_PWD 传给 mysqldump，不出现在进程参数和日志中
# ============================================================

umask 077

ENV_FILE="${GUATFOOD_ENV_FILE:-/etc/guatfood/guatfood.env}"
[ -r "$ENV_FILE" ] || { echo "缺少环境文件: $ENV_FILE" >&2; exit 1; }

set -a
# shellcheck disable=SC1090
. "$ENV_FILE"
set +a

: "${DB_NAME:?DB_NAME 未设置}"
: "${DB_USERNAME:?DB_USERNAME 未设置}"
: "${DB_PASSWORD:?DB_PASSWORD 未设置}"

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/guatfood}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"
UPLOAD_PATH="${UPLOAD_PATH:-/opt/guatfood/uploads/}"

case "$RETENTION_DAYS" in
    ''|*[!0-9]*) echo "BACKUP_RETENTION_DAYS 必须是正整数" >&2; exit 1 ;;
esac
[ "$RETENTION_DAYS" -ge 1 ] || { echo "BACKUP_RETENTION_DAYS 至少为 1" >&2; exit 1; }

mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"

STAMP="$(date +%Y%m%d-%H%M%S)"
DB_FILE="$BACKUP_DIR/guatfood-db-$STAMP.sql.gz"
UP_FILE="$BACKUP_DIR/guatfood-uploads-$STAMP.tar.gz"
DB_TMP="$DB_FILE.tmp"

cleanup() {
    rm -f "$DB_TMP" "$UP_FILE.tmp"
}
trap cleanup EXIT

echo "[1/3] 备份数据库 $DB_NAME ..."
# --no-defaults 必须是 mysqldump 的第一个参数，避免读取 root 的 ~/.my.cnf 干扰
DUMP_OPTS=(--no-defaults)
# MySQL 8 备份单个库也需要 PROCESS 权限；--no-tablespaces 可规避，
# 但部分 MariaDB 版本没有该参数，因此按实际支持情况追加。
if mysqldump --no-defaults --help 2>/dev/null | grep -q -- '--no-tablespaces'; then
    DUMP_OPTS+=(--no-tablespaces)
fi
DUMP_OPTS+=(
    --host="$DB_HOST"
    --port="$DB_PORT"
    --user="$DB_USERNAME"
    --single-transaction
    --quick
    --routines
    --triggers
    --databases "$DB_NAME"
)
MYSQL_PWD="$DB_PASSWORD" mysqldump "${DUMP_OPTS[@]}" | gzip -9 > "$DB_TMP"
mv "$DB_TMP" "$DB_FILE"

echo "[2/3] 备份上传目录 $UPLOAD_PATH ..."
if [ -d "$UPLOAD_PATH" ]; then
    UPLOAD_PARENT="$(dirname "${UPLOAD_PATH%/}")"
    UPLOAD_NAME="$(basename "${UPLOAD_PATH%/}")"
    tar -czf "$UP_FILE.tmp" -C "$UPLOAD_PARENT" "$UPLOAD_NAME"
    mv "$UP_FILE.tmp" "$UP_FILE"
else
    echo "  上传目录不存在，跳过图片备份"
fi

echo "[3/3] 清理超过 $RETENTION_DAYS 天的旧备份 ..."
find "$BACKUP_DIR" -maxdepth 1 -type f -name 'guatfood-*.gz' -mtime +"$RETENTION_DAYS" -delete

echo "备份完成："
if [ -f "$DB_FILE" ]; then ls -lh "$DB_FILE"; fi
if [ -f "$UP_FILE" ]; then ls -lh "$UP_FILE"; fi
