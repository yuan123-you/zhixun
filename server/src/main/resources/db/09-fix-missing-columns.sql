-- 09-fix-missing-columns.sql
-- 修复 cms_article 表缺失的列
-- 安全执行：使用存储过程检查列是否存在，避免重复添加报错

DELIMITER //

CREATE PROCEDURE IF NOT EXISTS add_column_if_missing(
    IN table_name VARCHAR(64),
    IN column_name VARCHAR(64),
    IN column_def TEXT
)
BEGIN
    DECLARE col_count INT DEFAULT 0;
    SELECT COUNT(*) INTO col_count
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = table_name
      AND COLUMN_NAME = column_name;

    IF col_count = 0 THEN
        SET @sql = CONCAT('ALTER TABLE `', table_name, '` ADD COLUMN `', column_name, '` ', column_def);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('Added column: ', column_name, ' to ', table_name) AS result;
    ELSE
        SELECT CONCAT('Column already exists: ', column_name) AS result;
    END IF;
END//

DELIMITER ;

-- 添加 share_count（分享数）
CALL add_column_if_missing('cms_article', 'share_count', 'INT DEFAULT 0 COMMENT ''分享数''');

-- 添加 device_info（发布设备信息）
CALL add_column_if_missing('cms_article', 'device_info', 'VARCHAR(100) NULL COMMENT ''发布设备信息''');

-- 添加 location（发布位置）
CALL add_column_if_missing('cms_article', 'location', 'VARCHAR(255) NULL COMMENT ''发布位置''');

-- 添加 ip_address（IP属地）
CALL add_column_if_missing('cms_article', 'ip_address', 'VARCHAR(45) NULL COMMENT ''IP属地''');

-- 添加 visibility（可见性）- 这是最可能导致问题的字段
CALL add_column_if_missing('cms_article', 'visibility', 'TINYINT NOT NULL DEFAULT 0 COMMENT ''可见性：0=公开，1=仅粉丝，2=互相关注，3=仅自己'' AFTER is_top');

-- 清理存储过程
DROP PROCEDURE IF EXISTS add_column_if_missing;
