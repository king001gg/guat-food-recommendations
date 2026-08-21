@echo off
REM ============================================================
REM  一键打包脚本（单 jar 部署）
REM  流程：前端 build -> 复制 dist 到后端 static -> 后端打 jar
REM  运行前需已安装：Node.js 18+、JDK 17+
REM  产物：backend\target\guatfood-backend-1.0.0.jar
REM ============================================================
setlocal
cd /d "%~dp0..\.."

echo [1/3] 构建前端 ...
pushd frontend
call npm install
if errorlevel 1 goto :fail
call npm run build
if errorlevel 1 goto :fail
popd

echo [2/3] 复制 frontend\dist 到 backend\src\main\resources\static ...
if exist "backend\src\main\resources\static" rmdir /S /Q "backend\src\main\resources\static"
mkdir "backend\src\main\resources\static"
xcopy /E /Y /Q "frontend\dist\*" "backend\src\main\resources\static\"
if errorlevel 1 goto :fail

echo [3/3] 打包后端 jar ...
pushd backend
call mvnw.cmd clean package -DskipTests
if errorlevel 1 goto :fail
popd

echo.
echo 打包完成：backend\target\guatfood-backend-1.0.0.jar
endlocal
exit /b 0

:fail
echo.
echo 打包失败，请检查上方错误信息。
endlocal
exit /b 1
