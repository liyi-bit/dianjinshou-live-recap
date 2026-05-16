-- V8: Sensitive word library seed data (representative samples per category)
-- Full 100k+ word library should be imported via CSV through the admin import API

-- 涉政类 (political)
INSERT INTO `sensitive_word_library` (`word`, `category`, `risk_level`, `replacement_suggestion`, `platform`, `source`, `is_active`) VALUES
('国家领导人', '涉政', 3, NULL, 'all', 'system', 1),
('政治敏感', '涉政', 3, NULL, 'all', 'system', 1),
('颠覆政权', '涉政', 3, NULL, 'all', 'system', 1),
('分裂国家', '涉政', 3, NULL, 'all', 'system', 1),
('反华势力', '涉政', 3, NULL, 'all', 'system', 1),
('暴力恐怖', '涉政', 3, NULL, 'all', 'system', 1),
('邪教组织', '涉政', 3, NULL, 'all', 'system', 1),
('煽动闹事', '涉政', 3, NULL, 'all', 'system', 1),
('境外势力', '涉政', 3, NULL, 'all', 'system', 1),
('反动言论', '涉政', 3, NULL, 'all', 'system', 1);

-- 涉黄类 (pornographic)
INSERT INTO `sensitive_word_library` (`word`, `category`, `risk_level`, `replacement_suggestion`, `platform`, `source`, `is_active`) VALUES
('色情内容', '涉黄', 3, NULL, 'all', 'system', 1),
('裸露', '涉黄', 2, NULL, 'all', 'system', 1),
('性暗示', '涉黄', 2, NULL, 'all', 'system', 1),
('低俗表演', '涉黄', 2, '才艺表演', 'all', 'system', 1),
('擦边球', '涉黄', 2, NULL, 'all', 'system', 1),
('软色情', '涉黄', 2, NULL, 'all', 'system', 1),
('性感诱惑', '涉黄', 2, '时尚穿搭', 'all', 'system', 1),
('约炮', '涉黄', 3, NULL, 'all', 'system', 1),
('一夜情', '涉黄', 3, NULL, 'all', 'system', 1),
('情色', '涉黄', 3, NULL, 'all', 'system', 1);

-- 违禁品类 (prohibited goods)
INSERT INTO `sensitive_word_library` (`word`, `category`, `risk_level`, `replacement_suggestion`, `platform`, `source`, `is_active`) VALUES
('枪支弹药', '违禁品', 3, NULL, 'all', 'system', 1),
('毒品', '违禁品', 3, NULL, 'all', 'system', 1),
('冰毒', '违禁品', 3, NULL, 'all', 'system', 1),
('大麻', '违禁品', 3, NULL, 'all', 'system', 1),
('走私', '违禁品', 3, NULL, 'all', 'system', 1),
('假币', '违禁品', 3, NULL, 'all', 'system', 1),
('管制刀具', '违禁品', 3, NULL, 'all', 'system', 1),
('窃听器', '违禁品', 2, NULL, 'all', 'system', 1),
('迷药', '违禁品', 3, NULL, 'all', 'system', 1),
('仿真枪', '违禁品', 2, NULL, 'all', 'system', 1);

-- 虚假宣传类 (false advertising)
INSERT INTO `sensitive_word_library` (`word`, `category`, `risk_level`, `replacement_suggestion`, `platform`, `source`, `is_active`) VALUES
('全网最低价', '虚假宣传', 2, '优惠价格', 'all', 'system', 1),
('史上最便宜', '虚假宣传', 2, '超值优惠', 'all', 'system', 1),
('假一赔万', '虚假宣传', 2, '品质保障', 'all', 'system', 1),
('100%有效', '虚假宣传', 2, '效果显著', 'all', 'system', 1),
('包治百病', '虚假宣传', 3, '辅助改善', 'all', 'system', 1),
('祖传秘方', '虚假宣传', 2, '传统配方', 'all', 'system', 1),
('无副作用', '虚假宣传', 2, '温和配方', 'all', 'system', 1),
('药到病除', '虚假宣传', 3, '改善症状', 'all', 'system', 1),
('国家认证', '虚假宣传', 2, '检测合格', 'all', 'system', 1),
('央视推荐', '虚假宣传', 2, '品牌推荐', 'all', 'system', 1),
('一针见效', '虚假宣传', 3, '快速起效', 'all', 'system', 1),
('永不反弹', '虚假宣传', 2, '长效作用', 'all', 'system', 1),
('纯天然无添加', '虚假宣传', 1, '天然成分', 'all', 'system', 1),
('不含防腐剂', '虚假宣传', 1, '配方温和', 'all', 'system', 1),
('零风险投资', '虚假宣传', 3, '投资有风险', 'all', 'system', 1);

