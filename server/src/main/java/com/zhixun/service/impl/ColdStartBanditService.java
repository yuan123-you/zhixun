package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.entity.Article;
import com.zhixun.entity.ArticleLike;
import com.zhixun.entity.ArticleViewHistory;
import com.zhixun.entity.Category;
import com.zhixun.entity.Collect;
import com.zhixun.enums.LikeTargetTypeEnum;
import com.zhixun.mapper.ArticleLikeMapper;
import com.zhixun.mapper.ArticleViewHistoryMapper;
import com.zhixun.mapper.CategoryMapper;
import com.zhixun.mapper.CollectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ColdStartBanditService {

    private final CategoryMapper categoryMapper;
    private final ArticleViewHistoryMapper articleViewHistoryMapper;
    private final ArticleLikeMapper articleLikeMapper;
    private final CollectMapper collectMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String BANDIT_COUNT_PREFIX = "bandit:count:";
    private static final String BANDIT_REWARD_PREFIX = "bandit:reward:";
    private static final String BANDIT_BEHAVIOR_PREFIX = "bandit:behavior:";
    private static final double EPSILON_START = 0.3;
    private static final double EPSILON_END = 0.1;
    private static final int BEHAVIOR_THRESHOLD = 10;
    private static final long BANDIT_STATE_TTL_DAYS = 30;
    private final Random random = new Random();

    public List<Long> recommendCategories(Long userId, int count) {
        List<Category> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().eq(Category::getStatus, 1));
        if (categories.isEmpty()) {
            return Collections.emptyList();
        }

        double epsilon = calculateEpsilon(userId);

        List<Long> categoryIds = categories.stream().map(Category::getId).collect(Collectors.toList());

        if (random.nextDouble() < epsilon) {
            Collections.shuffle(categoryIds);
            return categoryIds.stream().limit(count).collect(Collectors.toList());
        }

        List<MapEntry> scored = new ArrayList<>();
        for (Long categoryId : categoryIds) {
            double avgReward = getAverageReward(userId, categoryId);
            scored.add(new MapEntry(categoryId, avgReward));
        }

        scored.sort((a, b) -> Double.compare(b.value, a.value));
        return scored.stream().limit(count).map(e -> e.key).collect(Collectors.toList());
    }

    public void updateReward(Long userId, Long categoryId, double reward) {
        String countKey = BANDIT_COUNT_PREFIX + userId + ":" + categoryId;
        String rewardKey = BANDIT_REWARD_PREFIX + userId + ":" + categoryId;

        String countStr = stringRedisTemplate.opsForValue().get(countKey);
        long currentCount = countStr != null ? Long.parseLong(countStr) : 0;
        String rewardStr = stringRedisTemplate.opsForValue().get(rewardKey);
        double currentReward = rewardStr != null ? Double.parseDouble(rewardStr) : 0.0;

        currentCount++;
        currentReward += reward;

        stringRedisTemplate.opsForValue().set(countKey, String.valueOf(currentCount), BANDIT_STATE_TTL_DAYS, TimeUnit.DAYS);
        stringRedisTemplate.opsForValue().set(rewardKey, String.valueOf(currentReward), BANDIT_STATE_TTL_DAYS, TimeUnit.DAYS);

        String behaviorKey = BANDIT_BEHAVIOR_PREFIX + userId;
        stringRedisTemplate.opsForValue().increment(behaviorKey);
        stringRedisTemplate.expire(behaviorKey, BANDIT_STATE_TTL_DAYS, TimeUnit.DAYS);

        log.debug("Bandit奖励更新: userId={}, categoryId={}, reward={}, count={}", userId, categoryId, reward, currentCount);
    }

    public boolean shouldUseBandit(Long userId) {
        String behaviorKey = BANDIT_BEHAVIOR_PREFIX + userId;
        String behaviorStr = stringRedisTemplate.opsForValue().get(behaviorKey);
        int behaviorCount = behaviorStr != null ? Integer.parseInt(behaviorStr) : 0;

        if (behaviorCount < BEHAVIOR_THRESHOLD) {
            return true;
        }

        int dbBehaviorCount = countUserBehaviors(userId);
        if (dbBehaviorCount < BEHAVIOR_THRESHOLD) {
            stringRedisTemplate.opsForValue().set(behaviorKey, String.valueOf(dbBehaviorCount), BANDIT_STATE_TTL_DAYS, TimeUnit.DAYS);
            return true;
        }

        return false;
    }

    private double calculateEpsilon(Long userId) {
        String behaviorKey = BANDIT_BEHAVIOR_PREFIX + userId;
        String behaviorStr = stringRedisTemplate.opsForValue().get(behaviorKey);
        int behaviorCount = behaviorStr != null ? Integer.parseInt(behaviorStr) : 0;

        if (behaviorCount >= BEHAVIOR_THRESHOLD) {
            return EPSILON_END;
        }

        double progress = (double) behaviorCount / BEHAVIOR_THRESHOLD;
        return EPSILON_START - (EPSILON_START - EPSILON_END) * progress;
    }

    private double getAverageReward(Long userId, Long categoryId) {
        String countKey = BANDIT_COUNT_PREFIX + userId + ":" + categoryId;
        String rewardKey = BANDIT_REWARD_PREFIX + userId + ":" + categoryId;

        String countStr = stringRedisTemplate.opsForValue().get(countKey);
        String rewardStr = stringRedisTemplate.opsForValue().get(rewardKey);

        if (countStr == null || rewardStr == null) {
            return 0.0;
        }

        long count = Long.parseLong(countStr);
        double reward = Double.parseDouble(rewardStr);

        return count == 0 ? 0.0 : reward / count;
    }

    private int countUserBehaviors(Long userId) {
        int count = 0;

        Long viewCount = articleViewHistoryMapper.selectCount(
                new LambdaQueryWrapper<ArticleViewHistory>().eq(ArticleViewHistory::getUserId, userId));
        count += viewCount != null ? viewCount.intValue() : 0;

        Long likeCount = articleLikeMapper.selectCount(
                new LambdaQueryWrapper<ArticleLike>()
                        .eq(ArticleLike::getUserId, userId)
                        .eq(ArticleLike::getTargetType, LikeTargetTypeEnum.ARTICLE));
        count += likeCount != null ? likeCount.intValue() : 0;

        Long collectCount = collectMapper.selectCount(
                new LambdaQueryWrapper<Collect>().eq(Collect::getUserId, userId));
        count += collectCount != null ? collectCount.intValue() : 0;

        return count;
    }

    private static class MapEntry {
        final Long key;
        final double value;

        MapEntry(Long key, double value) {
            this.key = key;
            this.value = value;
        }
    }
}
