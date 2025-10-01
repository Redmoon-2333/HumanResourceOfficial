package com.redmoon2333.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "material_subcategory")
public class MaterialSubcategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subcategory_id")
    private Integer subcategoryId;
    
    @Column(name = "category_id")
    private Integer categoryId;
    
    @Column(name = "subcategory_name")
    private String subcategoryName;
    
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
    
    @Column(name = "create_time")
    private LocalDateTime createTime;
    
    // 构造函数
    public MaterialSubcategory() {
        this.createTime = LocalDateTime.now();
    }
    
    public MaterialSubcategory(Integer categoryId, String subcategoryName) {
        this();
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