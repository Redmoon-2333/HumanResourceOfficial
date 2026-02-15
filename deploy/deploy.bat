@echo off
chcp 65001 >nul
REM ============================================================
REM 人力资源中心官网 - 自动化部署脚本 (Windows)
REM 适用于2G内存服务器的一键部署
REM ============================================================

setlocal EnableDelayedExpansion

REM 脚本目录
set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%.."

echo ============================================================
echo   人力资源中心官网 - 自动化部署脚本
echo ============================================================
echo.

REM 检查Docker是否安装
echo [1/6] 检查Docker环境...
docker --version >nul 2>&1
if errorlevel 1 (
    echo [错误] Docker未安装
    echo 请先安装Docker: https://docs.docker.com/get-docker/
    exit /b 1
)

docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo [错误] Docker Compose未安装
    echo 请先安装Docker Compose: https://docs.docker.com/compose/install/
    exit /b 1
)
echo [OK] Docker环境检查通过
echo.

REM 检查环境变量文件
echo [2/6] 检查环境变量配置...
if not exist "%SCRIPT_DIR%.env" (
    echo [错误] 未找到.env文件
    echo 请执行以下操作:
    echo   1. copy %SCRIPT_DIR%.env.example %SCRIPT_DIR%.env
    echo   2. 编辑.env文件，填写实际配置
    exit /b 1
)

REM 读取环境变量检查关键配置
for /f "tokens=1,2 delims==" %%a in ('type "%SCRIPT_DIR%.env" ^| findstr /v "^#"') do (
    set "key=%%a"
    set "val=%%b"
    set "key=!key: =!"
    if "!key!"=="DB_PASSWORD" (
        if "!val!"=="your_secure_password_here" (
            echo [错误] 请设置数据库密码 ^(DB_PASSWORD^)
            exit /b 1
        )
    )
    if "!key!"=="JWT_SECRET" (
        if "!val!"=="your_jwt_secret_key_must_be_at_least_32_characters_long" (
            echo [错误] 请设置JWT密钥 ^(JWT_SECRET^)
            exit /b 1
        )
    )
)
echo [OK] 环境变量检查通过
echo.

REM 检查系统资源
echo [3/6] 检查系统资源...
for /f "skip=1" %%a in ('wmic ComputerSystem get TotalPhysicalMemory') do (
    if not defined TOTAL_MEM set "TOTAL_MEM=%%a"
)
set /a TOTAL_MEM_MB=%TOTAL_MEM:~0,-6%
echo 总内存: %TOTAL_MEM_MB%MB
if %TOTAL_MEM_MB% LSS 1800 (
    echo [警告] 内存小于2GB ^(%TOTAL_MEM_MB%MB^)，可能影响性能
) else (
    echo [OK] 内存充足
)
echo.

REM 清理旧容器和数据（可选）
echo [4/6] 清理旧容器...
set /p CLEANUP="是否清理旧容器和数据? (y/N): "
if /i "%CLEANUP%"=="y" (
    echo 停止并删除旧容器...
    cd /d "%SCRIPT_DIR%"
    docker-compose down --remove-orphans

    set /p PRUNE="是否清理Docker缓存? (y/N): "
    if /i "!PRUNE!"=="y" (
        echo 清理Docker缓存...
        docker system prune -f
    )
)
echo [OK] 清理完成
echo.

REM 构建和启动服务
echo [5/6] 构建和启动服务...
cd /d "%SCRIPT_DIR%"

echo 拉取基础镜像...
docker-compose pull

echo 构建服务...
docker-compose build --no-cache

echo 启动服务...
docker-compose up -d
echo [OK] 服务启动完成
echo.

REM 等待服务就绪
echo [6/6] 等待服务就绪...
echo 等待服务启动，这可能需要1-2分钟...
timeout /t 30 /nobreak >nul

REM 检查后端健康状态
cd /d "%SCRIPT_DIR%"
for /f "tokens=2 delims==" %%a in ('type .env ^| findstr "BACKEND_PORT"') do (
    set "BACKEND_PORT=%%a"
    set "BACKEND_PORT=!BACKEND_PORT: =!"
)
if not defined BACKEND_PORT set "BACKEND_PORT=8080"

echo 检查后端服务健康状态...
curl -sf http://localhost:%BACKEND_PORT%/actuator/health >nul 2>&1
if errorlevel 1 (
    echo [警告] 后端服务可能还在启动中，请稍后手动检查
) else (
    echo [OK] 后端服务已就绪
)
echo.

REM 显示部署结果
echo ============================================================
echo   部署完成！
echo ============================================================
echo.
echo 服务状态:
cd /d "%SCRIPT_DIR%"
docker-compose ps
echo.
echo 访问地址:
for /f "tokens=2 delims==" %%a in ('type .env ^| findstr "FRONTEND_PORT"') do (
    set "FRONTEND_PORT=%%a"
    set "FRONTEND_PORT=!FRONTEND_PORT: =!"
)
if not defined FRONTEND_PORT set "FRONTEND_PORT=80"
echo   前端: http://localhost:%FRONTEND_PORT%
echo   后端API: http://localhost:%BACKEND_PORT%
echo   后端健康检查: http://localhost:%BACKEND_PORT%/actuator/health
echo.
echo 常用命令:
echo   查看日志: docker-compose logs -f
echo   停止服务: docker-compose down
echo   重启服务: docker-compose restart
echo   查看资源使用: docker stats
echo.
echo [提示] 首次启动可能需要1-2分钟初始化数据库

pause
