package com.redmoon2333.config;

import com.redmoon2333.exception.ErrorCode;
import com.redmoon2333.exception.JwtException;
import com.redmoon2333.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT身份验证过滤器
 * 拦截所有请求，验证JWT令牌的有效性
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {

        // 获取Authorization头
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        // 检查是否有Bearer令牌
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                username = jwtUtil.getUsernameFromToken(token);
            } catch (Exception e) {
                logger.debug("无法从令牌中提取用户名: " + e.getMessage());
            }
        }

        // 如果用户名存在且当前没有认证信息
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // 验证令牌（现在集成了Redis验证）
                if (jwtUtil.validateToken(token)) {
                    // 获取用户身份信息
                    String roleHistory = jwtUtil.getRoleHistoryFromToken(token);
                    String currentRole = jwtUtil.getCurrentRoleFromToken(token);
                    
                    // 延长活跃用户的令牌有效期（可选功能）
                    // jwtUtil.extendTokenExpiration(token);
                    
                    // 创建权限列表
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    
                    // 根据身份历史添加权限
                    if (roleHistory != null && !roleHistory.trim().isEmpty()) {
                        String[] roles = roleHistory.split("&");
                        for (String role : roles) {
                            role = role.trim();
                            if (role.contains("部长")) {
                                authorities.add(new SimpleGrantedAuthority("ROLE_MINISTER"));
                            }
                            if (role.contains("部员")) {
                                authorities.add(new SimpleGrantedAuthority("ROLE_MEMBER"));
                            }
                            // 根据年级添加权限
                            if (role.contains("2024级")) {
                                authorities.add(new SimpleGrantedAuthority("ROLE_2024"));
                            }
                            if (role.contains("2023级")) {
                                authorities.add(new SimpleGrantedAuthority("ROLE_2023"));
                            }
                            if (role.contains("2022级")) {
                                authorities.add(new SimpleGrantedAuthority("ROLE_2022"));
                            }
                            if (role.contains("2021级")) {
                                authorities.add(new SimpleGrantedAuthority("ROLE_2021"));
                            }
                        }
                    }
                    
                    // 默认用户权限
                    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                    
                    // 创建认证令牌
                    UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // 设置到安全上下文
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    // 将用户信息添加到请求属性中，方便控制器使用
                    request.setAttribute("userId", jwtUtil.getUserIdFromToken(token));
                    request.setAttribute("username", username);
                    request.setAttribute("roleHistory", roleHistory);
                    request.setAttribute("currentRole", currentRole);
                }
            } catch (Exception e) {
                logger.debug("JWT令牌验证失败: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // 对于以下路径不进行JWT验证
        return path.equals("/api/auth/login") ||
               path.equals("/api/auth/register") ||
               path.equals("/api/auth/check-username") ||
               path.startsWith("/api/public/") ||
               path.equals("/") ||
               path.startsWith("/static/") ||
               path.startsWith("/uploads/") ||  // 上传的文件不需要JWT验证
               path.equals("/favicon.ico") ||
               // 活动查询接口（GET方法）不需要JWT验证
               ("GET".equals(method) && path.startsWith("/api/activities")) ||
               // 往届活动查询接口（GET方法）不需要JWT验证
               ("GET".equals(method) && path.startsWith("/api/past-activities")) ||
               // 用户公开信息查询接口不需要JWT验证
               path.equals("/api/users/alumni") ||
               path.startsWith("/api/users/search/name") ||
               // 日常图片查询接口（GET方法）不需要JWT验证 - "我们的日常"板块所有人可见
               ("GET".equals(method) && path.equals("/api/daily-images"));
    }
}