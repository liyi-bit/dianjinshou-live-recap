-- V6: Short video topic selection tables

-- 提取文案
CREATE TABLE IF NOT EXISTS `video_copywriting` (
  `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id`         BIGINT NOT NULL,
  `org_id`          BIGINT NOT NULL,
  `source_type`     VARCHAR(20) NOT NULL COMMENT 'url / local / recording',
  `source_url`      VARCHAR(2048) DEFAULT NULL,
  `storage_key`     VARCHAR(512) DEFAULT NULL,
  `title`           VARCHAR(200) DEFAULT NULL,
  `extracted_text`  LONGTEXT DEFAULT NULL,
  `polished_text`   LONGTEXT DEFAULT NULL,
  `word_count`      INT DEFAULT 0,
  `tags`            JSON DEFAULT NULL,
  `status`          VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/asr/polishing/completed/failed',
  `error_msg`       VARCHAR(1000) DEFAULT NULL,
  `copy_count`      INT DEFAULT 0,
  `created_at`      DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`         TINYINT DEFAULT 0,
  INDEX `idx_vc_user` (`user_id`),
  INDEX `idx_vc_org` (`org_id`),
  INDEX `idx_vc_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频文案提取';

-- 达人
CREATE TABLE IF NOT EXISTS `creators` (
  `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
  `platform`        VARCHAR(20) NOT NULL COMMENT 'douyin / kuaishou / shipinhao',
  `creator_id`      VARCHAR(100) NOT NULL COMMENT '平台达人ID',
  `nickname`        VARCHAR(100) NOT NULL,
  `avatar_url`      VARCHAR(1024) DEFAULT NULL,
  `follower_count`  BIGINT DEFAULT 0,
  `video_count`     INT DEFAULT 0,
  `industry`        VARCHAR(50) DEFAULT NULL,
  `description`     VARCHAR(500) DEFAULT NULL,
  `data_snapshot`   JSON DEFAULT NULL,
  `last_synced_at`  DATETIME DEFAULT NULL,
  `created_at`      DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE INDEX `uk_platform_creator` (`platform`, `creator_id`),
  INDEX `idx_cr_industry` (`industry`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='达人信息';

-- 达人视频
CREATE TABLE IF NOT EXISTS `creator_videos` (
  `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
  `creator_id`      BIGINT NOT NULL,
  `video_id`        VARCHAR(100) NOT NULL COMMENT '平台视频ID',
  `title`           VARCHAR(500) DEFAULT NULL,
  `cover_url`       VARCHAR(1024) DEFAULT NULL,
  `play_count`      BIGINT DEFAULT 0,
  `like_count`      BIGINT DEFAULT 0,
  `comment_count`   BIGINT DEFAULT 0,
  `share_count`     BIGINT DEFAULT 0,
  `publish_time`    DATETIME DEFAULT NULL,
  `duration`        INT DEFAULT 0,
  `url`             VARCHAR(2048) DEFAULT NULL,
  `created_at`      DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_cv_creator` (`creator_id`),
  INDEX `idx_cv_play` (`play_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='达人视频';

-- 达人订阅
CREATE TABLE IF NOT EXISTS `creator_subscriptions` (
  `id`                  BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id`             BIGINT NOT NULL,
  `org_id`              BIGINT NOT NULL,
  `creator_id`          BIGINT NOT NULL,
  `notify_on_new_video` TINYINT DEFAULT 1,
  `created_at`          DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted`             TINYINT DEFAULT 0,
  UNIQUE INDEX `uk_user_creator` (`user_id`, `creator_id`),
  INDEX `idx_cs_org` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='达人订阅';

-- 爆款订阅
CREATE TABLE IF NOT EXISTS `trending_subscriptions` (
  `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id`         BIGINT NOT NULL,
  `org_id`          BIGINT NOT NULL,
  `platform`        VARCHAR(20) DEFAULT NULL,
  `industry`        VARCHAR(50) DEFAULT NULL,
  `min_play_count`  BIGINT DEFAULT 100000,
  `min_like_count`  BIGINT DEFAULT 0,
  `keywords`        JSON DEFAULT NULL,
  `notify_enabled`  TINYINT DEFAULT 1,
  `created_at`      DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted`         TINYINT DEFAULT 0,
  INDEX `idx_ts_user` (`user_id`),
  INDEX `idx_ts_org` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='爆款订阅';

-- 订阅提醒
CREATE TABLE IF NOT EXISTS `trending_alerts` (
  `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
  `subscription_id` BIGINT NOT NULL,
  `video_id`        BIGINT DEFAULT NULL,
  `creator_id`      BIGINT DEFAULT NULL,
  `alert_type`      VARCHAR(20) NOT NULL COMMENT 'new_video / trending',
  `is_read`         TINYINT DEFAULT 0,
  `created_at`      DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_ta_sub` (`subscription_id`),
  INDEX `idx_ta_read` (`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订阅提醒';
