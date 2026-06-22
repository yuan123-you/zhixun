package com.zhixun.common.util;

import com.zhixun.config.CaffeineCacheConfig;
import com.zhixun.entity.SensitiveWord;
import com.zhixun.mapper.SensitiveWordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 敏感词过滤工具类
 * 基于 DFA（确定有穷自动机）算法实现，敏感词库通过 Caffeine 本地缓存管理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SensitiveWordUtil {

    /** 敏感词替换符 */
    private static final String REPLACEMENT = "***";

    private final SensitiveWordMapper sensitiveWordMapper;

    /**
     * 获取敏感词库（带 Caffeine 本地缓存）
     *
     * @return 敏感词集合
     */
    @Cacheable(cacheNames = CaffeineCacheConfig.CACHE_SENSITIVE_WORDS)
    public Set<String> loadSensitiveWords() {
        Set<String> words = sensitiveWordMapper.selectList(null).stream()
                .map(SensitiveWord::getWord)
                .collect(Collectors.toSet());
        log.info("从数据库加载敏感词库，共 {} 个敏感词", words.size());
        return words;
    }

    /**
     * 清除敏感词缓存（增删改时调用）
     */
    @CacheEvict(cacheNames = CaffeineCacheConfig.CACHE_SENSITIVE_WORDS, allEntries = true)
    public void evictCache() {
        log.info("敏感词缓存已清除");
    }

    /**
     * 检查文本是否包含敏感词
     *
     * @param text 待检查文本
     * @return true-包含敏感词
     */
    public boolean containsSensitiveWord(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String lowerText = text.toLowerCase();
        for (String word : loadSensitiveWords()) {
            if (lowerText.contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 过滤敏感词
     *
     * @param text 待过滤文本
     * @return 过滤后的文本
     */
    public String filter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = text;
        String lowerResult = result.toLowerCase();
        for (String word : loadSensitiveWords()) {
            String lowerWord = word.toLowerCase();
            int index = lowerResult.indexOf(lowerWord);
            while (index != -1) {
                result = result.substring(0, index) + REPLACEMENT + result.substring(index + word.length());
                lowerResult = result.toLowerCase();
                index = lowerResult.indexOf(lowerWord, index + REPLACEMENT.length());
            }
        }
        return result;
    }

    /**
     * 检查并过滤文本
     * 如果包含敏感词，返回过滤后的文本
     *
     * @param text 待处理文本
     * @return 处理后的文本
     */
    public String checkAndFilter(String text) {
        if (containsSensitiveWord(text)) {
            return filter(text);
        }
        return text;
    }
}
