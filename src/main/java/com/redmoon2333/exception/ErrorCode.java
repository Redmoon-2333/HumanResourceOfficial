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
    TOKEN_MISSING(2003, "缺少认证令牌"),
    TOKEN_EXPIRED(2004, "令牌已过期"),
    TOKEN_MALFORMED(2005, "令牌格式错误"),
    
    // 权限相关错误 3000-3999
    INSUFFICIENT_PERMISSIONS(3001, "权限不足"),
    UNAUTHORIZED_ACTIVITY_OPERATION(3002, "只有部长或副部长才能执行此操作"),
    
    // 活动相关错误 6000-6999
    ACTIVITY_NOT_FOUND(6001, "活动不存在"),
    ACTIVITY_ALREADY_EXISTS(6002, "活动已存在"),
    ACTIVITY_CREATION_FAILED(6003, "活动创建失败"),
    ACTIVITY_CREATE_FAILED(6003, "活动创建失败"), // 别名
    ACTIVITY_UPDATE_FAILED(6004, "活动更新失败"),
    ACTIVITY_DELETE_FAILED(6005, "活动删除失败"),
    ACTIVITY_LIST_FAILED(6006, "活动列表获取失败"),
    ACTIVITY_DETAIL_FAILED(6007, "活动详情获取失败"),
    
    // 活动图片相关错误 6100-6199
    ACTIVITY_IMAGE_SAVE_FAILED(6101, "活动图片保存失败"),
    ACTIVITY_IMAGE_DELETE_FAILED(6102, "活动图片删除失败"),
    ACTIVITY_IMAGE_LIST_FAILED(6103, "活动图片列表获取失败"),
    ACTIVITY_IMAGE_NOT_FOUND(6104, "活动图片不存在"),
    ACTIVITY_IMAGE_UPDATE_FAILED(6105, "活动图片更新失败"),
    
    // 往届活动相关错误 7000-7999
    NOT_FOUND(7001, "资源不存在"),
    OPERATION_ERROR(7002, "操作失败"),
    
    // 内部资料相关错误 8000-8999
    FILE_UPLOAD_ERROR(8001, "文件上传失败"),
    MATERIAL_NOT_FOUND(8002, "资料不存在"),
    MATERIAL_UPLOAD_FAILED(8003, "资料上传失败"),
    MATERIAL_QUERY_FAILED(8004, "资料查询失败"),
    MATERIAL_UPDATE_FAILED(8005, "资料更新失败"),
    MATERIAL_DELETE_FAILED(8006, "资料删除失败"),
    MATERIAL_DOWNLOAD_FAILED(8007, "资料下载失败"),
    CATEGORY_CREATE_FAILED(8008, "分类创建失败"),
    CATEGORY_QUERY_FAILED(8009, "分类查询失败"),
    SUBCATEGORY_CREATE_FAILED(8010, "子分类创建失败"),
    SUBCATEGORY_QUERY_FAILED(8011, "子分类查询失败"),
    
    // 请求参数相关错误 4000-4999
    INVALID_REQUEST_PARAMETER(4001, "请求参数不正确"),
    VALIDATION_FAILED(4002, "数据验证失败"),
    
    // 冲突相关错误 4100-4199
    CONFLICT(4101, "资源冲突"),
    
    // 系统错误 5000-5999
    SYSTEM_ERROR(5000, "系统内部错误"),

    // 任务相关错误 9001-9099
    TASK_NOT_FOUND(9001, "任务不存在"),
    TASK_ASSIGNMENT_NOT_FOUND(9002, "任务指派记录不存在"),
    TASK_ALREADY_DONE(9003, "任务已完成"),
    TASK_NOT_CREATOR(9004, "非任务创建者，无权操作"),
    TASK_EDIT_FIELD_DENIED(9005, "该字段不可修改"),
    REMIND_COOLDOWN_ACTIVE(9010, "催促冷却中"),
    ASSIGNEE_YEAR_MISMATCH(9011, "被指派人届别不匹配"),
    ASSIGNEE_ALREADY_ASSIGNED(9012, "该部员已被指派此任务"),

    // 站内信相关错误 9101-9199
    MESSAGE_NOT_FOUND(9101, "站内信不存在"),
    MESSAGE_NOT_OWNED(9102, "无权操作该站内信"),

    // 角色管理相关错误 9201-9299
    ROLE_INVALID(9201, "非法角色值"),
    ROLE_MINISTER_UNIQUE_VIOLATION(9202, "该届已存在部长"),
    ROLE_DEPUTY_CANNOT_APPOINT_MINISTER(9203, "副部长无权任命部长"),
    ROLE_HISTORY_PARSE_ERROR(9204, "角色历史解析失败"),
    ROLE_SELF_DEMOTE_NO_MINISTER(9205, "同届必须有至少1位部长，无法自我降级"),
    STUDENT_ID_DUPLICATE(9206, "学号已存在"),
    STUDENT_ID_FORMAT_INVALID(9207, "学号格式不正确");
    
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