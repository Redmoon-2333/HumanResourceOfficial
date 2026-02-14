package com.redmoon2333.service;

import com.redmoon2333.entity.DailyImage;
import com.redmoon2333.mapper.DailyImageMapper;
import com.redmoon2333.util.LocalFileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 日常活动图片服务层
 * 处理"我们的日常"板块轮播图的业务逻辑
 *
 * @author 人力资源中心技术组
 * @since 2026-02-13
 */
@Service
public class DailyImageService {

    private static final Logger logger = LoggerFactory.getLogger(DailyImageService.class);

    @Autowired
    private DailyImageMapper dailyImageMapper;

    @Autowired
    private LocalFileUtil localFileUtil;

    /**
     * 获取所有启用的图片列表（用于前端展示）
     * 按sort_order升序排列
     *
     * @return 启用的图片列表
     */
    public List<DailyImage> getActiveImages() {
        logger.debug("获取所有启用的日常活动图片");
        return dailyImageMapper.findAllActive();
    }

    /**
     * 获取所有图片列表（用于后台管理）
     *
     * @return 所有图片列表
     */
    public List<DailyImage> getAllImages() {
        logger.debug("获取所有日常活动图片");
        return dailyImageMapper.findAll();
    }

    /**
     * 根据ID获取图片详情
     *
     * @param imageId 图片ID
     * @return 图片实体，不存在返回null
     */
    public DailyImage getImageById(Integer imageId) {
        logger.debug("获取图片详情，ID: {}", imageId);
        return dailyImageMapper.findById(imageId);
    }

    /**
     * 添加新图片
     * 自动设置排序号为当前最大值+1
     *
     * @param dailyImage 图片实体
     * @return 添加后的图片（包含生成的ID）
     */
    @Transactional(rollbackFor = Exception.class)
    public DailyImage addImage(DailyImage dailyImage) {
        // 参数校验
        if (dailyImage.getImageUrl() == null || dailyImage.getImageUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("图片URL不能为空");
        }

        // 自动设置排序号
        if (dailyImage.getSortOrder() == null) {
            Integer maxOrder = dailyImageMapper.getMaxSortOrder();
            dailyImage.setSortOrder(maxOrder != null ? maxOrder + 1 : 0);
        }

        // 设置默认状态
        if (dailyImage.getIsActive() == null) {
            dailyImage.setIsActive(true);
        }

        logger.info("添加新图片: {}", dailyImage.getTitle());
        dailyImageMapper.insert(dailyImage);
        return dailyImage;
    }

    /**
     * 更新图片信息
     *
     * @param dailyImage 图片实体
     * @return 更新后的图片
     */
    @Transactional(rollbackFor = Exception.class)
    public DailyImage updateImage(DailyImage dailyImage) {
        if (dailyImage.getImageId() == null) {
            throw new IllegalArgumentException("图片ID不能为空");
        }

        // 检查图片是否存在
        DailyImage existing = dailyImageMapper.findById(dailyImage.getImageId());
        if (existing == null) {
            throw new IllegalArgumentException("图片不存在，ID: " + dailyImage.getImageId());
        }

        logger.info("更新图片，ID: {}", dailyImage.getImageId());
        dailyImageMapper.update(dailyImage);
        return dailyImageMapper.findById(dailyImage.getImageId());
    }

