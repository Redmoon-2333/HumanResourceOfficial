@echo off
chcp 65001 >nul
REM ============================================================
REM 人力资源中心官网 - Windows部署脚本
REM 用法: deploy.bat [命令]
REM 命令: start | stop | restart | status | logs | rebuild | clean
REM ============================================================

setlocal EnableDelayedExpansion

REM 项目配置
set PROJECT_NAME=hrofficial
set DEPLOY_DIR=%~dp0
set COMPOSE_FILE=%DEPLOY_DIR%docker-compose.yml

REM 颜色设置（Windows 10+）
set "GREEN=[92m"
set "RED=[91m"
set "YELLOW=[93m"
set "BLUE=[94m"
set "NC=[0m"

REM 检查环境
:check_env
if not exist "%DEPLOY_DIR%.env" (
    echo %RED%[ERROR]%NC% 未找到 .env 文件！
    echo %BLUE%[INFO]%NC% 请复制 .env.example 为 .env 并填写配置
    exit /b 1
)

REM 检查Docker
docker --version >nul 2>&1
if errorlevel 1 (
    echo %RED%[ERROR]%NC% Docker 未安装或未启动！
    exit /b 1
)

REM 主逻辑
if "%1"=="" goto help
if "%1"=="start" goto start
if "%1"=="stop" goto stop
if "%1"=="restart" goto restart
if "%1"=="status" goto status
if "%1"=="logs" goto logs
if "%1"=="rebuild" goto rebuild
if "%1"=="clean" goto clean
if "%1"=="help" goto help
goto unknown

:start
echo %BLUE%[INFO]%NC% 正在启动服务...
call :check_env
if errorlevel 1 exit /b 1
docker-compose -f "%COMPOSE_FILE%" up -d
echo %GREEN%[SUCCESS]%NC% 服务启动完成！
goto status

:stop
echo %BLUE%[INFO]%NC% 正在停止服务...
docker-compose -f "%COMPOSE_FILE%" down
echo %GREEN%[SUCCESS]%NC% 服务已停止
goto :eof

:restart
echo %BLUE%[INFO]%NC% 正在重启服务...
call :stop
call :start
goto :eof

:status
echo %BLUE%[INFO]%NC% 服务状态：
docker-compose -f "%COMPOSE_FILE%" ps
goto :eof

:logs
if "%2"=="" (
    docker-compose -f "%COMPOSE_FILE%" logs -f --tail=100
) else (
    docker-compose -f "%COMPOSE_FILE%" logs -f --tail=100 %2
)
goto :eof

:rebuild
echo %BLUE%[INFO]%NC% 正在重新构建服务...
call :check_env
if errorlevel 1 exit /b 1
docker-compose -f "%COMPOSE_FILE%" build --no-cache
docker-compose -f "%COMPOSE_FILE%" up -d --force-recreate
echo %GREEN%[SUCCESS]%NC% 重建完成！
goto :eof

:clean
echo %YELLOW%[WARN]%NC% 这将删除所有容器、网络和数据卷！
set /p confirm="确认继续？(y/N): "
if /i "!confirm!"=="y" (
    docker-compose -f "%COMPOSE_FILE%" down -v --rmi local
    echo %GREEN%[SUCCESS]%NC% 清理完成
) else (
    echo %BLUE%[INFO]%NC% 已取消
)
goto :eof

:help
echo 用法: %~nx0 [命令]
echo.
echo 可用命令：
echo   start     启动所有服务
echo   stop      停止所有服务
echo   restart   重启所有服务
echo   status    查看服务状态
echo   logs      查看日志 (可选: logs backend/mysql/redis)
echo   rebuild   重新构建并启动服务
echo   clean     清理所有容器和数据
echo   help      显示帮助信息
goto :eof

:unknown
echo %RED%[ERROR]%NC% 未知命令: %1
goto help

endlocal
