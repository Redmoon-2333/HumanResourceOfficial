package com.redmoon2333.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    /**
     * CORS配置
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 设置允许的域名 - 使用allowedOriginPatterns而不是allowedOrigins
        // 支持开发环境、生产环境和Docker环境
        configuration.setAllowedOriginPatterns(Arrays.asList(
            // Vue前端开发服务器（Vite）
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            // 其他前端端口
            "http://localhost:3000",
            "http://localhost:8081",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:8081",
            "http://localhost:8080",
            "http://127.0.0.1:8080",
            // Docker环境支持
            "http://hrofficial-backend:8080",
            "http://backend:8080",
            // 生产环境域名（根据实际情况修改）
            "https://yourdomain.com",
            "https://www.yourdomain.com",
            // 允许任何https协议的域名（生产环境请谨慎使用）
            "https://*",
            // 支持file://协议访问（用于本地HTML文件测试）
            "file://",
            "null"  // Chrome等浏览器打开file://时会将origin设为null
        ));
        
        // 设置允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"
        ));
        
        // 设置允许的请求头
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 设置暴露的响应头
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization", "Content-Type", "X-Requested-With", "Accept", 
            "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"
        ));
        
        // 允许发送Cookie等凭据信息
        configuration.setAllowCredentials(true);
        
        // 设置预检请求的缓存时间
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * 自定义访问拒绝处理器
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(200);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"权限不足，无法访问资源\",\"data\":null}");
        };
    }
    
    /**
     * 安全过滤链配置
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 启用CORS配置
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 禁用CSRF保护（使用JWT时不需要）
            .csrf(csrf -> csrf.disable())
            
            // 禁用session，使用无状态认证
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 配置请求授权
            .authorizeHttpRequests(auth -> auth
                // 允许OPTIONS请求（CORS预检）
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // 允许注册和登录接口访问
                .requestMatchers("/api/auth/**").permitAll()
                // 允许静态资源访问
                .requestMatchers("/css/**", "/js/**", "/images/**", "/static/**").permitAll()
                // 允许文件访问
                .requestMatchers("/files/**").permitAll()
                // 允许首页访问
                .requestMatchers("/", "/index.html", "/ai-test.html", "/favicon.ico").permitAll()
                // 允许公开API访问
                .requestMatchers("/api/public/**").permitAll()
                // 允许活动查询接口（GET方法）公开访问
                .requestMatchers(HttpMethod.GET, "/api/activities/**").permitAll()
                // 允许往届活动查询接口（GET方法）公开访问
                .requestMatchers(HttpMethod.GET, "/api/past-activities/**").permitAll()
                // 允许用户相关公开接口访问
                .requestMatchers(HttpMethod.GET, "/api/users/alumni").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/users/search/name/**").permitAll()
                // 允许调试接口访问
                .requestMatchers("/api/users/debug/**").permitAll()
                // AI接口允许（会验证JWT，权限检查由AOP处理）
                .requestMatchers("/api/ai/**").permitAll()
                // 其他请求需要认证
                .anyRequest().authenticated()
            )
            
            // 配置访问拒绝处理器
            .exceptionHandling(ex -> ex.accessDeniedHandler(accessDeniedHandler()))
            
            // 添加JWT过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            
        return http.build();
    }
}