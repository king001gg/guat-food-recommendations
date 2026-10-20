@echo off
REM ============================================================
REM  启动后端（单 jar，已内置前端页面）
REM  首次使用：copy run.env.example.cmd run.env.cmd，编辑后运行本脚本
REM  启动后访问：http://127.0.0.1:8080
REM ============================================================
setlocal
cd /d "%~dp0..\..\backend"

if not exist "%~dp0run.env.cmd" (
    echo [ERROR] 缺少 deploy\windows\run.env.cmd
    echo 请先执行：
    echo   copy deploy\windows\run.env.example.cmd deploy\windows\run.env.cmd
    echo 然后填写 DB_PASSWORD / JWT_SECRET / INIT_PASSWORD。
    exit /b 1
)

call "%~dp0run.env.cmd"
if errorlevel 1 exit /b 1

REM 拒绝未填写或仍为模板占位值的配置（纯 ASCII，避免中文编码差异）
if not defined DB_PASSWORD goto :missing_env
if not defined JWT_SECRET goto :missing_env
if not defined INIT_PASSWORD goto :missing_env
if /I "%DB_PASSWORD%"=="CHANGE_ME_STRONG_DB_PASSWORD" goto :placeholder_env
if /I "%JWT_SECRET%"=="CHANGE_ME_OPENSSL_RAND_BASE64_48" goto :placeholder_env
if /I "%INIT_PASSWORD%"=="CHANGE_ME_ADMIN_INITIAL_PASSWORD" goto :placeholder_env

REM 仅监听本机回环，避免 8080 直接暴露到局域网；如需局域网直连请显式修改
set SERVER_ADDRESS=127.0.0.1

java %JAVA_OPTS% -jar target\guatfood-backend-1.0.0.jar
endlocal
exit /b %errorlevel%

:missing_env
echo [ERROR] run.env.cmd 缺少 DB_PASSWORD / JWT_SECRET / INIT_PASSWORD
exit /b 1

:placeholder_env
echo [ERROR] run.env.cmd 仍是模板占位值，请填写真实密码与随机 JWT_SECRET
echo          提示：openssl rand -base64 48
exit /b 1
