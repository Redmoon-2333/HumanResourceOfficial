# 模块 5: Spring AI 与 RAG 篇

> 📌 **本篇重点**: Spring AI 基础、ChatClient 使用、RAG 检索增强、向量数据库、Function Calling
> 
> 🎯 **面试频率**: ⭐⭐⭐⭐⭐ (AI 应用开发热点，大模型落地必备技能)
> 
> 💡 **项目亮点**: 本项目实现了完整的 RAG 知识库、流式响应、对话记忆管理、工具调用

---

## 5.1 Spring AI 基础

### ❓ 面试官问：什么是 Spring AI？它的核心组件有哪些？

### ✅ 参考回答

**Spring AI** 是 Spring 官方推出的 AI 应用开发框架，简化与大模型 (LLM) 的集成。

**核心目标**:
- 提供统一的 API 抽象，屏蔽不同 AI 提供商的差异
- 与 Spring 生态无缝集成 (Boot, Cloud, Security)
- 支持同步/流式调用、RAG、工具调用等高级功能

**核心组件**:

```
┌─────────────────────────────────────────────────────────────┐
│                  Spring AI 架构                             │
├─────────────────────────────────────────────────────────────┤
│  1. ChatClient (聊天客户端)                                  │
│     └── 统一的大模型对话接口                                 │
│     └── 支持同步 (.call()) 和流式 (.stream()) 调用           │
├─────────────────────────────────────────────────────────────┤
│  2. ChatModel (聊天模型)                                     │
│     └── 底层模型抽象 (OpenAI, Anthropic, 智谱等)              │
│     └── 负责实际的 API 调用                                  │
├─────────────────────────────────────────────────────────────┤
│  3. Prompt (提示词)                                          │
│     └── 提示词模板管理                                       │
│     └── 提示词工程 (System/User/Assistant Message)          │
├─────────────────────────────────────────────────────────────┤
│  4. VectorStore (向量存储)                                   │
│     └── 文档向量化与存储                                     │
│     └── 相似度检索 (RAG 核心)                                │
├─────────────────────────────────────────────────────────────┤
│  5. EmbeddingModel (嵌入模型)                                │
│     └── 文本→向量转换                                       │
│     └── 支持本地/远程 Embedding API                         │
├─────────────────────────────────────────────────────────────┤
│  6. Tool Callback (工具回调)                                 │
│     └── Function Calling 支持                               │
│     └── AI 可自动调用外部 API                                │
└─────────────────────────────────────────────────────────────┘
```

### 📁 本项目中的体现

**ChatClient 配置** (多客户端支持不同场景):

```java
// SaaLLMConfig.java
@Configuration
public class SaaLLMConfig {
    
    @Bean
    @Qualifier("ecnuChatClient")
    public ChatClient ecnuChatClient(ChatClient.Builder builder) {
        return builder
            .defaultSystem("你是一个有用的人工智能助手")
            .defaultAdvisors(
                new MessageChatMemoryAdvisor(chatMemoryAdvisor),
                new RetrievalAugmentationAdvisor(...)
            )
            .build();
    }
    
    @Bean
    @Qualifier("ecnuToolChatClient")
    public ChatClient ecnuToolChatClient(ChatClient.Builder builder) {
        return builder
            .defaultToolCallbacks(toolService.getTools())  // 工具调用
            .build();
    }
    
    @Bean
    @Qualifier("planGeneratorChatClient")
    public ChatClient planGeneratorChatClient(ChatClient.Builder builder) {
        return builder
            .defaultSystem(promptConfig.getPlanGeneratorPrompt())
            .build();
    }
}
```

---

## 5.2 ChatClient 使用

### ❓ 面试官问：如何使用 ChatClient 进行对话？

### ✅ 参考回答

**ChatClient 调用模式**:

```
┌─────────────────────────────────────────────────────────────┐
│                    ChatClient API                           │
├─────────────────────────────────────────────────────────────┤
│  同步调用:                                                   │
│    String response = chatClient.prompt()                    │
│        .system("你是...")                                    │
│        .user("问题")                                         │
│        .call()                                               │
│        .content();                                           │
├─────────────────────────────────────────────────────────────┤
│  流式调用:                                                   │
│    Flux<String> stream = chatClient.prompt()                │
│        .system("你是...")                                    │
│        .user("问题")                                         │
│        .stream()                                             │
│        .content();                                           │
└─────────────────────────────────────────────────────────────┘
```

