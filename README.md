# HumanResourceOfficial - 学生会人力资源管理系统

<div align="center">

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.4-brightgreen.svg)
![Java](https://img.shields.io/badge/Java-17-orange.svg)
![MySQL](https://img.shields.io/badge/MySQL-8.0+-blue.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)

一个基于Spring Boot的现代化学生会人力资源管理系统后端服务

</div>

## 📖 项目简介

HumanResourceOfficial是一个功能完整的学生会人力资源管理系统后端服务，旨在为学生会提供数字化管理平台。系统支持用户认证、权限控制、活动管理、内部资料管理和往届成员查询等核心功能。

### ✨ 核心特性

- 🔐 **JWT无状态认证** - 基于JSON Web Token的安全认证机制
- 👥 **基于角色的权限控制** - 细粒度的权限管理体系
- 📋 **活动全生命周期管理** - 支持活动创建、更新、图片管理
- 📁 **内部资料管理** - 分类管理、文件上传、下载统计
- 🔍 **往届成员查询** - 历史数据保存与查询功能
- ☁️ **阿里云OSS集成** - 文件存储与管理
- 📱 **RESTful API设计** - 标准化的API接口
- 🔄 **分页查询支持** - 高效的数据分页处理

## 🏗️ 技术架构

### 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.1.4 | 核心框架 |
| Spring Security | 3.1.4 | 安全框架 |
| Spring Data JPA | 3.1.4 | 数据访问层 |
| MyBatis | 3.0.2 | 持久层框架 |
| MySQL | 8.0+ | 数据库 |
| JWT | 0.11.5 | 认证令牌 |
| PageHelper | 1.4.6 | 分页插件 |
| 阿里云OSS | 3.17.2 | 文件存储 |

### 系统架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   前端应用      │    │   API网关       │    │   后端服务      │
│  (Vue/React)    │───▶│   (可选)        │───▶│  Spring Boot    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                       │
                       ┌─────────────────┐            │
                       │   文件存储      │◀───────────┘
                       │  阿里云OSS      │
                       └─────────────────┘
                                                       │
                       ┌─────────────────┐            │
                       │   数据库        │◀───────────┘
                       │    MySQL        │
                       └─────────────────┘
```

## 📁 项目结构

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
│   ├── SecurityConfig.java         # 安全配置
│   └── WebConfig.java              # Web配置
├── controller/          # 控制器层
│   ├── ActivityController.java     # 活动管理
│   ├── AuthController.java         # 认证控制器
│   ├── MaterialController.java     # 资料管理
│   ├── PastActivityController.java # 往届活动
│   └── UserController.java         # 用户管理
├── dto/                 # 数据传输对象
├── entity/              # 实体类
├── enums/               # 枚举类
├── exception/           # 异常处理
├── mapper/              # MyBatis映射器
├── service/             # 业务服务层
├── util/                # 工具类
├── validation/          # 数据验证
└── Main.java            # 应用入口
```

## 🚀 快速开始

### 环境要求

- Java 17+
- Maven 3.6+
- MySQL 8.0+
- 阿里云OSS账号（可选）

### 1. 克隆项目

```bash
git clone https://github.com/yourusername/HumanResourceOfficial.git
cd HumanResourceOfficial
```

### 2. 数据库配置

创建MySQL数据库并导入数据表结构：

```sql
CREATE DATABASE hrofficial CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. 环境变量配置

创建环境变量或修改 `application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/hrofficial?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=GMT%2B8
    username: your_username
    password: your_password
    
# 阿里云OSS配置（可选）
aliyun:
  oss:
    endpoint: ${ALIYUN_OSS_ENDPOINT:oss-cn-hangzhou.aliyuncs.com}
    accessKeyId: ${ALIYUN_OSS_ACCESS_KEY_ID}
    accessKeySecret: ${ALIYUN_OSS_ACCESS_KEY_SECRET}
    bucketName: ${ALIYUN_OSS_BUCKET_NAME}
    domain: ${ALIYUN_OSS_DOMAIN:}
```

Windows环境变量设置：
```cmd
set ALIYUN_OSS_ACCESS_KEY_ID=your_access_key_id
set ALIYUN_OSS_ACCESS_KEY_SECRET=your_access_key_secret
set ALIYUN_OSS_BUCKET_NAME=your_bucket_name
```

Linux/Mac环境变量设置：
```bash
export ALIYUN_OSS_ACCESS_KEY_ID=your_access_key_id
export ALIYUN_OSS_ACCESS_KEY_SECRET=your_access_key_secret
export ALIYUN_OSS_BUCKET_NAME=your_bucket_name
```

### 4. 编译运行

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

### 5. 验证启动

访问 `http://localhost:8080` 确认服务启动成功。

## 📚 API文档

### 认证相关

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/auth/login` | 用户登录 | 公开 |
| POST | `/api/auth/register` | 用户注册 | 公开 |
| POST | `/api/auth/logout` | 用户登出 | 需认证 |

### 活动管理

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/activities` | 获取活动列表 | 需认证 |
| POST | `/api/activities` | 创建活动 | 部长 |
| GET | `/api/activities/{id}` | 获取活动详情 | 需认证 |
| PUT | `/api/activities/{id}` | 更新活动 | 部长 |
| DELETE | `/api/activities/{id}` | 删除活动 | 部长 |
| POST | `/api/activities/{id}/images` | 添加活动图片 | 部长 |
| DELETE | `/api/activities/images/{id}` | 删除活动图片 | 部长 |

### 内部资料管理

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/materials/upload` | 上传资料 | 部长 |
| GET | `/api/materials/category/{id}` | 按分类获取资料 | 部员+ |
| GET | `/api/materials/search` | 搜索资料 | 部员+ |
| POST | `/api/materials/categories` | 创建分类 | 部长 |
| POST | `/api/materials/subcategories` | 创建子分类 | 部长 |

### 往届活动管理

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/past-activities` | 分页查询往届活动 | 公开 |
| POST | `/api/past-activities` | 创建往届活动 | 部长 |
| PUT | `/api/past-activities/{id}` | 更新往届活动 | 部长 |
| DELETE | `/api/past-activities/{id}` | 删除往届活动 | 部长 |

详细的API文档请参考：[往届活动API文档](./往届活动API文档.md) 和 [OSS使用指南](./OSS使用指南.md)

## 🔐 权限系统

### 角色说明

- **游客**：未登录用户，只能访问公开资源
- **部员**：普通成员，可以查看活动和资料
- **部長/副部长**：管理员，拥有所有权限

### 权限注解

```java
@RequireMemberRole("操作描述")    // 需要部员及以上权限
@RequireMinisterRole("操作描述")  // 需要部长权限
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

- **User** - 用户信息
- **Activity** - 活动信息
- **ActivityImage** - 活动图片
- **Material** - 内部资料
- **PastActivity** - 往届活动
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
- 多格式支持
- 自动类型检测
- 文件删除管理

### 支持格式

- **图片**：JPG, PNG, GIF
- **文档**：PDF, DOC, DOCX, XLS, XLSX

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
      
jwt:
  secret: your-secret-key
  expiration: 7200000  # 2小时

pagehelper:
  helper-dialect: mysql
  reasonable: true
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

### 单元测试

```bash
mvn test
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
- **连接池**：HikariCP高性能连接池
- **缓存机制**：可扩展Redis缓存
- **异步处理**：文件上传异步处理
- **索引优化**：数据库索引优化

## 🔒 安全措施

- JWT令牌认证
- CORS跨域配置
- SQL注入防护
- XSS攻击防护
- 文件类型检查
- 权限细粒度控制

## 🚀 部署指南

### Docker部署（推荐）

```dockerfile
FROM openjdk:17-jdk-slim
COPY target/HumanResourceOfficial-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
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

- [ ] 添加Redis缓存支持
- [ ] 实现消息通知功能
- [ ] 添加数据导出功能
- [ ] 支持批量操作
- [ ] 实现定时任务
- [ ] 添加监控指标

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

感谢以下开源项目的支持：

- [Spring Boot](https://spring.io/projects/spring-boot)
- [MyBatis](https://mybatis.org/)
- [JWT](https://jwt.io/)
- [阿里云OSS](https://www.aliyun.com/product/oss)

---

<div align="center">
  
**⭐ 如果这个项目对你有帮助，请给个Star支持一下！**

</div>