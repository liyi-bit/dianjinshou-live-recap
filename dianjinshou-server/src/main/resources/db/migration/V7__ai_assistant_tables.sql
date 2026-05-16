-- V7: AI assistant tables

-- AI 会话
CREATE TABLE IF NOT EXISTS `ai_sessions` (
  `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id`         BIGINT NOT NULL,
  `org_id`          BIGINT NOT NULL,
  `assistant_type`  VARCHAR(20) NOT NULL COMMENT 'operation / compliance / script',
  `title`           VARCHAR(200) DEFAULT NULL,
  `message_count`   INT DEFAULT 0,
  `last_message_at` DATETIME DEFAULT NULL,
  `status`          VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT 'active / archived',
  `created_at`      DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`         TINYINT DEFAULT 0,
  INDEX `idx_as_user` (`user_id`),
  INDEX `idx_as_org` (`org_id`),
  INDEX `idx_as_type` (`assistant_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI会话';

-- Add session_id to ai_conversations
ALTER TABLE `ai_conversations` ADD COLUMN `session_id` BIGINT DEFAULT NULL AFTER `comparison_id`;
ALTER TABLE `ai_conversations` ADD INDEX `idx_ac_session` (`session_id`);

-- 敏感词库
CREATE TABLE IF NOT EXISTS `sensitive_word_library` (
  `id`                      BIGINT AUTO_INCREMENT PRIMARY KEY,
  `word`                    VARCHAR(200) NOT NULL,
  `category`                VARCHAR(30) NOT NULL COMMENT '涉政/涉黄/违禁品/虚假宣传/绝对化用语/引导场外/其他',
  `risk_level`              TINYINT NOT NULL DEFAULT 1 COMMENT '1-3',
  `replacement_suggestion`  VARCHAR(500) DEFAULT NULL,
  `platform`                VARCHAR(20) DEFAULT 'all' COMMENT 'all/douyin/kuaishou/shipinhao',
  `industry`                VARCHAR(50) DEFAULT NULL,
  `source`                  VARCHAR(20) DEFAULT 'system' COMMENT 'system/custom/user',
  `is_active`               TINYINT DEFAULT 1,
  `created_at`              DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_sw_word` (`word`),
  INDEX `idx_sw_category` (`category`),
  INDEX `idx_sw_source` (`source`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词库';

-- 话术模板
CREATE TABLE IF NOT EXISTS `script_templates` (
  `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name`            VARCHAR(50) NOT NULL,
  `description`     VARCHAR(500) DEFAULT NULL,
  `category`        VARCHAR(30) DEFAULT NULL,
  `icon`            VARCHAR(50) DEFAULT NULL,
  `prompt_template` TEXT DEFAULT NULL,
  `input_fields`    JSON DEFAULT NULL,
  `sort_order`      INT DEFAULT 0,
  `is_active`       TINYINT DEFAULT 1,
  `created_at`      DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='话术模板';

-- 话术生成记录
CREATE TABLE IF NOT EXISTS `script_generations` (
  `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id`         BIGINT NOT NULL,
  `org_id`          BIGINT NOT NULL,
  `template_id`     BIGINT NOT NULL,
  `input_params`    JSON DEFAULT NULL,
  `generated_text`  LONGTEXT DEFAULT NULL,
  `ai_model`        VARCHAR(50) DEFAULT NULL,
  `tokens_used`     INT DEFAULT 0,
  `rating`          TINYINT DEFAULT NULL COMMENT '1-5',
  `created_at`      DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted`         TINYINT DEFAULT 0,
  INDEX `idx_sg_user` (`user_id`),
  INDEX `idx_sg_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='话术生成记录';

-- 初始化 9 种话术模板
INSERT INTO `script_templates` (`name`, `description`, `category`, `icon`, `prompt_template`, `input_fields`, `sort_order`) VALUES
('开场白话术', '直播开场白，吸引观众停留', '开场', 'icon-play-arrow', '请为{industry}行业的直播间生成一段开场白话术，产品是{product}，目标人群是{audience}。要求：吸引停留、引发好奇、自然过渡到产品介绍。', '[{"name":"industry","label":"行业","type":"text"},{"name":"product","label":"产品","type":"text"},{"name":"audience","label":"目标人群","type":"text"}]', 1),
('留人话术', '防止观众流失的互动话术', '互动', 'icon-user-group', '请为{industry}行业直播间生成留人话术，当前正在介绍{product}，观众可能因为{reason}想要离开。要求：制造紧迫感、给予期待、增加互动。', '[{"name":"industry","label":"行业","type":"text"},{"name":"product","label":"产品","type":"text"},{"name":"reason","label":"离开原因","type":"text"}]', 2),
('产品介绍话术', '突出产品卖点的介绍话术', '产品', 'icon-gift', '请为{product}生成产品介绍话术，核心卖点是{selling_points}，价格是{price}。要求：FAB法则、痛点引入、场景化描述。', '[{"name":"product","label":"产品名称","type":"text"},{"name":"selling_points","label":"核心卖点","type":"textarea"},{"name":"price","label":"价格","type":"text"}]', 3),
('促单话术', '临门一脚的成交话术', '成交', 'icon-thunderbolt', '请为{product}生成促单话术，当前优惠是{offer}，库存{stock}件。要求：制造稀缺感、限时优惠、从众心理、行动号召。', '[{"name":"product","label":"产品","type":"text"},{"name":"offer","label":"优惠信息","type":"text"},{"name":"stock","label":"库存","type":"text"}]', 4),
('互动话术', '提高直播间互动率的话术', '互动', 'icon-message', '请为{industry}行业直播间生成互动话术，当前话题是{topic}。要求：引导评论、引导点赞、引导关注、提问互动。', '[{"name":"industry","label":"行业","type":"text"},{"name":"topic","label":"当前话题","type":"text"}]', 5),
('粉团引导话术', '引导加入粉丝团的话术', '粉丝', 'icon-star', '请生成引导观众加入粉丝团的话术，粉丝团福利包括{benefits}。要求：强调专属福利、制造身份认同、限时优惠。', '[{"name":"benefits","label":"粉丝团福利","type":"textarea"}]', 6),
('私域引流话术', '引导加微信/社群的话术', '引流', 'icon-share-alt', '请为{industry}行业生成私域引流话术，引导方式是{method}，引流福利是{gift}。要求：合规（不违反平台规则）、自然植入、给予价值。', '[{"name":"industry","label":"行业","type":"text"},{"name":"method","label":"引导方式","type":"text"},{"name":"gift","label":"引流福利","type":"text"}]', 7),
('异议处理话术', '应对观众质疑的话术', '异议', 'icon-question-circle', '请生成应对以下异议的话术：观众质疑"{objection}"，产品是{product}。要求：先认同、再解释、给证据、转化为购买理由。', '[{"name":"objection","label":"观众质疑","type":"text"},{"name":"product","label":"产品","type":"text"}]', 8),
('收尾话术', '直播结束前的话术', '收尾', 'icon-check-circle', '请为{industry}行业直播间生成收尾话术，今日主推{product}，下一场直播时间是{next_time}。要求：感谢观众、预告下次、最后号召关注。', '[{"name":"industry","label":"行业","type":"text"},{"name":"product","label":"主推产品","type":"text"},{"name":"next_time","label":"下次直播时间","type":"text"}]', 9);
