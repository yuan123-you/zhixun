package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import com.zhixun.config.CaffeineCacheConfig;
import com.zhixun.entity.ArticleTag;
import com.zhixun.entity.Tag;
import com.zhixun.entity.UserTagFollow;
import com.zhixun.mapper.ArticleTagMapper;
import com.zhixun.mapper.TagMapper;
import com.zhixun.mapper.UserTagFollowMapper;
import com.zhixun.service.OpenSearchSyncService;
import com.zhixun.service.TagService;
import com.zhixun.vo.TagVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 标签服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final ArticleTagMapper articleTagMapper;
    private final UserTagFollowMapper userTagFollowMapper;
    private final OpenSearchSyncService openSearchSyncService;

    @Override
    @CacheEvict(cacheNames = {CaffeineCacheConfig.CACHE_TAG_LIST, CaffeineCacheConfig.CACHE_TAG_HOT, CaffeineCacheConfig.CACHE_TAG_CLOUD}, allEntries = true)
    public Long create(String name) {
        // 校验标签名称唯一性
        if (StringUtils.hasText(name)) {
            Long count = tagMapper.selectCount(
                    new LambdaQueryWrapper<Tag>().eq(Tag::getName, name));
            if (count > 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "标签名称已存在");
            }
        }
        Tag tag = new Tag();
        tag.setName(name);
        tag.setArticleCount(0L);
        tagMapper.insert(tag);
        return tag.getId();
    }

    @Override
    @CacheEvict(cacheNames = {CaffeineCacheConfig.CACHE_TAG_LIST, CaffeineCacheConfig.CACHE_TAG_HOT, CaffeineCacheConfig.CACHE_TAG_CLOUD}, allEntries = true)
    public void update(Long id, String name) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "标签不存在");
        }
        tag.setName(name);
        tagMapper.updateById(tag);
    }

    @Override
    @CacheEvict(cacheNames = {CaffeineCacheConfig.CACHE_TAG_LIST, CaffeineCacheConfig.CACHE_TAG_HOT, CaffeineCacheConfig.CACHE_TAG_CLOUD}, allEntries = true)
    public void delete(Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "标签不存在");
        }
        tagMapper.deleteById(id);
    }

    @Override
    public Tag getById(Long id) {
        return tagMapper.selectById(id);
    }

    @Override
    @Cacheable(cacheNames = CaffeineCacheConfig.CACHE_TAG_LIST)
    public List<TagVO> list() {
        List<Tag> tags = tagMapper.selectList(
                new LambdaQueryWrapper<Tag>().orderByDesc(Tag::getCreatedAt));
        return tags.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    @Cacheable(cacheNames = CaffeineCacheConfig.CACHE_TAG_HOT, key = "#limit")
    public List<TagVO> hot(int limit) {
        List<Tag> tags = tagMapper.selectList(
                new LambdaQueryWrapper<Tag>()
                        .orderByDesc(Tag::getArticleCount)
                        .last("LIMIT " + limit));
        return tags.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = {CaffeineCacheConfig.CACHE_TAG_LIST, CaffeineCacheConfig.CACHE_TAG_HOT, CaffeineCacheConfig.CACHE_TAG_CLOUD}, allEntries = true)
    public void mergeTag(Long sourceTagId, Long targetTagId) {
        if (sourceTagId.equals(targetTagId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "源标签和目标标签不能相同");
        }

        Tag sourceTag = tagMapper.selectById(sourceTagId);
        if (sourceTag == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "源标签不存在");
        }

        Tag targetTag = tagMapper.selectById(targetTagId);
        if (targetTag == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "目标标签不存在");
        }

        // 查询源标签下所有文章关联
        List<ArticleTag> sourceArticleTags = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getTagId, sourceTagId));

        if (!sourceArticleTags.isEmpty()) {
            // 收集受影响的文章ID，用于后续同步OpenSearch
            List<Long> affectedArticleIds = sourceArticleTags.stream()
                    .map(ArticleTag::getArticleId)
                    .distinct()
                    .collect(Collectors.toList());

            // 查询目标标签下已有的文章ID，避免重复关联
            List<ArticleTag> targetArticleTags = articleTagMapper.selectList(
                    new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getTagId, targetTagId));
            List<Long> targetArticleIds = targetArticleTags.stream()
                    .map(ArticleTag::getArticleId)
                    .collect(Collectors.toList());

            // 将源标签的文章关联转移到目标标签（跳过已存在的关联）
            for (ArticleTag at : sourceArticleTags) {
                if (!targetArticleIds.contains(at.getArticleId())) {
                    at.setTagId(targetTagId);
                    articleTagMapper.updateById(at);
                } else {
                    // 目标标签已有该文章关联，直接删除源标签的关联
                    articleTagMapper.deleteById(at.getId());
                }
            }

            // 更新目标标签的 article_count
            long newTargetCount = articleTagMapper.selectCount(
                    new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getTagId, targetTagId));
            targetTag.setArticleCount(newTargetCount);
            tagMapper.updateById(targetTag);

            // 同步OpenSearch（非关键操作，失败不影响主事务）
            try {
                openSearchSyncService.syncArticles(affectedArticleIds);
            } catch (Exception e) {
                log.error("标签合并同步OpenSearch失败, affectedArticleIds={}: {}", affectedArticleIds, e.getMessage());
            }
        }

        // 删除源标签
        tagMapper.deleteById(sourceTagId);

        log.info("标签合并完成：源标签{}({})合并到目标标签{}({})", sourceTagId, sourceTag.getName(), targetTagId, targetTag.getName());
    }

    @Override
    @Cacheable(cacheNames = CaffeineCacheConfig.CACHE_TAG_CLOUD)
    public List<TagVO> getTagCloud() {
        List<Tag> tags = tagMapper.selectList(
                new LambdaQueryWrapper<Tag>()
                        .gt(Tag::getArticleCount, 0)
                        .orderByDesc(Tag::getArticleCount));
        return tags.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    @CacheEvict(cacheNames = {CaffeineCacheConfig.CACHE_TAG_LIST, CaffeineCacheConfig.CACHE_TAG_HOT, CaffeineCacheConfig.CACHE_TAG_CLOUD}, allEntries = true)
    public void followTag(Long userId, Long tagId) {
        Tag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "标签不存在");
        }
        // 检查是否已关注
        Long count = userTagFollowMapper.selectCount(
                new LambdaQueryWrapper<UserTagFollow>()
                        .eq(UserTagFollow::getUserId, userId)
                        .eq(UserTagFollow::getTagId, tagId));
        if (count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "已关注该标签");
        }
        UserTagFollow follow = new UserTagFollow();
        follow.setUserId(userId);
        follow.setTagId(tagId);
        userTagFollowMapper.insert(follow);
        log.info("用户{}关注标签{}", userId, tagId);
    }

    @Override
    @CacheEvict(cacheNames = {CaffeineCacheConfig.CACHE_TAG_LIST, CaffeineCacheConfig.CACHE_TAG_HOT, CaffeineCacheConfig.CACHE_TAG_CLOUD}, allEntries = true)
    public void unfollowTag(Long userId, Long tagId) {
        userTagFollowMapper.delete(
                new LambdaQueryWrapper<UserTagFollow>()
                        .eq(UserTagFollow::getUserId, userId)
                        .eq(UserTagFollow::getTagId, tagId));
        log.info("用户{}取消关注标签{}", userId, tagId);
    }

    @Override
    public List<TagVO> getFollowedTags(Long userId) {
        List<UserTagFollow> follows = userTagFollowMapper.selectList(
                new LambdaQueryWrapper<UserTagFollow>()
                        .eq(UserTagFollow::getUserId, userId)
                        .orderByDesc(UserTagFollow::getCreatedAt));
        if (follows.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> tagIds = follows.stream()
                .map(UserTagFollow::getTagId)
                .collect(Collectors.toSet());
        List<Tag> tags = tagMapper.selectBatchIds(tagIds);
        return tags.stream().map(tag -> {
            TagVO vo = convertToVO(tag);
            vo.setIsFollowed(true);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean isTagFollowed(Long userId, Long tagId) {
        Long count = userTagFollowMapper.selectCount(
                new LambdaQueryWrapper<UserTagFollow>()
                        .eq(UserTagFollow::getUserId, userId)
                        .eq(UserTagFollow::getTagId, tagId));
        return count > 0;
    }

    @Override
    public List<TagVO> searchTags(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }
        List<Tag> tags = tagMapper.selectList(
                new LambdaQueryWrapper<Tag>()
                        .like(Tag::getName, keyword)
                        .orderByDesc(Tag::getArticleCount)
                        .last("LIMIT 20"));
        return tags.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    @CacheEvict(cacheNames = {CaffeineCacheConfig.CACHE_TAG_LIST, CaffeineCacheConfig.CACHE_TAG_HOT, CaffeineCacheConfig.CACHE_TAG_CLOUD}, allEntries = true)
    public void syncTagArticleCount(Long tagId) {
        if (tagId != null) {
            // 同步指定标签
            long count = articleTagMapper.selectCount(
                    new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getTagId, tagId));
            Tag tag = tagMapper.selectById(tagId);
            if (tag != null) {
                tag.setArticleCount(count);
                tagMapper.updateById(tag);
            }
            log.info("同步标签{}文章数：{}", tagId, count);
        } else {
            // 同步所有标签
            List<Tag> allTags = tagMapper.selectList(null);
            for (Tag tag : allTags) {
                long count = articleTagMapper.selectCount(
                        new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getTagId, tag.getId()));
                tag.setArticleCount(count);
                tagMapper.updateById(tag);
            }
            log.info("同步所有标签文章数完成，共{}个标签", allTags.size());
        }
    }

    /**
     * 将实体转换为 VO
     */
    private TagVO convertToVO(Tag tag) {
        TagVO vo = new TagVO();
        vo.setId(tag.getId());
        vo.setName(tag.getName());
        vo.setArticleCount(tag.getArticleCount());
        vo.setCreatedAt(tag.getCreatedAt());
        return vo;
    }
}
