package com.redmoon2333.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限流注解
 * <p>
 * Why: 基于 Redis + Lua 脚本实现滑动窗口限流算法，防止接口被滥用
 * Warning: key 支持 SpEL 表达式，空字符串表示使用客户端 IP 作为限流键
 * </p>
 *
 * @author RedMoon2333
 * @since 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /**
     * 限流键（支持 SpEL 表达式）
     * <p>
     * Why: 支持动态键（如用户 ID、IP 等），实现细粒度限流
     * Warning: 空字符串表示使用客户端 IP 作为默认限流键
     * </p>
     *
     * @return 限流键 SpEL 表达式
     */
    String key() default "";

    /**
     * 窗口内最大请求数
     * <p>
     * Why: 控制单位时间内的请求频率
     * Warning: 值越小限流越严格，建议根据业务场景调整
     * </p>
     *
     * @return 最大请求数
     */
    int maxRequests() default 10;

    /**
     * 时间窗口大小（秒）
     * <p>
     * Why: 定义限流的时间窗口
     * Warning: 窗口大小应与业务场景匹配，避免过短导致误限流
     * </p>
     *
     * @return 窗口大小（秒）
     */
    long windowSize() default 60;
}