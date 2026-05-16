-- V31 当时以为 analysis_tasks.status 是 VARCHAR，实际是 ENUM（V1 init_schema.sql 里就写死了）。
-- 新的 AnalysisStatus 枚举值（recording/transcribing/transcribed）往里写会被 MySQL truncated
-- 导致 submit-asr 接口 500。这里把 analysis_tasks.status 的 ENUM 补齐。
ALTER TABLE analysis_tasks
  MODIFY COLUMN status
  ENUM('pending','asr_processing','recording','transcribing','transcribed',
       'ai_processing','completed','failed')
  COLLATE utf8mb4_unicode_ci DEFAULT 'pending';
