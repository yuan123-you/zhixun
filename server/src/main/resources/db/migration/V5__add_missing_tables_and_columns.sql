-- ============================================================
-- V5: 补充缺失的表和列（私信、群聊、入群申请）
-- ============================================================

-- cms_group_message 增加 @提及用户 列
ALTER TABLE `cms_group_message`
    ADD COLUMN `mentioned_user_ids` VARCHAR(500) NULL COMMENT '提及的用户ID列表(JSON数组)';

-- user_message 增加消息类型列
ALTER TABLE `user_message`
    ADD COLUMN `type` VARCHAR(20) NOT NULL DEFAULT 'text' COMMENT '消息类型: text/image/voice/file/system';

-- 创建入群申请表
CREATE TABLE `cms_group_join_request` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `group_id` BIGINT NOT NULL COMMENT '群组ID',
    `user_id` BIGINT NOT NULL COMMENT '申请用户ID',
    `message` VARCHAR(500) NULL COMMENT '申请留言',
    `status` INT NOT NULL DEFAULT 0 COMMENT '状态: 0=待审核 1=已批准 2=已拒绝',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_group_status` (`group_id`, `status`),
    INDEX `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群组入群申请';

-- cms_comment 增加图片列
ALTER TABLE `cms_comment`
    ADD COLUMN `images` TEXT NULL COMMENT '评论图片(JSON数组)';
