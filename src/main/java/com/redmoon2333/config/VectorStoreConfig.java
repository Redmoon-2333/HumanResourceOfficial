package com.redmoon2333.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
