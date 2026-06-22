package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.common.result.PageResult;
import com.zhixun.entity.Notification;
import com.zhixun.enums.NotificationTypeEnum;
import com.zhixun.mapper.NotificationMapper;
import com.zhixun.service.NotificationService;
import com.zhixun.vo.NotificationVO;
import com.zhixun.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
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

    /** 未读通知数 Redis Key 前缀 */
    private static final String UNREAD_COUNT_PREFIX = "notification:unread:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createNotification(Long userId, Integer type, String title, String content, Long relatedId) {
        NotificationTypeEnum typeEnum = NotificationTypeEnum.values()[type - 1];

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(typeEnum);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setIsRead(0);
        notification.setRelatedId(relatedId);

        notificationMapper.insert(notification);

        // 更新 Redis 未读计数
        String unreadKey = UNREAD_COUNT_PREFIX + userId;
        stringRedisTemplate.opsForValue().increment(unreadKey);
        stringRedisTemplate.expire(unreadKey, 7, TimeUnit.DAYS);

        // 通过 RabbitMQ 异步推送通知
        try {
            Map<String, Object> mqMessage = Map.of(
                    "type", "NOTIFICATION",
                    "data", Map.of(
                            "userId", userId,
                            "id", notification.getId(),
                            "type", type,
                            "title", title,
                            "content", content
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
            String unreadKey = UNREAD_COUNT_PREFIX + userId;
            String countStr = stringRedisTemplate.opsForValue().get(unreadKey);
            if (countStr != null) {
                int count = Integer.parseInt(countStr);
                if (count > 0) {
                    stringRedisTemplate.opsForValue().decrement(unreadKey);
                }
            }
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

    // ========== 内部方法 ==========

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
        vo.setCreatedAt(notification.getCreatedAt());
        return vo;
    }
}
