-- V35: business cloud space sync foundation

ALTER TABLE streamers
    ADD COLUMN cloud_sync_enabled TINYINT NOT NULL DEFAULT 0 COMMENT 'Whether future recordings for this streamer sync to cloud space' AFTER auto_ai_analysis;

ALTER TABLE cloud_files
    ADD COLUMN business_type VARCHAR(32) NULL COMMENT 'full_recap/clip_recap/full_comparison/clip_comparison' AFTER file_type,
    ADD COLUMN business_id BIGINT NULL COMMENT 'Source business record id' AFTER business_type,
    ADD COLUMN recording_id BIGINT NULL COMMENT 'Related recording id' AFTER business_id,
    ADD COLUMN clip_id BIGINT NULL COMMENT 'Related clip id' AFTER recording_id,
    ADD COLUMN comparison_id BIGINT NULL COMMENT 'Related comparison id' AFTER clip_id,
    ADD COLUMN streamer_id BIGINT NULL COMMENT 'Related streamer id' AFTER comparison_id,
    ADD COLUMN anchor_name VARCHAR(128) NULL COMMENT 'Anchor display name' AFTER streamer_id,
    ADD COLUMN industry_id BIGINT NULL COMMENT 'Related industry id' AFTER anchor_name,
    ADD COLUMN account_type VARCHAR(32) NULL COMMENT 'own/industry/competitor' AFTER industry_id,
    ADD COLUMN upload_account VARCHAR(128) NULL COMMENT 'Upload account display name' AFTER account_type,
    ADD COLUMN recorded_at DATETIME NULL COMMENT 'Recording start time' AFTER upload_account,
    ADD COLUMN duration_seconds INT NULL COMMENT 'Video duration seconds' AFTER recorded_at,
    ADD COLUMN display_name VARCHAR(256) NULL COMMENT 'Cloud display file name' AFTER duration_seconds,
    ADD COLUMN local_exists TINYINT NOT NULL DEFAULT 1 COMMENT 'Whether local source still exists' AFTER display_name,
    ADD COLUMN readonly_restored TINYINT NOT NULL DEFAULT 0 COMMENT 'Whether restored as local readonly source' AFTER local_exists,
    ADD COLUMN upload_progress INT NOT NULL DEFAULT 100 COMMENT 'Upload progress 0-100' AFTER readonly_restored,
    ADD INDEX idx_cloud_user_business (user_id, business_type, business_id),
    ADD INDEX idx_cloud_user_status (user_id, status),
    ADD INDEX idx_cloud_recorded_at (recorded_at),
    ADD INDEX idx_cloud_streamer (streamer_id);

ALTER TABLE upload_tasks
    ADD COLUMN business_type VARCHAR(32) NULL COMMENT 'full_recap/clip_recap/full_comparison/clip_comparison' AFTER storage_key,
    ADD COLUMN business_id BIGINT NULL COMMENT 'Source business record id' AFTER business_type,
    ADD COLUMN local_file_path VARCHAR(1024) NULL COMMENT 'Local file path for resume' AFTER business_id,
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0 COMMENT 'Automatic retry count' AFTER status,
    ADD COLUMN next_retry_at DATETIME NULL COMMENT 'Next retry time' AFTER retry_count,
    ADD COLUMN last_error TEXT NULL COMMENT 'Last upload error' AFTER next_retry_at,
    ADD COLUMN progress INT NOT NULL DEFAULT 0 COMMENT 'Upload progress 0-100' AFTER last_error,
    ADD COLUMN client_task_id VARCHAR(64) NULL COMMENT 'Desktop persistent queue id' AFTER progress,
    ADD INDEX idx_upload_user_business (user_id, business_type, business_id),
    ADD INDEX idx_upload_next_retry (status, next_retry_at),
    ADD INDEX idx_upload_client_task (client_task_id);
