package com.dianjinshou.modules.admin.service;

import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.storage.StorageProperties;
import com.dianjinshou.integration.ai.AiConfig;
import com.dianjinshou.integration.dahansan3tong.DahanSmsProperties;
import org.springframework.stereotype.Component;

/**
 * Single source of truth for third-party credentials/endpoints.
 *
 * 两类配置：
 *   1. 平台级（短信、MinIO）—— 全局，沿用 system_settings 表，所有账号共享
 *   2. 用户级（云雾 AI）—— 按 user_id 隔离，但有免费 fallback
 *
 * <h3>用户级读取策略（免费额度方案）：</h3>
 * getter 的优先级：
 *   1. 用户自己在 user_third_party_settings 里配置的值（有就用，不计数）
 *   2. 没配 → 回退到 system_settings 里的全局默认值（相当于系统赠送的免费额度）
 *   3. 系统级也空 → 抛 {@link ErrorCode#THIRD_PARTY_NOT_CONFIGURED}
 *
 * 配额检查（是否超过 5 次免费 AI 解析）由业务入口用 {@link #hasOwnAiConfig(Long)}
 * 显式判断后自行 +1 计数，不在 getter 里做（一次 AI 调用会读多个 key，放 getter 会重复计数）。
 */
@Component
public class ThirdPartySettings {

    public static final String K_AI_PROVIDER = "ai.provider";
    public static final String K_YUNWU_API_KEY = "ai.yunwu.api_key";
    public static final String K_YUNWU_ENDPOINT = "ai.yunwu.endpoint";
    public static final String K_YUNWU_MODEL = "ai.yunwu.model";

    public static final String K_SMS_DAHAN_ENDPOINT = "sms.dahan.endpoint";
    public static final String K_SMS_DAHAN_ACCOUNT = "sms.dahan.account";
    public static final String K_SMS_DAHAN_PASSWORD = "sms.dahan.password";
    public static final String K_SMS_DAHAN_SIGN = "sms.dahan.sign";

    public static final String K_STORAGE_ENDPOINT = "storage.endpoint";
    public static final String K_STORAGE_ACCESS_KEY = "storage.access_key";
    public static final String K_STORAGE_SECRET_KEY = "storage.secret_key";
    public static final String K_STORAGE_BUCKET_RECORDINGS = "storage.bucket_recordings";
    public static final String K_STORAGE_BUCKET_FILES = "storage.bucket_files";
    public static final String K_STORAGE_BUCKET_CLIPS = "storage.bucket_clips";

    private final SystemSettingsService settings;
    private final UserThirdPartySettingsService userSettings;
    private final AiConfig.AiProperties aiProps;
    private final DahanSmsProperties smsProps;
    private final StorageProperties storageProps;

    public ThirdPartySettings(SystemSettingsService settings,
                              UserThirdPartySettingsService userSettings,
                              AiConfig.AiProperties aiProps,
                              DahanSmsProperties smsProps,
                              StorageProperties storageProps) {
        this.settings = settings;
        this.userSettings = userSettings;
        this.aiProps = aiProps;
        this.smsProps = smsProps;
        this.storageProps = storageProps;
    }

    // ============== 用户级配置读取 (用户空 → 回落到全局默认；都空 → 抛 THIRD_PARTY_NOT_CONFIGURED) ==============

    /** 内部：user → system_settings → application.yml 三层回退。第一个非空值返回。 */
    private String readWithFallback(Long userId, String key, String yamlFallback) {
        String v = userSettings.get(userId, key);
        if (v != null && !v.isEmpty()) return v;
        String sys = settings.get(key, null);
        if (sys != null && !sys.isEmpty()) return sys;
        return yamlFallback;
    }

    /** 读取用户级密钥；都空抛业务异常引导去配置页。 */
    private String requireWithFallback(Long userId, String key, String humanName) {
        String v = readWithFallback(userId, key, null);
        if (v == null || v.isEmpty()) {
            throw new BusinessException(ErrorCode.THIRD_PARTY_NOT_CONFIGURED,
                    "请先在设置→第三方接入中配置 " + humanName);
        }
        return v;
    }

    // AI - Yunwu
    public String getAiProvider(Long userId)   { return readWithFallback(userId, K_AI_PROVIDER, aiProps.getProvider()); }
    public String getYunwuApiKey(Long userId)  { return requireWithFallback(userId, K_YUNWU_API_KEY, "云雾 AI API Key"); }
    public String getYunwuEndpoint(Long userId){ return readWithFallback(userId, K_YUNWU_ENDPOINT, aiProps.getYunwuEndpoint()); }
    public String getYunwuModel(Long userId)   { return readWithFallback(userId, K_YUNWU_MODEL, aiProps.getYunwuModel()); }

    /** 非抛异常版本（设置页加载自己当前值显示）。只读用户自己的配置，不回退到全局。 */
    public String getYunwuApiKeyOptional(Long userId)    { return userSettings.get(userId, K_YUNWU_API_KEY, ""); }

    // ============== 配额判定 ==============

    /**
     * 判定：用户是否已经配置了自己的 AI 密钥。
     * <p>返回 true → 不走免费配额；false → 使用全局默认密钥，需要检查 5 次配额</p>
     */
    public boolean hasOwnAiConfig(Long userId) {
        if (userId == null) return false;
        String apiKey = userSettings.get(userId, K_YUNWU_API_KEY);
        return apiKey != null && !apiKey.isEmpty();
    }

    // SMS - Dahan
    public String getSmsEndpoint()  { return settings.get(K_SMS_DAHAN_ENDPOINT, smsProps.getDahan().getEndpoint()); }
    public String getSmsAccount()   { return settings.get(K_SMS_DAHAN_ACCOUNT,  smsProps.getDahan().getAccount()); }
    public String getSmsPassword()  { return settings.get(K_SMS_DAHAN_PASSWORD, smsProps.getDahan().getPassword()); }
    public String getSmsSign()      { return settings.get(K_SMS_DAHAN_SIGN,     smsProps.getDahan().getSign()); }

    // Storage - MinIO
    public String getStorageEndpoint()        { return settings.get(K_STORAGE_ENDPOINT, storageProps.getEndpoint()); }
    public String getStorageAccessKey()       { return settings.get(K_STORAGE_ACCESS_KEY, storageProps.getAccessKey()); }
    public String getStorageSecretKey()       { return settings.get(K_STORAGE_SECRET_KEY, storageProps.getSecretKey()); }
    public String getBucketRecordings()       { return settings.get(K_STORAGE_BUCKET_RECORDINGS, storageProps.getBucketRecordings()); }
    public String getBucketFiles()            { return settings.get(K_STORAGE_BUCKET_FILES, storageProps.getBucketFiles()); }
    public String getBucketClips()            { return settings.get(K_STORAGE_BUCKET_CLIPS, storageProps.getBucketClips()); }
}
