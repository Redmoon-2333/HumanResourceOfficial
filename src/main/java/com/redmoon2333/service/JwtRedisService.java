package com.redmoon2333.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * JWT-Redis集成服务类
 * 处理JWT令牌在Redis中的存储、验证和黑名单管理
 */
@Service
public class JwtRedisService {

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
     * @param userId 用户ID
     * @param token JWT令牌
     * @param username 用户名
     */
    public void storeToken(Integer userId, String token, String username) {
        String tokenKey = JWT_TOKEN_PREFIX + token;
        String userTokenKey = USER_TOKEN_PREFIX + userId;
        
        // 存储令牌信息，包含用户ID和用户名
        String tokenInfo = userId + ":" + username;
        
        // 设置令牌过期时间（与JWT过期时间保持一致）
        long expirationSeconds = jwtExpiration / 1000;
        
        // 存储令牌映射
        stringRedisTemplate.opsForValue().set(tokenKey, tokenInfo, expirationSeconds, TimeUnit.SECONDS);
        
        // 存储用户当前令牌（支持单点登录，可选）
        stringRedisTemplate.opsForValue().set(userTokenKey, token, expirationSeconds, TimeUnit.SECONDS);
    }

    /**
     * 验证令牌是否存在且有效
     * @param token JWT令牌
     * @return 是否有效
     */
    public boolean isTokenValid(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        String tokenKey = JWT_TOKEN_PREFIX + token;
        return stringRedisTemplate.hasKey(tokenKey);
    }

    /**
     * 获取令牌关联的用户信息
     * @param token JWT令牌
     * @return 用户信息（格式：userId:username）
     */
    public String getTokenUserInfo(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        
        String tokenKey = JWT_TOKEN_PREFIX + token;
        return stringRedisTemplate.opsForValue().get(tokenKey);
    }

    /**
     * 获取令牌关联的用户ID
     * @param token JWT令牌
     * @return 用户ID
     */
    public Integer getTokenUserId(String token) {
        String userInfo = getTokenUserInfo(token);
        if (userInfo == null || !userInfo.contains(":")) {
            return null;
        }
        
        try {
            String[] parts = userInfo.split(":");
            return Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 获取令牌关联的用户名
     * @param token JWT令牌
     * @return 用户名
     */
    public String getTokenUsername(String token) {
        String userInfo = getTokenUserInfo(token);
        if (userInfo == null || !userInfo.contains(":")) {
            return null;
        }
        
        String[] parts = userInfo.split(":");
        return parts.length > 1 ? parts[1] : null;
    }

    /**
     * 将令牌加入黑名单（退出登录）
     * @param token JWT令牌
     * @param remainingTime 令牌剩余有效时间（秒）
     */
    public void addToBlacklist(String token, long remainingTime) {
        if (token == null || token.trim().isEmpty()) {
            return;
        }
        
        String blacklistKey = JWT_BLACKLIST_PREFIX + token;
        String tokenKey = JWT_TOKEN_PREFIX + token;
        
        // 添加到黑名单，设置过期时间为令牌剩余时间
        stringRedisTemplate.opsForValue().set(blacklistKey, "true", remainingTime, TimeUnit.SECONDS);
        
        // 删除正常令牌存储
        stringRedisTemplate.delete(tokenKey);
    }

    /**
     * 检查令牌是否在黑名单中
     * @param token JWT令牌
     * @return 是否在黑名单中
     */
    public boolean isTokenBlacklisted(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        String blacklistKey = JWT_BLACKLIST_PREFIX + token;
        return stringRedisTemplate.hasKey(blacklistKey);
    }

    /**
     * 删除用户的所有令牌（强制下线）
     * @param userId 用户ID
     */
    public void revokeUserTokens(Integer userId) {
        String userTokenKey = USER_TOKEN_PREFIX + userId;
        String currentToken = stringRedisTemplate.opsForValue().get(userTokenKey);
        
        if (currentToken != null) {
            // 将当前令牌加入黑名单
            addToBlacklist(currentToken, jwtExpiration / 1000);
        }
        
        // 删除用户令牌记录
        stringRedisTemplate.delete(userTokenKey);
    }

    /**
     * 刷新令牌存储
     * @param oldToken 旧令牌
     * @param newToken 新令牌
     * @param userId 用户ID
     * @param username 用户名
     */
    public void refreshToken(String oldToken, String newToken, Integer userId, String username) {
        // 将旧令牌加入黑名单
        if (oldToken != null) {
            addToBlacklist(oldToken, jwtExpiration / 1000);
        }
        
        // 存储新令牌
        storeToken(userId, newToken, username);
    }

    /**
     * 延长令牌有效期
     * @param token JWT令牌
     * @param userId 用户ID
     * @param username 用户名
     */
    public void extendTokenExpiration(String token, Integer userId, String username) {
        if (isTokenValid(token) && !isTokenBlacklisted(token)) {
            // 重新存储令牌，刷新过期时间
            storeToken(userId, token, username);
        }
    }

    /**
     * 获取Redis中存储的令牌数量（监控用）
     * @return 令牌数量
     */
    public long getActiveTokenCount() {
        return stringRedisTemplate.keys(JWT_TOKEN_PREFIX + "*").size();
    }

    /**
     * 获取黑名单中的令牌数量（监控用）
     * @return 黑名单令牌数量
     */
    public long getBlacklistedTokenCount() {
        return stringRedisTemplate.keys(JWT_BLACKLIST_PREFIX + "*").size();
    }
}