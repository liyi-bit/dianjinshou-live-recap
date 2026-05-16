package com.dianjinshou.modules.admin.controller;

import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.admin.service.DailyAiQuotaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前用户的每日 AI 复盘额度查询（v1.1.0）。
 * 前端：逐字稿页面顶部显示"已用 X/10 · 明日 0 点重置"。
 */
@RestController
@RequestMapping("/api/v1/quota")
public class DailyQuotaController {

    private final DailyAiQuotaService quota;

    public DailyQuotaController(DailyAiQuotaService quota) {
        this.quota = quota;
    }

    @GetMapping("/daily")
    public ApiResponse<DailyAiQuotaService.DailyQuotaStatus> getDaily() {
        SecurityUser user = SecurityContextHelper.currentUser();
        if (user == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        return ApiResponse.success(quota.getStatus(user.getUserId()));
    }
}
