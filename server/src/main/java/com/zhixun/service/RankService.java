package com.zhixun.service;

import com.zhixun.vo.HotArticleVO;

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
}
