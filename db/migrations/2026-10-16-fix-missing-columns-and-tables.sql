-- ============================================================
-- 迁移: 补齐缺失列与数据表 (评论图片、群提及、私信类型、通知发送者、入群申请、操作/浏览日志)
-- 日期: 2026-10-16
-- 解决问题:
-- 1. cms_comment 缺少 images 列导致评论列表加载 500
-- 2. cms_group_message 缺少 mentioned_user_ids 列
-- 3. user_message 缺少 type 列及外键阻碍 AI 消息发送
-- 4. sys_notification 缺少 sender_id 和 target_all 列
-- 5. cms_group_join_request、article_view_history、article_operate_log 缺失
-- ============================================================

-- 1. 评论图片列（解决评论加载报错 Unknown column 'images' in 'field list'）
ALTER TABLE cms_comment ADD COLUMN images JSON DEFAULT NULL COMMENT '评论图片URL列表（JSON数组）' AFTER content;

-- 2. 群消息提及用户列
ALTER TABLE cms_group_message ADD COLUMN mentioned_user_ids VARCHAR(500) NULL COMMENT '提及的用户ID列表(JSON数组)';

-- 3. 私信消息类型与外键约束优化（允许 AI 助手发送私信）
ALTER TABLE user_message ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'text' COMMENT '消息类型: text/image/voice/file/system' AFTER receiver_id;
ALTER TABLE user_message DROP FOREIGN KEY user_message_ibfk_1;
ALTER TABLE user_message DROP FOREIGN KEY user_message_ibfk_2;

-- 4. 系统通知发送者和群发标记
ALTER TABLE sys_notification ADD COLUMN sender_id BIGINT DEFAULT NULL COMMENT '发送者ID（管理员发送时为管理员ID，系统触发时为null）' AFTER user_id;
ALTER TABLE sys_notification ADD COLUMN target_all TINYINT DEFAULT 0 COMMENT '是否群发通知：0-否，1-是' AFTER group_key;
CREATE INDEX idx_sys_notification_sender ON sys_notification(sender_id);

-- 5. 入群申请表
CREATE TABLE IF NOT EXISTS cms_group_join_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL COMMENT '群组ID',
    user_id BIGINT NOT NULL COMMENT '申请用户ID',
    message VARCHAR(500) NULL COMMENT '申请留言',
    status INT NOT NULL DEFAULT 0 COMMENT '状态: 0=待审核 1=已批准 2=已拒绝',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_group_status (group_id, status),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组入群申请';

-- 6. 作品浏览历史表
CREATE TABLE IF NOT EXISTS article_view_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    article_id BIGINT NOT NULL,
    user_id BIGINT,
    ip VARCHAR(50),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_article_view_history_article (article_id),
    INDEX idx_article_view_history_user (user_id),
    INDEX idx_article_view_history_user_time (user_id, create_time),
    INDEX idx_article_view_history_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作品浏览历史表';

-- 7. 作品操作日志表
CREATE TABLE IF NOT EXISTS article_operate_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL COMMENT '作品ID',
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    action VARCHAR(50) NOT NULL COMMENT '操作类型：approve/reject/takedown',
    reason VARCHAR(500) NULL COMMENT '操作原因',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_article_id (article_id),
    INDEX idx_operator_id (operator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作品操作日志';

-- 记录迁移历史
INSERT INTO migration_history (version, description, executed_at) VALUES (5, '2026-10-16-fix-missing-columns-and-tables.sql', NOW());
