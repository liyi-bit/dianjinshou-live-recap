package com.dianjinshou.common.security;

import com.dianjinshou.config.JwtProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthFilterTest {

    private JwtUtil jwtUtil;
    private JwtAuthFilter filter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-for-dianjinshou-must-be-at-least-256-bits-long-enough");
        props.setAccessTokenTtl(7200);
        props.setRefreshTokenTtl(604800);
        jwtUtil = new JwtUtil(props);
        objectMapper = new ObjectMapper();
        filter = new JwtAuthFilter(jwtUtil, objectMapper);
        SecurityContextHolder.clearContext();
    }

    @Test
    void noAuthHeader_continuesChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(200, response.getStatus());
    }

    @Test
    void validAccessToken_setsSecurityContext() throws Exception {
        String token = jwtUtil.generateAccessToken(42L, "admin", 5L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        SecurityUser user = (SecurityUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        assertEquals(42L, user.getUserId());
        assertEquals("admin", user.getRole());
        assertEquals(5L, user.getOrgId());
    }

    @Test
    void refreshToken_returns401() throws Exception {
        String token = jwtUtil.generateRefreshToken(42L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("40100"));
    }

    @Test
    void expiredToken_returns401WithTokenExpired() throws Exception {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-for-dianjinshou-must-be-at-least-256-bits-long-enough");
        props.setAccessTokenTtl(0);
        props.setRefreshTokenTtl(0);
        JwtUtil expiredJwtUtil = new JwtUtil(props);
        String token = expiredJwtUtil.generateAccessToken(1L, "admin", 1L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("40101"));
    }

    @Test
    void invalidToken_returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid.token.here");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("40100"));
    }

    @Test
    void nonBearerHeader_continuesChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(200, response.getStatus());
    }

    // ========== QA 增量: 边界测试 ==========

    @Test
    void optionsRequest_continuesChain() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("OPTIONS", "/api/v1/test");
        req.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        filter.doFilterInternal(req, resp, new MockFilterChain());

        assertEquals(200, resp.getStatus());
    }

    @Test
    void validAccessToken_hasRoleAuthority() throws Exception {
        String token = jwtUtil.generateAccessToken(1L, "operator", 10L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertTrue(SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_OPERATOR")));
    }

    @Test
    void errorResponse_isJson() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertTrue(response.getContentType().startsWith("application/json"),
                "Content-Type should start with application/json, actual: " + response.getContentType());
    }
}
