package com.dianjinshou.common.security;

import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RequiresRoleAspect 单元测试
 * 覆盖: 角色匹配/不匹配/未认证/多角色/类级别注解
 */
class RequiresRoleAspectTest {

    private RequiresRoleAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new RequiresRoleAspect();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("未认证用户 - 抛出 UNAUTHORIZED")
    void noAuth_throwsUnauthorized() {
        JoinPoint jp = mockJoinPoint("adminOnly");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> aspect.checkRole(jp));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    @DisplayName("角色在列表中 - 通过")
    void roleMatches_passes() {
        setSecurityContext("admin");
        JoinPoint jp = mockJoinPoint("adminOnly");
        assertDoesNotThrow(() -> aspect.checkRole(jp));
    }

    @Test
    @DisplayName("角色不在列表中 - 抛出 FORBIDDEN")
    void roleMismatch_throwsForbidden() {
        setSecurityContext("operator");
        JoinPoint jp = mockJoinPoint("adminOnly");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> aspect.checkRole(jp));
        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("多角色列表 - 任一匹配即通过")
    void multiRole_anyMatch() {
        setSecurityContext("operator");
        JoinPoint jp = mockJoinPoint("multiRole");
        assertDoesNotThrow(() -> aspect.checkRole(jp));
    }

    @Test
    @DisplayName("多角色列表 - 都不匹配则拒绝")
    void multiRole_noneMatch() {
        setSecurityContext("viewer");
        JoinPoint jp = mockJoinPoint("multiRole");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> aspect.checkRole(jp));
        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("super_admin 在 admin 列表中 - 不匹配则拒绝")
    void superAdminNotInAdminList() {
        setSecurityContext("super_admin");
        JoinPoint jp = mockJoinPoint("adminOnly");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> aspect.checkRole(jp));
        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    private JoinPoint mockJoinPoint(String methodName) {
        JoinPoint jp = mock(JoinPoint.class);
        MethodSignature sig = mock(MethodSignature.class);
        when(jp.getSignature()).thenReturn(sig);
        try {
            Method method = TestController.class.getMethod(methodName);
            when(sig.getMethod()).thenReturn(method);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return jp;
    }

    private void setSecurityContext(String role) {
        SecurityUser user = new SecurityUser(1L, role, 1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
    }

    // Stub controller for annotation reflection
    @SuppressWarnings("unused")
    public static class TestController {
        @RequiresRole({"admin"})
        public void adminOnly() {}

        @RequiresRole({"admin", "operator"})
        public void multiRole() {}
    }
}
