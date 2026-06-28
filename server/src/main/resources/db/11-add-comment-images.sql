-- ============================================
-- 迁移 11: 为评论表添加图片字段
-- 描述: cms_comment 表新增 images 列（JSON 格式存储图片 URL 列表）
-- 日期: 2026-06-28
-- ============================================

-- MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS，先检查再执行
-- 安全幂等脚本：重复执行不会报错

-- 检查并添加 images 列
SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cms_comment' AND COLUMN_NAME = 'images'
);
SET @stmt = IF(@col_exists = 0,
    'ALTER TABLE cms_comment ADD COLUMN images JSON DEFAULT NULL COMMENT ''评论图片URL列表（JSON数组）'' AFTER content',
    'SELECT ''images column already exists'' AS info'
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
