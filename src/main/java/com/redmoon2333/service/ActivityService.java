package com.redmoon2333.service;

import com.redmoon2333.entity.Activity;
import com.redmoon2333.entity.ActivityImage;
import com.redmoon2333.exception.BusinessException;
import com.redmoon2333.exception.ErrorCode;
import com.redmoon2333.mapper.ActivityImageMapper;
import com.redmoon2333.mapper.ActivityMapper;
import com.redmoon2333.util.OssUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
    
    @Autowired(required = false) // 设置为非必需，避免启动时因缺少OSS配置而失败
    private OssUtil ossUtil;
    
    public Activity createActivity(Activity activity) {
        logger.info("开始创建活动: {}", activity.getActivityName());
        
        // 保存活动信息到数据库
        activityMapper.insert(activity);
        logger.info("活动创建成功，活动ID: {}", activity.getActivityId());
        
        return activity;
    }
    
    public List<Activity> getAllActivities() {
        logger.info("获取所有活动列表");
        try {
            List<Activity> activities = activityMapper.selectAll();
            logger.info("成功获取活动列表，共{}条记录", activities.size());
            return activities;
        } catch (Exception e) {
            logger.error("获取活动列表时发生异常: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.ACTIVITY_LIST_FAILED);
        }
    }
    
    public Activity getActivityById(Integer activityId) {
        logger.info("根据ID获取活动详情: ID={}", activityId);
        try {
            Activity activity = activityMapper.selectById(activityId);
            if (activity == null) {
                logger.warn("未找到指定活动: ID={}", activityId);
                throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND);
            }
            logger.info("成功获取活动详情: ID={}", activityId);
            return activity;
        } catch (BusinessException e) {
            throw e; // 重新抛出业务异常
        } catch (Exception e) {
            logger.error("获取活动详情时发生异常: ID={}, 错误: {}", activityId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.ACTIVITY_DETAIL_FAILED);
        }
    }
    
    public Activity updateActivity(Integer activityId, Activity activityDetails) {
        logger.info("开始更新活动: ID={}", activityId);
        
        try {
            // 先检查活动是否存在
            Activity existingActivity = activityMapper.selectById(activityId);
            if (existingActivity == null) {
                logger.warn("尝试更新不存在的活动: ID={}", activityId);
                throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND);
            }
            
            // 更新活动信息
            existingActivity.setActivityName(activityDetails.getActivityName());
            existingActivity.setBackground(activityDetails.getBackground());
            existingActivity.setSignificance(activityDetails.getSignificance());
            existingActivity.setPurpose(activityDetails.getPurpose());
            existingActivity.setProcess(activityDetails.getProcess());
            existingActivity.setUpdateTime(LocalDateTime.now());
            
            int result = activityMapper.update(existingActivity);
            
            if (result > 0) {
                logger.info("活动更新成功: ID={}", activityId);
                return existingActivity;
            } else {
                logger.error("活动更新失败: ID={}", activityId);
                throw new BusinessException(ErrorCode.ACTIVITY_UPDATE_FAILED);
            }
        } catch (BusinessException e) {
            throw e; // 重新抛出业务异常
        } catch (Exception e) {
            logger.error("更新活动时发生异常: ID={}, 错误: {}", activityId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.ACTIVITY_UPDATE_FAILED);
        }
    }
    
    public void deleteActivity(Integer activityId) {
        logger.info("开始删除活动: ID={}", activityId);
        
        try {
            // 先检查活动是否存在
            Activity existingActivity = activityMapper.selectById(activityId);
            if (existingActivity == null) {
                logger.warn("尝试删除不存在的活动: ID={}", activityId);
                throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND);
            }
            
            // 删除关联的图片记录
            List<ActivityImage> images = activityImageMapper.findByActivityId(activityId);
            logger.info("找到{}张关联图片需要删除", images.size());
            
            for (ActivityImage image : images) {
                // 从OSS删除图片文件
                String imageUrl = image.getImageUrl();
                logger.debug("准备删除图片文件: {}", imageUrl);
                
                // 从URL中提取OSS文件路径
                String filePath = extractOssFilePath(imageUrl);
                if (filePath != null) {
                    if (ossUtil != null) {
                        try {
                            ossUtil.deleteFile(filePath);
                        } catch (Exception e) {
                            logger.warn("删除OSS文件失败: {}", filePath, e);
                        }
                    } else {
                        logger.warn("OSS未配置，跳过文件删除: {}", filePath);
                    }
                }
                
                // 删除数据库记录
                activityImageMapper.deleteById(image.getImageId());
            }
            
            // 删除活动
            int result = activityMapper.deleteById(activityId);
            
            if (result > 0) {
                logger.info("活动删除成功: ID={}, 关联图片{}张", activityId, images.size());
            } else {
                logger.error("活动删除失败: ID={}", activityId);
                throw new BusinessException(ErrorCode.ACTIVITY_DELETE_FAILED);
            }
        } catch (BusinessException e) {
            throw e; // 重新抛出业务异常
        } catch (Exception e) {
            logger.error("删除活动时发生异常: ID={}, 错误: {}", activityId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.ACTIVITY_DELETE_FAILED);
        }
    }
    
    // 从OSS URL中提取文件路径
    private String extractOssFilePath(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }
        
        // 处理自定义域名的情况
        String domain = System.getProperty("aliyun.oss.domain");
        if (domain != null && !domain.isEmpty() && imageUrl.startsWith(domain)) {
            return imageUrl.substring(domain.length() + 1); // +1 是为了去掉开头的 "/"
        }
        
        // 处理默认OSS域名的情况
        // 格式: https://bucket-name.endpoint/path
        int thirdSlashIndex = imageUrl.indexOf("/", 8); // 跳过 https://
        if (thirdSlashIndex != -1) {
            return imageUrl.substring(thirdSlashIndex + 1);
        }
        
        return null;
    }
    
    public String saveImage(MultipartFile file) throws IOException {
        logger.info("开始保存活动图片: 文件名={}, 大小={} bytes", file.getOriginalFilename(), file.getSize());
        
        // 检查OSS工具是否可用
        if (ossUtil == null) {
            logger.error("文件上传功能不可用，请联系系统管理员检查OSS配置");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传功能不可用，请联系系统管理员检查OSS配置");
        }
        
        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
        String uniqueFilename = UUID.randomUUID().toString() + extension;
        logger.debug("生成唯一文件名: {}", uniqueFilename);

        // 使用OSS工具类上传文件
        String ossFilePath = ossUtil.uploadFile(file);

        // 返回OSS文件路径
        logger.info("图片保存成功: {}", ossFilePath);
        return ossFilePath;
    }

    public ActivityImage addImageToActivity(Integer activityId, ActivityImage activityImage) {
        logger.info("为活动添加图片: 活动ID={}, 图片描述={}", activityId, activityImage.getDescription());

        try {
            // 检查活动是否存在
            Activity activity = activityMapper.selectById(activityId);
            if (activity == null) {
                throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND);
            }

            // 保存图片到OSS
            // 注意: activityImage 对象不应该包含 MultipartFile 字段
            // 这里假设文件已经在控制器层处理并传入 imageUrl
            // String imageUrl = saveImage(activityImage.getImageFile());
            activityImage.setUploadTime(LocalDateTime.now());

            // 设置默认排序顺序
            if (activityImage.getSortOrder() == null) {
                activityImage.setSortOrder(0);
            }

            // 插入数据库
            int result = activityImageMapper.insert(activityImage);
            if (result != 1) {
                throw new BusinessException(ErrorCode.ACTIVITY_IMAGE_SAVE_FAILED);
            }

            logger.info("图片添加成功: 图片ID={}, 图片URL={}", activityImage.getImageId(), activityImage.getImageUrl());
            return activityImage;
        } catch (Exception e) {
            logger.error("为活动添加图片时发生异常: 活动ID={}, 错误: {}", activityId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.ACTIVITY_IMAGE_SAVE_FAILED);
        }
    }

    public void deleteActivityImagesByActivityId(Integer activityId) {
        logger.info("删除活动的所有图片: 活动ID={}", activityId);

        try {
            List<ActivityImage> images = activityImageMapper.findByActivityId(activityId);
            logger.info("找到{}张图片需要删除", images.size());
            
            for (ActivityImage image : images) {
                // 删除OSS上的文件
                if (ossUtil != null) {
                    try {
                        ossUtil.deleteFile(image.getImageUrl());
                    } catch (Exception e) {
                        logger.warn("删除OSS文件失败: {}", image.getImageUrl(), e);
                    }
                } else {
                    logger.warn("OSS未配置，跳过文件删除: {}", image.getImageUrl());
                }
            }

            // 删除数据库记录
            activityImageMapper.deleteByActivityId(activityId);
            logger.info("活动图片删除成功: 活动ID={}", activityId);
        } catch (Exception e) {
            logger.error("删除活动图片时发生异常: 活动ID={}, 错误: {}", activityId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.ACTIVITY_IMAGE_DELETE_FAILED);
        }
    }
    
    public List<ActivityImage> getImagesByActivityId(Integer activityId) {
        logger.info("获取活动的图片列表: 活动ID={}", activityId);
        
        try {
            // 检查活动是否存在
            Activity activity = activityMapper.selectById(activityId);
            if (activity == null) {
                logger.warn("尝试获取不存在的活动的图片列表: 活动ID={}", activityId);
                throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND);
            }
            
            List<ActivityImage> images = activityImageMapper.findByActivityId(activityId);
            logger.info("成功获取活动图片列表: 活动ID={}, 图片数量={}", activityId, images.size());
            return images;
        } catch (BusinessException e) {
            throw e; // 重新抛出业务异常
        } catch (Exception e) {
            logger.error("获取活动图片列表时发生异常: 活动ID={}, 错误: {}", activityId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.ACTIVITY_IMAGE_LIST_FAILED);
        }
    }
    
    public ActivityImage updateActivityImage(Integer imageId, ActivityImage imageDetails) {
        logger.info("更新活动图片: 图片ID={}", imageId);
        
        try {
            // 检查图片是否存在
            ActivityImage existingImage = activityImageMapper.findById(imageId);
            if (existingImage == null) {
                logger.warn("尝试更新不存在的活动图片: 图片ID={}", imageId);
                throw new BusinessException(ErrorCode.ACTIVITY_IMAGE_NOT_FOUND);
            }
            
            // 更新图片信息
            existingImage.setDescription(imageDetails.getDescription());
            existingImage.setSortOrder(imageDetails.getSortOrder());
            
            int result = activityImageMapper.update(existingImage);
            
            if (result > 0) {
                logger.info("活动图片更新成功: 图片ID={}", imageId);
                return existingImage;
            } else {
                logger.error("活动图片更新失败: 图片ID={}", imageId);
                throw new BusinessException(ErrorCode.ACTIVITY_IMAGE_UPDATE_FAILED);
            }
        } catch (BusinessException e) {
            throw e; // 重新抛出业务异常
        } catch (Exception e) {
            logger.error("更新活动图片时发生异常: 图片ID={}, 错误: {}", imageId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.ACTIVITY_IMAGE_UPDATE_FAILED);
        }
    }
    
    public void deleteActivityImage(Integer imageId) {
        logger.info("删除活动图片: 图片ID={}", imageId);
        
        try {
            // 检查图片是否存在
            ActivityImage existingImage = activityImageMapper.findById(imageId);
            if (existingImage == null) {
                logger.warn("尝试删除不存在的活动图片: 图片ID={}", imageId);
                throw new BusinessException(ErrorCode.ACTIVITY_IMAGE_NOT_FOUND);
            }
            
            // 从OSS删除图片文件
            String imageUrl = existingImage.getImageUrl();
            String filePath = extractOssFilePath(imageUrl);
            
            if (filePath != null) {
                if (ossUtil != null) {
                    try {
                        ossUtil.deleteFile(filePath);
                    } catch (Exception e) {
                        logger.warn("删除OSS文件失败: {}", filePath, e);
                    }
                } else {
                    logger.warn("OSS未配置，跳过文件删除: {}", filePath);
                }
            }
            
            // 删除数据库记录
            int result = activityImageMapper.deleteById(imageId);
            
            if (result > 0) {
                logger.info("活动图片删除成功: 图片ID={}, 文件路径={}", imageId, filePath);
            } else {
                logger.error("活动图片删除失败: 图片ID={}", imageId);
                throw new BusinessException(ErrorCode.ACTIVITY_IMAGE_DELETE_FAILED);
            }
        } catch (BusinessException e) {
            throw e; // 重新抛出业务异常
        } catch (Exception e) {
            logger.error("删除活动图片时发生异常: 图片ID={}, 错误: {}", imageId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.ACTIVITY_IMAGE_DELETE_FAILED);
        }
    }
    
    public String getImageUrlById(Integer imageId) {
        if (imageId == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "图片ID不能为空");
        }
        
        ActivityImage image = activityImageMapper.findById(imageId);
        if (image == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "未找到指定的图片");
        }
        
        return image.getImageUrl();
    }

}




