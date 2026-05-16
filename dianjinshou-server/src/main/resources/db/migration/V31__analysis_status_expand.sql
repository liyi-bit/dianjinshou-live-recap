-- 扩展 recordings.analysis_status ENUM 加入新状态
--   recording: 视频录制中（预留）
--   transcribing: 本地 ASR 正在转写
--   transcribed: 逐字稿已生成，等待用户手动触发 AI 复盘
-- analysis_tasks.status 是 VARCHAR，应用层扩展枚举即可，不用改 schema
ALTER TABLE recordings
  MODIFY COLUMN analysis_status
  ENUM('none','recording','pending','processing','asr_processing',
       'transcribing','transcribed','ai_processing','completed','diagnosed','failed')
  COLLATE utf8mb4_unicode_ci DEFAULT 'none';
