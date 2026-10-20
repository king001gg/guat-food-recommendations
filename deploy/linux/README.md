# Linux 部署指南（Nginx + HTTPS + systemd，国内安全部署）

采用「**单 jar + Nginx 反代**」方案：后端 jar 内置前端页面，Nginx 负责
HTTPS 终止与反向代理，后端仅监听 `127.0.0.1` 不暴露公网。

> 国内服务器部署前请先完成 **ICP 备案** 与 **公安备案**（详见仓库根目录 [`SECURITY.md`](../../SECURITY.md)）。

## 0. 前置条件

- 一台全新 **Ubuntu 22.04 / Debian 12** 服务器（阿里云/腾讯云轻量均可）
- 域名已实名认证、**ICP 备案通过**、A 记录解析到服务器
- 安全组开放 `80`、`443`，`22`(SSH) 建议仅限你的 IP

## 1. 本地打包（单 jar）

```bash
bash deploy/linux/build.sh
# 产物：backend/target/guatfood-backend-1.0.0.jar
```

脚本会执行 `npm ci`、前端构建，并运行后端测试（H2 内存库）后再打包。

## 2. 上传到服务器

```bash
scp -r deploy/linux user@服务器IP:/tmp/
scp backend/target/guatfood-backend-1.0.0.jar user@服务器IP:/tmp/deploy/linux/
```

> 不再需要上传 `init.sql`。数据库表结构与后续升级由应用启动时的 **Flyway** 自动执行
> （`backend/src/main/resources/db/migration/`）。

## 3. 一键部署

在服务器上（需要 root/sudo）：

```bash
cd /tmp/deploy/linux
sudo bash deploy.sh 你的域名.com
```

脚本会自动：

1. 安装 `openjdk-17 / nginx / mysql-server / openssl`
2. 创建 `guatfood` 运行用户与 `/opt/guatfood/{releases,uploads}`
3. 准备 `/etc/guatfood/guatfood.env`（首次会停下等待你编辑）
4. 创建数据库与最小权限账号（表结构由 Flyway 启动时迁移）
5. 安装 Nginx（证书不存在时先生成 30 天自签证书）、开启 gzip 与单 IP 限流
6. 安装 systemd 服务（最小权限加固）与每日备份定时器
7. 发布到 `releases/<版本>-<sha12>` 并切换 `current`
8. 健康检查：存活失败自动回滚；数据库未就绪则明确提示且不误回滚
9. 清理旧版本（保留最近 3 个，且保护 `current` / `previous`）

> 脚本中途会提示你编辑 `/etc/guatfood/guatfood.env`。`DB_PASSWORD` 至少 8 位，
> `JWT_SECRET` 至少 32 字节，`INIT_PASSWORD` 至少 8 位；检测到模板占位值会直接中止。
> 生产不会创建演示账号，首次空库只创建 `admin`。

## 4. 替换 HTTPS 证书（acme.sh + 阿里云/腾讯云 DNS）

部署脚本首次启动时会生成 **自签证书**（浏览器会提示不安全），请尽快替换为正式证书：

```bash
curl https://get.acme.sh | sh
# 阿里云：在 RAM 控制台创建 AccessKey，授予 DNS 管理权限
export Ali_Key="你的AccessKeyID" Ali_Secret="你的AccessKeySecret"
~/.acme.sh/acme.sh --issue --dns dns_ali -d 你的域名.com -d www.你的域名.com
~/.acme.sh/acme.sh --install-cert -d 你的域名.com \
  --key-file /etc/nginx/ssl/privkey.pem \
  --fullchain-file /etc/nginx/ssl/fullchain.pem \
  --reloadcmd "systemctl reload nginx"
```

> 也可用阿里云/腾讯云控制台的免费 DV 证书，下载后覆盖 `/etc/nginx/ssl/` 下的两个文件，
> 再执行 `systemctl reload nginx`。

## 5. 发布与回滚

发布目录结构：

```text
/opt/guatfood/
├── releases/
│   ├── guatfood-backend-1.0.0-<sha12>/app.jar
│   └── ...
├── current -> releases/guatfood-backend-1.0.0-<sha12>
└── uploads/
```

