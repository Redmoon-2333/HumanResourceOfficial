# HumanResourceOfficial - 学生会人力资源管理系统

<div align="center">

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)
![Vue3](https://img.shields.io/badge/Vue3-3.3+-42b883.svg)
![Java](https://img.shields.io/badge/Java-21-orange.svg)
![MySQL](https://img.shields.io/badge/MySQL-8.0+-blue.svg)
![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0-purple.svg)
![AI](https://img.shields.io/badge/AI-Qwen-purple.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)

一个基于Spring Boot + Vue3 + Spring AI的现代化学生会人力资源管理系统，集成RAG知识库问答功能

</div>

## 📖 项目简介

HumanResourceOfficial是一个功能完整的学生会人力资源管理系统，采用前后端分离架构，集成Spring AI智能助手和RAG(检索增强生成)知识库问答，旨在为学生会提供数字化、智能化的管理平台。系统支持用户认证、权限控制、活动管理、内部资料管理、往届成员查询、AI智能对话、RAG知识库问答和策划案生成等核心功能。

### ✨ 核心特性

#### 后端服务
- 🔐 **JWT无状态认证** - 基于JSON Web Token的安全认证机制
- 👥 **基于角色的权限控制** - 细粒度的权限管理体系（游客/部员/部长）
- 📋 **活动全生命周期管理** - 活动介绍、往届活动展示
- 📁 **内部资料管理** - 三级分类管理、文件上传下载、预签名URL安全下载
- 🔍 **往届成员查询** - 按年份分组的历史成员数据查询
- ☁️ **阿里云OSS集成** - 文件安全存储与预签名URL下载
- 🤖 **AI智能助手** - 集成通义千问大模型，流式对话输出
- 📚 **RAG知识库问答** - 基于Spring AI的检索增强生成，支持向量检索
- 🔍 **语义检索** - Qdrant向量数据库，智能文档检索
- 📝 **AI策划案生成** - 智能生成活动策划方案
- 🧠 **Tool Calling** - AI工具调用，数据库查询集成
- 📱 **RESTful API设计** - 标准化的API接口
- 🔄 **分页查询支持** - 高效的数据分页处理

#### 前端应用
- 🎨 **Vue3 + TypeScript** - 现代化前端框架
- 🧩 **Element Plus UI** - 美观的组件库
- 🎯 **Pinia状态管理** - 轻量级状态管理方案
- 🛣️ **Vue Router** - 单页面路由管理
- 💬 **流式AI对话** - Server-Sent Events实时流式输出
- 📊 **响应式设计** - 适配多种屏幕尺寸
- 🔒 **路由权限守卫** - 基于角色的前端权限控制

## 🏗️ 技术架构

### 技术栈

#### 后端技术
| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.3.5 | 核心框架 |
| Spring AI | 1.0.0 | AI集成框架 |
| Spring AI Alibaba | 1.0.0.2 | 通义千问集成 |
| Spring Security | 3.1.4 | 安全框架 |
| Spring Data JPA | 3.1.4 | 数据访问层 |
| MyBatis | 3.0.2 | 持久层框架 |
| MySQL | 8.0+ | 数据库 |
| JWT | 0.11.5 | 认证令牌 |
| PageHelper | 1.4.6 | 分页插件 |
| 阿里云OSS | 3.17.2 | 文件存储 |
| 通义千问API | - | AI大模型 |
| Qdrant | 1.9.1 | 向量数据库 |
| Spring AI Qdrant | 1.0.0 | 向量存储 |
| OkHttp | 4.12.0 | HTTP客户端 |

#### 前端技术
| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.3+ | 渐进式框架 |
| TypeScript | 5.0+ | 类型系统 |
| Vite | 4.4+ | 构建工具 |
| Element Plus | 2.4+ | UI组件库 |
| Pinia | 2.1+ | 状态管理 |
| Vue Router | 4.2+ | 路由管理 |
| Axios | 1.5+ | HTTP客户端 |
| EventSource | - | SSE流式通信 |

### 系统架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   前端应用      │    │   后端服务      │    │   AI服务        │
│  Vue3 + TS      │───▶│  Spring Boot    │───▶│  通义千问API    │
│  Element Plus   │    │  + Spring AI    │    │  (流式输出)     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
        │                       │                       │
        │              ┌─────────────────┐    ┌─────────────────┐
        │              │   文件存储      │    │  向量数据库     │
        └─────────────▶│  阿里云OSS      │    │   Qdrant        │
                       │  (预签名URL)    │    │ (RAG知识库)     │
                       └─────────────────┘    └─────────────────┘
                                │                       │
                       ┌─────────────────────────────────┘
                       │   数据库        │              
                       │    MySQL        │              
                       └─────────────────┘              
```

## 📁 项目结构

### 后端结构
```
src/main/java/com/redmoon2333/
├── annotation/          # 自定义注解
│   ├── RequireMemberRole.java      # 部员权限注解
│   └── RequireMinisterRole.java    # 部长权限注解
├── aspect/              # AOP切面
│   └── PermissionAspect.java       # 权限验证切面
├── config/              # 配置类
│   ├── JwtAuthenticationFilter.java # JWT认证过滤器
│   ├── OssConfig.java              # OSS配置
│   ├── QdrantConfig.java           # Qdrant向量数据库配置
│   ├── RagConfig.java              # RAG功能配置
│   ├── VectorStoreConfig.java      # Spring AI VectorStore配置
│   ├── SecurityConfig.java         # 安全配置
│   └── WebConfig.java              # Web配置（CORS）
├── controller/          # 控制器层
│   ├── ActivityController.java     # 活动介绍管理
│   ├── AuthController.java         # 认证控制器
│   ├── MaterialController.java     # 资料管理（三级分类）
│   ├── PastActivityController.java # 往届活动
│   ├── AlumniController.java       # 往届成员
│   ├── AiChatController.java       # AI对话（流式输出）
│   ├── RagController.java          # RAG知识库管理
│   └── UserController.java         # 用户管理
├── dto/                 # 数据传输对象
├── entity/              # 实体类
├── enums/               # 枚举类
├── exception/           # 异常处理
├── mapper/              # MyBatis映射器
├── service/             # 业务服务层
│   ├── AiChatService.java          # AI对话服务
│   ├── MaterialService.java        # 资料管理服务
│   ├── RagManagementService.java   # RAG知识库管理
│   ├── RagRetrievalService.java    # RAG检索服务
│   ├── EmbeddingService.java       # 文本向量化服务
│   └── OssUtil.java                # OSS工具类
├── util/                # 工具类
├── validation/          # 数据验证
└── Main.java            # 应用入口
```

### 前端结构
```
hrofficial-frontend/
├── src/
│   ├── api/             # API接口定义
│   │   ├── auth.ts              # 认证接口
│   │   ├── activity.ts          # 活动接口
│   │   ├── material.ts          # 资料接口
│   │   ├── pastActivity.ts      # 往届活动接口
│   │   ├── alumni.ts            # 往届成员接口
│   │   └── ai.ts                # AI对话接口
│   ├── components/      # 公共组件
│   │   ├── Layout.vue           # 布局组件
│   │   └── ChatMessage.vue      # 聊天消息组件
│   ├── router/          # 路由配置
│   │   └── index.ts             # 路由定义（权限守卫）
│   ├── stores/          # 状态管理
│   │   └── user.ts              # 用户状态
│   ├── types/           # TypeScript类型定义
│   │   └── index.ts
│   ├── utils/           # 工具函数
│   │   └── http.ts              # HTTP请求封装
│   ├── views/           # 页面组件
│   │   ├── Login.vue            # 登录页
│   │   ├── Register.vue         # 注册页
│   │   ├── Home.vue             # 首页
│   │   ├── Activities.vue       # 活动介绍
│   │   ├── PastActivities.vue   # 往届活动
│   │   ├── Materials.vue        # 资料管理（三级分类）
│   │   ├── Alumni.vue           # 往届成员
│   │   ├── AiChat.vue           # AI对话（流式）
│   │   └── PlanGenerator.vue    # 策划案生成
│   ├── App.vue          # 根组件
│   └── main.ts          # 入口文件
├── public/              # 静态资源
├── index.html
├── package.json
├── tsconfig.json
└── vite.config.ts       # Vite配置
```

## 🚀 快速开始

### 环境要求

- Java 21+
- Maven 3.6+
- MySQL 8.0+
- Node.js 16+
- npm/yarn
- Qdrant 1.7+ (向量数据库)
- 阿里云OSS账号
- 通义千问API密钥

### 后端启动

#### 1. 克隆项目

```bash
git clone https://github.com/yourusername/HumanResourceOfficial.git
cd HumanResourceOfficial
```

#### 2. 数据库配置

创建MySQL数据库并导入数据表结构：

```sql
CREATE DATABASE hrofficial CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### 3. 环境变量配置

创建环境变量或修改 `application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/hrofficial?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=GMT%2B8
    username: your_username
    password: your_password

# Qdrant向量数据库配置
qdrant:
  host: localhost
  port: 6334
  collection-name: campus_knowledge
  use-tls: false

# RAG配置
rag:
  knowledge-base-path: src/main/resources/rag-knowledge-base
  chunk-size: 800
  chunk-overlap: 100
  retrieval-top-k: 5
  score-threshold: 0.0  # 相似度阈值，0表示不过滤
  vector-dimension: 1536
  batch-size: 25
    
# 阿里云OSS配置
aliyun:
  oss:
    endpoint: ${ALIYUN_OSS_ENDPOINT:oss-cn-hangzhou.aliyuncs.com}
    accessKeyId: ${ALIYUN_OSS_ACCESS_KEY_ID}
    accessKeySecret: ${ALIYUN_OSS_ACCESS_KEY_SECRET}
    bucketName: ${ALIYUN_OSS_BUCKET_NAME}
    domain: ${ALIYUN_OSS_DOMAIN:}

# 通义千问AI配置
qwen:
  api:
    key: ${QWEN_API_KEY}
    url: https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation
    model: qwen-plus
```

Windows环境变量设置：
```cmd
set ALIYUN_OSS_ACCESS_KEY_ID=your_access_key_id
set ALIYUN_OSS_ACCESS_KEY_SECRET=your_access_key_secret
set ALIYUN_OSS_BUCKET_NAME=your_bucket_name
set QWEN_API_KEY=your_qwen_api_key
set aliQwen_api=your_qwen_api_key
```

Linux/Mac环境变量设置：
```bash
export ALIYUN_OSS_ACCESS_KEY_ID=your_access_key_id
export ALIYUN_OSS_ACCESS_KEY_SECRET=your_access_key_secret
export ALIYUN_OSS_BUCKET_NAME=your_bucket_name
export QWEN_API_KEY=your_qwen_api_key
export aliQwen_api=your_qwen_api_key
```

#### 4. 启动Qdrant向量数据库

使用Docker快速启动Qdrant:

```bash
docker run -p 6333:6333 -p 6334:6334 \
  -v $(pwd)/qdrant_storage:/qdrant/storage \
  qdrant/qdrant
```

或使用Docker Compose:

```yaml
version: '3.8'
services:
  qdrant:
    image: qdrant/qdrant:latest
    ports:
      - "6333:6333"  # HTTP API
      - "6334:6334"  # gRPC API
    volumes:
      - ./qdrant_storage:/qdrant/storage
```

启动后访问 `http://localhost:6333/dashboard` 查看Qdrant管理界面。

#### 5. 编译运行后端

```bash
# 编译项目
mvn clean compile

# 运行项目
mvn spring-boot:run
```

或直接运行主类：
```bash
java -jar target/HumanResourceOfficial-1.0-SNAPSHOT.jar
```

#### 6. 验证后端启动

访问 `http://localhost:8080` 确认服务启动成功。

### 前端启动

#### 1. 进入前端目录

```bash
cd hrofficial-frontend
```

#### 2. 安装依赖

```bash
npm install
# 或
yarn install
```

#### 3. 配置环境变量

创建 `.env.development` 文件：

```env
VITE_API_BASE_URL=http://localhost:8080
```

#### 4. 启动开发服务器

```bash
npm run dev
# 或
yarn dev
```

#### 5. 访问前端应用

浏览器访问 `http://localhost:5173`

### 生产环境部署

#### 前端构建

```bash
cd hrofficial-frontend
npm run build
# 构建产物在 dist/ 目录
```

#### 后端打包

```bash
mvn clean package -DskipTests
```

## 📚 API文档

### 认证相关

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/auth/login` | 用户登录 | 公开 |
| POST | `/api/auth/register` | 用户注册（需激活码） | 公开 |
| POST | `/api/auth/logout` | 用户登出 | 需认证 |

### 活动介绍管理

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/activities` | 获取活动介绍列表 | 公开 |
| POST | `/api/activities` | 创建活动介绍 | 部长 |
| GET | `/api/activities/{id}` | 获取活动详情 | 公开 |
| PUT | `/api/activities/{id}` | 更新活动介绍 | 部长 |
| DELETE | `/api/activities/{id}` | 删除活动介绍 | 部长 |

### 内部资料管理（三级分类）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/materials/upload` | 上传资料 | 部员+ |
| GET | `/api/materials/download-url/{id}` | 获取预签名下载URL | 部员+ |
| GET | `/api/materials/categories` | 获取所有分类 | 部员+ |
| POST | `/api/materials/category` | 创建分类 | 部长 |
| PUT | `/api/materials/category/{id}` | 更新分类 | 部长 |
| GET | `/api/materials/category/{id}/subcategories` | 获取子分类列表 | 部员+ |
| POST | `/api/materials/subcategory` | 创建子分类 | 部长 |
| PUT | `/api/materials/subcategory/{id}` | 更新子分类 | 部长 |
| GET | `/api/materials/subcategory/{id}` | 获取子分类下的资料 | 部员+ |
| PUT | `/api/materials/{id}` | 更新资料信息 | 部长 |
| DELETE | `/api/materials/{id}` | 删除资料 | 部长 |

### 往届活动管理

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/past-activities` | 分页查询往届活动 | 公开 |
| POST | `/api/past-activities` | 创建往届活动 | 部长 |
| PUT | `/api/past-activities/{id}` | 更新往届活动 | 部长 |
| DELETE | `/api/past-activities/{id}` | 删除往届活动 | 部长 |

### 往届成员管理

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/alumni` | 获取往届成员列表（按年份） | 公开 |
| POST | `/api/alumni` | 批量添加往届成员 | 部长 |
| DELETE | `/api/alumni/{year}` | 删除指定年份成员 | 部长 |

### AI智能助手

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/ai/chat/stream` | AI流式对话（SSE） | 部员+ |
| POST | `/api/ai/chat-with-rag` | RAG增强对话（SSE） | 部员+ |
| POST | `/api/ai/plan/generate` | 生成活动策划案 | 部员+ |

### RAG知识库管理

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/rag/initialize` | 初始化知识库 | 部长 |
| GET | `/api/rag/stats` | 获取知识库统计 | 部员+ |
| GET | `/api/rag/test-retrieve` | 测试检索（无阈值） | 部员+ |

详细的API文档请参考相关文档说明。

### RAG知识库初始化

```bash
# 初始化知识库（扫描并向量化知识库文件）
curl -X POST http://localhost:8080/api/rag/initialize \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "sourcePath": "src/main/resources/rag-knowledge-base",
    "forceReindex": false
  }'

# 响应示例
{
  "code": 200,
  "message": "成功",
  "data": {
    "totalFiles": 5,
    "processedFiles": 5,
    "failedFiles": 0,
    "totalChunks": 25,
    "newChunks": 25,
    "duplicateChunks": 0,
    "errors": []
  }
}
```

### RAG增强对话示例

```javascript
// 前端使用EventSource接收RAG增强的流式对话
const eventSource = new EventSource(
  '/api/ai/chat-with-rag?message=办公地点&useRAG=true&enableTools=false'
);

eventSource.onmessage = (event) => {
  if (event.data === '[DONE]') {
    eventSource.close();
    return;
  }
  const chunk = event.data;
  // 逐字显示AI回复（基于知识库检索增强）
  displayMessage(chunk);
};

eventSource.onerror = () => {
  eventSource.close();
};
```

### 知识库检索示例

```bash
# 测试检索（不设置相似度阈值）
curl -X GET "http://localhost:8080/api/rag/test-retrieve?query=办公地点&topK=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# 响应示例
{
  "code": 200,
  "message": "成功",
  "data": [
    {
      "content": "人力资源中心办公地点位于....",
      "fileName": "人力资源中心简介.txt",
      "score": 0.826
    },
    {
      "content": "使用说明：本知识库包含....",
      "fileName": "00-使用说明.txt",
      "score": 0.899
    }
  ]
}
```

## 🧠 RAG知识库系统

### RAG架构

```
用户查询 → Embedding向量化 → Qdrant检索 → 上下文增强 → AI生成回答
                                 ↓
                        知识库文档（向量存储）
```

### 核心组件

#### 1. 向量数据库 (Qdrant)
- 存储文档向量
- 高性能相似度检索
- 支持元数据过滤
- Collection: `campus_knowledge`

#### 2. 文本处理管道
```java
文件读取(TextReader) 
  → 文本分块(TokenTextSplitter, 800字符/块, 100字符重叠)
  → 向量化(DashScope text-embedding-v3, 1536维)
  → 存储(VectorStore.add())
```

#### 3. 检索增强流程
1. 用户输入查询
2. 查询文本向量化
3. Qdrant检索top-K相似文档（默认5个）
4. 相似度过滤（阈值0.0，不过滤）
5. 文档上下文注入系统提示词
6. AI生成增强回答

### 知识库管理

#### 文件去重机制
- 基于文件内容MD5哈希
- 避免重复向量化
- 支持增量更新

#### 支持的文档格式
- TXT文本文件
- PDF文档
- Word文档(.docx)
- 自动字符编码检测（UTF-8优先）

#### 批处理优化
- 每批最多25个文本（DashScope API限制）
- 自动内存管理
- 支持大规模文档处理

### 关键配置

```yaml
rag:
  knowledge-base-path: src/main/resources/rag-knowledge-base  # 知识库路径
  chunk-size: 800           # 分块大小
  chunk-overlap: 100        # 重叠大小
  retrieval-top-k: 5        # 检索文档数
  score-threshold: 0.0      # 相似度阈值（0=不过滤）
  vector-dimension: 1536    # 向量维度
  batch-size: 25            # 批处理大小
```

### 性能指标

- **检索延迟**: < 100ms (Qdrant本地部署)
- **向量化速度**: ~50文本/秒 (批量)
- **准确率**: 基于语义相似度，优于关键词匹配
- **扩展性**: 支持百万级文档

## 🔐 权限系统

### 角色说明

- **游客**：未登录用户，可访问公开资源（活动介绍、往届活动、往届成员）
- **部员**：普通成员，可查看资料、使用AI助手、上传资料
- **部长/副部长**：管理员，拥有所有管理权限（创建、编辑、删除）

### 权限注解

```java
@RequireMemberRole("peration Description")    // 需要部员及以上权限
@RequireMinisterRole("Operation Description")  // 需要部长权限
```

### JWT令牌结构

```json
{
  "userId": 1,
  "username": "admin",
  "roleHistory": "2024级部长,2023级部员",
  "exp": 1727890800
}
```

## 💾 数据模型

### 核心实体

- **User** - 用户信息（学号、姓名、角色历史）
- **Activity** - 活动介绍（背景、意义、目的、流程）
- **PastActivity** - 往届活动（推文URL、封面图片、年份）
- **Material** - 内部资料（三级分类：分类→子分类→资料）
- **MaterialCategory** - 资料主分类
- **MaterialSubcategory** - 资料子分类
- **AlumniMember** - 往届成员（姓名、年份、职位）
- **ActivationCode** - 激活码

### 数据库设计特点

- 支持软删除
- 时间戳记录
- 外键约束
- 索引优化
- UTF-8编码

## 🗂️ 文件管理

### 阿里云OSS集成

- 自动文件上传
- UUID文件命名
- **预签名URL下载**（1小时有效期）
- 多格式支持
- 自动类型检测
- 文件删除管理
- 安全访问控制

### 资料管理特性

#### 三级分类体系
```
主分类（如：学生会相关资料）
  └── 子分类（如：规章制度）
        └── 资料文件（PDF、DOC等）
```

#### 下载流程
1. 点击"生成下载链接"按钮
2. 后端生成1小时有效的预签名URL
3. 前端显示下载链接和操作按钮
4. 用户可复制链接或直接下载

### 支持格式

- **图片**：JPG, PNG, GIF
- **文档**：PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX

## 🔧 配置说明

### 应用配置

```yaml
server:
  port: 8080

spring:
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      
jwt:
  secret: your-secret-key
  expiration: 7200000  # 2小时

pagehelper:
  helper-dialect: mysql
  reasonable: true

# CORS配置
cors:
  allowed-origins: http://localhost:5173
  allowed-methods: GET,POST,PUT,DELETE,OPTIONS
```

### 前端配置

```typescript
// vite.config.ts
export default defineConfig({
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

### 日志配置

```yaml
logging:
  level:
    com.redmoon2333: DEBUG
    root: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{50} - %msg%n"
```

## 🧪 测试指南

### 后端测试

```bash
mvn test
```

### 前端测试

```bash
cd hrofficial-frontend
npm run test
```

### API测试

推荐使用Postman或类似工具进行API测试。

示例请求：

```bash
# 用户登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}'

# 获取活动列表（需要Token）
curl -X GET http://localhost:8080/api/activities \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 📈 性能优化

- **分页查询**：使用PageHelper避免大数据量查询
- **连接池**：HikariCP高性能连接池（最大20个连接）
- **流式输出**：AI对话使用SSE流式传输，减少等待时间
- **向量检索**：Qdrant HNSW索引，毫秒级检索
- **批量向量化**：DashScope批量API，提升25倍效率
- **预签名URL**：文件下载使用临时URL，避免直接暴露存储路径
- **前端优化**：
  - 路由懒加载
  - 组件按需引入
  - 图片懒加载
  - 请求防抖节流
- **异步处理**：文件上传、知识库初始化异步处理
- **索引优化**：数据库索引优化
- **内存管理**：文本分块后主动释放，定期GC

## 🔒 安全措施

- JWT令牌认证（2小时有效期）
- CORS跨域配置（仅允许指定域名）
- SQL注入防护（MyBatis预编译）
- XSS攻击防护
- 文件类型检查（白名单机制）
- 权限细粒度控制（注解+AOP）
- 预签名URL（1小时有效期，防止链接滥用）
- 激活码注册机制（防止恶意注册）
- 敏感信息环境变量配置

## 🚀 部署指南

### Docker部署（推荐）

#### 后端Dockerfile
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/HumanResourceOfficial-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
```

#### 前端Dockerfile
```dockerfile
FROM node:16-alpine as builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

#### Docker Compose（推荐）
```yaml
version: '3.8'
services:
  backend:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/hrofficial
      - ALIYUN_OSS_ACCESS_KEY_ID=${ALIYUN_OSS_ACCESS_KEY_ID}
      - ALIYUN_OSS_ACCESS_KEY_SECRET=${ALIYUN_OSS_ACCESS_KEY_SECRET}
      - QWEN_API_KEY=${QWEN_API_KEY}
      - aliQwen_api=${QWEN_API_KEY}
    depends_on:
      - db
      - qdrant
  
  frontend:
    build: ./hrofficial-frontend
    ports:
      - "80:80"
    depends_on:
      - backend
  
  db:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=root
      - MYSQL_DATABASE=hrofficial
    volumes:
      - mysql_data:/var/lib/mysql
  
  qdrant:
    image: qdrant/qdrant:latest
    ports:
      - "6333:6333"
      - "6334:6334"
    volumes:
      - qdrant_data:/qdrant/storage

volumes:
  mysql_data:
  qdrant_data:
```

### 传统部署

```bash
# 打包
mvn clean package -DskipTests

# 运行
java -jar target/HumanResourceOfficial-1.0-SNAPSHOT.jar
```

### 生产环境配置

```yaml
# application-prod.yml
spring:
  profiles:
    active: prod
  datasource:
    url: jdbc:mysql://prod-db:3306/hrofficial_prod
logging:
  level:
    root: WARN
    com.redmoon2333: INFO
```

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

### 代码规范

- 遵循阿里巴巴Java开发手册
- 使用中文注释
- 保持代码简洁明了
- 编写单元测试

## 📋 TODO

- [x] 集成Spring AI框架
- [x] RAG知识库问答
- [x] Qdrant向量数据库
- [x] 语义检索功能
- [ ] 添加Redis缓存支持
- [ ] 实现消息通知功能
- [ ] 添加数据导出功能（Excel）
- [ ] 支持批量操作
- [ ] 实现定时任务（自动清理过期下载链接）
- [ ] 添加监控指标（Prometheus）
- [ ] AI对话历史记录持久化
- [ ] 策划案模板管理
- [ ] 知识库可视化管理界面
- [ ] 多模态RAG（图片、视频）
- [ ] 移动端适配优化
- [ ] 多语言支持

## 🐛 问题反馈

如遇到问题，请在GitHub Issues中提交：

1. 详细描述问题
2. 提供复现步骤
3. 贴出错误日志
4. 说明环境信息

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 👥 团队

- **开发者**: REDACTED_REDIS_PASS2333
- **联系方式**: your-email@example.com

## 🙏 致谢

感谢以下开源项目和服务的支持：

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring AI](https://docs.spring.io/spring-ai/reference/)
- [Spring AI Alibaba](https://sca.aliyun.com/ai/)
- [Vue.js](https://vuejs.org/)
- [Element Plus](https://element-plus.org/)
- [MyBatis](https://mybatis.org/)
- [JWT](https://jwt.io/)
- [Qdrant](https://qdrant.tech/)
- [阿里云OSS](https://www.aliyun.com/product/oss)
- [通义千问](https://tongyi.aliyun.com/)

---

<div align="center">
  
**⭐ 如果这个项目对你有帮助，请给个Star支持一下！**

</div>