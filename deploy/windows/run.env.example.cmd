@echo off
REM ============================================================
REM  复制为 run.env.cmd 后填写真实值；run.env.cmd 已被 .gitignore 忽略
REM  注意：值请勿包含空格、单引号、# 等 shell 特殊字符
REM ============================================================
set SPRING_PROFILES_ACTIVE=prod
set DB_HOST=127.0.0.1
set DB_PORT=3306
set DB_NAME=guatfood
set DB_USERNAME=guatfood
REM 占位值使用纯 ASCII，run.cmd / 后端可稳定识别并拒绝启动
set DB_PASSWORD=CHANGE_ME_STRONG_DB_PASSWORD
set JWT_SECRET=CHANGE_ME_OPENSSL_RAND_BASE64_48
set INIT_PASSWORD=CHANGE_ME_ADMIN_INITIAL_PASSWORD

REM 生产不会创建演示账号；开发环境如需演示账号，请把 profile 改为 dev
REM 并额外设置 DEMO_PASSWORD（不要在生产设置演示账号密码）
REM set DEMO_PASSWORD=请改成开发演示账号强密码

REM 管理端口：只监听回环，供部署/监控健康检查；不要经 Nginx 暴露
set MANAGEMENT_PORT=8081
set MANAGEMENT_ADDRESS=127.0.0.1

REM 仅信任同机 Nginx 传来的 X-Forwarded-For
set TRUSTED_PROXIES=127.0.0.1,::1

REM 可选：上传目录（默认 backend\uploads），用于备份脚本定位图片
REM set UPLOAD_PATH=C:\guatfood\backend\uploads

REM JVM 参数（可按内存调整）
set JAVA_OPTS=-Xms256m -Xmx1024m -XX:+UseG1GC
