package com.redmoon2333.service;

import com.redmoon2333.entity.Material;
import com.redmoon2333.entity.MaterialCategory;
import com.redmoon2333.entity.MaterialSubcategory;
import com.redmoon2333.exception.BusinessException;
import com.redmoon2333.exception.ErrorCode;
import com.redmoon2333.mapper.MaterialCategoryMapper;
import com.redmoon2333.mapper.MaterialMapper;
import com.redmoon2333.mapper.MaterialSubcategoryMapper;
import com.redmoon2333.util.OssUtil;
import com.redmoon2333.util.PermissionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class MaterialService {
    
    private static final Logger logger = LoggerFactory.getLogger(MaterialService.class);
    
    // Redis缓存key前缀
    private static final String REDIS_PRESIGNED_URL_PREFIX = "material:presigned:";
    
    @Autowired
    private MaterialMapper materialMapper;
    
    @Autowired
    private MaterialCategoryMapper categoryMapper;
    
    @Autowired
    private MaterialSubcategoryMapper subcategoryMapper;
    
    @Autowired(required = false) // 设置为非必需，避免启动时因缺少OSS配置而失败
    private OssUtil ossUtil;
    
    @Autowired
    private PermissionUtil permissionUtil;
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 文件上传基础路径（OSS存储）
    private static final String UPLOAD_BASE_PATH = "uploads/materials/";

    /**
     * 上传资料文件
     * @param file 上传的文件
     * @param categoryId 分类ID
     * @param subcategoryId 子分类ID
     * @param materialName 资料名称
     * @param description 资料描述
     * @return 保存的资料对象
     */
    @Transactional
    public Material uploadMaterial(MultipartFile file, Integer categoryId, Integer subcategoryId, 
                                 String materialName, String description) throws IOException {
        try {
            // 检查权限（部员及以上）
            permissionUtil.checkMemberPermission();
            Integer uploaderId = permissionUtil.getCurrentUserId();

            logger.info("开始上传文件: 文件名={}, 大小={}, 用户ID={}", file.getOriginalFilename(), file.getSize(), uploaderId);

            // 检查OSS工具是否可用
            if (ossUtil == null) {
                logger.error("文件上传功能不可用，请联系系统管理员检查OSS配置");
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传功能不可用，请联系系统管理员检查OSS配置");
            }

            // 获取文件扩展名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";

            // 生成唯一文件名
            String uniqueFilename = UUID.randomUUID().toString() + extension;

            // 上传到OSS并获取URL
            String fileUrl = ossUtil.uploadFile(file);

            // 创建资料对象
            Material material = new Material();
            material.setCategoryId(categoryId);
            material.setSubcategoryId(subcategoryId);
            material.setMaterialName(materialName);
            material.setDescription(description);
            material.setFileUrl(fileUrl);
            material.setFileSize((int) file.getSize());
            material.setFileType(extension);
            material.setUploadTime(LocalDateTime.now());
            material.setUploaderId(uploaderId);

            // 保存到数据库
            materialMapper.insert(material);

            logger.info("文件上传成功: 原始文件名={}, OSS文件URL={}", originalFilename, fileUrl);

            return material;
        } catch (Exception e) {
            logger.error("文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR);
        }
    }

    /**
     * 上传资料
     * @param categoryId 分类ID
     * @param subcategoryId 子分类ID
     * @param materialName 资料名称
     * @param description 描述
     * @param file 文件
     * @param uploaderId 上传者ID
     * @return 保存的资料对象
     */
    @Transactional
    public Material uploadMaterial(Integer categoryId, Integer subcategoryId, 
                                  String materialName, String description, 
                                  MultipartFile file, Integer uploaderId) throws IOException {
        logger.info("开始上传资料: {}", materialName);
        
        // 检查OSS工具是否可用
        if (ossUtil == null) {
            logger.error("文件上传功能不可用，请联系系统管理员检查OSS配置");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传功能不可用，请联系系统管理员检查OSS配置");
        }
        
        // 上传到OSS并获取URL
        String fileUrl = ossUtil.uploadFile(file);
        logger.debug("文件上传至OSS成功，URL: {}", fileUrl);
        
        // 创建资料记录
        Material material = new Material();
        material.setCategoryId(categoryId);
        material.setSubcategoryId(subcategoryId);
        material.setMaterialName(materialName);
        material.setDescription(description);
        material.setFileUrl(fileUrl);
        material.setUploaderId(uploaderId);
        
        materialMapper.insert(material);
        logger.info("资料上传成功，资料ID: {}", material.getMaterialId());
        
        return material;
    }

    /**
     * 删除资料文件
     * @param fileUrl 文件URL
     */
    public void deleteMaterialFile(String fileUrl) {
        logger.info("开始删除资料文件: {}", fileUrl);
        
        if (fileUrl != null && !fileUrl.isEmpty()) {
            // 从URL中提取OSS文件路径
            String filePath = extractOssFilePath(fileUrl);
            if (filePath != null) {
                if (ossUtil != null) {
                    try {
                        ossUtil.deleteFile(filePath);
                        logger.info("资料文件删除成功: {}", filePath);
                    } catch (Exception e) {
                        logger.warn("删除OSS文件失败: {}", filePath, e);
                    }
                } else {
                    logger.warn("OSS未配置，跳过文件删除: {}", filePath);
                }
            }
        }
    }

    /**
     * 从OSS URL中提取文件路径
     * @param fileUrl OSS文件URL
     * @return 文件路径
     */
    private String extractOssFilePath(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return null;
        }

        // 处理自定义域名的情况
        String domain = System.getProperty("aliyun.oss.domain");
        if (domain != null && !domain.isEmpty() && fileUrl.startsWith(domain)) {
            return fileUrl.substring(domain.length() + 1); // +1 是为了去掉开头的 "/"
        }

        // 处理默认OSS域名的情况
        // 格式: https://bucket-name.endpoint/path
        int thirdSlashIndex = fileUrl.indexOf("/", 8); // 跳过 https://
        if (thirdSlashIndex != -1) {
            return fileUrl.substring(thirdSlashIndex + 1);
        }

        return null;
    }

    /**
     * 根据ID获取资料详情
     * @param materialId 资料ID
     * @return 资料对象
     */
    public Material getMaterialById(Integer materialId) {
        logger.info("获取资料详情: materialId={}", materialId);
        
        // 检查权限（部员及以上）
        permissionUtil.checkMemberPermission();

        Material material = materialMapper.findById(materialId);
        if (material == null) {
            logger.warn("未找到指定资料: materialId={}", materialId);
            throw new RuntimeException("指定的资料不存在");
        }
        logger.info("成功获取资料详情: materialId={}", materialId);
        return material;
    }

    /**
     * 根据分类ID获取资料列表
     * @param categoryId 分类ID
     * @return 资料列表
     */
    public List<Material> getMaterialsByCategoryId(Integer categoryId) {
        logger.info("根据分类ID获取资料列表: categoryId={}", categoryId);
        
        // 检查权限（部员及以上）
        permissionUtil.checkMemberPermission();

        List<Material> materials = materialMapper.findByCategoryId(categoryId);
        logger.info("成功获取资料列表: categoryId={}, 数量={}", categoryId, materials.size());
        return materials;
    }

    /**
     * 增加资料下载次数
     * @param materialId 资料ID
     */
    private void incrementDownloadCount(Integer materialId) {
        logger.info("统计资料下载次数: materialId={}", materialId);

        Material material = materialMapper.findById(materialId);
        if (material == null) {
            logger.warn("未找到指定资料: materialId={}", materialId);
            return; // 不抛异常，防止影响URL生成
        }

        materialMapper.updateDownloadCount(materialId);
        logger.info("资料下载次数统计成功: materialId={}, 新下载次数={}", materialId, material.getDownloadCount() + 1);
    }

    /**
     * 创建分类
     * @param categoryName 分类名称
     * @param sortOrder 排序
     * @return 创建的分类对象
     */
    @Transactional
    public MaterialCategory createCategory(String categoryName, Integer sortOrder) {
        logger.info("创建分类: categoryName={}", categoryName);
        
        // 检查权限（部长/副部长）
        permissionUtil.checkMinisterPermission();

        // 检查分类是否已存在
        MaterialCategory existingCategory = categoryMapper.findByName(categoryName);
        if (existingCategory != null) {
            logger.warn("分类已存在: categoryName={}", categoryName);
            throw new RuntimeException("该分类已存在");
        }

        MaterialCategory category = new MaterialCategory(categoryName, sortOrder);
        categoryMapper.insert(category);
        
        logger.info("分类创建成功: categoryId={}", category.getCategoryId());
        return category;
    }

    /**
     * 创建子分类
     * @param categoryId 分类ID
     * @param subcategoryName 子分类名称
     * @param sortOrder 排序
     * @return 创建的子分类对象
     */
    @Transactional
    public MaterialSubcategory createSubcategory(Integer categoryId, String subcategoryName, Integer sortOrder) {
        logger.info("创建子分类: categoryId={}, subcategoryName={}", categoryId, subcategoryName);
        
        // 检查权限（部长/副部长）
        permissionUtil.checkMinisterPermission();

        // 检查分类是否存在
        MaterialCategory category = categoryMapper.findById(categoryId);
        if (category == null) {
            logger.warn("指定的分类不存在: categoryId={}", categoryId);
            throw new RuntimeException("指定的分类不存在");
        }

        // 检查子分类是否已存在
        MaterialSubcategory existingSubcategory = subcategoryMapper.findByNameAndCategoryId(subcategoryName, categoryId);
        if (existingSubcategory != null) {
            logger.warn("该分类下已存在同名子分类: categoryId={}, subcategoryName={}", categoryId, subcategoryName);
            throw new RuntimeException("该分类下已存在同名子分类");
        }

        MaterialSubcategory subcategory = new MaterialSubcategory(categoryId, subcategoryName, sortOrder);
        subcategoryMapper.insert(subcategory);
        
        logger.info("子分类创建成功: subcategoryId={}", subcategory.getSubcategoryId());
        return subcategory;
    }

    /**
     * 获取所有分类
     * @return 分类列表
     */
    public List<MaterialCategory> getAllCategories() {
        logger.info("获取所有分类");
        
        // 检查权限（部员及以上）
        permissionUtil.checkMemberPermission();

        List<MaterialCategory> categories = categoryMapper.findAll();
        logger.info("成功获取所有分类，数量={}", categories.size());
        return categories;
    }

    /**
     * 获取分类下的所有子分类
     * @param categoryId 分类ID
     * @return 子分类列表
     */
    public List<MaterialSubcategory> getSubcategoriesByCategoryId(Integer categoryId) {
        logger.info("获取分类下的所有子分类: categoryId={}", categoryId);
        
        // 检查权限（部员及以上）
        permissionUtil.checkMemberPermission();

        List<MaterialSubcategory> subcategories = subcategoryMapper.findByCategoryId(categoryId);
        logger.info("成功获取子分类列表: categoryId={}, 数量={}", categoryId, subcategories.size());
        return subcategories;
    }

    /**
     * 根据子分类ID获取资料列表
     * @param subcategoryId 子分类ID
     * @return 资料列表
     */
    public List<Material> getMaterialsBySubcategoryId(Integer subcategoryId) {
        logger.info("根据子分类ID获取资料列表: subcategoryId={}", subcategoryId);
        
        // 检查权限（部员及以上）
        permissionUtil.checkMemberPermission();

        List<Material> materials = materialMapper.findBySubcategoryId(subcategoryId);
        logger.info("成功获取资料列表: subcategoryId={}, 数量={}", subcategoryId, materials.size());
        return materials;
    }

    /**
     * 获取所有资料
     * @return 资料列表
     */
    public List<Material> getAllMaterials() {
        logger.info("获取所有资料");
        
        // 检查权限（部员及以上）
        permissionUtil.checkMemberPermission();

        List<Material> materials = materialMapper.findAll();
        logger.info("成功获取所有资料，数量={}", materials.size());
        return materials;
    }

    /**
     * 按名称搜索资料
     * @param name 资料名称
     * @return 资料列表
     */
    public List<Material> searchMaterialsByName(String name) {
        logger.info("按名称搜索资料: name={}", name);
        
        // 检查权限（部员及以上）
        permissionUtil.checkMemberPermission();

        List<Material> materials = materialMapper.findByNameContaining(name);
        logger.info("成功搜索资料: name={}, 数量={}", name, materials.size());
        return materials;
    }

    /**
     * 更新资料信息
     * @param materialId 资料ID
     * @param categoryId 分类ID
     * @param subcategoryId 子分类ID
     * @param materialName 资料名称
     * @param description 描述
     * @return 更新后的资料对象
     */
    @Transactional
    public Material updateMaterial(Integer materialId, Integer categoryId, Integer subcategoryId, 
                                   String materialName, String description) {
        logger.info("更新资料信息: materialId={}", materialId);
        
        // 检查权限（部长/副部长）
        permissionUtil.checkMinisterPermission();

        Material material = materialMapper.findById(materialId);
        if (material == null) {
            logger.warn("未找到指定资料: materialId={}", materialId);
            throw new RuntimeException("指定的资料不存在");
        }

        material.setCategoryId(categoryId);
        material.setSubcategoryId(subcategoryId);
        material.setMaterialName(materialName);
        material.setDescription(description);

        materialMapper.update(material);
        logger.info("资料信息更新成功: materialId={}", materialId);
        return material;
    }

    /**
     * 删除资料
     * @param materialId 资料ID
     */
    @Transactional
    public void deleteMaterial(Integer materialId) {
        logger.info("删除资料: materialId={}", materialId);
        
        // 检查权限（部长/副部长）
        permissionUtil.checkMinisterPermission();

        Material material = materialMapper.findById(materialId);
        if (material == null) {
            logger.warn("未找到指定资料: materialId={}", materialId);
            throw new RuntimeException("指定的资料不存在");
        }

        // 删除文件
        deleteMaterialFile(material.getFileUrl());

        // 删除数据库记录
        materialMapper.deleteById(materialId);

        logger.info("资料删除成功: materialId={}", materialId);
    }

    /**
     * 为资料文件生成预签名URL，用于安全下载
     * 使用Redis缓存，如果缓存未过期则直接返回，否则重新生成
     * 每次生成新URL时自动统计下载次数
     * 
     * @param materialId 资料ID
     * @param expirationSeconds 过期时间（秒），默认1小时
     * @return 预签名URL
     */
    @Transactional
    public String generateDownloadUrl(Integer materialId, Long expirationSeconds) {
        logger.info("为资料生成预签名URL: materialId={}, 过期时间={}s", materialId, expirationSeconds);
        
        // 检查权限（部员及以上）
        permissionUtil.checkMemberPermission();
        
        // 构建Redis缓存key
        String cacheKey = REDIS_PRESIGNED_URL_PREFIX + materialId;
        
        // 尝试从Redis获取缓存的URL
        String cachedUrl = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedUrl != null && !cachedUrl.isEmpty()) {
            logger.info("从Redis缓存中获取预签名URL: materialId={}", materialId);
            return cachedUrl;
        }
        
        // 缓存未命中或已过期，生成新URL
        logger.info("缓存未命中，生成新的预签名URL: materialId={}", materialId);
        
        // 获取资料信息
        String fileUrl = getFileUrlById(materialId);
        
        // 从完整URL中提取文件路径
        String filePath = extractOssFilePath(fileUrl);
        if (filePath == null || filePath.isEmpty()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件路径解析失败");
        }
        
        // 生成预签名URL
        String presignedUrl = ossUtil.generatePresignedUrl(filePath, expirationSeconds);
        
        // 存入Redis缓存，TTL为预签名URL的过期时间减去60秒（保证提前失效）
        long cacheTtl = Math.max(expirationSeconds - 60, 300); // 最小5分钟
        stringRedisTemplate.opsForValue().set(cacheKey, presignedUrl, cacheTtl, TimeUnit.SECONDS);
        logger.info("预签名URL已缓存到Redis: materialId={}, TTL={}s", materialId, cacheTtl);
        
        // 统计下载次数（每次生成新URL时累加）
        incrementDownloadCount(materialId);
        
        return presignedUrl;
    }
    
    /**
     * 为资料文件生成预签名URL（使用默认过期时间）
     * @param materialId 资料ID
     * @return 预签名URL
     */
    public String generateDownloadUrl(Integer materialId) {
        return generateDownloadUrl(materialId, 3600L); // 默认1小时
    }
    
    /**
     * 获取资料文件URL（保留原有接口兼容性）
     * @param materialId 资料ID
     * @return 文件URL
     */
    public String getFileUrlById(Integer materialId) {
        if (materialId == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "资料ID不能为空");
        }
        
        Material material = materialMapper.findById(materialId);
        if (material == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "未找到指定的资料");
        }
        
        return material.getFileUrl();
    }
    
    /**
     * 更新分类信息
     * @param categoryId 分类ID
     * @param categoryName 分类名称
     * @param sortOrder 排序
     * @return 更新后的分类对象
     */
    @Transactional
    public MaterialCategory updateCategory(Integer categoryId, String categoryName, Integer sortOrder) {
        logger.info("更新分类信息: categoryId={}, categoryName={}", categoryId, categoryName);
        
        // 检查权限（部长/副部长）
        permissionUtil.checkMinisterPermission();
        
        // 检查分类是否存在
        MaterialCategory category = categoryMapper.findById(categoryId);
        if (category == null) {
            logger.warn("未找到指定分类: categoryId={}", categoryId);
            throw new RuntimeException("指定的分类不存在");
        }
        
        // 检查名称是否与其他分类重复
        MaterialCategory existingCategory = categoryMapper.findByName(categoryName);
        if (existingCategory != null && !existingCategory.getCategoryId().equals(categoryId)) {
            logger.warn("分类名称已存在: categoryName={}", categoryName);
            throw new RuntimeException("该分类名称已存在");
        }
        
        category.setCategoryName(categoryName);
        category.setSortOrder(sortOrder);
        categoryMapper.update(category);
        
        logger.info("分类信息更新成功: categoryId={}", categoryId);
        return category;
    }
    
    /**
     * 更新子分类信息
     * @param subcategoryId 子分类ID
     * @param subcategoryName 子分类名称
     * @param sortOrder 排序
     * @return 更新后的子分类对象
     */
    @Transactional
    public MaterialSubcategory updateSubcategory(Integer subcategoryId, String subcategoryName, Integer sortOrder) {
        logger.info("更新子分类信息: subcategoryId={}, subcategoryName={}", subcategoryId, subcategoryName);
        
        // 检查权限（部长/副部长）
        permissionUtil.checkMinisterPermission();
        
        // 检查子分类是否存在
        MaterialSubcategory subcategory = subcategoryMapper.findById(subcategoryId);
        if (subcategory == null) {
            logger.warn("未找到指定子分类: subcategoryId={}", subcategoryId);
            throw new RuntimeException("指定的子分类不存在");
        }
        
        // 检查名称是否与同分类下的其他子分类重复
        MaterialSubcategory existingSubcategory = subcategoryMapper.findByNameAndCategoryId(
            subcategoryName, subcategory.getCategoryId());
        if (existingSubcategory != null && !existingSubcategory.getSubcategoryId().equals(subcategoryId)) {
            logger.warn("该分类下已存在同名子分类: categoryId={}, subcategoryName={}", 
                subcategory.getCategoryId(), subcategoryName);
            throw new RuntimeException("该分类下已存在同名子分类");
        }
        
        subcategory.setSubcategoryName(subcategoryName);
        subcategory.setSortOrder(sortOrder);
        subcategoryMapper.update(subcategory);
        
        logger.info("子分类信息更新成功: subcategoryId={}", subcategoryId);
        return subcategory;
    }
}






