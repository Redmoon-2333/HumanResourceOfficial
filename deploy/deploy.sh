#!/bin/bash
# ============================================================
# 人力资源中心官网 - 部署脚本
# 用法: ./deploy.sh [命令]
# 命令: start | stop | restart | status | logs | rebuild | clean
# ============================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目配置
PROJECT_NAME="hrofficial"
DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
COMPOSE_FILE="$DEPLOY_DIR/docker-compose.yml"

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查环境
check_env() {
    if [ ! -f "$DEPLOY_DIR/.env" ]; then
        log_error "未找到 .env 文件！"
        log_info "请复制 .env.example 为 .env 并填写配置"
        exit 1
    fi
    
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装！"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        log_error "Docker Compose 未安装！"
        exit 1
    fi
}

# 启动服务
start() {
    log_info "正在启动服务..."
    check_env
    docker-compose -f "$COMPOSE_FILE" up -d
    log_success "服务启动完成！"
    status
}

# 停止服务
stop() {
    log_info "正在停止服务..."
    docker-compose -f "$COMPOSE_FILE" down
    log_success "服务已停止"
}

# 重启服务
restart() {
    log_info "正在重启服务..."
    stop
    start
}

# 查看状态
status() {
    log_info "服务状态："
    docker-compose -f "$COMPOSE_FILE" ps
}

# 查看日志
logs() {
    local service=$1
    if [ -z "$service" ]; then
        docker-compose -f "$COMPOSE_FILE" logs -f --tail=100
    else
        docker-compose -f "$COMPOSE_FILE" logs -f --tail=100 "$service"
    fi
}

# 重新构建
rebuild() {
    log_info "正在重新构建服务..."
    check_env
    docker-compose -f "$COMPOSE_FILE" build --no-cache
    docker-compose -f "$COMPOSE_FILE" up -d --force-recreate
    log_success "重建完成！"
}

# 清理
clean() {
    log_warn "这将删除所有容器、网络和数据卷！"
    read -p "确认继续？(y/N): " confirm
    if [ "$confirm" = "y" ] || [ "$confirm" = "Y" ]; then
        docker-compose -f "$COMPOSE_FILE" down -v --rmi local
        log_success "清理完成"
    else
        log_info "已取消"
    fi
}

# 初始化数据库
init_db() {
    log_info "正在初始化数据库..."
    if [ -f "$DEPLOY_DIR/init/init_database.sql" ]; then
        docker exec -i hrofficial-mysql mysql -uroot -p$(grep DB_PASSWORD "$DEPLOY_DIR/.env" | cut -d'=' -f2) < "$DEPLOY_DIR/init/init_database.sql"
        log_success "数据库初始化完成"
    else
        log_error "未找到数据库初始化脚本"
    fi
}

# 备份数据库
backup_db() {
    local backup_file="$DEPLOY_DIR/backup/db_backup_$(date +%Y%m%d_%H%M%S).sql"
    mkdir -p "$DEPLOY_DIR/backup"
    log_info "正在备份数据库到 $backup_file ..."
    docker exec hrofficial-mysql mysqldump -uroot -p$(grep DB_PASSWORD "$DEPLOY_DIR/.env" | cut -d'=' -f2) --all-databases > "$backup_file"
    log_success "数据库备份完成"
}

# 帮助信息
help() {
    echo "用法: $0 [命令]"
    echo ""
    echo "可用命令："
    echo "  start     启动所有服务"
    echo "  stop      停止所有服务"
    echo "  restart   重启所有服务"
    echo "  status    查看服务状态"
    echo "  logs      查看日志 (可选: logs backend/mysql/redis)"
    echo "  rebuild   重新构建并启动服务"
    echo "  clean     清理所有容器和数据"
    echo "  init-db   初始化数据库"
    echo "  backup-db 备份数据库"
    echo "  help      显示帮助信息"
}

# 主函数
main() {
    cd "$DEPLOY_DIR"
    
    case "$1" in
        start)
            start
            ;;
        stop)
            stop
            ;;
        restart)
            restart
            ;;
        status)
            status
            ;;
        logs)
            logs "$2"
            ;;
        rebuild)
            rebuild
            ;;
        clean)
            clean
            ;;
        init-db)
            init_db
            ;;
        backup-db)
            backup_db
            ;;
        help|--help|-h|"")
            help
            ;;
        *)
            log_error "未知命令: $1"
            help
            exit 1
            ;;
    esac
}

main "$@"
