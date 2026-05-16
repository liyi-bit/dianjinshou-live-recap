-- ============================================================
-- 点金手 v1.0 数据库初始化 — SPEC §4 全部 17 张表
-- MySQL 8.0 · InnoDB · utf8mb4
-- ============================================================

-- 1. organizations
CREATE TABLE IF NOT EXISTS organizations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL COMMENT '组织名称',
    owner_id BIGINT NOT NULL COMMENT '创建者用户ID',
    vip_level TINYINT DEFAULT 0 COMMENT '企业会员等级: 0=免费, 3=企业',
    vip_expire_at DATETIME NULL COMMENT '企业会员到期',
    max_members INT DEFAULT 20 COMMENT '最大成员数,上限20',
    deleted TINYINT(1) DEFAULT 0,
    created_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    INDEX idx_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织/企业';

-- 2. users
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL COMMENT '昵称/显示名',
    phone VARCHAR(20) NOT NULL UNIQUE COMMENT '手机号(登录凭证)',
    email VARCHAR(128) NULL,
    password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt哈希',
    avatar_url VARCHAR(512) NULL,
    role ENUM('super_admin','admin','operator','anchor') DEFAULT 'operator' COMMENT '4级RBAC角色',
    org_id BIGINT NULL COMMENT '所属组织',
    vip_level TINYINT DEFAULT 0 COMMENT '0=免费, 3=企业',
    vip_expire_at DATETIME NULL,
    ai_quota_total BIGINT DEFAULT 500000 COMMENT 'AI字数总额度',
    ai_quota_used BIGINT DEFAULT 0 COMMENT '已使用字数',
    duration_quota_total BIGINT DEFAULT 0 COMMENT '时长额度(秒)',
    duration_quota_used BIGINT DEFAULT 0,
    status TINYINT DEFAULT 1 COMMENT '0=禁用, 1=正常, 2=待审核',
    last_login_at DATETIME NULL,
    wechat_open_id VARCHAR(100) NULL,
    qq_open_id VARCHAR(100) NULL,
    deleted TINYINT(1) DEFAULT 0,
    created_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    INDEX idx_phone (phone),
    INDEX idx_org (org_id),
    INDEX idx_vip_expire (vip_expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 3. user_sessions
CREATE TABLE IF NOT EXISTS user_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    access_token_hash VARCHAR(255) NOT NULL COMMENT 'JWT hash用于吊销',
    refresh_token_hash VARCHAR(255) NOT NULL,
    device_fingerprint VARCHAR(255),
    ip_address VARCHAR(50),
    user_agent TEXT,
    expires_at DATETIME NOT NULL,
    created_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_user (user_id),
    INDEX idx_access_hash (access_token_hash),
    INDEX idx_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录会话';

-- 4. industries
CREATE TABLE IF NOT EXISTS industries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '行业名称',
    parent_id BIGINT NULL COMMENT '父行业ID',
    level TINYINT DEFAULT 1 COMMENT '1=一级, 2=二级',
    code VARCHAR(50) UNIQUE COMMENT '行业代码',
    is_system TINYINT(1) DEFAULT 1,
    sort_order INT DEFAULT 0,
    deleted TINYINT(1) DEFAULT 0,
    created_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行业字典';

-- 5. streamers
CREATE TABLE IF NOT EXISTS streamers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '添加者',
    org_id BIGINT NULL COMMENT '所属组织',
    platform ENUM('douyin','kuaishou','shipinhao') NOT NULL COMMENT '直播平台',
    room_id VARCHAR(128) NULL COMMENT '平台直播间ID',
    room_url VARCHAR(512) NULL COMMENT '直播间链接',
    anchor_name VARCHAR(128) NOT NULL COMMENT '主播昵称',
    anchor_avatar VARCHAR(512) NULL,
    account_id VARCHAR(128) NULL COMMENT '平台账号ID',
    industry_id BIGINT NULL COMMENT '行业ID',
    account_type ENUM('own','competitor','industry') DEFAULT 'own' COMMENT '自有/竞品/同行业',
    live_room_mode VARCHAR(32) NULL COMMENT '直播间模式',
    account_stage VARCHAR(16) NULL COMMENT '账号阶段',
    account_issue VARCHAR(200) NULL COMMENT '账号问题描述(<=200字)',
    account_level VARCHAR(16) NULL COMMENT '账号水平',
    traffic_structure VARCHAR(32) NULL COMMENT '流量结构',
    broadcast_time_start TIME NULL COMMENT '开播开始时间',
    broadcast_time_end TIME NULL COMMENT '开播结束时间',
    default_language VARCHAR(16) DEFAULT '中文通用' COMMENT '默认识别语言',
    is_monitoring TINYINT(1) DEFAULT 0 COMMENT '是否开启自动监控',
    monitor_config JSON NULL COMMENT '监控配置JSON',
    shipinhao_auth_token TEXT NULL COMMENT '视频号授权token',
    total_sessions INT DEFAULT 0 COMMENT '累计录制场次',
    today_sessions INT DEFAULT 0 COMMENT '今日录制场次',
    last_live_at DATETIME NULL COMMENT '最近开播时间',
    status TINYINT DEFAULT 1 COMMENT '0=已删除, 1=正常, 2=监控中, 3=录制中',
    deleted TINYINT(1) DEFAULT 0,
    created_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    INDEX idx_user (user_id),
    INDEX idx_org (org_id),
    INDEX idx_platform_account (platform, account_id),
    INDEX idx_monitoring (is_monitoring),
    INDEX idx_user_account_type (user_id, account_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='直播间/主播';

-- 6. recordings
CREATE TABLE IF NOT EXISTS recordings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    streamer_id BIGINT NOT NULL COMMENT '关联直播间',
    user_id BIGINT NOT NULL,
    org_id BIGINT NULL,
    local_file_path VARCHAR(1024) NULL COMMENT '本地视频路径',
    local_file_name VARCHAR(256) NOT NULL COMMENT '显示文件名(<=15字)',
    stream_url VARCHAR(1024) NULL COMMENT '录制时直播流地址',
    start_time DATETIME NULL COMMENT '录制开始',
    end_time DATETIME NULL COMMENT '录制结束',
    duration INT DEFAULT 0 COMMENT '时长(秒)',
    file_size BIGINT DEFAULT 0 COMMENT '文件大小(字节)',
    resolution VARCHAR(16) NULL COMMENT '分辨率',
    segment_index INT DEFAULT 1 COMMENT '分段序号',
    session_id VARCHAR(64) NULL COMMENT '场次标识',
    core_data JSON NULL COMMENT '核心数据JSON',
    sensitive_word_count INT DEFAULT 0,
    operation_keyword_count INT DEFAULT 0,
    status ENUM('monitoring','recording','completed','failed','deleted') DEFAULT 'monitoring',
    analysis_status ENUM('none','pending','processing','completed','diagnosed','failed') DEFAULT 'none',
    error_msg TEXT NULL,
    deleted TINYINT(1) DEFAULT 0,
    created_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    INDEX idx_streamer (streamer_id),
    INDEX idx_user (user_id),
    INDEX idx_org (org_id),
    INDEX idx_start_time (start_time),
    INDEX idx_status (status),
    INDEX idx_analysis_status (analysis_status),
    INDEX idx_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='录制记录';

-- 7. analysis_tasks
CREATE TABLE IF NOT EXISTS analysis_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recording_id BIGINT NOT NULL COMMENT '关联录制',
    user_id BIGINT NOT NULL,
    org_id BIGINT NULL,
    type ENUM('full','clip') NOT NULL COMMENT '分析类型',
    parent_task_id BIGINT NULL COMMENT '整场任务ID(切片用)',
    clip_start INT NULL COMMENT '切片起始秒',
    clip_end INT NULL,
    clip_category VARCHAR(32) NULL COMMENT '14种切片分类',
    clip_filename VARCHAR(255) NULL,
    clip_remark VARCHAR(100) NULL,
    status ENUM('pending','asr_processing','ai_processing','completed','failed') DEFAULT 'pending',
    priority TINYINT DEFAULT 5 COMMENT '优先级(1-10越小越高)',
    ai_model VARCHAR(32) DEFAULT 'doubao' COMMENT 'doubao/deepseek_r1',
    industry VARCHAR(64) NULL COMMENT '分析时的行业',
    asr_text LONGTEXT NULL COMMENT 'ASR全文',
    asr_word_count INT DEFAULT 0,
    ai_result JSON NULL COMMENT 'AI分析结果',
    ai_diagnosis JSON NULL COMMENT 'AI诊断报告',
    keyword_summary JSON NULL COMMENT '关键词汇总',
    sensitive_words JSON NULL COMMENT '敏感词检测结果',
    sensitive_count INT DEFAULT 0,
    content_compass JSON NULL COMMENT '内容罗盘数据',
    optimization_action TEXT NULL COMMENT '优化动作(<=500字)',
    optimization_goal TEXT NULL COMMENT '优化目的(<=500字)',
    summary TEXT NULL COMMENT '小结',
    consumed_chars BIGINT DEFAULT 0 COMMENT '消耗AI字数',
    error_msg TEXT NULL,
    started_at DATETIME NULL,
    completed_at DATETIME NULL,
    deleted TINYINT(1) DEFAULT 0,
    created_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    INDEX idx_recording (recording_id),
    INDEX idx_user (user_id),
    INDEX idx_org (org_id),
    INDEX idx_status (status),
    INDEX idx_type (type),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI分析任务';

-- 8. asr_paragraphs
CREATE TABLE IF NOT EXISTS asr_paragraphs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL COMMENT '关联分析任务',
    paragraph_index INT NOT NULL COMMENT '段落序号(从0)',
    start_time VARCHAR(12) NOT NULL COMMENT 'HH:MM:SS',
    end_time VARCHAR(12) NULL,
    natural_time VARCHAR(12) NULL COMMENT '自然时间',
    text_content TEXT NOT NULL COMMENT '话术文本',
    word_count INT DEFAULT 0 COMMENT '段落字数',
    words_per_min INT DEFAULT 0 COMMENT '语速(字/分钟)',
    online_count INT NULL,
    barrage_count INT NULL,
    transaction_count INT NULL,
    interaction_rate DECIMAL(5,2) NULL COMMENT '互动率%',
    transaction_rate DECIMAL(5,2) NULL COMMENT '成交率%',
    sales_amount DECIMAL(12,2) NULL COMMENT '销售额',
    uv_value DECIMAL(8,2) NULL COMMENT 'UV价值',
    speaker_id VARCHAR(32) NULL COMMENT '说话人标识',
    script_category VARCHAR(32) NULL COMMENT 'AI话术分类',
    is_highlighted TINYINT(1) DEFAULT 0 COMMENT '用户标记高亮',
    deleted TINYINT(1) DEFAULT 0,
    created_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_task (task_id),
    INDEX idx_start_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ASR分钟段落明细';

-- 9. keywords
CREATE TABLE IF NOT EXISTS keywords (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NULL COMMENT '关联分析任务',
    comparison_id BIGINT NULL COMMENT '关联对比',
    type ENUM('sensitive','operational') NOT NULL,
    category VARCHAR(64) NULL,
    sub_category VARCHAR(64) NULL,
    word VARCHAR(128) NOT NULL,
    hit_count_video1 INT DEFAULT 0,
    hit_count_video2 INT DEFAULT 0 COMMENT '对比用',
    total_count INT DEFAULT 0,
    source ENUM('system','custom') DEFAULT 'system',
    scene_desc TEXT NULL,
    industry VARCHAR(64) NULL,
    risk_level TINYINT NULL COMMENT '风险等级: 1=低, 2=中, 3=高',
    sentence_refs JSON NULL COMMENT '命中句子定位',
    deleted TINYINT(1) DEFAULT 0,
    created_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_task (task_id),
    INDEX idx_comparison (comparison_id),
    INDEX idx_type_cat (type, category),
    INDEX idx_word (word)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='运营关键词/敏感词明细';

-- 10. optimization_actions
CREATE TABLE IF NOT EXISTS optimization_actions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    action TEXT NOT NULL COMMENT '优化动作(<=500字)',
    goal TEXT NULL COMMENT '优化目的(<=500字)',
    deleted TINYINT(1) DEFAULT 0,
    created_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优化动作';

-- 11. recap_notes
CREATE TABLE IF NOT EXISTS recap_notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    tab_type VARCHAR(30) NOT NULL COMMENT 'MINUTE_SEGMENTS/AI_SCRIPT/RECAP_SUMMARY/CORRECTIONS',
    content_html MEDIUMTEXT COMMENT '富文本HTML',
    deleted TINYINT(1) DEFAULT 0,
    created_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_task_tab (task_id, tab_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记批注(4tab)';

-- 12. comparisons
CREATE TABLE IF NOT EXISTS comparisons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    org_id BIGINT NULL,
    type ENUM('full','clip') NOT NULL COMMENT '整场/切片对比',
    recording_id_optimize BIGINT NOT NULL COMMENT '优化场次录制ID',
    recording_id_reference BIGINT NOT NULL COMMENT '参考场次录制ID',
    task_id_optimize BIGINT NULL,
    task_id_reference BIGINT NULL,
    clip_category VARCHAR(32) NULL COMMENT '仅切片对比使用',
    ai_comparison_result JSON NULL COMMENT 'AI对比结果',
    ai_model VARCHAR(32) DEFAULT 'deepseek_r1',
    status ENUM('pending','processing','completed','failed') DEFAULT 'pending',
    deleted TINYINT(1) DEFAULT 0,
    created_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    INDEX idx_user (user_id),
    INDEX idx_org (org_id),
    INDEX idx_status (status),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对比复盘';

-- 13. comparison_drafts
CREATE TABLE IF NOT EXISTS comparison_drafts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    first_recording_id BIGINT NOT NULL COMMENT '第一方锁定的录制ID',
    list_context VARCHAR(50) NOT NULL COMMENT 'AI_FULL_RECAP/AI_CLIP',
    expires_at DATETIME NOT NULL COMMENT '30分钟后过期',
    created_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_user (user_id) COMMENT '每用户同时只有一个draft'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='加入对比待配对状态';

-- 14. ai_conversations
CREATE TABLE IF NOT EXISTS ai_conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    task_id BIGINT NULL COMMENT '关联分析任务(单场)',
    comparison_id BIGINT NULL COMMENT '关联对比',
    assistant_type ENUM('operation','compliance','script') NOT NULL COMMENT '助手类型',
    ai_model VARCHAR(32) NULL,
    role ENUM('user','assistant','system') NOT NULL,
    content LONGTEXT NOT NULL COMMENT '消息内容(Markdown)',
    thinking TEXT NULL COMMENT '深度思考(DeepSeek R1专用)',
    preset_question_id INT NULL COMMENT '预设问题编号(1-12)',
    tokens_used INT DEFAULT 0,
    deleted TINYINT(1) DEFAULT 0,
    created_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_user (user_id),
    INDEX idx_task (task_id),
    INDEX idx_comparison (comparison_id),
    INDEX idx_assistant (assistant_type),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话记录';

-- 15. vip_plans
CREATE TABLE IF NOT EXISTS vip_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL COMMENT '套餐名称',
    level TINYINT NOT NULL COMMENT '0=免费, 3=企业',
    duration_days INT NOT NULL COMMENT '有效期天数',
    price DECIMAL(10,2) NOT NULL,
    ai_quota BIGINT NOT NULL COMMENT 'AI字数额度',
    max_rooms INT DEFAULT 10,
    max_members INT DEFAULT 20,
    features JSON NULL COMMENT '功能特性列表',
    is_active TINYINT(1) DEFAULT 1,
    sort_order INT DEFAULT 0,
    deleted TINYINT(1) DEFAULT 0,
    created_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员套餐配置';

-- 16. operation_logs
CREATE TABLE IF NOT EXISTS operation_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    action VARCHAR(64) NOT NULL COMMENT '操作类型',
    target_type VARCHAR(32) NULL,
    target_id BIGINT NULL,
    detail JSON NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(512) NULL,
    created_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_user (user_id),
    INDEX idx_action (action),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作审计日志';

-- 17. dictionaries + dictionary_keywords
CREATE TABLE IF NOT EXISTS dictionaries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL COMMENT 'NULL表示系统词库',
    org_id BIGINT NULL,
    name VARCHAR(100) NOT NULL,
    industry_id BIGINT NULL,
    is_system TINYINT(1) DEFAULT 0,
    description TEXT NULL,
    deleted TINYINT(1) DEFAULT 0,
    created_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='词库';

CREATE TABLE IF NOT EXISTS dictionary_keywords (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dictionary_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL COMMENT '关键词分类',
    sub_category VARCHAR(50) NULL,
    keyword VARCHAR(200) NOT NULL,
    description TEXT NULL,
    replacement_suggestion TEXT NULL COMMENT '敏感词替换建议',
    deleted TINYINT(1) DEFAULT 0,
    created_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_dict_cat (dictionary_id, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='词库关键词';

-- 18. customer_service_tickets
CREATE TABLE IF NOT EXISTS customer_service_tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    contact VARCHAR(100) NULL,
    status VARCHAR(20) DEFAULT 'OPEN',
    reply TEXT NULL,
    replied_at DATETIME NULL,
    created_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客服工单';
