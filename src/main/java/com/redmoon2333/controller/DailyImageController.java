package com.redmoon2333.controller;

import com.redmoon2333.annotation.RequireMinisterRole;
import com.redmoon2333.dto.ApiResponse;
import com.redmoon2333.entity.DailyImage;
import com.redmoon2333.service.DailyImageService;
import com.redmoon2333.util.LocalFileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 日常活动图片控制器
 * 提供"我们的日常"板块轮播图的RESTful API
 *
 * <p>API设计遵循RESTful规范：</p>
 * <ul>
 *   <li>GET /api/daily-images - 获取所有启用的图片（公开接口）</li>
 *   <li>GET /api/daily-images/all - 获取所有图片（管理接口）</li>
 *   <li>GET /api/daily-images/{id} - 获取图片详情</li>
 *   <li>POST /api/daily-images - 添加图片</li>
 *   <li>PUT /api/daily-images/{id} - 更新图片</li>
 *   <li>DELETE /api/daily-images/{id} - 删除图片</li>
 *   <li>PUT /api/daily-images/{id}/status - 更新图片状态</li>
 * </ul>
 *
 * @author 人力资源中心技术组
 * @since 2026-02-13
 */
@RestController
@RequestMapping("/api/daily-images")
public class DailyImageController {

    private static final Logger logger = LoggerFactory.getLogger(DailyImageController.class);

    @Autowired
    private DailyImageService dailyImageService;

    @Autowired
    private LocalFileUtil localFileUtil;

    /**
     * 获取所有启用的图片列表
     * 用于前端首页"我们的日常"板块轮播图展示
     *
     * @return 启用的图片列表
     */
    @GetMapping
    public ApiResponse<List<DailyImage>> getActiveImages() {
        logger.debug("获取启用的日常活动图片列表");
        List<DailyImage> images = dailyImageService.getActiveImages();
        return ApiResponse.success(images);
    }

    /**
     * 获取所有图片列表（包含禁用状态）
     * 用于后台管理
     *
     * @return 所有图片列表
     */
    @GetMapping("/all")
    @RequireMinisterRole("获取所有日常活动图片")
    public ApiResponse<List<DailyImage>> getAllImages() {
        logger.debug("获取所有日常活动图片列表");
        List<DailyImage> images = dailyImageService.getAllImages();
        return ApiResponse.success(images);
    }

    /**
     * 根据ID获取图片详情
     *
     * @param id 图片ID
     * @return 图片详情
     */
    @GetMapping("/{id}")
    @RequireMinisterRole("获取日常活动图片详情")
    public ApiResponse<DailyImage> getImageById(@PathVariable Integer id) {
        logger.debug("获取图片详情，ID: {}", id);
        DailyImage image = dailyImageService.getImageById(id);
        if (image == null) {
            return ApiResponse.error("图片不存在", 404);
        }
        return ApiResponse.success(image);
    }

    /**
     * 添加新图片
     *
     * @param dailyImage 图片信息
     * @return 添加后的图片
     */
    @PostMapping
    @RequireMinisterRole("添加日常活动图片")
    public ApiResponse<DailyImage> addImage(@RequestBody DailyImage dailyImage) {
        logger.info("添加新图片: {}", dailyImage.getTitle());
        try {
            DailyImage saved = dailyImageService.addImage(dailyImage);
            return ApiResponse.success(saved);
        } catch (IllegalArgumentException e) {
            logger.warn("添加图片参数错误: {}", e.getMessage());
            return ApiResponse.error(e.getMessage(), 400);
        } catch (Exception e) {
            logger.error("添加图片失败", e);
            return ApiResponse.error("添加图片失败: " + e.getMessage(), 500);
        }
    }

    /**
     * 更新图片信息
     *
     * @param id 图片ID
     * @param dailyImage 图片信息
     * @return 更新后的图片
     */
    @PutMapping("/{id}")
    @RequireMinisterRole("更新日常活动图片")
    public ApiResponse<DailyImage> updateImage(
            @PathVariable Integer id,
            @RequestBody DailyImage dailyImage) {
        logger.info("更新图片，ID: {}", id);
        dailyImage.setImageId(id);
        try {
            DailyImage updated = dailyImageService.updateImage(dailyImage);
            return ApiResponse.success(updated);
        } catch (IllegalArgumentException e) {
            logger.warn("更新图片参数错误: {}", e.getMessage());
            return ApiResponse.error(e.getMessage(), 400);
        } catch (Exception e) {
            logger.error("更新图片失败", e);
            return ApiResponse.error("更新图片失败: " + e.getMessage(), 500);
        }
    }

