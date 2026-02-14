# 人力资源中心官网 - 从零开始部署指南

## 目录
1. [部署方案概述](#部署方案概述)
2. [服务器要求](#服务器要求)
3. [方案一：本地构建 + 服务器部署（推荐）](#方案一本地构建--服务器部署推荐)
4. [方案二：服务器直接构建部署](#方案二服务器直接构建部署)
5. [环境变量配置详解](#环境变量配置详解)
6. [常见问题排查](#常见问题排查)
7. [性能优化建议](#性能优化建议)
8. [维护与监控](#维护与监控)

---

## 部署方案概述

本项目提供两种部署方案：

| 方案 | 适用场景 | 优点 | 缺点 |
|------|----------|------|------|
| **方案一** | 服务器配置较低（2G内存） | 本地构建不占用服务器资源 | 需要本地有Docker环境 |
| **方案二** | 服务器配置充足（4G+内存） | 部署流程简单直接 | 构建时占用大量资源 |

**推荐方案一**，因为：
- 2G内存服务器在构建时容易OOM（内存溢出）
- 本地构建可以复用Docker缓存，加快构建速度
- 上传到服务器的文件更小（只需镜像tar包）

---

## 服务器要求

### 最低配置（2G内存）
```
CPU: 1核
内存: 2GB
磁盘: 20GB
系统: Ubuntu 20.04+ / CentOS 7+ / Debian 10+
网络: 公网IP或内网可访问
```

### 推荐配置
```
CPU: 2核
内存: 4GB
磁盘: 50GB SSD
带宽: 5Mbps+
```

### 软件依赖
```bash
# Docker版本要求
Docker: 20.10+
Docker Compose: 2.0+
```

---

## 方案一：本地构建 + 服务器部署（推荐）

### 架构流程

```mermaid
flowchart LR
    A[本地开发机] -->|1. 构建镜像| B[Docker镜像]
    B -->|2. 导出tar包| C[镜像文件]
    C -->|3. 上传到服务器| D[服务器]
    D -->|4. 加载镜像| E[部署完成]
```

### 步骤详解

#### 第1步：本地环境准备

确保本地已安装：
- Docker Desktop 或 Docker Engine
- Git（用于克隆代码）

```bash
# 检查Docker版本
docker --version
docker-compose --version
```

#### 第2步：克隆项目代码

```bash
git clone <你的代码仓库地址>
cd HumanResourceOfficial
```

#### 第3步：执行构建导出脚本

```bash
# Linux/macOS
chmod +x deploy/build-and-export.sh
./deploy/build-and-export.sh

# Windows (使用Git Bash)
bash deploy/build-and-export.sh
```

脚本会自动：
1. 构建后端Docker镜像
2. 构建前端Docker镜像
3. 导出镜像为tar.gz压缩包
4. 复制所有部署配置文件

导出完成后，会在 `deploy/export/` 目录生成：
```
deploy/export/
├── hrofficial-backend.tar.gz    # 后端镜像
├── hrofficial-frontend.tar.gz   # 前端镜像
├── docker-compose.yml           # Docker编排配置
├── .env.example                 # 环境变量模板
├── deploy-on-server.sh          # 服务器部署脚本
├── nginx.conf                   # Nginx配置
└── init/                        # 数据库初始化脚本
```

#### 第4步：上传到服务器

**方式A：使用scp命令**
```bash
# 打包导出目录
cd deploy
tar -czf export.tar.gz export/

# 上传到服务器（替换your-server-ip为你的服务器IP）
scp export.tar.gz root@your-server-ip:/opt/

# 登录服务器解压
ssh root@your-server-ip
cd /opt
tar -xzf export.tar.gz
cd export
```

**方式B：使用FTP工具**
- 使用FileZilla、WinSCP等工具
- 将 `export` 目录上传到服务器的 `/opt/` 目录

#### 第5步：服务器端配置

```bash
# 进入部署目录
cd /opt/export

# 复制环境变量模板
cp .env.example .env

# 编辑环境变量（使用vi或nano）
vi .env
```

**必须修改的配置项：**
```bash
# 数据库密码（必须修改）
DB_PASSWORD=YourStrongPassword123!

# JWT密钥（必须修改，至少32位）
JWT_SECRET=YourJWTSecretKeyMustBeAtLeast32CharactersLong

# 阿里云DashScope API密钥（AI功能必需）
ALIYUN_DASHSCOPE_API_KEY=sk-your-api-key-here
```

#### 第6步：执行部署

```bash
# 给脚本执行权限
chmod +x deploy-on-server.sh

# 执行部署
./deploy-on-server.sh
```

部署脚本会自动：
1. 加载Docker镜像
2. 启动MySQL、Redis、后端、前端服务
3. 等待服务就绪
4. 显示访问地址

#### 第7步：验证部署

```bash
# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f backend

# 测试API接口
curl http://localhost:8080/actuator/health
```

访问地址：
- 前端：`http://your-server-ip`
- 后端API：`http://your-server-ip:8080`

---

## 方案二：服务器直接构建部署

### 适用条件
- 服务器内存 ≥ 4GB
- 服务器有公网访问（拉取镜像）

### 部署步骤

#### 第1步：服务器安装Docker

```bash
# Ubuntu/Debian
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
newgrp docker

# 验证安装
docker --version
docker-compose --version
```

#### 第2步：上传项目代码

```bash
# 本地执行：打包项目（排除node_modules和target）
cd HumanResourceOfficial
tar -czf deploy.tar.gz \
  --exclude='hrofficial-frontend/node_modules' \
  --exclude='target' \
  --exclude='.git' \
  .

# 上传到服务器
scp deploy.tar.gz root@your-server-ip:/opt/

# 服务器解压
ssh root@your-server-ip
cd /opt
tar -xzf deploy.tar.gz
```

#### 第3步：配置环境变量

```bash
cd /opt/deploy
cp deploy/.env.example deploy/.env
vi deploy/.env  # 编辑配置
```

#### 第4步：执行部署

```bash
cd /opt/deploy/deploy
chmod +x deploy.sh
./deploy.sh
```

---

## 环境变量配置详解

### 核心配置项

| 变量名 | 必填 | 说明 | 示例 |
|--------|------|------|------|
| `DB_PASSWORD` | ✅ | MySQL root密码 | `MySecureP@ss123` |
| `JWT_SECRET` | ✅ | JWT签名密钥（≥32位） | `MySecretKeyForJWTSigning2024` |
| `ALIYUN_DASHSCOPE_API_KEY` | ⚠️ | 阿里云AI密钥 | `sk-abc123...` |
| `REDIS_PASSWORD` | ❌ | Redis密码（可选） | `redisPass123` |

### 完整配置示例

```bash
# ===================
# 数据库配置
# ===================
DB_NAME=hrofficial
DB_USERNAME=root
DB_PASSWORD=YourStrongPassword123!
MYSQL_PORT=3306

# ===================
# Redis配置
# ===================
REDIS_PASSWORD=
REDIS_PORT=6379

# ===================
# JWT配置
# ===================
JWT_SECRET=YourJWTSecretKeyMustBeAtLeast32CharactersLong
JWT_EXPIRATION=604800000

# ===================
# 阿里云DashScope配置（AI对话）
# ===================
ALIYUN_DASHSCOPE_API_KEY=sk-your-dashscope-api-key

# ===================
# 阿里云OSS配置（可选，用于文件存储）
# ===================
ALIYUN_OSS_ACCESS_KEY_ID=
ALIYUN_OSS_ACCESS_KEY_SECRET=
ALIYUN_OSS_BUCKET_NAME=

# ===================
# 端口配置
# ===================
BACKEND_PORT=8080
FRONTEND_PORT=80
```

---

## 常见问题排查

### 问题1：容器启动后立即退出

**现象：**
```bash
docker-compose ps
# 显示状态为 Exited (1)
```

**排查：**
```bash
# 查看具体错误日志
docker-compose logs backend

# 常见原因：
# 1. 环境变量未配置
# 2. 端口被占用
# 3. 内存不足
```

**解决：**
```bash
# 检查端口占用
netstat -tlnp | grep 8080

# 修改端口（.env文件）
BACKEND_PORT=8081
```

### 问题2：数据库连接失败

**现象：**
```
Caused by: java.net.ConnectException: Connection refused
```

**排查：**
```bash
# 检查MySQL容器状态
docker-compose ps mysql

# 查看MySQL日志
docker-compose logs mysql

# 测试连接
docker-compose exec mysql mysql -uroot -p -e "SELECT 1"
```

**解决：**
```bash
# 等待MySQL完全启动（首次启动需要30-60秒）
sleep 60

# 重启后端服务
docker-compose restart backend
```

### 问题3：内存不足导致OOM

**现象：**
```
OutOfMemoryError: Java heap space
```

**排查：**
```bash
# 查看内存使用
docker stats

# 查看系统内存
free -h
```

**解决：**
已在Dockerfile中配置JVM参数优化：
```bash
-Xms128m -Xmx512m  # 限制堆内存最大512MB
```

如果仍然OOM，可以：
1. 升级服务器内存到4GB
2. 关闭非必要服务
3. 减少MySQL和Redis内存配置

### 问题4：前端无法访问后端API

**现象：**
- 前端页面能打开
- 登录或获取数据失败

**排查：**
```bash
# 检查后端健康状态
curl http://localhost:8080/actuator/health

# 检查前端配置
cat deploy/nginx.conf | grep proxy_pass
```

**解决：**
```bash
# 确保nginx配置正确
# 如果后端端口不是8080，修改nginx.conf
proxy_pass http://backend:8080/api/;
```

### 问题5：RAG知识库初始化失败

**现象：**
- 上传文档后无法检索
- 向量数据库报错

**排查：**
```bash
# 检查Redis向量支持
docker-compose exec redis redis-cli FT._LIST

# 检查向量维度配置
docker-compose logs backend | grep "Embedding"
```

**解决：**
```bash
# 清空Redis数据重新初始化
docker-compose exec redis redis-cli FLUSHALL

# 重启后端服务
docker-compose restart backend
```

---

## 性能优化建议

### 2G内存服务器优化

#### 1. 内存分配规划
```
总内存: 2048 MB
├── MySQL: 256 MB
├── Redis: 256 MB
├── 后端Java: 512 MB (堆) + 256 MB (非堆) ≈ 768 MB
├── Nginx: 64 MB
└── 系统预留: 约 700 MB
```

#### 2. JVM参数优化（已配置）
```bash
-Xms128m -Xmx512m          # 堆内存限制
-XX:MetaspaceSize=64m      # 元空间初始
-XX:MaxMetaspaceSize=128m  # 元空间最大
-XX:+UseG1GC               # G1垃圾收集器
```

#### 3. MySQL内存优化（已配置）
```ini
innodb_buffer_pool_size=128M
max_connections=50
query_cache_size=16M
```

#### 4. Redis内存优化（已配置）
```bash
--maxmemory 192mb
--maxmemory-policy allkeys-lru
```

### 高并发优化

如果访问量较大，建议：
1. 升级服务器到4G内存
2. 使用Nginx反向代理做负载均衡
3. 启用Redis缓存
4. 配置CDN加速静态资源

---

## 维护与监控

### 日常维护命令

```bash
# 查看所有服务状态
docker-compose ps

# 查看资源使用
docker stats

# 查看日志
docker-compose logs -f --tail=100

# 查看后端日志
docker-compose logs -f backend

# 重启服务
docker-compose restart backend

# 停止所有服务
docker-compose down

# 完全清理（包括数据卷）
docker-compose down -v
```

### 数据备份

```bash
# 备份MySQL数据
docker-compose exec mysql mysqldump -uroot -p hrofficial > backup_$(date +%Y%m%d).sql

# 备份上传文件
tar -czf uploads_backup_$(date +%Y%m%d).tar.gz /var/lib/docker/volumes/hrofficial-backend-uploads/

# 备份Redis数据
docker-compose exec redis redis-cli BGSAVE
```

### 监控脚本

创建监控脚本 `monitor.sh`：
```bash
#!/bin/bash
# 系统监控脚本

echo "=== 系统资源 ==="
free -h

echo ""
echo "=== Docker容器状态 ==="
docker-compose ps

echo ""
echo "=== 容器资源使用 ==="
docker stats --no-stream

echo ""
echo "=== 后端健康检查 ==="
curl -s http://localhost:8080/actuator/health | jq .
```

---

## 安全建议

### 1. 修改默认端口
```bash
# .env文件
BACKEND_PORT=8080  # 改为非标准端口
FRONTEND_PORT=80   # 如有需要可改为其他端口
```

### 2. 配置防火墙
```bash
# 只开放必要端口
ufw allow 80/tcp
ufw allow 443/tcp  # 如果使用HTTPS
ufw allow 8080/tcp  # 后端端口
ufw enable
```

### 3. 定期更新镜像
```bash
# 拉取最新基础镜像
docker pull mysql:8.0.40-debian
docker pull redis/redis-stack-server:7.4.0-v0
docker pull eclipse-temurin:21-jre-alpine

# 重新构建并部署
./deploy.sh
```

---

## 获取帮助

如遇到部署问题：

1. 查看详细日志：`docker-compose logs -f`
2. 检查环境变量：`cat deploy/.env`
3. 验证端口占用：`netstat -tlnp`
4. 检查系统资源：`free -h && df -h`

---

**文档版本：** v1.0  
**适用项目：** 人力资源中心官网  
**最后更新：** 2026-02-14
