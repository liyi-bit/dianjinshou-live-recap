package com.dianjinshou.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 静态工具 — 从 SecurityContext 获取当前登录用户
 */
public final class SecurityContextHelper {

    private SecurityContextHelper() {}

    public static SecurityUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SecurityUser) {
            return (SecurityUser) auth.getPrincipal();
        }
        return null;
    }

    public static Long currentUserId() {
        SecurityUser user = currentUser();
        return user != null ? user.getUserId() : null;
    }

    public static Long currentOrgId() {
        SecurityUser user = currentUser();
        return user != null ? user.getOrgId() : null;
    }

    public static String currentRole() {
        SecurityUser user = currentUser();
        return user != null ? user.getRole() : null;
    }
}
