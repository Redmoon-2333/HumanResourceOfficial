package com.redmoon2333.mapper;

import com.redmoon2333.entity.MaterialCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MaterialCategoryMapper {
    
    /**
     * 根据ID查找分类
     */
    MaterialCategory findById(@Param("categoryId") Integer categoryId);
    
    /**
     * 查找所有分类（按排序顺序）
     */
    List<MaterialCategory> findAll();
    
    /**
     * 根据分类名称查找
     */
    MaterialCategory findByName(@Param("categoryName") String categoryName);
    
    /**
     * 插入新分类
     */
    int insert(MaterialCategory materialCategory);
    
    /**
     * 更新分类信息
     */
    int update(MaterialCategory materialCategory);
    
    /**
     * 根据ID删除分类
     */
    int deleteById(@Param("categoryId") Integer categoryId);
}