**调用链解析**:
```
chatClient           → 获取客户端实例
    .prompt()        → 创建 Prompt 构建器
        .system()    → 设置系统提示词 (角色设定)
        .user()      → 设置用户消息
        .call()      → 同步执行
        .stream()    → 流式执行
            .content() → 提取响应内容
```

### 📁 本项目中的体现

**AIChatService.java** (同步对话):

```java
@Service
public class AIChatService {
    
    @Resource(name = "ecnuChatClient")
    private ChatClient chatClient;
    
    public ChatResponse chat(Integer userId, String message) {
        // 获取对话历史
        List<Message> history = chatMemory.get("user_" + userId);
        
        // 构建提示词
        String response = chatClient.prompt()
                .system(promptConfig.getSystemPrompt())  // 系统提示
                .messages(history.toArray(new Message[0]))  // 历史消息
                .user(message)  // 当前消息
                .call()  // 同步调用
                .content();  // 提取内容
        
        // 格式化并保存记忆
        response = MarkdownFormatter.format(response);
        chatMemory.add("user_" + userId, List.of(
            new UserMessage(message),
            new AssistantMessage(response)
        ));
        
        return new ChatResponse(response, "user_" + userId);
    }
}
```

**流式对话**:

```java
public Flux<String> chatStream(Integer userId, String message) {
    List<Message> history = chatMemory.get("user_" + userId);
    
    StringBuilder fullResponse = new StringBuilder();
    
    return chatClient.prompt()
            .system(promptConfig.getSystemPrompt())
            .messages(history.toArray(new Message[0]))
            .user(message)
            .stream()  // 流式调用
            .content()
            .limitRate(100)  // 背压控制
            .map(chunk -> {
                fullResponse.append(chunk);
                return chunk;
            })
            .doOnComplete(() -> {
                // 流式完成后保存完整对话
                chatMemory.add("user_" + userId, List.of(
                    new UserMessage(message),
                    new AssistantMessage(fullResponse.toString())
                ));
            });
}
```

---

## 5.3 对话记忆管理

### ❓ 面试官问：如何实现多轮对话的记忆功能？

### ✅ 参考回答

**记忆管理方案**:

| 方案 | 实现 | 优点 | 缺点 |
|------|------|------|------|
| **内存存储** | ConcurrentHashMap | 简单快速 | 重启丢失，无法共享 |
| **Redis 存储** | Redis List/Hash | 持久化，可共享 | 增加依赖 |
| **数据库存储** | MySQL 表 | 永久存储 | 查询慢 |
| **向量检索** | Vector DB | 语义相关检索 | 复杂度高 |

**本项目采用 Redis 存储方案**:

```
对话记忆结构:
┌─────────────────────────────────────────────────────────────┐
│  Redis Key: chat:memory:user_{userId}                        │
├─────────────────────────────────────────────────────────────┤
│  Value: List<ChatMessageRecord>                             │
│    [                                                         │
│      {                                                        │
│        "role": "user",                                       │
│        "content": "你好",                                     │
│        "timestamp": 1709251200000                            │
│      },                                                       │
│      {                                                        │
│        "role": "assistant",                                  │
│        "content": "你好！有什么可以帮助你的？",                │
│        "timestamp": 1709251205000                            │
│      }                                                        │
│    ]                                                         │
└─────────────────────────────────────────────────────────────┘
```

### 📁 本项目中的体现

**RedisChatMemory.java** ([`src/main/java/com/redmoon2333/config/RedisChatMemory.java`](../../src/main/java/com/redmoon2333/config/RedisChatMemory.java)):

