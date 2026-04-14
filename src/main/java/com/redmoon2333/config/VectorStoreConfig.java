package com.redmoon2333.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import redis.clients.jedis.JedisPooled;

import java.util.List;
import java.util.Map;

@Configuration
public class VectorStoreConfig {

    private static final Logger logger = LoggerFactory.getLogger(VectorStoreConfig.class);

    @Value("${spring.ai.vectorstore.redis.index-name:spring-ai-index}")
    private String indexName;

    @Value("${spring.ai.vectorstore.redis.prefix:embedding:}")
    private String prefix;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${rag.embedding-dimensions:1024}")
    private int embeddingDimensions;

    @Bean
    public JedisPooled jedisPooled() {
        return new JedisPooled(redisHost, redisPort);
    }

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel, JedisPooled jedis) {
        logger.info("初始化 Redis Vector Store: {}:{}, index: {}", redisHost, redisPort, indexName);

        testEmbeddingDimensions(embeddingModel);

        try {
            return RedisVectorStore.builder(jedis, embeddingModel)
                    .indexName(indexName)
                    .prefix(prefix)
                    .initializeSchema(true)
                    .build();
        } catch (Exception e) {
            logger.warn("RedisVectorStore 自动初始化失败（Embedding API 不可用），使用延迟初始化: {}", e.getMessage());
            return RedisVectorStore.builder(jedis, embeddingModel)
                    .indexName(indexName)
                    .prefix(prefix)
                    .initializeSchema(false)
                    .build();
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureIndexExists(ApplicationReadyEvent event) {
        try {
            VectorStore vs = event.getApplicationContext().getBean(VectorStore.class);
            JedisPooled jedis = event.getApplicationContext().getBean(JedisPooled.class);

            try {
                jedis.ftInfo(indexName);
                logger.info("Redis向量索引 {} 已存在", indexName);
            } catch (Exception e) {
                logger.info("Redis向量索引不存在，正在创建...");
                String testDocId = "__init_test_doc__";
                Document testDoc = new Document(testDocId, "初始化测试文档", Map.of("type", "init"));
                vs.add(List.of(testDoc));
                vs.delete(List.of(testDocId));
                logger.info("Redis向量索引 {} 创建成功", indexName);
            }
        } catch (Exception e) {
            logger.warn("检查/创建Redis向量索引失败: {}", e.getMessage());
        }
    }

    private void testEmbeddingDimensions(EmbeddingModel embeddingModel) {
        try {
            float[] embedding = embeddingModel.embed("测试文本");
            logger.info("Embedding模型实际输出维度: {}", embedding.length);
        } catch (Exception e) {
            logger.warn("无法测试Embedding模型维度: {}", e.getMessage());
        }
    }

    public String getIndexName() {
        return indexName;
    }

    public String getPrefix() {
        return prefix;
    }
}
