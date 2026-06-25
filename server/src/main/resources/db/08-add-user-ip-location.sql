-- 08-add-user-ip-location.sql
-- 用户表添加IP属地字段
-- 执行前请确保数据库已备份

ALTER TABLE `sys_user`
    ADD COLUMN `ip_location` VARCHAR(100) NULL COMMENT 'IP属地' AFTER `province`;
