# Windows 部署指南（单 jar 方案）

默认采用「**单 jar**」方案：前端 `dist` 打进后端 jar，**只需一个 `java -jar`**，
浏览器访问 `http://localhost:8080` 即可看到完整站点（含接口与上传图片）。

## 一、安装依赖

1. JDK 17+：推荐 [Temurin 17](https://adoptium.net/)，或 `winget install EclipseAdoptium.Temurin.17.JDK`
2. Node.js 18+（仅打包前端时需要）
3. MySQL 8.x：`winget install Oracle.MySQL`，记住 root 密码

## 二、初始化数据库

先只创建数据库（表结构由应用启动时的 **Flyway** 自动创建/升级）：

```bat
mysql -uroot -p < backend\src\main\resources\db\init.sql
```

再创建应用专用账号（不要用 root 跑应用）：

```sql
CREATE USER 'guatfood'@'localhost' IDENTIFIED BY '你的强密码';
GRANT ALL PRIVILEGES ON guatfood.* TO 'guatfood'@'localhost';
FLUSH PRIVILEGES;
```

> 旧库首次启动时，Flyway 会自动建立基线（版本 1）并执行 `db/migration/` 中后续脚本，
> 无需手工建表，也不要把 `flyway.clean` 用于生产。

## 三、一键打包

双击运行本目录下的 `build.cmd`（或命令行执行），生成内置前端页面的 jar：

```bat
deploy\windows\build.cmd
```

产物：`backend\target\guatfood-backend-1.0.0.jar`

## 四、配置并启动

先复制环境变量模板，再填写真实密钥（`run.env.cmd` 已被 `.gitignore` 忽略，不会入库）：

```bat
copy deploy\windows\run.env.example.cmd deploy\windows\run.env.cmd
notepad deploy\windows\run.env.cmd
```

> `run.env.cmd` 含明文密码。建议立即限制访问权限（仅当前用户与管理员）：
>
> ```bat
> icacls deploy\windows\run.env.cmd /inheritance:r /grant:r "%USERNAME%:F" "*S-1-5-32-544:F"
> ```
>
> 模板中的 `CHANGE_ME_*` 占位值未替换时，`run.cmd` 和后端都会拒绝启动。

然后双击 `run.cmd`（或命令行执行）：

```bat
deploy\windows\run.cmd
```

启动成功后浏览器打开 **http://127.0.0.1:8080**（`run.cmd` 默认只监听本机回环）。

- 管理员账号：`admin`
- 密码：`run.env.cmd` 里设置的 `INIT_PASSWORD`
- 上传的图片保存在 `backend\uploads\`（通过 `/uploads/**` 访问）
- 种子数据里的「美食图鉴」及 51 张图片菜已打包进 jar，无需额外复制图片

> 生产首次启动（空库）只创建 `admin`，不会写入演示账号或虚假评分。
> `INIT_PASSWORD` 仅在首次初始化时使用；修改它不会重置已有账号密码。
> 首次登录后请立即在「个人中心 → 账号安全」修改密码（旧 Token 会立即失效），
> 或由管理员在「用户管理 → 重置密码」处理。

### 健康检查（管理端口）

管理端口默认只监听 `127.0.0.1:8081`，用于区分「应用启动失败」与「数据库未就绪」：

```bat
curl http://127.0.0.1:8081/actuator/health/liveness
curl http://127.0.0.1:8081/actuator/health/readiness
```

Nginx 配置已显式屏蔽 `/actuator`。

## 五、开机自启（可选）

- 用 [NSSM](https://nssm.cc/) 或 [WinSW](https://github.com/winsw/winsw) 把 `run.cmd` 注册为 Windows 服务
- 或在「任务计划程序」新建开机任务运行 `run.cmd`

## 六、备份与恢复

`backup.ps1` 会读取 `run.env.cmd` 中的 `DB_*` 配置，用 `mysqldump` 导出数据库，
并把上传目录压缩为 zip；密码只通过 `MYSQL_PWD` 进程环境变量传递，不出现在命令行参数中。

```bat
powershell -ExecutionPolicy Bypass -File deploy\windows\backup.ps1
```

可选参数：

| 参数 | 默认值 | 说明 |
|---|---|---|
| `-BackupDir` | `C:\guatfood\backups` | 备份输出目录 |
| `-RetentionDays` | `14` | 保留天数，超期自动删除 `guatfood-*.zip` |
| `-MysqldumpPath` | 自动从 PATH 查找 | MySQL 客户端完整路径 |
| `-UploadPath` | `backend\uploads` | 上传图片目录 |

定时执行：打开「任务计划程序」→ 创建基本任务 → 每天 03:00 →
启动程序 `powershell.exe`，参数：

```text
-NoProfile -ExecutionPolicy Bypass -File "E:\你的路径\deploy\windows\backup.ps1"
```

恢复（先停止后端）：

```bat
REM 1) 解压并恢复数据库（dump 内含 CREATE DATABASE/USE）
powershell -Command "Expand-Archive -Path 'C:\guatfood\backups\guatfood-db-<时间戳>.sql.zip' -DestinationPath '%TEMP%\guatfood-restore' -Force"
mysql -uroot -p < "%TEMP%\guatfood-restore\guatfood-db-<时间戳>.sql"

REM 2) 恢复上传图片（压缩包内是 uploads 目录下的文件）
powershell -Command "Expand-Archive -Path 'C:\guatfood\backups\guatfood-uploads-<时间戳>.zip' -DestinationPath 'C:\你的路径\backend\uploads' -Force"

REM 3) 重新启动后端并检查
deploy\windows\run.cmd
curl http://127.0.0.1:8081/actuator/health/readiness
```

> 备份包含用户数据，建议定期复制到移动硬盘 / 对象存储，并做一次恢复演练。

## 附：Nginx for Windows 方案（可选）

若希望前端与后端分离、由 Nginx 托管静态页面，可参考本目录 `nginx.conf`；
后端仍用同一 jar 以纯接口方式运行（`run.cmd` 不变即可，内置前端不影响接口）。

架构：

```
浏览器 → Nginx :80 ─┬─ /、/assets/         → C:\guatfood\frontend\dist（静态文件）
                     ├─ /api/               → 127.0.0.1:8080（后端接口）
                     └─ /uploads/           → 127.0.0.1:8080（上传图片）
```

### 1. 下载并解压

从 [nginx.org/en/download.html](http://nginx.org/en/download.html) 下载 **Windows stable** 版 zip，
解压到 **不含空格和中文的路径**，例如 `C:\nginx`。

### 2. 替换配置

```bat
copy C:\nginx\conf\nginx.conf C:\nginx\conf\nginx.conf.bak
copy deploy\windows\nginx.conf C:\nginx\conf\nginx.conf
```

然后编辑 `C:\nginx\conf\nginx.conf`，把 `root` 改成本机实际的 dist 目录（用正斜杠）：

```nginx
root   C:/guatfood/frontend/dist;
```

> 该目录由 `npm run build` / `build.cmd` 生成。即使 jar 已内置前端，
> Nginx 方案仍需要这个独立的 `frontend\dist` 目录供静态托管。

改完先校验语法（**必须在 `C:\nginx` 目录下执行**，否则找不到 `logs/`、`temp/`）：

```bat
cd /d C:\nginx
nginx.exe -t
```

看到 `syntax is ok`、`test is successful` 再继续。

### 3. 启动

先启动后端（`run.cmd`），确认 `http://127.0.0.1:8080` 能打开，
再启动 Nginx：

```bat
cd /d C:\nginx
start nginx
```

> 不要双击 `nginx.exe`：它需要以自身目录为工作目录，用 `start nginx` 让它后台运行。

验证进程：`tasklist | findstr nginx`（通常有两个：主进程 + worker）。

浏览器访问 **http://localhost** 即可。

`run.cmd` 已默认设置 `SERVER_ADDRESS=127.0.0.1`，后端只监听本机回环，
8080 不会直接暴露到局域网。若确实需要局域网直连，请显式修改 `run.cmd` 中的这一行。

### 4. 验证清单

| 检查项 | 操作 | 预期 |
|---|---|---|
| 首页 | 访问 `http://localhost` | 正常显示 |
| SPA 回退 | 从排行榜点进任意窗口详情，直接刷新该地址（如 `/windows/2`） | 正常显示，不是 404 |
| 接口 | 打开排行榜页面（会请求 `/api/windows`） | 有数据返回 |
| **大图上传** | 管理员登录，上传一张 **>1MB** 的菜品图片 | 成功（验证 `client_max_body_size 20m` 生效） |
| 静态缓存 | `curl -I http://localhost/assets/<任一带 hash 的文件>.js` | `Cache-Control: public, max-age=31536000, immutable` |
| 入口不缓存 | `curl -I http://localhost/index.html` | `Cache-Control: no-cache` |
| gzip | `curl -s -D - -o NUL -H "Accept-Encoding: gzip" http://localhost/assets/<某个 .js>` | 含 `Content-Encoding: gzip`（HEAD 请求不会压缩，必须用 GET） |
| 管理端点 | `curl -I http://localhost/actuator/health` | `404`（Nginx 防御性屏蔽） |

### 5. 常用命令

```bat
cd /d C:\nginx
nginx.exe -t              REM 校验配置
nginx.exe -s reload       REM 改完配置后热重载
nginx.exe -s quit         REM 优雅停止
nginx.exe -s stop         REM 强制停止
```

日志：`C:\nginx\logs\access.log`、`C:\nginx\logs\error.log`。排查问题先看 `error.log`。

### 6. 开机自启

Windows 版 Nginx **不支持注册为系统服务**，用 [NSSM](https://nssm.cc/) 包装：

```bat
nssm install guatfood-nginx C:\nginx\nginx.exe
nssm set guatfood-nginx AppDirectory C:\nginx
nssm set guatfood-nginx Start SERVICE_AUTO_START
nssm start guatfood-nginx
```

关键是 `AppDirectory` 必须设为 `C:\nginx`。也可改用「任务计划程序」开机运行 `nginx.exe`，
但同样要设置「起始于」为 `C:\nginx`。

### 7. 常见问题

**上传大图报 413**
- Nginx 返回的 413 是 **HTML 页面** → 说明 Nginx 的 `client_max_body_size` 没生效，检查配置是否替换、是否执行过 `-s reload`
- 前端提示「文件大小超出限制」（JSON，code 400）→ 那是后端的限制：单文件 10MB / 单请求 20MB
  （见 `backend/src/main/resources/application.yml` 的 `spring.servlet.multipart`），属于正常业务提示

**接口 404 / 502**
- `404`：`proxy_pass` 地址写错，或请求没走 `/api/` 前缀
- `502`：后端没启动或端口不是 8080，先用 `curl http://127.0.0.1:8080/api/canteens` 确认
- `504`：后端处理超时（当前 `proxy_read_timeout 60s`）

**刷新页面 404**
`try_files $uri $uri/ /index.html;` 没生效，确认配置已替换并 `nginx.exe -s reload`。

**启动失败 / 80 端口被占用**

```bat
netstat -ano | findstr :80
```

常见占用者：IIS（`net stop W3SVC`）、Skype、`http.sys` 相关服务。
也可把 `listen 80;` 改成 `listen 8080;`（注意与后端端口冲突，需同步改后端的 `server.port`）。

**静态页 403 / 404**
`root` 路径拼写错误，或运行 Nginx 的账户没有该目录的读取权限。
用 NSSM 注册服务时默认以 `LocalSystem` 运行，一般不会有权限问题。

**改完配置不生效**
先 `nginx.exe -t` 确认语法，再 `nginx.exe -s reload`；`-t` 报错的配置不会被加载。

> Windows 版 Nginx 的已知限制：`worker_processes` 只能为 1（本配置已设置）、不支持
> `reuseport`、没有原生服务管理。长期对外提供服务仍建议使用 `deploy/linux` 方案。
