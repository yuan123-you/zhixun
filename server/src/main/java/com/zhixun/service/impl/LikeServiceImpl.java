package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.enums.ArticleStatusEnum;
import com.zhixun.common.result.ErrorCode;
import com.zhixun.common.result.PageResult;
import com.zhixun.config.Slave;
import com.zhixun.entity.Article;
import com.zhixun.entity.ArticleLike;
import com.zhixun.entity.Comment;
import com.zhixun.enums.LikeTargetTypeEnum;
import com.zhixun.mapper.ArticleLikeMapper;
import com.zhixun.mapper.ArticleMapper;
import com.zhixun.mapper.CommentMapper;
import com.zhixun.service.LikeService;
import com.zhixun.vo.ArticleVO;
import com.zhixun.vo.TagVO;
import com.zhixun.config.RedisConfig;
import com.zhixun.entity.ArticleTag;
import com.zhixun.entity.Tag;
import com.zhixun.entity.User;
import com.zhixun.entity.Category;
import com.zhixun.mapper.ArticleTagMapper;
import com.zhixun.mapper.TagMapper;
import com.zhixun.mapper.UserMapper;
import com.zhixun.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 点赞服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final ArticleLikeMapper articleLikeMapper;
    private final ArticleMapper articleMapper;
    private final SecurityUtil securityUtil;
    private final CommentMapper commentMapper;
    private final ArticleTagMapper articleTagMapper;
    private final TagMapper tagMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /** 点赞状态 Redis Key 前缀 */
    private static final String LIKE_STATUS_PREFIX = "like:status:";
    /** 点赞计数 Redis Key 前缀 */
    private static final String LIKE_COUNT_PREFIX = "like:count:";
    /** 浏览量 Redis Key 前缀 */
    private static final String VIEW_COUNT_PREFIX = "article:view:";
    /** 作品详情缓存 Key 前缀 */
    private static final String ARTICLE_DETAIL_PREFIX = "article:detail:";
    /** 作品列表缓存 Key 前缀 */
    private static final String ARTICLE_LIST_PREFIX = "article:list:";
    /** 相关推荐缓存 Key 前缀 */
    private static final String RELATED_ARTICLES_PREFIX = "article:related:";
    /** 排行榜缓存 Key 前缀 */
    private static final String RANK_KEY_PREFIX = "rank:hot:";
    /** 热门动态缓存 Key 前缀 */
    private static final String HOT_FEED_KEY_PREFIX = "feed:hot:";
    /** 推荐缓存 Key 前缀 */
    private static final String RECOMMEND_KEY_PREFIX = "feed:recommend:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleLike(Long userId, Long targetId, Integer targetType) {
        validateIds(userId, targetId);
        LikeTargetTypeEnum targetTypeEnum = parseTargetType(targetType);
        lockAndValidateTarget(userId, targetId, targetTypeEnum);

        ArticleLike existingLike = articleLikeMapper.selectInteractionForUpdate(userId, targetId, targetType);
        boolean liked = existingLike == null;
        if (liked) {
            ArticleLike like = new ArticleLike();
            like.setUserId(userId);
            like.setTargetId(targetId);
            like.setTargetType(targetTypeEnum);
            requireWrite(articleLikeMapper.insert(like));
        } else {
            requireWrite(articleLikeMapper.deleteById(existingLike.getId()));
        }

        // Target lock serializes toggles; the current fact read also repairs historical count drift.
        long likeCount = articleLikeMapper.countTargetFactsForUpdate(targetId, targetType);
        requireWrite(targetTypeEnum == LikeTargetTypeEnum.ARTICLE
                ? articleMapper.saveLikeCount(targetId, likeCount)
                : commentMapper.saveLikeCount(targetId, likeCount));

        afterCommit(() -> {
            invalidateKey(LIKE_STATUS_PREFIX + userId + ":" + targetId + ":" + targetType);
            invalidateKey(LIKE_COUNT_PREFIX + targetId + ":" + targetType);
            if (targetTypeEnum == LikeTargetTypeEnum.ARTICLE) clearArticleCache(targetId);
        });

        Map<String, Object> result = new HashMap<>();
        result.put("isLiked", liked);
        result.put("likeCount", likeCount);
        return result;
    }

    @Override
    @Slave
    public boolean isLiked(Long userId, Long targetId, Integer targetType) {
        validateIds(userId, targetId);
        LikeTargetTypeEnum targetTypeEnum = parseTargetType(targetType);
        // 先查 Redis 缓存
        String statusKey = LIKE_STATUS_PREFIX + userId + ":" + targetId + ":" + targetType;
        String cached = stringRedisTemplate.opsForValue().get(statusKey);
        if (cached != null) {
            return "1".equals(cached);
        }

        // 缓存未命中或 Redis 不可用，查数据库
        LambdaQueryWrapper<ArticleLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleLike::getUserId, userId)
                .eq(ArticleLike::getTargetId, targetId)
                .eq(ArticleLike::getTargetType, targetTypeEnum);
        boolean liked = articleLikeMapper.selectCount(wrapper) > 0;

        // 写入缓存
        String cacheKey = LIKE_STATUS_PREFIX + userId + ":" + targetId + ":" + targetType;
        stringRedisTemplate.opsForValue().set(cacheKey, liked ? "1" : "0", 1, TimeUnit.HOURS);
        return liked;
    }

    @Override
    @Slave
    public PageResult<ArticleVO> getUserLikes(Long userId, Integer page, Integer pageSize) {
        // 查询用户点赞的作品类型记录
        LambdaQueryWrapper<ArticleLike> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.eq(ArticleLike::getUserId, userId)
                .eq(ArticleLike::getTargetType, LikeTargetTypeEnum.ARTICLE)
                .orderByDesc(ArticleLike::getCreatedAt);
        Page<ArticleLike> likePage = new Page<>(page, pageSize);
        Page<ArticleLike> likeResult = articleLikeMapper.selectPage(likePage, likeWrapper);

        if (likeResult.getRecords().isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L, page, pageSize);
        }

        // 获取点赞的作品ID列表
        List<Long> articleIds = likeResult.getRecords().stream()
                .map(ArticleLike::getTargetId)
                .collect(Collectors.toList());

        // 批量查询作品
        List<Article> articles = articleMapper.selectBatchIds(articleIds);
        List<ArticleVO> voList = convertToVOList(articles);

        return new PageResult<>(voList, likeResult.getTotal(), page, pageSize);
    }

    // ========== 内部方法 ==========

    private LikeTargetTypeEnum parseTargetType(Integer value) {
        for (LikeTargetTypeEnum type : LikeTargetTypeEnum.values()) {
            if (type.getValue().equals(value)) return type;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "点赞目标类型错误");
    }

    private void lockAndValidateTarget(Long userId, Long targetId, LikeTargetTypeEnum type) {
        if (type == LikeTargetTypeEnum.ARTICLE) {
            validateArticleAccess(articleMapper.selectForUpdate(targetId), userId);
            return;
        }
        // Always lock parent article before comment to keep a consistent lock order.
        Comment candidate = commentMapper.selectById(targetId);
        if (candidate == null || candidate.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评论不存在");
        }
        validateArticleAccess(articleMapper.selectForUpdate(candidate.getArticleId()), userId);
        Comment locked = commentMapper.selectForUpdate(targetId);
        if (locked == null) throw new BusinessException(ErrorCode.NOT_FOUND, "评论不存在");
        if (!java.util.Objects.equals(candidate.getArticleId(), locked.getArticleId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "评论所属作品已变更，请重试");
        }
    }


    /**
     * 批量将作品实体列表转换为 VO 列表
     */
    private List<ArticleVO> convertToVOList(List<Article> articles) {
        if (CollectionUtils.isEmpty(articles)) {
            return Collections.emptyList();
        }

        Set<Long> userIds = articles.stream().map(Article::getAuthorId).collect(Collectors.toSet());
        Set<Long> categoryIds = articles.stream()
                .map(Article::getCategoryId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        List<Long> articleIds = articles.stream().map(Article::getId).collect(Collectors.toList());

        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, Category> categoryMap = categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        List<ArticleTag> allArticleTags = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>().in(ArticleTag::getArticleId, articleIds));
        Set<Long> tagIds = allArticleTags.stream().map(ArticleTag::getTagId).collect(Collectors.toSet());
        Map<Long, Tag> tagMap = CollectionUtils.isEmpty(tagIds) ? Collections.emptyMap()
                : tagMapper.selectBatchIds(tagIds).stream().collect(Collectors.toMap(Tag::getId, t -> t));
        Map<Long, List<ArticleTag>> articleTagMap = allArticleTags.stream()
                .collect(Collectors.groupingBy(ArticleTag::getArticleId));

        return articles.stream().map(article -> {
            ArticleVO vo = new ArticleVO();
            vo.setId(article.getId());
            vo.setTitle(article.getTitle());
            vo.setSummary(article.getSummary());
            vo.setCoverImage(article.getCoverImage());
            vo.setStatus(article.getStatus() != null ? article.getStatus().getValue() : null);
            // 浏览数 = 数据库值 + Redis增量
            long viewCount = article.getViewCount() != null ? article.getViewCount() : 0L;
            try {
                String viewCountStr = stringRedisTemplate.opsForValue().get(VIEW_COUNT_PREFIX + article.getId());
                if (viewCountStr != null) {
                    viewCount += Long.parseLong(viewCountStr);
                }
            } catch (Exception e) {
                log.warn("获取Redis浏览量失败，使用数据库浏览量: {}", e.getMessage());
            }
            vo.setViewCount(viewCount);
            vo.setLikeCount(article.getLikeCount());
            vo.setCommentCount(article.getCommentCount());
            vo.setCollectCount(article.getCollectCount());
            vo.setIsTop(article.getIsTop());
            vo.setCreatedAt(article.getCreatedAt());

            User author = userMap.get(article.getAuthorId());
            if (author != null) {
                vo.setAuthorName(author.getNickname());
                vo.setAuthorAvatar(author.getAvatar());
            }
            if (article.getCategoryId() != null) {
                Category category = categoryMap.get(article.getCategoryId());
                if (category != null) {
                    vo.setCategoryName(category.getName());
                }
            }
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
     * 清除作品相关缓存
     */
    private void validateIds(Long userId, Long targetId) {
        if (userId == null || userId <= 0 || targetId == null || targetId <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户或目标ID错误");
        }
    }

    private void requireWrite(int affected) {
        if (affected != 1) throw new BusinessException(ErrorCode.CONFLICT, "互动记录已变更，请重试");
    }

    /** Mirrors existing article-detail access rules; publication/review states are never changed. */
    private void validateArticleAccess(Article article, Long userId) {
        if (article == null || article.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "作品不存在");
        }
        boolean ownerOrAdmin = userId.equals(article.getAuthorId()) || securityUtil.isAdmin();
        if (article.getStatus() != ArticleStatusEnum.PUBLISHED && !ownerOrAdmin) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "作品不存在");
        }
        Integer visibility = article.getVisibility();
        if (visibility == null || visibility == Article.VISIBILITY_PUBLIC || ownerOrAdmin) return;
        if ((visibility == Article.VISIBILITY_FOLLOWERS || visibility == Article.VISIBILITY_MUTUAL)
                && articleMapper.countInteractionFollow(userId, article.getAuthorId()) > 0
                && (visibility != Article.VISIBILITY_MUTUAL
                    || articleMapper.countInteractionFollow(article.getAuthorId(), userId) > 0)) return;
        throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看此作品");
    }

    private void afterCommit(Runnable effects) {
        // Never perform an external effect unless an actual encompassing transaction commits.
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("互动写入必须在事务中执行");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { effects.run(); }
        });
    }

    private void cacheEffect(String name, Runnable effect) {
        if (stringRedisTemplate == null) return;
        try { effect.run(); } catch (Exception e) { log.warn("互动缓存操作失败, {}: {}", name, e.getMessage()); }
    }

    private void invalidateKey(String key) {
        cacheEffect(key, () -> stringRedisTemplate.delete(key));
    }

    private void clearArticleCache(Long articleId) {
        invalidateKey(ARTICLE_DETAIL_PREFIX + articleId);
        invalidateKey(RELATED_ARTICLES_PREFIX + articleId);
        for (String prefix : List.of(ARTICLE_LIST_PREFIX, RANK_KEY_PREFIX, HOT_FEED_KEY_PREFIX, RECOMMEND_KEY_PREFIX)) {
            cacheEffect(prefix, () -> {
                Set<String> keys = new java.util.HashSet<>();
                try (org.springframework.data.redis.core.Cursor<String> cursor = stringRedisTemplate.scan(
                        org.springframework.data.redis.core.ScanOptions.scanOptions().match(prefix + "*").count(100).build())) {
                    while (cursor.hasNext()) keys.add(cursor.next());
                }
                if (!keys.isEmpty()) stringRedisTemplate.delete(keys);
            });
        }
        cacheEffect("data-version", () -> RedisConfig.incrementDataVersion(stringRedisTemplate));
    }
}
