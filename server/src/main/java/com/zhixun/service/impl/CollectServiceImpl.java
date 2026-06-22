package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import com.zhixun.common.result.PageResult;
import com.zhixun.entity.Article;
import com.zhixun.entity.Collect;
import com.zhixun.mapper.ArticleMapper;
import com.zhixun.mapper.CollectMapper;
import com.zhixun.service.CollectService;
import com.zhixun.vo.ArticleVO;
import com.zhixun.vo.TagVO;
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
import org.springframework.beans.factory.annotation.Autowired;
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
 * 收藏服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectServiceImpl implements CollectService {

    private final CollectMapper collectMapper;
    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final TagMapper tagMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;

    /** Redis 模板（可选，Redis 不可用时为 null） */
    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    /** 收藏状态 Redis Key 前缀 */
    private static final String COLLECT_STATUS_PREFIX = "collect:status:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleCollect(Long userId, Long articleId, String groupName) {
        // 检查文章是否存在
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文章不存在");
        }

        // 查询是否已收藏
        LambdaQueryWrapper<Collect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collect::getUserId, userId)
                .eq(Collect::getArticleId, articleId);
        Collect existingCollect = collectMapper.selectOne(wrapper);

        boolean collected;
        if (existingCollect != null) {
            // 已收藏，取消收藏
            collectMapper.deleteById(existingCollect.getId());
            // 减少收藏计数（SQL 原子操作）
            articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                    .eq(Article::getId, articleId)
                    .gt(Article::getCollectCount, 0)
                    .setSql("collect_count = collect_count - 1"));
            collected = false;
        } else {
            // 未收藏，创建收藏记录
            Collect collect = new Collect();
            collect.setUserId(userId);
            collect.setArticleId(articleId);
            collectMapper.insert(collect);
            // 增加收藏计数（SQL 原子操作）
            articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                    .eq(Article::getId, articleId)
                    .setSql("collect_count = collect_count + 1"));
            collected = true;
        }

        // 获取当前收藏数
        article = articleMapper.selectById(articleId);
        Long collectCount = article != null ? article.getCollectCount() : 0L;

        // 更新 Redis 缓存
        String statusKey = COLLECT_STATUS_PREFIX + userId + ":" + articleId;
        stringRedisTemplate.opsForValue().set(statusKey, collected ? "1" : "0", 1, TimeUnit.HOURS);

        Map<String, Object> result = new HashMap<>();
        result.put("collected", collected);
        result.put("collect_count", collectCount);
        return result;
    }

    @Override
    public PageResult<ArticleVO> getUserCollects(Long userId, String groupName, Integer page, Integer pageSize) {
        // 查询用户收藏记录
        LambdaQueryWrapper<Collect> collectWrapper = new LambdaQueryWrapper<>();
        collectWrapper.eq(Collect::getUserId, userId)
                .orderByDesc(Collect::getCreatedAt);
        Page<Collect> collectPage = new Page<>(page, pageSize);
        Page<Collect> collectResult = collectMapper.selectPage(collectPage, collectWrapper);

        if (collectResult.getRecords().isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L, page, pageSize);
        }

        // 获取收藏的文章ID列表
        List<Long> articleIds = collectResult.getRecords().stream()
                .map(Collect::getArticleId)
                .collect(Collectors.toList());

        // 批量查询文章
        List<Article> articles = articleMapper.selectBatchIds(articleIds);
        List<ArticleVO> voList = convertToVOList(articles);

        return new PageResult<>(voList, collectResult.getTotal(), page, pageSize);
    }

    @Override
    public List<String> getCollectGroups(Long userId) {
        // 查询用户所有收藏记录的分组（当前 Collect 实体没有 groupName 字段，返回空列表）
        // 如果需要分组功能，需在 Collect 实体中添加 groupName 字段
        return Collections.emptyList();
    }

    // ========== 内部方法 ==========

    /**
     * 批量将文章实体列表转换为 VO 列表
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
            vo.setViewCount(article.getViewCount());
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
}
