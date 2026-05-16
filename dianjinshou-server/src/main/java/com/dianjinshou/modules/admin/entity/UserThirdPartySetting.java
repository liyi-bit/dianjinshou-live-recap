package com.dianjinshou.modules.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户级第三方密钥配置 —— 按 (user_id, setting_key) 联合主键。
 * 每个用户必须单独配置自己的云雾 / 腾讯 ASR+COS 密钥后才能使用对应功能。
 */
@TableName("user_third_party_settings")
public class UserThirdPartySetting implements Serializable {

    private Long userId;
    private String settingKey;
    private String settingValue;
    private LocalDateTime updatedAt;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getSettingKey() { return settingKey; }
    public void setSettingKey(String settingKey) { this.settingKey = settingKey; }

    public String getSettingValue() { return settingValue; }
    public void setSettingValue(String settingValue) { this.settingValue = settingValue; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
