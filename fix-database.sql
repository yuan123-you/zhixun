-- ============================================================
-- 知讯系统 - 本地数据库修复脚本
-- 补全缺失的表和字段
-- ============================================================

USE zhixun;

-- ============================================================
-- 1. 修复 cms_article 表的缺失列（MySQL 8.0+）
-- ============================================================
ALTER TABLE cms_article ADD COLUMN IF NOT EXISTS share_count INT DEFAULT 0 COMMENT '分享数';
ALTER TABLE cms_article ADD COLUMN IF NOT EXISTS device_info VARCHAR(100) NULL COMMENT '发布设备信息';
ALTER TABLE cms_article ADD COLUMN IF NOT EXISTS location VARCHAR(255) NULL COMMENT '发布位置';
ALTER TABLE cms_article ADD COLUMN IF NOT EXISTS ip_address VARCHAR(45) NULL COMMENT 'IP属地';
ALTER TABLE cms_article ADD COLUMN IF NOT EXISTS visibility TINYINT NOT NULL DEFAULT 0 COMMENT '可见性：0=公开，1=仅粉丝，2=互相关注，3=仅自己';

-- 修复 sys_user 表的缺失列
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS uid VARCHAR(30) NULL UNIQUE COMMENT '用户自定义ID';
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS uid_updated_at DATETIME NULL COMMENT '上次修改UID时间';
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS province VARCHAR(50) NULL COMMENT '所属省份';

-- ============================================================
-- 2. 创建 V2 新功能表
-- ============================================================

-- 话题表
CREATE TABLE IF NOT EXISTS cms_topic (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL COMMENT '话题名称',
    description VARCHAR(500) DEFAULT NULL,
    cover_image VARCHAR(500) DEFAULT NULL,
    article_count BIGINT DEFAULT 0,
    follow_count BIGINT DEFAULT 0,
    hot_score DECIMAL(10,2) DEFAULT 0.00,
    is_official INT DEFAULT 0 COMMENT '是否官方话题',
    status INT DEFAULT 0 COMMENT '0正常 1隐藏',
    creator_id BIGINT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_topic_name (name),
    INDEX idx_topic_hot (hot_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='话题表';

-- 话题-文章关联表
CREATE TABLE IF NOT EXISTS cms_topic_article (
    id BIGINT NOT NULL AUTO_INCREMENT,
    topic_id BIGINT NOT NULL,
    article_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_topic_article (topic_id, article_id),
    INDEX idx_topic_article_article (article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='话题文章关联表';

-- 话题关注表
CREATE TABLE IF NOT EXISTS cms_topic_follow (
    id BIGINT NOT NULL AUTO_INCREMENT,
    topic_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_user_topic (user_id, topic_id),
    INDEX idx_topic_follow_topic (topic_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='话题关注表';

-- 群组表
CREATE TABLE IF NOT EXISTS cms_group_info (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    avatar VARCHAR(500) DEFAULT NULL,
    description VARCHAR(500) DEFAULT NULL,
    owner_id BIGINT NOT NULL,
    member_count BIGINT DEFAULT 1,
    max_members INT DEFAULT 200,
    is_public INT DEFAULT 1 COMMENT '是否公开',
    status INT DEFAULT 0 COMMENT '0正常 1已解散',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_group_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组表';

-- 群组成员表
CREATE TABLE IF NOT EXISTS cms_group_member (
    id BIGINT NOT NULL AUTO_INCREMENT,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role INT DEFAULT 0 COMMENT '0成员 1管理员 2群主',
    nickname VARCHAR(50) DEFAULT NULL COMMENT '群内昵称',
    unread_count INT DEFAULT 0,
    is_muted INT DEFAULT 0,
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_group_user (group_id, user_id),
    INDEX idx_group_member_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组成员表';

-- 群组消息表
CREATE TABLE IF NOT EXISTS cms_group_message (
    id BIGINT NOT NULL AUTO_INCREMENT,
    group_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content TEXT NOT NULL COMMENT '消息内容(AES加密)',
    message_type VARCHAR(20) DEFAULT 'text' COMMENT 'text/image/voice/file/system',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_group_msg_group (group_id),
    INDEX idx_group_msg_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组消息表';

-- 用户主页主题表
CREATE TABLE IF NOT EXISTS user_profile_theme (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    theme_color VARCHAR(10) DEFAULT '#3b82f6',
    background_image VARCHAR(500) DEFAULT NULL,
    background_style VARCHAR(20) DEFAULT 'cover',
    font_family VARCHAR(100) DEFAULT NULL,
    bio_bg_color VARCHAR(10) DEFAULT NULL,
    card_style VARCHAR(50) DEFAULT 'default',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_theme_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户主页主题表';

-- 内容模板表
CREATE TABLE IF NOT EXISTS cms_template (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    cover_image VARCHAR(500) DEFAULT NULL,
    category VARCHAR(50) DEFAULT NULL,
    content TEXT NOT NULL COMMENT '模板内容(markdown)',
    tags VARCHAR(200) DEFAULT NULL,
    use_count BIGINT DEFAULT 0,
    is_official INT DEFAULT 0,
    creator_id BIGINT DEFAULT NULL,
    status INT DEFAULT 0 COMMENT '0正常 1隐藏',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_template_category (category),
    INDEX idx_template_use (use_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容模板表';

-- 文章举报表
CREATE TABLE IF NOT EXISTS cms_article_report (
    id BIGINT NOT NULL AUTO_INCREMENT,
    article_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    reason VARCHAR(100) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    status INT DEFAULT 0 COMMENT '0待处理 1已忽略 2已删除',
    handled_by BIGINT DEFAULT NULL,
    handled_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_report_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章举报表';

-- 用户举报表
CREATE TABLE IF NOT EXISTS sys_user_report (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reported_user_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    reason VARCHAR(100) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    status INT DEFAULT 0 COMMENT '0待处理 1已忽略 2已封禁',
    handled_by BIGINT DEFAULT NULL,
    handled_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_user_report_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户举报表';

-- 文章协作表
CREATE TABLE IF NOT EXISTS cms_article_collaborator (
    id BIGINT NOT NULL AUTO_INCREMENT,
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    permission VARCHAR(10) DEFAULT 'edit' COMMENT 'edit/view',
    status INT DEFAULT 0 COMMENT '0待接受 1已接受 2已拒绝',
    invited_by BIGINT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_article_user_collab (article_id, user_id),
    INDEX idx_collaborator_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章协作表';

-- ============================================================
-- 4. 验证修复结果
-- ============================================================
SELECT '=== 修复后的表列表 ===' AS '';
SHOW TABLES;

SELECT '=== cms_article 表现在包含的列 ===' AS '';
SHOW COLUMNS FROM cms_article;

