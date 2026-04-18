# 模块 3: Redis 缓存与分布式锁篇

> 📌 **本篇重点**: 缓存应用场景、缓存穿透/击穿/雪崩、分布式锁、限流算法
> 
> 🎯 **面试频率**: ⭐⭐⭐⭐⭐ (中高级开发必问，Redis 是面试重灾区)
> 
> 💡 **项目亮点**: 本项目实现了完整的缓存体系 (@Cacheable)、布隆过滤器、Redisson 分布式锁、滑动窗口限流

---

## 3.1 Redis 应用场景

### ❓ 面试官问：Redis 在项目中有哪些应用场景？

### ✅ 参考回答

**本项目中的 5 大应用场景**:

| 场景 | 实现方式 | 优势 | 本项目位置 |
|------|---------|------|-----------|
| **缓存** | `@Cacheable` | 减轻 DB 压力，提升查询速度 | `ActivityService.getAllActivities()` |
| **分布式锁** | Redisson `RLock` | 防止并发数据不一致 | `ActivityService.createActivity()` |
| **限流** | Redis + Lua 滑动窗口 | 防止接口滥用 | `AuthController.login()` |
| **布隆过滤器** | `SETBIT`/`GETBIT` | 快速判断 ID 是否存在 | `RedisBloomFilterUtil` |
| **Token 存储** | `RedisTemplate` | JWT 黑名单、强制下线 | `JwtRedisService` |

**性能提升对比**:
```
查询活动列表 (1000 条数据):
- 直接查 DB:  ~50ms
- Redis 缓存：  ~2ms  (提升 25 倍)

限流检查:
- 数据库计数：  ~10ms (需要加锁)
- Redis+Lua:    ~1ms  (原子操作)
```

### 📁 本项目中的体现

**CacheConfig.java** ([`src/main/java/com/redmoon2333/config/CacheConfig.java`](../../src/main/java/com/redmoon2333/config/CacheConfig.java)):
```java
@Configuration
@EnableCaching  // 启用缓存注解
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))  // 默认 30 分钟过期
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();  // 禁止缓存 null 值
        
        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build();
    }
}
```

---

## 3.2 @Cacheable 原理

### ❓ 面试官问：@Cacheable 注解的工作原理是什么？

### ✅ 参考回答

**执行流程**:

```
1. 方法调用
       ↓
2. CacheInterceptor 拦截
       ↓
3. 生成 Cache Key (默认使用 SimpleKeyGenerator)
       ↓
4. 查询缓存
       ↓
   ┌───────┴───────┐
   │               │
 命中           未命中
   │               │
   ↓               ↓
返回缓存值    执行目标方法
                   │
                   ↓
              将结果写入缓存
                   │
                   ↓
              返回结果
```

**关键参数**:
```java
@Cacheable(
    value = "activity",           // 缓存名称 (对应 Redis key 前缀)
    key = "#activityId",          // 缓存键 (SpEL 表达式)
    unless = "#result == null",   // 条件：结果不为 null 时才缓存
    condition = "#activityId > 0" // 条件：满足时才执行缓存
)
```

### 📁 本项目中的体现

**ActivityService.java** (行 51-62):
```java
@Cacheable(value = "activity:list", key = "'all'")
public List<Activity> getAllActivities() {
    logger.info("获取所有活动列表");
    try {
        List<Activity> activities = activityMapper.selectList(null);
        logger.info("成功获取活动列表，共{}条记录", activities.size());
        return activities;
    } catch (Exception e) {
        logger.error("获取活动列表时发生异常:{}", e.getMessage(), e);
        throw new BusinessException(ErrorCode.ACTIVITY_LIST_FAILED);
    }
}
```

**Redis 中的 Key 结构**:
```
activity:list::all    →  JSON 序列化的活动列表
activity::1           →  JSON 序列化的活动详情 (ID=1)
user::zhangsan        →  JSON 序列化的用户信息
```

**缓存清除** (`@CacheEvict`):
```java
// 创建活动后清除列表缓存
@CacheEvict(value = "activity:list", key = "'all'")
public Activity createActivity(Activity activity) { ... }

// 更新活动后清除单个缓存和列表缓存
@CacheEvict(value = {"activity", "activity:list"}, key = "#activityId")
public Activity updateActivity(Integer activityId, Activity activity) { ... }
```

---

