package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.common.result.PageResult;
import com.zhixun.common.util.ServiceUtils;
import com.zhixun.config.RedisConfig;
import com.zhixun.config.Slave;
import com.zhixun.entity.Article;
import com.zhixun.entity.ArticleTag;
import com.zhixun.entity.Category;
import com.zhixun.entity.Tag;
import com.zhixun.entity.User;
import com.zhixun.entity.UserFollow;
import com.zhixun.entity.UserPreferredCategory;
import com.zhixun.entity.UserPreferredTag;
import com.zhixun.enums.ArticleStatusEnum;
import com.zhixun.mapper.ArticleMapper;
import com.zhixun.mapper.ArticleTagMapper;
import com.zhixun.mapper.CategoryMapper;
import com.zhixun.mapper.TagMapper;
import com.zhixun.mapper.UserFollowMapper;
import com.zhixun.mapper.UserMapper;
import com.zhixun.mapper.UserPreferredCategoryMapper;
import com.zhixun.mapper.UserPreferredTagMapper;
import com.zhixun.service.FeedService;
import com.zhixun.vo.ArticleVO;
import com.zhixun.vo.TagVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 信息流服务实现
 * <p>
 * 增强功能：
 * - 冷启动处理：新用户使用偏好分类热门作品 + 全局热门 + 随机探索
 * - 多样性重排：MMR 启发的重排，避免推荐结果同质化
 * - 时效性提升：近期发布作品获得额外加分
 * - 真正刷新：refresh=1 时清除子服务缓存并引入随机性，每次刷新展示不同作品
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final UserPreferredCategoryMapper userPreferredCategoryMapper;
    private final UserPreferredTagMapper userPreferredTagMapper;
    private final UserFollowMapper userFollowMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final CollaborativeFilterService collaborativeFilterService;
    private final ColdStartBanditService coldStartBanditService;
    private final ContentBasedRecommendService contentBasedRecommendService;

    /** 推荐缓存 Key 前缀 */
    private static final String RECOMMEND_KEY_PREFIX = "feed:recommend:";
    /** 推荐缓存 TTL（10分钟） */
    private static final long RECOMMEND_TTL_MINUTES = 10;

    /** 最新动态缓存 Key 前缀 */
    private static final String LATEST_FEED_KEY_PREFIX = "feed:latest:";
    /** 最新动态缓存 TTL（2分钟） */
    private static final long LATEST_FEED_TTL_MINUTES = 2;

    /** 热门动态缓存 Key 前缀 */
    private static final String HOT_FEED_KEY_PREFIX = "feed:hot:";
    /** 热门动态缓存 TTL（5分钟） */
    private static final long HOT_FEED_TTL_MINUTES = 5;

    /** 关注动态缓存 Key 前缀 */
    private static final String FOLLOWING_FEED_KEY_PREFIX = "feed:following:";
    /** 关注动态缓存 TTL（5分钟） */
    private static final long FOLLOWING_FEED_TTL_MINUTES = 5;

    /** 关注动态时间线 Key 前缀（Redis Sorted Set，score = 发布时间戳） */
    private static final String TIMELINE_KEY_PREFIX = "timeline:user:";

    /** 时效性加分：7天内发布的作品获得加分 */
    private static final long RECENCY_BOOST_DAYS = 7;
    /** 时效性加分值 */
    private static final double RECENCY_BOOST_SCORE = 0.15;
    /** 多样性惩罚系数：同一分类占比超过阈值时降低分数 */
    private static final double DIVERSITY_PENALTY = 0.3;
    /** 多样性目标：同一分类最多占比 */
    private static final double MAX_CATEGORY_RATIO = 0.3;
    /** 冷启动随机探索比例 */
    private static final double COLD_START_EXPLORATION_RATIO = 0.2;

    @Override
    @Slave
    public PageResult<ArticleVO> getRecommendFeed(Long userId, Integer refresh, Integer page, Integer pageSize) {
        // 未登录用户返回热门作品
        if (userId == null) {
            return getHotFeed(page, pageSize);
        }

        // 检查是否需要刷新批次
        String refreshKey = null;
        boolean isRefresh = refresh != null && refresh == 1;
        if (isRefresh) {
            // 清除子服务缓存，确保推荐管线产出真正新鲜的结果
            invalidateRecommendCaches(userId);
            // 生成新的 refresh_key
            refreshKey = UUID.randomUUID().toString().replace("-", "");
            try {
                stringRedisTemplate.opsForValue().set(RECOMMEND_KEY_PREFIX + "key:" + userId, refreshKey, RECOMMEND_TTL_MINUTES, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.warn("Redis 不可用，跳过缓存推荐刷新批次: {}", e.getMessage());
            }
        } else {
            // 尝试获取已有的 refresh_key
            String existingKey = null;
            try {
                existingKey = stringRedisTemplate.opsForValue().get(RECOMMEND_KEY_PREFIX + "key:" + userId);
            } catch (Exception e) {
                log.warn("Redis 不可用，跳过读取推荐批次: {}", e.getMessage());
            }
            if (existingKey != null) {
                refreshKey = existingKey;
            } else {
                // 没有已有批次，生成新的
                refreshKey = UUID.randomUUID().toString().replace("-", "");
                try {
                    stringRedisTemplate.opsForValue().set(RECOMMEND_KEY_PREFIX + "key:" + userId, refreshKey, RECOMMEND_TTL_MINUTES, TimeUnit.MINUTES);
                } catch (Exception e) {
                    log.warn("Redis 不可用，跳过缓存推荐刷新批次: {}", e.getMessage());
                }
            }
        }

        // 尝试从缓存获取
        String cacheKey = RECOMMEND_KEY_PREFIX + refreshKey;
        String cachedIds = null;
        try {
            cachedIds = stringRedisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.warn("Redis 不可用，跳过推荐缓存读取: {}", e.getMessage());
        }
        if (cachedIds != null) {
            // 从缓存中分页
            return getPagedFromCache(cacheKey, page, pageSize);
        }

        // 缓存未命中，生成推荐列表
        List<Long> recommendedArticleIds = generateRecommendedIds(userId);

        // 缓存推荐结果（带 TTL 抖动防雪崩）
        if (!recommendedArticleIds.isEmpty()) {
            // 将作品ID列表以逗号分隔存入 Redis
            String idsStr = recommendedArticleIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            long jitteredTTL = RedisConfig.jitteredTTLFromMinutes(RECOMMEND_TTL_MINUTES);
            try {
                stringRedisTemplate.opsForValue().set(cacheKey, idsStr, jitteredTTL, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Redis 不可用，跳过推荐缓存写入: {}", e.getMessage());
            }
        }

        return getPagedFromCache(cacheKey, page, pageSize);
    }

    @Override
    @Slave
    public PageResult<ArticleVO> getLatestFeed(Integer page, Integer pageSize) {
        // 尝试从 Redis 缓存获取
        String cacheKey = LATEST_FEED_KEY_PREFIX + page + ":" + pageSize;
        String cachedIds = null;
        try {
            cachedIds = stringRedisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.warn("Redis 不可用，跳过最新动态缓存读取: {}", e.getMessage());
        }
        if (cachedIds != null) {
            return getPagedFromCache(cacheKey, page, pageSize);
        }

        // 缓存未命中，查询数据库
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                .orderByDesc(Article::getCreatedAt);

        Page<Article> articlePage = new Page<>(page, pageSize);
        Page<Article> result = articleMapper.selectPage(articlePage, wrapper);

        List<ArticleVO> voList = convertToVOList(result.getRecords());

        // 缓存作品ID列表（仅缓存当前页的ID，供后续相同分页请求命中）
        if (!CollectionUtils.isEmpty(voList)) {
            String idsStr = voList.stream()
                    .map(vo -> String.valueOf(vo.getId()))
                    .collect(Collectors.joining(","));
            long jitteredTTL = RedisConfig.jitteredTTLFromMinutes(LATEST_FEED_TTL_MINUTES);
            try {
                stringRedisTemplate.opsForValue().set(cacheKey, idsStr, jitteredTTL, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Redis 不可用，跳过最新动态缓存写入: {}", e.getMessage());
            }
        }

        return new PageResult<>(voList, result.getTotal(), page, pageSize);
    }

    @Override
    @Slave
    public PageResult<ArticleVO> getFollowingFeed(Long userId, Integer page, Integer pageSize) {
        // 尝试从 Redis 缓存获取
        String cacheKey = FOLLOWING_FEED_KEY_PREFIX + userId;
        String cachedIds = null;
        try {
            cachedIds = stringRedisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.warn("Redis 不可用，跳过关注动态缓存读取: {}", e.getMessage());
        }
        if (cachedIds != null) {
            return getPagedFromCache(cacheKey, page, pageSize);
        }

        // 缓存未命中，优先从 Redis Sorted Set 时间线获取
        String timelineKey = TIMELINE_KEY_PREFIX + userId;
        Long timelineSize = null;
        try {
            timelineSize = stringRedisTemplate.opsForZSet().size(timelineKey);
        } catch (Exception e) {
            log.warn("Redis 不可用，跳过时间线大小查询: {}", e.getMessage());
        }
        if (timelineSize != null && timelineSize > 0) {
            return getFollowingFeedFromTimeline(userId, timelineKey, page, pageSize);
        }

        // 时间线也不存在，从数据库查询并构建缓存
        PageResult<ArticleVO> result = getFollowingFeedFromDB(userId, page, pageSize);

        // 异步缓存结果
        if (!CollectionUtils.isEmpty(result.getList())) {
            cacheFollowingFeedAsync(userId, result.getList());
        }

        return result;
    }

    @Override
    public PageResult<ArticleVO> getFollowingFeedByCursor(Long userId, LocalDateTime cursor, Integer pageSize) {
        // 优先从 Redis Sorted Set 时间线获取
        String timelineKey = TIMELINE_KEY_PREFIX + userId;
        Long timelineSize = null;
        try {
            timelineSize = stringRedisTemplate.opsForZSet().size(timelineKey);
        } catch (Exception e) {
            log.warn("Redis 不可用，跳过时间线大小查询: {}", e.getMessage());
        }

        if (timelineSize != null && timelineSize > 0) {
            return getFollowingFeedByCursorFromTimeline(userId, timelineKey, cursor, pageSize);
        }

        // 时间线不存在，从数据库查询
        // 查询关注的用户列表
        List<UserFollow> follows = userFollowMapper.selectList(
                new LambdaQueryWrapper<UserFollow>().eq(UserFollow::getFollowerId, userId));

        if (CollectionUtils.isEmpty(follows)) {
            return new PageResult<>(Collections.emptyList(), 0L, 1, pageSize);
        }

        List<Long> followUserIds = follows.stream()
                .map(UserFollow::getFollowingId)
                .collect(Collectors.toList());

        // 查询关注用户的已发布作品（游标分页）
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                .in(Article::getAuthorId, followUserIds);
        if (cursor != null) {
            wrapper.lt(Article::getCreatedAt, cursor);
        }
        wrapper.orderByDesc(Article::getCreatedAt)
                .last("LIMIT " + pageSize);

        List<Article> articles = articleMapper.selectList(wrapper);
        List<ArticleVO> voList = convertToVOList(articles);

        // 异步构建时间线
        rebuildTimelineAsync(userId);

        return new PageResult<>(voList, null, 1, pageSize);
    }

    @Override
    @Slave
    public PageResult<ArticleVO> getHotFeed(Integer page, Integer pageSize) {
        // 尝试从 Redis 缓存获取
        String cacheKey = HOT_FEED_KEY_PREFIX + page + ":" + pageSize;
        String cachedIds = null;
        try {
            cachedIds = stringRedisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.warn("Redis 不可用，跳过热门动态缓存读取: {}", e.getMessage());
        }
        if (cachedIds != null) {
            return getPagedFromCache(cacheKey, page, pageSize);
        }

        // 缓存未命中，查询数据库
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                .orderByDesc(Article::getViewCount)
                .orderByDesc(Article::getLikeCount)
                .orderByDesc(Article::getCreatedAt);

        Page<Article> articlePage = new Page<>(page, pageSize);
        Page<Article> result = articleMapper.selectPage(articlePage, wrapper);

        List<ArticleVO> voList = convertToVOList(result.getRecords());

        // 缓存作品ID列表（仅缓存当前页的ID，供后续相同分页请求命中）
        if (!CollectionUtils.isEmpty(voList)) {
            String idsStr = voList.stream()
                    .map(vo -> String.valueOf(vo.getId()))
                    .collect(Collectors.joining(","));
            long jitteredTTL = RedisConfig.jitteredTTLFromMinutes(HOT_FEED_TTL_MINUTES);
            try {
                stringRedisTemplate.opsForValue().set(cacheKey, idsStr, jitteredTTL, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Redis 不可用，跳过热门动态缓存写入: {}", e.getMessage());
            }
        }

        return new PageResult<>(voList, result.getTotal(), page, pageSize);
    }

    @Override
    @Async
    public void fanoutOnPublish(Long authorId, Long articleId, LocalDateTime publishedAt) {
        try {
            // 查询作者的所有粉丝
            List<UserFollow> followers = userFollowMapper.selectList(
                    new LambdaQueryWrapper<UserFollow>().eq(UserFollow::getFollowingId, authorId));

            if (CollectionUtils.isEmpty(followers)) {
                return;
            }

            double score = publishedAt.toEpochSecond(ZoneOffset.of("+8"));
            String articleValue = String.valueOf(articleId);

            // 推送到每个粉丝的时间线
            for (UserFollow follow : followers) {
                String timelineKey = TIMELINE_KEY_PREFIX + follow.getFollowerId();
                stringRedisTemplate.opsForZSet().add(timelineKey, articleValue, score);
                // 设置时间线过期时间（7天）
                stringRedisTemplate.expire(timelineKey, 7, TimeUnit.DAYS);
            }

            // 清除关注动态缓存，使下次请求重新加载
            for (UserFollow follow : followers) {
                String cacheKey = FOLLOWING_FEED_KEY_PREFIX + follow.getFollowerId();
                stringRedisTemplate.delete(cacheKey);
            }

            log.info("Fan-out 完成: authorId={}, articleId={}, 粉丝数={}", authorId, articleId, followers.size());
        } catch (Exception e) {
            log.error("Fan-out 失败: authorId={}, articleId={}, error={}", authorId, articleId, e.getMessage());
        }
    }

    @Override
    public void clearFeedCaches() {
        try {
            Set<String> keysToDelete = new HashSet<>();
            String[] patterns = {
                    LATEST_FEED_KEY_PREFIX + "*",
                    HOT_FEED_KEY_PREFIX + "*",
                    RECOMMEND_KEY_PREFIX + "*"
            };
            for (String pattern : patterns) {
                try (org.springframework.data.redis.core.Cursor<String> cursor = stringRedisTemplate.scan(
                        org.springframework.data.redis.core.ScanOptions.scanOptions()
                                .match(pattern)
                                .count(100)
                                .build())) {
                    while (cursor.hasNext()) {
                        keysToDelete.add(cursor.next());
                    }
                }
            }
            if (!keysToDelete.isEmpty()) {
                stringRedisTemplate.delete(keysToDelete);
                log.info("已清除 Feed 缓存: {} 个 key", keysToDelete.size());
            }
        } catch (Exception e) {
            log.warn("清除 Feed 缓存失败（不影响主流程）: {}", e.getMessage());
        }
    }

    // ========== 内部方法 ==========

    /**
     * 清除用户的推荐子服务缓存（协同过滤 + 内容相似度）
     * 在 refresh=1 时调用，确保推荐管线从源头产出全新结果
     */
    private void invalidateRecommendCaches(Long userId) {
        try {
            // 清除协同过滤缓存（cf:recommend:{userId} + cf:similarity:{userId}）
            stringRedisTemplate.delete("cf:recommend:" + userId);
            stringRedisTemplate.delete("cf:similarity:" + userId);
            // 清除内容相似度推荐缓存（cb:recommend:{userId}）
            stringRedisTemplate.delete("cb:recommend:" + userId);
            log.debug("已清除用户推荐子缓存: userId={}", userId);
        } catch (Exception e) {
            log.warn("清除推荐子缓存失败（不影响主流程）: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 从数据库查询关注动态
     */
    private PageResult<ArticleVO> getFollowingFeedFromDB(Long userId, Integer page, Integer pageSize) {
        // 查询关注的用户列表
        List<UserFollow> follows = userFollowMapper.selectList(
                new LambdaQueryWrapper<UserFollow>().eq(UserFollow::getFollowerId, userId));

        if (CollectionUtils.isEmpty(follows)) {
            return new PageResult<>(Collections.emptyList(), 0L, page, pageSize);
        }

        List<Long> followUserIds = follows.stream()
                .map(UserFollow::getFollowingId)
                .collect(Collectors.toList());

        // 查询关注用户的已发布作品
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                .in(Article::getAuthorId, followUserIds)
                .orderByDesc(Article::getCreatedAt);

        Page<Article> articlePage = new Page<>(page, pageSize);
        Page<Article> result = articleMapper.selectPage(articlePage, wrapper);

        List<ArticleVO> voList = convertToVOList(result.getRecords());
        return new PageResult<>(voList, result.getTotal(), page, pageSize);
    }

    /**
     * 从 Redis Sorted Set 时间线获取关注动态（页码分页）
     */
    private PageResult<ArticleVO> getFollowingFeedFromTimeline(Long userId, String timelineKey, Integer page, Integer pageSize) {
        Long total = null;
        try {
            total = stringRedisTemplate.opsForZSet().size(timelineKey);
        } catch (Exception e) {
            log.warn("Redis 不可用，降级到数据库查询关注动态: {}", e.getMessage());
            return getFollowingFeedFromDB(userId, page, pageSize);
        }
        if (total == null || total == 0) {
            return new PageResult<>(Collections.emptyList(), 0L, page, pageSize);
        }

        // Sorted Set 按分数降序（最新在前）
        long start = (long) (page - 1) * pageSize;
        long end = start + pageSize - 1;
        Set<String> articleIdStrs = null;
        try {
            articleIdStrs = stringRedisTemplate.opsForZSet().reverseRange(timelineKey, start, end);
        } catch (Exception e) {
            log.warn("Redis 不可用，降级到数据库查询关注动态: {}", e.getMessage());
            return getFollowingFeedFromDB(userId, page, pageSize);
        }

        if (CollectionUtils.isEmpty(articleIdStrs)) {
            return new PageResult<>(Collections.emptyList(), total, page, pageSize);
        }

        List<Long> articleIds = articleIdStrs.stream()
                .map(Long::parseLong)
                .collect(Collectors.toList());

        // 批量查询作品
        List<Article> articles = articleMapper.selectBatchIds(articleIds);
        // 过滤已发布状态的作品
        articles = articles.stream()
                .filter(a -> a.getStatus() == ArticleStatusEnum.PUBLISHED && a.getDeletedAt() == null)
                .collect(Collectors.toList());

        // 保持时间线中的顺序
        Map<Long, Article> articleMap = articles.stream()
                .collect(Collectors.toMap(Article::getId, a -> a));
        List<Article> orderedArticles = articleIds.stream()
                .map(articleMap::get)
                .filter(a -> a != null)
                .collect(Collectors.toList());

        List<ArticleVO> voList = convertToVOList(orderedArticles);
        return new PageResult<>(voList, total, page, pageSize);
    }

    /**
     * 从 Redis Sorted Set 时间线获取关注动态（游标分页）
     */
    private PageResult<ArticleVO> getFollowingFeedByCursorFromTimeline(Long userId, String timelineKey, LocalDateTime cursor, Integer pageSize) {
        Set<String> articleIdStrs;
        if (cursor == null) {
            // 首次加载，获取最新的 pageSize 条
            try {
                articleIdStrs = stringRedisTemplate.opsForZSet().reverseRange(timelineKey, 0, pageSize - 1);
            } catch (Exception e) {
                log.warn("Redis 不可用，降级到数据库查询: {}", e.getMessage());
                return getFollowingFeedFromDB(userId, 1, pageSize);
            }
        } else {
            // 游标分页：获取分数小于 cursor 的 pageSize 条
            double maxScore = cursor.toEpochSecond(ZoneOffset.of("+8"));
            try {
                articleIdStrs = stringRedisTemplate.opsForZSet().reverseRangeByScore(timelineKey, 0, maxScore - 1, 0, pageSize);
            } catch (Exception e) {
                log.warn("Redis 不可用，降级到数据库查询: {}", e.getMessage());
                return getFollowingFeedFromDB(userId, 1, pageSize);
            }
        }

        if (CollectionUtils.isEmpty(articleIdStrs)) {
            return new PageResult<>(Collections.emptyList(), null, 1, pageSize);
        }

        List<Long> articleIds = articleIdStrs.stream()
                .map(Long::parseLong)
                .collect(Collectors.toList());

        // 批量查询作品
        List<Article> articles = articleMapper.selectBatchIds(articleIds);
        articles = articles.stream()
                .filter(a -> a.getStatus() == ArticleStatusEnum.PUBLISHED && a.getDeletedAt() == null)
                .collect(Collectors.toList());

        // 保持时间线中的顺序
        Map<Long, Article> articleMap = articles.stream()
                .collect(Collectors.toMap(Article::getId, a -> a));
        List<Article> orderedArticles = articleIds.stream()
                .map(articleMap::get)
                .filter(a -> a != null)
                .collect(Collectors.toList());

        List<ArticleVO> voList = convertToVOList(orderedArticles);
        return new PageResult<>(voList, null, 1, pageSize);
    }

    /**
     * 异步缓存关注动态结果
     */
    @Async
    public void cacheFollowingFeedAsync(Long userId, List<ArticleVO> articles) {
        try {
            String cacheKey = FOLLOWING_FEED_KEY_PREFIX + userId;
            String idsStr = articles.stream()
                    .map(vo -> String.valueOf(vo.getId()))
                    .collect(Collectors.joining(","));
            stringRedisTemplate.opsForValue().set(cacheKey, idsStr, FOLLOWING_FEED_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("缓存关注动态失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 异步重建用户时间线
     */
    @Async
    public void rebuildTimelineAsync(Long userId) {
        try {
            // 查询关注的用户列表
            List<UserFollow> follows = userFollowMapper.selectList(
                    new LambdaQueryWrapper<UserFollow>().eq(UserFollow::getFollowerId, userId));

            if (CollectionUtils.isEmpty(follows)) {
                return;
            }

            List<Long> followUserIds = follows.stream()
                    .map(UserFollow::getFollowingId)
                    .collect(Collectors.toList());

            // 查询关注用户的已发布作品（最近500条）
            LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                    .in(Article::getAuthorId, followUserIds)
                    .orderByDesc(Article::getCreatedAt)
                    .last("LIMIT 500");
            List<Article> articles = articleMapper.selectList(wrapper);

            if (CollectionUtils.isEmpty(articles)) {
                return;
            }

            // 构建 Redis Sorted Set
            String timelineKey = TIMELINE_KEY_PREFIX + userId;
            for (Article article : articles) {
                if (article.getCreatedAt() != null) {
                    double score = article.getCreatedAt().toEpochSecond(ZoneOffset.of("+8"));
                    stringRedisTemplate.opsForZSet().add(timelineKey, String.valueOf(article.getId()), score);
                }
            }
            // 设置过期时间（7天）
            stringRedisTemplate.expire(timelineKey, 7, TimeUnit.DAYS);

            log.info("重建时间线完成: userId={}, 作品数={}", userId, articles.size());
        } catch (Exception e) {
            log.error("重建时间线失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 基于协同过滤 + Bandit冷启动 + 内容相似度 + 偏好推荐生成推荐作品ID列表
     * <p>
     * 增强功能：
     * - 冷启动处理：新用户使用偏好分类热门作品 + 全局热门 + 随机探索
     * - 多样性重排：MMR 启发的重排，避免推荐结果同质化
     * - 时效性提升：近期发布作品获得额外加分
     */
    private List<Long> generateRecommendedIds(Long userId) {
        List<Long> articleIds = new ArrayList<>();
        boolean isColdStart = coldStartBanditService.shouldUseBandit(userId);

        // ========== 第一阶段：协同过滤推荐 ==========
        try {
            List<Long> cfArticleIds = collaborativeFilterService.recommendArticles(userId, 80);
            if (!CollectionUtils.isEmpty(cfArticleIds)) {
                articleIds.addAll(cfArticleIds);
                log.debug("协同过滤推荐作品数: {}", cfArticleIds.size());
            }
        } catch (Exception e) {
            log.warn("协同过滤推荐异常，降级到偏好推荐: {}", e.getMessage());
        }

        // ========== 第二阶段：冷启动处理 ==========
        if (isColdStart) {
            // 冷启动用户：使用偏好分类热门作品 + 全局热门 + 随机探索
            List<Long> coldStartIds = getColdStartArticleIds(userId, articleIds);
            articleIds.addAll(coldStartIds);
            log.debug("冷启动推荐作品数: {}", coldStartIds.size());
        } else if (articleIds.size() < 20) {
            // Bandit冷启动推荐（随机采样）
            try {
                List<Long> banditCategoryIds = coldStartBanditService.recommendCategories(userId, 5);
                if (!CollectionUtils.isEmpty(banditCategoryIds)) {
                    Set<Long> excludeIds = new HashSet<>(articleIds);
                    LambdaQueryWrapper<Article> banditWrapper = new LambdaQueryWrapper<>();
                    banditWrapper.eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                            .in(Article::getCategoryId, banditCategoryIds)
                            .notIn(!excludeIds.isEmpty(), Article::getId, excludeIds)
                            .orderByDesc(Article::getCreatedAt)
                            .last("LIMIT 80");
                    List<Article> banditCandidates = articleMapper.selectList(banditWrapper);
                    if (!banditCandidates.isEmpty()) {
                        Collections.shuffle(banditCandidates);
                        List<Long> banditArticleIds = banditCandidates.stream()
                                .limit(30)
                                .map(Article::getId).collect(Collectors.toList());
                        articleIds.addAll(banditArticleIds);
                        log.debug("Bandit冷启动推荐作品数: {}", banditArticleIds.size());
                    }
                }
            } catch (Exception e) {
                log.warn("Bandit冷启动推荐异常: {}", e.getMessage());
            }
        }

        // ========== 第三阶段：内容相似度推荐 ==========
        if (articleIds.size() < 20) {
            try {
                Set<Long> excludeIds = new HashSet<>(articleIds);
                List<Long> cbArticleIds = contentBasedRecommendService.recommendByContent(userId, 30);
                cbArticleIds = cbArticleIds.stream()
                        .filter(id -> !excludeIds.contains(id))
                        .collect(Collectors.toList());
                articleIds.addAll(cbArticleIds);
                log.debug("内容相似度推荐作品数: {}", cbArticleIds.size());
            } catch (Exception e) {
                log.warn("内容相似度推荐异常: {}", e.getMessage());
            }
        }

        // ========== 第四阶段：偏好推荐兜底 ==========
        if (articleIds.size() < 20) {
            List<Long> preferenceIds = getPreferenceBasedArticleIds(userId, articleIds);
            articleIds.addAll(preferenceIds);
        }

        // ========== 第五阶段：热门作品补充（带随机采样） ==========
        if (articleIds.size() < 20) {
            int need = 50 - articleIds.size();
            // 拉取 3 倍候选，随机采样以实现每次刷新产出不同作品
            LambdaQueryWrapper<Article> hotWrapper = new LambdaQueryWrapper<>();
            hotWrapper.eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                    .notIn(!articleIds.isEmpty(), Article::getId, articleIds)
                    .orderByDesc(Article::getViewCount)
                    .last("LIMIT " + (need * 3));
            List<Article> hotCandidates = articleMapper.selectList(hotWrapper);
            if (!hotCandidates.isEmpty()) {
                // 加权随机采样：浏览量越高的作品被选中概率越大，但不保证每次都一样
                Collections.shuffle(hotCandidates);
                hotCandidates.sort((a, b) -> {
                    double scoreA = (a.getViewCount() != null ? a.getViewCount() : 0) + ThreadLocalRandom.current().nextDouble() * 1000;
                    double scoreB = (b.getViewCount() != null ? b.getViewCount() : 0) + ThreadLocalRandom.current().nextDouble() * 1000;
                    return Double.compare(scoreB, scoreA);
                });
                List<Long> sampledIds = hotCandidates.stream()
                        .limit(need)
                        .map(Article::getId)
                        .collect(Collectors.toList());
                articleIds.addAll(sampledIds);
            }
        }

        // ========== 第六阶段：多样性重排 + 时效性提升 ==========
        articleIds = applyDiversityAndRecency(articleIds);

        return articleIds;
    }

    /**
     * 冷启动用户的推荐逻辑
     * <p>
     * 策略：
     * 1. 用户注册时选择的偏好分类下的热门作品（50%）— 随机采样
     * 2. 全局热门作品（30%）— 随机采样
     * 3. 随机探索作品，来自不同分类（20%）
     */
    private List<Long> getColdStartArticleIds(Long userId, List<Long> excludeIds) {
        List<Long> result = new ArrayList<>();
        Set<Long> allExcludeIds = new HashSet<>(excludeIds);

        // 1. 偏好分类下的热门作品（随机采样）
        List<UserPreferredCategory> preferredCategories = userPreferredCategoryMapper.selectList(
                new LambdaQueryWrapper<UserPreferredCategory>().eq(UserPreferredCategory::getUserId, userId));
        List<Long> categoryIds = preferredCategories.stream()
                .map(UserPreferredCategory::getCategoryId)
                .collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(categoryIds)) {
            LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                    .in(Article::getCategoryId, categoryIds)
                    .notIn(!allExcludeIds.isEmpty(), Article::getId, allExcludeIds)
                    .orderByDesc(Article::getViewCount)
                    .last("LIMIT 60");
            List<Article> categoryCandidates = articleMapper.selectList(wrapper);
            if (!categoryCandidates.isEmpty()) {
                Collections.shuffle(categoryCandidates);
                List<Long> categoryArticleIds = categoryCandidates.stream()
                        .limit(25)
                        .map(Article::getId)
                        .collect(Collectors.toList());
                result.addAll(categoryArticleIds);
                allExcludeIds.addAll(categoryArticleIds);
            }
        }

        // 2. 全局热门作品（随机采样）
        LambdaQueryWrapper<Article> hotWrapper = new LambdaQueryWrapper<>();
        hotWrapper.eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                .notIn(!allExcludeIds.isEmpty(), Article::getId, allExcludeIds)
                .orderByDesc(Article::getViewCount)
                .last("LIMIT 40");
        List<Article> hotCandidates = articleMapper.selectList(hotWrapper);
        if (!hotCandidates.isEmpty()) {
            Collections.shuffle(hotCandidates);
            List<Long> hotArticleIds = hotCandidates.stream()
                    .limit(15)
                    .map(Article::getId)
                    .collect(Collectors.toList());
            result.addAll(hotArticleIds);
            allExcludeIds.addAll(hotArticleIds);
        }

        // 3. 随机探索作品（来自不同分类）
        int explorationCount = (int) (result.size() * COLD_START_EXPLORATION_RATIO / (1 - COLD_START_EXPLORATION_RATIO));
        explorationCount = Math.max(5, explorationCount);
        LambdaQueryWrapper<Article> exploreWrapper = new LambdaQueryWrapper<>();
        exploreWrapper.eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                .notIn(!allExcludeIds.isEmpty(), Article::getId, allExcludeIds)
                .orderByDesc(Article::getCreatedAt)
                .last("LIMIT " + explorationCount * 3);
        List<Article> exploreCandidates = articleMapper.selectList(exploreWrapper);
        if (!CollectionUtils.isEmpty(exploreCandidates)) {
            Collections.shuffle(exploreCandidates);
            List<Long> exploreIds = exploreCandidates.stream()
                    .limit(explorationCount)
                    .map(Article::getId)
                    .collect(Collectors.toList());
            result.addAll(exploreIds);
        }

        return result;
    }

    /**
     * 应用多样性重排和时效性提升
     * <p>
     * 1. 时效性提升：7天内发布的作品获得额外加分
     * 2. 多样性重排：MMR（Maximal Marginal Relevance）启发的重排
     * - 对同一分类的作品进行惩罚，避免推荐结果同质化
     * - 确保推荐列表中不同分类的作品比例合理
     */
    private List<Long> applyDiversityAndRecency(List<Long> articleIds) {
        if (CollectionUtils.isEmpty(articleIds)) {
            return articleIds;
        }

        // 批量查询作品信息（用于获取分类和发布时间）
        List<Article> articles = articleMapper.selectBatchIds(articleIds);
        if (CollectionUtils.isEmpty(articles)) {
            return articleIds;
        }

        Map<Long, Article> articleMap = articles.stream()
                .collect(Collectors.toMap(Article::getId, a -> a, (a, b) -> a));

        // 1. 计算每篇作品的综合分数（基础分 + 时效性加分 + 随机扰动）
        Map<Long, Double> scoreMap = new HashMap<>();
        for (Long articleId : articleIds) {
            Article article = articleMap.get(articleId);
            if (article == null) {
                continue;
            }

            // 基础分：基于浏览量归一化
            double baseScore = 0.5;
            if (article.getViewCount() != null && article.getViewCount() > 0) {
                baseScore = Math.min(1.0, Math.log10(article.getViewCount()) / 5.0);
            }

            // 时效性加分
            double recencyBoost = calculateRecencyBoost(article);

            // 随机扰动：让分数相近的作品每次刷新排列不同，但不颠覆整体质量排序
            double perturbation = ThreadLocalRandom.current().nextDouble(-0.08, 0.08);

            scoreMap.put(articleId, baseScore + recencyBoost + perturbation);
        }

        // 2. MMR 启发的多样性重排
        List<Long> result = new ArrayList<>();
        Map<Long, Integer> categoryCount = new HashMap<>();
        Set<Long> selectedIds = new HashSet<>();

        for (Long articleId : articleIds) {
            if (selectedIds.contains(articleId)) {
                continue;
            }

            Article article = articleMap.get(articleId);
            if (article == null) {
                result.add(articleId);
                selectedIds.add(articleId);
                continue;
            }

            double score = scoreMap.getOrDefault(articleId, 0.5);

            // 分类多样性惩罚
            if (article.getCategoryId() != null) {
                int count = categoryCount.getOrDefault(article.getCategoryId(), 0);
                double currentRatio = (double) count / Math.max(1, result.size());
                if (currentRatio > MAX_CATEGORY_RATIO && count > 0) {
                    score -= DIVERSITY_PENALTY;
                }
            }

            // 分数太低的作品跳过（但保证至少有结果）
            if (score < 0 && result.size() >= 20) {
                continue;
            }

            result.add(articleId);
            selectedIds.add(articleId);

            if (article.getCategoryId() != null) {
                categoryCount.merge(article.getCategoryId(), 1, Integer::sum);
            }
        }

        return result;
    }

    /**
     * 计算作品的时效性加分
     * 7天内发布的作品获得加分，越新加分越高
     *
     * @param article 作品实体
     * @return 时效性加分 [0, RECENCY_BOOST_SCORE]
     */
    private double calculateRecencyBoost(Article article) {
        LocalDateTime publishTime = article.getPublishAt() != null ? article.getPublishAt() : article.getCreatedAt();
        if (publishTime == null) {
            return 0.0;
        }

        long daysSincePublish = ChronoUnit.DAYS.between(publishTime, LocalDateTime.now());
        if (daysSincePublish < 0) {
            daysSincePublish = 0;
        }

        if (daysSincePublish >= RECENCY_BOOST_DAYS) {
            return 0.0;
        }

        // 线性衰减：发布当天加分最高，7天后为0
        return RECENCY_BOOST_SCORE * (1.0 - (double) daysSincePublish / RECENCY_BOOST_DAYS);
    }

    /**
     * 基于用户偏好的作品推荐（兜底策略，带随机采样）
     */
    private List<Long> getPreferenceBasedArticleIds(Long userId, List<Long> excludeIds) {
        List<Long> result = new ArrayList<>();

        // 获取用户偏好的分类
        List<UserPreferredCategory> preferredCategories = userPreferredCategoryMapper.selectList(
                new LambdaQueryWrapper<UserPreferredCategory>().eq(UserPreferredCategory::getUserId, userId));
        List<Long> categoryIds = preferredCategories.stream()
                .map(UserPreferredCategory::getCategoryId)
                .collect(Collectors.toList());

        // 获取用户偏好的标签
        List<UserPreferredTag> preferredTags = userPreferredTagMapper.selectList(
                new LambdaQueryWrapper<UserPreferredTag>().eq(UserPreferredTag::getUserId, userId));
        List<Long> tagIds = preferredTags.stream()
                .map(UserPreferredTag::getTagId)
                .collect(Collectors.toList());

        // 查询偏好分类下的作品（拉取 3 倍候选，随机采样）
        if (!CollectionUtils.isEmpty(categoryIds)) {
            LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                    .in(Article::getCategoryId, categoryIds)
                    .notIn(!excludeIds.isEmpty(), Article::getId, excludeIds)
                    .orderByDesc(Article::getViewCount)
                    .last("LIMIT 200");
            List<Article> candidates = articleMapper.selectList(wrapper);
            if (!candidates.isEmpty()) {
                Collections.shuffle(candidates);
                List<Long> sampledIds = candidates.stream()
                        .limit(80)
                        .map(Article::getId)
                        .collect(Collectors.toList());
                result.addAll(sampledIds);
            }
        }

        // 如果偏好标签不为空，查询包含这些标签的作品并追加（随机采样）
        if (!CollectionUtils.isEmpty(tagIds)) {
            List<ArticleTag> articleTags = articleTagMapper.selectList(
                    new LambdaQueryWrapper<ArticleTag>().in(ArticleTag::getTagId, tagIds));
            Set<Long> allExcludeIds = new HashSet<>(excludeIds);
            allExcludeIds.addAll(result);
            List<Long> tagArticleIds = articleTags.stream()
                    .map(ArticleTag::getArticleId)
                    .filter(id -> !allExcludeIds.contains(id))
                    .distinct()
                    .collect(Collectors.toList());

            if (!tagArticleIds.isEmpty()) {
                List<Article> tagArticles = articleMapper.selectList(
                        new LambdaQueryWrapper<Article>()
                                .eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                                .in(Article::getId, tagArticleIds)
                                .last("LIMIT 100"));
                if (!tagArticles.isEmpty()) {
                    Collections.shuffle(tagArticles);
                    List<Long> sampledTagIds = tagArticles.stream()
                            .limit(40)
                            .map(Article::getId)
                            .collect(Collectors.toList());
                    result.addAll(sampledTagIds);
                }
            }
        }

        return result;
    }

    /**
     * 从缓存分页获取推荐结果
     */
    private PageResult<ArticleVO> getPagedFromCache(String cacheKey, Integer page, Integer pageSize) {
        String idsStr = null;
        try {
            idsStr = stringRedisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.warn("Redis 不可用，跳过缓存分页读取: {}", e.getMessage());
        }
        if (idsStr == null || idsStr.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L, page, pageSize);
        }

        String[] idArray = idsStr.split(",");
        int total = idArray.length;
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        if (fromIndex >= total) {
            return new PageResult<>(Collections.emptyList(), (long) total, page, pageSize);
        }

        // 获取当前页的作品ID
        List<Long> pageIds = new ArrayList<>();
        for (int i = fromIndex; i < toIndex; i++) {
            pageIds.add(Long.parseLong(idArray[i]));
        }

        // 批量查询作品
        List<Article> articles = articleMapper.selectBatchIds(pageIds);
        // 保持缓存中的顺序
        Map<Long, Article> articleMap = articles.stream()
                .collect(Collectors.toMap(Article::getId, a -> a));
        List<Article> orderedArticles = pageIds.stream()
                .map(articleMap::get)
                .filter(a -> a != null)
                .collect(Collectors.toList());

        List<ArticleVO> voList = convertToVOList(orderedArticles);
        return new PageResult<>(voList, (long) total, page, pageSize);
    }

    /**
     * 批量将作品实体列表转换为 VO 列表
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

        // 批量查询作者信息（带空安全和重复处理）
        Map<Long, User> userMap = getSafeUserMap(userIds);

        // 批量查询分类信息（带空安全和重复处理）
        Map<Long, Category> categoryMap = CollectionUtils.isEmpty(categoryIds) ? Collections.emptyMap()
                : ServiceUtils.safeToList(categoryMapper.selectBatchIds(categoryIds)).stream()
                .collect(Collectors.toMap(Category::getId, c -> c, (existing, replacement) -> existing));

        // 批量查询标签关联
        List<ArticleTag> allArticleTags = ServiceUtils.safeToList(articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>().in(ArticleTag::getArticleId, articleIds)));
        Set<Long> tagIds = allArticleTags.stream().map(ArticleTag::getTagId).collect(Collectors.toSet());

        // 批量查询标签信息（带空安全和重复处理）
        Map<Long, Tag> tagMap = CollectionUtils.isEmpty(tagIds) ? Collections.emptyMap()
                : ServiceUtils.safeToList(tagMapper.selectBatchIds(tagIds)).stream()
                .collect(Collectors.toMap(Tag::getId, t -> t, (existing, replacement) -> existing));

        // 按作品ID分组标签
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
            vo.setAuthorId(article.getAuthorId());
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
     * 安全获取用户映射，处理 null 值和重复键
     */
    private Map<Long, User> getSafeUserMap(Set<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        if (CollectionUtils.isEmpty(users)) {
            return Collections.emptyMap();
        }
        return users.stream()
                .collect(Collectors.toMap(User::getId, u -> u, (existing, replacement) -> existing));
    }

}
