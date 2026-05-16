-- 按用户隔离的第三方密钥配置（云雾 AI / 腾讯 ASR+COS）
-- 业务逻辑：
--   - 用户有自己的配置 → 用自己的，无限额
--   - 用户没自己的配置 → 走 system_settings 里的全局默认密钥，但限额 5 个视频的 AI 解析
--   - 超限后拦下所有需要 AI/ASR 的操作，提示去配置页
-- 因此 system_settings 的 7 个 key 保留作为全局默认（**不删除**）。

CREATE TABLE IF NOT EXISTS user_third_party_settings (
    user_id       BIGINT      NOT NULL,
    setting_key   VARCHAR(128) NOT NULL,
    setting_value TEXT,
    updated_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id, setting_key),
    KEY idx_setting_key (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='用户级第三方服务配置（云雾 AI、腾讯 ASR/COS 等）';

-- 主账号 id=3（12345）已有数百次历史 AI 分析，若走免费配额会立刻超限。
-- 把 system_settings 里的 7 个 key 复制成他的 user 级配置，让他以"有自己的密钥"身份工作，不走免费配额。
-- 其他账号 user_third_party_settings 为空 → 走免费 5 次配额。
INSERT INTO user_third_party_settings (user_id, setting_key, setting_value, updated_at)
SELECT 3 AS user_id, s.setting_key, s.setting_value, NOW(3)
  FROM system_settings s
 WHERE s.setting_key IN (
   'ai.provider',
   'ai.yunwu.api_key',
   'ai.yunwu.endpoint',
   'ai.yunwu.model',
   'asr.provider',
   'asr.tencent.secret_id',
   'asr.tencent.secret_key',
   'asr.tencent.cos_bucket',
   'asr.tencent.cos_region'
 )
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), updated_at = VALUES(updated_at);
