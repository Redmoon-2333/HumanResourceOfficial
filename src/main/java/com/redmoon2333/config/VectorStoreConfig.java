package com.redmoon2333.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.CreateCollection;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Collections.VectorsConfig;
import jakarta.annotation.PostConstruct;

/**
 * VectorStore 配置类
 * 参考 Spring AI Alibaba 文档的最佳实践
 */
@Configuration
public class VectorStoreConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(VectorStoreConfig.class);
    
    @Value("${qdrant.collection-name:campus_knowledge}")
    private String collectionName;
    
    @Value("${spring.ai.vectorstore.qdrant.initialize-schema:true}")
    private boolean initializeSchema;
    
    @Autowired
    private RagConfig ragConfig;
    
    @Autowired
    private QdrantClient qdrantClient;
    
    /**
     * 应用启动后初始化Collection
     * 确保Collection存在，避免NOT_FOUND错误
     */
    @PostConstruct
    public void initializeCollection() {
        try {
            logger.info("检查Collection是否存在: {}", collectionName);
            
            boolean exists = qdrantClient.collectionExistsAsync(collectionName).get();
            
            if (!exists) {
                logger.warn("Collection {} 不存在，开始创建...", collectionName);
                
                VectorParams vectorParams = VectorParams.newBuilder()
                    .setSize(ragConfig.getVectorDimension())
                    .setDistance(Distance.Cosine)
                    .build();
                
                CreateCollection createCollection = CreateCollection.newBuilder()
                    .setCollectionName(collectionName)
                    .setVectorsConfig(VectorsConfig.newBuilder()
                        .setParams(vectorParams)
                        .build())
                    .build();
                
                qdrantClient.createCollectionAsync(createCollection).get();
                logger.info("Collection {} 创建成功！", collectionName);
            } else {
                logger.info("Collection {} 已存在", collectionName);
            }
            
        } catch (Exception e) {
            logger.error("Collection初始化失败", e);
            throw new RuntimeException("Collection初始化失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 配置 Embedding 模型
     * 参考文档: SAA-11Embed2vector
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        String apiKey = System.getenv("aliQwen_api");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("通义千问API Key未配置，请设置环境变量 aliQwen_api");
        }
        
        logger.info("初始化 Embedding 模型: text-embedding-v3");
        
        return new DashScopeEmbeddingModel(
            DashScopeApi.builder().apiKey(apiKey).build()
        );
    }
    
    /**
     * 注意: qdrantClient bean 由 QdrantConfig 提供
     * 此处不再重复定义，避免 bean 冲突
     */
    
    /**
     * 配置 VectorStore
     * 使用Builder模式创建
     */
    @Bean
    public VectorStore vectorStore(QdrantClient qdrantClient, EmbeddingModel embeddingModel) {
        logger.info("初始化 VectorStore: collection={}, initSchema={}", 
                   collectionName, initializeSchema);
        
        try {
            // 使用Builder模式 - 传入qdrantClient和embeddingModel
            return QdrantVectorStore.builder(qdrantClient, embeddingModel)
                    .collectionName(collectionName)
                    .initializeSchema(initializeSchema)
                    .build();
        } catch (Exception e) {
            logger.error("VectorStore初始化失败", e);
            throw new RuntimeException("VectorStore初始化失败: " + e.getMessage(), e);
        }
    }
}
