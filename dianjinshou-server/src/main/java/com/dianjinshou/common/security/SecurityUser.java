package com.dianjinshou.common.security;

/**
 * 从 JWT 中解析出的当前登录用户信息，放入 SecurityContext
 */
public class SecurityUser {

    private final Long userId;
    private final String role;
    private final Long orgId;

    public SecurityUser(Long userId, String role, Long orgId) {
        this.userId = userId;
        this.role = role;
        this.orgId = orgId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public Long getOrgId() {
        return orgId;
    }
}
