package com.redmoon2333.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = RoleHistoryValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRoleHistory {
    String message() default "角色历史格式不正确，应为类似'2023级部员&2024级部长'的格式";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}