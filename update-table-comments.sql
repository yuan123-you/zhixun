-- ============================================================
-- 更新数据库表注释
-- 执行方式：mysql -u root -p zhixun < update-table-comments.sql
-- ============================================================

ALTER TABLE sys_user COMMENT = '用户表';
ALTER TABLE cms_category COMMENT = '分类表';
ALTER TABLE cms_tag COMMENT = '标签表';
ALTER TABLE cms_article COMMENT = '文章/作品表';
ALTER TABLE cms_article_tag COMMENT = '作品标签关联表';
ALTER TABLE cms_article_image COMMENT = '作品图片表';
ALTER TABLE cms_like COMMENT = '点赞表';
ALTER TABLE cms_collect COMMENT = '收藏表';
ALTER TABLE cms_comment COMMENT = '评论表';
ALTER TABLE cms_view_history COMMENT = '浏览记录表';
ALTER TABLE sys_sensitive_word COMMENT = '敏感词表';
ALTER TABLE sys_operation_log COMMENT = '操作日志表';
ALTER TABLE sys_login_log COMMENT = '登录日志表';
ALTER TABLE sys_notification COMMENT = '通知消息表';
ALTER TABLE user_follow COMMENT = '关注表';
ALTER TABLE user_message COMMENT = '私信表';
ALTER TABLE user_settings COMMENT = '用户偏好设置表';
ALTER TABLE user_preferred_category COMMENT = '用户偏好分类关联表';
ALTER TABLE user_preferred_tag COMMENT = '用户偏好标签关联表';
ALTER TABLE cms_topic COMMENT = '话题表';
ALTER TABLE cms_topic_article COMMENT = '话题文章关联表';
ALTER TABLE cms_topic_follow COMMENT = '话题关注表';
ALTER TABLE cms_group_info COMMENT = '群组表';
ALTER TABLE cms_group_member COMMENT = '群组成员表';
ALTER TABLE cms_group_message COMMENT = '群组消息表';
ALTER TABLE user_profile_theme COMMENT = '用户主页主题表';
ALTER TABLE cms_template COMMENT = '内容模板表';
ALTER TABLE cms_article_report COMMENT = '文章举报表';
ALTER TABLE sys_user_report COMMENT = '用户举报表';
ALTER TABLE cms_article_collaborator COMMENT = '文章协作表';
ALTER TABLE sys_sensitive_whitelist COMMENT = '敏感词白名单表';
ALTER TABLE user_tag_follow COMMENT = '用户关注标签表';
ALTER TABLE cms_comment_report COMMENT = '评论举报表';
ALTER TABLE sys_security_audit_log COMMENT = '安全审计日志表';
ALTER TABLE cms_banner COMMENT = '轮播图/横幅表';
ALTER TABLE sys_announcement COMMENT = '系统公告表';
ALTER TABLE cms_danmaku COMMENT = '弹幕表';
ALTER TABLE cms_repost COMMENT = '转发表';

-- 验证结果
SELECT TABLE_NAME, TABLE_COMMENT
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
ORDER BY TABLE_NAME;
