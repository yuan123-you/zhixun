-- V7: Group message enhancements - add mentioned_user_ids column
ALTER TABLE cms_group_message ADD COLUMN mentioned_user_ids VARCHAR(500) DEFAULT NULL COMMENT '@提及的用户ID列表,逗号分隔';
