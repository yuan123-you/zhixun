-- ============================================================
-- V2: 新功能模块数据库迁移
-- 包含: 话题/群组/签到/经验/徽章/主页主题/模板/举报/协作
-- ============================================================

-- 话题表
CREATE TABLE IF NOT EXISTS `cms_topic` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL COMMENT '话题名称',
    `description` VARCHAR(500) DEFAULT NULL,
    `cover_image` VARCHAR(500) DEFAULT NULL,
    `article_count` BIGINT DEFAULT 0,
    `follow_count` BIGINT DEFAULT 0,
    `hot_score` DECIMAL(10,2) DEFAULT 0.00,
    `is_official` INT DEFAULT 0 COMMENT '是否官方话题',
    `status` INT DEFAULT 0 COMMENT '0正常 1隐藏',
    `creator_id` BIGINT DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_name` (`name`),
    INDEX `idx_hot_score` (`hot_score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='话题表';

-- 话题-文章关联表
CREATE TABLE IF NOT EXISTS `cms_topic_article` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `topic_id` BIGINT NOT NULL,
    `article_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_topic_article` (`topic_id`, `article_id`),
    INDEX `idx_article_id` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='话题文章关联表';

-- 话题关注表
CREATE TABLE IF NOT EXISTS `cms_topic_follow` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `topic_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_user_topic` (`user_id`, `topic_id`),
    INDEX `idx_topic_id` (`topic_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='话题关注表';

-- 群组表
CREATE TABLE IF NOT EXISTS `cms_group_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `avatar` VARCHAR(500) DEFAULT NULL,
    `description` VARCHAR(500) DEFAULT NULL,
    `owner_id` BIGINT NOT NULL,
    `member_count` BIGINT DEFAULT 1,
    `max_members` INT DEFAULT 200,
    `is_public` INT DEFAULT 1 COMMENT '是否公开',
    `status` INT DEFAULT 0 COMMENT '0正常 1已解散',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_owner_id` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组表';

-- 群组成员表
CREATE TABLE IF NOT EXISTS `cms_group_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `group_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `role` INT DEFAULT 0 COMMENT '0成员 1管理员 2群主',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '群内昵称',
    `unread_count` INT DEFAULT 0,
    `is_muted` INT DEFAULT 0,
    `joined_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_group_user` (`group_id`, `user_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组成员表';

-- 群组消息表
CREATE TABLE IF NOT EXISTS `cms_group_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `group_id` BIGINT NOT NULL,
    `sender_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL COMMENT '消息内容(AES加密)',
    `message_type` VARCHAR(20) DEFAULT 'text' COMMENT 'text/image/voice/file/system',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_group_id` (`group_id`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组消息表';

-- 签到表
CREATE TABLE IF NOT EXISTS `user_check_in` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `check_in_date` DATE NOT NULL,
    `consecutive_days` INT DEFAULT 1,
    `points` INT DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_user_date` (`user_id`, `check_in_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户签到表';

-- 用户经验表
CREATE TABLE IF NOT EXISTS `user_experience` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `exp` BIGINT DEFAULT 0,
    `level` INT DEFAULT 1,
    `level_name` VARCHAR(20) DEFAULT '初级用户',
    `next_level_exp` BIGINT DEFAULT 100,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户经验表';

-- 徽章表
CREATE TABLE IF NOT EXISTS `sys_badge` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL,
    `description` VARCHAR(200) DEFAULT NULL,
    `icon` VARCHAR(500) DEFAULT NULL,
    `category` VARCHAR(20) DEFAULT NULL COMMENT 'sign_in/content/social/achievement/special',
    `condition` VARCHAR(200) DEFAULT NULL COMMENT '获取条件',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='徽章表';

-- 用户徽章表
CREATE TABLE IF NOT EXISTS `user_badge` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `badge_id` BIGINT NOT NULL,
    `earned_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_user_badge` (`user_id`, `badge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户徽章表';

-- 用户主页主题表
CREATE TABLE IF NOT EXISTS `user_profile_theme` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `theme_color` VARCHAR(10) DEFAULT '#3b82f6',
    `background_image` VARCHAR(500) DEFAULT NULL,
    `background_style` VARCHAR(20) DEFAULT 'cover',
    `font_family` VARCHAR(100) DEFAULT NULL,
    `bio_bg_color` VARCHAR(10) DEFAULT NULL,
    `card_style` VARCHAR(50) DEFAULT 'default',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户主页主题表';

-- 内容模板表
CREATE TABLE IF NOT EXISTS `cms_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `description` VARCHAR(500) DEFAULT NULL,
    `cover_image` VARCHAR(500) DEFAULT NULL,
    `category` VARCHAR(50) DEFAULT NULL,
    `content` TEXT NOT NULL COMMENT '模板内容(markdown)',
    `tags` VARCHAR(200) DEFAULT NULL,
    `use_count` BIGINT DEFAULT 0,
    `is_official` INT DEFAULT 0,
    `creator_id` BIGINT DEFAULT NULL,
    `status` INT DEFAULT 0 COMMENT '0正常 1隐藏',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_category` (`category`),
    INDEX `idx_use_count` (`use_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容模板表';

-- 文章举报表
CREATE TABLE IF NOT EXISTS `cms_article_report` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `article_id` BIGINT NOT NULL,
    `reporter_id` BIGINT NOT NULL,
    `reason` VARCHAR(100) NOT NULL,
    `description` VARCHAR(500) DEFAULT NULL,
    `status` INT DEFAULT 0 COMMENT '0待处理 1已忽略 2已删除',
    `handled_by` BIGINT DEFAULT NULL,
    `handled_at` DATETIME DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章举报表';

-- 用户举报表
CREATE TABLE IF NOT EXISTS `sys_user_report` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `reported_user_id` BIGINT NOT NULL,
    `reporter_id` BIGINT NOT NULL,
    `reason` VARCHAR(100) NOT NULL,
    `description` VARCHAR(500) DEFAULT NULL,
    `status` INT DEFAULT 0 COMMENT '0待处理 1已忽略 2已封禁',
    `handled_by` BIGINT DEFAULT NULL,
    `handled_at` DATETIME DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户举报表';

-- 文章协作表
CREATE TABLE IF NOT EXISTS `cms_article_collaborator` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `article_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `permission` VARCHAR(10) DEFAULT 'edit' COMMENT 'edit/view',
    `status` INT DEFAULT 0 COMMENT '0待接受 1已接受 2已拒绝',
    `invited_by` BIGINT DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_article_user` (`article_id`, `user_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章协作表';

-- ============================================================
-- 初始徽章数据
-- ============================================================
INSERT INTO `sys_badge` (`id`, `name`, `description`, `icon`, `category`, `condition`) VALUES
(1, '首批用户', '平台最早的一批用户', '/badges/pioneer.svg', 'special', '注册即获得'),
(2, '连签达人', '连续签到7天', '/badges/checkin7.svg', 'sign_in', '连续签到7天'),
(3, '百赞作者', '累计获得100个点赞', '/badges/like100.svg', 'content', '累计获赞100次'),
(4, '千粉作者', '粉丝数突破1000', '/badges/fan1000.svg', 'social', '粉丝数达到1000'),
(5, '日更达人', '连续30天发布内容', '/badges/daily.svg', 'content', '连续30天发布'),
(6, '评论达人', '累计发表100条评论', '/badges/comment100.svg', 'content', '累计评论100条'),
(7, '收藏达人', '收藏内容超过50篇', '/badges/collect50.svg', 'content', '收藏数达到50'),
(8, '年度创作者', '年度表现优异的创作者', '/badges/yearly.svg', 'achievement', '年度评选获得');
