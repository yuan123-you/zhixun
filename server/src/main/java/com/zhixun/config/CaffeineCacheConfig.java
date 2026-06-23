package com.zhixun.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 本地缓存配置
 * 用于分类树、标签列表、敏感词库等读多写少的场景
 * <p>
 * 当 Redis 可用时，多级缓存管理器优先使用（见 MultiLevelCacheConfig）；
 * 当 Redis 不可用时，降级为纯 Caffeine 本地缓存。
 */
@Configuration
public class CaffeineCacheConfig {

    /** 分类树缓存名称 */
    public static final String CACHE_CATEGORY_TREE = "categoryTree";
    /** 标签列表缓存名称 */
    public static final String CACHE_TAG_LIST = "tagList";
    /** 热门标签缓存名称 */
    public static final String CACHE_TAG_HOT = "tagHot";
    /** 标签云缓存名称 */
    public static final String CACHE_TAG_CLOUD = "tagCloud";
    /** 敏感词库缓存名称 */
    public static final String CACHE_SENSITIVE_WORDS = "sensitiveWords";
    /** 敏感词白名单缓存名称 */
    public static final String CACHE_SENSITIVE_WHITELIST = "sensitiveWhitelist";
    /** 热门文章缓存名称 */
    public static final String CACHE_ARTICLE_HOT = "articleHot";
    /** 用户资料缓存名称 */
    public static final String CACHE_USER_PROFILE = "userProfile";

    /**
     * 纯 Caffeine 本地缓存管理器（Redis 不可用时的降级方案）
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "multiLevelCacheManager")
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheSpecification("maximumSize=500,expireAfterWrite=10m,recordStats");
        // 注册所有缓存名称
        cacheManager.setCacheNames(java.util.List.of(
                CACHE_CATEGORY_TREE,
                CACHE_TAG_LIST,
                CACHE_TAG_HOT,
                CACHE_TAG_CLOUD,
                CACHE_SENSITIVE_WORDS,
                CACHE_SENSITIVE_WHITELIST,
                CACHE_ARTICLE_HOT,
                CACHE_USER_PROFILE
        ));
        return cacheManager;
    }

    /**
     * 分类树专用缓存：容量小、过期时间短
     */
    @Bean
    public Caffeine<Object, Object> categoryTreeCaffeine() {
        return Caffeine.newBuilder()
                .maximumSize(10)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats();
    }

    /**
     * 标签列表缓存：中等容量
     */
    @Bean
    public Caffeine<Object, Object> tagListCaffeine() {
        return Caffeine.newBuilder()
                .maximumSize(50)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats();
    }

    /**
     * 敏感词库缓存：容量较大、过期时间较长
     */
    @Bean
    public Caffeine<Object, Object> sensitiveWordsCaffeine() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats();
    }

    /**
     * 敏感词白名单缓存：容量中等、过期时间较长
     */
    @Bean
    public Caffeine<Object, Object> sensitiveWhitelistCaffeine() {
        return Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats();
    }
}
