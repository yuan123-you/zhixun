-- ============================================================
-- V3: 群组群号 + 用户设置扩展
-- ============================================================

-- 群组表增加群号字段
ALTER TABLE `cms_group_info`
    ADD COLUMN `group_number` VARCHAR(10) DEFAULT NULL COMMENT '群号(6-10位纯数字)';

-- 群号唯一索引
CREATE UNIQUE INDEX `uk_group_number` ON `cms_group_info` (`group_number`);

-- user_settings 表扩展新设置字段
ALTER TABLE `user_settings`
    ADD COLUMN `content_recommend` INT DEFAULT 1 COMMENT '内容推荐: 0关闭 1开启',
    ADD COLUMN `auto_play_video` INT DEFAULT 1 COMMENT '自动播放视频: 0关闭 1仅WiFi 2始终',
    ADD COLUMN `quiet_hours_enabled` INT DEFAULT 0 COMMENT '免打扰: 0关闭 1开启',
    ADD COLUMN `quiet_hours_start` VARCHAR(5) DEFAULT '22:00' COMMENT '免打扰开始时间',
    ADD COLUMN `quiet_hours_end` VARCHAR(5) DEFAULT '08:00' COMMENT '免打扰结束时间',
    ADD COLUMN `show_view_count` INT DEFAULT 1 COMMENT '显示阅读量: 0关闭 1开启',
    ADD COLUMN `allow_search` INT DEFAULT 1 COMMENT '允许被搜索: 0关闭 1开启';
