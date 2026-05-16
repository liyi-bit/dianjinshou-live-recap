package com.dianjinshou.common.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class SecurityContextHelperTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void currentUser_returnsNull_whenNoAuth() {
        assertNull(SecurityContextHelper.currentUser());
        assertNull(SecurityContextHelper.currentUserId());
        assertNull(SecurityContextHelper.currentOrgId());
        assertNull(SecurityContextHelper.currentRole());
    }

    @Test
    void currentUser_returnsSecurityUser_whenAuthenticated() {
        SecurityUser user = new SecurityUser(42L, "admin", 5L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));

        assertEquals(user, SecurityContextHelper.currentUser());
        assertEquals(42L, SecurityContextHelper.currentUserId());
        assertEquals(5L, SecurityContextHelper.currentOrgId());
        assertEquals("admin", SecurityContextHelper.currentRole());
    }

    // ========== QA 增量: 边界测试 ==========

    @Test
    void authenticated_orgId() {
        SecurityUser user = new SecurityUser(1L, "operator", 99L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
        assertEquals(99L, SecurityContextHelper.currentOrgId());
    }

    @Test
    void authenticated_superAdmin_nullOrgId() {
        SecurityUser user = new SecurityUser(1L, "super_admin", null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
        assertEquals("super_admin", SecurityContextHelper.currentRole());
    }

    @Test
    void authenticated_currentUser_object() {
        SecurityUser user = new SecurityUser(10L, "viewer", 3L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
        SecurityUser returned = SecurityContextHelper.currentUser();
        assertNotNull(returned);
        assertEquals(10L, returned.getUserId());
        assertEquals("viewer", returned.getRole());
        assertEquals(3L, returned.getOrgId());
    }

    @Test
    void nonSecurityUserPrincipal_returnsNull() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("stringPrincipal", null, Collections.emptyList()));
        assertNull(SecurityContextHelper.currentUser());
        assertNull(SecurityContextHelper.currentUserId());
    }
}
