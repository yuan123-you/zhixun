package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import com.zhixun.common.result.PageResult;
import com.zhixun.common.util.AesUtil;
import com.zhixun.common.util.SensitiveWordUtil;
import com.zhixun.config.RabbitMQConfig;
import com.zhixun.dto.social.MessageSendRequest;
import com.zhixun.entity.User;
import com.zhixun.entity.UserFollow;
import com.zhixun.entity.UserMessage;
import com.zhixun.entity.UserSettings;
import com.zhixun.mapper.UserFollowMapper;
import com.zhixun.mapper.UserMapper;
import com.zhixun.mapper.UserMessageMapper;
import com.zhixun.mapper.UserSettingsMapper;
import com.zhixun.service.MessageService;
import com.zhixun.service.AIService;
import com.zhixun.service.ai.AiResults;
import com.zhixun.dto.ai.AIWriteRequest;
import com.zhixun.vo.AIResponseVO;
import com.zhixun.vo.ConversationVO;
import com.zhixun.vo.MessageVO;
import com.zhixun.websocket.ChatWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.socket.TextMessage;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 私信服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final UserMessageMapper userMessageMapper;
    private final UserMapper userMapper;
    private final UserSettingsMapper userSettingsMapper;
    private final UserFollowMapper userFollowMapper;
    private final AesUtil aesUtil;
    private final SensitiveWordUtil sensitiveWordUtil;
    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final AIService aiService;
    private final ObjectMapper objectMapper;

    /** 未读消息数 Redis Key 前缀 */
    private static final String UNREAD_COUNT_PREFIX = "message:unread:";
    /** 会话摘要 Redis Key 前缀 */
    private static final String CONVERSATION_PREFIX = "message:conversation:";
    /** AI助手头像（data URI） */
    private static final String AI_ASSISTANT_AVATAR = "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MCIgaGVpZ2h0PSI0MCIgdmlld0JveD0iMCAwIDQwIDQwIj48cmVjdCB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIGZpbGw9IiM2MzY2ZjEiIHJ4PSIyMCIvPjx0ZXh0IHg9IjIwIiB5PSIyNiIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZmlsbD0iI2ZmZiIgZm9udC1zaXplPSIxNiIgZm9udC13ZWlnaHQ9ImJvbGQiPkFJPC90ZXh0Pjwvc3ZnPg==";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageVO sendMessage(Long senderId, MessageSendRequest request) {
        Long receiverId = request.getReceiverId();
        String rawContent = request.getContent();

        log.info("私信发送请求: senderId={}, receiverId={}, contentLength={}", senderId, receiverId,
                rawContent != null ? rawContent.length() : 0);

        // 不能给自己发私信
        if (senderId.equals(receiverId)) {
            log.warn("私信发送拒绝: 不能给自己发私信, senderId={}", senderId);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能给自己发私信");
        }

        // 检查接收者是否存在
        User receiver = userMapper.selectById(receiverId);
        if (receiver == null) {
            log.warn("私信发送失败: 接收者不存在, receiverId={}", receiverId);
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "接收者不存在");
        }

        // 检查私信权限（是否允许陌生人私信）
        checkMessagePermission(senderId, receiverId);

        // 敏感词检查
        if (sensitiveWordUtil.containsSensitiveWord(rawContent)) {
            log.warn("私信发送拒绝: 包含敏感词, senderId={}, receiverId={}", senderId, receiverId);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "消息内容包含敏感词，请修改后重新发送");
        }

        // AES-256-GCM 加密消息内容（仅文本类型加密，图片/语音/文件等URL无需加密）
        String encryptedContent;
        String msgType = request.getType() != null ? request.getType() : "text";
        if ("text".equals(msgType)) {
            try {
                encryptedContent = aesUtil.encrypt(rawContent);
            } catch (Exception e) {
                log.error("私信发送失败: AES加密异常, senderId={}, receiverId={}", senderId, receiverId, e);
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "消息发送失败，请稍后重试");
            }
        } else {
            // 非文本类型（image/voice/file/ai_reply等）直接存储明文URL，无需加密
            encryptedContent = rawContent;
        }

        // 创建消息记录
        UserMessage message = new UserMessage();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(encryptedContent);
        message.setType(request.getType() != null ? request.getType() : "text");
        message.setIsRead(0);
        persistMessage(message);

        log.info("私信已插入，等待事务提交: messageId={}, senderId={}, receiverId={}", message.getId(), senderId, receiverId);

        // 构建响应并在事务内捕获脱离实体/响应对象的推送数据，提交后不再查询数据库。
        MessageVO vo = buildMessageVO(message, senderId, receiverId);
        vo.setContent(rawContent);
        Long messageId = message.getId();
        Map<String, Object> notification = Map.of(
                "type", "CHAT",
                "data", Map.of(
                        "receiverId", receiverId,
                        "id", messageId,
                        "senderId", senderId,
                        "content", rawContent,
                        "messageType", message.getType(),
                        "senderNickname", vo.getSenderNickname() != null ? vo.getSenderNickname() : "",
                        "senderAvatar", vo.getSenderAvatar() != null ? vo.getSenderAvatar() : "",
                        "createdAt", message.getCreatedAt().toString()
                )
        );
        afterCommit(() -> publishMessageEffects(senderId, receiverId, messageId, rawContent, notification));

        return vo;
    }

    /** 插入必须成功，时间戳同时用于持久化、响应和推送。 */
    private void persistMessage(UserMessage message) {
        message.setCreatedAt(LocalDateTime.now());
        try {
            if (userMessageMapper.insert(message) != 1 || message.getId() == null) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "消息发送失败，请稍后重试");
            }
        } catch (Exception e) {
            log.error("私信发送失败: 数据库插入异常, senderId={}, receiverId={}",
                    message.getSenderId(), message.getReceiverId(), e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "消息发送失败，请稍后重试");
        }
    }

    private void afterCommit(Runnable effect) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            // REQUIRED 加入外层事务时，回调也等待外层提交；回滚不会执行。
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    effect.run();
                }
            });
        } else {
            effect.run();
        }
    }

    private void publishMessageEffects(Long senderId, Long receiverId, Long messageId,
                                       String rawContent, Map<String, Object> notification) {
        try {
            String unreadKey = UNREAD_COUNT_PREFIX + receiverId + ":" + senderId;
            stringRedisTemplate.opsForValue().increment(unreadKey);
            stringRedisTemplate.expire(unreadKey, 7, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("私信未读缓存更新失败: messageId={}", messageId, e);
        }
        try {
            updateConversationCache(senderId, receiverId, rawContent);
        } catch (Exception e) {
            log.warn("私信会话缓存更新失败: messageId={}", messageId, e);
        }
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.CHAT_EXCHANGE, "zhixun.chat.sent", notification);
            log.info("私信MQ推送成功: messageId={}, receiverId={}", messageId, receiverId);
        } catch (Exception e) {
            log.warn("RabbitMQ 推送私信失败，尝试直推: messageId={}, error={}", messageId, e.getMessage());
            try {
                String json = objectMapper.writeValueAsString(notification);
                ChatWebSocketHandler.sendToUser(receiverId, new TextMessage(json));
                log.info("私信直推成功: messageId={}, receiverId={}", messageId, receiverId);
            } catch (Exception ex) {
                log.error("私信直推也失败: messageId={}, error={}", messageId, ex.getMessage());
            }
        }
    }

    @Override
    public PageResult<ConversationVO> getConversations(Long userId, Integer page, Integer pageSize) {
        // 查询与每个用户的最新消息（按最新消息时间倒序）
        // 先查询发送给当前用户的消息
        LambdaQueryWrapper<UserMessage> receivedWrapper = new LambdaQueryWrapper<>();
        receivedWrapper.eq(UserMessage::getReceiverId, userId)
                .orderByDesc(UserMessage::getCreatedAt);
        List<UserMessage> receivedMessages = userMessageMapper.selectList(receivedWrapper);

        // 查询当前用户发送的消息
        LambdaQueryWrapper<UserMessage> sentWrapper = new LambdaQueryWrapper<>();
        sentWrapper.eq(UserMessage::getSenderId, userId)
                .orderByDesc(UserMessage::getCreatedAt);
        List<UserMessage> sentMessages = userMessageMapper.selectList(sentWrapper);

        // 合并并按对方用户ID分组，取最新消息
        Map<Long, UserMessage> latestMessageMap = new java.util.LinkedHashMap<>();
        for (UserMessage msg : receivedMessages) {
            Long otherUserId = msg.getSenderId();
            if (!latestMessageMap.containsKey(otherUserId) ||
                    msg.getCreatedAt().isAfter(latestMessageMap.get(otherUserId).getCreatedAt())) {
                latestMessageMap.put(otherUserId, msg);
            }
        }
        for (UserMessage msg : sentMessages) {
            Long otherUserId = msg.getReceiverId();
            if (!latestMessageMap.containsKey(otherUserId) ||
                    msg.getCreatedAt().isAfter(latestMessageMap.get(otherUserId).getCreatedAt())) {
                latestMessageMap.put(otherUserId, msg);
            }
        }

        // 按最新消息时间排序
        List<Map.Entry<Long, UserMessage>> sortedEntries = latestMessageMap.entrySet().stream()
                .sorted((a, b) -> b.getValue().getCreatedAt().compareTo(a.getValue().getCreatedAt()))
                .collect(Collectors.toList());

        // 分页
        int total = sortedEntries.size();
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        if (fromIndex >= total) {
            return new PageResult<>(Collections.emptyList(), (long) total, page, pageSize);
        }
        List<Map.Entry<Long, UserMessage>> pagedEntries = sortedEntries.subList(fromIndex, toIndex);

        // 批量查询用户信息
        Set<Long> otherUserIds = pagedEntries.stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(otherUserIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 构建 ConversationVO 列表
        List<ConversationVO> voList = pagedEntries.stream().map(entry -> {
            Long otherUserId = entry.getKey();
            UserMessage latestMsg = entry.getValue();

            ConversationVO vo = new ConversationVO();
            vo.setUserId(otherUserId);

            User otherUser = userMap.get(otherUserId);
            if (otherUser != null) {
                vo.setNickname(otherUser.getNickname());
                vo.setAvatar(otherUser.getAvatar());
            }

            // 解密消息内容（仅文本类型需要解密，非文本类型直接使用原始内容）
            String lastMsgType = latestMsg.getType() != null ? latestMsg.getType() : "text";
            if ("text".equals(lastMsgType)) {
                try {
                    vo.setLastMessage(aesUtil.decrypt(latestMsg.getContent()));
                } catch (Exception e) {
                    // 解密失败时使用原始内容（可能是加密功能上线前的明文消息，或密钥变更后的旧密文）
                    vo.setLastMessage(latestMsg.getContent());
                }
            } else {
                vo.setLastMessage(latestMsg.getContent());
            }

            vo.setLastMessageTime(latestMsg.getCreatedAt());

            // 获取未读数
            String unreadKey = UNREAD_COUNT_PREFIX + userId + ":" + otherUserId;
            String unreadStr = stringRedisTemplate.opsForValue().get(unreadKey);
            vo.setUnreadCount(unreadStr != null ? Integer.parseInt(unreadStr) : 0);

            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(voList, (long) total, page, pageSize);
    }

    @Override
    public PageResult<MessageVO> getMessages(Long userId, Long targetUserId, Integer page, Integer pageSize) {
        // 使用 MyBatis-Plus 分页
        Page<UserMessage> pageParam = new Page<>(page, pageSize);

        LambdaQueryWrapper<UserMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                        .eq(UserMessage::getSenderId, userId).eq(UserMessage::getReceiverId, targetUserId))
                .or(w -> w
                        .eq(UserMessage::getSenderId, targetUserId).eq(UserMessage::getReceiverId, userId));
        wrapper.orderByDesc(UserMessage::getId);

        Page<UserMessage> result = userMessageMapper.selectPage(pageParam, wrapper);
        List<UserMessage> messages = result.getRecords();

        // 标记已读
        markAsRead(userId, targetUserId);

        // 构建 MessageVO 列表
        List<MessageVO> voList = messages.stream()
                .map(msg -> buildMessageVO(msg, userId, targetUserId))
                .collect(Collectors.toList());

        // 反转列表，使最早的消息在前
        Collections.reverse(voList);

        return new PageResult<>(voList, result.getTotal(), page, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long userId, Long targetUserId) {
        // 将对方发给自己的未读消息标记为已读
        LambdaUpdateWrapper<UserMessage> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserMessage::getSenderId, targetUserId)
                .eq(UserMessage::getReceiverId, userId)
                .eq(UserMessage::getIsRead, 0)
                .set(UserMessage::getIsRead, 1);
        userMessageMapper.update(null, updateWrapper);

        // 清除 Redis 未读计数
        String unreadKey = UNREAD_COUNT_PREFIX + userId + ":" + targetUserId;
        stringRedisTemplate.delete(unreadKey);

        // 通过 RabbitMQ 异步通知发送者已读
        try {
            Map<String, Object> mqMessage = Map.of(
                    "type", "READ",
                    "data", Map.of(
                            "receiverId", targetUserId,
                            "readBy", userId
                    )
            );
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CHAT_EXCHANGE,
                    "zhixun.chat.read",
                    mqMessage);
        } catch (Exception e) {
            log.warn("RabbitMQ 推送已读通知失败: {}", e.getMessage());
        }
    }

    @Override
    public Integer getUnreadCount(Long userId) {
        // 查询数据库中的未读消息数
        LambdaQueryWrapper<UserMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserMessage::getReceiverId, userId)
                .eq(UserMessage::getIsRead, 0);
        return Math.toIntExact(userMessageMapper.selectCount(wrapper));
    }

    // ========== 内部方法 ==========

    /**
     * 检查私信权限（是否允许陌生人私信）
     * 业务语义：
     * - messagePermission = 0：允许所有人（包括陌生人）
     * - messagePermission = 1：仅互相关注的人
     */
    private void checkMessagePermission(Long senderId, Long receiverId) {
        // 查询接收者的设置
        LambdaQueryWrapper<UserSettings> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSettings::getUserId, receiverId);
        UserSettings settings = userSettingsMapper.selectOne(wrapper);

        // 未设置或显式允许所有人：直接放行
        if (settings == null || settings.getMessagePermission() == null || settings.getMessagePermission() == 0) {
            return;
        }

        // 设置为"仅互相关注的人"：必须双向关注
        if (settings.getMessagePermission() == 1) {
            boolean isMutualFollow = isMutualFollow(senderId, receiverId);
            if (!isMutualFollow) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "对方仅允许互相关注的人发送私信");
            }
        }
    }

    /**
     * 检查是否互相关注
     */
    private boolean isMutualFollow(Long userId1, Long userId2) {
        boolean user1Follows2 = userFollowMapper.selectCount(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, userId1)
                        .eq(UserFollow::getFollowingId, userId2)) > 0;
        boolean user2Follows1 = userFollowMapper.selectCount(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, userId2)
                        .eq(UserFollow::getFollowingId, userId1)) > 0;
        return user1Follows2 && user2Follows1;
    }

    /**
     * 更新会话摘要缓存
     */
    private void updateConversationCache(Long userId1, Long userId2, String lastMessage) {
        String key1 = CONVERSATION_PREFIX + userId1 + ":" + userId2;
        String key2 = CONVERSATION_PREFIX + userId2 + ":" + userId1;
        String value = lastMessage.length() > 50 ? lastMessage.substring(0, 50) : lastMessage;
        stringRedisTemplate.opsForValue().set(key1, value, 7, TimeUnit.DAYS);
        stringRedisTemplate.opsForValue().set(key2, value, 7, TimeUnit.DAYS);
    }

    /**
     * 发送AI助手消息（私信场景）
     * 调用AI服务生成回复，以AI助手身份(senderId=0)保存到会话中
     */
    @Override
    public MessageVO sendAIMessage(Long senderId, Long targetUserId, String question) {
        // 1. 校验接收者存在
        User receiver = userMapper.selectById(targetUserId);
        if (receiver == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        // 2. 调用AI服务
        AIWriteRequest aiReq = new AIWriteRequest();
        aiReq.setPrompt(question);
        aiReq.setMode("chat");

        AIResponseVO aiResp;
        try {
            aiResp = aiService.generateText(aiReq);
        } catch (Exception e) {
            log.error("AI服务调用异常: senderId={}, targetUserId={}, question={}", senderId, targetUserId, question, e);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 服务暂时不可用，请稍后再试");
        }

        String aiContent = AiResults.requireFinal(aiResp).getContent();

        // 3. 保存AI回复消息（senderId=targetUserId 让其归属到当前对话，避免在会话列表中产生独立条目）
        UserMessage message = new UserMessage();
        message.setSenderId(targetUserId); // 归属到目标用户的会话（前端通过 type=ai_reply 识别AI身份）
        message.setReceiverId(senderId); // AI回复给提问者
        message.setType("ai_reply");
        message.setContent(aiContent); // AI回复不加密，直接存储明文
        message.setIsRead(0);
        persistMessage(message);

        // 4. 构建VO，覆盖AI助手头像和昵称
        MessageVO vo = buildMessageVO(message, senderId, targetUserId);
        vo.setSenderNickname("AI助手");
        vo.setSenderAvatar(AI_ASSISTANT_AVATAR);
        vo.setContent(aiContent); // 返回明文

        return vo;
    }

    /**
     * 构建消息 VO
     */
    private MessageVO buildMessageVO(UserMessage message, Long currentUserId, Long targetUserId) {
        MessageVO vo = new MessageVO();
        vo.setId(message.getId());
        vo.setSenderId(message.getSenderId());
        vo.setReceiverId(message.getReceiverId());
        vo.setType(message.getType() != null ? message.getType() : "text");
        vo.setIsRead(message.getIsRead());
        vo.setCreatedAt(message.getCreatedAt());

        // 解密消息内容（仅文本类型需要解密，非文本类型直接使用原始内容）
        String msgType = message.getType() != null ? message.getType() : "text";
        if ("text".equals(msgType)) {
            try {
                vo.setContent(aesUtil.decrypt(message.getContent()));
            } catch (Exception e) {
                // 解密失败时使用原始内容（可能是加密功能上线前的明文消息，或密钥变更后的旧密文）
                vo.setContent(message.getContent());
            }
        } else {
            // 非文本类型（image/voice/file/ai_reply等）存储时未加密，直接使用原始内容
            vo.setContent(message.getContent());
        }

        // 查询发送者信息（AI回复消息固定使用AI助手头像和昵称，不受 senderId 影响）
        if ("ai_reply".equals(msgType)) {
            vo.setSenderNickname("AI助手");
            vo.setSenderAvatar(AI_ASSISTANT_AVATAR);
        } else {
            User sender = userMapper.selectById(message.getSenderId());
            if (sender != null) {
                vo.setSenderNickname(sender.getNickname());
                vo.setSenderAvatar(sender.getAvatar());
            }
        }

        User receiver = userMapper.selectById(message.getReceiverId());
        if (receiver != null) {
            vo.setReceiverNickname(receiver.getNickname());
            vo.setReceiverAvatar(receiver.getAvatar());
        }

        return vo;
    }
}
