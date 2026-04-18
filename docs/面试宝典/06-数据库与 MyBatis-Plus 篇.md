# 模块 6: 数据库与 MyBatis-Plus 篇

> 📌 **本篇重点**: MyBatis-Plus 优势、逻辑删除、自动填充、分页插件、索引优化、慢 SQL 排查
> 
> 🎯 **面试频率**: ⭐⭐⭐⭐⭐ (数据库是后端开发基石，ORM 框架必问)
> 
> 💡 **项目亮点**: 本项目使用 MyBatis-Plus 3.5.12，实现自动填充、分页查询、逻辑删除

---

## 6.1 MyBatis-Plus 优势

### ❓ 面试官问：为什么选择 MyBatis-Plus？它 compared to 原生 MyBatis 有什么优势？

### ✅ 参考回答

**MyBatis-Plus (MP)** 是 MyBatis 的增强工具，在 MyBatis 基础上简化开发。

**核心优势**:

| 特性 | 原生 MyBatis | MyBatis-Plus | 优势说明 |
|------|-------------|--------------|----------|
| **CRUD 方法** | 需手写 XML | 内置通用 Mapper | 减少 80% 样板代码 |
| **分页查询** | 手动写 LIMIT | 自动分页插件 | 一行代码搞定 |
| **条件构造** | 手写 WHERE | LambdaQueryWrapper | 类型安全，防 SQL 注入 |
| **代码生成** | 无 | 自动生成 Entity/Mapper/Service | 快速搭建项目 |
| **插件扩展** | 手动实现 | 丰富插件 (乐观锁/逻辑删除) | 开箱即用 |

**代码对比**:

```
原生 MyBatis:
  1. 写 XML: <select id="selectById"> SELECT * FROM user WHERE id = #{id} </select>
  2. 写 Mapper 接口: User selectById(Integer id);
  3. 调用：userMapper.selectById(id);

MyBatis-Plus:
  1. Mapper 继承 BaseMapper<User>
  2. 直接调用：userMapper.selectById(id);  // 无需 XML
```

### 📁 本项目中的体现

**BaseMapper 继承** (所有 Mapper):

```java
// UserMapper.java
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    // 自定义查询方法 (MP 没有的场景)
    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(String username);
    
    @Select("SELECT * FROM user WHERE name LIKE CONCAT('%', #{name}, '%')")
    List<User> findByNameLike(String name);
}
```

**Service 使用** (无需写 XML):

```java
@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    // 使用 MP 内置方法
    public User getUserById(Integer userId) {
        return userMapper.selectById(userId);  // 内置方法
    }
    
    public List<User> getAllUsers() {
        return userMapper.selectList(null);  // 查询所有，null 表示无条件
    }
    
    // 条件查询
    public List<User> getUsersByRole(String role) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole, role);
        return userMapper.selectList(wrapper);
    }
}
```

---

## 6.2 逻辑删除实现

### ❓ 面试官问：什么是逻辑删除？如何实现？

### ✅ 参考回答

**逻辑删除 vs 物理删除**:

| 删除方式 | 实现 | 优点 | 缺点 |
|---------|------|------|------|
| **物理删除** | DELETE FROM table | 真正删除数据 | 数据丢失，无法恢复 |
| **逻辑删除** | UPDATE table SET deleted = 1 | 数据保留，可恢复 | 占用存储空间 |

**逻辑删除原理**:

```
物理删除:
  DELETE FROM user WHERE id = 1
  → 数据永久丢失

逻辑删除:
  UPDATE user SET deleted = 1, deleted_time = NOW() WHERE id = 1
  → 数据仍在，标记为已删除
  → 查询时自动过滤 deleted = 1 的记录
```

**适用场景**:
- 核心业务数据 (订单、用户)
- 需要审计追溯的场景
- 可能恢复的数据

### 📁 本项目中的体现

**实体类配置** (使用 `@TableLogic` 注解):

```java
@Data
@TableName("activity")
public class Activity {
    
    @TableId(type = IdType.AUTO)
    private Integer activityId;
    
    private String activityName;
    
    // 逻辑删除字段
    @TableLogic  // 标记为逻辑删除字段
    private Integer deleted;  // 0=未删除，1=已删除
    
    private LocalDateTime deletedTime;  // 删除时间
}
```

**配置文件** (application.yml):

```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted  # 全局逻辑删除字段名
      logic-delete-value: 1        # 已删除值
      logic-not-delete-value: 0    # 未删除值
```

