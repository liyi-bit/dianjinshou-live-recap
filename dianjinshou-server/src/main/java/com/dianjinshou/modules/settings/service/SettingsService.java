package com.dianjinshou.modules.settings.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.modules.auth.entity.User;
import com.dianjinshou.modules.auth.mapper.UserMapper;
import com.dianjinshou.modules.settings.dto.AccountSettingsRequest;
import com.dianjinshou.modules.settings.dto.SubAccountRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SettingsService {

    private static final int MAX_SUB_ACCOUNTS = 20;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public SettingsService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public Map<String, Object> getAccountSettings() {
        Long userId = SecurityContextHelper.currentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        Map<String, Object> settings = new HashMap<>();
        settings.put("username", user.getUsername());
        settings.put("phone", user.getPhone());
        settings.put("avatarUrl", user.getAvatarUrl());
        settings.put("email", user.getEmail());
        settings.put("wechatBound", user.getWechatOpenId() != null);
        settings.put("qqBound", user.getQqOpenId() != null);
        return settings;
    }

    @Transactional
    public void updateAccountSettings(AccountSettingsRequest request) {
        Long userId = SecurityContextHelper.currentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            // Check if phone is already taken by another user
            User existing = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone()));
            if (existing != null && !existing.getId().equals(userId)) {
                throw new BusinessException(ErrorCode.CONFLICT, "该手机号已被使用");
            }
            user.setPhone(request.getPhone());
        }
        if (request.getNewPassword() != null && request.getOldPassword() != null) {
            if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "原密码不正确");
            }
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        }
        userMapper.updateById(user);
    }

    public Map<String, Object> getMembership() {
        Long userId = SecurityContextHelper.currentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        Map<String, Object> membership = new HashMap<>();
        membership.put("vipLevel", user.getVipLevel());
        membership.put("vipExpireAt", user.getVipExpireAt());
        membership.put("aiQuotaTotal", user.getAiQuotaTotal());
        membership.put("aiQuotaUsed", user.getAiQuotaUsed());
        membership.put("durationQuotaTotal", user.getDurationQuotaTotal());
        membership.put("durationQuotaUsed", user.getDurationQuotaUsed());
        return membership;
    }

    public List<Map<String, Object>> getSubAccounts() {
        Long orgId = SecurityContextHelper.currentOrgId();
        if (orgId == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无组织信息");
        }
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getOrgId, orgId)
                        .ne(User::getId, SecurityContextHelper.currentUserId()));
        return users.stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("phone", u.getPhone());
            m.put("role", u.getRole());
            m.put("status", u.getStatus());
            m.put("createdAt", u.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void createSubAccount(SubAccountRequest request) {
        Long orgId = SecurityContextHelper.currentOrgId();
        if (orgId == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无组织信息");
        }

        // Check max 20
        long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getOrgId, orgId));
        if (count >= MAX_SUB_ACCOUNTS) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "子账号数量已达上限(" + MAX_SUB_ACCOUNTS + "个)");
        }

        // Check duplicate phone
        User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone()));
        if (existing != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "该手机号已注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setOrgId(orgId);
        user.setStatus(1);
        user.setVipLevel(0);
        user.setAiQuotaTotal(0L);
        user.setAiQuotaUsed(0L);
        user.setDurationQuotaTotal(0L);
        user.setDurationQuotaUsed(0L);
        userMapper.insert(user);
    }

    @Transactional
    public void deleteSubAccount(Long subAccountId) {
        Long orgId = SecurityContextHelper.currentOrgId();
        User user = userMapper.selectById(subAccountId);
        if (user == null || !orgId.equals(user.getOrgId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "子账号不存在");
        }
        if (user.getId().equals(SecurityContextHelper.currentUserId())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "不能删除自己");
        }
        userMapper.deleteById(subAccountId);
    }

    @Transactional
    public String uploadAvatar(MultipartFile file) {
        Long userId = SecurityContextHelper.currentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                : ".png";
        String filename = "avatar_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;

        try {
            Path uploadDir = Paths.get("uploads", "avatars");
            Files.createDirectories(uploadDir);
            Path filePath = uploadDir.resolve(filename);
            file.transferTo(filePath.toFile());

            String url = "/uploads/avatars/" + filename;
            user.setAvatarUrl(url);
            userMapper.updateById(user);
            return url;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "头像上传失败");
        }
    }
}
