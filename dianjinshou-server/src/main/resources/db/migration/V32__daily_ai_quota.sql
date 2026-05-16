-- 每日 AI 复盘限额：每用户 10 次/天，Asia/Shanghai 本地零点重置
-- ai_quota_unlimited = 1 的用户不受此限（白名单）
ALTER TABLE users
  ADD COLUMN daily_ai_used        INT         NOT NULL DEFAULT 0
    COMMENT '今日 AI 复盘已消耗次数',
  ADD COLUMN daily_ai_reset_at    DATETIME    NULL
    COMMENT '下一次重置时间（本地零点）。next check 时 < NOW 则归零',
  ADD COLUMN ai_quota_unlimited   TINYINT(1)  NOT NULL DEFAULT 0
    COMMENT '是否豁免每日限额（1=不限；0=受 10 次/天限制）';

-- 所有现有用户初始化重置时间为"明天零点"（北京时间）
-- 这样今天开始就是完整一天的 10 次额度
UPDATE users
   SET daily_ai_reset_at = DATE_ADD(CURDATE(), INTERVAL 1 DAY)
 WHERE daily_ai_reset_at IS NULL;
