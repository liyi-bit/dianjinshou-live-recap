package com.dianjinshou.modules.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.JwtUtil;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.modules.auth.dto.*;
import com.dianjinshou.modules.auth.entity.User;
import com.dianjinshou.modules.auth.entity.UserSession;
import com.dianjinshou.modules.auth.mapper.UserMapper;
import com.dianjinshou.modules.auth.mapper.UserSessionMapper;
import com.dianjinshou.modules.auth.vo.LoginVO;
import com.dianjinshou.modules.auth.vo.MeVO;
import com.dianjinshou.modules.auth.vo.TokenVO;
import com.dianjinshou.modules.auth.vo.UserVO;
import com.dianjinshou.modules.organization.entity.Organization;
import com.dianjinshou.modules.organization.mapper.OrganizationMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Map<Integer, String> VIP_NAMES = new HashMap<>();
    static {
        VIP_NAMES.put(0, "免费版");
        VIP_NAMES.put(1, "基础版");
        VIP_NAMES.put(2, "专业版");
        VIP_NAMES.put(3, "企业版");
    }

    private final UserMapper userMapper;
    private final UserSessionMapper userSessionMapper;
    private final OrganizationMapper organizationMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SmsService smsService;

    public AuthService(UserMapper userMapper,
                       UserSessionMapper userSessionMapper,
                       OrganizationMapper organizationMapper,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       SmsService smsService) {
        this.userMapper = userMapper;
        this.userSessionMapper = userSessionMapper;
        this.organizationMapper = organizationMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.smsService = smsService;
    }

    public LoginVO login(LoginRequest request, String ipAddress, String userAgent) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone()));
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "手机号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "手机号或密码错误");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        return buildLoginVO(user, ipAddress, userAgent);
    }

    public LoginVO smsLogin(SmsLoginRequest request, String ipAddress, String userAgent) {
        smsService.verifyCode(request.getPhone(), "login", request.getCode());
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone()));
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        return buildLoginVO(user, ipAddress, userAgent);
    }

    @Transactional
    public LoginVO register(RegisterRequest request, String ipAddress, String userAgent) {
        smsService.verifyCode(request.getPhone(), "register", request.getCode());
        Long existCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone()));
        if (existCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "手机号已注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole("operator");
        user.setVipLevel(3);
        user.setVipExpireAt(LocalDateTime.now().plusDays(30));
        user.setAiQuotaTotal(50000000L);  // 5000 万字
        user.setAiQuotaUsed(0L);
        user.setDurationQuotaTotal(2160000L); // 600 hours of analysis time (seconds)
        user.setDurationQuotaUsed(0L);
        user.setStatus(1);
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.insert(user);

        // Auto-create a default organization for the new user
        Organization org = new Organization();
        org.setName(user.getUsername() + "的组织");
        org.setOwnerId(user.getId());
        org.setMaxMembers(20);
        org.setVipLevel(0);
        organizationMapper.insert(org);

        user.setOrgId(org.getId());
        userMapper.updateById(user);

        return buildLoginVO(user, ipAddress, userAgent);
    }

    public TokenVO refreshToken(RefreshTokenRequest request) {
        Claims claims;
        try {
            claims = jwtUtil.parseToken(request.getRefreshToken());
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "Refresh Token 已过期，请重新登录");
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "无效的 Refresh Token");
        }

        if (!jwtUtil.isRefreshToken(claims)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "无效的 Refresh Token");
        }

        Long userId = jwtUtil.getUserId(claims);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole(), user.getOrgId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        TokenVO vo = new TokenVO();
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshToken);
        vo.setExpiresIn(jwtUtil.getAccessTokenTtlSeconds());
        return vo;
    }

    public void logout() {
        Long userId = SecurityContextHelper.currentUserId();
        if (userId == null) {
            return;
        }
        // Delete sessions for current user (simple approach; can be refined with specific token hash)
        userSessionMapper.delete(
                new LambdaQueryWrapper<UserSession>().eq(UserSession::getUserId, userId));
    }

    public MeVO me() {
        Long userId = SecurityContextHelper.currentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        MeVO meVO = new MeVO();
        meVO.setUser(UserVO.fromEntity(user));

        if (user.getOrgId() != null) {
            Organization org = organizationMapper.selectById(user.getOrgId());
            if (org != null) {
                MeVO.OrgInfo orgInfo = new MeVO.OrgInfo();
                orgInfo.setId(org.getId());
                orgInfo.setName(org.getName());
                orgInfo.setMaxMembers(org.getMaxMembers());
                meVO.setOrg(orgInfo);
            }
        }

        MeVO.VipInfo vipInfo = new MeVO.VipInfo();
        vipInfo.setLevel(user.getVipLevel());
        vipInfo.setName(VIP_NAMES.getOrDefault(user.getVipLevel(), "未知"));
        vipInfo.setExpireAt(user.getVipExpireAt() != null ? user.getVipExpireAt().format(DATE_TIME_FORMATTER) : null);
        meVO.setVipInfo(vipInfo);

        MeVO.QuotaInfo quotaInfo = new MeVO.QuotaInfo();
        quotaInfo.setAiQuotaTotal(user.getAiQuotaTotal());
        quotaInfo.setAiQuotaUsed(user.getAiQuotaUsed());
        quotaInfo.setDurationQuotaTotal(user.getDurationQuotaTotal());
        quotaInfo.setDurationQuotaUsed(user.getDurationQuotaUsed());
        meVO.setQuotaInfo(quotaInfo);

        return meVO;
    }

    private LoginVO buildLoginVO(User user, String ipAddress, String userAgent) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole(), user.getOrgId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        saveSession(user.getId(), accessToken, refreshToken, ipAddress, userAgent);

        LoginVO vo = new LoginVO();
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshToken);
        vo.setExpiresIn(jwtUtil.getAccessTokenTtlSeconds());
        vo.setUser(UserVO.fromEntity(user));
        return vo;
    }

    private void saveSession(Long userId, String accessToken, String refreshToken,
                             String ipAddress, String userAgent) {
        UserSession session = new UserSession();
        session.setUserId(userId);
        session.setAccessTokenHash(sha256(accessToken));
        session.setRefreshTokenHash(sha256(refreshToken));
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        session.setExpiresAt(LocalDateTime.now().plusDays(7));
        session.setCreatedAt(LocalDateTime.now());
        userSessionMapper.insert(session);
    }

    @Transactional
    public Map<String, Object> initUserOrg(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        Map<String, Object> result = new HashMap<>();
        if (user.getOrgId() != null) {
            result.put("message", "User already has org_id=" + user.getOrgId());
            return result;
        }
        Organization org = new Organization();
        org.setName(user.getUsername() + "的组织");
        org.setOwnerId(user.getId());
        org.setMaxMembers(20);
        org.setVipLevel(0);
        organizationMapper.insert(org);

        user.setOrgId(org.getId());
        userMapper.updateById(user);

        result.put("message", "Created org " + org.getId() + " for user " + userId);
        result.put("orgId", org.getId());
        return result;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