**自动注入 SQL** (MP 自动处理):

```
正常删除:
  userMapper.deleteById(1);
  → MP 自动生成：UPDATE activity SET deleted = 1 WHERE activity_id = 1

正常查询:
  userMapper.selectById(1);
  → MP 自动生成：SELECT * FROM activity WHERE activity_id = 1 AND deleted = 0
```

---

## 6.3 自动填充功能

### ❓ 面试官问：如何实现创建时间、更新时间的自动填充？

### ✅ 参考回答

**自动填充原理**:

```
┌─────────────────────────────────────────────────────────────┐
│              MyBatis-Plus 自动填充机制                       │
├─────────────────────────────────────────────────────────────┤
│  1. 实体类字段标注 @TableField(fill = FieldFill.INSERT)      │
│  2. 实现 MetaObjectHandler 接口                              │
│  3. 插入/更新时自动调用 handler 填充指定字段                   │
└─────────────────────────────────────────────────────────────┘
```

**填充时机**:

| 填充类型 | 注解 | 触发时机 |
|---------|------|----------|
| INSERT | `FieldFill.INSERT` | 仅插入时填充 |
| UPDATE | `FieldFill.UPDATE` | 仅更新时填充 |
| INSERT_UPDATE | `FieldFill.INSERT_UPDATE` | 插入和更新都填充 |

### 📁 本项目中的体现

**实体类标注** (Activity.java):

```java
@Data
@TableName("activity")
public class Activity {
    
    @TableId(type = IdType.AUTO)
    private Integer activityId;
    
    private String activityName;
    
    // 创建时间 - 插入时自动填充
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    // 更新时间 - 插入和更新时都填充
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

**自动填充处理器**:

```java
@Configuration
public class MybatisPlusConfig {
    
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                // 插入时填充 createTime 和 updateTime
                this.strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
            }
            
            @Override
            public void updateFill(MetaObject metaObject) {
                // 更新时填充 updateTime
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
            }
        };
    }
}
```

**使用效果**:

```java
// 插入操作 - 自动填充 createTime 和 updateTime
Activity activity = new Activity();
activity.setActivityName("测试活动");
activityMapper.insert(activity);
// activity.getCreateTime() 和 getUpdateTime() 自动有值

// 更新操作 - 自动填充 updateTime
activity.setActivityName("新名称");
activityMapper.updateById(activity);
// activity.getUpdateTime() 自动更新为当前时间
```

---

## 6.4 分页插件原理

### ❓ 面试官问：MyBatis-Plus 的分页插件是如何工作的？

### ✅ 参考回答

**分页插件原理**:

```
┌─────────────────────────────────────────────────────────────┐
│                MyBatis-Plus 分页原理                        │
├─────────────────────────────────────────────────────────────┤
│  1. 拦截 SQL 执行 (使用 MyBatis Interceptor)                  │
│  2. 解析原始 SQL                                             │
│  3. 自动添加 LIMIT 子句                                       │
│  4. 同时执行 COUNT 查询获取总数                               │
│  5. 返回分页结果 (records + total)                           │
└─────────────────────────────────────────────────────────────┘
```

**SQL 改写示例**:

```
原始 SQL:
  SELECT * FROM activity

分页后 (page=2, size=10):
  SELECT * FROM activity LIMIT 10 OFFSET 10

同时执行 COUNT:
  SELECT COUNT(*) FROM activity
```

### 📁 本项目中的体现

**分页配置** (MybatisPlusConfig.java):

```java
@Configuration
public class MybatisPlusConfig {
    
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 分页插件 (指定数据库类型)
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        
        // 防止全表更新/删除插件
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        
        return interceptor;
    }
}
```

**Service 使用分页**:

```java
@Service
public class ActivityService {
    
    @Autowired
    private ActivityMapper activityMapper;
    
