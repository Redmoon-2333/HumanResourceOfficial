package com.redmoon2333.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * AI对话记忆配置类
 *
 * Spring AI 1.1.x 版本变更说明：
 * - ChatMemory接口简化，使用简单的内存实现
 * - 如需Redis持久化，可自行实现 ChatMemory 接口
 * 
 * 注意：RedisTemplate 已在 RedisConfig 中配置，此处不再重复定义
 */
@Configuration
public class RedisMemoryConfig {

    /**
     * 配置ChatMemory
     * 使用简单的内存实现（Spring AI 1.1.x兼容方式）
     *
     * Why: Spring AI 1.1.x 简化了ChatMemory接口，可自行实现Redis存储
     * 当前使用内存实现，如需Redis持久化可后续扩展
     */
    @Bean
    public ChatMemory chatMemory() {
        return new ChatMemory() {
            private final Map<String, List<org.springframework.ai.chat.messages.Message>> conversations = new ConcurrentHashMap<>();

            @Override
            public void add(String conversationId, List<org.springframework.ai.chat.messages.Message> messages) {
                conversations.computeIfAbsent(conversationId, k -> new CopyOnWriteArrayList<>()).addAll(messages);
            }

            @Override
            public List<org.springframework.ai.chat.messages.Message> get(String conversationId) {
                List<org.springframework.ai.chat.messages.Message> messages = conversations.get(conversationId);
                return messages != null ? List.copyOf(messages) : List.of();
            }

            @Override
            public void clear(String conversationId) {
                conversations.remove(conversationId);
            }
        };
    }
}
