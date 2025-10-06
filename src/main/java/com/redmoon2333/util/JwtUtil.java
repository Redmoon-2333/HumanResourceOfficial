package com.redmoon2333.util;

import com.redmoon2333.service.JwtRedisService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 * 用于生成、解析和验证JWT令牌
 */
@Component
public class JwtUtil {

    @Autowired
    private JwtRedisService jwtRedisService;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * 生成密钥
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 为用户生成JWT令牌
     * @param userId 用户ID
     * @param username 用户名
     * @param roleHistory 身份历史（格式：2024级部长&2023级部员）
     * @return JWT令牌
     */
    public String generateToken(Integer userId, String username, String roleHistory) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("roleHistory", roleHistory);
        
        // 解析身份历史，提取当前身份
        String currentRole = getCurrentRole(roleHistory);
        claims.put("currentRole", currentRole);
        
        String token = createToken(claims, username);
        
        // 将令牌存储到Redis
        jwtRedisService.storeToken(userId, token, username);
        
        return token;
    }

    /**
     * 创建JWT令牌
     * @param claims 声明
     * @param subject 主题（通常是用户名）
     * @return JWT令牌
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 从身份历史中获取当前身份（最新的身份）
     * @param roleHistory 身份历史（格式：2024级部长&2023级部员）
     * @return 当前身份
     */
    private String getCurrentRole(String roleHistory) {
        if (roleHistory == null || roleHistory.trim().isEmpty()) {
            return "";
        }
        
        String[] roles = roleHistory.split("&");
        if (roles.length > 0) {
            return roles[0].trim(); // 返回第一个身份作为当前身份
        }
        
        return roleHistory.trim();
    }

    /**
     * 从令牌中获取用户名
     * @param token JWT令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * 从令牌中获取用户ID
     * @param token JWT令牌
     * @return 用户ID
     */
    public Integer getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("userId", Integer.class);
    }

    /**
     * 从令牌中获取身份历史
     * @param token JWT令牌
     * @return 身份历史
     */
    public String getRoleHistoryFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("roleHistory", String.class);
    }

    /**
     * 从令牌中获取当前身份
     * @param token JWT令牌
     * @return 当前身份
     */
    public String getCurrentRoleFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("currentRole", String.class);
    }

    /**
     * 从令牌中获取过期时间
     * @param token JWT令牌
     * @return 过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * 从令牌中获取特定声明
     * @param token JWT令牌
     * @param claimsResolver 声明解析器
     * @return 声明值
     */
    public <T> T getClaimFromToken(String token, java.util.function.Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 从令牌中获取所有声明
     * @param token JWT令牌
     * @return 所有声明
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 检查令牌是否过期
     * @param token JWT令牌
     * @return 是否过期
     */
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * 验证令牌（集成Redis验证）
     * @param token JWT令牌
     * @return 是否有效
     */
    public Boolean validateToken(String token) {
        try {
            // 首先检查令牌是否在黑名单中
            if (jwtRedisService.isTokenBlacklisted(token)) {
                return false;
            }
            
            // 检查令牌是否在Redis中存在
            if (!jwtRedisService.isTokenValid(token)) {
                return false;
            }
            
            // 验证JWT令牌的签名和过期时间
            getAllClaimsFromToken(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 刷新令牌（生成新的令牌）
     * @param token 旧的JWT令牌
     * @return 新的JWT令牌
     */
    public String refreshToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            Integer userId = claims.get("userId", Integer.class);
            String username = claims.getSubject();
            String roleHistory = claims.get("roleHistory", String.class);
            
            // 生成新令牌
            String newToken = generateToken(userId, username, roleHistory);
            
            // 在Redis中更新令牌
            jwtRedisService.refreshToken(token, newToken, userId, username);
            
            return newToken;
        } catch (JwtException | IllegalArgumentException e) {
            throw new RuntimeException("无法刷新令牌", e);
        }
    }

    /**
     * 检查用户是否具有特定身份
     * @param token JWT令牌
     * @param targetRole 目标身份（如："部长"、"部员"）
     * @return 是否具有该身份
     */
    public Boolean hasRole(String token, String targetRole) {
        try {
            String roleHistory = getRoleHistoryFromToken(token);
            if (roleHistory == null || roleHistory.trim().isEmpty()) {
                return false;
            }
            
            String[] roles = roleHistory.split("&");
            for (String role : roles) {
                if (role.trim().contains(targetRole)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 退出登录（将令牌加入黑名单）
     * @param token JWT令牌
     */
    public void logout(String token) {
        try {
            // 获取令牌剩余有效时间
            Date expiration = getExpirationDateFromToken(token);
            long remainingTime = (expiration.getTime() - System.currentTimeMillis()) / 1000;
            
            if (remainingTime > 0) {
                // 将令牌加入黑名单
                jwtRedisService.addToBlacklist(token, remainingTime);
            }
        } catch (Exception e) {
            // 即使解析失败，也要尝试加入黑名单
            jwtRedisService.addToBlacklist(token, expiration / 1000);
        }
    }

    /**
     * 强制用户下线（撤销用户所有令牌）
     * @param userId 用户ID
     */
    public void revokeUserTokens(Integer userId) {
        jwtRedisService.revokeUserTokens(userId);
    }

    /**
     * 棄用令牌是否在黑名单中
     * @param token JWT令牌
     * @return 是否在黑名单中
     */
    public Boolean isTokenBlacklisted(String token) {
        return jwtRedisService.isTokenBlacklisted(token);
    }

    /**
     * 延长令牌有效期（用于活跃用户）
     * @param token JWT令牌
     */
    public void extendTokenExpiration(String token) {
        try {
            Integer userId = getUserIdFromToken(token);
            String username = getUsernameFromToken(token);
            if (userId != null && username != null) {
                jwtRedisService.extendTokenExpiration(token, userId, username);
            }
        } catch (Exception e) {
            // 忽略延长失败
        }
    }

}