    public IPage<Activity> getActivitiesByPage(int pageNum, int pageSize) {
        // 创建分页对象
        Page<Activity> page = new Page<>(pageNum, pageSize);
        
        // 执行分页查询
        IPage<Activity> result = activityMapper.selectPage(page, null);
        
        // result 包含:
        // - records: 当前页数据列表
        // - total: 总记录数
        // - pages: 总页数
        // - current: 当前页码
        // - size: 每页大小
        
        return result;
    }
}
```

**Controller 返回分页结果**:

```java
@GetMapping("/activities/page")
public ApiResponse<Map<String, Object>> getActivitiesByPage(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size) {
    
    IPage<Activity> activityPage = activityService.getActivitiesByPage(page, size);
    
    Map<String, Object> result = new HashMap<>();
    result.put("records", activityPage.getRecords());  // 数据列表
    result.put("total", activityPage.getTotal());      // 总数
    result.put("pages", activityPage.getPages());      // 总页数
    result.put("current", activityPage.getCurrent());  // 当前页
    
    return ApiResponse.success(result);
}
```

---

## 6.5 索引优化策略

### ❓ 面试官问：如何设计数据库索引？有什么优化策略？

### ✅ 参考回答

**索引类型**:

| 索引类型 | 特点 | 适用场景 |
|---------|------|----------|
| **主键索引** | 唯一，非空，聚簇索引 | 主键 |
| **唯一索引** | 唯一，可空 | 用户名、邮箱 |
| **普通索引** | 无限制 | 普通查询字段 |
| **复合索引** | 多字段组合 | 多条件联合查询 |
| **全文索引** | 全文搜索 | 大文本搜索 |

**索引设计原则**:

```
1. 查询频繁的字段建索引
2. WHERE/ORDER BY/JOIN 的字段建索引
3. 区分度高的字段建索引 (如手机号，不适合性别)
4. 复合索引遵循最左前缀原则
5. 避免过度索引 (影响写入性能)
```

**最左前缀原则**:

```
复合索引：(name, age, city)

✅ 可以使用索引:
  WHERE name = '张三'
  WHERE name = '张三' AND age = 20
  WHERE name = '张三' AND age = 20 AND city = '北京'

❌ 不能使用索引:
  WHERE age = 20              # 跳过 name
  WHERE city = '北京'          # 跳过 name 和 age
  WHERE age = 20 AND city = '北京'  # 跳过 name
