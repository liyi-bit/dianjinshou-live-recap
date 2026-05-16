-- 客户端/服务端错误上报日志表
-- 用途：集中收集桌面主进程 / 渲染进程 / 后端业务错误，方便定位问题
-- 保留策略：默认 90 天（清理 job 未来版本加）
CREATE TABLE IF NOT EXISTS error_logs (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id         BIGINT NULL,
  org_id          BIGINT NULL,
  level           VARCHAR(16)  NOT NULL COMMENT 'error|warn|fatal',
  scope           VARCHAR(64)  NOT NULL COMMENT 'asr|ai|record|updater|main|renderer|api',
  source          VARCHAR(32)  NOT NULL COMMENT 'desktop-main|desktop-renderer|server',
  client_version  VARCHAR(32)  NULL,
  platform        VARCHAR(64)  NULL,
  user_agent      TEXT         NULL,
  message         VARCHAR(1024) NOT NULL,
  stack           TEXT         NULL,
  recording_id    BIGINT       NULL,
  task_id         BIGINT       NULL,
  model_version   VARCHAR(64)  NULL,
  details         JSON         NULL COMMENT '任意结构化上下文（filePath/modelConfig/httpStatus 等）',
  breadcrumbs     JSON         NULL COMMENT '最近 50 条面包屑（route/click/api 调用）',
  ip              VARCHAR(64)  NULL,
  occurred_at     DATETIME(3)  NOT NULL,
  received_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_user_time     (user_id, occurred_at),
  KEY idx_level_scope   (level, scope),
  KEY idx_client        (client_version),
  KEY idx_occurred      (occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='客户端/服务端错误上报日志';
