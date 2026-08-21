@echo off
REM ============================================================
REM  启动后端（单 jar，已内置前端页面）
REM  请先修改下面的数据库密码 / JWT 密钥 / 初始管理员密码
REM  启动后访问：http://localhost:8080
REM ============================================================
setlocal
cd /d "%~dp0..\..\backend"

set SPRING_PROFILES_ACTIVE=prod
set DB_HOST=127.0.0.1
set DB_PORT=3306
set DB_NAME=guatfood
set DB_USERNAME=guatfood
set DB_PASSWORD=请改成你的数据库密码
set JWT_SECRET=请改成一段足够长的随机字符串
set INIT_PASSWORD=请改成你的管理员初始密码

java -jar target\guatfood-backend-1.0.0.jar
endlocal
