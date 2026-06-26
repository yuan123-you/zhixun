package com.zhixun.config.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 多级缓存管理器
 * <p>
 * 管理 L1 (Caffeine) + L2 (Redis) 两级缓存实例。
 * 每个缓存名称对应一个 MultiLevelCache 实例。
 * <p>
 * 缓存配置说明：
 * - articleHot: 热门作品缓存，L1=5min/500条，L2=30min
 * - userProfile: 用户资料缓存，L1=10min/200条，L2=20min
 * - categoryTree: 分类树缓存，L1=10min/10条，L2=30min
 * - tagList: 标签列表缓存，L1=10min/50条，L2=30min
 */
@Slf4j
public class MultiLevelCacheManager implements CacheManager {

    private final Map<String, MultiLevelCache> cacheMap = new ConcurrentHashMap<>();
    private final RedisTemplate<String, Object> redisTemplate;
    private final String l2KeyPrefix;
    private final CacheInvalidationListener cacheInvalidationListener;

    /** 默认 L1 最大容量 */
    private static final int DEFAULT_L1_MAX_SIZE = 200;
    /** 默认 L1 过期时间（分钟） */
    private static final int DEFAULT_L1_EXPIRE_MINUTES = 10;
    /** 默认 L2 过期时间（秒） */
    private static final long DEFAULT_L2_TTL_SECONDS = 1800;

    /** L2 Key 前缀 */
    private static final String DEFAULT_L2_KEY_PREFIX = "mlc:";

    public MultiLevelCacheManager(RedisTemplate<String, Object> redisTemplate,
                                  CacheInvalidationListener cacheInvalidationListener) {
        this(redisTemplate, DEFAULT_L2_KEY_PREFIX, cacheInvalidationListener);
    }

    public MultiLevelCacheManager(RedisTemplate<String, Object> redisTemplate, String l2KeyPrefix,
                                  CacheInvalidationListener cacheInvalidationListener) {
        this.redisTemplate = redisTemplate;
        this.l2KeyPrefix = l2KeyPrefix;
        this.cacheInvalidationListener = cacheInvalidationListener;
    }

    @Override
    public Cache getCache(String name) {
        return cacheMap.computeIfAbsent(name, this::createCache);
    }

    @Override
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(cacheMap.keySet());
    }

    /**
     * 根据缓存名称创建多级缓存实例
     * 不同缓存名称使用不同的 L1/L2 配置
     */
    private MultiLevelCache createCache(String name) {
        CacheConfig config = getCacheConfig(name);

        com.github.benmanes.caffeine.cache.Cache<Object, Object> l1Cache = Caffeine.newBuilder()
                .maximumSize(config.l1MaxSize)
                .expireAfterWrite(config.l1ExpireMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();

        MultiLevelCache cache = new MultiLevelCache(name, l1Cache, redisTemplate,
                config.l2TtlSeconds, l2KeyPrefix);

        log.info("创建多级缓存: name={}, L1(maxSize={}, expire={}min), L2(ttl={}s)",
                name, config.l1MaxSize, config.l1ExpireMinutes, config.l2TtlSeconds);

        // 订阅缓存失效通知频道，保证多实例间 L1 缓存一致性
        if (cacheInvalidationListener != null) {
            cacheInvalidationListener.subscribe(name);
        }

        return cache;
    }

    /**
     * 根据缓存名称获取缓存配置
     */
    private CacheConfig getCacheConfig(String name) {
        switch (name) {
            case "articleHot":
                return new CacheConfig(500, 5, 1800);
            case "userProfile":
                return new CacheConfig(200, 10, 1200);
            case "categoryTree":
                return new CacheConfig(10, 10, 1800);
            case "tagList":
            case "tagHot":
            case "tagCloud":
                return new CacheConfig(50, 10, 1800);
            case "sensitiveWords":
                return new CacheConfig(500, 30, 3600);
            case "sensitiveWhitelist":
                return new CacheConfig(200, 30, 3600);
            default:
                return new CacheConfig(DEFAULT_L1_MAX_SIZE, DEFAULT_L1_EXPIRE_MINUTES, DEFAULT_L2_TTL_SECONDS);
        }
    }

    /**
     * 应用启动时清空所有缓存的 L2 层（Redis），避免旧服务残留的数据污染
     * L1 (Caffeine) 无需清理（新进程内存已为空）
     */
    public void clearAllCachesOnStartup() {
        String pattern = l2KeyPrefix + "*";
        try {
            java.util.Set<String> keys = new java.util.HashSet<>();
            org.springframework.data.redis.core.Cursor<String> cursor = redisTemplate.scan(
                    org.springframework.data.redis.core.ScanOptions.scanOptions()
                            .match(pattern)
                            .count(100)
                            .build());
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
            cursor.close();
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("启动时清理缓存: 清除 L2 (Redis) 残留缓存 {} 条", keys.size());
            } else {
                log.info("启动时清理缓存: 无残留缓存");
            }
        } catch (Exception e) {
            log.warn("启动时清理缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 缓存配置
     */
    private static class CacheConfig {
        final int l1MaxSize;
        final int l1ExpireMinutes;
        final long l2TtlSeconds;

        CacheConfig(int l1MaxSize, int l1ExpireMinutes, long l2TtlSeconds) {
            this.l1MaxSize = l1MaxSize;
            this.l1ExpireMinutes = l1ExpireMinutes;
            this.l2TtlSeconds = l2TtlSeconds;
        }
    }
}
