package com.redmoon2333.dto;

import com.redmoon2333.entity.Material;
import com.redmoon2333.entity.MaterialCategory;
import com.redmoon2333.entity.MaterialSubcategory;

import java.time.LocalDateTime;

public class MaterialResponse {
    private Integer materialId;
    private Integer categoryId;
    private Integer subcategoryId;
    private String materialName;
    private String description;
    private String fileUrl;
    private Integer fileSize;
    private String fileType;
    private LocalDateTime uploadTime;
    private Integer uploaderId;
    private Integer downloadCount;
    
    // 关联信息
    private String categoryName;
    private String subcategoryName;
    
    public MaterialResponse() {}
    
    public MaterialResponse(Material material) {
        this.materialId = material.getMaterialId();
        this.categoryId = material.getCategoryId();
        this.subcategoryId = material.getSubcategoryId();
        this.materialName = material.getMaterialName();
        this.description = material.getDescription();
        this.fileUrl = material.getFileUrl();
        this.fileSize = material.getFileSize();
        this.fileType = material.getFileType();
        this.uploadTime = material.getUploadTime();
        this.uploaderId = material.getUploaderId();
        this.downloadCount = material.getDownloadCount();
    }
    
    public MaterialResponse(Material material, MaterialCategory category, MaterialSubcategory subcategory) {
        this(material);
        if (category != null) {
            this.categoryName = category.getCategoryName();
        }
        if (subcategory != null) {
            this.subcategoryName = subcategory.getSubcategoryName();
        }
    }
    
    // Getters and Setters
    public Integer getMaterialId() {
        return materialId;
    }
    
    public void setMaterialId(Integer materialId) {
        this.materialId = materialId;
    }
    
    public Integer getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }
    
    public Integer getSubcategoryId() {
        return subcategoryId;
    }
    
    public void setSubcategoryId(Integer subcategoryId) {
        this.subcategoryId = subcategoryId;
    }
    
    public String getMaterialName() {
        return materialName;
    }
    
    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getFileUrl() {
        return fileUrl;
    }
    
    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
    
    public Integer getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Integer fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    
    public LocalDateTime getUploadTime() {
        return uploadTime;
    }
    
    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }
    
    public Integer getUploaderId() {
        return uploaderId;
    }
    
    public void setUploaderId(Integer uploaderId) {
        this.uploaderId = uploaderId;
    }
    
    public Integer getDownloadCount() {
        return downloadCount;
    }
    
    public void setDownloadCount(Integer downloadCount) {
        this.downloadCount = downloadCount;
    }
    
    public String getCategoryName() {
        return categoryName;
    }
    
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
    
    public String getSubcategoryName() {
        return subcategoryName;
    }
    
    public void setSubcategoryName(String subcategoryName) {
        this.subcategoryName = subcategoryName;
    }
}