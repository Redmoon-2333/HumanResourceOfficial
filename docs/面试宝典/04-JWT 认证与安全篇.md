# 模块 4: JWT 认证与安全篇

> 📌 **本篇重点**: JWT 结构与原理、令牌管理、RBAC 权限、Spring Security、安全攻防
> 
> 🎯 **面试频率**: ⭐⭐⭐⭐⭐ (中高级开发必问，安全是生产系统基石)
> 
> 💡 **项目亮点**: 本项目实现了完整的 JWT 认证体系、令牌黑名单、强制下线、密码加密

---

## 4.1 JWT 结构与原理

### ❓ 面试官问：JWT 是什么？它的结构是怎样的？

### ✅ 参考回答

**JWT (JSON Web Token)** 是一种开放的行业标准 (RFC 7519)，用于在各方之间安全地传输信息作为 JSON 对象。

**核心特点**:
- **无状态**: 服务端不保存 session，适合分布式系统
- **自包含**: 载荷中包含所有必要信息
- **可验证**: 使用签名确保数据未被篡改

**结构 (3 部分)**:

```
┌─────────────────────────────────────────────────────────────┐
│                    JWT = Header.Payload.Signature            │
├─────────────────────────────────────────────────────────────┤
│  1. Header (头部)                                           │
│     {                                                       │
│       "alg": "HS256",    // 签名算法                        │
│       "typ": "JWT"       // 令牌类型                        │
│     }                                                       │
│     → Base64Url 编码                                         │
├─────────────────────────────────────────────────────────────┤
│  2. Payload (载荷/负载)                                      │
│     {                                                       │
│       "sub": "1234567890",          // 主题 (用户 ID)        │
│       "username": "zhangsan",       // 用户名 (自定义声明)   │
│       "iat": 1516239022,            // 签发时间              │
│       "exp": 1516242622             // 过期时间              │
│     }                                                       │
│     → Base64Url 编码                                         │
├─────────────────────────────────────────────────────────────┤
│  3. Signature (签名)                                        │
│     HMACSHA256(                                             │
│       base64UrlEncode(header) + "." + base64UrlEncode(payload), │
│       secret_key                                            │
│     )                                                       │
│     → 防止篡改                                               │
└─────────────────────────────────────────────────────────────┘

最终格式：xxxxx.yyyyy.zzzzz (3 部分用.连接)
```

**优缺点**:

| 优点 | 缺点 |
|------|------|
| 无状态，易于扩展 | 无法主动失效 (需黑名单) |
| 适合移动端 | 载荷过大影响性能 |
| 跨域支持好 | 签名泄露后风险大 |
| 减少数据库查询 | 需要 HTTPS 传输 |

### 📁 本项目中的体现

**JwtUtil.java** ([`src/main/java/com/redmoon2333/util/JwtUtil.java`](../../src/main/java/com/redmoon2333/util/JwtUtil.java)):

```java
@Component
public class JwtUtil {
    
    @Value("${jwt.secret:YourSecretKeyForJWTSignatureMustBeLongEnough}")
    private String secretKey;
    
    @Value("${jwt.expiration:86400000}")  // 默认 24 小时
    private long expiration;
    
    /**
     * 生成 JWT 令牌
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .setSubject(String.valueOf(user.getUserId()))  // 主题：用户 ID
                .claim("username", user.getUsername())          // 自定义声明：用户名
                .claim("role", user.getRole())                  // 自定义声明：角色
                .setIssuedAt(now)                               // 签发时间
                .setExpiration(expirationDate)                  // 过期时间
                .signWith(SignatureAlgorithm.HS256, secretKey)  // 签名算法
                .compact();
    }
    
    /**
     * 从令牌中获取用户 ID
     */
    public Integer getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody();
        
        return Integer.parseInt(claims.getSubject());
    }
    
    /**
     * 验证令牌是否有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(secretKey)
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

---

## 4.2 JWT 生成与验证

### ❓ 面试官问：如何生成和验证 JWT 令牌？

### ✅ 参考回答

**生成 JWT 的步骤**:

```
1. 准备 Header (指定算法和类型)
       ↓
2. 准备 Payload (添加声明：sub, username, exp 等)
       ↓
3. 使用 Base64Url 分别编码 Header 和 Payload
       ↓
4. 拼接 encodedHeader.encodedPayload
       ↓
5. 使用指定算法 (如 HS256) + 密钥 对拼接结果签名
       ↓
6. 最终格式：encodedHeader.encodedPayload.signature
```

**验证 JWT 的步骤**:

```
1. 检查令牌格式是否正确 (3 部分，用.分隔)
       ↓
