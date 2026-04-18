# 模块 1: Spring Boot 基础篇

> 📌 **本篇重点**: Spring Boot 核心机制、启动流程、配置管理、Bean 生命周期
> 
> 🎯 **面试频率**: ⭐⭐⭐⭐⭐ (所有 Java 后端面试必问)

---

## 1.1 Spring Boot 启动流程

### ❓ 面试官问：请详细描述 Spring Boot 的启动流程

### ✅ 参考回答

Spring Boot 启动流程可以分为 **5 个核心阶段**：

```
┌─────────────────────────────────────────────────────────────┐
│  1. 创建 SpringApplication 实例                              │
│     └── 推断应用类型 (SERVLET/REACTIVE/COMMON)               │
│     └── 加载 ApplicationContextInitializer                   │
│     └── 加载 SpringApplicationRunListener                    │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  2. 调用 run() 方法                                           │
│     └── 启动计时器                                            │
│     └── 获取 Environment 环境                                 │
│     └── 准备 ConfigurableEnvironment                         │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  3. 创建 ApplicationContext 容器                             │
│     └── 创建 AnnotationConfigServletWebServerApplicationContext│
│     └── 应用 ApplicationContextInitializer                   │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  4. 刷新容器 (refresh)                                        │
│     └── 扫描 @Component, @Configuration 等注解                │
│     └── 注册 BeanDefinition                                   │
│     └── 实例化 Bean (单例)                                    │
│     └── 初始化 Bean (调用@PostConstruct, InitializingBean)   │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  5. 调用 CommandLineRunner 和 ApplicationRunner              │
│     └── 执行应用启动后的回调逻辑                              │
└─────────────────────────────────────────────────────────────┘
```

### 📁 本项目中的体现

**入口类**: [`src/main/java/com/redmoon2333/Main.java`](../../src/main/java/com/redmoon2333/Main.java)

```java
@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
```

**自动配置类**:
- `SecurityConfig.java` - Spring Security 配置
- `RedisConfig.java` - Redis 配置
- `MybatisPlusConfig.java` - MyBatis-Plus 配置

---

## 1.2 自动配置原理

### ❓ 面试官问：Spring Boot 自动配置是怎么实现的？

### ✅ 参考回答

**核心注解**: `@EnableAutoConfiguration`

**实现原理**:
```
@SpringBootApplication
    └── @EnableAutoConfiguration
            └── @Import(AutoConfigurationImportSelector.class)
                    └── 读取 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
                            └── 加载所有自动配置类
```

**条件注解**:
- `@ConditionalOnClass` - 当类路径下有指定类时生效
- `@ConditionalOnBean` - 当容器中有指定 Bean 时生效
- `@ConditionalOnMissingBean` - 当容器中没有指定 Bean 时生效 (常用)
- `@ConditionalOnProperty` - 当配置文件中指定了属性时生效

### 📁 本项目中的体现

**pom.xml** 中引入了大量 starter:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <!-- 自动配置 Spring MVC, Tomcat, Jackson -->
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
    <!-- 自动配置 RedisTemplate -->
</dependency>
```

**自定义配置** (覆盖自动配置):
```java
// RedisConfig.java - 自定义 RedisTemplate 的序列化方式
@Bean
public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    return template;
}
```

---

## 1.3 配置文件优先级

### ❓ 面试官问：Spring Boot 配置文件的加载优先级是什么？

### ✅ 参考回答

**优先级从高到低** (共 14 级，常用如下):

```
1. 命令行参数 (--server.port=8081)
2. 来自 java:comp/env 的 JNDI 属性
3. 系统属性 (System.getProperties())
4. 操作系统环境变量
5. jar 包外部的 application-{profile}.properties/yml
6. jar 包内部的 application-{profile}.properties/yml
7. jar 包外部的 application.properties/yml
8. jar 包内部的 application.properties/yml
9. @Configuration 类上的 @PropertySource
10. Spring Boot 默认配置
```

### 📁 本项目中的体现

**配置文件结构**:
```
src/main/resources/
├── application.yml              # 主配置 (优先级 8)
└── application-dev.yml          # 开发环境配置 (优先级 8，但 profile 激活)
```

**激活 profile**:
```yaml
# application.yml
spring:
  profiles:
    active: dev  # 激活开发环境配置
