package com.zhixun.config.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 多级缓存配置
 * <p>
 * 启用 L1 (Caffeine) + L2 (Redis) 两级缓存。
 * 当 Redis 可用时自动使用多级缓存，否则降级为纯 Caffeine 本地缓存。
 * <p>
 * 启动时会自动清空 L2 (Redis) 残留缓存，防止旧服务实例的脏数据污染。
 */
@Slf4j
@Configuration
@EnableCaching
@RequiredArgsConstructor
public class MultiLevelCacheConfig {

    private CacheManager cacheManagerInstance;

    /**
     * 多级缓存管理器（需要 Redis）
     * 优先级高于纯 Caffeine 缓存管理器
     */
    @Bean("multiLevelCacheManager")
    @ConditionalOnBean(RedisConnectionFactory.class)
    public CacheManager multiLevelCacheManager(RedisTemplate<String, Object> redisTemplate,
                                               CacheInvalidationListener cacheInvalidationListener) {
        log.info("初始化多级缓存管理器 (L1=Caffeine, L2=Redis)");
        MultiLevelCacheManager manager = new MultiLevelCacheManager(redisTemplate, cacheInvalidationListener);
        this.cacheManagerInstance = manager;
        return manager;
    }

    /**
     * 应用启动完成后，清空 Redis 中残留的旧缓存数据
     * 防止因 Redis 持久化导致旧服务实例的缓存污染新实例
     */
    @EventListener(ApplicationReadyEvent.class)
    public void clearStaleCacheOnStartup() {
        if (cacheManagerInstance instanceof MultiLevelCacheManager) {
            log.info("应用启动完成，开始清理残留缓存...");
            ((MultiLevelCacheManager) cacheManagerInstance).clearAllCachesOnStartup();
        }
    }
}
