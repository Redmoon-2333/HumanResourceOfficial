package com.redmoon2333.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;

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

    @Bean
    public JedisPooled jedisPooled() {
        // 如果有密码则使用密码，否则不使用
        if (redisPassword != null && !redisPassword.isEmpty()) {
            JedisClientConfig config = DefaultJedisClientConfig.builder()
                .password(redisPassword)
                .build();
            return new JedisPooled(new HostAndPort(redisHost, redisPort), config);
        }
        return new JedisPooled(new HostAndPort(redisHost, redisPort));
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

    public String getIndexName() {
        return indexName;
    }

    public String getPrefix() {
        return prefix;
    }
}