## 3.3 缓存穿透/击穿/雪崩

### ❓ 面试官问：什么是缓存穿透、击穿、雪崩？如何解决？

### ✅ 参考回答

**三者对比**:

| 问题 | 原因 | 影响 | 解决方案 | 本项目实现 |
|------|------|------|----------|-----------|
| **穿透** | 查询不存在的数据 | 请求直达 DB | 布隆过滤器、缓存空值 | `RedisBloomFilterUtil` |
| **击穿** | 热点 key 过期瞬间大量请求 | DB 瞬时压力大 | 互斥锁、逻辑过期 | `RedisMutexUtil` |
| **雪崩** | 大量 key 同时过期 | 系统崩溃 | 随机过期时间、服务降级 | TTL 随机化 |

**详细图解**:

```
┌─────────────────────────────────────────────────────────────┐
│                     缓存穿透                                │
│  用户 → 查询 ID=-1 → 缓存无 → 查 DB 无 → 缓存不写 → 每次都查 DB  │
│  解决：布隆过滤器快速返回"一定不存在"                          │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                     缓存击穿                                │
│  热点 key 过期 → 1000 个请求同时到达 → DB 压力大                │
│  解决：互斥锁 (只让一个请求查 DB，其他等待)                      │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                     缓存雪崩                                │
│  10000 个 key 同时过期 → 所有请求直达 DB → 系统崩溃              │
│  解决：过期时间加随机值 (如 30 分钟 + 随机 1-5 分钟)               │
└─────────────────────────────────────────────────────────────┘
```

### 📁 本项目中的体现

**布隆过滤器防穿透** (`RedisBloomFilterUtil.java` 行 127-151):
```java
/**
 * 判断 ID 可能存在于布隆过滤器中
 * Warning: 存在约 0.1% 的误判率
 * 返回 true 表示 ID 可能存在，仍需数据库验证
 * 返回 false 表示 ID 一定不存在，可直接返回
 */
public boolean mightExist(String type, Integer id) {
    if (stringRedisTemplate == null) {
        logger.warn("StringRedisTemplate 未注入，跳过布隆过滤器检查");
        return true;  // 保守策略：未初始化时返回 true，交由数据库判断
    }
    
    try {
        String key = getBloomFilterKey(type);
        long bitPosition = getBitPosition(id);
        
        Long result = stringRedisTemplate.execute((RedisCallback<Long>) connection -> {
            byte[] keyBytes = key.getBytes();
            Boolean bitValue = connection.getBit(keyBytes, bitPosition);
            return bitValue ? 1L : 0L;
        });
        
        return result != null && result == 1;  // 位=1 表示可能存在
    } catch (Exception e) {
        logger.error("布隆过滤器查询失败：type={}, id={}", type, id, e);
        return true;  // 失败时返回 true，交由数据库判断
    }
}
```

**互斥锁防击穿** (`RedisMutexUtil.java` 行 75-97):
```java
/**
 * 尝试获取分布式锁
 * 使用 SETNX 命令原子性地设置键值对
 */
public boolean tryLock(String key, String value, long timeout, TimeUnit unit) {
    String lockKey = LOCK_PREFIX + key;
    
    try {
        // SETNX + EXPIRE 原子操作
        Boolean result = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, value, timeout, unit);
        
        if (Boolean.TRUE.equals(result)) {
            logger.debug("获取锁成功：key={}, value={}", lockKey, value);
            return true;
        } else {
            logger.debug("获取锁失败：key={}（已被其他进程占用）", lockKey);
            return false;
        }
    } catch (Exception e) {
        logger.error("获取锁异常：key={}", key, e);
        return false;
    }
}
```

**使用示例** (伪代码，实际项目推荐用 Redisson):
```java
public Activity getActivityById(Integer activityId) {
    // 1. 布隆过滤器检查 (防穿透)
    if (!bloomFilter.mightExist("activity", activityId)) {
        return null;  // 一定不存在，直接返回
    }
    
    // 2. 查询缓存
    Activity cached = redisTemplate.opsForValue().get("activity:" + activityId);
    if (cached != null) {
        return cached;
    }
    
    // 3. 互斥锁查 DB(防击穿)
    String lockKey = "lock:activity:" + activityId;
    String lockValue = UUID.randomUUID().toString();
    
    if (redisMutex.tryLock(lockKey, lockValue, 5, TimeUnit.SECONDS)) {
        try {
            // 双重检查缓存 (其他线程可能已写入)
            cached = redisTemplate.opsForValue().get("activity:" + activityId);
            if (cached != null) {
                return cached;
            }
            
            // 查 DB
            Activity activity = activityMapper.selectById(activityId);
            if (activity != null) {
                redisTemplate.opsForValue().set("activity:" + activityId, activity, 30, TimeUnit.MINUTES);
            }
            return activity;
        } finally {
            redisMutex.unlock(lockKey, lockValue);
        }
    } else {
        // 等待锁释放后重试
        Thread.sleep(50);
        return getActivityById(activityId);
    }
}
```

