# AI聊天系统测试指南

## 一、单元测试

### 1. 编译测试
```bash
# 后端编译
mvn clean compile

# 前端编译
cd hrofficial-frontend
npm run build
```

### 2. 运行集成测试
```bash
# 方式1：使用Spring Boot运行测试
mvn spring-boot:run -Dspring-boot.run.main-class=com.redmoon2333.IntegrationTest

# 方式2：打包后运行
mvn clean package -DskipTests
java -jar target/HumanResourceOfficial-1.0-SNAPSHOT.jar
```

## 二、API接口测试

### 使用HTTP Client测试
1. 安装 VS Code REST Client 插件 或 IntelliJ HTTP Client
2. 打开 `test-api.http` 文件
3. 修改 `@token` 为你的JWT Token
4. 依次运行测试请求

### 使用curl测试

```bash
# 1. 登录获取Token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"your_username","password":"your_password"}'

# 2. 测试RAG对话
curl -X POST http://localhost:8080/api/ai/chat-with-r