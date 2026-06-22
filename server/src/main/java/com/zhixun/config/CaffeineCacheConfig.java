package com.zhixun.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 本地缓存配置
 * 用于分类树、标签列表、敏感词库等读多写少的场景
 */
@Configuration
public class CaffeineCacheConfig {

    /** 分类树缓存名称 */
    public static final String CACHE_CATEGORY_TREE = "categoryTree";
    /** 标签列表缓存名称 */
    public static final String CACHE_TAG_LIST = "tagList";
    /** 热门标签缓存名称 */
    public static final String CACHE_TAG_HOT = "tagHot";
    /** 敏感词库缓存名称 */
    public static final String CACHE_SENSITIVE_WORDS = "sensitiveWords";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheSpecification("maximumSize=500,expireAfterWrite=10m,recordStats");
        // 注册所有缓存名称
        cacheManager.setCacheNames(java.util.List.of(
                CACHE_CATEGORY_TREE,
                CACHE_TAG_LIST,
                CACHE_TAG_HOT,
                CACHE_SENSITIVE_WORDS
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
}
