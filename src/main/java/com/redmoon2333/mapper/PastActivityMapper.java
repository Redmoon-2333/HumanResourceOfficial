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
     * 查找所有往届活动（分页）
     */
    List<PastActivity> findAll();
    
    /**
     * 根据年份查找往届活动（分页）
     */
    List<PastActivity> findByYear(@Param("year") Integer year);
    
    /**
     * 根据活动名称模糊查询（分页）
     */
    List<PastActivity> findByTitleLike(@Param("title") String title);
    
    /**
     * 根据年份和活动名称查询（分页）
     */
    List<PastActivity> findByYearAndTitleLike(@Param("year") Integer year, @Param("title") String title);
    
    /**
     * 获取所有年份列表
     */
    List<Integer> findDistinctYears();
    
    /**
     * 根据年份统计活动数量
     */
    int countByYear(@Param("year") Integer year);
    
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