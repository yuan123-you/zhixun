package com.zhixun.service;

import com.zhixun.common.result.PageResult;
import com.zhixun.vo.ArticleVO;

/**
 * 信息流服务接口
 */
public interface FeedService {

    /**
     * 首页个性化推荐
     *
     * @param userId   用户ID（未登录为null）
     * @param refresh  是否刷新（1=生成新批次）
     * @param page     页码
     * @param pageSize 每页大小
     * @return 推荐文章分页结果
     */
    PageResult<ArticleVO> getRecommendFeed(Long userId, Integer refresh, Integer page, Integer pageSize);

    /**
     * 最新发布
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @return 最新文章分页结果
     */
    PageResult<ArticleVO> getLatestFeed(Integer page, Integer pageSize);

    /**
     * 关注动态
     *
     * @param userId   用户ID
     * @param page     页码
     * @param pageSize 每页大小
     * @return 关注用户的文章分页结果
     */
    PageResult<ArticleVO> getFollowingFeed(Long userId, Integer page, Integer pageSize);
}
