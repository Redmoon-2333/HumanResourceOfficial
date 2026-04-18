package com.redmoon2333.aspect;

import com.redmoon2333.annotation.DistributedLock;
import com.redmoon2333.exception.BusinessException;
import com.redmoon2333.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 分布式锁切面
 * 基于 Redisson 实现，拦截标注 @DistributedLock 注解的方法
 * 
 * 功能特性:
 * - 支持 SpEL 表达式动态生成锁键
 * - 自动获取/释放锁
 * - 等待超时抛出异常
 * - 锁自动过期防止死锁
 * 
 * @author RedMoon2333
 * @since 2026-04-15
 */
@Aspect
@Component
public class DistributedLockAspect {

    private static final Logger logger = LoggerFactory.getLogger(DistributedLockAspect.class);

    private final RedissonClient redissonClient;
    private final SpelExpressionParser spelExpressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Autowired
    public DistributedLockAspect(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 定义切点：所有标注 @DistributedLock 的方法
     */
    @Pointcut("@annotation(com.redmoon2333.annotation.DistributedLock)")
    public void distributedLockPointcut() {
    }

    /**
     * 环绕通知：实现分布式锁逻辑
     * 
     * @param joinPoint 切点
     * @return 方法执行结果
     * @throws Throwable 方法抛出的异常
     */
    @Around("distributedLockPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLock lockAnnotation = method.getAnnotation(DistributedLock.class);

        // 解析 SpEL 表达式获取锁键
        String lockKey = evaluateLockKey(lockAnnotation.key(), joinPoint);
        
        logger.info("分布式锁 - 尝试获取锁：{}, 方法：{}.{}", 
                lockKey, 
                joinPoint.getTarget().getClass().getSimpleName(), 
                method.getName());

        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;

        try {
            // 尝试获取锁
            acquired = lock.tryLock(
                    lockAnnotation.waitTime(), 
                    lockAnnotation.leaseTime(), 
                    lockAnnotation.timeUnit()
            );

            if (!acquired) {
                logger.warn("分布式锁 - 获取锁失败：{}, 方法：{}.{}", 
                        lockKey,
                        joinPoint.getTarget().getClass().getSimpleName(), 
                        method.getName());
                throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
            }

            logger.debug("分布式锁 - 获取成功：{}", lockKey);
            
            // 执行业务逻辑
            return joinPoint.proceed();

        } catch (InterruptedException e) {
            // 恢复中断状态
            Thread.currentThread().interrupt();
            logger.error("分布式锁 - 获取锁时被中断：{}", lockKey, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        } finally {
            // 释放锁
            if (acquired && lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                    logger.debug("分布式锁 - 释放成功：{}", lockKey);
                } catch (Exception e) {
                    logger.error("分布式锁 - 释放锁失败：{}", lockKey, e);
                }
            }
        }
    }

    /**
     * 解析 SpEL 表达式获取锁键
     * 
     * @param spelExpression SpEL 表达式
     * @param joinPoint 切点
     * @return 解析后的锁键
     */
    private String evaluateLockKey(String spelExpression, ProceedingJoinPoint joinPoint) {
        // 移除引号（如果存在）
        String expressionStr = spelExpression.trim();
        if (expressionStr.startsWith("\"") && expressionStr.endsWith("\"")) {
            expressionStr = expressionStr.substring(1, expressionStr.length() - 1);
        }

        Expression expression = spelExpressionParser.parseExpression(expressionStr);
        
        // 创建评估上下文
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        // 获取方法参数
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(signature.getMethod());
        
        // 将参数名和值绑定到上下文中
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                // 支持参数名方式
                context.setVariable(parameterNames[i], args[i]);
                // 同时支持 arg0, arg1 方式
                context.setVariable("arg" + i, args[i]);
            }
        }

        // 设置 HttpServletRequest 为多个变量名，支持不同写法
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            context.setVariable("request", request);
            context.setVariable("httpRequest", request);
        }
        
        // 评估表达式
        String lockKey = expression.getValue(context, String.class);
        
        if (lockKey == null || lockKey.isEmpty()) {
            throw new IllegalStateException("分布式锁键不能为空，表达式：" + spelExpression);
        }
        
        return lockKey;
    }
}