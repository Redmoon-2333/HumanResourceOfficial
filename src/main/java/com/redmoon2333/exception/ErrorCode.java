package com.redmoon2333.exception;

public enum ErrorCode {
    // 用户相关错误 1000-1999
    USER_NOT_FOUND(1001, "用户不存在"),
    USERNAME_EXISTS(1002, "用户名已存在"),
    INVALID_CREDENTIALS(1003, "用户名或密码错误"),
    INVALID_ACTIVATION_CODE(1004, "激活码无效或已过期"),
    PASSWORD_MISMATCH(1005, "两次输入的密码不一致"),
    
    // Token相关错误 2000-2999
    INVALID_TOKEN(2001, "令牌无效或已过期"),
    TOKEN_REQUIRED(2002, "需要提供有效的令牌"),
    
    // 权限相关错误 3000-3999
    INSUFFICIENT_PERMISSIONS(3001, "权限不足"),
    
    // 请求参数相关错误 4000-4999
    INVALID_REQUEST_PARAMETER(4001, "请求参数不正确"),
    VALIDATION_FAILED(4002, "数据验证失败"),
    
    // 系统错误 5000-5999
    SYSTEM_ERROR(5000, "系统内部错误");
    
    private final int code;
    private final String message;
    
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
}