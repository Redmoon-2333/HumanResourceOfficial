package com.redmoon2333.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Qdrant向量数据库配置类
 * 负责创建和管理Qdrant客户端连接
 */
@Configuration
public class QdrantConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(QdrantConfig.class);
    
    @Value("${qdrant.host:localhost}")
    private String qdrantHost;
    
    @Value("${qdrant.port:6334}")
    private int qdrantPort;
    
    @Value("${qdrant.api-key:}")
    private String apiKey;
    
    @Value("${qdrant.collection-name:campus_knowledge}")
    private String collectionName;
    
    @Value("${qdrant.use-tls:false}")
    private boolean useTls;
    
    /**
     * 创建Qdrant客户端Bean
     * 使用gRPC协议连接Qdrant服务
     * 
     * @return QdrantClient实例
     */
    @Bean
    public QdrantClient qdrantClient() {
        try {
            logger.info("正在初始化Qdrant客户端，连接地址: {}:{}", qdrantHost, qdrantPort);
            
            QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(qdrantHost, qdrantPort, useTls);
            
            // 如果配置了API Key，则使用认证
            if (apiKey != null && !apiKey.isEmpty()) {
                builder.withApiKey(apiKey);
                logger.info("Qdrant客户端启用API Key认证");
            }
            
            QdrantClient client = new QdrantClient(builder.build());
            
            logger.info("Qdrant客户端初始化成功，Collection名称: {}", collectionName);
            return client;
            
        } catch (Exception e) {
            logger.error("Qdrant客户端初始化失败", e);
            throw new RuntimeException("无法连接到Qdrant服务: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取Collection名称
     * 
     * @return Collection名称
     */
    public String getCollectionName() {
        return collectionName;
    }
}
