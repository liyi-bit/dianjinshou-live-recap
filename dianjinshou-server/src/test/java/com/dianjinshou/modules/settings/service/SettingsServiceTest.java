package com.dianjinshou.modules.settings.service;

import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.auth.entity.User;
import com.dianjinshou.modules.auth.mapper.UserMapper;
import com.dianjinshou.modules.settings.dto.AccountSettingsRequest;
import com.dianjinshou.modules.settings.dto.SubAccountRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SettingsService settingsService;

    @BeforeEach
    void setUp() {
        SecurityUser user = new SecurityUser(1L, "admin", 5L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAccountSettings_success() {
        User user = buildUser();
        when(userMapper.selectById(1L)).thenReturn(user);

        Map<String, Object> result = settingsService.getAccountSettings();

        assertEquals("测试用户", result.get("username"));
        assertEquals("13800138000", result.get("phone"));
    }

    @Test
    void getAccountSettings_notFound() {
        when(userMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> settingsService.getAccountSettings());
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void updateAccountSettings_changeUsername() {
        User user = buildUser();
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        AccountSettingsRequest req = new AccountSettingsRequest();
        req.setUsername("新名字");

        settingsService.updateAccountSettings(req);
        verify(userMapper).updateById(any(User.class));
    }

    @Test
    void updateAccountSettings_changePassword_wrongOld() {
        User user = buildUser();
        when(userMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        AccountSettingsRequest req = new AccountSettingsRequest();
        req.setOldPassword("wrong");
        req.setNewPassword("newpass123");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> settingsService.updateAccountSettings(req));
        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    void getMembership_success() {
        User user = buildUser();
        user.setVipLevel(3);
        user.setAiQuotaTotal(5000000L);
        user.setAiQuotaUsed(1000000L);
        when(userMapper.selectById(1L)).thenReturn(user);

        Map<String, Object> result = settingsService.getMembership();

        assertEquals(3, result.get("vipLevel"));
        assertEquals(5000000L, result.get("aiQuotaTotal"));
        assertEquals(1000000L, result.get("aiQuotaUsed"));
    }

    @Test
    void createSubAccount_maxExceeded() {
        when(userMapper.selectCount(any())).thenReturn(20L);

        SubAccountRequest req = new SubAccountRequest();
        req.setUsername("子账号");
        req.setPhone("13900139000");
        req.setPassword("pass1234");
        req.setRole("operator");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> settingsService.createSubAccount(req));
        assertEquals(ErrorCode.BUSINESS_RULE_VIOLATION, ex.getErrorCode());
    }

    @Test
    void createSubAccount_duplicatePhone() {
        when(userMapper.selectCount(any())).thenReturn(5L);
        when(userMapper.selectOne(any())).thenReturn(new User());

        SubAccountRequest req = new SubAccountRequest();
        req.setUsername("子账号");
        req.setPhone("13900139000");
        req.setPassword("pass1234");
        req.setRole("operator");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> settingsService.createSubAccount(req));
        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
    }

    @Test
    void createSubAccount_success() {
        when(userMapper.selectCount(any())).thenReturn(5L);
        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userMapper.insert(any(User.class))).thenReturn(1);

        SubAccountRequest req = new SubAccountRequest();
        req.setUsername("子账号");
        req.setPhone("13900139000");
        req.setPassword("pass1234");
        req.setRole("operator");

        settingsService.createSubAccount(req);
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void deleteSubAccount_cannotDeleteSelf() {
        User user = buildUser();
        when(userMapper.selectById(1L)).thenReturn(user);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> settingsService.deleteSubAccount(1L));
        assertEquals(ErrorCode.BUSINESS_RULE_VIOLATION, ex.getErrorCode());
    }

    private User buildUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("测试用户");
        user.setPhone("13800138000");
        user.setOrgId(5L);
        user.setVipLevel(0);
        user.setAiQuotaTotal(500000L);
        user.setAiQuotaUsed(0L);
        user.setDurationQuotaTotal(0L);
        user.setDurationQuotaUsed(0L);
        user.setPasswordHash("$2a$10$hash");
        return user;
    }
}
