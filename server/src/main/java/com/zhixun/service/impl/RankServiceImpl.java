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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    @Override
    public List<HotArticleVO> getHotRank(String period, Long categoryId, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 20;
        }

        // 构建缓存 Key
        String cacheKey = RANK_KEY_PREFIX + period + ":" + (categoryId != null ? categoryId : "all");

        // 尝试从缓存获取文章ID列表
        if (stringRedisTemplate != null) {
            String cachedIds = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cachedIds != null && !cachedIds.isEmpty()) {
                List<Long> articleIds = parseIds(cachedIds);
                return buildHotArticleVOList(articleIds, limit);
            }
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
            String idsStr = articleIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            stringRedisTemplate.opsForValue().set(cacheKey, idsStr, RANK_TTL_MINUTES, TimeUnit.MINUTES);
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
        vo.setViewCount(article.getViewCount() != null ? article.getViewCount().intValue() : 0);
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
