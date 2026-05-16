package com.dianjinshou.modules.admin.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the JSON structure consumed by the third-party settings UI tab.
 *
 * 两类分组：
 *   - 用户级（AI）：每个 account 独立配置 —— 本 service 的方法需要 userId
 *   - 平台级（SMS / Storage）：仅限 SUPER_ADMIN 看到；或者前端隐藏
 *
 * Secret-typed 字段显示 "********"；raw 值不回传（前端保存时原样提交视为"不变"）。
 */
@Service
public class ThirdPartySettingsViewService {

    private static final String SECRET_HOLDER = "********";

    private final ThirdPartySettings tps;
    private final SystemSettingsService systemSettings;

    public ThirdPartySettingsViewService(ThirdPartySettings tps,
                                          SystemSettingsService systemSettings) {
        this.tps = tps;
        this.systemSettings = systemSettings;
    }

    /** 构建"我的第三方配置"视图（只包含用户级 AI 分组）。 */
    public Map<String, Object> buildUserView(Long userId) {
        List<Map<String, Object>> groups = new ArrayList<>();

        groups.add(group("ai", "AI 模型 (云雾 Yunwu)", new Field[]{
            text(ThirdPartySettings.K_AI_PROVIDER, "Provider", tps.getAiProvider(userId), "yunwu / doubao / deepseek / mock"),
            secret(ThirdPartySettings.K_YUNWU_API_KEY, "API Key", tps.getYunwuApiKeyOptional(userId), "sk-xxxxxxxx"),
            text(ThirdPartySettings.K_YUNWU_ENDPOINT, "Endpoint", tps.getYunwuEndpoint(userId), "https://api.yunwu.ai/v1"),
            text(ThirdPartySettings.K_YUNWU_MODEL, "模型", tps.getYunwuModel(userId), "gpt-4o-mini"),
        }));

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("groups", groups);
        return root;
    }

    /** 平台级配置（SMS / Storage）—— 只给 SUPER_ADMIN/ADMIN 看的视图。 */
    public Map<String, Object> buildPlatformView() {
        List<Map<String, Object>> groups = new ArrayList<>();

        groups.add(group("sms", "短信 (大汉三通)", new Field[]{
            text(ThirdPartySettings.K_SMS_DAHAN_ENDPOINT, "API 地址", systemSettings.get(ThirdPartySettings.K_SMS_DAHAN_ENDPOINT, ""), "https://dhst.bangkao.com/json/sms/Submit"),
            text(ThirdPartySettings.K_SMS_DAHAN_ACCOUNT, "账号", systemSettings.get(ThirdPartySettings.K_SMS_DAHAN_ACCOUNT, ""), ""),
            secret(ThirdPartySettings.K_SMS_DAHAN_PASSWORD, "密码", systemSettings.get(ThirdPartySettings.K_SMS_DAHAN_PASSWORD, ""), ""),
            text(ThirdPartySettings.K_SMS_DAHAN_SIGN, "签名", systemSettings.get(ThirdPartySettings.K_SMS_DAHAN_SIGN, ""), "学东"),
        }));

        groups.add(group("storage", "对象存储 (MinIO)", new Field[]{
            text(ThirdPartySettings.K_STORAGE_ENDPOINT, "Endpoint", systemSettings.get(ThirdPartySettings.K_STORAGE_ENDPOINT, ""), "http://localhost:9000"),
            text(ThirdPartySettings.K_STORAGE_ACCESS_KEY, "Access Key", systemSettings.get(ThirdPartySettings.K_STORAGE_ACCESS_KEY, ""), "minioadmin"),
            secret(ThirdPartySettings.K_STORAGE_SECRET_KEY, "Secret Key", systemSettings.get(ThirdPartySettings.K_STORAGE_SECRET_KEY, ""), ""),
            text(ThirdPartySettings.K_STORAGE_BUCKET_RECORDINGS, "Bucket - 录制", systemSettings.get(ThirdPartySettings.K_STORAGE_BUCKET_RECORDINGS, ""), "recordings"),
            text(ThirdPartySettings.K_STORAGE_BUCKET_FILES, "Bucket - 文件", systemSettings.get(ThirdPartySettings.K_STORAGE_BUCKET_FILES, ""), "files"),
            text(ThirdPartySettings.K_STORAGE_BUCKET_CLIPS, "Bucket - 切片", systemSettings.get(ThirdPartySettings.K_STORAGE_BUCKET_CLIPS, ""), "clips"),
        }));

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("groups", groups);
        return root;
    }

    private Map<String, Object> group(String key, String label, Field[] fields) {
        Map<String, Object> g = new LinkedHashMap<>();
        g.put("key", key);
        g.put("label", label);
        List<Map<String, Object>> fieldList = new ArrayList<>();
        for (Field f : fields) fieldList.add(f.toMap());
        g.put("fields", fieldList);
        return g;
    }

    private Field text(String key, String label, String value, String placeholder) {
        return new Field(key, label, "text", value == null ? "" : value, false, placeholder);
    }

    private Field secret(String key, String label, String value, String placeholder) {
        boolean has = value != null && !value.isEmpty();
        return new Field(key, label, "secret", has ? SECRET_HOLDER : "", has, placeholder);
    }

    private static class Field {
        final String key, label, type, value, placeholder;
        final boolean masked;
        Field(String key, String label, String type, String value, boolean masked, String placeholder) {
            this.key = key; this.label = label; this.type = type;
            this.value = value; this.masked = masked; this.placeholder = placeholder;
        }
        Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", key);
            m.put("label", label);
            m.put("type", type);
            m.put("value", value);
            m.put("masked", masked);
            m.put("placeholder", placeholder);
            return m;
        }
    }
}
