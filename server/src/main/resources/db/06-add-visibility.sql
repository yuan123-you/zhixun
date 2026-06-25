-- 06-add-visibility.sql
-- 为 cms_article 表添加可见性字段

ALTER TABLE cms_article
ADD COLUMN visibility TINYINT NOT NULL DEFAULT 0 COMMENT '可见性：0=公开，1=仅粉丝，2=互相关注，3=仅自己'
AFTER is_top;

-- 创建索引用于快速查询
CREATE INDEX idx_cms_article_visibility ON cms_article(visibility);
