package com.zhixun.config.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 多级缓存配置
 * <p>
 * 启用 L1 (Caffeine) + L2 (Redis) 两级缓存。
 * 当 Redis 可用时自动使用多级缓存，否则降级为纯 Caffeine 本地缓存。
 */
@Slf4j
@Configuration
@EnableCaching
@RequiredArgsConstructor
public class MultiLevelCacheConfig {

    /**
     * 多级缓存管理器（需要 Redis）
     * 优先级高于纯 Caffeine 缓存管理器
     */
    @Bean("multiLevelCacheManager")
    @ConditionalOnBean(RedisConnectionFactory.class)
    public CacheManager multiLevelCacheManager(RedisTemplate<String, Object> redisTemplate) {
        log.info("初始化多级缓存管理器 (L1=Caffeine, L2=Redis)");
        return new MultiLevelCacheManager(redisTemplate);
    }
}