---

## 3.4 布隆过滤器原理

### ❓ 面试官问：布隆过滤器的原理是什么？优缺点？

### ✅ 参考回答

**原理图解**:

```
        元素 A ("activity:1")
             ↓
    ┌────────┴────────┐
    │   哈希函数 1     │ → 位置 3  → 设为 1
    │   哈希函数 2     │ → 位置 7  → 设为 1
    │   哈希函数 3     │ → 位置 15 → 设为 1
    └─────────────────┘
    
    布隆过滤器 (位数组):
    0 1 0 1 0 0 0 1 0 0 0 0 0 0 0 1 ...
          ↑     ↑             ↑
         位置 3  位置 7        位置 15
    
    查询元素 B:
    - 3 个哈希位置都为 1 → 可能存在 (但有误判)
    - 任意 1 个位置为 0 → 一定不存在
```

**核心特点**:
| 特性 | 说明 |
|------|------|
| **空间效率** | 远高于 HashMap (只需 1 bit/元素) |
| **查询时间** | O(k)，k 为哈希函数个数 (常数) |
| **误判率** | 存在 False Positive(误判为存在) |
| **漏判率** | 0%，False Negative 不可能发生 |
| **删除** | 不支持安全删除 (会影响其他元素) |

**误判率公式**:
```
P = (1 - e^(-kn/m))^k

其中:
- n: 元素数量
- m: 位数组大小
- k: 哈希函数个数

最优 k ≈ (m/n) × ln(2)
```

### 📁 本项目中的体现 (及改进建议)

**当前实现** (`RedisBloomFilterUtil.java` 行 259-262):
```java
/**
 * 计算 ID 对应的位位置
 * Warning: 使用简单的哈希函数：ID * 质数
 * 实际项目中可使用更复杂的哈希算法（如 MurmurHash）
 */
private long getBitPosition(Integer id) {
    return id.longValue() * 31;  // 单一哈希，冲突概率较高
}
```

**问题**: 
- 只使用 1 个哈希函数，误判率偏高
- 位数组大小无限制，可能溢出

**改进建议** (多哈希函数):
```java
private List<Long> getBitPositions(Integer id) {
    List<Long> positions = new ArrayList<>();
    
    // 使用 3 个不同的哈希函数
    long hash1 = murmurHash(id, 0);
    long hash2 = murmurHash(id, 1);
    long hash3 = murmurHash(id, 2);
    
    positions.add(hash1 % BIT_ARRAY_SIZE);
    positions.add(hash2 % BIT_ARRAY_SIZE);
    positions.add(hash3 % BIT_ARRAY_SIZE);
    
    return positions;
}

private long murmurHash(Integer id, long seed) {
    // MurmurHash3 实现
    long hash = seed;
    hash ^= id;
    hash *= 0xc6a4a7935bd1e995L;
    hash ^= hash >>> 47;
    hash *= 0xc6a4a7935bd1e995L;
    return hash;
}
```

---

## 3.5 Redis 分布式锁

### ❓ 面试官问：如何用 Redis 实现分布式锁？

### ✅ 参考回答

**演进过程**:

```
版本 1: SETNX (有死锁风险)
  SETNX lock_key unique_value
  问题：获取锁后程序崩溃，锁永远不释放

版本 2: SETNX + EXPIRE (非原子，仍有问题)
  SETNX lock_key unique_value
  EXPIRE lock_key 30
  问题：两条命令非原子，EXPIRE 可能失败

版本 3: SET NX EX (正确版本)
  SET lock_key unique_value NX EX 30
  原子操作，同时设置值和过期时间

版本 4: Redisson (生产推荐)
  - 看门狗自动续期
  - 可重入锁
  - 等待锁机制
```

