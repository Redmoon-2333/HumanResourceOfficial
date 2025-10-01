package com.redmoon2333.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "material_category")
public class MaterialCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Integer categoryId;
    
    @Column(name = "category_name")
    private String categoryName;
    
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
    
    @Column(name = "create_time")
    private LocalDateTime createTime;
    
    // 构造函数
    public MaterialCategory() {
        this.createTime = LocalDateTime.now();
    }
    
    public MaterialCategory(String categoryName) {
        this();
        this.categoryName = categoryName;
    }
    
    public MaterialCategory(String categoryName, Integer sortOrder) {
        this(categoryName);
        this.sortOrder = sortOrder;
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
    
    @Override
    public String toString() {
        return "MaterialCategory{" +
                "categoryId=" + categoryId +
                ", categoryName='" + categoryName + '\'' +
                ", sortOrder=" + sortOrder +
                ", createTime=" + createTime +
                '}';
    }
}