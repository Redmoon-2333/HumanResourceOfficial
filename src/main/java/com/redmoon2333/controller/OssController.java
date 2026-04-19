package com.redmoon2333.controller;

import com.redmoon2333.annotation.RequireMemberRole;
import com.redmoon2333.dto.ApiResponse;
import com.redmoon2333.dto.PresignedUrlRequest;
import com.redmoon2333.dto.PresignedUrlResponse;
import com.redmoon2333.entity.Material;
import com.redmoon2333.service.ActivityService;
import com.redmoon2333.service.MaterialService;
import com.redmoon2333.util.OssUtil;
import com.aliyun.oss.model.OSSObject;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
    @RequireMemberRole("生成预签名URL")
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
    @RequireMemberRole("获取资料预签名URL")
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
    @RequireMemberRole("获取活动图片预签名URL")
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
     * 根据资料ID直接下载文件（服务端代理，使用 OSS SDK 绕过浏览器 Origin/Referer 策略）
     * 原理: 浏览器 Origin 头触发 OSS Referer 拦截，服务端通过 SDK 直连 OSS 无此问题
     * @param materialId 资料ID
     * @param response HTTP 响应
     */
    @RequireMemberRole("下载资料")
    @GetMapping("/download/material/{materialId}")
    public void downloadMaterial(@PathVariable Integer materialId, HttpServletResponse response) {
        logger.info("收到资料下载请求: materialId={}", materialId);

        OSSObject ossObject = null;
        try {
            Material material = materialService.getMaterialForDownload(materialId);

            String fileUrl = material.getFileUrl();
            if (fileUrl == null || fileUrl.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"message\":\"未找到指定的资料文件\"}");
                return;
            }

            String filePath = extractFilePathFromUrl(fileUrl);
            if (filePath == null || filePath.isEmpty()) {
                logger.error("无法从 URL 中提取文件路径: {}", fileUrl);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"message\":\"文件路径解析失败\"}");
                return;
            }

            ossObject = ossUtil.getFileStream(filePath);

            String contentType = ossObject.getObjectMetadata().getContentType();
            long contentLength = ossObject.getObjectMetadata().getContentLength();

            String downloadFilename = material.getMaterialName();
            String fileType = material.getFileType();
            if (fileType != null && !fileType.isEmpty() && !downloadFilename.endsWith(fileType)) {
                downloadFilename = downloadFilename + fileType;
            }

            String encodedFilename = URLEncoder.encode(downloadFilename, StandardCharsets.UTF_8)
                    .replace("+", "%20");

            response.setContentType(contentType != null ? contentType : "application/octet-stream");
            response.setContentLengthLong(contentLength);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename);

            try (InputStream in = ossObject.getObjectContent();
                 OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                out.flush();
            }

            materialService.incrementDownloadCount(materialId);
            logger.info("资料下载完成: materialId={}, 文件名={}", materialId, downloadFilename);
        } catch (Exception e) {
            logger.error("资料下载失败: materialId={}", materialId, e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"message\":\"下载失败: " + e.getMessage() + "\"}");
            } catch (Exception ex) {
                logger.error("写入错误响应失败", ex);
            }
        } finally {
            if (ossObject != null) {
                try {
                    ossObject.close();
                } catch (Exception e) {
                    logger.warn("关闭 OSSObject 失败", e);
                }
            }
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

        // 处理标准OSS URL的情况
        // 例如: https://bucket-name.oss-cn-hangzhou.aliyuncs.com/path/to/file.pdf -> path/to/file.pdf
        // 跳过协议部分 (https:// 或 http://)
        int protocolEndIndex = fileUrl.indexOf("://");
        if (protocolEndIndex >= 0) {
            int pathStartIndex = fileUrl.indexOf('/', protocolEndIndex + 3);
            if (pathStartIndex >= 0 && pathStartIndex < fileUrl.length() - 1) {
                return fileUrl.substring(pathStartIndex + 1);
            }
        }

        // 如果没有协议前缀，查找第一个斜杠
        int firstSlashIndex = fileUrl.indexOf('/');
        if (firstSlashIndex >= 0 && firstSlashIndex < fileUrl.length() - 1) {
            return fileUrl.substring(firstSlashIndex + 1);
        }

        // 如果URL中没有路径，返回原始URL
        return fileUrl;
    }
}