# RAG功能实现完成说明

## 已完成的功能

### 后端功能 ✅

1. **Maven依赖**
   - ✅ Qdrant Java客户端 (1.9.1)
   - ✅ gRPC依赖 (1.58.0)
   - ✅ Apache POI (Word文档解析)
   - ✅ PDFBox (PDF文档解析)

2. **配置类**
   - ✅ `QdrantConfig` - Qdrant连接管理
   - ✅ `RagConfig` - RAG功能参数配置

3. **工具类**
   - ✅ `DocumentParser` - 支持TXT、DOCX、PDF文件解析
   - ✅ `TextChunker` - 文本分块和MD5计算

4. **服务类**
   - ✅ `EmbeddingService` - 文本向量化（集成通义千问Embedding API）
   - ✅ `RagManagementService` - 知识库初始化和管理

5. **控制器**
   - ✅ `RagController` - 提供初始化和统计接口

6. **DTO类**
   - ✅ `RagInitRequest` - 初始化请求
   - ✅ `RagInitResponse` - 初始化响应
   - ✅ `RagStatsResponse` - 统计信息响应
   - ✅ `RagChatRequest` - RAG聊天请求

### 前端功能 ✅

1. **管理页面**
   - ✅ `RagManagement.vue` - 知识库管理界面
   - ✅ 统计信息展示
   - ✅ 知识库初始化功能
   - ✅ 初始化结果展示

2. **路由配置**
   - ✅ 添加 `/rag-management` 路由

## 配置说明

### 1. application.yml 配置

需要在 `src/main/resources/application.yml` 中添加以下配置：

```yaml
# Qdrant配置
qdrant:
  host: localhost  # 开发环境
  # host: your-server-ip  # 生产环境
  port: 6334
  api-key: ${QDRANT_API_KEY:}  # 可选，如启用认证
  collection-name: campus_knowledge
  use-tls: false

# RAG配置
rag:
  knowledge-base-path: src/main/resources/rag-knowledge-base
  chunk-size: 800
  chunk-overlap: 100
  embedding-model: text-embedding-v3
  retrieval-top-k: 5
  score-threshold: 0.7
  vector-dimension: 1536
  enable-batch-processing: true
  batch-size: 25
```

### 2. 环境变量

确保已配置通义千问API Key：
```bash
export aliQwen_api="your-qwen-api-key"
```

### 3. Qdrant部署

#### 方式A：Docker部署（推荐）

```bash
# 拉取镜像
docker pull qdrant/qdrant

# 创建数据目录
mkdir -p /data/qdrant_storage

# 启动容器
docker run -d \
  --name qdrant \
  -p 6333:6333 \
  -p 6334:6334 \
  -v /data/qdrant_storage:/qdrant/storage \
  qdrant/qdrant
```

#### 方式B：本地安装

```bash
# 下载Qdrant
wget https://github.com/qdrant/qdrant/releases/download/v1.7.0/qdrant-x86_64-unknown-linux-gnu.tar.gz

# 解压并运行
tar -xzf qdrant-x86_64-unknown-linux-gnu.tar.gz
./qdrant
```

## 使用流程

### 1. 准备知识库文件

在 `src/main/resources/rag-knowledge-base/` 目录下按以下结构组织文件：

```
rag-knowledge-base/
├── 00-使用说明.txt
├── 01-校园生活/
│   ├── 饮食/
│   │   └── 食堂指南.txt
│   ├── 出行/
│   │   ├── 校内交通.txt
│   │   └── 校外交通.txt
│   └── 住宿/
│       └── 宿舍管理.txt
├── 02-学习指导/
│   ├── 选课建议/
│   │   ├── 通识课选课.txt
│   │   └── 专业课选课.txt
│   └── 学习方法/
│       └── 时间管理.txt
└── 04-部门信息/
    └── 人力资源中心简介.txt
```

### 2. 启动Qdrant服务

