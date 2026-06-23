package com.zhixun.service;

import com.zhixun.vo.HotArticleVO;
import com.zhixun.vo.TagVO;
import com.zhixun.vo.UserVO;

import java.util.List;

/**
 * 排行服务接口
 */
public interface RankService {

    /**
     * 热点排行榜
     *
     * @param period     时间周期：daily/weekly/monthly
     * @param categoryId 分类ID（可选）
     * @param limit      返回数量
     * @return 热榜文章列表
     */
    List<HotArticleVO> getHotRank(String period, Long categoryId, Integer limit);

    /**
     * 热门推荐（分类维度）
     *
     * @param categoryId 分类ID（可选，null则返回全站热门）
     * @param limit      返回数量
     * @return 热门文章列表
     */
    List<HotArticleVO> getHotArticles(Long categoryId, Integer limit);

    /**
     * 相关推荐（基于标签/分类）
     *
     * @param articleId 文章ID
     * @param limit    返回数量
     * @return 相关文章列表
     */
    List<HotArticleVO> getRelatedArticles(Long articleId, Integer limit);

    /**
     * 热门标签（最近7天文章数最多的标签）
     *
     * @param limit 返回数量
     * @return 热门标签列表
     */
    List<TagVO> getHotTags(int limit);

    /**
     * 热门用户（最近7天文章浏览量最高的用户）
     *
     * @param limit 返回数量
     * @return 热门用户列表
     */
    List<UserVO> getHotUsers(int limit);

    /**
     * 实时热榜（基于 Redis Sorted Set 滑动窗口）
     *
     * @param limit 返回数量
     * @return 实时热门文章列表
     */
    List<HotArticleVO> getRealtimeHot(int limit);
}
