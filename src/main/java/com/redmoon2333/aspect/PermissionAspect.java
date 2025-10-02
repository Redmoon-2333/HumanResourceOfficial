package com.redmoon2333.aspect;

import com.redmoon2333.annotation.RequireMemberRole;
import com.redmoon2333.annotation.RequireMinisterRole;
import com.redmoon2333.util.PermissionUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 权限验证切面
 */
@Aspect
@Component
public class PermissionAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(PermissionAspect.class);
    
    @Autowired
    private PermissionUtil permissionUtil;
    
    /**
     * 部长权限验证切点
     */
    @Before("@annotation(requireMinisterRole)")
    public void checkMinisterPermission(JoinPoint joinPoint, RequireMinisterRole requireMinisterRole) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        logger.info("权限验证开始 - {}#{}, 要求: {}", className, methodName, requireMinisterRole.value());
        
        // 执行权限检查
        permissionUtil.checkMinisterPermission();
        
        logger.info("权限验证通过 - {}#{}", className, methodName);
    }
    
    /**
     * 部员权限验证切点
     */
    @Before("@annotation(requireMemberRole)")
    public void checkMemberPermission(JoinPoint joinPoint, RequireMemberRole requireMemberRole) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        logger.info("权限验证开始 - {}#{}, 要求: {}", className, methodName, requireMemberRole.value());
        
        // 执行权限检查
        permissionUtil.checkMemberPermission();
        
        logger.info("权限验证通过 - {}#{}", className, methodName);
    }
}