    /**
     * 删除图片
     *
     * @param id 图片ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @RequireMinisterRole("删除日常活动图片")
    public ApiResponse<Void> deleteImage(@PathVariable Integer id) {
        logger.info("删除图片，ID: {}", id);
        try {
            dailyImageService.deleteImage(id);
            return ApiResponse.success(null);
        } catch (Exception e) {
            logger.error("删除图片失败", e);
            return ApiResponse.error("删除图片失败: " + e.getMessage(), 500);
        }
    }

    /**
     * 批量删除图片
     *
     * @param request 包含imageIds的请求体
     * @return 操作结果
     */
    @PostMapping("/batch-delete")
    @RequireMinisterRole("批量删除日常活动图片")
    public ApiResponse<Void> batchDeleteImages(@RequestBody Map<String, List<Integer>> request) {
        List<Integer> imageIds = request.get("imageIds");
        if (imageIds == null || imageIds.isEmpty()) {
            return ApiResponse.error("图片ID列表不能为空", 400);
        }
        logger.info("批量删除图片，数量: {}", imageIds.size());
        try {
            dailyImageService.batchDeleteImages(imageIds);
            return ApiResponse.success(null);
        } catch (Exception e) {
            logger.error("批量删除图片失败", e);
            return ApiResponse.error("批量删除图片失败: " + e.getMessage(), 500);
        }
    }

