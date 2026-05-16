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
import java.util.Arrays;

@Aspect
@Component
@Order(1)
public class RequiresRoleAspect {

    @Before("@annotation(com.dianjinshou.common.security.RequiresRole) || @within(com.dianjinshou.common.security.RequiresRole)")
    public void checkRole(JoinPoint joinPoint) {
        SecurityUser user = SecurityContextHelper.currentUser();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        RequiresRole annotation = getAnnotation(joinPoint);
        if (annotation == null) {
            return;
        }

        String currentRole = user.getRole();
        boolean allowed = Arrays.asList(annotation.value()).contains(currentRole);
        if (!allowed) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private RequiresRole getAnnotation(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        RequiresRole annotation = method.getAnnotation(RequiresRole.class);
        if (annotation != null) {
            return annotation;
        }

        return method.getDeclaringClass().getAnnotation(RequiresRole.class);
    }
}
