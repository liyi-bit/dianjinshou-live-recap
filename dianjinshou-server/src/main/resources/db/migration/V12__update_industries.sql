-- V12: 更新行业字典 — 对标抖音电商/爱复盘行业分类体系
-- 清空旧数据，重新插入完整行业列表

DELETE FROM industries;
ALTER TABLE industries AUTO_INCREMENT = 1;

-- ========== 一级行业（34个） ==========
INSERT INTO industries (id, name, parent_id, level, code, is_system, sort_order) VALUES
-- 服饰类
(1,  '女装',       NULL, 1, 'women_clothing',    1, 1),
(2,  '男装',       NULL, 1, 'men_clothing',      1, 2),
(3,  '童装',       NULL, 1, 'kids_clothing',     1, 3),
(4,  '内衣',       NULL, 1, 'underwear',         1, 4),
(5,  '鞋靴',       NULL, 1, 'shoes',             1, 5),
(6,  '箱包',       NULL, 1, 'bags',              1, 6),
(7,  '珠宝饰品',   NULL, 1, 'jewelry',           1, 7),
-- 美妆个护
(8,  '美妆',       NULL, 1, 'makeup',            1, 8),
(9,  '护肤',       NULL, 1, 'skincare',          1, 9),
(10, '个护家清',   NULL, 1, 'personal_care',     1, 10),
-- 食品
(11, '食品',       NULL, 1, 'food',              1, 11),
(12, '生鲜',       NULL, 1, 'fresh',             1, 12),
(13, '茗茶',       NULL, 1, 'tea',               1, 13),
(14, '酒水饮料',   NULL, 1, 'drinks',            1, 14),
(15, '滋补膳食',   NULL, 1, 'health_food',       1, 15),
-- 家居生活
(16, '家居日用',   NULL, 1, 'home_daily',        1, 16),
(17, '家纺布艺',   NULL, 1, 'home_textile',      1, 17),
(18, '家装建材',   NULL, 1, 'home_decor',        1, 18),
(19, '厨具餐具',   NULL, 1, 'kitchen',           1, 19),
(20, '家电',       NULL, 1, 'appliance',         1, 20),
-- 数码
(21, '手机数码',   NULL, 1, 'digital',           1, 21),
(22, '电脑办公',   NULL, 1, 'computer',          1, 22),
-- 母婴运动
(23, '母婴',       NULL, 1, 'baby',              1, 23),
(24, '运动户外',   NULL, 1, 'sports',            1, 24),
-- 文化教育
(25, '图书',       NULL, 1, 'books',             1, 25),
(26, '教育培训',   NULL, 1, 'education',         1, 26),
(27, '知识付费',   NULL, 1, 'knowledge',         1, 27),
-- 其他行业
(28, '宠物',       NULL, 1, 'pets',              1, 28),
(29, '鲜花园艺',   NULL, 1, 'flowers',           1, 29),
(30, '医疗健康',   NULL, 1, 'health',            1, 30),
(31, '汽车',       NULL, 1, 'auto',              1, 31),
(32, '文玩',       NULL, 1, 'collectibles',      1, 32),
(33, '本地生活',   NULL, 1, 'local_life',        1, 33),
(34, '其他',       NULL, 1, 'other',             1, 99);

-- ========== 二级行业 ==========
INSERT INTO industries (name, parent_id, level, code, is_system, sort_order) VALUES
-- 女装
('连衣裙',    1, 2, 'women_dress',        1, 1),
('T恤衬衫',   1, 2, 'women_shirt',        1, 2),
('裤装',      1, 2, 'women_pants',        1, 3),
('外套',      1, 2, 'women_coat',         1, 4),
('大码女装',  1, 2, 'women_plus',         1, 5),
('中老年女装',1, 2, 'women_elderly',      1, 6),

-- 男装
('T恤polo',   2, 2, 'men_tshirt',         1, 1),
('裤装',      2, 2, 'men_pants',          1, 2),
('外套夹克',  2, 2, 'men_jacket',         1, 3),
('衬衫',      2, 2, 'men_shirt',          1, 4),

-- 童装
('婴幼儿服饰',3, 2, 'kids_infant',        1, 1),
('儿童服饰',  3, 2, 'kids_children',      1, 2),
('亲子装',    3, 2, 'kids_family',        1, 3),

