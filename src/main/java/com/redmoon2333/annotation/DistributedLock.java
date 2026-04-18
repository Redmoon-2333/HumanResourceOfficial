package com.redmoon2333.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁注解
 * 基于 Redisson 实现，用于防止并发场景下的数据不一致问题
 * 
 * 使用示例:
 * <pre>
 * {@code
 * @DistributedLock(key = "'activity:create:' + #userId", waitTime = 5, leaseTime = 30)
 * public void createActivity(@RequestParam Long userId, @RequestBody ActivityDTO dto) {
 *     // 业务逻辑
 * }
 * }
 * </pre>
 * 
 * @author RedMoon2333
 * @since 2026-04-15
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    /**
     * 锁的键名，支持 SpEL 表达式
     * 可以使用方法参数，如："'order:' + #orderId"
     * 
     * @return 锁的键名
     */
    String key();

    /**
     * 等待锁的时间（秒）
     * 如果超过此时间仍未获取到锁，则抛出异常
     * 
     * @return 等待时间，默认 5 秒
     */
    long waitTime() default 5;

    /**
     * 锁的租赁时间（秒）
     * 超过此时间后锁自动释放，防止死锁
     * 
     * @return 租赁时间，默认 30 秒
     */
    long leaseTime() default 30;

    /**
     * 时间单位
     * 
     * @return 时间单位，默认 SECONDS
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}