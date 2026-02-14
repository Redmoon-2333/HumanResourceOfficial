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
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 本地文件上传工具类
 * 统一图片存储管理
 * 目录结构：
 *   uploads/
 *   ├── images/           # 图片统一存储目录
 *   │   ├── daily/        # 我们的日常图片
 *   │   ├── activities/   # 活动照片
 *   │   └── avatar/       # 用户头像
 *   └── materials/        # 资料文件（保持不变）
 */
@Component
public class LocalFileUtil {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileUtil.class);

    @Value("${local.file.upload.path:uploads}")
    private String uploadBasePath;

    @Value("${local.file.access.url:http://localhost:8080}")
    private String accessBaseUrl;

    // ==================== 图片存储子目录定义 ====================

    /** 图片统一存储目录 */
    private static final String IMAGES_DIR = "images";

    /** 我们的日常图片子目录 */
    private static final String DAILY_IMAGES_DIR = "daily";

    /** 活动照片子目录 */
    private static final String ACTIVITIES_IMAGES_DIR = "activities";

    /** 用户头像子目录 */
    private static final String AVATAR_IMAGES_DIR = "avatar";

    /** 资料文件存储目录（保持不变） */
    private static final String MATERIALS_DIR = "materials";

    // ==================== 图片类型枚举 ====================

    /**
     * 图片类型枚举
     */
    public enum ImageType {
        DAILY("我们的日常", DAILY_IMAGES_DIR),
        ACTIVITY("活动照片", ACTIVITIES_IMAGES_DIR),
        AVATAR("用户头像", AVATAR_IMAGES_DIR);

        private final String displayName;
        private final String directory;

        ImageType(String displayName, String directory) {
            this.displayName = displayName;
            this.directory = directory;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDirectory() {
            return directory;
        }
    }

    /**
     * 初始化上传目录
     */
    @PostConstruct
    public void init() {
        createUploadDirectory();
    }

    /**
     * 创建上传目录结构
     */
    private void createUploadDirectory() {
        try {
            Path basePath = getAbsoluteUploadPath();

            // 创建图片统一存储目录
            Path imagesPath = basePath.resolve(IMAGES_DIR);
            createDirectoryIfNotExists(imagesPath);

            // 创建各类图片子目录
            for (ImageType type : ImageType.values()) {
                Path typePath = imagesPath.resolve(type.getDirectory());
                createDirectoryIfNotExists(typePath);
            }

            // 创建资料文件目录
            Path materialsPath = basePath.resolve(MATERIALS_DIR);
            createDirectoryIfNotExists(materialsPath);

            logger.info("文件上传目录初始化完成: {}", basePath.toAbsolutePath());

        } catch (Exception e) {
            logger.warn("初始化上传目录时出现异常，但应用继续启动: {}", e.getMessage());
        }
    }

    /**
     * 创建目录（如果不存在）
     */
    private void createDirectoryIfNotExists(Path path) {
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                setDirectoryPermissions(path, "777");
                logger.info("创建目录: {}", path.toAbsolutePath());
            } else {
                logger.debug("目录已存在: {}", path.toAbsolutePath());
            }
        } catch (IOException e) {
            logger.warn("无法创建目录: {}, 原因: {}", path.toAbsolutePath(), e.getMessage());
        }
    }

    // ==================== 统一图片上传接口 ====================

    /**
     * 上传图片到指定分类目录
     *
     * @param file     图片文件
     * @param imageType 图片类型（决定存储位置）
     * @return 文件访问URL
     * @throws IOException 文件操作异常
     */
    public String uploadImage(MultipartFile file, ImageType imageType) throws IOException {
        logger.info("开始上传[{}]图片: 文件名={}, 大小={} bytes",
                imageType.getDisplayName(), file.getOriginalFilename(), file.getSize());

        // 验证文件
        validateImageFile(file);

        // 生成文件保存路径
        String savedPath = generateImageSavePath(file, imageType);

        // 保存文件
        saveFile(file, savedPath);

        // 构建访问URL
        String accessUrl = buildAccessUrl(savedPath);

        logger.info("[{}]图片上传成功: URL={}", imageType.getDisplayName(), accessUrl);

        return accessUrl;
    }

    /**
     * 上传我们的日常图片
     * 便捷方法，等同于 uploadImage(file, ImageType.DAILY)
     */
    public String uploadDailyImage(MultipartFile file) throws IOException {
        return uploadImage(file, ImageType.DAILY);
    }

    /**
     * 上传活动照片
     * 便捷方法，等同于 uploadImage(file, ImageType.ACTIVITY)
     */
    public String uploadActivityImage(MultipartFile file) throws IOException {
        return uploadImage(file, ImageType.ACTIVITY);
    }

    /**
     * 上传用户头像
     * 便捷方法，等同于 uploadImage(file, ImageType.AVATAR)
     */
    public String uploadAvatarImage(MultipartFile file) throws IOException {
        return uploadImage(file, ImageType.AVATAR);
    }

    // ==================== 文件验证 ====================

    /**
     * 验证图片文件
     */
    private void validateImageFile(MultipartFile file) {
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
        long maxSize = 50 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("图片文件大小不能超过50MB");
        }
    }

    /**
     * 检查是否为有效的图片扩展名
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

    // ==================== 路径生成 ====================

    /**
     * 生成图片保存路径
     */
    private String generateImageSavePath(MultipartFile file, ImageType imageType) {
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);

        // 使用日期作为子目录，便于管理
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

        // 生成唯一文件名
        String uniqueFilename = UUID.randomUUID().toString() + "." + extension;

        // 构建完整路径: images/{type}/yyyy/MM/dd/{filename}
        return Paths.get(IMAGES_DIR, imageType.getDirectory(), dateDir, uniqueFilename).toString();
    }

    /**
     * 获取文件扩展名
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

    // ==================== 文件保存 ====================

    /**
     * 保存文件到本地
     */
    private void saveFile(MultipartFile file, String relativePath) throws IOException {
        Path fullPath = getAbsoluteUploadPath().resolve(relativePath);

        // 确保父目录存在
        Path parentDir = fullPath.getParent();
        if (parentDir != null) {
            try {
                if (!Files.exists(parentDir)) {
                    Files.createDirectories(parentDir);
                    setDirectoryPermissions(parentDir, "777");
                    logger.info("创建上传目录: {}", parentDir.toAbsolutePath());
                }

                if (!Files.isWritable(parentDir)) {
                    try {
                        setDirectoryPermissions(parentDir, "777");
                        if (!Files.isWritable(parentDir)) {
                            throw new IOException("上传目录不可写: " + parentDir.toAbsolutePath());
                        }
                    } catch (UnsupportedOperationException e) {
                        logger.warn("系统不支持POSIX权限设置，继续尝试保存文件");
                    }
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

    // ==================== 权限设置 ====================

    /**
     * 设置目录权限
     */
    private void setDirectoryPermissions(Path path, String permissionString) {
        try {
            Set<PosixFilePermission> permissions = new HashSet<>();
            int mode = Integer.parseInt(permissionString, 8);

            if ((mode & 0400) != 0) permissions.add(PosixFilePermission.OWNER_READ);
            if ((mode & 0200) != 0) permissions.add(PosixFilePermission.OWNER_WRITE);
            if ((mode & 0100) != 0) permissions.add(PosixFilePermission.OWNER_EXECUTE);
            if ((mode & 0040) != 0) permissions.add(PosixFilePermission.GROUP_READ);
            if ((mode & 0020) != 0) permissions.add(PosixFilePermission.GROUP_WRITE);
            if ((mode & 0010) != 0) permissions.add(PosixFilePermission.GROUP_EXECUTE);
            if ((mode & 0004) != 0) permissions.add(PosixFilePermission.OTHERS_READ);
            if ((mode & 0002) != 0) permissions.add(PosixFilePermission.OTHERS_WRITE);
            if ((mode & 0001) != 0) permissions.add(PosixFilePermission.OTHERS_EXECUTE);

            Files.setPosixFilePermissions(path, permissions);
            logger.debug("设置目录权限为 {}: {}", permissionString, path.toAbsolutePath());
        } catch (IOException e) {
            logger.debug("设置目录权限失败: {}", e.getMessage());
        } catch (UnsupportedOperationException e) {
            logger.debug("当前系统不支持POSIX权限设置");
        }
    }

    // ==================== 路径工具 ====================

    /**
     * 获取上传路径的绝对路径
     */
    private Path getAbsoluteUploadPath() {
        Path path = Paths.get(uploadBasePath);
        if (path.isAbsolute()) {
            return path;
        } else {
            return Paths.get(System.getProperty("user.dir"), uploadBasePath).toAbsolutePath();
        }
    }

    /**
     * 构建文件访问URL
     * Why: 生产环境返回完整URL，避免前端端口不一致导致的404
     */
    private String buildAccessUrl(String relativePath) {
        String normalizedPath = relativePath.replace(File.separator, "/");
        // 移除末尾的斜杠，避免双斜杠
        String baseUrl = accessBaseUrl.endsWith("/") ? accessBaseUrl.substring(0, accessBaseUrl.length() - 1) : accessBaseUrl;
        return baseUrl + "/uploads/" + normalizedPath;
    }

    // ==================== 文件删除 ====================

    /**
     * 删除本地文件
     */
    public boolean deleteFile(String fileUrl) {
        logger.info("开始删除本地文件: {}", fileUrl);

        try {
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
     */
    private String extractRelativePath(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return null;
        }

        String prefix = "/uploads/";
        int prefixIndex = fileUrl.indexOf(prefix);
        if (prefixIndex != -1) {
            return fileUrl.substring(prefixIndex + prefix.length());
        }

        return null;
    }

    /**
     * 检查本地文件是否存在
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