-- 内衣
('文胸',      4, 2, 'underwear_bra',      1, 1),
('内裤',      4, 2, 'underwear_brief',    1, 2),
('保暖内衣',  4, 2, 'underwear_thermal',  1, 3),
('家居服',    4, 2, 'underwear_homewear', 1, 4),
('袜子',      4, 2, 'underwear_socks',    1, 5),

-- 鞋靴
('女鞋',      5, 2, 'shoes_women',        1, 1),
('男鞋',      5, 2, 'shoes_men',          1, 2),
('运动鞋',    5, 2, 'shoes_sport',        1, 3),
('童鞋',      5, 2, 'shoes_kids',         1, 4),
('靴子',      5, 2, 'shoes_boots',        1, 5),

-- 箱包
('女包',      6, 2, 'bags_women',         1, 1),
('男包',      6, 2, 'bags_men',           1, 2),
('行李箱',    6, 2, 'bags_luggage',       1, 3),
('功能包',    6, 2, 'bags_functional',    1, 4),

-- 珠宝饰品
('黄金珠宝',  7, 2, 'jewelry_gold',       1, 1),
('翡翠玉石',  7, 2, 'jewelry_jade',       1, 2),
('银饰',      7, 2, 'jewelry_silver',     1, 3),
('时尚配饰',  7, 2, 'jewelry_fashion',    1, 4),
('手表',      7, 2, 'jewelry_watch',      1, 5),

-- 美妆
('底妆',      8, 2, 'makeup_base',        1, 1),
('唇妆',      8, 2, 'makeup_lip',         1, 2),
('眼妆',      8, 2, 'makeup_eye',         1, 3),
('美妆工具',  8, 2, 'makeup_tools',       1, 4),
('香水',      8, 2, 'makeup_perfume',     1, 5),

-- 护肤
('面部护肤',  9, 2, 'skincare_face',      1, 1),
('面膜',      9, 2, 'skincare_mask',      1, 2),
('防晒',      9, 2, 'skincare_sunscreen', 1, 3),
('身体护理',  9, 2, 'skincare_body',      1, 4),
('美容仪器',  9, 2, 'skincare_device',    1, 5),

-- 个护家清
('洗发护发',  10, 2, 'care_hair',         1, 1),
('口腔护理',  10, 2, 'care_oral',         1, 2),
('纸品湿巾',  10, 2, 'care_tissue',       1, 3),
('家庭清洁',  10, 2, 'care_cleaning',     1, 4),
('驱蚊驱虫',  10, 2, 'care_repellent',    1, 5),

-- 食品
('零食糕点',  11, 2, 'food_snack',        1, 1),
('方便速食',  11, 2, 'food_instant',      1, 2),
('粮油调味',  11, 2, 'food_staple',       1, 3),
('坚果炒货',  11, 2, 'food_nuts',         1, 4),
('肉类卤味',  11, 2, 'food_meat',         1, 5),

-- 生鲜
('水果',      12, 2, 'fresh_fruit',       1, 1),
('蔬菜',      12, 2, 'fresh_vegetable',   1, 2),
('海鲜水产',  12, 2, 'fresh_seafood',     1, 3),

-- 茗茶
('绿茶',      13, 2, 'tea_green',         1, 1),
('红茶',      13, 2, 'tea_black',         1, 2),
('白茶',      13, 2, 'tea_white',         1, 3),
('普洱',      13, 2, 'tea_puer',          1, 4),
('花果茶',    13, 2, 'tea_herbal',        1, 5),

-- 酒水饮料
('白酒',      14, 2, 'drinks_baijiu',     1, 1),
('红酒',      14, 2, 'drinks_wine',       1, 2),
('啤酒',      14, 2, 'drinks_beer',       1, 3),
('饮料冲调',  14, 2, 'drinks_beverage',   1, 4),
('咖啡',      14, 2, 'drinks_coffee',     1, 5),

-- 滋补膳食
('蜂蜜',      15, 2, 'health_food_honey', 1, 1),
('燕窝',      15, 2, 'health_food_nest',  1, 2),
('枸杞参类',  15, 2, 'health_food_herbs', 1, 3),
('保健品',    15, 2, 'health_food_supp',  1, 4),

-- 家居日用
('收纳整理',  16, 2, 'home_storage',      1, 1),
('日用百货',  16, 2, 'home_daily_use',    1, 2),
('居家装饰',  16, 2, 'home_decoration',   1, 3),

-- 家纺布艺
('床品四件套',17, 2, 'textile_bedding',   1, 1),
('毛巾浴巾',  17, 2, 'textile_towel',     1, 2),
('窗帘',      17, 2, 'textile_curtain',   1, 3),
('抱枕坐垫',  17, 2, 'textile_cushion',   1, 4),

