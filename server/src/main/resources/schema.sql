-- 知讯平台数据库建表脚本 (MySQL 8.0+)

-- 1. sys_user 用户表
CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键，自增起始值 10000',
  uid VARCHAR(30) NOT NULL COMMENT '用户自定义ID（大小写字母+数字+下划线，30天内可修改一次）',
  uid_updated_at DATETIME COMMENT '上次修改UID时间',
  username VARCHAR(50) NOT NULL COMMENT '用户名',
  password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希（BCrypt）',
  nickname VARCHAR(50) COMMENT '昵称',
  avatar VARCHAR(500) COMMENT '头像URL',
  email VARCHAR(100) COMMENT '邮箱（AES加密存储）',
  email_hash VARCHAR(64) COMMENT '邮箱MD5哈希（用于唯一性校验）',
  phone VARCHAR(100) COMMENT '手机号（AES加密存储）',
  phone_hash VARCHAR(64) COMMENT '手机号MD5哈希（用于唯一性校验）',
  role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色：USER/ADMIN/SUPER_ADMIN',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
  bio VARCHAR(500) COMMENT '个人简介',
  gender TINYINT DEFAULT 0 COMMENT '性别：0=未知，1=男，2=女',
  show_gender_on_profile TINYINT DEFAULT 0 COMMENT '是否在主页展示性别',
  province VARCHAR(50) COMMENT '所属省份',
  ip_location VARCHAR(100) COMMENT 'IP属地',
  is_online TINYINT DEFAULT 0 COMMENT '是否在线',
  last_active_at DATETIME COMMENT '最后活跃时间',
  follow_count INT DEFAULT 0 COMMENT '关注数',
  follower_count INT DEFAULT 0 COMMENT '粉丝数',
  article_count INT DEFAULT 0 COMMENT '作品数',
  wechat_openid VARCHAR(100) COMMENT '微信OpenID',
  qq_openid VARCHAR(100) COMMENT 'QQ OpenID',
  last_login_at DATETIME COMMENT '最后登录时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_username (username),
  UNIQUE KEY uk_uid (uid),
  UNIQUE KEY uk_email_hash (email_hash),
  UNIQUE KEY uk_phone_hash (phone_hash)
) ENGINE=InnoDB AUTO_INCREMENT=10000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
CREATE INDEX idx_sys_user_role ON sys_user(role);
CREATE INDEX idx_sys_user_status ON sys_user(status);
CREATE INDEX idx_sys_user_uid ON sys_user(uid);

-- 2. cms_category 分类表
CREATE TABLE IF NOT EXISTS cms_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT DEFAULT 0,
  name VARCHAR(50) NOT NULL,
  icon VARCHAR(500),
  sort_order INT DEFAULT 0,
  status TINYINT DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类表';
CREATE INDEX idx_cms_category_parent ON cms_category(parent_id);

-- 3. cms_tag 标签表
CREATE TABLE IF NOT EXISTS cms_tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL UNIQUE,
  article_count INT DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签表';

