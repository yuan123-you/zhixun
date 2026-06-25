-- 用户UID和所属地迁移脚本
-- 添加 uid 字段（用户可自定义的唯一标识号）
-- 添加 uid_updated_at 字段（记录上次修改UID时间，用于30天限制）
-- 添加 province 字段（用户所属省份）

ALTER TABLE sys_user
    ADD COLUMN uid VARCHAR(30) NULL UNIQUE AFTER id,
    ADD COLUMN uid_updated_at DATETIME NULL AFTER uid,
    ADD COLUMN province VARCHAR(50) NULL AFTER bio;

-- 为现有用户生成默认UID（格式: user_ + 8位随机字符）
-- 使用用户名+ID的hash后8位作为默认UID
UPDATE sys_user SET uid = CONCAT('user_', LOWER(SUBSTRING(MD5(CONCAT(username, id)), 1, 8))) WHERE uid IS NULL;

-- 添加NOT NULL约束
ALTER TABLE sys_user MODIFY COLUMN uid VARCHAR(30) NOT NULL;

-- 添加索引
CREATE INDEX idx_sys_user_uid ON sys_user(uid);
