package com.redmoon2333.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * RAG功能配置类
 * 包含文档处理、向量化、检索等相关配置参数
 */
@Configuration
@ConfigurationProperties(prefix = "rag")
@Data
public class RagConfig {
    
    /**
     * 知识库文件目录路径
     */
    private String knowledgeBasePath = "src/main/resources/rag-knowledge-base";
    
    /**
     * 文本分块大小（字符数）
     */
    private int chunkSize = 800;
    
    /**
     * 分块重叠大小（字符数）
     */
    private int chunkOverlap = 100;
    
    /**
     * Embedding模型名称
     */
    private String embeddingModel = "text-embedding-v3";
    
    /**
     * 检索时返回的最相关文档数量
     */
    private int retrievalTopK = 5;
    
    /**
     * 相似度阈值(0-1之间)
     * 通义千问Embedding模型建议设置为0.1-0.2，或设为0不过滤
     * 注意: Qdrant返回的是distance(距离)，需要转换为similarity
     */
    private double scoreThreshold = 0.0;
    
    /**
     * 向量维度（通义千问embedding-v3为1536维）
     */
    private int vectorDimension = 1536;
    
    /**
     * 是否启用批处理模式
     */
    private boolean enableBatchProcessing = true;
    
    /**
     * 批处理大小
     */
    private int batchSize = 25;
}
