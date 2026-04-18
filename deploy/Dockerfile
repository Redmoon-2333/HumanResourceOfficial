FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/HumanResourceOfficial-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080

# 支持从外部配置目录加载配置文件
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.config.additional-location=optional:file:/app/config/"]
