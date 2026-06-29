-- ============================================================
-- V4: 修复 sys_user 表缺失的关键字段
-- 解决注册时因缺少 uid、email_hash、phone_hash 等列导致
-- 的 INSERT 失败和事务回滚问题
-- ============================================================

-- 补充 UID 相关字段
ALTER TABLE sys_user ADD COLUMN uid VARCHAR(30) NULL UNIQUE COMMENT '用户自定义ID';
ALTER TABLE sys_user ADD COLUMN uid_updated_at DATETIME NULL COMMENT '上次修改UID时间';

-- 补充邮箱/手机号哈希字段（用于唯一性校验和隐私保护）
ALTER TABLE sys_user ADD COLUMN email_hash VARCHAR(64) NULL UNIQUE COMMENT '邮箱MD5哈希';
ALTER TABLE sys_user ADD COLUMN phone_hash VARCHAR(64) NULL UNIQUE COMMENT '手机号MD5哈希';

-- 补充第三方登录相关字段
ALTER TABLE sys_user ADD COLUMN wechat_openid VARCHAR(100) NULL COMMENT '微信OpenID';
ALTER TABLE sys_user ADD COLUMN qq_openid VARCHAR(100) NULL COMMENT 'QQ OpenID';

-- 补充用户资料扩展字段
ALTER TABLE sys_user ADD COLUMN province VARCHAR(50) NULL COMMENT '所属省份';
ALTER TABLE sys_user ADD COLUMN gender INT DEFAULT 0 COMMENT '性别：0=未知，1=男，2=女';
ALTER TABLE sys_user ADD COLUMN show_gender_on_profile INT DEFAULT 0 COMMENT '主页展示性别：0-否，1-是';
ALTER TABLE sys_user ADD COLUMN ip_location VARCHAR(50) NULL COMMENT 'IP属地';

-- 确保 email/phone 字段长度足够存储 AES 加密后的值（加密后最长可达 255 字符）
ALTER TABLE sys_user MODIFY COLUMN email VARCHAR(255) NULL COMMENT '邮箱(AES加密)';
ALTER TABLE sys_user MODIFY COLUMN phone VARCHAR(255) NULL COMMENT '手机号(AES加密)';

-- 补充 sys_notification 的 group_key 字段（用于合并同类通知，注册欢迎通知依赖此字段）
ALTER TABLE sys_notification ADD COLUMN group_key VARCHAR(100) NULL COMMENT '分组键，合并同类通知';
CREATE INDEX idx_sys_notification_group_key ON sys_notification(user_id, group_key);
