package com.redmoon2333.util;

import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 缓存 TTL 工具类
 * <p>
 * 用于设置带随机偏移的 TTL，防止缓存雪崩（Cache Avalanche）
 * <p>
 * Why: 当大量缓存同时过期时，会导致数据库瞬间承受巨大压力。
 * 通过为 TTL 添加随机偏移量，可以分散缓存过期时间，避免集中失效。
 *
 * @author RedMoon2333
 * @since 1.0
 */
public class CacheTTLUtil {

    /**
     * 默认基础 TTL（30 分钟）
     */
    private static final long DEFAULT_BASE_TTL_SECONDS = 30 * 60;

    /**
     * 默认随机偏移量（5 分钟）
     */
    private static final long DEFAULT_RANDOM_OFFSET_SECONDS = 5 * 60;

    /**
     * 最小 TTL 限制（60 秒）
     * <p>
     * Warning: TTL 不能太短，否则缓存命中率会大幅下降
     */
    private static final long MIN_TTL_SECONDS = 60;

    private CacheTTLUtil() {
        // 工具类私有构造函数，防止实例化
    }

    /**
     * 设置带随机偏移的 TTL
     * <p>
     * 实际 TTL = baseTTLSeconds ± randomOffsetSeconds
     * 保证最小 TTL 不低于 60 秒
     *
     * @param redisTemplate        Redis 模板
     * @param key                  缓存键
     * @param value                缓存值
     * @param baseTTLSeconds       基础 TTL（秒）
     * @param randomOffsetSeconds  随机偏移量（秒）
     * @param <K>                  键类型
     * @param <V>                  值类型
     *
     * @see #setWithStandardRandomTTL(RedisTemplate, Object, Object)
     */
    public static <K, V> void setWithRandomTTL(
            RedisTemplate<K, V> redisTemplate,
            K key,
            V value,
            long baseTTLSeconds,
            long randomOffsetSeconds) {

        // 生成随机偏移量：[-randomOffsetSeconds, +randomOffsetSeconds]
        long randomOffset = ThreadLocalRandom.current()
                .nextLong(-randomOffsetSeconds, randomOffsetSeconds);

        // 计算实际 TTL，确保不低于最小限制
        long actualTTL = Math.max(baseTTLSeconds + randomOffset, MIN_TTL_SECONDS);

        redisTemplate.opsForValue().set(key, value, actualTTL, TimeUnit.SECONDS);
    }

    /**
     * 使用标准随机 TTL 设置缓存
     * <p>
     * 默认配置：30 分钟 ± 5 分钟
     * 即实际 TTL 在 25-35 分钟之间随机
     * <p>
     * Why: 这是大多数业务场景的推荐配置，平衡了缓存命中率和内存占用
     *
     * @param redisTemplate Redis 模板
     * @param key           缓存键
     * @param value         缓存值
     * @param <K>           键类型
     * @param <V>           值类型
     */
    public static <K, V> void setWithStandardRandomTTL(
            RedisTemplate<K, V> redisTemplate,
            K key,
            V value) {

        setWithRandomTTL(
                redisTemplate,
                key,
                value,
                DEFAULT_BASE_TTL_SECONDS,
                DEFAULT_RANDOM_OFFSET_SECONDS
        );
    }

    /**
     * 计算随机后的实际 TTL
     * <p>
     * 用于日志记录或监控场景
     *
     * @param baseTTLSeconds      基础 TTL（秒）
     * @param randomOffsetSeconds 随机偏移量（秒）
     * @return 实际 TTL（秒），保证不低于 60 秒
     */
    public static long calculateRandomTTL(long baseTTLSeconds, long randomOffsetSeconds) {
        long randomOffset = ThreadLocalRandom.current()
                .nextLong(-randomOffsetSeconds, randomOffsetSeconds);
        return Math.max(baseTTLSeconds + randomOffset, MIN_TTL_SECONDS);
    }
}