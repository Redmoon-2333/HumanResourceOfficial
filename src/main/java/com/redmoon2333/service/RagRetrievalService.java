package com.redmoon2333.service;

import com.redmoon2333.config.QdrantConfig;
import com.redmoon2333.config.RagConfig;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * RAG检索服务
 * 负责从向量数据库中检索相关文档
 */
@Service
public class RagRetrievalService {
    
    private static final Logger logger = LoggerFactory.getLogger(RagRetrievalService.class);
    
    @Autowired
    private QdrantClient qdrantClient;
    
    @Autowired
    private QdrantConfig qdrantConfig;
    
    @Autowired
    private RagConfig ragConfig;
    
    @Autowired
    private EmbeddingService embeddingService;
    
    /**
     * 检索相关文档
     * 
     * @param query 查询文本
     * @return 相关文档列表
     */
    public List<RetrievedDocument> retrieve(String query) {
        return retrieve(query, ragConfig.getRetrievalTopK(), ragConfig.getScoreThreshold());
    }
    
    /**
     * 检索相关文档（自定义参数）
     * 
     * @param query 查询文本
     * @param topK 返回文档数量
     * @param scoreThreshold 相似度阈值
     * @return 相关文档列表
     */
    public List<RetrievedDocument> retrieve(String query, int topK, double scoreThreshold) {
        List<RetrievedDocument> documents = new ArrayList<>();
        
        try {
            logger.debug("开始检索，查询: {}, topK: {}, 阈值: {}", query, topK, scoreThreshold);
            
            // 1. 将查询文本向量化
            float[] queryVector = embeddingService.embedText(query);
            
            // 2. 构建搜索请求
            List<Float> vectorList = new ArrayList<>();
            for (float v : queryVector) {
                vectorList.add(v);
            }
            
            SearchPoints searchPoints = SearchPoints.newBuilder()
                .setCollectionName(qdrantConfig.getCollectionName())
                .addVector(vectorList)
                .setLimit(topK)
                .setScoreThreshold((float) scoreThreshold)
                .setWithPayload(WithPayloadSelector.newBuilder()
                    .setEnable(true)
                    .build())
                .build();
            
            // 3. 执行搜索
            List<ScoredPoint> results = qdrantClient.searchAsync(searchPoints).get();
            
            // 4. 处理结果
            for (ScoredPoint point : results) {
                RetrievedDocument doc = new RetrievedDocument();
                doc.setScore(point.getScore());
                
                // 提取payload
                if (point.getPayloadMap().containsKey("content")) {
                    doc.setContent(point.getPayloadMap().get("content").getStringValue());
                }
                if (point.getPayloadMap().containsKey("file_name")) {
                    doc.setFileName(point.getPayloadMap().get("file_name").getStringValue());
                }
                if (point.getPayloadMap().containsKey("category")) {
                    doc.setCategory(point.getPayloadMap().get("category").getStringValue());
                }
                if (point.getPayloadMap().containsKey("chunk_index")) {
                    doc.setChunkIndex((int) point.getPayloadMap().get("chunk_index").getIntegerValue());
                }
                
                documents.add(doc);
                logger.trace("检索到文档: {} (得分: {})", doc.getFileName(), doc.getScore());
            }
            
            logger.info("检索完成，共找到 {} 个相关文档", documents.size());
            
        } catch (ExecutionException | InterruptedException e) {
            logger.error("检索失败", e);
            throw new RuntimeException("检索失败: " + e.getMessage(), e);
        }
        
        return documents;
    }
    
    /**
     * 将检索到的文档格式化为上下文文本
     * 
     * @param documents 文档列表
     * @return 格式化的上下文
     */
    public String formatContext(List<RetrievedDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return "";
        }
        
        StringBuilder context = new StringBuilder();
        context.append("以下是相关知识库内容：\n\n");
        
        for (int i = 0; i < documents.size(); i++) {
            RetrievedDocument doc = documents.get(i);
            context.append(String.format("[来源%d: %s]\n", i + 1, doc.getFileName()));
            context.append(doc.getContent());
            context.append("\n\n");
        }
        
        return context.toString();
    }
    
    /**
     * 检索到的文档实体类
     */
    public static class RetrievedDocument {
        private String content;
        private String fileName;
        private String category;
        private int chunkIndex;
        private float score;
        
        // Getters and Setters
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public String getFileName() {
            return fileName;
        }
        
        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
        
        public String getCategory() {
            return category;
        }
        
        public void setCategory(String category) {
            this.category = category;
        }
        
        public int getChunkIndex() {
            return chunkIndex;
        }
        
        public void setChunkIndex(int chunkIndex) {
            this.chunkIndex = chunkIndex;
        }
        
        public float getScore() {
            return score;
        }
        
        public void setScore(float score) {
            this.score = score;
        }
    }
}
