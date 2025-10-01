package com.redmoon2333.service;

import com.redmoon2333.entity.Activity;
import com.redmoon2333.entity.ActivityImage;
import com.redmoon2333.exception.BusinessException;
import com.redmoon2333.exception.ErrorCode;
import com.redmoon2333.mapper.ActivityImageMapper;
import com.redmoon2333.mapper.ActivityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ActivityService {
    
    private static final Logger logger = LoggerFactory.getLogger(ActivityService.class);
    
    @Autowired
    private ActivityMapper activityMapper;
    
    @Autowired
    private ActivityImageMapper activityImageMapper;
    
    public Activity createActivity(Activity activity) {
        logger.info("开始创建活动: {}", activity.getActivityName());
        
        try {
            activity.setCreateTime(LocalDateTime.now());
            activity.setUpdateTime(LocalDateTime.now());
            
            int result = activityMapper.insert(activity);
            if (result <= 0) {
                logger.error("活动创建失败，数据库插入返回结果: {}", result);
                throw new BusinessException(ErrorCode.ACTIVITY_CREATION_FAILED);
            }
            
            logger.info("活动创建成功: ID={}, 名称={}", activity.getActivityId(), activity.getActivityName());
            return activity;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("创建活动时发生异常: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.ACTIVITY_CREATION_FAILED);
        }
    }
    
    public Activity getActivityById(Integer activityId) {
        logger.debug("查询活动详情: ID={}", activityId);
        
        try {
            Activity activity = activityMapper.selectById(activityId);
            if (activity == null) {
                logger.warn("活动不存在: ID={}", activityId);
                throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND);
            }
            
            logger.debug("成功查询到活动: ID={}, 名称={}", activityId, activity.getActivityName());
            return activity;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("查询活动时发生异常: ID={}, 错误: {}", activityId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }
    
    public List<Activity> getAllActivities() {
        logger.debug("查询所有活动列表");
        
        try {
            List<Activity> activities = activityMapper.selectAll();
            logger.info("成功查询所有活动，共 {} 个活动", activities.size());
            return activities;
        } catch (Exception e) {
            logger.error("查询所有活动时发生异常: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }
    
    public Activity updateActivity(Activity activity) {
        logger.info("开始更新活动: ID={}, 名称={}", activity.getActivityId(), activity.getActivityName());
        
        try {
            // 检查活动是否存在
            Activity existingActivity = activityMapper.selectById(activity.getActivityId());
            if (existingActivity == null) {
                logger.warn("尝试更新不存在的活动: ID={}", activity.getActivityId());
                throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND);
            }
            
            activity.setUpdateTime(LocalDateTime.now());
            
            int result = activityMapper.update(activity);
            if (result <= 0) {
                logger.error("活动更新失败，数据库更新返回结果: {}", result);
                throw new BusinessException(ErrorCode.ACTIVITY_UPDATE_FAILED);
            }
            
            logger.info("活动更新成功: ID={}, 名称={}", activity.getActivityId(), activity.getActivityName());
            return activity;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("更新活动时发生异常: ID={}, 错误: {}", activity.getActivityId(), e.getMessage(), e);
            throw new BusinessException(ErrorCode.ACTIVITY_UPDATE_FAILED);
        }
    }
    
    public void deleteActivity(Integer activityId) {
        logger.info("开始删除活动: ID={}", activityId);
        
        try {
            // 检查活动是否存在
            Activity existingActivity = activityMapper.selectById(activityId);
            if (existingActivity == null) {
                logger.warn("尝试删除不存在的活动: ID={}", activityId);
                throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND);
            }
            
            // 删除活动相关的图片
            activityImageMapper.deleteByActivityId(activityId);
            
            int result = activityMapper.deleteById(activityId);
            if (result <= 0) {
                logger.error("活动删除失败，数据库删除返回结果: {}", result);
                throw new BusinessException(ErrorCode.ACTIVITY_DELETE_FAILED);
            }
            
            logger.info("活动删除成功: ID={}, 名称={}", activityId, existingActivity.getActivityName());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("删除活动时发生异常: ID={}, 错误: {}", activityId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.ACTIVITY_DELETE_FAILED);
        }
    }
    
    public String saveImage(MultipartFile file) throws IOException {
        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
        String uniqueFilename = UUID.randomUUID().toString() + extension;
        
        // 获取项目根路径并创建 uploads 目录
        String projectRoot = System.getProperty("user.dir");
        File uploadDir = new File(projectRoot, "uploads");
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        
        File destFile = new File(uploadDir, uniqueFilename);
        file.transferTo(destFile);
        
        // 返回相对路径
        return "/uploads/" + uniqueFilename;
    }
    
    public ActivityImage addImageToActivity(Integer activityId, ActivityImage activityImage) {
        logger.info("为活动添加图片: 活动ID={}, 图片描述={}", activityId, activityImage.getDescription());
        
        try {
            // 检查活动是否存在
            Activity activity = activityMapper.selectById(activityId);
            if (activity == null) {
                logger.warn("尝试为不存在的活动添加图片: 活动ID={}", activityId);
                throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND);
            }
            
            activityImage.setActivityId(activityId);
            activityImage.setUploadTime(LocalDateTime.now());
            
            int result = activityImageMapper.insert(activityImage);
            if (result <= 0) {
                logger.error("添加活动图片失败，数据库插入返回结果: {}", result);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "添加活动图片失败");
            }
            
            logger.info("成功为活动添加图片: 活动ID={}, 图片ID={}", activityId, activityImage.getImageId());
            return activityImage;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("为活动添加图片时发生异常: 活动ID={}, 错误: {}", activityId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "添加活动图片失败", e);
        }
    }
    
    public List<ActivityImage> getImagesByActivityId(Integer activityId) {
        logger.debug("获取活动的图片列表: 活动ID={}", activityId);
        
        try {
            // 检查活动是否存在
            Activity activity = activityMapper.selectById(activityId);
            if (activity == null) {
                logger.warn("尝试获取不存在的活动的图片: 活动ID={}", activityId);
                throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND);
            }
            
            List<ActivityImage> images = activityImageMapper.findByActivityId(activityId);
            logger.debug("成功获取活动的图片列表: 活动ID={}, 图片数量={}", activityId, images.size());
            return images;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("获取活动图片列表时发生异常: 活动ID={}, 错误: {}", activityId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取活动图片列表失败", e);
        }
    }
    
    public ActivityImage updateActivityImage(Integer imageId, ActivityImage activityImage) {
        logger.info("更新活动图片: 图片ID={}", imageId);
        
        try {
            // 检查图片是否存在
            ActivityImage existingImage = activityImageMapper.findById(imageId);
            if (existingImage == null) {
                logger.warn("尝试更新不存在的活动图片: 图片ID={}", imageId);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "活动图片不存在");
            }
            
            activityImage.setImageId(imageId);
            int result = activityImageMapper.update(activityImage);
            if (result <= 0) {
                logger.error("更新活动图片失败，数据库更新返回结果: {}", result);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新活动图片失败");
            }
            
            logger.info("成功更新活动图片: 图片ID={}", imageId);
            return activityImage;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("更新活动图片时发生异常: 图片ID={}, 错误: {}", imageId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新活动图片失败", e);
        }
    }
    
    public void deleteActivityImage(Integer imageId) {
        logger.info("删除活动图片: 图片ID={}", imageId);
        
        try {
            // 检查图片是否存在
            ActivityImage existingImage = activityImageMapper.findById(imageId);
            if (existingImage == null) {
                logger.warn("尝试删除不存在的活动图片: 图片ID={}", imageId);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "活动图片不存在");
            }
            
            int result = activityImageMapper.deleteById(imageId);
            if (result <= 0) {
                logger.error("删除活动图片失败，数据库删除返回结果: {}", result);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除活动图片失败");
            }
            
            logger.info("成功删除活动图片: 图片ID={}", imageId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("删除活动图片时发生异常: 图片ID={}, 错误: {}", imageId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除活动图片失败", e);
        }
    }
}