-- 绝对化用语类 (absolute terms)
INSERT INTO `sensitive_word_library` (`word`, `category`, `risk_level`, `replacement_suggestion`, `platform`, `source`, `is_active`) VALUES
('最好的', '绝对化用语', 1, '优质的', 'all', 'system', 1),
('第一', '绝对化用语', 1, '领先', 'all', 'system', 1),
('最佳', '绝对化用语', 1, '优秀', 'all', 'system', 1),
('最优', '绝对化用语', 1, '更优', 'all', 'system', 1),
('独一无二', '绝对化用语', 1, '独特', 'all', 'system', 1),
('绝无仅有', '绝对化用语', 1, '稀有', 'all', 'system', 1),
('国家级', '绝对化用语', 2, '高品质', 'all', 'system', 1),
('世界级', '绝对化用语', 2, '高端', 'all', 'system', 1),
('顶级', '绝对化用语', 1, '高端', 'all', 'system', 1),
('极致', '绝对化用语', 1, '出色', 'all', 'system', 1),
('万能', '绝对化用语', 2, '多功能', 'all', 'system', 1),
('首选', '绝对化用语', 1, '推荐', 'all', 'system', 1),
('唯一', '绝对化用语', 1, '特有', 'all', 'system', 1),
('最高级', '绝对化用语', 1, '高端', 'all', 'system', 1),
('全球领先', '绝对化用语', 2, '行业领先', 'all', 'system', 1);

-- 引导场外类 (off-platform guidance)
INSERT INTO `sensitive_word_library` (`word`, `category`, `risk_level`, `replacement_suggestion`, `platform`, `source`, `is_active`) VALUES
('加微信', '引导场外', 2, NULL, 'douyin', 'system', 1),
('加我微信', '引导场外', 2, NULL, 'douyin', 'system', 1),
('微信号', '引导场外', 2, NULL, 'douyin', 'system', 1),
('加V', '引导场外', 2, NULL, 'all', 'system', 1),
('私聊我', '引导场外', 1, '评论区留言', 'all', 'system', 1),
('私信我', '引导场外', 1, '评论区留言', 'douyin', 'system', 1),
('场外交易', '引导场外', 3, NULL, 'all', 'system', 1),
('线下交易', '引导场外', 2, NULL, 'all', 'system', 1),
('直接转账', '引导场外', 2, '平台下单', 'all', 'system', 1),
('闲鱼搜', '引导场外', 2, NULL, 'douyin', 'system', 1);

-- 其他类 (others)
INSERT INTO `sensitive_word_library` (`word`, `category`, `risk_level`, `replacement_suggestion`, `platform`, `source`, `is_active`) VALUES
('刷单', '其他', 2, NULL, 'all', 'system', 1),
('好评返现', '其他', 2, NULL, 'all', 'system', 1),
('互刷', '其他', 2, NULL, 'all', 'system', 1),
('刷好评', '其他', 2, NULL, 'all', 'system', 1),
('删差评', '其他', 2, NULL, 'all', 'system', 1),
('水军', '其他', 2, NULL, 'all', 'system', 1),
('买粉', '其他', 2, NULL, 'all', 'system', 1),
('买赞', '其他', 2, NULL, 'all', 'system', 1),
('代拍', '其他', 1, NULL, 'all', 'system', 1),
('黄牛', '其他', 1, NULL, 'all', 'system', 1);

-- Platform-specific: 快手
INSERT INTO `sensitive_word_library` (`word`, `category`, `risk_level`, `replacement_suggestion`, `platform`, `source`, `is_active`) VALUES
('去抖音看', '引导场外', 2, NULL, 'kuaishou', 'system', 1),
('抖音搜索', '引导场外', 2, NULL, 'kuaishou', 'system', 1);

-- Platform-specific: 视频号
INSERT INTO `sensitive_word_library` (`word`, `category`, `risk_level`, `replacement_suggestion`, `platform`, `source`, `is_active`) VALUES
('去抖音看', '引导场外', 2, NULL, 'shipinhao', 'system', 1),
('快手搜索', '引导场外', 2, NULL, 'shipinhao', 'system', 1);
