package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.entity.Article;
import com.zhixun.entity.ArticleTag;
import com.zhixun.entity.Category;
import com.zhixun.entity.Tag;
import com.zhixun.entity.User;
import com.zhixun.enums.ArticleStatusEnum;
import com.zhixun.mapper.ArticleMapper;
import com.zhixun.mapper.ArticleTagMapper;
import com.zhixun.mapper.CategoryMapper;
import com.zhixun.mapper.TagMapper;
import com.zhixun.mapper.UserMapper;
import com.zhixun.service.RankService;
import com.zhixun.vo.HotArticleVO;
import com.zhixun.vo.TagVO;
import com.zhixun.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 排行服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankServiceImpl implements RankService {

    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /** 排行榜缓存 Key 前缀 */
    private static final String RANK_KEY_PREFIX = "rank:hot:";
    /** 排行榜缓存 TTL（5分钟） */
    private static final long RANK_TTL_MINUTES = 5;
    /** 实时热榜 Redis Sorted Set Key（与 HotScoreServiceImpl 保持一致） */
    private static final String HOT_RANK_KEY = "hot:rank";
    /** 浏览量 Redis Key 前缀 */
    private static final String VIEW_COUNT_PREFIX = "article:view:";

    @Override
    public List<HotArticleVO> getHotRank(String period, Long categoryId, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 20;
        }

        // 构建缓存 Key
        String cacheKey = RANK_KEY_PREFIX + period + ":" + (categoryId != null ? categoryId : "all");

        // 尝试从缓存获取文章ID列表
        try {
            if (stringRedisTemplate != null) {
                String cachedIds = stringRedisTemplate.opsForValue().get(cacheKey);
                if (cachedIds != null && !cachedIds.isEmpty()) {
                    List<Long> articleIds = parseIds(cachedIds);
                    return buildHotArticleVOList(articleIds, limit);
                }
            }
        } catch (Exception e) {
            log.warn("从Redis获取排行榜缓存失败，将从数据库查询: {}", e.getMessage());
        }

        // 缓存未命中，查询数据库
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, ArticleStatusEnum.PUBLISHED);

        // 按时间周期筛选
        LocalDateTime since = getSinceTime(period);
        if (since != null) {
            wrapper.ge(Article::getCreatedAt, since);
        }

        // 按分类筛选
        if (categoryId != null) {
            wrapper.eq(Article::getCategoryId, categoryId);
        }

        // 按热度排序（浏览量*1 + 点赞*5 + 评论*3 + 收藏*8 的近似排序）
        wrapper.orderByDesc(Article::getViewCount)
                .orderByDesc(Article::getLikeCount)
                .orderByDesc(Article::getCommentCount)
                .orderByDesc(Article::getCollectCount)
                .last("LIMIT " + limit * 2);

        List<Article> articles = articleMapper.selectList(wrapper);

        // 计算热度分并排序
        List<Article> sortedArticles = articles.stream()
                .sorted((a, b) -> {
                    double scoreA = calculateSimpleHotScore(a);
                    double scoreB = calculateSimpleHotScore(b);
                    return Double.compare(scoreB, scoreA);
                })
                .limit(limit)
                .collect(Collectors.toList());

        List<Long> articleIds = sortedArticles.stream()
                .map(Article::getId)
                .collect(Collectors.toList());

        // 缓存结果
        if (!articleIds.isEmpty()) {
            try {
                String idsStr = articleIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
                stringRedisTemplate.opsForValue().set(cacheKey, idsStr, RANK_TTL_MINUTES, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.warn("缓存排行榜数据到Redis失败: {}", e.getMessage());
            }
        }

        return buildHotArticleVOList(articleIds, limit);
    }

    @Override
    public List<HotArticleVO> getHotArticles(Long categoryId, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }

        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, ArticleStatusEnum.PUBLISHED);

        if (categoryId != null) {
            wrapper.eq(Article::getCategoryId, categoryId);
        }

        wrapper.orderByDesc(Article::getViewCount)
                .orderByDesc(Article::getLikeCount)
                .orderByDesc(Article::getCreatedAt)
                .last("LIMIT " + limit);

        List<Article> articles = articleMapper.selectList(wrapper);
        return convertToHotArticleVOList(articles);
    }

    @Override
    public List<HotArticleVO> getRelatedArticles(Long articleId, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 6;
        }

        // 获取当前文章
        Article currentArticle = articleMapper.selectById(articleId);
        if (currentArticle == null) {
            return Collections.emptyList();
        }

        // 获取当前文章的标签ID列表
        List<ArticleTag> articleTags = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, articleId));
        List<Long> tagIds = articleTags.stream().map(ArticleTag::getTagId).collect(Collectors.toList());

        // 查询同标签的文章
        List<Long> relatedArticleIds = new ArrayList<>();
        if (!CollectionUtils.isEmpty(tagIds)) {
            List<ArticleTag> relatedTags = articleTagMapper.selectList(
                    new LambdaQueryWrapper<ArticleTag>()
                            .in(ArticleTag::getTagId, tagIds)
                            .ne(ArticleTag::getArticleId, articleId));
            relatedArticleIds = relatedTags.stream()
                    .map(ArticleTag::getArticleId)
                    .distinct()
                    .collect(Collectors.toList());
        }

        // 查询同分类的文章
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                .ne(Article::getId, articleId);

        if (currentArticle.getCategoryId() != null) {
            wrapper.eq(Article::getCategoryId, currentArticle.getCategoryId());
        }

        // 优先同标签文章
        if (!relatedArticleIds.isEmpty()) {
            // 先查同标签的
            LambdaQueryWrapper<Article> tagWrapper = new LambdaQueryWrapper<>();
            tagWrapper.eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                    .in(Article::getId, relatedArticleIds)
                    .orderByDesc(Article::getViewCount)
                    .last("LIMIT " + limit);
            List<Article> tagArticles = articleMapper.selectList(tagWrapper);

            if (tagArticles.size() < limit) {
                // 不足则补充同分类文章
                List<Long> existIds = tagArticles.stream().map(Article::getId).collect(Collectors.toList());
                existIds.add(articleId);
                wrapper.notIn(Article::getId, existIds)
                        .orderByDesc(Article::getViewCount)
                        .last("LIMIT " + (limit - tagArticles.size()));
                List<Article> categoryArticles = articleMapper.selectList(wrapper);
                tagArticles.addAll(categoryArticles);
            }

            return convertToHotArticleVOList(tagArticles.stream().limit(limit).collect(Collectors.toList()));
        }

        // 没有同标签文章，返回同分类文章
        wrapper.orderByDesc(Article::getViewCount).last("LIMIT " + limit);
        List<Article> articles = articleMapper.selectList(wrapper);
        return convertToHotArticleVOList(articles);
    }

    @Override
    public List<TagVO> getHotTags(int limit) {
        if (limit <= 0) {
            limit = 10;
        }

        // 查询最近7天已发布的文章
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<Article> recentArticles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                        .ge(Article::getCreatedAt, since)
                        .select(Article::getId));

        if (CollectionUtils.isEmpty(recentArticles)) {
            return Collections.emptyList();
        }

        List<Long> articleIds = recentArticles.stream()
                .map(Article::getId)
                .collect(Collectors.toList());

        // 查询这些文章的标签关联
        List<ArticleTag> articleTags = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>().in(ArticleTag::getArticleId, articleIds));

        if (CollectionUtils.isEmpty(articleTags)) {
            return Collections.emptyList();
        }

        // 按标签分组统计文章数
        Map<Long, Long> tagCount = articleTags.stream()
                .collect(Collectors.groupingBy(ArticleTag::getTagId, Collectors.counting()));

        // 按文章数降序排序，取前 limit 个
        List<Long> hotTagIds = tagCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (hotTagIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 查询标签详情
        List<Tag> tags = tagMapper.selectBatchIds(hotTagIds);
        Map<Long, Tag> tagMap = tags.stream()
                .collect(Collectors.toMap(Tag::getId, t -> t));

        // 按热度排序构建 VO
        return hotTagIds.stream()
                .map(tagId -> {
                    Tag tag = tagMap.get(tagId);
                    if (tag == null) {
                        return null;
                    }
                    TagVO vo = new TagVO();
                    vo.setId(tag.getId());
                    vo.setName(tag.getName());
                    vo.setArticleCount(tagCount.get(tagId));
                    vo.setCreatedAt(tag.getCreatedAt());
                    return vo;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserVO> getHotUsers(int limit) {
        if (limit <= 0) {
            limit = 10;
        }

        // 查询最近7天已发布的文章
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<Article> recentArticles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                        .ge(Article::getCreatedAt, since)
                        .select(Article::getId, Article::getAuthorId,
                                Article::getViewCount, Article::getLikeCount));

        if (CollectionUtils.isEmpty(recentArticles)) {
            return Collections.emptyList();
        }

        // 按作者分组，统计浏览量总和
        Map<Long, Long> userViewCount = new HashMap<>();
        for (Article article : recentArticles) {
            if (article.getAuthorId() != null) {
                long views = article.getViewCount() != null ? article.getViewCount() : 0L;
                userViewCount.merge(article.getAuthorId(), views, Long::sum);
            }
        }

        // 按浏览量降序排序，取前 limit 个
        List<Long> hotUserIds = userViewCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (hotUserIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 查询用户详情
        List<User> users = userMapper.selectBatchIds(hotUserIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 按热度排序构建 VO
        return hotUserIds.stream()
                .map(userId -> {
                    User user = userMap.get(userId);
                    if (user == null) {
                        return null;
                    }
                    UserVO vo = new UserVO();
                    vo.setId(user.getId());
                    vo.setUsername(user.getUsername());
                    vo.setNickname(user.getNickname());
                    vo.setAvatar(user.getAvatar());
                    vo.setRole(user.getRole() != null ? user.getRole().name() : null);
                    vo.setStatus(user.getStatus());
                    vo.setCreatedAt(user.getCreatedAt());
                    vo.setFollowCount(user.getFollowCount());
                    vo.setFollowerCount(user.getFollowerCount());
                    vo.setArticleCount(user.getArticleCount());
                    return vo;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<HotArticleVO> getRealtimeHot(int limit) {
        if (limit <= 0) {
            limit = 20;
        }

        // 从 Redis Sorted Set 获取热门文章（按分数降序，滑动窗口由热度分时间衰减实现）
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(HOT_RANK_KEY, 0, limit - 1);

        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }

        // 提取文章ID和分数
        List<Long> articleIds = new ArrayList<>();
        Map<Long, Double> scoreMap = new HashMap<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple.getValue() != null) {
                try {
                    Long articleId = Long.parseLong(tuple.getValue());
                    articleIds.add(articleId);
                    scoreMap.put(articleId, tuple.getScore());
                } catch (NumberFormatException e) {
                    log.warn("解析实时热榜文章ID失败: {}", tuple.getValue());
                }
            }
        }

        if (articleIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 查询文章详情并保持 Sorted Set 顺序
        List<Article> articles = articleMapper.selectBatchIds(articleIds);
        Map<Long, Article> articleMap = articles.stream()
                .collect(Collectors.toMap(Article::getId, a -> a));

        List<Article> orderedArticles = articleIds.stream()
                .map(articleMap::get)
                .filter(Objects::nonNull)
                .filter(a -> a.getStatus() == ArticleStatusEnum.PUBLISHED)
                .collect(Collectors.toList());

        // 转换为 VO（批量查询作者信息）
        List<HotArticleVO> voList = convertToHotArticleVOList(orderedArticles);

        // 使用 Redis 中的实时分数覆盖
        for (HotArticleVO vo : voList) {
            vo.setScore(scoreMap.getOrDefault(vo.getId(), vo.getScore()));
        }

        return voList;
    }

    // ========== 内部方法 ==========

    /**
     * 根据周期获取起始时间
     */
    private LocalDateTime getSinceTime(String period) {
        LocalDateTime now = LocalDateTime.now();
        if ("daily".equals(period)) {
            return now.minusDays(1);
        } else if ("weekly".equals(period)) {
            return now.minusWeeks(1);
        } else if ("monthly".equals(period)) {
            return now.minusMonths(1);
        }
        return null;
    }

    /**
     * 简单热度分计算（用于排序）
     */
    private double calculateSimpleHotScore(Article article) {
        long viewCount = article.getViewCount() != null ? article.getViewCount() : 0L;
        long likeCount = article.getLikeCount() != null ? article.getLikeCount() : 0L;
        long commentCount = article.getCommentCount() != null ? article.getCommentCount() : 0L;
        long collectCount = article.getCollectCount() != null ? article.getCollectCount() : 0L;

        double interactionScore = viewCount * 1.0 + likeCount * 5.0 + commentCount * 3.0 + collectCount * 8.0;

        LocalDateTime publishTime = article.getPublishAt() != null ? article.getPublishAt() : article.getCreatedAt();
        if (publishTime != null) {
            long hours = Math.max(1, java.time.Duration.between(publishTime, LocalDateTime.now()).toHours());
            return interactionScore / Math.pow(hours, 1.5);
        }
        return interactionScore;
    }

    /**
     * 解析缓存的ID字符串
     */
    private List<Long> parseIds(String idsStr) {
        List<Long> ids = new ArrayList<>();
        for (String id : idsStr.split(",")) {
            try {
                ids.add(Long.parseLong(id.trim()));
            } catch (NumberFormatException e) {
                log.warn("解析文章ID失败: {}", id);
            }
        }
        return ids;
    }

    /**
     * 根据文章ID列表构建 HotArticleVO 列表
     */
    private List<HotArticleVO> buildHotArticleVOList(List<Long> articleIds, Integer limit) {
        if (CollectionUtils.isEmpty(articleIds)) {
            return Collections.emptyList();
        }

        List<Long> pageIds = articleIds.stream().limit(limit).collect(Collectors.toList());
        List<Article> articles = articleMapper.selectBatchIds(pageIds);

        // 保持顺序
        Map<Long, Article> articleMap = articles.stream()
                .collect(Collectors.toMap(Article::getId, a -> a));

        return pageIds.stream()
                .map(articleMap::get)
                .filter(a -> a != null)
                .map(this::convertToHotArticleVO)
                .collect(Collectors.toList());
    }

    /**
     * 批量将文章列表转换为 HotArticleVO 列表
     */
    private List<HotArticleVO> convertToHotArticleVOList(List<Article> articles) {
        if (CollectionUtils.isEmpty(articles)) {
            return Collections.emptyList();
        }

        // 批量查询作者信息
        Set<Long> userIds = articles.stream().map(Article::getAuthorId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return articles.stream().map(article -> {
            HotArticleVO vo = convertToHotArticleVO(article);
            User author = userMap.get(article.getAuthorId());
            if (author != null) {
                vo.setAuthorNickname(author.getNickname());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 将文章实体转换为 HotArticleVO
     */
    private HotArticleVO convertToHotArticleVO(Article article) {
        HotArticleVO vo = new HotArticleVO();
        vo.setId(article.getId());
        vo.setTitle(article.getTitle());
        // 浏览数 = 数据库值 + Redis增量
        long viewCount = article.getViewCount() != null ? article.getViewCount() : 0L;
        try {
            if (stringRedisTemplate != null) {
                String viewCountStr = stringRedisTemplate.opsForValue().get(VIEW_COUNT_PREFIX + article.getId());
                if (viewCountStr != null) {
                    viewCount += Long.parseLong(viewCountStr);
                }
            }
        } catch (Exception e) {
            log.warn("获取Redis浏览量失败，使用数据库浏览量: {}", e.getMessage());
        }
        vo.setViewCount((int) viewCount);
        vo.setLikeCount(article.getLikeCount() != null ? article.getLikeCount().intValue() : 0);
        vo.setCommentCount(article.getCommentCount() != null ? article.getCommentCount().intValue() : 0);
        vo.setScore(calculateSimpleHotScore(article));
        vo.setCreatedAt(article.getCreatedAt());

        // 查询作者昵称
        User author = userMapper.selectById(article.getAuthorId());
        if (author != null) {
            vo.setAuthorNickname(author.getNickname());
        }

        return vo;
    }
}
