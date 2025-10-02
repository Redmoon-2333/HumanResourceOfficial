package com.redmoon2333.dto;

public class SubcategoryRequest {
    private Integer categoryId;
    private String subcategoryName;
    private Integer sortOrder;
    
    // Getters and Setters
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
}