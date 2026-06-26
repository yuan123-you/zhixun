-- ============================================
-- 迁移 10: 添加性别和性别展示开关字段
-- 描述: sys_user 表新增 gender 和 show_gender_on_profile 列
-- 日期: 2026-06-26
-- ============================================

-- MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS，先检查再执行
-- 安全幂等脚本：重复执行不会报错

-- 检查并添加 gender 列
SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'gender'
);
SET @stmt = IF(@col_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN gender TINYINT DEFAULT 0 COMMENT ''性别：0=未知，1=男，2=女'' AFTER bio',
    'SELECT ''gender column already exists'' AS info'
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 show_gender_on_profile 列
SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'show_gender_on_profile'
);
SET @stmt = IF(@col_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN show_gender_on_profile TINYINT DEFAULT 0 COMMENT ''是否在个人主页展示性别：0-否，1-是'' AFTER gender',
    'SELECT ''show_gender_on_profile column already exists'' AS info'
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
