-- 动态配置：覆盖 application.yml 中的部分键值，支持运行时修改
CREATE TABLE IF NOT EXISTS system_settings (
    setting_key   VARCHAR(128) NOT NULL PRIMARY KEY COMMENT '配置键，如 ai.yunwu.api_key',
    setting_value TEXT         NULL                 COMMENT '配置值（明文存储；前端展示时掩码）',
    updated_by    BIGINT       NULL                 COMMENT '最后修改人 user.id',
    updated_at    DATETIME(3)  DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动态配置（第三方接入凭据等）';
