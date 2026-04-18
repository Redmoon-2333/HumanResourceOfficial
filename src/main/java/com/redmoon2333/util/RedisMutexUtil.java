package com.redmoon2333.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 分布式互斥锁工具类
 * <p>
 * 使用 SETNX 命令实现分布式锁，支持所有权验证和超时自动释放
 * 用于防止多个请求同时修改同一资源，保证数据一致性
 * <p>
 * Why: 在分布式环境下，传统的 synchronized 无法跨 JVM 生效
 * 使用 Redis 实现分布式锁可以确保同一时间只有一个请求能获取锁
 * <p>
 * 核心特性：
 * 1. 原子性获取锁（SETNX + EXPIRE）
 * 2. 所有权验证（释放锁时验证 value）
 * 3. 超时自动释放（防止死锁）
 *
 * @author RedMoon2333
 * @since 1.0
 */
@Component
public class RedisMutexUtil {

    private static final Logger logger = LoggerFactory.getLogger(RedisMutexUtil.class);

    /**
     * 分布式锁 Key 前缀
     */
    private static final String LOCK_PREFIX = "lock:";

    /**
     * 默认锁超时时间（10 秒）
     * <p>
     * Warning: 超时时间不宜过长，否则死锁时影响更大
     * 也不宜过短，否则可能业务未执行完锁就释放了
     */
    private static final long DEFAULT_LOCK_TIMEOUT_SECONDS = 10;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试获取分布式锁
     * <p>
     * 使用 SETNX 命令原子性地设置键值对
     * 只有当 key 不存在时才能设置成功
     *
     * @param key     锁的标识（如："activity:1:update"）
     * @param value   锁的所有者标识（建议使用 UUID 或线程 ID）
     * @param timeout 锁超时时间（自动释放）
     * @param unit    时间单位
     * @return true 表示获取锁成功，false 表示获取失败
     *
     * @example
     * // 获取活动 ID=1 的更新锁
     * String lockKey = "activity:1:update";
     * String lockValue = UUID.randomUUID().toString();
     * if (tryLock(lockKey, lockValue, 10, TimeUnit.SECONDS)) {
     *     try {
     *         // 执行业务逻辑
     *         activityService.update(activity);
     *     } finally {
     *         unlock(lockKey, lockValue);
     *     }
     * }
     */
    public boolean tryLock(String key, String value, long timeout, TimeUnit unit) {
        String lockKey = LOCK_PREFIX + key;

        try {
            // 使用 SETNX 原子性地设置键值对并设置过期时间
            // nx = only set if not exists
            Boolean result = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, value, timeout, unit);

            if (Boolean.TRUE.equals(result)) {
                logger.debug("获取锁成功：key={}, value={}, timeout={} {}",
                        lockKey, value, timeout, unit);
                return true;
            } else {
                logger.debug("获取锁失败：key={}（已被其他进程占用）", lockKey);
                return false;
            }

        } catch (Exception e) {
            logger.error("获取锁异常：key={}", key, e);
            return false;
        }
    }

    /**
     * 获取分布式锁（简化版本，使用默认超时时间）
     *
     * @param key   锁的标识
     * @param value 锁的所有者标识
     * @return true 表示获取锁成功，false 表示获取失败
     */
    public boolean tryLock(String key, String value) {
        return tryLock(key, value, DEFAULT_LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 释放分布式锁
     * <p>
     * Warning: 必须验证锁的所有权，避免误删其他进程的锁
     * 只有当当前进程持有的锁才能释放
     *
     * @param key   锁的标识
     * @param value 锁的所有者标识（必须与获取锁时一致）
     *
     * @see #tryLock(String, String, long, TimeUnit)
     */
    public void unlock(String key, String value) {
        String lockKey = LOCK_PREFIX + key;

        try {
            // 获取当前锁的值
            String currentValue = stringRedisTemplate.opsForValue().get(lockKey);

            // 验证所有权：只有当前进程持有的锁才能释放
            if (value != null && value.equals(currentValue)) {
                stringRedisTemplate.delete(lockKey);
                logger.debug("释放锁成功：key={}, value={}", lockKey, value);
            } else {
                logger.warn("释放锁失败：key={}（锁不属于当前进程）", lockKey);
            }

        } catch (Exception e) {
            logger.error("释放锁异常：key={}", key, e);
        }
    }

    /**
     * 检查锁是否被占用
     *
     * @param key 锁的标识
     * @return true 表示锁被占用，false 表示锁未占用
     */
    public boolean isLocked(String key) {
        String lockKey = LOCK_PREFIX + key;
        Boolean exists = stringRedisTemplate.hasKey(lockKey);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * 获取锁的剩余过期时间
     *
     * @param key 锁的标识
     * @return 剩余秒数，-1 表示未设置过期时间，-2 表示 key 不存在
     */
    public long getLockTTL(String key) {
        String lockKey = LOCK_PREFIX + key;
        return stringRedisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
    }

    /**
     * 延长锁的过期时间（看门狗机制）
     * <p>
     * 用于业务执行时间较长时，防止锁提前释放
     *
     * @param key     锁的标识
     * @param value   锁的所有者标识
     * @param timeout 延长的超时时间
     * @param unit    时间单位
     * @return true 表示延长成功，false 表示延长失败（锁不属于当前进程）
     */
    public boolean extendLock(String key, String value, long timeout, TimeUnit unit) {
        String lockKey = LOCK_PREFIX + key;

        try {
            // 验证所有权
            String currentValue = stringRedisTemplate.opsForValue().get(lockKey);
            if (value != null && value.equals(currentValue)) {
                // 延长过期时间
                stringRedisTemplate.expire(lockKey, timeout, unit);
                logger.debug("延长锁成功：key={}, timeout={} {}", lockKey, timeout, unit);
                return true;
            } else {
                logger.warn("延长锁失败：key={}（锁不属于当前进程）", lockKey);
                return false;
            }

        } catch (Exception e) {
            logger.error("延长锁异常：key={}", key, e);
            return false;
        }
    }

    /**
     * 强制释放锁（不考虑所有权）
     * <p>
     * Warning: 仅在特殊场景下使用（如系统故障恢复）
     * 可能破坏其他进程的正常业务
     *
     * @param key 锁的标识
     */
    public void forceUnlock(String key) {
        String lockKey = LOCK_PREFIX + key;

        try {
            stringRedisTemplate.delete(lockKey);
            logger.warn("强制释放锁：key={}", lockKey);

        } catch (Exception e) {
            logger.error("强制释放锁异常：key={}", key, e);
        }
    }

    /**
     * 配置 StringRedisSerializer
     * <p>
     * Why: 使用 StringRedisSerializer 确保 key 和 value 都以字符串形式存储
     * 避免默认序列化器带来的兼容性问题
     *
     * @return StringRedisSerializer 实例
     */
    public static StringRedisSerializer stringSerializer() {
        return new StringRedisSerializer();
    }
}