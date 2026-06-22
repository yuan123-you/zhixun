package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.entity.Article;
import com.zhixun.enums.ArticleStatusEnum;
import com.zhixun.mapper.ArticleMapper;
import com.zhixun.service.HotScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 热度分计算服务实现
 * 公式: hot_score = (view*1 + like*5 + comment*3 + collect*8) / max(1, hours_since_publish)^1.5
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotScoreServiceImpl implements HotScoreService {

    private final ArticleMapper articleMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /** 热度分 Redis Key 前缀 */
    private static final String HOT_SCORE_PREFIX = "hot:score:";

    /** 热度分排行 Redis Key */
    private static final String HOT_RANK_KEY = "hot:rank";

    @Override
    public double calculateHotScore(Article article) {
        // 计算互动分
        long viewCount = article.getViewCount() != null ? article.getViewCount() : 0L;
        long likeCount = article.getLikeCount() != null ? article.getLikeCount() : 0L;
        long commentCount = article.getCommentCount() != null ? article.getCommentCount() : 0L;
        long collectCount = article.getCollectCount() != null ? article.getCollectCount() : 0L;

        double interactionScore = viewCount * 1.0 + likeCount * 5.0 + commentCount * 3.0 + collectCount * 8.0;

        // 计算时间衰减因子
        LocalDateTime publishTime = article.getPublishAt() != null ? article.getPublishAt() : article.getCreatedAt();
        long hoursSincePublish = 1;
        if (publishTime != null) {
            hoursSincePublish = Math.max(1, Duration.between(publishTime, LocalDateTime.now()).toHours());
        }

        // 热度分 = 互动分 / 时间衰减^1.5
        double timeDecay = Math.pow(hoursSincePublish, 1.5);
        return interactionScore / Math.max(1.0, timeDecay);
    }

    @Override
    public void updateHotScore(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            return;
        }

        double hotScore = calculateHotScore(article);

        // 存入 Redis Sorted Set
        stringRedisTemplate.opsForZSet().add(HOT_RANK_KEY, String.valueOf(articleId), hotScore);

        // 同时存入单独的 Key 方便查询
        stringRedisTemplate.opsForValue().set(
                HOT_SCORE_PREFIX + articleId,
                String.valueOf(hotScore),
                2, TimeUnit.HOURS);
    }

    @Override
    public void batchUpdateHotScores() {
        log.info("开始批量更新热度分...");

        // 查询所有已发布文章
        List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                        .select(Article::getId, Article::getViewCount, Article::getLikeCount,
                                Article::getCommentCount, Article::getCollectCount,
                                Article::getPublishAt, Article::getCreatedAt));

        if (CollectionUtils.isEmpty(articles)) {
            log.info("没有需要更新热度分的文章");
            return;
        }

        int count = 0;
        for (Article article : articles) {
            try {
                double hotScore = calculateHotScore(article);
                stringRedisTemplate.opsForZSet().add(HOT_RANK_KEY, String.valueOf(article.getId()), hotScore);
                count++;
            } catch (Exception e) {
                log.error("更新文章热度分失败, articleId={}: {}", article.getId(), e.getMessage());
            }
        }

        log.info("批量更新热度分完成，共更新 {} 篇文章", count);
    }
}