    /**
     * 更新图片状态（启用/禁用）
     *
     * @param id 图片ID
     * @param request 包含isActive的请求体
     * @return 操作结果
     */
    @PutMapping("/{id}/status")
    @RequireMinisterRole("更新日常活动图片状态")
    public ApiResponse<Void> updateImageStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, Boolean> request) {
        Boolean isActive = request.get("isActive");
        if (isActive == null) {
            return ApiResponse.error("状态参数不能为空", 400);
        }
        logger.info("更新图片状态，ID: {}, 状态: {}", id, isActive);
        try {
            dailyImageService.updateImageStatus(id, isActive);
            return ApiResponse.success(null);
        } catch (Exception e) {
            logger.error("更新图片状态失败", e);
            return ApiResponse.error("更新图片状态失败: " + e.getMessage(), 500);
        }
    }

    /**
     * 交换图片排序
     *
     * @param request 包含imageId1和imageId2的请求体
     * @return 操作结果
     */
    @PostMapping("/swap-order")
    @RequireMinisterRole("交换日常活动图片排序")
    public ApiResponse<Void> swapImageOrder(@RequestBody Map<String, Integer> request) {
        Integer imageId1 = request.get("imageId1");
        Integer imageId2 = request.get("imageId2");
        if (imageId1 == null || imageId2 == null) {
            return ApiResponse.error("图片ID不能为空", 400);
        }
        logger.info("交换图片排序，ID: {} <-> {}", imageId1, imageId2);
        try {
            dailyImageService.swapImageOrder(imageId1, imageId2);
            return ApiResponse.success(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage(), 400);
        } catch (Exception e) {
            logger.error("交换图片排序失败", e);
            return ApiResponse.error("交换图片排序失败: " + e.getMessage(), 500);
        }
    }

    /**
     * 上传图片
     * 支持直接上传图片文件，自动保存到本地存储并创建数据库记录
     *
     * @param file        图片文件
     * @param title       图片标题（可选）
     * @param description 图片描述（可选）
     * @return 上传后的图片信息
     */
    @PostMapping("/upload")
    @RequireMinisterRole("上传日常活动图片")
    public ApiResponse<DailyImage> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description) {

        logger.info("上传日常活动图片: 文件名={}, 标题={}", file.getOriginalFilename(), title);

        // 参数校验
        if (file.isEmpty()) {
            return ApiResponse.error("图片文件不能为空", 400);
        }

        // 设置默认标题
        if (title == null || title.trim().isEmpty()) {
            title = file.getOriginalFilename();
        }

        try {
            DailyImage savedImage = dailyImageService.uploadImage(file, title, description);
            return ApiResponse.success(savedImage);
        } catch (IllegalArgumentException e) {
            logger.warn("上传图片参数错误: {}", e.getMessage());
            return ApiResponse.error(e.getMessage(), 400);
        } catch (IOException e) {
            logger.error("上传图片文件失败", e);
            return ApiResponse.error("文件上传失败: " + e.getMessage(), 500);
        } catch (Exception e) {
            logger.error("上传图片失败", e);
            return ApiResponse.error("上传失败: " + e.getMessage(), 500);
        }
    }

    /**
     * 仅上传图片文件，不创建数据库记录
     * 用于编辑图片时替换文件
     *
     * @param file 图片文件
     * @return 上传后的图片URL
     */
    @PostMapping("/upload-file")
    @RequireMinisterRole("上传日常活动图片文件")
    public ApiResponse<String> uploadImageFile(@RequestParam("file") MultipartFile file) {
        logger.info("上传日常活动图片文件: 文件名={}", file.getOriginalFilename());

        // 参数校验
        if (file.isEmpty()) {
            return ApiResponse.error("图片文件不能为空", 400);
        }

        try {
            String imageUrl = dailyImageService.uploadImageFile(file);
            return ApiResponse.success(imageUrl);
        } catch (IllegalArgumentException e) {
            logger.warn("上传图片文件参数错误: {}", e.getMessage());
            return ApiResponse.error(e.getMessage(), 400);
        } catch (IOException e) {
            logger.error("上传图片文件失败", e);
            return ApiResponse.error("文件上传失败: " + e.getMessage(), 500);
        } catch (Exception e) {
            logger.error("上传图片文件失败", e);
            return ApiResponse.error("上传失败: " + e.getMessage(), 500);
        }
    }

    /**
     * 按类型上传图片文件
     * 支持不同模块的图片上传到对应目录
     *
     * @param file 图片文件
     * @param type 图片类型 (daily-我们的日常, activity-活动照片, avatar-用户头像)
     * @return 上传后的图片URL
     */
    @PostMapping("/upload-by-type")
    @RequireMinisterRole("上传图片文件")
    public ApiResponse<String> uploadImageByType(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type) {
        logger.info("按类型上传图片文件: 文件名={}, 类型={}", file.getOriginalFilename(), type);

        // 参数校验
        if (file.isEmpty()) {
            return ApiResponse.error("图片文件不能为空", 400);
        }

        try {
            LocalFileUtil.ImageType imageType;
            switch (type) {
                case "daily":
                    imageType = LocalFileUtil.ImageType.DAILY;
                    break;
                case "activity":
                    imageType = LocalFileUtil.ImageType.ACTIVITY;
                    break;
                case "avatar":
                    imageType = LocalFileUtil.ImageType.AVATAR;
                    break;
                default:
                    return ApiResponse.error("不支持的图片类型", 400);
            }

            String imageUrl = localFileUtil.uploadImage(file, imageType);
            return ApiResponse.success("上传成功", imageUrl);
        } catch (IllegalArgumentException e) {
            logger.warn("上传图片文件参数错误: {}", e.getMessage());
            return ApiResponse.error(e.getMessage(), 400);
        } catch (IOException e) {
            logger.error("上传图片文件失败", e);
            return ApiResponse.error("文件上传失败: " + e.getMessage(), 500);
        } catch (Exception e) {
            logger.error("上传图片文件失败", e);
            return ApiResponse.error("上传失败: " + e.getMessage(), 500);
        }
    }

    /**
     * 删除图片并删除本地文件
     *
     * @param id 图片ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}/with-file")
    @RequireMinisterRole("删除日常活动图片及文件")
    public ApiResponse<Void> deleteImageWithFile(@PathVariable Integer id) {
        logger.info("删除图片及本地文件，ID: {}", id);
        try {
            dailyImageService.deleteImageWithFile(id);
            return ApiResponse.success(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage(), 400);
        } catch (Exception e) {
            logger.error("删除图片失败", e);
            return ApiResponse.error("删除失败: " + e.getMessage(), 500);
        }
    }
}
