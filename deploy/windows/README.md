# Windows 部署指南（单 jar 方案）

默认采用「**单 jar**」方案：前端 `dist` 打进后端 jar，**只需一个 `java -jar`**，
浏览器访问 `http://localhost:8080` 即可看到完整站点（含接口与上传图片）。

## 一、安装依赖

1. JDK 17+：推荐 [Temurin 17](https://adoptium.net/)，或 `winget install EclipseAdoptium.Temurin.17.JDK`
2. Node.js 18+（仅打包前端时需要）
3. MySQL 8.x：`winget install Oracle.MySQL`，记住 root 密码

## 二、初始化数据库

在命令行（或 MySQL 客户端）执行建表脚本：

```bat
mysql -uroot -p < backend\src\main\resources\db\init.sql
```

再创建应用专用账号（不要用 root 跑应用）：

```sql
CREATE USER 'guatfood'@'localhost' IDENTIFIED BY '你的强密码';
GRANT ALL PRIVILEGES ON guatfood.* TO 'guatfood'@'localhost';
FLUSH PRIVILEGES;
```

## 三、一键打包

双击运行本目录下的 `build.cmd`（或命令行执行），生成内置前端页面的 jar：

```bat
deploy\windows\build.cmd
```

产物：`backend\target\guatfood-backend-1.0.0.jar`

## 四、配置并启动

编辑 `deploy\windows\run.cmd`，把 `DB_PASSWORD`、`JWT_SECRET`、`INIT_PASSWORD`
改成你自己的值，然后双击运行（或命令行 `run.cmd`）。

启动成功后浏览器打开 **http://localhost:8080**。

- 管理员账号：`admin`
- 密码：`run.cmd` 里设置的 `INIT_PASSWORD`
- 上传的图片保存在 `backend\uploads\`（通过 `/uploads/**` 访问）

> 首次启动会自动把管理员、演示账号、食堂/档口/菜品等种子数据写入 MySQL。

## 五、开机自启（可选）

- 用 [NSSM](https://nssm.cc/) 或 [WinSW](https://github.com/winsw/winsw) 把 `run.cmd` 注册为 Windows 服务
- 或在「任务计划程序」新建开机任务运行 `run.cmd`

## 附：Nginx for Windows 方案（可选）

若希望前端与后端分离、由 Nginx 托管静态页面，可参考本目录 `nginx.conf`；
后端仍用同一 jar 以纯接口方式运行（`run.cmd` 不变即可，内置前端不影响接口）。
