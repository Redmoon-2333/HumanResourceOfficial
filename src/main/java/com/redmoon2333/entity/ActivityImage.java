package com.redmoon2333.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("activity_image")
public class ActivityImage {
    @TableId(type = IdType.AUTO)
    private Integer imageId;
    
    private Integer activityId;
    
    private String imageUrl;
    
    private String description;
    
    private Integer sortOrder = 0;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime uploadTime;
    
    // 构造函数
    public ActivityImage() {
    }
    
    public ActivityImage(Integer activityId, String imageUrl, String description) {
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
