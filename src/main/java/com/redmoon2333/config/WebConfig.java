package com.redmoon2333.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 使用allowedOriginPatterns替代allowedOrigins以支持allowCredentials
                .allowedOriginPatterns(
                    "http://localhost:*",
                    "http://127.0.0.1:*",
                    "http://81.70.218.85:*",  // 生产服务器地址
                    "https://*",
                    "file://",  // 支持本地文件访问
                    "null"      // 支持file://协议（Chrome会将origin设为null）
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Type")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 配置静态资源映射
     * Note: /uploads/** 的映射已在 StaticResourceConfig 中配置
     * 这里不需要重复配置，避免冲突
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // StaticResourceConfig 已配置 /uploads/** 映射
        // 这里可以添加其他资源映射（如需要）
    }
}