```

**实际部署时** (使用命令行参数覆盖):
```bash
java -jar HumanResourceOfficial.jar \
  --spring.profiles.active=prod \
  --server.port=8080 \
  --DB_PASSWORD=secure_password
```

---

## 1.4 Bean 的生命周期

### ❓ 面试官问：请描述 Spring Bean 的生命周期

### ✅ 参考回答

```
┌─────────────────────────────────────────────────────────┐
│  1. 实例化 Bean (Constructor)                            │
│     └── 调用构造函数创建 Bean 实例                          │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│  2. 属性赋值 (Populate)                                  │
│     └── @Autowired 注入依赖                               │
│     └── @Value 注入配置值                                 │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│  3. Aware 接口回调                                       │
│     └── BeanNameAware.setBeanName()                     │
│     └── BeanFactoryAware.setBeanFactory()               │
│     └── ApplicationContextAware.setApplicationContext() │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│  4. BeanPostProcessor 前置处理                           │
│     └── postProcessBeforeInitialization()                │
│     └── AOP 代理在此创建                                  │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│  5. 初始化 (Initialization)                              │
│     └── @PostConstruct 标注的方法                         │
│     └── InitializingBean.afterPropertiesSet()           │
│     └── init-method 配置的方法                            │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│  6. BeanPostProcessor 后置处理                           │
│     └── postProcessAfterInitialization()                 │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│  7. Bean 就绪，可以使用                                 │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│  8. 销毁 (Destruction)                                   │
│     └── @PreDestroy 标注的方法                            │
│     └── DisposableBean.destroy()                        │
│     └── destroy-method 配置的方法                         │
└─────────────────────────────────────────────────────────┘
```

### 📁 本项目中的体现

**@PostConstruct 使用** (`RedisBloomFilterUtil.java` 行 71-82):
```java
@Component
public class RedisBloomFilterUtil {
    
    @PostConstruct  // Bean 初始化后执行
    public void initActivityFilter() {
        logger.info("Redis 布隆过滤器初始化完成");
        preLoadActivities();  // 预加载活动数据到布隆过滤器
    }
}
```

**@PreDestroy 使用**:
```java
@Component
public class SomeComponent {
    
    @PreDestroy  // Bean 销毁前执行
    public void destroy() {
        logger.info("资源释放...");
    }
}
```

---

## 1.5 依赖注入方式

### ❓ 面试官问：Spring 有哪些依赖注入方式？推荐哪种？

### ✅ 参考回答

**3 种主要方式**:

| 方式 | 代码示例 | 优点 | 缺点 | 推荐度 |
|------|---------|------|------|--------|
| **构造器注入** | `public Service(Repository repo) { this.repo = repo; }` | 依赖不可变，易测试，强制依赖 | 依赖多时代码冗长 | ⭐⭐⭐⭐⭐ |
| **Setter 注入** | `@Autowired public void setRepo(Repository r) { this.repo = r; }` | 依赖可变，灵活 | 依赖可能为 null | ⭐⭐⭐ |
| **字段注入** | `@Autowired private Repository repo;` | 代码简洁 | 难测试，违反单一职责 | ⭐⭐ |

**最佳实践**:
- 强制依赖使用构造器注入
- 可选依赖使用 Setter 注入
- 避免使用字段注入 (难测试，隐藏依赖)

### 📁 本项目中的体现

**当前代码** (字段注入):
```java
@Service
public class ActivityService {
    
    @Autowired  // 字段注入，不推荐
    private ActivityMapper activityMapper;
    
    @Autowired
    private ActivityImageMapper activityImageMapper;
}
```

**推荐重构为** (构造器注入):
```java
@Service
@RequiredArgsConstructor  // Lombok 生成构造器
public class ActivityService {
    
    private final ActivityMapper activityMapper;  // final 确保不可变
    private final ActivityImageMapper activityImageMapper;
    
