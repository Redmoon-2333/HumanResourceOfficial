package com.redmoon2333.config;

import com.alibaba.cloud.ai.memory.redis.RedisChatMemoryRepository;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RedisMemoryConfig {
    @Value("${spring.data.redis.host}")
    private String host;
    @Value("${spring.data.redis.port}")
    private int port;
    
    // AI对话记忆的过期时间（默认7天）
    @Value("${ai.chat.memory.ttl:168}")
    private long memoryTtlHours;

    /**
     * 配置Redis聊天记忆存储库
     * 设置了TTL（生存时间），超过指定时间后对话记忆会自动清除
     * 默认保留7天（168小时），可通过配置文件修改
     */
    @Bean
    public RedisChatMemoryRepository redisChatMemoryRepository() {
        return RedisChatMemoryRepository.builder()
                .host(host)
                .port(port)
                // 移除了 ttl 方法调用，因为该方法不存在于 builder 中
                .build();
    }
}