2. 使用相同密钥和算法重新计算签名
       ↓
3. 比较计算出的签名与令牌中的签名是否一致
       ↓
4. 检查是否过期 (exp 声明)
       ↓
5. 检查签发时间是否在未来 (iat 声明)
       ↓
6. (可选) 检查是否在黑名单中
```

### 📁 本项目中的体现

**令牌生成** (登录成功时):

```java
@PostMapping("/login")
public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest request) {
    // 1. 验证用户名密码
    User user = userService.authenticate(request.getUsername(), request.getPassword());
    
    // 2. 生成 JWT 令牌
    String token = jwtUtil.generateToken(user);
    
    // 3. 构建响应
    Map<String, Object> loginResult = new HashMap<>();
    loginResult.put("token", token);
    loginResult.put("user", user);
    
    return ApiResponse.success("登录成功", loginResult);
}
```

**令牌验证** (过滤器中):

```java
// JwtAuthenticationFilter.java
@Override
public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
    HttpServletRequest request = (HttpServletRequest) req;
    String authHeader = request.getHeader("Authorization");
    
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String token = authHeader.substring(7);
        
        // 验证令牌
        if (jwtUtil.validateToken(token)) {
            // 验证通过，设置用户上下文
            Integer userId = jwtUtil.getUserIdFromToken(token);
            // ... 继续处理请求
            chain.doFilter(req, res);
        }
    }
}
```

---

## 4.3 令牌黑名单机制

### ❓ 面试官问：JWT 无法主动失效，如何解决？

### ✅ 参考回答

**问题**: JWT 是无状态的，一旦签发，在过期前无法主动失效。

**解决方案**:

| 方案 | 实现 | 优点 | 缺点 |
|------|------|------|------|
| **Redis 黑名单** | 将 token 或 jti 存入 Redis | 简单直接，支持主动失效 | 增加 Redis 依赖 |
| **令牌版本号** | 用户表增加 tokenVersion，JWT 携带版本号 | 无需黑名单 | 需查数据库 |
| **短令牌 + 刷新** | Access Token 短效 + Refresh Token 长效 | 安全性高 | 架构复杂 |

**本项目采用 Redis 黑名单方案**:

```
┌─────────────────────────────────────────────────────────────┐
│                    令牌黑名单机制                            │
├─────────────────────────────────────────────────────────────┤
│  1. 用户登出 → 将 token 加入 Redis 黑名单                     │
│  2. 设置过期时间 = JWT 剩余有效期                            │
│  3. 每次请求验证 token 时 → 先检查是否在黑名单               │
│  4. 黑名单中的 token → 直接拒绝                              │
└─────────────────────────────────────────────────────────────┘
```

### 📁 本项目中的体现

**JwtRedisService.java** (令牌黑名单管理):

```java
@Service
public class JwtRedisService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    
    /**
     * 将令牌加入黑名单
     */
    public void addToBlacklist(String token, long expirationTime) {
        // 计算剩余有效期
        long ttl = expirationTime - System.currentTimeMillis();
        if (ttl > 0) {
            redisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + token,
                "blacklisted",
                ttl,
                TimeUnit.MILLISECONDS
            );
        }
    }
    
    /**
     * 检查令牌是否在黑名单中
     */
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(
            redisTemplate.hasKey(BLACKLIST_PREFIX + token)
        );
    }
}
```

---

## 4.4 强制下线实现

### ❓ 面试官问：如何实现管理员强制用户下线？

### ✅ 参考回答

**实现思路**:

```
方案 1: 令牌黑名单 (推荐)
  1. 获取用户的所有有效令牌
  2. 将令牌全部加入黑名单
  3. 用户下次请求时，令牌验证失败

方案 2: 令牌版本号
  1. 用户表增加 tokenVersion 字段
  2. 强制下线时 +1 版本号
  3. 旧令牌的版本号失效
  4. 验证 token 时比对版本号
```

**强制下线流程**:

```
管理员发起请求 → 后端接口
       ↓
根据 userId 查找用户
       ↓
将该用户的所有 token 加入 Redis 黑名单
       ↓
返回操作结果
       ↓
用户下次请求 → token 验证失败 → 401 未授权
```

### 📁 本项目中的体现

**AuthController.java** (强制下线接口):

```java
/**
 * 强制用户下线
 */
