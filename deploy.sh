#!/bin/bash

# ===========================================
# 人力资源管理系统 - 自动化部署脚本 v2.0
# 支持 MySQL + Redis + Spring Boot 应用
# ===========================================

set -e
trap 'echo "[错误] 脚本执行失败，行号: $LINENO"' ERR

# 配置
APP_NAME="人力资源管理系统"
LOG_FILE="./deploy.log"
BACKUP_DIR="./backups"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 日志函数
log() { echo -e "${GREEN}[$(date '+%H:%M:%S')]${NC} $1" | tee -a $LOG_FILE; }
warn() { echo -e "${YELLOW}[警告]${NC} $1" | tee -a $LOG_FILE; }
error() { echo -e "${RED}[错误]${NC} $1" | tee -a $LOG_FILE; }
info() { echo -e "${BLUE}[信息]${NC} $1" | tee -a $LOG_FILE; }

# 显示横幅
show_banner() {
    echo -e "${GREEN}"
    echo "=========================================="
    echo "      $APP_NAME"
    echo "      自动化部署脚本 v2.0"
    echo "   (JWT-Redis增强版 + 健康检查)"
    echo "=========================================="
    echo -e "${NC}"
}

# 检查系统环境
check_system() {
    log "[步骤1] 系统环境检查..."
    
    # 检查Docker
    if ! command -v docker &> /dev/null; then
        error "Docker未安装，请先安装Docker"
        exit 1
    fi
    info "Docker版本: $(docker --version | grep -oE '[0-9]+\.[0-9]+\.[0-9]+')"
    
    # 检查Docker Compose
    if command -v docker-compose &> /dev/null && docker-compose --version &> /dev/null; then
        DOCKER_COMPOSE_CMD="docker-compose"
        info "使用 docker-compose"
    elif docker compose version &> /dev/null; then
        DOCKER_COMPOSE_CMD="docker compose"
        info "使用 Docker Compose 插件"
    else
        error "Docker Compose 未安装或损坏"
        exit 1
    fi
    
    # 检查系统资源
    local total_mem=$(free -m 2>/dev/null | awk 'NR==2{printf "%.1f", $2/1024}' || echo "未知")
    info "系统内存: ${total_mem}GB"
}

