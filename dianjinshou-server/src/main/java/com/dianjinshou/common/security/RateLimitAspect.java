package com.dianjinshou.common.security;

import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

@Aspect
@Component
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    private static final String LUA_SCRIPT =
            "local key = KEYS[1] " +
            "local max = tonumber(ARGV[1]) " +
            "local window = tonumber(ARGV[2]) " +
            "local current = tonumber(redis.call('GET', key) or '0') " +
            "if current >= max then " +
            "  return 0 " +
            "end " +
            "current = redis.call('INCR', key) " +
            "if current == 1 then " +
            "  redis.call('EXPIRE', key, window) " +
            "end " +
            "return 1";

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> redisScript;

    public RateLimitAspect(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.redisScript = new DefaultRedisScript<>();
        this.redisScript.setScriptText(LUA_SCRIPT);
        this.redisScript.setResultType(Long.class);
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        Long userId = SecurityContextHelper.currentUserId();
        if (userId == null) {
            return joinPoint.proceed();
        }

        String keyPrefix = rateLimit.key();
        if (keyPrefix.isEmpty()) {
            MethodSignature sig = (MethodSignature) joinPoint.getSignature();
            Method method = sig.getMethod();
            keyPrefix = method.getDeclaringClass().getSimpleName() + ":" + method.getName();
        }

        String redisKey = "rate_limit:" + keyPrefix + ":" + userId;
        List<String> keys = Collections.singletonList(redisKey);

        try {
            Long allowed = redisTemplate.execute(redisScript, keys,
                    String.valueOf(rateLimit.max()),
                    String.valueOf(rateLimit.windowSeconds()));

            if (allowed == null || allowed == 0) {
                throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis unavailable, skipping rate limit check: {}", e.getMessage());
        }

        return joinPoint.proceed();
    }
}
