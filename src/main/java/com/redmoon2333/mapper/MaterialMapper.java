package com.redmoon2333.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.redmoon2333.entity.Material;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MaterialMapper extends BaseMapper<Material> {
    
    /**
     * 根据分类 ID 查找资料列表
     */
    List<Material> findByCategoryId(@Param("categoryId") Integer categoryId);
    
    /**
     * 根据子分类 ID 查找资料列表
     */
    List<Material> findBySubcategoryId(@Param("subcategoryId") Integer subcategoryId);
    
    /**
     * 根据上传者 ID 查找资料列表
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
     * 更新下载次数
     */
    int updateDownloadCount(@Param("materialId") Integer materialId);
    
    /**
     * 根据分类 ID 删除所有资料
     */
    int deleteByCategoryId(@Param("categoryId") Integer categoryId);
    
    /**
     * 根据子分类 ID 删除所有资料
     */
    int deleteBySubcategoryId(@Param("subcategoryId") Integer subcategoryId);
}