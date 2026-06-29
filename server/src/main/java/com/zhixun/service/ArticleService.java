package com.zhixun.service;

import com.zhixun.common.result.PageResult;
import com.zhixun.dto.article.ArticleCreateRequest;
import com.zhixun.dto.article.ArticlePublishRequest;
import com.zhixun.dto.article.ArticleQueryRequest;
import com.zhixun.dto.article.ArticleStatusRequest;
import com.zhixun.dto.article.ArticleUpdateRequest;
import com.zhixun.dto.article.ArticleVisibilityRequest;
import com.zhixun.vo.ArticleDetailVO;
import com.zhixun.vo.ArticleInteractionUserVO;
import com.zhixun.vo.ArticleVO;

import java.util.List;
import java.util.Map;

/**
 * 作品服务接口
 */
public interface ArticleService {

    /**
     * 发布作品
     *
     * @param userId   用户ID
     * @param request  作品创建请求
     * @param clientIp 客户端IP
     * @return 作品ID
     */
    Long createArticle(Long userId, ArticleCreateRequest request, String clientIp);

    /**
     * 编辑作品
     *
     * @param userId    用户ID
     * @param articleId 作品ID
     * @param request   作品更新请求
     */
    void updateArticle(Long userId, Long articleId, ArticleUpdateRequest request);

    /**
     * 获取作品详情
     *
     * @param articleId    作品ID
     * @param currentUserId 当前用户ID（未登录为null）
     * @return 作品详情 VO
     */
    ArticleDetailVO getArticleDetail(Long articleId, Long currentUserId);

    /**
     * 获取作品列表
     *
     * @param request 查询请求
     * @return 分页结果
     */
    PageResult<ArticleVO> getArticleList(ArticleQueryRequest request);

    /**
     * 删除作品（软删除）
     *
     * @param userId    用户ID
     * @param articleId 作品ID
     */
    void deleteArticle(Long userId, Long articleId);

    /**
     * 作品状态变更（管理员）
     *
     * @param userId    操作人ID
     * @param articleId 作品ID
     * @param request   状态变更请求
     */
    void updateArticleStatus(Long userId, Long articleId, ArticleStatusRequest request);

    /**
     * 相关推荐（基于标签/分类）
     * 按匹配标签数降序、浏览量降序排序，结果缓存30分钟
     *
     * @param articleId 作品ID
     * @param limit     返回数量
     * @return 相关作品列表
     */
    List<ArticleVO> getRelatedArticles(Long articleId, Integer limit);

    /**
     * 增加作品分享次数
     *
     * @param articleId 作品ID
     */
    void incrementShareCount(Long articleId);

    /**
     * 定时发布：将到期的待审核作品发布
     */
    void publishScheduledArticles();

    /**
     * 修改作品可见性
     *
     * @param userId    用户ID
     * @param articleId 作品ID
     * @param request   可见性请求
     */
    void updateArticleVisibility(Long userId, Long articleId, ArticleVisibilityRequest request);

    /**
     * 发布草稿（将草稿状态改为待审核或已发布）
     *
     * @param userId    用户ID
     * @param articleId 作品ID
     * @param request   发布请求（含定时发布时间）
     */
    void publishDraft(Long userId, Long articleId, ArticlePublishRequest request);

    /**
     * 清理过期草稿（30天未更新的草稿）
     *
     * @return 清理的草稿数量
     */
    int cleanExpiredDrafts();

    /**
     * 检查作品详情缓存与数据库数据的一致性
     * 返回不一致的作品ID列表及修复结果
     *
     * @return 一致性检查结果（包含不一致条目数、修复条目数等）
     */
    Map<String, Object> checkArticleDetailConsistency();

    /**
     * 获取作品浏览者列表（仅作者可查看）
     *
     * @param articleId     作品ID
     * @param currentUserId 当前用户ID
     * @param page          页码
     * @param pageSize      每页大小
     * @return 浏览者分页列表
     */
    PageResult<ArticleInteractionUserVO> getArticleViewers(Long articleId, Long currentUserId, int page, int pageSize);

    /**
     * 获取作品点赞者列表（仅作者可查看）
     *
     * @param articleId     作品ID
     * @param currentUserId 当前用户ID
     * @param page          页码
     * @param pageSize      每页大小
     * @return 点赞者分页列表
     */
    PageResult<ArticleInteractionUserVO> getArticleLikers(Long articleId, Long currentUserId, int page, int pageSize);
}
