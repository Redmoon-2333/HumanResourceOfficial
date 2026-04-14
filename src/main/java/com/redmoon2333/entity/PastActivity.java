package com.redmoon2333.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("past_activity")
public class PastActivity {
    @TableId(type = IdType.AUTO)
    private Integer pastActivityId;
    
    private String title;
    
    private String coverImage;
    
    private String pushUrl;
    
    private Integer year;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    // 构造函数
    public PastActivity() {
    }
    
    public PastActivity(String title, String coverImage, String pushUrl, Integer year) {
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
