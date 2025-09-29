package com.redmoon2333.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class RoleHistoryValidator implements ConstraintValidator<ValidRoleHistory, String> {
    
    // 匹配类似"2023级部员&2024级部长"的格式
    // 每个角色项格式为：四位年份 + "级" + 角色名称
    // 多个角色项之间用"&"分隔
    private static final String ROLE_HISTORY_PATTERN = "^(\\d{4}级[\\u4e00-\\u9fa5]+)(&(\\d{4}级[\\u4e00-\\u9fa5]+))*$";
    
    private static final Pattern pattern = Pattern.compile(ROLE_HISTORY_PATTERN);
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // 允许为空
        if (value == null || value.isEmpty()) {
            return true;
        }
        
        // 检查格式是否匹配
        if (!pattern.matcher(value).matches()) {
            return false;
        }
        
        return true;
    }
}