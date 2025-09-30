package com.redmoon2333.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ActivityRequest {
    @NotBlank(message = "活动名称不能为空")
    @Size(max = 255, message = "活动名称长度不能超过255个字符")
    private String activityName;
    
    @NotBlank(message = "活动背景不能为空")
    private String background;
    
    @NotBlank(message = "活动意义不能为空")
    private String significance;
    
    @NotBlank(message = "活动目的不能为空")
    private String purpose;
    
    @NotBlank(message = "活动流程不能为空")
    private String process;
    
    // Getters and Setters
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
}