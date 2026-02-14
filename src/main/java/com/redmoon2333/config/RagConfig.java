package com.redmoon2333.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * RAG功能配置类
 * 包含文档处理、向量化、检索等相关配置参数
 * 
 * 迁移说明：
 * - 移除了Qdrant相关配置
 * - Redis Vector Store配置通过 spring.ai.vectorstore.redis.* 配置
 */
@Configuration
@ConfigurationProperties(prefix = "rag")
@Data
public class RagConfig {
    
    /**
     * 知识库文件目录路径
     * 支持通过环境变量 RAG_KNOWLEDGE_BASE_PATH 配置
     */
    private String knowledgeBasePath = System.getenv("RAG_KNOWLEDGE_BASE_PATH") != null 
        ? System.getenv("RAG_KNOWLEDGE_BASE_PATH") 
        : "src/main/resources/rag-knowledge-base";
    
    /**
     * 文本分块大小（字符数）
     * 减小分块可以提高关键词密度，改善检索效果
     * 400字符适合实体名称查询（如"秋林阁"、"图书馆"等）
     */
    private int chunkSize = 400;
    
    /**
     * 分块重叠大小（字符数）
     * 设置为分块大小的25-30%，确保关键信息不被截断
     */
    private int chunkOverlap = 100;
    
    /**
     * 最小分块大小（字符数）
     * 过小的分块会被合并，避免过度碎片化
     * 建议设置为chunkSize的30%左右
     */
    private int minChunkSize = 120;
    
    /**
     * Embedding模型名称
     */
    private String embeddingModel = "text-embedding-v3";
    
    /**
     * 检索时返回的最相关文档数量
     * 建议设置为3-5，减少内存使用和提高检索效率
     */
    private int retrievalTopK = 5;
    
    /**
     * 相似度阈值(0-1之间)
     * 通义千问Embedding模型建议设置为0.1-0.2，或设为0不过滤
     * Warning: Redis Vector Store的相似度计算方式与Qdrant可能不同
     */
    private double scoreThreshold = 0.0;
    
    /**
     * 向量维度（通义千问embedding-v3为1536维）
     * Warning: 此值必须与Embedding模型输出维度一致
     */
    private int vectorDimension = 1536;
    
    /**
     * 是否启用批处理模式
     */
    private boolean enableBatchProcessing = true;
    
    /**
     * 批处理大小
     * Warning: 低配服务器建议设置为5-10，防止OOM
     */
    private int batchSize = 25;
    
    // ============================================================
    // 智能分块配置
    // ============================================================
    
    /**
     * 是否启用智能语义分块
     * 启用后会按章节、段落、句子边界进行智能分割
     * 提高分块的语义完整性，改善RAG检索效果
     */
    private boolean enableSemanticChunking = true;
    
    /**
     * 是否自动识别文档类型
     * 启用后会根据文档内容自动选择最佳分块策略
     */
    private boolean autoDetectDocType = true;
    
    // ============================================================
    // 低内存模式配置（适用于2核2G等低配服务器）
    // ============================================================
    
    /**
     * 是否启用低内存模式
     * 启用后会减小批处理大小、增加处理间隔、主动GC
     * Why: 2核2G服务器在RAG初始化时容易因内存不足而死机
     */
    private boolean lowMemoryMode = false;
    
    /**
     * 内存使用率警告阈值（0.0 - 1.0）
     * 超过此阈值时会触发GC
     */
    private double memoryWarningThreshold = 0.7;
    
    /**
     * 内存使用率危险阈值（0.0 - 1.0）
     * 超过此阈值时会暂停处理等待内存释放
     */
    private double memoryCriticalThreshold = 0.85;
    
    /**
     * 文件处理间隔（毫秒）
     * 每处理完一个文件后的等待时间，用于降低CPU和内存峰值
     */
    private long fileProcessDelayMs = 0;
    
    /**
     * 单个文件大小限制（MB）
     * 超过此大小的文件将被跳过，防止解析大文件时OOM
     * 设置为0表示不限制
     */
    private long maxFileSizeMB = 0;
    
    /**
     * 低内存模式下的批处理大小
     * 当启用lowMemoryMode时，会覆盖batchSize
     */
    private int lowMemoryBatchSize = 5;
    
    /**
     * 低内存模式下的文件处理间隔（毫秒）
     */
    private long lowMemoryFileDelayMs = 500;
    
    /**
     * 获取实际使用的批处理大小
     * 根据是否启用低内存模式返回对应值
     */
    public int getEffectiveBatchSize() {
        return lowMemoryMode ? lowMemoryBatchSize : batchSize;
    }
    
    /**
     * 获取实际使用的文件处理间隔
     * 根据是否启用低内存模式返回对应值
     */
    public long getEffectiveFileDelayMs() {
        return lowMemoryMode ? lowMemoryFileDelayMs : fileProcessDelayMs;
    }
}
