package com.dianjinshou.modules.admin.service;

import com.dianjinshou.modules.admin.entity.UserThirdPartySetting;
import com.dianjinshou.modules.admin.mapper.UserThirdPartySettingMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户级第三方密钥读写服务。
 *
 * 和 {@link SystemSettingsService} 的不同：
 *   - SystemSettingsService 是全局配置（sms / storage 等平台级），带内存 cache
 *   - 本服务是 per-user 配置（ai.yunwu.*），直查 DB 避免 cache 刷新问题
 *     （用户量级小 + 每个业务入口只命中 1-2 次，不值得缓存）
 */
@Service
public class UserThirdPartySettingsService {

    private static final Logger log = LoggerFactory.getLogger(UserThirdPartySettingsService.class);

    private final UserThirdPartySettingMapper mapper;

    public UserThirdPartySettingsService(UserThirdPartySettingMapper mapper) {
        this.mapper = mapper;
    }

    /** 返回用户该 key 的值；空或不存在返回 null。不做 fallback —— 调用方负责处理"未配置"语义。 */
    public String get(Long userId, String key) {
        if (userId == null) return null;
        String v = mapper.selectValue(userId, key);
        return (v == null || v.isEmpty()) ? null : v;
    }

    /** 带默认值版本。key 空时返回 fallback（通常是 application.yml 里给的兜底，但业务一般不希望 fallback）。 */
    public String get(Long userId, String key, String fallback) {
        String v = get(userId, key);
        return v != null ? v : fallback;
    }

    /** 列出该用户所有配置（用于设置页加载）。 */
    public List<UserThirdPartySetting> listByUser(Long userId) {
        return mapper.selectByUserId(userId);
    }

    /** Upsert 一条配置。value 为空字符串等同删除（回到"未配置"状态）。 */
    public void set(Long userId, String key, String value) {
        if (userId == null || key == null) return;
        UserThirdPartySetting existing = null;
        List<UserThirdPartySetting> rows = mapper.selectByUserId(userId);
        for (UserThirdPartySetting row : rows) {
            if (key.equals(row.getSettingKey())) { existing = row; break; }
        }
        if (value == null || value.isEmpty()) {
            if (existing != null) {
                // 删除
                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserThirdPartySetting> q =
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                q.eq("user_id", userId).eq("setting_key", key);
                mapper.delete(q);
            }
            return;
        }
        if (existing == null) {
            UserThirdPartySetting s = new UserThirdPartySetting();
            s.setUserId(userId);
            s.setSettingKey(key);
            s.setSettingValue(value);
            s.setUpdatedAt(LocalDateTime.now());
            mapper.insert(s);
        } else {
            com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<UserThirdPartySetting> u =
                    new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
            u.eq("user_id", userId).eq("setting_key", key)
             .set("setting_value", value)
             .set("updated_at", LocalDateTime.now());
            mapper.update(null, u);
        }
        log.debug("Set user[{}] {} = ***", userId, key);
    }
}
