-- V9: Share links table

CREATE TABLE IF NOT EXISTS `share_links` (
  `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id`         BIGINT NOT NULL,
  `org_id`          BIGINT NOT NULL,
  `cloud_file_id`   BIGINT NOT NULL,
  `share_code`      VARCHAR(8) NOT NULL,
  `password`        VARCHAR(10) DEFAULT NULL COMMENT '4位密码，可选',
  `expires_at`      DATETIME DEFAULT NULL,
  `max_downloads`   INT DEFAULT NULL,
  `download_count`  INT DEFAULT 0,
  `view_count`      INT DEFAULT 0,
  `status`          VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT 'active/expired/disabled',
  `created_at`      DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE INDEX `idx_sl_code` (`share_code`),
  INDEX `idx_sl_user` (`user_id`),
  INDEX `idx_sl_file` (`cloud_file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分享链接';
