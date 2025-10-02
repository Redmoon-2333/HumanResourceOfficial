package com.redmoon2333.dto;

import com.redmoon2333.entity.MaterialCategory;

import java.time.LocalDateTime;

public class CategoryResponse {
    private Integer categoryId;
    private String categoryName;
    private Integer sortOrder;
    private LocalDateTime createTime;
    
    public CategoryResponse() {}
    
    public CategoryResponse(MaterialCategory category) {
        this.categoryId = category.getCategoryId();
        this.categoryName = category.getCategoryName();
        this.sortOrder = category.getSortOrder();
        this.createTime = category.getCreateTime();
    }
    
    // Getters and Setters
    public Integer getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }
    
    public String getCategoryName() {
        return categoryName;
    }
    
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
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