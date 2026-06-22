package com.zhixun.service.impl;

import com.zhixun.service.SearchHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 搜索历史服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchHistoryServiceImpl implements SearchHistoryService {

    private final StringRedisTemplate stringRedisTemplate;

    /** 搜索历史 Key 前缀 */
    private static final String SEARCH_HISTORY_PREFIX = "search:history:";
    /** 热门搜索词 Key */
    private static final String HOT_SEARCH_KEY = "search:hot";
    /** 搜索历史最大保留条数 */
    private static final int MAX_HISTORY_SIZE = 20;
    /** 搜索历史过期天数 */
    private static final long HISTORY_TTL_DAYS = 30;

    @Override
    public void recordSearchHistory(Long userId, String keyword) {
        if (userId == null || !StringUtils.hasText(keyword)) {
            return;
        }
        if (stringRedisTemplate == null) {
            return;
        }
        String key = SEARCH_HISTORY_PREFIX + userId;
        try {
            // 移除已存在的相同关键词（去重）
            stringRedisTemplate.opsForList().remove(key, 0, keyword);
            // 将关键词推入列表头部
            stringRedisTemplate.opsForList().leftPush(key, keyword);
            // 保留最近20条
            stringRedisTemplate.opsForList().trim(key, 0, MAX_HISTORY_SIZE - 1);
            // 设置过期时间
            stringRedisTemplate.expire(key, HISTORY_TTL_DAYS, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("记录搜索历史失败: {}", e.getMessage());
        }
    }

    @Override
    public List<String> getSearchHistory(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        String key = SEARCH_HISTORY_PREFIX + userId;
        try {
            List<String> history = stringRedisTemplate.opsForList().range(key, 0, MAX_HISTORY_SIZE - 1);
            return history != null ? history : new ArrayList<>();
        } catch (Exception e) {
            log.error("获取搜索历史失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public void clearSearchHistory(Long userId) {
        if (userId == null) {
            return;
        }
        String key = SEARCH_HISTORY_PREFIX + userId;
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            log.error("清空搜索历史失败: {}", e.getMessage());
        }
    }

    @Override
    public void incrementSearchCount(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }
        try {
            stringRedisTemplate.opsForZSet().incrementScore(HOT_SEARCH_KEY, keyword, 1);
        } catch (Exception e) {
            log.error("增加搜索词频次失败: {}", e.getMessage());
        }
    }

    /**
     * 获取热门搜索词
     *
     * @param limit 返回数量
     * @return 热门搜索词列表
     */
    public List<String> getHotSearchKeywords(int limit) {
        try {
            Set<String> keywords = stringRedisTemplate.opsForZSet()
                    .reverseRange(HOT_SEARCH_KEY, 0, limit - 1);
            return keywords != null ? new ArrayList<>(keywords) : new ArrayList<>();
        } catch (Exception e) {
            log.error("获取热门搜索词失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
