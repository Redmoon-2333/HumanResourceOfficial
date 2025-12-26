package com.redmoon2333.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Redis内存清理任务
 * 定期清理过期的对话记忆,防止内存泄漏
 */
@Component
public class RedisMemoryCleanupTask {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisMemoryCleanupTask.class);
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 每天凌晨3点清理7天前的对话记忆
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldConversations() {
        logger.info("开始清理过期的对话记忆...");
        
        try {
            // 查找所有以 user_ 开头的会话key
            Set<String> keys = redisTemplate.keys("user_*");
            
            if (keys != null && !keys.isEmpty()) {
                logger.info("找到 {} 个会话key", keys.size());
                
                // 统计清理数量
                int cleanedCount = 0;
                
                for (String key : keys) {
                    try {
                        // 检查key的TTL
                        Long ttl = redisTemplate.getExpire(key);
                        
                        // 如果key没有设置过期时间或者已经过期,则删除
                        if (ttl == null || ttl == -1) {
                            redisTemplate.delete(key);
                            cleanedCount++;
                            logger.debug("清理无TTL的key: {}", key);
                        }
                    } catch (Exception e) {
                        logger.error("清理key失败: {}", key, e);
                    }
                }
                
                logger.info("清理完成,共清理 {} 个过期会话", cleanedCount);
            } else {
                logger.info("没有找到需要清理的会话");
            }
            
        } catch (Exception e) {
            logger.error("清理对话记忆时发生错误", e);
        }
    }
    
    /**
     * 手动触发清理
     * @return 清理的数量
     */
    public int manualCleanup() {
        logger.info("手动触发清理过期对话记忆...");
        
        int cleanedCount = 0;
        
        try {
            Set<String> keys = redisTemplate.keys("user_*");
            
            if (keys != null) {
                for (String key : keys) {
                    Long ttl = redisTemplate.getExpire(key);
                    if (ttl == null || ttl == -1) {
                        redisTemplate.delete(key);
                        cleanedCount++;
                    }
                }
            }
            
            logger.info("手动清理完成,共清理 {} 个会话", cleanedCount);
            
        } catch (Exception e) {
            logger.error("手动清理失败", e);
        }
        
        return cleanedCount;
    }
}
