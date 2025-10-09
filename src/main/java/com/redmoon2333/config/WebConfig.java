package com.redmoon2333.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置本地图片访问路径(/file/activity-images/**)映射到本上传目录(/uploads/)
        registry.addResourceHandler("/file/activity-images/**")
                .addResourceLocations("file:uploads/");
    }
}