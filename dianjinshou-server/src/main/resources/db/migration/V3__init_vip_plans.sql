-- VIP 套餐初始数据
INSERT INTO vip_plans (name, level, duration_days, price, ai_quota, max_rooms, max_members, features, is_active, sort_order)
VALUES
    ('免费版', 0, 36500, 0.00, 500000, 1, 1, '["基础AI分析","单路录制","50万字额度"]', 1, 0),
    ('企业版', 3, 365, 2999.00, 5000000, 10, 20, '["全维度AI分析","10路并发录制","500万字额度","20个子账号","优先客服"]', 1, 1);
