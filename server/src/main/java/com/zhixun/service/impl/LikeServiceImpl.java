package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.common.exception.BusinessException;
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
        LikeTargetTypeEnum targetTypeEnum = LikeTargetTypeEnum.values()[targetType - 1];

        // 检查目标是否存在
        validateTarget(targetId, targetTypeEnum);

        // 查询是否已点赞
        LambdaQueryWrapper<ArticleLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleLike::getUserId, userId)
                .eq(ArticleLike::getTargetId, targetId)
                .eq(ArticleLike::getTargetType, targetTypeEnum);
        ArticleLike existingLike = articleLikeMapper.selectOne(wrapper);

        boolean liked;
        if (existingLike != null) {
            // 已点赞，取消点赞
            articleLikeMapper.deleteById(existingLike.getId());
            // 减少计数（SQL 原子操作）
            decrementLikeCount(targetId, targetTypeEnum);
            liked = false;
        } else {
            // 未点赞，创建点赞记录
            ArticleLike like = new ArticleLike();
            like.setUserId(userId);
            like.setTargetId(targetId);
            like.setTargetType(targetTypeEnum);
            articleLikeMapper.insert(like);
            // 增加计数（SQL 原子操作）
            incrementLikeCount(targetId, targetTypeEnum);
            liked = true;
        }

        // 获取当前点赞数
        Long likeCount = getLikeCountFromDB(targetId, targetTypeEnum);

        // 更新 Redis 缓存
        String statusKey = LIKE_STATUS_PREFIX + userId + ":" + targetId + ":" + targetType;
        stringRedisTemplate.opsForValue().set(statusKey, liked ? "1" : "0", 1, TimeUnit.HOURS);
        String countKey = LIKE_COUNT_PREFIX + targetId + ":" + targetType;
        stringRedisTemplate.opsForValue().set(countKey, String.valueOf(likeCount), 1, TimeUnit.HOURS);

        // 清除作品相关缓存，确保列表和详情页数据一致
        if (targetTypeEnum == LikeTargetTypeEnum.ARTICLE) {
            clearArticleCache(targetId);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("isLiked", liked);
        result.put("likeCount", likeCount);
        return result;
    }

    @Override
    @Slave
    public boolean isLiked(Long userId, Long targetId, Integer targetType) {
        // 先查 Redis 缓存
        String statusKey = LIKE_STATUS_PREFIX + userId + ":" + targetId + ":" + targetType;
        String cached = stringRedisTemplate.opsForValue().get(statusKey);
        if (cached != null) {
            return "1".equals(cached);
        }

        // 缓存未命中或 Redis 不可用，查数据库
        LikeTargetTypeEnum targetTypeEnum = LikeTargetTypeEnum.values()[targetType - 1];
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

    /**
     * 校验点赞目标是否存在
     */
    private void validateTarget(Long targetId, LikeTargetTypeEnum targetType) {
        if (targetType == LikeTargetTypeEnum.ARTICLE) {
            Article article = articleMapper.selectById(targetId);
            if (article == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "作品不存在");
            }
        } else if (targetType == LikeTargetTypeEnum.COMMENT) {
            Comment comment = commentMapper.selectById(targetId);
            if (comment == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "评论不存在");
            }
        }
    }

    /**
     * 增加点赞计数（SQL 原子操作）
     */
    private void incrementLikeCount(Long targetId, LikeTargetTypeEnum targetType) {
        if (targetType == LikeTargetTypeEnum.ARTICLE) {
            articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                    .eq(Article::getId, targetId)
                    .setSql("like_count = like_count + 1"));
        } else if (targetType == LikeTargetTypeEnum.COMMENT) {
            commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                    .eq(Comment::getId, targetId)
                    .setSql("like_count = like_count + 1"));
        }
    }

    /**
     * 减少点赞计数（SQL 原子操作）
     */
    private void decrementLikeCount(Long targetId, LikeTargetTypeEnum targetType) {
        if (targetType == LikeTargetTypeEnum.ARTICLE) {
            articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                    .eq(Article::getId, targetId)
                    .gt(Article::getLikeCount, 0)
                    .setSql("like_count = like_count - 1"));
        } else if (targetType == LikeTargetTypeEnum.COMMENT) {
            commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                    .eq(Comment::getId, targetId)
                    .gt(Comment::getLikeCount, 0)
                    .setSql("like_count = like_count - 1"));
        }
    }

    /**
     * 从数据库获取点赞数
     */
    private Long getLikeCountFromDB(Long targetId, LikeTargetTypeEnum targetType) {
        if (targetType == LikeTargetTypeEnum.ARTICLE) {
            Article article = articleMapper.selectById(targetId);
            return article != null ? article.getLikeCount() : 0L;
        } else if (targetType == LikeTargetTypeEnum.COMMENT) {
            Comment comment = commentMapper.selectById(targetId);
            return comment != null ? comment.getLikeCount().longValue() : 0L;
        }
        return 0L;
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
    private void clearArticleCache(Long articleId) {
        try {
            stringRedisTemplate.delete(ARTICLE_DETAIL_PREFIX + articleId);
            stringRedisTemplate.delete(RELATED_ARTICLES_PREFIX + articleId);
            // 清除作品列表缓存（使用 SCAN 替代 KEYS）
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
            // 清除排行榜缓存
            Set<String> rankKeys = new java.util.HashSet<>();
            try (org.springframework.data.redis.core.Cursor<String> cursor = stringRedisTemplate.scan(
                    org.springframework.data.redis.core.ScanOptions.scanOptions()
                            .match(RANK_KEY_PREFIX + "*")
                            .count(100)
                            .build())) {
                while (cursor.hasNext()) {
                    rankKeys.add(cursor.next());
                }
            }
            if (!rankKeys.isEmpty()) {
                stringRedisTemplate.delete(rankKeys);
            }
            // 清除热门动态缓存
            Set<String> hotKeys = new java.util.HashSet<>();
            try (org.springframework.data.redis.core.Cursor<String> cursor = stringRedisTemplate.scan(
                    org.springframework.data.redis.core.ScanOptions.scanOptions()
                            .match(HOT_FEED_KEY_PREFIX + "*")
                            .count(100)
                            .build())) {
                while (cursor.hasNext()) {
                    hotKeys.add(cursor.next());
                }
            }
            if (!hotKeys.isEmpty()) {
                stringRedisTemplate.delete(hotKeys);
            }
            // 清除推荐缓存
            Set<String> recommendKeys = new java.util.HashSet<>();
            try (org.springframework.data.redis.core.Cursor<String> cursor = stringRedisTemplate.scan(
                    org.springframework.data.redis.core.ScanOptions.scanOptions()
                            .match(RECOMMEND_KEY_PREFIX + "*")
                            .count(100)
                            .build())) {
                while (cursor.hasNext()) {
                    recommendKeys.add(cursor.next());
                }
            }
            if (!recommendKeys.isEmpty()) {
                stringRedisTemplate.delete(recommendKeys);
            }
        } catch (Exception e) {
            log.warn("清除作品相关缓存失败, articleId={}: {}", articleId, e.getMessage());
        }
        // 递增数据版本号，通知客户端数据已变更
        try {
            RedisConfig.incrementDataVersion(stringRedisTemplate);
        } catch (Exception e) {
            log.debug("递增数据版本号失败: {}", e.getMessage());
        }
    }
}