@PreAuthorize("hasRole('ADMIN')")  // 仅管理员可操作
@PostMapping("/force-logout")
public ApiResponse<Void> forceLogout(@RequestParam Integer userId) {
    // 1. 获取用户的所有活跃令牌 (可从 Redis 或数据库查询)
    List<String> userTokens = jwtRedisService.getUserTokens(userId);
    
    // 2. 将所有令牌加入黑名单
    for (String token : userTokens) {
        try {
            Claims claims = jwtUtil.getClaimsFromToken(token);
            Date expiration = claims.getExpiration();
            jwtRedisService.addToBlacklist(token, expiration.getTime());
        } catch (Exception e) {
            logger.warn("忽略无效的 token: {}", token);
        }
    }
    
    return ApiResponse.success("用户已强制下线", null);
}
```

---

## 4.5 RBAC 权限模型

### ❓ 面试官问：什么是 RBAC？项目中如何实现？

### ✅ 参考回答

**RBAC (Role-Based Access Control)**: 基于角色的访问控制。

**核心概念**:
```
用户 (User) → 拥有 → 角色 (Role) → 拥有 → 权限 (Permission)
```

**本项目简化实现** (用户直接关联角色):

```
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│    User     │      │    Role     │      │  Permission │
├─────────────┤      ├─────────────┤      ├─────────────┤
│ userId      │──→   │ roleId      │──→   │ permissionId│
│ username    │      │ roleName    │      │ permission  │
│ password    │      │ description │      │ name        │
│ role (FK)   │      └─────────────┘      └─────────────┘
└─────────────┘
```

**权限粒度**:
- **粗粒度**: 角色级别 (ADMIN/USER/GUEST)
- **细粒度**: 权限级别 (user:create, user:delete, activity:view)

### 📁 本项目中的体现

**User 实体** (简化版 RBAC):

```java
@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Integer userId;
    
    private String username;
    private String password;  // BCrypt 加密存储
    
    private String role;  // 角色：ADMIN, USER, GUEST
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
```

**权限注解使用**:

```java
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")  // 类级别权限控制
public class AdminController {
    
    @PostMapping("/user/ban")
    @PreAuthorize("hasAuthority('user:ban')")  // 方法级别细粒度控制
    public ApiResponse<Void> banUser(@RequestParam Integer userId) {
        // 封禁用户逻辑
    }
}
```

---

## 4.6 Spring Security 配置

### ❓ 面试官问：Spring Security 如何配置？核心组件有哪些？

### ✅ 参考回答

**核心组件**:

```
┌─────────────────────────────────────────────────────────────┐
│                 Spring Security 过滤链                       │
├─────────────────────────────────────────────────────────────┤
│  1. SecurityContextPersistenceFilter                         │
│     └── 从 Session 恢复 SecurityContext                       │
│  2. UsernamePasswordAuthenticationFilter                     │
│     └── 处理登录表单提交                                     │
│  3. BasicAuthenticationFilter                               │
│     └── 处理 HTTP Basic 认证                                 │
│  4. ExceptionTranslationFilter                              │
│     └── 捕获异常，决定返回 401 或 403                          │
│  5. FilterSecurityInterceptor                               │
│     └── 访问控制决策                                         │
└─────────────────────────────────────────────────────────────┘
```

**配置要点**:
- 哪些接口需要认证
- 哪些接口公开访问
- 使用什么认证方式 (JWT/Session)
- 跨域配置
- 异常处理

### 📁 本项目中的体现

**SecurityConfig.java** ([`src/main/java/com/redmoon2333/config/SecurityConfig.java`](../../src/main/java/com/redmoon2333/config/SecurityConfig.java)):

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // 启用@PreAuthorize 注解
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF (前后端分离 + JWT 无需 CSRF)
            .csrf(csrf -> csrf.disable())
            
            // 跨域配置
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 会话管理 (无状态)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 授权规则
            .authorizeHttpRequests(auth -> auth
                // 公开接口
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                // 管理员接口
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // 其他接口需要认证
                .anyRequest().authenticated()
            )
            
            // 添加 JWT 过滤器
            .addFilterBefore(jwtAuthenticationFilter, 
                UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

---

## 4.7 CORS 跨域配置

### ❓ 面试官问：什么是 CORS？如何配置？

### ✅ 参考回答

**CORS (Cross-Origin Resource Sharing)**: 跨域资源共享，用于解决浏览器的同源策略限制。

**同源策略**: 协议 + 域名 + 端口 三者必须相同，否则为跨域。

**预检请求 (Preflight)**:
```
浏览器发送 OPTIONS 请求 (预检)
       ↓
服务端响应 CORS 头
       ↓