```java
@Configuration
public class RedisChatMemory {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String KEY_PREFIX = "chat:memory:";
    
    /**
     * 获取对话历史
     */
    public List<Message> get(String conversationId) {
        String key = KEY_PREFIX + conversationId;
        ListOperations<String, Object> ops = redisTemplate.opsForList();
        
        List<Object> rawMessages = ops.range(key, 0, -1);
        
        if (rawMessages == null || rawMessages.isEmpty()) {
            return new ArrayList<>();
        }
        
        return rawMessages.stream()
            .map(this::deserializeMessage)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    /**
     * 添加消息到对话历史
     */
    public void add(String conversationId, List<Message> messages) {
        String key = KEY_PREFIX + conversationId;
        ListOperations<String, Object> ops = redisTemplate.opsForList();
        
        for (Message message : messages) {
            ops.rightPush(key, serializeMessage(message));
        }
        
        // 限制最大消息数量 (防止内存溢出)
        ops.trim(key, -MAX_MESSAGES, -1);
        
        // 设置过期时间 (7 天)
        redisTemplate.expire(key, 7, TimeUnit.DAYS);
    }
    
    /**
     * 清除对话历史
     */
    public void clear(String conversationId) {
        String key = KEY_PREFIX + conversationId;
        redisTemplate.delete(key);
    }
}
```

---

## 5.4 RAG 检索增强

### ❓ 面试官问：什么是 RAG？如何实现？

### ✅ 参考回答

**RAG (Retrieval-Augmented Generation)**: 检索增强生成，结合向量检索和 LLM 生成。

**RAG 工作流程**:

```
┌─────────────────────────────────────────────────────────────┐
│                    RAG 工作流程                              │
├─────────────────────────────────────────────────────────────┤
│  1. 用户提问: "公司有哪些团建活动？"                         │
│         ↓                                                   │
│  2. 问题向量化: Embedding 模型将问题转为向量                  │
│         ↓                                                   │
│  3. 向量检索: 在向量数据库中搜索相似文档                      │
│         ↓                                                   │
│  4. 检索结果: 返回 Top-K 最相关的文档片段                      │
│         ↓                                                   │
│  5. 构建 Prompt: 系统提示 + 检索结果 + 用户问题               │
│         ↓                                                   │
│  6. LLM 生成：大模型基于检索结果生成答案                      │
│         ↓                                                   │
│  7. 返回答案: "公司有以下团建活动：..."                       │
└─────────────────────────────────────────────────────────────┘
```

**为什么需要 RAG**:
- **解决知识时效性**: LLM 训练数据有截止日期
- **减少幻觉**: 基于真实文档生成，减少胡编乱造
- **私有知识**: 企业文档、内部资料无需训练
- **可解释性**: 可追溯答案来源

### 📁 本项目中的体现

**RagConfig.java** (RAG 配置):

```java
@Configuration
@ConfigurationProperties(prefix = "rag")
@Data
public class RagConfig {
    
    // 知识库文件路径
    private String knowledgeBasePath = "src/main/resources/rag-knowledge-base";
    
    // 分块大小 (字符数)
    private int chunkSize = 400;
    
    // 分块重叠 (字符数)
    private int chunkOverlap = 100;
    
    // Embedding 模型
    private String embeddingModel = "ecnu-embedding-small";
    
    // 检索返回数量
    private int retrievalTopK = 5;
    
    // 向量维度
    private int vectorDimension = 1024;
}
```

**AIChatService.java** (RAG 增强调用):

```java
public Flux<String> chatWithRag(Integer userId, String message, boolean useRAG, boolean enableTools) {
    String conversationId = "user_" + userId;
    List<Message> history = chatMemory.get(conversationId);
    
    var promptSpec = chatClient.prompt()
            .system(promptConfig.getSystemPrompt())
            .messages(history.toArray(new Message[0]))
            .user(message);
    
    // 启用 RAG 时添加检索增强 Advisor
    if (useRAG && vectorStore != null) {
        promptSpec = promptSpec.advisors(buildRagAdvisor());
    }
    
    return promptSpec.stream()
            .content()
            .doOnComplete(() -> {
                // 保存对话记忆
                chatMemory.add(conversationId, List.of(
                    new UserMessage(message),
                    new AssistantMessage(fullResponse.toString())
                ));
            });
}

private RetrievalAugmentationAdvisor buildRagAdvisor() {
    return RetrievalAugmentationAdvisor.builder()
            .documentRetriever(VectorStoreDocumentRetriever.builder()
                    .vectorStore(vectorStore)
                    .topK(5)  // 返回 5 个最相关文档
                    .build())
            .build();
}
```

