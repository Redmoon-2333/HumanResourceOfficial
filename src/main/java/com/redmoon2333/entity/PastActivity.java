package com.redmoon2333.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "past_activity")
public class PastActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer pastActivityId;
    
    private String title;
    
    private String coverImage;
    
    private String pushUrl;
    
    private Integer year;
    
    private LocalDateTime createTime;
    
    // 构造函数
    public PastActivity() {
        this.createTime = LocalDateTime.now();
    }
    
    public PastActivity(String title, String coverImage, String pushUrl, Integer year) {
        this();
        this.title = title;
        this.coverImage = coverImage;
        this.pushUrl = pushUrl;
        this.year = year;
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
        return "PastActivity{" +
                "pastActivityId=" + pastActivityId +
                ", title='" + title + '\'' +
                ", coverImage='" + coverImage + '\'' +
                ", pushUrl='" + pushUrl + '\'' +
                ", year=" + year +
                ", createTime=" + createTime +
                '}';
    }
}