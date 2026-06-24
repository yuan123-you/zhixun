package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.common.result.PageResult;
import com.zhixun.entity.Notification;
import com.zhixun.entity.User;
import com.zhixun.entity.UserSettings;
import com.zhixun.enums.NotificationTypeEnum;
import com.zhixun.mapper.NotificationMapper;
import com.zhixun.mapper.UserMapper;
import com.zhixun.mapper.UserSettingsMapper;
import com.zhixun.service.EmailService;
import com.zhixun.service.NotificationService;
import com.zhixun.vo.NotificationVO;
import com.zhixun.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 通知服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final UserSettingsMapper userSettingsMapper;
    private final UserMapper userMapper;
    private final EmailService emailService;

    /** 未读通知数 Redis Key 前缀 */
    private static final String UNREAD_COUNT_PREFIX = "notification:unread:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createNotification(Long userId, Integer type, String title, String content, Long relatedId) {
        createNotification(userId, type, title, content, relatedId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createNotification(Long userId, Integer type, String title, String content, Long relatedId, String groupKey) {
        NotificationTypeEnum typeEnum = NotificationTypeEnum.values()[type - 1];

        // 检查用户通知偏好设置
        if (!isNotificationAllowed(userId, typeEnum)) {
            log.debug("用户 {} 已关闭 {} 类型通知，跳过创建", userId, typeEnum.getDescription());
            return;
        }

        // 分组逻辑：如果存在相同 groupKey 的未读通知，则更新该通知的内容和计数
        if (groupKey != null) {
            LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Notification::getUserId, userId)
                    .eq(Notification::getGroupKey, groupKey)
                    .eq(Notification::getIsRead, 0)
                    .orderByDesc(Notification::getCreatedAt)
                    .last("LIMIT 1");
            Notification existing = notificationMapper.selectOne(wrapper);
            if (existing != null) {
                // 更新已有分组通知的标题和内容
                existing.setTitle(title);
                existing.setContent(content);
                existing.setRelatedId(relatedId);
                notificationMapper.updateById(existing);

                // 推送通知（仍需推送以提醒用户）
                pushNotification(userId, existing, type);
                // 异步发送邮件通知
                sendEmailNotificationAsync(userId, typeEnum, title, content);
                return;
            }
        }

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(typeEnum);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setIsRead(0);
        notification.setRelatedId(relatedId);
        notification.setGroupKey(groupKey);

        notificationMapper.insert(notification);

        // 更新 Redis 未读计数
        String unreadKey = UNREAD_COUNT_PREFIX + userId;
        stringRedisTemplate.opsForValue().increment(unreadKey);
        stringRedisTemplate.expire(unreadKey, 7, TimeUnit.DAYS);

        // 通过 RabbitMQ 异步推送通知
        pushNotification(userId, notification, type);

        // 异步发送邮件通知
        sendEmailNotificationAsync(userId, typeEnum, title, content);
    }

    @Override
    public PageResult<NotificationVO> getNotifications(Long userId, Integer type, Integer page, Integer pageSize) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId);

        // 按类型筛选
        if (type != null) {
            NotificationTypeEnum typeEnum = NotificationTypeEnum.values()[type - 1];
            wrapper.eq(Notification::getType, typeEnum);
        }

        wrapper.orderByDesc(Notification::getCreatedAt);

        Page<Notification> notificationPage = new Page<>(page, pageSize);
        Page<Notification> result = notificationMapper.selectPage(notificationPage, wrapper);

        List<NotificationVO> voList = result.getRecords().stream()
                .map(this::buildNotificationVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), page, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            return;
        }

        // 权限校验
        if (!notification.getUserId().equals(userId)) {
            return;
        }

        if (notification.getIsRead() == 0) {
            notification.setIsRead(1);
            notificationMapper.updateById(notification);

            // 减少 Redis 未读计数
            decrementUnreadCount(userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Long userId) {
        // 批量更新为已读
        LambdaUpdateWrapper<Notification> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1);
        notificationMapper.update(null, updateWrapper);

        // 清除 Redis 未读计数
        String unreadKey = UNREAD_COUNT_PREFIX + userId;
        stringRedisTemplate.delete(unreadKey);
    }

    @Override
    public Integer getUnreadCount(Long userId) {
        // 先查 Redis 缓存
        String unreadKey = UNREAD_COUNT_PREFIX + userId;
        String countStr = stringRedisTemplate.opsForValue().get(unreadKey);
        if (countStr != null) {
            return Integer.parseInt(countStr);
        }

        // 缓存未命中，查数据库
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0);
        int count = Math.toIntExact(notificationMapper.selectCount(wrapper));

        // 写入缓存
        stringRedisTemplate.opsForValue().set(unreadKey, String.valueOf(count), 7, TimeUnit.DAYS);
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotification(Long userId, Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            return;
        }

        // 权限校验：只能删除自己的通知
        if (!notification.getUserId().equals(userId)) {
            return;
        }

        // 如果是未读通知，需要减少未读计数
        if (notification.getIsRead() == 0) {
            decrementUnreadCount(userId);
        }

        notificationMapper.deleteById(notificationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchMarkAsRead(Long userId, List<Long> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return;
        }

        // 查询这些通知中有多少属于该用户且未读
        LambdaQueryWrapper<Notification> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .in(Notification::getId, notificationIds);
        long unreadCount = notificationMapper.selectCount(queryWrapper);

        if (unreadCount == 0) {
            return;
        }

        // 批量更新为已读
        LambdaUpdateWrapper<Notification> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .in(Notification::getId, notificationIds)
                .set(Notification::getIsRead, 1);
        notificationMapper.update(null, updateWrapper);

        // 减少 Redis 未读计数
        String unreadKey = UNREAD_COUNT_PREFIX + userId;
        String countStr = stringRedisTemplate.opsForValue().get(unreadKey);
        if (countStr != null) {
            long currentCount = Long.parseLong(countStr);
            long newCount = Math.max(0, currentCount - unreadCount);
            if (newCount == 0) {
                stringRedisTemplate.delete(unreadKey);
            } else {
                stringRedisTemplate.opsForValue().set(unreadKey, String.valueOf(newCount), 7, TimeUnit.DAYS);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteNotifications(Long userId, List<Long> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return;
        }

        // 查询这些通知中有多少属于该用户且未读
        LambdaQueryWrapper<Notification> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .in(Notification::getId, notificationIds);
        long unreadCount = notificationMapper.selectCount(queryWrapper);

        // 批量删除（只删除属于该用户的通知）
        LambdaQueryWrapper<Notification> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(Notification::getUserId, userId)
                .in(Notification::getId, notificationIds);
        notificationMapper.delete(deleteWrapper);

        // 减少 Redis 未读计数
        if (unreadCount > 0) {
            String unreadKey = UNREAD_COUNT_PREFIX + userId;
            String countStr = stringRedisTemplate.opsForValue().get(unreadKey);
            if (countStr != null) {
                long currentCount = Long.parseLong(countStr);
                long newCount = Math.max(0, currentCount - unreadCount);
                if (newCount == 0) {
                    stringRedisTemplate.delete(unreadKey);
                } else {
                    stringRedisTemplate.opsForValue().set(unreadKey, String.valueOf(newCount), 7, TimeUnit.DAYS);
                }
            }
        }
    }

    // ========== 内部方法 ==========

    /**
     * 检查用户是否允许接收该类型通知
     */
    private boolean isNotificationAllowed(Long userId, NotificationTypeEnum typeEnum) {
        LambdaQueryWrapper<UserSettings> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSettings::getUserId, userId);
        UserSettings settings = userSettingsMapper.selectOne(wrapper);

        // 如果没有设置记录，默认允许
        if (settings == null) {
            return true;
        }

        return switch (typeEnum) {
            case SYSTEM, AUDIT -> settings.getNotifySystem() != null && settings.getNotifySystem() == 1;
            case INTERACT, COMMENT_REPLY, MENTION -> settings.getNotifyInteract() != null && settings.getNotifyInteract() == 1;
            case MESSAGE -> settings.getNotifyMessage() != null && settings.getNotifyMessage() == 1;
            case FOLLOW -> settings.getNotifyFollow() != null && settings.getNotifyFollow() == 1;
        };
    }

    /**
     * 通过 RabbitMQ 推送通知
     */
    private void pushNotification(Long userId, Notification notification, Integer type) {
        try {
            Map<String, Object> mqMessage = Map.of(
                    "type", "NOTIFICATION",
                    "data", Map.of(
                            "userId", userId,
                            "id", notification.getId(),
                            "type", type,
                            "title", notification.getTitle(),
                            "content", notification.getContent()
                    )
            );
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_EXCHANGE,
                    "zhixun.notification.created",
                    mqMessage);
        } catch (Exception e) {
            log.warn("RabbitMQ 推送通知失败: {}", e.getMessage());
        }
    }

    /**
     * 异步发送邮件通知
     */
    @Async
    public void sendEmailNotificationAsync(Long userId, NotificationTypeEnum typeEnum, String title, String content) {
        try {
            User user = userMapper.selectById(userId);
            if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
                return;
            }

            // 跳过无效邮箱（如种子数据中的 @example.com 等不可达地址）
            if (isInvalidEmail(user.getEmail())) {
                return;
            }

            // 检查用户是否允许邮件通知
            if (!isNotificationAllowed(userId, typeEnum)) {
                return;
            }

            String subject = "【知讯】" + title;
            String text = "尊敬的用户，您好！\n\n"
                    + content + "\n\n"
                    + "此邮件由知讯平台系统自动发送，请勿直接回复。";

            emailService.sendNotificationEmail(user.getEmail(), subject, text);
            log.info("通知邮件发送成功: userId={}, type={}", userId, typeEnum.getDescription());
        } catch (Exception e) {
            log.warn("通知邮件发送失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 判断邮箱是否为无效地址（不可达）
     * 过滤种子数据中的假邮箱和明显无效的域名
     */
    private boolean isInvalidEmail(String email) {
        if (email == null) return true;
        String lower = email.toLowerCase();
        return lower.endsWith("@example.com")
                || lower.endsWith("@example.org")
                || lower.endsWith("@example.net")
                || lower.endsWith("@test.com")
                || lower.endsWith("@localhost")
                || !lower.contains("@");
    }

    /**
     * 减少 Redis 未读计数
     */
    private void decrementUnreadCount(Long userId) {
        String unreadKey = UNREAD_COUNT_PREFIX + userId;
        try {
            Long newCount = stringRedisTemplate.opsForValue().decrement(unreadKey);
            if (newCount != null && newCount <= 0) {
                stringRedisTemplate.delete(unreadKey);
            }
        } catch (Exception e) {
            log.warn("减少未读通知计数失败: {}", e.getMessage());
        }
    }

    /**
     * 构建通知 VO
     */
    private NotificationVO buildNotificationVO(Notification notification) {
        NotificationVO vo = new NotificationVO();
        vo.setId(notification.getId());
        vo.setType(notification.getType() != null ? notification.getType().getValue() : null);
        vo.setTitle(notification.getTitle());
        vo.setContent(notification.getContent());
        vo.setIsRead(notification.getIsRead());
        vo.setRelatedId(notification.getRelatedId());
        vo.setGroupKey(notification.getGroupKey());
        vo.setCreatedAt(notification.getCreatedAt());

        // 如果有分组键，查询分组内通知数量
        if (notification.getGroupKey() != null) {
            LambdaQueryWrapper<Notification> groupWrapper = new LambdaQueryWrapper<>();
            groupWrapper.eq(Notification::getUserId, notification.getUserId())
                    .eq(Notification::getGroupKey, notification.getGroupKey());
            long groupCount = notificationMapper.selectCount(groupWrapper);
            vo.setGroupedCount((int) groupCount);
        } else {
            vo.setGroupedCount(1);
        }

        return vo;
    }
}
