package com.redmoon2333.exception;

import com.redmoon2333.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 处理静态资源找不到异常（如favicon.ico）
     * 降低日志级别为WARN，减少日志噪音
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleNoResourceFoundException(NoResourceFoundException e) {
        // 对于favicon.ico等常见静态资源，使用WARN级别
        String resourcePath = e.getMessage();
        if (resourcePath != null && (resourcePath.contains("favicon.ico") || 
                                     resourcePath.contains(".css") || 
                                     resourcePath.contains(".js") ||
                                     resourcePath.contains(".png") ||
                                     resourcePath.contains(".jpg"))) {
            logger.warn("静态资源找不到: {}", resourcePath);
        } else {
            logger.error("资源找不到: {}", resourcePath);
        }
        // 不返回JSON响应，让浏览器自然处琄04
    }
    
    /**
     * 处理文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        logger.warn("文件上传大小超限: {}", e.getMessage());
        return ApiResponse.error("文件大小超过限制，请上传小于100MB的文件", ErrorCode.FILE_UPLOAD_ERROR.getCode());
    }
    
    /**
     * 处理JWT相关异常
     */
    @ExceptionHandler(JwtException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleJwtException(JwtException e) {
        logger.warn("JWT异常: 错误码={}, 错误信息={}", e.getErrorCode().getCode(), e.getErrorCode().getMessage());
        return ApiResponse.error(e.getErrorCode().getMessage(), e.getErrorCode().getCode());
    }
    
    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        logger.warn("业务异常: 错误码={}, 错误信息={}", e.getErrorCode().getCode(), e.getErrorCode().getMessage());
        return ApiResponse.error(e.getErrorCode().getMessage(), e.getErrorCode().getCode());
    }
    
    /**
     * 处理Spring Security权限拒绝异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<String> handleAccessDeniedException(AccessDeniedException e) {
        logger.warn("权限拒绝: {}", e.getMessage());
        return ApiResponse.error("权限不足，无法访问资源", ErrorCode.INSUFFICIENT_PERMISSIONS.getCode());
    }
    
    /**
     * 处理参数验证异常（@Validated 验证失败）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Map<String, String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        Map<String, String> errors = new HashMap<>();
        
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        
        String firstErrorMessage = bindingResult.getAllErrors().get(0).getDefaultMessage();
        logger.warn("参数验证失败: {}, 详细错误: {}", firstErrorMessage, errors);
        return ApiResponse.error(firstErrorMessage, ErrorCode.VALIDATION_FAILED.getCode());
    }
    
    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleBindException(BindException e) {
        String errorMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        logger.warn("绑定异常: {}", errorMessage);
        return ApiResponse.error(errorMessage, ErrorCode.VALIDATION_FAILED.getCode());
    }
    
    /**
     * 处理其他运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleRuntimeException(RuntimeException e) {
        logger.error("运行时异常: {}", e.getMessage(), e);
        return ApiResponse.error(e.getMessage(), ErrorCode.SYSTEM_ERROR.getCode());
    }
    
    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.warn("非法参数异常: {}", e.getMessage());
        return ApiResponse.error(e.getMessage(), ErrorCode.INVALID_REQUEST_PARAMETER.getCode());
    }
    
    /**
     * 处理数字格式异常
     */
    @ExceptionHandler(NumberFormatException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleNumberFormatException(NumberFormatException e) {
        logger.warn("数字格式异常: {}", e.getMessage());
        return ApiResponse.error("参数格式错误", ErrorCode.INVALID_REQUEST_PARAMETER.getCode());
    }
    
    /**
     * 处理其他未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleException(Exception e) {
        logger.error("未捕获的异常: {}", e.getMessage(), e);
        return ApiResponse.error("系统内部错误", ErrorCode.SYSTEM_ERROR.getCode());
    }
}