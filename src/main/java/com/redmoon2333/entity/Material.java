package com.redmoon2333.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("material")
public class Material {
    @TableId(type = IdType.AUTO)
    private Integer materialId;
    
    private Integer categoryId;
    
    private Integer subcategoryId;
    
    private String materialName;
    
    private String description;
    
    private String fileUrl;
    
    private Integer fileSize;
    
    private String fileType;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime uploadTime;
    
    private Integer uploaderId;
    
    private Integer downloadCount = 0;
    
    // 构造函数
    public Material() {
    }
    
    public Material(Integer categoryId, Integer subcategoryId, String materialName, 
                   String fileUrl, Integer uploaderId) {
        this.categoryId = categoryId;
        this.subcategoryId = subcategoryId;
        this.materialName = materialName;
        this.fileUrl = fileUrl;
        this.uploaderId = uploaderId;
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
    
    @Override
    public String toString() {
        return "Material{" +
                "materialId=" + materialId +
                ", categoryId=" + categoryId +
                ", subcategoryId=" + subcategoryId +
                ", materialName='" + materialName + '\'' +
                ", description='" + description + '\'' +
                ", fileUrl='" + fileUrl + '\'' +
                ", fileSize=" + fileSize +
                ", fileType='" + fileType + '\'' +
                ", uploadTime=" + uploadTime +
                ", uploaderId=" + uploaderId +
                ", downloadCount=" + downloadCount +
                '}';
    }
}
