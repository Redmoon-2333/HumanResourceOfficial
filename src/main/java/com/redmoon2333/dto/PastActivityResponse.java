package com.redmoon2333.dto;

import com.redmoon2333.entity.PastActivity;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 往届活动响应DTO
 */
public class PastActivityResponse {
    
    private Integer pastActivityId;
    private String title;
    private String coverImage;
    private String pushUrl;
    private Integer year;
    private LocalDateTime createTime;
    
    // 构造函数
    public PastActivityResponse() {}
    
    public PastActivityResponse(PastActivity pastActivity) {
        this.pastActivityId = pastActivity.getPastActivityId();
        this.title = pastActivity.getTitle();
        this.coverImage = pastActivity.getCoverImage();
        this.pushUrl = pastActivity.getPushUrl();
        this.year = pastActivity.getYear();
        this.createTime = pastActivity.getCreateTime();
    }
    
    // 静态转换方法
    public static PastActivityResponse from(PastActivity pastActivity) {
        return new PastActivityResponse(pastActivity);
    }
    
    public static List<PastActivityResponse> fromList(List<PastActivity> pastActivities) {
        return pastActivities.stream()
                .map(PastActivityResponse::from)
                .toList();
    }
    
    // Getters and Setters
    public Integer getPastActivityId() {
        return pastActivityId;
    }
    
    public void setPastActivityId(Integer pastActivityId) {
        this.pastActivityId = pastActivityId;
    }
    
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
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    @Override
    public String toString() {
        return "PastActivityResponse{" +
                "pastActivityId=" + pastActivityId +
                ", title='" + title + '\'' +
                ", coverImage='" + coverImage + '\'' +
                ", pushUrl='" + pushUrl + '\'' +
                ", year=" + year +
                ", createTime=" + createTime +
                '}';
    }
}