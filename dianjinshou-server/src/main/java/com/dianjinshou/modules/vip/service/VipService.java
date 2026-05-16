package com.dianjinshou.modules.vip.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.modules.auth.entity.User;
import com.dianjinshou.modules.auth.mapper.UserMapper;
import com.dianjinshou.modules.vip.entity.VipPlan;
import com.dianjinshou.modules.vip.mapper.VipPlanMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VipService {

    private final VipPlanMapper vipPlanMapper;
    private final UserMapper userMapper;

    public VipService(VipPlanMapper vipPlanMapper, UserMapper userMapper) {
        this.vipPlanMapper = vipPlanMapper;
        this.userMapper = userMapper;
    }

    public List<VipPlan> listPlans() {
        return vipPlanMapper.selectList(
                new LambdaQueryWrapper<VipPlan>()
                        .eq(VipPlan::getIsActive, 1)
                        .orderByAsc(VipPlan::getSortOrder));
    }

    public Map<String, Object> getQuotaInfo() {
        Long userId = SecurityContextHelper.currentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        Map<String, Object> info = new HashMap<>();
        info.put("aiQuotaTotal", user.getAiQuotaTotal());
        info.put("aiQuotaUsed", user.getAiQuotaUsed());
        info.put("aiQuotaRemaining", user.getAiQuotaTotal() - user.getAiQuotaUsed());
        info.put("durationQuotaTotal", user.getDurationQuotaTotal());
        info.put("durationQuotaUsed", user.getDurationQuotaUsed());
        info.put("vipLevel", user.getVipLevel());
        info.put("vipExpireAt", user.getVipExpireAt());

        // Max concurrent recordings by VIP level
        int maxConcurrent = user.getVipLevel() != null && user.getVipLevel() >= 3 ? 10 : 1;
        info.put("maxConcurrentRecordings", maxConcurrent);
        return info;
    }

    public boolean hasQuotaForAnalysis(Long userId, long requiredChars) {
        User user = userMapper.selectById(userId);
        if (user == null) return false;
        long remaining = user.getAiQuotaTotal() - user.getAiQuotaUsed();
        return remaining >= requiredChars;
    }

    public String redeemCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "兑换码不能为空");
        }
        // TODO: Implement actual redeem code logic with redeem_codes table
        // For now, validate format and return pending message
        throw new BusinessException(ErrorCode.PARAM_ERROR, "兑换码无效或已使用");
    }
}
