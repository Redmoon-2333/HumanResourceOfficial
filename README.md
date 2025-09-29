# 学生会部门官方网站 - 登录注册功能

## 项目简介

这是一个基于Spring Boot的学生会部门官方网站项目，目前实现了基础的用户登录注册功能。

## 技术栈

- **后端**: Spring Boot 3.1.4
- **数据库**: MySQL 8.0
- **安全**: Spring Security
- **ORM**: Spring Data JPA
- **前端**: HTML + CSS + JavaScript
- **构建工具**: Maven

## 功能特性

### 已实现功能
- ✅ 用户注册（基于激活码）
- ✅ 用户登录
- ✅ 用户信息管理
- ✅ 激活码管理
- ✅ 基础的权限控制
- ✅ 前端交互页面

### 待实现功能
- ⏳ 活动展示模块
- ⏳ 往届活动模块  
- ⏳ 内部资料模块
- ⏳ 往届社员模块

## 快速开始

### 1. 环境要求

- JDK 17+
- MySQL 8.0+
- Maven 3.6+

### 2. 数据库设置

1. 启动MySQL服务
2. 执行数据库初始化脚本：
   ```bash
   mysql -u root -p < database/init.sql
   ```
3. 确保数据库连接配置正确（src/main/resources/application.yml）

### 3. 运行项目

1. 更新Maven依赖：
   ```bash
   mvn clean install
   ```

2. 启动Spring Boot应用：
   ```bash
   mvn spring-boot:run
   ```
   
   或者运行Main类：
   ```bash
   java -jar target/HumanResourceOfficial-1.0-SNAPSHOT.jar
   ```

3. 访问应用：
   - 前端页面: http://localhost:8080
   - API文档: http://localhost:8080/api/test/health

## API接口说明

### 认证相关接口

#### 1. 用户登录
- **URL**: `POST /api/auth/login`
- **请求体**:
  ```json
  {
    "username": "用户名",
    "password": "密码"
  }
  ```
- **响应**:
  ```json
  {
    "success": true,
    "message": "登录成功",
    "data": {
      "userId": 1,
      "username": "用户名",
      "roleHistory": "角色信息"
    }
  }
  ```

#### 2. 用户注册
- **URL**: `POST /api/auth/register`
- **请求体**:
  ```json
  {
    "username": "用户名",
    "password": "密码",
    "confirmPassword": "确认密码",
    "activationCode": "激活码",
    "roleHistory": "角色信息(可选)"
  }
  ```

#### 3. 检查用户名
- **URL**: `GET /api/auth/check-username?username=用户名`

#### 4. 获取当前用户
- **URL**: `GET /api/auth/current-user`

#### 5. 用户登出
- **URL**: `POST /api/auth/logout`

### 测试接口

#### 1. 系统健康检查
- **URL**: `GET /api/test/health`

#### 2. 生成激活码
- **URL**: `POST /api/test/generate-activation-code`

## 测试说明

### 使用预设的激活码进行注册测试：
- `TEST123456789ABC`
- `DEMO987654321DEF`
- `SAMPLE111222333`

### 或者通过前端页面生成新的激活码

### 测试管理员账号：
- 用户名: `admin`
- 密码: `admin123`

## 项目结构

```
src/
├── main/
│   ├── java/com/redmoon2333/
│   │   ├── Main.java                 # 启动类
│   │   ├── config/                   # 配置类
│   │   │   ├── SecurityConfig.java   # 安全配置
│   │   │   └── WebConfig.java        # Web配置
│   │   ├── controller/               # 控制器
│   │   │   ├── AuthController.java   # 认证控制器
│   │   │   └── TestController.java   # 测试控制器
│   │   ├── dto/                      # 数据传输对象
│   │   │   ├── ApiResponse.java      # 通用响应类
│   │   │   ├── LoginRequest.java     # 登录请求
│   │   │   └── RegisterRequest.java  # 注册请求
│   │   ├── entity/                   # 实体类
│   │   │   ├── User.java             # 用户实体
│   │   │   └── ActivationCode.java   # 激活码实体
│   │   ├── repository/               # 数据访问层
│   │   │   ├── UserRepository.java
│   │   │   └── ActivationCodeRepository.java
│   │   └── service/                  # 服务层
│   │       └── AuthService.java      # 认证服务
│   └── resources/
│       ├── application.yml           # 应用配置
│       └── static/
│           └── index.html            # 前端页面
├── database/
│   └── init.sql                      # 数据库初始化脚本
└── pom.xml                          # Maven配置文件
```

## 配置说明

### application.yml 主要配置项：

- **数据库连接**: 修改 `spring.datasource` 下的配置
- **服务器端口**: 修改 `server.port`
- **日志级别**: 修改 `logging.level`

### 安全配置：

- CSRF保护已禁用（开发环境）
- `/api/auth/**` 路径开放访问
- 静态资源开放访问
- 其他路径需要认证

## 开发说明

### 添加新功能的步骤：

1. 在 `entity` 包中创建实体类
2. 在 `repository` 包中创建数据访问接口
3. 在 `service` 包中实现业务逻辑
4. 在 `controller` 包中创建REST接口
5. 添加必要的DTO类
6. 更新前端页面（如需要）

### 数据库迁移：

项目使用JPA自动DDL功能，启动时会自动创建/更新表结构。生产环境建议关闭此功能并使用手动SQL脚本。

## 问题排查

### 常见问题：

1. **数据库连接失败**: 检查MySQL服务是否启动，用户名密码是否正确
2. **端口占用**: 修改application.yml中的server.port配置
3. **依赖下载失败**: 检查网络连接，尝试使用国内Maven镜像

### 日志查看：

应用日志会输出到控制台，详细的数据库SQL日志也会显示。

## 后续开发计划

1. 完善用户权限管理
2. 实现活动管理模块
3. 添加文件上传功能
4. 实现往届成员管理
5. 优化前端界面设计
6. 添加单元测试

## 贡献指南

1. Fork项目
2. 创建功能分支
3. 提交更改
4. 推送到分支
5. 创建Pull Request