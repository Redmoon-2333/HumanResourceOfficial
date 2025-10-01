package com.redmoon2333.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_image")
public class ActivityImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Integer imageId;
    
    @Column(name = "activity_id")
    private Integer activityId;
    
    @Column(name = "image_url")
    private String imageUrl;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
    
    @Column(name = "upload_time")
    private LocalDateTime uploadTime;
    
    // 构造函数
    public ActivityImage() {
        this.uploadTime = LocalDateTime.now();
    }
    
    public ActivityImage(Integer activityId, String imageUrl, String description) {
        this();
        this.activityId = activityId;
        this.imageUrl = imageUrl;
        this.description = description;
    }
    
    // Getters and Setters
    public Integer getImageId() {
        return imageId;
    }
    
    public void setImageId(Integer imageId) {
        this.imageId = imageId;
    }
    
    public Integer getActivityId() {
        return activityId;
    }
    
    public void setActivityId(Integer activityId) {
        this.activityId = activityId;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Integer getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public LocalDateTime getUploadTime() {
        return uploadTime;
    }
    
    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }
    
    @Override
    public String toString() {
        return "ActivityImage{" +
                "imageId=" + imageId +
                ", activityId=" + activityId +
                ", imageUrl='" + imageUrl + '\'' +
                ", description='" + description + '\'' +
                ", sortOrder=" + sortOrder +
                ", uploadTime=" + uploadTime +
                '}';
    }
}