**核心命令**:
```bash
# 获取锁 (原子操作)
SET lock_key unique_value NX EX 30

# 释放锁 (Lua 脚本保证原子性)
if redis.call("get", KEYS[1]) == ARGV[1] then
    return redis.call("del", KEYS[1])
else
    return 0
end
```

### 📁 本项目中的体现

**Redisson 配置** (`config/RedissonConfig.java`):
```java
@Configuration
public class RedissonConfig {
    
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = String.format("redis://%s:%d", redisHost, redisPort);
        config.useSingleServer().setAddress(address);
        return Redisson.create(config);
    }
}
```

**分布式锁切面** (`aspect/DistributedLockAspect.java` 行 85-107):
```java
RLock lock = redissonClient.getLock(lockKey);
boolean acquired = false;

try {
    // 尝试获取锁，等待 5 秒，租约 30 秒
    acquired = lock.tryLock(
        lockAnnotation.waitTime(),   // 5 秒
        lockAnnotation.leaseTime(),  // 30 秒
        lockAnnotation.timeUnit()
    );
    
    if (!acquired) {
        throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
    }
    
    // 执行业务逻辑
    return joinPoint.proceed();
    
} finally {
    // 释放锁
    if (acquired && lock.isHeldByCurrentThread()) {
        lock.unlock();
    }
}
```

---

## 3.6 Redisson 看门狗机制

### ❓ 面试官问：Redisson 的看门狗 (Watchdog) 是什么？

### ✅ 参考回答

**看门狗机制**:

```
┌─────────────────────────────────────────────────────────────┐
│                    看门狗工作原理                            │
├─────────────────────────────────────────────────────────────┤
│  1. 获取锁时未指定 leaseTime → 启用看门狗                     │
│  2. 初始锁过期时间 = 30 秒 (默认)                             │
│  3. 启动后台定时任务 (每 10 秒检查一次)                        │
│  4. 如果锁仍被持有 → 续期到 30 秒                             │
│  5. 业务完成/程序崩溃 → 停止续期，锁自动释放                  │
└─────────────────────────────────────────────────────────────┘
```

**为什么需要看门狗**:
- 业务执行时间不确定，锁过期太短可能提前释放
- 程序崩溃无法主动释放锁
- 看门狗自动续期，兼顾安全性和可用性

**注意**: 本项目中指定了 `leaseTime=30`，**不会启用看门狗**，锁会在 30 秒后自动释放。

### 📁 本项目中的体现

**当前配置** (固定租约时间，无看门狗):
```java
@DistributedLock(key = "'activity:create:' + #activity.activityName", waitTime = 5, leaseTime = 30)
public Activity createActivity(Activity activity) {
    // 锁会在 30 秒后自动释放，即使业务未执行完
}
```

**启用看门狗** (不指定 leaseTime):
```java
// 修改注解，不指定 leaseTime 或使用特殊值
@DistributedLock(key = "'activity:create:' + #activity.activityName", waitTime = 5)
// leaseTime 不传 → 启用看门狗，自动续期直到业务完成
```

---

## 3.7 滑动窗口限流算法

### ❓ 面试官问：限流算法有哪些？滑动窗口如何实现？

### ✅ 参考回答

**4 种限流算法对比**:

| 算法 | 原理 | 优点 | 缺点 | 本项目使用 |
|------|------|------|------|-----------|
| **固定窗口** | 单位时间内计数 | 简单 | 临界问题 | ❌ |
| **滑动窗口** | 窗口内时间戳 | 精确 | 实现复杂 | ✅ |
| **令牌桶** | 定时生成令牌 | 允许突发 | 需额外存储 | ❌ |
| **漏桶** | 固定速率流出 | 平滑流量 | 无法突发 | ❌ |

**滑动窗口图解**:

```
时间轴 →
|----|----|----|----|----|----|----|----|
0    10   20   30   40   50   60   70   80(秒)

当前时间 = 65 秒
窗口大小 = 60 秒

窗口范围：[5, 65]
统计此范围内的请求数：
- 时间戳 15: 不在窗口内 (删除)
- 时间戳 25: 在窗口内 (计数+1)
- 时间戳 55: 在窗口内 (计数+1)
- 时间戳 62: 在窗口内 (计数+1)

当前计数 = 3
如果 maxRequests = 5 → 允许通过
如果 maxRequests = 2 → 限流
```

