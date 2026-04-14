package com.redmoon2333.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("material_subcategory")
public class MaterialSubcategory {
    @TableId(type = IdType.AUTO)
    private Integer subcategoryId;
    
    private Integer categoryId;
    
    private String subcategoryName;
    
    private Integer sortOrder = 0;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    // 构造函数
    public MaterialSubcategory() {
    }
    
    public MaterialSubcategory(Integer categoryId, String subcategoryName) {
        this.categoryId = categoryId;
        this.subcategoryName = subcategoryName;
    }
    
    public MaterialSubcategory(Integer categoryId, String subcategoryName, Integer sortOrder) {
        this(categoryId, subcategoryName);
        this.sortOrder = sortOrder;
    }
    
    // Getters and Setters
    public Integer getSubcategoryId() {
        return subcategoryId;
    }
    
    public void setSubcategoryId(Integer subcategoryId) {
        this.subcategoryId = subcategoryId;
    }
    
    public Integer getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }
    
    public String getSubcategoryName() {
        return subcategoryName;
    }
    
    public void setSubcategoryName(String subcategoryName) {
        this.subcategoryName = subcategoryName;
    }
    
    public Integer getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    @Override
    public String toString() {
        return "MaterialSubcategory{" +
                "subcategoryId=" + subcategoryId +
                ", categoryId=" + categoryId +
                ", subcategoryName='" + subcategoryName + '\'' +
                ", sortOrder=" + sortOrder +
                ", createTime=" + createTime +
                '}';
    }
}
