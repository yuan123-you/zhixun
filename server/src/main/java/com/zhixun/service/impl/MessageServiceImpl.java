package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import com.zhixun.common.result.PageResult;
import com.zhixun.common.util.AesUtil;
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
import com.zhixun.vo.ConversationVO;
import com.zhixun.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;

    /** 未读消息数 Redis Key 前缀 */
    private static final String UNREAD_COUNT_PREFIX = "message:unread:";
    /** 会话摘要 Redis Key 前缀 */
    private static final String CONVERSATION_PREFIX = "message:conversation:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageVO sendMessage(Long senderId, MessageSendRequest request) {
        Long receiverId = request.getReceiverId();

        // 不能给自己发私信
        if (senderId.equals(receiverId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能给自己发私信");
        }

        // 检查接收者是否存在
        User receiver = userMapper.selectById(receiverId);
        if (receiver == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "接收者不存在");
        }

        // 检查私信权限（是否允许陌生人私信）
        checkMessagePermission(senderId, receiverId);

        // AES-256-GCM 加密消息内容
        String encryptedContent = aesUtil.encrypt(request.getContent());

        // 创建消息记录
        UserMessage message = new UserMessage();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(encryptedContent);
        message.setIsRead(0);
        userMessageMapper.insert(message);

        // 更新 Redis 未读计数
        String unreadKey = UNREAD_COUNT_PREFIX + receiverId + ":" + senderId;
        stringRedisTemplate.opsForValue().increment(unreadKey);
        stringRedisTemplate.expire(unreadKey, 7, TimeUnit.DAYS);

        // 更新会话摘要缓存
        updateConversationCache(senderId, receiverId, request.getContent());

        // 构建 MessageVO
        MessageVO vo = buildMessageVO(message, senderId, receiverId);
        vo.setContent(request.getContent()); // 返回明文给发送者

        // 通过 RabbitMQ 异步推送给接收者
        try {
            Map<String, Object> mqMessage = Map.of(
                    "type", "CHAT",
                    "data", Map.of(
                            "receiverId", receiverId,
                            "id", message.getId(),
                            "senderId", senderId,
                            "content", request.getContent(),
                            "createdAt", message.getCreatedAt().toString()
                    )
            );
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CHAT_EXCHANGE,
                    "zhixun.chat.sent",
                    mqMessage);
        } catch (Exception e) {
            log.warn("RabbitMQ 推送私信失败: {}", e.getMessage());
        }

        return vo;
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

            // 解密消息内容
            try {
                vo.setLastMessage(aesUtil.decrypt(latestMsg.getContent()));
            } catch (Exception e) {
                vo.setLastMessage("[消息解密失败]");
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
    public PageResult<MessageVO> getMessages(Long userId, Long targetUserId, Long beforeId, Integer limit) {
        LambdaQueryWrapper<UserMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                        .eq(UserMessage::getSenderId, userId).eq(UserMessage::getReceiverId, targetUserId))
                .or(w -> w
                        .eq(UserMessage::getSenderId, targetUserId).eq(UserMessage::getReceiverId, userId));

        if (beforeId != null) {
            wrapper.lt(UserMessage::getId, beforeId);
        }

        wrapper.orderByDesc(UserMessage::getId);
        wrapper.last("LIMIT " + limit);

        List<UserMessage> messages = userMessageMapper.selectList(wrapper);

        // 标记已读
        markAsRead(userId, targetUserId);

        // 构建 MessageVO 列表
        List<MessageVO> voList = messages.stream()
                .map(msg -> buildMessageVO(msg, userId, targetUserId))
                .collect(Collectors.toList());

        // 反转列表，使最早的消息在前
        Collections.reverse(voList);

        return new PageResult<>(voList, (long) messages.size(), 1, limit);
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
     */
    private void checkMessagePermission(Long senderId, Long receiverId) {
        // 查询接收者的设置
        LambdaQueryWrapper<UserSettings> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSettings::getUserId, receiverId);
        UserSettings settings = userSettingsMapper.selectOne(wrapper);

        // 如果接收者关闭了私信权限，且双方不是互相关注
        if (settings != null && settings.getMessagePermission() != null && settings.getMessagePermission() == 0) {
            // 检查是否互相关注
            boolean isMutualFollow = isMutualFollow(senderId, receiverId);
            if (!isMutualFollow) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "对方未开启陌生人私信");
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
     * 构建消息 VO
     */
    private MessageVO buildMessageVO(UserMessage message, Long currentUserId, Long targetUserId) {
        MessageVO vo = new MessageVO();
        vo.setId(message.getId());
        vo.setSenderId(message.getSenderId());
        vo.setReceiverId(message.getReceiverId());
        vo.setIsRead(message.getIsRead());
        vo.setCreatedAt(message.getCreatedAt());

        // 解密消息内容
        try {
            vo.setContent(aesUtil.decrypt(message.getContent()));
        } catch (Exception e) {
            vo.setContent("[消息解密失败]");
        }

        // 查询发送者和接收者信息
        User sender = userMapper.selectById(message.getSenderId());
        if (sender != null) {
            vo.setSenderNickname(sender.getNickname());
            vo.setSenderAvatar(sender.getAvatar());
        }

        User receiver = userMapper.selectById(message.getReceiverId());
        if (receiver != null) {
            vo.setReceiverNickname(receiver.getNickname());
            vo.setReceiverAvatar(receiver.getAvatar());
        }

        return vo;
    }
}
