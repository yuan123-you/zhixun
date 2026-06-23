package com.zhixun.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 缓存穿透/击穿/雪崩防护组件
 *
 * 1. 缓存穿透防护：查询不存在的数据时缓存空值（TTL 5min）+ 布隆过滤器预判
 * 2. 缓存击穿防护：使用互斥锁（Redis SETNX）重建缓存
 * 3. 缓存雪崩防护：TTL加随机偏移±10%
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CachePenetrationProtection {

    private final StringRedisTemplate redisTemplate;

    /** 空值缓存前缀 */
    private static final String NULL_CACHE_PREFIX = "cache:null:";

    /** 互斥锁前缀 */
    private static final String LOCK_PREFIX = "cache:lock:";

    /** 布隆过滤器 Redis Key */
    private static final String BLOOM_FILTER_KEY = "cache:bloom:filter";

    /** 布隆过滤器初始化标记 Key */
    private static final String BLOOM_INITIALIZED_KEY = "cache:bloom:initialized";

    /** 空值缓存过期时间（秒） */
    private static final long NULL_CACHE_TTL_SECONDS = 300;

    /** 互斥锁超时时间（秒） */
    private static final long LOCK_TIMEOUT_SECONDS = 10;

    /** 互斥锁获取等待时间（毫秒） */
    private static final long LOCK_WAIT_MILLIS = 50;

    /** 互斥锁最大重试次数 */
    private static final int LOCK_MAX_RETRY = 20;

    /**
     * 布隆过滤器参数
     * 使用 Redis Bitmap 实现简易布隆过滤器
     * 预期元素数量 100000，误判率 1%
     * 需要的 bit 数 ≈ -n*ln(p)/(ln2)^2 ≈ 958505，取 1048576 (2^20 = 128KB)
     * 哈希函数数量 k ≈ (m/n)*ln2 ≈ 7
     */
    private static final long BLOOM_BIT_SIZE = 1048576L;
    private static final int BLOOM_HASH_COUNT = 7;

    // ==================== 布隆过滤器 ====================

    /**
     * 向布隆过滤器中添加元素
     *
     * @param key 元素标识
     */
    public void bloomAdd(String key) {
        for (int i = 0; i < BLOOM_HASH_COUNT; i++) {
            long bitIndex = bloomHash(key, i);
            redisTemplate.opsForValue().setBit(BLOOM_FILTER_KEY, bitIndex, true);
        }
        log.debug("布隆过滤器添加元素: {}", key);
    }

    /**
     * 批量向布隆过滤器中添加元素
     *
     * @param keys 元素标识列表
     */
    public void bloomAddAll(Iterable<String> keys) {
        int count = 0;
        for (String key : keys) {
            bloomAdd(key);
            count++;
        }
        log.info("布隆过滤器批量添加元素: count={}", count);
    }

    /**
     * 检查元素是否可能存在于布隆过滤器中
     * 返回 true 表示可能存在（有误判率），返回 false 表示一定不存在
     *
     * @param key 元素标识
     * @return true-可能存在，false-一定不存在
     */
    public boolean bloomMightContain(String key) {
        for (int i = 0; i < BLOOM_HASH_COUNT; i++) {
            long bitIndex = bloomHash(key, i);
            Boolean bit = redisTemplate.opsForValue().getBit(BLOOM_FILTER_KEY, bitIndex);
            if (!Boolean.TRUE.equals(bit)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 标记布隆过滤器已初始化
     */
    public void markBloomInitialized() {
        redisTemplate.opsForValue().set(BLOOM_INITIALIZED_KEY, "1", 24, TimeUnit.HOURS);
        log.info("布隆过滤器标记为已初始化");
    }

    /**
     * 检查布隆过滤器是否已初始化
     */
    public boolean isBloomInitialized() {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLOOM_INITIALIZED_KEY));
    }

    /**
     * 布隆过滤器哈希函数
     * 使用双重哈希（Double Hashing）模拟多个哈希函数
     * h_i(x) = (h1(x) + i * h2(x)) % m
     */
    private long bloomHash(String key, int index) {
        long h1 = key.hashCode();
        // 第二个哈希：使用不同种子
        long h2 = (h1 >>> 16) ^ (key.length() * 31L);
        return Math.abs((h1 + (long) index * h2) % BLOOM_BIT_SIZE);
    }

    // ==================== 缓存穿透防护 ====================

    /**
     * 缓存空值，防止缓存穿透
     * 当查询数据库未命中时，缓存一个空值标记，避免反复穿透到数据库
     *
     * @param key 缓存键
     */
    public void cacheNullValue(String key) {
        String nullCacheKey = NULL_CACHE_PREFIX + key;
        redisTemplate.opsForValue().set(nullCacheKey, "1", NULL_CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        log.debug("缓存空值: {}, TTL: {}s", key, NULL_CACHE_TTL_SECONDS);
    }

    /**
     * 检查是否为空值缓存
     *
     * @param key 缓存键
     * @return true-是空值缓存（表示数据库中不存在该数据）
     */
    public boolean isNullCache(String key) {
        String nullCacheKey = NULL_CACHE_PREFIX + key;
        return Boolean.TRUE.equals(redisTemplate.hasKey(nullCacheKey));
    }

    /**
     * 清除空值缓存
     *
     * @param key 缓存键
     */
    public void evictNullCache(String key) {
        String nullCacheKey = NULL_CACHE_PREFIX + key;
        redisTemplate.delete(nullCacheKey);
    }

    // ==================== 缓存击穿防护 ====================

    /**
     * 获取互斥锁，防止缓存击穿
     * 使用 Redis SETNX 实现分布式互斥锁，确保只有一个线程重建缓存
     *
     * @param key 缓存键
     * @return true-获取锁成功
     */
    public boolean tryLock(String key) {
        String lockKey = LOCK_PREFIX + key;
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(acquired);
    }

    /**
     * 释放互斥锁
     *
     * @param key 缓存键
     */
    public void unlock(String key) {
        String lockKey = LOCK_PREFIX + key;
        redisTemplate.delete(lockKey);
    }

    /**
     * 等待互斥锁并获取
     * 在缓存击穿场景下，其他线程等待锁释放后直接从缓存获取数据
     *
     * @param key 缓存键
     * @return true-获取锁成功（可以重建缓存），false-超时未获取到锁
     */
    public boolean waitLock(String key) {
        for (int i = 0; i < LOCK_MAX_RETRY; i++) {
            String lockKey = LOCK_PREFIX + key;
            Boolean locked = redisTemplate.hasKey(lockKey);
            if (!Boolean.TRUE.equals(locked)) {
                return tryLock(key);
            }
            try {
                Thread.sleep(LOCK_WAIT_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    // ==================== 缓存雪崩防护 ====================

    /**
     * 为 TTL 添加随机偏移，防止大量缓存同时失效导致雪崩
     * 偏移范围为 ±10%
     *
     * @param baseTTLSeconds 基础 TTL（秒）
     * @return 添加随机偏移后的 TTL（秒）
     */
    public long randomizeTTL(long baseTTLSeconds) {
        if (baseTTLSeconds <= 0) {
            return baseTTLSeconds;
        }
        // 计算 ±10% 的偏移量
        long offset = Math.max(1, baseTTLSeconds / 10);
        long randomOffset = ThreadLocalRandom.current().nextLong(-offset, offset + 1);
        long finalTTL = baseTTLSeconds + randomOffset;
        // 确保最终 TTL 不小于 1 秒
        return Math.max(1, finalTTL);
    }

    /**
     * 带随机偏移的缓存写入
     * 自动为 TTL 添加 ±10% 的随机偏移，防止缓存雪崩
     *
     * @param key           缓存键
     * @param value         缓存值
     * @param baseTTLSeconds 基础 TTL（秒）
     */
    public void setWithRandomizedTTL(String key, String value, long baseTTLSeconds) {
        long finalTTL = randomizeTTL(baseTTLSeconds);
        redisTemplate.opsForValue().set(key, value, finalTTL, TimeUnit.SECONDS);
        log.debug("缓存写入: {}, TTL: {}s (基础: {}s, 偏移: {}s)",
                key, finalTTL, baseTTLSeconds, finalTTL - baseTTLSeconds);
    }

    // ==================== 综合防护：安全获取缓存 ====================

    /**
     * 安全获取缓存数据（综合穿透/击穿/雪崩防护）
     *
     * <p>使用方式：
     * <pre>
     *   String data = cacheProtection.safeGet(key, baseTTL, () -&gt; {
     *       // 从数据库查询数据
     *       return dbQuery(key);
     *   });
     * </pre>
     *
     * @param key           缓存键
     * @param baseTTLSeconds 基础 TTL（秒）
     * @param loader        数据加载器（缓存未命中时从数据库加载）
     * @return 缓存数据，可能为 null
     */
    public String safeGet(String key, long baseTTLSeconds, java.util.function.Supplier<String> loader) {
        // 1. 先查缓存
        String cachedValue = redisTemplate.opsForValue().get(key);
        if (cachedValue != null) {
            return cachedValue;
        }

        // 2. 检查是否为空值缓存（穿透防护）
        if (isNullCache(key)) {
            log.debug("命中空值缓存: {}", key);
            return null;
        }

        // 3. 布隆过滤器预判（穿透防护增强）
        if (isBloomInitialized() && !bloomMightContain(key)) {
            log.debug("布隆过滤器判定不存在: {}", key);
            return null;
        }

        // 4. 尝试获取互斥锁（击穿防护）
        if (tryLock(key)) {
            try {
                // 获取锁成功，从数据库加载数据
                String value = loader.get();
                if (value != null) {
                    // 写入缓存，带随机 TTL（雪崩防护）
                    setWithRandomizedTTL(key, value, baseTTLSeconds);
                } else {
                    // 数据库也没有，缓存空值（穿透防护）
                    cacheNullValue(key);
                }
                return value;
            } finally {
                unlock(key);
            }
        } else {
            // 获取锁失败，等待锁释放后从缓存获取
            if (waitLock(key)) {
                try {
                    cachedValue = redisTemplate.opsForValue().get(key);
                    if (cachedValue != null) {
                        return cachedValue;
                    }
                    // 缓存仍然没有，降级为直接查库
                    return loader.get();
                } finally {
                    unlock(key);
                }
            }
            // 等待超时，降级为直接查库
            log.warn("缓存互斥锁等待超时，降级直接查询数据库: {}", key);
            return loader.get();
        }
    }
}
