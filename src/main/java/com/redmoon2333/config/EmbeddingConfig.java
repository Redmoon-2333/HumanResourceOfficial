package com.redmoon2333.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Embedding模型配置类
 *
 * Why: Spring AI 1.1.x 需要显式配置Embedding模型
 * 确保知识库构建和查询使用相同的Embedding模型，避免向量维度不匹配
 */
@Slf4j
@Configuration
public class EmbeddingConfig {

    @Value("${aliyun.dashscope.api-key}")
    private String apiKey;

    @Value("${rag.embedding-model:text-embedding-v3}")
    private String embeddingModelName;

    @Value("${rag.embedding-dimensions:1024}")
    private Integer embeddingDimensions;

    /**
     * 配置DashScope Embedding模型
     *
     * Why: 显式指定dimensions参数确保向量维度一致性
     *      通义千问text-embedding-v3支持多种输出维度：1024, 1536(默认), 2048
     *      必须与Redis Vector Store索引维度完全匹配
     *
     * Warning: 修改此配置后必须清空Redis索引重建，否则会出现维度不匹配错误
     *          参考错误：query vector blob size (4096) does not match index's expected size (6144)
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        log.info("初始化 DashScope Embedding模型: model={}, dimensions={}", embeddingModelName, embeddingDimensions);

        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(apiKey)
                .build();

        // Spring AI Alibaba 1.0.x 使用构造函数创建EmbeddingModel
        // 显式设置dimensions确保向量维度可控
        DashScopeEmbeddingOptions options = DashScopeEmbeddingOptions.builder()
                .withModel(embeddingModelName)
                .withDimensions(embeddingDimensions)
                .build();

        return new DashScopeEmbeddingModel(dashScopeApi, MetadataMode.EMBED, options);
    }
}
