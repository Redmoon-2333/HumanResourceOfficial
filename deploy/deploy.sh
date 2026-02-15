#!/bin/bash
# ============================================================
# 人力资源中心官网 - 自动化部署脚本 (Linux/macOS)
# 适用于2G内存服务器的一键部署
# ============================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo -e "${BLUE}============================================================${NC}"
echo -e "${BLUE}  人力资源中心官网 - 自动化部署脚本${NC}"
echo -e "${BLUE}============================================================${NC}"
echo ""

# 检查Docker是否安装
check_docker() {
    echo -e "${YELLOW}[1/6] 检查Docker环境...${NC}"
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}错误: Docker未安装${NC}"
        echo "请先安装Docker: https://docs.docker.com/get-docker/"
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null; then
        echo -e "${RED}错误: Docker Compose未安装${NC}"
        echo "请先安装Docker Compose: https://docs.docker.com/compose/install/"
        exit 1
    fi

    echo -e "${GREEN}✓ Docker环境检查通过${NC}"
    echo ""
}

# 检查环境变量文件
check_env() {
    echo -e "${YELLOW}[2/6] 检查环境变量配置...${NC}"

    if [ ! -f "$SCRIPT_DIR/.env" ]; then
        echo -e "${RED}错误: 未找到.env文件${NC}"
        echo "请执行以下操作:"
        echo "  1. cp $SCRIPT_DIR/.env.example $SCRIPT_DIR/.env"
        echo "  2. 编辑.env文件，填写实际配置"
        exit 1
    fi

    # 检查关键环境变量
    source "$SCRIPT_DIR/.env"

    if [ -z "$DB_PASSWORD" ] || [ "$DB_PASSWORD" = "your_secure_password_here" ]; then
        echo -e "${RED}错误: 请设置数据库密码 (DB_PASSWORD)${NC}"
        exit 1
    fi

    if [ -z "$JWT_SECRET" ] || [ "$JWT_SECRET" = "your_jwt_secret_key_must_be_at_least_32_characters_long" ]; then
        echo -e "${RED}错误: 请设置JWT密钥 (JWT_SECRET)${NC}"
        exit 1
    fi

    if [ -z "$ALIYUN_DASHSCOPE_API_KEY" ] || [ "$ALIYUN_DASHSCOPE_API_KEY" = "your_dashscope_api_key_here" ]; then
        echo -e "${YELLOW}警告: 未设置阿里云DashScope API密钥，AI功能将不可用${NC}"
    fi

    echo -e "${GREEN}✓ 环境变量检查通过${NC}"
    echo ""
}

# 检查系统资源
check_resources() {
    echo -e "${YELLOW}[3/6] 检查系统资源...${NC}"

    # 检查内存
    TOTAL_MEM=$(free -m | awk '/^Mem:/{print $2}')
    echo "总内存: ${TOTAL_MEM}MB"

    if [ "$TOTAL_MEM" -lt 1800 ]; then
        echo -e "${YELLOW}警告: 内存小于2GB (${TOTAL_MEM}MB)，可能影响性能${NC}"
    else
        echo -e "${GREEN}✓ 内存充足${NC}"
    fi

    # 检查磁盘空间
    DISK_USAGE=$(df -h "$SCRIPT_DIR" | awk 'NR==2 {print $5}' | sed 's/%//')
    echo "磁盘使用率: ${DISK_USAGE}%"

    if [ "$DISK_USAGE" -gt 80 ]; then
        echo -e "${YELLOW}警告: 磁盘空间不足，请清理后再部署${NC}"
    else
        echo -e "${GREEN}✓ 磁盘空间充足${NC}"
    fi

    echo ""
}

# 清理旧容器和数据（可选）
cleanup() {
    echo -e "${YELLOW}[4/6] 清理旧容器...${NC}"

    read -p "是否清理旧容器和数据? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "停止并删除旧容器..."
        cd "$SCRIPT_DIR"
        docker-compose down --remove-orphans

        read -p "是否清理Docker缓存? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            echo "清理Docker缓存..."
            docker system prune -f
        fi
    fi

    echo -e "${GREEN}✓ 清理完成${NC}"
    echo ""
}

# 构建和启动服务
deploy() {
    echo -e "${YELLOW}[5/6] 构建和启动服务...${NC}"

    cd "$SCRIPT_DIR"

    # 拉取最新镜像
    echo "拉取基础镜像..."
    docker-compose pull

    # 构建服务
    echo "构建服务..."
    docker-compose build --no-cache

    # 启动服务
    echo "启动服务..."
    docker-compose up -d

    echo -e "${GREEN}✓ 服务启动完成${NC}"
    echo ""
}

# 等待服务就绪并显示状态
wait_for_ready() {
    echo -e "${YELLOW}[6/6] 等待服务就绪...${NC}"

    cd "$SCRIPT_DIR"

    # 等待MySQL
    echo "等待MySQL就绪..."
    for i in {1..30}; do
        if docker-compose exec -T mysql mysqladmin ping -h localhost -u root -p"$DB_PASSWORD" &> /dev/null; then
            echo -e "${GREEN}✓ MySQL已就绪${NC}"
            break
        fi
        sleep 2
        echo -n "."
    done

    # 等待Redis
    echo "等待Redis就绪..."
    for i in {1..30}; do
        if docker-compose exec -T redis redis-cli ping &> /dev/null; then
            echo -e "${GREEN}✓ Redis已就绪${NC}"
            break
        fi
        sleep 2
        echo -n "."
    done

    # 等待后端服务
    echo "等待后端服务就绪..."
    for i in {1..60}; do
        if curl -sf http://localhost:${BACKEND_PORT:-8080}/actuator/health &> /dev/null; then
            echo -e "${GREEN}✓ 后端服务已就绪${NC}"
            break
        fi
        sleep 2
        echo -n "."
    done

    echo ""
}

# 显示部署结果
show_status() {
    echo -e "${GREEN}============================================================${NC}"
    echo -e "${GREEN}  部署完成！${NC}"
    echo -e "${GREEN}============================================================${NC}"
    echo ""
    echo "服务状态:"
    cd "$SCRIPT_DIR"
    docker-compose ps
    echo ""
    echo "访问地址:"
    echo "  前端: http://localhost:${FRONTEND_PORT:-80}"
    echo "  后端API: http://localhost:${BACKEND_PORT:-8080}"
    echo "  后端健康检查: http://localhost:${BACKEND_PORT:-8080}/actuator/health"
    echo ""
    echo "常用命令:"
    echo "  查看日志: docker-compose logs -f"
    echo "  停止服务: docker-compose down"
    echo "  重启服务: docker-compose restart"
    echo "  查看资源使用: docker stats"
    echo ""
    echo -e "${YELLOW}提示: 首次启动可能需要1-2分钟初始化数据库${NC}"
}

# 主函数
main() {
    check_docker
    check_env
    check_resources
    cleanup
    deploy
    wait_for_ready
    show_status
}

# 执行主函数
main
