package com.redmoon2333.mapper;

import com.redmoon2333.entity.Material;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MaterialMapper {
    
    /**
     * 根据ID查找资料
     */
    Material findById(@Param("materialId") Integer materialId);
    
    /**
     * 根据分类ID查找资料列表
     */
    List<Material> findByCategoryId(@Param("categoryId") Integer categoryId);
    
    /**
     * 根据子分类ID查找资料列表
     */
    List<Material> findBySubcategoryId(@Param("subcategoryId") Integer subcategoryId);
    
    /**
     * 根据上传者ID查找资料列表
     */
    List<Material> findByUploaderId(@Param("uploaderId") Integer uploaderId);
    
    /**
     * 查找所有资料
     */
    List<Material> findAll();
    
    /**
     * 根据资料名称模糊查询
     */
    List<Material> findByNameLike(@Param("materialName") String materialName);
    
    /**
     * 根据资料名称模糊查询（包含）
     */
    List<Material> findByNameContaining(@Param("materialName") String materialName);
    
    /**
     * 插入新资料
     */
    int insert(Material material);
    
    /**
     * 更新资料信息
     */
    int update(Material material);
    
    /**
     * 更新下载次数
     */
    int updateDownloadCount(@Param("materialId") Integer materialId);
    
    /**
     * 根据ID删除资料
     */
    int deleteById(@Param("materialId") Integer materialId);
    
    /**
     * 根据分类ID删除所有资料
     */
    int deleteByCategoryId(@Param("categoryId") Integer categoryId);
    
    /**
     * 根据子分类ID删除所有资料
     */
    int deleteBySubcategoryId(@Param("subcategoryId") Integer subcategoryId);
}