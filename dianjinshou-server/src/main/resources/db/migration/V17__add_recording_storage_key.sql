-- Add storage_key column for remote file storage (MinIO)
ALTER TABLE recordings ADD COLUMN storage_key VARCHAR(500) DEFAULT NULL AFTER local_file_name;