浏览器发送实际请求 (GET/POST)
```

**响应头含义**:

| Header | 含义 |
|--------|------|
| Access-Control-Allow-Origin | 允许的来源 |
| Access-Control-Allow-Methods | 允许的方法 |
| Access-Control-Allow-Headers | 允许的请求头 |
| Access-Control-Allow-Credentials | 是否允许携带凭证 |
| Access-Control-Max-Age | 预检结果缓存时间 |

### 📁 本项目中的体现

**SecurityConfig.java** (CORS 配置):

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    
    // 允许的源
    config.setAllowedOrigins(Arrays.asList(
        "http://localhost:3000",
        "https://yourdomain.com"
    ));
    
    // 允许的方法
    config.setAllowedMethods(Arrays.asList(
        "GET", "POST", "PUT", "DELETE", "OPTIONS"
    ));
    
    // 允许的请求头
    config.setAllowedHeaders(Arrays.asList(
        "Authorization",
        "Content-Type",
        "X-Requested-With"
    ));
    
    // 暴露的响应头
    config.setExposedHeaders(Arrays.asList(
        "X-RateLimit-Remaining",
        "X-RateLimit-Reset"
    ));
    
    // 允许携带凭证 (Cookie/Authorization)
    config.setAllowCredentials(true);
    
    // 预检结果缓存 3600 秒
    config.setMaxAge(3600L);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

**WebConfig.java** (全局 CORS 配置):

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")  // 生产环境应指定具体域名
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

---

## 4.8 SQL 注入防护

### ❓ 面试官问：什么是 SQL 注入？如何防护？

### ✅ 参考回答

**SQL 注入原理**:

```
正常输入:
  用户名：zhangsan
  SQL: SELECT * FROM user WHERE username = 'zhangsan'

恶意输入:
  用户名：zhangsan' OR '1'='1
  SQL: SELECT * FROM user WHERE username = 'zhangsan' OR '1'='1'
  → 绕过密码验证，登录任意账号

更危险:
  用户名：zhangsan'; DROP TABLE user; --
  SQL: SELECT * FROM user WHERE username = 'zhangsan'; DROP TABLE user; --'
  → 删除整张表
```

**防护方案**:

| 方案 | 实现 | 效果 |
|------|------|------|
| **参数化查询** | PreparedStatement/MyBatis #{} | ⭐⭐⭐⭐⭐ (推荐) |
| **ORM 框架** | MyBatis-Plus/JPA | ⭐⭐⭐⭐⭐ |
| **输入校验** | 白名单验证 | ⭐⭐⭐ |
| **特殊字符转义** | 替换 ' " \ 等 | ⭐⭐ (不推荐单独使用) |

### 📁 本项目中的体现

**正确使用 MyBatis #{}** (参数化查询):

```java
// UserMapper.java
@Select("SELECT * FROM user WHERE username = #{username}")
User findByUsername(String username);

// ActivityMapper.java
@Select("SELECT * FROM activity WHERE activity_name LIKE CONCAT('%', #{keyword}, '%')")
List<Activity> searchByKeyword(String keyword);
```

**❌ 错误示例** (字符串拼接，禁止使用):

```java
// 危险！不要这样做!
@Select("SELECT * FROM user WHERE username = '${username}'")
User findByUsernameUnsafe(String username);
```

**动态 SQL 也需参数化**:

```xml
<!-- 正确写法 -->
<select id="selectByCondition" resultType="Activity">
    SELECT * FROM activity
    <where>
        <if test="keyword != null">
            AND activity_name LIKE CONCAT('%', #{keyword}, '%')
        </if>
        <if test="status != null">
            AND status = #{status}
        </if>
    </where>
</select>
```

---

## 4.9 XSS 攻击防护

### ❓ 面试官问：什么是 XSS？如何防护？

### ✅ 参考回答

**XSS (Cross-Site Scripting)**: 跨站脚本攻击，攻击者在网页中注入恶意脚本。

**攻击类型**:

| 类型 | 描述 | 示例 |
|------|------|------|
| **存储型 XSS** | 恶意脚本存储到数据库 | 评论/留言中注入 `<script>` |
| **反射型 XSS** | 恶意脚本通过 URL 参数反射 | 搜索关键词注入 |
| **DOM 型 XSS** | 客户端 JS 操作 DOM 导致 | `innerHTML` 直接渲染用户输入 |

**防护方案**:

```
1. 输入过滤
   └── 白名单验证 (只允许预期的字符)
   
2. 输出编码
   └── HTML 实体编码：< → &lt;  > → &gt;  " → &quot;
   
3. HTTP 头防护
   └── X-XSS-Protection: 1
   └── Content-Security-Policy (CSP)
   
4. Cookie 防护
   └── HttpOnly (禁止 JS 访问)
   └── Secure (仅 HTTPS 传输)
```

### 📁 本项目中的体现

**全局 XSS 过滤器**:

```java
@Component
@Order(2)
public class XssFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        
        // 添加安全响应头
        response.setHeader("X-XSS-Protection", "1; mode=block");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Content-Security-Policy", "default-src 'self'");
        
        // 包装请求，对输入进行 XSS 过滤
        XssHttpServletRequestWrapper wrappedRequest = 
            new XssHttpServletRequestWrapper(request);
        
        chain.doFilter(wrappedRequest, res);
    }
}
```

**输入过滤工具**:

```java
public class XssUtil {
    
