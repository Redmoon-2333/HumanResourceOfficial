package com.redmoon2333.service;

import com.redmoon2333.entity.Material;
import com.redmoon2333.entity.MaterialCategory;
import com.redmoon2333.entity.MaterialSubcategory;
import com.redmoon2333.mapper.MaterialCategoryMapper;
import com.redmoon2333.mapper.MaterialMapper;
import com.redmoon2333.mapper.MaterialSubcategoryMapper;
import com.redmoon2333.util.PermissionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MaterialService {
    
    private static final Logger logger = LoggerFactory.getLogger(MaterialService.class);
    
    @Autowired
    private MaterialMapper materialMapper;
    
    @Autowired
    private MaterialCategoryMapper categoryMapper;
    
    @Autowired
    private MaterialSubcategoryMapper subcategoryMapper;
    
    @Autowired
    private PermissionUtil permissionUtil;
    
    // 文件上传基础路径（本地存储）
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
            
            logger.info("开始上传文件: 文件名={}, 大小={}, 用户ID={}", 
                file.getOriginalFilename(), file.getSize(), uploaderId);
            
            // 检查分类和子分类是否存在
            MaterialCategory category = categoryMapper.findById(categoryId);
            if (category == null) {
                logger.warn("指定的分类不存在: categoryId={}", categoryId);
                throw new RuntimeException("指定的分类不存在");
            }
            
            MaterialSubcategory subcategory = subcategoryMapper.findById(subcategoryId);
            if (subcategory == null) {
                logger.warn("指定的子分类不存在: subcategoryId={}", subcategoryId);
                throw new RuntimeException("指定的子分类不存在");
            }
            
            // 保存文件到本地
            String fileUrl = saveFileToLocal(file);
            
            // 创建资料对象
            Material material = new Material();
            material.setCategoryId(categoryId);
            material.setSubcategoryId(subcategoryId);
            material.setMaterialName(materialName);
            material.setDescription(description);
            material.setFileUrl(fileUrl);
            material.setFileSize((int) file.getSize());
            material.setFileType(file.getContentType());
            material.setUploaderId(uploaderId);
            material.setUploadTime(LocalDateTime.now());
            
            // 保存到数据库
            materialMapper.insert(material);
            
            logger.info("文件上传成功: materialId={}, 文件路径={}", material.getMaterialId(), fileUrl);
            
            return material;
        } catch (IOException e) {
            logger.error("文件上传失败: 文件IO异常", e);
            throw e;
        } catch (Exception e) {
            logger.error("文件上传失败: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 将文件保存到本地
     * @param file 上传的文件
     * @return 文件访问路径
     */
    private String saveFileToLocal(MultipartFile file) throws IOException {
        try {
            // 获取项目根路径
            String projectRoot = System.getProperty("user.dir");
            String absoluteUploadPath = Paths.get(projectRoot, UPLOAD_BASE_PATH).toString();
            
            // 确保上传目录存在
            File uploadDir = new File(absoluteUploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
                logger.info("创建上传目录: {}", absoluteUploadPath);
            }
            
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            
            // 保存文件
            String filePath = Paths.get(absoluteUploadPath, uniqueFilename).toString();
            File destFile = new File(filePath);
            file.transferTo(destFile);
            
            // 返回相对于项目根目录的路径，用于访问文件
            String relativePath = UPLOAD_BASE_PATH + uniqueFilename;
            
            logger.info("文件保存成功: 原始文件名={}, 保存路径={}, 访问路径={}", originalFilename, filePath, relativePath);
            
            return relativePath;
        } catch (IOException e) {
            logger.error("文件保存失败: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 根据ID获取资料详情
     * @param materialId 资料ID
     * @return 资料对象
     */
    public Material getMaterialById(Integer materialId) {
        // 检查权限（部员及以上）
        permissionUtil.checkMemberPermission();
        
        Material material = materialMapper.findById(materialId);
        if (material == null) {
            throw new RuntimeException("指定的资料不存在");
        }
        return material;
    }
    
    /**
     * 根据分类ID获取资料列表
     * @param categoryId 分类ID
     * @return 资料列表
     */
    public List<Material> getMaterialsByCategoryId(Integer categoryId) {
        // 检查权限（部员及以上）
        permissionUtil.checkMemberPermission();
        
        return materialMapper.findByCategoryId(categoryId);
    }
    
    /**
     * 根据子分类ID获取资料列表
     * @param subcategoryId 子分类ID
     * @return 资料列表
     */
    public List<Material> getMaterialsBySubcategoryId(Integer subcategoryId) {
        // 检查权限（部员及以上）
        permissionUtil.checkMemberPermission();
        
        return materialMapper.findBySubcategoryId(subcategoryId);
    }
    
    /**
     * 获取所有资料
     * @return 资料列表
     */
    public List<Material> getAllMaterials() {
        // 检查权限（部员及以上）
        permissionUtil.checkMemberPermission();
        
        return materialMapper.findAll();
    }
    
    /**
     * 根据资料名称模糊查询
     * @param materialName 资料名称关键词
     * @return 资料列表
     */
    public List<Material> searchMaterialsByName(String materialName) {
        // 检查权限（部员及以上）
        permissionUtil.checkMemberPermission();
        
        return materialMapper.findByNameLike("%" + materialName + "%");
    }
    
    /**
     * 更新资料信息
     * @param materialId 资料ID
     * @param categoryId 分类ID
     * @param subcategoryId 子分类ID
     * @param materialName 资料名称
     * @param description 资料描述
     * @return 更新后的资料对象
     */
    @Transactional
    public Material updateMaterial(Integer materialId, Integer categoryId, Integer subcategoryId,
                                 String materialName, String description) {
        // 检查权限（部员及以上）
        permissionUtil.checkMemberPermission();
        Integer currentUserId = permissionUtil.getCurrentUserId();
        
        // 获取原始资料
        Material material = materialMapper.findById(materialId);
        if (material == null) {
            throw new RuntimeException("指定的资料不存在");
        }
        
        // 检查权限：部员只能修改自己上传的资料，部长/副部长可以修改所有资料
        if (!isUserAllowedToModifyMaterial(material, currentUserId)) {
            throw new RuntimeException("您只能修改自己上传的资料");
        }
        
        // 检查分类和子分类是否存在
        if (categoryId != null) {
            MaterialCategory category = categoryMapper.findById(categoryId);
            if (category == null) {
                throw new RuntimeException("指定的分类不存在");
            }
            material.setCategoryId(categoryId);
        }
        
        if (subcategoryId != null) {
            MaterialSubcategory subcategory = subcategoryMapper.findById(subcategoryId);
            if (subcategory == null) {
                throw new RuntimeException("指定的子分类不存在");
            }
            material.setSubcategoryId(subcategoryId);
        }
        
        // 更新资料信息
        if (materialName != null) {
            material.setMaterialName(materialName);
        }
        if (description != null) {
            material.setDescription(description);
        }
        
        materialMapper.update(material);
        
        return material;
    }
    
    /**
     * 删除资料
     * @param materialId 资料ID
     */
    @Transactional
    public void deleteMaterial(Integer materialId) {
        // 检查权限（部员及以上）
        permissionUtil.checkMemberPermission();
        Integer currentUserId = permissionUtil.getCurrentUserId();
        
        // 检查资料是否存在
        Material material = materialMapper.findById(materialId);
        if (material == null) {
            throw new RuntimeException("指定的资料不存在");
        }
        
        // 检查权限：部员只能删除自己上传的资料，部长/副部长可以删除所有资料
        if (!isUserAllowedToModifyMaterial(material, currentUserId)) {
            throw new RuntimeException("您只能删除自己上传的资料");
        }
        
        // 删除资料记录
        materialMapper.deleteById(materialId);
        
        // 删除本地文件（如果存在）
        String fileUrl = material.getFileUrl();
        if (fileUrl != null && !fileUrl.isEmpty()) {
            String projectRoot = System.getProperty("user.dir");
            String absoluteFilePath = Paths.get(projectRoot, fileUrl).toString();
            File file = new File(absoluteFilePath);
            if (file.exists()) {
                file.delete();
                logger.info("文件删除成功: {}", absoluteFilePath);
            }
        }
    }
    
    /**
     * 检查用户是否有权限修改/删除指定资料
     * @param material 资料对象
     * @param currentUserId 当前用户ID
     * @return 是否有权限
     */
    private boolean isUserAllowedToModifyMaterial(Material material, Integer currentUserId) {
        // 如果是部长或副部长，可以修改所有资料
        String roleHistory = permissionUtil.getCurrentUserRoleHistory();
        if (permissionUtil.hasMinisterRole(roleHistory)) {
            return true;
        }
        
        // 如果是普通部员，只能修改自己上传的资料
        return material.getUploaderId().equals(currentUserId);
    }
    
    /**
     * 增加资料下载次数
     * @param materialId 资料ID
     */
    @Transactional
    public void incrementDownloadCount(Integer materialId) {
        // 检查权限（部员及以上）
        permissionUtil.checkMemberPermission();
        
        Material material = materialMapper.findById(materialId);
        if (material == null) {
            throw new RuntimeException("指定的资料不存在");
        }
        
        materialMapper.updateDownloadCount(materialId);
        
        logger.info("资料下载次数增加: materialId={}, 新下载次数={}", materialId, material.getDownloadCount() + 1);
    }
    
    /**
     * 创建分类
     * @param categoryName 分类名称
     * @param sortOrder 排序
     * @return 创建的分类对象
     */
    @Transactional
    public MaterialCategory createCategory(String categoryName, Integer sortOrder) {
        // 检查权限（部长/副部长）
        permissionUtil.checkMinisterPermission();
        
        // 检查分类是否已存在
        MaterialCategory existingCategory = categoryMapper.findByName(categoryName);
        if (existingCategory != null) {
            throw new RuntimeException("分类名称已存在");
        }
        
        MaterialCategory category = new MaterialCategory(categoryName, sortOrder);
        categoryMapper.insert(category);
        
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
        // 检查权限（部长/副部长）
        permissionUtil.checkMinisterPermission();
        
        // 检查分类是否存在
        MaterialCategory category = categoryMapper.findById(categoryId);
        if (category == null) {
            throw new RuntimeException("指定的分类不存在");
        }
        
        // 检查子分类是否已存在
        MaterialSubcategory existingSubcategory = subcategoryMapper.findByNameAndCategoryId(subcategoryName, categoryId);
        if (existingSubcategory != null) {
            throw new RuntimeException("该分类下已存在同名子分类");
        }
        
        MaterialSubcategory subcategory = new MaterialSubcategory(categoryId, subcategoryName, sortOrder);
        subcategoryMapper.insert(subcategory);
        
        return subcategory;
    }
    
    /**
     * 获取所有分类
     * @return 分类列表
     */
    public List<MaterialCategory> getAllCategories() {
        // 检查权限（部员及以上）
        permissionUtil.checkMemberPermission();
        
        return categoryMapper.findAll();
    }
    
    /**
     * 获取分类下的所有子分类
     * @param categoryId 分类ID
     * @return 子分类列表
     */
    public List<MaterialSubcategory> getSubcategoriesByCategoryId(Integer categoryId) {
        // 检查权限（部员及以上）
        permissionUtil.checkMemberPermission();
        
        return subcategoryMapper.findByCategoryId(categoryId);
    }
}