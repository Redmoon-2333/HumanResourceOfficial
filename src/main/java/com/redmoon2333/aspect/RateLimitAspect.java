package com.redmoon2333.aspect;

import com.redmoon2333.annotation.RateLimit;
import com.redmoon2333.exception.BusinessException;
import com.redmoon2333.exception.ErrorCode;
import com.redmoon2333.util.RedisRateLimiter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 限流切面
 * <p>
 * Why: 基于 AOP 实现声明式限流，无需在每个方法中手动编写限流逻辑
 * Warning: SpEL 表达式解析失败时降级为 IP 限流，Redis 异常时降级放行请求
 * </p>
 *
 * @author RedMoon2333
 * @since 1.0
 */
@Aspect
@Component
public class RateLimitAspect {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitAspect.class);

    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final RedisRateLimiter redisRateLimiter;

    @Autowired
    public RateLimitAspect(RedisRateLimiter redisRateLimiter) {
        this.redisRateLimiter = redisRateLimiter;
    }

    /**
     * 限流切点定义
     * <p>
     * Why: 匹配所有标注了@RateLimit 注解的方法
     * Warning: 确保@RateLimit 注解的 RetentionPolicy 为 RUNTIME
     * </p>
     */
    @Pointcut("@annotation(com.redmoon2333.annotation.RateLimit)")
    public void rateLimitPointcut() {
    }

    /**
     * 限流环绕通知
     * <p>
     * Why: 在方法执行前进行限流检查，超过限制时抛出业务异常
     * Warning: 限流失败时记录日志但不中断业务，Redis 异常时降级放行
     * </p>
     *
     * @param joinPoint 切点
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around("rateLimitPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        // 解析限流键
        String key = resolveRateLimitKey(joinPoint, rateLimit);
        
        // 执行限流检查
        boolean allowed = redisRateLimiter.tryAcquire(
                key,
                rateLimit.maxRequests(),
                rateLimit.windowSize()
        );

        if (!allowed) {
            logger.warn("限流拦截 - method: {}#{}, key: {}", 
                    joinPoint.getTarget().getClass().getSimpleName(), 
                    signature.getName(), key);
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }

        logger.debug("限流通过 - method: {}#{}, key: {}", 
                joinPoint.getTarget().getClass().getSimpleName(), 
                signature.getName(), key);

        // 执行目标方法
        return joinPoint.proceed();
    }

    /**
     * 解析限流键
     * <p>
     * Why: 支持 SpEL 表达式动态解析限流键（如用户 ID、参数值等）
     * Warning: SpEL 解析失败或为空时降级为客户端 IP 限流
     * </p>
     *
     * @param joinPoint 切点
     * @param rateLimit 限流注解
     * @return 限流键
     */
    private String resolveRateLimitKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String keyExpression = rateLimit.key();

        // 空字符串表示使用客户端 IP 作为限流键
        if (keyExpression == null || keyExpression.trim().isEmpty()) {
            return "rate_limit:ip:" + getClientIp();
        }

        try {
            // 解析 SpEL 表达式
            Expression expr = expressionParser.parseExpression(keyExpression);
            StandardEvaluationContext context = new StandardEvaluationContext();

            // 设置目标对象作为根对象
            context.setRootObject(joinPoint.getTarget());

            // 设置方法参数 - 同时支持 arg0 和参数名两种方式
            Object[] args = joinPoint.getArgs();
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] parameterNames = signature.getParameterNames();

            for (int i = 0; i < args.length; i++) {
                // 支持 arg0, arg1 方式
                context.setVariable("arg" + i, args[i]);
                // 支持参数名方式（需要编译时保留参数名）
                if (parameterNames != null && parameterNames.length > i) {
                    context.setVariable(parameterNames[i], args[i]);
                }
            }

            // 设置 HttpServletRequest 为多个变量名，支持不同写法
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                context.setVariable("request", request);
                context.setVariable("httpRequest", request);
                context.setVariable("servletRequest", request);
            }

            Object result = expr.getValue(context);
            String resolvedKey = result != null ? String.valueOf(result) : "";

            // 解析结果为空时降级为 IP 限流
            if (resolvedKey.trim().isEmpty()) {
                logger.debug("SpEL 解析结果为空，降级为 IP 限流");
                return "rate_limit:ip:" + getClientIp();
            }

            return "rate_limit:" + resolvedKey;
        } catch (Exception e) {
            logger.warn("SpEL 表达式解析失败，降级为 IP 限流 - expression: {}, error: {}", 
                    keyExpression, e.getMessage());
            return "rate_limit:ip:" + getClientIp();
        }
    }

    /**
     * 获取客户端 IP 地址
     * <p>
     * Why: 支持代理服务器场景，优先从 X-Forwarded-For 等头部获取真实 IP
     * Warning: 头部可能被伪造，生产环境建议在网关层统一处理
     * </p>
     *
     * @return 客户端 IP 地址
     */
    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }

        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");

        // 处理代理服务器场景
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理多级代理（取第一个 IP）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip != null ? ip : "unknown";
    }
}