-- Add 'ai_processing' and 'asr_processing' to recordings.analysis_status enum
ALTER TABLE recordings
    MODIFY COLUMN analysis_status ENUM('none','pending','processing','asr_processing','ai_processing','completed','diagnosed','failed')
    COLLATE utf8mb4_unicode_ci DEFAULT 'none';
