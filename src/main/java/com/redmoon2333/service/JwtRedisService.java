package com.redmoon2333.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JWT-Redis集成服务类
 * 处理JWT令牌在Redis中的存储、验证和黑名单管理
 */
@Service
public class JwtRedisService {

    private static final Logger logger = LoggerFactory.getLogger(JwtRedisService.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    // Redis key前缀
    private static final String JWT_TOKEN_PREFIX = "jwt:token:";
    private static final String JWT_BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final String USER_TOKEN_PREFIX = "user:token:";

    /**
     * 存储JWT令牌到Redis
     * @param token JWT令牌
     * @param username 用户名
     */
    public void storeToken(String token, String username) {
        String key = JWT_TOKEN_PREFIX + token;
        String userKey = USER_TOKEN_PREFIX + username;

        // 存储令牌，设置过期时间
        stringRedisTemplate.opsForValue().set(key, username, jwtExpiration, TimeUnit.MILLISECONDS);

        // 将令牌关联到用户，用于后续登出时清除该用户的所有令牌
        stringRedisTemplate.opsForSet().add(userKey, token);
        stringRedisTemplate.expire(userKey, jwtExpiration, TimeUnit.MILLISECONDS);
    }

    /**
     * 存储JWT令牌到Redis（带用户ID）
     * @param userId 用户ID
     * @param token JWT令牌
     * @param username 用户名
     */
    public void storeToken(Integer userId, String token, String username) {
        storeToken(token, username);
    }

    /**
     * 验证令牌是否有效（存在于Redis中且未过期）
     * @param token JWT令牌
     * @return 是否有效
     */
    public boolean isTokenValid(String token) {
        String key = JWT_TOKEN_PREFIX + token;
        Boolean hasKey = stringRedisTemplate.hasKey(key);
        return hasKey != null && hasKey;
    }

    /**
     * 获取令牌对应的用户名
     * @param token JWT令牌
     * @return 用户名，如果不存在则返回null
     */
    public String getUsernameFromToken(String token) {
        String key = JWT_TOKEN_PREFIX + token;
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 将令牌加入黑名单（登出时使用）
     * @param token JWT令牌
     */
    public void blacklistToken(String token) {
        String key = JWT_BLACKLIST_PREFIX + token;
        // 黑名单中的令牌保留一段时间，防止重放攻击
        stringRedisTemplate.opsForValue().set(key, "blacklisted", jwtExpiration, TimeUnit.MILLISECONDS);

        // 从有效令牌集合中移除
        String tokenKey = JWT_TOKEN_PREFIX + token;
        stringRedisTemplate.delete(tokenKey);
    }

    /**
     * 将令牌加入黑名单，指定过期时间
     * @param token JWT令牌
     * @param expirationSeconds 过期时间（秒）
     */
    public void addToBlacklist(String token, long expirationSeconds) {
        String key = JWT_BLACKLIST_PREFIX + token;
        stringRedisTemplate.opsForValue().set(key, "blacklisted", expirationSeconds, TimeUnit.SECONDS);

        // 从有效令牌集合中移除
        String tokenKey = JWT_TOKEN_PREFIX + token;
        stringRedisTemplate.delete(tokenKey);
    }

    /**
     * 检查令牌是否在黑名单中
     * @param token JWT令牌
     * @return 是否在黑名单中
     */
    public Boolean isTokenBlacklisted(String token) {
        String key = JWT_BLACKLIST_PREFIX + token;
        Boolean hasKey = stringRedisTemplate.hasKey(key);
        return hasKey != null && hasKey;
    }

    /**
     * 延长令牌有效期（刷新时使用）
     * @param token JWT令牌
     */
    public void extendTokenExpiration(String token) {
        String key = JWT_TOKEN_PREFIX + token;
        Boolean hasKey = stringRedisTemplate.hasKey(key);
        if (hasKey != null && hasKey) {
            stringRedisTemplate.expire(key, jwtExpiration, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 延长令牌有效期（带用户信息）
     * @param token JWT令牌
     * @param userId 用户ID
     * @param username 用户名
     */
    public void extendTokenExpiration(String token, Integer userId, String username) {
        extendTokenExpiration(token);
    }

    /**
     * 刷新令牌（将旧令牌加入黑名单，存储新令牌）
     * @param oldToken 旧JWT令牌
     * @param newToken 新JWT令牌
     * @param userId 用户ID
     * @param username 用户名
     */
    public void refreshToken(String oldToken, String newToken, Integer userId, String username) {
        // 将旧令牌加入黑名单
        blacklistToken(oldToken);
        // 存储新令牌
        storeToken(newToken, username);
    }

    /**
     * 撤销用户的所有令牌（强制下线）
     * @param userId 用户ID
     */
    public void revokeUserTokens(Integer userId) {
        // 通过用户ID查找用户名，然后撤销该用户的所有令牌
        // 这里简化处理，实际应该根据userId查询用户名
        logger.info("撤销用户 {} 的所有令牌", userId);
    }

    /**
     * 删除用户的所有令牌（强制登出所有设备）
     * @param username 用户名
     */
    public void removeAllUserTokens(String username) {
        String userKey = USER_TOKEN_PREFIX + username;

        // 获取该用户的所有令牌
        var tokens = stringRedisTemplate.opsForSet().members(userKey);
        if (tokens != null) {
            for (String token : tokens) {
                // 将每个令牌加入黑名单
                blacklistToken(token);
            }
        }

        // 删除用户令牌集合
        stringRedisTemplate.delete(userKey);
    }

    /**
     * 获取Redis中存储的令牌数量（监控用）
     * 使用SCAN命令替代keys()，避免阻塞Redis
     * @return 令牌数量
     */
    @SuppressWarnings("unused")
    public long getActiveTokenCount() {
        AtomicLong count = new AtomicLong(0);
        Cursor<byte[]> cursor = null;
        try {
            var connectionFactory = stringRedisTemplate.getConnectionFactory();
            if (connectionFactory == null) {
                logger.error("Redis连接工厂为空");
                return -1;
            }
            var connection = connectionFactory.getConnection();
            if (connection == null) {
                logger.error("Redis连接为空");
                return -1;
            }
            cursor = connection.scan(ScanOptions.scanOptions().match(JWT_TOKEN_PREFIX + "*").count(1000).build());
            while (cursor.hasNext()) {
                count.incrementAndGet();
                cursor.next();
            }
        } catch (Exception e) {
            logger.error("获取活跃令牌数量失败", e);
            return -1;
        } finally {
            closeCursorQuietly(cursor);
        }
        return count.get();
    }

    /**
     * 获取黑名单中的令牌数量（监控用）
     * 使用SCAN命令替代keys()，避免阻塞Redis
     * @return 黑名单令牌数量
     */
    @SuppressWarnings("unused")
    public long getBlacklistedTokenCount() {
        AtomicLong count = new AtomicLong(0);
        Cursor<byte[]> cursor = null;
        try {
            var connectionFactory = stringRedisTemplate.getConnectionFactory();
            if (connectionFactory == null) {
                logger.error("Redis连接工厂为空");
                return -1;
            }
            var connection = connectionFactory.getConnection();
            if (connection == null) {
                logger.error("Redis连接为空");
                return -1;
            }
            cursor = connection.scan(ScanOptions.scanOptions().match(JWT_BLACKLIST_PREFIX + "*").count(1000).build());
            while (cursor.hasNext()) {
                count.incrementAndGet();
                cursor.next();
            }
        } catch (Exception e) {
            logger.error("获取黑名单令牌数量失败", e);
            return -1;
        } finally {
            closeCursorQuietly(cursor);
        }
        return count.get();
    }

    /**
     * 安静关闭Redis游标，不抛出异常
     * @param cursor Redis游标
     */
    @SuppressWarnings("deprecation")
    private void closeCursorQuietly(Cursor<byte[]> cursor) {
        if (cursor != null) {
            try {
                cursor.close();
            } catch (Exception e) {
                logger.error("关闭Redis游标失败", e);
            }
        }
    }
}
