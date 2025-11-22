@echo off
chcp 65001 >nul
echo ========================================
echo    人力资源管理系统 - 前端安装脚本
echo ========================================
echo.

cd /d %~dp0

echo [1/4] 检查Node.js环境...
node -v >nul 2>&1
if errorlevel 1 (
    echo ❌ 错误: 未安装Node.js
    echo 请访问 https://nodejs.org/ 下载并安装Node.js
    pause
    exit /b 1
)
echo ✅ Node.js已安装

echo.
echo [2/4] 安装项目依赖...
call npm install
if errorlevel 1 (
    echo ❌ 依赖安装失败
    pause
    exit /b 1
)

echo.
echo [3/4] 安装Element Plus UI库...
call npm install element-plus @element-plus/icons-vue
if errorlevel 1 (
    echo ❌ Element Plus安装失败
    pause
    exit /b 1
)

echo.
echo [4/4] 安装Axios HTTP库...
call npm install axios
if errorlevel 1 (
    echo ❌ Axios安装失败
    pause
    exit /b 1
)

echo.
echo ========================================
echo ✅ 安装完成！
echo ========================================
echo.
echo 🚀 运行以下命令启动开发服务器:
echo    npm run dev
echo.
echo 或者直接运行: start-dev.bat
echo.
pause
