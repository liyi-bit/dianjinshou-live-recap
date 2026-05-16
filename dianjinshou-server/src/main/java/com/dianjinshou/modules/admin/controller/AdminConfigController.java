package com.dianjinshou.modules.admin.controller;

import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.admin.service.DefaultAiQuotaService;
import com.dianjinshou.modules.admin.service.SystemSettingsService;
import com.dianjinshou.modules.admin.service.ThirdPartySettings;
import com.dianjinshou.modules.admin.service.ThirdPartySettingsViewService;
import com.dianjinshou.modules.admin.service.UserThirdPartySettingsService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 第三方接入配置接口。
 *
 * 现在每个账号有独立的 AI 密钥 —— GET/PUT 都作用于当前登录用户自己的配置。
 * 平台级配置（短信、MinIO）保留在 /admin/config/platform（仅 admin 可访问）。
 */
@RestController
@RequestMapping("/api/v1/admin/config")
public class AdminConfigController {

    private static final String SECRET_HOLDER = "********";

    /** 允许通过本接口写入的用户级 key（白名单，防止用户误改到全局 key）。 */
    private static final Set<String> USER_LEVEL_KEYS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    ThirdPartySettings.K_AI_PROVIDER,
                    ThirdPartySettings.K_YUNWU_API_KEY,
                    ThirdPartySettings.K_YUNWU_ENDPOINT,
                    ThirdPartySettings.K_YUNWU_MODEL
            )));

    private final SystemSettingsService settingsService;
    private final UserThirdPartySettingsService userSettingsService;
    private final ThirdPartySettingsViewService viewService;
    private final DefaultAiQuotaService quotaService;

    public AdminConfigController(SystemSettingsService settingsService,
                                  UserThirdPartySettingsService userSettingsService,
                                  ThirdPartySettingsViewService viewService,
                                  DefaultAiQuotaService quotaService) {
        this.settingsService = settingsService;
        this.userSettingsService = userSettingsService;
        this.viewService = viewService;
        this.quotaService = quotaService;
    }

    /** 当前用户的默认密钥免费配额用量。 */
    @GetMapping("/quota")
    public ApiResponse<DefaultAiQuotaService.QuotaStatus> getQuota() {
        SecurityUser user = requireLogin();
        return ApiResponse.success(quotaService.getStatus(user.getUserId()));
    }

    /** 返回当前用户的第三方配置（AI）。 */
    @GetMapping
    public ApiResponse<Map<String, Object>> getConfig() {
        SecurityUser user = requireLogin();
        return ApiResponse.success(viewService.buildUserView(user.getUserId()));
    }

    /**
     * 批量更新当前用户的第三方配置。
     * Body: 扁平 map {@code settingKey -> value}
     *   - "********" 代表"保持不变"（前端对 masked 字段的约定）
     *   - 空字符串代表"删除该配置"（用户可以手动清空）
     *   - 非用户级 key 会被拒绝（避免误改平台配置）
     */
    @PutMapping
    public ApiResponse<Void> updateConfig(@RequestBody Map<String, String> body) {
        SecurityUser user = requireLogin();
        Long userId = user.getUserId();
        for (Map.Entry<String, String> e : body.entrySet()) {
            String key = e.getKey();
            String val = e.getValue();
            if (SECRET_HOLDER.equals(val)) continue;  // 保持不变
            if (!USER_LEVEL_KEYS.contains(key)) continue;  // 非白名单 key 忽略
            userSettingsService.set(userId, key, val == null ? "" : val);
        }
        return ApiResponse.success();
    }

    /**
     * 平台级配置（SMS + MinIO）—— 仅管理员可读/写；存放在 system_settings 表。
     */
    @GetMapping("/platform")
    public ApiResponse<Map<String, Object>> getPlatformConfig() {
        requireAdmin();
        return ApiResponse.success(viewService.buildPlatformView());
    }

    @PutMapping("/platform")
    public ApiResponse<Void> updatePlatformConfig(@RequestBody Map<String, String> body) {
        SecurityUser user = requireAdmin();
        Map<String, String> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : body.entrySet()) {
            if (SECRET_HOLDER.equals(e.getValue())) continue;
            // 只允许平台级 key 通过此入口
            String key = e.getKey();
            if (USER_LEVEL_KEYS.contains(key)) continue;
            sanitized.put(key, e.getValue() == null ? "" : e.getValue());
        }
        settingsService.setAll(sanitized, user.getUserId());
        return ApiResponse.success();
    }

    private SecurityUser requireLogin() {
        SecurityUser user = SecurityContextHelper.currentUser();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    private SecurityUser requireAdmin() {
        SecurityUser user = requireLogin();
        String role = user.getRole();
        if (!"admin".equalsIgnoreCase(role) && !"super_admin".equalsIgnoreCase(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return user;
    }
}
