package com.redmoon2333.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 静态资源配置
 * 用于配置本地文件访问路径
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    
    private static final Logger logger = LoggerFactory.getLogger(StaticResourceConfig.class);
    
    @Value("${local.file.upload.path:uploads}")
    private String uploadPath;
    
    @PostConstruct
    public void init() {
        // 确保上传目录存在
        createUploadDirectoryIfNotExists();
        logger.info("静态资源配置初始化完成，上传路径: {}", uploadPath);
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源处理器
        // 将 /uploads/** 的请求映射到本地上传目录
        String absoluteUploadPath = getAbsoluteUploadPath();
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absoluteUploadPath + File.separator)
                .setCachePeriod(3600) // 设置缓存时间为1小时
                .resourceChain(true);
        
        logger.info("配置静态资源映射: /uploads/** -> file:{}", absoluteUploadPath);
        
        // 保持默认的静态资源处理器
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600)
                .resourceChain(true);
    }
    
    /**
     * 获取上传路径的绝对路径
     * @return 绝对路径
     */
    private String getAbsoluteUploadPath() {
        Path path = Paths.get(uploadPath);
        if (path.isAbsolute()) {
            return path.toString();
        } else {
            // 相对路径，基于当前工作目录
            return Paths.get(System.getProperty("user.dir"), uploadPath).toAbsolutePath().toString();
        }
    }
    
    /**
     * 创建上传目录（如果不存在）
     */
    private void createUploadDirectoryIfNotExists() {
        try {
            Path uploadDir = Paths.get(getAbsoluteUploadPath());
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                logger.info("创建上传目录: {}", uploadDir.toAbsolutePath());
            } else {
                logger.info("上传目录已存在: {}", uploadDir.toAbsolutePath());
            }
        } catch (Exception e) {
            logger.error("创建上传目录失败: {}", e.getMessage(), e);
            throw new RuntimeException("无法创建文件上传目录", e);
        }
    }
}