    /**
     * HTML 实体编码
     */
    public static String escapeHtml(String input) {
        if (input == null) return null;
        
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;");
    }
    
    /**
     * 白名单过滤 (只允许字母、数字、下划线)
     */
    public static String filterAlphanumeric(String input) {
        if (input == null) return null;
        return input.replaceAll("[^a-zA-Z0-9_]", "");
    }
}
```

**前端防护** (React/Vue 自动转义):

```javascript
// React 自动转义，危险操作会警告
<p>{userInput}</p>  // 安全，自动转义

// 危险！不要这样做
<div dangerouslySetInnerHTML={{__html: userInput}} />  // 除非完全信任输入
```

---

## 4.10 密码加密存储

### ❓ 面试官问：密码应该如何加密存储？

### ✅ 参考回答

**密码存储原则**:

```
❌ 禁止明文存储
❌ 禁止使用 MD5/SHA1 (可彩虹表破解)
❌ 禁止简单加盐 (salt 硬编码)
✅ 使用专用密码哈希函数
```

**推荐算法**:

| 算法 | 特点 | 推荐度 |
|------|------|--------|
| **bcrypt** | 自适应成本因子，内置盐 | ⭐⭐⭐⭐⭐ (本项目使用) |
| **Argon2** | 内存困难函数，抗 GPU | ⭐⭐⭐⭐⭐ |
| **scrypt** | 内存困难函数 | ⭐⭐⭐⭐ |
| **PBKDF2** | 迭代次数可调 | ⭐⭐⭐ |

**bcrypt 原理**:

```
password + 随机 salt → bcrypt 哈希 (60 字符)
       ↓
cost factor = 12 (迭代 2^12 次)
       ↓
即使密码相同，每次生成的哈希也不同 (因 salt 不同)
```

### 📁 本项目中的体现

**UserService.java** (密码加密):

```java
@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    /**
     * 密码加密
     */
    public String encodePassword(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));
    }
    
    /**
     * 密码校验
     */
    public boolean checkPassword(String rawPassword, String hashedPassword) {
        try {
            return BCrypt.checkpw(rawPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            // 处理格式错误的哈希
            return false;
        }
    }
    
    /**
     * 用户注册
     */
    @Transactional
    public User register(String username, String password) {
        // 检查用户名是否存在
        if (userMapper.findByUsername(username) != null) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        
        // 密码加密
        String hashedPassword = encodePassword(password);
        
        // 创建用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(hashedPassword);
        user.setRole("USER");
        user.setCreateTime(LocalDateTime.now());
        
        userMapper.insert(user);
        return user;
    }
    
    /**
     * 用户认证
     */
    public User authenticate(String username, String password) {
        User user = userMapper.findByUsername(username);
        
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        if (!checkPassword(password, user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        
        return user;
    }
}
```

**依赖引入** (pom.xml):

```xml
<dependency>
    <groupId>org.mindrot</groupId>
    <artifactId>jbcrypt</artifactId>
    <version>0.4</version>
</dependency>
```

---

## 📝 本篇小结

### 核心知识点回顾

1. ✅ JWT 结构与原理 (Header.Payload.Signature)
2. ✅ JWT 生成与验证流程
3. ✅ 令牌黑名单机制 (Redis 存储)
4. ✅ 强制下线实现 (批量加入黑名单)
5. ✅ RBAC 权限模型 (用户→角色→权限)
6. ✅ Spring Security 配置 (过滤链、授权规则)
7. ✅ CORS 跨域配置 (预检请求、响应头)
8. ✅ SQL 注入防护 (参数化查询)
9. ✅ XSS 攻击防护 (输入过滤、输出编码)
10. ✅ 密码加密存储 (bcrypt 算法)

### 面试准备建议

**初级开发**: 重点掌握 4.1、4.2、4.5、4.10
**中级开发**: 重点掌握 4.3、4.4、4.6、4.7
**高级开发**: 深入理解 4.8 SQL 注入原理、4.9 XSS 防护、4.6 Security 过滤链

---

> 💡 **下一步**: 继续学习 [模块 5: Spring AI 与 RAG 篇](./05-SpringAI 与 RAG 篇.md)
