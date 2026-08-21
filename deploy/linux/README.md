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

## 2. 上传到服务器

```bash
scp -r deploy/linux user@服务器IP:/tmp/
scp backend/target/guatfood-backend-1.0.0.jar user@服务器IP:/tmp/deploy/linux/
scp backend/src/main/resources/db/init.sql user@服务器IP:/tmp/deploy/linux/
```

## 3. 一键部署

在服务器上（需要 root/sudo）：

```bash
cd /tmp/deploy/linux
sudo bash deploy.sh 你的域名.com
```

脚本会自动：安装依赖 → 创建 `guatfood` 运行用户 → 复制 jar → 写入环境变量模板
→ 安装 systemd 服务 → 安装 Nginx 配置 → 初始化 MySQL。

> 脚本中途会提示你编辑 `/etc/guatfood/guatfood.env`，务必把
> `DB_PASSWORD`、`JWT_SECRET`、`INIT_PASSWORD` 改成强密码/强随机值。

## 4. 配置 HTTPS 证书（acme.sh + 阿里云/腾讯云 DNS）

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

> 也可用阿里云/腾讯云控制台的免费 DV 证书，下载后上传到 `/etc/nginx/ssl/`。

## 5. 启动与验证

```bash
systemctl restart guatfood
systemctl reload nginx
systemctl status guatfood          # 看到 Started GuatFoodApplication 即成功
curl -k https://127.0.0.1/api/windows?page=1&size=1   # 应返回 {"code":200,...}
```

浏览器访问 `https://你的域名.com`，管理员账号 `admin`，密码为 `guatfood.env` 里的 `INIT_PASSWORD`。

## 6. 防火墙加固（服务器本机）

```bash
sudo ufw allow 80/tcp && sudo ufw allow 443/tcp && sudo ufw limit 22/tcp
sudo ufw --force enable
```

## 7. 备份（建议 cron 定时）

```bash
# 每天 3 点备份数据库 + 上传图片
0 3 * * * mysqldump -u guatfood -p'密码' guatfood > /backup/guatfood_$(date +\%F).sql && tar -czf /backup/uploads_$(date +\%F).tgz /opt/guatfood/uploads/
```

## 手动部署（不用 deploy.sh 时）

按 `deploy.sh` 内部顺序手动执行即可：安装依赖 → 建用户/目录 → 复制 jar →
编辑 `/etc/guatfood/guatfood.env` → 复制 `guatfood.service` → 复制 `nginx.conf`
并替换域名 → `mysql < init.sql` → `systemctl enable --now guatfood` → `nginx -s reload`。
