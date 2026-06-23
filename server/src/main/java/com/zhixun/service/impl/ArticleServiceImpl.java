package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import com.zhixun.common.result.PageResult;
import com.zhixun.common.util.SensitiveWordUtil;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.config.RabbitMQConfig;
import com.zhixun.config.Slave;
import com.zhixun.dto.article.ArticleCreateRequest;
import com.zhixun.dto.article.ArticleQueryRequest;
import com.zhixun.dto.article.ArticleStatusRequest;
import com.zhixun.dto.article.ArticleUpdateRequest;
import com.zhixun.entity.Article;
import com.zhixun.entity.ArticleOperateLog;
import com.zhixun.entity.ArticleTag;
import com.zhixun.entity.ArticleViewHistory;
import com.zhixun.entity.Category;
import com.zhixun.entity.Tag;
import com.zhixun.entity.User;
import com.zhixun.enums.ArticleStatusEnum;
import com.zhixun.mapper.ArticleMapper;
import com.zhixun.mapper.ArticleOperateLogMapper;
import com.zhixun.mapper.ArticleTagMapper;
import com.zhixun.mapper.ArticleViewHistoryMapper;
import com.zhixun.mapper.CategoryMapper;
import com.zhixun.mapper.TagMapper;
import com.zhixun.mapper.UserMapper;
import com.zhixun.security.HtmlWhitelistFilter;
import com.zhixun.service.ArticleService;
import com.zhixun.service.FeedService;
import com.zhixun.service.OpenSearchSyncService;
import com.zhixun.vo.ArticleDetailVO;
import com.zhixun.vo.ArticleVO;
import com.zhixun.vo.TagVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 文章服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final ArticleViewHistoryMapper articleViewHistoryMapper;
    private final ArticleOperateLogMapper articleOperateLogMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final SensitiveWordUtil sensitiveWordUtil;
    private final SecurityUtil securityUtil;
    private final OpenSearchSyncService openSearchSyncService;
    private final FeedService feedService;
    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;

    /** 浏览量 Redis Key 前缀 */
    private static final String VIEW_COUNT_PREFIX = "article:view:";

    /** 相关推荐 Redis Key 前缀 */
    private static final String RELATED_ARTICLES_PREFIX = "article:related:";

    /** 相关推荐缓存时间（30分钟） */
    private static final long RELATED_CACHE_MINUTES = 30;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createArticle(Long userId, ArticleCreateRequest request) {
        Article article = new Article();
        article.setAuthorId(userId);
        article.setCategoryId(request.getCategoryId());
        article.setTitle(sanitize(request.getTitle()));
        // 先过滤敏感词，再做HTML白名单过滤
        String filteredContent = sensitiveWordUtil.filter(request.getContent());
        article.setContent(HtmlWhitelistFilter.filterRichText(filteredContent));
        article.setSummary(sanitize(request.getSummary()));
        article.setCoverImage(request.getCoverImage());
        article.setViewCount(0L);
        article.setLikeCount(0L);
        article.setCommentCount(0L);
        article.setCollectCount(0L);
        article.setIsTop(0);
        article.setIsRecommend(0);

        // 敏感词检测（异步，先保存为草稿或待审核状态）
        boolean hasSensitiveWord = checkSensitiveWord(request.getTitle(), request.getContent());
        if (hasSensitiveWord) {
            // 包含敏感词，设为待审核状态
            article.setStatus(ArticleStatusEnum.PENDING);
        } else {
            // 无敏感词，默认设为草稿状态
            article.setStatus(ArticleStatusEnum.DRAFT);
        }

        // 处理请求中的状态
        if (request.getStatus() != null) {
            ArticleStatusEnum requestStatus = ArticleStatusEnum.fromValue(request.getStatus());
            if (requestStatus == ArticleStatusEnum.DRAFT || requestStatus == ArticleStatusEnum.PENDING) {
                article.setStatus(requestStatus);
            }
        }

        // 处理定时发布
        if (request.getPublishAt() != null) {
            article.setPublishAt(request.getPublishAt());
        }

        // 如果状态为已发布，设置发布时间
        if (article.getStatus() == ArticleStatusEnum.PUBLISHED) {
            article.setPublishAt(LocalDateTime.now());
        }

        // 创建文章记录
        articleMapper.insert(article);

        // 处理标签关联
        handleTags(article.getId(), request.getTagIds());

        // 同步到 OpenSearch（仅已发布状态才同步）
        if (article.getStatus() == ArticleStatusEnum.PUBLISHED) {
            openSearchSyncService.syncArticle(article.getId());
            // Fan-out on write：推送到粉丝时间线
            feedService.fanoutOnPublish(userId, article.getId(), article.getPublishAt());
        }

        return article.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateArticle(Long userId, Long articleId, ArticleUpdateRequest request) {
        Article article = getArticleOrThrow(articleId);

        // 权限校验：仅作者或管理员可编辑
        if (!securityUtil.isOwnerOrAdmin(article.getAuthorId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权编辑此文章");
        }

        // 更新文章内容
        article.setTitle(sanitize(request.getTitle()));
        // 先过滤敏感词，再做HTML白名单过滤
        String filteredContent = sensitiveWordUtil.filter(request.getContent());
        article.setContent(HtmlWhitelistFilter.filterRichText(filteredContent));
        article.setSummary(sanitize(request.getSummary()));
        article.setCategoryId(request.getCategoryId());
        article.setCoverImage(request.getCoverImage());

        // 敏感词检测
        boolean hasSensitiveWord = checkSensitiveWord(request.getTitle(), request.getContent());
        if (hasSensitiveWord) {
            article.setStatus(ArticleStatusEnum.PENDING);
        }

        // 如果状态变更为已发布，设置发布时间
        if (article.getStatus() == ArticleStatusEnum.PUBLISHED && article.getPublishAt() == null) {
            article.setPublishAt(LocalDateTime.now());
        }

        articleMapper.updateById(article);

        // 更新标签关联
        handleTags(articleId, request.getTagIds());

        // 同步到 OpenSearch
        openSearchSyncService.syncArticle(articleId);
    }

    @Override
    @Slave
    public ArticleDetailVO getArticleDetail(Long articleId, Long currentUserId) {
        Article article = getArticleOrThrow(articleId);

        // 未登录用户只能查看已发布的文章
        if (currentUserId == null && article.getStatus() != ArticleStatusEnum.PUBLISHED) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文章不存在");
        }

        // 已登录非作者/管理员只能查看已发布的文章
        if (currentUserId != null
                && article.getStatus() != ArticleStatusEnum.PUBLISHED
                && !article.getAuthorId().equals(currentUserId)
                && !securityUtil.isAdmin()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文章不存在");
        }

        // 构建详情 VO
        ArticleDetailVO vo = new ArticleDetailVO();
        vo.setId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());
        vo.setContent(article.getContent());
        vo.setCoverImage(article.getCoverImage());
        vo.setStatus(article.getStatus() != null ? article.getStatus().getValue() : null);
        vo.setViewCount(article.getViewCount());
        vo.setLikeCount(article.getLikeCount());
        vo.setCommentCount(article.getCommentCount());
        vo.setCollectCount(article.getCollectCount());
        vo.setShareCount(article.getShareCount());
        vo.setRejectReason(article.getRejectReason());
        vo.setCategoryId(article.getCategoryId());
        vo.setCreatedAt(article.getCreatedAt());
        vo.setUpdatedAt(article.getUpdatedAt());

        // 查询作者信息
        User author = userMapper.selectById(article.getAuthorId());
        if (author != null) {
            vo.setAuthorName(author.getNickname());
            vo.setAuthorAvatar(author.getAvatar());
        }

        // 查询分类信息
        if (article.getCategoryId() != null) {
            Category category = categoryMapper.selectById(article.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }

        // 查询标签列表
        vo.setTags(getArticleTags(articleId));

        // 获取 Redis 中的浏览量
        String viewCountStr = stringRedisTemplate.opsForValue().get(VIEW_COUNT_PREFIX + articleId);
        if (viewCountStr != null) {
            vo.setViewCount(article.getViewCount() + Long.parseLong(viewCountStr));
        }

        // 记录浏览历史（异步）
        recordViewHistoryAsync(articleId, currentUserId);

        // 增加浏览量（异步，Redis 计数后批量写 DB）
        incrementViewCountAsync(articleId);

        return vo;
    }

    @Override
    @Slave
    public PageResult<ArticleVO> getArticleList(ArticleQueryRequest request) {
        // 构建查询条件
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();

        // 公开查询默认只查已发布
        if (request.getStatus() != null) {
            wrapper.eq(Article::getStatus, ArticleStatusEnum.fromValue(request.getStatus()));
        } else {
            wrapper.eq(Article::getStatus, ArticleStatusEnum.PUBLISHED);
        }

        // 按分类筛选
        if (request.getCategoryId() != null) {
            wrapper.eq(Article::getCategoryId, request.getCategoryId());
        }

        // 按作者筛选
        if (request.getUserId() != null) {
            wrapper.eq(Article::getAuthorId, request.getUserId());
        }

        // 按关键词筛选
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w.like(Article::getTitle, request.getKeyword())
                    .or().like(Article::getSummary, request.getKeyword()));
        }

        // 按标签筛选（需要先查询该标签下的文章ID）
        if (request.getTagId() != null) {
            List<ArticleTag> articleTags = articleTagMapper.selectList(
                    new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getTagId, request.getTagId()));
            if (articleTags.isEmpty()) {
                return new PageResult<>(Collections.emptyList(), 0L, request.getPageNum(), request.getPageSize());
            }
            List<Long> articleIds = articleTags.stream()
                    .map(ArticleTag::getArticleId)
                    .collect(Collectors.toList());
            wrapper.in(Article::getId, articleIds);
        }

        // 排序方式
        String sort = request.getSortBy();
        if ("hot".equals(sort)) {
            // 热门：按浏览量降序
            wrapper.orderByDesc(Article::getViewCount).orderByDesc(Article::getCreatedAt);
        } else if ("recommend".equals(sort)) {
            // 推荐：按点赞数+浏览量降序
            wrapper.orderByDesc(Article::getIsTop).orderByDesc(Article::getLikeCount).orderByDesc(Article::getViewCount);
        } else {
            // 最新：按创建时间降序
            wrapper.orderByDesc(Article::getIsTop).orderByDesc(Article::getCreatedAt);
        }

        // 分页查询
        Page<Article> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<Article> result = articleMapper.selectPage(page, wrapper);

        // 批量查询关联数据
        List<ArticleVO> voList = convertToVOList(result.getRecords());

        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteArticle(Long userId, Long articleId) {
        Article article = getArticleOrThrow(articleId);

        // 权限校验：仅作者或管理员可删除
        if (!securityUtil.isOwnerOrAdmin(article.getAuthorId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除此文章");
        }

        // 软删除文章
        Article softDelete = new Article();
        softDelete.setId(articleId);
        softDelete.setDeletedAt(LocalDateTime.now());
        articleMapper.updateById(softDelete);

        // 删除标签关联
        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, articleId));

        // 从 OpenSearch 删除文章和关联图片索引
        openSearchSyncService.deleteArticle(articleId);
        openSearchSyncService.deleteImagesByArticle(articleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateArticleStatus(Long userId, Long articleId, ArticleStatusRequest request) {
        // 仅管理员可操作
        if (!securityUtil.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可变更文章状态");
        }

        Article article = getArticleOrThrow(articleId);

        ArticleStatusEnum targetStatus = ArticleStatusEnum.fromValue(request.getStatus());
        String action = null;
        String reason = request.getRejectReason();

        // 确定操作类型
        if (targetStatus == ArticleStatusEnum.PUBLISHED) {
            // 审核通过
            action = ArticleOperateLog.ACTION_APPROVE;
            article.setStatus(ArticleStatusEnum.PUBLISHED);
            if (article.getPublishAt() == null) {
                article.setPublishAt(LocalDateTime.now());
            }
            article.setRejectReason(null);
        } else if (targetStatus == ArticleStatusEnum.REJECTED) {
            // 驳回
            action = ArticleOperateLog.ACTION_REJECT;
            if (!StringUtils.hasText(reason)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "驳回需填写原因");
            }
            article.setStatus(ArticleStatusEnum.REJECTED);
            article.setRejectReason(reason);
        } else if (targetStatus == ArticleStatusEnum.OFFLINE) {
            // 下架
            action = ArticleOperateLog.ACTION_TAKEDOWN;
            if (!StringUtils.hasText(reason)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "下架需填写原因");
            }
            article.setStatus(ArticleStatusEnum.OFFLINE);
            article.setRejectReason(reason);
        } else {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的状态变更");
        }

        articleMapper.updateById(article);

        // 记录操作日志
        ArticleOperateLog operateLog = new ArticleOperateLog();
        operateLog.setArticleId(articleId);
        operateLog.setOperatorId(userId);
        operateLog.setAction(action);
        operateLog.setReason(reason);
        articleOperateLogMapper.insert(operateLog);

        // 通过 RabbitMQ 发送通知给作者
        try {
            Map<String, Object> mqMessage = Map.of(
                    "action", action,
                    "articleId", articleId,
                    "authorId", article.getAuthorId(),
                    "reason", reason != null ? reason : "",
                    "data", Map.of(
                            "userId", article.getAuthorId(),
                            "type", "ARTICLE_STATUS",
                            "title", "文章状态变更",
                            "content", "您的文章「" + article.getTitle() + "」状态已变更为：" + targetStatus.getDescription()
                    )
            );
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ARTICLE_EVENT_EXCHANGE,
                    "zhixun.article.status",
                    mqMessage);
        } catch (Exception e) {
            log.warn("RabbitMQ 推送文章状态变更通知失败: {}", e.getMessage());
        }

        // 同步到 OpenSearch
        if (targetStatus == ArticleStatusEnum.PUBLISHED) {
            openSearchSyncService.syncArticle(articleId);
            openSearchSyncService.syncImagesByArticle(articleId);
            // Fan-out on write：推送到粉丝时间线
            feedService.fanoutOnPublish(article.getAuthorId(), articleId, article.getPublishAt());
        } else {
            // 非发布状态从索引中删除
            openSearchSyncService.deleteArticle(articleId);
            openSearchSyncService.deleteImagesByArticle(articleId);
        }
    }

    @Override
    public List<ArticleVO> getRelatedArticles(Long articleId, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 6;
        }

        // 尝试从缓存获取
        String cacheKey = RELATED_ARTICLES_PREFIX + articleId;
        String cachedIds = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedIds != null && !cachedIds.isEmpty()) {
            List<Long> articleIds = parseRelatedIds(cachedIds);
            if (!articleIds.isEmpty()) {
                List<Article> articles = articleMapper.selectBatchIds(articleIds);
                // 保持缓存中的顺序
                Map<Long, Article> articleMap = articles.stream()
                        .collect(Collectors.toMap(Article::getId, a -> a));
                List<Article> ordered = articleIds.stream()
                        .map(articleMap::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                return convertToVOList(ordered);
            }
        }

        // 缓存未命中，查询数据库
        Article currentArticle = articleMapper.selectById(articleId);
        if (currentArticle == null || currentArticle.getDeletedAt() != null) {
            return Collections.emptyList();
        }

        // 获取当前文章的标签
        List<ArticleTag> articleTags = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, articleId));
        Set<Long> currentTagIds = articleTags.stream()
                .map(ArticleTag::getTagId)
                .collect(Collectors.toSet());

        // 查询同标签的文章，统计每个文章匹配的标签数
        Map<Long, Long> tagMatchCount = new HashMap<>();
        if (!currentTagIds.isEmpty()) {
            List<ArticleTag> relatedTags = articleTagMapper.selectList(
                    new LambdaQueryWrapper<ArticleTag>()
                            .in(ArticleTag::getTagId, currentTagIds)
                            .ne(ArticleTag::getArticleId, articleId));
            tagMatchCount = relatedTags.stream()
                    .collect(Collectors.groupingBy(ArticleTag::getArticleId, Collectors.counting()));
        }

        // 构建查询：同标签或同分类的已发布文章
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                .ne(Article::getId, articleId);

        Set<Long> tagMatchedIds = tagMatchCount.keySet();
        boolean hasTagMatches = !tagMatchedIds.isEmpty();
        boolean hasCategory = currentArticle.getCategoryId() != null;

        if (hasTagMatches && hasCategory) {
            wrapper.and(w -> w.in(Article::getId, tagMatchedIds)
                    .or().eq(Article::getCategoryId, currentArticle.getCategoryId()));
        } else if (hasTagMatches) {
            wrapper.in(Article::getId, tagMatchedIds);
        } else if (hasCategory) {
            wrapper.eq(Article::getCategoryId, currentArticle.getCategoryId());
        } else {
            // 无标签无分类，无法推荐
            return Collections.emptyList();
        }

        wrapper.orderByDesc(Article::getViewCount)
                .last("LIMIT " + limit * 3);

        List<Article> candidates = articleMapper.selectList(wrapper);

        // 按匹配标签数降序、浏览量降序排序
        final Map<Long, Long> finalTagMatchCount = tagMatchCount;
        candidates.sort(Comparator
                .comparing((Article a) -> finalTagMatchCount.getOrDefault(a.getId(), 0L), Comparator.reverseOrder())
                .thenComparing(Article::getViewCount, Comparator.nullsLast(Comparator.reverseOrder())));

        List<Article> result = candidates.stream()
                .limit(limit)
                .collect(Collectors.toList());

        // 缓存结果（文章ID列表）
        List<Long> resultIds = result.stream()
                .map(Article::getId)
                .collect(Collectors.toList());
        if (!resultIds.isEmpty()) {
            String idsStr = resultIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            stringRedisTemplate.opsForValue().set(cacheKey, idsStr, RELATED_CACHE_MINUTES, TimeUnit.MINUTES);
        }

        return convertToVOList(result);
    }

    @Override
    public void incrementShareCount(Long articleId) {
        // 校验文章是否存在
        getArticleOrThrow(articleId);
        // 使用 SQL 增量更新分享次数
        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getId, articleId)
                .setSql("share_count = share_count + 1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishScheduledArticles() {
        LocalDateTime now = LocalDateTime.now();

        // 查询所有 status=PENDING 且 publishAt <= now() 且 publishAt IS NOT NULL 的文章
        List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, ArticleStatusEnum.PENDING)
                        .isNotNull(Article::getPublishAt)
                        .le(Article::getPublishAt, now)
        );

        if (articles.isEmpty()) {
            return;
        }

        log.info("定时发布：找到 {} 篇到期文章", articles.size());

        for (Article article : articles) {
            try {
                // 更新状态为已发布
                article.setStatus(ArticleStatusEnum.PUBLISHED);
                articleMapper.updateById(article);

                // 同步到 OpenSearch
                openSearchSyncService.syncArticle(article.getId());

                // 推送到粉丝时间线
                feedService.fanoutOnPublish(article.getAuthorId(), article.getId(), article.getPublishAt());

                // 清除相关 Redis 缓存
                clearArticleCache(article.getId());

                log.info("定时发布：文章 {} 已发布", article.getId());
            } catch (Exception e) {
                log.error("定时发布：文章 {} 发布失败: {}", article.getId(), e.getMessage());
            }
        }
    }

    // ========== 内部方法 ==========

    /**
     * 清除文章相关 Redis 缓存
     */
    private void clearArticleCache(Long articleId) {
        try {
            stringRedisTemplate.delete(VIEW_COUNT_PREFIX + articleId);
            stringRedisTemplate.delete(RELATED_ARTICLES_PREFIX + articleId);
        } catch (Exception e) {
            log.warn("清除文章 {} 缓存失败: {}", articleId, e.getMessage());
        }
    }

    /**
     * 获取文章或抛出异常
     */
    private Article getArticleOrThrow(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文章不存在");
        }
        if (article.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文章不存在");
        }
        return article;
    }

    /**
     * 解析缓存的相关推荐文章ID字符串
     */
    private List<Long> parseRelatedIds(String idsStr) {
        List<Long> ids = new ArrayList<>();
        for (String id : idsStr.split(",")) {
            try {
                ids.add(Long.parseLong(id.trim()));
            } catch (NumberFormatException e) {
                log.warn("解析相关推荐文章ID失败: {}", id);
            }
        }
        return ids;
    }

    /**
     * 敏感词检测
     */
    private boolean checkSensitiveWord(String title, String content) {
        return sensitiveWordUtil.containsSensitiveWord(title)
                || sensitiveWordUtil.containsSensitiveWord(content);
    }

    /**
     * XSS 防护过滤
     * - 标题/摘要等纯文本字段：HTML 转义
     * - 富文本内容字段：由调用处单独使用 HtmlWhitelistFilter.filterRichText() 白名单过滤
     */
    private String sanitize(String input) {
        if (input == null) return null;
        return HtmlWhitelistFilter.escapePlainText(input);
    }

    /**
     * 处理标签关联
     */
    private void handleTags(Long articleId, List<Long> tagIds) {
        // 先删除旧的标签关联
        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, articleId));

        // 插入新的标签关联
        if (!CollectionUtils.isEmpty(tagIds)) {
            for (Long tagId : tagIds) {
                ArticleTag articleTag = new ArticleTag();
                articleTag.setArticleId(articleId);
                articleTag.setTagId(tagId);
                articleTagMapper.insert(articleTag);
            }
        }
    }

    /**
     * 获取文章的标签列表
     */
    private List<TagVO> getArticleTags(Long articleId) {
        List<ArticleTag> articleTags = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, articleId));
        if (articleTags.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> tagIds = articleTags.stream().map(ArticleTag::getTagId).collect(Collectors.toList());
        List<Tag> tags = tagMapper.selectBatchIds(tagIds);
        return tags.stream().map(tag -> {
            TagVO vo = new TagVO();
            vo.setId(tag.getId());
            vo.setName(tag.getName());
            vo.setArticleCount(tag.getArticleCount());
            vo.setCreatedAt(tag.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 批量将文章实体列表转换为 VO 列表
     */
    private List<ArticleVO> convertToVOList(List<Article> articles) {
        if (CollectionUtils.isEmpty(articles)) {
            return Collections.emptyList();
        }

        // 收集所有需要查询的 ID
        Set<Long> userIds = articles.stream().map(Article::getAuthorId).collect(Collectors.toSet());
        Set<Long> categoryIds = articles.stream()
                .map(Article::getCategoryId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        List<Long> articleIds = articles.stream().map(Article::getId).collect(Collectors.toList());

        // 批量查询作者信息
        Map<Long, User> userMap = CollectionUtils.isEmpty(userIds) ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 批量查询分类信息
        Map<Long, Category> categoryMap = CollectionUtils.isEmpty(categoryIds) ? Collections.emptyMap()
                : categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        // 批量查询标签关联
        List<ArticleTag> allArticleTags = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>().in(ArticleTag::getArticleId, articleIds));
        Set<Long> tagIds = allArticleTags.stream().map(ArticleTag::getTagId).collect(Collectors.toSet());

        // 批量查询标签信息
        Map<Long, Tag> tagMap = CollectionUtils.isEmpty(tagIds) ? Collections.emptyMap()
                : tagMapper.selectBatchIds(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, t -> t));

        // 按文章ID分组标签
        Map<Long, List<ArticleTag>> articleTagMap = allArticleTags.stream()
                .collect(Collectors.groupingBy(ArticleTag::getArticleId));

        // 构建 VO 列表
        return articles.stream().map(article -> {
            ArticleVO vo = new ArticleVO();
            vo.setId(article.getId());
            vo.setTitle(article.getTitle());
            vo.setSummary(article.getSummary());
            vo.setCoverImage(article.getCoverImage());
            vo.setStatus(article.getStatus() != null ? article.getStatus().getValue() : null);
            vo.setViewCount(article.getViewCount());
            vo.setLikeCount(article.getLikeCount());
            vo.setCommentCount(article.getCommentCount());
            vo.setCollectCount(article.getCollectCount());
            vo.setIsTop(article.getIsTop());
            vo.setCreatedAt(article.getCreatedAt());

            // 设置作者信息
            User author = userMap.get(article.getAuthorId());
            if (author != null) {
                vo.setAuthorName(author.getNickname());
                vo.setAuthorAvatar(author.getAvatar());
            }

            // 设置分类信息
            if (article.getCategoryId() != null) {
                Category category = categoryMap.get(article.getCategoryId());
                if (category != null) {
                    vo.setCategoryName(category.getName());
                }
            }

            // 设置标签列表
            List<ArticleTag> articleTags = articleTagMap.get(article.getId());
            if (!CollectionUtils.isEmpty(articleTags)) {
                List<TagVO> tags = articleTags.stream().map(at -> {
                    Tag tag = tagMap.get(at.getTagId());
                    if (tag != null) {
                        TagVO tagVO = new TagVO();
                        tagVO.setId(tag.getId());
                        tagVO.setName(tag.getName());
                        tagVO.setArticleCount(tag.getArticleCount());
                        tagVO.setCreatedAt(tag.getCreatedAt());
                        return tagVO;
                    }
                    return null;
                }).filter(t -> t != null).collect(Collectors.toList());
                vo.setTags(tags);
            } else {
                vo.setTags(Collections.emptyList());
            }

            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 异步记录浏览历史
     */
    @Async
    public void recordViewHistoryAsync(Long articleId, Long userId) {
        try {
            ArticleViewHistory history = new ArticleViewHistory();
            history.setArticleId(articleId);
            history.setUserId(userId);
            articleViewHistoryMapper.insert(history);
        } catch (Exception e) {
            log.error("记录浏览历史失败: {}", e.getMessage());
        }
    }

    /**
     * 异步增加浏览量（Redis 计数）
     */
    @Async
    public void incrementViewCountAsync(Long articleId) {
        try {
            String key = VIEW_COUNT_PREFIX + articleId;
            Long count = stringRedisTemplate.opsForValue().increment(key);
            // 设置过期时间（首次计数时）
            if (count != null && count == 1) {
                stringRedisTemplate.expire(key, 1, TimeUnit.HOURS);
            }
            // 当累计浏览量达到阈值时，批量写入数据库
            if (count != null && count % 10 == 0) {
                // 使用 SQL 增量更新，避免覆盖已有浏览量
                articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                        .eq(Article::getId, articleId)
                        .setSql("view_count = view_count + " + count));
                // 重置 Redis 计数
                stringRedisTemplate.opsForValue().set(key, "0");
            }
        } catch (Exception e) {
            log.error("增加浏览量失败: {}", e.getMessage());
        }
    }
}
