package com.redmoon2333.service;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import com.redmoon2333.config.RagConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 向量化服务
 * 负责将文本转换为向量表示
 */
@Service
public class EmbeddingService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);
    
    @Autowired
    private RagConfig ragConfig;
    
    private DashScopeEmbeddingModel embeddingModel;
    
    /**
     * 初始化Embedding模型
     */
    private void initEmbeddingModel() {
        if (embeddingModel == null) {
            String apiKey = System.getenv("aliQwen_api");
            if (apiKey == null || apiKey.isEmpty()) {
                throw new RuntimeException("通义千问API Key未配置");
            }
            
            embeddingModel = new DashScopeEmbeddingModel(
                DashScopeApi.builder().apiKey(apiKey).build(),
                DashScopeEmbeddingOptions.builder()
                    .withModel(ragConfig.getEmbeddingModel())
                    .build()
            );
            
            logger.info("Embedding模型初始化成功，模型: {}", ragConfig.getEmbeddingModel());
        }
    }
    
    /**
     * 将单个文本向量化
     * 
     * @param text 文本内容
     * @return 向量数组
     */
    public float[] embedText(String text) {
        initEmbeddingModel();
        
        try {
            logger.debug("开始向量化文本，长度: {} 字符", text.length());
            
            EmbeddingRequest request = new EmbeddingRequest(List.of(text), null);
            EmbeddingResponse response = embeddingModel.call(request);
            
            List<Double> embedding = response.getResults().get(0).getOutput();
            float[] vector = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vector[i] = embedding.get(i).floatValue();
            }
            
            logger.debug("文本向量化完成，向量维度: {}", vector.length);
            return vector;
            
        } catch (Exception e) {
            logger.error("文本向量化失败", e);
            throw new RuntimeException("向量化失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 批量向量化文本
     * 
     * @param texts 文本列表
     * @return 向量列表
     */
    public List<float[]> embedBatch(List<String> texts) {
        initEmbeddingModel();
        
        List<float[]> vectors = new ArrayList<>();
        
        if (!ragConfig.isEnableBatchProcessing() || texts.size() <= ragConfig.getBatchSize()) {
            // 如果文本数量不超过批处理大小，一次性处理
            try {
                logger.debug("批量向量化 {} 个文本", texts.size());
                
                EmbeddingRequest request = new EmbeddingRequest(texts, null);
                EmbeddingResponse response = embeddingModel.call(request);
                
                response.getResults().forEach(result -> {
                    List<Double> embedding = result.getOutput();
                    float[] vector = new float[embedding.size()];
                    for (int i = 0; i < embedding.size(); i++) {
                        vector[i] = embedding.get(i).floatValue();
                    }
                    vectors.add(vector);
                });
                
                logger.debug("批量向量化完成，共 {} 个向量", vectors.size());
                
            } catch (Exception e) {
                logger.error("批量向量化失败", e);
                throw new RuntimeException("批量向量化失败: " + e.getMessage(), e);
            }
        } else {
            // 分批处理
            int batchSize = ragConfig.getBatchSize();
            for (int i = 0; i < texts.size(); i += batchSize) {
                int end = Math.min(i + batchSize, texts.size());
                List<String> batch = texts.subList(i, end);
                vectors.addAll(embedBatch(batch));
            }
        }
        
        return vectors;
    }
}
