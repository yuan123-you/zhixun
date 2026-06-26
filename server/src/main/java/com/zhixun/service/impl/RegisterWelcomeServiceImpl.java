package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhixun.common.util.AesUtil;
import com.zhixun.config.RabbitMQConfig;
import com.zhixun.entity.Notification;
import com.zhixun.entity.User;
import com.zhixun.entity.UserFollow;
import com.zhixun.entity.UserMessage;
import com.zhixun.entity.UserSettings;
import com.zhixun.enums.NotificationTypeEnum;
import com.zhixun.enums.RoleEnum;
import com.zhixun.mapper.NotificationMapper;
import com.zhixun.mapper.UserFollowMapper;
import com.zhixun.mapper.UserMapper;
import com.zhixun.mapper.UserMessageMapper;
import com.zhixun.mapper.UserSettingsMapper;
import com.zhixun.service.RegisterWelcomeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 新用户注册欢迎服务实现
 *
 * <p>高并发设计要点：</p>
 * <ul>
 *   <li>user_follow 表的 (follower_id, following_id) 唯一索引防止重复关注</li>
 *   <li>关注关系插入失败时捕获 DuplicateKeyException，保证幂等性</li>
 *   <li>粉丝/关注计数使用 SQL 原子操作 (SET col = col + 1)，避免读写竞争</li>
 *   <li>官方账号 ID 通过 volatile + 双重检查锁缓存，避免重复 DB 查询</li>
 *   <li>欢迎通知/私信失败不影响注册主流程（best-effort 降级）</li>
 *   <li>RabbitMQ 推送在事务提交后异步执行，避免阻塞注册响应</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterWelcomeServiceImpl implements RegisterWelcomeService {

    /** 配置的官方账号ID（可通过环境变量 OFFICIAL_ACCOUNT_ID 覆盖） */
    @Value("${app.official-account-id:0}")
    private Long configuredOfficialAccountId;

    private final UserMapper userMapper;
    private final UserFollowMapper userFollowMapper;
    private final UserMessageMapper userMessageMapper;
    private final NotificationMapper notificationMapper;
    private final UserSettingsMapper userSettingsMapper;
    private final AesUtil aesUtil;
    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;

    /** 未读消息数 Redis Key 前缀 */
    private static final String UNREAD_COUNT_PREFIX = "message:unread:";
    /** 会话摘要 Redis Key 前缀 */
    private static final String CONVERSATION_PREFIX = "message:conversation:";
    /** 未读通知数 Redis Key 前缀 */
    private static final String UNREAD_NOTIFICATION_PREFIX = "notification:unread:";
    /** 通知列表缓存 Key 前缀 */
    private static final String NOTIFICATION_LIST_PREFIX = "notification:list:";
    /** 通知设置缓存 Key 前缀 */
    private static final String NOTIFICATION_SETTINGS_PREFIX = "notification:settings:";

    /**
     * 缓存的官方账号ID。
     * null = 未初始化，-1 = 已查找但未找到，正数 = 有效的官方账号ID
     */
    private volatile Long cachedOfficialAccountId;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleNewUserRegistration(Long userId) {
        Long officialAccountId = resolveOfficialAccountId();
        if (officialAccountId == null) {
            log.warn("官方账号未配置或不存在，跳过新用户注册欢迎流程: userId={}", userId);
            return;
        }

        // 1. 自动关注官方账号（关键操作，失败回滚注册事务）
        autoFollowOfficialAccount(userId, officialAccountId);

        // 2. 发送欢迎通知（best-effort，失败不回滚）
        sendWelcomeNotification(userId, officialAccountId);

        // 3. 发送欢迎私信（best-effort，失败不回滚）
        sendWelcomeMessage(userId, officialAccountId);
    }

    // ========== 官方账号ID解析 ==========

    /**
     * 解析官方账号ID。
     * 优先级：配置文件配置 > SUPER_ADMIN 用户 > 第一个管理员
     * 使用 volatile + 双重检查锁确保线程安全的高效缓存。
     */
    private Long resolveOfficialAccountId() {
        if (cachedOfficialAccountId != null) {
            return cachedOfficialAccountId > 0 ? cachedOfficialAccountId : null;
        }
        synchronized (this) {
            if (cachedOfficialAccountId != null) {
                return cachedOfficialAccountId > 0 ? cachedOfficialAccountId : null;
            }
            // 策略1：从配置文件读取
            if (configuredOfficialAccountId != null && configuredOfficialAccountId > 0) {
                User officialUser = userMapper.selectById(configuredOfficialAccountId);
                if (officialUser != null && officialUser.getStatus() == User.STATUS_NORMAL) {
                    cachedOfficialAccountId = configuredOfficialAccountId;
                    log.info("官方账号已配置: id={}, username={}", officialUser.getId(), officialUser.getUsername());
                    return cachedOfficialAccountId;
                }
                log.warn("配置的官方账号ID无效或已禁用: {}", configuredOfficialAccountId);
            }
            // 策略2：查找 SUPER_ADMIN 用户
            User superAdmin = userMapper.selectOne(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getRole, RoleEnum.SUPER_ADMIN)
                            .eq(User::getStatus, User.STATUS_NORMAL)
                            .last("LIMIT 1"));
            if (superAdmin != null) {
                cachedOfficialAccountId = superAdmin.getId();
                log.info("使用 SUPER_ADMIN 作为官方账号: id={}, username={}", superAdmin.getId(), superAdmin.getUsername());
                return cachedOfficialAccountId;
            }
            // 策略3：查找任意管理员
            User admin = userMapper.selectOne(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getRole, RoleEnum.ADMIN)
                            .eq(User::getStatus, User.STATUS_NORMAL)
                            .last("LIMIT 1"));
            if (admin != null) {
                cachedOfficialAccountId = admin.getId();
                log.info("使用管理员作为官方账号: id={}, username={}", admin.getId(), admin.getUsername());
                return cachedOfficialAccountId;
            }
            cachedOfficialAccountId = -1L;
            log.warn("未找到任何可用的官方账号");
            return null;
        }
    }

    // ========== 自动关注 ==========

    /**
     * 新用户自动关注官方账号。
     * 高并发场景通过唯一索引 + DuplicateKeyException 保证幂等。
     * 粉丝数/关注数更新使用 SQL 原子操作，避免读写竞争。
     */
    private void autoFollowOfficialAccount(Long userId, Long officialAccountId) {
        // 幂等检查：已关注则跳过（减少不必要的唯一索引冲突）
        Long existsCount = userFollowMapper.selectCount(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, userId)
                        .eq(UserFollow::getFollowingId, officialAccountId));
        if (existsCount != null && existsCount > 0) {
            log.debug("用户已关注官方账号，跳过: userId={}, officialAccountId={}", userId, officialAccountId);
            return;
        }

        // 创建关注记录（唯一索引兜底，并发重复插入时抛 DuplicateKeyException）
        UserFollow follow = new UserFollow();
        follow.setFollowerId(userId);
        follow.setFollowingId(officialAccountId);
        try {
            userFollowMapper.insert(follow);
        } catch (DuplicateKeyException e) {
            log.debug("关注记录并发重复，唯一索引拦截: follower={}, following={}", userId, officialAccountId);
            return;
        }

        // 原子更新新用户的关注数 +1
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .setSql("follow_count = follow_count + 1"));

        // 原子更新官方账号的粉丝数 +1
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, officialAccountId)
                .setSql("follower_count = follower_count + 1"));
    }

    // ========== 欢迎通知 ==========

    /**
     * 发送系统欢迎通知。
     * 使用 try-catch 降级，通知失败不影响注册主事务。
     * 复用 NotificationServiceImpl 的通知创建流程（含 RabbitMQ 推送 + 邮件通知）。
     */
    private void sendWelcomeNotification(Long userId, Long officialAccountId) {
        try {
            User officialUser = userMapper.selectById(officialAccountId);
            String officialName = officialUser != null ? officialUser.getNickname() : "知讯";

            // 直接创建通知记录（复用现有服务逻辑，在同一事务中执行）
            createSystemNotification(userId, officialAccountId, officialName);
        } catch (Exception e) {
            log.warn("发送欢迎通知失败（已降级，不影响注册）: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 创建系统通知（内联实现，与 NotificationServiceImpl 逻辑一致，确保在同一事务中）
     */
    private void createSystemNotification(Long userId, Long officialAccountId, String officialName) {
        String title = "欢迎加入知讯";
        String content = "欢迎来到知讯！" + officialName + " 已关注了你，快去看看吧～";
        String groupKey = "welcome:" + userId;

        // 检查用户通知偏好（系统通知默认开启）
        if (!isSystemNotificationAllowed(userId)) {
            log.debug("用户已关闭系统通知，跳过: userId={}", userId);
            return;
        }

        // 分组去重：相同 groupKey 的未读通知不再重复创建
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
                .eq(Notification::getGroupKey, groupKey)
                .eq(Notification::getIsRead, 0)
                .orderByDesc(Notification::getCreatedAt)
                .last("LIMIT 1");
        Notification existing = notificationMapper.selectOne(wrapper);
        if (existing != null) {
            log.debug("欢迎通知已存在（分组去重）: userId={}", userId);
            return;
        }

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(NotificationTypeEnum.SYSTEM);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setIsRead(0);
        notification.setRelatedId(officialAccountId);
        notification.setGroupKey(groupKey);
        notificationMapper.insert(notification);

        // 更新 Redis 未读通知计数
        try {
            String unreadKey = UNREAD_NOTIFICATION_PREFIX + userId;
            stringRedisTemplate.opsForValue().increment(unreadKey);
            stringRedisTemplate.expire(unreadKey, 7, TimeUnit.DAYS);

            // 清除通知列表缓存
            clearNotificationCache(userId);
        } catch (Exception e) {
            log.warn("更新通知Redis缓存失败: userId={}, error={}", userId, e.getMessage());
        }

        // RabbitMQ 推送推迟到事务提交后，确保 DB 写入已持久化
        Long notificationId = notification.getId();
        try {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        pushNotificationViaMq(userId, notificationId, title, content);
                    }
                });
            } else {
                pushNotificationViaMq(userId, notificationId, title, content);
            }
        } catch (Exception e) {
            log.warn("注册通知MQ推送回调失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 检查用户是否允许接收系统通知（默认允许）
     */
    private boolean isSystemNotificationAllowed(Long userId) {
        try {
            // 查询用户设置（新用户刚创建 settings 记录，直接查 DB）
            LambdaQueryWrapper<UserSettings> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserSettings::getUserId, userId);
            UserSettings settings = userSettingsMapper.selectOne(wrapper);
            if (settings == null) {
                return true; // 无设置记录，默认允许
            }
            return settings.getNotifySystem() == null || settings.getNotifySystem() == 1;
        } catch (Exception e) {
            log.warn("查询用户通知偏好失败，默认允许: userId={}", userId);
            return true;
        }
    }

    // ========== 欢迎私信 ==========

    /**
     * 发送欢迎私信。
     * 使用 try-catch 降级，私信失败不影响注册主事务。
     * 绕过 MessageService 的权限检查（官方账号有权直接发私信给新用户）。
     */
    private void sendWelcomeMessage(Long userId, Long officialAccountId) {
        try {
            User officialUser = userMapper.selectById(officialAccountId);
            String officialName = officialUser != null ? officialUser.getNickname() : "知讯";

            String welcomeContent = "Hi，欢迎加入知讯！我是" + officialName + "，知讯平台的官方账号。\n\n"
                    + "在这里，你可以：\n"
                    + "📖 浏览海量优质内容\n"
                    + "✍️ 发布你的原创作品\n"
                    + "💬 与其他用户互动交流\n"
                    + "🔔 关注感兴趣的用户和标签\n\n"
                    + "如有任何问题，随时私信我～期待你的精彩表现！";

            // AES加密消息内容
            String encryptedContent = aesUtil.encrypt(welcomeContent);

            // 创建私信记录
            UserMessage message = new UserMessage();
            message.setSenderId(officialAccountId);
            message.setReceiverId(userId);
            message.setContent(encryptedContent);
            message.setIsRead(0);
            userMessageMapper.insert(message);

            // 更新 Redis 未读计数和会话缓存（失败降级）
            try {
                String unreadKey = UNREAD_COUNT_PREFIX + userId + ":" + officialAccountId;
                stringRedisTemplate.opsForValue().increment(unreadKey);
                stringRedisTemplate.expire(unreadKey, 7, TimeUnit.DAYS);

                String convKey = CONVERSATION_PREFIX + userId + ":" + officialAccountId;
                String convKeyReverse = CONVERSATION_PREFIX + officialAccountId + ":" + userId;
                String preview = welcomeContent.length() > 50 ? welcomeContent.substring(0, 50) : welcomeContent;
                stringRedisTemplate.opsForValue().set(convKey, preview, 7, TimeUnit.DAYS);
                stringRedisTemplate.opsForValue().set(convKeyReverse, preview, 7, TimeUnit.DAYS);
            } catch (Exception e) {
                log.warn("更新私信Redis缓存失败: userId={}, error={}", userId, e.getMessage());
            }

            // RabbitMQ 推送推迟到事务提交后
            Long messageId = message.getId();
            String finalContent = welcomeContent;
            try {
                if (TransactionSynchronizationManager.isSynchronizationActive()) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            pushMessageViaMq(userId, officialAccountId, messageId, finalContent);
                        }
                    });
                } else {
                    pushMessageViaMq(userId, officialAccountId, messageId, finalContent);
                }
            } catch (Exception e) {
                log.warn("注册私信MQ推送回调失败: userId={}, error={}", userId, e.getMessage());
            }

        } catch (Exception e) {
            log.warn("发送欢迎私信失败（已降级，不影响注册）: userId={}, error={}", userId, e.getMessage());
        }
    }

    // ========== MQ 推送 ==========

    /**
     * 通过 RabbitMQ 推送通知消息到 WebSocket 网关
     */
    private void pushNotificationViaMq(Long userId, Long notificationId, String title, String content) {
        try {
            Map<String, Object> mqMessage = Map.of(
                    "type", "NOTIFICATION",
                    "data", Map.of(
                            "userId", userId,
                            "id", notificationId,
                            "type", NotificationTypeEnum.SYSTEM.getValue(),
                            "title", title,
                            "content", content
                    )
            );
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_EXCHANGE,
                    "zhixun.notification.created",
                    mqMessage);
        } catch (Exception e) {
            log.warn("RabbitMQ 推送欢迎通知失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 通过 RabbitMQ 推送私信消息到 WebSocket 网关
     */
    private void pushMessageViaMq(Long userId, Long officialAccountId, Long messageId, String content) {
        try {
            Map<String, Object> mqMessage = Map.of(
                    "type", "CHAT",
                    "data", Map.of(
                            "receiverId", userId,
                            "id", messageId,
                            "senderId", officialAccountId,
                            "content", content,
                            "createdAt", java.time.LocalDateTime.now().toString()
                    )
            );
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CHAT_EXCHANGE,
                    "zhixun.chat.sent",
                    mqMessage);
        } catch (Exception e) {
            log.warn("RabbitMQ 推送欢迎私信失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    // ========== 缓存清理 ==========

    /**
     * 清除用户通知列表缓存
     */
    private void clearNotificationCache(Long userId) {
        try {
            Set<String> keys = stringRedisTemplate.keys(NOTIFICATION_LIST_PREFIX + userId + ":*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("清除通知列表缓存失败: userId={}", userId);
        }
    }
}
