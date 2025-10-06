# JAR包部署文件清单与说明

## 📋 服务器端必要文件清单

### 🎯 必须上传的文件（5个）
```
/opt/hrofficial/
├── app.jar                 # 编译后的JAR包（主程序）
├── Dockerfile              # Docker镜像构建文件
├── docker-compose.yml      # Docker服务编排文件
├── .env                    # 环境变量配置文件
├── init.sql                # 数据库初始化脚本
└── deploy.sh               # Linux部署脚本
```

### 📁 自动创建的目录
部署脚本会自动创建以下目录：
```
├── data/                   # 数据持久化目录
├── logs/                   # 日志文件目录
└── mysql-init/             # MySQL初始化脚本目录
```

## 🔨 本机准备工作

### 1. 编译JAR包
在本机项目根目录执行：
```bash
# Windows
mvn clean package -DskipTests
copy target\HumanResourceOfficial-1.0-SNAPSHOT.jar app.jar

# Linux/Mac
mvn clean package -DskipTests
cp target/HumanResourceOfficial-1.0-SNAPSHOT.jar app.jar
```

### 2. 创建上传包
```bash
# 创建部署目录
mkdir hrofficial-deploy
cd hrofficial-deploy

# 复制必要文件
cp ../app.jar .
cp ../Dockerfile .
cp ../docker-compose.yml .
cp ../.env .
cp ../init.sql .
cp ../deploy.sh .

# 设置脚本执行权限（如果在Linux环境）
chmod +x deploy.sh
```

## 📤 文件上传方式

### 方式一：SCP上传（推荐）
```bash
# 压缩文件包
tar -czf hrofficial-deploy.tar.gz hrofficial-deploy/

# 上传到服务器
scp hrofficial-deploy.tar.gz root@your_server_ip:/opt/

# 在服务器解压
ssh root@your_server_ip
cd /opt
tar -xzf hrofficial-deploy.tar.gz
mv hrofficial-deploy hrofficial
cd hrofficial
```

### 方式二：SFTP工具上传
使用FileZilla、WinSCP等工具：
1. 连接服务器
2. 创建目录：`/opt/hrofficial`
3. 上传所有6个文件到该目录
4. 设置deploy.sh执行权限：`chmod +x deploy.sh`

### 方式三：手动复制粘贴（小文件）
对于配置文件，可以直接复制内容：
```bash
# 在服务器上创建文件
nano /opt/hrofficial/.env
# 粘贴.env文件内容

nano /opt/hrofficial/init.sql
# 粘贴init.sql文件内容
```

## 🚀 服务器部署步骤

### 1. 连接服务器
```bash
ssh root@your_server_ip
```

### 2. 进入部署目录
```bash
cd /opt/hrofficial
```

### 3. 验证文件完整性
```bash
ls -la
# 应该看到：app.jar, Dockerfile, docker-compose.yml, .env, init.sql, deploy.sh

# 检查JAR包大小
ls -lh app.jar
```

### 4. 执行部署
```bash
./deploy.sh
```

## 🔍 部署验证

### 自动验证项目
部署脚本会自动检查：
- ✅ 必要文件是否存在
- ✅ Docker环境是否正常
- ✅ JAR包是否有效
- ✅ 服务是否成功启动
- ✅ MySQL连接是否正常
- ✅ 应用接口是否响应

### 手动验证
```bash
# 检查容器状态
docker-compose ps

# 查看应用日志
docker-compose logs backend

# 测试应用访问
curl http://localhost:8080

# 测试数据库连接
docker exec -it hrofficial-mysql mysql -u hruser -p
```

## 🔒 安全优势

### 1. 源码保护
- ✅ 服务器上不存储源代码
- ✅ 编译后的JAR包难以反编译
- ✅ 降低代码泄露风险

### 2. 部署简化
- ✅ 文件数量少，上传快速
- ✅ 无需服务器端编译环境
- ✅ 部署流程标准化

### 3. 环境隔离
- ✅ 开发环境与生产环境完全分离
- ✅ 避免依赖版本冲突
- ✅ 容器化隔离运行

## ⚠️ 注意事项

### 文件权限
```bash
# 确保deploy.sh有执行权限
chmod +x deploy.sh

# 确保目录权限正确
chmod 755 /opt/hrofficial
```

### 环境变量
检查.env文件中的配置：
```bash
cat .env
# 确保数据库密码等配置正确
```

### 防火墙设置
```bash
# 开放必要端口
sudo ufw allow 8080
sudo ufw allow 3306  # 如需外部访问数据库
```

## 🔄 更新部署

当需要更新应用时：
1. 在本机重新编译JAR包
2. 上传新的app.jar到服务器
3. 重新执行部署脚本：`./deploy.sh`

脚本会自动：
- 停止旧容器
- 使用新JAR包重新构建镜像
- 启动新服务

## 🆘 故障排除

### 常见问题
1. **文件缺失**：确保所有6个文件都已上传
2. **权限问题**：检查deploy.sh是否有执行权限
3. **端口冲突**：确保8080和3306端口未被占用
4. **JAR包损坏**：重新编译并上传JAR包

### 日志查看
```bash
# 查看部署日志
docker-compose logs

# 查看特定服务日志
docker-compose logs backend
docker-compose logs mysql
```

---

**✅ 通过这种JAR包部署模式，您可以安全、快速地将应用部署到生产服务器，同时保护源代码安全！**