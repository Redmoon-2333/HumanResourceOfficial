package com.redmoon2333.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * AI对话记忆配置类
 *
 * Why: 配置ChatMemory实现，默认使用Redis持久化存储
 * 优势：
 * 1. 服务重启不丢失对话历史
 * 2. 多实例部署时共享对话状态
 * 3. 支持过期时间自动清理
 *
 * 如需切换回内存存储，可注释掉@Primary或修改配置
 */
@Configuration
public class RedisMemoryConfig {

    /**
     * Redis持久化的ChatMemory实现
     * Why: 使用Redis替代内存存储，实现对话历史的持久化
     * Warning: 需要确保Redis服务可用，否则对话功能将受影响
     */
    @Bean
    @Primary
    public ChatMemory chatMemory(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        return new RedisChatMemory(redisTemplate, objectMapper);
    }
}