---

## 5.5 向量数据库

### ❓ 面试官问：向量数据库有哪些？如何选择？

### ✅ 参考回答

**常见向量数据库**:

| 向量数据库 | 类型 | 特点 | 适用场景 |
|-----------|------|------|----------|
| **Redis Stack** | 内存 + 持久化 | 简单，与 Redis 共享 | 小规模，已有 Redis |
| **Qdrant** | 专用向量 DB | 性能最强，功能丰富 | 大规模生产 |
| **Milvus** | 专用向量 DB | 开源，支持 PB 级 | 大规模检索 |
| **Weaviate** | 专用向量 DB | 内置向量 + 关键词混合检索 | 混合检索场景 |
| **Pinecone** | SaaS 服务 | 免运维，按量付费 | 快速原型，海外部署 |
| **Chroma** | 嵌入式 | 轻量级，本地优先 | 开发测试 |

**选型建议**:
- **小规模 (<100 万向量)**: Redis Stack
- **中大规模**: Qdrant/Milvus
- **快速原型**: Chroma/Pinecone
- **混合检索**: Weaviate

### 📁 本项目中的体现

**VectorStoreConfig.java** (Redis Vector Store 配置):

```java
@Configuration
public class VectorStoreConfig {
    
    @Autowired
    private RedisConnectionFactory redisConnectionFactory;
    
    @Autowired
    private EmbeddingModel embeddingModel;
    
    @Autowired
    private RagConfig ragConfig;
    
    @Bean
    public VectorStore vectorStore() {
        // 配置 Redis 向量存储
        RedisVectorStoreConfig vectorStoreConfig = new RedisVectorStoreConfig();
        vectorStoreConfig.setIndexName("rag-knowledge");
        vectorStoreConfig.setDimension(ragConfig.getVectorDimension());
        
        return RedisVectorStore.builder(redisConnectionFactory)
                .indexConfig(vectorStoreConfig)
                .build();
    }
}
```

**应用配置** (application.yml):

```yaml
spring:
  ai:
    vectorstore:
      redis:
        index-name: rag-knowledge
        dimension: 1024
        distance-type: cosine
```

---

## 5.6 Embedding 模型

### ❓ 面试官问：Embedding 模型是什么？有哪些选择？

### ✅ 参考回答

**Embedding 模型**: 将文本转换为固定长度的向量表示，语义相似的文本向量距离更近。

**核心指标**:
- **维度**: 向量长度 (常见 384/768/1024/1536)
- **相似度计算**: 余弦相似度/欧氏距离/点积
- **最大输入长度**: token 限制 (常见 512/1024)

**常见 Embedding 模型**:

| 模型 | 维度 | 特点 | 来源 |
|------|------|------|------|
| **text-embedding-3-small** | 1536 | OpenAI 官方，效果好 | OpenAI API |
| **text-embedding-3-large** | 3072 | OpenAI 大模型 | OpenAI API |
| **m3e-base** | 768 | 中文优化，开源 | Moka |
| **bge-large-zh** | 1024 | 中文优化，效果好 | BAAI |
| **ecnu-embedding-small** | 1024 | 华东师大本地模型 | 本地部署 |

### 📁 本项目中的体现

**SaaLLMConfig.java** (Embedding 模型配置):

```java
@Configuration
public class SaaLLMConfig {
    
    @Value("${spring.ai.chat.options.embedding-model:ecnu-embedding-small}")
    private String embeddingModelName;
    
    @Bean
    public EmbeddingModel embeddingModel() {
        // 使用 ECNU 提供的本地 Embedding 模型
        return new ECNUEmbeddingModel(embeddingModelName);
    }
}
```

**配置文件** (application.yml):

```yaml
spring:
  ai:
    chat:
      options:
        embedding-model: ecnu-embedding-small
rag:
  embedding-model: ecnu-embedding-small
  vector-dimension: 1024  # 必须与模型输出维度一致
```

