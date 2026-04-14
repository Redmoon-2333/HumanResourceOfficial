package com.redmoon2333.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.redmoon2333.entity.ActivityImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ActivityImageMapper extends BaseMapper<ActivityImage> {
    
    /**
     * 根据 ID 查找图片
     */
    ActivityImage findById(@Param("imageId") Integer imageId);
    
    /**
     * 根据活动 ID 查找图片列表
     */
    List<ActivityImage> findByActivityId(@Param("activityId") Integer activityId);
    
    /**
     * 根据活动 ID 删除所有图片
     * @param activityId 活动 ID
     * @return 删除的记录数
     */
    int deleteByActivityId(@Param("activityId") Integer activityId);
    
}