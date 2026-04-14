# Configuration Package - Spring Boot Configs

**Parent:** `../../AGENTS.md` | **Files:** 16 config classes

---

## OVERVIEW

Centralized configuration management for Spring Boot application. Handles security, Redis, AI integration, file storage, and cross-cutting concerns.

---

## STRUCTURE

```
config/
├── SecurityConfig.java          # JWT auth, CORS, role-based security
├── RedisConfig.java             # Redis connection, vector store setup
├── AiConfig.java                # Spring AI chat client, embedding model
├── OssConfig.java               # Aliyun OSS client configuration
├── WebConfig.java               # MVC config, message converters
├── CorsConfig.java              # CORS policies (separate bean)
├── MyBatisPlusConfig.java       # MP pagination, logical delete, underscor
├── JwtAuthenticationFilter.java # JWT token validation filter
├── RequestLoggingFilter.java    # HTTP request logging
├── PermissionAspect.java        # AOP for permission logging
├── AiChatMemoryConfig.java      # Redis-based chat memory (TTL 7 days)
├── RagConfig.java               # RAG retrieval, chunking params
├── FileUploadConfig.java        # Max file size (50MB), request size (100MB)
├── ThreadPoolConfig.java        # Async task execution pools
├── PerformanceMonitorConfig.java # Memory, vector index monitoring
└── SwaggerConfig.java           # API documentation (if present)
```

---

## WHERE TO LOOK

| Task | Config Class | Key Properties |
|------|--------------|----------------|
| **Modify JWT settings** | `SecurityConfig` + `JwtUtil` | Secret key, expiration (7 days default) |
| **Change Redis behavior** | `RedisConfig` + `AiChatMemoryConfig` | Connection, vector index name, TTL |
| **Update AI models** | `AiConfig` + `RagConfig` | Model names, temperature, chunk size |
| **Configure OSS** | `OssConfig` | Endpoint, access keys, bucket name |
| **Adjust MP behavior** | `MyBatisPlusConfig` | Pagination, logical delete values |
| **Modify CORS** | `CorsConfig` | Allowed origins, methods, headers |
| **Change file limits** | `FileUploadConfig` | Max file/request sizes |
| **Tune thread pools** | `ThreadPoolConfig` | Core/max pool sizes, queue capacity |

---

## CONFIGURATION PATTERNS

### 1. Security Configuration (`SecurityConfig.java`)

**Key Features:**
- JWT-based stateless authentication
- Role-based access control (RBAC)
- CORS enabled for frontend
- JWT blacklist in Redis for logout

**Environment Variables:**
```yaml
JWT_SECRET: ${JWT_SECRET:default_256_bit_key_for_dev_only}
JWT_EXPIRATION: ${JWT_EXPIRATION:604800000}  # 7 days in ms
```

**Pattern:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addAuthenticationFilter(new JwtAuthenticationFilter(jwtUtil))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/api/activities", "/api/past-activities/**").permitAll()
                .anyRequest().authenticated()
            );
    }
}
```

### 2. Redis Configuration (`RedisConfig.java`)

**Key Features:**
- Lettuce connection factory
- Redis Vector Store for RAG
- JSON serialization for complex objects

**Environment Variables:**
```yaml
REDIS_HOST: ${REDIS_HOST:localhost}
REDIS_PORT: ${REDIS_PORT:6379}
REDIS_PASSWORD: ${REDIS_PASSWORD:}
```

**Vector Store Setup:**
```java
@Bean
public VectorStore vectorStore(RedisConnectionFactory factory) {
    return RedisVectorStore.builder(factory)
        .indexName("campus-knowledge-index")
        .prefix("rag:embedding:")
        .dimensions(1024)
        .build();
}
```

### 3. AI Configuration (`AiConfig.java` + `RagConfig.java`)

**Key Features:**
- Spring AI OpenAI client (ECNU AI compatible)
- Separate models for chat, tool calling, embedding
- RAG retrieval parameters

**Environment Variables:**
```yaml
CHATECNU_BASE_URL: ${CHATECNU_BASE_URL:https://chat.ecnu.edu.cn/open/api/v1}
CHATECNU_API_KEY: ${CHATECNU_API_KEY:}
AI_TOOL_MODEL: ${AI_TOOL_MODEL:ecnu-max}
```

**Model Configuration:**
```yaml
spring:
  ai:
    openai:
      chat:
        options:
          model: ecnu-plus
          temperature: 0.7
      embedding:
        options:
          model: ecnu-embedding-small
    tool-model: ecnu-max
```

**RAG Parameters:**
```yaml
rag:
  chunk-size: 400
  chunk-overlap: 100
  retrieval-top-k: 5
  enable-semantic-chunking: true
```

### 4. MyBatis-Plus Configuration (`MyBatisPlusConfig.java`)

**Key Features:**
- Logical delete handler
- Underscore to camelCase mapping
- Built-in pagination interceptor

**Configuration:**
```java
@Configuration
public class MyBatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor paginationInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }
    
    @Bean
    public LogicDeleteRemovePlugin logicDeletePlugin() {
        return new LogicDeleteRemovePlugin();
    }
}
```

**Entity Pattern:**
```java
@TableName("user")
@Data
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableLogic  // 0 = active, 1 = deleted
    private Integer deleted;
}
```

### 5. OSS Configuration (`OssConfig.java`)

**Key Features:**
- Aliyun OSS client
- Pre-signed URL generation for secure uploads
- Optional (can use local storage)

**Environment Variables:**
```yaml
ALIYUN_OSS_ENDPOINT: ${ALIYUN_OSS_ENDPOINT:oss-cn-beijing.aliyuncs.com}
ALIYUN_OSS_ACCESS_KEY_ID: ${ALIYUN_OSS_ACCESS_KEY_ID:}
ALIYUN_OSS_ACCESS_KEY_SECRET: ${ALIYUN_OSS_ACCESS_KEY_SECRET:}
ALIYUN_OSS_BUCKET_NAME: ${ALIYUN_OSS_BUCKET_NAME:}
```

---

## CONFIG INTERACTIONS

```
SecurityConfig
    ↓ (uses Redis for JWT blacklist)
RedisConfig
    ↓ (provides VectorStore for RAG)
RagConfig + AiConfig
    ↓ (OSS for file storage, optional)
OssConfig
```

---

## NOTES

- **All configs use `@Configuration`** - Spring bean definitions
- **Environment variable injection** - Use `${VAR:default}` pattern
- **Profile-specific configs** - `application-dev.yml`, `application-prod.yml`
- **No config in code** - All externalized via `application.yml` or env vars
- **Redis Stack required** - Must use `redis/redis-stack` image for vector search
- **JWT secret MUST be changed** - Default value is dev-only, use env var in production

---

## HIERARCHY

- **Parent:** `../AGENTS.md` (backend package)
- **Root:** `../../../AGENTS.md`