---

## 5.7 智能分块策略

### ❓ 面试官问：RAG 中文档如何分块？有什么策略？

### ✅ 参考回答

**分块策略对比**:

| 策略 | 方法 | 优点 | 缺点 | 适用场景 |
|------|------|------|------|----------|
| **固定大小** | 按字符数切分 | 简单，易于控制 | 可能截断语义 | 通用场景 |
| **按段落** | 按空行/标题切分 | 语义完整 | 块大小不均 | 结构化文档 |
| **按句子** | 按句号切分 | 细粒度 | 碎片化 | 问答对 |
| **递归分块** | 段落→句子→字符 | 平衡粒度与完整 | 复杂 | 长文档 |
| **智能分块** | 识别章节/标题 | 语义最完整 | 实现复杂 | 技术文档/书籍 |

**本项目智能分块实现**:

```
文档类型识别 → 选择分块策略 → 分块 → 向量化
      ↓              ↓           ↓        ↓
  PDF/MD/TXT    固定/段落    400 字符   Embedding
```

### 📁 本项目中的体现

**SmartTextChunker.java** (智能分块器):

```java
@Component
public class SmartTextChunker {
    
    @Autowired
    private RagConfig ragConfig;
    
    /**
     * 智能分块
     * 根据文档类型自动选择最佳分块策略
     */
    public List<TextChunk> chunk(String content, String docType) {
        if (ragConfig.isEnableSemanticChunking()) {
            return semanticChunk(content, docType);
        } else {
            return fixedSizeChunk(content);
        }
    }
    
    /**
     * 语义分块：按章节、段落边界智能分割
     */
    private List<TextChunk> semanticChunk(String content, String docType) {
        List<TextChunk> chunks = new ArrayList<>();
        
        // 识别章节标题 (Markdown 的 ##, ###)
        String[] sections = content.split("(?=#\\s+)");
        
        for (String section : sections) {
            // 如果章节过大，继续按段落分割
            if (section.length() > ragConfig.getChunkSize()) {
                String[] paragraphs = section.split("\n\n+");
                chunks.addAll(chunkParagraphs(paragraphs));
            } else {
                chunks.add(new TextChunk(section.trim(), null));
            }
        }
        
        return chunks;
    }
    
    /**
     * 固定大小分块
     */
    private List<TextChunk> fixedSizeChunk(String content) {
        List<TextChunk> chunks = new ArrayList<>();
        int chunkSize = ragConfig.getChunkSize();
        int overlap = ragConfig.getChunkOverlap();
        
        for (int i = 0; i < content.length(); i += chunkSize - overlap) {
            int end = Math.min(i + chunkSize, content.length());
            String chunk = content.substring(i, end);
            chunks.add(new TextChunk(chunk, null));
            
            if (end >= content.length()) break;
        }
        
        return chunks;
    }
}
```

---

## 5.8 流式响应实现

### ❓ 面试官问：如何实现流式响应？有什么应用场景？

### ✅ 参考回答

**流式响应原理**:

```
传统响应 (等待全部生成):
  用户请求 → LLM 生成全文 → 一次性返回
  └── 延迟高，用户等待时间长

流式响应 (边生成边返回):
  用户请求 → LLM 生成第一个 token → 立即返回
              ↓
           生成第二个 token → 立即返回
              ↓
           ... 持续输出直到完成
  └── 延迟低，用户体验好
```

**技术实现**:
- **SSE (Server-Sent Events)**: 单向推送，浏览器原生支持
- **WebSocket**: 双向通信，更复杂
- **HTTP Chunked Transfer**: 分块传输

**本项目使用 SSE**:

```
客户端                     服务端
  │                         │
  │─── POST /api/ai/chat-stream ──→│
  │                         │
  │←─ data: {chunk1} ────────────│
  │←─ data: {chunk2} ────────────│
  │←─ data: {chunk3} ────────────│
  │←─ data: [DONE] ──────────────│
```

### 📁 本项目中的体现

**AIChatController.java** (流式接口):

