-- 用户的飞书机器人（每用户可配多个机器人，每个独立一条长连接）
CREATE TABLE IF NOT EXISTS user_feishu_bots (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL COMMENT '所属用户',
  app_id VARCHAR(64) NOT NULL UNIQUE COMMENT '飞书应用 AppId',
  app_secret VARCHAR(128) NOT NULL COMMENT '飞书应用 AppSecret（明文存，调 SDK 需要）',
  bot_name VARCHAR(64) COMMENT '展示名（用户自定义）',
  status TINYINT DEFAULT 1 COMMENT '0=禁用 1=启用',
  last_connected_at DATETIME NULL COMMENT '最近一次 ws 建连成功时间',
  last_error VARCHAR(255) NULL COMMENT '最近一次连接/消息处理错误',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_user (user_id),
  INDEX idx_app_id (app_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户的飞书机器人配置';
