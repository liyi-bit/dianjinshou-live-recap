-- flyway:placeholderReplacement=false
-- 后台管理员独立账号表（与业务 users 表分离）

CREATE TABLE IF NOT EXISTS admin_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE COMMENT '登录用户名',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 10轮',
    display_name VARCHAR(64) NULL COMMENT '显示昵称',
    email VARCHAR(128) NULL,
    role ENUM('admin_super','admin_normal') NOT NULL DEFAULT 'admin_normal' COMMENT '后台角色',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
    last_login_at DATETIME NULL,
    last_login_ip VARCHAR(64) NULL,
    failed_login_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台管理员账号';

-- seed: 初始超管 admin / test123456 (BCrypt $2b$10)
INSERT INTO admin_accounts (username, password_hash, display_name, role, status)
VALUES ('admin', '$2b$10$flS7jh0/9UdJ9US5Pwcl4OAzuloNR7uHfZk37l4LcVjv./SnMB8Bm', '超级管理员', 'admin_super', 1);
