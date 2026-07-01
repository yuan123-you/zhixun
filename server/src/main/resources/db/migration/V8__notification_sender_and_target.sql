-- V8: Add sender_id and target_all columns to sys_notification
ALTER TABLE sys_notification ADD COLUMN sender_id BIGINT DEFAULT NULL COMMENT '发送者ID（管理员发送时为管理员ID，系统触发时为null）' AFTER user_id;
ALTER TABLE sys_notification ADD COLUMN target_all TINYINT DEFAULT 0 COMMENT '是否群发通知：0-否，1-是' AFTER group_key;
CREATE INDEX idx_sys_notification_sender ON sys_notification(sender_id);
