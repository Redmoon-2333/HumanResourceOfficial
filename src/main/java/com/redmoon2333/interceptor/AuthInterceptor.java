package com.redmoon2333.interceptor;

import com.redmoon2333.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 鉴权拦截器
 * <p>
 * 功能：
 * 1. 检查请求头中的 JWT Token 是否有效
 * 2. 将用户 ID 注入到请求属性中，供后续使用
 * 3. 放行的请求直接传递给下一个拦截器或控制器
 *
 * 执行时机：在 TokenRefreshInterceptor 之后执行
 *
 * @author RedMoon2333
 * @since 1.0
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);

    @Autowired
    private JwtUtil jwtUtil;

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

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.debug("请求未携带有效的 Authorization 头，路径：{}", request.getRequestURI());
            // 未携带 Token，但先不拦截，交给需要权限的接口自己判断
            // 这样可以支持公开接口和需权限接口共存
            return true;
        }

        String token = authHeader.substring(7);

        // 验证 Token 有效性
        if (!jwtUtil.validateToken(token)) {
            logger.warn("Token 验证失败，路径：{}, 可能原因：令牌过期、被撤销或在黑名单中", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            try {
                response.getWriter().write("{\"code\":401,\"message\":\"登录已过期，请重新登录\"}");
            } catch (Exception e) {
                logger.error("返回错误响应失败", e);
            }
            return false;
        }

        // Token 有效，提取用户信息并注入到请求属性
        try {
            Integer userId = jwtUtil.getUserIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);
            String currentRole = jwtUtil.getCurrentRoleFromToken(token);

            if (userId != null && username != null) {
                // 将用户信息注入到请求属性，供 Controller 使用
                request.setAttribute("userId", userId);
                request.setAttribute("username", username);
                request.setAttribute("currentRole", currentRole);

                logger.debug("用户鉴权成功：userId={}, username={}, role={}", userId, username, currentRole);
            } else {
                logger.warn("Token 解析失败：userId={}, username={}", userId, username);
                return false;
            }
        } catch (Exception e) {
            logger.error("解析 Token 时发生异常", e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            try {
                response.getWriter().write("{\"code\":401,\"message\":\"Token 解析失败\"}");
            } catch (Exception ex) {
                logger.error("返回错误响应失败", ex);
            }
            return false;
        }

        return true;
    }
}
