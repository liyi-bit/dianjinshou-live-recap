package com.dianjinshou.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogSanitizerTest {

    @Test
    void sanitizesPhoneNumber() {
        String input = "用户 13812345678 登录";
        String result = LogSanitizer.sanitize(input);
        assertEquals("用户 138****5678 登录", result);
    }

    @Test
    void sanitizesJwtToken() {
        String input = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwiZXhwIjoxNjk5OTk5OTk5fQ.signature";
        String result = LogSanitizer.sanitize(input);
        assertTrue(result.contains("***[REDACTED]"));
        assertFalse(result.contains("eyJzdWIiOiIxIiwiZXhwIjoxNjk5OTk5OTk5fQ"));
    }

    @Test
    void sanitizesPasswordInJson() {
        String input = "{\"phone\":\"13812345678\",\"password\":\"secret123\"}";
        String result = LogSanitizer.sanitize(input);
        assertTrue(result.contains("***[REDACTED]"));
        assertFalse(result.contains("secret123"));
    }

    @Test
    void sanitizesPasswordHashInJson() {
        String input = "{\"passwordHash\":\"$2a$10$abcdef\"}";
        String result = LogSanitizer.sanitize(input);
        assertTrue(result.contains("***[REDACTED]"));
        assertFalse(result.contains("$2a$10$abcdef"));
    }

    @Test
    void handlesNullInput() {
        assertNull(LogSanitizer.sanitize(null));
    }

    @Test
    void preservesNonSensitiveData() {
        String input = "User created organization TestOrg";
        assertEquals(input, LogSanitizer.sanitize(input));
    }
}