-- 4. cms_article 作品表
CREATE TABLE IF NOT EXISTS cms_article (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  author_id BIGINT NOT NULL,
  category_id BIGINT,
  title VARCHAR(200) NOT NULL,
  summary VARCHAR(500),
  content LONGTEXT NOT NULL,
  cover_image VARCHAR(500),
  status TINYINT NOT NULL DEFAULT 0,
  is_top TINYINT DEFAULT 0,
  visibility TINYINT NOT NULL DEFAULT 0 COMMENT '可见性：0=公开，1=仅粉丝，2=互相关注，3=仅自己',
  is_recommend TINYINT DEFAULT 0,
  view_count INT DEFAULT 0,
  like_count INT DEFAULT 0,
  comment_count INT DEFAULT 0,
  collect_count INT DEFAULT 0,
  share_count INT DEFAULT 0,
  hot_score DECIMAL(10,2) DEFAULT 0,
  publish_at DATETIME,
  reject_reason VARCHAR(500),
  device_info VARCHAR(100),
  location VARCHAR(255) COMMENT '发布位置',
  ip_address VARCHAR(45) COMMENT '发布IP属地',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME,
  FOREIGN KEY (author_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  FOREIGN KEY (category_id) REFERENCES cms_category(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章/作品表';
CREATE INDEX idx_cms_article_author ON cms_article(author_id);
CREATE INDEX idx_cms_article_category ON cms_article(category_id);
CREATE INDEX idx_cms_article_status ON cms_article(status);
CREATE INDEX idx_cms_article_publish ON cms_article(publish_at);
CREATE INDEX idx_cms_article_hot ON cms_article(hot_score DESC);
CREATE INDEX idx_cms_article_deleted ON cms_article(deleted_at);
CREATE INDEX idx_cms_article_visibility ON cms_article(visibility);

-- 5. cms_article_tag 作品标签关联表
CREATE TABLE IF NOT EXISTS cms_article_tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  article_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  UNIQUE KEY uk_article_tag (article_id, tag_id),
  FOREIGN KEY (article_id) REFERENCES cms_article(id) ON DELETE CASCADE,
  FOREIGN KEY (tag_id) REFERENCES cms_tag(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作品标签关联表';
CREATE INDEX idx_cms_article_tag_article ON cms_article_tag(article_id);
CREATE INDEX idx_cms_article_tag_tag ON cms_article_tag(tag_id);

-- 6. cms_article_image 作品图片表
CREATE TABLE IF NOT EXISTS cms_article_image (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  article_id BIGINT NOT NULL,
  url VARCHAR(500) NOT NULL,
  type TINYINT NOT NULL,
  width INT,
  height INT,
  sort_order INT DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (article_id) REFERENCES cms_article(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作品图片表';
CREATE INDEX idx_cms_article_image_article ON cms_article_image(article_id);

-- 7. cms_like 点赞表
CREATE TABLE IF NOT EXISTS cms_like (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  target_id BIGINT NOT NULL,
  target_type TINYINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_like (user_id, target_id, target_type),
  FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点赞表';
CREATE INDEX idx_cms_like_user ON cms_like(user_id);
CREATE INDEX idx_cms_like_target ON cms_like(target_id, target_type);

-- 8. cms_collect 收藏表
CREATE TABLE IF NOT EXISTS cms_collect (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  article_id BIGINT NOT NULL,
  group_name VARCHAR(50) DEFAULT '默认',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_collect (user_id, article_id),
  FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  FOREIGN KEY (article_id) REFERENCES cms_article(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';
CREATE INDEX idx_cms_collect_user ON cms_collect(user_id);
CREATE INDEX idx_cms_collect_article ON cms_collect(article_id);

-- 9. cms_comment 评论表
CREATE TABLE IF NOT EXISTS cms_comment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  article_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  parent_id BIGINT DEFAULT 0,
  reply_to_id BIGINT DEFAULT 0,
  content VARCHAR(1000) NOT NULL,
  status TINYINT DEFAULT 1,
  like_count INT DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at DATETIME,
  FOREIGN KEY (article_id) REFERENCES cms_article(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';
CREATE INDEX idx_cms_comment_article ON cms_comment(article_id);
CREATE INDEX idx_cms_comment_user ON cms_comment(user_id);
CREATE INDEX idx_cms_comment_parent ON cms_comment(parent_id);

-- 10. cms_view_history 浏览记录表
CREATE TABLE IF NOT EXISTS cms_view_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  article_id BIGINT NOT NULL,
  ip VARCHAR(50),
  user_agent VARCHAR(500),
  view_duration INT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE SET NULL,
  FOREIGN KEY (article_id) REFERENCES cms_article(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='浏览记录表';
CREATE INDEX idx_cms_view_history_user ON cms_view_history(user_id);
CREATE INDEX idx_cms_view_history_article ON cms_view_history(article_id);
CREATE INDEX idx_cms_view_history_created ON cms_view_history(created_at);

-- 11. sys_sensitive_word 敏感词表
CREATE TABLE IF NOT EXISTS sys_sensitive_word (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  word VARCHAR(100) NOT NULL UNIQUE,
  level TINYINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='敏感词表';

-- 12. sys_operation_log 操作日志表
CREATE TABLE IF NOT EXISTS sys_operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  operator_id BIGINT,
  module VARCHAR(50) NOT NULL,
  action VARCHAR(50) NOT NULL,
  target_type VARCHAR(50),
  target_id BIGINT,
  detail TEXT,
  ip VARCHAR(50),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (operator_id) REFERENCES sys_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';
CREATE INDEX idx_sys_operation_log_operator ON sys_operation_log(operator_id);
CREATE INDEX idx_sys_operation_log_created ON sys_operation_log(created_at);

-- 13. sys_login_log 登录日志表
CREATE TABLE IF NOT EXISTS sys_login_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  username VARCHAR(50) NOT NULL,
  status TINYINT NOT NULL,
  ip VARCHAR(50),
  location VARCHAR(100),
  user_agent VARCHAR(500),
  fail_reason VARCHAR(200),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';
CREATE INDEX idx_sys_login_log_user ON sys_login_log(user_id);
CREATE INDEX idx_sys_login_log_created ON sys_login_log(created_at);

-- 14. sys_notification 通知消息表
CREATE TABLE IF NOT EXISTS sys_notification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  sender_id BIGINT DEFAULT NULL COMMENT '发送者ID（管理员发送时为管理员ID，系统触发时为null）',
  type TINYINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  content VARCHAR(1000),
  is_read TINYINT DEFAULT 0,
  related_id BIGINT,
  group_key VARCHAR(100),
  target_all TINYINT DEFAULT 0 COMMENT '是否群发通知：0-否，1-是',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知消息表';
CREATE INDEX idx_sys_notification_user ON sys_notification(user_id);
CREATE INDEX idx_sys_notification_read ON sys_notification(is_read);
CREATE INDEX idx_sys_notification_group_key ON sys_notification(user_id, group_key);
CREATE INDEX idx_sys_notification_sender ON sys_notification(sender_id);

-- 15. user_follow 关注表
CREATE TABLE IF NOT EXISTS user_follow (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  follower_id BIGINT NOT NULL,
  following_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_follow (follower_id, following_id),
  FOREIGN KEY (follower_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  FOREIGN KEY (following_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关注表';
CREATE INDEX idx_user_follow_follower ON user_follow(follower_id);
CREATE INDEX idx_user_follow_following ON user_follow(following_id);

-- 16. user_message 私信表
CREATE TABLE IF NOT EXISTS user_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sender_id BIGINT NOT NULL,
  receiver_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  type VARCHAR(20) NOT NULL DEFAULT 'text' COMMENT '消息类型: text/image/voice/file/system',
  is_read TINYINT DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- 不使用外键约束：允许 AI 助手（sender_id=0）发送私信，避免级联删除丢失消息
  -- FOREIGN KEY (sender_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  -- FOREIGN KEY (receiver_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私信表';
CREATE INDEX idx_user_message_sender ON user_message(sender_id);
CREATE INDEX idx_user_message_receiver ON user_message(receiver_id);
CREATE INDEX idx_user_message_read ON user_message(is_read);

-- 17. user_settings 用户偏好设置表
CREATE TABLE IF NOT EXISTS user_settings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  notify_system TINYINT DEFAULT 1,
  notify_interact TINYINT DEFAULT 1,
  notify_message TINYINT DEFAULT 1,
  notify_follow TINYINT DEFAULT 1,
  show_online_status TINYINT DEFAULT 1,
  message_permission TINYINT DEFAULT 0,
  save_view_history TINYINT DEFAULT 1,
  content_recommend INT DEFAULT 1 COMMENT '内容推荐: 0关闭 1开启',
  auto_play_video INT DEFAULT 1 COMMENT '自动播放视频: 0关闭 1仅WiFi 2始终',
  quiet_hours_enabled INT DEFAULT 0 COMMENT '免打扰: 0关闭 1开启',
  quiet_hours_start VARCHAR(5) DEFAULT '22:00' COMMENT '免打扰开始时间',
  quiet_hours_end VARCHAR(5) DEFAULT '08:00' COMMENT '免打扰结束时间',
  show_view_count INT DEFAULT 1 COMMENT '显示阅读量: 0关闭 1开启',
  allow_search INT DEFAULT 1 COMMENT '允许被搜索: 0关闭 1开启',
  font_size TINYINT DEFAULT 1,
  theme VARCHAR(20) DEFAULT 'light',
  language VARCHAR(10) DEFAULT 'zh-CN',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户偏好设置表';

-- 18. user_preferred_category 用户偏好分类关联表
CREATE TABLE IF NOT EXISTS user_preferred_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  type TINYINT NOT NULL,
  UNIQUE KEY uk_preferred_category (user_id, category_id, type),
  FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  FOREIGN KEY (category_id) REFERENCES cms_category(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户偏好分类关联表';
CREATE INDEX idx_user_preferred_category_user ON user_preferred_category(user_id);

-- 19. user_preferred_tag 用户偏好标签关联表
CREATE TABLE IF NOT EXISTS user_preferred_tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  type TINYINT NOT NULL,
  UNIQUE KEY uk_preferred_tag (user_id, tag_id, type),
  FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  FOREIGN KEY (tag_id) REFERENCES cms_tag(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户偏好标签关联表';
CREATE INDEX idx_user_preferred_tag_user ON user_preferred_tag(user_id);

-- ============================================================
-- V2: 新功能模块（话题/群组/主页主题/模板/举报/协作）
-- ============================================================

-- 20. cms_topic 话题表
CREATE TABLE IF NOT EXISTS cms_topic (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL COMMENT '话题名称',
    description VARCHAR(500) DEFAULT NULL,
    cover_image VARCHAR(500) DEFAULT NULL,
    article_count BIGINT DEFAULT 0,
    follow_count BIGINT DEFAULT 0,
    hot_score DECIMAL(10,2) DEFAULT 0.00,
    is_official INT DEFAULT 0 COMMENT '是否官方话题',
    status INT DEFAULT 0 COMMENT '0正常 1隐藏',
    creator_id BIGINT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_topic_name (name),
    INDEX idx_topic_hot (hot_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='话题表';

-- 21. cms_topic_article 话题-文章关联表
CREATE TABLE IF NOT EXISTS cms_topic_article (
    id BIGINT NOT NULL AUTO_INCREMENT,
    topic_id BIGINT NOT NULL,
    article_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_topic_article (topic_id, article_id),
    INDEX idx_topic_article_article (article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='话题文章关联表';

-- 22. cms_topic_follow 话题关注表
CREATE TABLE IF NOT EXISTS cms_topic_follow (
    id BIGINT NOT NULL AUTO_INCREMENT,
    topic_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_user_topic (user_id, topic_id),
    INDEX idx_topic_follow_topic (topic_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='话题关注表';

-- 23. cms_group_info 群组表
CREATE TABLE IF NOT EXISTS cms_group_info (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    group_number VARCHAR(10) DEFAULT NULL COMMENT '群号(6-10位纯数字)',
    avatar VARCHAR(500) DEFAULT NULL,
    description VARCHAR(500) DEFAULT NULL,
    owner_id BIGINT NOT NULL,
    member_count BIGINT DEFAULT 1,
    max_members INT DEFAULT 200,
    is_public INT DEFAULT 1 COMMENT '是否公开',
    status INT DEFAULT 0 COMMENT '0正常 1已解散',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_group_number (group_number),
    INDEX idx_group_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组表';

-- 24. cms_group_member 群组成员表
CREATE TABLE IF NOT EXISTS cms_group_member (
    id BIGINT NOT NULL AUTO_INCREMENT,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role INT DEFAULT 0 COMMENT '0成员 1管理员 2群主',
    nickname VARCHAR(50) DEFAULT NULL COMMENT '群内昵称',
    unread_count INT DEFAULT 0,
    is_muted INT DEFAULT 0,
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_group_user (group_id, user_id),
    INDEX idx_group_member_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组成员表';

-- 25. cms_group_message 群组消息表
CREATE TABLE IF NOT EXISTS cms_group_message (
    id BIGINT NOT NULL AUTO_INCREMENT,
    group_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content TEXT NOT NULL COMMENT '消息内容(AES加密)',
    message_type VARCHAR(20) DEFAULT 'text' COMMENT 'text/image/voice/file/system',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_group_msg_group (group_id),
    INDEX idx_group_msg_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组消息表';

-- 30. user_profile_theme 用户主页主题表
CREATE TABLE IF NOT EXISTS user_profile_theme (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    theme_color VARCHAR(10) DEFAULT '#3b82f6',
    background_image VARCHAR(500) DEFAULT NULL,
    background_style VARCHAR(20) DEFAULT 'cover',
    font_family VARCHAR(100) DEFAULT NULL,
    bio_bg_color VARCHAR(10) DEFAULT NULL,
    card_style VARCHAR(50) DEFAULT 'default',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_theme_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户主页主题表';

-- 31. cms_template 内容模板表
CREATE TABLE IF NOT EXISTS cms_template (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    cover_image VARCHAR(500) DEFAULT NULL,
    category VARCHAR(50) DEFAULT NULL,
    content TEXT NOT NULL COMMENT '模板内容(markdown)',
    tags VARCHAR(200) DEFAULT NULL,
    use_count BIGINT DEFAULT 0,
    is_official INT DEFAULT 0,
    creator_id BIGINT DEFAULT NULL,
    status INT DEFAULT 0 COMMENT '0正常 1隐藏',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_template_category (category),
    INDEX idx_template_use (use_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容模板表';

-- 32. cms_article_report 文章举报表
CREATE TABLE IF NOT EXISTS cms_article_report (
    id BIGINT NOT NULL AUTO_INCREMENT,
    article_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    reason VARCHAR(100) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    status INT DEFAULT 0 COMMENT '0待处理 1已忽略 2已删除',
    handled_by BIGINT DEFAULT NULL,
    handled_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_report_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章举报表';

-- 33. sys_user_report 用户举报表
CREATE TABLE IF NOT EXISTS sys_user_report (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reported_user_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    reason VARCHAR(100) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    status INT DEFAULT 0 COMMENT '0待处理 1已忽略 2已封禁',
    handled_by BIGINT DEFAULT NULL,
    handled_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_user_report_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户举报表';

-- 34. cms_article_collaborator 文章协作表
CREATE TABLE IF NOT EXISTS cms_article_collaborator (
    id BIGINT NOT NULL AUTO_INCREMENT,
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    permission VARCHAR(10) DEFAULT 'edit' COMMENT 'edit/view',
    status INT DEFAULT 0 COMMENT '0待接受 1已接受 2已拒绝',
    invited_by BIGINT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_article_user_collab (article_id, user_id),
    INDEX idx_collaborator_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章协作表';

-- 35. sys_sensitive_whitelist 敏感词白名单表
CREATE TABLE IF NOT EXISTS sys_sensitive_whitelist (
    id BIGINT NOT NULL AUTO_INCREMENT,
    word VARCHAR(100) NOT NULL COMMENT '白名单词',
    created_by BIGINT DEFAULT NULL COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_whitelist_word (word),
    FOREIGN KEY (created_by) REFERENCES sys_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='敏感词白名单表';

-- 36. user_tag_follow 用户关注标签表
CREATE TABLE IF NOT EXISTS user_tag_follow (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_user_tag_follow (user_id, tag_id),
    INDEX idx_user_tag_follow_user (user_id),
    INDEX idx_user_tag_follow_tag (tag_id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES cms_tag(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注标签表';

-- 37. cms_comment_report 评论举报表
CREATE TABLE IF NOT EXISTS cms_comment_report (
    id BIGINT NOT NULL AUTO_INCREMENT,
    comment_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    reason VARCHAR(500) DEFAULT NULL COMMENT '举报原因',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1已忽略 2已删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_cms_comment_report_comment (comment_id),
    INDEX idx_cms_comment_report_reporter (reporter_id),
    INDEX idx_cms_comment_report_status (status),
    FOREIGN KEY (comment_id) REFERENCES cms_comment(id) ON DELETE CASCADE,
    FOREIGN KEY (reporter_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论举报表';

-- 38. sys_security_audit_log 安全审计日志表
CREATE TABLE IF NOT EXISTS sys_security_audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_type VARCHAR(50) NOT NULL COMMENT '事件类型',
    user_id BIGINT DEFAULT NULL COMMENT '操作人',
    ip VARCHAR(50) DEFAULT NULL COMMENT '操作IP',
    method VARCHAR(10) DEFAULT NULL COMMENT '请求方法',
    path VARCHAR(500) DEFAULT NULL COMMENT '请求路径',
    detail TEXT DEFAULT NULL COMMENT '详情',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_sys_security_audit_log_event_type (event_type),
    INDEX idx_sys_security_audit_log_user_id (user_id),
    INDEX idx_sys_security_audit_log_ip (ip),
    INDEX idx_sys_security_audit_log_created (created_at),
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='安全审计日志表';

-- 39. cms_banner 轮播图/横幅表
CREATE TABLE IF NOT EXISTS cms_banner (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL COMMENT '标题',
    image_url VARCHAR(500) NOT NULL COMMENT '图片地址',
    link_url VARCHAR(500) DEFAULT NULL COMMENT '跳转链接',
    link_type TINYINT DEFAULT 1 COMMENT '链接类型：1=内部 2=外部',
    sort_order INT DEFAULT 0 COMMENT '排序',
    start_time DATETIME DEFAULT NULL COMMENT '开始时间',
    end_time DATETIME DEFAULT NULL COMMENT '结束时间',
    status TINYINT DEFAULT 1 COMMENT '0隐藏 1显示',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_cms_banner_status (status),
    INDEX idx_cms_banner_time (start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='轮播图/横幅表';

-- 40. sys_announcement 系统公告表
CREATE TABLE IF NOT EXISTS sys_announcement (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL COMMENT '公告标题',
    content VARCHAR(2000) NOT NULL COMMENT '公告内容',
    type TINYINT DEFAULT 1 COMMENT '公告类型',
    is_top TINYINT DEFAULT 0 COMMENT '是否置顶',
    start_time DATETIME DEFAULT NULL COMMENT '生效时间',
    end_time DATETIME DEFAULT NULL COMMENT '失效时间',
    status TINYINT DEFAULT 1 COMMENT '0隐藏 1显示',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_sys_announcement_status (status),
    INDEX idx_sys_announcement_time (start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统公告表';

-- 41. cms_danmaku 弹幕表
CREATE TABLE IF NOT EXISTS cms_danmaku (
    id BIGINT NOT NULL AUTO_INCREMENT,
    article_id BIGINT NOT NULL COMMENT '关联作品',
    user_id BIGINT NOT NULL COMMENT '发送用户',
    content VARCHAR(500) NOT NULL COMMENT '弹幕内容',
    color VARCHAR(10) DEFAULT '#FFFFFF' COMMENT '弹幕颜色',
    position VARCHAR(10) DEFAULT 'scroll' COMMENT '弹幕位置：scroll/top/bottom',
    time_point DECIMAL(10,2) DEFAULT 0.00 COMMENT '弹幕时间点(秒)',
    status TINYINT DEFAULT 1 COMMENT '0隐藏 1显示',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_cms_danmaku_article (article_id),
    INDEX idx_cms_danmaku_user (user_id),
    INDEX idx_cms_danmaku_time (article_id, time_point),
    FOREIGN KEY (article_id) REFERENCES cms_article(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='弹幕表';

-- 42. cms_repost 转发表
CREATE TABLE IF NOT EXISTS cms_repost (
    id BIGINT NOT NULL AUTO_INCREMENT,
    article_id BIGINT NOT NULL COMMENT '原作品ID',
    user_id BIGINT NOT NULL COMMENT '转发用户',
    content VARCHAR(500) DEFAULT NULL COMMENT '转发附言',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_cms_repost_article (article_id),
    INDEX idx_cms_repost_user (user_id),
    INDEX idx_cms_repost_created (created_at),
    FOREIGN KEY (article_id) REFERENCES cms_article(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='转发表';

-- ============================================================
-- 兼容性修复：为老版本数据库补充缺失列（MySQL 8.0+，幂等操作）
-- 注意：sys_user 表已在上方 CREATE TABLE 中包含所有列，此处不再修补
-- ============================================================
ALTER TABLE cms_article ADD COLUMN IF NOT EXISTS share_count INT DEFAULT 0 COMMENT '分享数';
ALTER TABLE cms_article ADD COLUMN IF NOT EXISTS device_info VARCHAR(100) NULL COMMENT '发布设备信息';
ALTER TABLE cms_article ADD COLUMN IF NOT EXISTS location VARCHAR(255) NULL COMMENT '发布位置';
ALTER TABLE cms_article ADD COLUMN IF NOT EXISTS ip_address VARCHAR(45) NULL COMMENT 'IP属地';
ALTER TABLE cms_article ADD COLUMN IF NOT EXISTS visibility TINYINT NOT NULL DEFAULT 0 COMMENT '可见性';
