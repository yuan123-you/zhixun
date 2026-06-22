package com.zhixun.service.impl;

import com.zhixun.mapper.UserMapper;
import com.zhixun.service.OnlineStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 在线状态服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnlineStatusServiceImpl implements OnlineStatusService {

    private final UserMapper userMapper;

    private final StringRedisTemplate stringRedisTemplate;

    /** 在线状态 Redis Key 前缀 */
    private static final String ONLINE_STATUS_PREFIX = "online:status:";
    /** 最后活跃时间 Redis Key 前缀 */
    private static final String LAST_ACTIVE_PREFIX = "online:last_active:";
    /** 在线状态超时时间（分钟） */
    private static final int ONLINE_TIMEOUT_MINUTES = 5;

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
    public boolean getOnlineStatus(Long userId) {
        // 检查隐私设置：如果用户关闭了在线状态显示，则返回 false
        // 此处简化处理，直接查 Redis
        String key = ONLINE_STATUS_PREFIX + userId;
        String status = stringRedisTemplate.opsForValue().get(key);
        return "1".equals(status);
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
