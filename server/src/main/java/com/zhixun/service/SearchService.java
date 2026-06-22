package com.zhixun.service;

import com.zhixun.common.result.PageResult;
import com.zhixun.vo.ArticleVO;
import com.zhixun.vo.SearchResultVO;
import com.zhixun.vo.SearchSuggestResultVO;

import java.util.List;

/**
 * 搜索服务接口
 */
public interface SearchService {

    /**
     * 综合搜索
     *
     * @param keyword    搜索关键词
     * @param type       搜索类型：all/article/user/image
     * @param categoryId 分类ID（可选）
     * @param tagId      标签ID（可选）
     * @param timeRange  时间范围：day/week/month/year（可选）
     * @param startDate  自定义起始日期（可选）
     * @param endDate    自定义结束日期（可选）
     * @param sort       排序方式：default/hot/newest（可选）
     * @param page       页码
     * @param pageSize   每页大小
     * @return 搜索结果
     */
    SearchResultVO search(String keyword, String type, Long categoryId, Long tagId,
                           String timeRange, String startDate, String endDate,
                           String sort, Integer page, Integer pageSize);

    /**
     * 搜索建议
     *
     * @param keyword 关键词
     * @return 结构化建议结果
     */
    SearchSuggestResultVO getSuggestions(String keyword);

    /**
     * 热门搜索词
     *
     * @return 热门搜索词列表
     */
    List<String> getHotSearches();

    /**
     * 清空搜索历史
     *
     * @param userId 用户ID
     */
    void clearSearchHistory(Long userId);
}
