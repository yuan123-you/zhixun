package com.zhixun.common.util;

import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import com.zhixun.config.CaffeineCacheConfig;
import com.zhixun.entity.SensitiveWhitelist;
import com.zhixun.entity.SensitiveWord;
import com.zhixun.enums.SensitiveLevelEnum;
import com.zhixun.mapper.SensitiveWhitelistMapper;
import com.zhixun.mapper.SensitiveWordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 敏感词过滤工具类
 * 基于 DFA（确定有穷自动机）Trie 树算法实现，时间复杂度 O(n)
 * 支持级别过滤和白名单机制
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SensitiveWordUtil {

    /** 敏感词替换符 */
    private static final String REPLACEMENT = "***";

    /** DFA Trie 中标记词尾的 key */
    private static final String END_FLAG = "isEnd";

    /** DFA 字典树根节点 */
    private volatile Map<String, Object> dfaRoot = new HashMap<>();

    /** 敏感词级别映射（word -> level） */
    private volatile Map<String, SensitiveLevelEnum> wordLevelMap = new HashMap<>();

    private final SensitiveWordMapper sensitiveWordMapper;
    private final SensitiveWhitelistMapper sensitiveWhitelistMapper;

    // ========== 缓存管理 ==========

    /**
     * 获取敏感词库（带 Caffeine 本地缓存）
     */
    @Cacheable(cacheNames = CaffeineCacheConfig.CACHE_SENSITIVE_WORDS)
    public List<SensitiveWord> loadSensitiveWords() {
        List<SensitiveWord> words = sensitiveWordMapper.selectList(null);
        log.info("从数据库加载敏感词库，共 {} 个敏感词", words.size());
        return words;
    }

    /**
     * 获取白名单（带 Caffeine 本地缓存）
     */
    @Cacheable(cacheNames = CaffeineCacheConfig.CACHE_SENSITIVE_WHITELIST)
    public List<SensitiveWhitelist> loadWhitelist() {
        List<SensitiveWhitelist> list = sensitiveWhitelistMapper.selectList(null);
        log.info("从数据库加载敏感词白名单，共 {} 条", list.size());
        return list;
    }

    /**
     * 清除敏感词缓存（增删改时调用）
     */
    @CacheEvict(cacheNames = CaffeineCacheConfig.CACHE_SENSITIVE_WORDS, allEntries = true)
    public void evictCache() {
        log.info("敏感词缓存已清除");
    }

    /**
     * 清除白名单缓存（增删改时调用）
     */
    @CacheEvict(cacheNames = CaffeineCacheConfig.CACHE_SENSITIVE_WHITELIST, allEntries = true)
    public void evictWhitelistCache() {
        log.info("敏感词白名单缓存已清除");
    }

    // ========== DFA 构建 ==========

    /**
     * 构建 DFA 字典树
     * 从数据库加载所有敏感词，构建 Trie 树
     */
    @SuppressWarnings("unchecked")
    public synchronized void buildDFA() {
        List<SensitiveWord> words = loadSensitiveWords();
        Map<String, Object> root = new HashMap<>(words.size());
        Map<String, SensitiveLevelEnum> levelMap = new HashMap<>(words.size());

        for (SensitiveWord word : words) {
            String w = word.getWord().toLowerCase();
            levelMap.put(w, word.getLevel());
            addToDFA(root, w);
        }

        this.dfaRoot = root;
        this.wordLevelMap = levelMap;
        log.info("DFA 字典树构建完成，共 {} 个敏感词", words.size());
    }

    /**
     * 向 DFA 字典树中添加一个敏感词
     */
    @SuppressWarnings("unchecked")
    private void addToDFA(Map<String, Object> root, String word) {
        Map<String, Object> current = root;
        for (int i = 0; i < word.length(); i++) {
            String ch = String.valueOf(word.charAt(i));
            Map<String, Object> next = (Map<String, Object>) current.get(ch);
            if (next == null) {
                next = new HashMap<>(2);
                current.put(ch, next);
            }
            current = next;
        }
        current.put(END_FLAG, "1");
    }

    /**
     * 确保 DFA 已初始化（双重检查锁）
     */
    private void ensureDFAInitialized() {
        if (dfaRoot.isEmpty()) {
            synchronized (this) {
                if (dfaRoot.isEmpty()) {
                    buildDFA();
                }
            }
        }
    }

    // ========== 白名单检查 ==========

    /**
     * 检查指定位置匹配到的敏感词是否在白名单中
     * 如果敏感词是白名单词的子串，则视为白名单命中
     *
     * @param text     原始文本
     * @param startIdx 敏感词在文本中的起始位置
     * @param endIdx   敏感词在文本中的结束位置（不含）
     * @return true-在白名单中，不应被过滤
     */
    private boolean isWhitelisted(String text, int startIdx, int endIdx) {
        List<SensitiveWhitelist> whitelist = loadWhitelist();
        if (whitelist.isEmpty()) {
            return false;
        }

        String matchedWord = text.substring(startIdx, endIdx).toLowerCase();

        for (SensitiveWhitelist entry : whitelist) {
            String whiteWord = entry.getWord().toLowerCase();
            // 在文本中查找所有白名单词出现的位置
            int idx = 0;
            while (idx < text.length()) {
                int foundAt = text.toLowerCase().indexOf(whiteWord, idx);
                if (foundAt == -1) {
                    break;
                }
                int whiteEnd = foundAt + whiteWord.length();
                // 如果敏感词匹配区域完全在白名单词区域内，则白名单生效
                if (startIdx >= foundAt && endIdx <= whiteEnd) {
                    return true;
                }
                idx = foundAt + 1;
            }
        }
        return false;
    }

    // ========== DFA 扫描 ==========

    /**
     * 使用 DFA 算法扫描文本，找到所有敏感词及其位置和级别
     *
     * @param text 待扫描文本
     * @return 匹配结果列表
     */
    @SuppressWarnings("unchecked")
    private List<MatchResult> scanText(String text) {
        List<MatchResult> results = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return results;
        }

        ensureDFAInitialized();

        String lowerText = text.toLowerCase();
        int length = lowerText.length();
        Map<String, Object> root = this.dfaRoot;

        for (int i = 0; i < length; i++) {
            Map<String, Object> current = root;
            int j = i;
            int lastMatchEnd = -1;

            while (j < length) {
                String ch = String.valueOf(lowerText.charAt(j));
                Map<String, Object> next = (Map<String, Object>) current.get(ch);
                if (next == null) {
                    break;
                }
                current = next;
                if ("1".equals(current.get(END_FLAG))) {
                    lastMatchEnd = j + 1;
                }
                j++;
            }

            // 贪心匹配：取最长的敏感词
            if (lastMatchEnd != -1) {
                String matchedWord = lowerText.substring(i, lastMatchEnd);
                SensitiveLevelEnum level = wordLevelMap.get(matchedWord);

                // 白名单检查
                if (!isWhitelisted(text, i, lastMatchEnd)) {
                    results.add(new MatchResult(i, lastMatchEnd, matchedWord, level));
                }

                // 跳过已匹配的部分
                i = lastMatchEnd - 1;
            }
        }

        return results;
    }

    // ========== 公开 API ==========

    /**
     * 检查文本是否包含敏感词
     *
     * @param text 待检查文本
     * @return true-包含敏感词
     */
    public boolean containsSensitiveWord(String text) {
        return !scanText(text).isEmpty();
    }

    /**
     * 检查文本是否包含指定级别及以上的敏感词
     *
     * @param text  待检查文本
     * @param level 最低级别（包含该级别及以上）
     * @return true-包含满足级别的敏感词
     */
    public boolean containsSensitiveWord(String text, SensitiveLevelEnum level) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        List<MatchResult> results = scanText(text);
        return results.stream().anyMatch(r -> r.level != null && r.level.getValue() >= level.getValue());
    }

    /**
     * 过滤敏感词（低级和中级别替换为***，高级别抛出异常）
     *
     * @param text 待过滤文本
     * @return 过滤后的文本
     */
    public String filter(String text) {
        return filter(text, SensitiveLevelEnum.LOW);
    }

    /**
     * 过滤指定级别及以上的敏感词
     * - LOW(1): 替换为***
     * - MEDIUM(2): 替换为***，并记录日志
     * - HIGH(3): 抛出异常，直接拦截
     *
     * @param text         待过滤文本
     * @param minLevel     最低过滤级别（过滤该级别及以上）
     * @return 过滤后的文本
     */
    public String filter(String text, SensitiveLevelEnum minLevel) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        List<MatchResult> results = scanText(text);
        if (results.isEmpty()) {
            return text;
        }

        // 检查是否有高级别敏感词
        for (MatchResult result : results) {
            if (result.level == SensitiveLevelEnum.HIGH && minLevel.getValue() <= SensitiveLevelEnum.HIGH.getValue()) {
                log.warn("内容包含高级别敏感词，已拦截。敏感词：{}", result.word);
                throw new BusinessException(ErrorCode.SENSITIVE_WORD_DETECTED, "内容包含严重敏感词，禁止发布");
            }
        }

        // 替换敏感词
        StringBuilder sb = new StringBuilder(text);
        // 从后往前替换，避免索引偏移
        for (int i = results.size() - 1; i >= 0; i--) {
            MatchResult result = results.get(i);
            if (result.level == null || result.level.getValue() < minLevel.getValue()) {
                continue;
            }

            sb.replace(result.startIdx, result.endIdx, REPLACEMENT);

            // 中级别记录日志
            if (result.level == SensitiveLevelEnum.MEDIUM) {
                log.warn("内容包含中级别敏感词，已替换。敏感词：{}", result.word);
            }
        }

        return sb.toString();
    }

    /**
     * 检查并过滤文本
     * 如果包含敏感词，返回过滤后的文本
     *
     * @param text 待处理文本
     * @return 处理后的文本
     */
    public String checkAndFilter(String text) {
        return checkAndFilter(text, SensitiveLevelEnum.LOW);
    }

    /**
     * 检查并过滤文本（指定最低过滤级别）
     *
     * @param text     待处理文本
     * @param minLevel 最低过滤级别
     * @return 处理后的文本
     */
    public String checkAndFilter(String text, SensitiveLevelEnum minLevel) {
        if (containsSensitiveWord(text, minLevel)) {
            return filter(text, minLevel);
        }
        return text;
    }

    // ========== 内部类 ==========

    /**
     * DFA 匹配结果
     */
    private static class MatchResult {
        /** 敏感词在文本中的起始位置 */
        final int startIdx;
        /** 敏感词在文本中的结束位置（不含） */
        final int endIdx;
        /** 匹配到的敏感词 */
        final String word;
        /** 敏感词级别 */
        final SensitiveLevelEnum level;

        MatchResult(int startIdx, int endIdx, String word, SensitiveLevelEnum level) {
            this.startIdx = startIdx;
            this.endIdx = endIdx;
            this.word = word;
            this.level = level;
        }
    }
}
