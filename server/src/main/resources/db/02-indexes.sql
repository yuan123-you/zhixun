-- 知讯平台数据库索引优化迁移脚本
-- 执行前提：01-schema.sql 已执行

-- ============================================================
-- 1. 复合索引优化
-- ============================================================

-- 作品列表查询：按状态+发布时间筛选（最频繁的查询场景）
CREATE INDEX idx_cms_article_status_publish ON cms_article(status, publish_at);

-- 用户浏览历史查询：按用户+时间范围查询
CREATE INDEX idx_cms_view_history_user_time ON cms_view_history(user_id, created_at);

-- 未读通知查询：按用户+已读状态+时间排序
CREATE INDEX idx_sys_notification_user_read_time ON sys_notification(user_id, is_read, created_at);

-- ============================================================
-- 2. 表结构迁移（配合数据存储策略调整）
-- ============================================================

-- 2.1 user_settings 表：theme, font_size, language 列已在 schema 中移除（改为客户端本地存储）
-- 2.2 cms_view_history 表：ip, user_agent 列已在 schema 中移除（改为客户端本地存储）

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

-- ============================================================
-- 5. 评论系统索引优化
-- ============================================================

-- 评论列表查询：按作品+状态+时间排序（最频繁的评论查询场景）
CREATE INDEX idx_cms_comment_article_status ON cms_comment(article_id, status, created_at);

-- 评论审核查询：按状态+时间排序（管理员审核场景）
CREATE INDEX idx_cms_comment_status_created ON cms_comment(status, created_at);

-- 用户评论查询：按用户+状态+时间排序
CREATE INDEX idx_cms_comment_user_status ON cms_comment(user_id, status, created_at);

-- ============================================================
-- 6. 点赞和收藏查询优化
-- ============================================================

-- 用户点赞列表查询：按用户+类型+时间排序
CREATE INDEX idx_cms_like_user_type_created ON cms_like(user_id, target_type, created_at);

-- 用户收藏列表查询：按用户+时间排序
CREATE INDEX idx_cms_collect_user_created ON cms_collect(user_id, created_at);
