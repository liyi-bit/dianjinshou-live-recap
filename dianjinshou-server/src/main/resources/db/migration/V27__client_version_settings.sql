-- 客户端版本管理：minVersion 低于此值的客户端启动时被强制升级
-- latestVersion 仅作为展示用途；实际下载包由 latest.yml 决定
-- 初始值设为 1.0.4（= 当前线上最新版），不触发强制升级
INSERT INTO system_settings (setting_key, setting_value, updated_at) VALUES
  ('client.min_version',    '1.0.4', NOW(3)),
  ('client.latest_version', '1.0.4', NOW(3))
ON DUPLICATE KEY UPDATE updated_at = updated_at;
