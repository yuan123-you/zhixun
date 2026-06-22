package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import com.zhixun.config.CaffeineCacheConfig;
import com.zhixun.entity.Category;
import com.zhixun.mapper.CategoryMapper;
import com.zhixun.service.CategoryService;
import com.zhixun.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分类服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    @Override
    @CacheEvict(cacheNames = CaffeineCacheConfig.CACHE_CATEGORY_TREE, allEntries = true)
    public Long create(String name, Long parentId, Integer sort, String icon) {
        Category category = new Category();
        category.setName(name);
        category.setParentId(parentId != null ? parentId : 0L);
        category.setSortOrder(sort != null ? sort : 0);
        category.setIcon(icon);
        category.setStatus(1);
        categoryMapper.insert(category);
        return category.getId();
    }

    @Override
    @CacheEvict(cacheNames = CaffeineCacheConfig.CACHE_CATEGORY_TREE, allEntries = true)
    public void update(Long id, String name, Long parentId, Integer sort, String icon) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        category.setName(name);
        category.setParentId(parentId);
        category.setSortOrder(sort);
        category.setIcon(icon);
        categoryMapper.updateById(category);
    }

    @Override
    @CacheEvict(cacheNames = CaffeineCacheConfig.CACHE_CATEGORY_TREE, allEntries = true)
    public void delete(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        categoryMapper.deleteById(id);
    }

    @Override
    public Category getById(Long id) {
        return categoryMapper.selectById(id);
    }

    @Override
    public List<Category> list() {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, 1)
                        .orderByAsc(Category::getSortOrder)
                        .orderByDesc(Category::getCreatedAt));
    }

    @Override
    @Cacheable(cacheNames = CaffeineCacheConfig.CACHE_CATEGORY_TREE)
    public List<CategoryVO> tree() {
        return buildCategoryTree();
    }

    /**
     * 构建分类树
     */
    private List<CategoryVO> buildCategoryTree() {
        // 查询所有正常状态的分类
        List<Category> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, 1)
                        .orderByAsc(Category::getSortOrder)
                        .orderByDesc(Category::getCreatedAt));

        // 转换为 VO
        List<CategoryVO> voList = categories.stream().map(cat -> {
            CategoryVO vo = new CategoryVO();
            vo.setId(cat.getId());
            vo.setParentId(cat.getParentId());
            vo.setName(cat.getName());
            vo.setSort(cat.getSortOrder());
            vo.setIcon(cat.getIcon());
            vo.setChildren(new ArrayList<>());
            return vo;
        }).collect(Collectors.toList());

        // 构建树形结构
        Map<Long, List<CategoryVO>> parentIdMap = voList.stream()
                .collect(Collectors.groupingBy(vo -> vo.getParentId() == null ? 0L : vo.getParentId()));

        // 递归设置子分类
        voList.forEach(vo -> vo.setChildren(parentIdMap.get(vo.getId())));

        // 返回顶级分类
        return parentIdMap.getOrDefault(0L, new ArrayList<>());
    }
}
