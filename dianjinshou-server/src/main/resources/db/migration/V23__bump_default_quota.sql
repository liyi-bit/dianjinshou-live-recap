-- 默认配额上调：AI 字数 5000 万、分析时长 600 小时
UPDATE users SET ai_quota_total = 50000000 WHERE ai_quota_total < 50000000;
UPDATE users SET duration_quota_total = 2160000 WHERE duration_quota_total < 2160000;
