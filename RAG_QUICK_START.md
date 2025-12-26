# RAG功能快速开始指南

## 🎯 功能简介

RAG（检索增强生成）功能已成功集成到人力资源管理系统中，支持：
- ✅ 向量数据库管理（基于Qdrant）
- ✅ 文档解析（TXT、DOCX、PDF）
- ✅ 智能分块和去重（MD5）
- ✅ 语义检索增强AI对话
- ✅ Web管理界面

## 🚀 快速开始

### 第一步：部署Qdrant

**使用Docker（推荐）：**
```bash
docker run -d \
  --name qdrant \
  -p 6333:6333 \
  -p 6334:6334 \
  -v $(pwd)/qdrant_storage:/qdrant/storage \
  qdrant/qdrant
```

验证部署：
```bash
curl http://localhost:6333/
```

### 第二步：配置应用

在`application.yml`中添加配置：

```yaml
qdrant:
  host: localhost
  port: 6334
  collection-name: campus_knowledge
  use-tls: false

rag:
  knowledge-base-path: src/main/resources/rag-knowledge-base
  chunk-size: 800
  chunk-overlap: 100
  embedding-model: text-embedding-v3
  retrieval-top-k: 5
  score-threshold: 0.7
  vector-dimension: 1536
```

### 第三步：准备知识库文件

在`src/main/resources/rag-knowledge-base/`目录创建内容：

```
rag-knowledge-base/
├── 00-使用说明.txt
├── 04-部门信息/
│   └── 人力资源中心简介.txt
└── ... (更多内容)
```

### 第四步：启动服务

```bash
# 启动后端
mvn spring-boot:run

# 启动前端
cd hrofficial-frontend
npm run dev
```

### 第五步：初始化知识库

1. 访问 `http://localhost:5173/rag-management`
2. 使用部长账号登录
3. 点击"初始化知识库"按钮
4. 等待处理完成，查看结果

### 第六步：使用RAG对话

**API调用示例：**
```bash
curl -X POST http://localhost:8080/api/ai/chat-with-rag \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "人力资源中心的职能是什么？",
    "useRAG": true
  }'
```

## 📋 完整功能列表

### 后端实现 ✅

| 组件 | 状态 | 说明 |
|------|------|------|
| Maven依赖 | ✅ | Qdrant、gRPC、POI、PDFBox |
| 配置类 | ✅ | QdrantConfig、RagConfig |
| 文档解析 | ✅ | TXT、DOCX、PDF支持 |
| 文本分块 | ✅ | 智能切分、边界识别 |
| MD5去重 | ✅ | 防止重复内容 |
| 向量化服务 | ✅ | 通义千问Embedding API |
| 检索服务 | ✅ | 语义搜索、相似度过滤 |
| RAG聊天 | ✅ | 检索增强生成 |
| 管理接口 | ✅ | 初始化、统计、同步 |

### 前端实现 ✅

| 组件 | 状态 | 说明 |
|------|------|------|
| 管理页面 | ✅ | Vue3组件、Element Plus |
| 统计展示 | ✅ | 向量数、Collection信息 |
| 初始化功能 | ✅ | 表单提交、结果展示 |
| 路由配置 | ✅ | /rag-management |

### 待扩展功能 ⏳

| 功能 | 优先级 | 说明 |
|------|--------|------|
| Tool Calling | 中 | AI调用数据库工具 |
| 增量同步 | 中 | 自动检测文件变化 |
| 分类统计 | 低 | 按分类展示向量数 |
| 批量删除 | 低 | 管理界面批量操作 |

## 🔧 API文档

### 1. 初始化知识库

```
POST /api/rag/initialize
Authorization: Bearer {token}
Content-Type: application/json

请求体：
{
  "sourcePath": "src/main/resources/rag-knowledge-base",
  "forceReindex": false
}

响应：
{
  "code": 200,
  "data": {
    "totalFiles": 10,
    "processedFiles": 10,
    "totalChunks": 50,
    "newChunks": 50,
    "duplicateChunks": 0
  }
}
```

### 2. 获取统计信息

```
GET /api/rag/stats
Authorization: Bearer {token}

响应：
{
  "code": 200,
  "data": {
    "totalVectors": 50,
    "collectionName": "campus_knowledge",
    "vectorDimension": 1536
  }
}
```

### 3. RAG增强对话

```
POST /api/ai/chat-with-rag
Authorization: Bearer {token}
Content-Type: application/json

请求体：
{
  "message": "你的问题",
  "useRAG": true
}

响应：SSE流式输出
```

## 💡 使用技巧

### 知识库内容编写

1. **文件编码**：必须使用UTF-8
2. **文件大小**：建议单文件<5MB
3. **内容质量**：准确、完整、实用
4. **更新频率**：定期审核和更新

### 最佳实践

1. **首次初始化**：使用小批量文件测试
2. **定期备份**：备份Qdrant数据目录
3. **监控日志**：关注初始化和检索日志
4. **参数调优**：根据效果调整chunk_size和score_threshold

### 常见问题

**Q1: 初始化速度慢？**
A: 向量化需要时间，属于正常现象。可通过批处理优化。

**Q2: 检索结果不相关？**
A: 调低score_threshold或增加top_k值。

**Q3: Qdrant连接失败？**
A: 检查服务是否启动，端口是否正确。

**Q4: 文件解析失败？**
A: 确保文件格式正确，编码为UTF-8。

## 📊 性能指标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| 检索延迟 | <500ms | 单次检索时间 |
| 初始化速度 | ~10文件/分钟 | 取决于API限流 |
| 向量维度 | 1536 | 通义千问标准 |
| 支持文档 | 100+ | 推荐规模 |

## 🎓 进阶使用

### 自定义检索参数

```java
// 在RagRetrievalService中调用
List<RetrievedDocument> docs = ragRetrievalService.retrieve(
    query,
    10,    // topK
    0.6    // scoreThreshold
);
```

### 手动向量存储

```java
// 获取向量
float[] vector = embeddingService.embedText("内容");

// 存储到Qdrant
ragManagementService.storeVector(...);
```

### 批量向量化

```java
List<String> texts = Arrays.asList("文本1", "文本2");
List<float[]> vectors = embeddingService.embedBatch(texts);
```

## 📞 技术支持

遇到问题请：
1. 查看日志文件
2. 检查配置是否正确
3. 参考API文档
4. 联系技术负责人

---

**祝使用愉快！🎉**
