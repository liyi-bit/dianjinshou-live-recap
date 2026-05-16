package com.dianjinshou.modules.auth;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.security.JwtUtil;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.auth.dto.LoginRequest;
import com.dianjinshou.modules.auth.dto.RefreshTokenRequest;
import com.dianjinshou.modules.auth.dto.RegisterRequest;
import com.dianjinshou.modules.auth.entity.User;
import com.dianjinshou.modules.auth.entity.UserSession;
import com.dianjinshou.modules.auth.mapper.UserMapper;
import com.dianjinshou.modules.auth.mapper.UserSessionMapper;
import com.dianjinshou.modules.auth.service.AuthService;
import com.dianjinshou.modules.auth.vo.LoginVO;
import com.dianjinshou.modules.auth.vo.MeVO;
import com.dianjinshou.modules.auth.vo.TokenVO;
import com.dianjinshou.modules.organization.entity.Organization;
import com.dianjinshou.modules.organization.mapper.OrganizationMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthIntegrationTest {

    @Mock private UserMapper userMapper;
    @Mock private UserSessionMapper userSessionMapper;
    @Mock private OrganizationMapper organizationMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @BeforeAll
    static void initCache() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        TableInfoHelper.initTableInfo(assistant, User.class);
        TableInfoHelper.initTableInfo(assistant, UserSession.class);
        TableInfoHelper.initTableInfo(assistant, Organization.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void fullAuthFlow_register_login_refresh_me() {
        // 1. Register
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(passwordEncoder.encode("pass123")).thenReturn("$2a$10$hashed");
        when(userMapper.insert(any(User.class))).thenReturn(1);
        when(jwtUtil.generateAccessToken(any(), eq("operator"), any())).thenReturn("access_token_1");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh_token_1");
        when(jwtUtil.getAccessTokenTtlSeconds()).thenReturn(3600L);
        when(userSessionMapper.insert(any())).thenReturn(1);

        RegisterRequest regReq = new RegisterRequest();
        regReq.setUsername("testuser");
        regReq.setPhone("13800138000");
        regReq.setPassword("pass123");
        regReq.setCode("123456");

        LoginVO registerResult = authService.register(regReq, "127.0.0.1", "TestAgent");

        assertNotNull(registerResult);
        assertEquals("access_token_1", registerResult.getAccessToken());
        assertEquals("refresh_token_1", registerResult.getRefreshToken());
        assertEquals("testuser", registerResult.getUser().getUsername());

        // 2. Login with same credentials
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("testuser");
        savedUser.setPhone("13800138000");
        savedUser.setPasswordHash("$2a$10$hashed");
        savedUser.setRole("operator");
        savedUser.setOrgId(5L);
        savedUser.setVipLevel(0);
        savedUser.setStatus(1);

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(savedUser);
        when(passwordEncoder.matches("pass123", "$2a$10$hashed")).thenReturn(true);
        when(userMapper.updateById(any())).thenReturn(1);
        when(jwtUtil.generateAccessToken(1L, "operator", 5L)).thenReturn("access_token_2");

        LoginRequest loginReq = new LoginRequest();
        loginReq.setPhone("13800138000");
        loginReq.setPassword("pass123");

        LoginVO loginResult = authService.login(loginReq, "127.0.0.1", "TestAgent");

        assertNotNull(loginResult);
        assertEquals("access_token_2", loginResult.getAccessToken());

        // 3. Refresh token
        Claims refreshClaims = new DefaultClaims();
        refreshClaims.put("type", "refresh");
        refreshClaims.put("userId", 1L);

        when(jwtUtil.parseToken("refresh_token_1")).thenReturn(refreshClaims);
        when(jwtUtil.isRefreshToken(refreshClaims)).thenReturn(true);
        when(jwtUtil.getUserId(refreshClaims)).thenReturn(1L);
        when(userMapper.selectById(1L)).thenReturn(savedUser);
        when(jwtUtil.generateAccessToken(1L, "operator", 5L)).thenReturn("access_token_3");
        when(jwtUtil.generateRefreshToken(1L)).thenReturn("refresh_token_2");

        RefreshTokenRequest refreshReq = new RefreshTokenRequest();
        refreshReq.setRefreshToken("refresh_token_1");

        TokenVO refreshResult = authService.refreshToken(refreshReq);

        assertEquals("access_token_3", refreshResult.getAccessToken());
        assertEquals("refresh_token_2", refreshResult.getRefreshToken());

        // 4. Get current user info
        setSecurityContext(1L, "operator", 5L);
        when(userMapper.selectById(1L)).thenReturn(savedUser);

        Organization org = new Organization();
        org.setId(5L);
        org.setName("TestOrg");
        org.setMaxMembers(20);
        when(organizationMapper.selectById(5L)).thenReturn(org);

        MeVO meResult = authService.me();

        assertEquals("testuser", meResult.getUser().getUsername());
        assertEquals("TestOrg", meResult.getOrg().getName());
        assertEquals(0, meResult.getVipInfo().getLevel());
    }

    @Test
    void register_duplicatePhone_rejected() {
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        RegisterRequest req = new RegisterRequest();
        req.setUsername("dup");
        req.setPhone("13800138000");
        req.setPassword("pass");
        req.setCode("123456");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.register(req, "127.0.0.1", "UA"));
        assertTrue(ex.getMessage().contains("手机号已注册"));
    }

    @Test
    void login_wrongPassword_rejected() {
        User user = new User();
        user.setId(1L);
        user.setPhone("13800138000");
        user.setPasswordHash("$2a$10$hashed");
        user.setStatus(1);

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
        when(passwordEncoder.matches("wrong", "$2a$10$hashed")).thenReturn(false);

        LoginRequest req = new LoginRequest();
        req.setPhone("13800138000");
        req.setPassword("wrong");

        assertThrows(BusinessException.class, () -> authService.login(req, "127.0.0.1", "UA"));
    }

    @Test
    void login_disabledAccount_rejected() {
        User user = new User();
        user.setId(1L);
        user.setPhone("13800138000");
        user.setPasswordHash("$2a$10$hashed");
        user.setStatus(0);

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        LoginRequest req = new LoginRequest();
        req.setPhone("13800138000");
        req.setPassword("pass");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(req, "127.0.0.1", "UA"));
        assertTrue(ex.getMessage().contains("禁用"));
    }

    private void setSecurityContext(Long userId, String role, Long orgId) {
        SecurityUser su = new SecurityUser(userId, role, orgId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(su, null, Collections.emptyList()));
    }
}
