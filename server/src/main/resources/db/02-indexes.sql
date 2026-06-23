-- 知讯平台数据库索引优化迁移脚本
-- 执行前提：01-schema.sql 已执行

-- ============================================================
-- 1. 复合索引优化
-- ============================================================

-- 文章列表查询：按状态+发布时间筛选（最频繁的查询场景）
CREATE INDEX idx_cms_article_status_publish ON cms_article(status, publish_at);

-- 用户浏览历史查询：按用户+时间范围查询
CREATE INDEX idx_cms_view_history_user_time ON cms_view_history(user_id, created_at);

-- 未读通知查询：按用户+已读状态+时间排序
CREATE INDEX idx_sys_notification_user_read_time ON sys_notification(user_id, is_read, created_at);

-- ============================================================
-- 2. 表结构迁移（配合数据存储策略调整）
-- ============================================================

-- 2.1 user_settings 表：移除 theme, font_size, language 列（改为客户端本地存储）
ALTER TABLE user_settings
  DROP COLUMN IF EXISTS theme,
  DROP COLUMN IF EXISTS font_size,
  DROP COLUMN IF EXISTS language;

-- 2.2 cms_view_history 表：移除 ip, user_agent 列（改为客户端本地存储）
ALTER TABLE cms_view_history
  DROP COLUMN IF EXISTS ip,
  DROP COLUMN IF EXISTS user_agent;

-- ============================================================
-- 3. 浏览历史表分区策略（按月分区，便于清理旧数据）
-- ============================================================
-- 注意：分区表需要主键包含分区键，以下操作需要先删除外键约束
-- 实际执行时需根据数据量评估是否需要分区

-- 如果浏览历史数据量极大（>100万行），可启用分区：
-- ALTER TABLE cms_view_history DROP FOREIGN KEY fk_view_history_user;
-- ALTER TABLE cms_view_history DROP FOREIGN KEY fk_view_history_article;
-- ALTER TABLE cms_view_history PARTITION BY RANGE (TO_DAYS(created_at)) (
--   PARTITION p_202501 VALUES LESS THAN (TO_DAYS('2025-02-01')),
--   PARTITION p_202502 VALUES LESS THAN (TO_DAYS('2025-03-01')),
--   PARTITION p_future VALUES LESS THAN MAXVALUE
-- );

-- ============================================================
-- 4. 数据保留策略相关索引
-- ============================================================

-- 操作日志按时间范围清理
-- 已有 idx_sys_operation_log_created，无需额外索引

-- 登录日志按时间范围清理
-- 已有 idx_sys_login_log_created，无需额外索引