### 📁 本项目中的实现

**Lua 脚本** (`RedisRateLimiter.java` 行 34-49):
```lua
local key = KEYS[1]
local now = tonumber(ARGV[1])          -- 当前时间戳 (毫秒)
local windowSize = tonumber(ARGV[2])   -- 窗口大小 (毫秒)
local maxRequests = tonumber(ARGV[3])  -- 最大请求数

-- 1. 移除窗口外的时间戳
redis.call('ZREMRANGEBYSCORE', key, 0, now - windowSize)

-- 2. 统计窗口内的请求数
local count = redis.call('ZCARD', key)

-- 3. 判断是否超过限制
if count < maxRequests then
    -- 4. 添加当前请求时间戳
    redis.call('ZADD', key, now, now)
    redis.call('EXPIRE', key, windowSize)
    return 1  -- 允许
else
    return 0  -- 限流
end
```

**Java 调用** (`RedisRateLimiter.java` 行 71-89):
```java
public boolean tryAcquire(String key, int maxRequests, long windowSize) {
    try {
        long now = System.currentTimeMillis();
        List<String> keys = List.of(key);
        Object[] args = new Object[]{now, windowSize * 1000, maxRequests};
        
        Long result = redisTemplate.execute(rateLimitScript, keys, args);
        boolean allowed = result != null && result == 1;
        
        if (!allowed) {
            logger.warn("限流触发 - key: {}, maxRequests: {}, windowSize: {}s", 
                       key, maxRequests, windowSize);
        }
        
        return allowed;
    } catch (Exception e) {
        // Redis 异常时降级放行
        logger.error("限流服务异常，降级放行 - key: {}", key, e.getMessage());
        return true;
    }
}
```

---

## 3.8 Lua 脚本原子性

### ❓ 面试官问：Redis Lua 脚本有什么特点？

### ✅ 参考回答

**核心特点**:

| 特性 | 说明 | 优势 |
|------|------|------|
| **原子性** | 脚本执行期间不会被其他命令打断 | 避免并发问题 |
| **减少网络开销** | 多条命令一次发送 | 降低延迟 |
| **代码复用** | 脚本存储在 Redis | 多次执行只需传参 |
| **事务支持** | 类似事务，但更灵活 | 可条件判断 |

**执行流程**:
```
1. 客户端发送 Lua 脚本到 Redis
2. Redis 编译并缓存脚本
3. 后续调用只需传参数和脚本 SHA
4. Redis 原子执行脚本
5. 返回结果
```

**注意事项**:
- 脚本执行时间不能超过 `lua-time-limit` (默认 5 秒)
- 避免在脚本中使用 `KEYS *` 等模糊匹配
- 所有 Key 必须通过 KEYS 数组传入 (支持集群)

### 📁 本项目中的体现

**脚本注册与执行** (`RedisRateLimiter.java` 行 51-57):
```java
private final DefaultRedisScript<Long> rateLimitScript;

public RedisRateLimiter(RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
    // 编译 Lua 脚本
    this.rateLimitScript = new DefaultRedisScript<>(RATE_LIMIT_SCRIPT, Long.class);
}

// 执行脚本
Long result = redisTemplate.execute(rateLimitScript, keys, args);
```

---

## 3.9 缓存一致性策略

### ❓ 面试官问：如何保证缓存和数据库的一致性？

### ✅ 参考回答

**4 种常见策略**:

| 策略 | 做法 | 优点 | 缺点 | 适用场景 |
|------|------|------|------|----------|
| **先删缓存，再更 DB** | delete cache → update DB | 简单 | 可能读到旧数据 | ❌ 不推荐 |
| **先更 DB，再删缓存** | update DB → delete cache | 简单，一致性较好 | 极端情况不一致 | ✅ 推荐 |
| **延迟双删** | 删→更 DB→sleep→再删 | 更可靠 | 延迟时间难确定 | 高一致性要求 |
| **Canal 监听 binlog** | DB 变更→MQ→删缓存 | 解耦，可靠 | 架构复杂 | 大型系统 |

**推荐方案**: **先更 DB，再删缓存** (Cache Aside Pattern)

```
更新操作:
1. 更新数据库
2. 删除缓存

查询操作:
1. 查缓存 → 命中则返回
2. 未命中 → 查 DB → 写缓存 → 返回
```