# 检查必要文件
check_files() {
    log "[步骤2] 检查部署文件..."
    
    local required_files=("app.jar" "Dockerfile" "docker-compose.yml" ".env")
    local missing_files=()
    
    for file in "${required_files[@]}"; do
        if [ ! -f "$file" ]; then
            missing_files+=("$file")
        else
            local file_size=$(du -h "$file" | cut -f1)
            info "✓ $file ($file_size)"
        fi
    done
    
    if [ ${#missing_files[@]} -ne 0 ]; then
        error "缺少以下必要文件:"
        printf '   - %s\n' "${missing_files[@]}"
        exit 1
    fi
    
    # 验证JAR包
    local jar_size=$(du -h app.jar | cut -f1)
    if [[ "$jar_size" == "0"* ]]; then
        error "JAR包文件损坏或为空"
        exit 1
    fi
    info "JAR包验证通过: $jar_size"
    
    # 检查配置
    if grep -q "your_strong_password" .env 2>/dev/null; then
        warn "检测到默认密码，请修改.env文件中的密码配置"
    fi
}

# 环境初始化
init_environment() {
    log "[步骤3] 环境初始化..."
    
    # 创建必要目录
    local dirs=("data" "logs" "mysql-init" "docker/redis" "uploads" "$BACKUP_DIR")
    for dir in "${dirs[@]}"; do
        mkdir -p "$dir"
        info "创建目录: $dir"
    done
    
    # 设置目录权限 (Windows环境下可能失败，但不影响功能)
    chmod 755 data logs mysql-init "$BACKUP_DIR" 2>/dev/null || warn "权限设置跳过（Windows环境）"
    chmod 755 docker/redis 2>/dev/null || warn "Redis目录权限设置跳过"
    
    # 设置uploads目录权限，确保容器内用户可以写入
    if [ -d "uploads" ]; then
        chmod 777 uploads 2>/dev/null || warn "uploads目录权限设置跳过"
        info "uploads目录权限已设置为777以支持容器写入"
    fi
    
    # 处理数据库初始化脚本
    if [ -f "init.sql" ]; then
        cp init.sql mysql-init/
        info "数据库初始化脚本已准备"
    else
        echo "-- 数据库初始化脚本占位符" > mysql-init/init.sql
        warn "未找到init.sql，创建空的初始化脚本"
    fi
    
    # 确保Redis配置文件存在
    if [ ! -f "docker/redis/redis.conf" ]; then
        info "创建Redis默认配置文件"
        cat > docker/redis/redis.conf << 'EOF'
# Redis 生产环境配置
bind 0.0.0.0
port 6379
protected-mode no
maxmemory 512mb
maxmemory-policy allkeys-lru
save 900 1
save 300 10
save 60 10000
appendonly yes
appendfsync everysec
loglevel notice
timeout 300
tcp-keepalive 300
databases 16
dir /data
slowlog-log-slower-than 10000
slowlog-max-len 128
EOF
        chmod 644 docker/redis/redis.conf 2>/dev/null || true
        info "Redis配置文件创建完成"
    else
        info "Redis配置文件已存在"
    fi
}

# 备份现有数据
backup_data() {
    log "[步骤4] 数据备份..."
    
    local backup_timestamp=$(date +%Y%m%d_%H%M%S)
    local backup_path="$BACKUP_DIR/backup_$backup_timestamp"
    
    if docker ps | grep -q "hrofficial"; then
        mkdir -p "$backup_path"
        
        # 备份MySQL数据
        if docker ps | grep -q "hrofficial-mysql"; then
            docker exec hrofficial-mysql mysqldump -u root -p${MYSQL_ROOT_PASSWORD} --all-databases > "$backup_path/mysql_backup.sql" 2>/dev/null || warn "MySQL备份失败"
        fi
        
        # 备份Redis数据
        if docker ps | grep -q "hrofficial-redis"; then
            docker exec hrofficial-redis redis-cli BGSAVE 2>/dev/null || warn "Redis备份失败"
        fi
        
        info "备份完成: $backup_path"
    else
        info "无运行中的容器，跳过备份"
    fi
}

# 清理旧环境
cleanup_old() {
    log "[步骤5] 清理旧环境..."
    
    set +e
    $DOCKER_COMPOSE_CMD down -v --timeout 30 2>/dev/null
    
    # 清理悬空镜像
    local dangling_images=$(docker images -f "dangling=true" -q)
    if [ ! -z "$dangling_images" ]; then
        docker rmi $dangling_images 2>/dev/null || true
        info "清理悬空镜像"
    fi
    
    set -e
    info "环境清理完成"
}

# 构建和启动服务
deploy_services() {
    log "[步骤6] 构建和启动服务..."
    
    # 拉取基础镜像
    info "拉取基础镜像..."
    docker pull mysql:8.0 &
    docker pull redis:7.2-alpine &
    docker pull openjdk:17-jdk-slim &
    wait
    
    # 构建并启动服务
    info "启动服务容器..."
    $DOCKER_COMPOSE_CMD up -d --build --force-recreate
    
    if [ $? -ne 0 ]; then
        error "服务启动失败"
        echo "查看详细错误: $DOCKER_COMPOSE_CMD logs"
        exit 1
    fi
    
    info "服务启动命令执行完成"
}

# 等待服务启动
wait_for_services() {
    log "[步骤7] 等待服务启动..."
    
    # 等待MySQL
    info "等待MySQL启动..."
    for i in {1..30}; do
        if docker exec hrofficial-mysql mysqladmin ping -h localhost --silent 2>/dev/null; then
            info "✓ MySQL服务就绪 (${i}秒)"
            break
        fi
        [ $i -eq 30 ] && warn "MySQL启动超时"
        sleep 2
    done
    
    # 等待Redis
    info "等待Redis启动..."
    for i in {1..15}; do
        if docker exec hrofficial-redis redis-cli ping 2>/dev/null | grep -q "PONG"; then
            info "✓ Redis服务就绪 (${i}秒)"
            break
        fi
        [ $i -eq 15 ] && warn "Redis启动超时"
        sleep 2
    done
    
    # 等待应用
    info "等待应用启动..."
    for i in {1..60}; do
        if curl -f http://localhost:8080 >/dev/null 2>&1 || curl -f http://localhost:8080/actuator/health >/dev/null 2>&1; then
            info "✓ 应用服务就绪 (${i}秒)"
            break
        fi
        [ $i -eq 60 ] && warn "应用启动超时，请检查日志: $DOCKER_COMPOSE_CMD logs backend"
        sleep 3
    done
}

# 健康检查
health_check() {
    log "[步骤8] 系统健康检查..."
    
    local health_score=0
    local total_checks=5
    
    # MySQL连接检查
    if docker exec hrofficial-mysql mysqladmin ping -h localhost --silent 2>/dev/null; then
        info "✓ MySQL连接正常"
        ((health_score++))
    else
        warn "✗ MySQL连接失败"
    fi
    
    # Redis连接检查
    if docker exec hrofficial-redis redis-cli ping 2>/dev/null | grep -q "PONG"; then
        info "✓ Redis连接正常"
        ((health_score++))
    else
        warn "✗ Redis连接失败"
    fi
    
    # 应用基础检查
    if curl -f http://localhost:8080 >/dev/null 2>&1; then
        info "✓ 应用基础访问正常"
        ((health_score++))
    else
        warn "✗ 应用基础访问失败"
    fi
    
    # 应用健康检查
    if curl -f http://localhost:8080/actuator/health >/dev/null 2>&1; then
        info "✓ 应用健康检查通过"
        ((health_score++))
    else
        warn "✗ 应用健康检查失败"
    fi
    
    # 容器状态检查
    local running_containers=$(docker-compose ps --services --filter "status=running" | wc -l)
    local total_containers=$(docker-compose ps --services | wc -l)
    if [ "$running_containers" -eq "$total_containers" ]; then
        info "✓ 所有容器运行正常 ($running_containers/$total_containers)"
        ((health_score++))
    else
        warn "✗ 部分容器未运行 ($running_containers/$total_containers)"
    fi
    
    # 健康评分
    local health_percentage=$((health_score * 100 / total_checks))
    if [ $health_percentage -ge 80 ]; then
        info "系统健康状态: 良好 ($health_score/$total_checks - $health_percentage%)"
    elif [ $health_percentage -ge 60 ]; then
        warn "系统健康状态: 一般 ($health_score/$total_checks - $health_percentage%)"
    else
        error "系统健康状态: 异常 ($health_score/$total_checks - $health_percentage%)"
    fi
}

# 显示部署信息
show_deployment_info() {
    log "[步骤9] 部署信息汇总..."
    
    local server_ip=$(hostname -I | awk '{print $1}' 2>/dev/null || echo "localhost")
    
    echo -e "${GREEN}"
    echo "=========================================="
    echo "🎉 部署完成！"
    echo "=========================================="
    echo -e "${NC}"
    
    echo -e "${BLUE}📋 服务访问信息:${NC}"
    echo "   应用地址: http://$server_ip:8080"
    echo "   数据库地址: $server_ip:3306"
    echo "   Redis地址: $server_ip:6379"
    echo "   默认管理员: hucongyucpp2"
    echo
    
    echo -e "${BLUE}🔧 管理命令:${NC}"
    echo "   查看所有日志: $DOCKER_COMPOSE_CMD logs -f"
    echo "   查看应用日志: $DOCKER_COMPOSE_CMD logs -f backend"
    echo "   查看数据库日志: $DOCKER_COMPOSE_CMD logs -f mysql"
    echo "   查看Redis日志: $DOCKER_COMPOSE_CMD logs -f redis"
    echo "   停止服务: $DOCKER_COMPOSE_CMD down"
    echo "   重启服务: $DOCKER_COMPOSE_CMD restart"
    echo "   查看服务状态: $DOCKER_COMPOSE_CMD ps"
    echo
    
    echo -e "${BLUE}🔍 故障排查:${NC}"
    echo "   实时监控: watch '$DOCKER_COMPOSE_CMD ps'"
    echo "   资源监控: docker stats"
    echo "   端口检查: netstat -tlnp | grep ':8080\\|:3306\\|:6379'"
    echo
    
    echo -e "${YELLOW}⚠️  安全提示:${NC}"
    echo "   1. 请定期备份数据库和Redis数据"
    echo "   2. 监控系统资源使用情况"
    echo "   3. 定期查看应用日志"
    echo "   4. 建议配置防火墙和SSL证书"
    echo
    
    echo -e "${GREEN}==========================================${NC}"
}

# 主函数
main() {
    touch $LOG_FILE
    
    show_banner
    check_system
    check_files
    init_environment
    backup_data
    cleanup_old
    deploy_services
    wait_for_services
    health_check
    show_deployment_info
    
    log "部署脚本执行完成！"
    echo "详细日志请查看: $LOG_FILE"
}

# 脚本入口
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi