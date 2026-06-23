package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.entity.UserSettings;
import com.zhixun.mapper.UserSettingsMapper;
import com.zhixun.service.OnlineStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 在线状态服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnlineStatusServiceImpl implements OnlineStatusService {

    private final UserSettingsMapper userSettingsMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /** 在线状态 Redis Key 前缀 */
    private static final String ONLINE_STATUS_PREFIX = "online:status:";
    /** 最后活跃时间 Redis Key 前缀 */
    private static final String LAST_ACTIVE_PREFIX = "online:last_active:";
    /** 用户隐私设置 Redis Key 前缀 */
    private static final String SETTINGS_SHOW_ONLINE_PREFIX = "settings:show_online:";
    /** 在线状态超时时间（分钟） */
    private static final int ONLINE_TIMEOUT_MINUTES = 5;
    /** 隐私设置缓存时间（分钟） */
    private static final int SETTINGS_CACHE_MINUTES = 30;

    @Override
    public void updateOnlineStatus(Long userId, boolean online) {
        String key = ONLINE_STATUS_PREFIX + userId;
        if (online) {
            stringRedisTemplate.opsForValue().set(key, "1", ONLINE_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            // 更新最后活跃时间
            String activeKey = LAST_ACTIVE_PREFIX + userId;
            stringRedisTemplate.opsForValue().set(activeKey, LocalDateTime.now().toString(), 1, TimeUnit.HOURS);
        } else {
            stringRedisTemplate.delete(key);
        }
    }

    @Override
    public Boolean getOnlineStatus(Long userId, Long requesterId) {
        // 自己查看自己的在线状态不受隐私设置限制
        if (requesterId != null && requesterId.equals(userId)) {
            String key = ONLINE_STATUS_PREFIX + userId;
            String status = stringRedisTemplate.opsForValue().get(key);
            return "1".equals(status);
        }

        // 检查目标用户的隐私设置：是否允许显示在线状态
        if (!isShowOnlineStatus(userId)) {
            return null; // 不可见
        }

        // 读取在线状态
        String key = ONLINE_STATUS_PREFIX + userId;
        String status = stringRedisTemplate.opsForValue().get(key);
        return "1".equals(status);
    }

    @Override
    public Map<Long, Boolean> batchGetOnlineStatus(List<Long> userIds, Long requesterId) {
        Map<Long, Boolean> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }

        for (Long userId : userIds) {
            // 自己查看自己的在线状态不受隐私设置限制
            if (requesterId != null && requesterId.equals(userId)) {
                String key = ONLINE_STATUS_PREFIX + userId;
                String status = stringRedisTemplate.opsForValue().get(key);
                result.put(userId, "1".equals(status));
                continue;
            }

            // 检查目标用户的隐私设置
            if (!isShowOnlineStatus(userId)) {
                // 不可见的用户不放入结果中
                continue;
            }

            // 读取在线状态
            String key = ONLINE_STATUS_PREFIX + userId;
            String status = stringRedisTemplate.opsForValue().get(key);
            result.put(userId, "1".equals(status));
        }

        return result;
    }

    @Override
    public void updateShowOnlineStatus(Long userId, Integer showOnlineStatus) {
        // 更新数据库
        LambdaQueryWrapper<UserSettings> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSettings::getUserId, userId);
        UserSettings settings = userSettingsMapper.selectOne(wrapper);

        if (settings == null) {
            settings = new UserSettings();
            settings.setUserId(userId);
            settings.setShowOnlineStatus(showOnlineStatus);
            settings.setNotifySystem(1);
            settings.setNotifyInteract(1);
            settings.setNotifyMessage(1);
            settings.setNotifyFollow(1);
            settings.setMessagePermission(0);
            settings.setSaveViewHistory(1);
            settings.setFontSize(1);
            settings.setTheme("light");
            settings.setLanguage("zh-CN");
            userSettingsMapper.insert(settings);
        } else {
            settings.setShowOnlineStatus(showOnlineStatus);
            userSettingsMapper.updateById(settings);
        }

        // 清除 Redis 缓存，确保下次查询时读取最新设置
        String cacheKey = SETTINGS_SHOW_ONLINE_PREFIX + userId;
        stringRedisTemplate.delete(cacheKey);
        log.debug("用户 {} 更新在线状态可见性为 {}，已清除缓存", userId, showOnlineStatus);
    }

    @Override
    public void invalidateShowOnlineStatusCache(Long userId) {
        String cacheKey = SETTINGS_SHOW_ONLINE_PREFIX + userId;
        stringRedisTemplate.delete(cacheKey);
        log.debug("已清除用户 {} 的在线状态可见性缓存", userId);
    }

    /**
     * 检查目标用户是否允许显示在线状态
     * 优先从 Redis 缓存读取，缓存未命中则从数据库读取并写入缓存
     */
    private boolean isShowOnlineStatus(Long userId) {
        // 1. 先从 Redis 缓存读取
        String cacheKey = SETTINGS_SHOW_ONLINE_PREFIX + userId;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return "1".equals(cached);
        }

        // 2. 从数据库读取
        UserSettings settings = userSettingsMapper.selectOne(
                new LambdaQueryWrapper<UserSettings>().eq(UserSettings::getUserId, userId));

        // 默认值为1（显示在线状态）
        int showOnlineStatus = (settings != null && settings.getShowOnlineStatus() != null)
                ? settings.getShowOnlineStatus() : 1;

        // 3. 写入 Redis 缓存
        stringRedisTemplate.opsForValue().set(cacheKey, String.valueOf(showOnlineStatus),
                SETTINGS_CACHE_MINUTES, TimeUnit.MINUTES);

        return showOnlineStatus == 1;
    }

    @Override
    public void heartbeat(Long userId) {
        String key = ONLINE_STATUS_PREFIX + userId;
        // 刷新在线状态过期时间
        stringRedisTemplate.opsForValue().set(key, "1", ONLINE_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        // 更新最后活跃时间
        String activeKey = LAST_ACTIVE_PREFIX + userId;
        stringRedisTemplate.opsForValue().set(activeKey, LocalDateTime.now().toString(), 1, TimeUnit.HOURS);
    }
}
