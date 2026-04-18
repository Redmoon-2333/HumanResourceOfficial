package com.redmoon2333.interceptor;

import com.redmoon2333.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Date;

/**
 * Token 自动刷新拦截器
 * <p>
 * 功能：
 * 1. 检查 Token 剩余有效期
 * 2. 当剩余时间低于阈值时，自动延长 Token 有效期
 * 3. 用户有任何操作访问网页时自动刷新，实现"活跃用户不掉线"
 *
 * 执行时机：在 AuthInterceptor 之前执行，确保先刷新再鉴权
 *
 * 刷新策略：
 * - 当 Token 剩余有效期 < 30 分钟时，自动延长到完整有效期
 * - 避免频繁刷新 Redis 中的过期时间
 *
 * @author RedMoon2333
 * @since 1.0
 */
@Component
public class TokenRefreshInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(TokenRefreshInterceptor.class);

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Token 刷新阈值（分钟）
     * 当剩余有效期低于此值时，自动刷新
     */
    @Value("${jwt.refresh.threshold:30}")
    private Integer refreshThresholdMinutes;

    /**
     * 在请求处理之前进行拦截
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param handler 被拦截的处理器
     * @return true 继续执行，false 中断请求
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 获取 Authorization 头
        String authHeader = request.getHeader("Authorization");

        // 没有 Token 头，直接放行（可能是公开接口）
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return true;
        }

        String token = authHeader.substring(7);

        try {
            // 获取 Token 过期时间
            Date expiration = jwtUtil.getExpirationDateFromToken(token);
            if (expiration == null) {
                // 无法获取过期时间，可能是无效 Token，放行交给 AuthInterceptor 处理
                return true;
            }

            // 计算剩余有效期（毫秒）
            long now = System.currentTimeMillis();
            long remainingTime = expiration.getTime() - now;

            // 如果 Token 已过期，放行交给 AuthInterceptor 拦截
            if (remainingTime <= 0) {
                logger.debug("Token 已过期，剩余时间：{}ms", remainingTime);
                return true;
            }

            // 计算刷新阈值（毫秒）
            long thresholdMillis = refreshThresholdMinutes * 60 * 1000L;

            // 如果剩余时间低于阈值，自动刷新
            if (remainingTime < thresholdMillis) {
                logger.debug("Token 剩余时间 {}ms 低于阈值 {}ms，自动刷新", remainingTime, thresholdMillis);

                // 延长 Token 有效期
                jwtUtil.extendTokenExpiration(token);

                logger.info("Token 自动刷新成功，用户：{}", jwtUtil.getUsernameFromToken(token));
            } else {
                // Token 有效期充足，不需要刷新
                logger.trace("Token 剩余时间充足：{}ms，无需刷新", remainingTime);
            }

        } catch (Exception e) {
            // Token 解析失败，放行交给 AuthInterceptor 处理
            logger.debug("Token 解析失败，交给鉴权拦截器处理：{}", e.getMessage());
        }

        // 无论是否刷新，都继续执行后续拦截器
        return true;
    }
}