- 部署脚本会保留最近 3 个版本；健康检查失败自动切回上一版，并确认回滚版本存活。
- 手动回滚：

```bash
ls -1dt /opt/guatfood/releases/*
ln -sfn /opt/guatfood/releases/<旧版本目录> /opt/guatfood/current
systemctl restart guatfood
curl -fsS http://127.0.0.1:8081/actuator/health/readiness
```

## 6. 启动与健康检查

```bash
systemctl is-active guatfood       # active
systemctl status guatfood          # 看到 Started GuatFoodApplication 即成功
systemctl status guatfood-backup.timer

# 管理端口只监听回环，不在 Nginx 中暴露
curl -fsS http://127.0.0.1:8081/actuator/health/liveness
curl -fsS http://127.0.0.1:8081/actuator/health/readiness

curl -k https://127.0.0.1/api/windows?page=1&size=1   # 应返回 {"code":200,...}
```

- `liveness` 失败通常表示进程/端口/配置问题 → 部署脚本会自动回滚
- `readiness` 失败通常表示 **数据库不可用** → 脚本不会回滚代码，请检查 MySQL：
  `systemctl status mysql`、`mysqladmin ping`、`/etc/guatfood/guatfood.env` 中的 `DB_*`

浏览器访问 `https://你的域名.com`，管理员账号 `admin`，密码为 `guatfood.env` 里的
`INIT_PASSWORD`。该密码仅在空库首次启动时使用；修改环境变量不会重置已有账号。
首次登录后建议立即在「个人中心 → 账号安全」修改密码。

## 7. 防火墙加固（服务器本机）

```bash
sudo ufw allow 80/tcp && sudo ufw allow 443/tcp && sudo ufw limit 22/tcp
sudo ufw --force enable
```

> `8080` 与 `8081` 均只监听 `127.0.0.1`，不需要对公网开放。

## 8. 备份与恢复

部署脚本会安装：

- `/usr/local/bin/guatfood-backup.sh`：MySQL（`mysqldump`）+ 上传目录
- `guatfood-backup.timer`：每天 **03:00** 执行，错过则开机补跑
- 默认目录 `/var/backups/guatfood`，保留 **14 天**（可改 `BACKUP_DIR` / `BACKUP_RETENTION_DAYS`）

```bash
# 手动立即备份
sudo systemctl start guatfood-backup.service
journalctl -u guatfood-backup.service -n 50 --no-pager
ls -lh /var/backups/guatfood/

# 查看下次执行时间
systemctl list-timers guatfood-backup.timer
```

恢复（建议先停应用，恢复后再启动）：

```bash
sudo systemctl stop guatfood

# 1) 恢复数据库（dump 内含 CREATE DATABASE/USE，使用有权限的账号）
gunzip -c /var/backups/guatfood/guatfood-db-<时间戳>.sql.gz | mysql -uroot -p

# 2) 恢复上传图片（压缩包内是 uploads/ 目录）
tar -xzf /var/backups/guatfood/guatfood-uploads-<时间戳>.tar.gz -C /opt/guatfood
chown -R guatfood:guatfood /opt/guatfood/uploads

sudo systemctl start guatfood
curl -fsS http://127.0.0.1:8081/actuator/health/readiness
```

> 备份目录包含用户数据，建议再同步到对象存储或另一台机器；`BACKUP_DIR` 若改动，
> 需同步修改 `guatfood-backup.service` 的 `ReadWritePaths`。`UPLOAD_PATH` 会在部署时
> 自动写入 `guatfood.service` 的 `ReadWritePaths`，但自定义路径仍需确保 `guatfood`
> 用户可写。

## 手动部署（不用 deploy.sh 时）

按 `deploy.sh` 内部顺序手动执行即可：安装依赖 → 建用户/目录 → 编辑 `/etc/guatfood/guatfood.env`
→ 安装 systemd 服务与备份脚本 → 安装 Nginx 配置并替换域名 → 创建数据库与账号
→ 把 jar 复制为 `/opt/guatfood/releases/<版本>/app.jar` → `ln -sfn` 到 `/opt/guatfood/current`
→ `systemctl enable --now guatfood` → `nginx -s reload`。
表结构由 Flyway 在首次启动时自动创建，无需手工执行建表 SQL。
