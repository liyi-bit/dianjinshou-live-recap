package com.dianjinshou.common.security;

import java.lang.annotation.*;

/**
 * RBAC 角色权限注解 — 标注在 Controller 方法或类上
 * 表示当前用户必须拥有指定角色之一才可访问
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresRole {
    String[] value();
}
