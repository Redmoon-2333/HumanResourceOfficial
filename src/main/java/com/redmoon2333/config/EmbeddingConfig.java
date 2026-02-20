package com.redmoon2333.config;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Embedding模型配置类（ECNU重构版）
 *
 * Why: 使用ECNU提供的ecnu-embedding-small模型
 *      通过OpenAI兼容接口调用，与Chat模型共用同一个API Key
 *
 * ECNU Embedding模型:
 * - ecnu-embedding-small: 1024维向量输出
 *
 * Warning: 修改此配置后必须清空Redis索引重建，否则会出现维度不匹配错误
 */
@Configuration
public class EmbeddingConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${rag.embedding-model:ecnu-embedding-small}")
    private String embeddingModelName;

    @Value("${rag.embedding-dimensions:1024}")
    private Integer embeddingDimensions;

    @Bean
    public OpenAiApi openAiApi() {
        return OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel(OpenAiApi openAiApi) {
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(embeddingModelName)
                .dimensions(embeddingDimensions)
                .build();

        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, options);
    }
}
