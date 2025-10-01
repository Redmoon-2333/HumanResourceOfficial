package com.redmoon2333.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 往届活动请求DTO
 */
public class PastActivityRequest {
    
    @NotBlank(message = "活动标题不能为空")
    private String title;
    
    private String coverImage;
    
    private String pushUrl;
    
    @NotNull(message = "年份不能为空")
    private Integer year;
    
    // 构造函数
    public PastActivityRequest() {}
    
    public PastActivityRequest(String title, String coverImage, String pushUrl, Integer year) {
        this.title = title;
        this.coverImage = coverImage;
        this.pushUrl = pushUrl;
        this.year = year;
    }
    
    // Getters and Setters
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getCoverImage() {
        return coverImage;
    }
    
    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }
    
    public String getPushUrl() {
        return pushUrl;
    }
    
    public void setPushUrl(String pushUrl) {
        this.pushUrl = pushUrl;
    }
    
    public Integer getYear() {
        return year;
    }
    
    public void setYear(Integer year) {
        this.year = year;
    }
    
    @Override
    public String toString() {
        return "PastActivityRequest{" +
                "title='" + title + '\'' +
                ", coverImage='" + coverImage + '\'' +
                ", pushUrl='" + pushUrl + '\'' +
                ", year=" + year +
                '}';
    }
}