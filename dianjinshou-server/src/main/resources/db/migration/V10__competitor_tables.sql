-- Competitor analysis reports table
CREATE TABLE IF NOT EXISTS competitor_reports (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    org_id      BIGINT       NOT NULL,
    streamer_id BIGINT       NOT NULL COMMENT '当前主播ID',
    competitor_streamer_id BIGINT NOT NULL COMMENT '竞品主播ID',
    recording_id BIGINT      NULL     COMMENT '当前主播录制ID',
    competitor_recording_id BIGINT NULL COMMENT '竞品主播录制ID',
    report      JSON         NULL     COMMENT '对比分析报告JSON',
    ai_model    VARCHAR(50)  NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT 'pending/processing/completed/failed',
    error_msg   VARCHAR(500) NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NULL     ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_competitor_user (user_id),
    INDEX idx_competitor_org (org_id),
    INDEX idx_competitor_streamer (streamer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='竞品分析报告';
