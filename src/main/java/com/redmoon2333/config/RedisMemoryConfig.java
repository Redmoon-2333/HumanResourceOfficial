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
        RedisChatMemoryRepository repository = RedisChatMemoryRepository.builder()
                .host(host)
                .port(port)
                .build();
        // 通过setter方法设置TTL（转换为秒）
        // 这样可以防止对话记忆无限累积导致内存泄漏
        try {
            java.lang.reflect.Method setTtlMethod = repository.getClass().getMethod("setTtl", Duration.class);
            setTtlMethod.invoke(repository, Duration.ofHours(memoryTtlHours));
        } catch (Exception e) {
            // 如果设置TTL失败，记录警告但不中断启动
            System.err.println("警告: 无法为Redis聊天记忆设置TTL，可能导致内存泄漏: " + e.getMessage());
        }
        return repository;
    }
}