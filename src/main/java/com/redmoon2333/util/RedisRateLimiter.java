package com.redmoon2333.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Redis 限流工具类
 * <p>
 * Why: 基于 Redis + Lua 脚本实现滑动窗口限流算法，提供分布式限流能力
 * Warning: Redis 异常时会降级放行请求，确保服务可用性
 * </p>
 *
 * @author RedMoon2333
 * @since 1.0
 */
@Component
public class RedisRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(RedisRateLimiter.class);

    /**
     * 滑动窗口限流 Lua 脚本
     * <p>
     * Why: 使用有序集合实现滑动窗口，比固定窗口更精确
     * Warning: 脚本必须原子执行，避免并发问题
     * </p>
     */
    private static final String RATE_LIMIT_SCRIPT =
            "local key = KEYS[1]" +
            "local now = tonumber(ARGV[1])" +
            "local windowSize = tonumber(ARGV[2])" +
            "local maxRequests = tonumber(ARGV[3])" +
            "" +
            "redis.call('ZREMRANGEBYSCORE', key, 0, now - windowSize)" +
            "local count = redis.call('ZCARD', key)" +
            "" +
            "if count < maxRequests then" +
            "    redis.call('ZADD', key, now, now)" +
            "    redis.call('EXPIRE', key, windowSize)" +
            "    return 1" +
            "else" +
            "    return 0" +
            "end";

    private final DefaultRedisScript<Long> rateLimitScript;
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisRateLimiter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.rateLimitScript = new DefaultRedisScript<>(RATE_LIMIT_SCRIPT, Long.class);
    }

    /**
     * 尝试获取限流许可
     * <p>
     * Why: 核心限流方法，使用滑动窗口算法判断是否允许请求通过
     * Warning: Redis 异常时降级放行，避免单点故障导致服务不可用
     * </p>
     *
     * @param key        限流键
     * @param maxRequests 窗口内最大请求数
     * @param windowSize  时间窗口大小（秒）
     * @return true = 允许请求，false = 限流
     */
    public boolean tryAcquire(String key, int maxRequests, long windowSize) {
        try {
            long now = System.currentTimeMillis();
            List<String> keys = List.of(key);
            Object[] args = new Object[]{now, windowSize * 1000, maxRequests};

            Long result = redisTemplate.execute(rateLimitScript, keys, args);
            boolean allowed = result != null && result == 1;

            if (!allowed) {
                logger.warn("限流触发 - key: {}, maxRequests: {}, windowSize: {}s", key, maxRequests, windowSize);
            }

            return allowed;
        } catch (Exception e) {
            logger.error("限流服务异常，降级放行 - key: {}, error: {}", key, e.getMessage());
            return true;
        }
    }
}