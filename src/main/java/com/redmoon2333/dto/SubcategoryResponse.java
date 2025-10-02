package com.redmoon2333.dto;

import com.redmoon2333.entity.MaterialSubcategory;

import java.time.LocalDateTime;

public class SubcategoryResponse {
    private Integer subcategoryId;
    private Integer categoryId;
    private String subcategoryName;
    private Integer sortOrder;
    private LocalDateTime createTime;
    
    public SubcategoryResponse() {}
    
    public SubcategoryResponse(MaterialSubcategory subcategory) {
        this.subcategoryId = subcategory.getSubcategoryId();
        this.categoryId = subcategory.getCategoryId();
        this.subcategoryName = subcategory.getSubcategoryName();
        this.sortOrder = subcategory.getSortOrder();
        this.createTime = subcategory.getCreateTime();
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
}