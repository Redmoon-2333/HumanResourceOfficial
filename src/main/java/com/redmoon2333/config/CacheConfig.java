package com.redmoon2333.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Spring Cache 配置类
 * 
 * 功能：
 * 1. 启用注解缓存（@Cacheable、@CacheEvict）
 * 2. 配置 Redis 序列化方式
 * 3. 设置默认 TTL
 * 
 * @author Sisyphus
 * @since 2026-04-15
 */
@Configuration
@EnableCaching  // 启用缓存注解
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        // 配置 ObjectMapper 支持 Java 8 日期时间类型（LocalDateTime 等）
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 配置 Redis 序列化方式
        // 使用 SimpleModule 方式注册类型信息，避免 activateDefaultTyping 的问题
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            // 默认 30 分钟过期
            .entryTtl(Duration.ofMinutes(30))
            // Key 采用 String 序列化
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            // Value 采用 JSON 序列化（支持 Java 8 日期时间）
            // 使用 defaultTyped 方式自动添加 @class 类型信息
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
            // 禁止缓存 null 值
            .disableCachingNullValues();

        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build();
    }
}