```java
@PostMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> chatStream(
        @RequestBody ChatRequest request,
        HttpServletRequest httpRequest) {
    
    Integer userId = (Integer) httpRequest.getAttribute("userId");
    
    return aiChatService.chatStream(userId, request.getMessage())
            .doOnSubscribe(subscription -> 
                logger.info("客户端开始订阅流式响应，用户 ID: {}", userId))
            .doOnComplete(() -> 
                logger.info("流式对话完成，用户 ID: {}", userId))
            .doOnCancel(() -> 
                logger.warn("客户端取消了流式请求，用户 ID: {}", userId))
            .doOnError(error -> {
                // 客户端断开连接不记录为 error
                if (isClientDisconnect(error)) {
                    logger.warn("客户端断开连接");
                } else {
                    logger.error("流式对话错误", error);
                }
            })
            .onErrorResume(error -> 
                isClientDisconnect(error) ? Flux.empty() : Flux.error(error));
}
```

**前端调用示例** (JavaScript):

```javascript
const response = await fetch('/api/ai/chat-stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message: '你好' })
});

const reader = response.body.getReader();
const decoder = new TextDecoder();

while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    
    const chunk = decoder.decode(value);
    // 处理 SSE 格式：data: {...}
    const lines = chunk.split('\n');
    for (const line of lines) {
        if (line.startsWith('data: ')) {
            const data = JSON.parse(line.slice(6));
            console.log('收到数据:', data);
        }
    }
}
```

---

## 5.9 Function Calling

### ❓ 面试官问：什么是 Function Calling？如何实现？

### ✅ 参考回答

**Function Calling (工具调用)**: 让 LLM 能够调用外部函数/API，获取实时信息或执行操作。

**工作原理**:

```
┌─────────────────────────────────────────────────────────────┐
│                  Function Calling 流程                       │
├─────────────────────────────────────────────────────────────┤
│  1. 用户提问: "查询张三的联系方式"                            │
│         ↓                                                   │
│  2. LLM 分析：需要调用 getUserContact 函数                    │
│         ↓                                                   │
│  3. LLM 返回：函数名 + 参数 {name: "张三"}                     │
│         ↓                                                   │
│  4. 服务端：执行 getUserContact("张三")                       │
│         ↓                                                   │
│  5. 获取结果：{phone: "138****1234", email: "zhang@..."}     │
│         ↓                                                   │
│  6. LLM 整合：生成自然语言回复"张三的联系方式是..."           │
└─────────────────────────────────────────────────────────────┘
```

**典型应用场景**:
- 查询数据库/实时信息
- 调用第三方 API (天气、股票)
- 执行操作 (发送邮件、创建订单)
- 计算/数据处理

### 📁 本项目中的体现

**ToolService.java** (工具定义):

```java
@Service
public class ToolService {
    
    /**
     * 查询活动详情
     */
    @Tool(description = "查询活动详情信息")
    public String queryActivity(@ToolParam(description = "活动 ID 或名称") String activityIdentifier) {
        // 实现查询逻辑
        Activity activity = activityService.getActivityByName(activityIdentifier);
        return activity != null ? activity.toString() : "未找到该活动";
    }
    
    /**
     * 查询成员信息
     */
    @Tool(description = "查询社团成员信息")
    public String queryMember(@ToolParam(description = "成员姓名或 ID") String memberIdentifier) {
        // 实现查询逻辑
        Member member = memberService.getMemberByIdentifier(memberIdentifier);
        return member != null ? member.toString() : "未找到该成员";
    }
}
```

**ChatClient 配置工具调用**:

```java
@Bean
@Qualifier("ecnuToolChatClient")
public ChatClient ecnuToolChatClient(ChatClient.Builder builder) {
    return builder
        .defaultToolCallbacks(toolService.getTools())  // 注册工具
        .build();
}
```

**工具调用执行**:

```java
public Flux<String> handleToolCalling(String message, Integer userId) {
    ToolCallback[] tools = ToolCallbacks.from(toolService);
    
    String response = toolChatClient.prompt()
            .system(promptConfig.getSystemPromptWithTools())
            .user(message)
            .toolCallbacks(tools)  // 启用工具调用
            .call()
            .content();
    
    return simulateStream(response);
}
```

