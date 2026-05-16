-- 每个主播独立的"录制完自动走 AI 分析"开关，默认 true（兼容现状）
-- 关闭后录制仍会转 MP4，但不会跑 ASR / 写 transcript / 触发后端 AI 分析
ALTER TABLE streamers
    ADD COLUMN auto_ai_analysis TINYINT(1) NOT NULL DEFAULT 1 COMMENT '录制完成后是否自动进行 AI 分析：1=是 0=否'
    AFTER is_monitoring;
