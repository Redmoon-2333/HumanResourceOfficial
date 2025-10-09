package com.redmoon2333.controller;

import com.redmoon2333.dto.ApiResponse;
import com.redmoon2333.dto.PresignedUrlRequest;
import com.redmoon2333.dto.PresignedUrlResponse;
import com.redmoon2333.service.ActivityService;
import com.redmoon2333.service.MaterialService;
import com.redmoon2333.util.OssUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/oss")
public class OssController {
    
    private static final Logger logger = LoggerFactory.getLogger(OssController.class);
    
    @Autowired
    private OssUtil ossUtil;
    
    @Autowired
    private MaterialService materialService;
    
    @Autowired
    private ActivityService activityService;
    
    /**
     * 生成预签名URL用于临时访问私有文件
     * @param request 包含文件路径和过期时间的请求
     * @return 预签名URL响应
     */
    @PostMapping("/presigned-url")
    public ApiResponse<PresignedUrlResponse> generatePresignedUrl(@Valid @RequestBody PresignedUrlRequest request) {
        logger.info("收到生成预签名URL请求: 文件路径={}, 过期时间={}秒", request.getFilePath(), request.getExpirationSeconds());
        
        try {
            String presignedUrl = ossUtil.generatePresignedUrl(request.getFilePath(), request.getExpirationSeconds());
            
            PresignedUrlResponse response = new PresignedUrlResponse();
            response.setPresignedUrl(presignedUrl);
            response.setFileName(request.getFilePath());
            
            // 计算过期时间戳
            long expirationTime = System.currentTimeMillis() + (request.getExpirationSeconds() != null ? 
                    request.getExpirationSeconds() : 3600L) * 1000;
            response.setExpirationTime(expirationTime);
            
            logger.info("预签名URL生成成功: {}", presignedUrl);
            return ApiResponse.success(response);
        } catch (Exception e) {
            logger.error("生成预签名URL失败", e);
            return ApiResponse.error("生成预签名URL失败: " + e.getMessage(), 500);
        }
    }
    
    /**
     * 根据资料ID获取预签名URL
     * @param materialId 资料ID
     * @param expirationSeconds 过期时间（秒）
     * @return 预签名URL响应
     */
    @GetMapping("/presigned-url/material/{materialId}")
    public ApiResponse<PresignedUrlResponse> generatePresignedUrlForMaterial(
            @PathVariable Integer materialId,
            @RequestParam(defaultValue = "3600") Long expirationSeconds) {
        logger.info("收到为资料生成预签名URL请求: materialId={}, 过期时间={}秒", materialId, expirationSeconds);
        
        try {
            // 获取资料信息
            String fileUrl = materialService.getFileUrlById(materialId);
            if (fileUrl == null) {
                logger.warn("未找到ID为{}的资料", materialId);
                return ApiResponse.error("未找到指定的资料", 404);
            }
            
            // 从完整URL中提取文件路径
            String filePath = extractFilePathFromUrl(fileUrl);
            if (filePath == null || filePath.isEmpty()) {
                logger.error("无法从URL中提取文件路径: {}", fileUrl);
                return ApiResponse.error("文件路径解析失败", 500);
            }
            
            String presignedUrl = ossUtil.generatePresignedUrl(filePath, expirationSeconds);
            
            PresignedUrlResponse response = new PresignedUrlResponse();
            response.setPresignedUrl(presignedUrl);
            response.setFileName(filePath);
            
            // 计算过期时间戳
            long expirationTime = System.currentTimeMillis() + expirationSeconds * 1000;
            response.setExpirationTime(expirationTime);
            
            logger.info("为资料生成预签名URL成功: materialId={}, URL={}", materialId, presignedUrl);
            return ApiResponse.success(response);
        } catch (Exception e) {
            logger.error("为资料生成预签名URL失败", e);
            return ApiResponse.error("生成预签名URL失败: " + e.getMessage(), 500);
        }
    }
    
    /**
     * 根据活动图片ID获取预签名URL
     * @param imageId 图片ID
     * @param expirationSeconds 过期时间（秒）
     * @return 预签名URL响应
     */
    @GetMapping("/presigned-url/activity-image/{imageId}")
    public ApiResponse<PresignedUrlResponse> generatePresignedUrlForActivityImage(
            @PathVariable Integer imageId,
            @RequestParam(defaultValue = "3600") Long expirationSeconds) {
        logger.info("收到为活动图片生成预签名URL请求: imageId={}, 过期时间={}秒", imageId, expirationSeconds);
        
        try {
            // 获取图片信息
            String fileUrl = activityService.getImageUrlById(imageId);
            if (fileUrl == null) {
                logger.warn("未找到ID为{}的活动图片", imageId);
                return ApiResponse.error("未找到指定的活动图片", 404);
            }
            
            // 从完整URL中提取文件路径
            String filePath = extractFilePathFromUrl(fileUrl);
            if (filePath == null || filePath.isEmpty()) {
                logger.error("无法从URL中提取文件路径: {}", fileUrl);
                return ApiResponse.error("文件路径解析失败", 500);
            }
            
            String presignedUrl = ossUtil.generatePresignedUrl(filePath, expirationSeconds);
            
            PresignedUrlResponse response = new PresignedUrlResponse();
            response.setPresignedUrl(presignedUrl);
            response.setFileName(filePath);
            
            // 计算过期时间戳
            long expirationTime = System.currentTimeMillis() + expirationSeconds * 1000;
            response.setExpirationTime(expirationTime);
            
            logger.info("为活动图片生成预签名URL成功: imageId={}, URL={}", imageId, presignedUrl);
            return ApiResponse.success(response);
        } catch (Exception e) {
            logger.error("为活动图片生成预签名URL失败", e);
            return ApiResponse.error("生成预签名URL失败: " + e.getMessage(), 500);
        }
    }
    
    /**
     * 从完整URL中提取OSS文件路径
     * @param fileUrl 完整的文件URL
     * @return 文件路径
     */
    private String extractFilePathFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return null;
        }
        
        // 处理自定义域名的情况
        // 例如: https://oss.example.com/path/to/file.pdf -> path/to/file.pdf
        int lastSlashIndex = fileUrl.lastIndexOf("/");
        if (lastSlashIndex >= 0 && lastSlashIndex < fileUrl.length() - 1) {
            return fileUrl.substring(lastSlashIndex + 1);
        }
        
        // 处理标准OSS URL的情况
        // 例如: https://bucket-name.oss-cn-hangzhou.aliyuncs.com/path/to/file.pdf -> path/to/file.pdf
        int thirdSlashIndex = -1;
        int slashCount = 0;
        for (int i = 0; i < fileUrl.length(); i++) {
            if (fileUrl.charAt(i) == '/') {
                slashCount++;
                if (slashCount == 3) {
                    thirdSlashIndex = i;
                    break;
                }
            }
        }
        
        if (thirdSlashIndex >= 0 && thirdSlashIndex < fileUrl.length() - 1) {
            return fileUrl.substring(thirdSlashIndex + 1);
        }
        
        return fileUrl;
    }
}