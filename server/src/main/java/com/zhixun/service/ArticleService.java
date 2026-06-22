package com.zhixun.service;

import com.zhixun.common.result.PageResult;
import com.zhixun.dto.article.ArticleCreateRequest;
import com.zhixun.dto.article.ArticleQueryRequest;
import com.zhixun.dto.article.ArticleStatusRequest;
import com.zhixun.dto.article.ArticleUpdateRequest;
import com.zhixun.vo.ArticleDetailVO;
import com.zhixun.vo.ArticleVO;

/**
 * 文章服务接口
 */
public interface ArticleService {

    /**
     * 发布文章
     *
     * @param userId  用户ID
     * @param request 文章创建请求
     * @return 文章ID
     */
    Long createArticle(Long userId, ArticleCreateRequest request);

    /**
     * 编辑文章
     *
     * @param userId    用户ID
     * @param articleId 文章ID
     * @param request   文章更新请求
     */
    void updateArticle(Long userId, Long articleId, ArticleUpdateRequest request);

    /**
     * 获取文章详情
     *
     * @param articleId    文章ID
     * @param currentUserId 当前用户ID（未登录为null）
     * @return 文章详情 VO
     */
    ArticleDetailVO getArticleDetail(Long articleId, Long currentUserId);

    /**
     * 获取文章列表
     *
     * @param request 查询请求
     * @return 分页结果
     */
    PageResult<ArticleVO> getArticleList(ArticleQueryRequest request);

    /**
     * 删除文章（软删除）
     *
     * @param userId    用户ID
     * @param articleId 文章ID
     */
    void deleteArticle(Long userId, Long articleId);

    /**
     * 文章状态变更（管理员）
     *
     * @param userId    操作人ID
     * @param articleId 文章ID
     * @param request   状态变更请求
     */
    void updateArticleStatus(Long userId, Long articleId, ArticleStatusRequest request);
}
