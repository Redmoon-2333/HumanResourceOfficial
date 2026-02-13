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

/**
 * VectorStore 配置类（精简版）
 *
 * 使用 Spring AI Redis Vector Store 进行向量存储
 * Warning: 需要 Redis 服务端支持 RediSearch 和 RedisJSON 模块
 */
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

    @Bean
    public JedisPooled jedisPooled() {
        return new JedisPooled(redisHost, redisPort);
    }

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel, JedisPooled jedis) {
        logger.info("初始化 Redis Vector Store: {}:{}, index: {}", redisHost, redisPort, indexName);

        // Why: 测试Embedding模型实际输出维度，确保与Redis索引维度一致
        // Warning: 如果维度不匹配，必须删除Redis索引重建
        testEmbeddingDimensions(embeddingModel);

        return RedisVectorStore.builder(jedis, embeddingModel)
                .indexName(indexName)
                .prefix(prefix)
                .initializeSchema(true)
                .build();
    }

    /**
     * 应用启动后自动初始化Redis向量索引
     * 
     * Why: initializeSchema(true)只有在添加数据时才会创建索引
     *      如果索引不存在，查询时会报"No such index"错误
     *      通过插入并删除一个测试文档来触发索引创建
     */
    @EventListener(ApplicationReadyEvent.class)
    public void ensureIndexExists(ApplicationReadyEvent event) {
        try {
            VectorStore vs = event.getApplicationContext().getBean(VectorStore.class);
            JedisPooled jedis = event.getApplicationContext().getBean(JedisPooled.class);
            
            // 检查索引是否存在
            try {
                jedis.ftInfo(indexName);
                logger.info("Redis向量索引 {} 已存在", indexName);
            } catch (Exception e) {
                // 索引不存在，通过插入测试文档触发创建
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

    /**
     * 测试Embedding模型实际输出维度
     *
     * Why: 确保Embedding模型配置的dimensions参数实际生效
     *      避免配置与实际输出维度不一致导致的查询错误
     *
     * @param embeddingModel Embedding模型
     */
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
