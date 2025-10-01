package com.redmoon2333.enums;

/**
 * 激活码状态枚举
 */
public enum ActivationStatus {
    未使用("未使用"),
    已使用("已使用");
    
    private final String description;
    
    ActivationStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return description;
    }
}