package com.redmoon2333.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.redmoon2333.entity.MaterialCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MaterialCategoryMapper extends BaseMapper<MaterialCategory> {
    
    /**
     * 查找所有分类（按排序顺序）
     */
    List<MaterialCategory> findAll();
    
    /**
     * 根据分类名称查找
     */
    MaterialCategory findByName(@Param("categoryName") String categoryName);
}