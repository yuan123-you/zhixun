package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.entity.Article;
import com.zhixun.entity.ArticleTag;
import com.zhixun.entity.ArticleViewHistory;
import com.zhixun.enums.ArticleStatusEnum;
import com.zhixun.mapper.ArticleMapper;
import com.zhixun.mapper.ArticleTagMapper;
import com.zhixun.mapper.ArticleViewHistoryMapper;
import com.zhixun.mapper.UserPreferredCategoryMapper;
import com.zhixun.mapper.UserPreferredTagMapper;
import com.zhixun.entity.UserPreferredCategory;
import com.zhixun.entity.UserPreferredTag;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentBasedRecommendService {

    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final ArticleViewHistoryMapper articleViewHistoryMapper;
    private final UserPreferredCategoryMapper userPreferredCategoryMapper;
    private final UserPreferredTagMapper userPreferredTagMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String CB_RECOMMEND_PREFIX = "cb:recommend:";
    private static final long CACHE_TTL_MINUTES = 10;

    public List<Long> recommendByContent(Long userId, int count) {
        String cacheKey = CB_RECOMMEND_PREFIX + userId;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            List<Long> cachedList = parseIdList(cached);
            return cachedList.stream().limit(count).collect(Collectors.toList());
        }

        List<UserPreferredCategory> preferredCategories = userPreferredCategoryMapper.selectList(
                new LambdaQueryWrapper<UserPreferredCategory>().eq(UserPreferredCategory::getUserId, userId));
        List<Long> categoryIds = preferredCategories.stream()
                .map(UserPreferredCategory::getCategoryId)
                .collect(Collectors.toList());

        List<UserPreferredTag> preferredTags = userPreferredTagMapper.selectList(
                new LambdaQueryWrapper<UserPreferredTag>().eq(UserPreferredTag::getUserId, userId));
        List<Long> tagIds = preferredTags.stream()
                .map(UserPreferredTag::getTagId)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(categoryIds) && CollectionUtils.isEmpty(tagIds)) {
            return Collections.emptyList();
        }

        Set<Long> viewedArticleIds = getViewedArticleIds(userId);

        Map<Long, Double> articleScores = new HashMap<>();

        if (!CollectionUtils.isEmpty(categoryIds)) {
            LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                    .in(Article::getCategoryId, categoryIds);
            if (!viewedArticleIds.isEmpty()) {
                wrapper.notIn(Article::getId, viewedArticleIds);
            }
            List<Article> categoryArticles = articleMapper.selectList(wrapper);
            for (Article article : categoryArticles) {
                articleScores.merge(article.getId(), 1.0, Double::sum);
            }
        }

        if (!CollectionUtils.isEmpty(tagIds)) {
            List<ArticleTag> articleTags = articleTagMapper.selectList(
                    new LambdaQueryWrapper<ArticleTag>().in(ArticleTag::getTagId, tagIds));
            Map<Long, Long> articleTagCount = new HashMap<>();
            for (ArticleTag at : articleTags) {
                if (!viewedArticleIds.contains(at.getArticleId())) {
                    articleTagCount.merge(at.getArticleId(), 1L, Long::sum);
                }
            }

            for (Map.Entry<Long, Long> entry : articleTagCount.entrySet()) {
                double tagScore = entry.getValue() * 0.5;
                articleScores.merge(entry.getKey(), tagScore, Double::sum);
            }
        }

        List<Long> result = articleScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(count)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (!result.isEmpty()) {
            String idsStr = result.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            stringRedisTemplate.opsForValue().set(cacheKey, idsStr, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        }

        log.debug("内容相似度推荐: userId={}, count={}", userId, result.size());
        return result;
    }

    private Set<Long> getViewedArticleIds(Long userId) {
        List<ArticleViewHistory> viewHistories = articleViewHistoryMapper.selectList(
                new LambdaQueryWrapper<ArticleViewHistory>()
                        .eq(ArticleViewHistory::getUserId, userId)
                        .last("LIMIT 500"));
        return viewHistories.stream()
                .map(ArticleViewHistory::getArticleId)
                .collect(Collectors.toSet());
    }

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