### 📁 本项目中的体现

**Cache Aside 模式** (`ActivityService.java`):
```java
// 创建活动：先写 DB，再删缓存
@DistributedLock(key = "'activity:create:' + #activity.activityName")
@CacheEvict(value = "activity:list", key = "'all'")  // 先执行 DB 操作，后删缓存
public Activity createActivity(Activity activity) {
    activityMapper.insert(activity);  // 1. 写 DB
    return activity;                  // 2. 删缓存 (@CacheEvict 后置执行)
}

// 更新活动：先写 DB，再删缓存
@DistributedLock(key = "'activity:edit:' + #activityId")
@CacheEvict(value = {"activity", "activity:list"}, key = "#activityId")
public Activity updateActivity(Integer activityId, Activity activity) {
    Activity existing = activityMapper.selectById(activityId);
    // 更新字段...
    activityMapper.updateById(existing);  // 1. 写 DB
    return existing;                       // 2. 删缓存
}

// 查询活动：先查缓存，未命中查 DB
@Cacheable(value = "activity", key = "#activityId", unless = "#result == null")
public Activity getActivityById(Integer activityId) {
    return activityMapper.selectById(activityId);  // 缓存未命中时执行
}
```

---

## 3.10 Redis 序列化方式

### ❓ 面试官问：Redis 有哪些序列化方式？本项目用哪种？

### ✅ 参考回答

**常见序列化对比**:

| 方式 | 优点 | 缺点 | 可读性 | 性能 |
|------|------|------|--------|------|
| **JDK 默认** | 无需配置 | 序列化后体积大，不可读 | ❌ | ⭐⭐ |
| **String** | 简单 | 只能存字符串 | ✅ | ⭐⭐⭐⭐⭐ |
| **Jackson JSON** | 可读性好，支持复杂对象 | 需要配置 | ✅ | ⭐⭐⭐⭐ |
| **Protostuff** | 体积小，性能高 | 需要 schema | ❌ | ⭐⭐⭐⭐⭐ |

### 📁 本项目中的体现

**CacheConfig.java** (使用 Jackson JSON):
```java
@Bean
public CacheManager cacheManager(RedisConnectionFactory factory) {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());  // 支持 Java8 日期时间
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);  // 日期转字符串
    
    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(30))
        .serializeKeysWith(RedisSerializationContext.SerializationPair
            .fromSerializer(new StringRedisSerializer()))  // Key 用 String
        .serializeValuesWith(RedisSerializationContext.SerializationPair
            .fromSerializer(new GenericJackson2JsonRedisSerializer()))  // Value 用 JSON
        .disableCachingNullValues();
    
    return RedisCacheManager.builder(factory)
        .cacheDefaults(config)
        .build();
}
```

**Redis 中存储的数据格式**:
```json
// Key: activity::1
{
  "activityId": 1,
  "activityName": "测试活动",
  "background": "活动背景",
  "significance": "活动意义",
  "purpose": "活动目的",
  "process": "活动流程",
  "createTime": "2024-01-01T10:00:00",
  "updateTime": "2024-01-01T10:00:00"
}
```

---

## 📝 本篇小结

### 核心知识点回顾

1. ✅ Redis 5 大应用场景 (缓存、锁、限流、布隆过滤器、Token 存储)
2. ✅ @Cacheable 原理与使用
3. ✅ 缓存穿透/击穿/雪崩及解决方案
4. ✅ 布隆过滤器原理与实现
5. ✅ Redis 分布式锁演进 (SETNX → SET NX EX → Redisson)
6. ✅ Redisson 看门狗机制
7. ✅ 滑动窗口限流算法 (Lua 脚本实现)
8. ✅ Lua 脚本原子性
9. ✅ 缓存一致性策略 (先更 DB 再删缓存)
10. ✅ Redis 序列化方式 (Jackson JSON)

### 面试准备建议

**初级开发**: 重点掌握 3.1、3.2、3.3、3.10
**中级开发**: 重点掌握 3.4、3.5、3.7、3.9
**高级开发**: 深入理解 3.6 看门狗、3.8 Lua 原子性、3.9 一致性策略

---

> 💡 **下一步**: 继续学习 [模块 4: JWT 认证与安全篇](./04-JWT 认证与安全篇.md)
