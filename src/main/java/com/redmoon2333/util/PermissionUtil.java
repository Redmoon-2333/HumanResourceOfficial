package com.redmoon2333.util;

import com.redmoon2333.exception.BusinessException;
import com.redmoon2333.exception.ErrorCode;
import com.redmoon2333.exception.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 权限验证工具类
 */
@Component
public class PermissionUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(PermissionUtil.class);
    
    /**
     * 检查用户是否为部长或副部长
     * @throws BusinessException 如果权限不足
     */
    public void checkMinisterPermission() {
        HttpServletRequest request = getCurrentRequest();
        String roleHistory = (String) request.getAttribute("roleHistory");
        String username = (String) request.getAttribute("username");
        
        logger.debug("检查用户 {} 的部长权限，身份历史: {}", username, roleHistory);
        
        if (roleHistory == null || roleHistory.trim().isEmpty()) {
            logger.warn("用户 {} 没有身份信息，拒绝访问", username);
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACTIVITY_OPERATION);
        }
        
        // 检查是否包含部长或副部长身份
        boolean hasMinisterRole = hasMinisterRole(roleHistory);
        
        if (!hasMinisterRole) {
            logger.warn("用户 {} 权限不足，当前身份: {}，尝试执行需要部长权限的操作", username, roleHistory);
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACTIVITY_OPERATION);
        }
        
        logger.info("用户 {} 权限验证通过，身份: {}", username, roleHistory);
    }
    
    /**
     * 检查身份历史中是否包含部长或副部长身份
     * @param roleHistory 身份历史
     * @return 是否具有部长权限
     */
    private boolean hasMinisterRole(String roleHistory) {
        if (roleHistory == null || roleHistory.trim().isEmpty()) {
            return false;
        }
        
        String[] roles = roleHistory.split("&");
        for (String role : roles) {
            role = role.trim();
            if (role.contains("部长") || role.contains("副部长")) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取当前请求
     * @return HttpServletRequest
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            logger.error("无法获取当前请求上下文");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return attributes.getRequest();
    }
    
    /**
     * 获取当前用户ID
     * @return 用户ID
     */
    public Integer getCurrentUserId() {
        HttpServletRequest request = getCurrentRequest();
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            logger.error("无法获取当前用户ID，可能未携带有效的JWT令牌");
            throw new JwtException(ErrorCode.TOKEN_MISSING);
        }
        return userId;
    }
    
    /**
     * 获取当前用户名
     * @return 用户名
     */
    public String getCurrentUsername() {
        HttpServletRequest request = getCurrentRequest();
        String username = (String) request.getAttribute("username");
        if (username == null) {
            logger.error("无法获取当前用户名，可能未携带有效的JWT令牌");
            throw new JwtException(ErrorCode.TOKEN_MISSING);
        }
        return username;
    }
    
    /**
     * 获取当前用户身份历史
     * @return 身份历史
     */
    public String getCurrentUserRoleHistory() {
        HttpServletRequest request = getCurrentRequest();
        return (String) request.getAttribute("roleHistory");
    }
}