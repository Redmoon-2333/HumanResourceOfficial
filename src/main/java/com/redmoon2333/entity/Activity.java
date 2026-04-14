package com.redmoon2333.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("activity")
public class Activity {
    @TableId(type = IdType.AUTO)
    private Integer activityId;
    
    private String activityName;
    
    private String background;
    
    private String significance;
    
    private String purpose;
    
    private String process;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    // Constructors
    public Activity() {
    }
    
    public Activity(String activityName, String background, String significance, String purpose, String process) {
        this.activityName = activityName;
        this.background = background;
        this.significance = significance;
        this.purpose = purpose;
        this.process = process;
    }
    
    // Getters and Setters
    public Integer getActivityId() {
        return activityId;
    }
    
    public void setActivityId(Integer activityId) {
        this.activityId = activityId;
    }
    
    public String getActivityName() {
        return activityName;
    }
    
    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }
    
    public String getBackground() {
        return background;
    }
    
    public void setBackground(String background) {
        this.background = background;
    }
    
    public String getSignificance() {
        return significance;
    }
    
    public void setSignificance(String significance) {
        this.significance = significance;
    }
    
    public String getPurpose() {
        return purpose;
    }
    
    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
    
    public String getProcess() {
        return process;
    }
    
    public void setProcess(String process) {
        this.process = process;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
    
    @Override
    public String toString() {
        return "Activity{" +
                "activityId=" + activityId +
                ", activityName='" + activityName + '\'' +
                ", background='" + background + '\'' +
                ", significance='" + significance + '\'' +
                ", purpose='" + purpose + '\'' +
                ", process='" + process + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}
