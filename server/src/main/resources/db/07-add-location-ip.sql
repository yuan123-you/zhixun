-- 07-add-location-ip.sql
-- 作品表添加发布位置和IP属地字段
-- 执行前请确保数据库已备份

ALTER TABLE `cms_article`
    ADD COLUMN `location` VARCHAR(255) NULL COMMENT '发布位置' AFTER `device_info`,
    ADD COLUMN `ip_address` VARCHAR(45) NULL COMMENT '发布IP属地' AFTER `location`;
