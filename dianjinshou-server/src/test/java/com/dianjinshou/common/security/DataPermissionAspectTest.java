package com.dianjinshou.common.security;

import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataPermissionAspectTest {

    private DataPermissionAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new DataPermissionAspect();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noAuth_throwsUnauthorized() {
        JoinPoint jp = mockJoinPoint("methodWithDataPermission", "");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> aspect.checkDataPermission(jp));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    void superAdmin_bypasses() {
        setSecurityContext(1L, "super_admin", null);
        JoinPoint jp = mockJoinPoint("methodWithDataPermission", "");
        assertDoesNotThrow(() -> aspect.checkDataPermission(jp));
    }

    @Test
    void userWithoutOrg_throwsCrossOrgAccess() {
        setSecurityContext(1L, "admin", null);
        JoinPoint jp = mockJoinPoint("methodWithDataPermission", "");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> aspect.checkDataPermission(jp));
        assertEquals(ErrorCode.CROSS_ORG_ACCESS, ex.getErrorCode());
    }

    @Test
    void userWithOrg_noParamCheck_passes() {
        setSecurityContext(1L, "admin", 5L);
        JoinPoint jp = mockJoinPoint("methodWithDataPermission", "");
        assertDoesNotThrow(() -> aspect.checkDataPermission(jp));
    }

    @Test
    void matchingOrgId_passes() {
        setSecurityContext(1L, "admin", 5L);
        JoinPoint jp = mockJoinPointWithParam("methodWithOrgIdParam", "orgId", 5L);
        assertDoesNotThrow(() -> aspect.checkDataPermission(jp));
    }

    @Test
    void mismatchingOrgId_throwsCrossOrgAccess() {
        setSecurityContext(1L, "admin", 5L);
        JoinPoint jp = mockJoinPointWithParam("methodWithOrgIdParam", "orgId", 99L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> aspect.checkDataPermission(jp));
        assertEquals(ErrorCode.CROSS_ORG_ACCESS, ex.getErrorCode());
    }

    // ========== QA 增量: 边界测试 ==========

    @Test
    void superAdmin_withOrgId_bypasses() {
        setSecurityContext(1L, "super_admin", 5L);
        JoinPoint jp = mockJoinPoint("methodWithDataPermission", "");
        assertDoesNotThrow(() -> aspect.checkDataPermission(jp));
    }

    @Test
    void superAdmin_crossOrg_bypasses() {
        setSecurityContext(1L, "super_admin", 5L);
        JoinPoint jp = mockJoinPointWithParam("methodWithOrgIdParam", "orgId", 99L);
        assertDoesNotThrow(() -> aspect.checkDataPermission(jp));
    }

    @Test
    void operatorNoOrg_throwsCrossOrgAccess() {
        setSecurityContext(2L, "operator", null);
        JoinPoint jp = mockJoinPoint("methodWithDataPermission", "");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> aspect.checkDataPermission(jp));
        assertEquals(ErrorCode.CROSS_ORG_ACCESS, ex.getErrorCode());
    }

    @Test
    void nullRequestOrgId_passes() {
        setSecurityContext(1L, "admin", 5L);
        JoinPoint jp = mockJoinPointWithParam("methodWithOrgIdParam", "orgId", null);
        assertDoesNotThrow(() -> aspect.checkDataPermission(jp));
    }

    private JoinPoint mockJoinPoint(String methodName, String orgIdParam) {
        JoinPoint jp = mock(JoinPoint.class);
        MethodSignature sig = mock(MethodSignature.class);
        when(jp.getSignature()).thenReturn(sig);

        try {
            Method method = TestService.class.getMethod(methodName);
            when(sig.getMethod()).thenReturn(method);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        when(sig.getParameterNames()).thenReturn(new String[]{});
        when(jp.getArgs()).thenReturn(new Object[]{});
        return jp;
    }

    private JoinPoint mockJoinPointWithParam(String methodName, String paramName, Long value) {
        JoinPoint jp = mock(JoinPoint.class);
        MethodSignature sig = mock(MethodSignature.class);
        when(jp.getSignature()).thenReturn(sig);

        try {
            Method method = TestService.class.getMethod(methodName, Long.class);
            when(sig.getMethod()).thenReturn(method);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        when(sig.getParameterNames()).thenReturn(new String[]{paramName});
        when(jp.getArgs()).thenReturn(new Object[]{value});
        return jp;
    }

    private void setSecurityContext(Long userId, String role, Long orgId) {
        SecurityUser user = new SecurityUser(userId, role, orgId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
    }

    // Test service stubs for reflection
    @SuppressWarnings("unused")
    public static class TestService {
        @DataPermission
        public void methodWithDataPermission() {}

        @DataPermission(orgIdParam = "orgId")
        public void methodWithOrgIdParam(Long orgId) {}
    }
}
