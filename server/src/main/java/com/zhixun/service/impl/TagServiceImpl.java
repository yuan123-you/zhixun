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
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "标签名称不能为空");
        }
        name = name.trim();
        if (name.length() > 20) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "标签名称不能超过20个字符");
        }
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Tag::getName, name);
        if (tagMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "标签已存在");
        }
        Tag tag = new Tag();
        tag.setName(name);
        tagMapper.insert(tag);
        return tag.getId();
    }

    @Override
    public List<TagVO> list() {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Tag::getArticleCount);
        List<Tag> tags = tagMapper.selectList(wrapper);
        return tags.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<TagVO> hot(int limit) {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Tag::getArticleCount).last("LIMIT " + limit);
        List<Tag> tags = tagMapper.selectList(wrapper);
        return tags.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @CacheEvict(cacheNames = {CaffeineCacheConfig.CACHE_TAG_LIST, CaffeineCacheConfig.CACHE_TAG_HOT, CaffeineCacheConfig.CACHE_TAG_CLOUD}, allEntries = true)
    public void update(Long id, String name) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "标签不存在");
        }
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "标签名称不能为空");
        }
        name = name.trim();
        if (name.length() > 20) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "标签名称不能超过20个字符");
        }
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Tag::getName, name).ne(Tag::getId, id);
        if (tagMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "标签名称已存在");
        }
        tag.setName(name);
        tagMapper.updateById(tag);
    }

    @Override
    @CacheEvict(cacheNames = {CaffeineCacheConfig.CACHE_TAG_LIST, CaffeineCacheConfig.CACHE_TAG_HOT, CaffeineCacheConfig.CACHE_TAG_CLOUD}, allEntries = true)
    @Transactional
    public void delete(Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "标签不存在");
        }
        LambdaQueryWrapper<ArticleTag> atWrapper = new LambdaQueryWrapper<>();
        atWrapper.eq(ArticleTag::getTagId, id);
        articleTagMapper.delete(atWrapper);

        LambdaQueryWrapper<UserTagFollow> utfWrapper = new LambdaQueryWrapper<>();
        utfWrapper.eq(UserTagFollow::getTagId, id);
        userTagFollowMapper.delete(utfWrapper);

        tagMapper.deleteById(id);
    }

    @Override
    public Tag getById(Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "标签不存在");
        }
        return tag;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {CaffeineCacheConfig.CACHE_TAG_LIST, CaffeineCacheConfig.CACHE_TAG_HOT, CaffeineCacheConfig.CACHE_TAG_CLOUD}, allEntries = true)
    public void mergeTag(Long sourceId, Long targetId) {
        Tag source = tagMapper.selectById(sourceId);
        Tag target = tagMapper.selectById(targetId);
        if (source == null || target == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "标签不存在");
        }

        LambdaQueryWrapper<ArticleTag> atWrapper = new LambdaQueryWrapper<>();
        atWrapper.eq(ArticleTag::getTagId, sourceId);
        List<ArticleTag> articleTagList = articleTagMapper.selectList(atWrapper);

        Set<Long> affectedArticleIds = articleTagList.stream()
                .map(ArticleTag::getArticleId)
                .collect(Collectors.toSet());

        for (ArticleTag at : articleTagList) {
            LambdaQueryWrapper<ArticleTag> checkWrapper = new LambdaQueryWrapper<>();
            checkWrapper.eq(ArticleTag::getArticleId, at.getArticleId())
                    .eq(ArticleTag::getTagId, targetId);
            if (articleTagMapper.selectCount(checkWrapper) == 0) {
                at.setTagId(targetId);
                articleTagMapper.updateById(at);
            } else {
                articleTagMapper.deleteById(at.getId());
            }
        }

        LambdaQueryWrapper<UserTagFollow> utfWrapper = new LambdaQueryWrapper<>();
        utfWrapper.eq(UserTagFollow::getTagId, sourceId);
        List<UserTagFollow> followList = userTagFollowMapper.selectList(utfWrapper);
        for (UserTagFollow utf : followList) {
            LambdaQueryWrapper<UserTagFollow> checkWrapper = new LambdaQueryWrapper<>();
            checkWrapper.eq(UserTagFollow::getUserId, utf.getUserId())
                    .eq(UserTagFollow::getTagId, targetId);
            if (userTagFollowMapper.selectCount(checkWrapper) == 0) {
                utf.setTagId(targetId);
                userTagFollowMapper.updateById(utf);
            } else {
                userTagFollowMapper.deleteById(utf.getId());
            }
        }

        tagMapper.deleteById(sourceId);

        Long count = articleTagMapper.selectCount(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getTagId, targetId));
        target.setArticleCount(count);
        tagMapper.updateById(target);

        try {
            openSearchSyncService.syncArticles(affectedArticleIds.stream().collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("Merge tags OpenSearch sync failed: {}", e.getMessage());
        }
    }

    @Override
    @Cacheable(value = CaffeineCacheConfig.CACHE_TAG_CLOUD, unless = "#result == null || #result.isEmpty()")
    public List<TagVO> getTagCloud() {
        return list();
    }

    @Override
    @Transactional
    public void followTag(Long userId, Long tagId) {
        LambdaQueryWrapper<UserTagFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTagFollow::getUserId, userId)
                .eq(UserTagFollow::getTagId, tagId);
        if (userTagFollowMapper.selectCount(wrapper) > 0) {
            return;
        }
        UserTagFollow follow = new UserTagFollow();
        follow.setUserId(userId);
        follow.setTagId(tagId);
        userTagFollowMapper.insert(follow);
    }

    @Override
    @Transactional
    public void unfollowTag(Long userId, Long tagId) {
        LambdaQueryWrapper<UserTagFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTagFollow::getUserId, userId)
                .eq(UserTagFollow::getTagId, tagId);
        userTagFollowMapper.delete(wrapper);
    }

    @Override
    public List<TagVO> getFollowedTags(Long userId) {
        LambdaQueryWrapper<UserTagFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTagFollow::getUserId, userId);
        List<UserTagFollow> follows = userTagFollowMapper.selectList(wrapper);
        if (follows.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> tagIds = follows.stream().map(UserTagFollow::getTagId).collect(Collectors.toList());
        List<Tag> tags = tagMapper.selectBatchIds(tagIds);
        return tags.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public boolean isTagFollowed(Long userId, Long tagId) {
        LambdaQueryWrapper<UserTagFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTagFollow::getUserId, userId)
                .eq(UserTagFollow::getTagId, tagId);
        return userTagFollowMapper.selectCount(wrapper) > 0;
    }

    @Override
    public List<TagVO> searchTags(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Tag::getName, keyword)
                .orderByDesc(Tag::getArticleCount)
                .last("LIMIT 20");
        List<Tag> tags = tagMapper.selectList(wrapper);
        return tags.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @CacheEvict(cacheNames = {CaffeineCacheConfig.CACHE_TAG_LIST, CaffeineCacheConfig.CACHE_TAG_HOT, CaffeineCacheConfig.CACHE_TAG_CLOUD}, allEntries = true)
    public void syncTagArticleCount(Long tagId) {
        if (tagId != null) {
            Long count = articleTagMapper.selectCount(
                    new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getTagId, tagId));
            Tag tag = new Tag();
            tag.setId(tagId);
            tag.setArticleCount(count);
            tagMapper.updateById(tag);
        } else {
            List<Tag> tags = tagMapper.selectList(null);
            for (Tag tag : tags) {
                Long count = articleTagMapper.selectCount(
                        new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getTagId, tag.getId()));
                tag.setArticleCount(count);
                tagMapper.updateById(tag);
            }
        }
    }

    private TagVO toVO(Tag tag) {
        TagVO vo = new TagVO();
        vo.setId(tag.getId());
        vo.setName(tag.getName());
        vo.setArticleCount(tag.getArticleCount());
        return vo;
    }
}
