package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.entity.Article;
import com.zhixun.entity.ArticleLike;
import com.zhixun.entity.ArticleViewHistory;
import com.zhixun.entity.Collect;
import com.zhixun.enums.ArticleStatusEnum;
import com.zhixun.enums.LikeTargetTypeEnum;
import com.zhixun.mapper.ArticleLikeMapper;
import com.zhixun.mapper.ArticleMapper;
import com.zhixun.mapper.ArticleViewHistoryMapper;
import com.zhixun.mapper.CollectMapper;
import com.zhixun.config.RedisConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 协同过滤服务
 * <p>
 * 基于用户的协同过滤（User-based CF），使用 Jaccard 相似度计算用户相似度。
 * 通过用户行为数据（点赞、收藏、浏览）构建用户-文章交互矩阵，
 * 找到与当前用户兴趣相似的用户，推荐相似用户喜欢但当前用户未看过的文章。
 * <p>
 * 增强功能：
 * - 探索因子：20% 概率推荐随机文章，增加内容发现
 * - 冷启动处理：新用户无行为数据时返回空列表，由上层 FeedService 处理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollaborativeFilterService {

    private final ArticleLikeMapper articleLikeMapper;
    private final CollectMapper collectMapper;
    private final ArticleViewHistoryMapper articleViewHistoryMapper;
    private final ArticleMapper articleMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /** 用户相似度缓存 Key 前缀 */
    private static final String SIMILARITY_KEY_PREFIX = "cf:similarity:";
    /** 推荐结果缓存 Key 前缀 */
    private static final String RECOMMEND_KEY_PREFIX = "cf:recommend:";
    /** 缓存 TTL（1小时） */
    private static final long CACHE_TTL_HOURS = 1;
    /** 行为权重：点赞 */
    private static final double WEIGHT_LIKE = 3.0;
    /** 行为权重：收藏 */
    private static final double WEIGHT_COLLECT = 2.5;
    /** 行为权重：浏览 */
    private static final double WEIGHT_VIEW = 1.0;
    /** 探索因子比例（20% 的推荐位用于随机探索） */
    private static final double EXPLORATION_RATIO = 0.2;

    /**
     * 查找与指定用户兴趣最相似的 TopN 个用户
     *
     * @param userId 目标用户ID
     * @param topN   返回的相似用户数量
     * @return 相似用户ID列表（按相似度降序排列）
     */
    public List<Long> findSimilarUsers(Long userId, int topN) {
        // 1. 尝试从缓存获取
        String cacheKey = SIMILARITY_KEY_PREFIX + userId;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            return parseIdList(cached);
        }

        // 2. 获取目标用户的交互文章集合
        Map<Long, Double> targetUserVector = getUserArticleVector(userId);
        if (targetUserVector.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 获取与目标用户有交集的候选用户
        Set<Long> candidateUserIds = getCandidateUsers(userId, targetUserVector.keySet());

        // 4. 计算相似度并排序
        List<Map.Entry<Long, Double>> similarityList = new ArrayList<>();
        for (Long candidateId : candidateUserIds) {
            Map<Long, Double> candidateVector = getUserArticleVector(candidateId);
            double similarity = calculateJaccardSimilarity(targetUserVector, candidateVector);
            if (similarity > 0) {
                similarityList.add(Map.entry(candidateId, similarity));
            }
        }

        // 按相似度降序排列，取 TopN
        similarityList.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        List<Long> result = similarityList.stream()
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 5. 缓存结果（带 TTL 抖动防雪崩）
        if (!result.isEmpty()) {
            String idsStr = result.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            long jitteredTTL = RedisConfig.jitteredTTLFromHours(CACHE_TTL_HOURS);
            stringRedisTemplate.opsForValue().set(cacheKey, idsStr, jitteredTTL, TimeUnit.SECONDS);
        }

        return result;
    }

    /**
     * 基于协同过滤为指定用户推荐文章
     * <p>
     * 增强功能：
     * - 探索因子：20% 的推荐位用于随机文章，增加内容发现
     *
     * @param userId 目标用户ID
     * @param count  推荐文章数量
     * @return 推荐的文章ID列表
     */
    public List<Long> recommendArticles(Long userId, int count) {
        // 1. 尝试从缓存获取
        String cacheKey = RECOMMEND_KEY_PREFIX + userId;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            List<Long> cachedList = parseIdList(cached);
            return cachedList.stream().limit(count).collect(Collectors.toList());
        }

        // 2. 找到相似用户
        List<Long> similarUserIds = findSimilarUsers(userId, 20);
        if (similarUserIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 获取目标用户已交互的文章集合（排除这些）
        Set<Long> targetArticleIds = getUserArticleVector(userId).keySet();

        // 4. 收集相似用户喜欢但目标用户未看过的文章，按推荐分数排序
        Map<Long, Double> recommendScores = new HashMap<>();
        List<Long> similarUsers = similarUserIds;

        for (int i = 0; i < similarUsers.size(); i++) {
            Long similarUserId = similarUsers.get(i);
            // 相似度衰减权重：越靠前的相似用户权重越高
            double userWeight = 1.0 / (i + 1);
            Map<Long, Double> similarUserVector = getUserArticleVector(similarUserId);

            for (Map.Entry<Long, Double> entry : similarUserVector.entrySet()) {
                Long articleId = entry.getKey();
                // 排除目标用户已看过的文章
                if (!targetArticleIds.contains(articleId)) {
                    recommendScores.merge(articleId, entry.getValue() * userWeight, Double::sum);
                }
            }
        }

        // 5. 按推荐分数降序排列
        int cfCount = (int) (count * (1 - EXPLORATION_RATIO));
        List<Long> cfResult = recommendScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(cfCount)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 6. 探索因子：随机推荐不同分类的文章
        List<Long> explorationResult = getExplorationArticles(userId, targetArticleIds, cfResult, count - cfResult.size());
        List<Long> result = new ArrayList<>(cfResult);
        result.addAll(explorationResult);

        // 7. 缓存结果（带 TTL 抖动防雪崩）
        if (!result.isEmpty()) {
            String idsStr = result.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            long jitteredTTL = RedisConfig.jitteredTTLFromHours(CACHE_TTL_HOURS);
            stringRedisTemplate.opsForValue().set(cacheKey, idsStr, jitteredTTL, TimeUnit.SECONDS);
        }

        return result;
    }

    /**
     * 获取探索性推荐文章（随机从不同分类中选取）
     * 用于增加内容发现，避免推荐结果过于同质化
     *
     * @param userId          目标用户ID
     * @param targetArticleIds 目标用户已交互的文章ID
     * @param excludeIds      已推荐的文章ID
     * @param count           需要的探索文章数量
     * @return 探索文章ID列表
     */
    private List<Long> getExplorationArticles(Long userId, Set<Long> targetArticleIds, List<Long> excludeIds, int count) {
        if (count <= 0) {
            return Collections.emptyList();
        }

        try {
            Set<Long> allExcludeIds = new HashSet<>(targetArticleIds);
            allExcludeIds.addAll(excludeIds);

            // 随机获取不同分类的已发布文章
            LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                    .notIn(!allExcludeIds.isEmpty(), Article::getId, allExcludeIds)
                    .orderByDesc(Article::getCreatedAt)
                    .last("LIMIT " + count * 3);

            List<Article> candidates = articleMapper.selectList(wrapper);
            if (CollectionUtils.isEmpty(candidates)) {
                return Collections.emptyList();
            }

            // 随机选取 count 个
            Collections.shuffle(candidates);
            return candidates.stream()
                    .limit(count)
                    .map(Article::getId)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("获取探索文章失败: userId={}, error={}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 计算两个用户之间的 Jaccard 相似度
     * Jaccard(A, B) = |A ∩ B| / |A ∪ B|
     * 这里使用加权版本，考虑行为权重
     *
     * @param vectorA 用户A的文章交互向量
     * @param vectorB 用户B的文章交互向量
     * @return Jaccard 相似度 [0, 1]
     */
    public double calculateUserSimilarity(Map<Long, Double> vectorA, Map<Long, Double> vectorB) {
        return calculateJaccardSimilarity(vectorA, vectorB);
    }

    // ========== 内部方法 ==========

    /**
     * 计算加权 Jaccard 相似度
     */
    private double calculateJaccardSimilarity(Map<Long, Double> vectorA, Map<Long, Double> vectorB) {
        if (vectorA.isEmpty() || vectorB.isEmpty()) {
            return 0.0;
        }

        // 计算交集
        Set<Long> intersection = new HashSet<>(vectorA.keySet());
        intersection.retainAll(vectorB.keySet());

        if (intersection.isEmpty()) {
            return 0.0;
        }

        // 计算并集
        Set<Long> union = new HashSet<>(vectorA.keySet());
        union.addAll(vectorB.keySet());

        // 加权 Jaccard：交集权重之和 / 并集权重之和
        double intersectionWeight = 0.0;
        for (Long articleId : intersection) {
            intersectionWeight += Math.min(vectorA.get(articleId), vectorB.get(articleId));
        }

        double unionWeight = 0.0;
        for (Long articleId : union) {
            double weightA = vectorA.getOrDefault(articleId, 0.0);
            double weightB = vectorB.getOrDefault(articleId, 0.0);
            unionWeight += Math.max(weightA, weightB);
        }

        return unionWeight == 0.0 ? 0.0 : intersectionWeight / unionWeight;
    }

    /**
     * 获取用户的文章交互向量（加权）
     * key=文章ID, value=交互权重
     */
    private Map<Long, Double> getUserArticleVector(Long userId) {
        Map<Long, Double> vector = new HashMap<>();

        // 浏览历史（权重最低）
        List<ArticleViewHistory> viewHistories = articleViewHistoryMapper.selectList(
                new LambdaQueryWrapper<ArticleViewHistory>()
                        .eq(ArticleViewHistory::getUserId, userId)
                        .last("LIMIT 200"));
        for (ArticleViewHistory vh : viewHistories) {
            vector.merge(vh.getArticleId(), WEIGHT_VIEW, Double::sum);
        }

        // 收藏记录
        List<Collect> collects = collectMapper.selectList(
                new LambdaQueryWrapper<Collect>()
                        .eq(Collect::getUserId, userId)
                        .last("LIMIT 200"));
        for (Collect c : collects) {
            vector.merge(c.getArticleId(), WEIGHT_COLLECT, Double::sum);
        }

        // 点赞记录（仅文章类型）
        List<ArticleLike> likes = articleLikeMapper.selectList(
                new LambdaQueryWrapper<ArticleLike>()
                        .eq(ArticleLike::getUserId, userId)
                        .eq(ArticleLike::getTargetType, LikeTargetTypeEnum.ARTICLE)
                        .last("LIMIT 200"));
        for (ArticleLike like : likes) {
            vector.merge(like.getTargetId(), WEIGHT_LIKE, Double::sum);
        }

        return vector;
    }

    /**
     * 获取与目标用户有文章交集的候选用户
     */
    private Set<Long> getCandidateUsers(Long userId, Set<Long> articleIds) {
        Set<Long> candidateUserIds = new HashSet<>();

        // 从浏览历史中找候选用户
        if (!CollectionUtils.isEmpty(articleIds)) {
            List<ArticleViewHistory> viewHistories = articleViewHistoryMapper.selectList(
                    new LambdaQueryWrapper<ArticleViewHistory>()
                            .in(ArticleViewHistory::getArticleId, articleIds)
                            .ne(ArticleViewHistory::getUserId, userId)
                            .last("LIMIT 500"));
            for (ArticleViewHistory vh : viewHistories) {
                if (vh.getUserId() != null) {
                    candidateUserIds.add(vh.getUserId());
                }
            }
        }

        // 从收藏记录中找候选用户
        if (!CollectionUtils.isEmpty(articleIds)) {
            List<Collect> collects = collectMapper.selectList(
                    new LambdaQueryWrapper<Collect>()
                            .in(Collect::getArticleId, articleIds)
                            .ne(Collect::getUserId, userId)
                            .last("LIMIT 500"));
            for (Collect c : collects) {
                candidateUserIds.add(c.getUserId());
            }
        }

        // 从点赞记录中找候选用户
        if (!CollectionUtils.isEmpty(articleIds)) {
            List<ArticleLike> likes = articleLikeMapper.selectList(
                    new LambdaQueryWrapper<ArticleLike>()
                            .in(ArticleLike::getTargetId, articleIds)
                            .eq(ArticleLike::getTargetType, LikeTargetTypeEnum.ARTICLE)
                            .ne(ArticleLike::getUserId, userId)
                            .last("LIMIT 500"));
            for (ArticleLike like : likes) {
                candidateUserIds.add(like.getUserId());
            }
        }

        return candidateUserIds;
    }

    /**
     * 解析逗号分隔的ID列表字符串
     */
    private List<Long> parseIdList(String idsStr) {
        if (idsStr == null || idsStr.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList<>();
        for (String id : idsStr.split(",")) {
            try {
                result.add(Long.parseLong(id.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }
}
