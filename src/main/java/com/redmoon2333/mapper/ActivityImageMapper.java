package com.redmoon2333.mapper;

import com.redmoon2333.entity.ActivityImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ActivityImageMapper {
    
    /**
     * 根据ID查找图片
     */
    ActivityImage findById(@Param("imageId") Integer imageId);
    
    /**
     * 根据活动ID查找图片列表
     */
    List<ActivityImage> findByActivityId(@Param("activityId") Integer activityId);
    
    /**
     * 插入新图片
     */
    int insert(ActivityImage activityImage);
    
    /**
     * 更新图片信息
     */
    int update(ActivityImage activityImage);
    
    /**
     * 根据ID删除图片
     */
    int deleteById(@Param("imageId") Integer imageId);
    
    /**
     * 根据活动ID删除所有图片
     */
    int deleteByActivityId(@Param("activityId") Integer activityId);
}