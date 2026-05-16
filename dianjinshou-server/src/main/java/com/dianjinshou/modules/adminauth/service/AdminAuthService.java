package com.dianjinshou.modules.adminauth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.JwtUtil;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.adminauth.dto.AdminLoginRequest;
import com.dianjinshou.modules.adminauth.entity.AdminAccount;
import com.dianjinshou.modules.adminauth.mapper.AdminAccountMapper;
import com.dianjinshou.modules.adminauth.vo.AdminLoginVO;
import com.dianjinshou.modules.adminauth.vo.AdminMeVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AdminAuthService {

    private final AdminAccountMapper adminAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AdminAuthService(AdminAccountMapper adminAccountMapper,
                            PasswordEncoder passwordEncoder,
                            JwtUtil jwtUtil) {
        this.adminAccountMapper = adminAccountMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AdminLoginVO login(AdminLoginRequest request, String ip) {
        AdminAccount account = adminAccountMapper.selectOne(
                new LambdaQueryWrapper<AdminAccount>().eq(AdminAccount::getUsername, request.getUsername()));

        if (account == null || !passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (account.getStatus() == null || account.getStatus() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }

        account.setLastLoginAt(LocalDateTime.now());
        account.setLastLoginIp(ip);
        account.setFailedLoginCount(0);
        adminAccountMapper.updateById(account);

        String token = jwtUtil.generateAdminAccessToken(account.getId(), account.getRole());

        AdminLoginVO vo = new AdminLoginVO();
        vo.setAccessToken(token);
        vo.setExpiresIn(jwtUtil.getAccessTokenTtlSeconds());
        vo.setUser(AdminMeVO.fromEntity(account));
        return vo;
    }

    public AdminMeVO me() {
        SecurityUser user = SecurityContextHelper.currentUser();
        if (user == null || user.getRole() == null || !user.getRole().startsWith("admin_")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        AdminAccount account = adminAccountMapper.selectById(user.getUserId());
        if (account == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号不存在");
        }
        return AdminMeVO.fromEntity(account);
    }
}