-- 家装建材
('灯具',      18, 2, 'decor_lighting',    1, 1),
('卫浴',      18, 2, 'decor_bathroom',    1, 2),
('五金工具',  18, 2, 'decor_hardware',    1, 3),

-- 厨具餐具
('锅具',      19, 2, 'kitchen_pot',       1, 1),
('餐具',      19, 2, 'kitchen_tableware', 1, 2),
('水杯水壶',  19, 2, 'kitchen_cup',       1, 3),
('厨房小电器',19, 2, 'kitchen_gadget',    1, 4),

-- 家电
('大家电',    20, 2, 'appliance_large',   1, 1),
('小家电',    20, 2, 'appliance_small',   1, 2),
('个护电器',  20, 2, 'appliance_personal',1, 3),

-- 手机数码
('手机',      21, 2, 'digital_phone',     1, 1),
('平板',      21, 2, 'digital_tablet',    1, 2),
('耳机音箱',  21, 2, 'digital_audio',     1, 3),
('智能穿戴',  21, 2, 'digital_wearable',  1, 4),
('摄影器材',  21, 2, 'digital_camera',    1, 5),

-- 电脑办公
('电脑整机',  22, 2, 'computer_pc',       1, 1),
('电脑配件',  22, 2, 'computer_parts',    1, 2),
('办公用品',  22, 2, 'computer_office',   1, 3),

-- 母婴
('奶粉辅食',  23, 2, 'baby_food',         1, 1),
('纸尿裤',    23, 2, 'baby_diaper',       1, 2),
('玩具',      23, 2, 'baby_toys',         1, 3),
('孕产用品',  23, 2, 'baby_maternity',    1, 4),

-- 运动户外
('健身器材',  24, 2, 'sports_fitness',    1, 1),
('运动服饰',  24, 2, 'sports_wear',       1, 2),
('户外装备',  24, 2, 'sports_outdoor',    1, 3),
('瑜伽用品',  24, 2, 'sports_yoga',       1, 4),
('骑行装备',  24, 2, 'sports_cycling',    1, 5),

-- 图书
('少儿图书',  25, 2, 'books_kids',        1, 1),
('成人图书',  25, 2, 'books_adult',       1, 2),
('考试教辅',  25, 2, 'books_exam',        1, 3),

-- 教育培训
('学科辅导',  26, 2, 'edu_tutoring',      1, 1),
('职业技能',  26, 2, 'edu_skill',         1, 2),
('兴趣培训',  26, 2, 'edu_hobby',         1, 3),

-- 知识付费
('财商教育',  27, 2, 'knowledge_finance', 1, 1),
('心理咨询',  27, 2, 'knowledge_psych',   1, 2),
('语言培训',  27, 2, 'knowledge_lang',    1, 3),
('在线课程',  27, 2, 'knowledge_course',  1, 4),

-- 宠物
('宠物食品',  28, 2, 'pets_food',         1, 1),
('宠物用品',  28, 2, 'pets_supply',       1, 2),
('宠物服饰',  28, 2, 'pets_wear',         1, 3),

-- 鲜花园艺
('鲜切花',    29, 2, 'flowers_cut',       1, 1),
('绿植盆栽',  29, 2, 'flowers_plant',     1, 2),
('园艺工具',  29, 2, 'flowers_tools',     1, 3),

-- 医疗健康
('中医养生',  30, 2, 'health_tcm',        1, 1),
('医疗器械',  30, 2, 'health_device',     1, 2),
('营养保健',  30, 2, 'health_nutrition',  1, 3),

-- 汽车
('车品配件',  31, 2, 'auto_parts',        1, 1),
('车载电器',  31, 2, 'auto_electronics',  1, 2),
('汽车养护',  31, 2, 'auto_care',         1, 3),

-- 文玩
('手串念珠',  32, 2, 'collect_beads',     1, 1),
('紫砂陶瓷',  32, 2, 'collect_pottery',   1, 2),
('字画古玩',  32, 2, 'collect_antique',   1, 3),

-- 本地生活
('餐饮美食',  33, 2, 'local_food',        1, 1),
('休闲娱乐',  33, 2, 'local_leisure',     1, 2),
('丽人美发',  33, 2, 'local_beauty',      1, 3),

-- 其他
('综合',      34, 2, 'other_general',     1, 1);
