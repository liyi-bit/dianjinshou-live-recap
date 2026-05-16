package com.dianjinshou.common.security;

import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitAspectTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ProceedingJoinPoint joinPoint;

    private RateLimitAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new RateLimitAspect(redisTemplate);
        SecurityUser user = new SecurityUser(1L, "operator", 5L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsRequestWhenUnderLimit() throws Throwable {
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), anyString(), anyString()))
                .thenReturn(1L);
        when(joinPoint.proceed()).thenReturn("result");

        RateLimit rateLimit = createRateLimit(100, 60, "test");
        Object result = aspect.around(joinPoint, rateLimit);

        assertEquals("result", result);
        verify(joinPoint).proceed();
    }

    @Test
    void rejectsRequestWhenOverLimit() throws Throwable {
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), anyString(), anyString()))
                .thenReturn(0L);

        RateLimit rateLimit = createRateLimit(10, 60, "test");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> aspect.around(joinPoint, rateLimit));
        assertEquals(ErrorCode.TOO_MANY_REQUESTS.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    void skipsRateLimitForUnauthenticatedRequests() throws Throwable {
        SecurityContextHolder.clearContext();
        when(joinPoint.proceed()).thenReturn("result");

        RateLimit rateLimit = createRateLimit(10, 60, "test");
        Object result = aspect.around(joinPoint, rateLimit);

        assertEquals("result", result);
        verify(redisTemplate, never()).execute(any(RedisScript.class), any(List.class), anyString(), anyString());
    }

    // ========== QA 增量: 边界测试 ==========

    @Test
    void nullResponse_throwsTooManyRequests() throws Throwable {
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), anyString(), anyString()))
                .thenReturn(null);

        assertThrows(BusinessException.class,
                () -> aspect.around(joinPoint, createRateLimit(10, 60, "test")));
    }

    @Test
    void emptyKey_usesMethodSignature() throws Throwable {
        MethodSignature sig = mock(MethodSignature.class);
        Method method = getClass().getMethod("dummyMethod");
        when(sig.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(sig);
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), anyString(), anyString()))
                .thenReturn(1L);
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.around(joinPoint, createRateLimit(10, 60, ""));

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<String>> keysCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(redisTemplate).execute(any(RedisScript.class), keysCaptor.capture(), anyString(), anyString());

        String redisKey = keysCaptor.getValue().get(0);
        assertTrue(redisKey.startsWith("rate_limit:"));
        assertTrue(redisKey.contains("RateLimitAspectTest:dummyMethod"));
    }

    @SuppressWarnings("unused")
    public void dummyMethod() {}

    private RateLimit createRateLimit(int max, int windowSeconds, String key) {
        return new RateLimit() {
            @Override public int max() { return max; }
            @Override public int windowSeconds() { return windowSeconds; }
            @Override public String key() { return key; }
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return RateLimit.class;
            }
        };
    }
}
