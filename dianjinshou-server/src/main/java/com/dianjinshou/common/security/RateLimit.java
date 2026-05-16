package com.dianjinshou.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * Maximum requests allowed in the time window.
     */
    int max() default 100;

    /**
     * Time window in seconds.
     */
    int windowSeconds() default 60;

    /**
     * Rate limit key prefix (defaults to method name).
     */
    String key() default "";
}