---

## 5.10 Prompt 工程

### ❓ 面试官问：什么是 Prompt 工程？有什么技巧？

### ✅ 参考回答

**Prompt 工程**: 设计和优化提示词，以获得更好的 LLM 输出质量。

**核心技巧**:

| 技巧 | 说明 | 示例 |
|------|------|------|
| **角色设定** | 给 LLM 指定角色 | "你是一位资深 HR 专家" |
| **任务描述** | 清晰说明任务 | "请生成一份活动策划案" |
| **输出格式** | 指定输出结构 | "使用 Markdown 格式，包含活动背景、目标、流程" |
| **Few-Shot** | 提供示例 | "示例 1: ... 示例 2: ... 请参照上述格式" |
| **思维链** | 引导逐步思考 | "请一步步分析：首先... 其次... 最后..." |
| **约束条件** | 限制输出范围 | "不超过 500 字", "只输出 JSON" |

**Prompt 模板结构**:

```
┌─────────────────────────────────────────────────────────────┐
│                    Prompt 模板结构                          │
├─────────────────────────────────────────────────────────────┤
│  System: 你是人力资源官网的 AI 助手，负责解答社团活动相关问题    │
│         请使用专业、友好的语气回答                            │
├─────────────────────────────────────────────────────────────┤
│  Context: [检索到的相关文档]                                   │
│           - 活动策划流程文档...                               │
│           - 场地申请指南...                                   │
├─────────────────────────────────────────────────────────────┤
│  User: 如何申请活动场地？                                      │
├─────────────────────────────────────────────────────────────┤
│  Assistant: 根据提供的文档，申请活动场地需要以下步骤：...     │
└─────────────────────────────────────────────────────────────┘
```

### 📁 本项目中的体现

**PromptConfig.java** (提示词配置):

```java
@Configuration
@ConfigurationProperties(prefix = "prompt")
@Data
public class PromptConfig {
    
    /**
     * 系统提示词
     */
    private String systemPrompt = """
        你是人力资源官网的 AI 助手，负责解答社团活动相关问题。
        请使用专业、友好的语气回答。
        如果不确定答案，请如实告知用户。
        """;
    
    /**
     * 策划案生成提示词模板
     */
    private String planGeneratorPrompt = """
        你是一位资深的活动策划专家。
        请根据以下信息生成一份详细的活动策划案：
        
        - 活动主题：{theme}
        - 主办方：{organizer}
        - 活动时间：{eventTime}
        - 活动地点：{eventLocation}
        - 参与人数：{participants}
        
        策划案应包含：
        1. 活动背景与意义
        2. 活动目标
        3. 活动流程
        4. 人员分工
        5. 预算估算
        6. 风险预案
        
        请使用 Markdown 格式输出。
        """;
}
```

**配置文件** (application.yml):

```yaml
prompt:
  system-prompt: |
    你是人力资源官网的 AI 助手，负责解答社团活动相关问题。
    请使用专业、友好的语气回答。
  
  plan-generator-prompt: |
    你是一位资深的活动策划专家。
    请根据以下信息生成一份详细的活动策划案：
    ...
```

---

## 📝 本篇小结

### 核心知识点回顾

1. ✅ Spring AI 基础与核心组件
2. ✅ ChatClient 同步/流式调用
3. ✅ 对话记忆管理 (Redis 存储)
4. ✅ RAG 检索增强原理与实现
5. ✅ 向量数据库选型与配置
6. ✅ Embedding 模型选择
7. ✅ 智能分块策略
8. ✅ 流式响应实现 (SSE)
9. ✅ Function Calling 工具调用
10. ✅ Prompt 工程技巧

### 面试准备建议

**初级开发**: 重点掌握 5.1、5.2、5.3、5.8
**中级开发**: 重点掌握 5.4、5.5、5.7、5.9
**高级开发**: 深入理解 5.6 Embedding 原理、5.10 Prompt 工程、RAG 整体架构

---

> 💡 **下一步**: 继续学习 [模块 6: 数据库与 MyBatis-Plus 篇](./06-数据库与 MyBatis-Plus 篇.md)
