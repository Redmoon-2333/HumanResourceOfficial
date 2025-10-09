package com.redmoon2333.dto;

public class PresignedUrlResponse {
    private String presignedUrl;
    private String fileName;
    private Long expirationTime; // 过期时间戳

    public PresignedUrlResponse() {}

    public PresignedUrlResponse(String presignedUrl, String fileName, Long expirationTime) {
        this.presignedUrl = presignedUrl;
        this.fileName = fileName;
        this.expirationTime = expirationTime;
    }

    // Getters and Setters
    public String getPresignedUrl() {
        return presignedUrl;
    }

    public void setPresignedUrl(String presignedUrl) {
        this.presignedUrl = presignedUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(Long expirationTime) {
        this.expirationTime = expirationTime;
    }
}