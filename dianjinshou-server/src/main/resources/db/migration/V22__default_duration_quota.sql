-- 给存量用户补默认时长配额（100 小时 = 360000 秒），原本默认 0 导致前端展示 0 小时
UPDATE users SET duration_quota_total = 360000 WHERE duration_quota_total IS NULL OR duration_quota_total = 0;
