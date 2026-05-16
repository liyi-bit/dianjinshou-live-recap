-- V2: 初始化行业字典数据（二级分类）

-- 一级行业
INSERT INTO industries (id, name, parent_id, level, code, is_system, sort_order) VALUES
(1, '知识付费', NULL, 1, 'knowledge', 1, 1),
(2, '美妆护肤', NULL, 1, 'beauty', 1, 2),
(3, '食品饮料', NULL, 1, 'food', 1, 3),
(4, '服饰鞋包', NULL, 1, 'fashion', 1, 4),
(5, '3C数码', NULL, 1, 'digital', 1, 5),
(6, '家居百货', NULL, 1, 'home', 1, 6),
(7, '母婴亲子', NULL, 1, 'baby', 1, 7),
(8, '运动户外', NULL, 1, 'sports', 1, 8),
(9, '珠宝配饰', NULL, 1, 'jewelry', 1, 9),
(10, '图书教育', NULL, 1, 'books', 1, 10),
(11, '医疗健康', NULL, 1, 'health', 1, 11),
(12, '汽车', NULL, 1, 'auto', 1, 12),
(13, '娱乐', NULL, 1, 'entertainment', 1, 13),
(14, '其他', NULL, 1, 'other', 1, 99);

-- 二级行业
INSERT INTO industries (name, parent_id, level, code, is_system, sort_order) VALUES
-- 知识付费
('财商教育', 1, 2, 'knowledge_finance', 1, 1),
('职业技能', 1, 2, 'knowledge_skill', 1, 2),
('语言培训', 1, 2, 'knowledge_language', 1, 3),
('心理咨询', 1, 2, 'knowledge_psychology', 1, 4),

-- 美妆护肤
('彩妆', 2, 2, 'beauty_makeup', 1, 1),
('护肤', 2, 2, 'beauty_skincare', 1, 2),
('个护', 2, 2, 'beauty_personal', 1, 3),
('美容仪器', 2, 2, 'beauty_device', 1, 4),

-- 食品饮料
('零食', 3, 2, 'food_snack', 1, 1),
('生鲜', 3, 2, 'food_fresh', 1, 2),
('酒水饮料', 3, 2, 'food_drink', 1, 3),
('保健食品', 3, 2, 'food_health', 1, 4),

-- 服饰鞋包
('女装', 4, 2, 'fashion_women', 1, 1),
('男装', 4, 2, 'fashion_men', 1, 2),
('鞋靴', 4, 2, 'fashion_shoes', 1, 3),
('箱包', 4, 2, 'fashion_bags', 1, 4),

-- 3C数码
('手机', 5, 2, 'digital_phone', 1, 1),
('电脑', 5, 2, 'digital_computer', 1, 2),
('智能穿戴', 5, 2, 'digital_wearable', 1, 3),
('摄影器材', 5, 2, 'digital_camera', 1, 4),

-- 家居百货
('家具', 6, 2, 'home_furniture', 1, 1),
('日用百货', 6, 2, 'home_daily', 1, 2),
('厨房用品', 6, 2, 'home_kitchen', 1, 3),
('家纺', 6, 2, 'home_textile', 1, 4),

-- 母婴亲子
('奶粉辅食', 7, 2, 'baby_food', 1, 1),
('童装', 7, 2, 'baby_clothes', 1, 2),
('玩具', 7, 2, 'baby_toys', 1, 3),
('孕产', 7, 2, 'baby_maternity', 1, 4),

-- 运动户外
('健身器材', 8, 2, 'sports_fitness', 1, 1),
('运动服饰', 8, 2, 'sports_wear', 1, 2),
('户外装备', 8, 2, 'sports_outdoor', 1, 3),

-- 珠宝配饰
('黄金珠宝', 9, 2, 'jewelry_gold', 1, 1),
('翡翠玉石', 9, 2, 'jewelry_jade', 1, 2),
('时尚配饰', 9, 2, 'jewelry_accessory', 1, 3),

-- 图书教育
('少儿图书', 10, 2, 'books_kids', 1, 1),
('成人图书', 10, 2, 'books_adult', 1, 2),
('在线课程', 10, 2, 'books_course', 1, 3),

-- 医疗健康
('中医养生', 11, 2, 'health_tcm', 1, 1),
('医疗器械', 11, 2, 'health_device', 1, 2),
('营养保健', 11, 2, 'health_nutrition', 1, 3),

-- 汽车
('新车', 12, 2, 'auto_new', 1, 1),
('二手车', 12, 2, 'auto_used', 1, 2),
('汽车用品', 12, 2, 'auto_parts', 1, 3),

-- 娱乐
('游戏', 13, 2, 'entertainment_game', 1, 1),
('音乐', 13, 2, 'entertainment_music', 1, 2),
('才艺', 13, 2, 'entertainment_talent', 1, 3),

-- 其他
('综合', 14, 2, 'other_general', 1, 1);