    // Lombok @RequiredArgsConstructor 自动生成构造器
    // public ActivityService(ActivityMapper activityMapper, ActivityImageMapper activityImageMapper) { ... }
}
```

---

## 1.6 @RestController vs @Controller

### ❓ 面试官问：@RestController 和 @Controller 有什么区别？

### ✅ 参考回答

| 特性 | @Controller | @RestController |
|------|-------------|-----------------|
| **返回值** | 视图名称 (如："index") | 对象 (自动转 JSON) |
| **响应类型** | HTML 页面 | RESTful API |
| **组合注解** | - | @Controller + @ResponseBody |
| **使用场景** | SSR 服务端渲染 | 前后端分离 API |

### 📁 本项目中的体现

**本项目全部使用 @RestController** (前后端分离):
```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(...) {
        // 返回对象，自动序列化为 JSON
        return ApiResponse.success("登录成功", loginResult);
    }
}
```

**ApiResponse 统一响应** (`dto/ApiResponse.java`):
```java
@Data
@AllArgsConstructor
public class ApiResponse<T> {
    private int code;        // 0=成功，非 0=错误
    private String message;  // 响应消息
    private T data;          // 响应数据
    private long timestamp;  // 时间戳
    
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(0, message, data, System.currentTimeMillis());
    }
    
    public static <T> ApiResponse<T> error(String message, int code) {
        return new ApiResponse<>(code, message, null, System.currentTimeMillis());
    }
}
```

---

## 1.7 统一异常处理

### ❓ 面试官问：项目中如何处理全局异常？

### ✅ 参考回答

**使用 @RestControllerAdvice 实现全局异常处理**:

```
请求 → Controller → 抛出异常
              ↓
    @RestControllerAdvice
              ↓
    @ExceptionHandler
              ↓
    统一错误响应
```

### 📁 本项目中的体现

**GlobalExceptionHandler.java** ([`src/main/java/com/redmoon2333/exception/GlobalExceptionHandler.java`](../../src/main/java/com/redmoon2333/exception/GlobalExceptionHandler.java)):

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    // 处理业务异常
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        return ApiResponse.error(
            e.getErrorCode().getMessage(), 
            e.getErrorCode().getCode()
        );
    }
    
    // 处理参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return ApiResponse.error(errorMsg, 400);
    }
    
    // 处理未知异常
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResponse.error("系统内部错误", 500);
    }
}
```

**错误码定义** (`ErrorCode.java`):
```java
public enum ErrorCode {
    // 用户相关 1000-1999
    USER_NOT_FOUND(1001, "用户不存在"),
    USERNAME_EXISTS(1002, "用户名已存在"),
    
    // Token 相关 2000-2999
    INVALID_TOKEN(2001, "令牌无效或已过期"),
    TOKEN_EXPIRED(2004, "令牌已过期"),
    
    // 系统错误 5000-5999
    SYSTEM_ERROR(5000, "系统内部错误");
}
```

---

## 1.8 拦截器 vs 过滤器

### ❓ 面试官问：拦截器和过滤器有什么区别？

### ✅ 参考回答

| 特性 | Filter (过滤器) | Interceptor (拦截器) |
|------|----------------|---------------------|
| **所属规范** | Servlet 规范 | Spring 框架 |
| **执行时机** | 请求进入最早阶段 | DispatcherServlet 之后 |
| **可访问内容** | Request/Response | Request/Response/Handler |
| **依赖注入** | 不支持 | 支持 |
| **使用场景** | 编码过滤、安全头 | 权限校验、日志记录 |

**执行顺序**:
```
Request → Filter → Interceptor → Controller → Interceptor → Filter → Response
```

### 📁 本项目中的体现

**过滤器** (`JwtAuthenticationFilter.java` 行 33-117):
```java
@Component
@Order(1)  // 优先级最高，最先执行
public class JwtAuthenticationFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
        HttpServletRequest request = (HttpServletRequest) req;
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                // 验证通过，继续执行
                chain.doFilter(req, res);
            }
        }
    }
}
```

