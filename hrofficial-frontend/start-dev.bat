@echo off
chcp 65001 >nul
echo ========================================
echo    人力资源管理系统 - 启动开发服务器
echo ========================================
echo.

cd /d %~dp0

echo 🚀 正在启动开发服务器...
echo 📱 访问地址: http://localhost:5173
echo 🔄 热重载已启用
echo ⏹️  按 Ctrl+C 停止服务器
echo.

call npm run dev
