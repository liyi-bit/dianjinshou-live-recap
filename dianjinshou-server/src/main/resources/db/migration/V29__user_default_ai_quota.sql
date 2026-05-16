-- 免费配额：没有配置自己密钥的用户，最多可以用默认密钥完成 5 个视频的 AI 解析。
-- 超限后所有 AI/ASR 相关入口都拦下，提示去"设置→第三方接入"配置自己的密钥。
-- 每次 analysis_tasks AI 分析成功完成 +1；有自己密钥的用户不计数。

ALTER TABLE users
  ADD COLUMN default_ai_used INT NOT NULL DEFAULT 0
  COMMENT '使用默认密钥完成的 AI 视频解析次数（上限 5；有自己密钥后不再增加）';
