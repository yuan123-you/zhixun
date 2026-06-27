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
import com.zhixun.dto.article.ArticlePublishRequest;
import com.zhixun.dto.article.ArticleQueryRequest;
import com.zhixun.dto.article.ArticleStatusRequest;
import com.zhixun.dto.article.ArticleUpdateRequest;
import com.zhixun.dto.article.ArticleVisibilityRequest;
import com.zhixun.entity.Article;
import com.zhixun.entity.ArticleImage;
import com.zhixun.entity.ArticleLike;
import com.zhixun.entity.ArticleOperateLog;
import com.zhixun.entity.ArticleTag;
import com.zhixun.entity.ArticleViewHistory;
import com.zhixun.entity.Collect;
import com.zhixun.entity.UserFollow;
import com.zhixun.entity.Category;
import com.zhixun.entity.Tag;
import com.zhixun.entity.User;
import com.zhixun.enums.ArticleStatusEnum;
import com.zhixun.enums.ArticleVisibilityEnum;
import com.zhixun.enums.ImageTypeEnum;
import com.zhixun.enums.LikeTargetTypeEnum;
import com.zhixun.mapper.ArticleLikeMapper;
import com.zhixun.mapper.ArticleMapper;
import com.zhixun.mapper.ArticleOperateLogMapper;
import com.zhixun.mapper.ArticleTagMapper;
import com.zhixun.mapper.ArticleViewHistoryMapper;
import com.zhixun.mapper.CollectMapper;
import com.zhixun.mapper.UserFollowMapper;
import com.zhixun.mapper.CategoryMapper;
import com.zhixun.mapper.ArticleImageMapper;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zhixun.config.RedisConfig;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 作品服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final ArticleLikeMapper articleLikeMapper;
    private final ArticleViewHistoryMapper articleViewHistoryMapper;
    private final CollectMapper collectMapper;
    private final UserFollowMapper userFollowMapper;
    private final ArticleOperateLogMapper articleOperateLogMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final ArticleImageMapper articleImageMapper;
    private final SensitiveWordUtil sensitiveWordUtil;
    private final SecurityUtil securityUtil;
    private final OpenSearchSyncService openSearchSyncService;
    private final FeedService feedService;
    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    /** 浏览量 Redis Key 前缀 */
    private static final String VIEW_COUNT_PREFIX = "article:view:";

    /** 相关推荐 Redis Key 前缀 */
    private static final String RELATED_ARTICLES_PREFIX = "article:related:";

    /** 相关推荐缓存时间（30分钟） */
    private static final long RELATED_CACHE_MINUTES = 30;

    /** 作品列表缓存 Key 前缀 */
    private static final String ARTICLE_LIST_PREFIX = "article:list:";

    /** 作品列表缓存时间（2分钟） */
    private static final long ARTICLE_LIST_CACHE_MINUTES = 2;

    /** 作品详情缓存 Key 前缀 */
    private static final String ARTICLE_DETAIL_PREFIX = "article:detail:";

    /** 作品详情缓存时间（5分钟） */
    private static final long ARTICLE_DETAIL_CACHE_MINUTES = 5;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createArticle(Long userId, ArticleCreateRequest request, String clientIp) {
        Article article = new Article();
        article.setAuthorId(userId);
        // 处理分类：优先使用 categoryId，其次根据 categoryName 查找或创建
        Long categoryId = resolveCategoryId(request.getCategoryId(), request.getCategoryName());
        article.setCategoryId(categoryId);
        article.setTitle(sanitize(request.getTitle()));
        // 先过滤敏感词，再做HTML白名单过滤
        String rawContent = request.getContent() != null ? request.getContent() : "";
        String filteredContent;
        try {
            filteredContent = sensitiveWordUtil.filter(rawContent);
        } catch (BusinessException e) {
            // 高级别敏感词被 BusinessException 拦截，直接向上抛出由 GlobalExceptionHandler 处理
            throw e;
        } catch (Exception e) {
            log.error("敏感词过滤异常: {}", e.getMessage());
            filteredContent = rawContent;
        }
        article.setContent(HtmlWhitelistFilter.filterRichText(filteredContent));
        article.setSummary(sanitize(request.getSummary()));
        // 封面图：优先使用传入的 coverImage，否则取第一张图片
        String coverImage = request.getCoverImage();
        if (coverImage == null && request.getImages() != null && !request.getImages().isEmpty()) {
            coverImage = request.getImages().get(0);
        }
        article.setCoverImage(coverImage);
        article.setDeviceInfo(request.getDeviceInfo());
        article.setLocation(sanitize(request.getLocation()));
        article.setIpAddress(resolveIpLocation(clientIp));
        article.setViewCount(0L);
        article.setLikeCount(0L);
        article.setCommentCount(0L);
        article.setCollectCount(0L);
        article.setIsTop(0);
        article.setIsRecommend(0);

        // 敏感词检测
        boolean hasSensitiveWord;
        try {
            hasSensitiveWord = checkSensitiveWord(request.getTitle(), request.getContent());
        } catch (Exception e) {
            log.error("敏感词检测异常: {}", e.getMessage());
            hasSensitiveWord = false;
        }

        // 确定作品状态：根据用户意图（草稿/发布）和敏感词检测结果
        boolean isPublishIntent = false;
        Integer requestStatus = request.getStatus();
        if (requestStatus != null) {
            ArticleStatusEnum requestStatusEnum = ArticleStatusEnum.fromValue(requestStatus);
            // status=1（PENDING）或 status=2（PUBLISHED）都表示用户想要发布
            if (requestStatusEnum == ArticleStatusEnum.PENDING
                    || requestStatusEnum == ArticleStatusEnum.PUBLISHED) {
                isPublishIntent = true;
            }
        }

        if (isPublishIntent && !hasSensitiveWord) {
            // 用户意图发布 且 无敏感词 → 直接发布
            article.setStatus(ArticleStatusEnum.PUBLISHED);
        } else if (isPublishIntent) {
            // 用户意图发布 但 有敏感词 → 待审核
            article.setStatus(ArticleStatusEnum.PENDING);
        } else if (hasSensitiveWord) {
            // 保存草稿但有敏感词 → 待审核
            article.setStatus(ArticleStatusEnum.PENDING);
        } else {
            // 保存草稿且无敏感词 → 草稿
            article.setStatus(ArticleStatusEnum.DRAFT);
        }

        // 处理定时发布
        if (request.getPublishAt() != null) {
            article.setPublishAt(request.getPublishAt());
        }

        // 设置可见性（默认为公开）
        if (request.getVisibility() != null) {
            article.setVisibility(request.getVisibility());
        } else {
            article.setVisibility(ArticleVisibilityEnum.PUBLIC.getValue());
        }

        // 如果状态为已发布且未设置定时发布时间，设置发布时间为当前
        if (article.getStatus() == ArticleStatusEnum.PUBLISHED && article.getPublishAt() == null) {
            article.setPublishAt(LocalDateTime.now());
        }

        // 创建作品记录
        articleMapper.insert(article);

        // 更新用户作品数 +1
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .setSql("article_count = article_count + 1"));

        // 处理标签关联（支持 tagIds 和 tagNames）
        List<Long> resolvedTagIds = resolveTagIds(request.getTagIds(), request.getTagNames());
        handleTags(article.getId(), resolvedTagIds);

        // 保存作品图片
        saveArticleImages(article.getId(), request.getImages());

        // 同步到 OpenSearch（仅已发布状态才同步，非关键操作，失败不影响主事务）
        if (article.getStatus() == ArticleStatusEnum.PUBLISHED) {
            try {
                openSearchSyncService.syncArticle(article.getId());
            } catch (Exception e) {
                log.error("创建作品同步到OpenSearch失败, articleId={}: {}", article.getId(), e.getMessage());
            }
            // Fan-out on write：推送到粉丝时间线
            try {
                feedService.fanoutOnPublish(userId, article.getId(), article.getPublishAt());
            } catch (Exception e) {
                log.error("推送粉丝时间线失败, articleId={}: {}", article.getId(), e.getMessage());
            }
        }

        return article.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateArticle(Long userId, Long articleId, ArticleUpdateRequest request) {
        Article article = getArticleOrThrow(articleId);

        // 权限校验：仅作者或管理员可编辑
        if (!securityUtil.isOwnerOrAdmin(article.getAuthorId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权编辑此作品");
        }

        // 更新作品内容
        article.setTitle(sanitize(request.getTitle()));
        // 先过滤敏感词，再做HTML白名单过滤
        String filteredContent = sensitiveWordUtil.filter(request.getContent());
        article.setContent(HtmlWhitelistFilter.filterRichText(filteredContent));
        article.setSummary(sanitize(request.getSummary()));
        article.setCategoryId(request.getCategoryId());
        article.setCoverImage(request.getCoverImage());

        // 更新可见性
        if (request.getVisibility() != null) {
            article.setVisibility(request.getVisibility());
        }

        // 更新位置
        if (request.getLocation() != null) {
            article.setLocation(sanitize(request.getLocation()));
        }

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

        // 同步到 OpenSearch（非关键操作，失败不影响主事务）
        try {
            openSearchSyncService.syncArticle(articleId);
        } catch (Exception e) {
            log.error("更新作品同步到OpenSearch失败, articleId={}: {}", articleId, e.getMessage());
        }

        // 清除作品相关缓存
        clearArticleCache(articleId);
    }

    @Override
    @Slave
    public ArticleDetailVO getArticleDetail(Long articleId, Long currentUserId) {
        // 尝试从 Redis 缓存获取（仅未登录用户且作品已发布时使用缓存）
        if (currentUserId == null) {
            ArticleDetailVO cached = getArticleDetailFromCache(articleId);
            if (cached != null) {
                // 记录浏览历史（异步）
                recordViewHistoryAsync(articleId, currentUserId);
                // 增加浏览量（异步）
                incrementViewCountAsync(articleId);
                return cached;
            }
        }

        Article article = getArticleOrThrow(articleId);

        // 未登录用户只能查看已发布的作品
        if (currentUserId == null && article.getStatus() != ArticleStatusEnum.PUBLISHED) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "作品不存在");
        }

        // 已登录非作者/管理员只能查看已发布的作品
        if (currentUserId != null
                && article.getStatus() != ArticleStatusEnum.PUBLISHED
                && !article.getAuthorId().equals(currentUserId)
                && !securityUtil.isAdmin()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "作品不存在");
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
        vo.setIsTop(article.getIsTop());
        vo.setVisibility(article.getVisibility());
        vo.setRejectReason(article.getRejectReason());
        vo.setCategoryId(article.getCategoryId());
        vo.setCreatedAt(article.getCreatedAt());
        vo.setUpdatedAt(article.getUpdatedAt());
        vo.setDeviceInfo(article.getDeviceInfo());
        vo.setLocation(article.getLocation());
        vo.setIpAddress(article.getIpAddress());

        // 查询作者信息
        User author = userMapper.selectById(article.getAuthorId());
        if (author != null) {
            ArticleDetailVO.AuthorVO authorVO = new ArticleDetailVO.AuthorVO();
            authorVO.setId(author.getId());
            authorVO.setNickname(author.getNickname());
            authorVO.setAvatar(author.getAvatar());
            // 查询当前用户是否关注了该作者
            if (currentUserId != null && !currentUserId.equals(author.getId())) {
                try {
                    UserFollow follow = userFollowMapper.selectOne(
                            new LambdaQueryWrapper<UserFollow>()
                                    .eq(UserFollow::getFollowerId, currentUserId)
                                    .eq(UserFollow::getFollowingId, author.getId()));
                    authorVO.setIsFollowing(follow != null);
                } catch (Exception e) {
                    authorVO.setIsFollowing(false);
                }
            } else {
                authorVO.setIsFollowing(false);
            }
            vo.setAuthor(authorVO);
        }

        // 查询当前用户的点赞和收藏状态
        if (currentUserId != null) {
            try {
                // 点赞状态
                ArticleLike like = articleLikeMapper.selectOne(
                        new LambdaQueryWrapper<ArticleLike>()
                                .eq(ArticleLike::getUserId, currentUserId)
                                .eq(ArticleLike::getTargetId, articleId)
                                .eq(ArticleLike::getTargetType, LikeTargetTypeEnum.ARTICLE));
                vo.setIsLiked(like != null);
            } catch (Exception e) {
                vo.setIsLiked(false);
            }
            try {
                // 收藏状态
                Collect collect = collectMapper.selectOne(
                        new LambdaQueryWrapper<Collect>()
                                .eq(Collect::getUserId, currentUserId)
                                .eq(Collect::getArticleId, articleId));
                vo.setIsCollected(collect != null);
            } catch (Exception e) {
                vo.setIsCollected(false);
            }
        } else {
            vo.setIsLiked(false);
            vo.setIsCollected(false);
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

        // 查询作品图片
        vo.setImages(getArticleImageUrls(articleId));

        // 获取 Redis 中的浏览量
        try {
            String viewCountStr = stringRedisTemplate.opsForValue().get(VIEW_COUNT_PREFIX + articleId);
            if (viewCountStr != null) {
                vo.setViewCount(article.getViewCount() + Long.parseLong(viewCountStr));
            }
        } catch (Exception e) {
            log.warn("获取Redis浏览量失败，使用数据库浏览量: {}", e.getMessage());
        }

        // 缓存作品详情（仅已发布作品）
        if (article.getStatus() == ArticleStatusEnum.PUBLISHED) {
            cacheArticleDetail(articleId, vo);
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
        // 仅对公开查询（默认已发布状态）启用缓存
        boolean useCache = request.getStatus() == null || request.getStatus() == ArticleStatusEnum.PUBLISHED.getValue();
        String cacheKey = null;

        if (useCache) {
            cacheKey = buildArticleListCacheKey(request);
            PageResult<ArticleVO> cached = getArticleListFromCache(cacheKey);
            if (cached != null) {
                return cached;
            }
        }

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

        // 按标签筛选（需要先查询该标签下的作品ID）
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
        Long currentUserId = null;
        try {
            currentUserId = securityUtil.getCurrentUserId();
        } catch (Exception ignored) {}
        List<ArticleVO> voList = convertToVOList(result.getRecords(), currentUserId);

        PageResult<ArticleVO> pageResult = new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());

        // 缓存结果
        if (useCache && cacheKey != null) {
            cacheArticleList(cacheKey, pageResult);
        }

        return pageResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteArticle(Long userId, Long articleId) {
        Article article = getArticleOrThrow(articleId);

        // 权限校验：仅作者或管理员可删除
        if (!securityUtil.isOwnerOrAdmin(article.getAuthorId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除此作品");
        }

        // 软删除作品
        Article softDelete = new Article();
        softDelete.setId(articleId);
        softDelete.setDeletedAt(LocalDateTime.now());
        articleMapper.updateById(softDelete);

        // 更新用户作品数 -1
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, article.getAuthorId())
                .gt(User::getArticleCount, 0)
                .setSql("article_count = article_count - 1"));

        // 删除标签关联
        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, articleId));

        // 从 OpenSearch 删除作品和关联图片索引（非关键操作，失败不影响主事务）
        try {
            openSearchSyncService.deleteArticle(articleId);
            openSearchSyncService.deleteImagesByArticle(articleId);
        } catch (Exception e) {
            log.error("删除作品OpenSearch索引失败, articleId={}: {}", articleId, e.getMessage());
        }

        // 清除作品相关缓存
        clearArticleCache(articleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateArticleStatus(Long userId, Long articleId, ArticleStatusRequest request) {
        // 仅管理员可操作
        if (!securityUtil.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可变更作品状态");
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
                            "title", "作品状态变更",
                            "content", "您的作品「" + article.getTitle() + "」状态已变更为：" + targetStatus.getDescription()
                    )
            );
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ARTICLE_EVENT_EXCHANGE,
                    "zhixun.article.status",
                    mqMessage);
        } catch (Exception e) {
            log.warn("RabbitMQ 推送作品状态变更通知失败: {}", e.getMessage());
        }

        // 同步到 OpenSearch（非关键操作，失败不影响主事务）
        if (targetStatus == ArticleStatusEnum.PUBLISHED) {
            try {
                openSearchSyncService.syncArticle(articleId);
                openSearchSyncService.syncImagesByArticle(articleId);
            } catch (Exception e) {
                log.error("更新作品状态同步OpenSearch失败, articleId={}: {}", articleId, e.getMessage());
            }
            // Fan-out on write：推送到粉丝时间线
            try {
                feedService.fanoutOnPublish(article.getAuthorId(), articleId, article.getPublishAt());
            } catch (Exception e) {
                log.error("推送粉丝时间线失败, articleId={}: {}", articleId, e.getMessage());
            }
        } else {
            // 非发布状态从索引中删除
            try {
                openSearchSyncService.deleteArticle(articleId);
                openSearchSyncService.deleteImagesByArticle(articleId);
            } catch (Exception e) {
                log.error("删除作品OpenSearch索引失败, articleId={}: {}", articleId, e.getMessage());
            }
        }

        // 清除作品相关缓存
        clearArticleCache(articleId);
    }

    @Override
    public List<ArticleVO> getRelatedArticles(Long articleId, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 6;
        }

        // 尝试从缓存获取
        String cacheKey = RELATED_ARTICLES_PREFIX + articleId;
        try {
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
        } catch (Exception e) {
            log.warn("获取相关推荐缓存失败，将从数据库查询: {}", e.getMessage());
        }

        // 缓存未命中，查询数据库
        Article currentArticle = articleMapper.selectById(articleId);
        if (currentArticle == null || currentArticle.getDeletedAt() != null) {
            return Collections.emptyList();
        }

        // 获取当前作品的标签
        List<ArticleTag> articleTags = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, articleId));
        Set<Long> currentTagIds = articleTags.stream()
                .map(ArticleTag::getTagId)
                .collect(Collectors.toSet());

        // 查询同标签的作品，统计每个作品匹配的标签数
        Map<Long, Long> tagMatchCount = new HashMap<>();
        if (!currentTagIds.isEmpty()) {
            List<ArticleTag> relatedTags = articleTagMapper.selectList(
                    new LambdaQueryWrapper<ArticleTag>()
                            .in(ArticleTag::getTagId, currentTagIds)
                            .ne(ArticleTag::getArticleId, articleId));
            tagMatchCount = relatedTags.stream()
                    .collect(Collectors.groupingBy(ArticleTag::getArticleId, Collectors.counting()));
        }

        // 构建查询：同标签或同分类的已发布作品
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

        // 缓存结果（作品ID列表）
        List<Long> resultIds = result.stream()
                .map(Article::getId)
                .collect(Collectors.toList());
        if (!resultIds.isEmpty()) {
            try {
                String idsStr = resultIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
                stringRedisTemplate.opsForValue().set(cacheKey, idsStr, RELATED_CACHE_MINUTES, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.warn("缓存相关推荐数据失败: {}", e.getMessage());
            }
        }

        return convertToVOList(result);
    }

    @Override
    public void incrementShareCount(Long articleId) {
        // 校验作品是否存在
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

        // 查询所有 status=PENDING 且 publishAt <= now() 且 publishAt IS NOT NULL 的作品
        List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, ArticleStatusEnum.PENDING)
                        .isNotNull(Article::getPublishAt)
                        .le(Article::getPublishAt, now)
        );

        if (articles.isEmpty()) {
            return;
        }

        log.info("定时发布：找到 {} 篇到期作品", articles.size());

        for (Article article : articles) {
            try {
                // 更新状态为已发布
                article.setStatus(ArticleStatusEnum.PUBLISHED);
                articleMapper.updateById(article);

                // 同步到 OpenSearch（非关键操作，失败不影响发布）
                try {
                    openSearchSyncService.syncArticle(article.getId());
                } catch (Exception e) {
                    log.error("定时发布同步OpenSearch失败, articleId={}: {}", article.getId(), e.getMessage());
                }

                // 推送到粉丝时间线（非关键操作，失败不影响发布）
                try {
                    feedService.fanoutOnPublish(article.getAuthorId(), article.getId(), article.getPublishAt());
                } catch (Exception e) {
                    log.error("定时推送粉丝时间线失败, articleId={}: {}", article.getId(), e.getMessage());
                }

                // 清除相关 Redis 缓存
                clearArticleCache(article.getId());

                log.info("定时发布：作品 {} 已发布", article.getId());
            } catch (Exception e) {
                log.error("定时发布：作品 {} 发布失败: {}", article.getId(), e.getMessage());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateArticleVisibility(Long userId, Long articleId, ArticleVisibilityRequest request) {
        Article article = getArticleOrThrow(articleId);

        // 权限校验：仅作者可修改可见性
        if (!article.getAuthorId().equals(userId) && !securityUtil.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权修改此作品的可见性");
        }

        article.setVisibility(request.getVisibility());
        articleMapper.updateById(article);

        // 清除作品缓存
        clearArticleCache(articleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishDraft(Long userId, Long articleId, ArticlePublishRequest request) {
        Article article = getArticleOrThrow(articleId);

        // 权限校验：仅作者可操作
        if (!article.getAuthorId().equals(userId) && !securityUtil.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此作品");
        }

        // 仅草稿状态可发布
        if (article.getStatus() != ArticleStatusEnum.DRAFT) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅草稿状态的作品可以发布");
        }

        // 处理定时发布
        if (request.getPublishAt() != null) {
            // 定时发布：状态设为待审核
            article.setStatus(ArticleStatusEnum.PENDING);
            article.setPublishAt(request.getPublishAt());
        } else {
            // 立即发布：敏感词检测后决定状态
            boolean hasSensitiveWord = checkSensitiveWord(article.getTitle(), article.getContent());
            if (hasSensitiveWord) {
                article.setStatus(ArticleStatusEnum.PENDING);
            } else {
                article.setStatus(ArticleStatusEnum.PUBLISHED);
            }
            article.setPublishAt(LocalDateTime.now());
        }

        articleMapper.updateById(article);

        // 立即发布的作品同步到 OpenSearch 并推送到粉丝时间线
        if (article.getStatus() == ArticleStatusEnum.PUBLISHED) {
            try {
                openSearchSyncService.syncArticle(articleId);
            } catch (Exception e) {
                log.error("发布草稿同步OpenSearch失败, articleId={}: {}", articleId, e.getMessage());
            }
            try {
                feedService.fanoutOnPublish(article.getAuthorId(), articleId, article.getPublishAt());
            } catch (Exception e) {
                log.error("推送粉丝时间线失败, articleId={}: {}", articleId, e.getMessage());
            }
        }

        // 清除作品缓存
        clearArticleCache(articleId);
    }

    @Override
    public int cleanExpiredDrafts() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);

        // 查询30天前创建的草稿作品（未更新过的）
        List<Article> expiredDrafts = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, ArticleStatusEnum.DRAFT)
                        .lt(Article::getUpdatedAt, cutoff)
        );

        if (expiredDrafts.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (Article article : expiredDrafts) {
            try {
                // 软删除
                Article softDelete = new Article();
                softDelete.setId(article.getId());
                softDelete.setDeletedAt(LocalDateTime.now());
                articleMapper.updateById(softDelete);

                // 更新用户作品数 -1
                userMapper.update(null, new LambdaUpdateWrapper<User>()
                        .eq(User::getId, article.getAuthorId())
                        .gt(User::getArticleCount, 0)
                        .setSql("article_count = article_count - 1"));

                // 删除标签关联
                articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>()
                        .eq(ArticleTag::getArticleId, article.getId()));

                // 清除作品缓存
                clearArticleCache(article.getId());

                count++;
            } catch (Exception e) {
                log.error("清理过期草稿失败, articleId={}: {}", article.getId(), e.getMessage());
            }
        }

        if (count > 0) {
            log.info("清理过期草稿：共清理 {} 篇", count);
        }

        return count;
    }

    // ========== 内部方法 ==========

    /**
     * 清除作品相关 Redis 缓存
     */
    private void clearArticleCache(Long articleId) {
        try {
            stringRedisTemplate.delete(VIEW_COUNT_PREFIX + articleId);
            stringRedisTemplate.delete(RELATED_ARTICLES_PREFIX + articleId);
            stringRedisTemplate.delete(ARTICLE_DETAIL_PREFIX + articleId);
            // 清除作品列表缓存（使用 SCAN 安全扫描删除所有列表缓存）
            Set<String> listKeys = new java.util.HashSet<>();
            try (org.springframework.data.redis.core.Cursor<String> cursor = stringRedisTemplate.scan(
                    org.springframework.data.redis.core.ScanOptions.scanOptions()
                            .match(ARTICLE_LIST_PREFIX + "*")
                            .count(100)
                            .build())) {
                while (cursor.hasNext()) {
                    listKeys.add(cursor.next());
                }
            }
            if (!listKeys.isEmpty()) {
                stringRedisTemplate.delete(listKeys);
            }
        } catch (Exception e) {
            log.warn("清除作品 {} 缓存失败: {}", articleId, e.getMessage());
        }
        // 递增数据版本号，通知客户端数据已变更
        try {
            RedisConfig.incrementDataVersion(stringRedisTemplate);
        } catch (Exception e) {
            log.debug("递增数据版本号失败: {}", e.getMessage());
        }
    }

    /**
     * 获取作品或抛出异常
     */
    private Article getArticleOrThrow(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "作品不存在");
        }
        if (article.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "作品不存在");
        }
        return article;
    }

    /**
     * 解析缓存的相关推荐作品ID字符串
     */
    private List<Long> parseRelatedIds(String idsStr) {
        List<Long> ids = new ArrayList<>();
        for (String id : idsStr.split(",")) {
            try {
                ids.add(Long.parseLong(id.trim()));
            } catch (NumberFormatException e) {
                log.warn("解析相关推荐作品ID失败: {}", id);
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
     * 解析分类ID：优先使用 categoryId，其次根据 categoryName 查找或创建分类
     */
    private Long resolveCategoryId(Long categoryId, String categoryName) {
        if (categoryId != null) {
            return categoryId;
        }
        if (categoryName != null && !categoryName.trim().isEmpty()) {
            String name = categoryName.trim();
            // 查找已存在的分类
            Category existing = categoryMapper.selectOne(
                    new LambdaQueryWrapper<Category>().eq(Category::getName, name));
            if (existing != null) {
                return existing.getId();
            }
            // 创建新分类
            Category newCategory = new Category();
            newCategory.setName(name);
            newCategory.setStatus(1);
            newCategory.setSortOrder(0);
            categoryMapper.insert(newCategory);
            return newCategory.getId();
        }
        return null;
    }

    /**
     * 解析标签ID列表：支持 tagIds 和 tagNames，根据名称查找或创建标签
     */
    private List<Long> resolveTagIds(List<Long> tagIds, List<String> tagNames) {
        List<Long> result = new ArrayList<>();
        if (tagIds != null) {
            result.addAll(tagIds);
        }
        if (tagNames != null && !tagNames.isEmpty()) {
            for (String name : tagNames) {
                if (name == null || name.trim().isEmpty()) {
                    continue;
                }
                String trimmedName = name.trim();
                // 查找已存在的标签
                Tag existing = tagMapper.selectOne(
                        new LambdaQueryWrapper<Tag>().eq(Tag::getName, trimmedName));
                if (existing != null) {
                    if (!result.contains(existing.getId())) {
                        result.add(existing.getId());
                    }
                } else {
                    // 创建新标签
                    Tag newTag = new Tag();
                    newTag.setName(trimmedName);
                    newTag.setArticleCount(0L);
                    tagMapper.insert(newTag);
                    result.add(newTag.getId());
                }
            }
        }
        return result;
    }

    /**
     * 保存作品图片到 cms_article_image 表
     */
    private void saveArticleImages(Long articleId, List<String> imageUrls) {
        if (CollectionUtils.isEmpty(imageUrls)) {
            return;
        }
        // 先删除旧的图片记录
        articleImageMapper.delete(new LambdaQueryWrapper<ArticleImage>().eq(ArticleImage::getArticleId, articleId));

        int sortOrder = 0;
        for (String url : imageUrls) {
            if (url == null || url.trim().isEmpty()) {
                continue;
            }
            ArticleImage image = new ArticleImage();
            image.setArticleId(articleId);
            image.setUrl(url.trim());
            image.setType(ImageTypeEnum.CONTENT);
            image.setSortOrder(sortOrder++);
            articleImageMapper.insert(image);
        }
    }

    /**
     * 获取单个作品的图片URL列表
     */
    private List<String> getArticleImageUrls(Long articleId) {
        List<ArticleImage> images = articleImageMapper.selectList(
                new LambdaQueryWrapper<ArticleImage>()
                        .eq(ArticleImage::getArticleId, articleId)
                        .orderByAsc(ArticleImage::getSortOrder));
        if (images.isEmpty()) {
            return Collections.emptyList();
        }
        return images.stream()
                .map(ArticleImage::getUrl)
                .collect(Collectors.toList());
    }

    /**
     * 批量获取作品图片URL列表
     */
    private Map<Long, List<String>> getBatchArticleImageUrls(List<Long> articleIds) {
        Map<Long, List<String>> result = new HashMap<>();
        if (CollectionUtils.isEmpty(articleIds)) {
            return result;
        }
        try {
            List<ArticleImage> allImages = articleImageMapper.selectList(
                    new LambdaQueryWrapper<ArticleImage>()
                            .in(ArticleImage::getArticleId, articleIds)
                            .orderByAsc(ArticleImage::getSortOrder));
            for (ArticleImage img : allImages) {
                result.computeIfAbsent(img.getArticleId(), k -> new ArrayList<>()).add(img.getUrl());
            }
        } catch (Exception e) {
            log.warn("批量查询作品图片失败: {}", e.getMessage());
        }
        return result;
    }

    /**
     * 获取作品的标签列表
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
     * 批量将作品实体列表转换为 VO 列表
     */
    private List<ArticleVO> convertToVOList(List<Article> articles) {
        return convertToVOList(articles, null);
    }

    /**
     * 批量将作品实体列表转换为 VO 列表（含当前用户点赞/收藏状态）
     */
    private List<ArticleVO> convertToVOList(List<Article> articles, Long currentUserId) {
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

        // 按作品ID分组标签
        Map<Long, List<ArticleTag>> articleTagMap = allArticleTags.stream()
                .collect(Collectors.groupingBy(ArticleTag::getArticleId));

        // 批量查询作品图片
        Map<Long, List<String>> articleImageMap = getBatchArticleImageUrls(articleIds);

        // 批量查询 Redis 浏览量（multiGet 替代 N 次单独查询）
        Map<Long, Long> viewCountMap = batchGetViewCounts(articleIds);

        // 批量查询当前用户点赞状态
        final Set<Long> likedArticleIds;
        final Set<Long> collectedArticleIds;
        if (currentUserId != null) {
            Set<Long> liked = new HashSet<>();
            Set<Long> collected = new HashSet<>();
            try {
                List<ArticleLike> likes = articleLikeMapper.selectList(
                        new LambdaQueryWrapper<ArticleLike>()
                                .eq(ArticleLike::getUserId, currentUserId)
                                .eq(ArticleLike::getTargetType, LikeTargetTypeEnum.ARTICLE)
                                .in(ArticleLike::getTargetId, articleIds));
                liked = likes.stream().map(ArticleLike::getTargetId).collect(Collectors.toSet());
            } catch (Exception e) {
                log.warn("批量查询点赞状态失败: {}", e.getMessage());
            }
            try {
                List<Collect> collects = collectMapper.selectList(
                        new LambdaQueryWrapper<Collect>()
                                .eq(Collect::getUserId, currentUserId)
                                .in(Collect::getArticleId, articleIds));
                collected = collects.stream().map(Collect::getArticleId).collect(Collectors.toSet());
            } catch (Exception e) {
                log.warn("批量查询收藏状态失败: {}", e.getMessage());
            }
            likedArticleIds = liked;
            collectedArticleIds = collected;
        } else {
            likedArticleIds = Collections.emptySet();
            collectedArticleIds = Collections.emptySet();
        }

        // 构建 VO 列表
        return articles.stream().map(article -> {
            ArticleVO vo = new ArticleVO();
            vo.setId(article.getId());
            vo.setTitle(article.getTitle());
            vo.setSummary(article.getSummary());
            vo.setContent(article.getContent());
            vo.setCoverImage(article.getCoverImage());
            vo.setStatus(article.getStatus() != null ? article.getStatus().getValue() : null);
            // 浏览数 = 数据库值 + Redis增量
            long viewCount = article.getViewCount() != null ? article.getViewCount() : 0L;
            Long redisIncrement = viewCountMap.get(article.getId());
            if (redisIncrement != null) {
                viewCount += redisIncrement;
            }
            vo.setViewCount(viewCount);
            vo.setLikeCount(article.getLikeCount());
            vo.setCommentCount(article.getCommentCount());
            vo.setCollectCount(article.getCollectCount());
            vo.setIsTop(article.getIsTop());
            vo.setVisibility(article.getVisibility());
            vo.setDeviceInfo(article.getDeviceInfo());
            vo.setLocation(article.getLocation());
            vo.setIpAddress(article.getIpAddress());
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

            // 设置图片列表
            List<String> imageUrls = articleImageMap.get(article.getId());
            vo.setImages(imageUrls != null ? imageUrls : Collections.emptyList());

            // 设置当前用户点赞/收藏状态
            vo.setIsLiked(likedArticleIds.contains(article.getId()));
            vo.setIsCollected(collectedArticleIds.contains(article.getId()));

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
     * 异步增加浏览量（Redis 计数，Lua 脚本保证原子性）
     */
    @Async
    public void incrementViewCountAsync(Long articleId) {
        try {
            String key = VIEW_COUNT_PREFIX + articleId;
            // 使用 Lua 脚本保证原子性：自增 + 检查阈值 + 批量写入DB
            String luaScript =
                "local count = redis.call('INCR', KEYS[1]) " +
                "if count == 1 then " +
                "  redis.call('EXPIRE', KEYS[1], ARGV[2]) " +
                "end " +
                "if count % tonumber(ARGV[1]) == 0 then " +
                "  redis.call('SET', KEYS[1], '0') " +
                "  return count " +
                "end " +
                "return 0";

            Long result = stringRedisTemplate.execute(
                    new org.springframework.data.redis.core.script.DefaultRedisScript<>(luaScript, Long.class),
                    java.util.Collections.singletonList(key),
                    String.valueOf(10),  // 每10次批量写入DB
                    String.valueOf(3600) // 1小时过期
            );

            // 批量写入数据库
            if (result != null && result > 0) {
                articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                        .eq(Article::getId, articleId)
                        .setSql("view_count = view_count + " + result));
            }
        } catch (Exception e) {
            log.error("增加浏览量失败: {}", e.getMessage());
        }
    }

    /**
     * 批量获取作品的 Redis 浏览量增量（使用 multiGet 替代 N 次单独查询）
     */
    private Map<Long, Long> batchGetViewCounts(List<Long> articleIds) {
        Map<Long, Long> result = new HashMap<>();
        if (CollectionUtils.isEmpty(articleIds)) {
            return result;
        }
        try {
            List<String> keys = articleIds.stream()
                    .map(id -> VIEW_COUNT_PREFIX + id)
                    .collect(Collectors.toList());
            List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);
            if (values != null) {
                for (int i = 0; i < articleIds.size(); i++) {
                    String val = values.get(i);
                    if (val != null) {
                        try {
                            result.put(articleIds.get(i), Long.parseLong(val));
                        } catch (NumberFormatException e) {
                            log.warn("解析Redis浏览量失败, articleId={}: {}", articleIds.get(i), val);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("批量获取Redis浏览量失败，使用数据库浏览量: {}", e.getMessage());
        }
        return result;
    }

    /**
     * 构建作品列表缓存 Key（基于查询参数的哈希值）
     */
    private String buildArticleListCacheKey(ArticleQueryRequest request) {
        String raw = String.format("%s:%s:%s:%s:%s:%s",
                request.getCategoryId(),
                request.getTagId(),
                request.getKeyword(),
                request.getSortBy(),
                request.getPageNum(),
                request.getPageSize());
        int hash = raw.hashCode();
        return ARTICLE_LIST_PREFIX + Math.abs(hash);
    }

    /**
     * 从缓存获取作品列表
     */
    private PageResult<ArticleVO> getArticleListFromCache(String cacheKey) {
        try {
            String json = stringRedisTemplate.opsForValue().get(cacheKey);
            if (json != null) {
                ObjectMapper mapper = objectMapper.copy();
                mapper.registerModule(new JavaTimeModule());
                return mapper.readValue(json, mapper.getTypeFactory()
                        .constructParametricType(PageResult.class, ArticleVO.class));
            }
        } catch (Exception e) {
            log.warn("获取作品列表缓存失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 缓存作品列表
     */
    private void cacheArticleList(String cacheKey, PageResult<ArticleVO> pageResult) {
        try {
            ObjectMapper mapper = objectMapper.copy();
            mapper.registerModule(new JavaTimeModule());
            String json = mapper.writeValueAsString(pageResult);
            long ttl = RedisConfig.jitteredTTLFromMinutes(ARTICLE_LIST_CACHE_MINUTES);
            stringRedisTemplate.opsForValue().set(cacheKey, json, ttl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("缓存作品列表失败: {}", e.getMessage());
        }
    }

    /**
     * 从缓存获取作品详情
     */
    private ArticleDetailVO getArticleDetailFromCache(Long articleId) {
        try {
            String json = stringRedisTemplate.opsForValue().get(ARTICLE_DETAIL_PREFIX + articleId);
            if (json != null) {
                ObjectMapper mapper = objectMapper.copy();
                mapper.registerModule(new JavaTimeModule());
                return mapper.readValue(json, ArticleDetailVO.class);
            }
        } catch (Exception e) {
            log.warn("获取作品详情缓存失败, articleId={}: {}", articleId, e.getMessage());
        }
        return null;
    }

    /**
     * 缓存作品详情
     */
    private void cacheArticleDetail(Long articleId, ArticleDetailVO vo) {
        try {
            ObjectMapper mapper = objectMapper.copy();
            mapper.registerModule(new JavaTimeModule());
            String json = mapper.writeValueAsString(vo);
            long ttl = RedisConfig.jitteredTTLFromMinutes(ARTICLE_DETAIL_CACHE_MINUTES);
            stringRedisTemplate.opsForValue().set(ARTICLE_DETAIL_PREFIX + articleId, json, ttl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("缓存作品详情失败, articleId={}: {}", articleId, e.getMessage());
        }
    }

    @Override
    public Map<String, Object> checkArticleDetailConsistency() {
        Map<String, Object> result = new HashMap<>();
        List<Long> inconsistentIds = new ArrayList<>();
        int checkedCount = 0;
        int fixedCount = 0;

        try {
            // 扫描所有作品详情缓存 Key
            Set<String> detailKeys = new java.util.HashSet<>();
            try (org.springframework.data.redis.core.Cursor<String> cursor = stringRedisTemplate.scan(
                    org.springframework.data.redis.core.ScanOptions.scanOptions()
                            .match(ARTICLE_DETAIL_PREFIX + "*")
                            .count(100)
                            .build())) {
                while (cursor.hasNext()) {
                    detailKeys.add(cursor.next());
                }
            }

            checkedCount = detailKeys.size();
            ObjectMapper mapper = objectMapper.copy();
            mapper.registerModule(new JavaTimeModule());

            for (String key : detailKeys) {
                try {
                    String json = stringRedisTemplate.opsForValue().get(key);
                    if (json == null) {
                        continue;
                    }

                    ArticleDetailVO cached = mapper.readValue(json, ArticleDetailVO.class);
                    Long articleId = cached.getId();
                    if (articleId == null) {
                        continue;
                    }

                    // 从数据库获取最新数据
                    Article dbArticle = articleMapper.selectById(articleId);
                    if (dbArticle == null || dbArticle.getDeletedAt() != null) {
                        // 数据库中作品已删除，清除缓存
                        stringRedisTemplate.delete(key);
                        inconsistentIds.add(articleId);
                        fixedCount++;
                        continue;
                    }

                    // 对比关键字段
                    boolean inconsistent = false;
                    if (!Objects.equals(cached.getTitle(), dbArticle.getTitle())) {
                        inconsistent = true;
                    }
                    if (!Objects.equals(cached.getSummary(), dbArticle.getSummary())) {
                        inconsistent = true;
                    }
                    if (dbArticle.getStatus() != null && !Objects.equals(cached.getStatus(), dbArticle.getStatus().getValue())) {
                        inconsistent = true;
                    }
                    if (!Objects.equals(cached.getCategoryId(), dbArticle.getCategoryId())) {
                        inconsistent = true;
                    }
                    // 对比计数类字段（允许浏览量有小幅差异，因为 Redis 增量可能未刷回）
                    if (dbArticle.getLikeCount() != null && cached.getLikeCount() != null
                            && Math.abs(dbArticle.getLikeCount() - cached.getLikeCount()) > 1) {
                        inconsistent = true;
                    }
                    if (dbArticle.getCommentCount() != null && cached.getCommentCount() != null
                            && Math.abs(dbArticle.getCommentCount() - cached.getCommentCount()) > 1) {
                        inconsistent = true;
                    }
                    if (dbArticle.getCollectCount() != null && cached.getCollectCount() != null
                            && Math.abs(dbArticle.getCollectCount() - cached.getCollectCount()) > 1) {
                        inconsistent = true;
                    }

                    if (inconsistent) {
                        // 清除过期缓存，下次请求时重新从数据库加载
                        stringRedisTemplate.delete(key);
                        inconsistentIds.add(articleId);
                        fixedCount++;
                        log.info("缓存不一致：作品 {} 缓存已过期，已清除", articleId);
                    }
                } catch (Exception e) {
                    log.warn("检查缓存 Key {} 一致性失败: {}", key, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("扫描作品详情缓存失败: {}", e.getMessage());
        }

        result.put("checkedCount", checkedCount);
        result.put("inconsistentCount", inconsistentIds.size());
        result.put("fixedCount", fixedCount);
        result.put("inconsistentIds", inconsistentIds);
        return result;
    }

    /**
     * 通过 IP 解析地理位置（如"北京""上海"），失败时返回空字符串
     */
    private String resolveIpLocation(String ip) {
        if (ip == null || ip.isEmpty() || "127.0.0.1".equals(ip)
                || "0:0:0:0:0:0:0:1".equals(ip) || ip.startsWith("192.168.")
                || ip.startsWith("10.") || ip.startsWith("172.")) {
            return "";
        }
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(3))
                    .build();
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://ip-api.com/json/" + ip + "?lang=zh-CN&fields=city,regionName"))
                    .timeout(java.time.Duration.ofSeconds(3))
                    .GET()
                    .build();
            java.net.http.HttpResponse<String> resp = client.send(req,
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(resp.body());
                String city = node.has("city") ? node.get("city").asText() : "";
                String region = node.has("regionName") ? node.get("regionName").asText() : "";
                if (!city.isEmpty()) {
                    return city;
                }
                if (!region.isEmpty()) {
                    return region;
                }
            }
        } catch (Exception e) {
            log.warn("IP地理位置解析失败, ip={}: {}", ip, e.getMessage());
        }
        return "";
    }
}
