package com.redmoon2333.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 需要部长权限的注解
 * 标记在需要部长或副部长权限才能访问的方法上
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireMinisterRole {
    /**
     * 权限描述
     */
    String value() default "需要部长或副部长权限";
}