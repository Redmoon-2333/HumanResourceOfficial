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
 * RAG检索服务（精简版）
 *
 * 使用 Spring AI VectorStore 进行向量检索
 * Warning: Redis Vector Store 返回的相似度分数可能与 Qdrant 有差异
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
     */
    public List<RetrievedDocument> retrieve(String query) {
        return retrieve(query, ragConfig.getRetrievalTopK(), ragConfig.getScoreThreshold());
    }

    /**
     * 检索相关文档（自定义参数）
     */
    public List<RetrievedDocument> retrieve(String query, int topK, double scoreThreshold) {
        long startTime = System.currentTimeMillis();
        List<RetrievedDocument> documents = new ArrayList<>();

        try {
            logger.debug("开始检索: query={}, topK={}, threshold={}", query, topK, scoreThreshold);

            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(scoreThreshold)
                    .build();

            List<Document> results = vectorStore.similaritySearch(request);

            if (results == null || results.isEmpty()) {
                logger.warn("未检索到任何文档: {}", query);
                return documents;
            }

            for (Document doc : results) {
                RetrievedDocument retrieved = new RetrievedDocument();
                retrieved.setContent(doc.getText());
                retrieved.setFileName(getMetadata(doc, "file_name", "未知"));
                retrieved.setScore(getScore(doc));
                documents.add(retrieved);
            }

            logger.info("检索完成，找到 {} 个文档，耗时 {}ms", documents.size(), System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            logger.error("检索失败，耗时 {}ms", System.currentTimeMillis() - startTime, e);
            throw new RuntimeException("检索失败: " + e.getMessage(), e);
        }

        return documents;
    }

    /**
     * 测试检索（无阈值）
     */
    public List<RetrievedDocument> retrieveWithoutThreshold(String query, int topK) {
        List<RetrievedDocument> documents = new ArrayList<>();

        try {
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .build();

            List<Document> results = vectorStore.similaritySearch(request);

            if (results != null) {
                for (Document doc : results) {
                    RetrievedDocument retrieved = new RetrievedDocument();
                    retrieved.setContent(doc.getText());
                    retrieved.setFileName(getMetadata(doc, "file_name", "未知"));
                    retrieved.setScore(1.0f);
                    documents.add(retrieved);
                }
            }

        } catch (Exception e) {
            logger.error("测试检索失败", e);
        }

        return documents;
    }

    /**
     * 将检索结果格式化为上下文文本
     */
    public String formatContext(List<RetrievedDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder("以下是相关知识库内容：\n\n");
        for (int i = 0; i < documents.size(); i++) {
            RetrievedDocument doc = documents.get(i);
            context.append(String.format("[来源%d: %s]\n%s\n\n", i + 1, doc.getFileName(), doc.getContent()));
        }
        return context.toString();
    }

    // ==================== 私有辅助方法 ====================

    private String getMetadata(Document doc, String key, String defaultValue) {
        Object value = doc.getMetadata().get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private float getScore(Document doc) {
        Object score = doc.getMetadata().get("score");
        if (score != null) return ((Number) score).floatValue();

        Object distance = doc.getMetadata().get("distance");
        if (distance != null) return 1.0f - ((Number) distance).floatValue();

        Object similarity = doc.getMetadata().get("similarity");
        if (similarity != null) return ((Number) similarity).floatValue();

        return 1.0f;
    }
}
