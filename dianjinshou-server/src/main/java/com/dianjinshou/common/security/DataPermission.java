package com.dianjinshou.common.security;

import java.lang.annotation.*;

/**
 * 数据权限注解 — 标注在 Service 方法上
 * 表示该方法的数据操作需要 org_id 隔离校验
 *
 * super_admin 角色跳过隔离检查
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermission {

    /**
     * 标识参数中 orgId 的位置（参数名）
     * 如果为空，则从 SecurityContext 中取 orgId 自动注入
     */
    String orgIdParam() default "";
}