**拦截器** (项目中使用 Filter 替代，但原理相同):
```java
// 如需使用拦截器，配置如下:
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Autowired
    private AuthInterceptor authInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")  // 拦截路径
                .excludePathPatterns("/api/auth/login");  // 排除登录
    }
}
```

---

## 1.9 Starter 机制

### ❓ 面试官问：Spring Boot Starter 是什么？如何自定义 Starter？

### ✅ 参考回答

**Starter 是什么**:
- 一组预配置的依赖描述符
- 简化 Maven/Gradle 配置
- 自动引入传递依赖

**如何自定义 Starter**:

```
my-spring-boot-starter/
├── my-spring-boot-starter/          # 空项目，只含 pom.xml (依赖管理)
└── my-spring-boot-starter-autoconfigure/  # 自动配置模块
    ├── src/main/java/
    │   └── com/example/
    │       ├── MyService.java           # 业务类
    │       └── MyProperties.java        # 配置属性
    └── src/main/resources/
        └── META-INF/
            └── spring/
                └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
                    # 内容：com.example.MyAutoConfiguration
```

**自动配置类**:
```java
@Configuration
@ConditionalOnClass(MyService.class)
@EnableConfigurationProperties(MyProperties.class)
public class MyAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public MyService myService(MyProperties properties) {
        return new MyService(properties.getPrefix());
    }
}
```

### 📁 本项目中使用的 Starter

**官方 Starter**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

**第三方 Starter**:
```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>3.5.12</version>
</dependency>
```

---

## 1.10 Profiles 环境隔离

### ❓ 面试官问：如何在不同环境使用不同配置？

### ✅ 参考回答

**配置方式**:

```yaml
# application.yml (主配置)
spring:
  profiles:
    active: dev  # 激活开发环境

---
# application-dev.yml (开发环境)
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/dev_db
  redis:
    host: localhost

---
# application-prod.yml (生产环境)
spring:
  datasource:
    url: jdbc:mysql://prod-db:3306/prod_db
  redis:
    host: prod-redis
```

**激活方式**:
1. 配置文件：`spring.profiles.active=dev`
2. 命令行：`--spring.profiles.active=prod`
3. 环境变量：`SPRING_PROFILES_ACTIVE=prod`

### 📁 本项目中的体现

**application-dev.yml** ([`src/main/resources/application-dev.yml`](../../src/main/resources/application-dev.yml)):
```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: ${DB_URL:jdbc:mysql://localhost:3306/hrofficial}
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:}  # 默认空密码 (开发环境)
  
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}

aliyun:
  oss:
    endpoint: oss-cn-beijing.aliyuncs.com
    # ... 开发环境 OSS 配置
```

**生产环境部署**:
```bash
# 使用环境变量覆盖配置
export DB_URL=jdbc:mysql://prod-db:3306/hrofficial
export DB_USERNAME=prod_user
export DB_PASSWORD=secure_password_123
export REDIS_HOST=prod-redis

java -jar HumanResourceOfficial.jar --spring.profiles.active=prod
```

---

## 📝 本篇小结

### 核心知识点回顾

1. ✅ Spring Boot 启动流程 (5 个阶段)
2. ✅ 自动配置原理 (`@EnableAutoConfiguration`)
3. ✅ 配置文件优先级 (命令行 > 环境变量 > jar 包内)
4. ✅ Bean 生命周期 (实例化→属性赋值→初始化→销毁)
5. ✅ 依赖注入方式 (推荐构造器注入)
6. ✅ @RestController vs @Controller
7. ✅ 统一异常处理 (`@RestControllerAdvice`)
8. ✅ 拦截器 vs 过滤器 (执行时机、可访问内容)
9. ✅ Starter 机制 (依赖描述符 + 自动配置)
10. ✅ Profiles 环境隔离

### 面试准备建议

- **初级开发**: 重点掌握 1.4、1.5、1.6、1.7
- **中级开发**: 重点掌握 1.1、1.2、1.8、1.9
- **高级开发**: 深入理解 1.2 自动配置源码、1.9 自定义 Starter

---

> 💡 **下一步**: 继续学习 [模块 2: 注解与 AOP 切面篇](./02-注解与 AOP 切面篇.md)