```bash
# 检查Qdrant是否运行
curl http://localhost:6333/
```

### 3. 初始化知识库

访问前端页面：`http://localhost:5173/rag-management`

1. 点击"初始化知识库"按钮
2. 等待处理完成
3. 查看初始化结果和统计信息

### 4. 测试RAG功能

目前已实现基础的知识库管理功能。要完整使用RAG检索功能，还需要：

## 待完成功能（可选）

### 1. RAG检索集成到AI聊天 ⏳

需要在 `AIChatService` 中添加：
- 创建 `VectorStore` 实现
- 配置 `RetrievalAugmentationAdvisor`
- 在聊天接口中启用RAG模式

### 2. Tool Calling功能 ⏳

需要实现：
- 定义Tool Schema
- 注册工具到ChatClient
- 实现工具调用逻辑

### 3. 增量同步接口 ⏳

实现 `POST /api/rag/sync` 接口用于增量更新知识库

## API接口文档

### 1. 初始化知识库

```
POST /api/rag/initialize
```

**请求体：**
```json
{
  "sourcePath": "src/main/resources/rag-knowledge-base",
  "forceReindex": false
}
```

**响应：**
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "totalFiles": 10,
    "processedFiles": 10,
    "failedFiles": 0,
    "totalChunks": 50,
    "newChunks": 50,
    "duplicateChunks": 0,
    "errors": []
  }
}
```

### 2. 获取统计信息

```
GET /api/rag/stats
```

**响应：**
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "totalDocuments": 10,
    "totalVectors": 50,
    "categoryStats": {},
    "lastUpdateTime": "2024-12-26T10:30:00Z",
    "collectionName": "campus_knowledge",
    "vectorDimension": 1536
  }
}
```

## 测试步骤

1. **启动Qdrant**
   ```bash
   docker start qdrant
   # 或
   ./qdrant
   ```

2. **启动后端**
   ```bash
   mvn spring-boot:run
   ```

3. **启动前端**
   ```bash
   cd hrofficial-frontend
   npm run dev
   ```

4. **测试初始化**
   - 访问 `http://localhost:5173/rag-management`
   - 使用部长账号登录
   - 点击"初始化知识库"

5. **检查结果**
   - 查看初始化报告
   - 查看统计信息
   - 检查Qdrant中的向量数据：
     ```bash
     curl http://localhost:6333/collections/campus_knowledge
     ```

## 知识库内容编写指南

参考设计文档中的"知识库内容框架模板"部分，按照以下要点编写：

1. **文件格式**：UTF-8编码的TXT文件
2. **标题层级**：使用"一、二、三"或"1. 2. 3."
3. **段落分隔**：段落之间使用空行
4. **内容完整**：包含足够的细节和可操作的建议
5. **定期更新**：注明内容的有效期

## 常见问题

### Q1: Qdrant连接失败
**A:** 检查Qdrant服务是否启动，端口6334是否开放

### Q2: 向量化失败
**A:** 检查通义千问API Key是否正确配置

### Q3: 文件解析失败
**A:** 确保文件编码为UTF-8，格式正确

### Q4: 初始化很慢
**A:** 这是正常的，向量化需要时间。可以通过日志查看进度

## 性能优化建议

1. **批量处理**：启用批处理模式可以提高向量化效率
2. **并发控制**：大批量文件处理时注意API调用频率限制
3. **缓存机制**：对热门查询结果进行缓存
4. **定期清理**：删除无效或过期的向量数据

## 安全注意事项

1. **权限控制**：初始化接口仅部长可访问
2. **API Key保护**：不要在代码中硬编码API Key
3. **数据备份**：定期备份Qdrant数据
4. **内容审核**：上传前审核知识库内容，避免敏感信息

## 下一步计划

1. 完成RAG检索功能集成
2. 实现Tool Calling
3. 添加增量同步功能
4. 优化前端交互体验
5. 编写完整的知识库内容
