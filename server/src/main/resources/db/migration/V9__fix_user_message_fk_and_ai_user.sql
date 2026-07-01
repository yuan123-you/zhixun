-- V9: 修复 user_message 外键约束，允许 AI 助手（sender_id=0）发送私信
-- 问题：user_message.sender_id 有外键约束引用 sys_user(id)，
--        但 AI 助手消息使用 sender_id=0，sys_user 中无 id=0 的用户，
--        导致 INSERT 失败：SQLIntegrityConstraintViolationException

-- 1. 删除原有的外键约束（sender_id 和 receiver_id 的外键对私信场景不必要，
--    反而阻碍了系统用户/AI助手发送消息，且 ON DELETE CASCADE 会导致删除用户时级联删除私信）
ALTER TABLE user_message DROP FOREIGN KEY user_message_ibfk_1;
ALTER TABLE user_message DROP FOREIGN KEY user_message_ibfk_2;

-- 2. 添加索引保持查询性能（如果已存在则忽略）
-- sender_id 和 receiver_id 的索引已在建表时创建，无需重复添加
