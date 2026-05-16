SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'streamers' AND COLUMN_NAME = 'sec_uid');
SET @sql := IF(@exist > 0, 'SELECT 1', 'ALTER TABLE streamers ADD COLUMN sec_uid VARCHAR(255) NULL AFTER anchor_avatar');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
