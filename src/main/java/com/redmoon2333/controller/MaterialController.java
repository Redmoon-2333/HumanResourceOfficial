package com.redmoon2333.controller;

import com.redmoon2333.annotation.RequireMemberRole;
import com.redmoon2333.annotation.RequireMinisterRole;
import com.redmoon2333.dto.*;
import com.redmoon2333.entity.Material;
import com.redmoon2333.entity.MaterialCategory;
import com.redmoon2333.entity.MaterialSubcategory;
import com.redmoon2333.exception.BusinessException;
import com.redmoon2333.exception.ErrorCode;
import com.redmoon2333.service.MaterialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/materials")
public class MaterialController {
    
    private static final Logger logger = LoggerFactory.getLogger(MaterialController.class);
    
    @Autowired
    private MaterialService materialService;
    
    /**
     * 上传资料文件
     */
    @PostMapping("/upload")
    @RequireMemberRole("上传内部资料")
    public ApiResponse<MaterialResponse> uploadMaterial(
            @RequestParam("file") MultipartFile file,
            @RequestParam("categoryId") Integer categoryId,
            @RequestParam("subcategoryId") Integer subcategoryId,
            @RequestParam("materialName") String materialName,
            @RequestParam(value = "description", required = false) String description) {
        
        try {
            logger.info("收到文件上传请求: 文件名={}, 大小={}, categoryId={}, subcategoryId={}", 
                file.getOriginalFilename(), file.getSize(), categoryId, subcategoryId);
            
            Material material = materialService.uploadMaterial(file, categoryId, subcategoryId, materialName, description);
            MaterialResponse response = new MaterialResponse(material);
            return ApiResponse.success(response);
        } catch (MaxUploadSizeExceededException e) {
            logger.error("文件上传失败: 文件大小超过限制", e);
            return ApiResponse.error("文件大小超过限制，请上传小于100MB的文件", ErrorCode.FILE_UPLOAD_ERROR.getCode());
        } catch (IOException e) {
            logger.error("文件上传失败: 文件IO异常", e);
            return ApiResponse.error(ErrorCode.FILE_UPLOAD_ERROR.getMessage(), ErrorCode.FILE_UPLOAD_ERROR.getCode());
        } catch (Exception e) {
            logger.error("文件上传失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage(), ErrorCode.MATERIAL_UPLOAD_FAILED.getCode());
        }
    }
    
    /**
     * 下载资料文件
     */
    @GetMapping("/download/{materialId}")
    @RequireMemberRole("下载内部资料")
    public ResponseEntity<Resource> downloadMaterial(@PathVariable Integer materialId) {
        try {
            // 获取文件信息
            Material material = materialService.getMaterialById(materialId);
            
            // 获取项目根路径
            String projectRoot = System.getProperty("user.dir");
            String absoluteFilePath = Paths.get(projectRoot, material.getFileUrl()).toString();
            
            File file = new File(absoluteFilePath);
            if (!file.exists()) {
                logger.warn("文件不存在: {}", absoluteFilePath);
                return ResponseEntity.notFound().build();
            }
            
            // 增加下载次数
            materialService.incrementDownloadCount(materialId);
            
            // 创建资源
            FileSystemResource resource = new FileSystemResource(file);
            
            // 设置响应头，对文件名进行URL编码以支持中文
            String encodedFileName = URLEncoder.encode(material.getMaterialName(), StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName);
            headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(file.length()));
            
            // 确定内容类型
            String contentType = material.getFileType();
            if (contentType == null || contentType.isEmpty()) {
                contentType = "application/octet-stream";
            }
            
            logger.info("文件下载成功: materialId={}, 文件名={}", materialId, material.getMaterialName());
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(file.length())
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            logger.error("文件下载失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取资料详情
     */
    @GetMapping("/{materialId}")
    @RequireMemberRole("查看内部资料详情")
    public ApiResponse<MaterialResponse> getMaterial(@PathVariable Integer materialId) {
        try {
            Material material = materialService.getMaterialById(materialId);
            MaterialResponse response = new MaterialResponse(material);
            return ApiResponse.success(response);
        } catch (Exception e) {
            logger.error("获取资料详情失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage(), ErrorCode.MATERIAL_NOT_FOUND.getCode());
        }
    }
    
    /**
     * 根据分类ID获取资料列表
     */
    @GetMapping("/category/{categoryId}")
    @RequireMemberRole("查看分类下的内部资料")
    public ApiResponse<List<MaterialResponse>> getMaterialsByCategoryId(@PathVariable Integer categoryId) {
        try {
            List<Material> materials = materialService.getMaterialsByCategoryId(categoryId);
            List<MaterialResponse> responses = materials.stream()
                    .map(MaterialResponse::new)
                    .collect(Collectors.toList());
            return ApiResponse.success(responses);
        } catch (Exception e) {
            logger.error("根据分类ID获取资料列表失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage(), ErrorCode.MATERIAL_QUERY_FAILED.getCode());
        }
    }
    
    /**
     * 根据子分类ID获取资料列表
     */
    @GetMapping("/subcategory/{subcategoryId}")
    @RequireMemberRole("查看子分类下的内部资料")
    public ApiResponse<List<MaterialResponse>> getMaterialsBySubcategoryId(@PathVariable Integer subcategoryId) {
        try {
            List<Material> materials = materialService.getMaterialsBySubcategoryId(subcategoryId);
            List<MaterialResponse> responses = materials.stream()
                    .map(MaterialResponse::new)
                    .collect(Collectors.toList());
            return ApiResponse.success(responses);
        } catch (Exception e) {
            logger.error("根据子分类ID获取资料列表失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage(), ErrorCode.MATERIAL_QUERY_FAILED.getCode());
        }
    }
    
    /**
     * 获取所有资料
     */
    @GetMapping
    @RequireMemberRole("查看所有内部资料")
    public ApiResponse<List<MaterialResponse>> getAllMaterials() {
        try {
            List<Material> materials = materialService.getAllMaterials();
            List<MaterialResponse> responses = materials.stream()
                    .map(MaterialResponse::new)
                    .collect(Collectors.toList());
            return ApiResponse.success(responses);
        } catch (Exception e) {
            logger.error("获取所有资料失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage(), ErrorCode.MATERIAL_QUERY_FAILED.getCode());
        }
    }
    
    /**
     * 根据资料名称模糊查询
     */
    @GetMapping("/search")
    @RequireMemberRole("搜索内部资料")
    public ApiResponse<List<MaterialResponse>> searchMaterials(@RequestParam("name") String materialName) {
        try {
            List<Material> materials = materialService.searchMaterialsByName(materialName);
            List<MaterialResponse> responses = materials.stream()
                    .map(MaterialResponse::new)
                    .collect(Collectors.toList());
            return ApiResponse.success(responses);
        } catch (Exception e) {
            logger.error("搜索资料失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage(), ErrorCode.MATERIAL_QUERY_FAILED.getCode());
        }
    }
    
    /**
     * 更新资料信息
     */
    @PutMapping("/{materialId}")
    @RequireMemberRole("更新内部资料信息")
    public ApiResponse<MaterialResponse> updateMaterial(
            @PathVariable Integer materialId,
            @RequestBody MaterialRequest request) {
        try {
            Material material = materialService.updateMaterial(
                    materialId,
                    request.getCategoryId(),
                    request.getSubcategoryId(),
                    request.getMaterialName(),
                    request.getDescription()
            );
            MaterialResponse response = new MaterialResponse(material);
            return ApiResponse.success(response);
        } catch (Exception e) {
            logger.error("更新资料信息失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage(), ErrorCode.MATERIAL_UPDATE_FAILED.getCode());
        }
    }
    
    /**
     * 删除资料
     */
    @DeleteMapping("/{materialId}")
    @RequireMemberRole("删除内部资料")
    public ApiResponse<Void> deleteMaterial(@PathVariable Integer materialId) {
        try {
            materialService.deleteMaterial(materialId);
            return ApiResponse.success(null);
        } catch (Exception e) {
            logger.error("删除资料失败: {}", e.getMessage(), e);
            // 根据不同的异常类型返回不同的错误码
            if (e.getMessage().contains("资料不存在")) {
                return ApiResponse.error(e.getMessage(), ErrorCode.MATERIAL_NOT_FOUND.getCode());
            }
            return ApiResponse.error(e.getMessage(), ErrorCode.MATERIAL_DELETE_FAILED.getCode());
        }
    }
    
    /**
     * 创建分类
     */
    @PostMapping("/category")
    @RequireMinisterRole("创建资料分类")
    public ApiResponse<CategoryResponse> createCategory(@RequestBody CategoryRequest request) {
        try {
            MaterialCategory category = materialService.createCategory(
                    request.getCategoryName(),
                    request.getSortOrder()
            );
            CategoryResponse response = new CategoryResponse(category);
            return ApiResponse.success(response);
        } catch (Exception e) {
            logger.error("创建分类失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage(), ErrorCode.CATEGORY_CREATE_FAILED.getCode());
        }
    }
    
    /**
     * 获取所有分类
     */
    @GetMapping("/categories")
    @RequireMemberRole("查看资料分类")
    public ApiResponse<List<CategoryResponse>> getAllCategories() {
        try {
            List<MaterialCategory> categories = materialService.getAllCategories();
            List<CategoryResponse> responses = categories.stream()
                    .map(CategoryResponse::new)
                    .collect(Collectors.toList());
            return ApiResponse.success(responses);
        } catch (Exception e) {
            logger.error("获取所有分类失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage(), ErrorCode.CATEGORY_QUERY_FAILED.getCode());
        }
    }
    
    /**
     * 创建子分类
     */
    @PostMapping("/subcategory")
    @RequireMinisterRole("创建资料子分类")
    public ApiResponse<SubcategoryResponse> createSubcategory(@RequestBody SubcategoryRequest request) {
        try {
            MaterialSubcategory subcategory = materialService.createSubcategory(
                    request.getCategoryId(),
                    request.getSubcategoryName(),
                    request.getSortOrder()
            );
            SubcategoryResponse response = new SubcategoryResponse(subcategory);
            return ApiResponse.success(response);
        } catch (Exception e) {
            logger.error("创建子分类失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage(), ErrorCode.SUBCATEGORY_CREATE_FAILED.getCode());
        }
    }
    
    /**
     * 获取分类下的所有子分类
     */
    @GetMapping("/category/{categoryId}/subcategories")
    @RequireMemberRole("查看分类下的子分类")
    public ApiResponse<List<SubcategoryResponse>> getSubcategoriesByCategoryId(@PathVariable Integer categoryId) {
        try {
            List<MaterialSubcategory> subcategories = materialService.getSubcategoriesByCategoryId(categoryId);
            List<SubcategoryResponse> responses = subcategories.stream()
                    .map(SubcategoryResponse::new)
                    .collect(Collectors.toList());
            return ApiResponse.success(responses);
        } catch (Exception e) {
            logger.error("获取分类下的所有子分类失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage(), ErrorCode.SUBCATEGORY_QUERY_FAILED.getCode());
        }
    }
}