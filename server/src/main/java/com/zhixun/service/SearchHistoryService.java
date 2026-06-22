package com.zhixun.service;

import java.util.List;

/**
 * 搜索历史服务接口
 */
public interface SearchHistoryService {

    /**
     * 记录搜索历史
     *
     * @param userId  用户ID
     * @param keyword 搜索关键词
     */
    void recordSearchHistory(Long userId, String keyword);

    /**
     * 获取搜索历史
     *
     * @param userId 用户ID
     * @return 搜索历史列表（最近20条）
     */
    List<String> getSearchHistory(Long userId);

    /**
     * 清空搜索历史
     *
     * @param userId 用户ID
     */
    void clearSearchHistory(Long userId);

    /**
     * 增加搜索词频次
     *
     * @param keyword 搜索关键词
     */
    void incrementSearchCount(String keyword);

    /**
     * 获取热门搜索词
     *
     * @param limit 返回数量
     * @return 热门搜索词列表
     */
    List<String> getHotSearchKeywords(int limit);
}
