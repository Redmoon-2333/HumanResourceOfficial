package com.redmoon2333.mapper;

import com.redmoon2333.entity.MaterialSubcategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MaterialSubcategoryMapper {
    
    /**
     * 根据ID查找子分类
     */
    MaterialSubcategory findById(@Param("subcategoryId") Integer subcategoryId);
    
    /**
     * 根据分类ID查找所有子分类
     */
    List<MaterialSubcategory> findByCategoryId(@Param("categoryId") Integer categoryId);
    
    /**
     * 查找所有子分类
     */
    List<MaterialSubcategory> findAll();
    
    /**
     * 根据子分类名称和分类ID查找
     */
    MaterialSubcategory findByNameAndCategoryId(@Param("subcategoryName") String subcategoryName, 
                                               @Param("categoryId") Integer categoryId);
    
    /**
     * 插入新子分类
     */
    int insert(MaterialSubcategory materialSubcategory);
    
    /**
     * 更新子分类信息
     */
    int update(MaterialSubcategory materialSubcategory);
    
    /**
     * 根据ID删除子分类
     */
    int deleteById(@Param("subcategoryId") Integer subcategoryId);
    
    /**
     * 根据分类ID删除所有子分类
     */
    int deleteByCategoryId(@Param("categoryId") Integer categoryId);
}