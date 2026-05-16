package com.dianjinshou.common.security;

import com.dianjinshou.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-for-dianjinshou-must-be-at-least-256-bits-long-enough");
        props.setAccessTokenTtl(7200);
        props.setRefreshTokenTtl(604800);
        jwtUtil = new JwtUtil(props);
    }

    @Test
    void generateAccessToken_and_parse() {
        String token = jwtUtil.generateAccessToken(42L, "admin", 5L);
        assertNotNull(token);

        Claims claims = jwtUtil.parseToken(token);
        assertTrue(jwtUtil.isAccessToken(claims));
        assertFalse(jwtUtil.isRefreshToken(claims));
        assertEquals(42L, jwtUtil.getUserId(claims));
        assertEquals("admin", jwtUtil.getRole(claims));
        assertEquals(5L, jwtUtil.getOrgId(claims));
    }

    @Test
    void generateRefreshToken_and_parse() {
        String token = jwtUtil.generateRefreshToken(42L);
        Claims claims = jwtUtil.parseToken(token);

        assertTrue(jwtUtil.isRefreshToken(claims));
        assertFalse(jwtUtil.isAccessToken(claims));
        assertEquals(42L, jwtUtil.getUserId(claims));
        assertNull(jwtUtil.getRole(claims));
    }

    @Test
    void accessToken_nullOrgId_isHandled() {
        String token = jwtUtil.generateAccessToken(1L, "super_admin", null);
        Claims claims = jwtUtil.parseToken(token);
        assertNull(jwtUtil.getOrgId(claims));
    }

    @Test
    void expiredToken_throwsExpiredJwtException() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-for-dianjinshou-must-be-at-least-256-bits-long-enough");
        props.setAccessTokenTtl(0); // 0 seconds = immediately expired
        props.setRefreshTokenTtl(0);
        JwtUtil expiredJwtUtil = new JwtUtil(props);

        String token = expiredJwtUtil.generateAccessToken(1L, "admin", 1L);
        assertThrows(ExpiredJwtException.class, () -> expiredJwtUtil.parseToken(token));
    }

    @Test
    void invalidToken_throwsException() {
        assertThrows(Exception.class, () -> jwtUtil.parseToken("invalid.token.here"));
    }

    @Test
    void getAccessTokenTtlSeconds_returnsConfigured() {
        assertEquals(7200, jwtUtil.getAccessTokenTtlSeconds());
    }

    @Test
    void differentSecrets_cannotParseEachOther() {
        JwtProperties otherProps = new JwtProperties();
        otherProps.setSecret("another-secret-key-for-testing-different-from-above-must-be-long!!");
        otherProps.setAccessTokenTtl(7200);
        otherProps.setRefreshTokenTtl(604800);
        JwtUtil otherJwtUtil = new JwtUtil(otherProps);

        String token = jwtUtil.generateAccessToken(1L, "admin", 1L);
        assertThrows(Exception.class, () -> otherJwtUtil.parseToken(token));
    }

    // ========== QA 增量: 边界测试 ==========

    @Test
    void refreshToken_noRoleOrOrgId() {
        String token = jwtUtil.generateRefreshToken(1L);
        Claims claims = jwtUtil.parseToken(token);
        assertNull(jwtUtil.getRole(claims));
        assertNull(jwtUtil.getOrgId(claims));
    }

    @Test
    void expiredRefreshToken_throws() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-for-dianjinshou-must-be-at-least-256-bits-long-enough");
        props.setAccessTokenTtl(7200);
        props.setRefreshTokenTtl(0);
        JwtUtil expJwtUtil = new JwtUtil(props);

        String token = expJwtUtil.generateRefreshToken(1L);
        assertThrows(ExpiredJwtException.class, () -> expJwtUtil.parseToken(token));
    }

    @Test
    void emptyToken_throwsException() {
        assertThrows(Exception.class, () -> jwtUtil.parseToken(""));
    }

    @Test
    void accessTokenNotRefresh() {
        String token = jwtUtil.generateAccessToken(1L, "admin", 1L);
        Claims claims = jwtUtil.parseToken(token);
        assertTrue(jwtUtil.isAccessToken(claims));
        assertFalse(jwtUtil.isRefreshToken(claims));
    }

    @Test
    void refreshTokenNotAccess() {
        String token = jwtUtil.generateRefreshToken(1L);
        Claims claims = jwtUtil.parseToken(token);
        assertTrue(jwtUtil.isRefreshToken(claims));
        assertFalse(jwtUtil.isAccessToken(claims));
    }
}
