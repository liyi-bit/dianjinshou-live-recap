package com.dianjinshou.modules.client.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.modules.admin.entity.SystemSetting;
import com.dianjinshou.modules.admin.mapper.SystemSettingMapper;
import com.dianjinshou.modules.client.vo.ClientVersionVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客户端版本接口（无需登录 —— 桌面 app 启动时就要调，此时还没 token）。
 *
 * 直接查表绕过 SystemSettingsService 缓存：publish_desktop.py 用 SQL 直更行时，
 * 缓存不会刷新，所以这里每次命中数据库。QPS 很低（每个客户端启动 1 次）。
 */
@RestController
@RequestMapping("/api/v1/public")
public class ClientVersionController {

    private static final String KEY_MIN = "client.min_version";
    private static final String KEY_LATEST = "client.latest_version";
    private static final String DEFAULT_DOWNLOAD_URL =
            "http://localhost:18081/desktop/dianjinshou-desktop-latest-setup.exe";

    private final SystemSettingMapper settingMapper;

    public ClientVersionController(SystemSettingMapper settingMapper) {
        this.settingMapper = settingMapper;
    }

    @GetMapping("/client-version")
    public ApiResponse<ClientVersionVO> getClientVersion() {
        ClientVersionVO vo = new ClientVersionVO();
        vo.setMinVersion(readOr(KEY_MIN, "0.0.0"));
        vo.setLatestVersion(readOr(KEY_LATEST, "1.0.0"));
        vo.setDownloadUrl(DEFAULT_DOWNLOAD_URL);
        return ApiResponse.success(vo);
    }

    private String readOr(String key, String fallback) {
        SystemSetting s = settingMapper.selectById(key);
        if (s == null || s.getSettingValue() == null || s.getSettingValue().isEmpty()) {
            return fallback;
        }
        return s.getSettingValue();
    }
}
