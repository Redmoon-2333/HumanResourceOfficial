package com.redmoon2333.service;

import com.redmoon2333.entity.Activity;
import com.redmoon2333.exception.BusinessException;
import com.redmoon2333.exception.ErrorCode;
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

@Service
public class ActivityService {
    
    private static final Logger logger = LoggerFactory.getLogger(ActivityService.class);
    
    @Autowired
    private ActivityMapper activityMapper;
    
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
        String extension = originalFilename != null ? 
            originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
        String uniqueFilename = UUID.randomUUID().toString() + extension;
        
        // 保存到本地（预留阿里云OSS接口位置）
        String uploadDir = "uploads/activities/";
        File uploadDirFile = new File(uploadDir);
        if (!uploadDirFile.exists()) {
            uploadDirFile.mkdirs();
        }
        
        String filePath = uploadDir + uniqueFilename;
        File dest = new File(filePath);
        file.transferTo(dest);
        
        // TODO: 预留阿里云OSS接口位置
        // 可以在这里添加上传到阿里云OSS的代码
        
        return filePath;
    }
    
    public void deleteImage(String imageUrl) {
        // 删除本地文件（预留阿里云OSS接口位置）
        File imageFile = new File(imageUrl);
        if (imageFile.exists()) {
            imageFile.delete();
        }
        
        // TODO: 预留阿里云OSS接口位置
        // 可以在这里添加从阿里云OSS删除文件的代码
    }
}