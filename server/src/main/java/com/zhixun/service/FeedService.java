package com.zhixun.service;

import com.zhixun.common.result.PageResult;
import com.zhixun.vo.ArticleVO;

import java.time.LocalDateTime;

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
     * @return 推荐作品分页结果
     */
    PageResult<ArticleVO> getRecommendFeed(Long userId, Integer refresh, Integer page, Integer pageSize);

    /**
     * 最新发布
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @return 最新作品分页结果
     */
    PageResult<ArticleVO> getLatestFeed(Integer page, Integer pageSize);

    /**
     * 关注动态
     *
     * @param userId   用户ID
     * @param page     页码
     * @param pageSize 每页大小
     * @return 关注用户的作品分页结果
     */
    PageResult<ArticleVO> getFollowingFeed(Long userId, Integer page, Integer pageSize);

    /**
     * 关注动态（游标分页，支持无限滚动）
     *
     * @param userId   用户ID
     * @param cursor   游标（上一页最后一条作品的发布时间，首次为null）
     * @param pageSize 每页大小
     * @return 关注用户的作品分页结果
     */
    PageResult<ArticleVO> getFollowingFeedByCursor(Long userId, LocalDateTime cursor, Integer pageSize);

    /**
     * 热门推荐
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @return 热门作品分页结果
     */
    PageResult<ArticleVO> getHotFeed(Integer page, Integer pageSize);

    /**
     * 作品发布后推送到粉丝的时间线（fan-out on write）
     *
     * @param authorId      作者ID
     * @param articleId     作品ID
     * @param publishedAt   发布时间
     */
    void fanoutOnPublish(Long authorId, Long articleId, LocalDateTime publishedAt);

    /**
     * 清除全局 Feed 缓存（最新、热门、推荐），在新作品发布时调用，
     * 确保新作品能及时出现在主页各列表中。
     */
    void clearFeedCaches();
}
