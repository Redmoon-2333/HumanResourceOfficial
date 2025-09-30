package com.redmoon2333.dto;

import java.time.LocalDateTime;

public class ActivityResponse {
    private Integer activityId;
    private String activityName;
    private String background;
    private String significance;
    private String purpose;
    private String process;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    // Constructors
    public ActivityResponse() {
    }
    
    public ActivityResponse(Integer activityId, String activityName, String background, String significance, 
                           String purpose, String process, LocalDateTime createTime, LocalDateTime updateTime) {
        this.activityId = activityId;
        this.activityName = activityName;
        this.background = background;
        this.significance = significance;
        this.purpose = purpose;
        this.process = process;
        this.createTime = createTime;
        this.updateTime = updateTime;
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
}