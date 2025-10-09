package com.redmoon2333.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class PresignedUrlRequest {
    
    @NotBlank(message = "文件路径不能为空")
    private String filePath;
    
    @NotNull(message = "过期时间不能为空")
    @Positive(message = "过期时间必须为正数")
    private Long expirationSeconds; // 过期时间，单位秒，默认3600秒（1小时）

    public PresignedUrlRequest() {}

    // Getters and Setters
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getExpirationSeconds() {
        return expirationSeconds;
    }

    public void setExpirationSeconds(Long expirationSeconds) {
        this.expirationSeconds = expirationSeconds;
    }
}