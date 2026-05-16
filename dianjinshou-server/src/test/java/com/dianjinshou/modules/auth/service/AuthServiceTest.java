package com.dianjinshou.modules.auth.service;

import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.JwtUtil;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.config.JwtProperties;
import com.dianjinshou.modules.auth.dto.LoginRequest;
import com.dianjinshou.modules.auth.dto.RefreshTokenRequest;
import com.dianjinshou.modules.auth.dto.RegisterRequest;
import com.dianjinshou.modules.auth.dto.SmsLoginRequest;
import com.dianjinshou.modules.auth.entity.User;
import com.dianjinshou.modules.auth.entity.UserSession;
import com.dianjinshou.modules.auth.mapper.UserMapper;
import com.dianjinshou.modules.auth.mapper.UserSessionMapper;
import com.dianjinshou.modules.auth.vo.LoginVO;
import com.dianjinshou.modules.auth.vo.MeVO;
import com.dianjinshou.modules.auth.vo.TokenVO;
import com.dianjinshou.modules.organization.entity.Organization;
import com.dianjinshou.modules.organization.mapper.OrganizationMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private static final String JWT_SECRET =
            "test-secret-key-for-dianjinshou-must-be-at-least-256-bits-long-enough";

    private UserMapper userMapper;
    private UserSessionMapper userSessionMapper;
    private OrganizationMapper organizationMapper;
    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        userSessionMapper = mock(UserSessionMapper.class);
        organizationMapper = mock(OrganizationMapper.class);
        passwordEncoder = new BCryptPasswordEncoder(10);

        JwtProperties props = new JwtProperties();
        props.setSecret(JWT_SECRET);
        props.setAccessTokenTtl(7200);
        props.setRefreshTokenTtl(604800);
        jwtUtil = new JwtUtil(props);

        authService = new AuthService(userMapper, userSessionMapper, organizationMapper,
                passwordEncoder, jwtUtil);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void login_success() {
        User user = buildUser("13800138000", passwordEncoder.encode("Test1234"));
        when(userMapper.selectOne(any())).thenReturn(user);
        when(userMapper.updateById(any())).thenReturn(1);
        when(userSessionMapper.insert(any())).thenReturn(1);

        LoginRequest request = new LoginRequest();
        request.setPhone("13800138000");
        request.setPassword("Test1234");

        LoginVO vo = authService.login(request, "127.0.0.1", "TestAgent");

        assertNotNull(vo.getAccessToken());
        assertNotNull(vo.getRefreshToken());
        assertEquals(7200, vo.getExpiresIn());
        assertEquals("138****8000", vo.getUser().getPhone());
        assertEquals("admin", vo.getUser().getRole());
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        User user = buildUser("13800138000", passwordEncoder.encode("Test1234"));
        when(userMapper.selectOne(any())).thenReturn(user);

        LoginRequest request = new LoginRequest();
        request.setPhone("13800138000");
        request.setPassword("WrongPass1");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(request, "127.0.0.1", "TestAgent"));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    void login_userNotFound_throwsUnauthorized() {
        when(userMapper.selectOne(any())).thenReturn(null);

        LoginRequest request = new LoginRequest();
        request.setPhone("13800138000");
        request.setPassword("Test1234");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(request, "127.0.0.1", "TestAgent"));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    void login_disabledUser_throwsForbidden() {
        User user = buildUser("13800138000", passwordEncoder.encode("Test1234"));
        user.setStatus(0);
        when(userMapper.selectOne(any())).thenReturn(user);

        LoginRequest request = new LoginRequest();
        request.setPhone("13800138000");
        request.setPassword("Test1234");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(request, "127.0.0.1", "TestAgent"));
        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    @Test
    void register_success() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(100L);
            return 1;
        });
        when(userSessionMapper.insert(any())).thenReturn(1);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPhone("13900139000");
        request.setPassword("Test1234");
        request.setCode("1234");

        LoginVO vo = authService.register(request, "127.0.0.1", "TestAgent");

        assertNotNull(vo.getAccessToken());
        assertEquals("139****9000", vo.getUser().getPhone());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User saved = captor.getValue();
        assertEquals("testuser", saved.getUsername());
        assertEquals("operator", saved.getRole());
        assertTrue(passwordEncoder.matches("Test1234", saved.getPasswordHash()));
    }

    @Test
    void register_duplicatePhone_throwsConflict() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPhone("13800138000");
        request.setPassword("Test1234");
        request.setCode("1234");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.register(request, "127.0.0.1", "TestAgent"));
        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
    }

    @Test
    void refreshToken_success() {
        User user = buildUser("13800138000", "hash");
        when(userMapper.selectById(42L)).thenReturn(user);

        String refreshToken = jwtUtil.generateRefreshToken(42L);
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        TokenVO vo = authService.refreshToken(request);

        assertNotNull(vo.getAccessToken());
        assertNotNull(vo.getRefreshToken());
        assertEquals(7200, vo.getExpiresIn());
    }

    @Test
    void refreshToken_withAccessToken_throwsUnauthorized() {
        String accessToken = jwtUtil.generateAccessToken(42L, "admin", 5L);
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(accessToken);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshToken(request));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    void me_returnsUserInfo() {
        setSecurityContext(42L, "admin", 5L);

        User user = buildUser("13800138000", "hash");
        when(userMapper.selectById(42L)).thenReturn(user);

        Organization org = new Organization();
        org.setId(5L);
        org.setName("Test MCN");
        org.setMaxMembers(20);
        when(organizationMapper.selectById(5L)).thenReturn(org);

        MeVO meVO = authService.me();

        assertEquals("138****8000", meVO.getUser().getPhone());
        assertEquals("Test MCN", meVO.getOrg().getName());
        assertEquals(0, meVO.getVipInfo().getLevel());
        assertEquals("免费版", meVO.getVipInfo().getName());
        assertEquals(5000000L, meVO.getQuotaInfo().getAiQuotaTotal());
    }

    @Test
    void me_noAuth_throwsUnauthorized() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.me());
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    // ========== QA 增量: 短信登录测试 ==========

    @Test
    void smsLogin_success() {
        User user = buildUser("13900139000", passwordEncoder.encode("x"));
        when(userMapper.selectOne(any())).thenReturn(user);
        when(userMapper.updateById(any())).thenReturn(1);
        when(userSessionMapper.insert(any())).thenReturn(1);

        SmsLoginRequest request = new SmsLoginRequest();
        request.setPhone("13900139000");
        request.setCode("123456");

        LoginVO vo = authService.smsLogin(request, "127.0.0.1", "TestAgent");

        assertNotNull(vo.getAccessToken());
        assertNotNull(vo.getRefreshToken());
    }

    @Test
    void smsLogin_userNotFound_throwsUnauthorized() {
        when(userMapper.selectOne(any())).thenReturn(null);

        SmsLoginRequest request = new SmsLoginRequest();
        request.setPhone("13900139000");
        request.setCode("123456");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.smsLogin(request, "127.0.0.1", "TestAgent"));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    void smsLogin_disabledUser_throwsForbidden() {
        User user = buildUser("13900139000", "x");
        user.setStatus(0);
        when(userMapper.selectOne(any())).thenReturn(user);

        SmsLoginRequest request = new SmsLoginRequest();
        request.setPhone("13900139000");
        request.setCode("123456");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.smsLogin(request, "127.0.0.1", "TestAgent"));
        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    // ========== QA 增量: 登录后 lastLoginAt 和 session 验证 ==========

    @Test
    void login_updatesLastLoginAt() {
        User user = buildUser("13800138000", passwordEncoder.encode("Test1234"));
        when(userMapper.selectOne(any())).thenReturn(user);
        when(userMapper.updateById(any())).thenReturn(1);
        when(userSessionMapper.insert(any())).thenReturn(1);

        LoginRequest request = new LoginRequest();
        request.setPhone("13800138000");
        request.setPassword("Test1234");

        authService.login(request, "127.0.0.1", "TestAgent");

        verify(userMapper).updateById(any(User.class));
        assertNotNull(user.getLastLoginAt(), "lastLoginAt 应被设置");
    }

    @Test
    void login_createsSession() {
        User user = buildUser("13800138000", passwordEncoder.encode("Test1234"));
        when(userMapper.selectOne(any())).thenReturn(user);
        when(userMapper.updateById(any())).thenReturn(1);
        when(userSessionMapper.insert(any())).thenReturn(1);

        LoginRequest request = new LoginRequest();
        request.setPhone("13800138000");
        request.setPassword("Test1234");

        authService.login(request, "192.168.1.1", "Chrome/120");

        ArgumentCaptor<UserSession> captor = ArgumentCaptor.forClass(UserSession.class);
        verify(userSessionMapper).insert(captor.capture());
        UserSession session = captor.getValue();
        assertEquals(42L, session.getUserId());
        assertEquals("192.168.1.1", session.getIpAddress());
        assertEquals("Chrome/120", session.getUserAgent());
        assertNotNull(session.getAccessTokenHash());
        assertNotNull(session.getRefreshTokenHash());
    }

    // ========== QA 增量: 注册 BCrypt 和默认值验证 ==========

    @Test
    void register_passwordBcrypt() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(100L);
            return 1;
        });
        when(organizationMapper.insert(any())).thenAnswer(inv -> {
            Organization org = inv.getArgument(0);
            org.setId(10L);
            return 1;
        });
        when(userSessionMapper.insert(any())).thenReturn(1);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPhone("13900139000");
        request.setPassword("SecurePass1");
        request.setCode("1234");

        authService.register(request, "127.0.0.1", "TestAgent");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User saved = captor.getValue();
        assertTrue(saved.getPasswordHash().startsWith("$2a$"),
                "密码应以 BCrypt 格式存储 ($2a$ 前缀)");
        assertTrue(passwordEncoder.matches("SecurePass1", saved.getPasswordHash()),
                "BCrypt hash 应能匹配原始密码");
    }

    @Test
    void register_autoCreatesOrg() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(100L);
            return 1;
        });
        when(organizationMapper.insert(any())).thenAnswer(inv -> {
            Organization org = inv.getArgument(0);
            org.setId(10L);
            return 1;
        });
        when(userSessionMapper.insert(any())).thenReturn(1);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("orguser");
        request.setPhone("13700137000");
        request.setPassword("Test1234");
        request.setCode("9999");

        authService.register(request, "127.0.0.1", "TestAgent");

        ArgumentCaptor<Organization> orgCaptor = ArgumentCaptor.forClass(Organization.class);
        verify(organizationMapper).insert(orgCaptor.capture());
        Organization savedOrg = orgCaptor.getValue();
        assertEquals("orguser的组织", savedOrg.getName());
        assertEquals(100L, savedOrg.getOwnerId());
        assertEquals(20, savedOrg.getMaxMembers());
    }

    @Test
    void register_defaultVipLevel() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(100L);
            return 1;
        });
        when(organizationMapper.insert(any())).thenAnswer(inv -> {
            Organization org = inv.getArgument(0);
            org.setId(10L);
            return 1;
        });
        when(userSessionMapper.insert(any())).thenReturn(1);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("vipuser");
        request.setPhone("13500135000");
        request.setPassword("Test1234");
        request.setCode("1111");

        authService.register(request, "127.0.0.1", "TestAgent");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        assertEquals(0, captor.getValue().getVipLevel());
    }

    // ========== QA 增量: Token 刷新边界 ==========

    @Test
    void refreshToken_expired_throwsTokenExpired() {
        String expiredToken = Jwts.builder()
                .claim("userId", 42L)
                .claim("type", "refresh")
                .setIssuedAt(new Date(System.currentTimeMillis() - 200_000))
                .setExpiration(new Date(System.currentTimeMillis() - 100_000))
                .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(expiredToken);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshToken(request));
        assertEquals(ErrorCode.TOKEN_EXPIRED, ex.getErrorCode());
    }

    @Test
    void refreshToken_invalidToken_throwsUnauthorized() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid.jwt.token");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshToken(request));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    void refreshToken_userDeleted_throwsUnauthorized() {
        when(userMapper.selectById(42L)).thenReturn(null);

        String refreshToken = jwtUtil.generateRefreshToken(42L);
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshToken(request));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    // ========== QA 增量: 退出测试 ==========

    @Test
    void logout_deletesSession() {
        setSecurityContext(42L, "admin", 5L);
        when(userSessionMapper.delete(any())).thenReturn(1);

        authService.logout();

        verify(userSessionMapper).delete(any());
    }

    @Test
    void logout_noAuth_noException() {
        assertDoesNotThrow(() -> authService.logout());
        verify(userSessionMapper, never()).delete(any());
    }

    // ========== QA 增量: me() 边界测试 ==========

    @Test
    void me_noOrg_returnsNullOrg() {
        setSecurityContext(42L, "admin", null);

        User user = buildUser("13800138000", "hash");
        user.setOrgId(null);
        when(userMapper.selectById(42L)).thenReturn(user);

        MeVO meVO = authService.me();

        assertNull(meVO.getOrg(), "无 orgId 时 org 应为 null");
        assertNotNull(meVO.getUser());
        assertNotNull(meVO.getVipInfo());
    }

    @Test
    void me_vipLevelMapping() {
        Map<Integer, String> expected = new HashMap<>();
        expected.put(0, "免费版");
        expected.put(1, "基础版");
        expected.put(2, "专业版");
        expected.put(3, "企业版");

        for (Map.Entry<Integer, String> entry : expected.entrySet()) {
            SecurityContextHolder.clearContext();
            setSecurityContext(42L, "admin", 5L);

            User user = buildUser("13800138000", "hash");
            user.setVipLevel(entry.getKey());
            user.setOrgId(null);
            when(userMapper.selectById(42L)).thenReturn(user);

            MeVO meVO = authService.me();
            assertEquals(entry.getKey(), meVO.getVipInfo().getLevel());
            assertEquals(entry.getValue(), meVO.getVipInfo().getName(),
                    "VIP level " + entry.getKey() + " 应映射为 " + entry.getValue());
        }
    }

    @Test
    void me_quotaComplete() {
        setSecurityContext(42L, "admin", 5L);

        User user = buildUser("13800138000", "hash");
        user.setAiQuotaTotal(10000000L);
        user.setAiQuotaUsed(3000000L);
        user.setDurationQuotaTotal(7200L);
        user.setDurationQuotaUsed(1800L);
        user.setOrgId(null);
        when(userMapper.selectById(42L)).thenReturn(user);

        MeVO meVO = authService.me();

        assertEquals(10000000L, meVO.getQuotaInfo().getAiQuotaTotal());
        assertEquals(3000000L, meVO.getQuotaInfo().getAiQuotaUsed());
        assertEquals(7200L, meVO.getQuotaInfo().getDurationQuotaTotal());
        assertEquals(1800L, meVO.getQuotaInfo().getDurationQuotaUsed());
    }

    private User buildUser(String phone, String passwordHash) {
        User user = new User();
        user.setId(42L);
        user.setUsername("testuser");
        user.setPhone(phone);
        user.setPasswordHash(passwordHash);
        user.setRole("admin");
        user.setOrgId(5L);
        user.setVipLevel(0);
        user.setAiQuotaTotal(5000000L);
        user.setAiQuotaUsed(0L);
        user.setDurationQuotaTotal(0L);
        user.setDurationQuotaUsed(0L);
        user.setStatus(1);
        return user;
    }

    private void setSecurityContext(Long userId, String role, Long orgId) {
        SecurityUser securityUser = new SecurityUser(userId, role, orgId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(securityUser, null, Collections.emptyList()));
    }
}
