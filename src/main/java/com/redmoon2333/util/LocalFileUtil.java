package com.redmoon2333.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 本地文件上传工具类
 * 用于处理活动图片的本地存储
 */
@Component
public class LocalFileUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(LocalFileUtil.class);
    
    @Value("${local.file.upload.path:uploads}")
    private String uploadBasePath;
    
    @Value("${local.file.access.url:http://localhost:8080}")
    private String accessBaseUrl;
    
    // 活动图片存储子目录
    private static final String ACTIVITY_IMAGES_DIR = "activity-images";
    
    /**
     * 初始化上传目录
     */
    @PostConstruct
    public void init() {
        createUploadDirectory();
    }
    
    /**
     * 创建上传目录
     */
    private void createUploadDirectory() {
        try {
            Path basePath = getAbsoluteUploadPath();
            Path uploadPath = basePath.resolve(ACTIVITY_IMAGES_DIR);
            
            // 检查基础目录是否存在且可写
            if (!Files.exists(basePath)) {
                Files.createDirectories(basePath);
                logger.info("创建基础上传目录: {}", basePath.toAbsolutePath());
            }
            
            // 检查基础目录权限
            if (!Files.isWritable(basePath)) {
                logger.warn("基础上传目录不可写: {}", basePath.toAbsolutePath());
            }
            
            // 创建活动图片子目录
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                logger.info("创建上传目录: {}", uploadPath.toAbsolutePath());
            } else {
                logger.info("上传目录已存在: {}", uploadPath.toAbsolutePath());
            }
            
            // 验证目录可写性
            if (!Files.isWritable(uploadPath)) {
                logger.warn("上传目录不可写，可能影响文件上传功能: {}", uploadPath.toAbsolutePath());
            }
            
        } catch (IOException e) {
            logger.error("创建上传目录失败: {}", e.getMessage(), e);
            // 在生产环境中，如果是权限问题，我们记录警告但不阻止应用启动
            // 文件上传功能会在实际使用时进行权限检查和错误处理
            if (e instanceof java.nio.file.AccessDeniedException) {
                logger.warn("文件上传目录权限不足，文件上传功能可能受影响。请检查目录权限: {}", getAbsoluteUploadPath().resolve(ACTIVITY_IMAGES_DIR));
                return; // 不抛出异常，允许应用启动
            }
            throw new RuntimeException("无法创建文件上传目录: " + e.getMessage(), e);
        }
    }
    
    /**
     * 上传活动图片到本地服务器
     * @param file 要上传的文件
     * @return 文件访问URL
     * @throws IOException 文件操作异常
     */
    public String uploadActivityImage(MultipartFile file) throws IOException {
        logger.info("开始上传活动图片到本地: 文件名={}, 大小={} bytes", 
                   file.getOriginalFilename(), file.getSize());
        
        // 验证文件
        validateFile(file);
        
        // 生成文件保存路径
        String savedPath = generateSavePath(file);
        
        // 保存文件
        saveFile(file, savedPath);
        
        // 构建访问URL
        String accessUrl = buildAccessUrl(savedPath);
        
        logger.info("活动图片上传成功: 原始文件名={}, 保存路径={}, 访问URL={}", 
                   file.getOriginalFilename(), savedPath, accessUrl);
        
        return accessUrl;
    }
    
    /**
     * 验证文件
     * @param file 文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        
        // 验证文件类型
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!isValidImageExtension(extension)) {
            throw new IllegalArgumentException("不支持的图片格式，支持的格式: jpg, jpeg, png, gif, webp");
        }
        
        // 验证文件大小 (限制为50MB)
        long maxSize = 50 * 1024 * 1024; // 50MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("图片文件大小不能超过50MB");
        }
    }
    
    /**
     * 检查是否为有效的图片扩展名
     * @param extension 文件扩展名
     * @return 是否有效
     */
    private boolean isValidImageExtension(String extension) {
        String[] validExtensions = {"jpg", "jpeg", "png", "gif", "webp"};
        for (String validExt : validExtensions) {
            if (validExt.equals(extension)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 生成文件保存路径
     * @param file 文件
     * @return 相对路径
     */
    private String generateSavePath(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        
        // 使用日期作为子目录，便于管理
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        
        // 生成唯一文件名
        String uniqueFilename = UUID.randomUUID().toString() + "." + extension;
        
        return Paths.get(ACTIVITY_IMAGES_DIR, dateDir, uniqueFilename).toString();
    }
    
    /**
     * 获取文件扩展名
     * @param filename 文件名
     * @return 扩展名（不包含点）
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }
    
    /**
     * 保存文件到本地
     * @param file 文件
     * @param relativePath 相对路径
     * @throws IOException 文件操作异常
     */
    private void saveFile(MultipartFile file, String relativePath) throws IOException {
        // 获取绝对路径
        Path fullPath = getAbsoluteUploadPath().resolve(relativePath);
        
        // 确保父目录存在
        Path parentDir = fullPath.getParent();
        if (parentDir != null) {
            try {
                if (!Files.exists(parentDir)) {
                    Files.createDirectories(parentDir);
                    logger.info("创建上传目录: {}", parentDir.toAbsolutePath());
                }
                
                // 检查目录是否可写
                if (!Files.isWritable(parentDir)) {
                    throw new IOException("上传目录不可写: " + parentDir.toAbsolutePath());
                }
            } catch (IOException e) {
                logger.error("创建或检查上传目录失败: {}", parentDir.toAbsolutePath(), e);
                throw new IOException("无法准备上传目录: " + e.getMessage(), e);
            }
        }
        
        // 保存文件
        File destFile = fullPath.toFile();
        try {
            file.transferTo(destFile);
            logger.debug("文件保存成功: {}", fullPath.toAbsolutePath());
        } catch (IOException e) {
            logger.error("文件保存失败: {}", fullPath.toAbsolutePath(), e);
            throw new IOException("文件保存失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取上传路径的绝对路径
     * @return 绝对路径
     */
    private Path getAbsoluteUploadPath() {
        Path path = Paths.get(uploadBasePath);
        if (path.isAbsolute()) {
            return path;
        } else {
            // 相对路径，基于当前工作目录
            return Paths.get(System.getProperty("user.dir"), uploadBasePath).toAbsolutePath();
        }
    }
    
    /**
     * 构建文件访问URL
     * @param relativePath 相对路径
     * @return 访问URL
     */
    private String buildAccessUrl(String relativePath) {
        // 统一路径分隔符为 /
        String normalizedPath = relativePath.replace(File.separator, "/");
        return accessBaseUrl + "/files/" + normalizedPath;
    }
    
    /**
     * 删除本地文件
     * @param fileUrl 文件访问URL
     * @return 是否删除成功
     */
    public boolean deleteFile(String fileUrl) {
        logger.info("开始删除本地文件: {}", fileUrl);
        
        try {
            // 从URL中提取相对路径
            String relativePath = extractRelativePath(fileUrl);
            if (relativePath == null) {
                logger.warn("无法从URL中提取文件路径: {}", fileUrl);
                return false;
            }
            
            Path fullPath = getAbsoluteUploadPath().resolve(relativePath);
            boolean deleted = Files.deleteIfExists(fullPath);
            
            if (deleted) {
                logger.info("本地文件删除成功: {}", fullPath.toAbsolutePath());
            } else {
                logger.warn("本地文件不存在或删除失败: {}", fullPath.toAbsolutePath());
            }
            
            return deleted;
        } catch (IOException e) {
            logger.error("删除本地文件失败: {}", fileUrl, e);
            return false;
        }
    }
    
    /**
     * 从文件URL中提取相对路径
     * @param fileUrl 文件URL
     * @return 相对路径
     */
    private String extractRelativePath(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return null;
        }
        
        String prefix = "/files/";
        int prefixIndex = fileUrl.indexOf(prefix);
        if (prefixIndex != -1) {
            return fileUrl.substring(prefixIndex + prefix.length());
        }
        
        return null;
    }
    
    /**
     * 检查本地文件是否存在
     * @param fileUrl 文件URL
     * @return 文件是否存在
     */
    public boolean fileExists(String fileUrl) {
        String relativePath = extractRelativePath(fileUrl);
        if (relativePath == null) {
            return false;
        }
        
        Path fullPath = getAbsoluteUploadPath().resolve(relativePath);
        return Files.exists(fullPath);
    }
    
    /**
     * 获取文件的绝对路径
     * @param fileUrl 文件URL
     * @return 绝对路径
     */
    public String getAbsolutePath(String fileUrl) {
        String relativePath = extractRelativePath(fileUrl);
        if (relativePath == null) {
            return null;
        }
        
        Path fullPath = getAbsoluteUploadPath().resolve(relativePath);
        return fullPath.toAbsolutePath().toString();
    }
}