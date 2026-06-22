package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.common.result.PageResult;
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
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 信息流服务实现
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

    /** 推荐缓存 Key 前缀 */
    private static final String RECOMMEND_KEY_PREFIX = "feed:recommend:";
    /** 推荐缓存 TTL（10分钟） */
    private static final long RECOMMEND_TTL_MINUTES = 10;

    @Override
    public PageResult<ArticleVO> getRecommendFeed(Long userId, Integer refresh, Integer page, Integer pageSize) {
        // 未登录用户返回热门文章
        if (userId == null) {
            return getHotFeed(page, pageSize);
        }

        // 检查是否需要刷新批次
        String refreshKey = null;
        if (refresh != null && refresh == 1) {
            // 生成新的 refresh_key
            refreshKey = UUID.randomUUID().toString().replace("-", "");
            stringRedisTemplate.opsForValue().set(RECOMMEND_KEY_PREFIX + "key:" + userId, refreshKey, RECOMMEND_TTL_MINUTES, TimeUnit.MINUTES);
        } else {
            // 尝试获取已有的 refresh_key
            String existingKey = stringRedisTemplate.opsForValue().get(RECOMMEND_KEY_PREFIX + "key:" + userId);
            if (existingKey != null) {
                refreshKey = existingKey;
            } else {
                // 没有已有批次，生成新的
                refreshKey = UUID.randomUUID().toString().replace("-", "");
                stringRedisTemplate.opsForValue().set(RECOMMEND_KEY_PREFIX + "key:" + userId, refreshKey, RECOMMEND_TTL_MINUTES, TimeUnit.MINUTES);
            }
        }

        // 尝试从缓存获取
        String cacheKey = RECOMMEND_KEY_PREFIX + refreshKey;
        String cachedIds = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedIds != null) {
            // 从缓存中分页
            return getPagedFromCache(cacheKey, page, pageSize);
        }

        // 缓存未命中，生成推荐列表
        List<Long> recommendedArticleIds = generateRecommendedIds(userId);

        // 缓存推荐结果
        if (!recommendedArticleIds.isEmpty()) {
            // 将文章ID列表以逗号分隔存入 Redis
            String idsStr = recommendedArticleIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            stringRedisTemplate.opsForValue().set(cacheKey, idsStr, RECOMMEND_TTL_MINUTES, TimeUnit.MINUTES);
        }

        return getPagedFromCache(cacheKey, page, pageSize);
    }

    @Override
    public PageResult<ArticleVO> getLatestFeed(Integer page, Integer pageSize) {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                .orderByDesc(Article::getIsTop)
                .orderByDesc(Article::getCreatedAt);

        Page<Article> articlePage = new Page<>(page, pageSize);
        Page<Article> result = articleMapper.selectPage(articlePage, wrapper);

        List<ArticleVO> voList = convertToVOList(result.getRecords());
        return new PageResult<>(voList, result.getTotal(), page, pageSize);
    }

    @Override
    public PageResult<ArticleVO> getFollowingFeed(Long userId, Integer page, Integer pageSize) {
        // 查询关注的用户列表
        List<UserFollow> follows = userFollowMapper.selectList(
                new LambdaQueryWrapper<UserFollow>().eq(UserFollow::getFollowerId, userId));

        if (CollectionUtils.isEmpty(follows)) {
            return new PageResult<>(Collections.emptyList(), 0L, page, pageSize);
        }

        List<Long> followUserIds = follows.stream()
                .map(UserFollow::getFollowingId)
                .collect(Collectors.toList());

        // 查询关注用户的已发布文章
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                .in(Article::getAuthorId, followUserIds)
                .orderByDesc(Article::getCreatedAt);

        Page<Article> articlePage = new Page<>(page, pageSize);
        Page<Article> result = articleMapper.selectPage(articlePage, wrapper);

        List<ArticleVO> voList = convertToVOList(result.getRecords());
        return new PageResult<>(voList, result.getTotal(), page, pageSize);
    }

    // ========== 内部方法 ==========

    /**
     * 未登录用户返回热门文章
     */
    private PageResult<ArticleVO> getHotFeed(Integer page, Integer pageSize) {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                .orderByDesc(Article::getViewCount)
                .orderByDesc(Article::getLikeCount)
                .orderByDesc(Article::getCreatedAt);

        Page<Article> articlePage = new Page<>(page, pageSize);
        Page<Article> result = articleMapper.selectPage(articlePage, wrapper);

        List<ArticleVO> voList = convertToVOList(result.getRecords());
        return new PageResult<>(voList, result.getTotal(), page, pageSize);
    }

    /**
     * 基于用户偏好生成推荐文章ID列表
     */
    private List<Long> generateRecommendedIds(Long userId) {
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

        // 查询偏好分类下的文章
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, ArticleStatusEnum.PUBLISHED);

        if (!CollectionUtils.isEmpty(categoryIds)) {
            wrapper.in(Article::getCategoryId, categoryIds);
        }

        // 优先查询偏好分类的文章
        List<Article> preferredArticles = articleMapper.selectList(
                wrapper.orderByDesc(Article::getViewCount).orderByDesc(Article::getCreatedAt).last("LIMIT 100"));

        List<Long> articleIds = preferredArticles.stream()
                .map(Article::getId)
                .collect(Collectors.toList());

        // 如果偏好标签不为空，查询包含这些标签的文章并追加
        if (!CollectionUtils.isEmpty(tagIds)) {
            List<ArticleTag> articleTags = articleTagMapper.selectList(
                    new LambdaQueryWrapper<ArticleTag>().in(ArticleTag::getTagId, tagIds));
            List<Long> tagArticleIds = articleTags.stream()
                    .map(ArticleTag::getArticleId)
                    .filter(id -> !articleIds.contains(id))
                    .distinct()
                    .collect(Collectors.toList());

            if (!tagArticleIds.isEmpty()) {
                List<Article> tagArticles = articleMapper.selectList(
                        new LambdaQueryWrapper<Article>()
                                .eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                                .in(Article::getId, tagArticleIds)
                                .orderByDesc(Article::getViewCount)
                                .last("LIMIT 50"));
                List<Long> tagArticleIdList = tagArticles.stream()
                        .map(Article::getId)
                        .collect(Collectors.toList());
                articleIds.addAll(tagArticleIdList);
            }
        }

        // 如果推荐结果不足，补充热门文章
        if (articleIds.size() < 20) {
            LambdaQueryWrapper<Article> hotWrapper = new LambdaQueryWrapper<>();
            hotWrapper.eq(Article::getStatus, ArticleStatusEnum.PUBLISHED)
                    .notIn(!articleIds.isEmpty(), Article::getId, articleIds)
                    .orderByDesc(Article::getViewCount)
                    .last("LIMIT " + (50 - articleIds.size()));
            List<Article> hotArticles = articleMapper.selectList(hotWrapper);
            articleIds.addAll(hotArticles.stream().map(Article::getId).collect(Collectors.toList()));
        }

        return articleIds;
    }

    /**
     * 从缓存分页获取推荐结果
     */
    private PageResult<ArticleVO> getPagedFromCache(String cacheKey, Integer page, Integer pageSize) {
        String idsStr = stringRedisTemplate.opsForValue().get(cacheKey);
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

        // 获取当前页的文章ID
        List<Long> pageIds = new ArrayList<>();
        for (int i = fromIndex; i < toIndex; i++) {
            pageIds.add(Long.parseLong(idArray[i]));
        }

        // 批量查询文章
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
     * 批量将文章实体列表转换为 VO 列表
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

        // 按文章ID分组标签
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
}
