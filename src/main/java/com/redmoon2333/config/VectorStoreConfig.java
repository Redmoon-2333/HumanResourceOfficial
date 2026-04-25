package com.redmoon2333.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.exceptions.JedisDataException;
import java.util.Collections;
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

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    private JedisPooled jedisPooledInstance;

    @Bean
    public JedisPooled jedisPooled() {
        // 如果有密码则使用密码，否则不使用
        if (redisPassword != null && !redisPassword.isEmpty()) {
            JedisClientConfig config = DefaultJedisClientConfig.builder()
                .password(redisPassword)
                .build();
            jedisPooledInstance = new JedisPooled(new HostAndPort(redisHost, redisPort), config);
        } else {
            jedisPooledInstance = new JedisPooled(new HostAndPort(redisHost, redisPort));
        }
        return jedisPooledInstance;
    }

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel, JedisPooled jedis) {
        logger.info("初始化 Redis Vector Store: {}:{}, index: {}", redisHost, redisPort, indexName);

        try {
            float[] embedding = embeddingModel.embed("测试文本");
            logger.info("Embedding模型实际输出维度: {}", embedding.length);
        } catch (Exception e) {
            logger.warn("无法测试Embedding模型维度: {}", e.getMessage());
        }

        return RedisVectorStore.builder(jedis, embeddingModel)
                .indexName(indexName)
                .prefix(prefix)
                .initializeSchema(true)
                .build();
    }

    /**
     * 应用启动后检查并重建向量索引
     * 原因: FLUSHALL 会删除索引但保留数据，需要启动时检测并重建
     */
    @EventListener(ApplicationReadyEvent.class)
    public void ensureIndexExists() {
        if (jedisPooledInstance == null) {
            logger.warn("JedisPooled 未初始化，跳过索引检查");
            return;
        }

        try {
            // 检查索引是否存在
            jedisPooledInstance.ftInfo(indexName);
            logger.info("向量索引 {} 已存在，无需重建", indexName);
        } catch (JedisDataException e) {
            if (e.getMessage() != null && e.getMessage().contains("Unknown index name")) {
                logger.warn("向量索引 {} 不存在，尝试重建...", indexName);
                try {
                    // 删除可能损坏的旧索引
                    try {
                        jedisPooledInstance.ftDropIndex(indexName);
                        logger.info("已删除旧索引 {}", indexName);
                    } catch (Exception ignored) {}

                    // 重建索引
                    jedisPooledInstance.ftCreate(
                        indexName,
                        redis.clients.jedis.search.FTCreateParams.createParams()
                            .prefix(prefix),
                        redis.clients.jedis.search.schemafields.TextField.of("content"),
                        new redis.clients.jedis.search.schemafields.VectorField("embedding",
                            redis.clients.jedis.search.schemafields.VectorField.VectorAlgorithm.HNSW,
                            Map.of(
                                "TYPE", "FLOAT32",
                                "DIM", "1024",
                                "DISTANCE_METRIC", "COSINE",
                                "M", "16",
                                "EF_CONSTRUCTION", "200",
                                "EF_RUNTIME", "10"
                            ))
                    );
                    logger.info("向量索引 {} 重建成功", indexName);
                } catch (Exception ex) {
                    logger.error("向量索引 {} 重建失败: {}", indexName, ex.getMessage(), ex);
                }
            }
        } catch (Exception e) {
            logger.warn("检查向量索引状态失败: {}", e.getMessage());
        }
    }

    public String getIndexName() {
        return indexName;
    }

    public String getPrefix() {
        return prefix;
    }
}
