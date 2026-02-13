# ============================================================
# 人力资源中心官网 - 后端服务 Dockerfile
# 基于 OpenJDK 21，使用多阶段构建优化镜像大小
# ============================================================

# 构建阶段
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# 复制pom.xml并下载依赖（利用Docker缓存层）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码并构建
COPY src ./src
RUN mvn clean package -DskipTests -B

# 运行阶段 - 使用精简的JRE镜像
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="人力资源中心"
LABEL version="1.0"
LABEL description="人力资源中心官网后端服务"

# 设置时区
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/${TZ} /etc/localtime && \
    echo "${TZ}" > /etc/timezone && \
    apk del tzdata

# 创建非root用户运行应用
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# 从构建阶段复制jar包
COPY --from=builder /build/target/*.jar app.jar

# 创建日志和上传目录
RUN mkdir -p /app/logs /app/uploads && \
    chown -R appuser:appgroup /app

# 切换到非root用户
USER appuser

# 暴露端口
EXPOSE 8080

# JVM优化参数
ENV JAVA_OPTS="-Xms256m -Xmx512m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=100 \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom"

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
