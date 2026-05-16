-- V4: 云存储相关表 (v2.0)

-- 分片上传任务表
CREATE TABLE IF NOT EXISTS upload_tasks (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    org_id          BIGINT       NOT NULL,
    file_name       VARCHAR(256) NOT NULL,
    file_size       BIGINT       NOT NULL COMMENT '文件总大小(bytes)',
    content_type    VARCHAR(128) NOT NULL,
    bucket          VARCHAR(64)  NOT NULL,
    storage_key     VARCHAR(512) NOT NULL,
    total_parts     INT          NOT NULL,
    uploaded_parts  INT          NOT NULL DEFAULT 0,
    status          VARCHAR(20)  NOT NULL DEFAULT 'init' COMMENT 'init/uploading/completed/cancelled/failed',
    expires_at      DATETIME     NOT NULL COMMENT '上传任务过期时间',
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_user (user_id),
    INDEX idx_org (org_id),
    INDEX idx_status (status),
    INDEX idx_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分片上传任务';

-- 云端文件元数据表
CREATE TABLE IF NOT EXISTS cloud_files (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    org_id          BIGINT       NOT NULL,
    file_name       VARCHAR(256) NOT NULL,
    storage_key     VARCHAR(512) NOT NULL,
    bucket          VARCHAR(64)  NOT NULL,
    file_size       BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小(bytes)',
    content_type    VARCHAR(128) NOT NULL DEFAULT '',
    file_type       VARCHAR(32)  NOT NULL COMMENT 'recording/clip/analysis/document',
    source_id       BIGINT       NULL     COMMENT '关联录像/任务ID',
    checksum        VARCHAR(64)  NULL     COMMENT 'MD5',
    download_count  INT          NOT NULL DEFAULT 0,
    share_count     INT          NOT NULL DEFAULT 0,
    status          VARCHAR(20)  NOT NULL DEFAULT 'active' COMMENT 'active/archived/deleted',
    deleted         TINYINT      NOT NULL DEFAULT 0,
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    INDEX idx_user (user_id),
    INDEX idx_org (org_id),
    INDEX idx_file_type (file_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='云端文件元数据';

-- 文件分析任务表
CREATE TABLE IF NOT EXISTS file_analysis_tasks (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT        NOT NULL,
    org_id          BIGINT        NOT NULL,
    file_name       VARCHAR(256)  NOT NULL,
    storage_key     VARCHAR(512)  NOT NULL,
    file_size       BIGINT        NOT NULL DEFAULT 0,
    duration        INT           NULL     COMMENT '视频时长(秒)',
    industry_id     BIGINT        NULL,
    ai_model        VARCHAR(32)   NULL DEFAULT 'doubao',
    asr_text        LONGTEXT      NULL,
    status          VARCHAR(32)   NOT NULL DEFAULT 'pending' COMMENT 'pending/uploading/asr_processing/ai_processing/completed/failed',
    error_msg       TEXT          NULL,
    deleted         TINYINT       NOT NULL DEFAULT 0,
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    INDEX idx_user (user_id),
    INDEX idx_org (org_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件分析任务';

-- 文案预审表
CREATE TABLE IF NOT EXISTS copywriting_reviews (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT        NOT NULL,
    org_id          BIGINT        NOT NULL,
    text_content    MEDIUMTEXT    NOT NULL,
    industry_id     BIGINT        NULL,
    result          JSON          NULL     COMMENT '检测结果JSON',
    risk_score      INT           NULL     COMMENT '风险评分0-100',
    status          VARCHAR(20)   NOT NULL DEFAULT 'pending' COMMENT 'pending/processing/completed/failed',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_user (user_id),
    INDEX idx_org (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文案预审记录';

-- analysis_tasks 新增 file_analysis_task_id 字段
ALTER TABLE analysis_tasks ADD COLUMN file_analysis_task_id BIGINT NULL COMMENT '关联文件分析任务ID' AFTER parent_task_id;
ALTER TABLE analysis_tasks ADD INDEX idx_file_analysis (file_analysis_task_id);
