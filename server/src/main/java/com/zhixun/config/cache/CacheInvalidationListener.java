package com.zhixun.config.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis 缓存失效监听器
 *
 * 订阅 Redis Pub/Sub 频道，当其他实例发布缓存失效通知时，
 * 清除本地 L1 (Caffeine) 缓存，保证多实例间数据一致性。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class CacheInvalidationListener implements MessageListener {

    private final MultiLevelCacheManager cacheManager;
    private final RedisMessageListenerContainer redisMessageListenerContainer;

    /** 已订阅的频道 */
    private final Map<String, Boolean> subscribedChannels = new ConcurrentHashMap<>();

    public CacheInvalidationListener(@Lazy MultiLevelCacheManager cacheManager,
                                     RedisMessageListenerContainer redisMessageListenerContainer) {
        this.cacheManager = cacheManager;
        this.redisMessageListenerContainer = redisMessageListenerContainer;
    }

    /**
     * 订阅指定缓存名称的失效通知频道
     */
    public void subscribe(String cacheName) {
        String channel = "cache:evict:" + cacheName;
        if (subscribedChannels.putIfAbsent(channel, true) == null) {
            redisMessageListenerContainer.addMessageListener(this, new ChannelTopic(channel));
            log.info("订阅缓存失效频道: {}", channel);
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String key = new String(message.getBody());

        // 从频道名提取缓存名称：cache:evict:{cacheName}
        String cacheName = channel.substring("cache:evict:".length());

        try {
            MultiLevelCache cache = (MultiLevelCache) cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.invalidateL1(key);
                log.debug("收到缓存失效通知: cache={}, key={}", cacheName, key);
            }
        } catch (Exception e) {
            log.warn("处理缓存失效通知失败: channel={}, key={}, error={}", channel, key, e.getMessage());
        }
    }
}
