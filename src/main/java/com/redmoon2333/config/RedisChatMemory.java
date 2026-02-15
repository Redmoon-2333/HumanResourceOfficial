package com.redmoon2333.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Redis持久化的对话记忆实现
 *
 * Why: 替代内存存储，实现对话历史的持久化和分布式共享
 * 优势：
 * 1. 服务重启不丢失对话历史
 * 2. 多实例部署时共享对话状态
 * 3. 可设置过期时间自动清理
 * 4. 支持滑动窗口控制Token消耗
 *
 * Warning: 对话历史会随时间增长，建议配合滑动窗口策略控制Token消耗
 */
@Component
public class RedisChatMemory implements ChatMemory {
    private static final Logger logger = LoggerFactory.getLogger(RedisChatMemory.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // Why: 设置7天过期时间，避免Redis内存无限增长
    private static final Duration EXPIRATION = Duration.ofDays(7);

    // Redis key前缀，避免与其他业务key冲突
    private static final String KEY_PREFIX = "chat:memory:";

    // Why: 最大保留20轮对话（40条消息，每轮包含user+assistant），控制Token消耗
    // 估算：每轮约500-1000 tokens，20轮约10000-20000 tokens，在大多数模型限制内
    private static final int MAX_MESSAGE_PAIRS = 20;
    private static final int MAX_MESSAGES = MAX_MESSAGE_PAIRS * 2;

    public RedisChatMemory(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 添加消息到对话历史（带滑动窗口限制）
     *
     * @param conversationId 对话ID（使用userId确保隔离）
     * @param messages 要添加的消息列表
     */
    @Override
    public void add(String conversationId, List<org.springframework.ai.chat.messages.Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        String key = buildKey(conversationId);

        try {
            // 获取现有历史
            List<MessageRecord> history = getHistory(key);

            // 添加新消息
            for (org.springframework.ai.chat.messages.Message msg : messages) {
                history.add(new MessageRecord(
                    msg.getClass().getSimpleName(),
                    msg.getText()
                ));
            }

            // Why: 滑动窗口策略 - 只保留最近MAX_MESSAGES条消息
            // 避免Token消耗过多导致API调用失败或成本过高
            if (history.size() > MAX_MESSAGES) {
                int removeCount = history.size() - MAX_MESSAGES;
                history = history.subList(removeCount, history.size());
                logger.debug("触发滑动窗口清理: conversationId={}, 移除{}条旧消息", conversationId, removeCount);
            }

            // 保存回Redis并设置过期时间
            String json = objectMapper.writeValueAsString(history);
            redisTemplate.opsForValue().set(key, json, EXPIRATION);

            logger.debug("对话历史已保存到Redis: conversationId={}, 消息数={}", conversationId, history.size());
        } catch (JsonProcessingException e) {
            logger.error("保存对话历史到Redis失败: conversationId={}", conversationId, e);
        }
    }

    /**
     * 获取对话历史（用于AI调用）
     *
     * @param conversationId 对话ID
     * @return 消息列表
     */
    @Override
    public List<org.springframework.ai.chat.messages.Message> get(String conversationId) {
        String key = buildKey(conversationId);

        try {
            List<MessageRecord> records = getHistory(key);
            List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();

            for (MessageRecord record : records) {
                org.springframework.ai.chat.messages.Message msg = deserializeMessage(record);
                if (msg != null) {
                    messages.add(msg);
                }
            }

            logger.debug("从Redis获取对话历史: conversationId={}, 消息数={}", conversationId, messages.size());
            return messages;
        } catch (Exception e) {
            logger.error("从Redis获取对话历史失败: conversationId={}", conversationId, e);
            return List.of();
        }
    }

    /**
     * 获取对话历史记录（用于前端展示）
     *
     * @param conversationId 对话ID
     * @return 格式化的历史记录列表
     */
    public List<ChatMessageRecord> getChatHistory(String conversationId) {
        String key = buildKey(conversationId);
        List<MessageRecord> records = getHistory(key);
        List<ChatMessageRecord> result = new ArrayList<>();

        for (int i = 0; i < records.size(); i++) {
            MessageRecord record = records.get(i);
            // 跳过系统消息，只保留用户和AI的对话
            if (!"SystemMessage".equals(record.type)) {
                result.add(new ChatMessageRecord(
                    i,
                    "UserMessage".equals(record.type) ? "user" : "assistant",
                    record.content,
                    record.type
                ));
            }
        }

        return result;
    }

    /**
     * 获取对话统计信息
     *
     * @param conversationId 对话ID
     * @return 统计信息
     */
    public ChatHistoryStats getStats(String conversationId) {
        String key = buildKey(conversationId);
        List<MessageRecord> records = getHistory(key);

        long userCount = records.stream().filter(r -> "UserMessage".equals(r.type)).count();
        long assistantCount = records.stream().filter(r -> "AssistantMessage".equals(r.type)).count();
        long systemCount = records.stream().filter(r -> "SystemMessage".equals(r.type)).count();

        return new ChatHistoryStats(
            records.size(),
            (int) userCount,
            (int) assistantCount,
            (int) systemCount,
            MAX_MESSAGES,
            MAX_MESSAGE_PAIRS
        );
    }

    /**
     * 清除对话历史
     *
     * @param conversationId 对话ID
     */
    @Override
    public void clear(String conversationId) {
        String key = buildKey(conversationId);
        redisTemplate.delete(key);
        logger.info("对话历史已清除: conversationId={}", conversationId);
    }

    /**
     * 构建Redis key
     */
    private String buildKey(String conversationId) {
        return KEY_PREFIX + conversationId;
    }

    /**
     * 从Redis获取历史记录
     */
    private List<MessageRecord> getHistory(String key) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(json,
                objectMapper.getTypeFactory().constructCollectionType(List.class, MessageRecord.class));
        } catch (JsonProcessingException e) {
            logger.error("解析对话历史失败: key={}", key, e);
            return new ArrayList<>();
        }
    }

    /**
     * 反序列化消息记录为Spring AI Message对象
     */
    private org.springframework.ai.chat.messages.Message deserializeMessage(MessageRecord record) {
        try {
            return switch (record.type) {
                case "UserMessage" -> new UserMessage(record.content);
                case "AssistantMessage" -> new AssistantMessage(record.content);
                case "SystemMessage" -> new SystemMessage(record.content);
                default -> {
                    logger.warn("未知消息类型: {}", record.type);
                    yield null;
                }
            };
        } catch (Exception e) {
            logger.error("反序列化消息失败: type={}, content={}", record.type, record.content, e);
            return null;
        }
    }

    /**
     * 内部消息记录类，用于JSON序列化
     */
    private record MessageRecord(String type, String content) {}

    /**
     * 前端展示用的对话记录
     */
    public record ChatMessageRecord(int index, String role, String content, String rawType) {}

    /**
     * 对话历史统计信息
     */
    public record ChatHistoryStats(
        int totalMessages,
        int userMessages,
        int assistantMessages,
        int systemMessages,
        int maxMessages,
        int maxPairs
    ) {}
}
