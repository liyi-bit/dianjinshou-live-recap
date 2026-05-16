package com.dianjinshou.common.security;

import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 数据权限切面 — 强制校验 org_id 团队隔离
 *
 * 规则：
 * 1. super_admin 角色跳过隔离检查
 * 2. 其他角色必须有 orgId，且请求中的 orgId 必须与当前用户一致
 * 3. 如果注解指定了 orgIdParam，从方法参数中取值校验
 *    否则只检查当前用户是否有 orgId
 */
@Aspect
@Component
@Order(2)
public class DataPermissionAspect {

    @Before("@annotation(com.dianjinshou.common.security.DataPermission)")
    public void checkDataPermission(JoinPoint joinPoint) {
        SecurityUser currentUser = SecurityContextHelper.currentUser();
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // super_admin bypasses data isolation
        if ("super_admin".equals(currentUser.getRole())) {
            return;
        }
        // 后台管理员也跳过业务数据隔离
        if (currentUser.getRole() != null && currentUser.getRole().startsWith("admin_")) {
            return;
        }

        Long currentOrgId = currentUser.getOrgId();
        if (currentOrgId == null) {
            throw new BusinessException(ErrorCode.CROSS_ORG_ACCESS, "用户未关联组织");
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DataPermission annotation = method.getAnnotation(DataPermission.class);

        String orgIdParam = annotation.orgIdParam();
        if (orgIdParam.isEmpty()) {
            return;
        }

        Long requestOrgId = extractOrgId(joinPoint, signature, orgIdParam);
        if (requestOrgId != null && !requestOrgId.equals(currentOrgId)) {
            throw new BusinessException(ErrorCode.CROSS_ORG_ACCESS, "无权访问其他组织数据");
        }
    }

    private Long extractOrgId(JoinPoint joinPoint, MethodSignature signature, String paramName) {
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getName().equals(paramName)) {
                Object val = args[i];
                if (val instanceof Long) {
                    return (Long) val;
                }
                if (val instanceof Number) {
                    return ((Number) val).longValue();
                }
            }
        }

        // Fallback: check by parameter names from signature
        String[] paramNames = signature.getParameterNames();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                if (paramNames[i].equals(paramName)) {
                    Object val = args[i];
                    if (val instanceof Long) {
                        return (Long) val;
                    }
                    if (val instanceof Number) {
                        return ((Number) val).longValue();
                    }
                }
            }
        }

        return null;
    }
}
