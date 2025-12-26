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
                DashScopeApi.builder().apiKey(apiKey).build()
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
            
            // 使用直接指定模型的方式
            EmbeddingResponse embeddingResponse = embeddingModel.call(
                new EmbeddingRequest(
                    List.of(text),
                    DashScopeEmbeddingOptions.builder()
                        .withModel(ragConfig.getEmbeddingModel())
                        .build()
                )
            );
            
            logger.debug("收到Embedding响应");
            
            float[] embedding = embeddingResponse.getResults().get(0).getOutput();
            
            // 立即释放embeddingResponse
            embeddingResponse = null;
            
            logger.debug("文本向量化完成，向量维度: {}", embedding.length);
            
            return embedding;
            
        } catch (OutOfMemoryError oom) {
            logger.error("向量化时内存不足", oom);
            System.gc();
            throw new RuntimeException("向量化失败：内存不足", oom);
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
        
        if (texts == null || texts.isEmpty()) {
            return vectors;
        }
        
        try {
            // 分批处理，每批最多5个文本（降低单次API调用内存占用）
            int batchSize = 5;
            
            for (int i = 0; i < texts.size(); i += batchSize) {
                int end = Math.min(i + batchSize, texts.size());
                List<String> batch = texts.subList(i, end);
                
                logger.debug("批量向量化 {}-{}/{} 个文本", i+1, end, texts.size());
                
                try {
                    EmbeddingResponse embeddingResponse = embeddingModel.call(
                        new EmbeddingRequest(
                            batch,
                            DashScopeEmbeddingOptions.builder()
                                .withModel(ragConfig.getEmbeddingModel())
                                .build()
                        )
                    );
                    
                    // 提取向量
                    for (int j = 0; j < embeddingResponse.getResults().size(); j++) {
                        float[] embedding = embeddingResponse.getResults().get(j).getOutput();
                        vectors.add(embedding);
                    }
                    
                    // 释放embeddingResponse
                    embeddingResponse = null;
                    
                    logger.debug("批次 {} 向量化完成", (i/batchSize + 1));
                    
                    // 每批次后稍微延迟，给GC机会
                    if (i + batchSize < texts.size()) {
                        Thread.sleep(50);
                    }
                    
                } catch (OutOfMemoryError oom) {
                    logger.error("批量向量化时内存不足，尝试单文本处理", oom);
                    System.gc();
                    Thread.sleep(200);
                    
                    // 降级为单文本处理
                    for (String text : batch) {
                        try {
                            float[] vector = embedText(text);
                            vectors.add(vector);
                            Thread.sleep(100); // 给更多时间恢复
                        } catch (Exception e) {
                            logger.error("单文本向量化失败", e);
                            throw new RuntimeException("向量化失败: " + e.getMessage(), e);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("向量化被中断", e);
                }
            }
            
            logger.debug("批量向量化完成，共 {} 个向量", vectors.size());
            
        } catch (Exception e) {
            logger.error("批量向量化失败", e);
            throw new RuntimeException("批量向量化失败: " + e.getMessage(), e);
        }
        
        return vectors;
    }
}
