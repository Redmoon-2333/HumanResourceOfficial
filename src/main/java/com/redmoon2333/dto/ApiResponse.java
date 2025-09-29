package com.redmoon2333.dto;

public class ApiResponse<T> {
    
    private String message;
    private T data;
    private int code;
    
    // 私有构造函数
    private ApiResponse(String message, T data, int code) {
        this.message = message;
        this.data = data;
        this.code = code;
    }
    
    // 成功响应
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("操作成功", data, 200);
    }
    
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(message, data, 200);
    }
    
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(message, null, 200);
    }
    
    // 失败响应
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(message, null, 400);
    }
    
    public static <T> ApiResponse<T> error(String message, int code) {
        return new ApiResponse<>(message, null, code);
    }
    
    // Getters and Setters
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public int getCode() {
        return code;
    }
    
    public void setCode(int code) {
        this.code = code;
    }
}