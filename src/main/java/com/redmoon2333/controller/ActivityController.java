package com.redmoon2333.controller;

import com.redmoon2333.annotation.RequireMinisterRole;
import com.redmoon2333.dto.ActivityImageDTO;
import com.redmoon2333.dto.ActivityRequest;
import com.redmoon2333.dto.ActivityResponse;
import com.redmoon2333.entity.Activity;
import com.redmoon2333.entity.ActivityImage;
import com.redmoon2333.exception.BusinessException;
import com.redmoon2333.exception.ErrorCode;
import com.redmoon2333.service.ActivityService;
import com.redmoon2333.dto.ApiResponse;
import com.redmoon2333.util.PermissionUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {
    
    private static final Logger logger = LoggerFactory.getLogger(ActivityController.class);
    
    @Autowired
    private ActivityService activityService;
    
    @Autowired
    private PermissionUtil permissionUtil;
    
    /**
     * 创建新活动
     * 只有部长或副部长才能创建活动
     */
    @PostMapping
    @RequireMinisterRole("创建活动")
    public ResponseEntity<ApiResponse<ActivityResponse>> createActivity(@Valid @RequestBody ActivityRequest activityRequest) {
        String currentUser = permissionUtil.getCurrentUsername();
        logger.info("用户 {} 尝试创建活动: {}", currentUser, activityRequest.getActivityName());
        
        try {
            Activity activity = new Activity();
            BeanUtils.copyProperties(activityRequest, activity);
            // 确保设置创建时间和更新时间
            activity.setCreateTime(LocalDateTime.now());
            activity.setUpdateTime(LocalDateTime.now());
            
            Activity savedActivity = activityService.createActivity(activity);
            
            ActivityResponse response = new ActivityResponse();
            BeanUtils.copyProperties(savedActivity, response);
            
            logger.info("用户 {} 成功创建活动: ID={}, 名称={}", currentUser, savedActivity.getActivityId(), savedActivity.getActivityName());
            return ResponseEntity.ok(ApiResponse.success("活动创建成功", response));
        } catch (BusinessException e) {
            logger.error("用户 {} 创建活动失败: {}", currentUser, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("用户 {} 创建活动时发生异常: {}", currentUser, e.getMessage(), e);
            throw new BusinessException(ErrorCode.ACTIVITY_CREATION_FAILED);
        }
    }
    
    /**
     * 根据ID获取活动详情
     * 公开接口，无需鉴权
     */
    @GetMapping("/{activityId}")
    public ResponseEntity<ApiResponse<ActivityResponse>> getActivityById(@PathVariable Integer activityId) {
        logger.debug("公开查询活动详情: ID={}", activityId);
        
        try {
            Activity activity = activityService.getActivityById(activityId);
            
            ActivityResponse response = new ActivityResponse();
            BeanUtils.copyProperties(activity, response);
            
            logger.debug("成功查询活动: ID={}, 名称={}", activityId, activity.getActivityName());
            return ResponseEntity.ok(ApiResponse.success("查询成功", response));
        } catch (BusinessException e) {
            logger.warn("查询活动失败: ID={}, 错误: {}", activityId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("查询活动时发生异常: ID={}, 错误: {}", activityId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }
    
    /**
     * 获取所有活动列表
     * 公开接口，无需鉴权
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ActivityResponse>>> getAllActivities() {
        logger.debug("公开查询所有活动列表");
        
        try {
            List<Activity> activities = activityService.getAllActivities();
            
            List<ActivityResponse> responseList = activities.stream().map(activity -> {
                ActivityResponse response = new ActivityResponse();
                BeanUtils.copyProperties(activity, response);
                return response;
            }).collect(Collectors.toList());
            
            logger.info("成功查询所有活动，共 {} 个活动", responseList.size());
            return ResponseEntity.ok(ApiResponse.success("查询成功", responseList));
        } catch (Exception e) {
            logger.error("查询所有活动时发生异常: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }
    
    /**
     * 更新活动信息
     * 只有部长或副部长才能更新活动
     */
    @PutMapping("/{activityId}")
    @RequireMinisterRole("更新活动")
    public ResponseEntity<ApiResponse<ActivityResponse>> updateActivity(
            @PathVariable Integer activityId, 
            @Valid @RequestBody ActivityRequest activityRequest) {
        
        String currentUser = permissionUtil.getCurrentUsername();
        logger.info("用户 {} 尝试更新活动: ID={}, 新名称={}", currentUser, activityId, activityRequest.getActivityName());
        
        try {
            Activity existingActivity = activityService.getActivityById(activityId);
            
            BeanUtils.copyProperties(activityRequest, existingActivity);
            existingActivity.setActivityId(activityId);
            existingActivity.setUpdateTime(LocalDateTime.now());
            
            Activity updatedActivity = activityService.updateActivity(activityId, existingActivity);
            
            ActivityResponse response = new ActivityResponse();
            BeanUtils.copyProperties(updatedActivity, response);
            
            logger.info("用户 {} 成功更新活动: ID={}, 名称={}", currentUser, activityId, updatedActivity.getActivityName());
            return ResponseEntity.ok(ApiResponse.success("活动更新成功", response));
        } catch (BusinessException e) {
            logger.error("用户 {} 更新活动失败: ID={}, 错误: {}", currentUser, activityId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("用户 {} 更新活动时发生异常: ID={}, 错误: {}", currentUser, activityId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.ACTIVITY_UPDATE_FAILED);
        }
    }
    
    /**
     * 删除活动
     * 只有部长或副部长才能删除活动
     */
    @DeleteMapping("/{activityId}")
    @RequireMinisterRole("删除活动")
    public ResponseEntity<ApiResponse<Void>> deleteActivity(@PathVariable Integer activityId) {
        String currentUser = permissionUtil.getCurrentUsername();
        logger.info("用户 {} 尝试删除活动: ID={}", currentUser, activityId);
        
        try {
            // 先查询活动信息用于日志记录
            Activity activity = activityService.getActivityById(activityId);
            
            activityService.deleteActivity(activityId);
            
            logger.info("用户 {} 成功删除活动: ID={}, 名称={}", currentUser, activityId, activity.getActivityName());
            return ResponseEntity.ok(ApiResponse.success("活动删除成功"));
        } catch (BusinessException e) {
            logger.error("用户 {} 删除活动失败: ID={}, 错误: {}", currentUser, activityId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("用户 {} 删除活动时发生异常: ID={}, 错误: {}", currentUser, activityId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.ACTIVITY_DELETE_FAILED);
        }
    }
    
    /**
     * 为活动添加图片
     * 只有部长或副部长才能添加图片
     */
    @PostMapping("/{activityId}/images")
    @RequireMinisterRole("为活动添加图片")
    public ResponseEntity<ApiResponse<ActivityImageDTO>> addImageToActivity(
            @PathVariable Integer activityId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "sortOrder", defaultValue = "0") Integer sortOrder) {
        
        String currentUser = permissionUtil.getCurrentUsername();
        logger.info("用户 {} 尝试为活动添加图片: 活动ID={}", currentUser, activityId);
        
        try {
            // 保存文件并获取URL
            String imageUrl = activityService.saveImage(file);
            
            // 创建ActivityImage对象
            ActivityImage activityImage = new ActivityImage();
            activityImage.setActivityId(activityId);
            activityImage.setImageUrl(imageUrl);
            activityImage.setDescription(description);
            activityImage.setSortOrder(sortOrder);
            
            // 添加图片到活动
            ActivityImage savedImage = activityService.addImageToActivity(activityId, activityImage);
            
            // 转换为DTO
            ActivityImageDTO response = new ActivityImageDTO();
            BeanUtils.copyProperties(savedImage, response);
            
            logger.info("用户 {} 成功为活动添加图片: 活动ID={}, 图片ID={}", currentUser, activityId, savedImage.getImageId());
            return ResponseEntity.ok(ApiResponse.success("图片添加成功", response));
        } catch (IOException e) {
            logger.error("用户 {} 为活动添加图片时文件保存失败: 活动ID={}, 错误: {}", currentUser, activityId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件保存失败");
        } catch (BusinessException e) {
            logger.error("用户 {} 为活动添加图片失败: 活动ID={}, 错误: {}", currentUser, activityId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("用户 {} 为活动添加图片时发生异常: 活动ID={}, 错误: {}", currentUser, activityId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "添加图片失败");
        }
    }
    
    /**
     * 获取活动的所有图片
     * 公开接口，无需鉴权
     */
    @GetMapping("/{activityId}/images")
    public ResponseEntity<ApiResponse<List<ActivityImageDTO>>> getImagesByActivityId(@PathVariable Integer activityId) {
        logger.debug("公开查询活动的图片列表: 活动ID={}", activityId);
        
        try {
            List<ActivityImage> images = activityService.getImagesByActivityId(activityId);
            
            // 转换为DTO列表
            List<ActivityImageDTO> responseList = images.stream().map(image -> {
                ActivityImageDTO dto = new ActivityImageDTO();
                BeanUtils.copyProperties(image, dto);
                return dto;
            }).collect(Collectors.toList());
            
            logger.debug("成功查询活动的图片列表: 活动ID={}, 图片数量={}", activityId, responseList.size());
            return ResponseEntity.ok(ApiResponse.success("查询成功", responseList));
        } catch (BusinessException e) {
            logger.warn("查询活动图片列表失败: 活动ID={}, 错误: {}", activityId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("查询活动图片列表时发生异常: 活动ID={}, 错误: {}", activityId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "查询图片列表失败");
        }
    }
    
    /**
     * 更新活动图片信息
     * 只有部长或副部长才能更新图片
     */
    @PutMapping("/images/{imageId}")
    @RequireMinisterRole("更新活动图片")
    public ResponseEntity<ApiResponse<ActivityImageDTO>> updateActivityImage(
            @PathVariable Integer imageId,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "sortOrder", required = false) Integer sortOrder) {
        
        String currentUser = permissionUtil.getCurrentUsername();
        logger.info("用户 {} 尝试更新活动图片: 图片ID={}", currentUser, imageId);
        
        try {
            ActivityImage activityImage = new ActivityImage();
            activityImage.setDescription(description);
            activityImage.setSortOrder(sortOrder);
            
            ActivityImage updatedImage = activityService.updateActivityImage(imageId, activityImage);
            
            // 转换为DTO
            ActivityImageDTO response = new ActivityImageDTO();
            BeanUtils.copyProperties(updatedImage, response);
            
            logger.info("用户 {} 成功更新活动图片: 图片ID={}", currentUser, imageId);
            return ResponseEntity.ok(ApiResponse.success("图片更新成功", response));
        } catch (BusinessException e) {
            logger.error("用户 {} 更新活动图片失败: 图片ID={}, 错误: {}", currentUser, imageId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("用户 {} 更新活动图片时发生异常: 图片ID={}, 错误: {}", currentUser, imageId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新图片失败");
        }
    }
    
    /**
     * 删除活动图片
     * 只有部长或副部长才能删除图片
     */
    @DeleteMapping("/images/{imageId}")
    @RequireMinisterRole("删除活动图片")
    public ResponseEntity<ApiResponse<Void>> deleteActivityImage(@PathVariable Integer imageId) {
        String currentUser = permissionUtil.getCurrentUsername();
        logger.info("用户 {} 尝试删除活动图片: 图片ID={}", currentUser, imageId);
        
        try {
            activityService.deleteActivityImage(imageId);
            
            logger.info("用户 {} 成功删除活动图片: 图片ID={}", currentUser, imageId);
            return ResponseEntity.ok(ApiResponse.success("图片删除成功"));
        } catch (BusinessException e) {
            logger.error("用户 {} 删除活动图片失败: 图片ID={}, 错误: {}", currentUser, imageId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("用户 {} 删除活动图片时发生异常: 图片ID={}, 错误: {}", currentUser, imageId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除图片失败");
        }
    }
}