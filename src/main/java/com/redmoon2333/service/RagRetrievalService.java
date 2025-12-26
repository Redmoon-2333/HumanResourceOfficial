package com.redmoon2333.service;

import com.redmoon2333.config.RagConfig;
import com.redmoon2333.dto.RetrievedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG检索服务
 * 参考文档: SAA-12RAG4AiOps
 */
@Service
public class RagRetrievalService {
    
    private static final Logger logger = LoggerFactory.getLogger(RagRetrievalService.class);
    
    @Autowired
    private RagConfig ragConfig;
    
    @Autowired
    private VectorStore vectorStore;
    
    /**
     * 检索相关文档
     * 使用 Spring AI VectorStore
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
            
            // 构建检索请求 - 参考 SAA-11Embed2vector
            SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(scoreThreshold)
                .build();
            
            logger.debug("调用 VectorStore.similaritySearch(), VectorStore类型: {}", 
                        vectorStore.getClass().getName());
            
            List<Document> results = vectorStore.similaritySearch(searchRequest);
            
            logger.debug("原始检索结果数量: {}", results != null ? results.size() : "null");
            
            if (results == null || results.isEmpty()) {
                logger.warn("未检索到任何文档！查询: {}, topK: {}, scoreThreshold: {}", 
                           query, topK, scoreThreshold);
                return documents;
            }
            
            // 转换为 RetrievedDocument
            for (Document doc : results) {
                RetrievedDocument retrievedDoc = new RetrievedDocument();
                retrievedDoc.setContent(doc.getText());
                
                // 从元数据中提取文件名
                Object fileName = doc.getMetadata().get("file_name");
                retrievedDoc.setFileName(fileName != null ? fileName.toString() : "未知");
                
                // 从元数据中提取score(如果有)
                Object scoreObj = doc.getMetadata().get("score");
                float score = scoreObj != null ? ((Number) scoreObj).floatValue() : 1.0f;
                retrievedDoc.setScore(score);
                
                documents.add(retrievedDoc);
                logger.debug("检索到文档: {}, 分数: {}", retrievedDoc.getFileName(), score);
            }
            
            logger.info("检索完成，共找到 {} 个相关文档", documents.size());
            
        } catch (Exception e) {
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
     * 测试方法: 尝试不设置相似度阈值来检索
     * 用于调试和验证数据库中是否有数据
     */
    public List<RetrievedDocument> retrieveWithoutThreshold(String query, int topK) {
        List<RetrievedDocument> documents = new ArrayList<>();
        
        try {
            logger.info("测试检索(无阈值)，查询: {}, topK: {}", query, topK);
            
            // 不设置 similarityThreshold
            SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build();
            
            List<Document> results = vectorStore.similaritySearch(searchRequest);
            
            logger.info("测试检索结果数量: {}", results != null ? results.size() : "null");
            
            if (results != null) {
                for (Document doc : results) {
                    RetrievedDocument retrievedDoc = new RetrievedDocument();
                    retrievedDoc.setContent(doc.getText());
                    Object fileName = doc.getMetadata().get("file_name");
                    retrievedDoc.setFileName(fileName != null ? fileName.toString() : "未知");
                    retrievedDoc.setScore(1.0f);
                    documents.add(retrievedDoc);
                    logger.info("检索到文档: {}, metadata: {}", 
                               retrievedDoc.getFileName(), doc.getMetadata());
                }
            }
            
        } catch (Exception e) {
            logger.error("测试检索失败", e);
        }
        
        return documents;
    }
}
