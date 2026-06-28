-- V5: Fix missing columns in user_settings table
ALTER TABLE `user_settings` ADD COLUMN `content_recommend` INT DEFAULT 1;
ALTER TABLE `user_settings` ADD COLUMN `auto_play_video` INT DEFAULT 1;
ALTER TABLE `user_settings` ADD COLUMN `quiet_hours_enabled` INT DEFAULT 0;
ALTER TABLE `user_settings` ADD COLUMN `quiet_hours_start` VARCHAR(5) DEFAULT '22:00';
ALTER TABLE `user_settings` ADD COLUMN `quiet_hours_end` VARCHAR(5) DEFAULT '08:00';
ALTER TABLE `user_settings` ADD COLUMN `show_view_count` INT DEFAULT 1;
ALTER TABLE `user_settings` ADD COLUMN `allow_search` INT DEFAULT 1;