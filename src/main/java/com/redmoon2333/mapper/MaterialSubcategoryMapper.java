package com.redmoon2333.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.redmoon2333.entity.MaterialSubcategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MaterialSubcategoryMapper extends BaseMapper<MaterialSubcategory> {
    
    /**
     * 根据分类 ID 查找所有子分类
     */
    List<MaterialSubcategory> findByCategoryId(@Param("categoryId") Integer categoryId);
    
    /**
     * 查找所有子分类
     */
    List<MaterialSubcategory> findAll();
    
    /**
     * 根据子分类名称和分类 ID 查找
     */
    MaterialSubcategory findByNameAndCategoryId(@Param("subcategoryName") String subcategoryName, 
                                                @Param("categoryId") Integer categoryId);
    
    /**
     * 根据分类 ID 删除所有子分类
     */
    int deleteByCategoryId(@Param("categoryId") Integer categoryId);
}