package com.redmoon2333.util;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.PutObjectRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class OssUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(OssUtil.class);
    
    @Value("${aliyun.oss.endpoint:}")
    private String endpoint;
    
    @Value("${aliyun.oss.accessKeyId:}")
    private String accessKeyId;
    
    @Value("${aliyun.oss.accessKeySecret:}")
    private String accessKeySecret;
    
    @Value("${aliyun.oss.bucketName:}")
    private String bucketName;
    
    @Value("${aliyun.oss.domain:}")
    private String domain;
    
    private OSS ossClient;
    
    // 通过依赖注入获取OSS客户端
    public void setOssClient(OSS ossClient) {
        this.ossClient = ossClient;
        logger.info("OSS客户端注入状态: {}", ossClient != null ? "成功" : "失败");
    }
    
    /**
     * 检查OSS配置是否完整
     * @return 配置是否完整
     */
    private boolean isOssConfigured() {
        return endpoint != null && !endpoint.isEmpty() &&
               accessKeyId != null && !accessKeyId.isEmpty() &&
               accessKeySecret != null && !accessKeySecret.isEmpty() &&
               bucketName != null && !bucketName.isEmpty();
    }
    
    /**
     * 尝试手动创建OSS客户端（当自动注入失败时）
     */
    private void tryCreateOssClient() {
        if (ossClient == null && isOssConfigured()) {
            try {
                logger.info("尝试手动创建OSS客户端");
                ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
                logger.info("手动创建OSS客户端成功");
            } catch (Exception e) {
                logger.error("手动创建OSS客户端失败", e);
            }
        }
    }
    
    /**
     * 上传文件到OSS
     * @param file 要上传的文件
     * @return 文件在OSS中的URL路径
     * @throws IOException
     */
    public String uploadFile(MultipartFile file) throws IOException {
        logger.info("开始上传文件到OSS: 文件名={}, 大小={} bytes", file.getOriginalFilename(), file.getSize());
        
        // 如果OSS客户端未初始化，尝试手动创建
        if (ossClient == null) {
            tryCreateOssClient();
        }
        
        // 如果OSS客户端仍未初始化，说明缺少配置或创建失败，抛出异常
        if (ossClient == null) {
            String errorMessage = String.format(
                "OSS客户端未配置或创建失败。配置检查: endpoint=%s, accessKeyId=%s, accessKeySecret=%s, bucketName=%s。" +
                "请检查阿里云OSS配置是否正确，或确认OSS配置类是否被正确加载。",
                endpoint != null ? endpoint : "未配置",
                accessKeyId != null ? maskSensitiveInfo(accessKeyId) : "未配置",
                accessKeySecret != null ? maskSensitiveInfo(accessKeySecret) : "未配置",
                bucketName != null ? bucketName : "未配置"
            );
            logger.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
        
        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
        String uniqueFilename = UUID.randomUUID().toString() + extension;
        
        logger.debug("生成唯一文件名: {}", uniqueFilename);

        // 创建PutObjectRequest
        PutObjectRequest request = new PutObjectRequest(bucketName, uniqueFilename, file.getInputStream());

        // 上传文件
        ossClient.putObject(request);
        
        // 构建并返回文件URL
        String fileUrl = buildFileUrl(uniqueFilename);
        logger.info("文件上传成功，URL: {}", fileUrl);
        
        return fileUrl;
    }
    
    /**
     * 删除OSS中的文件
     * @param filePath 文件路径
     */
    public void deleteFile(String filePath) {
        logger.info("开始删除OSS文件: {}", filePath);
        
        // 如果OSS客户端未初始化，尝试手动创建
        if (ossClient == null) {
            tryCreateOssClient();
        }
        
        if (ossClient != null) {
            ossClient.deleteObject(bucketName, filePath);
            logger.info("OSS文件删除成功: {}", filePath);
        } else {
            String errorMessage = String.format(
                "OSS客户端未配置或创建失败。配置检查: endpoint=%s, accessKeyId=%s, accessKeySecret=%s, bucketName=%s。" +
                "请检查阿里云OSS配置是否正确，或确认OSS配置类是否被正确加载。",
                endpoint != null ? endpoint : "未配置",
                accessKeyId != null ? maskSensitiveInfo(accessKeyId) : "未配置",
                accessKeySecret != null ? maskSensitiveInfo(accessKeySecret) : "未配置",
                bucketName != null ? bucketName : "未配置"
            );
            logger.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
    }
    
    /**
     * 生成预签名URL用于临时访问私有文件
     * @param filePath 文件路径
     * @param expirationSeconds 过期时间（秒）
     * @return 预签名URL
     */
    public String generatePresignedUrl(String filePath, Long expirationSeconds) {
        logger.info("开始生成预签名URL: 文件路径={}, 过期时间={}秒", filePath, expirationSeconds);
        
        // 如果OSS客户端未初始化，尝试手动创建
        if (ossClient == null) {
            tryCreateOssClient();
        }
        
        if (ossClient != null) {
            // 设置默认过期时间为1小时
            if (expirationSeconds == null || expirationSeconds <= 0) {
                expirationSeconds = 3600L; // 默认1小时
            }
            
            // 设置URL过期时间
            Date expiration = new Date(new Date().getTime() + expirationSeconds * 1000);
            
            // 生成预签名URL
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, filePath);
            request.setExpiration(expiration);
            
            URL url = ossClient.generatePresignedUrl(request);
            String presignedUrl = url.toString();
            
            logger.info("预签名URL生成成功: {}", presignedUrl);
            return presignedUrl;
        } else {
            String errorMessage = String.format(
                "OSS客户端未配置或创建失败。配置检查: endpoint=%s, accessKeyId=%s, accessKeySecret=%s, bucketName=%s。" +
                "请检查阿里云OSS配置是否正确，或确认OSS配置类是否被正确加载。",
                endpoint != null ? endpoint : "未配置",
                accessKeyId != null ? maskSensitiveInfo(accessKeyId) : "未配置",
                accessKeySecret != null ? maskSensitiveInfo(accessKeySecret) : "未配置",
                bucketName != null ? bucketName : "未配置"
            );
            logger.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
    }
    
    /**
     * 遮蔽敏感信息，只显示前几位和后几位字符
     * @param sensitiveInfo 敏感信息
     * @return 遮蔽后的信息
     */
    private String maskSensitiveInfo(String sensitiveInfo) {
        if (sensitiveInfo == null || sensitiveInfo.isEmpty()) {
            return "未配置";
        }
        
        if (sensitiveInfo.length() <= 8) {
            // 对于较短的信息，只显示第一位和最后一位
            return sensitiveInfo.charAt(0) + "******" + 
                   sensitiveInfo.charAt(sensitiveInfo.length()-1);
        } else {
            // 对于较长的信息，显示前3位和后3位
            return sensitiveInfo.substring(0, 3) + "******" + 
                   sensitiveInfo.substring(sensitiveInfo.length()-3);
        }
    }
    
    /**
     * 根据文件名获取文件内容类型
     * @param filename 文件名
     * @return 内容类型
     */
    private String getContentType(String filename) {
        String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        switch (ext) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "pdf":
                return "application/pdf";
            case "doc":
            case "docx":
                return "application/msword";
            case "xls":
            case "xlsx":
                return "application/vnd.ms-excel";
            default:
                return "application/octet-stream";
        }
    }
    
    /**
     * 构建文件访问URL
     * @param filePath 文件路径
     * @return 完整的URL
     */
    private String buildFileUrl(String filePath) {
        String fileUrl;
        if (domain != null && !domain.isEmpty()) {
            fileUrl = domain + "/" + filePath;
        } else {
            fileUrl = "https://" + bucketName + "." + endpoint + "/" + filePath;
        }
        logger.debug("构建文件URL: {}", fileUrl);
        return fileUrl;
    }
    
    // OSS客户端由配置类管理，无需手动关闭
}