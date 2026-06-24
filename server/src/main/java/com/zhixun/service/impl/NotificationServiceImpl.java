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

import java.util.*;
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
    /** 通知列表缓存 Key 前缀 */
    private static final String NOTIFICATION_LIST_PREFIX = "notification:list:";
    /** 用户通知设置缓存 Key 前缀 */
    private static final String NOTIFICATION_SETTINGS_PREFIX = "notification:settings:";

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

                // 清除该用户的通知列表缓存
                clearNotificationListCache(userId);

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

        // 清除该用户的通知列表缓存
        clearNotificationListCache(userId);

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
        // 构建缓存 Key
        String cacheKey = NOTIFICATION_LIST_PREFIX + userId + ":" + (type != null ? type : "all") + ":" + page + ":" + pageSize;

        // 尝试从缓存获取通知ID列表
        String cachedIds = stringRedisTemplate.opsForValue().get(cacheKey);
        List<Notification> notifications;
        long total;

        if (cachedIds != null) {
            // 缓存命中，解析ID列表并批量查询
            List<Long> ids = Arrays.stream(cachedIds.split(","))
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            if (ids.isEmpty()) {
                return new PageResult<>(Collections.emptyList(), 0L, page, pageSize);
            }

            // 从缓存中获取 total 值
            String totalKey = cacheKey + ":total";
            String totalStr = stringRedisTemplate.opsForValue().get(totalKey);
            total = totalStr != null ? Long.parseLong(totalStr) : ids.size();

            // 批量查询通知详情，保持缓存中的顺序
            List<Notification> fetched = notificationMapper.selectBatchIds(ids);
            Map<Long, Notification> notificationMap = fetched.stream()
                    .collect(Collectors.toMap(Notification::getId, n -> n));
            notifications = ids.stream()
                    .map(notificationMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } else {
            // 缓存未命中，查数据库
            LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Notification::getUserId, userId);

            if (type != null) {
                NotificationTypeEnum typeEnum = NotificationTypeEnum.values()[type - 1];
                wrapper.eq(Notification::getType, typeEnum);
            }

            wrapper.orderByDesc(Notification::getCreatedAt);

            Page<Notification> notificationPage = new Page<>(page, pageSize);
            Page<Notification> result = notificationMapper.selectPage(notificationPage, wrapper);
            notifications = result.getRecords();
            total = result.getTotal();

            // 缓存通知ID列表（逗号分隔），2分钟TTL
            String idsStr = notifications.stream()
                    .map(n -> String.valueOf(n.getId()))
                    .collect(Collectors.joining(","));
            stringRedisTemplate.opsForValue().set(cacheKey, idsStr, 2, TimeUnit.MINUTES);
            // 缓存 total 值
            String totalKey = cacheKey + ":total";
            stringRedisTemplate.opsForValue().set(totalKey, String.valueOf(total), 2, TimeUnit.MINUTES);
        }

        // 批量查询分组计数，解决 N+1 问题
        Map<String, Integer> groupCountMap = batchQueryGroupCounts(notifications);

        List<NotificationVO> voList = notifications.stream()
                .map(notification -> buildNotificationVO(notification, groupCountMap))
                .collect(Collectors.toList());

        return new PageResult<>(voList, total, page, pageSize);
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

            // 清除该用户的通知列表缓存
            clearNotificationListCache(userId);

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

        // 清除该用户的通知列表缓存
        clearNotificationListCache(userId);

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

        // 清除该用户的通知列表缓存
        clearNotificationListCache(userId);
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

        // 清除该用户的通知列表缓存
        clearNotificationListCache(userId);

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

        // 清除该用户的通知列表缓存
        clearNotificationListCache(userId);

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
        // 先从缓存获取用户设置
        String settingsKey = NOTIFICATION_SETTINGS_PREFIX + userId;
        UserSettings settings;

        String cachedSettings = stringRedisTemplate.opsForValue().get(settingsKey);
        if (cachedSettings != null) {
            // 缓存命中，解析设置值
            if ("null".equals(cachedSettings)) {
                // 缓存了"无设置记录"的状态
                return true;
            }
            // 格式：notifySystem,notifyInteract,notifyMessage,notifyFollow
            String[] parts = cachedSettings.split(",");
            settings = new UserSettings();
            settings.setNotifySystem(parseInteger(parts[0]));
            settings.setNotifyInteract(parseInteger(parts[1]));
            settings.setNotifyMessage(parseInteger(parts[2]));
            settings.setNotifyFollow(parseInteger(parts[3]));
        } else {
            // 缓存未命中，查数据库
            LambdaQueryWrapper<UserSettings> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserSettings::getUserId, userId);
            settings = userSettingsMapper.selectOne(wrapper);

            // 写入缓存（5分钟TTL）
            if (settings == null) {
                stringRedisTemplate.opsForValue().set(settingsKey, "null", 5, TimeUnit.MINUTES);
                return true;
            } else {
                String settingsValue = nullableInt(settings.getNotifySystem()) + ","
                        + nullableInt(settings.getNotifyInteract()) + ","
                        + nullableInt(settings.getNotifyMessage()) + ","
                        + nullableInt(settings.getNotifyFollow());
                stringRedisTemplate.opsForValue().set(settingsKey, settingsValue, 5, TimeUnit.MINUTES);
            }
        }

        return switch (typeEnum) {
            case SYSTEM, AUDIT -> settings.getNotifySystem() != null && settings.getNotifySystem() == 1;
            case INTERACT, COMMENT_REPLY, MENTION -> settings.getNotifyInteract() != null && settings.getNotifyInteract() == 1;
            case MESSAGE -> settings.getNotifyMessage() != null && settings.getNotifyMessage() == 1;
            case FOLLOW -> settings.getNotifyFollow() != null && settings.getNotifyFollow() == 1;
        };
    }

    /**
     * 将可空 Integer 转为字符串，null 用 "N" 表示
     */
    private String nullableInt(Integer value) {
        return value != null ? String.valueOf(value) : "N";
    }

    /**
     * 解析缓存中的整数值，"N" 表示 null
     */
    private Integer parseInteger(String value) {
        return "N".equals(value) ? null : Integer.parseInt(value);
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
     * 批量查询分组内通知数量，解决 N+1 问题
     */
    private Map<String, Integer> batchQueryGroupCounts(List<Notification> notifications) {
        // 收集所有非空的 groupKey
        Set<String> groupKeys = notifications.stream()
                .map(Notification::getGroupKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (groupKeys.isEmpty()) {
            return Collections.emptyMap();
        }

        // 需要知道 userId（同一用户的通知列表）
        Long userId = notifications.isEmpty() ? null : notifications.get(0).getUserId();

        // 一次性查询所有分组的计数
        Map<String, Integer> groupCountMap = new HashMap<>();
        if (userId != null) {
            LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Notification::getUserId, userId)
                    .in(Notification::getGroupKey, groupKeys)
                    .select(Notification::getGroupKey);
            List<Notification> groupNotifications = notificationMapper.selectList(wrapper);
            groupNotifications.stream()
                    .collect(Collectors.groupingBy(Notification::getGroupKey, Collectors.counting()))
                    .forEach((key, count) -> groupCountMap.put(key, count.intValue()));
        }

        return groupCountMap;
    }

    /**
     * 构建通知 VO
     */
    private NotificationVO buildNotificationVO(Notification notification, Map<String, Integer> groupCountMap) {
        NotificationVO vo = new NotificationVO();
        vo.setId(notification.getId());
        vo.setType(notification.getType() != null ? notification.getType().getValue() : null);
        vo.setTitle(notification.getTitle());
        vo.setContent(notification.getContent());
        vo.setIsRead(notification.getIsRead());
        vo.setRelatedId(notification.getRelatedId());
        vo.setGroupKey(notification.getGroupKey());
        vo.setCreatedAt(notification.getCreatedAt());

        // 从批量查询的 Map 中获取分组计数
        if (notification.getGroupKey() != null) {
            Integer count = groupCountMap.get(notification.getGroupKey());
            vo.setGroupedCount(count != null ? count : 1);
        } else {
            vo.setGroupedCount(1);
        }

        return vo;
    }

    /**
     * 清除用户通知列表缓存
     */
    private void clearNotificationListCache(Long userId) {
        try {
            Set<String> keys = stringRedisTemplate.keys(NOTIFICATION_LIST_PREFIX + userId + ":*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("清除通知列表缓存失败: {}", e.getMessage());
        }
    }
}
