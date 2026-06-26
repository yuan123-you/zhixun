-- 知讯平台数据库建表脚本 (MySQL 8.0+)

-- 1. sys_user 用户表
CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  nickname VARCHAR(50),
  avatar VARCHAR(500),
  email VARCHAR(100) UNIQUE,
  phone VARCHAR(100) UNIQUE,
  role VARCHAR(20) NOT NULL DEFAULT 'USER',
  status TINYINT NOT NULL DEFAULT 1,
  bio VARCHAR(500),
  is_online TINYINT DEFAULT 0,
  last_active_at DATETIME,
  follow_count INT DEFAULT 0,
  follower_count INT DEFAULT 0,
  article_count INT DEFAULT 0,
  last_login_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_sys_user_role ON sys_user(role);
CREATE INDEX idx_sys_user_status ON sys_user(status);

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_cms_category_parent ON cms_category(parent_id);

-- 3. cms_tag 标签表
CREATE TABLE IF NOT EXISTS cms_tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL UNIQUE,
  article_count INT DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
  is_recommend TINYINT DEFAULT 0,
  view_count INT DEFAULT 0,
  like_count INT DEFAULT 0,
  comment_count INT DEFAULT 0,
  collect_count INT DEFAULT 0,
  hot_score DECIMAL(10,2) DEFAULT 0,
  publish_at DATETIME,
  reject_reason VARCHAR(500),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME,
  FOREIGN KEY (author_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  FOREIGN KEY (category_id) REFERENCES cms_category(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_cms_article_author ON cms_article(author_id);
CREATE INDEX idx_cms_article_category ON cms_article(category_id);
CREATE INDEX idx_cms_article_status ON cms_article(status);
CREATE INDEX idx_cms_article_publish ON cms_article(publish_at);
CREATE INDEX idx_cms_article_hot ON cms_article(hot_score DESC);
CREATE INDEX idx_cms_article_deleted ON cms_article(deleted_at);

-- 5. cms_article_tag 作品标签关联表
CREATE TABLE IF NOT EXISTS cms_article_tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  article_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  UNIQUE KEY uk_article_tag (article_id, tag_id),
  FOREIGN KEY (article_id) REFERENCES cms_article(id) ON DELETE CASCADE,
  FOREIGN KEY (tag_id) REFERENCES cms_tag(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_cms_view_history_user ON cms_view_history(user_id);
CREATE INDEX idx_cms_view_history_article ON cms_view_history(article_id);
CREATE INDEX idx_cms_view_history_created ON cms_view_history(created_at);

-- 11. sys_sensitive_word 敏感词表
CREATE TABLE IF NOT EXISTS sys_sensitive_word (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  word VARCHAR(100) NOT NULL UNIQUE,
  level TINYINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_sys_login_log_user ON sys_login_log(user_id);
CREATE INDEX idx_sys_login_log_created ON sys_login_log(created_at);

-- 14. sys_notification 通知消息表
CREATE TABLE IF NOT EXISTS sys_notification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  type TINYINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  content VARCHAR(1000),
  is_read TINYINT DEFAULT 0,
  related_id BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_sys_notification_user ON sys_notification(user_id);
CREATE INDEX idx_sys_notification_read ON sys_notification(is_read);

-- 15. user_follow 关注表
CREATE TABLE IF NOT EXISTS user_follow (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  follower_id BIGINT NOT NULL,
  following_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_follow (follower_id, following_id),
  FOREIGN KEY (follower_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  FOREIGN KEY (following_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_user_follow_follower ON user_follow(follower_id);
CREATE INDEX idx_user_follow_following ON user_follow(following_id);

-- 16. user_message 私信表
CREATE TABLE IF NOT EXISTS user_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sender_id BIGINT NOT NULL,
  receiver_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  is_read TINYINT DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (sender_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  FOREIGN KEY (receiver_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  font_size TINYINT DEFAULT 1,
  theme VARCHAR(20) DEFAULT 'light',
  language VARCHAR(10) DEFAULT 'zh-CN',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 18. user_preferred_category 用户偏好分类关联表
CREATE TABLE IF NOT EXISTS user_preferred_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  type TINYINT NOT NULL,
  UNIQUE KEY uk_preferred_category (user_id, category_id, type),
  FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  FOREIGN KEY (category_id) REFERENCES cms_category(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_user_preferred_tag_user ON user_preferred_tag(user_id);
