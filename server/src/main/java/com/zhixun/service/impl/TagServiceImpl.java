package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import com.zhixun.config.CaffeineCacheConfig;
import com.zhixun.entity.Tag;
import com.zhixun.mapper.TagMapper;
import com.zhixun.service.TagService;
import com.zhixun.vo.TagVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;

    @Override
    @CacheEvict(cacheNames = {CaffeineCacheConfig.CACHE_TAG_LIST, CaffeineCacheConfig.CACHE_TAG_HOT}, allEntries = true)
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
    @CacheEvict(cacheNames = {CaffeineCacheConfig.CACHE_TAG_LIST, CaffeineCacheConfig.CACHE_TAG_HOT}, allEntries = true)
    public void update(Long id, String name) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "标签不存在");
        }
        tag.setName(name);
        tagMapper.updateById(tag);
    }

    @Override
    @CacheEvict(cacheNames = {CaffeineCacheConfig.CACHE_TAG_LIST, CaffeineCacheConfig.CACHE_TAG_HOT}, allEntries = true)
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
