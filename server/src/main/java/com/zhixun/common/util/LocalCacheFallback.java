package com.zhixun.common.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
public class LocalCacheFallback {

    private final Cache<String, Object> localCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    @SuppressWarnings("unchecked")
    public <T> T getOrFallback(String key, Supplier<T> remoteLoader, Supplier<T> fallbackSupplier) {
        Object cached = localCache.getIfPresent(key);
        if (cached != null) {
            return (T) cached;
        }

        try {
            T remoteValue = remoteLoader.get();
            if (remoteValue != null) {
                localCache.put(key, remoteValue);
                return remoteValue;
            }
        } catch (Exception e) {
            log.warn("[LocalCacheFallback] 远程加载失败，使用兜底数据, key: {}, error: {}", key, e.getMessage());
        }

        T fallbackValue = fallbackSupplier.get();
        if (fallbackValue != null) {
            localCache.put(key, fallbackValue);
        }
        return fallbackValue;
    }

    public void put(String key, Object value) {
        localCache.put(key, value);
    }

    public void evict(String key) {
        localCache.invalidate(key);
    }
}
