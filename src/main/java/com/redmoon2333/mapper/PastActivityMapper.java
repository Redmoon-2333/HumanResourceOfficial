package com.redmoon2333.mapper;

import com.redmoon2333.entity.PastActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PastActivityMapper {
    
    /**
     * 根据ID查找往届活动
     */
    PastActivity findById(@Param("pastActivityId") Integer pastActivityId);
    
    /**
     * 查找所有往届活动
     */
    List<PastActivity> findAll();
    
    /**
     * 根据年份查找往届活动
     */
    List<PastActivity> findByYear(@Param("year") Integer year);
    
    /**
     * 插入新往届活动
     */
    int insert(PastActivity pastActivity);
    
    /**
     * 更新往届活动信息
     */
    int update(PastActivity pastActivity);
    
    /**
     * 根据ID删除往届活动
     */
    int deleteById(@Param("pastActivityId") Integer pastActivityId);
}