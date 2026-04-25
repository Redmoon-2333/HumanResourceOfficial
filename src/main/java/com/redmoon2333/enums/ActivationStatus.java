package com.redmoon2333.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 激活码状态枚举
 */
public enum ActivationStatus {
    未使用("未使用", 0),
    已使用("已使用", 1),
    已过期("已过期", 2);

    @EnumValue
    private final int code;
    private final String description;

    ActivationStatus(String description, int code) {
        this.description = description;
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String toString() {
        return description;
    }
}