    /**
     * 删除图片
     *
     * @param imageId 图片ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteImage(Integer imageId) {
        logger.info("删除图片，ID: {}", imageId);
        dailyImageMapper.deleteById(imageId);
    }

    /**
     * 批量删除图片
     *
     * @param imageIds 图片ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteImages(List<Integer> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            return;
        }
        logger.info("批量删除图片，数量: {}", imageIds.size());
        dailyImageMapper.batchDelete(imageIds);
    }

    /**
     * 更新图片状态（启用/禁用）
     *
     * @param imageId 图片ID
     * @param isActive 状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateImageStatus(Integer imageId, Boolean isActive) {
        logger.info("更新图片状态，ID: {}, 状态: {}", imageId, isActive);
        dailyImageMapper.updateStatus(imageId, isActive);
    }

    /**
     * 调整图片排序
     * 交换两个图片的sort_order
     *
     * @param imageId1 图片1 ID
     * @param imageId2 图片2 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void swapImageOrder(Integer imageId1, Integer imageId2) {
        DailyImage image1 = dailyImageMapper.findById(imageId1);
        DailyImage image2 = dailyImageMapper.findById(imageId2);

        if (image1 == null || image2 == null) {
            throw new IllegalArgumentException("图片不存在");
        }

        Integer tempOrder = image1.getSortOrder();
        image1.setSortOrder(image2.getSortOrder());
        image2.setSortOrder(tempOrder);

        dailyImageMapper.update(image1);
        dailyImageMapper.update(image2);

        logger.info("交换图片排序，ID: {} <-> {}", imageId1, imageId2);
    }

    /**
     * 上传图片并保存到数据库
     * 用于前端直接上传图片文件
     *
     * @param file 图片文件
     * @param title 图片标题
     * @param description 图片描述
     * @return 保存后的图片实体
     * @throws IOException 文件上传异常
     */
    @Transactional(rollbackFor = Exception.class)
    public DailyImage uploadImage(MultipartFile file, String title, String description) throws IOException {
        logger.info("开始上传日常活动图片: 文件名={}, 标题={}", file.getOriginalFilename(), title);

        // 1. 上传文件到本地存储（使用 daily 分类）
        String imageUrl = localFileUtil.uploadDailyImage(file);

        // 2. 创建图片实体
        DailyImage dailyImage = new DailyImage();
        dailyImage.setImageUrl(imageUrl);
        dailyImage.setTitle(title);
        dailyImage.setDescription(description);
        dailyImage.setIsActive(true);

        // 3. 自动设置排序号
        Integer maxOrder = dailyImageMapper.getMaxSortOrder();
        dailyImage.setSortOrder(maxOrder != null ? maxOrder + 1 : 0);

        // 4. 保存到数据库
        dailyImageMapper.insert(dailyImage);

        logger.info("日常活动图片上传成功: ID={}, URL={}", dailyImage.getImageId(), imageUrl);
        return dailyImage;
    }

    /**
     * 仅上传图片文件，不创建数据库记录
     * 用于编辑图片时替换文件
     *
     * @param file 图片文件
     * @return 上传后的图片URL
     * @throws IOException 文件上传异常
     */
    public String uploadImageFile(MultipartFile file) throws IOException {
        logger.info("开始上传日常活动图片文件: 文件名={}", file.getOriginalFilename());

        // 上传文件到本地存储（使用 daily 分类）
        String imageUrl = localFileUtil.uploadDailyImage(file);

        logger.info("日常活动图片文件上传成功: URL={}", imageUrl);
        return imageUrl;
    }

    /**
     * 删除图片并删除本地文件
     *
     * @param imageId 图片ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteImageWithFile(Integer imageId) {
        logger.info("删除图片及本地文件，ID: {}", imageId);

        // 1. 获取图片信息
        DailyImage image = dailyImageMapper.findById(imageId);
        if (image == null) {
            throw new IllegalArgumentException("图片不存在，ID: " + imageId);
        }

        // 2. 删除本地文件
        String imageUrl = image.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                localFileUtil.deleteFile(imageUrl);
                logger.info("本地文件删除成功: {}", imageUrl);
            } catch (Exception e) {
                logger.warn("删除本地文件失败: {}", imageUrl, e);
                // 继续删除数据库记录，不阻断流程
            }
        }

        // 3. 删除数据库记录
        dailyImageMapper.deleteById(imageId);
        logger.info("图片记录删除成功，ID: {}", imageId);
    }
}
