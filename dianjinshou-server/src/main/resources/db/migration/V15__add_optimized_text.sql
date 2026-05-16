ALTER TABLE analysis_tasks ADD COLUMN optimized_text LONGTEXT NULL COMMENT 'AI优化后的原文' AFTER content_compass;