```

### 📁 本项目中的体现

**数据库表设计** (DDL):

```sql
CREATE TABLE user (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(50),
    role VARCHAR(20),
    
    -- 唯一索引 (防止用户名重复)
    UNIQUE INDEX idx_username (username),
    
    -- 普通索引 (加速按角色查询)
    INDEX idx_role (role),
    
    -- 复合索引 (加速按角色 + 姓名查询)
    INDEX idx_role_name (role, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE activity (
    activity_id INT PRIMARY KEY AUTO_INCREMENT,
    activity_name VARCHAR(100) NOT NULL,
    create_time DATETIME,
    
    -- 索引加速按时间排序
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**查询优化** (使用索引):

```java
// ✅ 使用索引查询
public User findByUsername(String username) {
    // idx_username 索引生效
    return userMapper.findByUsername(username);
}

// ✅ 复合索引生效 (最左前缀)
public List<User> findByRoleAndName(String role, String name) {
    // idx_role_name 索引生效
    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(User::getRole, role)
           .eq(User::getName, name);
    return userMapper.selectList(wrapper);
}

// ❌ 复合索引失效
public List<User> findByNameOnly(String name) {
    // idx_role_name 索引失效 (跳过 role)
    // 全表扫描
    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(User::getName, name);
    return userMapper.selectList(wrapper);
}
```

---

## 6.6 慢 SQL 排查

### ❓ 面试官问：如何发现和排查慢 SQL？

### ✅ 参考回答

**慢 SQL 发现**:

| 方法 | 实现 | 说明 |
|------|------|------|
| **慢查询日志** | MySQL slow_query_log | 记录超过阈值的 SQL |
| **SHOW PROCESSLIST** | 查看当前执行的 SQL | 实时排查 |
| **APM 工具** | SkyWalking/Prometheus | 监控接口响应时间 |
| **MyBatis 日志** | log-impl | 打印 SQL 执行时间 |

**排查步骤**:

```
1. 定位慢 SQL (慢查询日志/APM)
       ↓
2. 使用 EXPLAIN 分析执行计划
       ↓
3. 检查是否使用索引
       ↓
4. 检查是否有全表扫描
       ↓
5. 优化 SQL 或添加索引
       ↓
6. 验证优化效果
```

**EXPLAIN 结果分析**:

| 字段 | 含义 | 优化目标 |
|------|------|----------|
| **type** | 访问类型 | 至少达到 range，最好 ref 或 const |
| **key** | 使用的索引 | 必须有值 |
| **rows** | 扫描行数 | 越少越好 |
| **Extra** | 额外信息 | 避免 Using temporary/Using filesort |

### 📁 本项目中的体现

**开启慢查询日志** (MySQL 配置):

```ini
# my.cnf
[mysqld]
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 2  # 超过 2 秒的 SQL 记录为慢查询
```

**MyBatis 日志配置** (application.yml):

```yaml
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

logging:
  level:
    com.redmoon2333.mapper: DEBUG  # 打印 SQL 执行细节
```

**EXPLAIN 分析示例**:

```sql
-- 慢 SQL
SELECT * FROM activity WHERE activity_name LIKE '%测试%';

-- 分析执行计划
EXPLAIN SELECT * FROM activity WHERE activity_name LIKE '%测试%';

-- 结果分析:
-- type: ALL (全表扫描，需要优化)
-- key: NULL (没有使用索引)
-- rows: 10000 (扫描 10000 行)
-- Extra: Using where

-- 优化方案:
-- 1. 如果经常按名称搜索，添加全文索引
-- 2. 避免前缀模糊查询 (%测试%)
-- 3. 使用 Elasticsearch 等搜索引擎
```

---

## 6.7 事务注解使用

### ❓ 面试官问：@Transactional 如何使用？有什么注意事项？

### ✅ 参考回答

**事务传播行为**:

| 传播行为 | 含义 | 使用场景 |
|---------|------|----------|
| **REQUIRED** (默认) | 有事务则加入，无则新建 | 大多数场景 |
| **REQUIRES_NEW** | 总是新建事务，挂起当前事务 | 日志记录，不计入主事务 |
| **NESTED** | 嵌套事务，可部分回滚 | 批量操作，部分成功 |
| **SUPPORTS** | 有事务则支持，无则非事务 | 查询操作 |

**事务失效场景**:

```
1. 方法不是 public
   @Transactional on private/protected → 失效

2. 自调用 (同类方法调用)
   this.methodB();  // AOP 代理无法拦截
   → 事务失效

3. 异常被 catch 未抛出
   try { ... } catch (Exception e) { }
   → 事务不会回滚

4. 异常类型不匹配
   @Transactional(rollbackFor = Exception.class)
   throw new RuntimeException();  // 默认只回滚 RuntimeException
```

### 📁 本项目中的体现

**Service 事务使用**:

```java
@Service
public class AuthService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private ActivationCodeMapper activationCodeMapper;
    
    /**
     * 用户使用激活码注册
     * 需要事务保证数据一致性
     */
    @Transactional(rollbackFor = Exception.class)
    public RegisterResponse registerByActivationCode(RegisterRequest request) {
        // 1. 验证激活码
        ActivationCode code = activationCodeMapper.findByCode(request.getCode());
        if (code == null) {
            throw new BusinessException(ErrorCode.INVALID_ACTIVATION_CODE);
        }
        if (code.getStatus() == ActivationStatus.已使用) {
            throw new BusinessException(ErrorCode.ACTIVATION_CODE_USED);
        }
        
        // 2. 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(encodePassword(request.getPassword()));
        user.setRoleHistory(code.getRoleGranted());
        
        userMapper.insert(user);  // 如果失败，自动回滚
        
        // 3. 标记激活码已使用
        code.setStatus(ActivationStatus.已使用);
        code.setUserId(user.getUserId());
        code.setUseTime(LocalDateTime.now());
        
        activationCodeMapper.updateById(code);  // 如果失败，自动回滚
        
        return new RegisterResponse(user.getUserId(), "注册成功");
    }
}
```

**事务失效示例** (需要避免):

```java
@Service
public class UserService {
    
    // ❌ 错误：自调用导致事务失效
    public void register(User user) {
        // 外层方法无事务
        saveUser(user);  // 自调用，@Transactional 不生效
    }
    
    @Transactional
    private void saveUser(User user) {
        userMapper.insert(user);
    }
    
    // ✅ 正确：在 Controller 层调用带事务的方法
    // 或者将 @Transactional 移到外层方法
}
```

---

## 6.8 N+1 查询问题

### ❓ 面试官问：什么是 N+1 问题？如何解决？

### ✅ 参考回答

**N+1 问题**:

```
场景：查询 100 个用户，并获取每个用户的部门信息

❌ N+1 查询:
  1. SELECT * FROM user;                    // 1 次查询，返回 100 个用户
  2. for (User user : users) {
         SELECT * FROM dept WHERE id = ?;   // 100 次查询
     }
  总共：1 + 100 = 101 次查询

✅ 解决方案 1 - 预加载 (JOIN):
  SELECT u.*, d.* 
  FROM user u 
  LEFT JOIN dept d ON u.dept_id = d.id;
  总共：1 次查询

✅ 解决方案 2 - 批量查询 (IN):
  1. SELECT * FROM user;                    // 1 次
  2. SELECT * FROM dept WHERE id IN (1,2,3...);  // 1 次
  总共：2 次查询
```

### 📁 本项目中的体现

**N+1 问题示例** (UserService.java):

```java
// ❌ 错误示例：N+1 查询
public List<ActivationCodeResponse> getActivationCodesByUser(String token) {
    User user = authService.getUserFromToken(token);
    List<ActivationCode> codes = activationCodeMapper.findByCreatorId(user.getUserId());
    
    List<ActivationCodeResponse> result = new ArrayList<>();
    for (ActivationCode code : codes) {
        ActivationCodeResponse dto = new ActivationCodeResponse();
        dto.setCode(code.getCode());
        
        // N+1 问题：循环内查询用户
        if (code.getUserId() != null) {
            User usedBy = userMapper.selectById(code.getUserId());  // 每次循环查一次
            dto.setUsedByName(usedBy.getUsername());
        }
        
        result.add(dto);
    }
    return result;
}

// ✅ 正确示例：批量查询避免 N+1
public List<ActivationCodeResponse> getActivationCodesByUser(String token) {
    User user = authService.getUserFromToken(token);
    List<ActivationCode> codes = activationCodeMapper.findByCreatorId(user.getUserId());
    
    // 1. 先收集所有需要查询的用户 ID
    Set<Integer> userIds = codes.stream()
        .filter(code -> code.getUserId() != null)
        .map(ActivationCode::getUserId)
        .collect(Collectors.toSet());
    
    // 2. 批量查询用户
    Map<Integer, String> userNameMap = new HashMap<>();
    if (!userIds.isEmpty()) {
        List<User> users = userMapper.findByIds(new ArrayList<>(userIds));
        userNameMap = users.stream()
            .collect(Collectors.toMap(User::getUserId, User::getUsername));
    }
    
    // 3. 构建结果
    List<ActivationCodeResponse> result = new ArrayList<>();
    for (ActivationCode code : codes) {
        ActivationCodeResponse dto = new ActivationCodeResponse();
        dto.setCode(code.getCode());
        
        // 从内存 Map 中获取，不再查数据库
        dto.setUsedByName(userNameMap.get(code.getUserId()));
        
        result.add(dto);
    }
    return result;
}
```

---

## 6.9 连接池配置

### ❓ 面试官问：数据库连接池如何配置？HikariCP 有什么优势？

### ✅ 参考回答

**HikariCP 优势**:

| 特性 | 说明 |
|------|------|
| **性能最强** | 官方基准测试性能第一 |
| **零开销** | 字节码不生成代理 |
| **快速失败** | 连接超时可快速感知 |
| **连接检测** | 智能检测连接存活 |

**核心参数**:

| 参数 | 含义 | 推荐值 |
|------|------|--------|
| **maximum-pool-size** | 最大连接数 | CPU 核数 * 2 + 1 |
| **minimum-idle** | 最小空闲连接 | 与 max 相同或略小 |
| **connection-timeout** | 连接超时 (ms) | 30000 |
| **idle-timeout** | 空闲超时 (ms) | 600000 |
| **max-lifetime** | 连接最大生命周期 (ms) | 1800000 |

### 📁 本项目中的体现

**application.yml** (HikariCP 配置):

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/hrofficial?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD:}
    
    hikari:
      # 连接池名称
      pool-name: HikariPool-HROfficial
      
      # 最小空闲连接数
      minimum-idle: 5
      
      # 最大连接数
      maximum-pool-size: 20
      
      # 连接超时时间 (毫秒)
      connection-timeout: 30000
      
      # 空闲连接超时 (毫秒)
      idle-timeout: 600000
      
      # 连接最大生命周期 (毫秒)
      max-lifetime: 1800000
      
      # 连接测试查询
      connection-test-query: SELECT 1
```

**生产环境建议**:

```yaml
# 生产环境根据服务器配置调整
spring:
  datasource:
    hikari:
      # 4 核 8G 服务器推荐配置
      minimum-idle: 10
      maximum-pool-size: 50
      
      # 高并发场景可增加
      # 8 核 16G: maximum-pool-size = 100
```

---

## 6.10 数据库范式

### ❓ 面试官问：数据库三大范式是什么？实际项目中如何权衡？

### ✅ 参考回答

**三大范式**:

| 范式 | 要求 | 目的 |
|------|------|------|
| **第一范式 (1NF)** | 每列原子性，不可再分 | 消除重复列 |
| **第二范式 (2NF)** | 在 1NF 基础上，非主键字段完全依赖主键 | 消除部分依赖 |
| **第三范式 (3NF)** | 在 2NF 基础上，非主键字段直接依赖主键 | 消除传递依赖 |

**范式示例**:

```
❌ 违反 1NF:
  用户表：
  | 用户 ID | 姓名 | 电话              |
  | 1     | 张三 | 13812345678,13987654321 |
  
  ✅ 符合 1NF:
  | 用户 ID | 姓名 | 电话       |
  | 1     | 张三 | 13812345678 |
  | 1     | 张三 | 13987654321 |

❌ 违反 2NF:
  订单明细 (订单 ID, 产品 ID, 产品名称, 数量)
  → 产品名称只依赖产品 ID，不依赖订单 ID
  
  ✅ 符合 2NF:
  订单明细 (订单 ID, 产品 ID, 数量)
  产品表 (产品 ID, 产品名称)

❌ 违反 3NF:
  订单 (订单 ID, 用户 ID, 用户名, 下单时间)
  → 用户名通过用户 ID 间接依赖订单 ID
  
  ✅ 符合 3NF:
  订单 (订单 ID, 用户 ID, 下单时间)
  用户表 (用户 ID, 用户名)
```

**实际项目权衡**:

```
反范式化 (Denormalization) 场景:

1. 读多写少：适当冗余，减少 JOIN
   - 订单表冗余用户名 (避免每次 JOIN 用户表)
   
2. 报表统计：预计算字段
   - 订单表增加 order_total 字段
   
3. 分库分表：数据冗余
   - 子表冗余父表信息，避免跨库 JOIN
```

### 📁 本项目中的体现

**符合范式的设计**:

```sql
-- 用户表 (符合 3NF)
CREATE TABLE user (
    user_id INT PRIMARY KEY,
    username VARCHAR(50),
    password VARCHAR(255),
    name VARCHAR(50),
    role VARCHAR(20)
);

-- 活动表 (符合 3NF)
CREATE TABLE activity (
    activity_id INT PRIMARY KEY,
    activity_name VARCHAR(100),
    background TEXT,
    significance TEXT,
    purpose TEXT,
    process TEXT,
    create_time DATETIME,
    update_time DATETIME
);

-- 活动图片表 (符合 3NF)
CREATE TABLE activity_image (
    image_id INT PRIMARY KEY,
    activity_id INT,  -- 外键
    image_url VARCHAR(255),
    FOREIGN KEY (activity_id) REFERENCES activity(activity_id)
);
```

**反范式化示例** (日志记录):

```sql
-- 日志表故意冗余用户名字段
-- Why: 日志查询频繁，避免每次 JOIN 用户表
CREATE TABLE operation_log (
    log_id INT PRIMARY KEY,
    user_id INT,
    username VARCHAR(50),  -- 冗余，方便查询
    operation VARCHAR(100),
    create_time DATETIME
);
```

---

## 📝 本篇小结

### 核心知识点回顾

1. ✅ MyBatis-Plus 优势 (通用 Mapper、代码生成)
2. ✅ 逻辑删除实现 (@TableLogic)
3. ✅ 自动填充功能 (MetaObjectHandler)
4. ✅ 分页插件原理 (Interceptor 拦截改写 SQL)
5. ✅ 索引优化策略 (最左前缀原则)
6. ✅ 慢 SQL 排查 (慢查询日志、EXPLAIN 分析)
7. ✅ 事务注解使用 (@Transactional)
8. ✅ N+1 查询问题及解决方案
9. ✅ 连接池配置 (HikariCP)
10. ✅ 数据库范式与反范式化权衡

### 面试准备建议

**初级开发**: 重点掌握 6.1、6.2、6.3、6.9
**中级开发**: 重点掌握 6.4、6.5、6.7、6.8
**高级开发**: 深入理解 6.6 慢 SQL 排查、6.10 范式权衡、索引优化

---

> 💡 **下一步**: 继续学习 [模块 7: 综合实战与架构设计篇](./07-综合实战与架构设计篇.md)
