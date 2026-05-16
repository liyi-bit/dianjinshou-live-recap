-- Add missing clip_file_path column to analysis_tasks
ALTER TABLE analysis_tasks ADD COLUMN clip_file_path VARCHAR(500) DEFAULT NULL AFTER clip_filename;
