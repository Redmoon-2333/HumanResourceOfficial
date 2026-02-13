# 人力资源中心官网 - 文档中心

## 文档索引

### 📦 部署相关

| 文档 | 说明 | 路径 |
|------|------|------|
| [部署指南](deploy/DEPLOYMENT_GUIDE.md) | 完整的Docker部署流程 | `deploy/docs/DEPLOYMENT_GUIDE.md` |
| [Dockerfile](../deploy/Dockerfile) | 后端服务镜像构建文件 | `deploy/Dockerfile` |
| [docker-compose.yml](../deploy/docker-compose.yml) | 容器编排配置 | `deploy/docker-compose.yml` |

### 🗄️ 数据库相关

| 文档 | 说明 | 路径 |
|------|------|------|
| [数据库初始化脚本](../deploy/init/init_database.sql) | 表结构+测试数据 | `deploy/init/init_database.sql` |
| [密码说明](../database/PASSWORD_README.md) | 测试账号密码对照 | `database/PASSWORD_README.md` |

### 🔄 迁移相关

| 文档 | 说明 | 路径 |
|------|------|------|
| [Qdrant到Redis迁移](../QDRANT_TO_REDIS_MIGRATION.md) | 向量数据库迁移文档 | 根目录 |
| [RAG部署可行性分析](../RAG_DEPLOYMENT_FEASIBILITY_ANALYSIS.md) | 2核2GB服务器分析 | 根目录 |

### ⚙️ 配置相关

| 文档 | 说明 | 路径 |
|------|------|------|
| [环境变量模板](../deploy/.env.example) | 环境变量配置示例 | `deploy/.env.example` |
| [application.yml](../src/main/resources/application.yml) | 主配置文件 | `src/main/resources/` |

---

## 快速开始

### 1. 本地开发

```bash
# 克隆项目
git clone <repository_url>
cd HumanResourceOfficial

# 配置环境变量
cp .env.example .env
# 编辑.env填写必要配置

# 启动开发环境
mvn spring-boot:run
```

### 2. 生产部署

```bash
# 1. 本地打包
mvn clean package -DskipTests

# 2. 上传deploy目录到服务器
scp -r deploy/ user@server:/opt/hrofficial/

# 3. 服务器上启动
cd /opt/hrofficial
./deploy.sh start
```

---

## 目录结构

```
HumanResourceOfficial/
├── deploy/                    # 部署相关文件
│   ├── Dockerfile            # Docker镜像构建
│   ├── docker-compose.yml    # 容器编排
│   ├── .env.example          # 环境变量模板
│   ├── deploy.sh             # Linux部署脚本
│   ├── deploy.bat            # Windows部署脚本
│   ├── init/                 # 初始化脚本
│   │   └── init_database.sql
│   └── docs/                 # 部署文档
│       └── DEPLOYMENT_GUIDE.md
├── database/                  # 数据库相关
│   ├── init_database.sql     # 数据库初始化
│   └── PASSWORD_README.md    # 密码说明
├── docs/                      # 文档中心（本目录）
│   └── README.md
├── src/                       # 源代码
│   └── main/
│       ├── java/
│       └── resources/
├── hrofficial-frontend/       # 前端项目
├── pom.xml                    # Maven配置
└── README.md                  # 项目说明
```

---

## 联系方式

如有问题，请联系项目负责人。

---

**最后更新:** 2025-02-12
