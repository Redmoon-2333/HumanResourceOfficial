package com.redmoon2333.mapper;

import com.redmoon2333.entity.DailyImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 日常活动图片数据访问层
 * 提供对daily_image表的CRUD操作
 *
 * @author 人力资源中心技术组
 * @since 2026-02-13
 */
@Mapper
public interface DailyImageMapper {

    /**
     * 根据ID查找图片
     *
     * @param imageId 图片ID
     * @return 图片实体
     */
    DailyImage findById(@Param("imageId") Integer imageId);

    /**
     * 查询所有启用的图片列表（按排序顺序）
     * 用于前端轮播图展示
     *
     * @return 图片列表
     */
    List<DailyImage> findAllActive();

    /**
     * 查询所有图片列表（后台管理用）
     *
     * @return 图片列表
     */
    List<DailyImage> findAll();

    /**
     * 插入新图片
     *
     * @param dailyImage 图片实体
     * @return 影响行数
     */
    int insert(DailyImage dailyImage);

    /**
     * 更新图片信息
     *
     * @param dailyImage 图片实体
     * @return 影响行数
     */
    int update(DailyImage dailyImage);

    /**
     * 根据ID删除图片
     *
     * @param imageId 图片ID
     * @return 影响行数
     */
    int deleteById(@Param("imageId") Integer imageId);

    /**
     * 批量删除图片
     *
     * @param imageIds 图片ID列表
     * @return 影响行数
     */
    int batchDelete(@Param("imageIds") List<Integer> imageIds);

    /**
     * 获取最大排序号
     *
     * @return 最大排序号
     */
    Integer getMaxSortOrder();

    /**
     * 更新图片状态
     *
     * @param imageId 图片ID
     * @param isActive 状态
     * @return 影响行数
     */
    int updateStatus(@Param("imageId") Integer imageId, @Param("isActive") Boolean isActive);
}
