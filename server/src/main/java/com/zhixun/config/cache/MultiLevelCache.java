package com.zhixun.config.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * L1 (Caffeine 本地) + L2 (Redis) 两级缓存实现
 * <p>
 * 读取顺序：L1 -> L2 -> 数据库
 * 写入策略：Write-Through，同时写入 L1 和 L2
 * 失效策略：删除时同时清除 L1 和 L2
 */
@Slf4j
public class MultiLevelCache implements Cache {

    private final String name;
    private final com.github.benmanes.caffeine.cache.Cache<Object, Object> l1Cache;
    private final RedisTemplate<String, Object> redisTemplate;
    private final long l2TtlSeconds;
    private final String l2KeyPrefix;

    public MultiLevelCache(String name, com.github.benmanes.caffeine.cache.Cache<Object, Object> l1Cache,
                           RedisTemplate<String, Object> redisTemplate,
                           long l2TtlSeconds, String l2KeyPrefix) {
        this.name = name;
        this.l1Cache = l1Cache;
        this.redisTemplate = redisTemplate;
        this.l2TtlSeconds = l2TtlSeconds;
        this.l2KeyPrefix = l2KeyPrefix;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return l1Cache;
    }

    @Override
    public ValueWrapper get(Object key) {
        // 1. 先查 L1 (Caffeine)
        Object l1Value = l1Cache.getIfPresent(key);
        if (l1Value != null) {
            log.debug("L1 缓存命中: cache={}, key={}", name, key);
            return () -> l1Value;
        }

        // 2. L1 未命中，查 L2 (Redis)
        String l2Key = buildL2Key(key);
        Object l2Value = redisTemplate.opsForValue().get(l2Key);
        if (l2Value != null) {
            log.debug("L2 缓存命中: cache={}, key={}", name, key);
            // 回填 L1
            l1Cache.put(key, l2Value);
            return () -> l2Value;
        }

        log.debug("缓存未命中: cache={}, key={}", name, key);
        return null;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        ValueWrapper wrapper = get(key);
        if (wrapper != null) {
            Object value = wrapper.get();
            if (value != null && type != null && !type.isInstance(value)) {
                throw new IllegalStateException(
                        "缓存值类型不匹配: expected=" + type.getName() + ", actual=" + value.getClass().getName());
            }
            return type.cast(value);
        }
        return null;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper wrapper = get(key);
        if (wrapper != null) {
            @SuppressWarnings("unchecked")
            T value = (T) wrapper.get();
            return value;
        }

        // L1/L2 都未命中，通过 loader 加载
        try {
            @SuppressWarnings("unchecked")
            T value = valueLoader.call();
            if (value != null) {
                put(key, value);
            }
            return value;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    @Override
    public void put(Object key, Object value) {
        if (value == null) {
            return;
        }
        // Write-Through: 同时写入 L1 和 L2
        l1Cache.put(key, value);
        String l2Key = buildL2Key(key);
        redisTemplate.opsForValue().set(l2Key, value, l2TtlSeconds, TimeUnit.SECONDS);
        log.debug("写入多级缓存: cache={}, key={}, l2TTL={}s", name, key, l2TtlSeconds);
    }

    @Override
    public void evict(Object key) {
        // 同时清除 L1 和 L2
        l1Cache.invalidate(key);
        String l2Key = buildL2Key(key);
        redisTemplate.delete(l2Key);
        log.debug("清除多级缓存: cache={}, key={}", name, key);
    }

    @Override
    public void clear() {
        l1Cache.invalidateAll();
        // Redis 无法按前缀批量删除（除非使用 keys 命令，生产环境不推荐）
        // 这里只清除 L1，L2 依赖 TTL 自然过期
        log.warn("清除多级缓存: cache={}, 仅清除L1，L2依赖TTL自然过期", name);
    }

    /**
     * 构建 L2 (Redis) 的 Key
     */
    private String buildL2Key(Object key) {
        return l2KeyPrefix + name + ":" + key;
    }
}
